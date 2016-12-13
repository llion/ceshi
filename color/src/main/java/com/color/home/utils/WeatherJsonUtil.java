package com.color.home.utils;

import android.util.Log;


import com.color.home.model.WeatherBean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WeatherJsonUtil {
    private static final String TAG = "WeatherJsonUtil";
    private static final boolean DBG = true;

    public static WeatherBean WeatherJson(String[] mJsonText){
        WeatherBean mWeatherBean = new WeatherBean();
        try {
//        3种天气数据的JsonText
            JSONObject currentJson = new JSONObject(mJsonText[0]).getJSONObject("l");
            JSONObject locationJson = new JSONObject(mJsonText[1]).getJSONObject("c");
            JSONObject fore3dJson = new JSONObject(mJsonText[1]).getJSONObject("f");
            JSONObject f1_1Json = fore3dJson.getJSONArray("f1").getJSONObject(0);
            JSONArray indexJson = new JSONObject(mJsonText[2]).getJSONArray("i");
            if(DBG){
                Log.i(TAG,"---WeatherJson.currentJson: " + currentJson);
            }
//            详细的天气数据
            mWeatherBean.setTemperature(currentJson.getString("l1") + "℃");
            mWeatherBean.setHumidity(currentJson.getString("l2") + "%");
            mWeatherBean.setWind(currentJson.getString("l3") + "级");
            mWeatherBean.setWinddirection(resolveWindDirection(currentJson.getString("l4")));
            mWeatherBean.setLastuptime(currentJson.getString("l7"));
            if(DBG){
                Log.i(TAG,"---WeatherJson.Temperature: "+currentJson.getString("l1"));
            }
            mWeatherBean.setLocation(locationJson.getString("c3"));
            mWeatherBean.setAreaCode(locationJson.getString("c11"));
//            晚上查询不到当天白天的天气情况
            String weather = f1_1Json.getString("fa");
            if(weather.equals("")  || weather.equals(null)){
                weather = f1_1Json.getString("fb");
            }
            mWeatherBean.setWeather(resolveWeather(weather));

            mWeatherBean.setCold(indexJson.getJSONObject(0).getString("i4"));
        } catch (JSONException e) {
//            捕捉服务器空相应的异常(一般是由于API试用次数达到限制次数时出现的错误)
            if(DBG){
                Log.i(TAG,"---WeatherJson :Catch the sever responding null Exception !");
                Log.i(TAG,e.toString());
            }
            return mWeatherBean;
        }
        return mWeatherBean;
    }
    //        天气现象描述解析
    public static String resolveWeather(String weather){
        String mWeather = "";
        int temp = Integer.parseInt(weather);
        switch(temp){
            case 0: mWeather = "晴";
                break;
            case 1: mWeather = "多云";
                break;
            case 2: mWeather = "阴";
                break;
            case 3: mWeather = "阵雨";
                break;
            case 4: mWeather = "雷阵雨";
                break;
            case 5: mWeather = "雷阵雨伴有冰雹";
                break;
            case 6: mWeather = "雨夹雪";
                break;
            case 7: mWeather = "小雨";
                break;
            case 8: mWeather = "中雨";
                break;
            case 9: mWeather = "大雨";
                break;
            case 10: mWeather = "暴雨";
                break;
            case 11: mWeather = "大暴雨";
                break;
            case 12: mWeather = "特大暴雨";
                break;
            case 13: mWeather = "阵雪";
                break;
            case 14: mWeather = "小雪";
                break;
            case 15: mWeather = "中雪";
                break;
            case 16: mWeather = "大雪";
                break;
            case 17: mWeather = "暴雪";
                break;
            case 18: mWeather = "雾";
                break;
            case 19: mWeather = "冻雨";
                break;
            case 20: mWeather = "沙尘暴";
                break;
            case 21: mWeather = "小到中雨";
                break;
            case 22: mWeather = "中到大雨";
                break;
            case 23: mWeather = "大到暴雨";
                break;
            case 24: mWeather = "暴雨到大暴雨";
                break;
            case 25: mWeather = "大暴雨到特大暴雨";
                break;
            case 26: mWeather = "小到中雪";
                break;
            case 27: mWeather = "中到大雪";
                break;
            case 28: mWeather = "大到暴雪";
                break;
            case 29: mWeather = "浮沉";
                break;
            case 30: mWeather = "扬沙";
                break;
            case 31: mWeather = "强沙尘暴";
                break;
            case 53: mWeather = "霾";
                break;
            case 99: mWeather = "未知天气";
                break;
            default: mWeather = "未知天气";
                break;
        }
        return mWeather;
    }

    //    风向解析
    public static String resolveWindDirection(String windDirection){
        String mWindDirection = "";
        int temp = Integer.parseInt(windDirection);
        switch(temp){
            case 0:mWindDirection = "无持续风向";
                break;
            case 1:mWindDirection = "东北风";
                break;
            case 2:mWindDirection = "东风";
                break;
            case 3:mWindDirection = "东南风";
                break;
            case 4:mWindDirection = "南风";
                break;
            case 5:mWindDirection = "西南风";
                break;
            case 6:mWindDirection = "西风";
                break;
            case 7:mWindDirection = "西北风";
                break;
            case 8:mWindDirection = "北风";
                break;
            case 9:mWindDirection = "旋转风";
                break;
            default : mWindDirection = "多变风向";
                break;
        }
        return mWindDirection;
    }




}
