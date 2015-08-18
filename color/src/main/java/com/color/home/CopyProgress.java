package com.color.home;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.color.home.R.layout;
import com.color.widgets.floating.FloatingLayout;

public class CopyProgress {
    private final static String TAG = "CopyProgress";
    private static final boolean DBG = false;

    private MainActivity mActivity;
    private FloatingLayout mFl;

    public CopyProgress(MainActivity context) {
        mActivity = context;
    }

    void addProgress() {
        if (DBG)
            Log.d(TAG, "addProgress. [");

        if (mFl == null) {
            mFl = new FloatingLayout(mActivity.getApplicationContext());
            mFl.addView(LayoutInflater.from(mActivity).inflate(layout.layout_copy_progress, null));
        }

        mFl.showLayout();
        // No use, as will be obscured by GLSurfaceView.
        // ViewParent parent = mActivity.mContentVG.getParent();
        // if (parent instanceof FrameLayout) {
        // ((FrameLayout)parent).addView(mProgressView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        // }
    }

    void removeProgress() {
        if (DBG)
            Log.d(TAG, "removeProgress. [");

        mFl.hideLayout();
        // if (mProgressView != null && mProgressView.getParent() != null) {
        // ViewGroup vg = (ViewGroup) (mProgressView.getParent());
        // vg.removeView(mProgressView);
        //
        // mProgressView = null;
        // }
    }
}