package com.color.home.widgets.weather;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import com.color.home.ProgramParser.Item;
import com.color.home.model.WeatherBean;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.utils.WeatherInquirer;
import com.color.home.utils.WeatherJsonUtil;
import com.color.home.widgets.MultilinePageSplitter;
import com.color.home.widgets.multilines.ItemMultiLinesPagedText;

/**
 * Created by Administrator on 2017/1/10.
 */
public class ItemWeatherMLPagesView extends ItemMultiLinesPagedText implements NetworkObserver {
    private static final boolean DBG = false;
    private final static String TAG = "ItemWeatherMLPagesView";

    WeatherInquirer mWeatherInquirer;
    private NetworkConnectReceiver mNetworkConnectReceiver;

    public ItemWeatherMLPagesView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ItemWeatherMLPagesView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemWeatherMLPagesView(Context context) {
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

        postDelayed(this, duration);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mWeatherRunnable);
        if (mNetworkConnectReceiver != null)
            mContext.unregisterReceiver(mNetworkConnectReceiver);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        if (DBG)
            Log.d(TAG, "onLayout. mIsFirst= " + mIsFirst);
        if (mIsFirst && isNeedShowWeather(mItem)) {


            mNetworkConnectReceiver = new NetworkConnectReceiver(this);
            registerNetworkConnectReceiver(mContext, mNetworkConnectReceiver);

            try {
                mOnePicDuration = Long.parseLong(mItem.remainTime) * 100;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            mWeatherInquirer = new WeatherInquirer(mItem.regioncode);
            post(mWeatherRunnable);

            mIsFirst = false;
        } else if (mIsFirst) {
            if (DBG)
                Log.d(TAG, " nothing to display.");
        }

    }

    public static boolean isNeedShowWeather(Item item) {

        if (isShow(item.isshowweather) || isShow(item.isshowtemperature) || isShow(item.isshowair)
                || isShow(item.isshowhumidity) || isShow(item.isshowwind) || isShow(item.isshowcoldindex))
            return true;

        return false;
    }

    Runnable mWeatherRunnable = new Runnable() {
        @Override
        public void run() {

            Log.d(TAG, "querying weather...");
            new NetTask().execute("");

            postDelayed(this, 3 * 60 * 60 * 1000);//update data
        }
    };

    public static boolean isShow(String isshow) {
        if ("1".equals(isshow))
            return true;
        return false;
    }

    public static void registerNetworkConnectReceiver(Context context, NetworkConnectReceiver networkConnectReceiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(networkConnectReceiver, filter);
    }

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
                Log.d(TAG, "begin to retrive weather, requestTime= " + requestTime + ", Thread= " + Thread.currentThread());
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
                return getWeatherText(mItem, WeatherJsonUtil.getWeatherBean(mWeatherInquirer.getWeatherJson()));

            } else {
                if (DBG)
                    Log.d(TAG, "retrive failed. mText= " + mText);
                if (TextUtils.isEmpty(mText))
                    return "Failed to retrieve weather.";
                else {
                    Log.d(TAG, "Failed to retrieve weather, do not update.");
                    return null;
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (DBG)
                Log.d(TAG, "result= " + result + ", mText= " + mText);

            if (result != null && !result.equals(mText)) {
                mText = result;
                composeAandShow();
            } else if (result != null)
                Log.d(TAG, "the weather information has not changed.");

        }


    }

    public static String getWeatherText(Item item, WeatherBean weatherBean) {

        String text = "";

        if ("1".equals(weatherBean.getErrorCode())) {

            text = "error.\nreason: " + weatherBean.getReason();
        } else {
            if (isShow(item.isshowweather))
                text += item.weatherprefix + weatherBean.getInfo() + "\n";

            if (isShow(item.isshowtemperature))
                text += item.temperatureprefix + weatherBean.getTemperature() + "\n";

            if (isShow(item.isshowwind))
                text += item.windprefix + weatherBean.getWinddirection() + weatherBean.getWind() + "\n";

            if (isShow(item.isshowhumidity))
                text += item.humidity + weatherBean.getHumidity() + "\n";

            if (isShow(item.isshowair))
                text += item.airprefix + weatherBean.getCurPm() + weatherBean.getQuality() + "  PM2.5: " + weatherBean.getPm25() + "\n";

            if (isShow(item.isshowcoldindex))
                text += item.coldindex + weatherBean.getChuanyi() + "\n";
        }

        if (DBG)
            Log.d(TAG, "text.length()= " + text.length() + ", " + (text + "\n").length());
        return text.substring(0, text.length() - 1);
    }

    private void composeAandShow() {
        setVisibility(INVISIBLE);
        int maxLineNumPerPage = this.getHeight() / this.getLineHeight() + (this.getHeight() % this.getLineHeight() >= (int) this.getPaint().getFontMetrics(null) ? 1 : 0);
        if (maxLineNumPerPage < 1)
            maxLineNumPerPage = 1;//最少显示1行

        if (DBG)
            Log.d(TAG, "composeAandShow. lineHeight= " + this.getLineHeight() + ", this.height= " + this.getHeight()
                    + ", getFontMetrics= " + this.getPaint().getFontMetrics(null)
                    + ", getLineSpacingExtra= " + getLineSpacingExtra()
                    + ", maxLineNumPerPage= " + maxLineNumPerPage
                    + ", mHandler= " + mHandler
                    + ", mOnePicDuration= " + mOnePicDuration);

        mPageSplitter = new MultilinePageSplitter(maxLineNumPerPage, this);
        mPageSplitter.append(mText);

        setVisibility(VISIBLE);
        mPageIndex = 0;
        setPageText();

        if (mPageSplitter.getPages().size() > 1) {

            if (mHandler != null) {
                mHandler.stop();
                mHandler = null;
            }
            mHandler = new MTextMarquee(this, mOnePicDuration);
            mHandler.start();
        }
    }

    @Override
    public void nextPage() {
        mPageIndex++;
        if (mPageIndex >= mPageSplitter.getPages().size()) {
            mPageIndex = 0;
        }

        if (DBG)
            Log.i(TAG, "next page. mPageIndex=" + mPageIndex);
        setPageText();
    }

}
