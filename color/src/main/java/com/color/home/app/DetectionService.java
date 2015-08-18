package com.color.home.app;

import com.color.home.messages.MyMessenger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * @author zzjd7382
 * adb shell am startservice com.color.home/com.color.home.app.DetectionService
 *
 */
public class DetectionService extends Service {

    private static final int PORT_UDP_DETECTION = 40961;
    private MyMessenger mMm;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mMm = new MyMessenger(getApplicationContext(), PORT_UDP_DETECTION);
        mMm.startMessageReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        mMm.stopMessageReceiver();
    }
}
