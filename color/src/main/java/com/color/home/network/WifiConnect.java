package com.color.home.network;

import java.util.List;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.netplay.Config;
import com.color.home.network.wifi.WifiConfigManager;
import com.color.home.network.wifi.WifiParsedResult;

/**
 * used for: 1 open WIFI switcher. 2 connect to an assigned WIFI network
 * 
 * WifiConnect:V
 */
public class WifiConnect {
    private static final boolean DBG = true;
    private static final String TAG = "WifiConnect";

    private WifiManager mWifiManager;
    private String mHotSpotSsid;
    private String mHotPassword;
    private String mHotSpotType;
    private String mHidden;

    public WifiConnect() {
        mWifiManager = (WifiManager) AppController.getInstance().getSystemService(Context.WIFI_SERVICE);
    }

    public void connectTo(String assignedSsid, String assignedPassword,
            String assigendType, String hidden) {
        mHotSpotSsid = assignedSsid;
        mHotPassword = assignedPassword;
        mHotSpotType = assigendType;
        mHidden = hidden;

        // Force disable, no matter current WIFI status.
        // If current wifi is ON, in fact the AP is already disabled.
        boolean result = mWifiManager.setWifiApEnabled(null, false);
        if (DBG)
            Log.d(TAG, "connectTo. [setWifiApEnabled false, result=" + result);
//        setWifi(true);
//        addNetwork(createWifiInfo(mHotSpotSsid, mHotPassword, mHotSpotType));
        
        WifiParsedResult wifiResult = new WifiParsedResult(TextUtils.isEmpty(mHotSpotType) ? "WPA" : mHotSpotType, mHotSpotSsid, mHotPassword, Config.isTrue(mHidden));
        if (DBG)
            Log.d(TAG, "connectTo. [wifiResult=" + wifiResult);
        
        new WifiConfigManager(mWifiManager).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, wifiResult);
    }

    public void setWifi(boolean enable) {
        if(DBG)
            Log.d(TAG, "set wifi =" + enable);

        if (mWifiManager != null) {
//            if (mWifiManager.isWifiEnabled() != enable) {
            boolean enabled = mWifiManager.setWifiEnabled(enable);
            if (DBG)
                Log.d(TAG, "openWifi. [setWifiEnabled is enable=" + enable + ", result=" + enabled);
//            }else{
//                if(DBG)
//                    Log.d(TAG, "wifi is already enabled..");
//            }

        }else{
            Log.e(TAG, "wifi manager is null!");
        }
    }

    /*
     * add a hotspot and try to connect it. the state of open WIFI,from enable to enabling needs time. so when WIFI does not open
     * completely,let current thread wait.
     */
    private void addNetwork(WifiConfiguration newHotSpot) {
        if (DBG)
            Log.d(TAG, "addNetwork. [mWifiManager.getWifiState()=" + mWifiManager.getWifiState());
        
        int retries = 30;
        while (mWifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED && --retries > 0) {
            try {
                Thread.sleep(400);
                if (DBG)
                    Log.d(TAG, "addNetwork. [retries=" + new Integer(50 - retries)
                            + ", mWifiManager.getWifiState()=" + mWifiManager.getWifiState());

            } catch (InterruptedException ex) {
                Log.e(TAG, "addNetwork,but InterruptedException.");
            }
        }

        int netID = mWifiManager.addNetwork(newHotSpot);
        if (mWifiManager.enableNetwork(netID, true)) {
            Log.d(TAG, "addNetwork,enableNetwork success..");
        } else {
            Log.d(TAG, "addNetwork,enableNetwork failed..");
        }
        
        boolean result = mWifiManager.saveConfiguration();
        Log.d(TAG, "Save wifi config result=" + result);

    }

    private WifiConfiguration createWifiInfo(String SSID, String passWord,
            int type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        // remove the WIFI hotspot if we have stored it.
        WifiConfiguration tempConfig = this.IsExsits(SSID);
        if (tempConfig != null) {
            boolean removed = mWifiManager.removeNetwork(tempConfig.networkId);
            if (DBG)
                Log.d(TAG, "createWifiInfo. [removed=" + removed);
        }

        if (type == 3) // WIFICIPHER_WPA,just tested can used.
        {
            // if (!TextUtils.isEmpty(passWord)) {
            config.preSharedKey = "\"" + passWord + "\"";
            // }
            config.hiddenSSID = false;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);

            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            // config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA2_PSK);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

            // config.allowedPairwiseCiphers
            // .set(WifiConfiguration.PairwiseCipher.NONE);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);

            config.status = WifiConfiguration.Status.ENABLED;
            if (DBG)
                Log.d(TAG, "createWifiInfo. [Configed. ssid=" + SSID + ", pass=" + passWord);
        }
        return config;
    }

    private WifiConfiguration IsExsits(String SSID) {

        List<WifiConfiguration> existingConfigs = mWifiManager
                .getConfiguredNetworks();
        if (existingConfigs != null) {
            for (WifiConfiguration existingConfig : existingConfigs) {
                if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                    return existingConfig;
                }
            }
        }
        return null;
    }

}
