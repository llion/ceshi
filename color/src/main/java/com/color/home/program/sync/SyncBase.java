package com.color.home.program.sync;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.FileUtils;
import android.util.Log;

import com.android.volley.Cache.Entry;
import com.color.home.AppController;
import com.color.home.Constants;
import com.google.common.io.Files;

public class SyncBase {
    private final static String TAG = "SyncBase";
    private static final boolean DBG = false;

    private String mAbsPath;
    private String mFileName;
    private String mUrl;
    private Context mContext;

    public SyncBase(String url, String fileName) {
        super();
        mUrl = url;
        mFileName = fileName;
        
        mAbsPath = Constants.getAbsPath(mFileName);
        mContext = AppController.getInstance().getApplicationContext();
    }

    protected String getFileName() {
        return mFileName;
    }

    /**
     * @return true on saved.
     */
    protected boolean saveFile() {
        // Read from the cache entry's data, and write its bytes to the .downing file.
        Entry entry = AppController.getInstance().getCache().get(getUrl());
        if (entry != null) {
            byte[] fileBytes = entry.data;
            boolean isAlreadyDownloaded = isContentSameWithTargetFile(fileBytes);
            if (!isAlreadyDownloaded) {
                try {
                    // Overwrite if exist but content changed.
                    Files.write(fileBytes, new File(getTargetAbsPath()));
                    if (DBG) 
                        Log.d(TAG, "Saved saveFile=" + getTargetAbsPath());
                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "Error write to " + getTargetAbsPath(), e);
                }
            }
        }
    
        return false;
    }

    protected String getTargetAbsPath() {
        return mAbsPath;
    }

    protected String getUrl() {
        return mUrl;
    }

    /**
     * TODO: Make this method more accurate by take into md5 or byte compare. 
     * 
     * @param fileBytes
     * @return
     */
    protected boolean isContentSameWithTargetFile(byte[] fileBytes) {
        return new File(getTargetAbsPath()).length() == fileBytes.length;
    }
    
}