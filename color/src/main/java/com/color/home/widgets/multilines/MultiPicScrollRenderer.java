/*
 * This proprietary software may be used only as
 * authorised by a licensing agreement from ARM Limited
 * (C) COPYRIGHT 2013 ARM Limited
 * ALL RIGHTS RESERVED
 * The entire notice above must be reproduced on all authorised
 * copies and copies may only be made to the extent permitted
 * by a licensing agreement from ARM Limited.
 */

package com.color.home.widgets.multilines;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

public class MultiPicScrollRenderer implements GLSurfaceView.Renderer {
    private final static String TAG = "MultiPicScrollRenderer";
    private static final boolean DBG = false;

    private MultiPicScrollObject mTheTextObj;
    private boolean mIsFinished;

    // private float theViewportHeight = 0.0f;

    // private boolean mustRebuildText = true;

    // private long mLastTime;
    // private int mFPS;

    public MultiPicScrollRenderer(MultiPicScrollObject theTextObj) {
        super();
        mTheTextObj = theTextObj;

    }

    // // Touch UP event occured
    // public void touchUp(float aX, float aY) {
    // mustRebuildText = true;
    // }
    //
    // // Touch UP event occured
    // public void touchMove(float aX, float aY,
    // float aPrevX, float aPrevY) {
    // mTheTextObj.setRelPos(0.0f, 0.0f, (aPrevY - aY)/(float)(theViewportHeight / 4));
    // }
    //
    // // Touch UP event occured
    // public void touchDown(float aX, float aY) {
    // }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        if (DBG)
            android.util.Log.i(TAG, "onSurfaceCreated Extensions: " + GLES20.glGetString(GLES20.GL_EXTENSIONS));

        GLES20.glDisable(GL10.GL_DITHER);
        // Set the background frame color
//        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        /*
         * Just in case you would like to set UTF8 text as a string. Below is an example of chinese string converted to UTF8 char
         * utf8Chars[] = {0xEF, 0xBB, 0xBF, 0xE6, 0x9C, 0xAC, 0xE7, 0xBD, 0x91, 0xE9, 0xA6, 0x96, 0xE9, 0xA1, 0xB5}; byte byteChars[] = new
         * byte[utf8Chars.length]; for (int i = 0; i < utf8Chars.length; i++ ) byteChars[i] = (byte)utf8Chars[i];
         * 
         * String str = null; try { str = new String(byteChars, "UTF8"); } catch (Exception e) { } mTheTextObj.setText(str);
         */

        // Initialize text object
        mTheTextObj.init();

        checkGLError("onSurfaceCreated");
    }

    public void onDrawFrame(GL10 unused) {
        // Do not draw.
        if (mIsFinished == true) {
            if (DBG)
                Log.d(TAG, "onDrawFrame. [Finished.");
            return;
        }

        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Render text object
        mTheTextObj.render();

        // if (mustRebuildText) {
        // mTheTextObj.update();
        // mustRebuildText = false;
        // }

        if (DBG)
            checkGLError("onDrawFrame");

        // if (DBG) {
        // mFPS++;
        // long currentTime = System.currentTimeMillis();
        // if (currentTime - mLastTime >= 1000) {
        // Log.d(TAG, "onDrawFrame. this=" + this + ", [mFPS=" + mFPS);
        // mFPS = 0;
        // mLastTime = currentTime;
        // }
        // }
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
        if (DBG)
            Log.d(TAG, "onSurfaceChanged. [height= " + height);
        GLES20.glViewport(0, 0, width, height);
        
        try {
            mTheTextObj.setDimension(width, height);
            mTheTextObj.update();
            
            if (DBG)
                Log.d(TAG, "onSurfaceChanged. [width=" + width + ", height=" + height);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Update Projection matrix
        // mTheTextObj.updateCamera(width, height);
    }

    public void finish() {
        mIsFinished = true;
    }

    public void notFinish() {
        mIsFinished = false;
    }
}
