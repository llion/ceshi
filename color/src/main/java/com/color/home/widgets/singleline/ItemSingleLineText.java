package com.color.home.widgets.singleline;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import com.color.home.AppController;
import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.LogFont;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;

public class ItemSingleLineText extends TextView implements OnPlayFinishObserverable, Runnable {
    private static final boolean DBG = false;
    // never public, so that another class won't be messed up.
    private final static String TAG = "ItemSingleLineText";

    private Item mItem;
    private OnPlayFinishedListener mListener;
    private boolean mIsGlaring;
    private boolean mIsDetached;
    private int mDuration;
    private Runnable mRunnable;
    private Integer[] mSplitTexts;
    private String mText;
    private int mIndex;

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


        if (DBG)
            Log.i(TAG, "setItem. textview width=" + getWidth()
                    + ", full duration=" + mDuration);
//        mDuration = 1000;

        mIsGlaring = "1".equals(mItem.beglaring);
        if (mIsGlaring) {
            getPaint().setShader(new LinearGradient(0, 0, 100, 100, new int[] {
                    Color.RED, Color.GREEN, Color.BLUE },
                    null, Shader.TileMode.MIRROR));
        }
        
        
        
        int itemWidth = regionView.getRegionWidth();
        if (DBG)
            Log.d(TAG, "setItem. [itemWidth=" + itemWidth);
        mSplitTexts = splitText(mText, itemWidth);

        
        resetTextFromStart();
        
        int fullDuration = Integer.parseInt(item.duration);
        if (mSplitTexts.length < 2) {
            mDuration = fullDuration;
        } else {
            mDuration = fullDuration / (mSplitTexts.length - 1);
        }
        
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

        removeCallbacks(this);
        postDelayed(this, mDuration);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsDetached = true;

        boolean removeCallbacks = removeCallbacks(this);
        if (DBG)
            Log.i(TAG, "onDetachedFromWindow. Try to remove call back. result is removeCallbacks=" + removeCallbacks);

    }

    private void tellListener() {
        if (mListener != null) {
            if (DBG)
                Log.i(TAG, "tellListener. Tell listener =" + mListener);
            mListener.onPlayFinished(this);
        }
    }

    @Override
    public void run() {
        if (DBG)
            Log.i(TAG, "run.  mSplitTexts.length= " + mSplitTexts.length 
                    + ", mIndex=" + mIndex);
        
        if (mIndex >= mSplitTexts.length - 1) {
            tellListener();
            if (DBG)
                Log.d(TAG, "run. [End of texts.");
            
//            resetTextFromStart();
//            
//            removeCallbacks(this);
//            postDelayed(this, mDuration);
            
        } else {
            setText(mText.substring(mSplitTexts[mIndex], mSplitTexts[mIndex + 1]));
            mIndex++;
            
            removeCallbacks(this);
            postDelayed(this, mDuration);
        }
        
    }

}