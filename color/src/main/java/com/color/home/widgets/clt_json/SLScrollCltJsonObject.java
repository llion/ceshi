package com.color.home.widgets.clt_json;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.ProgramParser;
import com.color.home.Texts;
import com.color.home.model.CltDataInfo;
import com.color.home.model.CltJsonContent;
import com.color.home.utils.ColorHttpUtils;
import com.color.home.widgets.singleline.localscroll.TextObject;

import java.util.ArrayList;
import java.util.List;

import static com.color.home.widgets.clt_json.ItemMLPagedCltJsonView.getCltDateInfos;
import static com.color.home.widgets.clt_json.ItemMLPagedCltJsonView.getCltJsonList;
import static com.color.home.widgets.clt_json.ItemMLPagedCltJsonView.getTextList;
import static com.color.home.widgets.clt_json.ItemMLPagedCltJsonView.getUpdateInterval;
import static com.color.home.widgets.clt_json.ItemMLPagedCltJsonView.isUpdate;

/**
 * Created by Administrator on 2017/4/5.
 */

public class SLScrollCltJsonObject extends TextObject {

    protected static final boolean DBG = false;
    protected final static String TAG = "SLScrollCltJsonObject";

    private Handler mCltHandler;
    private HandlerThread mCltHandlerThread;
    private Runnable mCltRunnable;
    private ColorHttpUtils mColorHttpUtils;
    private List<CltDataInfo> mCltDataInfos;
    private ArrayList<CltJsonContent> mCltJsonList;
    protected boolean mNeedChangeTexture = false;

    public SLScrollCltJsonObject(Context context, ProgramParser.Item item) {
        super(context, item);
    }

    @Override
    public void update() {
        if (DBG)
            Log.d(TAG, "this is CLT_JSON.");
        mTexCount = 2;
        genTexs();
        prepareCltJson();

        mCltHandler.removeCallbacks(mCltRunnable);
        mCltHandler.post(mCltRunnable);
    }

    @Override
    public void render() {
        if (mNeedChangeTexture) {

            if (DBG)
                Log.d(TAG, "need change texture.");

            mNeedChangeTexture = false;
            changeTexture();
        }
        super.render();
    }

    private void prepareCltJson() {

        mCltHandlerThread = new HandlerThread("color-net-thread");
        mCltHandlerThread.start();
        mCltHandler = new Handler(mCltHandlerThread.getLooper());

        mColorHttpUtils = new ColorHttpUtils(mContext);
        mCltJsonList = getCltJsonList(Texts.getText(mItem));

        initCltRunnable();
    }

    private void initCltRunnable() {
        mCltRunnable = new Runnable() {
            @Override
            public void run() {
                if (DBG)
                    Log.d(TAG, "mCltRunnable. Thread= " + Thread.currentThread().getName());

                List<CltDataInfo> newCltDataInfos = mColorHttpUtils.getCltDataInfos(mCltJsonList);
                if (isUpdate(mCltDataInfos, newCltDataInfos)) {
                    Log.d(TAG, "data had updated.");

                    mCltDataInfos = getCltDateInfos(mCltDataInfos, newCltDataInfos);

                    setText(getCltText(getTextList(mCltDataInfos)));
                    setTextItemBitmapHash(mItem.getTextBitmapHash(mText));

                    mNeedChangeTexture = true;

                } else
                    Log.d(TAG, "data had not updated.");

                if (mCltHandler != null && getUpdateInterval(mItem) > 0) {
                    mCltHandler.removeCallbacks(this);
                    mCltHandler.postDelayed(this, getUpdateInterval(mItem));
                }
            }
        };
    }

    private String getCltText(List<String> textList) {
        if (textList == null || textList.size() == 0)
            return " ";

        StringBuilder sb = new StringBuilder();
        for (String text : textList)
            sb.append(text + "\n");

        return sb.toString();
    }

    protected void changeTexture() {

        AppController.MyBitmap texFromMemCache = texFromMemCache();
        if (DBG)
            Log.d(TAG, "texFromMemCache= " + texFromMemCache);

        if (texFromMemCache == null) {
            prepareTexture();

        } else {
            setSize(texFromMemCache);
        }

        if (mCurTextId == 0) {
            mCurTextId = 1;
        } else
            mCurTextId = 0;

        updatePageToTexId(1, mCurTextId);
        initShapes();
        setupMVP();

        Matrix.setIdentityM(mMMatrix, 0);
        GLES20.glUniform2f(muTexScaleHandle, (float) mPcWidth, (float) getEvenPcHeight());

        // Prepare the triangle data
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 12, mQuadVB);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        // Prepare the triangle data
        GLES20.glVertexAttribPointer(maTexCoordsHandle, 3, GLES20.GL_FLOAT, false, 12, mQuadTCB);
        GLES20.glEnableVertexAttribArray(maTexCoordsHandle);
    }

    public void reloadCltJson() {
        try {

            if (DBG)
                Log.d(TAG, "reloadCltJson. mCltHandler= " + mCltHandler + ", mCltRunnable= " + mCltRunnable);
            if (mCltHandler != null && mCltRunnable != null){
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

        if (mCltHandler != null){
            mCltHandler.removeCallbacks(mCltRunnable);
        }
        if (mCltHandlerThread != null){
            mCltHandlerThread.quit();
        }
    }
}
