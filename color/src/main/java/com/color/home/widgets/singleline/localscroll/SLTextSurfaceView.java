package com.color.home.widgets.singleline.localscroll;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.opengl.GLSurfaceView;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.LogFont;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.FinishObserver;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;
import com.color.home.widgets.multilines.ItemMLScrollMultipic2View;
import com.color.home.widgets.multilines.MultiPicScrollRenderer;
import com.color.home.widgets.singleline.MovingTextUtils;

public class SLTextSurfaceView extends GLSurfaceView implements Runnable, OnPlayFinishObserverable, FinishObserver, NetworkObserver {
    private final static String TAG = "SLTextSurfaceView";
    private static final boolean DBG = false;
    private TextRenderer mRenderer;
    private OnPlayFinishedListener mListener;
    private Item mItem;
    private TextObject mTextobj;
    private NetworkConnectReceiver mNetworkConnectReceiver;

    public SLTextSurfaceView(Context context, TextObject theTextObj) {
        super(context);
        this.mTextobj = theTextObj;

        // setEGLConfigChooser(false);
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);
    }

    public void setItem(RegionView regionView, Item item) {
        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        mRenderer = new TextRenderer(mTextobj);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // getHolder().setFormat(PixelFormat.RGBA_8888);

        mListener = regionView;
        this.mItem = item;

        mTextobj.setText(item.getTexts().mText);
        if (DBG)
            Log.d(TAG, "setItem. [item.getTextBitmapHash()=" + item.getTextBitmapHash() + ", text=" + item.getTexts().mText);
        mTextobj.setTextItemBitmapHash(item.getTextBitmapHash());

        // Color.
        if (DBG)
            Log.d(TAG, "setItem. [item.backcolor=" + item.backcolor);
        mTextobj.setTextColor(GraphUtils.parseColor(item.textColor));
        if (!TextUtils.isEmpty(item.backcolor) && !"0xFF000000".equals(item.backcolor)) {
            if ("0xFF010000".equals(item.backcolor)) {
                mTextobj.setBackgroundColor(GraphUtils.parseColor("0xFF000000"));
            } else {
                mTextobj.setBackgroundColor(GraphUtils.parseColor(item.backcolor));
            }
        } else {
            mTextobj.setBackgroundColor(GraphUtils.parseColor("0x00000000"));
        }

        // Size.
        int size = Integer.parseInt(item.logfont.lfHeight);

        mTextobj.setTextSize(size);

        LogFont logfont = item.logfont;
        if (logfont != null) {
            if (DBG) {
                Log.d(TAG, "logfont=" + logfont);
            }
            if (logfont.lfHeight != null) {
                mTextobj.setTextSize(Integer.parseInt(logfont.lfHeight));
            }

            if ("1".equals(logfont.lfUnderline)) {
                mTextobj.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
            }

            int style = Typeface.NORMAL;
            if ("1".equals(logfont.lfItalic)) {
                style = Typeface.ITALIC;
            }
            if ("700".equals(logfont.lfWeight)) {
                style |= Typeface.BOLD;
            }


            if (DBG) {
                Log.d(TAG, "style = " + style);
            }

            mTextobj.setTypeface(logfont.lfFaceName, style);
        }

        float pixelPerFrame = MovingTextUtils.getPixelPerFrame(item);
        if (DBG)
            Log.d(TAG, "setItem. [pixelPerFrame=" + pixelPerFrame);
//        mTextobj.setPixelPerFrame(Math.max(1, Math.round(pixelPerFrame)));
        mTextobj.setPixelPerFrame(pixelPerFrame);

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
            mTextobj.setRepeatCount(repeatCount);
            mTextobj.setView(this);
            // int mDuration = Integer.parseInt(item.duration);
        }

        boolean isGlaring = "1".equals(mItem.beglaring);
        if (isGlaring) {
            mTextobj.getPaint().setShader(new LinearGradient(0, 0, 100, 100, new int[] {
                    Color.RED, Color.GREEN, Color.BLUE },
                    null, Shader.TileMode.MIRROR));
        }

        mNetworkConnectReceiver = new NetworkConnectReceiver(this);
        ItemMLScrollMultipic2View.registerNetworkConnectReceiver(mContext, mNetworkConnectReceiver);

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

        if (mTextobj != null)
            mTextobj.removeCltRunnable();

        if (mNetworkConnectReceiver != null)
            mContext.unregisterReceiver(mNetworkConnectReceiver);
        // if (mAnim != null) {
        // if (DBG)
        // Log.i(TAG, "onDetachedFromWindow. mAnim=" + mAnim + ", not null, end it. Thread=" + Thread.currentThread());
        // mAnim.end();
        // mAnim = null;
        // }
    }

    @Override
    public void notifyPlayFinished() {
//        mRenderer.finish();

        if (DBG)
            Log.i(TAG, "tellListener. Tell listener =" + mListener);

        if (mListener != null) {
            mRenderer.finish();
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

    public TextRenderer getmRenderer() {
        return mRenderer;
    }

    @Override
    public void reloadContent() {
        if (DBG)
            Log.d(TAG, "reloadContent. mTheTextObj= " + mTextobj);
        if (mTextobj != null)
            mTextobj.setCltJsonText();
    }
}