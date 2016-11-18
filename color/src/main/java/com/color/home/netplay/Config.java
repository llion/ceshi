package com.color.home.netplay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.Constants;
import com.color.home.network.Ethernet;

/**
 * @author zzjd7382
 *
 *         Config:V
 *
 */
public class Config implements ConfigAPI {
    public static final boolean DBG = false;;
    public final static String TAG = "Config";

    public static final String KEY_SERVER_IPPORT = "ipcolonport";
    public static final String KEY_IS_WIFI_P2P = "iswifip2p";
    public static final String KEY_AP_SSID = ATTR_AP_SSID;
    public static final String KEY_AP_PASS = ATTR_AP_PASS;
    public static final String CMD_SCREENSHOT = "screenshot";

    private final static String CONFIG_FILE = "config.txt";
    /**
     * The expected config file is resident in this mnt sdcard root folder.
     */
    private String folderPlayMntSdcard0;
    public SharedPreferences mSp;
    private Context mContext;
    private FtpServer mFtpServer;
    public static final String UTF_8 = "UTF-8";

    public Config(Context context, String folderPlayMntSdcard0) {
        mContext = context;
        this.folderPlayMntSdcard0 = folderPlayMntSdcard0;
        mSp = getSharedPreferences(context);

        mFtpServer = new FtpServer();

        setupDefaultAPIfFirstRun();

        // Read config if ext storage exist and has config.txt.
        // it will write the sp if there is any.
        cfgFromUsb();
    }

    private void setupDefaultAPIfFirstRun() {
//        if (DBG) {
//            final Editor edit = mSp.edit();
//            edit.putBoolean("FirstInit", true);
//            edit.apply();
//        }

        if (mSp.getBoolean("FirstInit", true)) {
            String ro_serialno = SystemProperties.get("ro.serialno");
            String serialno = "0000";
            if (ro_serialno != null && ro_serialno.length() >= 4) {
                serialno = ro_serialno.substring(ro_serialno.length() - 4);
            }

            String modelname = "cx";
            if (ro_serialno != null && ro_serialno.length() >= 5) {
                modelname = ro_serialno.substring(3, 5).toLowerCase();
            }

            if (DBG)
                Log.d(TAG, "setupDefaultAPIfFirstRun. [serialno=" + serialno);

//            int [] candidateChannels = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
//            int channelrandom = candidateChannels[new Random().nextInt(11)];
            int [] candidateChannels = new int[] {1, 6, 11};
            int channelrandom = candidateChannels[new Random().nextInt(3)];

            if (DBG) {
                Log.d(TAG, "setupDefaultAPIfFirstRun. [Random channel=" + channelrandom);
            }

            saveAPInfo(true, modelname + "-" + serialno, "123456789", String.valueOf(channelrandom));
            final Editor edit = mSp.edit();
            edit.putBoolean("FirstInit", false);
            edit.apply();
        }
    }

    public Properties cfgFromUsb() {
        String path = folderPlayMntSdcard0 + "/" + CONFIG_FILE;
        File file = new File(path);

        return cfgByFile(file);
    }

    public Properties cfgByFile(File file) {
        if (DBG)
            Log.i(TAG, "readConfigFromExtStorage. file=" + file + ", file.exists()=" + file.exists());

        Properties pp = new Properties();
        FileInputStream in = null;
        if (file.exists()) {
            try {
                in = new FileInputStream(file);
                pp.load(in);

                if (DBG)
                    Log.d(TAG, "readAndSetupPropertiesFromUsb. [pp=" + pp);

                cfgByProperties(pp);
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return pp;
    }

    public void cfgByProperties(Properties pp) throws UnsupportedEncodingException {
        if (DBG) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) AppController.getInstance().getSystemService(
                    Context.CONNECTIVITY_SERVICE);
            NetworkInfo[] mNetworkInfo = mConnectivityManager.getAllNetworkInfo();
            for (NetworkInfo tt : mNetworkInfo) {
                Log.i(TAG, "networkInfo=" + tt);
            }
        }

        String locale = pp.getProperty(ATTR_LOCALE);
        if (locale != null) {
            mSp.edit().putString(ATTR_LOCALE, locale.trim()).apply();
        }

        String terminalName = pp.getProperty(ATTR_TERMINAL_NAME);
        if (terminalName != null) {
            if (isIsoFromTxtFile(pp))
                terminalName = new String(terminalName.getBytes("ISO-8859-1"), "UTF-8");
            mSp.edit().putString(ATTR_TERMINAL_NAME, terminalName.trim()).apply();
        }

        // Add text file charset support. eg. "UTF-8", "GBK" (default).
        String charset = pp.getProperty(ATTR_TEXT_CHARSET);
        if (charset != null) {
            mSp.edit().putString(ATTR_TEXT_CHARSET, charset.trim()).apply();
        }

        String antiAlias = pp.getProperty(ATTR_TEXT_ANTIALIAS);
        if (antiAlias != null) {
            mSp.edit().putString(ATTR_TEXT_ANTIALIAS, antiAlias.trim()).apply();
        }

        if (pp.getProperty(ATTR_MOBILE_ENABLED) != null)
            setMobileEnabled(pp);

        new Ethernet(mContext, pp);
        String ip = pp.getProperty(ATTR_SERVER_IP);
        if (ip != null)
            saveSrvIpNportFromUsb(ip.trim());

        if (pp.getProperty(ATTR_WIFI_ENABLED) != null) {
            if (isWifiModuleExists(mContext))
                new Wifi(pp);
        }

        if (pp.getProperty(ATTR_IS_WIFI_P2P) != null)
            saveWifiP2PFromExt(pp);

        String screenshot = pp.getProperty(CMD_SCREENSHOT);
        if (screenshot != null)
            screenshot(screenshot);

        mFtpServer.setupFtpService(pp);
    }

    private void setMobileEnabled(Properties pp) {
        Log.d(TAG, "currentlyAirplaneEnabled =" + (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1));

        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        String mobileMode = pp.getProperty(ConfigAPI.ATTR_MOBILE_ENABLED);
        boolean toEnableMobile = Config.isTrue(mobileMode);
        Settings.Global.putInt(mContext.getContentResolver(), ATTR_MOBILE_ENABLED, toEnableMobile ? 1 : 0);
        boolean toEnableAirplane = !toEnableMobile;
        Log.d(TAG, "Attempt to switch airplane mode to " + toEnableAirplane);
        cm.setAirplaneMode(toEnableAirplane);

        Settings.System.putInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON,toEnableAirplane ? 1 : 0);
//        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
//        intent.putExtra("state", toEnableAirplane);
//        mContext.sendBroadcast(intent);
        Log.d(TAG, "isAirplane mode on=" + Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0));

        mSp.edit().putString(ATTR_AP_SSID, "").apply();
        cm.setMobileDataEnabled(toEnableMobile);
    }

    private void setMobileDataState(boolean mobileDataEnabled)
    {
        try
        {
            TelephonyManager telephonyService = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

            Method setMobileDataEnabledMethod = telephonyService.getClass().getDeclaredMethod("setDataEnabled", boolean.class);

            if (null != setMobileDataEnabledMethod)
            {
                setMobileDataEnabledMethod.invoke(telephonyService, mobileDataEnabled);
            }
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error setting mobile data state", ex);
        }
    }

    public static boolean isIsoFromTxtFile(Properties pp) {
        return !pp.contains(UTF_8);
    }

    private void screenshot(String screenshot) {
        if (DBG)
            Log.d(TAG, "screenshot=" + screenshot);

        ArrayList<String> commands = new ArrayList<String>();
        String pngfilename = "screen.png";
        String screenshotsFolder = Constants.FOLDER_FTP + "/screenshots/";

        File file = new File(screenshotsFolder);
        if (!file.exists()) {
            file.mkdirs();
        }

        String path = screenshotsFolder + pngfilename;
        commands.add("screencap -p " + path);

        if (new File(Constants.FOLDER_USB_0).isDirectory()) {
            commands.add("cp " + path + " " + Constants.FOLDER_USB_0 + "/" + pngfilename);
        }

        if (DBG) {
            Log.d(TAG, "screenshot. [commands:");
            for (String str : commands) {
                Log.d(TAG, "screenshot. [command=" + str);
            }
        }

        Process process;
        try {
            process = FtpServer.RunAsRoot(commands.toArray(new String[0]));

            if (DBG) {
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = br.readLine();
                while (line != null) {
                    Log.d(TAG, "line=" + line);
                    line = br.readLine();
                }
                br.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveWifiP2PFromExt(Properties pp) throws UnsupportedEncodingException {
        String propIsAP = pp.getProperty(ATTR_IS_WIFI_P2P);
        if (propIsAP == null) {
            if (DBG)
                Log.d(TAG, "saveWifiP2PFromExt. [is.wifi.p2p not set, so do not save to pref. Abort.");
            return;
        }

        if (DBG)
            Log.i(TAG, "saveWifiP2PFromExt. strIsWifiP2PFromConfig=" + pp + ", Thread=" + Thread.currentThread());

        // final boolean enabledInPref = mSp.getBoolean(KEY_IS_WIFI_P2P, false);
        // No matter what, save the AP SSID/password.
        // The user could be intended to change the AP SSID or pass or both.
        final boolean apEnabledInUsb = Config.isTrue(propIsAP.trim());

        // Use apply() here, instead of commit().
        String ssid = pp.getProperty(ConfigAPI.ATTR_AP_SSID);
        String pass = pp.getProperty(ConfigAPI.ATTR_AP_PASS);
        String channel = pp.getProperty(ConfigAPI.ATTR_AP_CHANNEL, "6");
        // Only save ssid & pass on enabled.
        // why ? only save ssid/pass on enabled? 20150507.
        if (apEnabledInUsb && !TextUtils.isEmpty(ssid) && pass != null) {
            if (isIsoFromTxtFile(pp))
                ssid = new String(ssid.getBytes("ISO-8859-1"), "UTF-8");
        }

        if (ssid != null) {
            ssid = ssid.trim();
        }

        if (pass != null) {
            pass = pass.trim();
        }

        if (channel != null) {
            channel = channel.trim();
        }

        saveAPInfo(apEnabledInUsb, ssid, pass, channel);
    }

    /**
     * Save ap info to shared preferences, which in turn notifies WifiP2P.
     *
     * @param enabled
     * @param ssid
     * @param pass
     * @param channel
     */
    private void saveAPInfo(final boolean enabled, String ssid, String pass, String channel) {
        if (DBG)
            Log.d(TAG, "saveAPInfo. enabled= " + enabled + ", ssid= " + ssid + ", pass= " + pass + ", channel= " + channel);
        final Editor edit = mSp.edit();

        if (ssid != null) {
            edit.putString(Config.KEY_AP_SSID, ssid);
        }

        if (pass != null) {
            edit.putString(ConfigAPI.ATTR_AP_PASS, pass);
        }

        if (channel != null) {
            edit.putString(ConfigAPI.ATTR_AP_CHANNEL, channel);
        }

        // The KEY_IS_WIFI_P2P must be the last one to commit, because the listener is checking this one.
        // and upon receive this attrib's change notification, if will read SSID and PASS.

        // XXX: Shared pref will only notify upon a change of the KEY.
        // If the following KEY_IS_WIFI_P2P is not changed, no notification will be issued.
        edit.putBoolean(KEY_IS_WIFI_P2P, enabled).commit();
    }

    public void saveSrvIpNportFromUsb(String srvIpNPort) {
        final String ipNPort = mSp.getString(KEY_SERVER_IPPORT, "");
        if (ipNPort.equals(srvIpNPort)) {
            if (DBG)
                Log.i(TAG, "persistentServerIpNportFromExtStorage. serverIpNPort=" + srvIpNPort
                        + ", from sdcard is identical to the sp, do nothing.");

        } else {
            if (DBG)
                Log.i(TAG, "persistentServerIpNportFromExtStorage. Differs and save. sdcard ipport=" + srvIpNPort + ", while the sp="
                        + ipNPort);

            mSp.edit().putString(KEY_SERVER_IPPORT, srvIpNPort).commit();
        }
    }

    public boolean isAntialias() {
        return isTrue(mSp.getString(ATTR_TEXT_ANTIALIAS, "0"));
    }

    public static boolean isTrue(String property) {
        return "true".equalsIgnoreCase(property) || "1".equals(property);
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String getDeviceModelName() {
        String ro_serialno = SystemProperties.get("ro.serialno");
        String modelname = "cx";
        if (ro_serialno != null && ro_serialno.length() >= 5) {
            modelname = ro_serialno.substring(3, 5).toLowerCase();
        }
        return modelname;
    }


    public static boolean isWifiModuleExists(Context context) {
        //
        String deviceName = getDeviceModelName();
        if (DBG)
            Log.d(TAG, "device model name= " + deviceName);

        //
        if ("c3".equals(deviceName) || "c4".equals(deviceName)) {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
            if (DBG)
                Log.d(TAG, "usbDevice number= " + usbDevices.size());
            Iterator<UsbDevice> iterator = usbDevices.values().iterator();
            while (iterator.hasNext()) {
                UsbDevice usbDevice = iterator.next();

                if (DBG) {
                    Log.d(TAG, "DeviceId= " + usbDevice.getDeviceId() + ", DeviceName= " + usbDevice.getDeviceName() +
                            ", ProductId= " + usbDevice.getProductId() + ", VendorId= " + usbDevice.getVendorId()
                            + ", DeviceClass= " + usbDevice.getDeviceClass() + ", DeviceProtocol= " + usbDevice.getDeviceProtocol()
                            + ", InterfaceCount= " + usbDevice.getInterfaceCount());

                }

                if (usbDevice.getProductId() == 33145 && usbDevice.getVendorId() == 3034) {
                    if (DBG)
                        Log.d(TAG, deviceName + ", wifi module exists.");
                    return true;
                }
            }
            if (DBG)
                Log.d(TAG, deviceName + ", wifi module not exists.");
            return false;

        } else {
            if (DBG)
                Log.d(TAG, deviceName + ", wifi module exists.");
            return true;
        }
    }
}