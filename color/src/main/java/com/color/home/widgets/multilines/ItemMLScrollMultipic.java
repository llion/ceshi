package com.color.home.widgets.multilines;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.color.home.AppController;
import com.color.home.AppController.MyBitmap;
import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.Region;
import com.color.home.ProgramParser.ScrollPicInfo;
import com.color.home.widgets.ItemsAdapter;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ItemMLScrollMultipic extends ImageView implements OnPlayFinishObserverable {
    // public class ItemMultiLinesMultipic extends ImageView implements OnPlayFinishObserverable, Runnable {
    private static final boolean DBG = true;
    // never public, so that another class won't be messed up.
    private final static String TAG = "ItemMultiLinesPagedText";

    private Item mItem;
    private int mHeight;
    private OnPlayFinishedListener mListener;
    private boolean mIsAttached;
    private int mPageIndex;
    private int mOnePicDuration;
    private int mPicCount;
    private ScrollPicInfo mScrollpicinfo;

    public ItemMLScrollMultipic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ItemMLScrollMultipic(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemMLScrollMultipic(Context context) {
        super(context);
    }

    public void setItem(RegionView regionView, Region region, Item item) {
        mListener = regionView;
        this.mItem = item;

        mScrollpicinfo = item.scrollpicinfo;
        mOnePicDuration = Integer.parseInt(mScrollpicinfo.onePicDuration);
        mPicCount = Integer.parseInt(mScrollpicinfo.picCount);

        mPageIndex = 0;
        setPageText();
    }

    private void setPageText() {
        final String keyImgId = mScrollpicinfo.filePath.MD5 + mPageIndex;
        MyBitmap bitmapFromMemCache = AppController.getInstance().getBitmapFromMemCache(keyImgId);
        Bitmap resultBm = null;
        if (bitmapFromMemCache == null) {
            try {
                final byte[] head = new byte[8];
                final String absFilePath = ItemsAdapter.getAbsFilePathByFileSource(mScrollpicinfo.filePath);
                if (DBG)
                    Log.d(TAG, "setPageText. [absFilePath=" + absFilePath);
                if (DBG) Log.d(TAG, "thread=" + Thread.currentThread());

                InputStream is = new FileInputStream(absFilePath);
                is.skip(20);
                is.read(head, 0, 8);

                ByteBuffer bb = ByteBuffer.wrap(head);
                bb.order(ByteOrder.LITTLE_ENDIAN); // if you want little-endian

                if (DBG)
                    Log.i(TAG, "onCreate. [position=" + bb.position());

                int width = bb.getInt();
                int height = bb.getInt();

                if (DBG)
                    Log.i(TAG, "onCreate. [width=" + width + ", height=" + height);

                byte[] content = new byte[width * height * 4];
                // byte[] converted = new byte[mWidth * mHeight * 4];

                is.skip(1024 - 28 + mPageIndex * width * height * 4);
                // 0 in the content.
                int read = is.read(content, 0, width * height * 4);
                is.close();

                if (DBG)
                    Log.i(TAG, "onCreate. [read size=" + read);

                for (int i = 0; i < content.length; i += 4)
                {
                    final byte ele = content[i];
                    content[i] = content[i + 2];
                    content[i + 2] = ele;
                }

                // Now put these nice RGBA pixels into a Bitmap object

                Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                // bm.setPremultiplied(false);
                bm.copyPixelsFromBuffer(ByteBuffer.wrap(content));

                AppController.getInstance().addBitmapToMemoryCache(keyImgId, new MyBitmap(bm, width, height));
                resultBm = bm;

                // bm.setPixel(x, y, color)
                // bm.setPremultiplied(premultiplied)

                // ImageView iv = (ImageView) findViewById(R.id.iv);
                // iv.setAdjustViewBounds(false);
                // iv.setScaleType(ScaleType.CENTER);
                // iv.setImageBitmap(bm);

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            resultBm = bitmapFromMemCache.getBitmap();
        }

        setImageBitmap(resultBm);
    }

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

        if (mPicCount > 1) {
            mHandler = new MTextMarquee(this, mOnePicDuration);
            mHandler.start();
        }
    }

    MTextMarquee mHandler;

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
        }

        if (DBG)
            Log.i(TAG, "run. next page. mPageIndex=" + mPageIndex);
        setPageText();
    }

    // @Override
    // public void run() {
    // tellListener();
    // }

    private static final class MTextMarquee extends Handler {
        private final static String TAG = "MTextMarquee";
        private static final boolean DBG = false;
        private static final int MESSAGE_TICK = 0x1;

        private final WeakReference<ItemMLScrollMultipic> mView;
        private boolean mShouldStop;
        private int mMarqueeDelay;

        public MTextMarquee(ItemMLScrollMultipic view, int delay) {
            mMarqueeDelay = delay;
            mView = new WeakReference<ItemMLScrollMultipic>(view);
        }

        public void start() {
            final ItemMLScrollMultipic view = mView.get();
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

            final ItemMLScrollMultipic view = mView.get();
            if (view != null) {
                sendEmptyMessageDelayed(MESSAGE_TICK, mMarqueeDelay);
                view.nextPage();
            }
        }
    }
}
