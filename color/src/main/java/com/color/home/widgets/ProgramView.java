package com.color.home.widgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.FrameLayout;

import com.color.home.ProgramParser.Page;
import com.color.home.ProgramParser.Program;

public class ProgramView extends FrameLayout implements AdaptedProgram {
    private static final String TAG = "ProgramView";
    private static final boolean DBG = false;

    private final int FLIP_MSG = 1;
    private int mFlipInterval = Integer.MAX_VALUE;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == FLIP_MSG) {
                showNext();
            }
        }
    };

    private Adapter mAdapter;
    // Default to -1, so that we can set the 0.
    private int mDisplayedChild = -1;
    private Program mProgram;

    public ProgramView(Context context) {
        super(context);
    }

    public ProgramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // A view flipper should cycle through the views
        // Default to true.
        // loopview is set in the xml.
    }

    @Override
    public void setProgram(Program program) {
        this.mProgram = program;

        int width = Integer.parseInt(program.info.width);
        int height = Integer.parseInt(program.info.height);
        setLayoutParams(new FrameLayout.LayoutParams(width, height));
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter("com.clt.intent.ACTION_SWITCH_PAGE_BY_NAME");
        getContext().registerReceiver(mColorChangeReceiver, filter);
    }


    @Override
    protected void onAttachedToWindow() {
        registerReceiver();
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // by default when we update running, we want the
        // current view to animate in
        if (mColorChangeReceiver != null)
            getContext().unregisterReceiver(mColorChangeReceiver);

        if (DBG)
            Log.i(TAG, "onDetachedFromWindow. ");
        destroy();
    }

    private void destroy() {
        if (DBG)
            Log.i(TAG, "destroy. ");
        mHandler.removeMessages(FLIP_MSG);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (DBG)
            Log.i(TAG, "setAdapter. adapter=" + adapter);
        mAdapter = adapter;

        setDisplayedChild(0);
        scheduleNext();
    }

    @Override
    public int getFlipInterval() {
        return mFlipInterval;
    }

    @Override
    public void setFlipInterval(int flipInterval) {
        mFlipInterval = flipInterval;
    }

    @Override
    public void showNext() {
        if (DBG)
            Log.i(TAG, "showNext. ");

        final int pagesCount = mAdapter.getCount();
        if (mAdapter == null || pagesCount == 0) {
            if (DBG)
                Log.i(TAG, "showNext. No adapter, mAdapter= " + mAdapter);
            return;
        }

        if (mDisplayedChild == 0 && pagesCount == 1) {
            if (DBG)
                Log.d(TAG, "showNext. [Single page program, don't move on to next.");
            return;
        }

        setDisplayedChild(mDisplayedChild + 1);
        // View currentView = getCurrentView();
        // removeViewAt(0);
        // removeView(currentView);
        scheduleNext();
    }

    private void scheduleNext() {
        if (DBG)
            Log.i(TAG, "scheduleNext. ");

        updateFlipStatus();
        // if the flipper is currently flipping automatically, and showNext() is
        // called
        // we should we should make sure to reset the timer
        mHandler.removeMessages(FLIP_MSG);
        Message msg = mHandler.obtainMessage(FLIP_MSG);
        mHandler.sendMessageDelayed(msg, mFlipInterval);
        if (DBG)
            Log.i(TAG, "scheduleNext. mFlipInterval=" + mFlipInterval);
    }

    private void updateFlipStatus() {
        int displayedChild = getDisplayedChild();
        if (DBG)
            Log.i(TAG, "updateFlipStatus. displayedChild=" + displayedChild);

        Page page = (Page) getAdapter().getItem(displayedChild);
        if ("0".equals(page.looptype)) {
            if (DBG)
                Log.i(TAG, "updateFlipStatus. interval appointed = " + page.appointduration + ", displayedChild=" + displayedChild);
            int interval = Integer.parseInt(page.appointduration);
            setFlipInterval(interval);
        } else {
            setFlipInterval(Integer.MAX_VALUE);
            if (DBG)
                Log.i(TAG, "updateFlipStatus. No appointed interval, manual flip.");
        }
    }

    @Override
    public void setDisplayedChild(int displayedChild) {
        if (DBG)
            Log.i(TAG, "setDisplayedChild. displayedChild=" + displayedChild + ", mDisplayedChild=" + mDisplayedChild);

        if (displayedChild == mDisplayedChild) {
            if (DBG)
                Log.i(TAG, "setDisplayedChild. do nothing, same displayedChild=" + displayedChild);

            return;
        }

        if (DBG)
            Log.d(TAG, "setDisplayedChild. [x" + getAdapter().getCount());
        if (DBG)
            for (int i = 0; i < getAdapter().getCount() ; i ++)
                Log.d(TAG, "child= " + getChildAt(i));

        // loop.
        if (displayedChild >= getAdapter().getCount()) {
            displayedChild = 0;
            if (DBG)
                Log.d(TAG, "setDisplayedChild. [0");
        }

        mDisplayedChild = displayedChild;
        if (DBG)
            Log.d(TAG, "setDisplayedChild. [mDisplayedChild=" + mDisplayedChild);

        // The current page is removed before a new one being added. 
        View curView = getChildAt(0);
        if (DBG)
            Log.d(TAG, "setDisplayedChild. [curView" + curView);
        if (curView != null)
            removeView(curView);
        if (DBG)
            Log.d(TAG, "setDisplayedChild. [removeView=" + curView);

        if (DBG)
            Log.i(TAG, "setDisplayedChild. [2 mDisplayedChild=" + mDisplayedChild);
        addView(getAdapter().getView(mDisplayedChild, null, null));
    }

    @Override
    public int getDisplayedChild() {
        return mDisplayedChild;
    }

    @Override
    public Adapter getAdapter() {
        return mAdapter;
    }

    /*
     * Return true on the page switched to another page, otherwise false.
     */
    @Override
    public void onAllFinished(PageView pageView) {
        if (DBG)
            Log.d(TAG, "onAllFinished. pageView = " + pageView + ", chileAt(0)= " + getChildAt(0));
//        if (pageView != getChildAt(0))
//            return;

        int flipInterval = getFlipInterval();

        if (DBG)
            Log.i(TAG, "onAllFinished. pageView=" + pageView
                    + ", flipInterval=" + flipInterval);

        if (flipInterval == Integer.MAX_VALUE) {
            if (DBG)
                Log.i(TAG, "onAllFinished. pageView=" + pageView
                        + ", was tolled page time up, now flip.");
            showNext();
        } else {
            if (DBG)
                Log.i(TAG, "onAllFinished. pageView");
        }
    }
    private final BroadcastReceiver mColorChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String pageName = intent.getStringExtra("pageName");
            if (DBG)
                Log.d(TAG, "Page change message received. extra= " + pageName);

            if (!TextUtils.isEmpty(pageName)) {
                int pageIndex = findPageIndexByPageName(pageName);
                if (pageIndex >= 0 && pageIndex < mAdapter.getCount()) {
                    setDisplayedChild(pageIndex);

                    if (DBG)
                        Log.d(TAG, "Schedule for the switched page.");

                    scheduleNext();
                } else {
                    Log.w(TAG, "Page not found with page name=" + pageName);
                }
            }

        }
    };

    private int findPageIndexByPageName(String pageName) {
        Log.d(TAG, "findPageIndexByPageName=" + pageName);

        if (mProgram != null && pageName != null && mProgram.pages != null) {

            final int N = mProgram.pages.size();
            for (int i = 0; i < N; i++) {
                if (DBG)
                    Log.d(TAG, "Checkint page with name=" + mProgram.pages.get(i).name);

                if (pageName.equals(mProgram.pages.get(i).name)) {
                    if (DBG)
                        Log.d(TAG, "Found page with name=" + pageName);

                    return i;
                }
            }
        } else {
            Log.e(TAG, " mProgram = " + mProgram + ", pageName=" + pageName + ", pages=" + mProgram.pages);
        }

        // Not found.
        return -1;
    }
}
