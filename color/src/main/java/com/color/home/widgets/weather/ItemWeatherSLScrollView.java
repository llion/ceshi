package com.color.home.widgets.weather;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;

import com.color.home.ProgramParser;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.widgets.RegionView;
import com.color.home.widgets.multilines.ItemMLScrollMultipic2View;
import com.color.home.widgets.singleline.MovingTextUtils;
import com.color.home.widgets.singleline.localscroll.SLTextSurfaceView;
import com.color.home.widgets.singleline.localscroll.TextRenderer;

/**
 * Created by Administrator on 2017/1/11.
 */
public class ItemWeatherSLScrollView extends SLTextSurfaceView implements NetworkObserver{


    private NetworkConnectReceiver mNetworkConnectReceiver;
    private WeatherSLScrollObject mWeatherSLScrollObject;

    public ItemWeatherSLScrollView(Context context, ProgramParser.Item item) {
        super(context);

        mItem = item;
        this.mTextobj = getTextObject(context);

        setEGLContextClientVersion(2);

    }

    @Override
    public void setItem(RegionView regionView, ProgramParser.Item item) {
        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        mRenderer = new TextRenderer(mTextobj);
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
//        mTextobj.setPixelPerFrame(Math.max(1, Math.round(pixelPerFrame)));
        mTextobj.setPixelPerFrame(pixelPerFrame);

        long playLength = 300000;
        try {
            playLength = Long.parseLong(item.duration);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (DBG)
            Log.d(TAG, "setItem. [playLength=" + playLength);
        removeCallbacks(this);
        postDelayed(this, playLength);

        if (ItemWeatherMLPagesView.isNeedShowWeather(item)) {
            mNetworkConnectReceiver = new NetworkConnectReceiver(this);
        }
    }

    private WeatherSLScrollObject getTextObject(Context context) {

        mWeatherSLScrollObject = new WeatherSLScrollObject(context, mItem);
        return mWeatherSLScrollObject;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mNetworkConnectReceiver != null)
            ItemWeatherMLPagesView.registerNetworkConnectReceiver(mContext, mNetworkConnectReceiver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mWeatherSLScrollObject != null)
            mWeatherSLScrollObject.removeWeatherRunnable();

        if (mNetworkConnectReceiver != null)
            ItemWeatherMLPagesView.unRegisterNetworkConnectReceiver(mContext, mNetworkConnectReceiver);
    }

    @Override
    public void reloadContent() {
        if (DBG)
            Log.d(TAG, "reloadContent. mWeatherSLScrollObject= " + mWeatherSLScrollObject);
            if (mWeatherSLScrollObject != null)
                mWeatherSLScrollObject.reload();
    }
}
