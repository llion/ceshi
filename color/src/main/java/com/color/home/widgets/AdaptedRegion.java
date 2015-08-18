package com.color.home.widgets;

import android.view.View;

import com.color.home.ProgramParser.Region;

public interface AdaptedRegion {

    public abstract void setRegion(PageView pageView, Region region);

    public abstract void showNext();

    public abstract void onPlayFinished(View view);

    public abstract void notifyOnAllPlayed();

    public abstract int getDisplayedChild();

    public abstract ItemsAdapter getAdapter();

}