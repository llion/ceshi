package com.color.home.program.sync;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.Constants;
import com.color.home.ProgramParser;
import com.color.home.ProgramParser.Program;
import com.color.home.android.providers.downloads.CLDownloadManager;
import com.color.home.android.providers.downloads.CLStorageManager;
import com.color.home.android.providers.downloads.Downloads;
import com.color.home.netplay.FilesInDownloadingDb;
import com.google.common.primitives.Longs;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VsnSync extends SyncBase {
    final static String TAG = "VsnSync";
    private static final boolean DBG = false;

    private CLDownloadManager mDownloadMngr;
    private Context mContext;
    private Long mSize;

    public VsnSync(String vsnName, long size) {
        super(null, vsnName);
        mSize = size;

        mContext = AppController.getInstance().getApplicationContext();
        mDownloadMngr = AppController.getInstance().getDownloadMngr();

        parseAndDownloadAll();
    }

    public AllNDowningFiles parseAndDownloadAll() {
        if (DBG)
            Log.i(TAG, "parseAndDownloadAll. mAbsDownloadingFileName=" + getTargetAbsPath());
        ProgramParser pp = new ProgramParser(new File(getTargetAbsPath()));
        InputStream in = null;
        List<Program> programs = null;
        try {
            in = new BufferedInputStream(new FileInputStream(getTargetAbsPath()));
            programs = pp.parse(in);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "parseAndDownloadAll", e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "parseAndDownloadAll", e);
        } catch (IOException e) {
            Log.e(TAG, "parseAndDownloadAll", e);
        }finally {
            if(in != null){
                try{
                    in.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        if (programs == null) {
            Log.e(TAG, "parse. [Program is null.");
            return null;
        }
        final Set<String> allRelFiles = Program.collectFiles(programs);
        final Set<Long> downloadingFileIds = new HashSet<Long>(10);

        List<FilesInDownloadingDb> filesInDb = new ArrayList<FilesInDownloadingDb>();
        // Collect all the files that already in the downloading database.
        Cursor cursor = mDownloadMngr.queryForFilesInDb(Constants.convertToAbsPaths(mContext, allRelFiles));
        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                int status = cursor.getInt(cursor.getColumnIndex(CLDownloadManager.COLUMN_STATUS));
                int control = cursor.getInt(cursor.getColumnIndex(Downloads.Impl.COLUMN_CONTROL));
                String data = cursor.getString(cursor.getColumnIndex(Downloads.Impl._DATA));
                long id = cursor.getLong(cursor.getColumnIndex(Downloads.Impl._ID));
                filesInDb.add(new FilesInDownloadingDb(status, control, data, id));
                if (DBG)
                    Log.i(TAG, "parseAndDownloadAll. id=" + id + ", status=" + status + ", control=" + control + ", data=" + data
                            + ", Thread=" + Thread.currentThread());
            }
        } finally {
            cursor.close();
        }

        // /new/xxx.jpg
        List<String> filesPaused = new ArrayList<String>();
        List<String> filesSuccess = new ArrayList<String>();
        List<String> filesNeedDown = new ArrayList<String>();
        for (String aRelativeFile : allRelFiles) {
            int status = FilesInDownloadingDb.fileDownloadStatus(filesInDb, aRelativeFile);
            if (status == Downloads.Impl.STATUS_SUCCESS) {
                filesSuccess.add(aRelativeFile);
            } else if (status == Downloads.Impl.STATUS_PAUSED_BY_APP) {
                filesPaused.add(aRelativeFile);
            } else {
                filesNeedDown.add(aRelativeFile);
            }
        }

        if (filesPaused.size() > 0) {
            List<Long> ids = new ArrayList<Long>();
            for (int i = 0; i < filesPaused.size(); i++) {
                ids.add(FilesInDownloadingDb.getId(filesInDb, filesPaused.get(i)));
            }

            if (DBG) {
                for (int i = 0; i < ids.size(); i++) {
                    Log.i(TAG, "parseAndDownloadAll. resume down id=" + ids.get(i) + ", Thread=" + Thread.currentThread());
                }
            }
            mDownloadMngr.resumeDownFiles(Longs.toArray(ids));
            downloadingFileIds.addAll(ids);
        }

        for (String fnd : filesNeedDown) {
            long fetchingFileId = startDownloadFile(fnd);
            if (DBG)
                Log.i(TAG, "parseAndDownloadAll. fetchingFileId=" + fetchingFileId + ", Thread=" + Thread.currentThread());
            downloadingFileIds.add(fetchingFileId);
        }

        if (DBG) {
            for (String fsucess : filesSuccess) {
                Log.i(TAG, "parseAndDownloadAll. file already in store =" + fsucess + ", Thread=" + Thread.currentThread());
            }
        }

        return new AllNDowningFiles(allRelFiles, downloadingFileIds);
    }

    private long startDownloadFile(String relativeFilepath) {
        File destPath = new File(CLStorageManager.getDownloadDataDirectory(mContext), relativeFilepath);
        Uri remoteUriNeedEscape = Uri.parse(Constants.getRemoteFileUri(relativeFilepath));
        Uri escapedUri = remoteUriNeedEscape.buildUpon().path(remoteUriNeedEscape.getPath()).build();
        if (DBG)
            Log.i(TAG, "fetchFile. remoteUriNeedEscape=" + remoteUriNeedEscape + ", escapedUri=" + escapedUri);

        long newDownloadId = mDownloadMngr
                .enqueue(new CLDownloadManager.Request(escapedUri)
                        .setReferer(mSize.toString())
                        .setDescription(getFileName())
                        .setDestinationUri(Uri.fromFile(destPath))
                        );
        if (DBG)
            Log.i(TAG, "fetchFile. newDownloadId=" + newDownloadId + " , destfilepath=" + Uri.fromFile(destPath) + ", remotefileuri="
                    + Constants.getRemoteFileUri(relativeFilepath));
        return newDownloadId;
    }

    private static class AllNDowningFiles {
        public AllNDowningFiles(Set<String> allFiles, Set<Long> downingFiles) {
            this.mAllFiles = allFiles;
            this.mDowningIds = downingFiles;
        }

        Set<String> mAllFiles;
        Set<Long> mDowningIds;
    }
}
