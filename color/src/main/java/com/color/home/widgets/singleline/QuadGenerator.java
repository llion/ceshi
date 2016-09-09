package com.color.home.widgets.singleline;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class QuadGenerator {
    private final static String TAG = "QuadGenerator";
    private static final boolean DBG = false;

    private int mPcWidth;
    private int mPcHeight;

    private int mTexWidth;

    private int mItemWidth;

    private int mLastQuadWidth;

    private int mRepeatedQuadsSize;
    private int mWholeTexQuadsCount;
    // private static final String MNT_SDCARD_PNGS = "/mnt/sdcard/pngs";
//    public static final int MAX_TEXTURE_WIDTH_HEIGHT = 4096;

    public QuadGenerator(int pcWidth, int pcHeight, int texWidth, int itemWidth) {
        mPcWidth = pcWidth; //maybe an uneven number
        mPcHeight = pcHeight;
        mTexWidth = texWidth;
        mItemWidth = itemWidth;

        final int widthRemaining = mPcWidth % mTexWidth;
        mWholeTexQuadsCount = mPcWidth / mTexWidth + (widthRemaining == 0 ? 0 : 1);
        mLastQuadWidth = (widthRemaining == 0 ? mTexWidth : widthRemaining );

        final float askingForModelWholeWidth = mPcWidth + mItemWidth + 2.0f;
        // final float askingForModelWholeWidth = Math.max(mTotalTextWidth, mWidth) + Math.min(mTotalTextWidth, mWidth) + 2.0f;
        if (DBG)
            Log.d(TAG, "initShapes. [askingForModelWholeWidth=" + askingForModelWholeWidth + ", mPcWidth=" + mPcWidth
                    + ", mItemWidth=" + mItemWidth + ", mTexWidth=" + mTexWidth + ", mLastQuadWidth=" + mLastQuadWidth);

        int repeat = 1; // At least 1.
        try {
            repeat = (int) askingForModelWholeWidth / (int) mPcWidth;
        } catch (Exception e) {
            e.printStackTrace();
        }
        repeat++;
        mRepeatedQuadsSize = mWholeTexQuadsCount * repeat;
    }

    public QuadSegment getQuad(int i) {
        if (i >= mRepeatedQuadsSize) {
            Log.e(TAG, "getQuad. [Out of index. i=" + i + ", mRepeatedQuadsSize=" + mRepeatedQuadsSize);
            return null;
        }

        int whichRepeat = (i / mWholeTexQuadsCount);
        int whichIndexInRepeat = i % mWholeTexQuadsCount;
        int left = whichRepeat * mPcWidth + whichIndexInRepeat * mTexWidth;

        int texTop = whichIndexInRepeat * mPcHeight;

        // The last quad.
        boolean isLastQuadInRepeat = (whichIndexInRepeat == mWholeTexQuadsCount - 1);
        if (DBG)
            Log.d(TAG, "getQuad. [i=" + whichRepeat
                    + ", whichRepeat=" + whichRepeat
                    + ", whichIndexInRepeat=" + whichIndexInRepeat
                    + ", left=" + left
                    + ", texTop=" + texTop
                    + ", isLastQuadInRepeat=" + isLastQuadInRepeat
                    + ", this=" + this
                    );
        if (isLastQuadInRepeat) {
            return new QuadSegment(0, left, mLastQuadWidth, mPcHeight, texTop, 0);
        } else {
            return new QuadSegment(0, left, mTexWidth, mPcHeight, texTop, 0);
        }
    }

    @Override
    public String toString() {
        return "QuadGenerator [mPcWidth=" + mPcWidth + ", mPcHeight=" + mPcHeight + ", mTexWidth=" + mTexWidth + ", mItemWidth="
                + mItemWidth + ", mLastQuadWidth=" + mLastQuadWidth + ", mRepeatedQuadsSize=" + mRepeatedQuadsSize
                + ", mWholeTexQuadsCount=" + mWholeTexQuadsCount + "]";
    }

    public int getRepeatedQuadsSize() {
        return mRepeatedQuadsSize;
    }

    public static void toPng(Bitmap textBitmap, File file) {
    
        // String filename = "/mnt/sdcard/text." + this + ".png";
    
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            textBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static int findClosestPOT(int width, int height) {
        // 2 ^ 12 = 4096.
        for (int i = 1; i <= 12; i++) {
            int dim = 1 << i;
            if (DBG)
                Log.d(TAG, "getClosePOT. [i=" + i + ", dim=" + dim + ", dim*dim=" + dim * dim + ", width*height="
                        + width * height);
            if (dim * dim >= width * height) {
                if (width > height) {
                    if ((width / dim + ((width % dim) == 0 ? 0 : 1)) * height > dim) {
                        // Find next, as the height*(wrap back times) exceed the dim height.
                        if (DBG)
                            Log.w(TAG, "getClosePOT. [wraps exceed the height, dim not enough.");
                        continue;
                    }
                } else {//height <= width
                    if ((height / dim + (height % dim == 0 ? 0 : 1)) * width > dim) {
                        // Find next, as the height*(wrap back times) exceed the dim height.
                        if (DBG)
                            Log.w(TAG, "getClosePOT. [wraps exceed the width, dim not enough.");
                        continue;
                    }
                }
                if (DBG)
                    Log.d(TAG, "width= " + width + ", height= " + height + ", dim= " + dim);
                return dim;
            }
        }

        // MAX texture dimension supported, width/height is 4096.
        return 4096;
    }

}