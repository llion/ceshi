package com.color.home.program.sync;

import java.io.File;

public interface ProgramFile {

    public abstract boolean exist();

    public abstract File file();

    public abstract String getPath();

    public abstract String getFilename();

    public abstract void play();

}