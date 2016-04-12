package com.color.home.widgets.multilines;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ScrollView;
import android.widget.TextView;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.LogFont;
import com.color.home.R;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.PageSplitter;
import com.color.home.widgets.RegionView;

public class ItemMLScrollableText extends ScrollView implements OnPlayFinishObserverable, AnimatorListener, Runnable {
    private static final boolean DBG = false;
    // never public, so that another class won't be messed up.
    private final static String TAG = "ItemMultiLinesScrollableText";

    private TextView mTv;
    private Item mItem;
    private int mHeight;
    private boolean mIsScroll;
    private float mSpeed;
    private boolean mIsHeadConnectTail;
    private int mRepeatCount;
    private boolean mIsScrollByTime;
    private int mPlayLength;
    private boolean mIfSpeedByFrame;
    private float mSpeedByFrame;
    private ObjectAnimator mAnim;
    private OnPlayFinishedListener mListener;
    private String mStringFromFile;
    private boolean mIsDetached;
    private PageSplitter mPageSplitter;
    private TextPaint mTextPaint;
    private String mText;
    private boolean mCenteralAlign;

    public ItemMLScrollableText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    public ItemMLScrollableText(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public ItemMLScrollableText(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public void setItem(RegionView regionView, Item item) {
        mListener = regionView;
        this.mItem = item;
        mTv = (TextView) findViewById(R.id.textView);

        mText = item.getTexts().mText;

        mTv.setText(mText);
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
        
        mCenteralAlign = "1".equals(item.centeralalign);
        if (mCenteralAlign) {
            if (DBG)
                Log.d(TAG, "setItem. [CENTER_HORIZONTAL");
            mTv.setGravity(Gravity.CENTER_HORIZONTAL);
        }
        
        if (DBG)
            Log.i(TAG, "setItem. textview getHeight=" + mTv.getHeight());

        mIsScroll = "1".equals(item.isscroll);

        if (item.speed != null)
            mSpeed = Float.parseFloat(item.speed);
        mIsHeadConnectTail = "1".equals(item.isheadconnecttail);

        if (item.repeatcount != null)
            mRepeatCount = Integer.parseInt(item.repeatcount);
        mIsScrollByTime = "1".equals(item.isscrollbytime);

        if (item.playLength != null)
            mPlayLength = Integer.parseInt(item.playLength);
        mIfSpeedByFrame = "1".equals(item.ifspeedbyframe);

        if (item.speedbyframe != null)
            mSpeedByFrame = Float.parseFloat(item.speedbyframe);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        // Init only once.
        if (mHeight == 0) {

            mHeight = mTv.getHeight();
            int containerHeight = getHeight();

            if (DBG)
                Log.i(TAG, "onLayout. textview height = " + mHeight + ", ItemSingleLineText container, myheight=" + containerHeight);

            /*
             * scrollview height
             */
            mAnim = ObjectAnimator.ofFloat(mTv, "translationY", containerHeight, -mHeight);
            mAnim.setInterpolator(null); // default to linear.

            int distance = containerHeight - (-mHeight);
            // Default to 500
            int duration = 500;

            if (!mIfSpeedByFrame) { // px/sec.
                duration = (int) (1000 * distance / mSpeed);
            } else { // px/frame, 20.0f / sec is assumed.
                duration = (int) (1000 * distance / (mSpeedByFrame * 20.0f));
            }

            if (DBG)
                Log.i(TAG, "onLayout. duration=" + duration + ", distance=" + distance + ", mIsScrollByTime=" + mIsScrollByTime);
            //
            if (mIsScrollByTime) {
                // mPlayLength.
                mAnim.setRepeatCount(ValueAnimator.INFINITE);

                removeCallbacks(this);
                postDelayed(this, mPlayLength);
            } else {
                mAnim.setRepeatCount(mRepeatCount - 1);
                mAnim.addListener(this);
            }
            mAnim.setDuration(duration);
            mAnim.start();
            if (DBG)
                Log.i(TAG, "onLayout. repeat count=" + mAnim.getRepeatCount() + ", filesource=" + mItem.filesource.filepath);
        }

    }

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

    @Override
    public void onAnimationStart(Animator animation) {
        // TODO Auto-generated method stub
        if (DBG)
            Log.i(TAG, "onAnimationStart. animation=" + animation + ", Thread=" + Thread.currentThread());
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (DBG)
            Log.i(TAG, "onAnimationEnd. Finish item play due to play count up, animaiton = " + animation);

        if (!mIsDetached)
            tellListener();
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        if (DBG)
            Log.i(TAG, "onAnimationCancel. animation=" + animation + ", Thread=" + Thread.currentThread());

    }

    @Override
    public void onAnimationRepeat(Animator animation) {
        if (DBG)
            Log.i(TAG, "onAnimationRepeat. animation = " + animation);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsDetached = true;

        boolean removeCallbacks = removeCallbacks(this);
        if (DBG)
            Log.i(TAG, "onDetachedFromWindow. Try to remove call back. result is removeCallbacks=" + removeCallbacks);

        if (mAnim != null) {
            if (DBG)
                Log.i(TAG, "onDetachedFromWindow. mAnim=" + mAnim + ", not null, end it. Thread=" + Thread.currentThread());
            mAnim.end();
            mAnim = null;
        }

    }

    @Override
    public void run() {
        if (DBG)
            Log.i(TAG, "run. Finish item play due to play length time up = " + mPlayLength);
        tellListener();
    }
}