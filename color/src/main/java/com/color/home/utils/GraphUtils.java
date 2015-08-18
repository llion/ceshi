package com.color.home.utils;

import android.graphics.Color;

public class GraphUtils {

    public static int parseColor(String color) {
        return Color.parseColor(color.replace("0x", "#"));
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

}
