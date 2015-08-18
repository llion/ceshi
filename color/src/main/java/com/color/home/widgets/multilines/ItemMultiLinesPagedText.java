package com.color.home.widgets.multilines;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.LogFont;
import com.color.home.ProgramParser.Region;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.PageSplitter;
import com.color.home.widgets.RegionView;

public class ItemMultiLinesPagedText extends TextView implements OnPlayFinishObserverable, Runnable {
    private static final boolean DBG = false;
    // never public, so that another class won't be messed up.
    private final static String TAG = "ItemMultiLinesPagedText";

    private TextView mTv;
    private Item mItem;
    private int mHeight;
    private OnPlayFinishedListener mListener;
    private PageSplitter mPageSplitter;
    private TextPaint mTextPaint;
    private String mText;
    private boolean mIsAttached;
    private int mPageIndex;
    private long mItemDuration;
    private boolean mCenteralAlign;

    public ItemMultiLinesPagedText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ItemMultiLinesPagedText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemMultiLinesPagedText(Context context) {
        super(context);
    }

    public void setItem(RegionView regionView, Region region, Item item) {
        mListener = regionView;
        this.mItem = item;
        mTv = this;

        // From file normally.
        mText = item.getTexts().mText;

        // Color.
        if (!TextUtils.isEmpty(item.textColor))
            mTv.setTextColor(GraphUtils.parseColor(item.textColor));
        if (!TextUtils.isEmpty(item.backcolor) && !"0xFF000000".equals(item.backcolor))
            mTv.setBackgroundColor(GraphUtils.parseColor(item.backcolor));

        if (item.alhpa != null)
            mTv.setAlpha(Float.parseFloat(item.alhpa));
        // Size.
        LogFont logfont = item.logfont;
        if (logfont != null) {
            if (logfont.lfHeight != null) {
                mTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, Integer.parseInt(logfont.lfHeight) * 1.1f);
            }
            int typeface = Typeface.NORMAL;
            if ("1".equals(logfont.lfItalic)) {
                typeface = Typeface.ITALIC;
            }
            if ("700".equals(logfont.lfWeight)) {
                typeface |= Typeface.BOLD;
            }
            if ("1".equals(logfont.lfUnderline)) {
                mTv.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
            }
            mTv.setTypeface(mTv.getTypeface(), typeface);
        }

        mItemDuration = Integer.parseInt(item.duration);

        mPageSplitter = new PageSplitter(getRegionWidth(region), getRegionHeight(region), 1, 0);
        // mTextPaint = new TextPaint();
        // mTextPaint.setTextSize(mTv.getTextSize());
        mPageSplitter.append(mText, mTv.getPaint());
        
        mCenteralAlign = "1".equals(item.centeralalign);
        if (mCenteralAlign) {
            if (DBG)
                Log.d(TAG, "setItem. [CENTER_HORIZONTAL");
            setGravity(Gravity.CENTER_HORIZONTAL);
        }

        mPageIndex = 0;
        setPageText();
        if (DBG)
            Log.i(TAG, "onLayout. center align. page 0=" + mPageSplitter.getPages().get(0));

    }

    private void setPageText() {
        mTv.setText(mPageSplitter.getPages().get(mPageIndex));
    }

    private int getRegionHeight(Region region) {
        return Integer.parseInt(region.rect.height);
    }

    private int getRegionWidth(Region region) {
        if (DBG)
            Log.i(TAG, "getRegionWidth. region=" + region);
        return Integer.parseInt(region.rect.width);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mIsAttached = false;
        removeCallbacks(this);

        if (mHandler != null) {
            mHandler.stop();
            mHandler = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttached = true;

        // Schedule item time up.
        removeCallbacks(this);
        postDelayed(this, mItemDuration);

        if (mPageSplitter.getPages().size() > 1) {
            mHandler = new MTextMarquee(this);
            mHandler.start();
        }
    }

    MTextMarquee mHandler;

    @Override
    public void setListener(OnPlayFinishedListener listener) {
        this.mListener = listener;
    }

    @Override
    public void removeListener(OnPlayFinishedListener listener) {
        this.mListener = null;
    }

    private void tellListener() {
        if (mListener != null) {
            if (DBG)
                Log.i(TAG, "tellListener. Tell listener =" + mListener);
            mListener.onPlayFinished(this);
        }
    }

    public void nextPage() {
        mPageIndex++;
        if (mPageIndex >= mPageSplitter.getPages().size()) {
            mPageIndex = 0;
        }

        if (DBG)
            Log.i(TAG, "run. next page. mPageIndex=" + mPageIndex);
        setPageText();
    }

    @Override
    public void run() {
        tellListener();
    }

    private static final class MTextMarquee extends Handler {
        private final static String TAG = "MTextMarquee";
        private static final boolean DBG = false;
        private static final int MARQUEE_DELAY = 5000; // 5 sec.
        private static final int MESSAGE_TICK = 0x1;

        private final WeakReference<ItemMultiLinesPagedText> mView;
        private boolean mShouldStop;

        public MTextMarquee(ItemMultiLinesPagedText view) {
            mView = new WeakReference<ItemMultiLinesPagedText>(view);
        }

        public void start() {
            final ItemMultiLinesPagedText view = mView.get();
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

        void stop() {
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

            final ItemMultiLinesPagedText view = mView.get();
            if (view != null) {
                sendEmptyMessageDelayed(MESSAGE_TICK, MARQUEE_DELAY);
                view.nextPage();
            }
        }
    }
}
