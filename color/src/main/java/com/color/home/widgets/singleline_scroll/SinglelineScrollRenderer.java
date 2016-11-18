package com.color.home.widgets.singleline_scroll;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.color.home.widgets.singleline.localscroll.TextObject;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Administrator on 2016/11/11.
 */
public class SinglelineScrollRenderer implements GLSurfaceView.Renderer {
    private final static String TAG = "ScrollRenderer";
    private static final boolean DBG = false;

    private SinglineScrollObject mScrollObject;
    private boolean mIsFinished;

    public SinglelineScrollRenderer(SinglineScrollObject scrollObject){
        super();
        this.mScrollObject = scrollObject;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (DBG)
            android.util.Log.i(TAG, "onSurfaceCreated Extensions: " + GLES20.glGetString(GLES20.GL_EXTENSIONS));

        GLES20.glDisable(GL10.GL_DITHER);
        // Set the background frame color
//        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        mScrollObject.init();

        checkGLError("onSurfaceCreated");
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        // Do not draw.
        if (mIsFinished == true) {
            if (DBG)
                Log.d(TAG, "onDrawFrame. [Finished.");
            return;
        }

        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mScrollObject.render();

        if (DBG)
            checkGLError("onDrawFrame");

    }


    static public void checkGLError(final String aDesc) {
        int errorCode = GLES20.GL_NO_ERROR;
        do {
            errorCode = GLES20.glGetError();
            if (errorCode != GLES20.GL_NO_ERROR)
                android.util.Log.i("ERROR", "GL error: " + aDesc + " errorCode:" + errorCode);
        } while (errorCode != GLES20.GL_NO_ERROR);
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // theViewportHeight = height;
        // Update viewport
        GLES20.glViewport(0, 0, width, height);
        mScrollObject.setDimension(width, height);

        if (!mScrollObject.update())
            notFinish();

        if (DBG)
            Log.d(TAG, "onSurfaceChanged. [width=" + width + ", height=" + height);
    }

    public void finish() {
        mIsFinished = true;
    }

    public void notFinish() {
        mIsFinished = false;
    }
}
