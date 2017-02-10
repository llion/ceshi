package com.color.home.widgets.singleline_scroll;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Administrator on 2016/11/11.
 */
public class ScrollRSSRenderer extends SinglelineScrollRenderer {
    private final static String TAG = "ScrollRSSRenderer";
    private static final boolean DBG = false;


    public ScrollRSSRenderer(SinglineScrollObject scrollObject){
        super(scrollObject);
//        this.mScrollObject = scrollObject;
    }

}
