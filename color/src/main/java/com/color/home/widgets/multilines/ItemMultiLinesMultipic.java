package com.color.home.widgets.multilines;

import android.content.Context;
import android.graphics.Bitmap;
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
import com.color.home.widgets.ItemsAdapter;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ItemMultiLinesMultipic extends ImageView implements OnPlayFinishObserverable {
    // public class ItemMultiLinesMultipic extends ImageView implements OnPlayFinishObserverable, Runnable {
    private static final boolean DBG = true;
    // never public, so that another class won't be messed up.
    private final static String TAG = "ItemMultiLinesMultipic";

    private Item mItem;
    private int mHeight;
    private OnPlayFinishedListener mListener;
    private boolean mIsAttached;
    private int mPageIndex;
    private int mOnePicDuration;
    private int mPicCount;
    private ScrollPicInfo mScrollpicinfo;

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

        mMultipicinfo = item.multipicinfo;
        mOnePicDuration = Integer.parseInt(mMultipicinfo.onePicDuration);
        mPicCount = Integer.parseInt(mMultipicinfo.picCount);

        mPageIndex = 0;
        setPageText();
    }

    private void setPageText() {
        final String keyImgId = mMultipicinfo.filePath.MD5 + mPageIndex;

        MyBitmap bitmapFromMemCache = AppController.getInstance().getBitmapFromMemCache(keyImgId);
        if (bitmapFromMemCache == null) {
            new MulpicBmLoader().execute(new MulpicInfo(keyImgId, mPageIndex, mMultipicinfo.filePath));
        } else {
            Bitmap resultBm = bitmapFromMemCache.getBitmap();
            if (resultBm != null)
                setImageBitmap(resultBm);
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

        InputStream is = null;
        ZipInputStream isInsideZip = null;
        try {
            if (new File(absFilePath + ".zip").exists()) {
                if (DBG) Log.e(TAG, "Zipped. file=" + new File(absFilePath + ".zip"));
                isInsideZip = new ZipInputStream(new BufferedInputStream(new FileInputStream(new File(absFilePath + ".zip"))));
                ZipEntry ze = isInsideZip.getNextEntry();
                if (ze == null) {
                    Log.e(TAG, "Bad zip file.absFilePath=" + absFilePath);
                    return null; // is is closed in finally block.
                }

                if (DBG) {
                    Log.d(TAG, "zipped file name is=" + ze.getName());
                }

            } else {
                if (DBG)
                    Log.e(TAG, "Not zipped, use plain pic. trying to find file=" + new File(absFilePath + ".zip"));
                is = new FileInputStream(absFilePath);
            }

            InputStream readFromIs = (isInsideZip == null ? is : isInsideZip);
            readFromIs.skip(20);
            readFromIs.read(head, 0, 8);

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

            readFromIs.skip(1024 - 28 + index * picDatalength);
            // 0 in the content.

            int offsetInBuff = 0;
            while (offsetInBuff < picDatalength) {
                // the offset is the offset in content.
                int bytesRead = readFromIs.read(content, offsetInBuff, picDatalength - offsetInBuff);
                if (DBG)
                    Log.i(TAG, "loadFor. [read size=" + bytesRead + ", offsetInBuff=" + offsetInBuff);

                if (bytesRead == -1) {
                    // Incomplete data
                    if (DBG)
                        Log.i(TAG, "loadFor. [Incomplete data, exit prematurely. read size=" + bytesRead);
                    break;
                }
                offsetInBuff += bytesRead;
            }

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
            if (isInsideZip != null) {
                try {
                    isInsideZip.close();
                } catch (Exception e) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }

        }
        return null;
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

        private final WeakReference<ItemMultiLinesMultipic> mView;
        private boolean mShouldStop;
        private int mMarqueeDelay;

        public MTextMarquee(ItemMultiLinesMultipic view, int delay) {
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
}
