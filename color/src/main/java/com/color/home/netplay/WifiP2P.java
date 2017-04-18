package com.color.home.netplay;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.util.HexDump;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.Properties;

import static com.color.home.netplay.Config.isIsoFromTxtFile;
import static com.color.home.netplay.ConfigAPI.ATTR_IS_WIFI_P2P;

/**
 * @author zzjd7382 WifiP2P:V
 */
public class WifiP2P {
    final static String TAG = "WifiP2P";
    static final boolean DBG = false;
    private static final int MESSAGE_RETRY = 1;
    private final int DELAY = 5000;
    private Context mContext;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig = null;


    public void setupWifiAP() {
        if (DBG)
            Log.i(TAG,
                    "setupWifiAP. isAPConfigured()=" + isAPConfigured() + ", Device, mWifiManager.isWifiApEnabled()="
                            + mWifiManager.isWifiApEnabled());

        if (Config.isWifiModuleExists(mContext))
            if (isAPConfigured()) {
    //            if (!mWifiManager.isWifiApEnabled())
                // reenable is OK...
                enable();
    //            else {
    //                if (DBG)
    //                    Log.d(TAG, "setupWifiAP. [ap configed and currently device is in AP, ignore.");
    //            }
            } else {
                disable();
            }
    }

    public WifiP2P(Properties pp, Context context) throws UnsupportedEncodingException{
        mContext = context;

        if(mWifiManager == null)
            mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        String propIsAP = pp.getProperty(ATTR_IS_WIFI_P2P);
        if (propIsAP == null) {
            if (DBG)
                Log.d(TAG, "saveWifiP2PFromExt. [is.wifi.p2p not set, so do not save to pref. Abort.");
            return;
        }

        if (DBG)
            Log.i(TAG, "saveWifiP2PFromExt. strIsWifiP2PFromConfig=" + pp + ", Thread=" + Thread.currentThread());

        {
            final boolean enableAPRequested = Config.isTrue(propIsAP.trim());

//        Settings.Global.putInt(mContext.getContentResolver(), ConfigAPI.KEY_AP_ENABLED, apEnabledInUsb ? 1 : 0);

            String ssid = pp.getProperty(ConfigAPI.ATTR_AP_SSID);
            String pass = pp.getProperty(ConfigAPI.ATTR_AP_PASS);
            String channel = pp.getProperty(ConfigAPI.ATTR_AP_CHANNEL, "6");


            if (!TextUtils.isEmpty(ssid)) {
                ssid = ssid.trim();
            } else {
                return;
            }

            if (!TextUtils.isEmpty(pass)) {
                pass = pass.trim();
            } else {
                return;
            }

            if (!TextUtils.isEmpty(channel)) {
                channel = channel.trim();
            } else {
                return;
            }

            // 1, Check change
            if(! isApConfigChanged(enableAPRequested, ssid, pass, channel)){
                Log.d(TAG, "Ap config didn't change . Do not save config or setup ap");
                return ;
            }

            // 2, Save to SettingsProvider.

            if (DBG)
                Log.i(TAG, "Post apEnabledInUsb=" + enableAPRequested + ", Thread=" + Thread.currentThread() +
                        "ssid=" + ssid +
                        "pass=" + pass +
                        "channel=" + channel);

        }
//
//        if (Config.isWifiModuleExists(mContext)
//            if (enableAPRequested && ) {
//                if(!mWifiManager.isWifiApEnabled()) {
//                    enable(ssid, pass, channel);
//                    Settings.Global.putInt(mContext.getContentResolver(), ConfigAPI.ATTR_AP_CHANNEL, Integer.parseInt(channel));
//                }
//            } else {
//                disable();
//            }

        setupWifiAP();
    }

    private boolean isApConfigChanged(boolean isApConfig, String ssid, String pass, String channel ){
        if(DBG)
            Log.d(TAG, "isApConfigChanged..");
        boolean dirty = false;
        if(isApConfig != isAPConfigured()){
            saveApEnableConfig(isApConfig);
            dirty = true;
        }
        if(! ssid.equals(getSSIDFromProvider())){
            saveSSIDToProvider(ssid);
            dirty = true;
        }
        if(! pass.equals(getPassFromProvider())){
            savePassToProvider(pass);
            dirty = true;
        }
        if(! channel.equals(getChannelFromProvider())){
            saveChannelToProvider(channel);
            dirty = true;
        }
        return dirty;
    }

    private class WifiApConfig{
        private String ssid;
        private String pass;
        private String channel;

        public WifiApConfig(String ssid, String pass, String channel) {
            this.ssid = ssid;
            this.pass = pass;
            this.channel = channel;
        }
    }

    public WifiP2P(Context context) {
        mContext = context;

        // The enable AP logic is only available on a box.
        // if (Constants.TEST_MOBILE_DEVICE_ID.equals(Build.ID)) {
        // if (DBG)
        // Log.i(TAG, "Don't registerUSBWifiDongleReceiver. Not a box, Build.ID=" + Build.ID + ", isWIFIP2P()" + isWIFIP2P());
        // return;
        // }

        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);


//        updateAPInfo();

        setupWifiAP();
    }

    public static final class WifiP2PConfigManager extends AsyncTask<String,Object,Object> {
        private static final String TAG = WifiP2PConfigManager.class.getSimpleName();

        private final WifiManager wifiManager;

        public WifiP2PConfigManager(WifiManager wifiManager) {
            this.wifiManager = wifiManager;
        }

        @Override
        protected Object doInBackground(String... args) {
            if (DBG)
                Log.d(TAG, "Always wait in another thread before enable. wait 1.5 secs and enable. [SSID=" + args[0] + ", pass=" + args[1]);

            try {
                Thread.sleep(1500);
            } catch (Exception e) {
                // TODO: handle exception
            }

            // open WIFI AP
            try {
                WifiConfiguration netConfig = new WifiConfiguration();
                netConfig.SSID = args[0];
                netConfig.preSharedKey = args[1];
                netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);

                netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                // get hidden variable
                Field wpa2_psk = WifiConfiguration.KeyMgmt.class.getField("WPA2_PSK");
                wpa2_psk.setAccessible(true);
                netConfig.allowedKeyManagement.set(wpa2_psk.getInt(wpa2_psk.getName()));

                netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                netConfig.wepTxKeyIndex = Integer.parseInt(args[2]);

                boolean result = wifiManager.setWifiApEnabled(null, false);
                if (DBG)
                    Log.i(TAG, "disable. result=" + result);

                result = wifiManager.setWifiApEnabled(netConfig, true);
                if (DBG)
                    Log.i(TAG, "enable. result=" + result);

            } catch (IllegalAccessException e) {
                Log.e(TAG, "get WifiConfiguration.KeyMgmt WPA2_PSK error.", e);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "get WifiConfiguration.KeyMgmt WPA2_PSK error.", e);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "get WifiConfiguration.KeyMgmt WPA2_PSK error.", e);
            }

            return null; // don't care.
        }
    }

//    public void enable(String ssid, String pass, String channel) {
//        if (DBG)
//            Log.d(TAG, "Async me");
//
//        new WifiP2PConfigManager(mWifiManager).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ssid, pass, channel);
//
//        // assertTrue(mWifiManager.setWifiApEnabled(null, true));
//        // mWifiConfig = mWifiManager.getWifiApConfiguration();
//        // if (mWifiConfig != null) {
//        // Log.v(TAG, "mWifiConfig is " + mWifiConfig.toString());
//        // } else {
//        // Log.v(TAG, "mWifiConfig is null.");
//        // }
//    }

    public void enable() {
        if (DBG)
            Log.d(TAG, "Async me");

        new WifiP2PConfigManager(mWifiManager).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getSSIDFromProvider(), getPassFromProvider(), getChannelFromProvider());

        // assertTrue(mWifiManager.setWifiApEnabled(null, true));
        // mWifiConfig = mWifiManager.getWifiApConfiguration();
        // if (mWifiConfig != null) {
        // Log.v(TAG, "mWifiConfig is " + mWifiConfig.toString());
        // } else {
        // Log.v(TAG, "mWifiConfig is null.");
        // }
    }

    private void disable() {
        if (DBG)
            Log.v(TAG, "disable turn off wifi tethering");
        mWifiManager.setWifiApEnabled(null, false);

        if (DBG)
            Log.i(TAG, "disable. ");
        // disable is safe to be OK, while the enable not, as the system could
        // bring it down when it find the System settings is not turned on.
        // (DISABLE the ap.)

        // Reset the peer ip.
    }

    // // Test case 1: Test the soft AP SSID with letters
    // private void testApSsidWithAlphabet() {
    // WifiConfiguration config = new WifiConfiguration();
    // config.SSID = "abcdefghijklmnopqrstuvwxyz";
    // config.allowedKeyManagement.set(KeyMgmt.NONE);
    // mWifiConfig = config;
    // assertTrue(mWifiManager.setWifiApEnabled(mWifiConfig, true));
    // try {
    // Thread.sleep(DELAY);
    // } catch (InterruptedException e) {
    // Log.v(TAG, "exception " + e.getStackTrace());
    // }
    // assertNotNull(mWifiManager.getWifiApConfiguration());
    // assertEquals("wifi AP state is not enabled",
    // WifiManager.WIFI_AP_STATE_ENABLED, mWifiManager.getWifiApState());
    // }
    //
    // private void assertNotNull(WifiConfiguration wifiApConfiguration) {
    // // TODO Auto-generated method stub
    // if (DBG)
    // Log.i(TAG, "assertNotNull. wifiApConfiguration=" + wifiApConfiguration);
    //
    // }
    //
    // private void assertEquals(String string, int wifiApStateEnabled, int
    // wifiApState) {
    // // TODO Auto-generated method stub
    // if (DBG)
    // Log.i(TAG, "assertEquals. string=" + string + ", wifiApStateEnabled=" +
    // wifiApStateEnabled + ", wifiApState=" + wifiApState);
    //
    // if (DBG)
    // Log.i(TAG, "assertEquals. string=" + string + ", wifiApStateEnabled=" +
    // wifiApStateEnabled + ", wifiApState" + wifiApState);
    // }
    //
    // private void assertTrue(boolean setWifiApEnabled) {
    // if (DBG)
    // Log.i(TAG, "assertTrue. setWifiApEnabled=" + setWifiApEnabled);
    // }


    protected void updateTetherState(Object[] available, Object[] tethered, Object[] errored) {
        // TODO Auto-generated method stub
        if (DBG) {
            Log.i(TAG, "updateTetherState. available, tethered, errored");

            for (Object o : tethered) {
                String s = (String) o;
                if (DBG)
                    Log.i(TAG, "updateTetherState.  tethered = " + s);

            }
            for (Object o : errored) {
                String s = (String) o;
                if (DBG)
                    Log.i(TAG, "updateTetherState.  errored = " + s);
            }
        }

    }


    public void onDetroy() {
        if (DBG)
            Log.i(TAG, "onDetroy. ");

//        mSp.unregisterOnSharedPreferenceChangeListener(this);
    }


    public boolean isAPConfigured() {
        return Settings.Global.getInt(mContext.getContentResolver(), ConfigAPI.KEY_AP_ENABLED, 0) == 1;
    }

    public String getChannelFromProvider() {
        return Settings.Global.getString(mContext.getContentResolver(), ConfigAPI.ATTR_AP_CHANNEL);
    }

    public String getPassFromProvider() {
        return Settings.Global.getString(mContext.getContentResolver(), ConfigAPI.ATTR_AP_PASS);
    }

    public String getSSIDFromProvider() {
        return Settings.Global.getString(mContext.getContentResolver(), ConfigAPI.ATTR_AP_SSID);
    }

    public void saveApEnableConfig(boolean isApConfig) {
        Settings.Global.putInt(mContext.getContentResolver(), ConfigAPI.KEY_AP_ENABLED, isApConfig ? 1 : 0);
    }

    public void saveChannelToProvider(String channel) {
        Settings.Global.putString(mContext.getContentResolver(), ConfigAPI.ATTR_AP_CHANNEL, channel);
    }

    public void savePassToProvider(String pass) {
        Settings.Global.putString(mContext.getContentResolver(), ConfigAPI.ATTR_AP_PASS, pass);
    }

    public void saveSSIDToProvider(String ssid) {
        Settings.Global.putString(mContext.getContentResolver(), ConfigAPI.ATTR_AP_SSID, ssid);
    }

}
