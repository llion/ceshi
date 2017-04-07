package com.color.home.widgets.multilines;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
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
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.MultilinePageSplitter;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;

public class ItemMultiLinesPagedText extends TextView implements OnPlayFinishObserverable, Runnable {
    private static final boolean DBG = false;
    // never public, so that another class won't be messed up.
    private final static String TAG = "ItemMultiLinesPagedText";

    //    private TextView mTv;
    protected Item mItem;
    protected OnPlayFinishedListener mListener;
    protected MultilinePageSplitter mPageSplitter;
    protected String mText = "";
    protected int mPageIndex;
    private int mRealPlaytimes = 1;
    protected int mNeedPlayTimes = 1;
    protected long mOnePicDuration = 2000;
    protected boolean mHadOnLayout = true;

    public ItemMultiLinesPagedText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ItemMultiLinesPagedText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemMultiLinesPagedText(Context context) {
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

        mText = Texts.getText(item);

        if (TextUtils.isEmpty(mText)) {
            if (DBG)
                Log.d(TAG, "text is empty. mOnePicDuration= " + mOnePicDuration);
            removeCallbacks(this);
            postDelayed(this, mOnePicDuration);
            return;
        }

        initDisplay(item);

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
            Log.i(TAG, "setPageText. mPageIndex= " + mPageIndex);
        setText(mPageSplitter.getPages().get(mPageIndex));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (DBG)
            Log.i(TAG, "onLayout. Thread= " + Thread.currentThread());
        if (!mHadOnLayout) {

            composeAndShow();
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

    private void composeAndShow() {
        setVisibility(INVISIBLE);
//            int textBackColor = GraphUtils.parseColor("0xFF004040");
//        if (!TextUtils.isEmpty(item.textBackColor) && !"0xFF000000".equals(item.textBackColor)) {
//            if ("0xFF010000".equals(item.textBackColor)) {
//                textBackColor = GraphUtils.parseColor("0xFF000000");
//            } else {
//                textBackColor = GraphUtils.parseColor(item.textBackColor);
//            }
//        }
//            BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(textBackColor);

        int maxLineNumPerPage = this.getHeight() / this.getLineHeight() + (this.getHeight() % this.getLineHeight() >= (int) this.getPaint().getFontMetrics(null) ? 1 : 0);
        if (maxLineNumPerPage < 1)
            maxLineNumPerPage = 1;//最少显示1行

        if (DBG)
            Log.d(TAG, "onLayout. lineHeight= " + this.getLineHeight() + ", this.height= " + this.getHeight()
                    + ", getLineSpacingExtra= " + getLineSpacingExtra()
                    + ", maxLineNumPerPage= " + maxLineNumPerPage);

        mPageSplitter = new MultilinePageSplitter(maxLineNumPerPage, this);
        mPageSplitter.append(mText);

        setVisibility(VISIBLE);
        mPageIndex = 0;
        setPageText();

        if (mPageSplitter.getPages().size() > 0) {
            stopHandler();
            mHandler = new MTextMarquee(this, mOnePicDuration);
            mHandler.start();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DBG)
            Log.i(TAG, "onAttachedToWindow. layout= " + this.getLayout());

        mHadOnLayout = false;

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DBG)
            Log.i(TAG, "onDetachedFromWindow, mHandler= " + mHandler);

        removeCallbacks(this);

        stopHandler();
    }

    private void stopHandler() {
        if (mHandler != null) {
            mHandler.stop();
            mHandler = null;
        }
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
                    + ", mPageSplitter.getPages().size()= " + mPageSplitter.getPages().size());
        mPageIndex++;
        if (mPageIndex >= mPageSplitter.getPages().size()) {
            if (mRealPlaytimes == mNeedPlayTimes)
                tellListener();

            mRealPlaytimes++;
            mPageIndex = 0;
        }

        if (DBG)
            Log.i(TAG, "next page. mPageIndex=" + mPageIndex);
        setPageText();
    }

    @Override
    public void run() {
        tellListener();
    }

    protected static final class MTextMarquee extends Handler {
        private final static String TAG = "MTextMarquee";
        private static final boolean DBG = false;
        private static final int MESSAGE_TICK = 0x1;
        private long MARQUEE_DELAY;


        private final WeakReference<ItemMultiLinesPagedText> mView;
        private boolean mShouldStop;

        public MTextMarquee(ItemMultiLinesPagedText view, long onePicDuration) {
            mView = new WeakReference<ItemMultiLinesPagedText>(view);
            MARQUEE_DELAY = onePicDuration;
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

            final ItemMultiLinesPagedText view = mView.get();
            if (view != null) {
                sendEmptyMessageDelayed(MESSAGE_TICK, MARQUEE_DELAY);
                view.nextPage();
            }
        }
    }

}
