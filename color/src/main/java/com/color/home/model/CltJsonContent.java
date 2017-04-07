package com.color.home.model;

import org.json.JSONObject;

/**
 * Created by Administrator on 2017/3/16.
 */
public class CltJsonContent {

    String prefix;
    JSONObject jsonObject;

    public CltJsonContent(String prefix, JSONObject jsonObject) {
        this.prefix = prefix;
        this.jsonObject = jsonObject;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    @Override
    public String toString() {
        return "CltJsonContent{" +
                "prefix='" + prefix + '\'' +
                ", jsonObject=" + jsonObject +
                '}';
    }
}
