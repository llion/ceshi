package com.color.home;

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
