package com.color.home.utils;

import android.util.Log;


import java.io.IOException;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class WeatherInquirer {
    private static final String TAG = "WeatherInquirer";
    private static final boolean DBG = true;

    private boolean isCompleted = false;
    private String weatherJson = "";
    private URL weatherUrl;

    private OkHttpClient mClient;

    public WeatherInquirer(URL weatherUrl, String threadName) {
        this.weatherUrl = weatherUrl;
        this.mClient = new OkHttpClient();

    }

    public void retrieve() {

        weatherJson = "";
        isCompleted = false;
        if (DBG) {
            Log.i(TAG, "weatherUrl : " + weatherUrl + ", Thread=" + Thread.currentThread().getName());
        }

        Request request = new Request.Builder()
                .url(weatherUrl)
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
                    weatherJson = response.body().string();
                    isCompleted = true;
                }
            }
        });

    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public String weatherJson() {
        return weatherJson;
    }
}
