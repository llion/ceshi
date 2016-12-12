package com.color.home.widgets.singleline.cltjsonutils;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.jayway.jsonpath.JsonPath;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/12/6.
 */
public class CltJsonUtils {


    private static final boolean DBG = false;
    private static final String TAG = "CltJsonUtils";
    private Context context;

    public CltJsonUtils(Context context) {
        this.context = context;
    }

    public List<CltContent> cltContentList = new ArrayList<CltContent>();

    public String getCltText() {
        String str = "";
        String content = "";
            try {
                if (cltContentList != null && cltContentList.size() > 0){
                    for (CltContent cltContent : cltContentList){
                        str += cltContent.getPrefix();
                        content = getJsonFronNet(getUrl(cltContent.getJsonObject().getString("url")), cltContent.getJsonObject().getString("filter"));
                        if (DBG)
                            Log.d(TAG, "content= " + content);
//  str += getJsonFronNet(cltContent.getJsonObject().getString("url"), cltContent.getJsonObject().getString("filter"));
                        str += content;
                        if (DBG)
                            Log.d(TAG, "str= " + str);

                        str = str.replaceAll("[\\n]", "\n");
                        if (DBG)
                            Log.d(TAG, "str= " + str);
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        return str;
    }

    private String getUrl(String url) {
        if (DBG)
            Log.d(TAG, "getUrl. origin url= " + url);

        if (!TextUtils.isEmpty(url) && url.contains("$(account)")) {
            String usernameString = Settings.Global.getString(context.getContentResolver(), "user.name");
            if (DBG)
                Log.i(TAG, "setItem. usernameString=" + usernameString);
            if (!TextUtils.isEmpty(usernameString))
                url = url.replace("$(account)", usernameString);
        }

        if (DBG)
            Log.d(TAG, "getUrl. url= " + url);
        return url;
    }

    private String getJsonFronNet(String url, String filter) {
        String json = "";
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() == 200) {
                if (DBG)
                    Log.d(TAG, "response successful.");

                InputStream inputStream=conn.getInputStream();
                json=new String(convertIsToByteArray(inputStream));
                if (DBG)
                    Log.d(TAG, "response json= " + json);

                return  JsonPath.parse(json).read(filter);
            }
            return json;

        } catch (Exception e) {
            e.printStackTrace();
            return json;
        }

    }

    private byte[] convertIsToByteArray(InputStream inputStream) {
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        byte buffer[]=new byte[1024];
        int length=0;
        try {
            while ((length=inputStream.read(buffer))!=-1) {
                baos.write(buffer, 0, length);
            }
            inputStream.close();
            baos.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return baos.toByteArray();

    }

    public boolean initMapList(String text){

        String prefix, subStr;
        int firstMarkIndex;
        JSONObject jsonObject;
        for (; text.indexOf("CLT_JSON") != text.lastIndexOf("CLT_JSON"); ) {
            firstMarkIndex = text.indexOf("CLT_JSON");
            prefix = text.substring(0, firstMarkIndex);
            subStr = text.substring(firstMarkIndex + 8);
            if (DBG)
                Log.d(TAG, "firstMarkIndex= " + firstMarkIndex + ", prefix= " + prefix + ", subStr= " + subStr
                        + ", subStr.substring(0, subStr.indexOf(\"CLT_JSON\"))= " + subStr.substring(0, subStr.indexOf("CLT_JSON")));
            try {

                jsonObject = new JSONObject(subStr.substring(0, subStr.indexOf("CLT_JSON")));
                cltContentList.add(new CltContent(prefix, jsonObject));

                if (DBG)
                    Log.d(TAG, "firstMarkIndex= " + firstMarkIndex + ", url= " + jsonObject.getString("url")
                            + ", filter= " + jsonObject.getString("filter"));

            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (subStr.indexOf("CLT_JSON") + 8 < subStr.length())
                text = subStr.substring(subStr.indexOf("CLT_JSON") + 8);
            else
                text = subStr.substring(subStr.indexOf("CLT_JSON"));

        }

        if (cltContentList != null && cltContentList.size() > 0)
            return true;
        else
            return false;

    }

    public class CltContent {
        String prefix;
        JSONObject jsonObject;

        public CltContent(String prefix, JSONObject jsonObject) {
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

    }

}
