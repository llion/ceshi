package com.color.home.utils;

import android.graphics.Color;

public class GraphUtils {

    public static int parseColor(String color) {
        int resultColor = 0x0;
        try {
            resultColor = Color.parseColor(color.replace("0x", "#"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultColor;
    }

    /**
     * Invert the color, but keep the alpha.
     *
     * @param color
     * @return
     */
    public static int invertColor(int color) {
        return Color.argb(Color.alpha(color), Color.red(~color), Color.green(~color), Color.blue(~color));
    }

    public static void convertRGBFromPC(byte[] content) {
        for (int i = 0; i < content.length; i += 4)
        {
            // Swap the R B on when they differ.
            if (content[i] != content[i + 2]) {
                final byte ele = content[i];
                content[i] = content[i + 2];
                content[i + 2] = ele;
            }
        }
    }
}
