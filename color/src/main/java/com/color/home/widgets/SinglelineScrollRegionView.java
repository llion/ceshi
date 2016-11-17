package com.color.home.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.color.home.widgets.singleline_scroll.SinglelineScrollSurfaceView;
import com.color.home.widgets.singleline_scroll.SinglineScrollObject;

/**
 * Created by Administrator on 2016/11/11.
 */
public class SinglelineScrollRegionView extends RegionView {
    private final static String TAG = "ScrollRegionView";
    private static final boolean DBG = false;
    private static final boolean DRAW_DBG = false;


    public SinglelineScrollRegionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SinglelineScrollRegionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SinglelineScrollRegionView(Context context) {
        super(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        addScrollView();
    }

    private void addScrollView() {
        if (DBG)
            Log.i(TAG, "addScrollView. ");
        if (mRegion.items.size() > 0) {

            SinglelineScrollSurfaceView singleScrollView = new SinglelineScrollSurfaceView(getContext(), this, new SinglineScrollObject(getContext(), mRegion.items));
            singleScrollView.setItems(mRegion.items);
            addView(singleScrollView);

        } else {
            Log.e(TAG, "items is null.");
        }
    }

    @Override
    public void onPlayFinished(View view) {

        post(new Runnable() {

            @Override
            public void run() {
                if (DBG)
                    Log.i(TAG, "onPlayFinished. ");

                notifyOnAllPlayed();
                ((SinglelineScrollSurfaceView) getChildAt(0)).getRenderer().notFinish();
            }
        });

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
}