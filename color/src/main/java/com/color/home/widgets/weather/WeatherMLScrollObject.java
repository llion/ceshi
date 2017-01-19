package com.color.home.widgets.weather;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.ProgramParser;
import com.color.home.utils.WeatherInquirer;
import com.color.home.utils.WeatherJsonUtil;
import com.color.home.widgets.multilines.MultiPicScrollObject;
import com.color.home.widgets.singleline.localscroll.TextRenderer;

/**
 * Created by Administrator on 2017/1/10.
 */
public class WeatherMLScrollObject extends MultiPicScrollObject {

    private final static String TAG = "WeatherMLScrollObject";
    private static final boolean DBG = false;

    WeatherInquirer mWeatherInquirer;

    HandlerThread mHandlerThread;
    Handler mWeatherHandler;
    private boolean mIsNeedUpdate = false;
    private String mCurrentText = "";
    private int mCurrentTexId = 1;

    public WeatherMLScrollObject(Context context, ProgramParser.Item item) {
        super(context, item);
    }

    @Override
    protected void update() {
        if (ItemWeatherMLPagesView.isNeedShowWeather(mItem)) {

            mTexCount = 2;
            mWeatherInquirer = new WeatherInquirer(mItem.regioncode);

            mHandlerThread = new HandlerThread("weather-thread");
            mHandlerThread.start();
            Looper looper = mHandlerThread.getLooper();
            mWeatherHandler = new Handler(looper);

            mWeatherHandler.post(mWeatherRunnable);

        } else {
            if (DBG)
                Log.d(TAG, " nothing to display.");
        }


    }

    Runnable mWeatherRunnable = new Runnable() {
        @Override
        public void run() {

            mWeatherHandler.postDelayed(this, 3 * 60 * 60 * 1000);//update data

            long requestTime = System.currentTimeMillis();
            if (DBG)
                Log.d(TAG, "begin to retrive weather, requestTime= " + requestTime + ", Thread= " + Thread.currentThread());
            mWeatherInquirer.retrieve();

            while (true) {
                if (System.currentTimeMillis() - requestTime > 10000) {
                    Log.d(TAG, "" + new Exception("Time out on retrieving weather."));
                    break;

                } else {
                    if (mWeatherInquirer.ismIsCompleted())
                        break;
                    else {
                        if (DBG)
                            Log.d(TAG, "retrive is not end. currentTime= " + System.currentTimeMillis());

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            String text = null;
            if (mWeatherInquirer.ismIsCompleted()) {
                if (DBG)
                    Log.d(TAG, "retrive successful.");
                text = ItemWeatherMLPagesView.getWeatherText(mItem, WeatherJsonUtil.getWeatherBean(mWeatherInquirer.getWeatherJson()));
            } else {
                if (DBG)
                    Log.d(TAG, "retrive failed.");
                if (TextUtils.isEmpty(mCurrentText))
                    text = "Failed to retrieve weather.";
                else {
                    Log.d(TAG, "Failed to retrieve weather, do not update.");
                }
            }

            if (DBG)
                Log.d(TAG, "mCurrentText= " + mCurrentText + ", text= " + text + ", mIsNeedUpdate= " + mIsNeedUpdate);

            if (text != null && !mCurrentText.equals(text)) {
                mCurrentText = text;
                mIsNeedUpdate = true;

            } else if (text != null) {
                Log.d(TAG, "the weather information has not changed.");
            }
        }
    };


    private void prepareTexture() {

        getTextBitmapAndDrawToTexture(mCurrentText);

        if (mIsTallPCPic) {
            generateTallPCMulpicQuads();
        } else if (mIsFatPCPic) {
            generateFatPCMulpicQuads();
        }

        if (mCurrentTexId == 1)
            mCurrentTexId = 0;
        else if (mCurrentTexId == 0)
            mCurrentTexId = 1;

        updateTexIndexToTexId(mCurrentTexId, mCurrentTexId);

        if (DBG)
            android.util.Log.i("INFO", "bmpSize[" + mPcWidth + ", " + mPcHeight + "]");

        initShapes();

        setupMVP();

        if (mTexIds == null) {
            if (DBG)
                Log.d(TAG, "update. [No content as mTexIds is null.");
            return;
        }

        mSr = new SegRender(mHeight);

        resetPos();
        GLES20.glUniformMatrix4fv(muMMatrixHandle, 1, false, mMMatrix, 0);

        // Prepare the triangle data
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 12, mQuadVB);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        GLES20.glVertexAttribPointer(maTexCoordsHandle, 3, GLES20.GL_FLOAT, false, 12, mQuadTCB);
        GLES20.glEnableVertexAttribArray(maTexCoordsHandle);

        // Draw the triangle
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // isUpdateNeeded = false;

    }

    @Override
    public void render() {

        if (mIsNeedUpdate) {
            mIsNeedUpdate = false;
            prepareTexture();
        }

        float[] modelMat = mMMatrix;
        if (mIsGreaterThanAPixelPerFrame)
            Matrix.translateM(modelMat, 0, 0.f, mPixelPerFrame, 0.f);
        else {
            pixelTemp += mPixelPerFrame;
            if (pixelTemp >= 1.0f) {
                Matrix.translateM(modelMat, 0, 0.f, 1.0f, 0.f);
                pixelTemp -= 1;
            }
        }

        if (MATRIX_DBG)
            Log.d(TAG, "matrix[13] = " + mMMatrix[13]);

        if (mIsTallPCPic && isTallPCPicSurplus) {// fat multipic and tallPCPic is surplus
            if (modelMat[13] > mHeight + mTextureHeight * mRealSegmentsPerTex)
                reset();
        } else { // fat multipic || (tall multipic && !isTallPCPicSurplus)
            if (modelMat[13] > mHeight + mPcHeight)
                reset();
        }

        GLES20.glUniformMatrix4fv(muMMatrixHandle, 1, false, modelMat, 0);

        if (mSr != null)
            mSr.render(modelMat[13]);
    }

    public void removeWeatherRunnable() {

        if (DBG)
            Log.d(TAG, "removeCltRunnable. mWeatherHandler= " + mWeatherHandler
                    + ", mHandlerThread= " + mHandlerThread
                    + ", mWeatherRunnable= " + mWeatherRunnable);

        if (mWeatherHandler != null) {
            mWeatherHandler.removeCallbacks(mWeatherRunnable);
        }
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
    }

    class SegRender extends MultiPicScrollObject.SegRender {

        public SegRender(int windowHeight) {
            super(windowHeight);

            if (DBG)
                Log.d(TAG, "SegRender. windowHeight= " + windowHeight);
        }

        @Override
        protected void drawQuad(int quadIndex) {
            int texIndex = quadIndex / mMaxSegmentsPerTexContain;
            if (RENDER_DBG)
                Log.d(TAG, "drawQuad.quadIndex= " + quadIndex + ", texIndex= " + texIndex + ", mCurrentTexId= " + mCurrentTexId);

            if (RENDER_DBG)
                TextRenderer.checkGLError("glBindTexture");
            GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, 4, GLES20.GL_UNSIGNED_SHORT, quadIndex * 4 * 2);
        }
    }


    public void reload() {
        if (mWeatherHandler != null && mWeatherRunnable != null) {
            mWeatherHandler.removeCallbacks(mWeatherRunnable);
            mWeatherHandler.post(mWeatherRunnable);
        }
    }


}
