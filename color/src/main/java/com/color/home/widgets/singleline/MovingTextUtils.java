package com.color.home.widgets.singleline;

import android.util.Log;

import com.color.home.ProgramParser.Item;

public class MovingTextUtils {
    private final static String TAG = "MovingTextUtils";
    private static final boolean DBG = false;

    public static float getPixelPerFrame(Item item) {
        // points/sec.
        float mSpeed = Float.parseFloat(item.speed);
    
        // points/frame.
        boolean mIfSpeedByFrame = "1".equals(item.ifspeedbyframe);
        float mSpeedByFrame = Float.parseFloat(item.speedbyframe);
    
        float pixelPerFrame = 1.0f;
        if (mIfSpeedByFrame) {
            pixelPerFrame = mSpeedByFrame / 2.0f;
        } else {
            // 60frames/sec is assumed.
            pixelPerFrame = mSpeed / 60.0f;
        }

        if (DBG)
            Log.d(TAG, "setItem. [mSpeed=" + mSpeed + ", mIfSpeedByFrame=" + mIfSpeedByFrame + ", mSpeedByFrame=" + mSpeedByFrame
                    + ", pixelPerFrame=" + pixelPerFrame);

        return pixelPerFrame;

    }
    public static int evenIt(int dimension) {
        if (dimension % 2 == 1) return dimension + 1;
        else return dimension;
    }

}
