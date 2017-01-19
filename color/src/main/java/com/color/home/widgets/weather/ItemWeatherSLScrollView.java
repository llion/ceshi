package com.color.home.widgets.weather;

import android.content.Context;
import android.util.Log;

import com.color.home.ProgramParser;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.widgets.RegionView;
import com.color.home.widgets.singleline.localscroll.SLTextSurfaceView;

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
        super.setItem(regionView, item);

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
            ItemWeatherMLPagesView.registerNetworkConnectReceiver(mContext, mNetworkConnectReceiver);
        }
    }

    private WeatherSLScrollObject getTextObject(Context context) {

        mWeatherSLScrollObject = new WeatherSLScrollObject(context, mItem);
        return mWeatherSLScrollObject;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mWeatherSLScrollObject != null)
            mWeatherSLScrollObject.removeWeatherRunnable();

        if (mNetworkConnectReceiver != null)
            mContext.unregisterReceiver(mNetworkConnectReceiver);
    }

    @Override
    public void reloadContent() {
        if (DBG)
            Log.d(TAG, "reloadContent. mWeatherSLScrollObject= " + mWeatherSLScrollObject);
            if (mWeatherSLScrollObject != null)
                mWeatherSLScrollObject.reload();
    }
}
