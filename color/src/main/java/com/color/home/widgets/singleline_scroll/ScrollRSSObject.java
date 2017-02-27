package com.color.home.widgets.singleline_scroll;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;

import com.color.home.AppController;
import com.color.home.Constants;
import com.color.home.R;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.multilines.MultiPicScrollObject;
import com.color.home.widgets.singleline.QuadGenerator;
import com.color.home.widgets.singleline.QuadSegment;
import com.color.home.widgets.singleline.cltjsonutils.CltJsonUtils;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndContent;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.FeedException;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.SyndFeedInput;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Administrator on 2017/2/10.
 */
public class ScrollRSSObject extends SinglineScrollObject {
    private final static String TAG = "ScrollRSSObject";
    private static final boolean DBG = false;

    private HandlerThread mCltHandlerThread;
    private Handler mCltHandler;
    private Runnable mCltRunnable;
    CltJsonUtils mCltJsonUtils;

    private String mUrl = "";
    private String mText = "";
    private long mUpdateInterval = 3600000;
    private String mBackColor;
    private String mTextColor;
    private String mImageMd5;

    private boolean mNeedChangeTexture = false;
    private int mCurTextId = 0;

    private List<String> mFilters;
    private SyndFeed mFeed;
    private Bitmap mTextureBm;

    public ScrollRSSObject(Context context, String url) {
        super(context);
        mUrl = url;
        mCltJsonUtils = new CltJsonUtils(context);
    }

    @Override
    public boolean update() {
        genTexs();

        String rssContent = mCltJsonUtils.getContentFromNet(mUrl, null);
        if (!TextUtils.isEmpty(rssContent) && !Constants.NETWORK_EXCEPTION.equals(rssContent)) {

                initFeed(rssContent);
                mText = getText();

        } else {
            mText = mContext.getString(R.string.netException) + " ";
        }

        mCltHandlerThread = new HandlerThread("color-net-thread");
        mCltHandlerThread.start();
        mCltHandler = new Handler(mCltHandlerThread.getLooper());

        initCltRunnable();
        if (mUpdateInterval > 0) {
            mCltHandler.removeCallbacks(mCltRunnable);
            mCltHandler.postDelayed(mCltRunnable, mUpdateInterval);
        }

        if (!prepareTexture()) {
            return false;
        }

        initgl(0, 0);

        return true;


    }

    private void initCltRunnable() {
        mCltRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (DBG)
                        Log.d(TAG, "mCltRunnable. Thread= " + Thread.currentThread().getName());

                    String rssContent = mCltJsonUtils.getContentFromNet(mUrl, null);
                    if (!TextUtils.isEmpty(rssContent) && !Constants.NETWORK_EXCEPTION.equals(rssContent)) {

                        initFeed(rssContent);
                        String resultText = getText();
                        if (DBG)
                            Log.d(TAG, "(resultText == null)? " + (resultText == null)
                                    + ", mText.equals(resultText))? " + (mText.equals(resultText)));

                        if (resultText != null && !resultText.equals(mText)) {
                            Log.d(TAG, "the text had updated, need to change texture.");

                            mText = resultText;
                            mNeedChangeTexture = true;

                        } else {
                            if (mFilters.contains("image") && (mFeed != null && mFeed.getImage() != null && !TextUtils.isEmpty(mFeed.getImage().getUrl()))) {

                                byte[] bytes = mCltJsonUtils.getBitmapBytes(mFeed.getImage().getUrl());
                                if (bytes != null && !mImageMd5.equals(getMd5FromByte(bytes))) {
                                        Log.d(TAG, "the image had updated, need to change texture.");
                                    mNeedChangeTexture = true;

                                } else
                                    Log.d(TAG, "the text and image had not updated, needn't change texture.");

                            } else
                                Log.d(TAG, "the data had not updated, needn't change texture.");

                        }
                    } else {
                        Log.d(TAG, "the rss content is null or the network had exception, needn't change texture.");
                    }

                    if (mCltHandler != null && mCltHandlerThread != null && mUpdateInterval > 0) {
                        mCltHandler.removeCallbacks(this);
                        mCltHandler.postDelayed(this, mUpdateInterval);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private void initFeed(String rssContent) {
        InputStreamReader inputStreamReader = new InputStreamReader(new ByteArrayInputStream(rssContent.getBytes()));
        SyndFeedInput syndFeedInput = new SyndFeedInput();
        try {
            mFeed = null;
            mFeed = syndFeedInput.build(inputStreamReader);

            if (DBG)
                Log.d(TAG, "feed type= " + mFeed.getFeedType() + ", feed categories= " + mFeed.getCategories()
                        + ", Thread= " + Thread.currentThread());
        } catch (FeedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    @Override
    protected boolean prepareTexture() {

        mTexDim = -1;
        mBeginXinTexture = 0;
        mBeginYinTexture = 0;
        if (DBG)
            Log.d(TAG, "prepareTexture. Thread= " + Thread.currentThread());

        TextView textView = new TextView(mContext);
        initTextView(4096, textView);

        textView.setText(mText);
        textView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        textView.setDrawingCacheEnabled(true);

        Layout layout = textView.getLayout();

        //init mPcWidth, mPcHeight
        int pcWidth = 0;
        if (DBG)
            Log.d(TAG, "layout= " + layout + ", textView.getLineCount()= " + textView.getLineCount()
                    + ", textView.getText()= " + textView.getText());
        int lineCount = textView.getLineCount();
        int maxTextHeight = 0, maxTextTopToBaseline = 0;
        Rect lineBounds = new Rect();
        int height;
        for (int i = 0; i < lineCount; i++) {
            pcWidth += Math.ceil(layout.getLineWidth(i));

            textView.getPaint().getTextBounds(mText, layout.getLineStart(i), layout.getLineEnd(i), lineBounds);
            if (DBG)
                Log.d(TAG, "i= " + i
                        + ", layout.getLineWidth(i)= " + layout.getLineWidth(i)
                        + ", " + layout.getLineLeft(i)
                        + ", pcWidth= " + pcWidth + ", lineBounds= " + lineBounds
                        + ", lineBounds.height()= " + lineBounds.height()
                        + ", layout.getLineDescent(i)= " + layout.getLineDescent(i)
                        + ", layout.getLineAscent(i)= " + layout.getLineAscent(i)
                        + ", end string= " + mText.substring(Math.max(0, layout.getLineEnd(i) - 4), layout.getLineEnd(i)));


            height = layout.getLineDescent(i) - layout.getLineAscent(i);
            if (lineBounds.height() <= height) {
                if (lineBounds.height() > maxTextHeight) {
                    maxTextHeight = lineBounds.height();
                    maxTextTopToBaseline = -lineBounds.top;
                }
            } else {
                if (height > maxTextHeight) {
                    maxTextHeight = height;
                    maxTextTopToBaseline = -layout.getLineAscent(i);
                }
            }
        }

        if (DBG) {
            Paint.FontMetrics fm = textView.getPaint().getFontMetrics();
            Log.d(TAG, "FontMetrics:  ascent= " + fm.ascent + ", descent= " + fm.descent
                    + ", top= " + fm.top + ", bottom= " + fm.bottom
                    + ", " + textView.getScaleX());
        }
        mPcHeight = (maxTextHeight % 2 == 0) ? maxTextHeight : (maxTextHeight + 1);

        Bitmap image = null;
        if (mFilters.contains("image") && (mFeed != null && mFeed.getImage() != null && !TextUtils.isEmpty(mFeed.getImage().getUrl()))) {
            byte[] bytes = mCltJsonUtils.getBitmapBytes(mFeed.getImage().getUrl());
            if (bytes != null) {
                mImageMd5 = getMd5FromByte(bytes);
                image = getSuitableBitmap(bytes, getPcHeight());
                if (image != null) {
                    if (DBG)
                        Log.d(TAG, "image.getWidth= " + image.getWidth());

                    pcWidth += image.getWidth();
                }

            }
        } else if (DBG)
            Log.d(TAG, "filters contains image? " + mFilters.contains("image"));

        mPcWidth = pcWidth;
        if (DBG)
            Log.d(TAG, "image= " + image + ", mPcWidth= " + mPcWidth + ", mPcHeight= " + mPcHeight);

        if (mPcWidth == 0) {
            Log.d(TAG, ("the Pcwidth is zero, do not display anyting."));
            return false;

        } else if (mPcHeight == 0) {
            Log.d(TAG, "the Pcheight is zero, do not display anyting.");
            return false;

        }

        if (mPcWidth < mPcHeight) {
            //there is only one line in texture when pcWidth < pcHeight
            mTexDim = getTexDimOfTallPic(mPcHeight);
        }

        if (!MultiPicScrollObject.isMemoryEnough(getTexDim()))
            return false;

        //draw image, text to texture
        mTextureBm = Bitmap.createBitmap(getTexDim(), getTexDim(), Bitmap.Config.ARGB_8888);
        int maxPicWidthPerTexture = mPcWidth, maxPicHeightPerTexture = mPcHeight;
        int[] content;

        if (mPcWidth >= mPcHeight) {

            if (DBG)
                Log.d(TAG, "mPcHeight= " + mPcHeight + ", getLineHeight= " + textView.getLineHeight()
                        + ", " + layout.getHeight() / textView.getLineCount()
                        + ", " + textView.getLineSpacingExtra()
                        + ", " + textView.getLineSpacingMultiplier());

            content = new int[mTextureBm.getWidth()];
            maxPicWidthPerTexture = getTexDim() / mPcHeight * getTexDim();

        } else {
            content = new int[mPcWidth];
            maxPicHeightPerTexture = getTexDim() / mPcWidth * getTexDim();
        }

        //set image pixels to texture
        if (image != null) {

            if (mPcWidth >= mPcHeight)
                setTextureBmPixelsOfFatPic(image, -1, (mPcHeight - image.getHeight()) / 2, content, maxPicWidthPerTexture, mTextureBm);

            else {
                if (image.getHeight() < mPcHeight)
                    mBeginYinTexture = (mPcHeight - image.getHeight()) / 2; //center_vertical

                if (image.getHeight() > 4096) {
                    Bitmap bitmap = Bitmap.createBitmap(image, 0, 0, image.getWidth(), (4096 - mBeginYinTexture));
                    if (bitmap != null) {
                        setTextureBmPixelsOfTallPic(bitmap, content, maxPicHeightPerTexture, mTextureBm);
                        bitmap.recycle();
                    }

                } else {
                    setTextureBmPixelsOfTallPic(image, content, maxPicHeightPerTexture, mTextureBm);
                }

                mBeginXinTexture = image.getWidth();
                mBeginYinTexture = 0;
            }

            image.recycle();

        }

        if (DBG) {
            Log.d(TAG, ", textView.getLayoutParams= " + textView.getLayoutParams()
                    + ", textView.getLayoutParams().width= " + textView.getLayoutParams().width
                    + ", layout= " + layout
                    + ",  textView.getWidth()= " + textView.getWidth()
                    + ", textView.getHeight()= " + textView.getHeight()
                    + ", textView.getLineCount()= " + textView.getLineCount()
                    + ", textView.getLineHeight()= " + textView.getLineHeight()
                    + ", mText.length()= " + mText.length()
                    + ", last line text start index= " + layout.getLineStart(lineCount - 1)
                    + ", last line text end index= " + layout.getLineEnd(lineCount - 1)
                    + ", last line text= " + mText.substring(layout.getLineStart(lineCount - 1),
                    layout.getLineEnd(lineCount - 1)));

        }

        if (mPcWidth >= mPcHeight) {
            //keep the distance between picTop and baseline of all lines in layout are equal
            int baselineToPicTop = (mPcHeight - maxTextHeight) / 2 + maxTextTopToBaseline;
            getFatPicAndDrawToTexture(textView, layout, lineCount, baselineToPicTop, lineBounds, mTextureBm, maxPicWidthPerTexture, content);

        } else {
            getTallPicAndDrawToTexture(textView, layout, lineBounds, mTextureBm, maxPicHeightPerTexture, content);

        }
        if (DBG)
            Log.d(TAG, "after draw cache bitmap to texBm, mBeginXinTexture= " + mBeginXinTexture + ", mBeginYinTexture= " + mBeginYinTexture);

        textView.destroyDrawingCache();
        textView.setDrawingCacheEnabled(false);

        if (DBG_PNG) {
            Log.d(TAG, "mTextureBm= " + mTextureBm);
            if (mTextureBm != null) {
                new File("/mnt/sdcard/mul").mkdir();
                QuadGenerator.toPng(mTextureBm, new File("/mnt/sdcard/mul/" + "textureBm.png"));
            }
        }

        return true;
    }

    private int getTexDimOfTallPic(int pcHeight) {
        return QuadGenerator.findClosestPOT(pcHeight, pcHeight);
    }

    private String getMd5FromByte(byte[] bytes) {

        StringBuilder sb = new StringBuilder();
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(bytes);
            byte[] result = digest.digest();

            for (byte x : result) {
                if ((x & 0xff) >> 4 == 0) {
                    sb.append("0").append(Integer.toHexString(x & 0xff));
                } else {
                    sb.append(Integer.toHexString(x & 0xff));
                }
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        if (DBG)
            Log.d(TAG, "md5= " + sb.toString());
        return sb.toString();
    }

    private void getTallPicAndDrawToTexture(TextView textView, Layout layout, Rect lineBounds, Bitmap textureBm, int maxPicHeightPerTexture, int[] content) {

        //only get drawing cache of the first line
        textView.getPaint().getTextBounds(mText, layout.getLineStart(0), layout.getLineEnd(0), lineBounds);
        if (DBG)
            Log.d(TAG, "lineBounds= " + lineBounds + ", lineBounds.height()= " + lineBounds.height());

        textView.scrollTo(0, layout.getLineBaseline(0) + lineBounds.top);

        //there is just one line in texture.
        int realHeightToShow = Math.min(4096, lineBounds.height());

        if (layout.getLineWidth(0) * realHeightToShow * 4 > ViewConfiguration.get(mContext).getScaledMaximumDrawingCacheSize()) {

            if (DBG)
                Log.d(TAG, " View too large to fit into drawing cache, " +
                        "needs " + (layout.getLineWidth(0) * realHeightToShow * 4) + " bytes" +
                        ", only " + ViewConfiguration.get(mContext).getScaledMaximumDrawingCacheSize() + " available" +
                        ", scroll textView and draw cache bitmap to texture.");

            getSuitableCacheFromTallAndDrawToTexture(layout.getLineWidth(0), realHeightToShow,
                    content, maxPicHeightPerTexture, textView, textureBm);

        } else {
            if (DBG)
                Log.d(TAG, "it is able to get drawingCache from textView.");
            textView.layout(0, 0, (int) layout.getLineWidth(0), realHeightToShow);
            Bitmap cacheBitmap = textView.getDrawingCache();
            if (cacheBitmap != null)
                setTextureBmPixelsOfTallPic(cacheBitmap, content, maxPicHeightPerTexture, textureBm);

        }

    }

    private void getSuitableCacheFromTallAndDrawToTexture(float lineWidth, int lineHeightToShow, int[] content, int maxPicHeightPerTexture, TextView textView, Bitmap textureBm) {

        int maxCacheHeight = (int) (ViewConfiguration.get(mContext).getScaledMaximumDrawingCacheSize() / 4 / lineWidth);
        int segments = lineHeightToShow / maxCacheHeight;
        if (lineHeightToShow % maxCacheHeight > 0)
            segments++;
        if (DBG)
            Log.d(TAG, "getSuitableCacheFromTallAndDrawToTexture. lineWidth= " + lineWidth
                    + ", maxCacheHeight= " + maxCacheHeight + ", segments= " + segments);

        for (int i = 0; i < segments; i++) {

            if (DBG)
                Log.d(TAG, "getSuitableCacheFromFatAndDrawToTexture. i= " + i);

            if (mBeginXinTexture > (getTexDim() - mPcWidth)) {
                if (DBG)
                    Log.d(TAG, "texture is not enough.");
                break;
            }

            if (i > 0) {
                textView.destroyDrawingCache();
                textView.scrollBy(0, maxCacheHeight);
            }

            if (i == segments - 1)
                textView.layout(0, 0, (int) lineWidth, lineHeightToShow - i * maxCacheHeight);
            else
                textView.layout(0, 0, (int) lineWidth, maxCacheHeight);

            if (textView.getDrawingCache() != null) {
                if (DBG)
                    Log.d(TAG, "textView.getDrawingCache()=[ " + textView.getDrawingCache().getWidth()
                            + ", " + textView.getDrawingCache().getHeight());
                setTextureBmPixelsOfTallPic(textView.getDrawingCache(), content, maxPicHeightPerTexture, textureBm);

            }
        }
    }

    private void getFatPicAndDrawToTexture(TextView textView, Layout layout, int lineCount, int baselineToPicTop, Rect lineBounds, Bitmap textureBm, int maxPicWidthPerTexture, int[] content) {

        int cacheBitmapTopToBeginY, textHeight;
        for (int j = 0; j < lineCount; j++) {

            if (DBG)
                Log.d(TAG, "textview.getlayout: j= " + j
                        + ", getLineWidth= " + layout.getLineWidth(j)
                        + ", mBeginXinTexture= " + mBeginXinTexture
                        + ", mBeginYinTexture= " + mBeginYinTexture
                        + ", getLineTop= " + layout.getLineTop(j)
                        + ", getLineAscent= " + layout.getLineAscent(j)
                        + ", getLineBaseline= " + layout.getLineBaseline(j)
                        + ", getLineBottom= " + layout.getLineBottom(j)
                        + ", getLineDescent= " + layout.getLineDescent(j)
                        + ", getLineVisibleEnd= " + layout.getLineVisibleEnd(j));

            textView.getPaint().getTextBounds(mText, layout.getLineStart(j),
                    layout.getLineEnd(j), lineBounds);

            //
            textHeight = Math.min(lineBounds.height(), layout.getLineDescent(j) - layout.getLineAscent(j));

            // get cache bitmap
            cacheBitmapTopToBeginY = baselineToPicTop + Math.max(lineBounds.top, layout.getLineAscent(j));
            textView.scrollTo(0, Math.max(layout.getLineBaseline(j) + lineBounds.top,
                    layout.getLineBaseline(j) + layout.getLineAscent(j)));

            if (DBG)
                Log.d(TAG, "lineBounds= " + lineBounds + ", lineBounds.height()= " + lineBounds.height()
                        + ", cacheBitmapTopToBeginY= " + cacheBitmapTopToBeginY
                        + ", textView.getY  ()= " + textView.getY()
                        + ", textHeight= " + textHeight);

            if (layout.getLineWidth(j) * textHeight * 4 > ViewConfiguration.get(mContext).getScaledMaximumDrawingCacheSize()) {

                if (DBG)
                    Log.d(TAG, "j= " + j + ", View too large to fit into drawing cache, " +
                            "needs " + (layout.getLineWidth(0) * textHeight * 4) + " bytes" +
                            ", only " + ViewConfiguration.get(mContext).getScaledMaximumDrawingCacheSize() + " available" +
                            ", scroll textView and draw cache bitmap to texture.");

                getSuitableCacheFromFatAndDrawToTexture(layout.getLineWidth(j), cacheBitmapTopToBeginY, textHeight,
                        content, maxPicWidthPerTexture, textView, textureBm);

            } else {
                if (DBG)
                    Log.d(TAG, "j= " + j + ", it is able to get drawingCache from textView.");
                textView.layout(0, 0, (int) layout.getLineWidth(j), textHeight);
                Bitmap cacheBitmap = textView.getDrawingCache();
                if (cacheBitmap != null)
                    setTextureBmPixelsOfFatPic(cacheBitmap, -1, cacheBitmapTopToBeginY, content, maxPicWidthPerTexture, textureBm);

            }

            if (mBeginYinTexture > (getTexDim() - getPcHeight())) {
                if (DBG)
                    Log.d(TAG, "texture is not enough.");
                break;
            }

        }
    }

    private void setTextureBmPixelsOfTallPic(Bitmap bitmap, int[] content, int maxPicHeightPerTexture, Bitmap textureBm) {

        if (DBG)
            Log.d(TAG, "setTextureBmPixelsOfTallPic. bitmap.width= " + bitmap.getWidth() + ", bitmap.height= " + bitmap.getHeight()
                    + ", mBeginXinTexture= " + mBeginXinTexture + ", mBeginYinTexture= " + mBeginYinTexture);

        if (bitmap.getWidth() > (getTexDim() - mBeginXinTexture)) {
            if (DBG)
                Log.d(TAG, "texture is not enough.");
            return;
        }

        int remainAvailableHeightPerLine = getTexDim() - mBeginYinTexture;
        int needDrawHeight = bitmap.getHeight();
        int alreadyReadHeight = 0;

        if (remainAvailableHeightPerLine > 0) {//setpixels in remain space

            if (DBG)
                Log.d(TAG, "setTextureBmPixelsOfTallPic. needDrawHeight= " + needDrawHeight
                        + ", remainAvailableHeightPerLine= " + remainAvailableHeightPerLine
                        + ", mBeginXinTexture= " + mBeginXinTexture);
            if (needDrawHeight >= remainAvailableHeightPerLine) {
                if (DBG)
                    Log.d(TAG, "setTextureBmPixelsOfTallPic. needDrawHeight >= remainAvailableHeightPerLine.");

                for (int k = 0; k < remainAvailableHeightPerLine; k++) {
                    bitmap.getPixels(content, 0, bitmap.getWidth(), 0, k, bitmap.getWidth(), 1);
                    textureBm.setPixels(content, 0, getTexDim(), mBeginXinTexture, mBeginYinTexture + k, bitmap.getWidth(), 1);
                }
                alreadyReadHeight = remainAvailableHeightPerLine;
                needDrawHeight -= remainAvailableHeightPerLine;
                this.mBeginXinTexture += mPcWidth;
                mBeginYinTexture = 0;

            } else {//needDrawHeight < remainAvailableHeightPerLine
                if (DBG)
                    Log.d(TAG, "setTextureBmPixelsOfTallPic. needDrawHeight < remainAvailableHeightPerLine.");
                for (int k = 0; k < bitmap.getHeight(); k++) {
                    bitmap.getPixels(content, 0, bitmap.getWidth(), 0, k, bitmap.getWidth(), 1);
                    textureBm.setPixels(content, 0, getTexDim(), mBeginXinTexture, mBeginYinTexture + k, bitmap.getWidth(), 1);
                }
                mBeginYinTexture += needDrawHeight;
                return;
            }
        }

        if (DBG)
            Log.d(TAG, "setTextureBmPixelsOfTallPic." + ", needDrawHeight= " + needDrawHeight);
        if (needDrawHeight == 0) {
            if (DBG)
                Log.d(TAG, "setTextureBmPixelsOfTallPic. all bitmap pixels always set into textureBm.");
            return;
        }

        if (bitmap.getWidth() > (getTexDim() - mBeginXinTexture)) {
            if (DBG)
                Log.d(TAG, "texture is not enough.");
            return;
        }

        //beginYinTexture = 0
        if (DBG)
            Log.d(TAG, "mBeginYinTexture= " + mBeginYinTexture + ", maxPicHeightPerTexture= " + maxPicHeightPerTexture);

        int readHeight = Math.min(maxPicHeightPerTexture - (this.mBeginXinTexture / mPcWidth * getTexDim() + mBeginYinTexture), needDrawHeight);
        int segments = readHeight / mPcHeight;
        if (readHeight % mPcHeight > 0)
            segments++;

        if (DBG)
            Log.d(TAG, "maxPicHeightPerTexture= " + maxPicHeightPerTexture + ", readHeight= " + readHeight + ", segments= " + segments);

        int readSize = 0;
        for (int j = 0; j < segments; j++) {
            readSize = Math.min(readHeight - j * getTexDim(), getTexDim());

            if (DBG)
                Log.d(TAG, " mBeginXinTexture= " + mBeginXinTexture + ", readSize= " + readSize);
            for (int k = 0; k < readSize; k++) {
                if (DBG_READ)
                    Log.d(TAG, "j= " + j + ", k= " + k);
                bitmap.getPixels(content, 0, bitmap.getWidth(), 0, alreadyReadHeight + j * getTexDim() + k, bitmap.getWidth(), 1);
                textureBm.setPixels(content, 0, getTexDim(), mBeginXinTexture, mBeginYinTexture + k, bitmap.getWidth(), 1);
            }
            if ((j < segments - 1) || (j == (segments - 1) && readSize == getTexDim()))
                this.mBeginXinTexture += mPcWidth;
        }

        mBeginYinTexture = readSize % getTexDim();

    }

    private void initTextView(int width, TextView textView) {
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(-2, -2);
        textView.setLayoutParams(params);
        textView.setWidth(width);
        textView.setHeight(getPcHeight());
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.getPaint().setAntiAlias(AppController.getInstance().getCfg().isAntialias());
        textView.setBackgroundColor(GraphUtils.parseColor(mBackColor));
        textView.setTextColor(GraphUtils.parseColor(mTextColor));
        textView.setTextSize(mTextSize);
    }

    private void getSuitableCacheFromFatAndDrawToTexture(float lineWidth, int cacheBitmapTopToBeginY, int textHeight, int[] content,
                                                         float maxPicWidthPerTexture, TextView textView, Bitmap textureBm) {

        int maxCacheWidth = ViewConfiguration.get(mContext).getScaledMaximumDrawingCacheSize() / 4 / textHeight;
        int segments = (int) (lineWidth / maxCacheWidth);
        if (lineWidth % maxCacheWidth > 0)
            segments++;
        if (DBG)
            Log.d(TAG, "getSuitableCacheFromFatAndDrawToTexture. lineWidth= " + lineWidth
                    + ", maxCacheWidth= " + maxCacheWidth + ", segments= " + segments);

        for (int i = 0; i < segments; i++) {

            if (DBG)
                Log.d(TAG, "getSuitableCacheFromFatAndDrawToTexture. i= " + i);

            if (mBeginYinTexture > (getTexDim() - getPcHeight())) {
                if (DBG)
                    Log.d(TAG, "texture is not enough.");
                break;
            }

            if (i > 0) {
                textView.destroyDrawingCache();
                textView.scrollBy(maxCacheWidth, 0);
            }

            if (i == segments - 1)
                textView.layout(0, 0, (int) (Math.ceil(lineWidth) - i * maxCacheWidth), textHeight);
            else
                textView.layout(0, 0, maxCacheWidth, textHeight);

            if (textView.getDrawingCache() != null) {
                if (DBG)
                    Log.d(TAG, "textView.getDrawingCache()=[ " + textView.getDrawingCache().getWidth()
                            + ", " + textView.getDrawingCache().getHeight());
                setTextureBmPixelsOfFatPic(textView.getDrawingCache(), -1, cacheBitmapTopToBeginY, content, (int) maxPicWidthPerTexture, textureBm);

            }
        }

    }


    private void initgl(int pageTexIndex, int texId) {
        updatePageToTexId(pageTexIndex, texId);

        if (DBG)
            Log.i(TAG, "bmpSize[" + mPcWidth + ", " + getPcHeight() + "]");

        mRealReadPcWidth = getRealReadPcWidth(mPcWidth, mPcHeight, mTexDim);
        initShapes();

        setupMVP();

        Matrix.setIdentityM(mMMatrix, 0);
        // Matrix.translateM(mMMatrix, 0, thePosition.x, thePosition.y, thePosition.z);

        // GLES20.glUseProgram(mProgram);

        GLES20.glUniform2f(muTexScaleHandle, (float) mPcWidth, (float) getPcHeight());


        GLES20.glUniformMatrix4fv(muMMatrixHandle, 1, false, mMMatrix, 0);

        // Prepare the triangle data
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 12, mQuadVB);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        // Prepare the triangle data
        GLES20.glVertexAttribPointer(maTexCoordsHandle, 3, GLES20.GL_FLOAT, false, 12, mQuadTCB);
        GLES20.glEnableVertexAttribArray(maTexCoordsHandle);

        // Draw the triangle
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    protected void updateImage() {
        if (DBG)
            Log.d(TAG, "updateImage. mTextureBm= " + mTextureBm);
        if (mTextureBm != null){
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mTextureBm, 0);
            mTextureBm.recycle();
            mTextureBm = null;
            System.gc();
        }
    }

    private String getText() {
        StringBuilder stringBuilder = new StringBuilder();

        if (mFeed != null) {

            //pubdate
            if (mFilters.contains("pubdate") && mFeed.getPublishedDate() != null) {
                stringBuilder.append(mFeed.getPublishedDate() + " ");
            }

            //title, description
            boolean isShowDescrip = mFilters.contains("description");
            List<SyndEntry> entries = mFeed.getEntries();
            if (entries != null && entries.size() > 0) {
                if (DBG)
                    Log.d(TAG, "entries.size()= " + entries.size());

                Iterator<SyndEntry> iterator = entries.iterator();
                while (iterator.hasNext()) {
                    stringBuilder.append(" ");
                    SyndEntry entry = iterator.next();

                    if (!TextUtils.isEmpty(entry.getTitle())) {
                        stringBuilder.append(entry.getTitle());
                    }

                    if (isShowDescrip) {
                        stringBuilder.append(" ");

                        if (entry.getDescription() != null && !TextUtils.isEmpty(entry.getDescription().getValue())) {//rss/item/description
                            stringBuilder.append(entry.getDescription().getValue());

                        } else if (entry.getDescription() == null && entry.getContents() != null && entry.getContents().size() > 0) {//feed/entry/content
                            List<SyndContent> contents = entry.getContents();
                            if (DBG)
                                Log.d(TAG, "contents.size= " + contents.size());
                            for (SyndContent content : contents) {
                                stringBuilder.append(content.getValue());
                            }
                        }
                        stringBuilder.append(" ");
                    }

                }

            }
        } else {
            Log.d(TAG, "feed is null.");
        }

        return StringEscapeUtils.unescapeXml(stringBuilder.toString().replaceAll("<[^>]*>", ""));

    }

    @Override
    protected void setupPaint(Paint paint) {
        super.setupPaint(paint);

        paint.setColor(GraphUtils.parseColor(mTextColor));

        paint.setFlags(0);
        setTypeface(paint, AppController.getInstance().getTypeface("default"), 0);
    }

    @Override
    protected void setGLColor() {
        // R G B A.
        // GLES20.glClearColor(1.0f, 0.5f, 0.5f, 1.0f);
        //TODO::背景颜色(透明)
        int backgroundColor = GraphUtils.parseColor(mBackColor);//
        final float red = Color.red(backgroundColor) / 255.0f;
        final float green = Color.green(backgroundColor) / 255.0f;
        final float blue = Color.blue(backgroundColor) / 255.0f;
        final float alpha = Color.alpha(backgroundColor) / 255.0f;
        if (DBG)
            Log.d(TAG, "init. [r=" + red + ", g=" + green + ", b=" + blue + ", alpha=" + alpha);
        GLES20.glClearColor(red, green, blue, alpha);
    }

    @Override
    protected void genQuadSegs() {
        if (mPcWidth >= mPcHeight)
            super.genQuadSegs();
        else {
            if (DBG)
                Log.d(TAG, "mPcWidth < mPcHeight. genQuadSegs. [");
            QuadGenerator qg = new QuadGenerator(mPcWidth, Math.min(getPcHeight(), 4096), getTexDim(), mEvenedWidth, true);
            final int repeatedQuadsSize = qg.getRepeatedQuadsSize();
            if (DBG)
                Log.d(TAG, "repeatedQuadsSize= " + repeatedQuadsSize);

            mQuadSegs = new QuadSegment[repeatedQuadsSize];
            for (int i = 0; i < repeatedQuadsSize; i++) {
                mQuadSegs[i] = qg.getQuad(i, true);
            }
        }
    }

    @Override
    public void render() {

        if (mNeedChangeTexture) {
            if (DBG)
                Log.d(TAG, "render. need change texture. mCurTextId= " + mCurTextId);

            mNeedChangeTexture = false;

            try {
                if (mCurTextId == 0)
                    mCurTextId = 1;
                else mCurTextId = 0;

                if (prepareTexture())
                    initgl(0, mCurTextId);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        super.render();
    }

    protected void genTexs() {

        if (DBG)
            Log.d(TAG, "genTexs. [");

        mTexIds = new int[2];

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        SinglelineScrollRenderer.checkGLError("glActiveTexture");
        GLES20.glGenTextures(mTexIds.length, mTexIds, 0);
        SinglelineScrollRenderer.checkGLError("glGenTextures");
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(muTextureHandle, 0);
        SinglelineScrollRenderer.checkGLError("glUniform1i");

        // As byproduct, the 1 text is binded.
        for (int i = 0; i < mTexIds.length; i++) {
            initTexParam(i);
        }
    }

    private Bitmap getSuitableBitmap(byte[] bytes, int height) {

        if (bytes == null) {
            if (DBG)
                Log.d(TAG, "getArtworkQuick. bytes is null.");
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if (DBG)
            Log.d(TAG, "options=[" + options.outWidth + ", " + options.outHeight);

        int sampleSize = 1;
        int newHeight = options.outHeight >> 1;
        while (newHeight > height) {
            sampleSize <<= 1;
            newHeight >>= 1;
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        options.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if (DBG)
            Log.d(TAG, "getBitmapBytes. mPcHeight= " + getPcHeight()
                    + ", before scale, bitmap=[" + bitmap.getWidth() + ", " + bitmap.getHeight());

        if (bitmap.getHeight() > height) {
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            // 计算宽高缩放率
            float scaleWidth = height / ((float) bitmap.getHeight());
            float scaleHeight = scaleWidth;
            if (DBG)
                Log.d(TAG, "scaleWidth= " + scaleWidth);
            // 缩放图片动作
            matrix.postScale(scaleWidth, scaleHeight);

            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (DBG)
                Log.d(TAG, "getBitmap. newBitmap width= " + newBitmap.getWidth() + ", height= " + newBitmap.getHeight());

            bitmap.recycle();
            return newBitmap;
        }
        return bitmap;
    }


    public void reloadRSSContent() {
        try {

            if (DBG)
                Log.d(TAG, "reloadRSSContent. mCltHandler= " + mCltHandler);
            if (mCltHandler != null && mCltRunnable != null) {
                mCltHandler.removeCallbacks(mCltRunnable);
                mCltHandler.post(mCltRunnable);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeCltRunnable() {

        if (DBG)
            Log.d(TAG, "removeCltRunnable. mCltHandler= " + mCltHandler + ", mCltHandlerThread= " + mCltHandlerThread);

        if (mCltHandler != null) {
            mCltHandler.removeCallbacks(mCltRunnable);
        }
        if (mCltHandlerThread != null) {
            mCltHandlerThread.quit();
        }

        mCltHandler = null;
        mCltHandlerThread = null;
    }


    public void setBgColor(String bgColor) {
        mBackColor = bgColor;
    }

    public void setTextColor(String textColorStr) {
        mTextColor = textColorStr;
    }

    public void setFilters(List<String> filters) {
        mFilters = filters;
    }

    public void setInterval(long interval) {
        mUpdateInterval = interval;
    }
    private int getAppVersion() {

        try {
            PackageInfo info = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }
}
