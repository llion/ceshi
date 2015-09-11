package com.color.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PlayPrgmReceiver extends BroadcastReceiver {
    public static final String EXTRA_CONTENT = "content";
    private static final boolean DBG = false;
    private final static String TAG = "PlayPrgmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DBG)
            Log.i(TAG, "onReceive. intent data=" + intent.getData() + ", getAction=" + intent.getAction());

        if (Constants.ACTION_PLAY_PROGRAM.equals(intent.getAction())) {
            if (DBG)
                Log.i(TAG, "onReceive. com.clt.broadcast.playProgram , Thread=" + Thread.currentThread());
            Intent i = new Intent(context, MainActivity.class);
            i.setAction(intent.getAction());
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtras(intent);
            context.startActivity(i);
        }
    }
}
