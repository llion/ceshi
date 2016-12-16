package com.color.home.widgets.singleline.cltjsonutils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.internal.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Administrator on 2016/12/6.
 */
public class CltJsonUtils {

    private static final boolean DBG = false;
    private static final String TAG = "CltJsonUtils";
    public static final String FOLDER_LAN = "/mnt/sdcard/Android/data/com.color.home/files/Ftp";

    private Context mContext;
    private OkHttpClient mClient;
    public List<CltContent> mCltContentList;
    private File mCacheDir;

    private String mEtag = "";
    private boolean mIsFirstGetBitmap = true;


    public CltJsonUtils(Context context) {
        this.mContext = context;
        setClient();
    }

    private void setClient() {

        mCacheDir = getCacheDir("Okcache");
        if (!mCacheDir.exists())
            mCacheDir.mkdir();

        ensureTargetDirRoom();

        int cacheSize = 20 * 1024 * 1024;
        if (DBG)
            Log.d(TAG, "cacheSize= " + cacheSize / 1024 / 1024 + "M");
        Cache cache = new Cache(mCacheDir, cacheSize);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cache(cache);

        mClient = builder.build();
    }

    public String getCltText() {
        CacheControl cacheControl = new CacheControl.Builder().noCache().build();
        String str = "";
        try {
            if (mCltContentList != null && mCltContentList.size() > 0) {
                for (CltContent cltContent : mCltContentList) {
                    str += cltContent.getPrefix();
                    str += getContentFromNet(getUrl(cltContent.getJsonObject().getString("url")),
                            cltContent.getJsonObject().getString("filter"),
                            cacheControl);

                    if (DBG)
                        Log.d(TAG, "before replayce \"\\\\n\", str= " + str);

                    str = str.replace("\\n", "\n");
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
            String usernameString = Settings.Global.getString(mContext.getContentResolver(), "user.name");
            if (DBG)
                Log.i(TAG, "setItem. usernameString=" + usernameString);
            if (!TextUtils.isEmpty(usernameString))
                url = url.replace("$(account)", usernameString);
        }

        if (DBG)
            Log.d(TAG, "getUrl. url= " + url);
        return url;
    }

    private String getContentFromNet(String url, String filter, CacheControl cacheControl) {

        Response response = null;
        String content = "";

        try {
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json; charset=utf-8")
                    .get();
            Request request;
            if (!isNetworkAvailable()) {
                request = builder
                        .cacheControl(CacheControl.FORCE_CACHE)
                        .build();
            } else {

                ensureTargetDirRoom();
                request = builder
                        .cacheControl(cacheControl)
                        .build();
            }

            response = mClient.newCall(request).execute();

            if (DBG)
                Log.d(TAG, "getContentFromNet. mClient= " + mClient + ", Thread= " + Thread.currentThread());
            if (response.isSuccessful()) {
                if (DBG)
                    Log.d(TAG, "getContentFromNet. response.isSuccessful. cacheResponse= " + response.cacheResponse());
                content = JsonPath.parse(response.body().string()).read(filter);
            }


        } catch (Exception e) {
            e.printStackTrace();

        } finally {

            if (response != null)
                Utils.closeQuietly(response.body());

            if (DBG)
                Log.d(TAG, "getContentFromNet. content= " + content);
            return content;
        }

    }

    public boolean initMapList(String text) {

        mCltContentList = new ArrayList<CltContent>();
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
                mCltContentList.add(new CltContent(prefix, jsonObject));

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

        if (mCltContentList != null && mCltContentList.size() > 0)
            return true;
        else
            return false;

    }

    public byte[] getBitmapBytes(String originUrl) {

        HttpUrl httpUrl = HttpUrl.parse(originUrl);
        if (httpUrl == null)
            return null;

        boolean isNetworkAvailable = isNetworkAvailable();
        Response response = null;
        byte[] bytes = null;

        try {

            if (DBG)
                Log.d(TAG, "isNetworkAvailable= " + isNetworkAvailable + ", mIsFirstGetBitmap= " + mIsFirstGetBitmap);

            //if the network is available,access network.
            //if the network is not available and it is the first time to get bitmap, get bytes from cache
            if (isNetworkAvailable || (!isNetworkAvailable && mIsFirstGetBitmap)) {
                String url = httpUrl.scheme() + "://" + httpUrl.host() + (TextUtils.isEmpty(httpUrl.encodedPath()) ? "" : httpUrl.encodedPath());
                if (DBG)
                    Log.d(TAG, "getBitmapBytes. url= " + url);

                Request.Builder builder = new Request.Builder()
                        .url(url)
                        .get();
                Request request;

                if (!isNetworkAvailable) {
                    request = builder.cacheControl(CacheControl.FORCE_CACHE)
                            .build();

                } else {
                    ensureTargetDirRoom();
                    request = builder.addHeader("If-None-Match", mEtag).build();
                }

                response = mClient.newCall(request).execute();

                if (DBG)
                    Log.d(TAG, "response.code= " + response.code());
                if (response.code() == 304) {//source of network have not changed
                    if (DBG)
                        Log.d(TAG, "response.code= 304, response no change.");
                } else if (response.isSuccessful()) {
                    if (mIsFirstGetBitmap)
                        mIsFirstGetBitmap = false;

                    if (isNetworkAvailable) {//source of network have changed
                        Headers headers = response.headers();
                        for (int i = 0; i < headers.size(); i++) {
                            if ("ETag".equals(headers.name(i))) {
                                mEtag = headers.value(i);
                                if (DBG)
                                    Log.d(TAG, "mEtag= " + mEtag);
                                break;
                            }
                        }

                    } else {
                        if (DBG)
                            Log.d(TAG, "NetWork is not available. get bytes from cache.");
                    }

                    bytes = response.body().bytes();

                }

                if (DBG)
                    Log.d(TAG, "getBitmapBytes. mClient= " + mClient + ", Thread= " + Thread.currentThread()
                            + ",  response.cacheResponse()= " + response.cacheResponse()
                            + ", response.networkResponse= " + response.networkResponse());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null)
                Utils.closeQuietly(response.body());

            if (DBG)
                Log.d(TAG, "bytes == null? " + (bytes == null));
            return bytes;
        }
    }

    public static class CltContent {
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

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        if (DBG)
            Log.d(TAG, "isNetworkAvailable. activeNetworkInfo= " + activeNetworkInfo);

        return activeNetworkInfo != null;
    }

    private void ensureTargetDirRoom() {

        if (DBG) {
            long freeSize = mCacheDir.getUsableSpace() / 1024 / 1024;
            long totalSize = mCacheDir.getTotalSpace() / 1024 / 1024;
            Log.d(TAG, "ensureTargetDirRoom. cacheDir path= " + mCacheDir.getAbsolutePath() + ", freeSize=  " + freeSize
                    + "M, totalSize= " + totalSize + "M" + ", mCacheDir.listFiles() == null" + (mCacheDir.listFiles() == null));
        }

        if (mCacheDir.listFiles() != null && mCacheDir.listFiles().length > 0) {
            if (((mCacheDir.getUsableSpace() / 1024 / 1024) < 100) || (getDirSize(mCacheDir) > (100 * 1024 * 1024))) {
                clearDir(mCacheDir);
            }
        }
    }

    private int getDirSize(File dir) {
        File[] files = dir.listFiles();
        int dirSize = 0;
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isDirectory())
                    dirSize += getDirSize(file);
                else
                    dirSize += file.length();
            }
        }

        if (DBG)
            Log.d(TAG, "dir " + dir.getAbsolutePath() + " size= " + dirSize);
        return dirSize;
    }

    public void clearDir(File dir) {
        if (DBG)
            Log.d(TAG, "before clearing, cacheDir freeSize= " + dir.getUsableSpace());

        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                boolean delFlag = f.delete();
                if (DBG)
                    Log.d(TAG, f.getName() + " del- " + delFlag);
                if (!delFlag) {
                    try {
                        FileInputStream fi = new FileInputStream(f);
                        fi.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    f.delete();
                }
            }
        }
        if (DBG)
            Log.d(TAG, "after clearing, cacheDir freeSize= " + dir.getUsableSpace());

    }

    public File getCacheDir(String uniqueName) {
//        String cachePath;
//        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
//                || !Environment.isExternalStorageRemovable()) {
//            cachePath = mContext.getExternalCacheDir().getPath();
//        } else {
//            cachePath = mContext.getCacheDir().getPath();
//        }
        return new File(FOLDER_LAN + File.separator + uniqueName);
    }

}
