package com.color.home.utils;

import android.util.Log;


import com.color.home.model.WeatherBean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Date;
import java.text.SimpleDateFormat;

public class WeatherJsonUtil {
    private static final String TAG = "WeatherJsonUtil";
    private static final boolean DBG = false;


    public static WeatherBean getWeatherBean(String weatherJson) {
       WeatherBean weatherBean = new WeatherBean();

        try {
            JSONObject weatherObject = new JSONObject(weatherJson);
            int errorCode = weatherObject.getInt("error_code");
            String reason = weatherObject.getString("reason");
            JSONObject result = weatherObject.getJSONObject("result");

            weatherBean.setErrorCode(errorCode);
            weatherBean.setReason(reason);
            if (0 == errorCode && result != null){
                JSONObject data = result.getJSONObject("data");
                JSONObject realtime = data.getJSONObject("realtime");
                JSONObject weather = realtime.getJSONObject("weather");
                JSONObject wind = realtime.getJSONObject("wind");
                JSONObject pm25 = data.getJSONObject("pm25");

                String info = weather.getString("info");
                String temperature = weather.getString("temperature")  + "℃";
                String humidity = weather.getString("humidity")  + "%";
                String power = wind.getString("power");
                String direct = wind.getString("direct");
                String curPm = pm25.getString("curPm");
                String airPm25 = pm25.getString("pm25");
                String airQuality = pm25.getString("quality");

//                Date updateTime = new Date(Long.parseLong(data.getString("dataUptime")));
//                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
//                String dataUptime = data.getString("date") + " " + sdf.format(updateTime);
                JSONArray chuanyiArray = data.getJSONObject("life").getJSONObject("info").getJSONArray("chuanyi");

                if (DBG)
                    Log.d(TAG, "info= " + info + ", " + temperature + ", " + humidity + ", " + power + ", " + direct
                     + ", power= " + power);

                weatherBean.setInfo(info);
                weatherBean.setHumidity(humidity);
                weatherBean.setTemperature(temperature);
                weatherBean.setWind(power);
                weatherBean.setWinddirection(direct);
                weatherBean.setCurPm(curPm);
                weatherBean.setQuality(airQuality);
                weatherBean.setPm25(airPm25);
//                weatherBean.setLastuptime(dataUptime);

                String chuanyi = "";
                if (chuanyiArray != null) {
                    for (int i = 0; i <chuanyiArray.length(); i++){
                        chuanyi += chuanyiArray.getString(i) + ((i == (chuanyiArray.length() - 1))? "" : ",");
                    }
                }
                weatherBean.setChuanyi(chuanyi);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return weatherBean;
    }
}
