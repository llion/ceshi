/*
 * This proprietary software may be used only as
 * authorised by a licensing agreement from ARM Limited
 * (C) COPYRIGHT 2013 ARM Limited
 * ALL RIGHTS RESERVED
 * The entire notice above must be reproduced on all authorised
 * copies and copies may only be made to the extent permitted
 * by a licensing agreement from ARM Limited.
 */

package com.color.home.widgets.singleline.localscroll;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.AppController.MyBitmap;
import com.color.home.ProgramParser;
import com.color.home.ProgramParser.Item;
import com.color.home.widgets.multilines.MultiPicScrollObject;
import com.color.home.widgets.singleline.QuadGenerator;
import com.color.home.widgets.singleline.pcscroll.SLPCTextObject;
import com.google.common.hash.HashCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TextObject extends SLPCTextObject {
    private final static String TAG = "TextObject";
    private static final boolean DBG = false;
    private static final boolean DBG_READ = false;
    private static final boolean DBG_PNG = false;
    private static final int MAX_DRAW_TEXT_WIDTH = 33000;

    protected String mText = "";
    private float mTextSize = 20;
    private int mColor;

    public TextObject(Context context) {
        super(context, null);
        mContext = context;
    }

    protected Context mContext;

    protected Paint mPaint = new Paint();
    protected int mLineHeight;
    private HashCode mTextBitmapHash;

    public void setText(String aText) {
        mText = aText;
    }

    @Override
    public boolean prepareTexture() {
        if (mTextBitmapHash == null || TextUtils.isEmpty(mText)) {
            Log.e(TAG, "drawCanvasToTexture. [mTextBitmapHash should not be null and mText should not be null");
        }

        String text = mText;

        setupPaint();

        Rect boundsAllText = new Rect();
        mPaint.getTextBounds(text, 0, text.length(), boundsAllText);
        int topAllText = boundsAllText.top;
        if (DBG)
            Log.d(TAG, "bounds top: " + topAllText + " TextSize: " + mTextSize);
        mLineHeight = (int) mTextSize + 3;
        if (mLineHeight % 2 == 1) {
            mLineHeight++;
        }
        if (DBG)
            Log.d(TAG, " mTextSize = " + mTextSize + ", mLineHeight = " + mLineHeight);

        setPcHeight(mLineHeight);
        float measuredWidth = mPaint.measureText(text);
        int myw = (int) ensuredWidth(boundsAllText, measuredWidth);
        mPcWidth = myw;

        if (!MultiPicScrollObject.isMemoryEnough(getTexDim()))
            return false;

        mRealReadPcWidth = getRealReadPcWidth(mPcWidth, getPcHeight(), getTexDim());

        if (DBG)
            Log.d(TAG, "drawCanvasToTexture. [origin textBounds= " + boundsAllText
                    + ", boundsAllText.right= " + boundsAllText.right + ", text.length= " + text.length()
                    + ", realTextWidth=" + (measuredWidth) + ", mTextSize=" + mTextSize + ", myw= " + myw
                    + ", mRealReadPcWidth= " + mRealReadPcWidth + ", aText=" + text);

        LineSegment aline = new LineSegment(text, boundsAllText, mEvenedHeight);

//        File cacheDir = mContext.getCacheDir();
//        if (DBG)
//            Log.d(TAG, "drawCanvasToTexture. [hashCode=" + mTextBitmapHash.toString() + ", cacheDir=" + cacheDir);
//         All the text item has this hash.
//        Bitmap savedBitmap = null;
//        File bitmapCacheFile = new File(cacheDir, getPngName());
//        if (bitmapCacheFile.exists()) {
//            if (DBG)
//                Log.d(TAG, "drawCanvasToTexture. [bitmapCacheFile exist=" + bitmapCacheFile);
//            BitmapFactory.Options bo = new BitmapFactory.Options();
//            bo.inPurgeable = true;
//            // bo.inTempStorage = true;
//            savedBitmap = BitmapFactory.decodeFile(bitmapCacheFile.toString(), bo);
//
//            mPcWidth = savedBitmap.getWidth();
//            setPcHeight(savedBitmap.getHeight());
//            if (DBG)
//                Log.d(TAG, "drawCanvasToTexture. [mPcWidth=" + mPcWidth + ", mPcHeight=" + getPcHeight());
//        } else {
//            // Ensure that there is always room for a new .png
//
//            ensureTargetDirRoom();

        // Prepare bitmap canvas.
        if (DBG)
            Log.d(TAG, "drawCanvasToTexture. [mPcWidth=" + mPcWidth + ", mPcHeight=" + getPcHeight());
        Bitmap textureBm = Bitmap.createBitmap(getTexDim(), getTexDim(), Bitmap.Config.ARGB_8888);

        int saveBitmapWidth = myw, count = text.length();
        if (myw > MAX_DRAW_TEXT_WIDTH) {
            saveBitmapWidth = MAX_DRAW_TEXT_WIDTH;
            count = (int) (MAX_DRAW_TEXT_WIDTH / mTextSize);
        }
        Bitmap savedBitmap = Bitmap.createBitmap(saveBitmapWidth, getPcHeight(), Bitmap.Config.ARGB_8888);
        savedBitmap.eraseColor(Color.argb(0, 255, 255, 255));
        // Creates a new canvas that will draw into a bitmap instead of rendering into the screen
        Canvas bitmapCanvas = new Canvas(savedBitmap);
        aline.draw(bitmapCanvas, mPaint, topAllText, count, text.length(), savedBitmap, textureBm);
        if (DBG_PNG) {
            new File("/mnt/sdcard/mul").mkdir();
            QuadGenerator.toPng(savedBitmap, new File("/mnt/sdcard/mul/" + "origin" + getKeyImgId(0)));
        }
//            QuadGenerator.toPng(savedBitmap, bitmapCacheFile);
//        }

        if (DBG)
            Log.d(TAG, "savedBitmap.getWidth()=" + savedBitmap.getWidth()
                    + ", savedBitmap.getHeight()=" + savedBitmap.getHeight());

//        final Bitmap textureBm = Bitmap.createBitmap(getTexDim(), getTexDim(), Bitmap.Config.ARGB_8888);
//        int[] pixels = ByteBuffer.wrap(textureBm.mBuffer).asIntBuffer().array();

//        ByteBuffer bb = ByteBuffer.wrap(new byte[]{ 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 3 });
//        IntBuffer ib = bb.asIntBuffer();

//        int[] intArray = new int[getTexDim() * getTexDim()];
//        int[] content;
//        Bitmap textureBm = Bitmap.createBitmap(getTexDim(), getTexDim(), Bitmap.Config.ARGB_8888);
//        if (DBG)
//            Log.d(TAG, "prepareTexture. [mPcWidth=" + mPcWidth + ", getTexDim=" + getTexDim());
//        // + 1 because, / removes the tail.
//        // There could be an empty move, if the '%' is 0. So exclude the empty with the 'readSize' check.
//
//        if (mPcWidth >= getPcHeight()) {
////            for (int i = 0; i < mPcWidth / getTexDim() + 1; i++) {
////                int readWidth = Math.min(mPcWidth - i * getTexDim(), getTexDim());
////                int readHeight = getPcHeight();
////
////                if (readWidth != 0) {
////                    savedBitmap.getPixels(intArray, getPcHeight() * getTexDim() * i, getTexDim(), i * getTexDim(), 0, readWidth, readHeight);
////                }
////            }
//
//            int maxPicWidthPerTexture = getTexDim() / getPcHeight() * getTexDim();
//            int readWidth = Math.min(maxPicWidthPerTexture, mPcWidth);
//            int segments = readWidth / getTexDim();
//            if (readWidth % getTexDim() > 0)
//                segments++;
//            if (DBG)
//                Log.d(TAG, "maxPicWidthPerTexture= " + maxPicWidthPerTexture + ", readWidth= " + readWidth + ", segments= " + segments);
//            content = new int[Math.min(getTexDim(), mPcWidth)];
//            for (int i = 0; i < segments; i++){
//                int readSize = Math.min(readWidth - i * getTexDim(), getTexDim());
//                for (int j = 0; j < getPcHeight(); j++){
//                    if (DBG_READ)
//                        Log.d(TAG, "i= " + i + "j= " + j + ", i * getPcHeight() + j= " + (i * getPcHeight() + j));
//                    savedBitmap.getPixels(content, 0, getTexDim(), i * getTexDim(), j, readSize, 1);
//                    textureBm.setPixels(content, 0, getTexDim(), 0, i * getPcHeight() + j, readSize, 1);
//                }
//            }
//        } else { //mPcWidth < mPcHeight
//            if (DBG)
//                Log.d(TAG, "tall. mPcHeight = " + mPcWidth + ", getPcHeight()= " + getPcHeight()
//                        + ", texDim= " + getTexDim() + ", getPcHeight() / getTexDim() + 1= " + (getPcHeight() / getTexDim() + 1));
//
////            int readWidth = mPcWidth;
////            int readHeight = Math.min(getPcHeight(), getTexDim());
////            savedBitmap.getPixels(intArray, 0, getTexDim(), 0, 0, readWidth, readHeight);
//            content = new int[mPcWidth];
//            for (int i = 0; i < getPcHeight() / getTexDim() + 1; i++) {
//                int readHeight = Math.min(getPcHeight() - i * getTexDim(), getTexDim());
//                    for (int j = 0; j <readHeight ; j++) {
//                        savedBitmap.getPixels(content, 0, getTexDim(), 0, i * getTexDim() + j, mPcWidth, 1);
//                        textureBm.setPixels(content, 0, getTexDim(), i * mPcWidth, j, mPcWidth, 1);
//                    }
//            }
//
//        }

//        Bitmap textureBm = Bitmap.createBitmap(intArray, getTexDim(), getTexDim(), Bitmap.Config.ARGB_8888);

        if (DBG)
            Log.d(TAG, "textureBm.getWidth() = " + textureBm.getWidth() + ", textureBm.getHeight() = " + textureBm.getHeight());
        if (textureBm != null) {
            AppController.getInstance().addBitmapToMemoryCache(getKeyImgId(0), new MyBitmap(textureBm, mPcWidth, getPcHeight()));
        }

        if (DBG_PNG) {
            new File("/mnt/sdcard/mul").mkdir();
            QuadGenerator.toPng(textureBm, new File("/mnt/sdcard/mul/" + getKeyImgId(0)));
        }
        return true;

    }

    private void ensureTargetDirRoom() {
        File dataDir = new File("/data");
        long freeSize = dataDir.getUsableSpace() / 1024 / 1024;
        if (DBG) {
            Log.d(TAG, "data dir size By java.io.File : " + freeSize + "M");
        }

        if (freeSize != 0 && freeSize < 50) {
            clearDir(mContext.getCacheDir());
        }
    }

    public static void clearDir(File dir) {
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
//                if(f.isDirectory())
//                    clearDir(f);
//                else {
                boolean delFlag = f.delete();
                if (DBG)
                    Log.d(TAG, f.getName() + " del- " + delFlag);
                if (!delFlag) {
                    try {
                        FileInputStream fi = new FileInputStream(f);
                        fi.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    f.delete();
                }
//                }
            }
        }
        if (DBG) {
            File dataDir = new File("/data");
            float freeSize = dataDir.getUsableSpace() / 1024 / 1024;
            Log.d(TAG, "data dir available size after clearing : " + freeSize + "M");
        }
    }

    protected String getPngName() {
        return mTextBitmapHash.toString() + ".png";
    }

    @Override
    protected String getKeyImgId(int picIndex) {
        return getPngName();
    }

    public static class LineSegment {
        private int mWidth;
        private int mHeight;
        private String mText;
        private Rect mBounds;
        private int mEvenedHeight;

        // private String mLine;
        public LineSegment(String text, Rect bounds, int evenedHeight) {
            if (DBG)
                Log.d(TAG,
                        "LineSegment. text length=" + text.length() + ", bounds= " + bounds);
            mText = text; // + 1, as we are using exact indices.;
            mBounds = bounds;

//            // As by-product, calculate mWidth.
//            float measuredWidth = paint.measureText(mText);
//            if (DBG)
//                Log.d(TAG, "draw. [textBounds=" + mBounds + ", realTextWidth=" + (measuredWidth) + ", mText=" + mText);
//            final float myw = ensuredWidth(mBounds, measuredWidth);
//            mWidth = (int) myw;
            mEvenedHeight = evenedHeight;
        }

        /**
         * Draw is invoked before initShape.
         *
         * @param bitmapCanvas
         * @param paint
         * @param topAllText
         * @param count
         * @param length
         * @param savedBitmap
         * @param textureBm
         */
        public void draw(Canvas bitmapCanvas, Paint paint, int topAllText, int count, int length, Bitmap savedBitmap, Bitmap textureBm) {
//            paint.setLinearText(true);

            float baseline = (mEvenedHeight + mBounds.height() - 1.0f) / 2.0f - mBounds.bottom;

            if (DBG)
                Log.d(TAG, "draw. [paint antialias=" + paint.isAntiAlias()
                        + ", isLinear=" + paint.isLinearText() + ", topAllText = " + topAllText
                        + ",  bitmapCanvas.getHeight() = " + bitmapCanvas.getHeight() + ", mEvenedHeight =" + mEvenedHeight
                        + ", mBounds.height() = " + mBounds.height() + ", mBounds.bottom = " + mBounds.bottom + ", baseline = " + baseline);

            // Must align baseline.
            bitmapCanvas.save();
            bitmapCanvas.translate(0.f, (bitmapCanvas.getHeight() - mEvenedHeight) / 2.0f);
            int[] content;
            if (bitmapCanvas.getWidth() > bitmapCanvas.getHeight()) {
                String text;
                Rect bounds = new Rect();
                float measuredWidth;
                int maxPicWidthPerTexture = textureBm.getWidth() / bitmapCanvas.getHeight() * textureBm.getWidth();
                int textWidth, remainPicWidthPerTexture, beginXinTexture = 0, beginYinTexture = 0, remainAvailableWidthPerLine = 0, usedPicWidthPerTexture = 0;
                int readWidth, segments, readSize = 0;
                content = new int[textureBm.getWidth()];
                for (int i = 0; i < length; i += count) {

                    if (DBG)
                        Log.d(TAG, "i= " + i + ", count= " + count + ", (i + count)= " + (i + count) + ", length= " + length);

                    text = mText.substring(i, Math.min(i + count, length));
                    paint.getTextBounds(text, 0, text.length(), bounds);
                    measuredWidth = paint.measureText(text);
                    textWidth = (int) ensuredWidth(bounds, measuredWidth);
                    bitmapCanvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
                    bitmapCanvas.drawText(text, 0, text.length(), bounds.left < 0 ? -bounds.left : 0, baseline, paint);

                    if (DBG)
                        Log.d(TAG, "i= " + i + ", textWidth= " + textWidth + ", beginXinTexture= " + beginXinTexture
                                + ", beginYinTexture= " + beginYinTexture + ", text= " + text);

                    if (beginXinTexture > 0) {//setpixels in remain space
                        remainAvailableWidthPerLine = textureBm.getWidth() - beginXinTexture;
                        if (DBG)
                            Log.d(TAG, "textWidth= " + textWidth + ", remainAvailableWidthPerLine= " + remainAvailableWidthPerLine);
                        if (textWidth >= remainAvailableWidthPerLine) {
                            for (int k = 0; k < bitmapCanvas.getHeight(); k++) {
                                savedBitmap.getPixels(content, 0, savedBitmap.getWidth(), 0, k, remainAvailableWidthPerLine, 1);
                                textureBm.setPixels(content, 0, textureBm.getWidth(), beginXinTexture, beginYinTexture + k, remainAvailableWidthPerLine, 1);
                            }
                            beginXinTexture = 0;
                            beginYinTexture += bitmapCanvas.getHeight();
                            usedPicWidthPerTexture += remainAvailableWidthPerLine;
                            textWidth -= remainAvailableWidthPerLine;

                        } else {//textWidth < remainAvailableWidthPerLine
                            for (int k = 0; k < bitmapCanvas.getHeight(); k++) {
                                savedBitmap.getPixels(content, 0, savedBitmap.getWidth(), 0, k, textWidth, 1);
                                textureBm.setPixels(content, 0, textureBm.getWidth(), beginXinTexture, beginYinTexture + k, textWidth, 1);
                            }
                            beginXinTexture += textWidth;
                            usedPicWidthPerTexture += textWidth;
                            continue;

                        }

                    }

                    //beginXinTexture = 0
                    remainPicWidthPerTexture = maxPicWidthPerTexture - usedPicWidthPerTexture;
                    if (remainPicWidthPerTexture <= 0)
                        break;
                    readWidth = Math.min(remainPicWidthPerTexture, textWidth);
                    segments = readWidth / textureBm.getWidth();
                    if (readWidth % textureBm.getWidth() > 0)
                        segments++;
                    if (DBG)
                        Log.d(TAG, "maxPicWidthPerTexture= " + maxPicWidthPerTexture + ", readWidth= " + readWidth + ", segments= " + segments);
                    for (int j = 0; j < segments; j++) {
                        readSize = Math.min(readWidth - j * textureBm.getWidth(), textureBm.getWidth());
                        for (int k = 0; k < bitmapCanvas.getHeight(); k++) {
                            if (DBG_READ)
                                Log.d(TAG, "i= " + i + "j= " + j + ", k= " + k);
                            savedBitmap.getPixels(content, 0, savedBitmap.getWidth(),
                                    remainAvailableWidthPerLine + j * textureBm.getWidth(), k, readSize, 1);
                            textureBm.setPixels(content, 0, textureBm.getWidth(), 0, beginYinTexture + k, readSize, 1);
                        }
                        if ((j < segments - 1) || (j == (segments - 1) && readSize == textureBm.getWidth()))
                            beginYinTexture += bitmapCanvas.getHeight();
                    }

                    if (DBG_PNG) {
                        new File("/mnt/sdcard/mul").mkdir();
                        QuadGenerator.toPng(textureBm, new File("/mnt/sdcard/mul/" + i + ".png"));
                    }


                    if (DBG)
                        Log.d(TAG, "i= " + i + ", readWidth= " + readWidth + ", textWidth= " + textWidth + ", remainPicWidthPerTexture= " + remainPicWidthPerTexture);
                    if (remainPicWidthPerTexture <= textWidth)//texture not enough
                        break;

                    beginXinTexture = readSize % textureBm.getWidth();
                    usedPicWidthPerTexture += readWidth;
                }

            } else { //mPcWidth < mPcHeight
                bitmapCanvas.drawText(mText, mBounds.left < 0 ? -mBounds.left : 0, baseline, paint);
                content = new int[bitmapCanvas.getWidth()];
                int readHeight;
                for (int j = 0; j < bitmapCanvas.getHeight() / textureBm.getWidth() + 1; j++) {
                    readHeight = Math.min(bitmapCanvas.getHeight() - j * textureBm.getWidth(), textureBm.getWidth());
                    for (int k = 0; k < readHeight; k++) {
                        savedBitmap.getPixels(content, 0, savedBitmap.getWidth(), 0, j * textureBm.getWidth() + k, bitmapCanvas.getWidth(), 1);
                        textureBm.setPixels(content, 0, textureBm.getWidth(), j * bitmapCanvas.getWidth(), k, bitmapCanvas.getWidth(), 1);
                    }
                }

            }

            bitmapCanvas.restore();
            // bitmapCanvas.drawText(mText, bounds.left < 0 ? -bounds.left : 0, -bounds.top + 1.0f, paint);
        }

    }


    public void setupPaint() {
        mPaint.setTextSize(mTextSize);
        mPaint.setAntiAlias(AppController.getInstance().getCfg().isAntialias());
        if (DBG)
            Log.d(TAG, "setupPaint. [mPaint antialis=" + mPaint.isAntiAlias()
                    + ", linear=" + mPaint.isLinearText());
        // textPaint.setARGB(255, 255, 255, 255);
        mPaint.setColor(mColor);
        // If a hinting is available on the platform you are developing, you should enable it (uncomment the line below).
        // mPaint.setHinting(Paint.HINTING_ON);
        // mPaint.setSubpixelText(true); //Bad, will draw bad text.
        mPaint.setXfermode(new PorterDuffXfermode(Mode.SCREEN));
    }

    public static float ensuredWidth(Rect bounds, float measuredWidth) {
        float myw;
        float left = bounds.left;

        // [Measure] -> left < 0
        // ^^.-------. -> measuredWidth
        // .---------. -> myw(1) [myw = -left + measuredWidth] myw > bounds.width (OK)

        // [MeasureMeasure] -> left < 0
        // ^^.---------. -> measuredWidth
        // .-----------. -> myw(1) [myw = -left + measuredWidth]
        // myw(2) [myw < bounds.width()]
        // .--------------. ->myw = bounds.width(); // myw(3) (FIXED)

        if (left < 0.0f) {
            // myw = -left + measuredWidth; // myw(1)
            // if (myw < bounds.width()) { // myw(2)
            // myw = bounds.width(); // myw(3)
            // }
            myw = Math.max(-left + measuredWidth, bounds.width());
        } else {
            // myw = left + bounds.width(); // myw(a1)
            // if (myw < measuredWidth) { // myw(a2)
            // myw = measuredWidth; // myw(a3)
            // }
            myw = Math.max(left + bounds.width(), measuredWidth);
        }
        // ^[MeasureMeasureMeasure] -> left >= 0
        // .------------------------. -> measuredWidth
        // .---------------------. ->myw(a1) myw = left + bounds.width();
        // myw < measuredWidth) { // myw(a2)
        // myw = measuredWidth; // myw(a3) (FIXED)

        // ^[MeasureMeasureMeasure] -> left >= 0
        // .--------------------. -> measuredWidth
        // .----------------------. ->myw(a1) myw = left + bounds.width(); myw > measuredWidth (OK)
        return myw;
    }

    public void setTextColor(int parseColor) {
        mColor = parseColor;
        // TODO Auto-generated method stub
    }

    public void setTextSize(float size) {
        mTextSize = size;
    }

    public Paint getPaint() {
        return mPaint;
    }

    public void setRepeatCount(int repeatCount) {
        mRepeatCount = repeatCount;
    }

    public void setStyle(int style) {
        getPaint().setTypeface(Typeface.defaultFromStyle(style));
    }

    public void setTypeface(String fontName, int style) {
        if (DBG)
            Log.d(TAG, "setFont. [fontName=" + fontName + ", style=" + style);

        // getPaint().setTypeface(AppController.getInstance().getTypeface(fontName));
        setTypeface(AppController.getInstance().getTypeface(fontName), style);
    }

    public void setTypeface(Typeface tf, int style) {
        if (DBG) {
            Log.d(TAG, "setTypeface style=" + style);
        }
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }


            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            // Change to style - HMH instead of need.

            mPaint.setFakeBoldText((style & Typeface.BOLD) != 0);

            if (DBG) {
                Log.d(TAG, "FakeBoldText=" + ((style & Typeface.BOLD) != 0) + ", isFakeBoldText=" + mPaint.isFakeBoldText());
            }

//            mPaint.setSt
            mPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
            setTypeface(tf);
        } else {
            if (DBG) {
                Log.d(TAG, "setTypeface, style=0");
            }
            mPaint.setFakeBoldText(false);
            mPaint.setTextSkewX(0);
            setTypeface(tf);
        }
    }

    public void setTypeface(Typeface tf) {
        if (mPaint.getTypeface() != tf) {
            mPaint.setTypeface(tf);
        }
    }

    public void setTextItemBitmapHash(HashCode textBitmapHash) {
        mTextBitmapHash = textBitmapHash;
    }

}
