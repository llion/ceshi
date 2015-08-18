package com.color.home.widgets.clocks;

import java.util.TimeZone;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.color.home.R;
import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.Region;
import com.color.home.widgets.ItemData;
import com.color.home.widgets.RegionView;

public class ItemQuazAnalogClock extends View implements ItemData {
    private final static String TAG = "QuazAnalogClock";
    private static final boolean DBG = false;
    private Time mCalendar;
    private GradientDrawable mHourHand;
    private GradientDrawable mMinuteHand;
    private GradientDrawable mSecondHand;
    private GradientDrawable mQuadTick;
    private GradientDrawable mDialOuter;
    private GradientDrawable mDial;

    private GradientDrawable mFiveTick;
    private static final float sScale = 1.0f;

    // private Drawable mDial_frame;

    private int mDialWidth;
    private int mDialHeight;

    private boolean mAttached;

    private final Handler mHandler = new Handler();
    private float mMinutes;
    private float mHour;
    private boolean mChanged;
    private Region mRegion;
    private RegionView mRegionView;
    private Item mItem;
    
    private int mScaleType;

    @Override
    public void setRegion(Region region) {
        mRegion = region;

    }

    @Override
    public void setItem(RegionView regionView, Item item) {
        mRegionView = regionView;
        mItem = item;
        
//        mScaleType = Integer.parseInt(mItem.s)
    }

    public ItemQuazAnalogClock(Context context) {
        this(context, null);
    }

    public ItemQuazAnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ItemQuazAnalogClock(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        Resources r = context.getResources();

        mDial = (GradientDrawable) r.getDrawable(R.drawable.round_shape);
        mDialOuter = (GradientDrawable) r.getDrawable(R.drawable.round_frame);
        // mDial_frame = r.getDrawable(R.drawable.clock_frame);
        mFiveTick = (GradientDrawable) r.getDrawable(R.drawable.round_line_shape);
        mQuadTick = (GradientDrawable) r.getDrawable(R.drawable.round_line_shape);
        mHourHand = (GradientDrawable) r.getDrawable(R.drawable.round_line_shape);
        mMinuteHand = (GradientDrawable) r.getDrawable(R.drawable.round_line_shape);
        mSecondHand = (GradientDrawable) r.getDrawable(R.drawable.round_line_shape);

        mCalendar = new Time();

        // mDialWidth = mDial.getIntrinsicWidth();
        // mDialHeight = mDial.getIntrinsicHeight();
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
        }

        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mCalendar = new Time();

        // Make sure we update to the current time
        onTimeChanged();
        counter.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            counter.cancel();
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

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (DBG)
            Log.i(TAG, "onSizeChanged. w, h, oldw, oldh=" + w + "," + h + ", " + oldw + ", " + oldh + ", Thread=" + Thread.currentThread());

        // Always keep.
        if (w < h) {
            h = w;
        } else {
            w = h;
        }

        mCenterOffset = -w / 10;
        mFiveTickHeight = h / 2 * 1 / 8;
        mFrameThickness = mFiveTickHeight / 3;

        mDial.mutate();
        mDial.setSize(w / 7, h / 7);

        mDialOuter.mutate();
        mDialOuter.setSize(w, h);
        mDialOuter.setStroke(mFrameThickness, 0xFF777777);

        mFiveTick.mutate();
        mFiveTick.setSize(w * 1 / 60, mFiveTickHeight);

        mQuadTick.mutate();
        mQuadTick.setSize(w * 1 / 30, h / 2 * 1 / 7);

        mHourHand.mutate();
        mHourHand.setSize(w * 1 / 20, h / 2 * 3 / 7);

        mMinuteHand.mutate();
        mMinuteHand.setSize(w * 1 / 32, h / 2 * 4 / 7);

        mSecondHand.mutate();
        mSecondHand.setSize(w * 1 / 64, h / 2 * 5 / 7);

        mChanged = true;
    }

    boolean mSecondsChanged = false;
    float mSecondAngle = 0;
    private int mCenterOffset;

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
                    "onDraw. canvas, availableWidth=" + availableWidth + ", availableHeight=" + availableHeight + ", width=" + getWidth()
                            + ", height=" + getHeight());

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

        final Drawable dial = mDial;
        if (changed) {
            // Log.d(AnalogClock.DEBUGTAG,"Bounds params: " + (x - (w / 2)) + " " + (y - (h / 2)) + " " + ( x + (w / 2)) + " " + (y + (h /
            // 2)));
            w = dial.getIntrinsicWidth();
            h = dial.getIntrinsicHeight();
            dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
            // dial_frame.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
            // Log.d(AnalogClock.DEBUGTAG,"Bounds params: " + (x - (w / 2 + w/10)) + " " + (y - (h / 2 + h/10)) + " " + ( x + (w / 2 +
            // w/10)) + " " +
            // (y + (h / 2 + h/10)));
            // dial_frame.setBounds(x - (w/2 + w/10), y - (h/2 + h/10), x + (w/2 + w/10), y + (h/2 + h/10));
        }
        dial.draw(canvas);

        final Drawable dialOuter = mDialOuter;
        if (changed) {
            // Log.d(AnalogClock.DEBUGTAG,"Bounds params: " + (x - (w / 2)) + " " + (y - (h / 2)) + " " + ( x + (w / 2)) + " " + (y + (h /
            // 2)));
            w = dialOuter.getIntrinsicWidth();
            h = dialOuter.getIntrinsicHeight();
            dialOuter.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
            // dial_frame.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
            // Log.d(AnalogClock.DEBUGTAG,"Bounds params: " + (x - (w / 2 + w/10)) + " " + (y - (h / 2 + h/10)) + " " + ( x + (w / 2 +
            // w/10)) + " " +
            // (y + (h / 2 + h/10)));
            // dial_frame.setBounds(x - (w/2 + w/10), y - (h/2 + h/10), x + (w/2 + w/10), y + (h/2 + h/10));
        }
        dialOuter.draw(canvas);
        // dial_frame.draw(canvas);

        // Draw five tick.
        canvas.save();
        final Drawable fiveTick = mFiveTick;
        if (changed) {
            w = fiveTick.getIntrinsicWidth();
            h = fiveTick.getIntrinsicHeight();
            // bottom is the center
            fiveTick.setBounds(x - (w / 2), 0 + 2 * mFrameThickness, x + (w / 2), h + 2 * mFrameThickness);
        }
        for (int i = 1; i < 12; i++) {
            canvas.rotate(30, x, y);
            fiveTick.draw(canvas);
        }
        canvas.restore();

        // Draw quad tick.
        canvas.save();
        final Drawable quadTick = mQuadTick;
        if (changed) {
            w = quadTick.getIntrinsicWidth();
            h = quadTick.getIntrinsicHeight();
            // bottom is the center
            quadTick.setBounds(x - (w / 2), 0 + 2 * mFrameThickness, x + (w / 2), h + 2 * mFrameThickness);
        }

        canvas.rotate(-3, x, y);
        quadTick.draw(canvas);

        canvas.rotate(6, x, y);
        quadTick.draw(canvas);

        canvas.rotate(-3, x, y);
        for (int i = 1; i < 4; i++) {
            canvas.rotate(90, x, y);
            quadTick.draw(canvas);
        }
        canvas.restore();

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
            mCalendar.setToNow();

            if (DBG)
                Log.i(TAG, "onTick. millisUntilFinished, Thread=" + Thread.currentThread());
            int second = mCalendar.second;

            mSecondAngle = 6.0f * second;
            mSecondsChanged = true;
            // mChanged = true;
            ItemQuazAnalogClock.this.invalidate();
        }
    }

    private void onTimeChanged() {
        mCalendar.setToNow();

        int hour = mCalendar.hour;
        int minute = mCalendar.minute;
        int second = mCalendar.second;

        mMinutes = minute + second / 60.0f;
        mHour = hour + mMinutes / 60.0f;
        mChanged = true;
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
            }

            onTimeChanged();

            invalidate();
        }
    };
    private int mFiveTickHeight;
    private int mFrameThickness;
}