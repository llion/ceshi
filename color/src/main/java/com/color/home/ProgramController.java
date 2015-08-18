package com.color.home;

public class ProgramController {

    private boolean mIsNetwork;
    private String mPath;
    private String mFileName;

    public ProgramController(boolean isNetwork, String path, String fileName) {
        mIsNetwork = isNetwork;
        mPath = path;
        mFileName = fileName;
    }

    public void startProgram() {
//        startExtSdCardPrograms(getCurProgramVsnFile(false, path, fileName));
    }

}
