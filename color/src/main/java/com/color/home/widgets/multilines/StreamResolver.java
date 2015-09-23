package com.color.home.widgets.multilines;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by zzjd7382 on 9/11/2015.
 */
public class StreamResolver {
    private static final String TAG = StreamResolver.class.getSimpleName();
    private static final boolean DBG = false;

    private String absFilePath;
    private InputStream is = null;
    private ZipInputStream isInsideZip = null;

    public StreamResolver(String absFilePath) {
        this.absFilePath = absFilePath;
    }

    public StreamResolver resolve() throws IOException {
        if (new File(absFilePath + ".zip").exists()) {
            if (DBG) Log.e(TAG, "Zipped. file=" + new File(absFilePath + ".zip"));
            isInsideZip = new ZipInputStream(new BufferedInputStream(new FileInputStream(new File(absFilePath + ".zip"))));
            ZipEntry ze = isInsideZip.getNextEntry();
            if (ze == null) {
                Log.e(TAG, "Bad zip file.absFilePath=" + absFilePath);
                return this;
            }

            if (DBG) {
                Log.d(TAG, "zipped file name is=" + ze.getName());
            }

        } else {
            if (DBG)
                Log.e(TAG, "Not zipped,  use plain pic. trying to find file=" + new File(absFilePath + ".zip"));
            is = new FileInputStream(absFilePath);
        }

        return this;
    }

    public InputStream getReadFromIs() {
        return (isInsideZip == null ? is : isInsideZip);
    }

    public void close() {
        if (isInsideZip != null) {
            try {
                isInsideZip.close();
            } catch (Exception e) {
            }
        }
        if (is != null) {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
    }
}
