package com.color.home.program.sync;

import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.Constants;

import java.io.File;

public class Strategy {
    private final static String TAG = "Strategy";
    private static final boolean DBG = false;

    public void onUsbMounted() {
        if (DBG)
            Log.d(TAG, "onUsbMounted. [");

        FailedProgramChecker fp = new FailedProgramChecker(AppController.getInstance().getApplicationContext());
        fp.clear();
        // Try to play usb content. If no vsn, do nothing.
        // i.e., keep current playing.
        playVsnDir(Constants.FOLDER_USB_0);
    }

    public void onUsbRemoved() {
        if (DBG)
            Log.d(TAG, "onUsbRemoved. [");

        switch (Constants.absPathToSourceTypeID(getPlayingFolder())) {
        case Constants.TYPE_USB:
            // If it was playing usb content, try to switch to synced usb.
            // And consequently FTP not synced usb not OK.
            playInternals();
            break;
        default:
            break;
        }
    }

    // /**
    // * Means now we can download/play Net and Sync Usb. ???
    // */
    // public void onMntSdcardMounted() {
    // if (DBG)
    // Log.d(TAG, "onMntSdcardMounted. [Normally this won't happen.");
    // if (getPlayingType() != Constants.TYPE_USB) {
    // if (!playSyncedUsb()) {
    // playNet();
    // }
    // }
    // // If it's playing from USB, ignore.
    // }

    public void onHomeStarted() {
        if (DBG)
            Log.d(TAG, "onHomeStarted. [");
        FailedProgramChecker failedProgram = new FailedProgramChecker(AppController.getInstance().getApplicationContext());
        failedProgram.check();

        ProgramFile pf = PairedProgramFile.fromSettings(AppController.getInstance().getSettings());
        if (pf.exist() && failedProgram.okToPlay(pf.file())) {
            if (DBG)
                Log.d(TAG, "onHomeStarted. [From shared preference, play file=" + pf.file());
            pf.play();
            return;
        }

        if (!playVsnDir(Constants.FOLDER_USB_0))
            playInternals();
    }

    public void playInternals() {
        if (!playSyncedUsb())
            if (!playFtp())
                if (!playNet()) {
                    Log.w(TAG, "playInternals. [No content in the device.");
                    stopVsn();
                }
    }

    public void onAllDownloaded() {
        if (DBG)
            Log.d(TAG, "onAllDownloaded. [");

        playNet();
    }

    public void onForcePlayNet() {
        if (DBG)
            Log.d(TAG, "onForcePlayNet. [");
        playNet();
    }

    public void onUsbSynced() {
        if (DBG)
            Log.d(TAG, "onUsbSynced. [");

        playSyncedUsb();
        // Do not play syned USB (Flash internal synced USB) content,
        // even when sync finished.
//         if (!playSyncedUsb()) {
//             if (DBG)
//                 Log.d(TAG, "onHandleIntent. [no synced usb, play net.");
//             playNet();
//         }
    }

    public static String getPlayingVsn() {
        String vsnPlaying = null;
        Intent intent = AppController.getInstance().registerReceiver(null, new IntentFilter(Constants.ACTION_CURRENT_PROG_INDEX));
        if (intent != null) {
            vsnPlaying = intent.getStringExtra(Constants.EXTRA_FILE_NAME);
            if (DBG)
                Log.d(TAG, "getPlayingVsn. [Playing vsn=" + vsnPlaying);

        }
        return vsnPlaying;
    }

    public static String getPlayingFolder() {
        String folder = null;
        Intent intent = AppController.getInstance().registerReceiver(null, new IntentFilter(Constants.ACTION_CURRENT_PROG_INDEX));
        if (intent != null) {
            folder = intent.getStringExtra(Constants.EXTRA_PATH);
            if (DBG)
                Log.d(TAG, "getPlayingFolder. [Playing vsn folder =" + folder);
        }
        return folder;
    }

    private static boolean playNet() {
        if (DBG)
            Log.d(TAG, "Try playNet. [");

//        return new SyncedPrograms().play();
        return playVsnDir(Constants.FOLDER_NET);
    }

    private static boolean playSyncedUsb() {
        if (DBG)
            Log.d(TAG, "Try playSyncedUsb. [");

        return playVsnDir(Constants.FOLDER_SYNCED_USB);
    }

    private static boolean playVsnDir(String dir) {
        if (DBG)
            Log.d(TAG, "Try playVsnDir. [");

        File[] vsns = Constants.listVsns(dir);
        if (vsns != null && vsns.length > 0) {
            // Always sort Usb program.
            FailedProgramChecker fp = new FailedProgramChecker(AppController.getInstance().getApplicationContext());
            for (int i = 0; i < vsns.length; i++) {
                if (fp.okToPlay(vsns[i])) {
                    new FileProgramFile(vsns[i]).play();
                    return true;
                }
            }
        }

        return false;
    }

    private void stopVsn(){
        Intent intent = new Intent(Constants.ACTION_PLAY_PROGRAM);
        intent.putExtra(Constants.EXTRA_PATH, "");
        intent.putExtra(Constants.EXTRA_FILE_NAME, "");
        AppController.getInstance().sendBroadcast(intent);
    }

    private static boolean playFtp() {
        if (DBG)
            Log.d(TAG, "Try playFtp. [");
        return playVsnDir(Constants.FTP_PROGRAM_PATH);
    }

}
