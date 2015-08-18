package com.color.home.netplay;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

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
    private ServerConfig mInternetSer;

    public Connectivity(Context context) {
        this.mContext = context;

        initWifiP2P();
        mInternetSer = new ServerConfig(context);
    }

    public boolean hasServer() {
        if (DBG)
            Log.d(TAG, "hasServer. TextUtils.isEmpty(mInternetSer.getServerIpNPort()=" + TextUtils.isEmpty(mInternetSer.getServerIpNPort()));

        // 20140916. Even when the terminal is acting as an AP, do not refresh PC server. 8080/tcp tomcat.
        // ftp could be another one.
        return !TextUtils.isEmpty(mInternetSer.getServerIpNPort());
//        return mWifiP2P.isAPConfiged() || !TextUtils.isEmpty(mInternetSer.getServerIpNPort());
    }

    void initWifiP2P() {
        if (DBG)
            Log.i(TAG, "initWifiP2P. Thread=" + Thread.currentThread());

        mWifiP2P = new WifiP2P(mContext);

        if (DBG) {
            if (mWifiP2P.isAPConfiged()) {
                Log.i(TAG, "onCreate. isWIFIP2P.");
            } else {
                Log.i(TAG, "onCreate. not isWIFIP2P.");
            }
        }

    }

    private String getIpNPort() {
        // The server ip in the user's USB config is priorized.
        if (!TextUtils.isEmpty(mInternetSer.getServerIpNPort())) {
            if (DBG)
                Log.i(TAG, "getIpNPort. use internet ip N port=" + mInternetSer.getServerIpNPort());
            return mInternetSer.getServerIpNPort();
        } else if (mWifiP2P.getServerIp() != null) {
            if (DBG)
                Log.i(WifiP2P.TAG, "getIpNPort. use mWifiP2P.getServerIp= " + mWifiP2P.getServerIp());
            return mWifiP2P.getServerIp() + ":8080";
        }

        // return "192.168.1.103:8080";
        // return "192.168.1.125:8080";
        return "192.168.1.188:8080";
        // return "10.193.250.83:8080";
    }

    public String getProgramFolderUri() {
        return "http://" + getIpNPort() + "/program";
    }

    public String getColorControlUri() {
        return "http://" + getIpNPort() + "/color";
    }
}