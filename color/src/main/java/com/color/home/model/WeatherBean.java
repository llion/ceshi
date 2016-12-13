package com.color.home.model;


public class WeatherBean {
    private String location;//区域
    private String areaCode;//区域代码
    private String weather; //天气状况

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    private String temperature;  //温度
    private String humidity;  //湿度
    private String wind;    //风力
    private String winddirection; //风向
    private String cold; //穿衣指数
    private String lastuptime; //更新时间

    public String getCold() {
        return cold;
    }

    public void setCold(String cold) {
        this.cold = cold;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLastuptime() {
        return lastuptime;
    }

    public void setLastuptime(String lastuptime) {
        this.lastuptime = lastuptime;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getWind() {
        return wind;
    }

    public void setWind(String wind) {
        this.wind = wind;
    }

    public String getWinddirection() {
        return winddirection;
    }

    public void setWinddirection(String winddirection) {
        this.winddirection = winddirection;
    }
}


