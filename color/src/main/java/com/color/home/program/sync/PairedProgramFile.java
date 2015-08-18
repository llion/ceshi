package com.color.home.program.sync;

import java.io.File;

import com.color.home.AppController;
import com.color.home.Constants;

import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class PairedProgramFile implements ProgramFile {
    private String mPath;
    private String mFilename;

    public PairedProgramFile(String path, String filename) {
        mPath = path;
        mFilename = filename;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.color.home.program.sync.ProgramFileX#exist()
     */
    @Override
    public boolean exist() {
        if (TextUtils.isEmpty(mPath) || TextUtils.isEmpty(mFilename)) {
            return false;
        }
        return file().isFile();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.color.home.program.sync.ProgramFileX#file()
     */
    @Override
    public File file() {
        return new File(mPath + "/" + mFilename);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.color.home.program.sync.ProgramFileX#getPath()
     */
    @Override
    public String getPath() {
        return mPath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.color.home.program.sync.ProgramFileX#getFilename()
     */
    @Override
    public String getFilename() {
        return mFilename;
    }

    public static ProgramFile fromSettings(SharedPreferences sp) {
        return new PairedProgramFile(sp.getString(Constants.EXTRA_PATH, ""), sp.getString(Constants.EXTRA_FILE_NAME, ""));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.color.home.program.sync.ProgramFileX#play()
     */
    @Override
    public void play() {
        Intent intent = new Intent(Constants.ACTION_PLAY_PROGRAM);
        intent.putExtra(Constants.EXTRA_PATH, mPath);
        intent.putExtra(Constants.EXTRA_FILE_NAME, mFilename);
        AppController.getInstance().sendBroadcast(intent);
    }

}
