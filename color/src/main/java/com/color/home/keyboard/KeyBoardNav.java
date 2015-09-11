package com.color.home.keyboard;

import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

import com.color.home.MainActivity;

/**
 * Created by ZZJD7382 on 9/6/2015.
 */
public class KeyBoardNav {
    public static final String TAG = "KeyBoardNav";
    public static final boolean DBG = false;

    private final MainActivity mActivity;

    public KeyBoardNav(MainActivity mainActivity) {

        mActivity = mainActivity;
    }

    public void onKeyDown(int keyCode, KeyEvent event) {
        if (DBG)
            Log.d(TAG, "key=" + event.getDisplayLabel());

        Intent intent = new Intent("com.color.key.nav");
        intent.putExtra("code", keyCode);
        intent.putExtra("event", event);

        mActivity.sendBroadcast(intent);
    }
}
