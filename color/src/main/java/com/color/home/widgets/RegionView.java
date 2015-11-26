package com.color.home.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.Region;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.multilines.ItemMLScrollMultipic2View;
import com.color.home.widgets.multilines.ItemMLScrollableText;
import com.color.home.widgets.singleline.MovingTextUtils;
import com.color.home.widgets.singleline.localscroll.SLTextSurfaceView;

import java.util.Random;

public class RegionView extends FrameLayout implements OnPlayFinishedListener, AdaptedRegion {
    // never public, so that another class won't be messed up.
    private final static String TAG = "RegionView";
    private static final boolean DBG = false;

    private static final int[] sAnimationTypes = { 20, 21, 22, 23, 24, 25, 26, 27, 31, 38, 39, 40, 41, 42 };

    private Region mRegion;
    private PageView mPageView;

    private ShapeDrawable mDrawable;
    private ItemsAdapter mItemsAdapter;

    Random mRand = new Random();

    // Default to -1, so that we can set the 0.
    private int mDisplayedChild = -1;
    private ValueAnimator mCustomAppearingAnim;
    private ObjectAnimator customDisappearingAnim;
    private int mRegionWidth;
    private int mRegionHeight;
    public int getRegionHeight() {
        return mRegionHeight;
    }

    private boolean mIsAttached;

    private static class BorderShapeDrawable extends ShapeDrawable {
        private Paint mStrokePaint = new Paint();

        public BorderShapeDrawable(int width, int color) {
            super(new RectShape());
            mStrokePaint.setStyle(Paint.Style.STROKE);
            mStrokePaint.setStrokeWidth(width);
            mStrokePaint.setColor(color);
        }

        @Override
        protected void onDraw(Shape s, Canvas c, Paint p) {
            // s.draw(c, p);
            s.draw(c, mStrokePaint);
        }
    }

    public RegionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param context
     */
    public RegionView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.color.home.widgets.AdaptedRegion#setRegion(com.color.home.widgets .PageView, com.color.home.ProgramParser.Region)
     */
    @Override
    public void setRegion(PageView pageView, Region region) {
        final LayoutTransition transitioner = new LayoutTransition();
        setLayoutTransition(transitioner);
        transitioner.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        transitioner.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        transitioner.disableTransitionType(LayoutTransition.CHANGING);
        transitioner.disableTransitionType(LayoutTransition.DISAPPEARING);

        if (DBG)
            Log.i(TAG, "setRegion. pageView, region, transitioner=" + transitioner);

        if (region.rect != null && !"0".equals(region.rect.borderwidth))
            mDrawable = new BorderShapeDrawable(Integer.parseInt(region.rect.borderwidth), GraphUtils.parseColor(region.rect.bordercolor));

        this.mPageView = pageView;
        this.mRegion = region;
        // Donnot call it too later, i.e., later than setupItems.
        // XXX>>>>><<<<
        // setOnHierarchyChangeListener(this);

        setupRegionLayout();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mIsAttached = true;
        // Must setupItems after the window attached, otherwise, there no is initial appearing animation.
        setupItems();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttached = false;

        if (DBG)
            Log.d(TAG, "onDetachedFromWindow. [");
//        removeCallbacks(this);
    }

    private void setupAppearingTransition(LayoutTransition transition) {
        transition.setAnimator(LayoutTransition.APPEARING, mCustomAppearingAnim);
        if (DBG)
            new Exception().printStackTrace();

        if (DBG)
            Log.d(TAG, "setupAppearingTransition. [mCustomAppearingAnim=" + mCustomAppearingAnim
                    + ", this=" + this
                    + ", transition=" + transition);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (DBG)
            Log.i(TAG, "onLayout. changed, left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom + ", Thread="
                    + Thread.currentThread());
        if (mDrawable != null)
            mDrawable.setBounds(0, 0, right - left, bottom - top);
    }

    private void setupRegionLayout() {
        setRegionWidth(Integer.parseInt(mRegion.rect.width));
        setRegionHeight(Integer.parseInt(mRegion.rect.height));
        int x = Integer.parseInt(mRegion.rect.x);
        int y = Integer.parseInt(mRegion.rect.y);
        if (DBG)
            Log.i(TAG, "setupRegionLayout. width=" + getRegionWidth()
                    + ", height=" + getRegionHeight()
                    + ", x=" + x
                    + ", y=" + y);
        setLayoutParams(new AbsoluteLayout.LayoutParams(getRegionWidth(), getRegionHeight(), x, y));
    }

    private void setupItems() {
        if (DBG)
            Log.i(TAG, "setupItems. ");
        if (mRegion.items.size() > 0) {
            if (DBG)
                Log.i(TAG, "setupItems. [");
            setAdapter(new ItemsAdapter(getContext(), mRegion.items, mRegion, this));
        } else {
            Log.e(TAG, "setupItems. [XNo item.");
        }
    }

    private void setAdapter(ItemsAdapter itemsAdapter) {
        mItemsAdapter = itemsAdapter;

        setDisplayedChild(0);
    }

    // @Override
    // public void onAnimationStart(Animator animation) {
    // if (DBG)
    // Log.i(TAG, "onAnimationStart. ");
    // }
    //
    // @Override
    // public void onAnimationEnd(Animator animation) {
    // if (DBG)
    // Log.i(TAG, "onAnimationEnd. ");
    //
    // Object targetView = ((ObjectAnimator) animation).getTarget();
    // if (DBG)
    // Log.i(TAG, "onAnimationEnd. Remove target view=" + targetView);
    // if (targetView instanceof ViewGroup) {
    // ViewGroup vg = (ViewGroup) targetView;
    // vg.removeAllViewsInLayout();
    // removeViewInLayout(vg);
    // }
    // }
    //
    // @Override
    // public void onAnimationCancel(Animator animation) {
    // if (DBG)
    // Log.i(TAG, "onAnimationCancel. ");
    // }
    //
    // @Override
    // public void onAnimationRepeat(Animator animation) {
    // if (DBG)
    // Log.i(TAG, "onAnimationRepeat. ");
    // }

    @Override
    public void showNext() {
        if (!mIsAttached) {
            if (DBG)
                Log.d(TAG, "showNext. [Was detached, do not showNext. RegionView=" + this);
            return;
        }
        // Changing while Adding
        // PropertyValuesHolder pvhLeft = PropertyValuesHolder.ofInt("left", 0,
        // 1);
        // PropertyValuesHolder pvhTop = PropertyValuesHolder.ofInt("top", 0,
        // 1);
        // PropertyValuesHolder pvhRight = PropertyValuesHolder.ofInt("right",
        // 0,
        // 1);
        // PropertyValuesHolder pvhBottom = PropertyValuesHolder.ofInt("bottom",
        // 0, 1);
        /*
         * PropertyValuesHolder pvhScaleX = PropertyValuesHolder.ofFloat("scaleX", 0f, 1f); PropertyValuesHolder pvhScaleY =
         * PropertyValuesHolder.ofFloat("scaleY", 1f, 0f, 1f);
         */
        // final ObjectAnimator changeIn =
        // ObjectAnimator.ofPropertyValuesHolder(
        // this, pvhLeft, pvhTop, pvhRight, pvhBottom);
        /*
         * changeIn.addListener(new AnimatorListenerAdapter() { public void onAnimationEnd(Animator anim) { View view = (View)
         * ((ObjectAnimator) anim).getTarget(); view.setScaleX(1f); view.setScaleY(1f); } });
         */

        // ObjectAnimator scaleXIn = ObjectAnimator.ofFloat(this, "scaleX", 0f,
        // 1f);

        // PropertyValuesHolder pvhLeft = PropertyValuesHolder.ofInt("left", 0,
        // 100);
        // PropertyValuesHolder pvhScaleX =
        // PropertyValuesHolder.ofFloat("scaleX", 0f, 1f);
        // final ObjectAnimator changeIn =
        // ObjectAnimator.ofPropertyValuesHolder(this, pvhScaleX);
        // setInAnimation(changeIn);

        if (DBG)
            Log.i(TAG, "showNext. region id=" + mRegion.id);
        setDisplayedChild(mDisplayedChild + 1);
    }

    @Override
    public void onPlayFinished(final View view) {
        if (DBG)
            Log.i(TAG, "OnPlayFinished. view = " + view + ", getDisplayedChild()=" + getDisplayedChild()
                    + ", getAdapter().getCount()=" + getAdapter().getCount());

        post(new Runnable() {

            @Override
            public void run() {
                if (DBG)
                    Log.i(TAG, "run , " + "view instanceof ItemVideoView=" + (view instanceof ItemVideoView) + ", Thread=" + Thread.currentThread() + ", this=" + this);

                if (lastItemPlayed()) {
                    if (DBG)
                        Log.d(TAG, "run. [all played.");
                    // Must notifyOnAllPlayed before show Next.
                    // Because, the Page View could be removed, before showNext.
                    // And we will not display the next if the region view itself
                    // was detached.
                    notifyOnAllPlayed();
                } else {
                    if (DBG)
                        Log.i(TAG, "Continue play region next item, onPlayFinished.  Thread=" + Thread.currentThread());
                }

                if (view instanceof  ItemVideoView) {
                    if (DBG) {
                        Log.d(TAG, "is ItemVideoView.");
                    }
                    if (((ItemVideoView)view).ismIsLoop() && ((ItemVideoView)view).ismSeekable()) {
                        if (DBG) {
                            Log.d(TAG, "The video can loop itself, and there is only one item (ismIsLoop()), do not show next view.");
                        }
                        return;
                    }
                }

                // When this view is detached, showNext will be NOP, as there is isAttached flag check.
                showNext();

            }

        });

    }

    @Override
    public ItemsAdapter getAdapter() {
        return mItemsAdapter;
    }

    // @Override
    // public void onAnimationStart(Animator animation) {
    // if (DBG)
    // Log.i(TAG, "onAnimationStart. ");
    // }
    //
    // @Override
    // public void onAnimationEnd(Animator animation) {
    // if (DBG)
    // Log.i(TAG, "onAnimationEnd. ");
    //
    // Object targetView = ((ObjectAnimator) animation).getTarget();
    // if (DBG)
    // Log.i(TAG, "onAnimationEnd. Remove target view=" + targetView);
    // if (targetView instanceof ViewGroup) {
    // ViewGroup vg = (ViewGroup) targetView;
    // vg.removeAllViewsInLayout();
    // removeViewInLayout(vg);
    // }
    // }
    //
    // @Override
    // public void onAnimationCancel(Animator animation) {
    // if (DBG)
    // Log.i(TAG, "onAnimationCancel. ");
    // }
    //
    // @Override
    // public void onAnimationRepeat(Animator animation) {
    // if (DBG)
    // Log.i(TAG, "onAnimationRepeat. ");
    // }

    private void setDisplayedChild(int displayedChild) {
        if (DBG)
            Log.i(TAG, "setDisplayedChild. displayedChild=" + displayedChild);

        if (displayedChild == mDisplayedChild) {
            if (DBG)
                Log.i(TAG, "setDisplayedChild. do nothing, same displayedChild=" + displayedChild);

            return;
        }

        // This differs from the program view on switching the pages.
        // if there is only one page, no regenerate. add/remove the same contented page.
        // Otherwise, the region inside the page, will be regenerated, and the user
        // could see initial blank to content transition.
        if (displayedChild >= getAdapter().getCount()) {
            mDisplayedChild = 0;
        } else {
            mDisplayedChild = displayedChild;
        }

        // View curView = getChildAt(0);
        // if (curView != null) {
        // removeView(curView);
        // }

        // Even, if there is only one item, we generate/add another item,
        // then remove the first one (though the content is identical).
        // This results in a transition animation one the only(same) item content.
        // This is what the user would like to have.
        View view = getAdapter().getView(mDisplayedChild, null, null);
        if (DBG)
            Log.i(TAG, "setDisplayedChild., new view=" + view + ", Thread=" + Thread.currentThread()
                    + ", instanceof ItemVideoView=" + (view instanceof ItemVideoView));

        boolean noAnimation = false;
        View curView = getChildAt(0);
        if (curView != null && (curView instanceof SLTextSurfaceView)) {
            noAnimation = true;
        }
        // || view instanceof ItemSingleLineText
        if (view instanceof SLTextSurfaceView
                || view instanceof ItemMLScrollMultipic2View
                || view instanceof ItemMLScrollableText
                || view instanceof ItemVideoView) {
            if (DBG)
                Log.d(TAG, "setDisplayedChild. [No animation true.");
            noAnimation = true;
        }

        Item item = mRegion.items.get(mDisplayedChild);
        int type = Integer.parseInt(item.ineffect.Type);
        if (!noAnimation && type != 0 && !"1".equals(item.isscroll)) {
            if (DBG)
                Log.d(TAG, "setDisplayedChild. [type=" + type);
            mCustomAppearingAnim = getAnimationFor(type, item);
            if (mCustomAppearingAnim != null) {
                // mCustomAppearingAnim.setStartDelay(0L);
                if (DBG)
                    Log.d(TAG, "setDisplayedChild. [mCustomAppearingAnim=" + mCustomAppearingAnim);

                mCustomAppearingAnim.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        // If there is only one item, the remove old view will remove this item, which
                        // results in empty content.
                        if (DBG)
                            Log.d(TAG, "onAnimationEnd. [animation=" + animation);

                        // MUST not remove the view in this handler, otherwise STACKOVERFLOW.!!!
                        // removeOldView();
                        RegionView.this.post(new Runnable() {

                            @Override
                            public void run() {
                                removeOldView();
                            }

                        });
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        if (DBG)
                            Log.d(TAG, "onAnimationCancel. [animation=" + animation);
                        super.onAnimationCancel(animation);
                    }

                });
            }
        } else {
            mCustomAppearingAnim = null;
            if (DBG)
                Log.d(TAG, "setDisplayedChild. [No animation, view=" + view);
        }

        setupAppearingTransition(getLayoutTransition());

        addView(view);

        // Must remove old view after adding view. Otherwise, the remove old view
        // is inconsistent between animation ended and this one.
        // and result in removeOldView logic differ in condition of
        // there is only a first child view on initial start of the region.
        if (mCustomAppearingAnim == null || noAnimation) {
            removeOldView();
        }

        if (DBG)
            Log.d(TAG, "setDisplayedChild. [add view=" + view);

    }

    private ValueAnimator getAnimationFor(int type, Item item) {
        long duration = Long.parseLong(item.outeffect.Time);
        // long duration = Long.parseLong(item.ineffect.Time);
        if (DBG)
            Log.i(TAG, "getAnimationFor. duration=" + duration);

        PropertyValuesHolder left2Right = PropertyValuesHolder.ofFloat("translationX", -getRegionWidth(), 0f);
        PropertyValuesHolder right2Left = PropertyValuesHolder.ofFloat("translationX", getRegionWidth(), 0f);
        PropertyValuesHolder up2Down = PropertyValuesHolder.ofFloat("translationY", -getRegionHeight(), 0f);
        PropertyValuesHolder down2Up = PropertyValuesHolder.ofFloat("translationY", getRegionHeight(), 0f);
        // final ObjectAnimator changeIn =
        // ObjectAnimator.ofPropertyValuesHolder(
        // this, pvhLeft, pvhTop, pvhRight, pvhBottom);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 0f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 0f, 1f);

        // Random.
        if (type == 1) {
            // type = mRand.nextInt((48 - 2) + 1) + 2;
            type = sAnimationTypes[mRand.nextInt(sAnimationTypes.length)];
            if (DBG) {
                Log.d(TAG, "getAnimationFor. [animation random type=" + type);
            }
        }

        ValueAnimator customAppearingAnim = null;
        if (type == 20) {
            customAppearingAnim = ObjectAnimator.ofFloat(null, "translationY", getRegionHeight(), 0f);
        } else if (type == 21) {
            customAppearingAnim = ObjectAnimator.ofFloat(null, "translationY", -getRegionHeight(), 0f);
        } else if (type == 22) {
            customAppearingAnim = ObjectAnimator.ofFloat(null, "translationX", getRegionWidth(), 0f);
        } else if (type == 23) {
            // Must set this.
            customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, left2Right);
            // Diag.
        } else if (type == 24) {
            customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, right2Left, down2Up);
        } else if (type == 25) {
            customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, left2Right, down2Up);
        } else if (type == 26) {
            customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, right2Left, up2Down);
        } else if (type == 27) {
            customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, left2Right, up2Down);
        } else if (type == 31) {
            PropertyValuesHolder fadeIn = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
            customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, fadeIn);
        } else if (type == 38) {
            customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY);
        } else if (type == 39) {
            customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY, right2Left, down2Up);
        } else if (type == 40) {
            customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY, left2Right, down2Up);
        } else if (type == 42) { // invert 42 / 41 according to the test.
            customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY, right2Left, up2Down);
        } else if (type == 41) {
            customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY, left2Right, up2Down);
        }

        if (customAppearingAnim != null) {
            if (DBG)
                Log.i(TAG, "getAnimationFor. customAppearingAnim duration=" + duration);
            customAppearingAnim.setDuration(duration);
        }

        return customAppearingAnim;
    }

    @Override
    public int getDisplayedChild() {
        return mDisplayedChild;
    }

    // Don't use onDraw, it's never called in a ViewGroup.
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (DBG)
            Log.i(TAG, "onDraw. canvas");

        if (mDrawable != null)
            mDrawable.draw(canvas);
    }

    /*
     * MSUT BE RUN FROM UI THREAD!!!!
     */
    @Override
    public void notifyOnAllPlayed() {
        if (DBG)
            Log.i(TAG, "notifyOnAllPlayed. this=" + this);

        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalAccessError("Not from UI thread, cannot remove view.");
        }

        mPageView.onAllPlayed(this);
    }

    /**
     * Only happens after the next item was added.
     */
    private void removeOldView() {
        if (getChildCount() < 2) {
            if (DBG)
                Log.d(TAG, "setDisplayedChild. [First item, do not remove after animation end. getChildCount()=" + getChildCount());
            return;
        }

        View curView = getChildAt(0);
        if (DBG)
            Log.d(TAG, "removeOldView. [old view=" + curView);
        if (curView != null)
            removeView(curView);
    }

    public boolean lastItemPlayed() {
        return getDisplayedChild() >= getAdapter().getCount() - 1;
    }

    public int getRegionWidth() {
        return mRegionWidth;
    }

    public void setRegionWidth(int regionWidth) {
//        if (regionWidth % 2 == 1) {
//            mRegionWidth = regionWidth - 1;
//        } else {
            mRegionWidth = MovingTextUtils.evenIt(regionWidth); // TODO, check sanity.
//        }
    }

    private void setRegionHeight(int regionHeight) {
//        if (regionHeight % 2 == 1) {
            mRegionHeight = MovingTextUtils.evenIt(regionHeight);
//        } else {
//            mRegionHeight = regionHeight;
//        }
    }

}
