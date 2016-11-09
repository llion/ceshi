package com.color.home.netplay.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.color.home.netplay.Config;
import com.color.home.netplay.Wifi;
import com.color.home.netplay.WifiP2P;

/**
 * Created by Administrator on 2016/11/10.
 */
public class UsbAttachedReceiver extends BroadcastReceiver {
    public static final boolean DBG = false;
    public final static String TAG = "UsbAttachedReceiver";

    private WifiP2P mWifiP2P;

    public UsbAttachedReceiver(WifiP2P wifiP2P) {
        this.mWifiP2P = wifiP2P;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (DBG)
            Log.d(TAG, "action= " + action);

        if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action)) {
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (DBG)
                Log.d(TAG, "usbDevice= " + usbDevice);

            if (usbDevice != null && usbDevice.getProductId() == 33145 && usbDevice.getVendorId() == 3034) {
                if (DBG)
                    Log.d(TAG, "mWifiP2P=null? " + (mWifiP2P == null));
                if (mWifiP2P != null && mWifiP2P.isAPConfiged())
                    mWifiP2P.enable();
            }

        }
//        else {
//            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//            if (DBG)
//                Log.d(TAG, "usbDevice= " + usbDevice);
//
//            if (usbDevice != null && usbDevice.getProductId() == 33145 && usbDevice.getVendorId() == 3034) {
//                if (mWifiP2P != null)
//                    mWifiP2P.disable();
//            }
//        }
    }
}
