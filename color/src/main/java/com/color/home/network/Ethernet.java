package com.color.home.network;

import java.util.Properties;

import android.content.ContentResolver;
import android.content.Context;
import android.net.ethernet.EthernetManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.netplay.Config;
import com.color.home.netplay.ConfigAPI;

import static com.color.home.AppController.LOG_TYPE_ETHERNET_CONFIGURED;

public class Ethernet {
    private final static String TAG = "Ethernet";
    private static final boolean DBG = false;
    private ContentResolver mContentResolver;
    private Context mContext;
    private boolean mDirty = false;
    private Properties mPp;

    public Ethernet(Context context, Properties pp) {
        mContext = context;
        mPp = pp;
        mContentResolver = context.getContentResolver();

        setup();
    }

    public void setIp() {
        final String ip = mPp.getProperty(ConfigAPI.ATTR_LAN_STATIC_IP);
        if (ip != null && !ip.equals(System.getString(mContentResolver, System.ETHERNET_STATIC_IP))) {
            if (DBG)
                Log.i(TAG, "setIp. ip=" + ip);
            System.putString(mContentResolver, System.ETHERNET_STATIC_IP, ip);
            dirty();
        }
    }

    public void setNetmask() {
        final String netmask = mPp.getProperty(ConfigAPI.ATTR_LAN_STATIC_NETMASK);

        if (netmask != null && !netmask.equals(System.getString(mContentResolver, System.ETHERNET_STATIC_NETMASK))) {
            if (DBG)
                Log.i(TAG, "setNetmask. netmask=" + netmask);

            System.putString(mContentResolver, System.ETHERNET_STATIC_NETMASK, netmask);
            dirty();
        }
    }

    public void setGw() {
        final String gw = mPp.getProperty(ConfigAPI.ATTR_LAN_STATIC_GATEWAY);
        if (gw != null && !gw.equals(System.getString(mContentResolver, System.ETHERNET_STATIC_GATEWAY))) {
            if (DBG)
                Log.i(TAG, "setGw. gw=" + gw);
            System.putString(mContentResolver, System.ETHERNET_STATIC_GATEWAY, gw);
            dirty();
        }
    }

    public void setDns1() {
        final String dns1 = mPp.getProperty(ConfigAPI.ATTR_LAN_STATIC_DNS1);
        if (dns1 != null && !dns1.equals(System.getString(mContentResolver, System.ETHERNET_STATIC_DNS1))) {
            if (DBG)
                Log.i(TAG, "setDns1. dns1=" + dns1);
            System.putString(mContentResolver, System.ETHERNET_STATIC_DNS1, dns1);
            dirty();
        }
    }

    public void setDns2() {
        final String dns2 = mPp.getProperty(ConfigAPI.ATTR_LAN_STATIC_DNS2);
        if (dns2 != null && ! dns2.equals(System.getString(mContentResolver, System.ETHERNET_STATIC_DNS2))) {
            if (DBG)
                Log.i(TAG, "setDns2. dns2=" + dns2);
            System.putString(mContentResolver, System.ETHERNET_STATIC_DNS2, dns2);
            dirty();
        }
    }

    public void setEnabled() {
        final String enabled = mPp.getProperty(ConfigAPI.ATTR_LAN_ENABLED);
        if (enabled != null ) {

            boolean ethOnInSettings = Settings.Secure.getInt(mContentResolver, System.ETHERNET_ON, 0) == 1;
            boolean bEnabled = Config.isTrue(enabled);

            if (bEnabled != ethOnInSettings) {

                if (DBG)
                    try {
                        Log.i(TAG, "setEnabled. value=" + enabled + ", enabled=" + Settings.Secure.getInt(mContentResolver, System.ETHERNET_ON));
                    } catch (SettingNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }


                Settings.Secure.putInt(mContentResolver, System.ETHERNET_ON, bEnabled ? 1 : 0);
                dirty();
            }
        }
    }

    public void setLanMode() {
        String mode = mPp.getProperty(ConfigAPI.ATTR_LAN_MODE);

        if (mode != null) {
            int staticFlagInSettings = System.getInt(mContentResolver, System.ETHERNET_USE_STATIC_IP, 0);
            int staticFlagToSet = "static".equalsIgnoreCase(mode) ? 1 : 0;

            if (staticFlagToSet != staticFlagInSettings) {
                setEthStatic(staticFlagToSet);
                dirty();
            }
        }
    }

    private void setup() {
        setEnabled();
        setIp();
        setNetmask();
        setGw();
        setDns1();
        setDns2();
        setLanMode();

        save();
    }

    private void setEthStatic(int staticlan) {
        if (DBG)
            Log.i(TAG, "setEthStatic. isstaticip=" + staticlan);
        System.putInt(mContentResolver, System.ETHERNET_USE_STATIC_IP, staticlan);
        dirty();
    }

    private void dirty() {
        mDirty = true;
    }

    private void resetDirty() {
        mDirty = false;
    }

    private void action() {
        EthernetManager ethManager = (EthernetManager) mContext.getSystemService(Context.ETHERNET_SERVICE);
        if (ethManager != null) {
            boolean isEthernetOn = Settings.Secure.getInt(mContentResolver, System.ETHERNET_ON, 0) == 1;

            // Must bring down firstly the if, then enable if so as to validate the new config.
            // setEthernetEnabled is going to change the Secure settings.
            ethManager.setEthernetEnabled(false);
            AppController.getInstance().reportInternetLog(LOG_TYPE_ETHERNET_CONFIGURED, "Ethernet configured.", 6, "", isEthernetOn + "");
            // And then enable if applicable.
            ethManager.setEthernetEnabled(isEthernetOn);
            Log.i(TAG, "Enable ethernet =" + isEthernetOn);
        } else {
            Log.e(TAG, "get ethernet manager failed.");
        }
    }

    private void save() {
        if (mDirty) {
//            updateDb();

            if (DBG)
                Log.d(TAG, "save. [dirty and action.");
            action();
            resetDirty();
        }
    }

//    private void updateDb() {
//        ContentValues values = new ContentValues(8);
//        values.put(ColorContract.COLUMN_ENABLED, mEnabled ? 1 : 0);
//        values.put(ColorContract.COLUMN_ISSTATIC, mStaticlan);
//        values.put(ColorContract.COLUMN_IP, Wifi.normalize(mIp));
//        values.put(ColorContract.COLUMN_MASK, Wifi.normalize(mNetmask));
//        values.put(ColorContract.COLUMN_GW, Wifi.normalize(mGw));
//        values.put(ColorContract.COLUMN_DNS1, Wifi.normalize(mDns1));
//        values.put(ColorContract.COLUMN_DNS2, Wifi.normalize(mDns2));
//        int update = AppController.getInstance().getContentResolver().update(ColorContract.NETWORK_LAN_CONTENT_URI, values, null, null);
//        if (DBG)
//            Log.d(TAG, "updateDb LAN. [update=" + update);
//    }
}
