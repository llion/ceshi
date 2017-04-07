package com.color.home.widgets.clt_json;

import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import com.android.internal.util.FastMath;
import com.color.home.AppController;
import com.color.home.Constants;
import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.LogFont;
import com.color.home.Texts;
import com.color.home.model.CltDataInfo;
import com.color.home.model.CltJsonContent;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.utils.ColorHttpUtils;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.MultilinePageSplitter;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;

import net.minidev.json.JSONArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemMLPagedCltJsonView extends TextView implements OnPlayFinishObserverable, Runnable, NetworkObserver {
    private static final boolean DBG = false;
    // never public, so that another class won't be messed up.
    private final static String TAG = "ItemMultiPageCltJson";

    //    private TextView mTv;
    protected Item mItem;
    protected OnPlayFinishedListener mListener;
    protected MultilinePageSplitter mPageSplitter;
    protected int mPageIndex;
    private int mRealPlaytimes = 0;
    protected int mNeedPlayTimes = 1;
    protected long mOnePicDuration = 2000;
    protected boolean mHadOnLayout = true;

    private ColorHttpUtils mColorHttpUtils;
    private Runnable mCltRunnable;
    private NetworkConnectReceiver mNetworkConnectReceiver;


    private List<CltDataInfo> mCltDataInfos;
    //    private int mCltJsonType;
    private List<MultilinePageSplitter> mSplitters = new ArrayList<MultilinePageSplitter>();
    private int mSplitterIndex = 0;
    private int mMaxLineNumPerPage;
    private ArrayList<CltJsonContent> mCltJsonList;

//    public static final Pattern CLT_JSON_PATTERN = Pattern.compile("CLT_JSON\\{\"url\":\".*\",\"filter\":\".*\"\\}CLT_JSON");


    public ItemMLPagedCltJsonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ItemMLPagedCltJsonView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemMLPagedCltJsonView(Context context) {
        super(context);
    }

    public void setItem(RegionView regionView, Item item) {
        mListener = regionView;
        this.mItem = item;

        try {
            if (item.multipicinfo != null && item.multipicinfo.onePicDuration != null)
                mOnePicDuration = Integer.parseInt(item.multipicinfo.onePicDuration);

            if (item.playTimes != null)
                mNeedPlayTimes = Integer.parseInt(item.playTimes);

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (DBG)
            Log.d(TAG, "mOnePicDuration= " + mOnePicDuration + ", need play times= " + mNeedPlayTimes);
        if ((mOnePicDuration < 0) || (mNeedPlayTimes < 1)) {
            removeCallbacks(this);
            post(this);
            return;
        }

        mNetworkConnectReceiver = new NetworkConnectReceiver(this);

        prepareCltJson(item);

        initDisplay(item);


    }

    private void prepareCltJson(Item item) {

        mColorHttpUtils = new ColorHttpUtils(mContext);
        mCltJsonList = getCltJsonList(Texts.getText(item));

        mCltRunnable = new Runnable() {
            @Override
            public void run() {
                if (DBG)
                    Log.d(TAG, "mCltRunnable. Thread= " + Thread.currentThread());
                new NetTask().execute("");

            }
        };
    }

    public static ArrayList<CltJsonContent> getCltJsonList(String text) {

        ArrayList<CltJsonContent> cltJsonList = new ArrayList<CltJsonContent>();
        String prefix, subStr;
        JSONObject jsonObject;
        int firstMarkIndex;

        for (; text.indexOf("CLT_JSON") != text.lastIndexOf("CLT_JSON"); ) {
            firstMarkIndex = text.indexOf("CLT_JSON");
            prefix = text.substring(0, firstMarkIndex);
            subStr = text.substring(firstMarkIndex + 8);
            if (DBG)
                Log.d(TAG, "firstMarkIndex= " + firstMarkIndex + ", prefix= " + prefix + ", subStr= " + subStr
                        + ", subStr.substring(0, subStr.indexOf(\"CLT_JSON\"))= " + subStr.substring(0, subStr.indexOf("CLT_JSON")));

            try {
                jsonObject = new JSONObject(subStr.substring(0, subStr.indexOf("CLT_JSON")));
                if (DBG)
                    Log.d(TAG, "firstMarkIndex= " + firstMarkIndex + ", url= " + jsonObject.getString("url"));

                if (!TextUtils.isEmpty(jsonObject.getString("url")))
                    cltJsonList.add(new CltJsonContent(prefix, jsonObject));



            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (subStr.indexOf("CLT_JSON") + 8 < subStr.length())
                text = subStr.substring(subStr.indexOf("CLT_JSON") + 8);
            else
                text = subStr.substring(subStr.indexOf("CLT_JSON"));

        }


        if (DBG)
            for (int i = 0; i < cltJsonList.size(); i++)
                Log.d(TAG, "cltJson: " + cltJsonList.get(i));

        return cltJsonList;

    }

    protected void initDisplay(Item item) {
        int backcolor;
        if (!TextUtils.isEmpty(item.backcolor) && !"0xFF000000".equals(item.backcolor)) {
            if ("0xFF010000".equals(item.backcolor)) {
                backcolor = GraphUtils.parseColor("0xFF000000");
            } else {
                backcolor = GraphUtils.parseColor(item.backcolor);
            }
        } else
            backcolor = GraphUtils.parseColor("0x00000000");
        setBackgroundColor(backcolor);

        if (!TextUtils.isEmpty(item.textColor))
            setTextColor(GraphUtils.parseColor(item.textColor));

        if (item.alhpa != null)
            setAlpha(Float.parseFloat(item.alhpa));

        getPaint().setAntiAlias(AppController.getInstance().getCfg().isAntialias());

        LogFont logfont = item.logfont;
        if (logfont != null) {

            // Size.
            if (logfont.lfHeight != null) {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, Integer.parseInt(logfont.lfHeight));
            }

            //style
            int style = Typeface.NORMAL;
            if ("1".equals(logfont.lfItalic)) {
                style = Typeface.ITALIC;
            }
            if ("700".equals(logfont.lfWeight)) {
                style |= Typeface.BOLD;
            }
            if ("1".equals(logfont.lfUnderline)) {
                getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
            }

            Typeface typeface = AppController.getInstance().getTypeface(logfont.lfFaceName);
            if (typeface == null)
                typeface = Typeface.defaultFromStyle(style);

            if (DBG)
                Log.d(TAG, "style= " + style + ", lfFaceName= " + logfont.lfFaceName + ", typeface= " + typeface);

            setTypeface(typeface, style);

            setLineSpacing(getLineSpacingExtra(logfont), 1.0f);
            if (DBG)
                Log.d(TAG, "lineHeight= " + FastMath.round(getPaint().getFontMetrics(null) * 1.0f + getLineSpacingExtra(logfont)));
        }

        if (DBG)
            Log.d(TAG, "centeralAlign= " + item.centeralalign);

        if ("1".equals(item.centeralalign)) {//居中
            setGravity(Gravity.CENTER);

        } else if ("2".equals(item.centeralalign)) {//居右
            setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        }
    }

    private float getLineSpacingExtra(LogFont logcat) {
        return Integer.parseInt(logcat.lfHeight) / 4;
//        return 2.0f;
    }

    protected void setPageText() {
        if (DBG)
            Log.i(TAG, "setPageText. mSplitterIndex= " + mSplitterIndex + ", mPageIndex= " + mPageIndex);

        if (mPageIndex < mSplitters.get(mSplitterIndex).getPages().size())
            setText(mSplitters.get(mSplitterIndex).getPages().get(mPageIndex));

        else
            setText("");
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (DBG)
            Log.i(TAG, "onLayout. Thread= " + Thread.currentThread());
        if (!mHadOnLayout) {

            if (DBG)
                Log.d(TAG, "getHeight= " + this.getHeight() + ", getLineHeight= " + this.getLineHeight()
                        + ",  this.getPaint().getFontMetrics(null)= " + this.getPaint().getFontMetrics(null));
            mMaxLineNumPerPage = this.getHeight() / this.getLineHeight() + (this.getHeight() % this.getLineHeight() >= (int) this.getPaint().getFontMetrics(null) ? 1 : 0);
            if (mMaxLineNumPerPage < 1)
                mMaxLineNumPerPage = 1;//最少显示1行

            if (mCltRunnable != null) {
                //clt_json
                removeCallbacks(mCltRunnable);
                post(mCltRunnable);
            }

            mHadOnLayout = true;
        }

        if (DBG) {
            Log.i(TAG, "onLayout, mHadOnLayout= " + mHadOnLayout + ", layout= " + getLayout() + ", line count= " + getLineCount());
            Layout layout = getLayout();
            if (layout != null) {
                for (int i = 0; i < getLineCount(); i++) {
                    Log.d(TAG, "i= " + i + ", layout.getLineStart(i)= " + layout.getLineStart(i) +
                            ", layout.getLineEnd(i)= " + layout.getLineEnd(i));
                }
            }
        }
    }

    public static int getCltJsonType(List<CltDataInfo> cltDataInfos) {
        if (hadJsonAarray(cltDataInfos))
            if (isAllArraySizeEqual(cltDataInfos))//all array sizes are equal
                return Constants.CLT_JSON_ARRAY_SIZE_EQUAL;
            else
                return Constants.CLT_JSON_ARRAY_SIZE_UNEQUAL;

        else
            return Constants.CLT_JSON_NO_ARRAY;

    }

    public static String getTextFromCltDataInfos(int clt_json_type, int indexInArray, int currentArrayPosition, List<CltDataInfo> cltDataInfos) {

        if (clt_json_type == Constants.CLT_JSON_ARRAY_SIZE_EQUAL)
            return getTextAtIndexOfAarraySizeEqual(indexInArray, cltDataInfos);

        if (clt_json_type == Constants.CLT_JSON_ARRAY_SIZE_UNEQUAL)
            return getTextAtIndexOfAarraySizeUnequal(indexInArray, currentArrayPosition, cltDataInfos);

        return getAllContentStr(cltDataInfos);
    }

    public static String getTextAtIndexOfAarraySizeUnequal(int indexInArray, int currentArrayPosition, List<CltDataInfo> cltDataInfos) {

        StringBuilder sb = new StringBuilder();
        int arrayPosition = 1;

        if (cltDataInfos != null && cltDataInfos.size() > 0) {
            for (CltDataInfo cltDataInfo : cltDataInfos) {

                if (cltDataInfo.getContentArray() == null) {
                    //prefix
                    if (cltDataInfo.getPrefix() != null)
                        sb.append(cltDataInfo.getPrefix());

                    //content
                    sb.append(getContentStr(cltDataInfo));

                } else {

                    if (arrayPosition == currentArrayPosition) {
                        //prefix
                        if (cltDataInfo.getPrefix() != null)
                            sb.append(cltDataInfo.getPrefix());

                        if (indexInArray < cltDataInfo.getContentArray().size())
                            sb.append(getContentArrayStr(indexInArray, cltDataInfo));
                    }

                    arrayPosition++;
                }

            }
        }


        if (DBG)
            Log.d(TAG, "getTextAtIndexOfAarraySizeUnequal. sb= " + sb);
        return sb.toString();

    }

    public static JSONArray getArray(int position, List<CltDataInfo> cltDataInfos) {

        int arrayPosition = 0;
        if (cltDataInfos != null && cltDataInfos.size() > 0) {
            for (CltDataInfo cltDataInfo : cltDataInfos) {
                if (cltDataInfo.getContentArray() != null) {
                    arrayPosition++;
                    if (arrayPosition == position)
                        return cltDataInfo.getContentArray();
                }

            }
        }

        return null;
    }

    public static int getArraysNum(List<CltDataInfo> cltDataInfos) {
        int num = 0;
        if (cltDataInfos != null && cltDataInfos.size() > 0) {
            for (CltDataInfo cltDataInfo : cltDataInfos) {
                if (cltDataInfo.getContentArray() != null)
                    num++;
            }
        }

        if (DBG)
            Log.d(TAG, "array num= " + num);
        return num;
    }

    public static String getTextAtIndexOfAarraySizeEqual(int indexInArray, List<CltDataInfo> cltDataInfos) {
        StringBuilder sb = new StringBuilder();

        if (cltDataInfos != null && cltDataInfos.size() > 0) {
            for (CltDataInfo cltDataInfo : cltDataInfos) {

                //prefix
                if (cltDataInfo.getPrefix() != null)
                    sb.append(cltDataInfo.getPrefix());

                //this content is string
                if (cltDataInfo.getContentStr() != null)
                    sb.append(getContentStr(cltDataInfo));

                    //this content is array
                else if (cltDataInfo.getContentArray() != null && indexInArray < cltDataInfo.getContentArray().size())
                    sb.append(getContentArrayStr(indexInArray, cltDataInfo));

            }

        }

        if (DBG)
            Log.d(TAG, "getTextAtIndexOfAarraySizeEqual. indexInArray= " + indexInArray + ", sb= " + sb);

        return sb.toString();

    }

    private static String getContentArrayStr(int indexInArray, CltDataInfo cltDataInfo) {
        if (cltDataInfo.getContentArray().get(indexInArray) == null)
            return "";
        else
            return cltDataInfo.getContentArray().get(indexInArray).toString();
    }

    private static String getContentStr(CltDataInfo cltDataInfo) {
        if (TextUtils.isEmpty(cltDataInfo.getContentStr()) || cltDataInfo.getContentStr().equals(Constants.NETWORK_EXCEPTION))
            return "";
        else
            return cltDataInfo.getContentStr();
    }

    public static boolean isAllArraySizeEqual(List<CltDataInfo> cltDataInfos) {

        int size = -1;
        if (cltDataInfos != null && cltDataInfos.size() > 0) {
            for (CltDataInfo cltDataInfo : cltDataInfos) {
                if (cltDataInfo.getContentArray() != null) {
                    if (size == -1)//
                        size = cltDataInfo.getContentArray().size();//record the first array size

                    else if (cltDataInfo.getContentArray().size() != size)
                        return false;
                }

            }
        }

        if (DBG)
            Log.d(TAG, "all array sizes are equal, size= " + size);
        return true;
    }

    public static String getAllContentStr(List<CltDataInfo> cltDataInfos) {
        StringBuilder sb = new StringBuilder();

        if (cltDataInfos != null && cltDataInfos.size() > 0) {
            for (CltDataInfo cltDataInfo : cltDataInfos) {

                //prefix
                if (cltDataInfo.getPrefix() != null)
                    sb.append(cltDataInfo.getPrefix());

                //content
                if (cltDataInfo.getContentStr() != null)
                    sb.append(getContentStr(cltDataInfo));
            }

        }

        if (DBG)
            Log.d(TAG, "getAllContentStr. sb= " + sb);

        return sb.toString();
    }

    public static boolean hadJsonAarray(List<CltDataInfo> cltDataInfos) {

        if (cltDataInfos != null && cltDataInfos.size() > 0) {
            for (CltDataInfo cltDataInfo : cltDataInfos)
                if (cltDataInfo.getContentArray() != null) {
                    if (DBG)
                        Log.d(TAG, "there exist array in clt_json data.");
                    return true;
                }
        }

        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DBG)
            Log.i(TAG, "onAttachedToWindow. layout= " + this.getLayout());

        mHadOnLayout = false;

//        if (Texts.isValidCltJsonText(Texts.getText(mItem)))
//            mIsFirstTimeGetCltJson = true;

        if (mNetworkConnectReceiver != null)
            registerNetworkConnectReceiver(mContext, mNetworkConnectReceiver);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DBG)
            Log.i(TAG, "onDetachedFromWindow, mHandler= " + mHandler);

        removeCallbacks(this);

        if (mCltRunnable != null)
            removeCallbacks(mCltRunnable);

        if (mNetworkConnectReceiver != null)
            unRegisterNetworkConnectReceiver(mContext, mNetworkConnectReceiver);

        stopHandler();
    }

    private void stopHandler() {
        if (mHandler != null) {
            mHandler.stop();
            mHandler = null;
        }
    }


    public static void registerNetworkConnectReceiver(Context context, NetworkConnectReceiver networkConnectReceiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(networkConnectReceiver, filter);
    }

    public static void unRegisterNetworkConnectReceiver(Context context, NetworkConnectReceiver networkConnectReceiver){
        context.unregisterReceiver(networkConnectReceiver);
    }

    protected MTextMarquee mHandler;

    @Override
    public void setListener(OnPlayFinishedListener listener) {
        this.mListener = listener;
    }

    @Override
    public void removeListener(OnPlayFinishedListener listener) {
        this.mListener = null;
    }

    protected void tellListener() {
        if (DBG)
            Log.d(TAG, "tellListener. mListener= " + mListener);
        if (mListener != null) {
            if (DBG)
                Log.i(TAG, "tellListener. Tell listener =" + mListener);
            mListener.onPlayFinished(this);
            removeListener(mListener);
        }
    }

    public void nextPage() {
        if (DBG)
            Log.d(TAG, "mRealPlaytimes= " + mRealPlaytimes + ", mNeedPlayTimes= " + mNeedPlayTimes
                    + ", mSplitters.get(mSplitterIndex).getPages().size()= " + mSplitters.get(mSplitterIndex).getPages().size());
        mPageIndex++;

        if (mPageIndex >= mSplitters.get(mSplitterIndex).getPages().size()) {
            if (DBG)
                Log.d(TAG, "the content of the current splitter had showed finished, show the content " +
                        "of the next splitter.");

            mSplitterIndex++;
            if (mSplitterIndex >= mSplitters.size()) {
                if (DBG)
                    Log.d(TAG, "content of all splitters had showed finished, show the content " +
                            "of the first splitter");
                mRealPlaytimes++;
                if (mRealPlaytimes == mNeedPlayTimes)
                    post(this);

                mSplitterIndex = 0;
            }

            mPageIndex = 0;
            displayByPages();

        } else {
            if (DBG)
                Log.d(TAG, "the content of the current splitter had not showed completed, show the next" +
                        "page of the current splitter.");
            setPageText();
        }
    }

    @Override
    public void run() {
        tellListener();
    }

    @Override
    public void reloadContent() {
        if (DBG)
            Log.d(TAG, "reloadContent. mCltRunnable= " + mCltRunnable);
        if (mCltRunnable != null) {
            removeCallbacks(mCltRunnable);
            post(mCltRunnable);
        }

    }

    protected static final class MTextMarquee extends Handler {
        private final static String TAG = "MTextMarquee";
        private static final boolean DBG = false;
        private static final int MESSAGE_TICK = 0x1;
        private long MARQUEE_DELAY;


        private final WeakReference<ItemMLPagedCltJsonView> mView;
        private boolean mShouldStop;

        public MTextMarquee(ItemMLPagedCltJsonView view, long onePicDuration) {
            mView = new WeakReference<ItemMLPagedCltJsonView>(view);
            MARQUEE_DELAY = onePicDuration;
            if (DBG)
                Log.d(TAG, "MARQUEE_DELAY= " + MARQUEE_DELAY);
        }

        public void start() {
            final ItemMLPagedCltJsonView view = mView.get();
            if (view != null) {
                sendEmptyMessageDelayed(MESSAGE_TICK, MARQUEE_DELAY);
            }

        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_TICK:
                    if (mShouldStop) {
                        if (DBG)
                            Log.d(TAG, "handleMessage. [mShouldStop");
                        return;
                    }

                    tick();
                    break;
            }
        }

        public void stop() {
            if (DBG)
                Log.d(TAG, "stop.");

            removeMessages(MESSAGE_TICK);
            mShouldStop = true;
        }

        void tick() {
            if (DBG)
                Log.d(TAG, "tick.");

            removeMessages(MESSAGE_TICK);

            if (mShouldStop) {
                if (DBG)
                    Log.d(TAG, "tick. [mShouldStop");
                return;
            }

            final ItemMLPagedCltJsonView view = mView.get();
            if (view != null) {
                sendEmptyMessageDelayed(MESSAGE_TICK, MARQUEE_DELAY);
                view.nextPage();
            }
        }
    }

    public class NetTask extends AsyncTask<String, Void, List<CltDataInfo>> {

        @Override
        protected List<CltDataInfo> doInBackground(String... params) {

            return mColorHttpUtils.getCltDataInfos(mCltJsonList);
        }

        @Override
        protected void onPostExecute(List<CltDataInfo> newCltDataInfos) {
            if (DBG)
                Log.d(TAG, "onPostExecute. newCltDataInfos= " + newCltDataInfos);

            if (isUpdate(mCltDataInfos, newCltDataInfos)) {
                Log.d(TAG, "data had updated.");

                mCltDataInfos = getCltDateInfos(mCltDataInfos, newCltDataInfos);

                initSplitters();

                mSplitterIndex = 0;
                mPageIndex = 0;

                displayByPages();

            } else
                Log.d(TAG, "data had not updated.");

            if (getUpdateInterval(mItem) > 0) {
                removeCallbacks(mCltRunnable);
                postDelayed(mCltRunnable, getUpdateInterval(mItem));
            }

        }

    }

    public static long getUpdateInterval(Item item) {

        if (DBG)
            Log.d(TAG, "isNeedUpdate= " + item.isNeedUpdate + ", updateInterval= " + item.updateInterval);

        if ("1".equals(item.isNeedUpdate) && item.updateInterval != null) {
            try {
                return Long.parseLong(item.updateInterval);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    private void displayByPages() {
        setPageText();

        if (mSplitters.get(mSplitterIndex).getPages().size() > 0) {
            stopHandler();
            mHandler = new MTextMarquee(this, mOnePicDuration / mSplitters.get(mSplitterIndex).getPages().size());
            mHandler.start();

        } else {//no page, text is empty
            stopHandler();
            mHandler = new MTextMarquee(this, mOnePicDuration);
            mHandler.start();
        }
    }

    private void initSplitters() {

        List<String> textList = getTextList(mCltDataInfos);

        setVisibility(INVISIBLE);

        mSplitters.clear();
        for (int i = 0; i < textList.size(); i++) {
            MultilinePageSplitter splitter = new MultilinePageSplitter(mMaxLineNumPerPage, this);
            splitter.append(textList.get(i));
            mSplitters.add(splitter);
        }

        setVisibility(VISIBLE);

        if (DBG) {
            Log.d(TAG, "mSplitters size= " + mSplitters.size());

            for (int i = 0; i < mSplitters.size(); i++) {
                List<CharSequence> pages = mSplitters.get(i).getPages();
                for (int j = 0; j < pages.size(); j++)
                    Log.d(TAG, "i= " + i + ", j= " + j + ", page= " + pages.get(j));
            }

        }


    }

    public static List<String> getTextList(List<CltDataInfo> cltDataInfos) {

        List<String> textList = new ArrayList<String>();

        int cltJsonType = getCltJsonType(cltDataInfos);
        if (DBG)
            Log.d(TAG, "cltJsonType= " + cltJsonType);

        if (cltJsonType == Constants.CLT_JSON_ARRAY_SIZE_EQUAL) {
            if (DBG)
                Log.d(TAG, "getArray(1, cltDataInfos).size()= " + getArray(1, cltDataInfos).size());

            if (getArray(1, cltDataInfos).size() == 0)
                textList.add(getTextAtIndexOfAarraySizeEqual(0, cltDataInfos));

            else
                for (int index = 0; index < getArray(1, cltDataInfos).size(); index++) {
                    if (DBG)
                        Log.d(TAG, "index= " + index);
                    textList.add(getTextAtIndexOfAarraySizeEqual(index, cltDataInfos));
                }

        } else if (cltJsonType == Constants.CLT_JSON_ARRAY_SIZE_UNEQUAL) {

            int arrayNum = getArraysNum(cltDataInfos);

            for (int position = 1; position <= arrayNum; position++) {
                if (DBG)
                    Log.d(TAG, "position= " + position + ", getArray(position, cltDataInfos).size()= " + getArray(position, cltDataInfos).size());

                if (getArray(position, cltDataInfos).size() == 0)
                    textList.add(getTextAtIndexOfAarraySizeUnequal(0, position, cltDataInfos));

                else
                    for (int index = 0; index < getArray(position, cltDataInfos).size(); index++) {
                        if (DBG)
                            Log.d(TAG, "index= " + index);
                        textList.add(getTextAtIndexOfAarraySizeUnequal(index, position, cltDataInfos));
                    }
            }

        } else
            textList.add(getAllContentStr(cltDataInfos));

        if (DBG) {
            Log.d(TAG, "getTextList. textList.size= " + textList.size());
            for (int i = 0; i < textList.size(); i++)
                Log.d(TAG, "i= " + i + ", text= " + textList.get(i));
        }

        return textList;
    }

    public static List<CltDataInfo> getCltDateInfos(List<CltDataInfo> oldCltDataInfos, List<CltDataInfo> newCltDataInfos) {

        if (oldCltDataInfos == null)
            return newCltDataInfos;

        for (int i = 0; i < oldCltDataInfos.size(); i++) {

            if (isContentStrUpdate(oldCltDataInfos.get(i).getContentStr(), newCltDataInfos.get(i).getContentStr()))
                oldCltDataInfos.get(i).setContentStr(newCltDataInfos.get(i).getContentStr());

            if (isContentArrayUpdate(newCltDataInfos.get(i).getContentStr(), oldCltDataInfos.get(i).getContentArray(), newCltDataInfos.get(i).getContentArray()))
                oldCltDataInfos.get(i).setContentArray(newCltDataInfos.get(i).getContentArray());

        }

        return oldCltDataInfos;

    }


    public static boolean isUpdate(List<CltDataInfo> oldCltDataInfos, List<CltDataInfo> newCltDataInfos) {

        if (oldCltDataInfos == null) {
            return true;

        } else if (newCltDataInfos != null) {
            if (DBG)
                Log.d(TAG, "isUpdate. oldCltDataInfos.size= " + oldCltDataInfos.size() +
                        ", newCltDataInfos.size= " + newCltDataInfos.size());
            for (int i = 0; i < oldCltDataInfos.size(); i++) {

                if (isContentStrUpdate(oldCltDataInfos.get(i).getContentStr(), newCltDataInfos.get(i).getContentStr()))
                    return true;

                if (isContentArrayUpdate(newCltDataInfos.get(i).getContentStr(), oldCltDataInfos.get(i).getContentArray(), newCltDataInfos.get(i).getContentArray()))
                    return true;

            }

        }

        return false;
    }

    private static boolean isContentArrayUpdate(String newContentStr, JSONArray oldContentArray, JSONArray newContentArray) {

        if (newContentArray == null) {
            if (oldContentArray != null && !Constants.NETWORK_EXCEPTION.equals(newContentStr))//this array was changed to be null
                return true;

            return false;//old array and new array both are null, or the network exception cause the array be null
        }

        //new array is not null

        if (oldContentArray == null)
            return true;

        //old array and new array both are not null
        if (oldContentArray.size() != newContentArray.size())
            return true;

        //sizes are equal, compare every element in the two arrays
        for (int i = 0; i < oldContentArray.size(); i++) {
            if (oldContentArray.get(i) == null && newContentArray.get(i) != null)
                return true;

            if (oldContentArray.get(i) != null && newContentArray.get(i) == null)
                return true;

            if (oldContentArray.get(i) != null && newContentArray.get(i) != null &&
                    (!oldContentArray.get(i).toString().equals(newContentArray.get(i).toString())))
                return true;
        }

        return false;
    }

    private static boolean isContentStrUpdate(String oldContentStr, String newContentStr) {

        if (oldContentStr == null) {
            if (newContentStr != null && !newContentStr.equals(Constants.NETWORK_EXCEPTION))
                return true;

        } else {
            if (!Constants.NETWORK_EXCEPTION.equals(newContentStr) && !oldContentStr.equals(newContentStr))
                return true;

        }

        return false;

    }

}
