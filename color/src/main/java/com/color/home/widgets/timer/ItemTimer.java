package com.color.home.widgets.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

import com.color.home.AppController;
import com.color.home.ProgramParser.LogFont;
import com.color.home.ProgramParser.Region;
import com.color.home.ProgramParser.Item;
import com.color.home.R;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.ItemData;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Administrator on 2016/7/8.
 */
public class ItemTimer extends TextView implements ItemData, OnPlayFinishObserverable, Runnable{

    private static final boolean DBG = false;
    private static final String TAG  = "ItemTimer";

    private Item mItem;
    private boolean mAttached;
//    private MyCount counter;
    private OnPlayFinishedListener mListener;
    private long mPlayTime;

    private Long mDuration;
    private String mPrefix;
    private int mStyle;//show style
    int isMultiLine = 0;
    int isEndToTime;

    private String mDay;
    private String mHour;
    private String mMinute;
    private String mSecond;
    private boolean mHasDay;
    private boolean mHasHour;
    private boolean mHasMinute;
    private boolean mHasSecond;
    private long mTimeCount;

    private ForegroundColorSpan mDaySpan;
    private ForegroundColorSpan mHourSpan;
    private ForegroundColorSpan mMinuteSpan;
    private ForegroundColorSpan mSecondSpan;

    private SpannableStringBuilder mSpannable;

    private long mNowDate;
    private long mBetweenDate;
//    private Calendar mCalendar;
    private SimpleDateFormat mSdf;
    private Date mEndDate;

    private Resources mResources;



    public ItemTimer(Context context) {
        super(context);
        mResources = context.getResources();
        if (DBG)
            Log.d(TAG, "locale = " + mResources.getConfiguration().locale + " getLanguage = " + Locale.getDefault().getLanguage());
    }

    public ItemTimer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemTimer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    public void setItem(RegionView regionView, Item item) {

        mListener = regionView;
        mItem = item;
        mSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        mPlayTime = 0;

        setTextColor(GraphUtils.parseColor(item.textColor));

        int backColor;
        if (!TextUtils.isEmpty(item.backcolor) && !"0xFF000000".equals(item.backcolor)) {
            if ("0xFF010000".equals(item.backcolor)) {
                backColor = GraphUtils.parseColor("0xFF000000");
            } else {
                backColor = GraphUtils.parseColor(item.backcolor);
            }
        } else {
            backColor = GraphUtils.parseColor("0x00000000");
        }
        setBackgroundColor(backColor);
        getPaint().setAntiAlias(false);
//        setTextScaleX(1.2f);
        LogFont logFont = item.logfont;
        setTextSize(Integer.parseInt(logFont.lfHeight));

        // Typeface & font.
        int typeface = Typeface.NORMAL;
        if ("1".equals(logFont.lfItalic)) {
            typeface = Typeface.ITALIC;
            if(DBG)
                Log.d(TAG, "typeface1:" + typeface);
        }
        if ("700".equals(logFont.lfWeight)) {
            typeface |= Typeface.BOLD;
            if(DBG)
                Log.d(TAG, "typeface2:" + typeface);
        }
        if ("1".equals(logFont.lfUnderline)) {
            getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        }
        if(DBG)
            Log.d(TAG, "lfFaceName:" + logFont.lfFaceName);
        Typeface fontFamily = AppController.getInstance().generateTypeface(logFont.lfFaceName);
        setTypeface(Typeface.create(fontFamily, typeface));

        //BeToEndTime
        try {
            isEndToTime = Integer.parseInt(item.beToEndTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Duration
        try {
            mDuration = Long.parseLong(item.duration);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //EndDateTime
        try {
            mEndDate = mSdf.parse(item.endDateTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //IsMultiLine
        if (!TextUtils.isEmpty(item.isMultiLine))
            isMultiLine = Integer.parseInt(item.isMultiLine);

        //Day
        if ("1".equals(item.isShowDayCount)){
            mHasDay = true;
            mDaySpan = new ForegroundColorSpan(GraphUtils.parseColor(item.dayCountColor));
        }
        //Hour
        if ("1".equals(item.isShowHourCount)){
            mHasHour = true;
            mHourSpan = new ForegroundColorSpan(GraphUtils.parseColor(item.hourCountColor));
        }
        //Minute
        if ("1".equals(item.isShowMinuteCount)){
            mHasMinute = true;
            mMinuteSpan = new ForegroundColorSpan(GraphUtils.parseColor(item.minuteCountColor));
        }
        //Second
        if ("1".equals(item.isShowSecondCount)){
            mHasSecond = true;
            mSecondSpan = new ForegroundColorSpan(GraphUtils.parseColor(item.secondCountColor));
        }

        if (DBG)
            Log.d(TAG,"mHasDay = " + mHasDay + ", mHasHour = " + mHasHour + ", mHasMinute = " + mHasMinute + ", mHasSecond = " + mHasSecond);

        //Style
        if (!TextUtils.isEmpty(item.style)){
            try {
                mStyle = Integer.parseInt(item.style);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (isMultiLine == 0)
            setSingleLine();
        setGravity(Gravity.CENTER);

        mSpannable = new SpannableStringBuilder();
        mPrefix = item.prefix;
        if (!TextUtils.isEmpty(mPrefix))
            mSpannable.append(mPrefix);

//        createCalendar(TimeZone.getDefault().getID());
//        counter = new MyCount(mDuration, 1000);

    }

    @Override
    public void setRegion(Region region) {

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DBG)
            Log.d(TAG,"mPlayTime = " + mPlayTime + ", onAttachedToWindow");
        if (!mAttached){
            registerReceiver();
//            counter.start();
            if (mDuration > 0)
                onTimeChanged();
            else
                tellListener();
            mAttached = true;
        }

    }

    private void registerReceiver() {

        IntentFilter filter = new IntentFilter();

//        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

        getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());

    }

    private void onTimeChanged() {

//        mCalendar.setTimeInMillis(System.currentTimeMillis());
//        mNowDate = mCalendar.getTimeInMillis();

        mNowDate = System.currentTimeMillis();
        if (isEndToTime == 0) //正计时
            mBetweenDate = mNowDate - mEndDate.getTime();
        else //倒计时
            mBetweenDate = mEndDate.getTime() - mNowDate;

        if (mSpannable != null && (mSpannable.length() > mPrefix.length()))
            mSpannable = mSpannable.replace(0, mSpannable.length(), mPrefix);

        //Day
        if (mHasDay) {
            mTimeCount = mBetweenDate / (1000 * 60 * 60 * 24);

            if (isMultiLine == 1 && !TextUtils.isEmpty(mPrefix))
                mSpannable.append("\n");

            try {
                mDay = String.format("%d", mTimeCount < 0 ? 0 : mTimeCount);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (DBG)
                Log.d(TAG,"isEndToTime = " + isEndToTime + ", mDay = " + mDay);

            mSpannable.append(mDay);
            mSpannable.setSpan(mDaySpan, mSpannable.length() - mDay.length(), mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            mSpannable.append(mResources.getString(R.string.day));

        }
        //Hour
        if (mHasHour){
            if (mHasDay)
                mTimeCount = mBetweenDate / (1000 * 60 * 60) % 24;
            else
                mTimeCount = mBetweenDate / (1000 * 60 * 60);

            if (isMultiLine == 1 && !TextUtils.isEmpty(mSpannable))
                mSpannable.append("\n");

            try {
                mHour = String.format("%02d", mTimeCount < 0 ? 0 : mTimeCount);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (DBG)
                Log.d(TAG,"isEndToTime = " + isEndToTime + ", mHour = " + mHour);

            mSpannable.append(mHour);
            mSpannable.setSpan(mHourSpan, mSpannable.length() - mHour.length(), mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (mStyle == 0){//888天88小时08分08秒
                mSpannable.append(mResources.getString(R.string.hour));
            } else if (mHasMinute || mHasSecond){//888天88:08:08
                mSpannable.append(":");
            }

        }
        //Minute
        if (mHasMinute){

            if (mHasHour) {
                mTimeCount = mBetweenDate / (1000 * 60) % 60;
            } else {
                if (mHasDay)
                    mTimeCount = mBetweenDate / (1000 * 60) % (60 * 24);
                else
                    mTimeCount = mBetweenDate / (1000 * 60);

                if (isMultiLine == 1 && !TextUtils.isEmpty(mSpannable))
                    mSpannable.append("\n");
            }

            try {
                mMinute = String.format("%02d", mTimeCount < 0 ? 0 : mTimeCount);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (DBG)
                Log.d(TAG,"isEndToTime = " + isEndToTime + ", mMinute = " + mMinute);

            mSpannable.append(mMinute);
            mSpannable.setSpan(mMinuteSpan, mSpannable.length() - mMinute.length(), mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (mStyle == 0){//888天88小时08分08秒
                mSpannable.append(mResources.getString(R.string.minute));
            } else if (mHasSecond){//888天88:08:08
                mSpannable.append(":");
            }

        }
        //Second
        if (mHasSecond){

            if (mHasMinute) {
                mTimeCount = mBetweenDate / 1000 % 60;
            } else if (mHasHour) {
                mTimeCount = mBetweenDate / 1000 % (60 * 60);
            } else {
                if (mHasDay)
                    mTimeCount = mBetweenDate / 1000 % (60 * 60 * 24);
                else
                    mTimeCount = mBetweenDate / 1000;

                if (isMultiLine == 1 && !TextUtils.isEmpty(mSpannable))
                    mSpannable.append("\n");
            }

            try {
                mSecond = String.format("%02d", mTimeCount < 0 ? 0 : mTimeCount);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (DBG)
                Log.d(TAG,"isEndToTime = " + isEndToTime + ", mSecond = " + mSecond);

            mSpannable.append(mSecond);
            mSpannable.setSpan(mSecondSpan, mSpannable.length() - mSecond.length(), mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (mStyle == 0)//888天88小时08分08秒
                mSpannable.append(mResources.getString(R.string.second));
        }

        setText(mSpannable);

        if (DBG)
            Log.d(TAG,"mPlayTime = " + mPlayTime + ", mDuration = " + mDuration);
        if (mPlayTime == mDuration){
            tellListener();
        }
        removeCallbacks(this);
        postDelayed(this, 1000);
        mPlayTime += 1000;


    }

//    public class MyCount extends CountDownTimer {
//        public MyCount(long millisInFuture, long countDownInterval) {
//            super(millisInFuture, countDownInterval);
//        }
//
//        @Override
//        public void onFinish() {
//            if (DBG)
//                Log.d(TAG, "onFinish ");
//            tellListener();
//        }
//
//        @Override
//        public void onTick(long millisUntilFinished) {
//            if (DBG)
//                    Log.d(TAG, "coming mTicker," + " isEndToTime = " + isEndToTime + ", mDuration = " + mDuration
//                            + ", second remain = " + millisUntilFinished / 1000 + ", millisUntilFinished = " + millisUntilFinished);
//
//            onTimeChanged();
//
//        }
//    }

//    private void createCalendar(String timeZone) {
//        if (timeZone != null) {
//            mCalendar = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
//        } else {
//            mCalendar = Calendar.getInstance();
//        }
//        if (DBG)
//            Log.d(TAG," timeZone = " + timeZone + ", day = " + mCalendar.get(Calendar.DAY_OF_MONTH) + ", hour = " + mCalendar.get(Calendar.HOUR));
//
//    }

    @Override
    public void setListener(OnPlayFinishedListener listener) {
        mListener = listener;
    }

    @Override
    public void removeListener(OnPlayFinishedListener listener) {
        mListener = null;
    }

    private void tellListener() {
        if (DBG)
            Log.i(TAG,"mPlayTime = " +  mPlayTime + ", mListener = " + mListener);
        if (mListener != null) {
            if (DBG)
                Log.i(TAG, "tellListener. Tell listener = " + mListener);
            mListener.onPlayFinished(this);
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                String timeZone = intent.getStringExtra("time-zone");
                if (DBG)
                    Log.d(TAG,"timeZone = " + timeZone);

//                createCalendar(timeZone);
                try {
                    mSdf.setTimeZone(TimeZone.getTimeZone(timeZone));
                    mEndDate = mSdf.parse(mItem.endDateTime);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }
            onTimeChanged();
        }
    };

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DBG)
            Log.d(TAG,"mPlayTime = " + mPlayTime + ", onDetachedFromWindow, this = " + this);
        if (mAttached){
//            counter.cancel();
            removeCallbacks(this);
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }

    }

    @Override
    public void run() {
        onTimeChanged();
    }
}
