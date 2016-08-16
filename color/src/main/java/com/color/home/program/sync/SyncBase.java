package com.color.home.program.sync;

import android.content.Context;

import com.color.home.AppController;
import com.color.home.Constants;

import java.io.File;

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