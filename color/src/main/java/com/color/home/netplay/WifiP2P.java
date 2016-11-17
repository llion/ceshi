package com.color.home.netplay;

import android.content.ContentValues;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;

import com.color.home.AppController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;

import libcore.io.IoUtils;

/**
 * @author zzjd7382 WifiP2P:V
 */
public class WifiP2P implements OnSharedPreferenceChangeListener, ServerIpProvider {
    final static String TAG = "WifiP2P";
    static final boolean DBG = false;
    private static final int MESSAGE_RETRY = 1;
    private final int DELAY = 5000;
    private Context mContext;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig = null;
    private IntentFilter mIntentFilter;

    private SharedPreferences mSp;
    private String mPeerServerIp;
    private String mSSID;
    private String mPass;
    private boolean mAPConfiged;
    private String mChannel;

    public void enableApOnApplicable() {
        if (DBG)
            Log.i(TAG,
                    "enableApOnApplicable. isAPConfiged()=" + isAPConfiged() + ", Device, mWifiManager.isWifiApEnabled()="
                            + mWifiManager.isWifiApEnabled());

        if (isAPConfiged()) {
//            if (!mWifiManager.isWifiApEnabled())
            // reenable is OK...
                enable();
//            else {
//                if (DBG)
//                    Log.d(TAG, "enableApOnApplicable. [ap configed and currently device is in AP, ignore.");
//            }
        }
    }

    public WifiP2P(Context context) {
        mContext = context;

        // The enable AP logic is only available on a box.
        // if (Constants.TEST_MOBILE_DEVICE_ID.equals(Build.ID)) {
        // if (DBG)
        // Log.i(TAG, "Don't initWifiP2P. Not a box, Build.ID=" + Build.ID + ", isWIFIP2P()" + isWIFIP2P());
        // return;
        // }

        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        mSp = Config.getSharedPreferences(context);
        updateAPInfo();
        mSp.registerOnSharedPreferenceChangeListener(this);

        mIntentFilter = new IntentFilter(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);

        enableApOnApplicable();
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

    protected void enable() {
        if (DBG)
            Log.d(TAG, "Async me");
        
        new WifiP2PConfigManager(mWifiManager).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSSID, mPass, mChannel);

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
        mPeerServerIp = null;
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

    protected void enableWifiCheckBox() {
        // TODO Auto-generated method stub
        if (DBG)
            Log.i(TAG, "enableWifiCheckBox. ");
    }

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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (DBG)
            Log.i(TAG, "onSharedPreferenceChanged. sharedPreferences, key=" + key + ", Thread=" + Thread.currentThread());

        // TODO Auto-generated method stub
        // The system will notify us the same number of time of the number of the keys changed.
        // i.e., if no key changed, no notification of the onSharedPreferenceChanged.
        if (updateAPInfo()) {
            AppController.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (DBG)
                        Log.i(TAG, "onSharedPreferenceChanged. Post mAPConfiged=" + mAPConfiged + ", Thread=" + Thread.currentThread());
                    if (mAPConfiged) {
                        // if (!mWifiManager.isWifiApEnabled()) {
                        enable();
                    } else {
                        // if (mWifiManager.isWifiApEnabled()) {
                        disable();
                    }
                }
            });
        }

    }

    public void onDetroy() {
        if (DBG)
            Log.i(TAG, "onDetroy. ");

        mSp.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void readTetheringIpFromProc() {
        if (DBG)
            Log.i(TAG, "readTetheringIp. mPeerServerIp=" + mPeerServerIp);

        if (mPeerServerIp == null) {
            if (DBG)
                Log.i(TAG, "readTetheringIp. ");
            mPeerServerIp = findIp();
            if (mPeerServerIp != null) {
                if (DBG)
                    Log.i(TAG, "readTetheringIp. found ip=" + mPeerServerIp);
            }
        }

    }

    /**
     * Extract and save ip and corresponding MAC address from arp table in HashMap
     */
    private String findIp() {
        if (DBG)
            Log.i(TAG, "findIp. ");

        String resultIp = null;
        BufferedReader localBufferdReader = null;
        try {
            localBufferdReader = new BufferedReader(new FileReader(new File("/proc/net/arp")));
            String line = "";
            while ((line = localBufferdReader.readLine()) != null) {
                String[] ipmac = line.split("[ ]+");
                if (!ipmac[0].matches("IP")) {
                    String ip = ipmac[0];
                    String mac = ipmac[3];
                    if (DBG)
                        Log.i(TAG, "createArpMap. line=" + line + ", ip=" + ip);
                    if (ip.startsWith("192.168.43") && !mac.equals("00:00:00:00:00:00")) {
                        resultIp = ip;
                        if (DBG)
                            Log.i(TAG, "createArpMap. find ip=" + resultIp);
                    }
                    // if (!checkMapARP.containsKey(ip)) {
                    // checkMapARP.put(ip, mac);
                    // }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error read arp:", e);
        } finally {
            if (localBufferdReader != null) {
                IoUtils.closeQuietly(localBufferdReader);
            }
        }

        // Use the last one.
        return resultIp;
    }

    @Override
    public String getServerIp() {
        if (!isAPConfiged()) {
            if (DBG)
                Log.i(TAG, "getServerIp. ");
            return null;
        }

        if (mPeerServerIp == null) {
            readTetheringIpFromProc();
        }
        return mPeerServerIp;
    }

    public boolean isAPConfiged() {
        return mAPConfiged;
    }

    public boolean updateAPInfo() {
        boolean isAPConfiged = mSp.getBoolean(Config.KEY_IS_WIFI_P2P, false);

        int apEnabledConfig = Settings.Global.getInt(mContext.getContentResolver(), ConfigAPI.ATTR_AP_ENABLED, -1);

        if(apEnabledConfig != (isAPConfiged ? 1 : 0))
            Settings.Global.putInt(mContext.getContentResolver(), ConfigAPI.ATTR_AP_ENABLED, isAPConfiged ? 1 : 0);
        String ssid = mSp.getString(Config.KEY_AP_SSID, "");
        String pass = mSp.getString(ConfigAPI.ATTR_AP_PASS, "");
        String channel = mSp.getString(ConfigAPI.ATTR_AP_CHANNEL, "6");
        try {
            Settings.Global.putInt(mContext.getContentResolver(), ConfigAPI.ATTR_AP_CHANNEL, Integer.valueOf(channel));
        }catch (Exception e){
            e.printStackTrace();
        }

        if (DBG)
            Log.d(TAG, "updateAPInfo. [isAPConfiged=" + isAPConfiged
                    + ", ssid=" + ssid
                    + ", pass=" + pass
                    + ", channel=" + channel
                    + ", mIsAP=" + mAPConfiged
                    + ", mSSID=" + mSSID
                    + ", mPass=" + mPass
                    + ", mChannel=" + mChannel
                    );

        boolean dirty = false;
        if (isAPConfiged != mAPConfiged) {
            mAPConfiged = isAPConfiged;
            if (DBG)
                Log.d(TAG, "updateAPInfo. [isAPConfiged changed.");

            dirty = true;
        }

        if (!ssid.equals(mSSID)) {
            mSSID = ssid;
            if (DBG)
                Log.d(TAG, "updateAPInfo. [ssid changed.");

            dirty = true;
        }

        if (!pass.equals(mPass)) {
            mPass = pass;
            if (DBG)
                Log.d(TAG, "updateAPInfo. [pass changed.");

            dirty = true;
        }
        
        if (!channel.equals(mChannel)) {
            mChannel = channel;
            if (DBG)
                Log.d(TAG, "updateAPInfo. [channel changed.");
            
            dirty = true;
        }

        if (DBG)
            Log.d(TAG, "updateAPInfo. [dirty=" + dirty);

        if (dirty) {
            if (DBG)
                Log.d(TAG, "Dirty.");
//             updateDb(isAPConfiged, ssid, pass, channel);
        }

        return dirty;
    }

//    private void updateDb(boolean isAPConfiged, String ssid, String pass, String channel) {
//        ContentValues values = new ContentValues(3);
//        values.put(ColorContract.COLUMN_ENABLED, isAPConfiged ? 1 : 0);
//        values.put(ColorContract.COLUMN_SSID, Wifi.normalize(ssid));
//        values.put(ColorContract.COLUMN_PASS, Wifi.normalize(pass));
//        // Not
//        values.put(ColorContract.COLUMN_RES1, Wifi.normalize(channel));
//        int update = AppController.getInstance().getContentResolver().update(ColorContract.NETWORK_AP_CONTENT_URI, values, null, null);
//        if (DBG)
//            Log.d(TAG, "updateDb AP. [update=" + update);
//    }
}
