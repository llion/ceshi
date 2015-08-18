package com.color.home;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import android.util.Log;

public class USBOtg {
    private final static String TAG = "USBOtg";
    private static final boolean DBG = false;

    private static final String HOST_MODE = new String("1");
    private static final String SLAVE_MODE = new String("2");
    private static final String SYS_FILE = "/sys/bus/platform/drivers/usb20_otg/force_usb_mode";

    public void Write2File(File file, String mode) {
        Log.d(TAG, "Write2File,write mode = " + mode);
        if ((file == null) || (!file.exists()) || (mode == null))
            return;

        try {
            FileOutputStream fout = new FileOutputStream(file);
            PrintWriter pWriter = new PrintWriter(fout);
            pWriter.println(mode);
            pWriter.flush();
            pWriter.close();
            fout.close();
        } catch (Exception e) {
            Log.e(TAG, "Error change usb to otg.", e);
        }
    }

    public USBOtg() {
        Write2File(new File(SYS_FILE), SLAVE_MODE);
    }
}