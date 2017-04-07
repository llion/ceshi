package com.color.home.widgets.clt_json;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import com.color.home.AppController;
import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.LogFont;
import com.color.home.Texts;
import com.color.home.model.CltDataInfo;
import com.color.home.model.CltJsonContent;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.utils.ColorHttpUtils;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;

import java.util.ArrayList;
import java.util.List;

import static com.color.home.widgets.clt_json.ItemMLPagedCltJsonView.getCltDateInfos;
import static com.color.home.widgets.clt_json.ItemMLPagedCltJsonView.getCltJsonList;
import static com.color.home.widgets.clt_json.ItemMLPagedCltJsonView.getTextList;
import static com.color.home.widgets.clt_json.ItemMLPagedCltJsonView.getUpdateInterval;
import static com.color.home.widgets.clt_json.ItemMLPagedCltJsonView.isUpdate;

public class ItemSLPagedCltJsonView extends TextView implements OnPlayFinishObserverable, Runnable, NetworkObserver {
    private static final boolean DBG = false;
    // never public, so that another class won't be messed up.
    private final static String TAG = "ItemSLPagedCltJson";

    protected Item mItem;
    protected OnPlayFinishedListener mListener;
    private boolean mIsGlaring;
    private boolean mIsDetached;
    protected long mOnePicDuration = 2000;
    private Runnable mNotifyRunnable;
    protected Integer[] mSplitTexts;
    protected String mText = "";
    protected int mIndex;
    protected RegionView mRegionView;

    private ColorHttpUtils mColorHttpUtils;
    private Runnable mCltRunnable;
    private NetworkConnectReceiver mNetworkConnectReceiver;
    private int mNeedPlayTimes = 1;
    private int mRealPlaytimes = 0;

    private List<CltDataInfo> mCltDataInfos;

    private List<String> mTextList = new ArrayList<String>();
    private List<Integer[]> mSplitters = new ArrayList<Integer[]>();
    private int mSplitterIndex = 0;
    private int mPageIndex = 0;

    private ArrayList<CltJsonContent> mCltJsonList;

    public ItemSLPagedCltJsonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ItemSLPagedCltJsonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ItemSLPagedCltJsonView(Context context) {
        this(context, null);
    }

    public void setItem(RegionView regionView, Item item) {
        mListener = regionView;
        mRegionView = regionView;
        this.mItem = item;

        mNotifyRunnable = new Runnable() {
            @Override
            public void run() {
                tellListener();
            }
        };

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
            removeCallbacks(mNotifyRunnable);
            post(mNotifyRunnable);
            return;
        }

        if (DBG)
            Log.d(TAG, "setItem. [mRegionView.getWidth()=" + mRegionView.getWidth());
        initDisplay(item);

        prepareCltJson(item, Texts.getText(mItem));

    }

    private void prepareCltJson(Item item, String text) {

        mNetworkConnectReceiver = new NetworkConnectReceiver(this);

        mColorHttpUtils = new ColorHttpUtils(mContext);
        mCltJsonList = getCltJsonList(text);

        mCltRunnable = new Runnable() {
            @Override
            public void run() {
                new NetTask().execute("");

            }
        };
    }

    protected void initDisplay(Item item) {
        getPaint().setAntiAlias(AppController.getInstance().getCfg().isAntialias());

        // Color.
        setTextColor(GraphUtils.parseColor(item.textColor));
        if (!TextUtils.isEmpty(item.backcolor) && !"0xFF000000".equals(item.backcolor))
            setBackgroundColor(GraphUtils.parseColor(item.backcolor));
        // Size.
        int size = Integer.parseInt(item.logfont.lfHeight);
        setTextSize(TypedValue.COMPLEX_UNIT_PX, size);

        LogFont logfont = item.logfont;
        if (logfont != null) {
            if (logfont.lfHeight != null) {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, Integer.parseInt(logfont.lfHeight));
            }

            setTypeface(AppController.getInstance().getTypeface(logfont.lfFaceName));

            int typeface = Typeface.NORMAL;
            if ("1".equals(logfont.lfItalic)) {
                typeface = Typeface.ITALIC;
            }
            if ("700".equals(logfont.lfWeight)) {
                typeface |= Typeface.BOLD;
            }
            if ("1".equals(logfont.lfUnderline)) {
                getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
            }
            setTypeface(getTypeface(), typeface);
        }


        if (DBG)
            Log.i(TAG, "setItem. textview width=" + getWidth()
                    + ", full duration=" + item.duration);
//        mDuration = 1000;

        mIsGlaring = "1".equals(mItem.beglaring);
        if (mIsGlaring) {
            getPaint().setShader(new LinearGradient(0, 0, 100, 100, new int[]{
                    Color.RED, Color.GREEN, Color.BLUE},
                    null, Shader.TileMode.MIRROR));
        }
    }

    protected Integer[] splitText(String text, int width) {
        if (width == 0) {
            if (DBG)
                Log.d(TAG, "splitText. [BAD width 0.");
            return null;
        }

        if (DBG)
            Log.d(TAG, "splitText. text= " + text);

        ArrayList<Integer> segments = new ArrayList<Integer>(20);
        // 0, ->
        segments.add(0);

        int length = text.length();
        int start = 0;

        for (start = 0; start <= length - 1; ) {
            int breakText = getPaint().breakText(text, start, length, true, width, null);
            if (breakText == 0) {
                if (DBG)
                    Log.d(TAG, "splitText. [End of text.");
                break;
            }

            segments.add(start + breakText);

            if (DBG)
                Log.d(TAG, "splitText. [breakText=" + breakText
                        + ", array=" + (start + breakText));

            start += breakText;
        }

//      int breakText = getPaint().breakText(text, true, itemWidth, measuredWidth);

        return segments.toArray(new Integer[0]);
    }


    @Override
    public void setListener(OnPlayFinishedListener listener) {
        this.mListener = listener;
    }

    @Override
    public void removeListener(OnPlayFinishedListener listener) {
        this.mListener = null;
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DBG)
            Log.i(TAG, "onAttachedToWindow. image = " + (mItem.filesource == null ? "NULL" : mItem.filesource.filepath));

        if (mCltRunnable != null) {
            removeCallbacks(mCltRunnable);
            post(mCltRunnable);
        }

        if (mNetworkConnectReceiver != null)
            ItemMLPagedCltJsonView.registerNetworkConnectReceiver(mContext, mNetworkConnectReceiver);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsDetached = true;

        boolean removeCallbacks = removeCallbacks(this);

        if (mCltRunnable != null)
            removeCallbacks(mCltRunnable);

        if (mNetworkConnectReceiver != null)
            ItemMLPagedCltJsonView.unRegisterNetworkConnectReceiver(mContext, mNetworkConnectReceiver);

        if (DBG)
            Log.i(TAG, "onDetachedFromWindow. Try to remove call back. result is removeCallbacks=" + removeCallbacks);
    }

    protected void tellListener() {

        if (DBG)
            Log.i(TAG, "tellListener. Tell listener =" + mListener);
        if (mListener != null) {
            mListener.onPlayFinished(this);
            removeListener(mListener);
        }
    }

    @Override
    public void run() {
        if (DBG) {
            Log.i(TAG, "run.  mPageIndex= " + mPageIndex + ", mSplitterIndex= " + mSplitterIndex
                    + ", mSplitters.get(mSplitterIndex).length = " + mSplitters.get(mSplitterIndex).length
                    + ", mTextList.size= " + mTextList.size() + ", mSplitters.size= " + mSplitters.size()
                    + ", mRealPlaytimes= " + mRealPlaytimes + ", mNeedPlayTimes= " + mNeedPlayTimes);

        }

        if (mSplitters.size() == 1 && mSplitters.get(0).length <= 1){//content is empty
            if (DBG)
                Log.d(TAG, "there is only one splitter and the content is empty.");
            setText("");

            if (mRealPlaytimes >= mNeedPlayTimes) {
                removeCallbacks(mNotifyRunnable);
                post(mNotifyRunnable);
            }

            mRealPlaytimes++;

            removeCallbacks(this);
            postDelayed(this, mOnePicDuration);

            return;
        }


        if (mPageIndex >= (mSplitters.get(mSplitterIndex).length - 1)) {
            if (DBG)
                Log.d(TAG, "the content of the current splitter had showed finished, show the content" +
                        " of the next splitter.");

            mSplitterIndex++;
            if (mSplitterIndex >= mSplitters.size()) {
                if (DBG)
                    Log.d(TAG, "content of all splitters had showed finished, show the content" +
                            " of the first splitter");

                mRealPlaytimes++;
                if (mRealPlaytimes >= mNeedPlayTimes) {
                    removeCallbacks(mNotifyRunnable);
                    post(mNotifyRunnable);
                }
                mSplitterIndex = 0;
            }

            mPageIndex = 0;
        }

        if (DBG)
            Log.d(TAG, "mSplitters.get(mSplitterIndex).length= " + mSplitters.get(mSplitterIndex).length);

        if (mSplitters.get(mSplitterIndex).length > 1) {
            if (DBG)
                Log.d(TAG, "mTextList.get(mSplitterIndex)= " + mTextList.get(mSplitterIndex) +
                        ", mSplitters.get(mSplitterIndex).length= " + mSplitters.get(mSplitterIndex).length +
                        ", start= " + mSplitters.get(mSplitterIndex)[mPageIndex] +
                        ", end= " + mSplitters.get(mSplitterIndex)[mPageIndex + 1] +
                        ", text= " + mTextList.get(mSplitterIndex).substring(
                        mSplitters.get(mSplitterIndex)[mPageIndex], mSplitters.get(mSplitterIndex)[mPageIndex + 1]));

            setText(mTextList.get(mSplitterIndex).substring(
                    mSplitters.get(mSplitterIndex)[mPageIndex], mSplitters.get(mSplitterIndex)[mPageIndex + 1]));

            mPageIndex++;

            if (DBG)
                Log.d(TAG, "this page duration= " + (mOnePicDuration / (mSplitters.get(mSplitterIndex).length - 1)));
            removeCallbacks(this);
            postDelayed(this, mOnePicDuration / (mSplitters.get(mSplitterIndex).length - 1));

        } else {
            if (DBG)
                Log.d(TAG, "the content of the current slpitter is empty.");
            setText("");

            mPageIndex++;

            if (DBG)
                Log.d(TAG, "display the content of the next splitter after one page duration, " +
                        "this page duration= " + mOnePicDuration);

            removeCallbacks(this);
            postDelayed(this, mOnePicDuration);
        }
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

    public class NetTask extends AsyncTask<String, Void, List<CltDataInfo>> {

        @Override
        protected List<CltDataInfo> doInBackground(String... params) {

            return mColorHttpUtils.getCltDataInfos(mCltJsonList);
        }

        @Override
        protected void onPostExecute(List<CltDataInfo> newCltDateInfos) {
            if (DBG)
                Log.d(TAG, "onPostExecute. newCltDateInfos= " + newCltDateInfos);

            if (isUpdate(mCltDataInfos, newCltDateInfos)) {
                mCltDataInfos = getCltDateInfos(mCltDataInfos, newCltDateInfos);

                initSplittersAndTextList();
                mSplitterIndex = 0;
                mPageIndex = 0;

                removeCallbacks(ItemSLPagedCltJsonView.this);
                post(ItemSLPagedCltJsonView.this);

            } else if (DBG)
                Log.d(TAG, "onPostExecute. data had not updated.");

            if (getUpdateInterval(mItem) > 0) {
                removeCallbacks(mCltRunnable);
                postDelayed(mCltRunnable, getUpdateInterval(mItem));
            }

        }
    }

    private void initSplittersAndTextList() {

        if (mTextList != null && mTextList.size() > 0) {
            mTextList.clear();
            mTextList = null;
        }
        mTextList = getTextList(mCltDataInfos);

        mSplitters.clear();
        for (int i = 0; i < mTextList.size(); i++)
            mSplitters.add(splitText(mTextList.get(i), mRegionView.getRegionWidth()));

    }


}