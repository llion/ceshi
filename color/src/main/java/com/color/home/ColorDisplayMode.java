package com.color.home;

import android.util.Log;

import com.color.home.netplay.FtpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by hmh on 2015/12/3.
 */
public class ColorDisplayMode {
    private static final String TAG = "ColorDisplayMode";
    private static final boolean DBG = false;

    public static void setupDisplayMode() {

        setup(BuildConfig.DISPLAY_MODE);
    }

    private static void setup(String displayMode) {
        // XXX: adb shell "echo ftp.service.enabled=false> /mnt/usb_storage/USB_DISK0/udisk0/config.txt"
        // is not a valid Property file for Java.
        if (DBG)
            Log.d(TAG, "setup " + displayMode);

        ArrayList<String> commands = new ArrayList<String>();
        commands.add("echo " +
                displayMode +
                " > /sys/class/display/display0.HDMI/mode");
//        commands.add("iwconfig wlan0 power off");
        if (DBG)
            Log.d(TAG, "setup =" + displayMode + " [commands 0=" + commands.get(0));

        Process process;
        try {
            process = FtpServer.RunAsRoot(commands.toArray(new String[0]));

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            {
                String line = br.readLine();
                while (line != null) {
                    if (DBG)
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
