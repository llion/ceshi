package com.color.home.widgets.singleline;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.opengl.GLSurfaceView;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.ScrollPicInfo;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.FinishObserver;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;

public class SLPCHTSurfaceView extends GLSurfaceView implements Runnable, OnPlayFinishObserverable, FinishObserver {
    private final static String TAG = "SLPCHTSurfaceView";
    private static final boolean DBG = false;
    private SLPCHTRenderer mRenderer;
    private OnPlayFinishedListener mListener;
    private Item mItem;

    public SLPCHTSurfaceView(Context context) {
        super(context);
        if (DBG)
            Log.d(TAG, "SLPCHTSurfaceView.");
        // setEGLConfigChooser(false);
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);
    }

    public void setItem(RegionView regionView, Item item) {
        SLPCHTTextObject theTextObj = getTextObj(item.scrollpicinfo);
        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        mRenderer = new SLPCHTRenderer(theTextObj);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // getHolder().setFormat(PixelFormat.RGBA_8888);

        mListener = regionView;
        this.mItem = item;

        if (DBG)
            Log.d(TAG, "setItem. [item.getTextBitmapHash()=" + item.getTextBitmapHash() + ", text=" + item.getTexts().mText);
        theTextObj.setTextItemBitmapHash(item.getTextBitmapHash());
        
        // Color.
        theTextObj.setTextColor(GraphUtils.parseColor(item.textColor));
        if (!TextUtils.isEmpty(item.backcolor) && !"0xFF000000".equals(item.backcolor)) {
            if ("0xFF010000".equals(item.backcolor)) {
                theTextObj.setBackgroundColor(GraphUtils.parseColor("0xFF000000"));
            } else {
                theTextObj.setBackgroundColor(GraphUtils.parseColor(item.backcolor));
            }
        } else {
            theTextObj.setBackgroundColor(GraphUtils.parseColor("0x00000000"));
        }

        float pixelPerFrame = MovingTextUtils.getPixelPerFrame(item);
//        theTextObj.setPixelPerFrame(Math.max(1, Math.round(pixelPerFrame)));
        theTextObj.setPixelPerFrame(Math.round(pixelPerFrame));
        
        // Total play length in milisec.
        int mPlayLength = Integer.parseInt(item.playLength);
        if (DBG)
            Log.d(TAG, "setItem. [mPlayLength=" + mPlayLength);
        boolean mIsScrollByTime = "1".equals(item.isscrollbytime);
        if (mIsScrollByTime) {
            removeCallbacks(this);
            postDelayed(this, mPlayLength);
        } else {
            // Counts.
            int repeatCount = Integer.parseInt(item.repeatcount);
            if (DBG)
                Log.d(TAG, "setItem. [repeatCount=" + repeatCount);
            theTextObj.setRepeatCount(repeatCount);
            theTextObj.setView(this);
            // int mDuration = Integer.parseInt(item.duration);
        }

        boolean isGlaring = "1".equals(mItem.beglaring);
        if (isGlaring) {
            theTextObj.getPaint().setShader(new LinearGradient(0, 0, 100, 100, new int[] {
                    Color.RED, Color.GREEN, Color.BLUE },
                    null, Shader.TileMode.MIRROR));
        }

    }

    protected SLPCHTTextObject getTextObj(ScrollPicInfo scrollpicinfo) {
        return new SLPCHTTextObject(getContext(), scrollpicinfo);
    }

    @Override
    public void run() {
        if (DBG)
            Log.i(TAG, "run. Finish item play due to play length time up");
        notifyPlayFinished();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        boolean removeCallbacks = removeCallbacks(this);
        if (DBG)
            Log.i(TAG, "onDetachedFromWindow. Try to remove call back. result is removeCallbacks=" + removeCallbacks);

        // if (mAnim != null) {
        // if (DBG)
        // Log.i(TAG, "onDetachedFromWindow. mAnim=" + mAnim + ", not null, end it. Thread=" + Thread.currentThread());
        // mAnim.end();
        // mAnim = null;
        // }
    }

    @Override
    public void notifyPlayFinished() {
        mRenderer.finish();

        if (mListener != null) {
            if (DBG)
                Log.i(TAG, "tellListener. Tell listener =" + mListener);
            mListener.onPlayFinished(this);
        }
    }

    @Override
    public void setListener(OnPlayFinishedListener listener) {
        this.mListener = listener;
    }

    @Override
    public void removeListener(OnPlayFinishedListener listener) {
        this.mListener = null;
    }

}