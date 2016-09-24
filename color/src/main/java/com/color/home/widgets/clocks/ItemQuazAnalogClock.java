package com.color.home.widgets.clocks;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.color.home.AppController;
import com.color.home.ProgramParser;
import com.color.home.R;
import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.Region;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.FinishObserver;
import com.color.home.widgets.ItemData;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;

public class ItemQuazAnalogClock extends View implements ItemData, Runnable, FinishObserver {
    private final static String TAG = "QuazAnalogClock";
    private static final boolean DBG = false;
    private Calendar mCalendar;
    private GradientDrawable mHourHand;
    private GradientDrawable mMinuteHand;
    private GradientDrawable mSecondHand;
    private GradientDrawable mQuadTick;
    private GradientDrawable mDialOuter;
    private GradientDrawable mDial;
    private int backgroundColor;
    private long mDuration = 0;

    private GradientDrawable mFiveTick;//时标
    private GradientDrawable mMinuteTick;//分标
    private int mFiveTickHeight;
    private int mFrameThickness;
//    private static final float sScale = 1.0f;

//    private Drawable mDial_frame;
    private int mDialWidth;
    private int mDialHeight;

    private boolean mAttached;

    private final Handler mHandler = new Handler();
    private float mMinutes;
    private float mHour;
    private String mDate;
    private String mWeek;
    private boolean mChanged;
    private Region mRegion;
    private Item mItem;
    private OnPlayFinishedListener mListener;
    private ProgramParser.AnologClock mAnologClock;
    private ProgramParser.HhourScale mHourScale;
    private ProgramParser.MinuteScale mMinuteScale;
    private ProgramParser.ClockFont mClockFont;

    private int mFlag;
    private boolean hasDate = false, hasWeek = false;
    private SimpleDateFormat mSdfDate;
    private SimpleDateFormat mSdfWeek;
    private String mTzId;
    private static final int FLAG_DATE = 65536;
    private static final int FLAG_WEEK = 262144;

    private int mScaleType;
    private float[] mTranslates;//时标为数字时存放画布偏移量

    Paint mHourPaint;//数字时标画笔
    Paint mTextPaint;//固定文字画笔
    Paint mDatePaint;//日期画笔
    Paint mWeekPaint;//星期画笔

    boolean mSecondsChanged = false;
    float mSecondAngle = 0;
    private int mCenterOffset;
    private boolean[] mHasCalculate = {false, false, false, false};//固定文字、日期、星期、时标为数字时画布偏移量等是否计算过
    private float[] mOrigin = new float[4];

    //夏令时地区ZoneDescripID及对应的AvailableID
    int[] mZoneDescripIds = new int[]{3, 4, 5, 6, 7, 10, 11, 13, 14, 17, 19, 20, 21, 22, 24, 25, 28, 29, 32, 34, 37, 38, 39, 40,
                                        42, 43, 44, 45, 46, 49, 51, 52, 54, 57, 59, 60, 63, 72, 75, 77, 82, 85, 88, 90, 91, 93, 94, 96, 97, 100};
    int[] availableIds = new int[]{0, 0, 0, 0, 9, 14, 0, 10, 0, 29, 8, 41, 3, 0, 26, 21, 34, 0, 1, 39, 19, 21, 24, 40,
                                    14, 24, 13, 2, 14, 34, 18, 12, 20, 0, 0, 6, 4, 8, 6, 8, 1, 0, 5, 7, 2, 3, 8, 9, 4, 1};


    @Override
    public void setRegion(Region region) {
        mRegion = region;

    }

    @Override
    public void setItem(RegionView regionView, Item item) {
        mListener = regionView;
        mItem = item;
//        mScaleType = Integer.parseInt(mItem.s)
    }

    public ItemQuazAnalogClock(Context context, Item item) {
        this(context, null, item);
    }

    public ItemQuazAnalogClock(Context context, AttributeSet attrs, Item item) {
        this(context, attrs, 0, item);
    }

    public ItemQuazAnalogClock(Context context, AttributeSet attrs, int defStyle, Item item) {
        super(context, attrs, defStyle);
        Resources r = context.getResources();

        mItem = item;
        mAnologClock = item.anologClock;
        mHourScale = item.hhourScale;
        mMinuteScale = item.minuteScale;
        mClockFont = mAnologClock.clockFont;

        //background color
        if (!TextUtils.isEmpty(mItem.backcolor) && !"0xFF000000".equals(mItem.backcolor)) {
            if ("0xFF010000".equals(mItem.backcolor)) {
                backgroundColor = GraphUtils.parseColor("0xFF000000");
            } else {
                backgroundColor = GraphUtils.parseColor(item.backcolor);
            }
        } else {
            backgroundColor = GraphUtils.parseColor("0x00000000");
        }

        //play duration
        try {
            mDuration = Long.parseLong(item.duration);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        mDial = (GradientDrawable) r.getDrawable(R.drawable.round_shape);
//        mDialOuter = (GradientDrawable) r.getDrawable(R.drawable.round_frame);

        //时标
        if ("0".equals(mHourScale.shape))//圆形
            mFiveTick = (GradientDrawable) r.getDrawable(R.drawable.round_shape);
        else if ("1".equals(mHourScale.shape))//方形
            mFiveTick = (GradientDrawable) r.getDrawable(R.drawable.square_shape);
        else { //数字
            ProgramParser.HourFont hourFont = mClockFont.hourFont;
            mHourPaint = new Paint();
            mHourPaint.setAntiAlias(false);
            if (hourFont.lfHeight != null)
                mHourPaint.setTextSize(Integer.parseInt(hourFont.lfHeight));
            if ("1".equals(hourFont.lfUnderline)) {
                mHourPaint.setFlags(Paint.UNDERLINE_TEXT_FLAG);
            }
            int style = Typeface.NORMAL;
            if ("1".equals(hourFont.lfItalic)) {
                style = Typeface.ITALIC;
            }
            if ("700".equals(hourFont.lfWeight)) {
                style |= Typeface.BOLD;
            }
            Typeface tf = Typeface.create(AppController.getInstance().getTypeface(hourFont.lfFaceName), style);
            mHourPaint.setTypeface(tf);
            mHourPaint.setTextAlign(Paint.Align.CENTER);
            mHourPaint.setColor(GraphUtils.parseColor(mHourScale.clr));
            mTranslates = new float[24];
        }

        //分标
        if ("0".equals(mMinuteScale.shape))//圆形
            mMinuteTick = (GradientDrawable) r.getDrawable(R.drawable.round_shape);
        else//方形
            mMinuteTick = (GradientDrawable) r.getDrawable(R.drawable.square_shape);
//        mQuadTick = (GradientDrawable) r.getDrawable(R.drawable.round_line_shape);
        mHourHand = (GradientDrawable) r.getDrawable(R.drawable.round_line_shape);
        mMinuteHand = (GradientDrawable) r.getDrawable(R.drawable.round_line_shape);
        mSecondHand = (GradientDrawable) r.getDrawable(R.drawable.round_line_shape);


        //时区
        String tzStr = mItem.timezone;
        if (!TextUtils.isEmpty(tzStr)){
            int idx = tzStr.indexOf(".");
            if (idx != -1){
                if (idx < (tzStr.length() - 2)){
                    int offset;
                    float timezoneFloat = 0.f;
                    tzStr = tzStr.substring(0, idx + 3);
                    try {
                        timezoneFloat = Float.parseFloat(tzStr);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    offset = (int) (timezoneFloat * 60 * 60 * 1000);
                    if (DBG)
                        Log.d(TAG,"timezoneFloat = " + timezoneFloat + ", offset = " + offset);

                    String[] ids = TimeZone.getAvailableIDs(offset);
                    if (ids.length != 0) {
                        String descripId = mItem.zoneDescripId;
                        if (!TextUtils.isEmpty(descripId) && descripId.length()>12) {
                            descripId = descripId.substring(12, descripId.length());
                            if (DBG)
                                Log.d(TAG, "descripId = " + descripId);

                            int j;
                            int desId = 0;
                            try {
                                desId = Integer.parseInt(descripId);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            for (j = 0; j < mZoneDescripIds.length; j++){
                                if (desId == mZoneDescripIds[j])
                                    break;
                            }

                            if (j == mZoneDescripIds.length){//不使用夏令时
                                for (int k = 0; k < ids.length; k++){
                                    if (!TimeZone.getTimeZone(ids[k]).useDaylightTime()){
                                        mTzId = ids[k];
                                        break;
                                    }
                                }
                            }else{//使用夏令时
                                if (ids.length > availableIds[j])
                                    mTzId = String.valueOf(ids[availableIds[j]]);
                            }

                        }
                    }
                }
            }
        }
        if (TextUtils.isEmpty(mTzId)) {
            mTzId = TimeZone.getDefault().getID();
        }
        mCalendar = new GregorianCalendar();
        mCalendar.setTimeZone(TimeZone.getTimeZone(mTzId));
        if (DBG)
            Log.d(TAG, "mTzId = " + mTzId + ", tzStr = " + tzStr + ", mCalendar.getTimeZone() = " + mCalendar.getTimeZone());

        mDialWidth = mDial.getIntrinsicWidth();
        mDialHeight = mDial.getIntrinsicHeight();

        //固定文字画笔
        if (!TextUtils.isEmpty(mItem.text)){//有固定文字
            //初始化文字画笔
            ProgramParser.FixedText fixedText = mClockFont.fixedText;
            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(false);
            if (fixedText.lfHeight != null)
                mTextPaint.setTextSize(Integer.parseInt(fixedText.lfHeight));
            if ("1".equals(fixedText.lfUnderline))
                mTextPaint.setFlags(Paint.UNDERLINE_TEXT_FLAG);
            int style = Typeface.NORMAL;
            if ("1".equals(fixedText.lfItalic)) {
                style = Typeface.ITALIC;
                if (DBG)
                    Log.i(TAG, "style1 = " + style);
            }
            if ("700".equals(fixedText.lfWeight)) {
                style |= Typeface.BOLD;
                if (DBG)
                    Log.i(TAG, "style2 = " + style);
            }
            if (DBG)
                Log.i(TAG, "style = " + style + ", fixedText.lfItalic = " + fixedText.lfItalic + ", fixedText.lfWeight = " + fixedText.lfWeight);
            Typeface tf = Typeface.create(AppController.getInstance().getTypeface(fixedText.lfFaceName), style);
            mTextPaint.setTypeface(tf);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaint.setColor((int)(Long.parseLong(mClockFont.fixedTextColor)) | 0xFF000000);
        }

        try {
            mFlag = Integer.parseInt(mItem.anologClock.flags);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //日期画笔
        if (( mFlag & FLAG_DATE) == FLAG_DATE){//显示日期
            hasDate = true;
            ProgramParser.FixedDate fixedDate = mClockFont.fixedDate;
            mDatePaint = new Paint();
            mDatePaint.setAntiAlias(false);
            if (fixedDate.lfHeight != null)
                mDatePaint.setTextSize(Integer.parseInt(fixedDate.lfHeight));
            if ("1".equals(fixedDate.lfUnderline)) {
                mDatePaint.setFlags(Paint.UNDERLINE_TEXT_FLAG);
            }
            int style = Typeface.NORMAL;
            if ("1".equals(fixedDate.lfItalic)) {
                style = Typeface.ITALIC;
            }
            if ("700".equals(fixedDate.lfWeight)) {
                style |= Typeface.BOLD;
            }
            Typeface tf = Typeface.create(AppController.getInstance().getTypeface(fixedDate.lfFaceName), style);
            mDatePaint.setTypeface(tf);
            mDatePaint.setTextAlign(Paint.Align.CENTER);
            mDatePaint.setColor((int)(Long.parseLong(mClockFont.weekColor)) | 0xFF000000);//LedVision修改星期字体颜色，日期颜色变化
            mSdfDate = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMMdd"));
            mSdfDate.setTimeZone(TimeZone.getTimeZone(mTzId));
        }

        //星期画笔
        if (( mFlag & FLAG_WEEK) == FLAG_WEEK){//显示星期
            hasWeek = true;
            ProgramParser.FixedWeek fixedWeek = mClockFont.fixedWeek;
            mWeekPaint = new Paint();
            mWeekPaint.setAntiAlias(false);
            if (fixedWeek.lfHeight != null)
                mWeekPaint.setTextSize(Integer.parseInt(fixedWeek.lfHeight));
            if ("1".equals(fixedWeek.lfUnderline)) {
                mWeekPaint.setFlags(Paint.UNDERLINE_TEXT_FLAG);
            }
            int style = Typeface.NORMAL;
            if ("1".equals(fixedWeek.lfItalic)) {
                style = Typeface.ITALIC;
            }
            if ("700".equals(fixedWeek.lfWeight)) {
                style |= Typeface.BOLD;
            }
            Typeface tf = Typeface.create(AppController.getInstance().getTypeface(fixedWeek.lfFaceName), style);
            mWeekPaint.setTypeface(tf);
            mWeekPaint.setTextAlign(Paint.Align.CENTER);
            mWeekPaint.setColor((int)(Long.parseLong(mClockFont.dateColor)) | 0xFF000000);//LedVision中第一个DateColor
            mSdfWeek = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEE"));
            mSdfWeek.setTimeZone(TimeZone.getTimeZone(mTzId));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DBG)
            Log.i(TAG, "onAttachedToWindow. , Thread=" + Thread.currentThread());

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

            getContext().registerReceiver(mIntentReceiver, filter, null, mHandler);

            if (DBG)
                Log.i(TAG, "onAttachedToWindow. , mDuration=" + mDuration);
            if (mDuration >= 0){
                removeCallbacks(this);
                postDelayed(this, mDuration);
            }
        }



        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // The time zone may have changed while the receiver wasn't registered, so update the Time

        // Make sure we update to the current time
        onTimeChanged();
        counter.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            counter.cancel();
            removeCallbacks(this);
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float hScale = 1.0f;
        float vScale = 1.0f;

        if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth) {
            hScale = (float) widthSize / (float) mDialWidth;
        }

        if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight) {
            vScale = (float) heightSize / (float) mDialHeight;
        }

        float scale = Math.min(hScale, vScale);
        if (DBG)
            Log.d(TAG, "onMeasure params: " + widthSize + " "
                    + heightSize + " " + hScale + " " + vScale + " " + scale);

        try {
            setMeasuredDimension(resolveSizeAndState((int) (mDialWidth * scale), widthMeasureSpec, 0),
                    resolveSizeAndState((int) (mDialHeight * scale), heightMeasureSpec, 0));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public ItemQuazAnalogClock(Context context) {
        super(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (DBG)
            Log.i(TAG, "onSizeChanged. w, h, oldw, oldh = " + w + "," + h + ", " + oldw + ", " + oldh);

        // Always keep.
        if (w < h) {
            h = w;
        } else {
            w = h;
        }

        mCenterOffset = - w / 16;
        mFiveTickHeight = h / 2 * 1 / 8;
        mFrameThickness = mFiveTickHeight / 3;

        mDial.mutate();//中心圆点
        mDial.setColor(GraphUtils.parseColor(mAnologClock.secondPinClr));
        mDial.setSize(w / 12, h / 12);

//        mDialOuter.mutate();//最外层边框
//        mDialOuter.setSize(w, h);
//        mDialOuter.setStroke(mFrameThickness, 0xFF777777);

        if (mFiveTick != null) {
            mFiveTick.mutate();//12个时标
            mFiveTick.setColor(GraphUtils.parseColor(mHourScale.clr));
            mFiveTick.setSize(Integer.parseInt(mHourScale.width), Integer.parseInt(mHourScale.height));
        }

//        mQuadTick.mutate();//3,6,9,12   四个时标
//        mQuadTick.setSize(w * 1 / 30, h / 2 * 1 / 7);

        //分标
        mMinuteTick.mutate();
        mMinuteTick.setColor(GraphUtils.parseColor(mMinuteScale.clr));
        mMinuteTick.setSize(Integer.parseInt(mMinuteScale.width), Integer.parseInt(mMinuteScale.height));

        mHourHand.mutate();
        mHourHand.setColor(GraphUtils.parseColor(mAnologClock.hourPinClr));
        mHourHand.setSize(w * 1 / 24, h / 2 * 3 / 7);

        mMinuteHand.mutate();
        mMinuteHand.setColor(GraphUtils.parseColor(mAnologClock.minutePinClr));
        mMinuteHand.setSize(w * 1 / 32, h / 2 * 4 / 7);

        mSecondHand.mutate();
        mSecondHand.setColor(GraphUtils.parseColor(mAnologClock.secondPinClr));
        mSecondHand.setSize(w * 1 / 48, h / 2 * 5 / 7);

        mChanged = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean changed = mChanged;
        if (changed) {
            mChanged = false;
        }

        boolean seconds = mSecondsChanged;
        if (seconds) {
            mSecondsChanged = false;
        }
        int availableWidth = this.getMeasuredWidth();
        int availableHeight = this.getMeasuredHeight();
        if (DBG)
            Log.i(TAG,
                    "onDraw: availableWidth = " + availableWidth + ", availableHeight = " + availableHeight + ", width = " + getWidth()
                            + ", height = " + getHeight());

        // final Drawable dial_frame = mDial_frame;
        int w = getWidth();
        int h = getHeight();
        if (w < h) {
            h = w;
        } else {
            w = h;
        }
        int x = w / 2;
        int y = h / 2;
        boolean moved = false;
        int offsetx = getWidth() / 2 - x;
        int offsety = getHeight() / 2 - y;
        if (offsetx != 0 || offsety != 0) {
            moved = true;
        }
        if (moved) {
            canvas.save();
            canvas.translate(offsetx, offsety);
        }
        if (DBG)
            Log.d(TAG,"moved = " + moved + ", offsetx = " + offsetx + ", offsety = " + offsety);

        // Log.d(AnalogClock.DEBUGTAG,"onDraw params: " + availableWidth +" "+ availableHeight + " " +
        // x + " " + y + " " + w + " "+ h + " " + changed);

        // if (availableWidth < w || availableHeight < h) {
        // scaled = true;
        // // float scale = Math.min((float) availableWidth / (float) w,
        // // (float) availableHeight / (float) h);
        // canvas.save();
        // float scale1 = (float) 0.6;
        // float scale2 = (float) 0.8;
        //
        // // Log.d(AnalogClock.DEBUGTAG,"scale params: " + scale1 + " " + scale2);
        // canvas.scale(scale1, scale2, x, y);
        // }
        //

        canvas.drawColor(backgroundColor);

        //固定文字
        if (!TextUtils.isEmpty(mItem.text)){
            if (!mHasCalculate[0]) {//未绘制过固定文字
                try {
                    mOrigin[0] = (x / 3 + Float.parseFloat(mClockFont.fixedText.lfHeight));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mHasCalculate[0] = true;
            }
             canvas.drawText(mItem.text, x, mOrigin[0], mTextPaint);
        }

        //日期
        if (hasDate) {
            if (!mHasCalculate[1]) {//未绘制过日期
                mOrigin[1] = 3 * x / 2;
                mHasCalculate[1] = true;
            }
            canvas.drawText(mDate, x, mOrigin[1], mDatePaint);
        }

        //星期
        if (hasWeek) {
            if (!mHasCalculate[2]) {//未绘制过星期
                mOrigin[2] = 14 * x / 8;
                mHasCalculate[2] = true;
            }
            canvas.drawText(mWeek, x, mOrigin[2], mWeekPaint);
        }

        final Drawable dial = mDial;
        if (changed) {
//            // Log.d(AnalogClock.DEBUGTAG,"Bounds params: " + (x - (w / 2)) + " " + (y - (h / 2)) + " " + ( x + (w / 2)) + " " + (y + (h /
//            // 2)));
            w = dial.getIntrinsicWidth();
            h = dial.getIntrinsicHeight();
            dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
//            // dial_frame.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
//            // Log.d(AnalogClock.DEBUGTAG,"Bounds params: " + (x - (w / 2 + w/10)) + " " + (y - (h / 2 + h/10)) + " " + ( x + (w / 2 +
//            // w/10)) + " " +
//            // (y + (h / 2 + h/10)));
//            // dial_frame.setBounds(x - (w/2 + w/10), y - (h/2 + h/10), x + (w/2 + w/10), y + (h/2 + h/10));
        }
        dial.draw(canvas);

//        final Drawable dialOuter = mDialOuter;
//        if (changed) {
//            // Log.d(AnalogClock.DEBUGTAG,"Bounds params: " + (x - (w / 2)) + " " + (y - (h / 2)) + " " + ( x + (w / 2)) + " " + (y + (h /
//            // 2)));
//            w = dialOuter.getIntrinsicWidth();
//            h = dialOuter.getIntrinsicHeight();
//            dialOuter.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
//            // dial_frame.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
//            // Log.d(AnalogClock.DEBUGTAG,"Bounds params: " + (x - (w / 2 + w/10)) + " " + (y - (h / 2 + h/10)) + " " + ( x + (w / 2 +
//            // w/10)) + " " +
//            // (y + (h / 2 + h/10)));
//            // dial_frame.setBounds(x - (w/2 + w/10), y - (h/2 + h/10), x + (w/2 + w/10), y + (h/2 + h/10));
//        }
//        dialOuter.draw(canvas);
//        // dial_frame.draw(canvas);

        //分标
        canvas.save();
        final Drawable minuteTick = mMinuteTick;
        if (changed) {
            w = minuteTick.getIntrinsicWidth();
            h = minuteTick.getIntrinsicHeight();
            // bottom is the center
            minuteTick.setBounds(x - (w / 2), 0 + 2 * mFrameThickness, x + (w / 2), h + 2 * mFrameThickness);
        }
        for (int i = 1; i <= 72; i++) {//60个分标
            if ((i % 5) != 0) {//时标位置不画
                canvas.rotate(6, x, y);
                minuteTick.draw(canvas);
            }else
                canvas.rotate(6, x, y);
        }
        canvas.restore();

        // Draw five tick.时标
        canvas.save();
        if (!"2".equals(mItem.hhourScale.shape)) {//时标为圆形或方形
            final Drawable fiveTick = mFiveTick;
            if (changed) {
                w = fiveTick.getIntrinsicWidth();
                h = fiveTick.getIntrinsicHeight();
                // bottom is the center
                fiveTick.setBounds(x - (w / 2), 2 * mFrameThickness + minuteTick.getIntrinsicHeight() / 2 - h / 2, x + (w / 2), 2 * mFrameThickness + minuteTick.getIntrinsicHeight() / 2 + h / 2);
            }
            for (int i = 1; i <= 12; i++) {//12个时标
                canvas.rotate(30, x, y);
                fiveTick.draw(canvas);
            }
        }else{//时标为数字
            if (!mHasCalculate[3]){//未计算过画布偏移量
                float r = 0.f;
                try {
                    mOrigin[3] = 3 * Float.parseFloat(mClockFont.hourFont.lfHeight) / 10 + minuteTick.getIntrinsicHeight() / 2 + 2 * mFrameThickness;
                    r= x - mOrigin[3] + 3 * Float.parseFloat(mClockFont.hourFont.lfHeight) / 10;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mTranslates[0] = r * 0.5f;
                mTranslates[1] = (float) (r * (1 - Math.cos(Math.PI / 6)));
                mTranslates[2] = (float) (r * (Math.sin(Math.PI / 3) - 0.5f));
                mTranslates[3] = (float) (r * (Math.cos(Math.PI / 6) - Math.cos(Math.PI / 3)));
                mTranslates[4] = mTranslates[1];
                mTranslates[5] = mTranslates[0];
                mTranslates[6] = - mTranslates[4];
                mTranslates[7] = mTranslates[5];
                mTranslates[8] = - mTranslates[2];
                mTranslates[9] = mTranslates[3];
                mTranslates[10] = - mTranslates[0];
                mTranslates[11] = mTranslates[1];
                mTranslates[12] = - mTranslates[0];
                mTranslates[13] = - mTranslates[1];
                mTranslates[14] = - mTranslates[2];
                mTranslates[15] = - mTranslates[3];
                mTranslates[16] = - mTranslates[4];
                mTranslates[17] = - mTranslates[5];
                mTranslates[18] =  mTranslates[4];
                mTranslates[19] = - mTranslates[5];
                mTranslates[20] =  mTranslates[2];
                mTranslates[21] = - mTranslates[3];
                mTranslates[22] = mTranslates[0];
                mTranslates[23] = - mTranslates[1];
                mHasCalculate[3] = true;
            }
            if (DBG)
                Log.d(TAG,"translate[0] = " + mTranslates[0] + ", " +  mTranslates[1] + ", w = " + w);
            for (int i = 0; i < 23; ) {//12个数字时标
                canvas.translate(mTranslates[i], mTranslates[++ i]);
                canvas.drawText((i + 1) / 2 + "", x, mOrigin[3], mHourPaint);
                i++;
            }
        }
        canvas.restore();

        // Draw quad tick.
//        canvas.save();
//        final Drawable quadTick = mQuadTick;
//        if (changed) {
//            w = quadTick.getIntrinsicWidth();
//            h = quadTick.getIntrinsicHeight();
//            // bottom is the center
//            quadTick.setBounds(x - (w / 2), 0 + 2 * mFrameThickness, x + (w / 2), h + 2 * mFrameThickness);
//        }
//
//        canvas.rotate(-3, x, y);
//        quadTick.draw(canvas);
//
//        canvas.rotate(6, x, y);
//        quadTick.draw(canvas);
//
//        canvas.rotate(-3, x, y);
//        for (int i = 1; i < 4; i++) {
//            canvas.rotate(90, x, y);
//            quadTick.draw(canvas);
//        }
//        canvas.restore();

        // Draw Hour.
        canvas.save();
        canvas.rotate(mHour / 12.0f * 360.0f, x, y);
        final Drawable hourHand = mHourHand;
        if (changed) {
            w = hourHand.getIntrinsicWidth();
            h = hourHand.getIntrinsicHeight();
            if (DBG)
                Log.i(TAG, "onDraw. w,h=" + w + ", " + h + ", Thread=" + Thread.currentThread());
            // bottom is the center
            hourHand.setBounds(x - (w / 2), y - h + mCenterOffset, x + (w / 2), y + mCenterOffset);
        }
        hourHand.draw(canvas);
        canvas.restore();

        // Draw min.
        canvas.save();
        canvas.rotate(mMinutes / 60.0f * 360.0f, x, y);
        final Drawable minuteHand = mMinuteHand;
        if (changed) {
            w = minuteHand.getIntrinsicWidth();
            h = minuteHand.getIntrinsicHeight();
            minuteHand.setBounds(x - (w / 2), y - h + mCenterOffset, x + (w / 2), y + mCenterOffset);
        }
        minuteHand.draw(canvas);
        canvas.restore();

        // Draw sec.
        final Drawable secondHand = mSecondHand;
        canvas.save();
        canvas.rotate(mSecondAngle, x, y);
        if (seconds) {
            w = secondHand.getIntrinsicWidth();
            h = secondHand.getIntrinsicHeight();
            secondHand.setBounds(x - (w / 2), y - h + mCenterOffset, x + (w / 2), y + mCenterOffset);
        }
        secondHand.draw(canvas);
        canvas.restore();

        if (moved) {
            canvas.restore();
        }
    }

    MyCount counter = new MyCount(Integer.MAX_VALUE, 1000);


    public class MyCount extends CountDownTimer {
        public MyCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            counter.start();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            int second = mCalendar.get(Calendar.SECOND);
            if (DBG)
                Log.i(TAG, "onTick: Thread = " + Thread.currentThread() + ", second = " + second);

            mSecondAngle = 6.0f * second;
            mSecondsChanged = true;
            // mChanged = true;
            ItemQuazAnalogClock.this.invalidate();
        }
    }

    private void onTimeChanged() {
        mCalendar.setTimeInMillis(System.currentTimeMillis());
//        mCalendar.setTimeZone(TimeZone.getTimeZone(mTzId));
        if (DBG)
            Log.d(TAG,"mCalendar.getTimeZone().getDisplayName = " + mCalendar.getTimeZone().getDisplayName() + ", useDaylightTime : " + mCalendar.getTimeZone().useDaylightTime());
        int hour = mCalendar.get(Calendar.HOUR);
        int minute = mCalendar.get(Calendar.MINUTE);
        int second = mCalendar.get(Calendar.SECOND);

        mMinutes = minute + second / 60.0f;
        mHour = hour + mMinutes / 60.0f;

        if (hasDate || hasWeek){
            if (hasDate) {
                mDate = mSdfDate.format(mCalendar.getTimeInMillis());
            }
            if (hasWeek) {
                mWeek = mSdfWeek.format(mCalendar.getTimeInMillis());
            }
        }

        if (DBG)
            Log.d(TAG, "mCalendar = " + mCalendar + ", mDate = " + mDate + ", mWeek = " + mWeek + ", second = " + second);
        mChanged = true;

    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = new GregorianCalendar();
                mCalendar.setTimeZone(TimeZone.getTimeZone(mTzId));
                if (DBG)
                    Log.d(TAG, "mCalendar.getTimeZone().getID() = " + mCalendar.getTimeZone().getID());
            }

            onTimeChanged();

            invalidate();
        }
    };

    @Override
    public void notifyPlayFinished() {
        if (mListener != null) {
            if (DBG)
                Log.i(TAG, "tellListener. Tell listener =" + mListener);
            mListener.onPlayFinished(this);
        }
    }

    @Override
    public void run() {
        if (DBG)
            Log.i(TAG, "run. Finish item play due to play length time up");
        notifyPlayFinished();
    }

}