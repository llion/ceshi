package com.color.home.widgets.weather;

import android.content.Context;
import android.util.Log;

import com.color.home.ProgramParser;
import com.color.home.ProgramParser.Item;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.widgets.RegionView;
import com.color.home.widgets.multilines.ItemMLScrollMultipic2View;
import com.color.home.widgets.multilines.MultiPicScrollObject;

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

    @Override
    protected MultiPicScrollObject getTextObj(Item item) {

        mWeatherMLScrollObject = new WeatherMLScrollObject(getContext(), item);
        return mWeatherMLScrollObject;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mWeatherMLScrollObject != null)
            mWeatherMLScrollObject.removeWeatherRunnable();

        if (mNetworkConnectReceiver != null)
            mContext.unregisterReceiver(mNetworkConnectReceiver);
    }

    @Override
    public void reloadContent() {
        if (DBG)
            Log.d(TAG, "reloadContent. mWeatherMLScrollObject= " + mWeatherMLScrollObject);
        if (mWeatherMLScrollObject != null)
            mWeatherMLScrollObject.reload();
    }
}
