package com.color.home.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.Region;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.externalvideo.ItemExternalVideoView;
import com.color.home.widgets.multilines.ItemMLScrollMultipic2View;
import com.color.home.widgets.multilines.ItemMLScrollableText;
import com.color.home.widgets.multilines.ItemMultiLinesMultipic;
import com.color.home.widgets.multilines.ItemMultiLinesPagedText;
import com.color.home.widgets.singleline.ItemSingleLineText;
import com.color.home.widgets.singleline.MovingTextUtils;
import com.color.home.widgets.singleline.PCItemSingleLineText;
import com.color.home.widgets.singleline.SLPCHTSurfaceView;
import com.color.home.widgets.singleline.localscroll.SLTextSurfaceView;
import com.color.home.widgets.singleline.pcscroll.SLPCSurfaceView;
import com.color.home.widgets.sync_playing.ItemSyncImageView;
import com.color.home.widgets.timer.ItemTimer;

import java.util.Random;

public class RegionView extends FrameLayout implements OnPlayFinishedListener, AdaptedRegion, Drawable.Callback {
    // never public, so that another class won't be messed up.
    private final static String TAG = "RegionView";
    private static final boolean DBG = false;

    private static final boolean SYNC_DBG = true;
    private static final String SYNC_TAG = "SYNC_IMG";

    private static final boolean DBG_DRAW = false;
    private static final boolean DRAW_DBG = false;
    private static final int[] sTypes = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22,
            23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
            42, 43, 44, 45, 46, 47, 48};


    protected Region mRegion;
    private PageView mPageView;

    private Drawable mDrawable;
    private ItemsAdapter mItemsAdapter;

    //
    private int mRealAnimationType = 0;

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


    private static class BorderDrawable extends Drawable {
        private Paint mStrokePaint = new Paint();
        private RectF mRectF;
        private int mPathStyle;
        private float mPhase = 0.f;
        private Path mShape;
        private float[] mIntervals;

        public BorderDrawable(int width, int height, int borderWidth, int color) {

            if (DBG)
                Log.d(TAG, "borderWidth= " + borderWidth);

            mStrokePaint.setStyle(Paint.Style.STROKE);
            mStrokePaint.setColor(color);

            float lineWidth;
            if (borderWidth <= 2) {
                mStrokePaint.setStrokeWidth(borderWidth);
                lineWidth = borderWidth / 2.f;

            } else {
                mPathStyle = borderWidth;
                mStrokePaint.setStrokeWidth(1);

                if (borderWidth == 3) {
                    mIntervals = new float[]{8.f, 8.f};
                    lineWidth = 0.5f;

                } else {// triangle
                    setPathShape();
                    lineWidth = 2.f;
                }
            }

            mRectF = new RectF(lineWidth, lineWidth, width - lineWidth, height - lineWidth);
            if (DBG)
                Log.d(TAG, "mRectF= " + mRectF);
        }


        private void setPathShape() {
            mShape = new Path();
            mShape.moveTo(0, -2);
            mShape.lineTo(4, -2);
            mShape.lineTo(2, 2);
            mShape.close();
        }

        @Override
        public void draw(Canvas canvas) {

            if (DBG_DRAW)
                Log.d(TAG, "mRectF= " + mRectF
                        + ", canvas= " + canvas + ", [" + canvas.getWidth() + ", " + canvas.getHeight());

            if (mPathStyle <= 2){
                canvas.drawRect(mRectF, mStrokePaint);

            } else {
                mStrokePaint.setPathEffect(getPathEffect());
                canvas.drawRect(mRectF, mStrokePaint);

                 mPhase -= 0.3f;

                if (DBG_DRAW)
                    Log.d(TAG, "callBack= " + getCallback() + ", mPhase= " + mPhase);
                invalidateSelf();

            }

        }

        private PathEffect getPathEffect() {
            if (mPathStyle == 3){
                return new DashPathEffect(mIntervals, mPhase);
            } else{//mPathStyle == 4, triangle
                return new PathDashPathEffect(mShape, 8.f, mPhase, PathDashPathEffect.Style.MORPH);
            }
        }

        @Override
        public void setAlpha(int alpha) {

        }

        @Override
        public void setColorFilter(ColorFilter cf) {

        }

        @Override
        public int getOpacity() {
            return 0;
        }

    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        if (DBG_DRAW)
            Log.d(TAG, "invalidateDrawable. drawable= " + drawable);
        invalidate();
    }

    public RegionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RegionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
            Log.i(TAG, "setRegion. pageView, region, transitioner=" + transitioner + ", this= " + this
                    + ", parent= " + this.getParent());

        if (region.rect != null) {

            int borderWidth = 0;
            try {
                borderWidth = Integer.parseInt(region.rect.borderwidth);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (borderWidth > 0) {

                RegionView.this.setPadding(borderWidth, borderWidth, borderWidth, borderWidth);

                int rectWidth = 0, rectHeight = 0;
                try {
                    rectWidth = Integer.parseInt(region.rect.width);
                    rectHeight = Integer.parseInt(region.rect.height);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                mDrawable = new BorderDrawable(rectWidth, rectHeight, borderWidth, GraphUtils.parseColor(region.rect.bordercolor));
                if (borderWidth > 2) {
                    this.setLayerType(LAYER_TYPE_SOFTWARE, null);
                    mDrawable.setCallback(this);
                }

            }
        }

        this.mPageView = pageView;
        this.mRegion = region;
        // Donnot call it too later, i.e., later than setupItems.
        // XXX>>>>><<<<
        // setOnHierarchyChangeListener(this);

        setupRegionLayout();
    }

    public int getmRealAnimationType() {
        return mRealAnimationType;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mIsAttached = true;
        // Must setupItems after the window attached, otherwise, there no is initial appearing animation.
        // TODO: Try catch. HMH 0819
        if (!PageView.isSinglelineScrollRegion(mRegion))
            setupItems();
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttached = false;

        if (DBG)
            Log.d(TAG, "onDetachedFromWindow. [ this= " + this + ", getChildAt(0)= " + getChildAt(0) + ", parent= " + this.getParent());
//        removeCallbacks(this);
    }

    private void setupAppearingTransition(LayoutTransition transition, long duration) {
        transition.setAnimator(LayoutTransition.APPEARING, mCustomAppearingAnim);
        transition.setDuration(LayoutTransition.APPEARING, duration);
        //transition.setInterpolator(LayoutTransition.APPEARING, new LinearInterpolator());
        if (DBG)
            new Exception().printStackTrace();

        if (DBG)
            Log.d(TAG, "setupAppearingTransition. [mCustomAppearingAnim=" + mCustomAppearingAnim
                    + ", this=" + this
                    + ", transition=" + transition
                    + ", duration=" + duration);
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


    private ItemSyncImageView.CurrentSyncImageStatus getCurrentSyncImgStatus(){
        ItemSyncImageView.CurrentSyncImageStatus syncImgStatus = new ItemSyncImageView.CurrentSyncImageStatus();

        long regionDurationMs = 0;
        for(Item item : mRegion.items){
            //inEffect + stay + outEffect(stay)
            regionDurationMs += Long.parseLong(item.duration);
        }
        if(SYNC_DBG){
            Log.d(SYNC_TAG, "regionDurationMs=" + regionDurationMs);
        }

        long offsetMs = (System.currentTimeMillis() - ItemSyncImageView.BENCHMARK_TIMES_MS) % regionDurationMs;
        if(SYNC_DBG)
            Log.d(SYNC_TAG, "Sync offsetMs = " + offsetMs);

        long sumDuration = 0;
        for(int i = 0; i < mRegion.items.size(); i ++){
            sumDuration += Long.parseLong(mRegion.items.get(i).duration);
            if(sumDuration > offsetMs) {
                syncImgStatus.index = i;
                syncImgStatus.restStayTime = sumDuration - offsetMs;
                Log.d(SYNC_TAG, "index=" + i + ", sumDuration=" + sumDuration);
                break;
            }
        }

        return syncImgStatus;
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
                    + ", this= " + this + ", parent= " + this.getParent());

        post(new Runnable() {

            @Override
            public void run() {
                if (DBG)
                    Log.d(TAG, "view= " + view + ", this= " + this );
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

                if (view instanceof ItemVideoView) {
                    if (DBG) {
                        Log.d(TAG, "is ItemVideoView.");
                    }
                    if (((ItemVideoView) view).ismIsLoop() && ((ItemVideoView) view).ismSeekable()) {
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

        if (mDisplayedChild == 0 && getAdapter().getCount() == 1
                && (getChildAt(0) instanceof ItemTimer
                || getChildAt(0) instanceof ItemWebView
                || getChildAt(0) instanceof ItemMultiLinesPagedText
                || getChildAt(0) instanceof ItemMultiLinesMultipic
                || getChildAt(0) instanceof ItemMLScrollMultipic2View
                || getChildAt(0) instanceof SLTextSurfaceView
                || getChildAt(0) instanceof SLPCSurfaceView
                || getChildAt(0) instanceof SLPCHTSurfaceView
                || getChildAt(0) instanceof PCItemSingleLineText
                || getChildAt(0) instanceof ItemSingleLineText
                || getChildAt(0) instanceof ItemExternalVideoView)
                ) {
            if (DBG)
                Log.d(TAG, "showNext. getChildAt(0)= " + getChildAt(0));

            if (DBG)
                Log.d(TAG, "showNext. [Single item, Don't move on to next.");

            //ItemMLScrollMultipic2View
            if (getChildAt(0) instanceof ItemMLScrollMultipic2View) {
                ((ItemMLScrollMultipic2View) getChildAt(0)).getmRenderer().notFinish();
            }

            //SLTextSurfaceView
            if (getChildAt(0) instanceof SLTextSurfaceView) {
                ((SLTextSurfaceView) getChildAt(0)).getmRenderer().notFinish();
            }

            //SLPCSurfaceView
            if (getChildAt(0) instanceof SLPCSurfaceView) {
                ((SLPCSurfaceView) getChildAt(0)).getmRenderer().notFinish();
            }

            //SLPCHTSurfaceView
            if (getChildAt(0) instanceof SLPCHTSurfaceView) {
                ((SLPCHTSurfaceView) getChildAt(0)).getmRenderer().notFinish();
            }


            return;
        }

        boolean playAnimation = true;
        //If it is the first image in a sync region...
        if(ItemsAdapter.isSyncRegion(mRegion) && !"2".equals(mRegion.items.get(mDisplayedChild).type)) {
            if(SYNC_DBG){
                Log.d(SYNC_TAG, "Sync img region.");
            }

            ItemSyncImageView.CurrentSyncImageStatus syncImgStatus = getCurrentSyncImgStatus();
            if(SYNC_DBG){
                Log.d(SYNC_TAG, "syncImgStatus index=" + syncImgStatus.index);
                Log.d(SYNC_TAG, "syncImgStatus rest stay=" + syncImgStatus.restStayTime);
            }
            //TODO Switch child with no animation.
            playAnimation = false;
            displayedChild = syncImgStatus.index + 1;
//            setDisplayedChild(syncImgStatus.index + 1, false);
        }

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
        // Random.
        Item item = mRegion.items.get(mDisplayedChild);

        if(playAnimation)
            mRealAnimationType = getRealAnimationType(item);
        else
            mRealAnimationType = 0;

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
                || view instanceof ItemVideoView
                || view instanceof ItemMultiLinesPagedText
                || view instanceof SLPCSurfaceView
                || view instanceof SLPCHTSurfaceView
                || view instanceof ItemSingleLineText
//                || view instanceof PCItemSingleLineText
                || view instanceof ItemExternalVideoView) {
            if (DBG)
                Log.d(TAG, "setDisplayedChild. [No animation true.");
            noAnimation = true;
        }

        if (!noAnimation && mRealAnimationType != 0 && !"1".equals(item.isscroll)) {
            if (DBG)
                Log.d(TAG, "setDisplayedChild. [mRealAnimationType=" + mRealAnimationType);
            mCustomAppearingAnim = getAnimationFor(mRealAnimationType, item, view);
            if (mCustomAppearingAnim != null) {
                // mCustomAppearingAnim.setStartDelay(0L);
                if (DBG)
                    Log.d(TAG, "setDisplayedChild. [mCustomAppearingAnim=" + mCustomAppearingAnim);

                mCustomAppearingAnim.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        // If there is only one item, the remove old view will remove this item, which
                        // results in empty content.
                        if (DBG)
                            Log.d("addListener", "onAnimationEnd. [animation=" + animation);

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

        // long duration = Long.parseLong(item.outeffect.Time);
        long duration = 500L;

        if (item.ineffect != null && item.ineffect.Time != null) {
            try {
                duration = Long.parseLong(item.ineffect.Time);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (DBG)
            Log.i(TAG, "getAnimationFor. duration=" + duration);

        setupAppearingTransition(getLayoutTransition(), duration);

        if (view instanceof ItemWebView) {
            if (DBG)
                Log.d(TAG, "itemRect= " + item.itemRect);
            if (item.itemRect != null) {
                int left = 0;
                int top = 0;
                try {
                    left = -Integer.parseInt(item.itemRect.x);
                    top = -Integer.parseInt(item.itemRect.y);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if (DBG)
                    Log.d(TAG, "leftMargin= " + left + ", topMargin= " + top);

                if (left == 0 && top == 0) {
                    addView(view);
                } else {
                    LayoutParams params = new LayoutParams(getRegionWidth() - left, getRegionHeight() - top);
                    params.leftMargin = left;
                    params.topMargin = top;
                    addView(view, params);
                }

            } else
                addView(view);
        } else
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

    private int getRealAnimationType(Item item) {
        int type = 0;
        int realAnimType = 0;

        if (item.ineffect != null && !TextUtils.isEmpty(item.ineffect.Type)) {
            try {
                type = Integer.parseInt(item.ineffect.Type);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        if (type == 1) {
            // type = mRand.nextInt((48 - 2) + 1) + 2;
            realAnimType = sTypes[mRand.nextInt(sTypes.length)];
        } else {
            realAnimType = type;
        }
        if (DBG) {
            Log.d(TAG, "setDisplayedChild.... type=" + type + ", realAnimType=" + realAnimType);
        }

        return realAnimType;
    }

    private ValueAnimator getAnimationFor(final int animationType, Item item, View view) {//
        ValueAnimator customAppearingAnim = null;

        if (DBG) {
            Log.d(TAG, "getAnimationFor.... animationType=" + animationType);
        }

        if (animationType == 2 || animationType == 3 || animationType == 4 || animationType == 5 || animationType == 6 || animationType == 7 || animationType == 8
                || animationType == 9 || animationType == 10 || animationType == 11 || animationType == 12 || animationType == 13 || animationType == 14
                || animationType == 15 || animationType == 16 || animationType == 17 || animationType == 18 || animationType == 19 || animationType == 28
                || animationType == 29 || animationType == 30 || animationType == 32 || animationType == 33 || animationType == 34 || animationType == 35
                || animationType == 36 || animationType == 37 || animationType == 43 || animationType == 44 || animationType == 45 || animationType == 46
                || animationType == 47 || animationType == 48 || animationType == 20 || animationType == 21 || animationType == 22 || animationType == 23) { //覆盖或百叶窗或马赛克或上下闭合或旋转或中间四周
            if (DBG)
                Log.i(TAG, " animationType = " + animationType);

            if (view instanceof EffectView) {
                customAppearingAnim = ObjectAnimator.ofFloat(null, "switchingPercent", 0.0f, 1.0f);//switchingPercent即SwitchableImageView中的一个变量名

                ((EffectView) view).setEffectStyle(animationType);

            }

        } else {
            PropertyValuesHolder left2Right = PropertyValuesHolder.ofFloat("translationX", -getRegionWidth(), 0f);
            PropertyValuesHolder right2Left = PropertyValuesHolder.ofFloat("translationX", getRegionWidth(), 0f);
            PropertyValuesHolder up2Down = PropertyValuesHolder.ofFloat("translationY", -getRegionHeight(), 0f);
            PropertyValuesHolder down2Up = PropertyValuesHolder.ofFloat("translationY", getRegionHeight(), 0f);
            // final ObjectAnimator changeIn =
            // ObjectAnimator.ofPropertyValuesHolder(
            // this, pvhLeft, pvhTop, pvhRight, pvhBottom);
            PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 0f, 1f);
            PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 0f, 1f);

//            if (animationType == 20) {
//                customAppearingAnim = ObjectAnimator.ofFloat(null, "translationY", getRegionHeight(), 0f);
//            } else if (animationType == 21) {
//                customAppearingAnim = ObjectAnimator.ofFloat(null, "translationY", -getRegionHeight(), 0f);
//            } else if (animationType == 22) {
//                customAppearingAnim = ObjectAnimator.ofFloat(null, "translationX", getRegionWidth(), 0f);
//            } else if (animationType == 23) {
//                // Must set this.
//                customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, left2Right);
//                // Diag.
//            } else
            if (animationType == 24) {

                customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, right2Left, down2Up);
            } else if (animationType == 25) {
                customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, left2Right, down2Up);
            } else if (animationType == 26) {
                customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, right2Left, up2Down);
            } else if (animationType == 27) {
                customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, left2Right, up2Down);
            } else if (animationType == 31) {
                PropertyValuesHolder fadeIn = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
                customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, fadeIn);
            } else if (animationType == 38) {
                customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY);
            } else if (animationType == 39) {
                customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY, right2Left, down2Up);
            } else if (animationType == 40) {
                customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY, left2Right, down2Up);
            } else if (animationType == 42) { // invert 42 / 41 according to the test.
                customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY, right2Left, up2Down);
            } else if (animationType == 41) {
                customAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY, left2Right, up2Down);
            }
        }


//        if (customAppearingAnim != null) {
//            if (DBG)
//                Log.i(TAG, "getAnimationFor. customAppearingAnim duration=" + duration+"animationType="+animationType);
//            customAppearingAnim.setDuration(duration);
//
//        }

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
        if (DRAW_DBG)
            Log.i(TAG, "onDraw. canvas type=" + mRealAnimationType);

        if (mDrawable != null)
            mDrawable.draw(canvas);
    }

    /*
     * MUST BE RUN FROM UI THREAD!!!!
     */
    @Override
    public void notifyOnAllPlayed() {
        if (DBG)
            Log.i(TAG, "notifyOnAllPlayed. this=" + this + ", mPageViewisShown= " + mPageView.isShown() + ", this.isShown= " + this.isShown());

        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalAccessError("Not from UI thread, cannot remove view.");
        }

        if (this.isShown())
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

    public ValueAnimator getmCustomAppearingAnim() {
        return mCustomAppearingAnim;
    }

}
