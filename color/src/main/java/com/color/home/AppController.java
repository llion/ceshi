package com.color.home;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.Volley;
import com.color.home.android.providers.downloads.CLDownloadManager;
import com.color.home.android.providers.downloads.CLStorageManager;
import com.color.home.netplay.Config;
import com.color.home.netplay.ConfigAPI;
import com.color.home.netplay.Connectivity;
import com.color.home.netplay.FtpServer;
import com.color.home.program.sync.PollingUtils;
import com.color.home.program.sync.Strategy;
import com.color.home.program.sync.SyncService;
import com.color.home.provider.ColorContract;

public class AppController extends Application {

    private final static String TAG = "AppController";
    private static final boolean DBG = false;
    public static String sCharset = "GBK";

    /**
     * Global request queue for Volley
     */
    private RequestQueue mRequestQueue;
    private static Handler sHandler;
    static {
        final HandlerThread thread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        sHandler = new Handler(thread.getLooper());
    }


    public static class MyBitmap {
        private Bitmap mBitmap;

        public Bitmap getBitmap() {
            return mBitmap;
        }

        public int mSingleLineWidth;
        public int mSingleLineHeight;

        public MyBitmap(Bitmap bitmap, int singleLineWidth, int singleLineHeight) {
            mBitmap = bitmap;
            mSingleLineWidth = singleLineWidth;
            mSingleLineHeight = singleLineHeight;
        }

    }

    // Get max available VM memory, exceeding this amount will throw an
    // OutOfMemory exception. Stored in kilobytes as LruCache takes an
    // int in its constructor.
    static final int sMaxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    static {
    if (DBG)
        Log.d(TAG, "sMaxMemory=. [" + sMaxMemory);
    }
    // Use 1/3th of the available memory for this memory cache.
    // static final int sCacheSize = 500;
    static final int sCacheSize = sMaxMemory / 3;

    LruCache<String, MyBitmap> sMemoryCache = new LruCache<String, MyBitmap>(sCacheSize) {
        @Override
        protected int sizeOf(String key, MyBitmap mybitmap) {
            // The cache size will be measured in kilobytes rather than
            // number of items.
            return mybitmap.mBitmap.getByteCount() / 1024;
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, MyBitmap oldValue, MyBitmap newValue) {
            // Donot recycle here, as a bitmap could be using it.
            // if (oldValue != null)
            // oldValue.recycle();

            if (DBG)
                Log.d(TAG,
                        "entryRemoved. [reqest GC, oldValue=" + oldValue + ", newValue=" + newValue + ", Thread=" + Thread.currentThread());
            System.gc();

            super.entryRemoved(evicted, key, oldValue, newValue);
        }
    };
    /**
     * A singleton instance of the application class for easy access in other places
     */
    private static AppController sInstance;

    private Config mCfg;
    private Locale locale = null;

    private Connectivity mConnectivity;

    private CLDownloadManager mDownloadMngr;

    private Cache mCache;
    private Model mModel;
    private Strategy mStrategy;

    private File mDownloadDataDir;
    private SharedPreferences mSettings;

    private HashMap<String, Typeface> mTypefaces = new HashMap<String, Typeface>();

    public SharedPreferences getSettings() {
        return mSettings;
    }

    public Cache getCache() {
        return mCache;
    }

    public CLDownloadManager getDownloadMngr() {
        return mDownloadMngr;
    }

    public Typeface generateTypeface(String fontName) {
        try {

            if (Constants.FONT_KAI.equals(fontName)) {
                return Typeface.createFromFile(Constants.FONT_PATH + "simkai.ttf");
            } else if (Constants.FONT_HEI.equals(fontName)) {
                return Typeface.createFromFile(Constants.FONT_PATH + "simhei.ttf");
            } else if (Constants.FONT_FANGSONG.equals(fontName)) {
                return Typeface.createFromFile(Constants.FONT_PATH + "simfang.ttf");
            } else if (Constants.FONT_SONG.equals(fontName)) {
                return Typeface.createFromFile(Constants.FONT_PATH + "simsun.ttf");
            } else if (Constants.FONT_LISHU.equals(fontName)) {
                return Typeface.createFromFile(Constants.FONT_PATH + "simli.ttf");
            }
        } catch (Exception e) {
            Log.e(TAG, "generateTypeface. [fontName=" + fontName, e);
        }
        return null;
    }

    public void addBitmapToMemoryCache(String key, MyBitmap bitmap) {
        if (DBG)
            Log.d(TAG, "addBitmapToMemoryCache. [key=" + key + ", bitmap=" + bitmap);
        if (getBitmapFromMemCache(key) == null) {
            sMemoryCache.put(key, bitmap);
        }
    }

    public MyBitmap getBitmapFromMemCache(String key) {
        if (DBG)
            Log.d(TAG, "getBitmapFromMemCache. [key=" + key + ", bitmap=" + sMemoryCache.get(key));

        return sMemoryCache.get(key);
    }

    public void markProgram(String path, String fileName) {
        String pathNorm = TextUtils.isEmpty(path) ? "" : path;
        String fileNameNorm = TextUtils.isEmpty(fileName) ? "" : fileName;
        
        Intent intent = new Intent(Constants.ACTION_CURRENT_PROG_INDEX);
        intent.putExtra(Constants.EXTRA_INDEX, 999);
        intent.putExtra(Constants.EXTRA_PATH, pathNorm);
        intent.putExtra(Constants.EXTRA_FILE_NAME, fileNameNorm);
        sendStickyBroadcast(intent);

        Editor edit = getSettings().edit();
        edit.putString(Constants.EXTRA_PATH, pathNorm);
        edit.putString(Constants.EXTRA_FILE_NAME, fileNameNorm);
        edit.apply();
        
        ContentValues values = new ContentValues();
        values.put(ColorContract.COLUMN_PROGRAM_SOURCE, "updated path");
        values.put(ColorContract.COLUMN_PROGRAM_SOURCE, Constants.absFolderToSourceType(pathNorm));
        values.put(ColorContract.COLUMN_PROGRAM_PATH, pathNorm);
        values.put(ColorContract.COLUMN_PROGRAM_FILENAME, fileNameNorm);
        int update = getContentResolver()
                .update(Uri.withAppendedPath(ColorContract.PROGRAM_CONTENT_URI, "playing"), values, null, null);
        if (DBG)
            Log.i(TAG, "markProgram. [PROGRAM_CONTENT_URI update count=" + update);
        
    }

    public Typeface getTypeface(String fontName) {
        Typeface tf = null;
        if (mTypefaces.containsKey(fontName)) {
            tf = mTypefaces.get(fontName);
        } else {
            tf = generateTypeface(fontName);
            if (tf != null)
                mTypefaces.put(fontName, tf);
        }
        return tf;
    }

    public Connectivity getConnectivity() {
        return mConnectivity;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (DBG)
            Log.d(TAG, "onCreate. [sMaxMemory=" + sMaxMemory);
        // initialize the singleton
        sInstance = this;

        toast(this, getString(R.string.version_name), Toast.LENGTH_LONG);

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration config = getBaseContext().getResources().getConfiguration();
        String lang = mSettings.getString(ConfigAPI.ATTR_LOCALE, "");
        sCharset = mSettings.getString(ConfigAPI.ATTR_TEXT_CHARSET, "GBK");

        if (!"".equals(lang) && !config.locale.getLanguage().equals(lang))
        {
            locale = new Locale(lang);
            Locale.setDefault(locale);
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        }

        sHandler.postAtFrontOfQueue(new Runnable() {

            @Override
            public void run() {
                if (DBG)
                    Log.d(TAG, "run what's posted at front. [postAtFrontOfQueue.");
                // Do not setup, as the dimension is fixed to that in the kernel. 20150507.
//                 setup();
//                String displayMode = " 1920x1080p-60 ";
                String displayMode = " 1280x720p-60 ";
                setup(displayMode);
//                 setup480();
                mCfg = new Config(AppController.this, Constants.FOLDER_USB_0);
                mConnectivity = new Connectivity(AppController.this);


            }
        });

        mDownloadMngr = CLDownloadManager.getInst(getContentResolver(), getPackageName());

        mModel = new Model();
        mStrategy = new Strategy();

        try {
            mDownloadDataDir = CLStorageManager.getDownloadDataDirectory(this);
            if (mDownloadDataDir != null) {
                mDownloadDataDir.createNewFile();
            } else {
                Log.d(TAG, "onCreate. [mDownloadDataDir is NULL.");
            }
        } catch (IOException e) {
            Log.e(TAG, "onCreate", e);
        }

    }

//    public void setup() {
//        // XXX: adb shell "echo ftp.service.enabled=false> /mnt/usb_storage/USB_DISK0/udisk0/config.txt"
//        // is not a valid Property file for Java.
//        if (DBG)
//            Log.d(TAG, "setup1080 ");
//
//        ArrayList<String> commands = new ArrayList<String>();
//        commands.add("echo 1920x1080p-60 > /sys/class/display/display0.HDMI/mode");
////        commands.add("echo 1920x1080p-60 > /sys/class/display/display0.HDMI/mode");
//        if (DBG)
//            Log.d(TAG, "setup1080. [commands 0=" + commands.get(0));
//
//        Process process;
//        try {
//            process = FtpServer.RunAsRoot(commands.toArray(new String[0]));
//
//            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            {
//                String line = br.readLine();
//                while (line != null) {
//                    if (DBG)
//                        Log.d(TAG, "line=" + line);
//                    line = br.readLine();
//                }
//            }
//            br.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void setup(String displayMode) {
        // XXX: adb shell "echo ftp.service.enabled=false> /mnt/usb_storage/USB_DISK0/udisk0/config.txt"
        // is not a valid Property file for Java.
        if (DBG)
            Log.d(TAG, "setup " + displayMode);

        ArrayList<String> commands = new ArrayList<String>();
        commands.add("echo " +
        		displayMode +
        		" > /sys/class/display/display0.HDMI/mode");
//        commands.add("iwconfig wlan0 power off");
        if (DBG)
            Log.d(TAG, "setup =" + displayMode + " [commands 0=" + commands.get(0));

        Process process;
        try {
            process = FtpServer.RunAsRoot(commands.toArray(new String[0]));

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            {
                String line = br.readLine();
                while (line != null) {
                    if (DBG)
                        Log.d(TAG, "line=" + line);
                    line = br.readLine();
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//    public void setup480() {
//        // XXX: adb shell "echo ftp.service.enabled=false> /mnt/usb_storage/USB_DISK0/udisk0/config.txt"
//        // is not a valid Property file for Java.
//        if (DBG)
//            Log.d(TAG, "setup480 ");
//        
//        ArrayList<String> commands = new ArrayList<String>();
//        commands.add("echo 720x480p-60 > /sys/class/display/display0.HDMI/mode");
//        if (DBG)
//            Log.d(TAG, "setup480. [commands 0=" + commands.get(0));
//        
//        Process process;
//        try {
//            process = FtpServer.RunAsRoot(commands.toArray(new String[0]));
//            
//            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            {
//                String line = br.readLine();
//                while (line != null) {
//                    if (DBG)
//                        Log.d(TAG, "line=" + line);
//                    line = br.readLine();
//                }
//            }
//            br.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (locale != null)
        {
            newConfig.locale = locale;
            Locale.setDefault(locale);
            getBaseContext().getResources().updateConfiguration(newConfig, getBaseContext().getResources().getDisplayMetrics());
        }
    }

    public Strategy getStrategy() {
        return mStrategy;
    }

    public File getDownloadDataDir() {
        return mDownloadDataDir;
    }

    public Model getModel() {
        return mModel;
    }

    /**
     * @return ApplicationController singleton instance
     */
    public static synchronized AppController getInstance() {
        return sInstance;
    }

    /**
     * @return The Volley Request queue, the queue will be created if it is null
     */
    public RequestQueue getRequestQueue() {
        // lazy initialize the request queue, the queue instance will be
        // created when it is accessed for the first time
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext(), 1, sHandler);
            mCache = mRequestQueue.getCache();
            // Do not clear, as cur algorithm is if there is ALL DOWNLOADED broadcast, start net play.
            // But we could be playing USB or Synced USB.
            // mCache.clear();
        }

        return mRequestQueue;
    }

    public Config getCfg() {
        return mCfg;
    }

    public static Handler getHandler() {
        return sHandler;
    }

    /**
     * Adds the specified request to the global queue, if tag is specified then it is used else Default TAG is used.
     * 
     * @param req
     * @param tag
     */
    public <T> void addToRequestQueue(Request<T> req, String tag) {
        // set the default tag if tag is empty
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);

        VolleyLog.d("Adding request to queue: %s", req.getUrl());

        getRequestQueue().add(req);
    }

    /**
     * Adds the specified request to the global queue using the Default TAG.
     * 
     * @param req
     */
    public <T> void addToRequestQueue(Request<T> req) {
        // set the default tag if tag is empty
        req.setTag(TAG);

        getRequestQueue().add(req);
    }

    @Override
    public void onTerminate() {
        if (DBG)
            Log.d(TAG, "onTerminate. [");
        
        if (com.color.home.Constants.HTTP_SERVER_SUPPORT) {
            PollingUtils.stopPollingService(this, SyncService.class, Constants.ACTION_REFRESH);
        }
        super.onTerminate();
    }

    /**
     * Cancels all pending requests by the specified TAG, it is important to specify a TAG so that the pending/ongoing requests can be
     * cancelled.
     * 
     * @param tag
     */
    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }

    public static String getPlayingRootPath() {
        return getInstance().getModel().getPath();
    }

    private boolean mActivityStarted;

    public synchronized void setStarted() {
        mActivityStarted = true;
    }

    public synchronized boolean isStarted() {
        return mActivityStarted;
    }

    public void toast(final Context context, final String text, final int duration) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            toastMe(context, text, duration);
        } else {
            sHandler.post(new Runnable() {
                
                @Override
                public void run() {
                    toastMe(context, text, duration);
                }
            });
        }
    }

    private void toastMe(Context context, String text, int duration) {
        Toast cheatSheet = new Toast(context);
        TextView textView = new TextView(context);
        textView.setBackgroundColor(Color.BLACK);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(Color.WHITE);
        cheatSheet.setView(textView);
        cheatSheet.setDuration(duration);
        cheatSheet.setGravity(Gravity.TOP | Gravity.LEFT, 0, 0);
        cheatSheet.show();
    }
    

}