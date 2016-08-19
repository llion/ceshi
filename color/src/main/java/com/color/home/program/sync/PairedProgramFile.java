package com.color.home.program.sync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.Constants;

import java.io.File;

public class PairedProgramFile implements ProgramFile {
    private static final boolean DBG = false;
    private static final String TAG = "PairedProgramFile";
    private String mPath;
    private String mFilename;

    public PairedProgramFile(String path, String fileName) {
        String pathNorm = TextUtils.isEmpty(path) ? "" : path;
        // We saw ACTION_PROGRAM_STARTED [data uri=file:///mnt/internal_sd/Android/data/com.color.home/files/Ftp/program/%E6%88%91%E6%83%B3%E8%A6%81w_c8cc_c8cc5d93d28d705f3133b52440bb0270_5888.vsn
        // /mnt/internal_sd...
        String fileNameNorm = TextUtils.isEmpty(fileName) ? "" : fileName;

        mPath = AppController.normPathNameInternalSdToSdcard(pathNorm);
        mFilename = fileNameNorm;

        if (DBG)
            Log.d(TAG, "PairedProgramFile path=" + path
                    + ", normed to=" + mPath
            + ", fileNameNorm=" + fileNameNorm);
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
        return new File(mPath, mFilename);
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
