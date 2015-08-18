package com.color.home.netplay;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import android.content.ContentValues;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.network.WifiConnect;
import com.color.home.provider.ColorContract;

public class Wifi {
    private final static String TAG = "Wifi";
    private static final boolean DBG = false;
    final WifiConnect mWc;

    public Wifi(Properties pp) throws UnsupportedEncodingException {
        mWc = new WifiConnect();

        final boolean enabled = Config.isTrue(pp.getProperty(ConfigAPI.ATTR_WIFI_ENABLED));
        mWc.setWifi(enabled);

        if (enabled) {
            String ssid = pp.getProperty(ConfigAPI.ATTR_WIFI_SSID);
            String pass = pp.getProperty(ConfigAPI.ATTR_WIFI_PASS);
            String type = pp.getProperty(ConfigAPI.ATTR_WIFI_TYPE);
            String hidden = pp.getProperty(ConfigAPI.ATTR_WIFI_ISHIDDEN);
            // Removed the password != null config, as there could be open wifi.
            if (!TextUtils.isEmpty(ssid)) {
                if (Config.isIsoFromTxtFile(pp))
                    ssid = new String(ssid.getBytes("ISO-8859-1"), "UTF-8");
                mWc.connectTo(ssid, pass, type, hidden);
            }
        } 
        
        // We can re enable the wifi, but it's OK.
        // The user could specify enable WIFI, w/o give ssid or pass, 
        // as he is only enable a previously a good wifi connection.
        mWc.setWifi(enabled);

        saveToDb(pp, enabled);

    }

    public void saveToDb(Properties pp, final boolean enabled) {
        String ssid = pp.getProperty(ConfigAPI.ATTR_WIFI_SSID);
        String pass = pp.getProperty(ConfigAPI.ATTR_WIFI_PASS);

        ContentValues values = new ContentValues(3);
        values.put(ColorContract.COLUMN_ENABLED, enabled ? 1 : 0);
        values.put(ColorContract.COLUMN_SSID, normalize(ssid));
        values.put(ColorContract.COLUMN_PASS, normalize(pass));

        int update = AppController.getInstance().getContentResolver().update(ColorContract.NETWORK_WIFI_CONTENT_URI, values, null, null);
        if (DBG)
            Log.d(TAG, "Wifi. [update=" + update);
    }

    public static String normalize(String property) {
        return property != null ? property : "";
    }

}
