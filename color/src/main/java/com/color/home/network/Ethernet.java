package com.color.home.network;

import java.util.Properties;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.ethernet.EthernetManager;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.netplay.Config;
import com.color.home.netplay.ConfigAPI;
import com.color.home.netplay.Wifi;

public class Ethernet {
    private final static String TAG = "Ethernet";
    private static final boolean DBG = false;
    private ContentResolver mContentResolver;
    private Context mContext;
    private boolean mDirty = false;
    private Properties mPp;
    private boolean mEnabled;
    private int mStaticlan;
    private String mIp;
    private String mNetmask;
    private String mGw;
    private String mDns1;
    private String mDns2;

    public Ethernet(Context context, Properties pp) {
        mContext = context;
        mPp = pp;
        mContentResolver = context.getContentResolver();

        setup();
    }

    public void setIp() {
        mIp = mPp.getProperty(ConfigAPI.ATTR_LAN_STATIC_IP);
        if (mIp != null) {
            if (DBG)
                Log.i(TAG, "setIp. ip=" + mIp);
            System.putString(mContentResolver, System.ETHERNET_STATIC_IP, mIp);
            dirty();
        }
    }

    public void setNetmask() {
        mNetmask = mPp.getProperty(ConfigAPI.ATTR_LAN_STATIC_NETMASK);
        if (mNetmask != null) {
            if (DBG)
                Log.i(TAG, "setNetmask. netmask=" + mNetmask);
            System.putString(mContentResolver, System.ETHERNET_STATIC_NETMASK, mNetmask);
            dirty();
        }
    }

    public void setGw() {
        mGw = mPp.getProperty(ConfigAPI.ATTR_LAN_STATIC_GATEWAY);
        if (mGw != null) {
            if (DBG)
                Log.i(TAG, "setGw. gw=" + mGw);
            System.putString(mContentResolver, System.ETHERNET_STATIC_GATEWAY, mGw);
            dirty();
        }
    }

    public void setDns1() {
        mDns1 = mPp.getProperty(ConfigAPI.ATTR_LAN_STATIC_DNS1);
        if (mDns1 != null) {
            if (DBG)
                Log.i(TAG, "setDns1. dns1=" + mDns1);
            System.putString(mContentResolver, System.ETHERNET_STATIC_DNS1, mDns1);
            dirty();
        }
    }

    public void setDns2() {
        mDns2 = mPp.getProperty(ConfigAPI.ATTR_LAN_STATIC_DNS2);
        if (mDns2 != null) {
            if (DBG)
                Log.i(TAG, "setDns2. dns2=" + mDns2);
            System.putString(mContentResolver, System.ETHERNET_STATIC_DNS2, mDns2);
            dirty();
        }
    }

    public void setEnabled() {
        String property = mPp.getProperty(ConfigAPI.ATTR_LAN_ENABLED);
        if (property != null) {
            if (DBG)
                try {
                    Log.i(TAG, "setEnabled. value=" + property + ", enabled=" + System.getInt(mContentResolver, System.ETHERNET_ON));
                } catch (SettingNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            mEnabled = Config.isTrue(property);
            System.putInt(mContentResolver, System.ETHERNET_ON, mEnabled ? 1 : 0);
            dirty();
        }
    }

    public void setLanMode() {
        String mode = mPp.getProperty(ConfigAPI.ATTR_LAN_MODE);
        if (mode != null) {
            setEthStatic("static".equalsIgnoreCase(mode) ? 1 : 0);
            dirty();
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
        mStaticlan = staticlan;
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
            ethManager.setEthernetEnabled(false);
            boolean isEthernetOn = false;
            try {
                isEthernetOn = System.getInt(mContentResolver, System.ETHERNET_ON) == 1;
                ethManager.setEthernetEnabled(isEthernetOn);
            } catch (SettingNotFoundException e) {
                e.printStackTrace();
            }

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
