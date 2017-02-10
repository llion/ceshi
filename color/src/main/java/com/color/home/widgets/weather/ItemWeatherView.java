package com.color.home.widgets.weather;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.Region;
import com.color.home.model.WeatherBean;
import com.color.home.utils.SmartWeatherUrlUtil;
import com.color.home.utils.WeatherInquirer;
import com.color.home.utils.WeatherJsonUtil;
import com.color.home.widgets.ItemData;
import com.color.home.widgets.RegionView;

import java.net.MalformedURLException;
import java.net.URL;

public class ItemWeatherView extends LinearLayout implements ItemData {
    private final static String TAG = "ItemWeatherView";
    private static final boolean DBG = false;

    private Item mItem;
    private Region mRegion;

    public ItemWeatherView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ItemWeatherView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemWeatherView(Context context) {
        super(context);
    }

    @Override
    public void setRegion(Region region) {
        mRegion = region;
    }

    @Override
    public void setItem(RegionView regionView, Item item) {
        mItem = item;
        mAreaId = item.regioncode;

        // createWeatherLayout(getContext(), this);
    }

    private Context mContext;

    private String mAreaId;
    private AliasTextView mTextView;
    private AliasTextView mTextLocation;
    private AliasTextView mTextWeather;
    private AliasTextView mTextTemperature;
    private AliasTextView mTextHumidity;
    private AliasTextView mTextWind;
    private AliasTextView mTextColdIndex;
    private AliasTextView mTextLastUpTime;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String progress = msg.getData().getString("progress");
            if ("100".equals(progress)) {
                String[] weatherInfo = msg.getData().getStringArray("weatherJson");
                if (weatherInfo != null) {
                    if (DBG) {
                        for (int i = 0; i < weatherInfo.length; i++) {
                            Log.i(TAG, "---onPostExecute().result[" + i + "]: " + weatherInfo[i]);
                        }
                    }
                    WeatherBean weatherBean = WeatherJsonUtil.WeatherJson(weatherInfo);
                    mTextView.setVisibility(GONE);

                    mTextLocation = new AliasTextView(getContext());
                    addView(mTextLocation);
                    mTextLocation.setText(mItem.regionname);

                    if("1".equals(mItem.isshowweather)) {
                        mTextWeather = new AliasTextView(getContext());
                        addView(mTextWeather);
                        mTextWeather.setText(mItem.weatherprefix + weatherBean.getWeather() + "");
                    }

                    if("1".equals(mItem.isshowtemperature)) {
                        mTextTemperature = new AliasTextView(getContext());
                        addView(mTextTemperature);
                        mTextTemperature.setText(mItem.temperatureprefix + weatherBean.getTemperature() + "");
                    }

                    if("1".equals(mItem.isshowhumidity)) {
                        mTextHumidity = new AliasTextView(getContext());
                        addView(mTextHumidity);
                        mTextHumidity.setText(mItem.humidity + weatherBean.getHumidity() + "");
                    }

                    if("1".equals(mItem.isshowwind)) {
                        mTextWind = new AliasTextView(getContext());
                        addView(mTextWind);
                        mTextWind.setText(mItem.windprefix + weatherBean.getWinddirection() + ", " + weatherBean.getWind());
                    }

                    if("1".equals(mItem.isshowcoldindex)) {
                        mTextColdIndex = new AliasTextView(getContext());
                        addView(mTextColdIndex);
                        mTextColdIndex.setText(mItem.coldindex + weatherBean.getCold() + "");
                    }

                    mTextLastUpTime = new AliasTextView(getContext());
                    addView(mTextLastUpTime);
                    mTextLastUpTime.setText("Last up time : " + weatherBean.getLastuptime() + "");

//                        mTextView.setText("done.");
                }

            } else if ("-1".equals(progress)){
                mTextView.setText("Weather retrieving timeout ...");
            }
        }
    };


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setOrientation(VERTICAL);

        mTextView = new AliasTextView(getContext());
        addView(mTextView);

        mTextView.setText("Querying ...");
        mTextView.setVisibility(VISIBLE);


//        retrieveWeather(mAreaId);
        WeatherTask mWeatherTask = new WeatherTask(mAreaId);
        mWeatherTask.start();
    }

    class AliasTextView extends TextView{

        public AliasTextView(Context context) {
            this(context, null);
            this.getPaint().setAntiAlias(false);
        }

        public AliasTextView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public AliasTextView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void retrieveWeather(String areaId){
        if(DBG)
            Log.d(TAG, "retrieveWeather.. Thread=" + Thread.currentThread().getName());
        String[] type = {"observe", "forecast3d", "index"};
//        private String areaId;  //区域ID
        String[] weatherJson = new String[3];

        WeatherInquirer[] mWeatherInquirers = new WeatherInquirer[3];
        boolean isFinished = false;

        Message msg = Message.obtain();
        try {
            URL[] weatherUrl = new URL[3];
            for (int i = 0; i < 3; i++) {
                weatherUrl[i] = new URL(SmartWeatherUrlUtil.getInterfaceURL(areaId, type[i]));
                mWeatherInquirers[i] = new WeatherInquirer(weatherUrl[i], type[i]);
                mWeatherInquirers[i].retrieve();
            }

            long current = System.currentTimeMillis();
            while (!isFinished) {
                if(System.currentTimeMillis() - current > 10000) {
                    Log.e(TAG, "", new Exception("Time out on retrieving weather."));
                    msg.getData().putString("progress", "-1");
                    mHandler.sendMessage(msg);
                    return;
                }
                isFinished = true;
                for (int j = 0; j < 3; j++) {
                    weatherJson[j] = "";
                    if (!mWeatherInquirers[j].isCompleted()) {
                        isFinished = false;
                    }
                    Thread.sleep(20);
                }
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (isFinished) {
            for (int i = 0; i < 3; i++) {
                weatherJson[i] = mWeatherInquirers[i].weatherJson();
                if (DBG) {
                    Log.i(TAG, "---WeatherTask.weatherJson[" + i + "]: " + weatherJson[i]);
                }
            }
            msg.getData().putStringArray("weatherJson", weatherJson);
            msg.getData().putString("progress", "100");
        }
        mHandler.sendMessage(msg);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class WeatherTask extends Thread {
        private String[] type = {"observe", "forecast3d", "index"};
        private String mAreaId;  //区域ID
        private String[] weatherJson = new String[3];

        public WeatherTask(String areaId) {
            this.mAreaId = areaId;
        }

        @Override
        public void run() {
            WeatherInquirer[] mWeatherInquirers = new WeatherInquirer[3];
            boolean isFinished = false;

            Message msg = Message.obtain();
            try {
                URL[] weatherUrl = new URL[3];
                for (int i = 0; i < 3; i++) {
                    weatherUrl[i] = new URL(SmartWeatherUrlUtil.getInterfaceURL(mAreaId, type[i]));
                    mWeatherInquirers[i] = new WeatherInquirer(weatherUrl[i], type[i]);
                    mWeatherInquirers[i].retrieve();
                }

                long current = System.currentTimeMillis();
                while (!isFinished) {
                    if(System.currentTimeMillis() - current > 10000) {
                        Log.e(TAG, "", new Exception("Time out on retrieving weather."));
                        msg.getData().putString("progress", "-1");
                        mHandler.sendMessage(msg);
                        return;
                    }
                    isFinished = true;
                    for (int j = 0; j < 3; j++) {
                        weatherJson[j] = "";
                        if (!mWeatherInquirers[j].isCompleted()) {
                            isFinished = false;
                        }
                        Thread.sleep(20);
                    }
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (isFinished) {
                for (int i = 0; i < 3; i++) {
                    weatherJson[i] = mWeatherInquirers[i].weatherJson();
                    if (DBG) {
                        Log.i(TAG, "---WeatherTask.weatherJson[" + i + "]: " + weatherJson[i]);
                    }
                }
                msg.getData().putStringArray("weatherJson", weatherJson);
                msg.getData().putString("progress", "100");
            }
            mHandler.sendMessage(msg);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
