package com.color.home.widgets.multilines;

import android.content.Context;
import android.graphics.PixelFormat;
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
import com.color.home.widgets.singleline.MovingTextUtils;

public class ItemMLScrollMultipic2View extends GLSurfaceView implements Runnable, OnPlayFinishObserverable, FinishObserver {
    private final static String TAG = "ItemMLScrollMultipic2V";
    private static final boolean DBG = false;
    private MultiPicScrollRenderer mRenderer;
    private OnPlayFinishedListener mListener;
    private Item mItem;

    public ItemMLScrollMultipic2View(Context context) {
        super(context);

        // setEGLConfigChooser(false);
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);
    }

    public void setItem(RegionView regionView, Item item) {
        MultiPicScrollObject theTextObj = getTextObj(item.scrollpicinfo);

        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        mRenderer = new MultiPicScrollRenderer(theTextObj);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // getHolder().setFormat(PixelFormat.RGBA_8888);

        mListener = regionView;
        this.mItem = item;

        int color = 0;
        if (!TextUtils.isEmpty(item.backcolor) && !"0xFF000000".equals(item.backcolor)) {
            if ("0xFF010000".equals(item.backcolor)) {
                color = GraphUtils.parseColor("0xFF000000");
            } else {
                color = GraphUtils.parseColor(item.backcolor);
            }
        } else {
            color = GraphUtils.parseColor("0x00000000");
        }

        if ("1".equals(item.invertClr)) {
            theTextObj.setBackgroundColor(GraphUtils.invertColor(color));
            if (DBG) {
                Log.d(TAG, "setItem. [invertClr, color=" + dumpColor(color) + ", invert color=" + GraphUtils.invertColor(color));
            }
        } else {
            theTextObj.setBackgroundColor(color);
        }

        float pixelPerFrame = MovingTextUtils.getPixelPerFrame(item);
//        theTextObj.setPixelPerFrame(Math.max(1, Math.round(pixelPerFrame)));
        theTextObj.setPixelPerFrame(pixelPerFrame/2);


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

    }

    public static String dumpColor(int color) {
        return Integer.toHexString(color);
        // return "A=" + Color.alpha(color) + ", R=" + Color.red(color) + ", G=" + Color.green(color) + ", B=" + Color.blue(color);
    }

    protected MultiPicScrollObject getTextObj(ScrollPicInfo scrollPicInfo) {
        return new MultiPicScrollObject(getContext(), scrollPicInfo);
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