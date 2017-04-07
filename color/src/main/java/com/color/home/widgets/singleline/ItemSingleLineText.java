package com.color.home.widgets.singleline;

import java.util.ArrayList;

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
import com.color.home.Constants;
import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.LogFont;
import com.color.home.Texts;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;
import com.color.home.widgets.multilines.ItemMLScrollMultipic2View;
import com.color.home.widgets.singleline.cltjsonutils.CltJsonUtils;

public class ItemSingleLineText extends TextView implements OnPlayFinishObserverable, Runnable, NetworkObserver {
    private static final boolean DBG = false;
    // never public, so that another class won't be messed up.
    private final static String TAG = "ItemSingleLineText";

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

    private long mUpdateInterval = 0;
    private CltJsonUtils mCltJsonUtils;
    private Runnable mCltRunnable;
    private NetworkConnectReceiver mNetworkConnectReceiver;
    private int mNeedPlayTimes = 1;
    private int mRealPlaytimes = 1;
    private boolean mIsFirstTimeGetCltJson;

    public ItemSingleLineText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ItemSingleLineText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ItemSingleLineText(Context context) {
        this(context, null);
    }

    public void setItem(RegionView regionView, Item item) {
        mListener = regionView;
        mRegionView = regionView;
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

        mNotifyRunnable = new Runnable() {
            @Override
            public void run() {
                tellListener();
            }
        };

//        int itemWidth = regionView.getRegionWidth();
        if (DBG)
            Log.d(TAG, "setItem. [mRegionView.getWidth()=" + mRegionView.getWidth());
        initDisplay(item);

        String itemText = Texts.getText(item);
        if (Texts.isValidCltJsonText(itemText)) {
            if (DBG)
                Log.d(TAG, "this is CLT_JSON text.");

            prepareCltJson(item, itemText);

        } else {
            if (DBG)
                Log.d(TAG, "this is not CLT_JSON text.");

            mText = itemText;
            if (TextUtils.isEmpty(mText)) {
                if (DBG)
                    Log.d(TAG, "text is empty. tell Listener after one picture duration. mOnePicDuration= " + mOnePicDuration);
                postDelayed(mNotifyRunnable, mOnePicDuration);

                return;
            }

            displayByPages();

        }

    }

    private void prepareCltJson(Item item, String itemText) {
        if ("1".equals(item.isNeedUpdate) && item.updateInterval != null) {
            try {
                mUpdateInterval = Long.parseLong(item.updateInterval);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        mNetworkConnectReceiver = new NetworkConnectReceiver(this);

        mCltJsonUtils = new CltJsonUtils(mContext);
        mCltJsonUtils.initMapList(itemText);

        mCltRunnable = new Runnable() {
            @Override
            public void run() {
                if (DBG)
                    Log.d(TAG, "mCltRunnable. mUpdateInterval= " + mUpdateInterval + ", Thread= " + Thread.currentThread());
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

    private void displayByPages() {
        if (DBG)
            Log.d(TAG, "displayByPages. region width= " + mRegionView.getRegionWidth());

        mSplitTexts = splitText(mText, mRegionView.getRegionWidth());
        mIndex = 0;

        if (DBG)
            Log.d(TAG, "mSplitTexts.length= " + mSplitTexts.length + ", mText= " + mText
                    + ", this.getWidth= " + this.getWidth() + ", this.getHeight= " + this.getHeight());

//        int fullDuration = Integer.parseInt(mItem.duration);

        if (DBG)
            Log.d(TAG, "displayByPages. mSplitTexts.length= " + mSplitTexts.length);

//        if (mSplitTexts.length < 2) {
//            mDuration = fullDuration;
//        } else {
//            mDuration = fullDuration / (mSplitTexts.length - 1);
//        }

        if (DBG)
            Log.i(TAG, "displayByPages. textview width=" + getWidth() + ", per page duration= " + mOnePicDuration);

        removeCallbacks(this);
        post(this);
    }

    protected Integer[] splitText(String text, int width) {
        if (width == 0) {
            if (DBG)
                Log.d(TAG, "splitText. [BAD width 0.");
            return null;
        }
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
                        + ", array=" + (start + breakText)
                        + ", text length=" + mText.length());

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

        if (Texts.isValidCltJsonText(Texts.getText(mItem)))
            mIsFirstTimeGetCltJson = true;

        if (mCltRunnable != null) {
            removeCallbacks(mCltRunnable);
            post(mCltRunnable);
        }

        if (mNetworkConnectReceiver != null)
            ItemMLScrollMultipic2View.registerNetworkConnectReceiver(mContext, mNetworkConnectReceiver);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsDetached = true;

        boolean removeCallbacks = removeCallbacks(this);

        if (mCltRunnable != null)
            removeCallbacks(mCltRunnable);

        if (mNetworkConnectReceiver != null)
            ItemMLScrollMultipic2View.unRegisterNetworkConnectReceiver(mContext, mNetworkConnectReceiver);

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

        if (mSplitTexts == null){
            postDelayed(mNotifyRunnable, mOnePicDuration);
            return;
        }

        if (DBG) {
            Log.i(TAG, "run.  mSplitTexts.length= " + mSplitTexts.length
                    + ", mIndex=" + mIndex + ", Thread= " + Thread.currentThread());

        }

        if ((mSplitTexts.length <= 1)||
                ((mSplitTexts.length > 1) && (mIndex >= mSplitTexts.length - 1))) {
            if (DBG)
                Log.d(TAG, "run. [End of texts. mRealPlaytimes= " + mRealPlaytimes + ", mNeedPlayTimes= " + mNeedPlayTimes);

            if (mRealPlaytimes == mNeedPlayTimes)
                tellListener();

            mRealPlaytimes++;
            mIndex = 0;

        }

        if (mSplitTexts.length > 1) {//the mText is not empty
            setText(mText.substring(mSplitTexts[mIndex], mSplitTexts[mIndex + 1]));
            mIndex++;
        }

        removeCallbacks(this);
        postDelayed(this, mOnePicDuration);
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

    public class NetTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            return mCltJsonUtils.getCltText();
        }

        @Override
        protected void onPostExecute(String result) {
            if (DBG)
                Log.d(TAG, "onPostExecute. result= " + result);

            if (TextUtils.isEmpty(result) || Constants.NETWORK_EXCEPTION.equals(result)){
                if (DBG)
                    Log.d(TAG, "the data is empty or the network had exception.");

                if (mIsFirstTimeGetCltJson){
                    if (DBG)
                        Log.d(TAG, "the current text is empty, notify finished after one picture duration.");
                    removeCallbacks(mNotifyRunnable);
                    postDelayed(mNotifyRunnable, mOnePicDuration);
                }

            } else if (!result.equals(mText)){
                if (DBG)
                    Log.d(TAG, "onPostExecute. result not equals mText, update.");
                mText = result;
                displayByPages();

            } else  if (DBG)
                Log.d(TAG, "onPostExecute. result is not update.");

            if (mIsFirstTimeGetCltJson)
                mIsFirstTimeGetCltJson = false;

            if (mUpdateInterval > 0) {
                removeCallbacks(mCltRunnable);
                postDelayed(mCltRunnable, mUpdateInterval);
            }

        }
    }

}