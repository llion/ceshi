package com.color.home.widgets.singleline_scroll;

import android.content.Context;
import android.graphics.PixelFormat;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.ProgramParser;
import com.color.home.ProgramParser.Item;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.widgets.RegionView;
import com.color.home.widgets.multilines.ItemMLScrollMultipic2View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import okhttp3.HttpUrl;

/**
 * Created by Administrator on 2017/2/10.
 */
public class ScrollRSSSurfaceview extends SinglelineScrollSurfaceView implements NetworkObserver {

    private final static String TAG = "ScrollRSSView";
    private static final boolean DBG = false;
    private final ProgramParser.Region mRegion;

    private ScrollRSSObject mScrollRSSObject;
//    private ScrollRSSRenderer mScrollRSSRenderer;

    private NetworkConnectReceiver mNetworkConnectReceiver;



    public ScrollRSSSurfaceview(Context context, ProgramParser.Region region, RegionView regionView) {
        super(context);
        mContext = context;
        mListener = regionView;
        mRegion = region;
    }


    public void setRssItems(Item item, HttpUrl httpUrl) {

        setEGLContextClientVersion(2);
        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);

        mScrollRSSObject = new ScrollRSSObject(mContext, simplifyUrl(httpUrl));

        //filters
        List<String> filters = new ArrayList<String>();
        String filterStr = httpUrl.queryParameter("filter");
        if (!TextUtils.isEmpty(filterStr)) {
            String[] fields =filterStr.split("_");
            if (fields != null && fields.length > 0){
                for (String field : fields)
                    filters.add(field);
            }

        }
        mScrollRSSObject.setFilters(filters);

        //text size
        int textSize = 20;
        if (!TextUtils.isEmpty(httpUrl.queryParameter("size"))){
            try {
                textSize = Integer.parseInt(httpUrl.queryParameter("size"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

        } else {
            try {
                textSize = Integer.parseInt(mRegion.rect.height) - 2 * Integer.parseInt(mRegion.rect.borderwidth);
                if (textSize > 4)
                textSize -= 4;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        mScrollRSSObject.setTextSize(textSize);

        //background color
        String textColorStr = httpUrl.queryParameter("color");
        if (!TextUtils.isEmpty(textColorStr)) {
            mScrollRSSObject.setTextColor(textColorStr);
        } else
            mScrollRSSObject.setTextColor("0xFFFFFFFF");

        //background color
        String backColorStr = httpUrl.queryParameter("bgcolor");
        if (!TextUtils.isEmpty(backColorStr)) {
            mScrollRSSObject.setBgColor(backColorStr);
        } else
            mScrollRSSObject.setBgColor("0x00000000");

//        //pcHeight
//        int pcHeight = textSize + 4;
//        if (DBG)
//            Log.d(TAG, "textSize= " + textSize + ", pcHeight= " + pcHeight);
//        mScrollRSSObject.setPcHeight(pcHeight);

        //pixelsPerFrame
        float pixelPerFrame = 1.0f;
        String speedStr = httpUrl.queryParameter("speed");
        if (!TextUtils.isEmpty(speedStr)) {
            try {
                float speed = Integer.parseInt(speedStr);
                pixelPerFrame = speed / 60.f;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        mScrollRSSObject.setPixelPerFrame(pixelPerFrame);

        //update interval
        if (!TextUtils.isEmpty(httpUrl.queryParameter("interval"))){
            try {
                long interval = Long.parseLong(httpUrl.queryParameter("interval"));
                mScrollRSSObject.setInterval(interval);
            } catch (NumberFormatException e){
                e.printStackTrace();
            }

        }

        //play time
        int mPlayLength = 3600000;
        try {
            mPlayLength = Integer.parseInt(item.duration);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (DBG)
            Log.d(TAG, "setItem. [mPlayLength=" + mPlayLength);

        removeCallbacks(this);
        postDelayed(this, mPlayLength);

        mScrollRenderer = new ScrollRSSRenderer(mScrollRSSObject);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mScrollRenderer);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mNetworkConnectReceiver = new NetworkConnectReceiver(this);
        ItemMLScrollMultipic2View.registerNetworkConnectReceiver(mContext, mNetworkConnectReceiver);

    }

    private String simplifyUrl(HttpUrl httpUrl) {

        StringBuilder url = new StringBuilder();

        url.append(httpUrl.scheme() + "://"
                + httpUrl.host()
                + ":" + httpUrl.port()
                + (TextUtils.isEmpty(httpUrl.encodedPath()) ? "" : httpUrl.encodedPath()));

        if (DBG)
            Log.d(TAG, "simplifyUrl. querySize= " + httpUrl.querySize());
        if (httpUrl.querySize() > 0) {

            Set<String> set = null;
            String parameterName;
            for (int i = 0; i < httpUrl.querySize(); i++) {

                parameterName = httpUrl.queryParameterName(i);
                if (DBG)
                    Log.d(TAG, "parameterName= " + parameterName
                            + ", parameterValue= " + httpUrl.queryParameterValue(i));

                if (!(("type".equals(parameterName)) || ("bgcolor".equals(parameterName))
                        || ("color".equals(parameterName)) || ("size".equals(parameterName))
                        || ("speed".equals(parameterName)) || ("filter".equals(parameterName))
                        || ("interval".equals(parameterName)))) {

                    if (set == null)
                        set = new TreeSet<String>();
                    set.add(parameterName);
                }

            }

            if (set != null && set.size() > 0) {
                Iterator<String> iterator = set.iterator();
                boolean hadParameter = false;
                String validParameterName;

                while (iterator.hasNext()) {
                    validParameterName = iterator.next();
                    if (DBG)
                        Log.d(TAG, "validParameterName= " + validParameterName
                                + ", queryParameter= " + httpUrl.queryParameter(validParameterName));

                    if (hadParameter)
                        url.append("&");
                    else {//before first parameter
                        url.append("?");
                        hadParameter = true;
                    }

                    url.append(validParameterName);
                    url.append("=");
                    url.append(httpUrl.queryParameter(validParameterName));

                }
            }
        }


        if (DBG)
            Log.d(TAG, "simplifyUrl. url= " + url.toString());
        return url.toString();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DBG)
            Log.d(TAG, "onDetachedFromWindow. [mScrollRSSObject=" + mScrollRSSObject
                    + ", mNetworkConnectReceiver= " + mNetworkConnectReceiver);

        if (mScrollRSSObject != null){
            mScrollRSSObject.removeCltRunnable();
            mScrollRSSObject = null;
        }

        if (mNetworkConnectReceiver != null) {
            mContext.unregisterReceiver(mNetworkConnectReceiver);
            mNetworkConnectReceiver = null;
        }
    }

    @Override
    public void reloadContent() {
        if (DBG)
            Log.d(TAG, "reloadContent. mScrollRSSObject= " + mScrollRSSObject);
        if (mScrollRSSObject != null)
            mScrollRSSObject.reloadRSSContent();
    }
}
