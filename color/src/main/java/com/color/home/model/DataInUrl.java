package com.color.home.model;

/**
 * Created by Administrator on 2017/4/1.
 */

public class DataInUrl {

    String url;
    String data;

    public DataInUrl(String url, String data) {
        this.url = url;
        this.data = data;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
