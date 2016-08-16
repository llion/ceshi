package com.color.home.widgets.weather;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.Region;
import com.color.home.widgets.ItemData;
import com.color.home.widgets.RegionView;

public class ItemWeatherInfo extends LinearLayout implements ItemData {
    private final static String TAG = "ItemWeatherInfo";
    private static final boolean DBG = false;

    private Item mItem;
    private Region mRegion;

    public ItemWeatherInfo(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ItemWeatherInfo(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemWeatherInfo(Context context) {
        super(context);
    }

    @Override
    public void setRegion(Region region) {
        mRegion = region;
    }

    @Override
    public void setItem(RegionView regionView, Item item) {
        mItem = item;

        // createWeatherLayout(getContext(), this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }


}
