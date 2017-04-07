package com.color.home.widgets.clt_json;

import android.content.Context;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.ProgramParser;
import com.color.home.Texts;
import com.color.home.model.CltDataInfo;
import com.color.home.model.CltJsonContent;
import com.color.home.utils.ColorHttpUtils;
import com.color.home.widgets.multilines.MultiPicScrollObject;

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

public class MLScrollCltJsonObject extends MultiPicScrollObject {

    protected final static String TAG = "MLScrollCltJsonObject";
    protected static final boolean DBG = false;

    private boolean mNeedChangeTexture = false;

    private HandlerThread mCltHandlerThread;
    private Handler mCltHandler;
    private Runnable mCltRunnable;
    private ColorHttpUtils mColorHttpUtils;
    //    private boolean mIsFirstTimeGetCltJson;
    private List<CltDataInfo> mCltDataInfos;

    private ArrayList<CltJsonContent> mCltJsonList;

    public MLScrollCltJsonObject(Context context, ProgramParser.Item item) {
        super(context, item);
    }

    @Override
    protected void update() {

        synchronized (AppController.sLock) {
            if (DBG)
                Log.d(TAG, "update.");
            mTexCount = 2;

            initTextView();
            prepareCltJson();

            mCltHandler.removeCallbacks(mCltRunnable);
            mCltHandler.post(mCltRunnable);

            //do not generate quads and bind texture until get data from net
            return;
        }
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
                try {
                    if (DBG)
                        Log.d(TAG, "mCltRunnable. Thread= " + Thread.currentThread().getName());

                    List<CltDataInfo> newCltDataInfos = mColorHttpUtils.getCltDataInfos(mCltJsonList);
                    if (isUpdate(mCltDataInfos, newCltDataInfos)) {
                        Log.d(TAG, "data had updated.");

                        mCltDataInfos = getCltDateInfos(mCltDataInfos, newCltDataInfos);
                        setText(getCltText(getTextList(mCltDataInfos)));

                        mNeedChangeTexture = true;

                    } else
                        Log.d(TAG, "data had not updated.");

                    if (mCltHandler != null && getUpdateInterval(mItem) > 0) {
                        mCltHandler.removeCallbacks(this);
                        mCltHandler.postDelayed(this, getUpdateInterval(mItem));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public void render() {
        if (mNeedChangeTexture){
            if (DBG)
                Log.d(TAG, "render. need change texture. mCurTextId= " + mCurTextId + ", mIsTallPCPic= " + mIsTallPCPic
                        + ", mIsFatPCPic= " + mIsFatPCPic + ", Thread= " + Thread.currentThread());

            mNeedChangeTexture = false;

            try {
                if (initSizeAndDrawBitmapToTexture())
                    initAndPlayTexture();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        super.render();
    }

    protected void initAndPlayTexture() {

        if (mIsTallPCPic) {
            generateTallPCMulpicQuads();
        } else if (mIsFatPCPic) {
            // TODO: Handle Fat PC multipic.
            generateFatPCMulpicQuads();
        }
        if (mCurTextId == 0)
            mCurTextId = 1;
        else mCurTextId = 0;

        if (DBG)
            Log.d(TAG, "initAndPlayTexture. need play texture id= " + mCurTextId + ", Thread= " + Thread.currentThread());
        initUpdateTex(mCurTextId);

        if (DBG)
            android.util.Log.i("INFO", "bmpSize[" + mPcWidth + ", " + mPcHeight + "]");

        initShapes();
        setupMVP();

        if (mSr == null)
            mSr = new SegRender(mHeight);
        resetPos();

        GLES20.glUniformMatrix4fv(muMMatrixHandle, 1, false, mMMatrix, 0);

        // Prepare the triangle data
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 12, mQuadVB);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        // mT += 0.1f;
        // if (mT > 1.0f) {
        // mT = 0.0f;
        // }
        //
        // quadCB.put(1, mT);
        // quadCB.put(7, mT);

        // Prepare the triangle data
        GLES20.glVertexAttribPointer(maTexCoordsHandle, 3, GLES20.GL_FLOAT, false, 12, mQuadTCB);
        GLES20.glEnableVertexAttribArray(maTexCoordsHandle);

        // Draw the triangle
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void reloadCltJson(){
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

    private String getCltText(List<String> textList) {

        if (textList == null || textList.size() == 0)
            return " ";

        StringBuilder sb = new StringBuilder();
        for (String text : textList)
            sb.append(text + "\n");

        return sb.toString();
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
