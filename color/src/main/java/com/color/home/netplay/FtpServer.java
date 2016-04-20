package com.color.home.netplay;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Properties;

import android.util.Log;

import com.color.home.Constants;

public class FtpServer {
    private final static String TAG = "FtpServer";
    private static final boolean DBG = true;;
    public final static String ATTR_FTP_SERVICE_ENABLED = "ftp.service.enabled";

    public FtpServer() {

    }



    public void setupFtpService(Properties pp) {
        // XXX: adb shell "echo ftp.service.enabled=false> /mnt/usb_storage/USB_DISK0/udisk0/config.txt"
        // is not a valid Property file for Java.
        if (DBG)
            Log.d(TAG, "setupFtpService " + pp.getProperty(ATTR_FTP_SERVICE_ENABLED)
                    + ", pp=" + pp);
        String ftpServiceEnabled = pp.getProperty(ATTR_FTP_SERVICE_ENABLED);
        if (ftpServiceEnabled != null) {
            if (DBG)
                Log.d(TAG, "setupFtpService ftp service key found=" + ftpServiceEnabled);

            // SystemProperties.set("color.service.ftpd", Config.isEnabled(ftpServiceEnabled) ? "1" : "0");
            // String service = SystemProperties.get("color.service.ftpd");

            // register();

            ArrayList<String> commands = new ArrayList<String>();
            commands.add("setprop persist.color.service.ftpd " + (Config.isTrue(ftpServiceEnabled) ? "1" : "0"));
            if (DBG)
                Log.d(TAG, "setprop persist.color.service.ftpd. [commands 0=" + commands.get(0));

            Process process;
            try {
                process = RunAsRoot(commands.toArray(new String[0]));

                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                if (DBG) {
                    String line = br.readLine();
                    while (line != null) {
                        Log.d(TAG, "line=" + line);
                        line = br.readLine();
                    }
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Process RunAsRoot(String[] cmds) throws IOException {
        Process p = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        for (String tmpCmd : cmds) {
            os.writeBytes(tmpCmd + "\n");
        }
        os.writeBytes("exit\n");
        os.flush();
        os.close();
        return p;
    }

}
