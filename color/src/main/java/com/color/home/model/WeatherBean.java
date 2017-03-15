package com.color.home.model;


public class WeatherBean {
    private int errorCode;
    private String reason;
    private String info;//天气情况:多云...
    private String temperature;  //温度
    private String humidity;  //湿度
    private String wind;    //风力
    private String winddirection; //风向
    private String quality;//空气质量等级
    private String curPm;//当前空气指数
    private String pm25;//PM2.5
    private String chuanyi;//穿衣指数
    private String lastuptime; //更新时间

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getPm25() {
        return pm25;
    }

    public void setPm25(String pm25) {
        this.pm25 = pm25;
    }

    public String getCurPm() {
        return curPm;
    }

    public void setCurPm(String curPm) {
        this.curPm = curPm;
    }

    public String getChuanyi() {
        return chuanyi;
    }

    public void setChuanyi(String chuanyi) {
        this.chuanyi = chuanyi;
    }

    public String getLastuptime() {
        return lastuptime;
    }

    public void setLastuptime(String lastuptime) {
        this.lastuptime = lastuptime;
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


