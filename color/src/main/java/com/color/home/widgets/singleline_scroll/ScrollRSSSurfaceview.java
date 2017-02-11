package com.color.home.widgets.singleline_scroll;

import android.content.Context;
import android.graphics.PixelFormat;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.ProgramParser;
import com.color.home.ProgramParser.Item;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.widgets.RegionView;
import com.color.home.widgets.multilines.ItemMLScrollMultipic2View;

import okhttp3.HttpUrl;

/**
 * Created by Administrator on 2017/2/10.
 */
public class ScrollRSSSurfaceview extends SinglelineScrollSurfaceView implements NetworkObserver {

    private final static String TAG = "ScrollRSSView";
    private static final boolean DBG = false;
    private final ProgramParser.Region mRegion;

    private ScrollRSSObject mScrollRSSObject;
//    private ScrollRSSRenderer mScrollRSSRenderer;

    private NetworkConnectReceiver mNetworkConnectReceiver;



    public ScrollRSSSurfaceview(Context context, ProgramParser.Region region, RegionView regionView) {
        super(context);
        Log.d(TAG, "ScrollRSSSurfaceview.");
        mContext = context;
        mListener = regionView;
        mRegion = region;
    }


    public void setRssItems(Item item, HttpUrl httpUrl) {

        setEGLContextClientVersion(2);
        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);

        mScrollRSSObject = new ScrollRSSObject(mContext, httpUrl);

        int textSize = 11;
        try {
            textSize = Integer.parseInt(mRegion.rect.height) - 2 * Integer.parseInt(mRegion.rect.borderwidth) - 3;
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        mScrollRSSObject.setTextSize(textSize);

        //pcHeight
        int pcHeight = textSize + 3;
        if (pcHeight % 2 == 1) {
            pcHeight++;
        }
        if (DBG)
            Log.d(TAG, "textSize= " + textSize + ", pcHeight= " + pcHeight);
        mScrollRSSObject.setPcHeight(pcHeight);

        //pixelsPerFrame
        float pixelPerFrame = 1.0f;
        String speedStr = httpUrl.queryParameter("speed");
        if (!TextUtils.isEmpty(speedStr)) {
            try {
                float speed = Integer.parseInt(speedStr);
                pixelPerFrame = speed / 60.f;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        mScrollRSSObject.setPixelPerFrame(pixelPerFrame);

        //play time
        int mPlayLength = 3600000;
        try {
            mPlayLength = Integer.parseInt(item.duration);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (DBG)
            Log.d(TAG, "setItem. [mPlayLength=" + mPlayLength);

        removeCallbacks(this);
        postDelayed(this, mPlayLength);

        mScrollRenderer = new ScrollRSSRenderer(mScrollRSSObject);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mScrollRenderer);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mNetworkConnectReceiver = new NetworkConnectReceiver(this);
        ItemMLScrollMultipic2View.registerNetworkConnectReceiver(mContext, mNetworkConnectReceiver);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DBG)
            Log.d(TAG, "onDetachedFromWindow. [mScrollRSSObject=" + mScrollRSSObject
                    + ", mNetworkConnectReceiver= " + mNetworkConnectReceiver);

        if (mScrollRSSObject != null){
            mScrollRSSObject.removeCltRunnable();
            mScrollRSSObject = null;
        }

        if (mNetworkConnectReceiver != null) {
            mContext.unregisterReceiver(mNetworkConnectReceiver);
            mNetworkConnectReceiver = null;
        }
    }

    @Override
    public void reloadContent() {
        if (DBG)
            Log.d(TAG, "reloadContent. mScrollRSSObject= " + mScrollRSSObject);
        if (mScrollRSSObject != null)
            mScrollRSSObject.initRSSContent();
    }
}
