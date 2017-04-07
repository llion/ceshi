package com.color.home.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.Constants;
import com.color.home.model.CltJsonContent;
import com.color.home.model.CltDataInfo;
import com.color.home.model.DataInUrl;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.internal.Utils;

import net.minidev.json.JSONArray;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Administrator on 2017/3/29.
 */

public class ColorHttpUtils {

    private static final boolean DBG = false;
    private static final String TAG = "ColorHttpUtils";
    public static final String FOLDER_LAN = "/mnt/sdcard/Android/data/com.color.home/files/Ftp";

    private Context mContext;
    private OkHttpClient mClient;
    private File mCacheDir;

    private String mEtag = "";
    private boolean mIsFirstGetBitmap = true;

    public ColorHttpUtils(Context context) {
        this.mContext = context;
        initClient();
    }

    private void initClient() {

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

    public List<CltDataInfo> getCltDataInfos(ArrayList<CltJsonContent> cltJsonList) {

        if (cltJsonList == null || cltJsonList.size() == 0)
            return null;

        List<DataInUrl> dataAlreadyGet = new ArrayList<DataInUrl>();
        List<CltDataInfo> cltDataInfos = new ArrayList<CltDataInfo>();

        for (int i = 0; i < cltJsonList.size(); i++) {

            CltDataInfo cltDataInfo = new CltDataInfo();
            cltDataInfo.setPrefix(cltJsonList.get(i).getPrefix().replace("\\n", "\n"));

            //json data
            String data = "";
            try {
                String url = getUrl(cltJsonList.get(i).getJsonObject().getString("url"));
                data = getData(url, dataAlreadyGet);

                if (!hadRecord(url, dataAlreadyGet))//record this url and save the data
                    dataAlreadyGet.add(new DataInUrl(url, data));

            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                setContent(data, cltJsonList.get(i).getJsonObject().getString("filter"), cltDataInfo);
            } catch (JSONException e) {
                e.printStackTrace();
                cltDataInfo.setContentStr(data);//no filter
            }

            cltDataInfos.add(cltDataInfo);

        }

        dataAlreadyGet.clear();

        return cltDataInfos;

    }

    private boolean hadRecord(String url, List<DataInUrl> dataAlreadyGet) {

        if (dataAlreadyGet != null && dataAlreadyGet.size() > 0) {

            for (DataInUrl data : dataAlreadyGet)
                if (url.equals(data.getUrl())) {
                    if (DBG)
                        Log.d(TAG, "the url accessed had recorded.");
                    return true;
                }
        }

        return false;

    }

    public String getData(String url, List<DataInUrl> dataAlreadyGet) {

        if (TextUtils.isEmpty(url))
            return "";

        String data = getDataFromRecord(url, dataAlreadyGet);
        if (data == null) {
            if (DBG)
                Log.d(TAG, "this url have not accessed.");

            if (!isNetworkAvailable())
                data = getDataFromCache(url);
            else
                data = getDataFromNet(url);

        }

        return data.replace("\\n", "\n");
    }

    private void setContent(String data, String filter, CltDataInfo cltDataInfo) {
        if (DBG)
            Log.d(TAG, "setContent. data= " + data + ", filter= " + filter);

        if (TextUtils.isEmpty(filter) ||Constants.NETWORK_EXCEPTION.equals(data)) {
            cltDataInfo.setContentStr(data);
            return;
        }

        try {
            if (JsonPath.parse(data).read(filter) instanceof JSONArray) {
                if (DBG)
                    Log.d(TAG, "the result after filtered is array.");
                cltDataInfo.setContentArray((JSONArray) JsonPath.parse(data).read(filter));

            } else {
                if (DBG)
                    Log.d(TAG, "the result after filtered is not array.");
                cltDataInfo.setContentStr(JsonPath.parse(data).read(filter).toString());

            }
        } catch (Exception e) {//data is null or empty or the filter is wrong
            e.printStackTrace();
            cltDataInfo.setContentStr("");
        }

    }

    private String getDataFromNet(String url) {
        if (DBG)
            Log.d(TAG, "get data from net.");

        Request request = new Request.Builder()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .url(url)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build();

        Response response = null;
        String result = "";

        try {
            response = mClient.newCall(request).execute();
            if (response.isSuccessful()) {
                if (DBG)
                    Log.d(TAG, "get data from net successful.");
                result = response.body().string();

            } else {
                if (DBG)
                    Log.d(TAG, "get data from net failed. code= " + response.code()
                            + ", message= " + response.message());
            }

        } catch (Exception e) {
            e.printStackTrace();

            if (isNetworkException(e))
                result = Constants.NETWORK_EXCEPTION;

        } finally {
            if (response != null)
                Utils.closeQuietly(response);
        }

        return result;

    }

    private String getDataFromCache(String url) {
        if (DBG)
            Log.d(TAG, "get data from cache.");

        Request request = new Request.Builder()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .url(url)
                .cacheControl(CacheControl.FORCE_CACHE)
                .build();

        Response response = null;

        try {
            response = mClient.newCall(request).execute();
            if (response.isSuccessful()) {
                if (DBG)
                    Log.d(TAG, "get data from cache successful.");
                return response.body().string();

            } else {
                if (DBG)
                    Log.d(TAG, "get data from cache failed. code= " + response.code()
                            + ", message= " + response.message());
            }

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            if (response != null)
                Utils.closeQuietly(response);
        }

        return "";
    }

    public byte[] getBitmapBytesWithEtag(String originUrl) {
        if (DBG)
            Log.d(TAG, "originUrl= " + originUrl);

        HttpUrl httpUrl = HttpUrl.parse(originUrl);
        if (httpUrl == null)
            return null;

        if (DBG)
            Log.d(TAG, "scheme= " + httpUrl.scheme() + ", host= " + httpUrl.host()
                    + ", port= " + httpUrl.port() + ", encodedPath= " + httpUrl.encodedPath());

        boolean isNetworkAvailable = isNetworkAvailable();
        Response response = null;
        byte[] bytes = null;

        try {

            if (DBG)
                Log.d(TAG, "isNetworkAvailable= " + isNetworkAvailable + ", mIsFirstGetBitmap= " + mIsFirstGetBitmap);

            //if the network is available,access network.
            //if the network is not available and it is the first time to get bitmap, get bytes from cache
            if (isNetworkAvailable || (!isNetworkAvailable && mIsFirstGetBitmap)) {
                String url = httpUrl.scheme() + "://"
                        + httpUrl.host()
                        + ":" + httpUrl.port()
                        + (TextUtils.isEmpty(httpUrl.encodedPath()) ? "" : httpUrl.encodedPath());
                if (DBG)
                    Log.d(TAG, "getBitmapBytesWithEtag. url= " + url);

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
                    Log.d(TAG, "getBitmapBytesWithEtag. mClient= " + mClient + ", Thread= " + Thread.currentThread()
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

    public byte[] getBitmapBytes(String url) {

        Response response = null;
        byte[] bytes = null;
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
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build();
        }

        try {
            if (DBG)
                Log.d(TAG, "getBitmapBytes. Thread= " + Thread.currentThread());
            response = mClient.newCall(request).execute();
            if (response.isSuccessful()) {
                bytes = response.body().bytes();

            } else if (DBG)
                Log.d(TAG, "response faild. code= " + response.code() + ", message= " + response.message());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null)
                Utils.closeQuietly(response.body());
        }
        return bytes;
    }

    private String getDataFromRecord(String url, List<DataInUrl> dataAlreadyGet) {

        if (dataAlreadyGet != null && dataAlreadyGet.size() > 0) {

            for (DataInUrl data : dataAlreadyGet)
                if (url.equals(data.getUrl())) {
                    if (DBG)
                        Log.d(TAG, "the data of this url had already getted. url= " + url);
                    return data.getData();
                }
        }

        return null;

    }

    protected String getUrl(String url) {
        if (DBG)
            Log.d(TAG, "getUrl. origin url= " + url);
        if (!url.contains("http://") && !url.contains("https://"))
            url = "http://" + url;

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

    public boolean isNetworkException(Exception e) {

        if (e instanceof UnknownHostException) {
            Log.e(TAG, "UnknownHostException caught.");
            return true;
        }
        if (e instanceof ConnectException) {
            Log.e(TAG, "ConnectException caught.");
            return true;
        }
        if (e instanceof SocketTimeoutException) {
            Log.e(TAG, "SocketTimeoutException caught.");
            return true;
        }
        if (e instanceof SocketException) {
            Log.e(TAG, "SocketException caught.");
            return true;
        }
        return false;
    }

}
