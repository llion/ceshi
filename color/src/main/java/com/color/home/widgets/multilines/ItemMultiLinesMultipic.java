package com.color.home.widgets.multilines;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;

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

    private long duration = 500L;
    private int ineffectType = 0;
    private boolean isFirstComing = true;// the first time display picture, the view already has ineffect animation
    private boolean isTranslate = false;
    private Bitmap newBitmap = null;
    private Bitmap oldBitmap = null;


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

        ineffectType = mRegionView.getmRealAnimationType();

        if (ineffectType <= 23 && ineffectType >= 20)
            isTranslate = true;//上下左右平移

        mMultipicinfo = item.multipicinfo;
        try {
            mOnePicDuration = Integer.parseInt(mMultipicinfo.onePicDuration);
            mPicCount = Integer.parseInt(mMultipicinfo.picCount);
        } catch (Exception e) {
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
        if (DBG)
            Log.d(TAG, "setPageText. isTranslate= " + isTranslate + ", isFirstComing= " + isFirstComing);
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

                if (isTranslate)
                    newBitmap = resultBm;

                if (!isFirstComing && (ineffectType > 1 && ineffectType < 49)) //set effect of page turnning
                    appearAnimation();

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

            if (isTranslate)
                newBitmap = result;

            if (!isFirstComing && (ineffectType > 1 && ineffectType < 49)) //set effect of page turnning
                appearAnimation();

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

            if (DBG)
                Log.d(TAG, "Must read fully, as this is a zip inputstrea, it could return less than requested bytes on read.");
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

//    private int getRegionHeight(Region region) {
//        return Integer.parseInt(region.rect.height);
//    }
//
//    private int getRegionWidth(Region region) {
//        if (DBG)
//            Log.i(TAG, "getRegionWidth. region=" + region);
//        return Integer.parseInt(region.rect.width);
//    }

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
        if (DBG)
            Log.d(TAG, "nextPage.mPageIndex= " + mPageIndex);

        mPageIndex++;

        if (isTranslate) {
            //old bitmap
            oldBitmap = newBitmap;
        }

        if (mPageIndex >= mPicCount) {
            mPageIndex = 0;
            // On multiline finished play once, tell region view.
            tellListener();
        }
//        else {
//
//            if (DBG)
//                Log.i(TAG, "run. next page. mPageIndex=" + mPageIndex);
        if (isFirstComing)
            isFirstComing = false;
        setPageText();

//        }
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
        if (DBG_DRAW)
            Log.d(TAG, "onDraw. isTranslate= " + isTranslate + ", isFirstComing= " + isFirstComing + ", thread= " + Thread.currentThread());


        if (isTranslate && !isFirstComing)
            drawBitmap(switchingPercent, canvas);

        else {
            if (effect2)
                effectStyle.beforeDraw(canvas, switchingPercent);
            super.onDraw(canvas);//画图
            if (effect2)
                effectStyle.onDraw(this, canvas);
        }


    }

    private void drawBitmap(float switchingPercent, Canvas canvas) {

        try {

            int cWidth = canvas.getWidth();
            int cHeight = canvas.getHeight();
            Rect oldsrc = null, olddst = null, newsrc = null, newdst = null;
            if (DBG_DRAW)
                Log.d(TAG, "cWidth= " + cWidth + ", cHeight= " + cHeight);

            if (ineffectType == 20) {//上移
                oldsrc = new Rect(0, (int) (switchingPercent * cHeight), cWidth, cHeight);
                olddst = new Rect(0, 0, cWidth, cHeight - (int)(switchingPercent * cHeight));

                newsrc = new Rect(0, 0, cWidth, (int) (switchingPercent * cHeight));
                newdst = new Rect(0, cHeight - (int)(switchingPercent * cHeight), cWidth, cHeight);

//                canvas.drawBitmap(getOldBitmap(), 0, -switchingPercent * cHeight, null);
//                canvas.drawBitmap(newBitmap(), 0, (1 - switchingPercent) * cHeight, null);


            } else if (ineffectType == 21) {//下移

                oldsrc = new Rect(0, 0, cWidth, cHeight - (int)(switchingPercent * cHeight));
                olddst = new Rect(0, (int) (switchingPercent * cHeight), cWidth, cHeight);

                newsrc = new Rect(0, cHeight - (int)(switchingPercent * cHeight), cWidth, cHeight);
                newdst = new Rect(0, 0, cWidth, (int) (switchingPercent * cHeight));

            } else if (ineffectType == 22) {//左移
                oldsrc = new Rect((int) (switchingPercent * cWidth), 0, cWidth, cHeight);
                olddst = new Rect(0, 0, cWidth - (int)(switchingPercent * cWidth), cHeight);

                newsrc = new Rect(0, 0, (int) (switchingPercent * cWidth), cHeight);
                newdst = new Rect(cWidth - (int)(switchingPercent * cWidth), 0, cWidth, cHeight);

            } else if (ineffectType == 23) {//右移
                oldsrc = new Rect(0, 0, cWidth - (int)(switchingPercent * cWidth), cHeight);
                olddst = new Rect((int) (switchingPercent * cWidth), 0, cWidth, cHeight);

                newsrc = new Rect(cWidth - (int)(switchingPercent * cWidth), 0, cWidth, cHeight);
                newdst = new Rect(0, 0, (int) (switchingPercent * cWidth), cHeight);
            }


            if (DBG_DRAW)
                Log.d(TAG, "newsrc." + newsrc + ", switchingPercent= " + switchingPercent);

            if (DBG_DRAW)
                Log.d(TAG, "newdst. " + newdst);

            if (newBitmap != null)
                canvas.drawBitmap(newBitmap, newsrc, newdst, null);
            if (oldBitmap != null)
                canvas.drawBitmap(oldBitmap, oldsrc, olddst, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void appearAnimation() {
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
