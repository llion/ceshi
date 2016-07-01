package com.color.home.widgets.clocks;

/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewDebug.ExportedProperty;
import android.widget.TextView;

import com.color.home.AppController;
import com.color.home.ProgramParser.DigitalClock;
import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.Region;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.RegionView;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * <p>
 * <code>TextClock</code> can display the current date and/or time as a formatted string.
 * </p>
 * 
 * <p>
 * This view honors the 24-hour format system setting. As such, it is possible and recommended to provide two different formatting patterns:
 * one to display the date/time in 24-hour mode and one to display the date/time in 12-hour mode. Most callers will want to use the
 * defaults, though, which will be appropriate for the user's locale.
 * </p>
 * 
 * <p>
 * It is possible to determine whether the system is currently in 24-hour mode by calling {@link #is24HourModeEnabled()}.
 * </p>
 * 
 * <p>
 * The rules used by this widget to decide how to format the date and time are the following:
 * </p>
 * <ul>
 * <li>In 24-hour mode:
 * <ul>
 * <li>Use the value returned by {@link #getFormat24Hour()} when non-null</li>
 * <li>Otherwise, use the value returned by {@link #getFormat12Hour()} when non-null</li>
 * <li>Otherwise, use a default value appropriate for the user's locale, such as {@code h:mm a}</li>
 * </ul>
 * </li>
 * <li>In 12-hour mode:
 * <ul>
 * <li>Use the value returned by {@link #getFormat12Hour()} when non-null</li>
 * <li>Otherwise, use the value returned by {@link #getFormat24Hour()} when non-null</li>
 * <li>Otherwise, use a default value appropriate for the user's locale, such as {@code HH:mm}</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * <p>
 * The {@link CharSequence} instances used as formatting patterns when calling either {@link #setFormat24Hour(CharSequence)} or
 * {@link #setFormat12Hour(CharSequence)} can contain styling information. To do so, use a {@link android.text.Spanned} object. Note that if
 * you customize these strings, it is your responsibility to supply strings appropriate for formatting dates and/or times in the user's
 * locale.
 * </p>
 * 
 * @attr ref android.R.styleable#TextClock_format12Hour
 * @attr ref android.R.styleable#TextClock_format24Hour
 * @attr ref android.R.styleable#TextClock_timeZone
 */
public class ItemTextClock extends TextView {
    /**
     * The default formatting pattern in 12-hour mode. This pattern is used if {@link #setFormat12Hour(CharSequence)} is called with a null
     * pattern or if no pattern was specified when creating an instance of this class.
     * 
     * This default pattern shows only the time, hours and minutes, and an am/pm indicator.
     * 
     * @see #setFormat12Hour(CharSequence)
     * @see #getFormat12Hour()
     * 
     */
    public static final CharSequence DEFAULT_FORMAT_12_HOUR = "MM/dd/yy h:mm:ssaa";

    /**
     * The default formatting pattern in 24-hour mode. This pattern is used if {@link #setFormat24Hour(CharSequence)} is called with a null
     * pattern or if no pattern was specified when creating an instance of this class.
     * 
     * This default pattern shows only the time, hours and minutes.
     * 
     * @see #setFormat24Hour(CharSequence)
     * @see #getFormat24Hour()
     * 
     */
    public static final CharSequence DEFAULT_FORMAT_24_HOUR = "MM/dd/yy HH:mmaa";
    private static final boolean DBG = false;
    private static final String TAG  = "ItemTextClock";

    private CharSequence mFormat12 = DEFAULT_FORMAT_12_HOUR;
    private CharSequence mFormat24 = DEFAULT_FORMAT_24_HOUR;

    private CharSequence mFormat;
    private int mFormatType;
    private boolean mHasSeconds;

    private boolean mAttached;

    private Calendar mTime;
    private String mTimeZone;

    // private final ContentObserver mFormatChangeObserver = new ContentObserver(new Handler()) {
    // @Override
    // public void onChange(boolean selfChange) {
    // chooseFormat();
    // onTimeChanged();
    // }
    //
    // @Override
    // public void onChange(boolean selfChange, Uri uri) {
    // chooseFormat();
    // onTimeChanged();
    // }
    // };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                final String timeZone = intent.getStringExtra("time-zone");
                createTime(timeZone);
            }
            onTimeChanged();
        }
    };

    private final Runnable mTicker = new Runnable() {
        public void run() {
            onTimeChanged();

            long now = SystemClock.uptimeMillis();
            long next = now + (1000 - now % 1000);

            getHandler().postAtTime(mTicker, next);
        }
    };

    private int mFlag;

    /**
     * Creates a new clock using the default patterns for the current locale.
     * 
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     */
    @SuppressWarnings("UnusedDeclaration")
    public ItemTextClock(Context context) {
        super(context);
        init();
    }

    /**
     * Creates a new clock inflated from XML. This object's properties are intialized from the attributes specified in XML.
     * 
     * This constructor uses a default style of 0, so the only attribute values applied are those in the Context's Theme and the given
     * AttributeSet.
     * 
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the view
     */
    @SuppressWarnings("UnusedDeclaration")
    public ItemTextClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Creates a new clock inflated from XML. This object's properties are intialized from the attributes specified in XML.
     * 
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the view
     * @param defStyle
     *            The default style to apply to this view. If 0, no style will be applied (beyond what is included in the theme). This may
     *            either be an attribute resource, whose value will be retrieved from the current theme, or an explicit style resource
     */
    public ItemTextClock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextClock, defStyle, 0);
        // try {
        // mFormat12 = a.getText(R.styleable.TextClock_format12Hour);
        // mFormat24 = a.getText(R.styleable.TextClock_format24Hour);
        // mTimeZone = a.getString(R.styleable.TextClock_timeZone);
        // } finally {
        // a.recycle();
        // }

        init();
    }

    private void init() {
        // if (mFormat12 == null || mFormat24 == null) {
        // LocaleData ld = LocaleData.get(getContext().getResources().getConfiguration().locale);
        // if (mFormat12 == null) {
        // mFormat12 = ld.timeFormat12;
        // }
        // if (mFormat24 == null) {
        // mFormat24 = ld.timeFormat24;
        // }
        // }

        createTime(mTimeZone);
        // Wait until onAttachedToWindow() to handle the ticker
        chooseFormat(false);
    }

    private void createTime(String timeZone) {
        if (timeZone != null) {
            mTime = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
        } else {
            mTime = Calendar.getInstance();
        }
    }

    /**
     * Returns the formatting pattern used to display the date and/or time in 12-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.
     * 
     * @return A {@link CharSequence} or null.
     * 
     * @see #setFormat12Hour(CharSequence)
     * @see #is24HourModeEnabled()
     */
    public CharSequence getFormat12Hour() {
        return mFormat12;
    }

    /**
     * <p>
     * Specifies the formatting pattern used to display the date and/or time in 12-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.
     * </p>
     * 
     * <p>
     * If this pattern is set to null, {@link #getFormat24Hour()} will be used even in 12-hour mode. If both 24-hour and 12-hour formatting
     * patterns are set to null, the default pattern for the current locale will be used instead.
     * </p>
     * 
     * <p>
     * <strong>Note:</strong> if styling is not needed, it is highly recommended you supply a format string generated by
     * {@link DateFormat#getBestDateTimePattern(java.util.Locale, String)}. This method takes care of generating a format string adapted to
     * the desired locale.
     * </p>
     * 
     * 
     * @param format
     *            A date/time formatting pattern as described in {@link DateFormat}
     * 
     * @see #getFormat12Hour()
     * @see #is24HourModeEnabled()
     * @see DateFormat#getBestDateTimePattern(java.util.Locale, String)
     * @see DateFormat
     * 
     * @attr ref android.R.styleable#TextClock_format12Hour
     */
    public void setFormat12Hour(CharSequence format) {
        mFormat12 = format;

        chooseFormat();
        onTimeChanged();
    }

    /**
     * Returns the formatting pattern used to display the date and/or time in 24-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.
     * 
     * @return A {@link CharSequence} or null.
     * 
     * @see #setFormat24Hour(CharSequence)
     * @see #is24HourModeEnabled()
     */
    @ExportedProperty
    public CharSequence getFormat24Hour() {
        return mFormat24;
    }

    /**
     * <p>
     * Specifies the formatting pattern used to display the date and/or time in 24-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.
     * </p>
     * 
     * <p>
     * If this pattern is set to null, {@link #getFormat24Hour()} will be used even in 12-hour mode. If both 24-hour and 12-hour formatting
     * patterns are set to null, the default pattern for the current locale will be used instead.
     * </p>
     * 
     * <p>
     * <strong>Note:</strong> if styling is not needed, it is highly recommended you supply a format string generated by
     * {@link DateFormat#getBestDateTimePattern(java.util.Locale, String)}. This method takes care of generating a format string adapted to
     * the desired locale.
     * </p>
     * 
     * @param format
     *            A date/time formatting pattern as described in {@link DateFormat}
     * 
     * @see #getFormat24Hour()
     * @see #is24HourModeEnabled()
     * @see DateFormat#getBestDateTimePattern(java.util.Locale, String)
     * @see DateFormat
     * 
     * @attr ref android.R.styleable#TextClock_format24Hour
     */
    public void setFormat24Hour(CharSequence format) {
        mFormat24 = format;

        chooseFormat();
        onTimeChanged();
    }

    /**
     * Indicates whether the system is currently using the 24-hour mode.
     * 
     * When the system is in 24-hour mode, this view will use the pattern returned by {@link #getFormat24Hour()}. In 12-hour mode, the
     * pattern returned by {@link #getFormat12Hour()} is used instead.
     * 
     * If either one of the formats is null, the other format is used. If both formats are null, the default formats for the current locale
     * are used.
     * 
     * @return true if time should be displayed in 24-hour format, false if it should be displayed in 12-hour format.
     * 
     * @see #setFormat12Hour(CharSequence)
     * @see #getFormat12Hour()
     * @see #setFormat24Hour(CharSequence)
     * @see #getFormat24Hour()
     */
    public boolean is24HourModeEnabled() {
        return true;
        // return DateFormat.is24HourFormat(getContext());
    }

    /**
     * Indicates which time zone is currently used by this view.
     * 
     * @return The ID of the current time zone or null if the default time zone, as set by the user, must be used
     * 
     * @see TimeZone
     * @see java.util.TimeZone#getAvailableIDs()
     * @see #setTimeZone(String)
     */
    public String getTimeZone() {
        return mTimeZone;
    }

    /**
     * Sets the specified time zone to use in this clock. When the time zone is set through this method, system time zone changes (when the
     * user sets the time zone in settings for instance) will be ignored.
     * 
     * @param timeZone
     *            The desired time zone's ID as specified in {@link TimeZone} or null to user the time zone specified by the user (system
     *            time zone)
     * 
     * @see #getTimeZone()
     * @see java.util.TimeZone#getAvailableIDs()
     * @see TimeZone#getTimeZone(String)
     * 
     * @attr ref android.R.styleable#TextClock_timeZone
     */
    public void setTimeZone(String timeZone) {
        mTimeZone = timeZone;

        createTime(timeZone);
        onTimeChanged();
    }

    /**
     * Selects either one of {@link #getFormat12Hour()} or {@link #getFormat24Hour()} depending on whether the user has selected 24-hour
     * format.
     * 
     * Calling this method does not schedule or unschedule the time ticker.
     */
    private void chooseFormat() {
        chooseFormat(true);
    }

    /**
     * Returns the current format string. Always valid after constructor has finished, and will never be {@code null}.
     * 
     * @hide
     */
    public CharSequence getFormat() {
        return mFormat;
    }

    /**
     * Selects either one of {@link #getFormat12Hour()} or {@link #getFormat24Hour()} depending on whether the user has selected 24-hour
     * format.
     * 
     * @param handleTicker
     *            true if calling this method should schedule/unschedule the time ticker, false otherwise
     */
    private void chooseFormat(boolean handleTicker) {
        // final boolean format24Requested = is24HourModeEnabled();
        //
        // // LocaleData ld = LocaleData.get(getContext().getResources().getConfiguration().locale);
        // if (format24Requested) {
        // weekFormat = mFormat24;
        // // weekFormat = abc(mFormat24, mFormat12, ld.timeFormat24);
        // } else {
        // weekFormat = mFormat12;
        // // weekFormat = abc(mFormat12, mFormat24, ld.timeFormat12);
        // }

        mFormat = parseFormat();

        // Previously, we has seconds.
        boolean hadSeconds = mHasSeconds;

        // Now we has seconds?
        mHasSeconds = DateFormat.hasSeconds(mFormat);

        if (handleTicker && mAttached && hadSeconds != mHasSeconds) {
            if (hadSeconds)
                getHandler().removeCallbacks(mTicker);
            else
                mTicker.run();
        }
    }

    // /**
    // * Returns a if not null, else return b if not null, else return c.
    // */
    // private static CharSequence abc(CharSequence a, CharSequence b, CharSequence c) {
    // return a == null ? (b == null ? c : b) : a;
    // }

    private CharSequence parseFormat() {
        CharSequence format = "";
        if(DBG)
            Log.d(TAG, "default dateFormat : " + DateFormat.getTimeFormatString(this.getContext()));

//        if(mFormatType == 3){
//            return DateFormat.getTimeFormatString(this.getContext());
//        }
//         year exist.
        if(DBG)
            Log.e(TAG, "weekFormat Type : " + mFormatType);

        boolean hasYear = false;
        if ((mFlag & FLAG_YEAR) == FLAG_YEAR) {
            hasYear = true;
            if ((mFlag & YY) == YY) {
                format = format + "yy";
            } else {
                format = format + "yyyy";
            }
        }

        boolean hasMon = false;
        if ((mFlag & FLAG_MONTH) == FLAG_MONTH) {
            hasMon = true;
            if (hasYear) {
                format = format + "-";
            }
            format = format + "MM";
        }

        boolean hasDate = false;
        if ((mFlag & FLAG_DATE) == FLAG_DATE) {
            hasDate = true;
            if (hasYear || hasMon) {
                format = format + "-";
            }
            format = format + "dd";
        }

        if(mFormatType == 2){
            String s = format.toString().replace('-', '/');
            //move year str to the end
            if(hasYear) {
                s += ("/" + s.split("/")[0]);
                s = s.substring(s.indexOf('/') + 1);
            }
            format = s;
        }

        boolean hasWeekDay = false;
        if ((mFlag & FLAG_WEEK) == FLAG_WEEK) {
            hasWeekDay = true;
            if (hasYear || hasMon) {
                format = format + " ";
            }
            format = format + "EEEE";
        }

        boolean hasHour = false;
        if ((mFlag & FLAG_HOUR) == FLAG_HOUR) {
            hasHour = true;
            if (hasYear || hasMon || hasDate || hasWeekDay) {
                format = format + " ";
            }

            if ((mFlag & HOURS_24) == HOURS_24)
                format = format + "kk";
            else
                format = format + "hh";
        }

        boolean hasMin = false;
        if ((mFlag & FLAG_MIN) == FLAG_MIN) {
            hasMin = true;
            if (hasHour) {
                format = format + ":";
            }else{
                format = format + " ";
            }

            format = format + "mm";
        }

        boolean hasSec = false;
        if ((mFlag & FLAG_SEC) == FLAG_SEC) {
            hasSec = true;
            if (hasMin) {
                format = format + ":";
            }else{
                format = format + " ";
            }

            format = format + "ss";
        }

//        boolean hasAMPM = false;
//        if ((mFlag & FLAG_AMPM) == FLAG_AMPM && (mFlag & HOURS_24) != HOURS_24) {
//            hasAMPM = true;
//            if (hasHour || hasMin || hasSec) {
//                format = format + " ";
//            }
//
//            format = format + "AA";
//        }

        if(DBG)
            Log.d(TAG, "date Format generate : " + format );

        switch (mFormatType){
            case 1: {
                return format;
            }
            case 2:{
                return format;
            }
            case 3: {
//                return DateFormat.getLongDateFormat(getContext()).toString();
                return DateFormat.getBestDateTimePattern(Locale.getDefault(), format.toString().replace("-", ""));
//                return DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMMyyyy");
            }

        }
//        if(mFormatType == 3) {
//            if(DBG)
//                Log.d(TAG, "mFormatType == 3, change date format");
//            return DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyyEEEMMMMdHHmmss");
//        }

        return DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyyEEEEMMMMddkkmmss");
    }

    // 12, YYYY == 0;
    private static final int HOURS_24 = 2048;
    private static final int YY = 4096;

    private static final int FLAG_YEAR = 1;
    private static final int FLAG_MONTH = 2;
    private static final int FLAG_DATE = 4;
    private static final int FLAG_HOUR = 8;
    private static final int FLAG_MIN = 16;
    private static final int FLAG_SEC = 32;

    private static final int FLAG_WEEK = 512;
    private static final int FLAG_AMPM = 1024;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        getPaint().setAntiAlias(false);
        if (!mAttached) {
            mAttached = true;

            registerReceiver();
            // registerObserver();

            createTime(mTimeZone);

            if (mHasSeconds) {
                mTicker.run();
            } else {
                onTimeChanged();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mAttached) {
            unregisterReceiver();
            // unregisterObserver();

            getHandler().removeCallbacks(mTicker);

            mAttached = false;
        }
    }

    private void registerReceiver() {
        final IntentFilter filter = new IntentFilter();

        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

        getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
    }

    // private void registerObserver() {
    // final ContentResolver resolver = getContext().getContentResolver();
    // resolver.registerContentObserver(Settings.System.CONTENT_URI, true, mFormatChangeObserver);
    // }

    private void unregisterReceiver() {
        getContext().unregisterReceiver(mIntentReceiver);
    }

    // private void unregisterObserver() {
    // final ContentResolver resolver = getContext().getContentResolver();
    // resolver.unregisterContentObserver(mFormatChangeObserver);
    // }

    private void onTimeChanged() {
        mTime.setTimeInMillis(System.currentTimeMillis());
        setText(DateFormat.format(mFormat, mTime));

    }

    public void setRegion(Region region) {
        // TODO Auto-generated method stub
    }

    public void setItem(RegionView regionView, Item item) {
        DigitalClock digitalClock = item.digitalClock;
        mFlag = Integer.parseInt(digitalClock.flags);
        if(DBG)
            Log.d(TAG, "digitalclock flag : " + digitalClock.flags);
        mFormatType = Integer.parseInt(digitalClock.type);

        // <BackColor>0xFF00FF80</BackColor>
        setBackgroundColor(GraphUtils.parseColor(item.backcolor));

        // <ftColor>4294967295</ftColor>
        setTextColor((int)(Long.parseLong(digitalClock.ftColor)) | 0xFF000000);
        // It's not 0xFFFFFFFF, so don't use the following.
        // setTextColor(GraphUtils.parseColor(digitalClock.ftColor) | 0xFF000000);

        // We take the 0xFF000000 (BLACK) to transparent.
        if (!TextUtils.isEmpty(item.backcolor) && !"0xFF000000".equals(item.backcolor))
            setBackgroundColor(GraphUtils.parseColor(item.backcolor));
        else if("0xFF000000".equals(item.backcolor))
            setBackgroundColor(GraphUtils.parseColor("0x00000000"));

        // Size.
        int size = Integer.parseInt(digitalClock.ftSize);
        setTextSize(TypedValue.COMPLEX_UNIT_PX, size);

        // Typeface & font.
        int typeface = Typeface.NORMAL;
        if ("1".equals(digitalClock.bItalic)) {
            typeface = Typeface.ITALIC;
        }
        if ("1".equals(digitalClock.bBold)) {
            typeface |= Typeface.BOLD;
        }
        if ("1".equals(digitalClock.bUnderline)) {
            getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        }
        if(DBG)
            Log.d(TAG, "font family:" + digitalClock.name + " font type: "+ typeface);
        Typeface fontFamily = AppController.getInstance().generateTypeface(digitalClock.name);
        setTypeface(Typeface.create(fontFamily, typeface));

        setGravity(Gravity.CENTER);
        chooseFormat();
        onTimeChanged();
    }
}
