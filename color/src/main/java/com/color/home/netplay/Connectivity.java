package com.color.home.netplay;

import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;

import com.color.home.netplay.receiver.UsbAttachedReceiver;

/**
 * @author zzjd7382
 *
 *         Connectivity:V
 */
public class Connectivity {
    private final static String TAG = "Connectivity";
    private static final boolean DBG = false;
    private Context mContext;
    private WifiP2P mWifiP2P;
//    private ServerConfig mInternetSer;

    public Connectivity(Context context) {
        this.mContext = context;

        // WIFI AP stuff.
        mWifiP2P = new WifiP2P(mContext);
        registerUSBWifiDongleReceiver();
    }

    void registerUSBWifiDongleReceiver() {

        if (DBG)
            Log.i(TAG, "registerUSBWifiDongleReceiver. Thread=" + Thread.currentThread());

        String deviceName = Config.getDeviceModelName();
        if (DBG)
            Log.i(TAG, "registerUSBWifiDongleReceiver. deviceName=" + deviceName);
        if ("c3".equals(deviceName)) {
            IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_DEVICE_ATTACHED");
//        filter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
            mContext.registerReceiver(new UsbAttachedReceiver(mWifiP2P), filter);
        }
    }

}