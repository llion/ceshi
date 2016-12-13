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
import com.color.home.R;
import com.color.home.model.WeatherBean;
import com.color.home.utils.SmartWeatherUrlUtil;
import com.color.home.utils.WeatherJsonUtil;
import com.color.home.utils.WeatherServerUtil;
import com.color.home.widgets.ItemData;
import com.color.home.widgets.RegionView;

import java.net.MalformedURLException;
import java.net.URL;

public class ItemWeatherView extends LinearLayout implements ItemData {
    private final static String TAG = "ItemWeatherView";
    private static final boolean DBG = true;

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
    private TextView mTextView;
    private TextView mTextLocation;
    private TextView mTextWeather;
    private TextView mTextTemperature;
    private TextView mTextHumidity;
    private TextView mTextWind;
    private TextView mTextColdIndex;
    private TextView mTextLastUpTime;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String mProgress = msg.getData().getString("progress");
            if ("100".equals(mProgress)) {
                String[] weatherInfo = msg.getData().getStringArray("weatherJson");
                if (weatherInfo != null) {
                    if (DBG) {
                        for (int i = 0; i < weatherInfo.length; i++) {
                            Log.i(TAG, "---onPostExecute().result[" + i + "]: " + weatherInfo[i]);
                        }
                    }
                    WeatherBean weatherBean = WeatherJsonUtil.WeatherJson(weatherInfo);

                    mTextLocation = new TextView(getContext());
                    addView(mTextLocation);

                    mTextWeather = new TextView(getContext());
                    addView(mTextWeather);

                    if("1".equals(mItem.isshowtemperature)) {
                        mTextTemperature = new TextView(getContext());
                        addView(mTextTemperature);
                    }

                    if("1".equals(mItem.isshowhumidity)) {
                        mTextHumidity = new TextView(getContext());
                        addView(mTextHumidity);
                    }

                    if("1".equals(mItem.isshowwind)) {
                        mTextWind = new TextView(getContext());
                        addView(mTextWind);
                    }

                    if("1".equals(mItem.isshowcoldindex)) {
                        mTextColdIndex = new TextView(getContext());
                        addView(mTextColdIndex);
                    }

                    mTextLastUpTime = new TextView(getContext());
                    addView(mTextLastUpTime);

//                        mTextView.setText("done.");
                    mTextView.setVisibility(GONE);
                    mTextLocation.setText(mItem.regionname + weatherBean.getLocation() + "");
                    mTextWeather.setText(mItem.weatherprefix + weatherBean.getWeather() + "");

                    mTextTemperature.setText(mItem.temperatureprefix + weatherBean.getTemperature() + "");

                    mTextHumidity.setText(mItem.humidity + weatherBean.getHumidity() + "");

                    mTextWind.setText(mItem.windprefix + weatherBean.getWinddirection() + ", " + weatherBean.getWind());

                    mTextColdIndex.setText(mItem.coldindex + weatherBean.getCold() + "");

                    mTextLastUpTime.setText("Last up time : " + weatherBean.getLastuptime() + "");

                }

//                    TextView mText1 = new TextView(mContext);
//                    String[] weatherInfo = msg.getData().getStringArray("weatherJson");
//                    if(DBG){
//                        Log.i(TAG,"---handleMessage.weatherInfo.length: " + weatherInfo.length );
//                    }
//                    String weather = "" ;
//                    for(int i = 0; i < 3; i++ ){
//                        if(DBG){
//                                Log.i(TAG,"---handleMessage.weatherinfo[" + i + "] :" + weatherInfo[i]);
//                            }
//                        weather = weather + weatherInfo[i];
//                    }
//                    mText1.setText("WeatherInfo : " +weather);
//                    addView(mText1);
            } else {
                TextView mText2 = new TextView(mContext);
                mText2.setText("正在获取天气...");
                addView(mText2);
            }
        }
    };


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setOrientation(VERTICAL);

        mTextView = new TextView(getContext());
        addView(mTextView);

        mTextView.setText("获取天气...");
        mTextView.setVisibility(VISIBLE);


        WeatherTask mWeatherTask = new WeatherTask(mAreaId);
        mWeatherTask.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    class WeatherTask extends Thread {
        private String[] type = {"observe", "forecast3d", "index"};
        private String areaId;  //区域ID
        private String[] weatherJson = new String[3];

        public WeatherTask(String areaId) {
            this.areaId = areaId;
        }

        @Override
        public void run() {
            WeatherServerUtil[] mWeatherServerUtils = new WeatherServerUtil[3];
            boolean isFinished = false;
            try {
                URL[] weatherUrl = new URL[3];
                for (int i = 0; i < 3; i++) {
                    weatherUrl[i] = new URL(SmartWeatherUrlUtil.getInterfaceURL(areaId, type[i]));
                    mWeatherServerUtils[i] = new WeatherServerUtil(weatherUrl[i], type[i]);
                    mWeatherServerUtils[i].start();
                }

                while (!isFinished) {
                    isFinished = true;
                    for (int j = 0; j < 3; j++) {
                        weatherJson[j] = "";
                        if (!mWeatherServerUtils[j].isCompleted()) {
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
            Message msg = new Message();
            if (isFinished) {
                for (int i = 0; i < 3; i++) {
                    weatherJson[i] = mWeatherServerUtils[i].weatherJson();
                    if (DBG) {
                        Log.i(TAG, "---WeatherTask.weatherJson[" + i + "]: " + weatherJson[i]);
                    }
                }
                msg.getData().putStringArray("weatherJson", weatherJson);
                msg.getData().putString("progress", "100");
            } else {
                msg.getData().putString("progress", "50");
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
