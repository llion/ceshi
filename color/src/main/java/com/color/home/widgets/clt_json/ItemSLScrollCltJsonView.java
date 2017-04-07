package com.color.home.widgets.clt_json;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.ProgramParser;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.RegionView;
import com.color.home.widgets.singleline.MovingTextUtils;
import com.color.home.widgets.singleline.localscroll.SLTextSurfaceView;
import com.color.home.network.NetworkObserver;
import com.color.home.widgets.singleline.localscroll.TextRenderer;

/**
 * Created by Administrator on 2017/4/5.
 */

public class ItemSLScrollCltJsonView extends SLTextSurfaceView implements NetworkObserver {
    protected final static String TAG = "ItemSLScrollCltJsonView";
    protected static final boolean DBG = false;

    private SLScrollCltJsonObject mTheTextObj;
    private NetworkConnectReceiver mNetworkConnectReceiver;

    public ItemSLScrollCltJsonView(Context context, SLScrollCltJsonObject theTextObj) {
        super(context);
        this.mTheTextObj = theTextObj;

        setEGLContextClientVersion(2);
    }

    @Override
    public void setItem(RegionView regionView, ProgramParser.Item item) {
        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        mRenderer = new TextRenderer(mTheTextObj);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // getHolder().setFormat(PixelFormat.RGBA_8888);

        mListener = regionView;
        this.mItem = item;

        initDisplay(item);

        float pixelPerFrame = MovingTextUtils.getPixelPerFrame(item);
        if (DBG)
            Log.d(TAG, "setItem. [pixelPerFrame=" + pixelPerFrame);
        mTheTextObj.setPixelPerFrame(pixelPerFrame);

        // Total play length in milisec.
        long playLength = 300000;
        try {
            playLength = Long.parseLong(item.playLength);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (DBG)
            Log.d(TAG, "setItem. [playLength=" + playLength);

        boolean mIsScrollByTime = "1".equals(item.isscrollbytime);
        if (mIsScrollByTime) {
            removeCallbacks(this);
            postDelayed(this, playLength);
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
            mTheTextObj.setRepeatCount(repeatCount);
            mTheTextObj.setView(this);
            // int mDuration = Integer.parseInt(item.duration);
        }

        mNetworkConnectReceiver = new NetworkConnectReceiver(this);
    }
    protected void initDisplay(ProgramParser.Item item) {
        // Color.
        if (DBG)
            Log.d(TAG, "setItem. [item.backcolor=" + item.backcolor);
        mTheTextObj.setTextColor(GraphUtils.parseColor(item.textColor));
        if (!TextUtils.isEmpty(item.backcolor) && !"0xFF000000".equals(item.backcolor)) {
            if ("0xFF010000".equals(item.backcolor)) {
                mTheTextObj.setBackgroundColor(GraphUtils.parseColor("0xFF000000"));
            } else {
                mTheTextObj.setBackgroundColor(GraphUtils.parseColor(item.backcolor));
            }
        } else {
            mTheTextObj.setBackgroundColor(GraphUtils.parseColor("0x00000000"));
        }

        // Size.
        int size = Integer.parseInt(item.logfont.lfHeight);

        mTheTextObj.setTextSize(size);

        ProgramParser.LogFont logfont = item.logfont;
        if (logfont != null) {
            if (DBG) {
                Log.d(TAG, "logfont=" + logfont);
            }
            if (logfont.lfHeight != null) {
                mTheTextObj.setTextSize(Integer.parseInt(logfont.lfHeight));
            }

            if ("1".equals(logfont.lfUnderline)) {
                mTheTextObj.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
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

            mTheTextObj.setTypeface(logfont.lfFaceName, style);
        }

        boolean isGlaring = "1".equals(mItem.beglaring);
        if (isGlaring) {
            mTheTextObj.getPaint().setShader(new LinearGradient(0, 0, 100, 100, new int[]{
                    Color.RED, Color.GREEN, Color.BLUE},
                    null, Shader.TileMode.MIRROR));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mNetworkConnectReceiver != null)
            ItemMLPagedCltJsonView.registerNetworkConnectReceiver(mContext, mNetworkConnectReceiver);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mTheTextObj != null)
            mTheTextObj.removeCltRunnable();

        if (mNetworkConnectReceiver != null)
            ItemMLPagedCltJsonView.unRegisterNetworkConnectReceiver(mContext, mNetworkConnectReceiver);
    }

    @Override
    public void reloadContent() {

        if (DBG)
            Log.d(TAG, "reloadContent. mTheTextObj= " + mTheTextObj);
        if (mTheTextObj != null)
            mTheTextObj.reloadCltJson();

    }
}
