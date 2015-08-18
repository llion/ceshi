package com.color.home.program.sync;

import java.io.File;

import android.content.Intent;

import com.color.home.AppController;
import com.color.home.Constants;

public class FileProgramFile implements ProgramFile {
    private File mFile;

    @Override
    public boolean exist() {
        return mFile.isFile();
    }

    @Override
    public File file() {
        return mFile;
    }

    @Override
    public String getPath() {
        return mFile.getParentFile().getAbsolutePath();
    }

    @Override
    public String getFilename() {
        return mFile.getName();
    }
    
    
    public FileProgramFile(File file) {
        mFile = file;
    }

    @Override
    public void play() {
        Intent intent = new Intent(Constants.ACTION_PLAY_PROGRAM);
        intent.putExtra(Constants.EXTRA_PATH, mFile.getParentFile().getAbsolutePath());
        intent.putExtra(Constants.EXTRA_FILE_NAME, mFile.getName());
        AppController.getInstance().sendBroadcast(intent);
    }

}
