package com.color.home;

import android.text.TextUtils;

import java.io.File;
import java.util.List;

import com.color.home.ProgramParser.Program;

public class Model {

    private String mPath;
    private String mFileName;
    private List<Program> mPrograms;

    public List<Program> getPrograms() {
        return mPrograms;
    }

    public void setCurProgramPathFile(String path, String fileName) {
        mPath = path;
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
