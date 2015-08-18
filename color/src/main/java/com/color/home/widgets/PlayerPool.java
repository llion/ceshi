package com.color.home.widgets;

import java.util.HashMap;
import java.util.Map;

import android.media.MediaPlayer;
import android.util.Log;

public class PlayerPool {
    private final static String TAG = "PlayerPool";
    private static final boolean DBG = false;
    public static PlayerPool sPlayerPool;

    // private static MediaPlayer[] sPlayers = new MediaPlayer[4];
    private static Map<MediaPlayer, Integer> sPlayersMap = new HashMap<MediaPlayer, Integer>(4);

    private PlayerPool() {
        sPlayersMap.put(new MediaPlayer(), 0);
        sPlayersMap.put(new MediaPlayer(), 0);
        sPlayersMap.put(new MediaPlayer(), 0);
        sPlayersMap.put(new MediaPlayer(), 0);
    }

    public static PlayerPool getInst() {
        if (sPlayerPool == null) {
            sPlayerPool = new PlayerPool();
        }
        return sPlayerPool;
    }

    public MediaPlayer getFree() {
        for (MediaPlayer mp : sPlayersMap.keySet()) {
            if (!mp.isPlaying() && sPlayersMap.get(mp) == 0) {
                return mp;
            } else {
                if (DBG)
                    Log.i(TAG, "getFree. used mp=" + mp + ", Thread=" + Thread.currentThread());

            }
        }

        Log.e(TAG, "Error get free mp.");
        return null;
    }

    public void release(MediaPlayer mediaPlayer) {
        Integer status = sPlayersMap.get(mediaPlayer);
        sPlayersMap.put(mediaPlayer, 0);
        if (DBG)
            Log.i(TAG, "release. status=" + status + ", mediaPlayer=" + mediaPlayer + ", Thread=" + Thread.currentThread());

    }
}