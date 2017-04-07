package com.color.home.model;

import net.minidev.json.JSONArray;

/**
 * Created by Administrator on 2017/3/29.
 */

public class CltDataInfo {
    private String prefix;
    private String contentStr;
    private JSONArray contentArray;

    public String getContentStr() {
        return contentStr;
    }

    public void setContentStr(String contentStr) {
        this.contentStr = contentStr;
    }

    public JSONArray getContentArray() {
        return contentArray;
    }

    public void setContentArray(JSONArray jsonArray) {
        this.contentArray = jsonArray;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String toString() {
        return "CltDataInfo{" +
                "prefix='" + prefix + '\'' +
                ", contentStr='" + contentStr + '\'' +
                ", contentArray=" + contentArray +
                '}';
    }
}
