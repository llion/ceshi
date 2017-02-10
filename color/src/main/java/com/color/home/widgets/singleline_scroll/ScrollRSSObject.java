package com.color.home.widgets.singleline_scroll;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.Constants;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.singleline.QuadGenerator;
import com.color.home.widgets.singleline.cltjsonutils.CltJsonUtils;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import okhttp3.CacheControl;
import okhttp3.HttpUrl;

/**
 * Created by Administrator on 2017/2/10.
 */
public class ScrollRSSObject extends SinglineScrollObject {
    private final static String TAG = "ScrollRSSObject";
    private static final boolean DBG = false;
    private final HttpUrl mHttpUrl;
    RSSParser.RssProgram mRssProgram = null;
    List<RSSParser.RssItem> mRssItems;

    private HandlerThread mCltHandlerThread;
    private Handler mCltHandler;
    private Runnable mCltRunnable;
    CltJsonUtils mCltJsonUtils;
    private String mText = "";
    private long mUpdateInterval = 3600000;
    private String mBackColor = "0x00000000";
    private Paint mPaint;

    private boolean mNeedChangeTexture = false;
    private CacheControl mCacheControl;
    private int mCurTextId = 0;
    private String mTextColor = "0xFFFFFFFF";


    public ScrollRSSObject(Context context, HttpUrl httpUrl) {
        super(context);
        mHttpUrl = httpUrl;
        mCltJsonUtils = new CltJsonUtils(context);

    }

    @Override
    public boolean update() {
        genTexs();
        mCacheControl = new CacheControl.Builder().noCache().build();
        mPaint = new Paint();
        setupPaint(mPaint);

        String intervalStr = mHttpUrl.queryParameter("interval");
        if (!TextUtils.isEmpty(intervalStr)) {
            try {
                mUpdateInterval = Long.parseLong(intervalStr);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        prepareThread(mUpdateInterval);

        String rssContent = mCltJsonUtils.getContentFromNet(mHttpUrl.toString(), null, mCacheControl);

        if (!initRSSItems(rssContent))
            return false;

        if (mRssItems.size() == 0 || !prepareTexture()) {
            return false;
        }


        initgl(0, 0);

        return true;


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

    private boolean initRSSItems(String rssContent) {

        RSSParser rssParser = new RSSParser();
        InputStream is = new ByteArrayInputStream(rssContent.getBytes());
        try {
            mRssProgram = rssParser.parse(is);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        if (mRssProgram == null) {
            Log.d(TAG, "initRSSItems. program is null. ");
            return false;
        }

        mRssItems = mRssProgram.rssItems;
        return true;
    }

    private void prepareThread(long delayMillis) {

        if (DBG)
            Log.d(TAG, "prepareThread. mCltHandler= " + mCltHandler + ", delayMillis= " + delayMillis);

        if (mCltHandler == null) {
            mCltHandlerThread = new HandlerThread("another-thread");
            mCltHandlerThread.start();
            mCltHandler = new Handler(mCltHandlerThread.getLooper());
        }

        mCltRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (DBG)
                        Log.d(TAG, "mCltRunnable. Thread= " + Thread.currentThread().getName());

                    String rssContent = mCltJsonUtils.getContentFromNet(mHttpUrl.toString(), null, mCacheControl);

                    if (!Constants.NETWORK_EXCEPTION.equals(rssContent)) {

                        if (initRSSItems(rssContent)) {

                            if (!TextUtils.isEmpty(getText()) && !getText().equals(mText)) {
                                if (DBG)
                                    Log.d(TAG, "the data had updated.");

                                mText = getText();
                                mKey = "";
                                mNeedChangeTexture = true;
                            } else {
                                if (DBG)
                                    Log.d(TAG, "the data had not updated, needn't change texture." +
                                            ", mText= " + mText);
                            }
                        }

                    } else {
                        if (DBG)
                            Log.d(TAG, "network exception, needn't change texture.");
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

        if (mCltHandler != null && mCltHandlerThread != null) {
            mCltHandler.removeCallbacks(mCltRunnable);
            mCltHandler.postDelayed(mCltRunnable, delayMillis);
        }
    }

    @Override
    protected boolean prepareTexture() {

        mText = getText();

        if (DBG)
            Log.d(TAG, "mText= " + mText);

        if (TextUtils.isEmpty(mText))
            return false;

        String backColorStr = mHttpUrl.queryParameter("backcolor");
        if (!TextUtils.isEmpty(backColorStr))
            mBackColor = backColorStr;

        getTextBitmapAndDrawToTexture();

        return true;

    }

    private String getText() {
        String text = "";

        for (int i = 0; i < 3; i++) {
            RSSParser.RssItem rssItem = mRssItems.get(i);
            if (!TextUtils.isEmpty(rssItem.title)) {
                text += ("Title:" + rssItem.title);
            }
            if (!TextUtils.isEmpty(rssItem.description)) {
                text += ("Description:" + rssItem.description);
            }
        }

        return text;
    }

    private void getTextBitmapAndDrawToTexture() {
        Rect bounds = new Rect();
        mPaint.getTextBounds(mText, 0, mText.length(), bounds);
        float measureWidth = mPaint.measureText(mText, 0, mText.length());
        mPcWidth = (int) ensuredWidth(bounds, measureWidth);

        Bitmap textureBm = Bitmap.createBitmap(getTexDim(), getTexDim(), Bitmap.Config.ARGB_8888);
        textureBm.eraseColor(Color.argb(0, 255, 255, 255));

        int[] content = new int[textureBm.getWidth()];
        int maxPicWidthPerTexture = getTexDim() / mPcHeight * getTexDim();
        if (DBG)
            Log.d(TAG, "getTextBitmapAndDrawToTexture. maxPicWidthPerTexture= " + maxPicWidthPerTexture);

        if (!TextUtils.isEmpty(mText)) {
            mBeginXinTexture = mBeginYinTexture = 0;
            drawTextBitmapAndSetTexPixels(content, mText, GraphUtils.parseColor(mBackColor), mPaint,
                    maxPicWidthPerTexture, textureBm);

            if (DBG)
                Log.d(TAG, "after drawTextBitmapAndSetTexPixels, mBeginXinTexture= " + mBeginXinTexture
                        + ", mBeginYinTexture= " + mBeginYinTexture
                 + ", mPcWidth= " + mPcWidth + ", mPcHeight= " + mPcHeight);


            AppController.getInstance().addBitmapToMemoryCache(getKey(), new AppController.MyBitmap(textureBm, mPcWidth, getPcHeight()));
        }
        if (DBG_PNG) {
            new File("/mnt/sdcard/mul").mkdir();
            QuadGenerator.toPng(textureBm, new File("/mnt/sdcard/mul/" + "rss.png"));
        }
    }

    @Override
    protected String getKey() {

        if (DBG)
            Log.d(TAG, "getKey. mKey= " + mKey);
        if (!TextUtils.isEmpty(mKey))
            return mKey;

        int length = mText.length();

        final StringBuilder sb = new StringBuilder(length + mBackColor.length() + mTextColor.length());
        sb.append(mText).append(mBackColor).append(mTextColor);
        HashCode hashCode = Hashing.sha1().hashString(sb.toString(), Charset.forName("UTF-16"));

        mKey = hashCode.toString();

        return mKey;
    }

    @Override
    protected void setupPaint(Paint paint) {
        super.setupPaint(paint);

        String textColorStr = mHttpUrl.queryParameter("textcolor");
        if (!TextUtils.isEmpty(textColorStr))
            mTextColor = textColorStr;
        paint.setColor(GraphUtils.parseColor(mTextColor));

        paint.setFlags(0);
        setTypeface(paint, AppController.getInstance().getTypeface("default"), 0);
    }


    @Override
    public void render() {

        if (mNeedChangeTexture) {
            if (DBG)
                Log.d(TAG, "render. need change texture. mCurTextId= " + mCurTextId);

            mNeedChangeTexture = false;

            try {
                getTextBitmapAndDrawToTexture();
                if (mCurTextId == 0)
                    mCurTextId = 1;
                else mCurTextId = 0;

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

    public void initRSSContent() {
        try {

            if (DBG)
                Log.d(TAG, "initRSSContent. mCltHandler= " + mCltHandler);
            if (mCltHandler != null)
                prepareThread(0);

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
    }
}
