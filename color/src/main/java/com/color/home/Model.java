package com.color.home;

import android.text.TextUtils;
import android.util.Log;

import com.color.home.ProgramParser.Program;

import java.io.File;
import java.util.List;

public class Model {

    private static final boolean DBG = false;
    private static final String TAG = "Model";
    private String mPath;
    private String mFileName;
    private List<Program> mPrograms;

    public List<Program> getPrograms() {
        return mPrograms;
    }

    public void setCurProgramPathFile(File vsnFile) {
        setCurProgramPathFile(vsnFile.getParentFile().getAbsolutePath(), vsnFile.getName());
    }

    public void setCurProgramPathFile(String path, String fileName) {
        mPath = AppController.normPathNameInternalSdToSdcard(path);

        if (DBG)
            Log.d(TAG, "setCurProgramPathFile=" + path
            + ", normed to=" + mPath);

        mFileName = fileName;
    }


    public File getFile() {
        if (TextUtils.isEmpty(mPath) || TextUtils.isEmpty(mFileName)) {
            return null;
        }
        return new File(mPath, mFileName);
    }
    
    public String getPath() {
        return mPath;
    }

    public String getFileName() {
        return mFileName;
    }

    public void setPrograms(List<Program> programs) {
        mPrograms = programs;
    }
    

}
