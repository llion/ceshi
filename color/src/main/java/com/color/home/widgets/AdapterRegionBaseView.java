package com.color.home.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AdapterViewAnimator;

public abstract class AdapterRegionBaseView extends AdapterViewAnimator {
    protected static final String TAG = "AdapterRegionBaseView";
    protected static final boolean DBG = false;

    public AdapterRegionBaseView(Context context) {
        super(context);
    }

    public AdapterRegionBaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // A view flipper should cycle through the views
        // Default to true.
        // loopview is set in the xml.
    }

}
