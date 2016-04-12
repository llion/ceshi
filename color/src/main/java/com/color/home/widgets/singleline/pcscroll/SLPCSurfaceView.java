package com.color.home.widgets.singleline.pcscroll;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.opengl.GLSurfaceView;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.ScrollPicInfo;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.FinishObserver;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;
import com.color.home.widgets.singleline.MovingTextUtils;

public class SLPCSurfaceView extends GLSurfaceView implements Runnable, OnPlayFinishObserverable, FinishObserver {
    private final static String TAG = "SLPCSurfaceView";
    private static final boolean DBG = false;
    private SLPCRenderer mRenderer;
    private OnPlayFinishedListener mListener;
    private Item mItem;

    public SLPCSurfaceView(Context context) {
        super(context);

        // setEGLConfigChooser(false);
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);

//        Process.setThreadPriority(Process.THREAD_PRIORITY_LESS_FAVORABLE);

        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    public void setItem(RegionView regionView, Item item) {
        SLPCTextObject theTextObj = getTextObj(item.scrollpicinfo);

        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        mRenderer = new SLPCRenderer(theTextObj);
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
        if (DBG)
            Log.d(TAG, "setItem. [backcolor=" + item.backcolor);
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
        if (DBG)
            Log.d(TAG, "setItem. [pixelPerFrame=" + pixelPerFrame);
        theTextObj.setPixelPerFrame(Math.max(1, Math.round(pixelPerFrame)));

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
//            theTextObj.setItem(this.mItem);
            // int mDuration = Integer.parseInt(item.duration);
        }

//        boolean isGlaring = "1".equals(mItem.beglaring);
//        if (isGlaring) {
//            theTextObj.getPaint().setShader(new LinearGradient(0, 0, 100, 100, new int[] {
//                    Color.RED, Color.GREEN, Color.BLUE },
//                    null, Shader.TileMode.MIRROR));
//        }

    }

    protected SLPCTextObject getTextObj(ScrollPicInfo scrollpicinfo) {
        return new SLPCTextObject(getContext(), scrollpicinfo);
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

    /* (non-Javadoc)
     * @see com.color.home.widgets.singleline.FinishObserver#notifyPlayFinished()
     */
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