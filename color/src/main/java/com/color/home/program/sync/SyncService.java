package com.color.home.program.sync;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.color.home.AppController;
import com.color.home.Constants;
import com.color.home.ProgramParser.Program;
import com.color.home.SyncUsbService;
import com.color.home.android.providers.downloads.CLDownloadManager;
//         /storage/sdcard0/Android/data/com.color.home/files/Download
// setprop com.tag.Volley VERBOSE
// adb logcat Volley:V SyncService:V VsnSync:V *:S SyncBase:V SyncedPrograms:V CLIntentService:V AppController:V
// adb logcat Volley:V SyncService:V VsnSync:V *:S SyncBase:V SyncedPrograms:V CLIntentService:V MainActivity:V
// adb logcat SyncService:V VsnSync:V *:S SyncBase:V SyncedPrograms:V CLIntentService:V MainActivity:V
// adb shell setprop log.tag.Volley ERROR
// adb shell setprop log.tag.--------TCT------- ERROR
import com.color.home.netplay.Config;

// Compile the Volley.
// ant jar
// D:\git\volley>copy /Y  bin\volley.jar D:\ws\adt\Color\libs

// adb shell am startservice -a com.color.intent.action.REFRESH com.color.home/com.color.home.program.sync.SyncService
// C:\Users\zzjd7382>touch D:\Work\CityLight\apache-tomcat-7.0.47-windows-x64\apache-tomcat-7.0.47\webapps\ROOT\program\Landscape.vsn D:\Work\CityLight\apache-tomcat-7.0.47-windows-x64\apache-tomcat-7.0.47\webapps\ROOT\program\programs.json D:\Work\CityLight\apache-tomcat-7.0.47-windows-x64\apache-tomcat-7.0.47\webapps\ROOT\program\Rail.vsn

// (DBG.*) = false;
// \1 = false;

//D:\ws\adt\Color\testhost\Shell>adb shell cat /data/local.prop
//log.tag.RenderScript=SILENT
//log.tag.wpa_supplicant=SILENT
//log.tag.InputManager=SILENT
//log.tag.Volley=VERBOSE
//
//D:\ws\adt\Color\testhost\Shell>ls -l /data/local.prop
//ls: /data/local.prop: No such file or directory
//
//D:\ws\adt\Color\testhost\Shell>adb shell ls -l /data/local.prop
//-rw-r--r-- root     root          113 2014-06-13 11:17 local.prop
//
//D:\ws\adt\Color\testhost\Shell>

public class SyncService extends CLIntentService {
    private final static String TAG = "SyncService";
    private static final boolean DBG = false;
    private ProgramRequest mProgramReq;
    private Strategy mStrategy = AppController.getInstance().getStrategy();;

    private static class ProgramRequest extends JsonObjectRequest {
        public ProgramRequest(String url, Response.Listener<JSONObject> listener, Response.ErrorListener error) {
            super(url, null, listener, error);
        }
    }

    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();
    }

    private class ProgramListeners implements Response.Listener<JSONObject>, Response.ErrorListener {
        private String mUrl;

        public ProgramListeners(String url) {
            mUrl = url;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            if (DBG)
                Log.e(TAG, "error response=" + error.getMessage());
        }

        @Override
        public void onResponse(JSONObject response) {
            new ProgramSync(mUrl, Constants.PROGRAMS_JSON, response);
        }

    }

    public static void startService(Context context, Uri uri, String action) {
        context.startService(new Intent(action, uri, context, SyncService.class));
    }

    public static void startService(Context context, Intent intent) {
        Intent serviceIntent = new Intent(intent);
        serviceIntent.setClass(context, SyncService.class);
        context.startService(serviceIntent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (DBG)
            Log.v(TAG, "onHandleIntent. [action=" + action);

        if (Constants.HTTP_SERVER_SUPPORT && Constants.ACTION_REFRESH.equals(action)) {
            final String URL = AppController.getInstance().getConnectivity().getColorControlUri() + "/" + Constants.PROGRAMS_JSON;
            ProgramListeners pl = new ProgramListeners(URL);
            mProgramReq = new ProgramRequest(URL, pl, pl);
            mProgramReq.setShouldDeliverCache(false);
            // add the request object to the queue to be executed
            AppController.getInstance().addToRequestQueue(mProgramReq);
        } else if (CLDownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            ProgramSync.notifyIfAllDownloaded();
        } else if (Constants.ACTION_ALL_DOWNLOAD_FINISHED.equals(action)) {
            mStrategy.onAllDownloaded();
        } else if (Constants.ACTION_FORCE_PLAY_NET_PROGRAMJSON.equals(action)) {
            mStrategy.onForcePlayNet();
        } else if (Constants.ACTION_PROGRAM_STARTED.equals(action)) {
            if (DBG)
                Log.d(TAG, "onHandleIntent. ACTION_PROGRAM_STARTED [data uri=" + intent.getDataString());

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    AppController.getInstance().toast(getApplicationContext(), "> " + Constants.sourceTypeIDToSourceType(Constants.absPathToSourceTypeID(Strategy.getPlayingFolder())) + normalizePlayingVsn(), Toast.LENGTH_SHORT);
                    
                }

                public String normalizePlayingVsn() {
                    String playingVsn = Strategy.getPlayingVsn();
                    if (playingVsn != null) {
                        playingVsn = playingVsn.replace(".vsn", "");
                    }
                    if (TextUtils.isEmpty(playingVsn)) {
                        playingVsn = "";
                    } else {
                        playingVsn = "/" + playingVsn;
                    }
                    return playingVsn;
                }
            });

            if (Constants.absPathToSourceTypeID(Strategy.getPlayingFolder()) == Constants.TYPE_NET) {
                new SyncedPrograms().pruneDeprecatedVsnsButPlaying();
            }

            keepOnlyPlayingVSNUsingResources();

        } else if (Constants.ACTION_COLOR_CONFIG.equals(action)) {
            final String filePath = intent.getStringExtra("path");
            if (DBG) {
                Log.d(TAG, "onHandleIntent. set config. [path=" + filePath);
            }
            
            if (filePath == null) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    Set<String> keySet = extras.keySet();
                    
                    Properties pp = new Properties();
                    if (keySet != null) {
                        for (String key : keySet) {
                            pp.setProperty(key, extras.getString(key, ""));
                        }
                        
                        if (DBG)
                            Log.d(TAG, "onHandleIntent. [pp=" + pp);
                        
                        try {
                            pp.setProperty(Config.UTF_8, "1");
                            AppController.getInstance().getCfg().cfgByProperties(pp);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }
                
            } else {
                AppController.getInstance().getCfg().cfgByFile(new File(filePath));
            }
            
            

        } else {
            if (Constants.ACTION_COLOR_HOME_STARTED.equals(action)) {
                if (DBG)
                    Log.d(TAG, "onHandleIntent. [app started. 1. chances for scheduling a net refresh, 2. strategy onHomeStarted.");
                mStrategy.onHomeStarted();
                AppController.getInstance().setStarted();
            } else if (Constants.ACTION_USB_SYNCED.equals(action)) {
                if (DBG)
                    Log.d(TAG, "onHandleIntent. [ACTION_USB_SYNCED.");
                mStrategy.onUsbSynced();
            } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                if (DBG)
                    Log.d(TAG, "onHandleIntent. [mounted getDataString=" + intent.getDataString());
                if (!AppController.getInstance().isStarted()) {
                    if (DBG)
                        Log.d(TAG, "onHandleIntent. [ACTION_MEDIA_MOUNTED, but activity not fully created.");
                    return;
                }
                if (intent.getData().toString().endsWith("0")) {
                    File file = new File(Constants.FOLDER_USB_0 + "/Color.apk");
                    if (file.exists() && file.length() > 0) {
                        if (DBG)
                            Log.d(TAG, "onHandleIntent. ACTION_MEDIA_MOUNTED [Do not sync as Color.apk file exist=" + file);

                        return;
                    }

                    AppController.getInstance().getCfg().cfgFromUsb();
                    if (DBG)
                        Log.i(TAG,
                                "onHandleIntent. ACTION_MEDIA_MOUNTED. " + "Vsn file in this mounted Usb ="
                                        + Constants.getDefaultUsbVsnFile());
                    // Logic: only sync upon usb has vsn file presentation.
                    if (Constants.getDefaultUsbVsnFile() != null) {
                        // Sync files in the usb and in the internal usb map folder.
                        // Parallel: the following service's thread differ from the appcontroller handler.
                        if (!new File(Constants.FOLDER_USB_0 + "/nocopy.txt").exists()) {
                            SyncUsbService.startService();
                        }

                        mStrategy.onUsbMounted();
                    }
                    // } else if (intent.getData().toString().endsWith("/mnt/sdcard")) {
                    // mStrategy.onMntSdcardMounted();
                }
            } else if (Intent.ACTION_MEDIA_REMOVED.equals(action) && intent.getData().toString().endsWith("0")) {
                mStrategy.onUsbRemoved();
            }
        }

    }

    public void keepOnlyPlayingVSNUsingResources() {
        String path = AppController.getInstance().getModel().getPath();
        String fileName = AppController.getInstance().getModel().getFileName();

        List<Program> programs = AppController.getInstance().getModel().getPrograms();
        if (DBG)
            Log.d(TAG, "keepOnlyPlayingVSNUsingResources. [fileName=" + fileName);

        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(fileName) || programs == null) {
            Log.d(TAG, "NO program. ");
            return;
        }

        if (!path.startsWith("/mnt/sdcard")) {
            if (DBG)
                Log.d(TAG, "keepOnlyPlayingVSNUsingResources. [Not /mnt/sdcard, maybe USB key, so ignore.");
            return;
        }

        String resFolder = fileName.replace(".vsn", ".files");
        File resFolderFile = new File(path, resFolder);
        String[] listFiles = resFolderFile.list();
        if (listFiles == null) {
            Log.e(TAG, "Not a dir resFolder=" + resFolderFile);
            return;
        }
        Set<String> collectPathFiles = Program.collectFiles(programs);

        Set<String> pureFilenames = new HashSet<String>(collectPathFiles.size());

        // "/new.files/xx"
        final String folderPattern = "/" + resFolder + "/";
        for (String collectFile : collectPathFiles) {
            pureFilenames.add(collectFile.replace(folderPattern, ""));
        }

        if (DBG) {
            Log.d(TAG, "onHandleIntent. [path=" + path + ", fileName=" + fileName
                    + ", resFolderFile=" + resFolderFile);
            for (int i = 0; i < listFiles.length; i++) {
                Log.d(TAG, "onHandleIntent. [list file=" + listFiles[i]);
            }
            for (String file : collectPathFiles) {
                Log.d(TAG, "onHandleIntent. [file collected=" + file);
            }
            for (String filename : pureFilenames) {
                Log.d(TAG, "onHandleIntent. [filename=" + filename);
            }
        }

        List<String> mutable = new ArrayList<String>(Arrays.asList(listFiles));
        mutable.removeAll(pureFilenames);

        for (String shouldDel : mutable) {
            if (shouldDel.endsWith(".zip")) {
                if (DBG) {
                    Log.e(TAG, "onHandleIntent. [do not del zip file=" + shouldDel);
                }
                continue;
            }
            File toDel = new File(path, resFolder + "/" + shouldDel);
            boolean deleted = toDel.delete();

            if (DBG) {
                Log.d(TAG, "onHandleIntent. [shouldDel=" + shouldDel);
                Log.d(TAG, "onHandleIntent. [toDel=" + toDel);
                Log.d(TAG, "onHandleIntent. [deleted=" + deleted);
            }

        }

    }
}
