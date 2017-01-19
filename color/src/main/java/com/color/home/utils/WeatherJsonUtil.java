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
        String info = "", temperature = "", humidity = "", power = "", direct = "", curPm = "",
                airPm25 = "", airQuality = "", chuanyi = "";

        try {
            JSONObject weatherObject = new JSONObject(weatherJson);
            int errorCode = weatherObject.getInt("error_code");
            String reason = weatherObject.getString("reason");

            weatherBean.setErrorCode(errorCode);
            weatherBean.setReason(reason);

            JSONObject result = weatherObject.optJSONObject("result");
            if (0 == errorCode && result != null) {
                JSONObject data = result.optJSONObject("data");
                if (data != null) {

                    //realtime information: weather & wind
                    JSONObject realtime = data.optJSONObject("realtime");
                    if (realtime != null) {

                        JSONObject weather = realtime.optJSONObject("weather");
                        if (weather != null) {
                            info = weather.getString("info");
                            temperature = weather.getString("temperature") + "℃";
                            humidity = weather.getString("humidity") + "%";

                        }

                        JSONObject wind = realtime.optJSONObject("wind");
                        if (wind != null) {
                            power = wind.getString("power");
                            direct = wind.getString("direct");

                        }
                    }

                    //pm2.5
                    JSONObject pm25 = data.optJSONObject("pm25");
                    if (pm25 != null) {
                        curPm = pm25.getString("curPm");
                        airPm25 = pm25.getString("pm25");
                        airQuality = pm25.getString("quality");

                    }

                    JSONObject life = data.optJSONObject("life");
                    if (life != null) {
                        JSONObject infoObject = life.optJSONObject("info");
                        if (infoObject != null) {
                            JSONArray chuanyiArray = infoObject.optJSONArray("chuanyi");
                            if (chuanyiArray != null) {
                                if (DBG)
                                    Log.d(TAG, "chuanyiArray.length= " + chuanyiArray.length());
                                for (int i = 0; i < chuanyiArray.length(); i++) {
                                    chuanyi += chuanyiArray.getString(i) + ((i == (chuanyiArray.length() - 1)) ? "" : ",");
                                }
                                if (DBG)
                                    Log.d(TAG, "chuanyi= " + chuanyi);

                            }

                        }
                    }

//                Date updateTime = new Date(Long.parseLong(data.getString("dataUptime")));
//                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
//                String dataUptime = data.getString("date") + " " + sdf.format(updateTime);
//                weatherBean.setLastuptime(dataUptime);

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        weatherBean.setInfo(info);
        weatherBean.setHumidity(humidity);
        weatherBean.setTemperature(temperature);

        weatherBean.setWind(power);
        weatherBean.setWinddirection(direct);

        weatherBean.setCurPm(curPm);
        weatherBean.setQuality(airQuality);
        weatherBean.setPm25(airPm25);

        weatherBean.setChuanyi(chuanyi);
        return weatherBean;
    }
}
