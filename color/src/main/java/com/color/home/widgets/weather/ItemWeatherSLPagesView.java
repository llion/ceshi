package com.color.home.widgets.weather;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;

import com.color.home.R;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.utils.WeatherInquirer;
import com.color.home.utils.WeatherJsonUtil;
import com.color.home.widgets.singleline.ItemSingleLineText;

/**
 * Created by Administrator on 2017/1/10.
 */
public class ItemWeatherSLPagesView extends ItemSingleLineText implements NetworkObserver {
    private static final boolean DBG = false;
    private final static String TAG = "ItemWeatherSLPagesView";

    private boolean mIsFirstQuery = true;
    WeatherInquirer mWeatherInquirer;
    private NetworkConnectReceiver mNetworkConnectReceiver;

    public ItemWeatherSLPagesView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ItemWeatherSLPagesView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemWeatherSLPagesView(Context context) {
        super(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        long duration = 6000;
        try {
            duration = Long.parseLong(mItem.duration);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        postDelayed(mKeepRunnable, duration);
    }

    Runnable mKeepRunnable = new Runnable() {
        @Override
        public void run() {
            tellListener();
        }
    };


    @Override
    public void resetTextFromStart() {
        if (ItemWeatherMLPagesView.isNeedShowWeather(mItem)) {

            //register broadcast
            mNetworkConnectReceiver = new NetworkConnectReceiver(this);
            ItemWeatherMLPagesView.registerNetworkConnectReceiver(mContext, mNetworkConnectReceiver);

            setGravity(Gravity.CENTER);
            setSingleLine();

            mDuration = 2000;
            try {
                mDuration = Long.parseLong(mItem.remainTime) * 100;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            //
            mText = mContext.getString(R.string.queryingWeather);
            mSplitTexts = splitText(mText, mRegionView.getRegionWidth());

            mIndex = 0;
            removeCallbacks(ItemWeatherSLPagesView.this);
            post(ItemWeatherSLPagesView.this);

            //
            mWeatherInquirer = new WeatherInquirer(mItem.regioncode);
            removeCallbacks(mWeatherRunnable);
            post(mWeatherRunnable);

        } else if (DBG)
            Log.d(TAG, " nothing to display.");
    }

    Runnable mWeatherRunnable = new Runnable() {
        @Override
        public void run() {

            new NetTask().execute("");

            postDelayed(this, 3 * 60 * 60 * 1000);//update data
        }
    };

    @Override
    public void reloadContent() {
        if (DBG)
            Log.d(TAG, "reloadContent.  [mWeatherRunnable= " + mWeatherRunnable);
        if (mWeatherRunnable != null) {
            removeCallbacks(mWeatherRunnable);
            post(mWeatherRunnable);
        }
    }


    public class NetTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            long requestTime = System.currentTimeMillis();
            if (DBG)
                Log.d(TAG, "begin to retrive weather, requestTime= " + requestTime);
            mWeatherInquirer.retrieve();

            while (true) {
                if (System.currentTimeMillis() - requestTime > 10000) {
                    Log.d(TAG, "" + new Exception("Time out on retrieving weather."));
                    break;

                } else {
                    if (mWeatherInquirer.ismIsCompleted())
                        break;
                    else {
                        if (DBG)
                            Log.d(TAG, "retrive is not end.");

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }


            if (mWeatherInquirer.ismIsCompleted()) {
                if (DBG)
                    Log.d(TAG, "retrive successful.");
                return ItemWeatherMLPagesView.getWeatherText(mItem, WeatherJsonUtil.getWeatherBean(mWeatherInquirer.getWeatherJson()));
            } else {
                if (DBG)
                    Log.d(TAG, "retrive failed. mText= " + mText);
                if (mIsFirstQuery)
                    return mContext.getString(R.string.failedToGetWeather);
                else {
                    Log.d(TAG, "Failed to retrieve weather, do not update.");
                    return null;
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (DBG)
                Log.d(TAG, "mText= " + mText + ", result= " + result
                        + ", mRegionView.getRegionWidth()= " + mRegionView.getRegionWidth());

            if (mIsFirstQuery)
                mIsFirstQuery = false;

            if (result != null && !mText.equals(result)) {
                mText = result;
                mSplitTexts = splitText(result, mRegionView.getRegionWidth());

                mIndex = 0;
                removeCallbacks(ItemWeatherSLPagesView.this);
                post(ItemWeatherSLPagesView.this);

            } else if (result != null)
                Log.d(TAG, "the weather information has not changed.");

        }

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mKeepRunnable != null)
            removeCallbacks(mKeepRunnable);

        if (mWeatherRunnable != null)
            removeCallbacks(mWeatherRunnable);

        if (mNetworkConnectReceiver != null)
            mContext.unregisterReceiver(mNetworkConnectReceiver);
    }
}
