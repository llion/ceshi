package com.color.widgets.floating;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class FloatingLayout extends FrameLayout {
    // never public, so that another class won't be messed up.
    private final static String TAG = "FloatingLayout";
    private static final boolean DBG = false;

    public static interface OnLayoutChangedListener {

        public void onLayoutShown(FloatingLayout fLayout);

        public void onLayoutHidden(FloatingLayout fLayout);

    }

    protected Context mContext;

    private WindowManager.LayoutParams mLayoutParams;

    private List<OnLayoutChangedListener> mOnLayoutChangedListeners = null;

    private boolean mIsShown;

    public FloatingLayout(Context context) {
        this(context, null);
    }

    public FloatingLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mContext = context;

        initMembers();
    }

    private void initMembers() {
        mLayoutParams = generateWindowLayoutParameter();

        mOnLayoutChangedListeners = new ArrayList<OnLayoutChangedListener>();

        setFocusable(false);
        setFocusableInTouchMode(false);

//        setBackgroundColor(0xFFFF0000);
    }

    protected WindowManager.LayoutParams generateWindowLayoutParameter() {
        WindowManager.LayoutParams lp =
                new WindowManager.LayoutParams();

        lp.type = (WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                | WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY);
        lp.format = PixelFormat.TRANSPARENT;

        lp.gravity = Gravity.LEFT | Gravity.TOP;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;

        lp.x = 0;
        lp.y = 0;

        lp.flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        return lp;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return false;
    }

    public boolean isShown() {
        return mIsShown;
    }

    public void updateLayoutPosition(int x, int y) {
        mLayoutParams.x = x;
        mLayoutParams.y = y;

        if (mIsShown == false) {
            showLayout();
            return;
        }

        WindowManager winmgr =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (winmgr == null) {
            return;
        }

        winmgr.updateViewLayout(this, mLayoutParams);
    }

    public void showLayout() {
        if (mIsShown) {
            return;
        }

        if (mLayoutParams == null) {
            return;
        }

        WindowManager winmgr =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (winmgr == null) {
            return;
        }

        winmgr.addView(this, mLayoutParams);

        mIsShown = true;

        if (mOnLayoutChangedListeners != null) {
            for (OnLayoutChangedListener l : mOnLayoutChangedListeners) {
                Log.d(TAG, "l = " + l);
                l.onLayoutShown(this);
            }
        }
    }

    public void hideLayout() {
        if (mIsShown == false) {
            return;
        }

        WindowManager winmgr =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (winmgr == null) {
            return;
        }

        winmgr.removeView(this);

        mIsShown = false;

        if (mOnLayoutChangedListeners != null) {
            for (OnLayoutChangedListener l : mOnLayoutChangedListeners) {
                if (DBG)
                    Log.d(TAG, "l = " + l);
                l.onLayoutHidden(this);
            }
        }
    }

    public void addOnLayoutChangedListener(OnLayoutChangedListener l) {
        if (DBG)
            Log.d(TAG, "l = " + l);
        if (mOnLayoutChangedListeners != null) {
            mOnLayoutChangedListeners.add(l);
        }
    }

    public void removeOnLayoutChangedListener(OnLayoutChangedListener l) {
        if (DBG)
            Log.d(TAG, "l = " + l);
        if (mOnLayoutChangedListeners != null) {
            mOnLayoutChangedListeners.remove(l);
        }
    }

    public void clearOnLayoutChangedListener() {
        if (mOnLayoutChangedListeners != null) {
            mOnLayoutChangedListeners.clear();
        }
    }

}