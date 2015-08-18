package com.color.home.program.sync;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.Constants;
import com.color.home.PlayPrgmReceiver;
import com.google.common.io.Files;

public class SyncedPrograms {
    private final static String TAG = "SyncedPrograms";
    private static final boolean DBG = false;
    private List<String> mNamesInProgramJson = new ArrayList<String>();
    private List<File> mFilesInStore = new ArrayList<File>();

    public SyncedPrograms() {
        String jsonStr;
        try {
            jsonStr = Files.toString(new File(Constants.getAbsPath(Constants.PROGRAMS_JSON)), Charset.forName("UTF-8"));

            final JSONArray jsonArray = new JSONObject(jsonStr).getJSONArray("vsns");
            if (DBG)
                Log.d(TAG, "From store. [jsonArray=" + jsonArray.toString(4));

            final int N = jsonArray.length();
            for (int i = 0; i < N; i++) {
                final String name = jsonArray.getJSONObject(i).getString("name");
                if (DBG)
                    Log.d(TAG, "SyncedPrograms. [In JSON, vsn name=" + name);
                mNamesInProgramJson.add(name);
            }
        } catch (IOException e) {
            if (DBG)
                Log.e(TAG, "SyncedPrograms constructor.", e);
        } catch (JSONException e) {
            Log.e(TAG, "SyncedPrograms constructor.", e);
        }

        File[] files = Constants.listVsns(AppController.getInstance().getDownloadDataDir());
        // File[] files = AppController.getInstance().getDownloadDataDir().listFiles();
        if (files == null) {
            if (DBG)
                Log.d(TAG, "SyncedPrograms. [No vsn file in the net folder.");
            return;
        }

        mFilesInStore = new ArrayList<File>(Arrays.asList(files));
        if (DBG) {
            for (File file : mFilesInStore) {
                Log.d(TAG, "SyncedPrograms. [not iterator file=" + file);
            }
        }
    }

    public void pruneDeprecatedVsnsButPlaying() {
        // Wait a moment.
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Collect .vsn files to be removed.
        final String vsn = Strategy.getPlayingVsn();
        delDLResFilesNotInJsonAndPlaying(vsn);
        delSavedVsnNotInJsonAndPlaying(vsn);
    }

    public boolean play() {
        boolean result = false;
        if (mNamesInProgramJson.isEmpty()) {
            new PairedProgramFile("", "").play();
        } else if (!mFilesInStore.isEmpty()) {
            // Find the first vsn in the json, and also in store. TODO and also all res downloaded.
            final Iterator<File> iterator = mFilesInStore.iterator();
            while (iterator.hasNext() && !result) { // XXX: REMEMBER to break on first found. 20140805.
                File file = iterator.next();
                // Play the first one.
                if (mNamesInProgramJson.contains(file.getName())) {
                    if (DBG)
                        Log.d(TAG, "play. [file=" + file);
                    new FileProgramFile(file).play();
                    // Found one. Do not continue.
                    result = true;
                }
            }

            if (DBG)
                if (!result)
                    Log.d(TAG, "play. [files in store are not in json. Play nothing.");
        } else {
            if (DBG)
                Log.d(TAG, "play. [empty program json or empty files in store.");
        }

        return result;
    }

    private void delSavedVsnNotInJsonAndPlaying(String vsnPlaying) {
        // Del vsn files in store except those in program json and playing.
        final ArrayList<File> vsnFileToDel = collectVsnFilesToDel(vsnPlaying);
        if (vsnFileToDel.size() > 0) {
            // Delete files.
            for (File file : vsnFileToDel) {
                if (!file.delete()) {
                    if (DBG)
                        Log.e(TAG, "delSavedVsnNotInJsonAndPlaying, failed to delete:" + file);
                } else {
                    Log.d(TAG, "delSavedVsnNotInJsonAndPlaying. [Deleted file=" + file);
                }
            }
            // Files in store that are still valid after pruning.
            mFilesInStore.removeAll(vsnFileToDel);
        }
    }

    private void delDLResFilesNotInJsonAndPlaying(String vsnPlaying) {
        // Mark del the db rows neither in program json nor playing.
        final ArrayList<String> keepVsns = new ArrayList<String>(mNamesInProgramJson);
        if (!TextUtils.isEmpty(vsnPlaying)) {
            keepVsns.add(vsnPlaying);
        }

        if (DBG)
            for (String vsn : keepVsns)
                Log.d(TAG, "delDLResourceFilesNotInJsonAndPlaying. [Keep vsn=" + vsn);

        if (!keepVsns.isEmpty())
            AppController.getInstance().getDownloadMngr().markRowDeletedNotInVsns(keepVsns.toArray(new String[0]));
    }

    private ArrayList<File> collectVsnFilesToDel(String vsnPlaying) {
        // To be pruned vsn files is a subset of all the files in store.
        // In maximum, its size is that of the mFilesInStore.
        final ArrayList<File> vsnFileToDel = new ArrayList<File>(mFilesInStore.size());
        for (File vsnInStore : mFilesInStore) {
            final String vsnName = vsnInStore.getName();
            // Nom atter in json or not, current playing vsn, is not to be pruned.
            if (vsnName.equals(vsnPlaying)) {
                if (DBG)
                    Log.d(TAG, "collectVsnFilesToDel. [Ingore playing vsn=" + vsnPlaying);
                continue;
            }

            // Collect .vsn file in store.
            if (!mNamesInProgramJson.contains(vsnName)) {
                if (DBG)
                    Log.d(TAG, "collectVsnFilesToDel. [To be pruned file= " + vsnInStore);
                vsnFileToDel.add(vsnInStore);
            }
        }
        return vsnFileToDel;
    }
}
