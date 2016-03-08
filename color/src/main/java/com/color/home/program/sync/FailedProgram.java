package com.color.home.program.sync;

import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;

import com.color.home.netplay.FtpServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by hmh on 2016/2/25.
 */
public class FailedProgram {

    public static final String TRYING_VSN = "color.trying_vsn";
    public static final String TRYING_TIME = "color.trying_time";
    public static final String BAD_VSN = "color.BadVSN_";
    public static final String BADVSN_TOTAL = "color.badvsn_total";
    private static final String TAG = "FailedProgram";
    private static final boolean DBG = false;

    public FailedProgram() {
    }

    public FailedProgram(File vsn) {
//        SystemProperties.PROP_VALUE_MAX
        if (DBG) Log.d(TAG, "FailedProgram, vsn=" + vsn);
        if (DBG) new Exception().printStackTrace();

        String normal = normalizeVsnFile(vsn);

        ArrayList<String> commands = new ArrayList<String>();
        commands.add("setprop " + TRYING_VSN + " '" + normal + "'");
        commands.add("setprop " + TRYING_TIME + " " + String.valueOf(SystemClock.uptimeMillis()));
//        commands.add("iwconfig wlan0 power off");
//        if (DBG)
//            Log.d(TAG, "setup =" + displayMode + " [commands 0=" + commands.get(0));

        Process process;
        try {
            process = FtpServer.RunAsRoot(commands.toArray(new String[0]));

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            {
                String line = br.readLine();
                while (line != null) {
                    if (DBG)
                        Log.d(TAG, "FailedProgram check line=" + line);
                    line = br.readLine();
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //SystemProperties.set(TRYING_VSN, normal);
        //SystemProperties.set(TRYING_TIME, String.valueOf(SystemClock.uptimeMillis()));
    }

    private String normalizeVsnFile(File vsn) {
        String normal = vsn.toString();
        if (normal.length() > SystemProperties.PROP_VALUE_MAX - 1) {
            normal = normal.substring(normal.length() - SystemProperties.PROP_VALUE_MAX + 1);
        }
        return normal;
    }

    public void check() {
        long uptimeMillis = SystemClock.uptimeMillis();

        long lastUp = SystemProperties.getLong(TRYING_TIME, 0L);
        if (DBG) Log.d(TAG, "check");
        if (uptimeMillis - lastUp < 1200L) {
            int badvsn_total = SystemProperties.getInt(BADVSN_TOTAL, 0);
            badvsn_total++;

            SystemProperties.set(BADVSN_TOTAL, String.valueOf(badvsn_total));
            SystemProperties.set(BAD_VSN + badvsn_total, SystemProperties.get(TRYING_VSN));


            ArrayList<String> commands = new ArrayList<String>();
            commands.add("setprop " + BADVSN_TOTAL + " " + String.valueOf(badvsn_total));
            commands.add("setprop " + BAD_VSN + (badvsn_total - 1) + " '" + SystemProperties.get(TRYING_VSN) + "'");


            Process process;
            try {
                process = FtpServer.RunAsRoot(commands.toArray(new String[0]));

                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                {
                    String line = br.readLine();
                    while (line != null) {
                        if (DBG)
                            Log.d(TAG, "FailedProgram check line=" + line);
                        line = br.readLine();
                    }
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public boolean okToPlay(File vsn) {
        String normal = normalizeVsnFile(vsn);
        int badvsn_total = SystemProperties.getInt(BADVSN_TOTAL, 0);
        for (int i = 0; i < badvsn_total; i++) {
            String badVsn = SystemProperties.get(BAD_VSN + i);
            if (badVsn != null && badVsn.equals(normal)) {
                if (DBG) Log.d(TAG, "bad vsn=" + badVsn);
                return false;
            }
        }

        return true;
    }

    public void clear() {
        int badvsn_total = SystemProperties.getInt(BADVSN_TOTAL, 0);

        ArrayList<String> commands = new ArrayList<String>();
        for (int i = 0; i < badvsn_total; i++) {
            commands.add("setprop " + BAD_VSN + i + " X");
        }
        commands.add("setprop " + BADVSN_TOTAL + " 0");

        Process process;
        try {
            process = FtpServer.RunAsRoot(commands.toArray(new String[0]));

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            {
                String line = br.readLine();
                while (line != null) {
                    if (DBG)
                        Log.d(TAG, "FailedProgram clear bad vsns=" + line);
                    line = br.readLine();
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}