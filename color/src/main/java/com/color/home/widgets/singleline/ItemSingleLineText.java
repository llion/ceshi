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

    private Item mItem;
    private RegionView mRegionView;
    private OnPlayFinishedListener mListener;
    private boolean mIsGlaring;
    private boolean mIsDetached;
    private int mDuration;
    private Runnable mRunnable;
    private Integer[] mSplitTexts;
    private String mText;
    private int mIndex;

    private long mUpdateInterval = 0;
    private CltJsonUtils mCltJsonUtils;
    private Runnable mCltRunnable;
    private NetworkConnectReceiver mNetworkConnectReceiver;
    private boolean mIsCltJsonOk = false;

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

        mText = item.getTexts().mText;
        // At least a blank.
        if (TextUtils.isEmpty(mText)) {
            mText = " ";
        }

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


//        mDuration = 1000;

        mIsGlaring = "1".equals(mItem.beglaring);
        if (mIsGlaring) {
            getPaint().setShader(new LinearGradient(0, 0, 100, 100, new int[] {
                    Color.RED, Color.GREEN, Color.BLUE },
                    null, Shader.TileMode.MIRROR));
        }
        
        
        
//        int itemWidth = regionView.getRegionWidth();
        if (DBG)
            Log.d(TAG, "setItem. [mRegionView.getWidth()=" + mRegionView.getWidth());

        if (Texts.isCltJsonText(mText)){
            if (DBG)
                Log.d(TAG, "this is CLT_JSON text.");

            if ("1".equals(item.isNeedUpdate)) {
                mUpdateInterval = Long.parseLong(item.updateInterval);
            }
            mCltJsonUtils = new CltJsonUtils(mContext);

            mIsCltJsonOk = mCltJsonUtils.initMapList(mText);
            if (mIsCltJsonOk) {

                mCltRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (DBG)
                            Log.d(TAG, "mCltRunnable. mUpdateInterval= " + mUpdateInterval + ", Thread= " + Thread.currentThread());
                        new NetTask().execute("");

                        if ("1".equals(mItem.isNeedUpdate) && mUpdateInterval > 0) {
                            removeCallbacks(mCltRunnable);
                            postDelayed(mCltRunnable, mUpdateInterval);
                        }
                    }
                };

                removeCallbacks(mCltRunnable);
                post(mCltRunnable);

            } else
                firstShowText();

        } else {
            if (DBG)
                Log.d(TAG, "this is not CLT_JSON text.");

            firstShowText();

        }

        mNetworkConnectReceiver = new NetworkConnectReceiver(this);
        ItemMLScrollMultipic2View.registerNetworkConnectReceiver(mContext, mNetworkConnectReceiver);
        
    }

    private void firstShowText() {
        if (DBG)
            Log.d(TAG, "firstShowText.");

        mSplitTexts = splitText(mText, mRegionView.getWidth());
        resetTextFromStart();

        int fullDuration = Integer.parseInt(mItem.duration);

        if (DBG)
            Log.d(TAG, "firstShowText. mSplitTexts.length= " + mSplitTexts.length);

        if (mSplitTexts.length < 2) {
            mDuration = fullDuration;
        } else {
            mDuration = fullDuration / (mSplitTexts.length - 1);
        }

        if (DBG)
            Log.i(TAG, "firstShowText. textview width=" + getWidth()
                    + ", full duration=" + fullDuration + ", per page duration= " + mDuration);

        removeCallbacks(this);
        postDelayed(this, mDuration);
    }

    public void resetTextFromStart() {
        mIndex = 0;
        if (mSplitTexts.length < 2) {
            setText(mText);
        } else {
            setText(mText.substring(mSplitTexts[mIndex], mSplitTexts[mIndex + 1]));
            mIndex ++;
        }
    }
    
    private Integer[] splitText(String text, int width) {
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

//        removeCallbacks(this);
//        postDelayed(this, mDuration);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsDetached = true;

        boolean removeCallbacks = removeCallbacks(this);

        if (mCltRunnable != null)
            removeCallbacks(mCltRunnable);

        if (mNetworkConnectReceiver != null)
            mContext.unregisterReceiver(mNetworkConnectReceiver);

        if (DBG)
            Log.i(TAG, "onDetachedFromWindow. Try to remove call back. result is removeCallbacks=" + removeCallbacks);

    }

    private void tellListener() {
        if (mListener != null) {
            if (DBG)
                Log.i(TAG, "tellListener. Tell listener =" + mListener);
            mListener.onPlayFinished(this);
            removeListener(mListener);
        }
    }

    @Override
    public void run() {
        if (DBG)
            Log.i(TAG, "run.  mSplitTexts.length= " + mSplitTexts.length 
                    + ", mIndex=" + mIndex + ", Thread= " + Thread.currentThread());
        
        if (mIndex >= mSplitTexts.length - 1) {
            tellListener();
            if (DBG)
                Log.d(TAG, "run. [End of texts.");
            mIndex = 0;
//            resetTextFromStart();
//            
//            removeCallbacks(this);
//            postDelayed(this, mDuration);
            
        }

        if (mSplitTexts.length >= 2) {
            setText(mText.substring(mSplitTexts[mIndex], mSplitTexts[mIndex + 1]));
            mIndex ++;
        }

        removeCallbacks(this);
        postDelayed(this, mDuration);

        
    }

    @Override
    public void reloadContent() {
        if (DBG)
            Log.d(TAG, "reloadContent. mIsCltJsonOk= " + mIsCltJsonOk + ", mCltRunnable= " + mCltRunnable);
        if (mIsCltJsonOk && mCltRunnable != null){
            removeCallbacks(mCltRunnable);
            post(mCltRunnable);
        }

    }

    public class NetTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            return  mCltJsonUtils.getCltText();
        }

        @Override
        protected void onPostExecute(String result) {
            if (DBG)
                Log.d(TAG, "onPostExecute. result= " + result);
            if (result != null && !Constants.NETWORK_EXCEPTION.equals(result) && !result.equals(mText)) {
                if (DBG)
                    Log.d(TAG, "onPostExecute. result not equals mText, update.");
                mText = result;
                firstShowText();

            } else {
                if (DBG)
                    Log.d(TAG, "onPostExecute. result is not update.");
            }

        }
    }

}