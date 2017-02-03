package com.color.home.widgets.singleline;

import android.text.TextUtils;
import android.util.Log;

import com.color.home.ProgramParser.Item;
import com.color.home.netplay.FtpServer;

import java.io.IOException;
import java.util.ArrayList;

public class MovingTextUtils {
    private final static String TAG = "MovingTextUtils";
    private static final boolean DBG = false;
    public static boolean sIsPerformance = false;

    public static float getPixelPerFrame(Item item) {
        // TODO: Double check whether we really need the performance switch.
        // ensureSystemPerformance();

        float pixelPerFrame = 0.5f;
        try {
            // points/sec.
            float mSpeed = 30.f;
            if (!TextUtils.isEmpty(item.speed))
                 mSpeed = Float.parseFloat(item.speed);

            // points/frame.
            boolean mIfSpeedByFrame = "1".equals(item.ifspeedbyframe);

            float mSpeedByFrame = 1.0f;
            if (!TextUtils.isEmpty(item.speedbyframe))
                mSpeedByFrame = Float.parseFloat(item.speedbyframe);

            if (mIfSpeedByFrame) {
                pixelPerFrame = mSpeedByFrame;
            } else {
                // 60frames/sec is assumed.
                pixelPerFrame = mSpeed / 60.0f;
            }

            if (DBG)
                Log.d(TAG, "setItem. [mSpeed=" + mSpeed + ", mIfSpeedByFrame=" + mIfSpeedByFrame + ", mSpeedByFrame=" + mSpeedByFrame
                        + ", pixelPerFrame=" + pixelPerFrame);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return pixelPerFrame;

    }

    public static void ensureSystemPerformance() {
        if (!sIsPerformance) {

            ArrayList<String> cmd = new ArrayList<String>();
            cmd.add(" echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor \n");
            try {
                FtpServer.RunAsRoot(cmd.toArray(new String[0])).getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            sIsPerformance = true;
        }
    }

    public static int evenIt(int dimension) {
        if (dimension % 2 == 1)
            return dimension + 1;
        else
            return dimension;
    }

}
