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
import com.color.home.widgets.FinishObserver;
import com.color.home.widgets.ItemData;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;

public class ItemSmoothAnalogClock extends View implements ItemData, Runnable, FinishObserver {
    private final static String TAG = "SmoothAnalogClock";
    private static final boolean DBG = false;
    private Time mCalendar;
    private GradientDrawable mHourHand;
    private GradientDrawable mMinuteHand;
    private GradientDrawable mSecondHand;

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
    private OnPlayFinishedListener mListener;

    private long mDuration = 0;
    
    @Override
    public void setRegion(Region region) {
        mRegion = region;
        
    }

    @Override
    public void setItem(RegionView regionView, Item item) {
        mListener = regionView;
        mRegionView = regionView;
        mItem = item;
        
        try {
            mDuration = Long.parseLong(mItem.duration);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

    }
    
    public ItemSmoothAnalogClock(Context context) {
        this(context, null);
    }

    public ItemSmoothAnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    private GradientDrawable mMinTick;
    private GradientDrawable mQuadTick;

    public ItemSmoothAnalogClock(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        Resources r = context.getResources();

        // mDial_frame = r.getDrawable(R.drawable.clock_frame);
        mMinTick = (GradientDrawable) r.getDrawable(R.drawable.sharp_line_shape);
        mQuadTick = (GradientDrawable) r.getDrawable(R.drawable.sharp_line_shape);
        mHourHand = (GradientDrawable) r.getDrawable(R.drawable.sharp_line_shape);
        mMinuteHand = (GradientDrawable) r.getDrawable(R.drawable.sharp_line_shape);
        mSecondHand = (GradientDrawable) r.getDrawable(R.drawable.sharp_line_shape);

        mCalendar = new Time();

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

            if (mDuration >= 0){
                removeCallbacks(this);
                postDelayed(this, mDuration);
            }
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

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (DBG)
            Log.i(TAG, "onSizeChanged. w, h, oldw, oldh=" + w + "," + h + ", " + oldw + ", " + oldh + ", Thread=" + Thread.currentThread());
        
        if (w < h) {
            h = w;
        } else {
            w = h;
        }

        mCenterOffset = w / 2 * 1 / 6;

        mMinTick.mutate();
        mMinTick.setSize(w * 1 / 64, h / 2 * 1 / 16);

        mQuadTick.mutate();
        mQuadTick.setSize(w * 1 / 20, h / 2 * 1 / 8);

        mHourHand.mutate();
        mHourHand.setSize(w * 1 / 20, h / 2 * 2 / 3);

        mMinuteHand.mutate();
        mMinuteHand.setSize(w * 1 / 32, h / 2 * 3 / 4);

        mSecondHand.mutate();
        mSecondHand.setSize(w * 1 / 64, h / 2);

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

        canvas.save();
        final Drawable minTick = mMinTick;
        if (changed) {
            w = minTick.getIntrinsicWidth();
            h = minTick.getIntrinsicHeight();
            // bottom is the center
            minTick.setBounds(x - (w / 2), 0, x + (w / 2), h);
        }
        for (int i = 0; i < 60; i++) {
            canvas.rotate(6, x, y);
            minTick.draw(canvas);
        }
        canvas.restore();

        canvas.save();
        final Drawable quadTick = mQuadTick;
        if (changed) {
            w = quadTick.getIntrinsicWidth();
            h = quadTick.getIntrinsicHeight();
            // bottom is the center
            quadTick.setBounds(x - (w / 2), 0, x + (w / 2), h);
        }
        for (int i = 0; i < 12; i++) {
            canvas.rotate(30, x, y);
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

    MyCount counter = new MyCount(Integer.MAX_VALUE, 100);

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

            long currentTimeMillis = System.currentTimeMillis();
            long milisecond = currentTimeMillis % 1000;
            if (DBG) {
                Log.i(TAG,
                        "onTick. currentTimeMillis=" + currentTimeMillis + ", milisecond=" + milisecond + ", Thread="
                                + Thread.currentThread());
            }

//            0.001 * 1 / 360f.
            mSecondAngle = (currentTimeMillis % 60000) / 60000.0f * 360.0f;
//                    + milisecond * 6.f / 1000.f;
            mSecondsChanged = true;
            // mChanged = true;
            ItemSmoothAnalogClock.this.invalidate();
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