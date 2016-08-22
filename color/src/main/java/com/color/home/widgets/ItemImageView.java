package com.color.home.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.color.home.AppController;
import com.color.home.AppController.MyBitmap;
import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.Region;

public class ItemImageView extends EffectView implements OnPlayFinishObserverable {
    private static final boolean DBG = false;
    // never public, so that another class won't be messed up.
    private final static String TAG = "ItemImageView";

    private Item mItem;
    private String mFilePath;
    private Bitmap mBitmap;
    public Region mRegion;//....

    // private static BitmapFactory.Options sPurgeOption = new
    // BitmapFactory.Options();
    // private static BitmapFactory.Options sResampleOption = new
    // BitmapFactory.Options();

    public static class FilePathAndDim {
        String file;
        int width;
        int height;

        FilePathAndDim(String file, int width, int height) {
            super();
            this.file = file;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return "FilePathAndDim [file=" + file + ", width=" + width + ", height=" + height + "]";
        }
    }

    public ItemImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    public ItemImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public ItemImageView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public int mFinalAnimationType = 2;

    public void setItem(RegionView regionView, Item item) {
        mListener = regionView;
        this.mItem = item;
        mFilePath = item.getAbsFilePath();
        int width = 128;
        int height = 128;

        try {
            width = Integer.parseInt(mRegion.rect.width);
            height = Integer.parseInt(mRegion.rect.height);
            mDuration = Integer.parseInt(mItem.duration);
            if (DBG)
                Log.d(TAG, " mDuration = " + mDuration);
            setAlpha(Float.parseFloat(item.alhpa));

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (DBG)
            Log.i(TAG, "----------alpha    =" + item.alhpa);
        //setAlpha(Float.parseFloat(item.alhpa));


        if ("1".equals(item.reserveAS)) {//
            setScaleType(ScaleType.CENTER_INSIDE);
        } else {
            setScaleType(ScaleType.FIT_XY);
        }
        if (DBG)
            Log.i("rotation", "-----------goto rotation    =");
        //setRotationX(100.f);
        // out = Integer.parseInt(mItem.outeffect.Time);
        // ineffect = Integer.parseInt(mItem.ineffect.Time);
        loadBitmap(width, height);
    }

    // Assume that: the file path, pic name is changed when the content is different.
    public void loadBitmap(int width, int height) {
        final String imageKey = mFilePath;

        MyBitmap bitmapFromMemCache = AppController.getInstance().getBitmapFromMemCache(imageKey);
        if (bitmapFromMemCache != null) {
            setImageBitmap(bitmapFromMemCache.getBitmap());
        } else {
            new HelpDataParseWorker().execute(new FilePathAndDim(mFilePath, width, height));
        }
    }

    private class HelpDataParseWorker extends AsyncTask<FilePathAndDim, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(FilePathAndDim... params) {
            final FilePathAndDim info = params[0];
            if (DBG)
                Log.i(TAG, "doInBackground. loading image=" + info);

            // Bitmap decodeFile = decodeImagePurgeOnly(file);
            Bitmap decodeFile = getArtworkQuick(info.file, info.width, info.height);
            return decodeFile;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (DBG)
                Log.i(TAG, "onPostExecute. [result=" + result);

            if (mFilePath != null && result != null)
                AppController.getInstance().addBitmapToMemoryCache(mFilePath, new MyBitmap(result, 0, 0));
            setImageBitmap(result);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DBG)
            Log.i(TAG, "onAttachedToWindow. image = " + mItem.filesource.filepath);

        mRunnable = new Runnable() {

            @Override
            public void run() {
                if (DBG)
                    Log.i("Mduration", "run. img duration up = " + mItem.filesource.filepath + "    mduration=======" + mDuration);

                tellListener();
            }
        };
        postDelayed(mRunnable, mDuration);//进场 + 停留 + 出场
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DBG)
            Log.i(TAG, "onDetachedFromWindow. this=" + this);

        if (mRunnable != null) {
            removeCallbacks(mRunnable);
        }
    }

    private static Bitmap decodeImagePurgeOnly(final String file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true;
        Bitmap decodeFile = BitmapFactory.decodeFile(file, options);
        return decodeFile;
    }

    // Get album art for specified album. This method will not try to
    // fall back to getting artwork directly from the file, nor will
    // it attempt to repair the database.
    static Bitmap getArtworkQuick(String file, int w, int h) {
        // NOTE: There is in fact a 1 pixel border on the right side in the
        // ImageView
        // used to display this drawable. Take it into account now, so we don't
        // have to
        // scale later.

        // When the file is deleted during playing, such as when it's pruning
        // files, an Exception is thrown complaining file not found.
        // In the line "BitmapFactory.decodeFile(file, sBitmapOptionsCache);"
        BitmapFactory.Options sBitmapOptionsCache = new BitmapFactory.Options();
        int sampleSize = 1;
        // Compute the closest power-of-two scale factor
        // and pass that to sBitmapOptionsCache.inSampleSize, which will
        // result in faster decoding and better quality
        sBitmapOptionsCache.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, sBitmapOptionsCache);
        int nextWidth = sBitmapOptionsCache.outWidth >> 1;
        int nextHeight = sBitmapOptionsCache.outHeight >> 1;
        while (nextWidth > w && nextHeight > h) {
            sampleSize <<= 1;
            nextWidth >>= 1;
            nextHeight >>= 1;
        }

        sBitmapOptionsCache.inSampleSize = sampleSize;
        if (DBG)
            Log.d(TAG, "getArtworkQuick. [sampleSize=" + sampleSize + ", nextWidth=" + nextWidth
                    + ", nextHeight=" + nextHeight
                    + ", w=" + w
                    + ", h=" + h);

        sBitmapOptionsCache.inJustDecodeBounds = false;
        sBitmapOptionsCache.inPurgeable = true;
        Bitmap b = BitmapFactory.decodeFile(file, sBitmapOptionsCache);

        // if (b != null) {
        // // finally rescale to exactly the size we need
        // if (sBitmapOptionsCache.outWidth != w
        // || sBitmapOptionsCache.outHeight != h) {
        // Bitmap tmp = Bitmap.createScaledBitmap(b, w, h, true);
        // // Bitmap.createScaledBitmap() can return the same bitmap
        // if (tmp != b)
        // b.recycle();
        // b = tmp;
        // }
        // }
        return b;

    }

    public void setRegion(Region mRegion) {
        this.mRegion = mRegion;
        if (DBG)
            Log.d(TAG, "setRegion");
    }

    private OnPlayFinishedListener mListener;
    private int mDuration = 500;
    private Runnable mRunnable;

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

    //
    @Override
    protected void onDraw(Canvas canvas) {
        if (DBG) {
            Log.d(TAG, " onDraw(Canvas canvas), effect2= " + effect2 + ", switchingPercent= " + switchingPercent);
        }
        if (effect2)
            effectStyle.beforeDraw(canvas, switchingPercent);//限制新图出来的形状

        super.onDraw(canvas);//画图

        if (effect2)
            effectStyle.onDraw(this, canvas);//处理图

    }


}