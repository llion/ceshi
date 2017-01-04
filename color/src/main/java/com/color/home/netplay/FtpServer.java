package com.color.home.netplay;

import java.io.DataOutputStream;
import java.io.IOException;

public class FtpServer {
    private final static String TAG = "FtpServer";
    private static final boolean DBG = false;;

    public FtpServer() {

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
