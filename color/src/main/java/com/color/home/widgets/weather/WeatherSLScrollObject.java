package com.color.home.widgets.weather;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.ProgramParser.Item;
import com.color.home.utils.WeatherInquirer;
import com.color.home.utils.WeatherJsonUtil;
import com.color.home.widgets.multilines.MultiPicScrollObject;
import com.color.home.widgets.singleline.QuadGenerator;
import com.color.home.widgets.singleline.localscroll.TextObject;

/**
 * Created by Administrator on 2017/1/11.
 */
public class WeatherSLScrollObject extends TextObject {
    final static String TAG = "WeatherSLScrollObject";
    static final boolean DBG = false;

    private WeatherInquirer mWeatherInquirer;
    private HandlerThread mHandlerThread;
    private Handler mWeatherHandler;

    private boolean mIsNeedUpdate = false;
    private int mCurrentTexId = 0;

    private Item mItem;


    public WeatherSLScrollObject(Context context, Item item) {
        super(context);
        mItem = item;
    }

    @Override
    public void update() {
        if (ItemWeatherMLPagesView.isNeedShowWeather(mItem)) {
            mTexCount = 2;
            genTexs();

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
                Log.d(TAG, "begin to retrive weather, requestTime= " + requestTime);
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
                if (TextUtils.isEmpty(mText))
                    text = "Failed to retrieve weather.";
                else {
                    Log.d(TAG, "Failed to retrieve weather, do not update.");
                }
            }

            if (DBG)
                Log.d(TAG, "mText= " + mText + ", text= " + text + ", mIsNeedUpdate= " + mIsNeedUpdate);

            if (text != null && !mText.equals(text)) {
                mText = text;

                if (!mIsNeedUpdate)
                    mIsNeedUpdate = true;

            } else if (text != null) {
                Log.d(TAG, "the weather information has not changed.");
            }
        }
    };


    @Override
    public void render() {
        if (DBG_MATRIX)
            Log.d(TAG, "render... mIsNeedUpdate= " + mIsNeedUpdate);

        if (mIsNeedUpdate) {
            if (DBG)
                Log.d(TAG, "update data...");
            mIsNeedUpdate = false;

            mTexDim = -1;
            setTextItemBitmapHash(mItem.getTextBitmapHash(mText));

            AppController.MyBitmap texFromMemCache = texFromMemCache();
            if (DBG)
                Log.d(TAG, "render. [texFromMemCache= " + texFromMemCache);

            if (texFromMemCache == null)
                prepareTexture();
            else{
                setPcWidth(texFromMemCache.mSingleLineWidth);
                setPcHeight(texFromMemCache.mSingleLineHeight);
                setTexDim(QuadGenerator.findClosestPOT(mPcWidth, getEvenPcHeight()));

                mRealReadPcWidth = getRealReadPcWidth(mPcWidth, mPcHeight, getTexDim());
            }

            if (mCurrentTexId == 0)
                mCurrentTexId = 1;
            else mCurrentTexId = 0;

            initTex();
        }

        if (mIsGreaterThanAPixelPerFrame)
            Matrix.translateM(mMMatrix, 0, mPixelPerFrame, 0.f, 0.f);
        else {
            pixelTemp += mPixelPerFrame;
            if (pixelTemp <= -1.0f) {
                Matrix.translateM(mMMatrix, 0, -1.0f, 0.f, 0.f);
                pixelTemp += 1;
            }
        }

        if (DBG_MATRIX)
            Log.d(TAG, "mMMatrix [12]  = " + mMMatrix[12]);

        if (mMMatrix[12] < -mEvenedWidth - mRealReadPcWidth) {
            if (DBG)
                Log.d(TAG, "mRealReadPcWidth= " + mRealReadPcWidth);
            Matrix.setIdentityM(mMMatrix, 0);
            // if repeat count == 0, infinite loop.
            if (mRepeatCount != 0) {
                if (++mCurrentRepeats >= mRepeatCount) {
                    notifyFinish();
                }
            }
        }

        GLES20.glUniformMatrix4fv(muMMatrixHandle, 1, false, mMMatrix, 0);
        // GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        // if (DBG)
        // Log.d(TAG, "render. [mIndices.length=" + mIndices.length);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, mVertexcount, GLES20.GL_UNSIGNED_SHORT, 0);
    }

    private void initTex() {
        updatePageToTexId(mCurrentTexId, mCurrentTexId);


        if (DBG)
            android.util.Log.i(TAG, "bmpSize[" + mPcWidth + ", " + getPcHeight() + "]");

        initShapes();

        setupMVP();

        Matrix.setIdentityM(mMMatrix, 0);

        GLES20.glUniform2f(muTexScaleHandle, (float) mPcWidth, (float) getEvenPcHeight());

        GLES20.glUniformMatrix4fv(muMMatrixHandle, 1, false, mMMatrix, 0);

        // Prepare the triangle data
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 12, mQuadVB);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        // Prepare the triangle data
        GLES20.glVertexAttribPointer(maTexCoordsHandle, 3, GLES20.GL_FLOAT, false, 12, mQuadTCB);
        GLES20.glEnableVertexAttribArray(maTexCoordsHandle);

        // Draw the triangle
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
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

    public void reload() {
        if (DBG)
            Log.d(TAG, "reload. mWeatherHandler= " + mWeatherHandler + ", mWeatherRunnable= " + mWeatherRunnable);
        if (mWeatherHandler != null && mWeatherRunnable != null) {
            mWeatherHandler.removeCallbacks(mWeatherRunnable);
            mWeatherHandler.post(mWeatherRunnable);
        }
    }
}
