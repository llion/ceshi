package com.color.home.widgets;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.Region;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AnalogClock;

public class ItemClockView extends AnalogClock implements ItemData {

    private Region mRegion;
    private Item mItem;
    private RegionView mRegionView;

    public ItemClockView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ItemClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemClockView(Context context) {
        super(context);
    }

    @Override
    public void setRegion(Region region) {
        mRegion = region;
        
    }

    @Override
    public void setItem(RegionView regionView, Item item) {
        mRegionView = regionView;
        mItem = item;
        
    }

}
