package com.color.home.program.sync;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.Constants;

public class ProgramSync extends SyncBase {
    private final static String TAG = "ProgramSync";
    private static final boolean DBG_LIFECYCLE = false;;
    private Set<String> mVsnFiles;

    public ProgramSync(String url, String fileName, JSONObject json) {
        super(url, fileName);

        try {
            if (DBG_LIFECYCLE)
                Log.v(TAG, "onResponse Response=" + json.toString(4));

            saveFile();

            JSONArray jsonArray = json.getJSONArray("vsns");
            if (DBG_LIFECYCLE)
                Log.d(TAG, "onResponse. [jsonArray=" + jsonArray.toString(4));
            final int N = jsonArray.length();
            mVsnFiles = new HashSet<String>(N);
            for (int i = 0; i < N; i++) {
                String name = jsonArray.getJSONObject(i).getString("name");
                if (DBG_LIFECYCLE)
                    Log.d(TAG, "onResponse. [name=" + name);
                mVsnFiles.add(name);
            }

            Iterator<String> it = mVsnFiles.iterator();
            while (it.hasNext()) {
                String name = it.next();
                long size = download(name);
                if (size > 0L) {
                    new VsnSync(name, size);
                }
            }
            
            SyncedPrograms syncedProgs = new SyncedPrograms();
            // Prune not playing vsn, so that, downloading files can be canceled.
            syncedProgs.pruneDeprecatedVsnsButPlaying();
//          After all vsn saved and parsed, check whether that all the files have been downloaded already.
            notifyIfAllDownloaded();
        } catch (JSONException e) {
            Log.e(TAG, "Error json.", e);
        }
    }

    private long download(final String vsnName) {
        if (DBG_LIFECYCLE)
            Log.d(TAG, "Downloading [vsnName=" + vsnName);

        final String URL = AppController.getInstance().getConnectivity().getProgramFolderUri() + "/" + vsnName;
        return DownloadUtils.downloadFileTo(URL, Constants.getAbsPath(vsnName)) ;
    }

    public static void notifyIfAllDownloaded() {
        boolean isAllDownloaded = isAllDownloaded();
        if (isAllDownloaded) {
            if (DBG_LIFECYCLE)
                Log.d(TAG, "notifyOnAllDownloaded. [all downloaded.");
            // All downloaded.
            AppController.getInstance().sendBroadcast(new Intent(Constants.ACTION_ALL_DOWNLOAD_FINISHED));
        } else {
            if (DBG_LIFECYCLE)
                Log.d(TAG, "notifyOnAllDownloaded. [Still downloading some files");
        }
    }

    private static boolean isAllDownloaded() {
        return DownloadUtils.queryStatusUnsuccessfull(AppController.getInstance().getContentResolver()) == 0;
    }

}
