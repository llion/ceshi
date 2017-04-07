package com.color.home.widgets.weather;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;

import com.color.home.ProgramParser;
import com.color.home.ProgramParser.Item;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.widgets.RegionView;
import com.color.home.widgets.multilines.ItemMLScrollMultipic2View;
import com.color.home.widgets.multilines.MultiPicScrollObject;
import com.color.home.widgets.multilines.MultiPicScrollRenderer;
import com.color.home.widgets.singleline.MovingTextUtils;

public class ItemWeatherMLScrollView extends ItemMLScrollMultipic2View implements NetworkObserver {
    private final static String TAG = "ItemWeatherMLScrollView";
    private static final boolean DBG = false;

    private WeatherMLScrollObject mWeatherMLScrollObject;
    private NetworkConnectReceiver mNetworkConnectReceiver;

    public ItemWeatherMLScrollView(Context context) {
        super(context);

    }

    @Override
    public void setItem(RegionView regionView, Item item) {
        mTheTextObj = getTextObj(item);

        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        mRenderer = new MultiPicScrollRenderer(mTheTextObj);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // getHolder().setFormat(PixelFormat.RGBA_8888);

        mListener = regionView;
        this.mItem = item;

        initDisplay(item);

        float pixelPerFrame = MovingTextUtils.getPixelPerFrame(item);
//        mTheTextObj.setPixelPerFrame(Math.max(1, Math.round(pixelPerFrame)));
        mTheTextObj.setPixelPerFrame(pixelPerFrame);

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

    @Override
    protected MultiPicScrollObject getTextObj(Item item) {

        mWeatherMLScrollObject = new WeatherMLScrollObject(getContext(), item);
        return mWeatherMLScrollObject;
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

        if (mWeatherMLScrollObject != null)
            mWeatherMLScrollObject.removeWeatherRunnable();

        if (mNetworkConnectReceiver != null)
            ItemWeatherMLPagesView.unRegisterNetworkConnectReceiver(mContext, mNetworkConnectReceiver);
    }

    @Override
    public void reloadContent() {
        if (DBG)
            Log.d(TAG, "reloadContent. mWeatherMLScrollObject= " + mWeatherMLScrollObject);
        if (mWeatherMLScrollObject != null)
            mWeatherMLScrollObject.reload();
    }
}
