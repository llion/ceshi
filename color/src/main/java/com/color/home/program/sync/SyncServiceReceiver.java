package com.color.home.program.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
//adb logcat  FullscreenActivity:V InfoReceiver:V InfoService:V ActivityManager:V MainActivity:V SyncServiceReceiver:V SyncService:V FFMpegMediaPlayer:S *:E
public class SyncServiceReceiver extends BroadcastReceiver {
    private final static String TAG = "SyncServiceReceiver";
    private static final boolean DBG = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DBG)
            Log.i(TAG, "onReceive. [intent action=" + intent.getAction());
        SyncService.startService(context, intent);
    }

}
