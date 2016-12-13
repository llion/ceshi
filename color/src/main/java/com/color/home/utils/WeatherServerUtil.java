package com.color.home.utils;

import android.util.Log;

import com.squareup.okhttp.OkHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class WeatherServerUtil extends Thread {
    private static final String TAG = "WeatherServerUtil";
    private static final boolean DBG = true;

//    是否获取到天气
    private boolean isCompleted = false;
//    天气数据
    private String weatherJson = "";
//    获取天气的URL
    private URL weatherUrl;
//    当前线程的名字
    private String threadName;

    public WeatherServerUtil(URL weatherUrl , String threadName) {
        this.weatherUrl = weatherUrl;
        this.threadName = threadName;
        this.setName( threadName );
    }


    @Override
    public void run() {
        InputStream is = null;
        BufferedReader bf = null;
//        每次获取天气数据之前进行一次初始化
        weatherJson = "";
        isCompleted = false;
        try {
            if(DBG){
                Log.i(TAG,"---threadName: " + threadName + " , weatherUrl : "+weatherUrl);
            }
            HttpURLConnection conn = (HttpURLConnection)weatherUrl.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            if(conn != null){
                is = conn.getInputStream();
                bf = new BufferedReader(new InputStreamReader(is));
                String line = "";
                while((line = bf.readLine()) != null){
                    weatherJson += line;
                }
                isCompleted = true;
                if(DBG){
                    Log.i(TAG,"---threadName: " + threadName + " , weatherJson : "+weatherJson);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bf.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public boolean isCompleted(){
        return isCompleted;
    }
    public String weatherJson(){
        return weatherJson;
    }
}
