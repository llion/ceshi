package com.color.home.widgets.singleline_scroll;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import com.color.home.ProgramParser;
import com.color.home.ProgramParser.Item;
import com.color.home.widgets.FinishObserver;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.SinglelineScrollRegionView;
import com.color.home.widgets.singleline.MovingTextUtils;

import java.util.ArrayList;

/**
 * Created by Administrator on 2016/11/11.
 */
public class SinglelineScrollSurfaceView extends GLSurfaceView implements Runnable, OnPlayFinishObserverable, FinishObserver {
    private final static String TAG = "ScrollSurfaceView";
    private static final boolean DBG = false;

    private ArrayList<ProgramParser.Item> mItems;
    private SinglineScrollObject mScrollObject;
    protected SinglelineScrollRenderer mScrollRenderer;
    protected OnPlayFinishedListener mListener;

    public SinglelineScrollSurfaceView(Context context, SinglelineScrollRegionView singlelineScrollRegionView, SinglineScrollObject scrollObject) {
        super(context);

        mListener = singlelineScrollRegionView;
        mScrollObject = scrollObject;
        setEGLContextClientVersion(2);
    }

    public SinglelineScrollSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SinglelineScrollSurfaceView(Context context) {
        super(context);
    }



    @Override
    public void notifyPlayFinished() {

        if (DBG)
            Log.i(TAG, "tellListener. Tell listener =" + mListener);

        if (mListener != null) {
            mScrollRenderer.finish();
            mListener.onPlayFinished(this);
            removeListener(mListener);
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

    @Override
    public void run() {

        notifyPlayFinished();
    }

    public void setItems(ArrayList<Item> items) {
        this.mItems = items;

        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);

        //TODO::速度(第一个连续上移文本的速度)
        for (Item item : mItems){
            if ("5".equals(item.type) && "1".equals(item.isscroll)){
                //textSize
                int textSize = 11;//8号
                try {
                    textSize = Integer.parseInt(item.logfont.lfHeight);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                mScrollObject.setTextSize(textSize);

                //pcHeight
                int pcHeight = textSize + 3;
                if (pcHeight % 2 == 1) {
                    pcHeight++;
                }
                if (DBG)
                    Log.d(TAG, "textSize= " + textSize + ", pcHeight= " + pcHeight);
                mScrollObject.setPcHeight(pcHeight);

                //pixelsPerFrame
                mScrollObject.setPixelPerFrame(MovingTextUtils.getPixelPerFrame(item));

                //play time
                int mPlayLength = 0;
                try {
                    mPlayLength = Integer.parseInt(item.playLength);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                if (DBG)
                    Log.d(TAG, "setItem. [mPlayLength=" + mPlayLength);
                boolean mIsScrollByTime = "1".equals(item.isscrollbytime);

                if (mIsScrollByTime) {
                    removeCallbacks(this);
                    postDelayed(this, mPlayLength);
                } else {
                    // Counts.
                    int repeatCount = 1;
                    try {
                        repeatCount = Integer.parseInt(item.repeatcount);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                    if (DBG)
                        Log.d(TAG, "setItem. [repeatCount=" + repeatCount);
                    mScrollObject.setRepeatCount(repeatCount);
                    mScrollObject.setView(this);
                }

                break;
            }
        }

        mScrollRenderer = new SinglelineScrollRenderer(mScrollObject);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mScrollRenderer);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

    }

    public SinglelineScrollRenderer getRenderer() {
        return mScrollRenderer;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DBG)
            Log.i(TAG, "onDetachedFromWindow. Try to remove call back.");

        removeCallbacks(this);

    }
}
