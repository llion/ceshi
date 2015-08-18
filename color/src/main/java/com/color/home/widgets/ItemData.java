package com.color.home.widgets;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.Region;

public interface ItemData {

    public abstract void setRegion(Region region);

    public abstract void setItem(RegionView regionView, Item item);

}