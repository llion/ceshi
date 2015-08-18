package com.color.home.netplay;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;


public class ServerConfig implements OnSharedPreferenceChangeListener {
    private final static String TAG = "InternetServer";
    private static final boolean DBG = false;

    private Context mContext;
    public SharedPreferences mSp;
    public String mServerIpNPort;

    public ServerConfig(Context context) {
        this.mContext = context;
        mSp = Config.getSharedPreferences(mContext);
        
        updateServerConfig();
        mSp.registerOnSharedPreferenceChangeListener(this);
    }

    public void updateServerConfig() {
        // Must default to null, instead of "".
        mServerIpNPort = mSp.getString(Config.KEY_SERVER_IPPORT, null);
    }

    public String getServerIpNPort() {
        return mServerIpNPort;
    }

    public void registerSPObserver() {
        mSp.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (DBG)
            Log.i(TAG, "onSharedPreferenceChanged. key=" + key + ", Thread=" + Thread.currentThread());

        // Only key changed for what was before is notified to me.
        if (Config.KEY_SERVER_IPPORT.equals(key)) {
            final String ipandportFromSp = mSp.getString(Config.KEY_SERVER_IPPORT, "");
            if (DBG)
                Log.i(TAG, "onSharedPreferenceChanged. ipandport. ipandport in sp=" + ipandportFromSp);

            if (ipandportFromSp.equals(getServerIpNPort())) {
                if (DBG)
                    Log.i(TAG, "onSharedPreferenceChanged. not changed.");

            } else {
                mServerIpNPort = ipandportFromSp;
                if (DBG)
                    Log.i(TAG, "onSharedPreferenceChanged. new ipandport=" + ipandportFromSp + ", mServerIpNPort=" + getServerIpNPort());
            }
        }

    }

    public void onDestroy() {
        if (DBG)
            Log.i(TAG, "onDestroy. ");
    
        mSp.unregisterOnSharedPreferenceChangeListener(this);
    }

}