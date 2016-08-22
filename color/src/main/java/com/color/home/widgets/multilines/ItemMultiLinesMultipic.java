package com.color.home.widgets.multilines;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.color.home.AppController;
import com.color.home.AppController.MyBitmap;
import com.color.home.ProgramParser;
import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.MultiPicInfo;
import com.color.home.ProgramParser.Region;
import com.color.home.ProgramParser.ScrollPicInfo;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.EffectView;
import com.color.home.widgets.ItemsAdapter;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;
import com.google.common.io.ByteStreams;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ItemMultiLinesMultipic extends EffectView implements OnPlayFinishObserverable {
    // public class ItemMultiLinesMultipic extends ImageView implements OnPlayFinishObserverable, Runnable {
    private static final boolean DBG = false;
    private static final boolean DBG_DRAW = false;
    // never public, so that another class won't be messed up.
    private final static String TAG = "ItemMultiLinesMultipic";

    private Item mItem;
    private RegionView mRegionView;
    private int mHeight;
    private OnPlayFinishedListener mListener;
    private boolean mIsAttached;
    private int mPageIndex;
    private int mOnePicDuration;
    private int mPicCount;
    private ScrollPicInfo mScrollpicinfo;

    long duration = 500L;

    public ItemMultiLinesMultipic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ItemMultiLinesMultipic(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemMultiLinesMultipic(Context context) {
        super(context);
    }

    public void setItem(RegionView regionView, Region region, Item item) {
        mListener = regionView;
        this.mItem = item;
        this.mRegionView = regionView;

        mMultipicinfo = item.multipicinfo;
        try {
            mOnePicDuration = Integer.parseInt(mMultipicinfo.onePicDuration);
            mPicCount = Integer.parseInt(mMultipicinfo.picCount);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (DBG)
            Log.d(TAG, "mOnePicDuration= " + mOnePicDuration);

        mPageIndex = 0;

        if (DBG)
            Log.d(TAG, "ineffect= " + item.ineffect);
        if (item.ineffect != null && item.ineffect.Time != null) {

            try {
                duration = Long.parseLong(item.ineffect.Time);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (DBG)
            Log.i(TAG, "ineffect duration=" + duration);

        setPageText();
    }

    private void setPageText() {
        final String keyImgId = mMultipicinfo.filePath.MD5 + mPageIndex;

        MyBitmap bitmapFromMemCache = AppController.getInstance().getBitmapFromMemCache(keyImgId);
        if (DBG)
            Log.d(TAG, "keyImgId= " + keyImgId + ", bitmapFromMemCache= " + bitmapFromMemCache);
        if (bitmapFromMemCache == null) {
            new MulpicBmLoader().execute(new MulpicInfo(keyImgId, mPageIndex, mMultipicinfo.filePath));
        } else {
            Bitmap resultBm = bitmapFromMemCache.getBitmap();
            if (resultBm != null) {
                if (DBG)
                    Log.d(TAG, "mPageIndex= " + mPageIndex);
                if (mPageIndex > 0)
                    startAnimation();
                setImageBitmap(resultBm);
            }
        }

    }

    private static class MulpicInfo {
        public String keyImgId;
        public int index;
        public ProgramParser.FileSource filePath;

        public MulpicInfo(String keyImgId, int index, ProgramParser.FileSource filePath) {
            this.keyImgId = keyImgId;
            this.index = index;
            this.filePath = filePath;
        }
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
    class MulpicBmLoader extends AsyncTask<MulpicInfo, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(MulpicInfo... params) {
            return loadFor(params[0]);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (DBG)
                Log.d(TAG, "onPostExecute, result= " + result);
            if (mPageIndex > 0)
                startAnimation();
            setImageBitmap(result);
        }
    }

    public static Bitmap loadFor(final MulpicInfo mi) {
         String keyImgId = mi.keyImgId;
         int index = mi.index;
        ProgramParser.FileSource fileSource = mi.filePath;

        final byte[] head = new byte[8];
        final String absFilePath = ItemsAdapter.getAbsFilePathByFileSource(fileSource);
        if (DBG)
            Log.d(TAG, "setPageText. [absFilePath=" + absFilePath);

        StreamResolver streamResolver = null;
        try {
            streamResolver = new StreamResolver(absFilePath).resolve();
            InputStream readFromIs = streamResolver.getReadFromIs();
            if (readFromIs == null) {
                Log.e(TAG, "Bad file.absFilePath=" + absFilePath);
                return null;
            }

            if (DBG) Log.d(TAG, "skip fully.");
            ByteStreams.skipFully(readFromIs, 20);
            ByteStreams.readFully(readFromIs, head, 0, 8);

            ByteBuffer bb = ByteBuffer.wrap(head);
            bb.order(ByteOrder.LITTLE_ENDIAN); // if you want little-endian

            if (DBG)
                Log.i(TAG, "onCreate. [position=" + bb.position());

            int width = bb.getInt();
            int height = bb.getInt();

            if (DBG)
                Log.i(TAG, "onCreate. [width=" + width + ", height=" + height);

            final int picDatalength = width * height * 4;
            final byte[] content = new byte[picDatalength];
            // byte[] converted = new byte[mWidth * mHeight * 4];

            ByteStreams.skipFully(readFromIs, 1024 - 28 + index * picDatalength);
            // 0 in the content.

            if (DBG) Log.d(TAG, "Must read fully, as this is a zip inputstrea, it could return less than requested bytes on read.");
            ByteStreams.readFully(readFromIs, content, 0, picDatalength);

            GraphUtils.convertRGBFromPC(content);
            // Now put these nice RGBA pixels into a Bitmap object

            Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // bm.setPremultiplied(false);
            bm.copyPixelsFromBuffer(ByteBuffer.wrap(content));

            AppController.getInstance().addBitmapToMemoryCache(keyImgId, new MyBitmap(bm, width, height));

            return bm;

            // bm.setPixel(x, y, color)
            // bm.setPremultiplied(premultiplied)

            // ImageView iv = (ImageView) findViewById(R.id.iv);
            // iv.setAdjustViewBounds(false);
            // iv.setScaleType(ScaleType.CENTER);
            // iv.setImageBitmap(bm);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (streamResolver != null) {
                streamResolver.close();
            }

        }
        return null;
    }

//    public static void continuousRead(InputStream readFromIs, int length, byte[] buffer) throws IOException {
//        int offsetInBuff = 0;
//        while (offsetInBuff < length) {
//            // the offset is the offset in content.
//            int bytesRead = readFromIs.read(buffer, offsetInBuff, length - offsetInBuff);
//            if (DBG)
//                Log.i(TAG, "loadFor. [read size=" + bytesRead + ", offsetInBuff=" + offsetInBuff);
//
//            if (bytesRead == -1) {
//                // Incomplete data
//                if (DBG)
//                    Log.i(TAG, "loadFor. [Incomplete data, exit prematurely. read size=" + bytesRead);
//                break;
//            }
//            offsetInBuff += bytesRead;
//        }
//    }

    private int getRegionHeight(Region region) {
        return Integer.parseInt(region.rect.height);
    }

    private int getRegionWidth(Region region) {
        if (DBG)
            Log.i(TAG, "getRegionWidth. region=" + region);
        return Integer.parseInt(region.rect.width);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mIsAttached = false;
        // removeCallbacks(this);

        if (mHandler != null) {
            mHandler.stop();
            mHandler = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttached = true;

        // Schedule item time up.
        // removeCallbacks(this);
        // postDelayed(this, mOnePicDuration);

        if (mPicCount >= 1) {
            mHandler = new MTextMarquee(this, mOnePicDuration);
            mHandler.start();
        }
    }

    MTextMarquee mHandler;
    private MultiPicInfo mMultipicinfo;

    @Override
    public void setListener(OnPlayFinishedListener listener) {
        this.mListener = listener;
    }

    @Override
    public void removeListener(OnPlayFinishedListener listener) {
        this.mListener = null;
    }

    private void tellListener() {
        if (mListener != null) {
            if (DBG)
                Log.i(TAG, "tellListener. Tell listener =" + mListener);
            mListener.onPlayFinished(this);
        }
    }

    public void nextPage() {
        mPageIndex++;
        if (mPageIndex >= mPicCount) {
            mPageIndex = 0;
            // On multiline finished play once, tell region view.
            tellListener();
        } else {

            if (DBG)
                Log.i(TAG, "run. next page. mPageIndex=" + mPageIndex);
            setPageText();
        }
    }

    // @Override
    // public void run() {
    // tellListener();
    // }

    private static final class MTextMarquee extends Handler {
        private final static String TAG = "MTextMarquee";
        private static final boolean DBG = false;
        private static final int MESSAGE_TICK = 0x1;

        private final WeakReference<ItemMultiLinesMultipic> mView;
        private boolean mShouldStop;
        private long mMarqueeDelay;

        public MTextMarquee(ItemMultiLinesMultipic view, long delay) {
            mMarqueeDelay = delay;
            mView = new WeakReference<ItemMultiLinesMultipic>(view);
        }

        public void start() {
            final ItemMultiLinesMultipic view = mView.get();
            if (view != null) {
                sendEmptyMessageDelayed(MESSAGE_TICK, mMarqueeDelay);
            }

        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_TICK:
                if (mShouldStop) {
                    if (DBG)
                        Log.d(TAG, "handleMessage. [mShouldStop");
                    return;
                }

                tick();
                break;
            }
        }

        void stop() {
            if (DBG)
                Log.d(TAG, "stop.");

            removeMessages(MESSAGE_TICK);
            mShouldStop = true;
        }

        void tick() {
            if (DBG)
                Log.d(TAG, "tick.");

            removeMessages(MESSAGE_TICK);

            if (mShouldStop) {
                if (DBG)
                    Log.d(TAG, "tick. [mShouldStop");
                return;
            }

            final ItemMultiLinesMultipic view = mView.get();
            if (view != null) {
                sendEmptyMessageDelayed(MESSAGE_TICK, mMarqueeDelay);
                view.nextPage();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (DBG_DRAW) {
            Log.d(TAG, " onDraw(Canvas canvas), effect2= " + effect2 + ", switchingPercent= " + switchingPercent);
        }
        if (effect2)
            effectStyle.beforeDraw(canvas, switchingPercent);//限制新图出来的形状

        super.onDraw(canvas);//画图

        if (effect2)
            effectStyle.onDraw(this, canvas);//处理图

    }

    public void startAnimation() {
        ValueAnimator animator = mRegionView.getmCustomAppearingAnim();

        if (DBG) {
            Log.d(TAG, "animator= " + animator);
        }
        if (animator != null) {
            animator.setTarget(this);
            animator.setDuration(duration);
            animator.start();
        }

    }

}
