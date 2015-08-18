package com.color.home.netplay;

import java.util.List;

import android.util.Log;

public class FilesInDownloadingDb {
    private final static String TAG = "FilesInDb";
    private static final boolean DBG = false;

    int mStatus;
    int mControl;
    String mData;
    long mId;

    public FilesInDownloadingDb(int status, int control, String data, long id) {
        mStatus = status;
        mControl = control;
        mData = data;
        mId = id;
    }

    @Override
    public String toString() {
        return "FilesInDb [mStatus=" + mStatus + ", mControl=" + mControl + ", mData=" + mData + ", mId=" + mId + "]";
    }

    public static int fileDownloadStatus(List<FilesInDownloadingDb> filesInDb, String aRelativeFile) {
        for (FilesInDownloadingDb file : filesInDb) {
            if (file.mData.endsWith(aRelativeFile)) {
                if (DBG)
                    Log.i(TAG, "checkFile. Exist filesInDb=" + filesInDb + ", aRelativeFile=" + aRelativeFile + ", status=" + file.mStatus
                            + " , Thread=" + Thread.currentThread());
                return file.mStatus;
            }
        }
        return -1;
    }

    public static long getId(List<FilesInDownloadingDb> filesInDb, String aRelativeFile) {
        for (FilesInDownloadingDb file : filesInDb) {
            if (file.mData.endsWith(aRelativeFile)) {
                if (DBG)
                    Log.i(TAG, "getId. Exist filesInDb=" + filesInDb + ", aRelativeFile=" + aRelativeFile + ", status=" + file.mStatus
                            + " , Thread=" + Thread.currentThread());
                return file.mId;
            }
        }
        // Should not happend.
        return 0;
    }

}
