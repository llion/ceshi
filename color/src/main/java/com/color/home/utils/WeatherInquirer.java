package com.color.home.utils;

import android.os.SystemProperties;
import android.util.Log;


import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.color.home.network.IpUtils.bytesToHex;

public class WeatherInquirer {
    private static final String TAG = "WeatherInquirer";
    private static final boolean DBG = false;

    //// TODO: 2017/1/11 test
    private static final String CLT_WEATHER_URL = "http://backup.lednets.com:3000/api/weather/Query?";
//    private static final String CLT_WEATHER_URL = "http://192.168.1.88:3000/api/weather/Query?";
    private static final String PASSWORD = "CLTCLT180380580";

    private boolean mIsCompleted = false;
    private String mWeatherJson = "";
    private URL mWeatherUrl;

    private OkHttpClient mClient;

    public WeatherInquirer(String areaId) {
        try {
            String serialno = SystemProperties.get("ro.serialno");
            String versionCode = "1";
            if (DBG)
                Log.d(TAG, "url= " + CLT_WEATHER_URL + "S=" + serialno + "&V=" + versionCode
                                            + "&citycode=" + areaId
                                            + "&C=" + getMd5(serialno + versionCode + areaId + PASSWORD));

            this.mWeatherUrl = new URL(CLT_WEATHER_URL + "S=" + serialno + "&V=" + versionCode
                                            + "&citycode=" + areaId
                                            + "&C=" + getMd5(serialno + versionCode + areaId + PASSWORD));
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.mClient = new OkHttpClient();

    }

    private String getMd5(String str) {
        if (DBG)
            Log.d(TAG, "getMd5. [str= " + str);

        byte[] bytes = new byte[0];
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(str.getBytes("UTF-8"));
            bytes = messageDigest.digest();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytesToHex(bytes);
    }

    public void retrieve() {

        mWeatherJson = "";
        mIsCompleted = false;
        if (DBG) {
            Log.i(TAG, "retrieve. mWeatherUrl : " + mWeatherUrl + ", Thread=" + Thread.currentThread().getName());//AsynTask
        }

        Request request = new Request.Builder()
                .url(mWeatherUrl)
                .get()
                .build();
        Call call = mClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "", new Exception("Failed to retrieve weather."));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    if (DBG)
                        Log.e(TAG, "response successful" + ", Thread=" + Thread.currentThread().getName());//OKhttp
                    mWeatherJson = response.body().string();
                    mIsCompleted = true;
                }
            }
        });


    }

    public boolean ismIsCompleted() {
        return mIsCompleted;
    }

    public String getWeatherJson() {
        return mWeatherJson;
    }
}
