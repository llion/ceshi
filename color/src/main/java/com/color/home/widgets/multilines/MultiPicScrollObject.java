/*
 * This proprietary software may be used only as
 * authorised by a licensing agreement from ARM Limited
 * (C) COPYRIGHT 2013 ARM Limited
 * ALL RIGHTS RESERVED
 * The entire notice above must be reproduced on all authorised
 * copies and copies may only be made to the extent permitted
 * by a licensing agreement from ARM Limited.
 */

package com.color.home.widgets.multilines;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;

import com.color.home.AppController;
import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.ScrollPicInfo;
import com.color.home.Texts;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.ItemsAdapter;
import com.color.home.widgets.singleline.QuadGenerator;
import com.color.home.widgets.singleline.QuadSegment;
import com.color.home.widgets.singleline.cltjsonutils.CltJsonUtils;
import com.color.home.widgets.singleline.localscroll.TextRenderer;
import com.google.common.hash.HashCode;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiPicScrollObject {
    // private static final int MAX_TEXTURE_WIDTH_HEIGHT = 4096;
    private final static String TAG = "MultiPicScrollObject";
    private static final boolean DBG = false;
    private static final boolean PNG_DBG = false;
    private static final boolean RENDER_DBG = false;
    private static final boolean MATRIX_DBG = false;
    private static final boolean LAST_COLUM_DBG = false;
    // private static final String MNT_SDCARD_PNGS = "/mnt/sdcard/pngs";
    private static final int MAX_TEXTURE_WIDTH_HEIGHT = 4096;
    /**
     * Must be >= 2. Because there could be two quads w/ different texs.
     */
    // private static final int MAX_ACTIVE_TEX = 64;
    private static final int MAX_ACTIVE_TEX = 4;
    private static final boolean READ_DBG = false;

    protected int mPcWidth;
    /**
     * Full bitmap. e.g., 38498.
     */
    protected int mPcHeight;
    protected float mPixelPerFrame = -1f;
    private int mColor;
    protected int mCurrentRepeats = 0;
    private Context mContext;
    private Item mItem;
    private ScrollPicInfo mScrollpicinfo;
    /**
     * We split the whole big bitmap into how many pieces.
     */
    private int mTexCount = 2;
    private boolean mIsTallPCPic = false;
    private boolean mIsFatPCPic = false;
    private int mRealSegmentsPerTex;
    private boolean isTallPCPicSurplus;// is pcheight surplus than heightConsumedPerTex


//    ETC1Util.ETC1Texture etc1Texture = null;
    Bitmap mBitmap;
    public static final Pattern PATTERN = Pattern.compile("([a-zA-Z]+):\\s*(\\d+)");


    protected String mText = "";
    protected boolean mIsCltJson = false;
    private int mCurTextId = 0;
    private boolean mNeedChangeTexture = false;

    private HandlerThread mCltHandlerThread;
    private Handler mCltHandler;
    private Runnable mCltRunnable;

    // Constructor initialize all necessary members
    public MultiPicScrollObject(Context context, Item item) {
        mContext = context;
        mItem = item;
        mScrollpicinfo = item.scrollpicinfo;
        mPcWidth = 1;
        mPcHeight = 1;

        // mPicCount = Integer.parseInt(mScrollpicinfo.picCount);
    }

    void update() {
        synchronized (AppController.sLock) {
            if (DBG)
                Log.d(TAG, "update.");
        /* [Update Text Size] */

            // 3. Generate texture with the new evaluated font size
            if (DBG)
                Log.d(TAG, "mScrollpicinfo= " + mScrollpicinfo + ", sourceType= " + mItem.sourceType);

            if (!("0".equals(mItem.sourceType) && Texts.isCltJsonText(Texts.getText(mItem)))
                    && mScrollpicinfo != null && !"0".equals(mScrollpicinfo.picCount)
                    && mScrollpicinfo.filePath != null
                    && "1".equals(mScrollpicinfo.filePath.isrelative)
                    && !TextUtils.isEmpty(mScrollpicinfo.filePath.filepath)) {
                if (!drawCanvasToTexture())
                    return;

            } else {
                if (!getTextBitmapAndDrawToTexture())
                    return;
            }

            if (mIsTallPCPic) {
                generateTallPCMulpicQuads();
            } else if (mIsFatPCPic) {
                // TODO: Handle Fat PC multipic.
                generateFatPCMulpicQuads();
            }

            initUpdateTex(0);

            if (DBG)
                android.util.Log.i("INFO", "bmpSize[" + mPcWidth + ", " + mPcHeight + "]");

            initShapes();

            setupMVP();

            if (mTexIds == null) {
                if (DBG)
                    Log.d(TAG, "update. [No content as mTexIds is null.");
                return;
            }

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

    public void resetPos() {
        if (DBG)
            Log.d(TAG, "resetPos.");
        Matrix.setIdentityM(mMMatrix, 0);

        // Matrix.translateM(mMMatrix, 0, 0.f, 4000.f, 0.f);
    }

    public void setupMVP() {
        final float halfWidth = mWidth / 2;
        final float halfHeight = mHeight / 2;
        if (DBG)
            Log.d(TAG, "setupMVP. [halfWidth=" + halfWidth + ", halfHeight=" + halfHeight
                    + ", mWidth=" + mWidth
                    + ", mHeight=" + mHeight);
        Matrix.orthoM(mMVPMatrix, 0, -halfWidth, halfWidth, -halfHeight, halfHeight, -1, 1);

        // http://www.learnopengles.com/tag/modelview-matrix/
        // Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        // Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        Matrix.setIdentityM(mVMatrix, 0);
        // Move the view (content/coordination origin) up.
        Matrix.translateM(mVMatrix, 0, -mWidth / 2, -mHeight / 2, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, mVMatrix, 0);

        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
    }

    // Load shaders, create vertices, texture coordinates etc.
    public void init() {
        // R G B A.
        // mBackgroundColor
        final float red = Color.red(mBackgroundColor) / 255.0f;
        final float green = Color.green(mBackgroundColor) / 255.0f;
        final float blue = Color.blue(mBackgroundColor) / 255.0f;
        final float alpha = Color.alpha(mBackgroundColor) / 255.0f;
        if (DBG)
            Log.d(TAG, "init. [red=" + red + ", green=" + green + ", blue=" + blue + ", alpha=" + alpha);

        GLES20.glClearColor(red, green, blue, alpha);

        // Initialize the triangle vertex array
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        MultiPicScrollRenderer.checkGLError("loadShaders");

        mProgram = GLES20.glCreateProgram(); // Create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader); // Add the vertex shader to program
        MultiPicScrollRenderer.checkGLError("glAttachShader:vert");
        GLES20.glAttachShader(mProgram, fragmentShader); // Add the fragment shader to program
        MultiPicScrollRenderer.checkGLError("glAttachShader:frag");
        GLES20.glLinkProgram(mProgram); // Creates OpenGL program executables
        MultiPicScrollRenderer.checkGLError("glLinkProgram");
        GLES20.glUseProgram(mProgram);
        MultiPicScrollRenderer.checkGLError("glUseProgram");

        // Get handle to the vertex shader's vPosition member
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        MultiPicScrollRenderer.checkGLError("glGetAttribLocation:vPosition");
        // Get handle to the vertex shader's vPosition member
        maTexCoordsHandle = GLES20.glGetAttribLocation(mProgram, "vTexCoord");
        MultiPicScrollRenderer.checkGLError("glGetAttribLocation:vTexCoord");
        // get handle to uniform parameter
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MultiPicScrollRenderer.checkGLError("glGetUniformLocation:uMVPMatrix");

        // get handle to uniform parameter
        muMMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMMatrix");
        MultiPicScrollRenderer.checkGLError("glGetUniformLocation:uMMatrix");

        muTextureHandle = GLES20.glGetUniformLocation(mProgram, "u_s2dTexture");
        MultiPicScrollRenderer.checkGLError("glGetUniformLocation:u_s2dTexture");

        // muTexScaleHandle = GLES20.glGetUniformLocation(mProgram, "uTexScale");
        // MultiPicScrollRenderer.checkGLError("glGetUniformLocation:muTexScaleHandle");

        // GLES20.glHint(GLES20.GL_GENERATE_MIPMAP_HINT, GLES20.GL_NICEST);
        // TextRenderer.checkGLError("glHint");

        // // !!! Must set texture parameter otherwise, empty (black).
        // GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[1]);
        // TextRenderer.checkGLError("glBindTexture");
        // // Setup texture parameters
        // GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        // TextRenderer.checkGLError("glTexParameterf:GL_TEXTURE_MAG_FILTER");
        // // GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        // GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        // TextRenderer.checkGLError("glTexParameterf:GL_TEXTURE_MIN_FILTER");
        //
        // GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        // TextRenderer.checkGLError("glTexParameterf:GL_TEXTURE_WRAP_S");
        // GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        // TextRenderer.checkGLError("glTexParameterf:GL_TEXTURE_WRAP_T");

    }

    private void genTexs() {
        if (DBG)
            Log.d(TAG, "genTexs. [");

        mTexIds = new int[Math.min(mTexCount, MAX_ACTIVE_TEX)];

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        TextRenderer.checkGLError("glActiveTexture");
        GLES20.glGenTextures(mTexIds.length, mTexIds, 0);
        TextRenderer.checkGLError("glGenTextures");
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(muTextureHandle, 0);
        TextRenderer.checkGLError("glUniform1i");

        // As byproduct, the last text is binded.
        for (int i = 0; i < mTexIds.length; i++) {
            initTexParam(i);
        }
    }

    private void initTexParam(int texId) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexIds[texId]);
        TextRenderer.checkGLError("glBindTexture");
        // Setup texture parameters
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        TextRenderer.checkGLError("glTexParameterf:GL_TEXTURE_MAG_FILTER");
        // GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        TextRenderer.checkGLError("glTexParameterf:GL_TEXTURE_MIN_FILTER");

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        TextRenderer.checkGLError("glTexParameterf:GL_TEXTURE_WRAP_S");
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        TextRenderer.checkGLError("glTexParameterf:GL_TEXTURE_WRAP_T");
    }

    public void setBackgroundColor(int parseColor) {
        mBackgroundColor = parseColor;
    }


    private float pixelTemp = 0.0f;
    private boolean mIsGreaterThanAPixelPerFrame = false;

    public void render() {

        if (mNeedChangeTexture){
            if (DBG)
                Log.d(TAG, "render. need change texture. mCurTextId= " + mCurTextId + ", mIsTallPCPic= " + mIsTallPCPic
                 + ", mIsFatPCPic= " + mIsFatPCPic + ", Thread= " + Thread.currentThread());

            mNeedChangeTexture = false;

            initAndPlayTexture();
        }

        float[] modelMat = mMMatrix;
        // // Add program to OpenGL environment
//        if(Math.abs(mPixelPerFrame) > 1.0f){
//            mPixelPerFrame = Math.round(mPixelPerFrame);
//        }
//        pixelTemp += mPixelPerFrame;
        //mPixelPerFrame < 0
//        if(pixelTemp <= -1.0f) {
//            Matrix.translateM(modelMat, 0, 0.f, -(int)pixelTemp, 0.f);
//            pixelTemp += Math.abs((int)pixelTemp);
//        }
        if (mIsGreaterThanAPixelPerFrame)
            Matrix.translateM(modelMat, 0, 0.f, mPixelPerFrame, 0.f);
        else {
            pixelTemp += mPixelPerFrame;
            if (pixelTemp >= 1.0f) {
                Matrix.translateM(modelMat, 0, 0.f, 1.0f, 0.f);
                pixelTemp -= 1;
            }
        }


        //        Matrix.translateM(modelMat, 0, 0.f, -mPixelPerFrame, 0.f);

        if (MATRIX_DBG)
            Log.d(TAG, "matrix[13] = " + mMMatrix[13]);
        // resetPos();
        // Matrix.translateM(modelMat, 0, 0.f, -200.f, 0.f);

        // if (mMMatrix[13] > mHeight + mBitmapHeight) {
        //
        // }

        // 13 = y; 12 =x
        // GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[(int)mMMatrix[13] % 2]);

        // if (DBG) {
        // int i = 0;
        // for (float fl : mMMatrix) {
        // Log.d(TAG, "render. [fl=" + fl + ", i=" + i);
        // i++;
        // }
        // }

        // 09-08 23:04:05.580: D/TextObject(6052): render. [fl=639.0, i=12

//        if (modelMat[13] > mHeight + mPcHeight){
//            if (DBG)
//                Log.d(TAG, "render. [modelMat[13]=" + modelMat[13]);
//            resetPos();
//            mSr.resetPage();
//            // if repeat count == 0, infinite loop.
//            if (mRepeatCount != 0) {
//                if (++mCurrentRepeats >= mRepeatCount) {
//                    notifyFinish();
//                }
//            }
//        }

        if (MATRIX_DBG)
            Log.d(TAG, "mIsTallPCPic = " +mIsTallPCPic + ", isTallPCPicSurplus= " + isTallPCPicSurplus
             + ", mHeight + mTextureHeight * mRealSegmentsPerTex= " + (mHeight + mTextureHeight * mRealSegmentsPerTex)
            + ", mHeight + mPcHeight= " + (mHeight + mPcHeight) + ", modelMat[13]= " + modelMat[13]);

        if (mIsTallPCPic && isTallPCPicSurplus) {// fat multipic and tallPCPic is surplus
            if (modelMat[13] > mHeight + mTextureHeight * mRealSegmentsPerTex)
                reset();

        } else { // fat multipic || (tall multipic && !isTallPCPicSurplus)
            if (modelMat[13] > mHeight + mPcHeight) {
                if (DBG)
                    Log.d(TAG, "render. .modelMat[13]= " + modelMat[13] + ", mHeight + mPcHeight= " + (mHeight + mPcHeight));
//                initAndPlayTexture();
                reset();
            }
        }

        GLES20.glUniformMatrix4fv(muMMatrixHandle, 1, false, modelMat, 0);
        // GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        // if (DBG)
        // Log.d(TAG, "render. [mIndices.length=" + mIndices.length);

        if (mSr != null)
            mSr.render(modelMat[13]);

        // if (mPicCount )

    }

    private void initAndPlayTexture() {

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

    private void reset() {

        resetPos();
        mSr.resetPage();
        // if repeat count == 0, infinite loop.
        if (mRepeatCount != 0) {
            if (++mCurrentRepeats >= mRepeatCount) {
                notifyFinish();
            }
        }
    }

    private class SegRender {
        int mCurQuadIndex;
        private int mWindowHeight;

        public SegRender(int windowHeight) {
            mWindowHeight = windowHeight;
        }

        public void resetPage() {
            mCurQuadIndex = 0;

            if (DBG)
                Log.d(TAG, "resetPage.");

            // // if more than 2 pages, reset at least one tex.
            // if (mTexCount > 2) {
            // MultiPicScrollObject.this.updateTexIndexToTexId(0, 0);
            //
            // if (mTexCount > 3) {
            // MultiPicScrollObject.this.updateTexIndexToTexId(1, 1);
            // }
            // }

            // mTexCount = 3, MAX_ACTIVE_TEX = 2

            int resetTexCount = Math.max(0, Math.min(mTexCount - MAX_ACTIVE_TEX, MAX_ACTIVE_TEX));
            if (DBG)
                Log.d(TAG, "resetPage.resetTexCount= " + resetTexCount);
            for (int i = 0; i < resetTexCount; i++) {
                MultiPicScrollObject.this.updateTexIndexToTexId(i, i);
            }
        }

        public void render(float offset) {
            // if (offset >= MAX_TEXTURE_WIDTH_HEIGHT * mCurQuadIndex
            // && offset <= mWindowHeight + MAX_TEXTURE_WIDTH_HEIGHT * mCurQuadIndex) {
            // // } else if (offset > mWindowHeight + mPageHeight * mPage) {
            // drawQuad(mCurQuadIndex - 1);
            // drawQuad(mCurQuadIndex);
            // } else {
            // TODO: check the correctness of the following mTextureHeight variable. (mTextureWidth?)
            if (RENDER_DBG)
                Log.d(TAG, "render. [mCurQuadIndex=" + mCurQuadIndex + ", offset= " + offset);

            if (mIsTallPCPic) {
                if (RENDER_DBG)
                    Log.d(TAG, "draw tall quad.");

                if (offset >= mTextureHeight * (mCurQuadIndex + 1) && mCurQuadIndex < (mQuadSegs.length - 1)) {
                    if (RENDER_DBG)
                        Log.d(TAG, "next quad");

                    mCurQuadIndex++; // NOTE: mCurQuadIndex could be mQuadSegs.length

//                    if (isAnotherTex(mCurQuadIndex) && mQuadSegs.length != mCurQuadIndex) {
//                        if (RENDER_DBG)
//                            Log.d(TAG, "render. [isAnotherTex.");
//
//                        int texIndex = mCurQuadIndex / mMaxSegmentsPerTexContain;
//                        if (texIndex >= MAX_ACTIVE_TEX) { // Prepare the next tex. NOTE: The 0 .. MAX_ACTIVE_TEX-1 were pre-set.
//                            MultiPicScrollObject.this.updateTexIndexToTexId(texIndex, texIndex % MAX_ACTIVE_TEX); // 0 for page = 2
//                        }
//                    }
                }

                if (mCurQuadIndex - 1 >= 0 && offset >= mTextureHeight * mCurQuadIndex
                        && offset <= mWindowHeight + mTextureHeight * mCurQuadIndex) { // Always draw the prev quad.
                    if (RENDER_DBG)
                        Log.d(TAG, "draw the prev quad");

                    if (offset <= mWindowHeight){
                        for (int i = (int)offset / mTextureHeight; i >=1 ; i --)
                            drawQuad(mCurQuadIndex - i);

                    } else {
                        for (int i = mCurQuadIndex - ((int)offset - mWindowHeight) / mTextureHeight; i >=1 ; i --)
                            drawQuad(mCurQuadIndex - i);
                    }
                }

                if (RENDER_DBG)
                    Log.d(TAG, "mQuadSegs.length= " + mQuadSegs.length + ", mCurQuadIndex= " + mCurQuadIndex);
                drawQuad(mCurQuadIndex);

            } else if (mIsFatPCPic) { // fat multipic
                if (RENDER_DBG)
                    Log.d(TAG, "draw fat quad.");

                for (int i = 0; i < mQuadSegs.length; i++) {
                    if (RENDER_DBG)
                        Log.d(TAG, "draw fat quad. i= " + i);
                    drawQuad(i);
                }
            }

        }

        /**
         * Even and odd page draw w/ even/odd texture from the textids.
         *
         * @param quadIndex
         */
        private void drawQuad(int quadIndex) {
            int texIndex = quadIndex / mMaxSegmentsPerTexContain;
//            if (RENDER_DBG)
//                Log.d(TAG, "drawQuad.quadIndex= " + quadIndex + ", texIndex= " + texIndex);

            if (RENDER_DBG)
                Log.d(TAG, "drawQuad.quadIndex= " + quadIndex + ", mCurTextId= " + mCurTextId);

//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexIds[texIndex % MAX_ACTIVE_TEX]);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexIds[mCurTextId]);
            if (DBG)
                TextRenderer.checkGLError("glBindTexture");
            // 4 vertex indices per quad, 2 (unsigned short) bytes per index.

            // offset(indices here)'s unit is char* !!!!!!!!!!! not in the GL_UNSIGNED_SHORT.
            // so must use the (vertext count *2) for the offset.!!!!
            // void glDrawElements(GLenum mode, GLsizei count, GLenum type, const GLvoid *indices)
            GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, 4, GLES20.GL_UNSIGNED_SHORT, quadIndex * 4 * 2);
        }

        public boolean isAnotherTex(int quadIndex) {
            return quadIndex % mMaxSegmentsPerTexContain == 0;
        }

    }

    private SegRender mSr;

    protected void notifyFinish() {
        if (DBG)
            Log.d(TAG, "notifyFinish. [mSingleLineTextSurfaceView=" + mMultilinesScrollMultipic2View);
        if (mMultilinesScrollMultipic2View != null)
            mMultilinesScrollMultipic2View.notifyPlayFinished();
    }

    private int mTextureWidth = MAX_TEXTURE_WIDTH_HEIGHT;
    private int mTextureHeight = MAX_TEXTURE_WIDTH_HEIGHT;

    /* [Draw Canvas To Texture] */
    private boolean drawCanvasToTexture() {

        StreamResolver streamResolver = null;
        try {
            final byte[] head = new byte[8];
            final String absFilePath = ItemsAdapter.getAbsFilePathByFileSource(mScrollpicinfo.filePath);
            if (DBG)
                Log.d(TAG, "setPageText. [absFilePath=" + absFilePath);

            streamResolver = new StreamResolver(absFilePath).resolve();
            InputStream is = streamResolver.getReadFromIs();
            if (is == null) {
                Log.e(TAG, "Bad file.absFilePath=" + absFilePath);
                return false;
            }

            ByteStreams.skipFully(is, 20);
            ByteStreams.readFully(is, head, 0, 8);

            ByteBuffer bb = ByteBuffer.wrap(head);
            bb.order(ByteOrder.LITTLE_ENDIAN); // if you want little-endian

            if (DBG)
                Log.i(TAG, "drawCanvasToTexture. [position=" + bb.position());

            mPcWidth = bb.getInt();
            mPcHeight = bb.getInt();

            if (DBG)
                Log.i(TAG, "mPcWidth= " + mPcWidth + ", mPcHeight= " + mPcHeight);

            int shortestEdgeLength = QuadGenerator.findClosestPOT(mPcWidth, mPcHeight);
            if (DBG)
                Log.i(TAG, "shortestEdgeLength= " + shortestEdgeLength);

//               int maxBlocksInTheShortestDim = shortestEdgeLength / shortestDimFromPc * shortestDimFromPc;
//            int aTextCanContainsArea = maxBlocksInTheShortestDim * shortestEdgeLength;

//            if (aTextCanContainsArea < mPcWidth * mPcHeight) {
//                if (DBG)
//                    Log.d(TAG, "aTextCanContainsArea < mPcWidth * mPcHeight");
//                shortestEdgeLength = shortestEdgeLength * 2;
//            }

            if (shortestEdgeLength > 4096) {
                shortestEdgeLength = 4096;
                Log.e(TAG, "Bad multi pics area makes the texture length greater than 4096, fall back to 4096. mPcWidt=" + mPcWidth
                        + ", mPcHeight=" + mPcHeight);
            }

            mTextureWidth = mTextureHeight = shortestEdgeLength;
            if (DBG)
                Log.d(TAG, " mTextureWidth= " + mTextureWidth);

            //judge the MemFree is enough or not
            if (!isMemoryEnough(mTextureWidth))
                return false;

            int shortestDimFromPc = Math.min(mPcWidth, mPcHeight);
            if (DBG)
                Log.d(TAG, " shortestDimFromPc= " + shortestDimFromPc + ", mPcWidth= " + mPcWidth + ", mPcHeight= " + mPcHeight);

            if (shortestDimFromPc == mPcWidth) {
                mIsTallPCPic = true;
            } else {
                mIsFatPCPic = true;
            }
            if (DBG)
                Log.d(TAG, " mIsTallPCPic= " + mIsTallPCPic + ", mIsFatPCPic= " + mIsFatPCPic);

            if (mIsTallPCPic) {
                handleTallPicMulpic(is);
            } else if (mIsFatPCPic) {
                //TODO: Handle Fat pc multipic.
                handleFatPicMulpic(is);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // Do nothing here.
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (streamResolver != null) {
                streamResolver.close();
            }
        }

        return true;
    }

    //tall pic quadSize
    private void generateTallPCMulpicQuads() {
        // setImageBitmap(resultBm);
//        final int quadSize = ceilingBlocks(mPcHeight, mTextureHeight);
        final int quadSize = mRealSegmentsPerTex;

        if (DBG)
            Log.d(TAG, "generateTallPCMulpicQuads. [quadSize=" + quadSize);
        mQuadSegs = new QuadSegment[quadSize];
        for (int i = 0; i < quadSize; i++) {
            // The last quad.
            // if (isLastTex(i)) {
            // // GOOD. mQuadSegs[i] = new QuadSegment(1024, 2048);
            // mQuadSegs[i] = new QuadSegment(mPcWidth, mLastTexReadHeight, -mWidth / 2);
            // } else {
            // mQuadSegs[i] = new QuadSegment(mPcWidth, mTextureHeight, -mWidth / 2);
            // }

            int top = -i * mTextureHeight;
            int quadHeight = (i == quadSize - 1) ? lastColumnHeight() : mTextureHeight;
            mQuadSegs[i] = new QuadSegment(top, 0, mPcWidth, quadHeight, 0, (i % mRealSegmentsPerTex) * mPcWidth);
        }
    }

    //fat pic quadSize
    private void generateFatPCMulpicQuads() {
        // setImageBitmap(resultBm);
        final int quadSize = ceilingBlocks(mPcWidth, mTextureWidth);
        if (DBG)
            Log.d(TAG, "generateFatPCMulpicQuads. [quadSize=" + quadSize + ", [mMaxSegmentsPerTexContain= " + mMaxSegmentsPerTexContain);
        mQuadSegs = new QuadSegment[quadSize];
        for (int i = 0; i < quadSize; i++) {

            int left = i * mTextureWidth;
            int quadWidth = (i == quadSize - 1) ? lastRowWidth() : mTextureWidth;
            if (DBG)
                Log.d(TAG, "left= " + left + ", quadWidth= " + quadWidth + ", (i % mMaxSegmentsPerTexContain) * mPcHeight= " + (i % mMaxSegmentsPerTexContain) * mPcHeight);
            mQuadSegs[i] = new QuadSegment(0, left, quadWidth, mPcHeight, (i % mMaxSegmentsPerTexContain) * mPcHeight, 0);
        }
    }

    private void handleTallPicMulpic(InputStream is) throws IOException {
        mMaxSegmentsPerTexContain = mTextureWidth / mPcWidth;

        int heightConsumedPerTex = mMaxSegmentsPerTexContain * mTextureHeight;
        // final int heightRemaining = mPcHeight % heightPerTex;
//        mTexCount = ceilingBlocks(mPcHeight, heightConsumedPerTex);
//        mTexCount = 1;
        setRealSegmentsPerTex(heightConsumedPerTex);

        // Now the pic count is ready.
        genTexs();

        if (DBG)
            Log.i(TAG, "onCreate. [mBitmapWidth=" + mPcWidth + ", mBitmapHeight=" + mPcHeight + ", mHeight=" + mHeight
                    + ", mPicCount="
                    + mTexCount);

        // we skipped 20 read 8 = 28.
        ByteStreams.skipFully(is, 1024 - 28);

        // byte[] converted = new byte[mWidth * mHeight * 4];
        for (int i = 0; i < 1; i++) {
//            String keyImgId = mScrollpicinfo.filePath.MD5 + i;
//            if (DBG)
//                Log.d(TAG, "handleTallPicMulpic. keyImgId= " + keyImgId);
//            // Image already exist, offset to next image.
//            if (AppController.getInstance().getBitmapFromMemCache(keyImgId) != null) {
//                if (DBG)
//                    Log.d(TAG, "drawCanvasToTexture. [getBitmapFromMemCache exist for =" + keyImgId + ", mMaxSegmentsPerTexContain=" + mMaxSegmentsPerTexContain);
//                // MAX_TEXTURE_WIDTH_HEIGHT is inaccurate, but OK for the lastest item.
//                // Do not skip the last texture in a full texture size, as it could overflow the file.
//                // ByteStreams.skipFully(is, mPcWidth * MAX_TEXTURE_WIDTH_HEIGHT * mMaxSegmentsPerTexContain * 4);
//                if (i == mTexCount - 1) {
//                    if (DBG)
//                        Log.d(TAG, "drawCanvasToTexture. [do not skip full size in the file the last texture, as it overflows.");
//                    continue;
//                }
////                ByteStreams.skipFully(is, mPcWidth * mTextureHeight * mMaxSegmentsPerTexContain * 4);
////                continue;
//            }

            // Always square.
//            byte[] content = new byte[mPcWidth * 4];

            mBitmap = Bitmap.createBitmap(mTextureWidth, mTextureHeight, Bitmap.Config.ARGB_8888);

            if (DBG)
                Log.d(TAG, "mPcHeight= " + mPcHeight + ", heightConsumedPerTex= " + heightConsumedPerTex + ", mRealSegmentsPerTex= " + mRealSegmentsPerTex);
            readTallTextPic(is, mBitmap, mPcWidth, mPcHeight, mTextureWidth);
//            int lastTexReadHeight = ((mPcHeight - 1) % heightConsumedPerTex) + 1;
//            int columns = isLastTex(i) ? ceilingBlocks(lastTexReadHeight, mTextureHeight) : mMaxSegmentsPerTexContain;
//            for (int j = 0; j < mRealSegmentsPerTex; j++) {
//                // last column in last texture => (lastTex && j == columns - 1)
////                int readSrcHeight = ((isLastTex(i) && j == columns - 1) ? lastColumnHeight() : mTextureHeight);
//                int readSrcHeight;
//                if (mPcHeight >= heightConsumedPerTex) {
//                    readSrcHeight = mTextureHeight;
//
//                } else {
//                    if (j == mRealSegmentsPerTex - 1)
//                        readSrcHeight = lastColumnHeight();
//                    else
//                        readSrcHeight = mTextureHeight;
//                }
//                // int targetStrip = MAX_TEXTURE_WIDTH_HEIGHT;
//                // int targetOffset = j * mPcWidth;
//                // int readSrcWidth = Math.min(mPcWidth, MAX_TEXTURE_WIDTH_HEIGHT);
//                // // last column in last texture => (lastTex && j == columns - 1)
//                // int readSrcHeight = ((lastTex && j == columns - 1) ? ((mLastTexReadHeight - 1) % MAX_TEXTURE_WIDTH_HEIGHT + 1) :
//                // MAX_TEXTURE_WIDTH_HEIGHT);
//
//                // mTextWidth: 目标Texture宽度。
//                // j * mPcWidth :
//                readTallBlock(is, content, mBitmap, mTextureWidth, j * mPcWidth, Math.min(mPcWidth, mTextureWidth), readSrcHeight, mPcWidth);
//            }

//            GraphUtils.convertRGBFromPC(content);

            // Now put these nice RGBA pixels into a Bitmap object


            // bm.setPremultiplied(false);
//            bm.copyPixelsFromBuffer(ByteBuffer.wrap(content, 0, mTextureWidth * mTextureHeight * 4));
//            AppController.getInstance().addBitmapToMemoryCache(keyImgId, new MyBitmap(bm, mPcWidth, mPcHeight));

            //TODO:test texture compress (ETC)
//            Buffer buffer = ByteBuffer.wrap(content, 0, mTextureWidth * mTextureHeight * 4).order(ByteOrder.nativeOrder());
//            buffer.position(0);
//            try {
//                etc1Texture = ETC1Util.compressTexture(bbEtc, mTextureWidth, mTextureHeight, 3, 3 * mTextureWidth);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }


            if (PNG_DBG) {
                String keyImgId = mScrollpicinfo.filePath.MD5 + i;
                new File("/mnt/sdcard/mul").mkdir();
                QuadGenerator.toPng(mBitmap, new File("/mnt/sdcard/mul/" + keyImgId + ".png"));
            }

        }
    }


    private void handleFatPicMulpic(InputStream is) throws IOException {
        mMaxSegmentsPerTexContain = mTextureHeight / mPcHeight;

        int widthConsumedPerTex = mMaxSegmentsPerTexContain * mTextureWidth;

//        mTexCount = ceilingBlocks(mPcWidth, widthConsumedPerTex);
//        mTexCount = 1;
        // Now the pic count is ready.
        genTexs();

        if (DBG)
            Log.i(TAG, "onCreate. [mBitmapWidth=" + mPcWidth + ", mBitmapHeight=" + mPcHeight + ", mHeight=" + mHeight
                    + ", mPicCount="
                    + mTexCount);

        // we skipped 20 read 8 = 28.
        ByteStreams.skipFully(is, 1024 - 28);

        // byte[] converted = new byte[mWidth * mHeight * 4];
        for (int i = 0; i < 1; i++) {
//            String keyImgId = "" + mScrollpicinfo.filePath.filepath + mScrollpicinfo.filePath.MD5 + i;//
//            String keyImgId = mScrollpicinfo.filePath.MD5 + i;
//            if (DBG)
//                Log.d(TAG, "handleFatPicMulpic. keyImgId= " + keyImgId);
//            // Image already exist, offset to next image.
//            if (AppController.getInstance().getBitmapFromMemCache(keyImgId) != null) {
//                if (DBG)
//                    Log.d(TAG, "drawCanvasToTexture. [getBitmapFromMemCache exist for =" + keyImgId + ", mMaxSegmentsPerTexContain=" + mMaxSegmentsPerTexContain);
//                // MAX_TEXTURE_WIDTH_HEIGHT is inaccurate, but OK for the lastest item.
//                // Do not skip the last texture in a full texture size, as it could overflow the file.
//                // ByteStreams.skipFully(is, mPcWidth * MAX_TEXTURE_WIDTH_HEIGHT * mMaxSegmentsPerTexContain * 4);
//                if (i == mTexCount - 1) {
//                    if (DBG)
//                        Log.d(TAG, "drawCanvasToTexture. [do not skip full size in the file the last texture, as it overflows.");
//                    continue;
//                }
////                ByteStreams.skipFully(is, mPcHeight * mTextureWidth * mMaxSegmentsPerTexContain * 4);
////                continue;
//            }

            mBitmap = Bitmap.createBitmap(mTextureWidth, mTextureHeight, Bitmap.Config.ARGB_8888);
            readFatTextPic(is, mBitmap, mPcWidth, mPcHeight, mTextureWidth);

//            byte[] content = new byte[Math.min(mPcWidth, mTextureWidth) * 4];
//            int readRows = ceilingBlocks(mPcWidth, mTextureWidth);
//
//            int lastReadWidth = lastRowWidth();
//            if (DBG)
//                Log.d(TAG, "fat. readRows= " + readRows + ", lastReadWidth= " + lastReadWidth + ", mpcHeight= " + mPcHeight);


//            for (int j = 0; j < mPcHeight; j++) {
//                if (RENDER_DBG)
//                    Log.d(TAG, "j= " + j);
//                //InputStream is, byte[] content, int targetStrip, int targetOffset, int readSrcWidth, int readRows, int lastReadWidth
//                readFatBlock(is, content, mBitmap, mPcHeight * mTextureWidth, j, mTextureWidth, readRows, lastReadWidth);
//            }

//            GraphUtils.convertRGBFromPC(content);

            // Now put these nice RGBA pixels into a Bitmap object

//            Bitmap bm = Bitmap.createBitmap(mTextureWidth, mTextureHeight, Bitmap.Config.ARGB_8888);
//            // bm.setPremultiplied(false);
//            bm.copyPixelsFromBuffer(ByteBuffer.wrap(content, 0, mTextureWidth * mTextureHeight * 4));
//            AppController.getInstance().addBitmapToMemoryCache(keyImgId, new MyBitmap(bm, mPcWidth, mPcHeight));

            if (PNG_DBG) {
                String keyImgId = mScrollpicinfo.filePath.MD5 + i;
                new File("/mnt/sdcard/mul").mkdir();
                QuadGenerator.toPng(mBitmap, new File("/mnt/sdcard/mul/" + keyImgId + ".png"));
            }

        }
    }


    private boolean getTextBitmapAndDrawToTexture() {
        try {

            String text = Texts.getText(mItem);
            if (TextUtils.isEmpty(text)){
                if (DBG)
                    Log.d(TAG, "text is empty.");
                return false;
            }

            TextView textView = new TextView(mContext);
            if (DBG)
                Log.d(TAG, "getTextBitmapAndDrawToTexture. mWidth= " + mWidth + ", mHeight= " + mHeight);
            initTextView(textView);

            if (Texts.isCltJsonText(Texts.getText(mItem))){

                mIsCltJson = true;
                final CltJsonUtils cltJsonUtils;

                cltJsonUtils = new CltJsonUtils();
                if (cltJsonUtils.initMapList(text)) {

                    String resultText = cltJsonUtils.getCltText();
                    text = resultText;
                    setText(resultText);

                    if (DBG)
                        Log.d(TAG,"clt_json resultText= " + resultText);

                    if (DBG)
                        Log.d(TAG, "update. isNeedUpdate= " + mItem.isNeedUpdate + ", updateInterval= " + mItem.updateInterval);
                    if ("1".equals(mItem.isNeedUpdate)) {
                        long updateInterval = 0;
                        try {
                            updateInterval = Long.parseLong(mItem.updateInterval);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }

                        if (updateInterval > 0) {
                            prepareThread(cltJsonUtils, updateInterval, textView);
                        }
                    }
                }

            }

            if (!initSizeAndDrawBitmapToTexture(text, textView))
                return false;

        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;

    }

    private void setText(String text) {
        mText = text;
    }

    private void prepareThread(final CltJsonUtils cltJsonUtils, final long updateInterval, final TextView textView) throws Exception {
        mCltHandlerThread = new HandlerThread("another-thread");
        mCltHandlerThread.start();
        mCltHandler = new Handler(mCltHandlerThread.getLooper());

        mCltRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (DBG)
                        Log.d(TAG, "mCltRunnable. Thread= " + Thread.currentThread().getName());

                    String resultText = cltJsonUtils.getCltText();

                    if (!mText.equals(resultText)) {
                        if (DBG)
                            Log.d(TAG, "the data had updated.");

                        setText(resultText);
//
                        if (!initSizeAndDrawBitmapToTexture(resultText, textView)) {
                            if (mCltHandler != null && mCltHandlerThread != null) {
                                mCltHandler.removeCallbacks(this);
                                mCltHandler.postDelayed(this, updateInterval);
                            }
                            return;
                        }

                    if (DBG)
                        Log.d(TAG, "need to change texture content.");
                    if (mBitmap != null) {
                        mNeedChangeTexture = true;
                    }
////
                    } else {
                        if (DBG)
                            Log.d(TAG, "the data had not updated, needn't change texture.");
                    }

                    if (mCltHandler != null && mCltHandlerThread != null) {
                        mCltHandler.removeCallbacks(this);
                        mCltHandler.postDelayed(this, updateInterval);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        if (mCltHandler != null && mCltHandlerThread != null) {
            mCltHandler.removeCallbacks(mCltRunnable);
            mCltHandler.postDelayed(mCltRunnable, updateInterval);
        }
    }

    private boolean initSizeAndDrawBitmapToTexture(String text, TextView textView) throws Exception {
        if (DBG)
            Log.d(TAG, "initSizeAndDrawBitmapToTexture. textView= " + textView + ", text= " + text);

        textView.setText(text);
        textView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        if (DBG)
            Log.d(TAG, "getScaledMaximumDrawingCacheSize= " + ViewConfiguration.get(mContext).getScaledMaximumDrawingCacheSize());

        if (DBG)
            Log.d(TAG, "textView.getDrawingCache()= " + textView.getDrawingCache()
                     + ", textView.getMeasuredWidth()= " + textView.getMeasuredWidth()
                    + ", textView.getMeasuredHeight()= " + textView.getMeasuredHeight()
             + ", textView.getLayout= " + textView.getLayout()
                     + ", getWidth= " + textView.getLayout().getWidth()
                     + ", getHeight= " + textView.getLayout().getHeight()
                     + ", getAlignment= " + textView.getLayout().getAlignment()
                    + ", textView.getLineCount= " + textView.getLineCount()
             + ", textView.getLineHeight= " + textView.getLineHeight()
             + ", getLineWidth(0)= " + textView.getLayout().getLineWidth(0));

        int maxLineWidth = 0;
        for (int i = 0; i < textView.getLayout().getLineCount(); i++){

            if (textView.getLayout().getLineWidth(i) > maxLineWidth)
                maxLineWidth = (int) Math.ceil(textView.getLayout().getLineWidth(i));
        }

        if (DBG)
            Log.d(TAG, "maxLineWidth= " + maxLineWidth + ", mWidth= " + mWidth);

        int originPicWidth = Math.min(maxLineWidth, mWidth);
        mTextureWidth = mTextureHeight = QuadGenerator.findClosestPOT(originPicWidth, textView.getLayout().getHeight());
        if (DBG)
            Log.d(TAG, "mTextureWidth= " + mTextureWidth + ", originPicWidth= " + originPicWidth
             + ", textView.getLayout().getHeight()= " + textView.getLayout().getHeight());

        if (!isMemoryEnough(this.mTextureWidth))
            return false;

        if (mTextureWidth >= 4096){
            if (originPicWidth < textView.getLayout().getHeight()){//tall
                mPcWidth = originPicWidth;
                mPcHeight = Math.min(mTextureWidth / originPicWidth * mTextureWidth, textView.getLayout().getHeight());

            } else{//fat
                mPcHeight = textView.getLayout().getHeight();
                mPcWidth = Math.min(mTextureWidth / mPcHeight * mTextureWidth, originPicWidth);
            }
        } else{
            mPcWidth = originPicWidth;
            mPcHeight = textView.getLayout().getHeight();
        }
        int segments;
        if (mPcWidth > mPcHeight) {
            mIsFatPCPic = true;
            segments = mPcWidth / this.mTextureWidth;
            if (mPcWidth % this.mTextureWidth > 0)
                segments++;
            mRealSegmentsPerTex = mMaxSegmentsPerTexContain = segments;

        } else {
            mIsTallPCPic = true;
            segments = mPcHeight / mTextureHeight;
            if (mPcHeight % mTextureHeight > 0)
                segments++;
            mRealSegmentsPerTex = mMaxSegmentsPerTexContain = segments;
        }

        if (DBG)
            Log.d(TAG, "mPcWidth= " + mPcWidth + ", mPcHeight= " + mPcHeight + ", mTextureWidth= " + mTextureWidth);

//        mTexCount = 1;
        genTexs();
        mBitmap = Bitmap.createBitmap(this.mTextureWidth, mTextureHeight, Bitmap.Config.ARGB_8888);

        if (mPcWidth * mPcHeight * 4 > ViewConfiguration.get(mContext).getScaledMaximumDrawingCacheSize()){
            if (DBG)
                Log.d(TAG, "View too large to fit into drawing cache, " +
                        "needs " + mPcWidth * mPcHeight * 4 + " bytes" +
                        ", only " + ViewConfiguration.get(mContext).getScaledMaximumDrawingCacheSize() + " available" +
                        ", scroll textview and draw cache bitmap to texture.");

            if (mIsFatPCPic) {
                getFatPicAndDrawToTexture(textView);
            } else {
                getTallPicAndDrawToTexture(textView);
            }

            textView.destroyDrawingCache();
            textView.setDrawingCacheEnabled(false);

        } else {

            textView.destroyDrawingCache();
            textView.setDrawingCacheEnabled(true);
            textView.layout(0, 0, mPcWidth, mPcHeight);

            if (DBG)
                Log.d(TAG, "needs cache size= " + mPcWidth * mPcHeight * 4);

            if (!drawBitmapToTexture(textView.getDrawingCache(), segments)) {
                textView.destroyDrawingCache();
                textView.setDrawingCacheEnabled(false);
                return false;
            }

            textView.destroyDrawingCache();
            textView.setDrawingCacheEnabled(false);

        }

        if (PNG_DBG) {
            Log.d(TAG, "save mBitmap.");
            String keyImgId = "mBitmap" + mCurTextId;
            new File("/mnt/sdcard/mul").mkdir();
            QuadGenerator.toPng(mBitmap, new File("/mnt/sdcard/mul/" + keyImgId + ".png"));
        }
        return true;
    }

    private void getTallPicAndDrawToTexture(TextView textView) {

        int maxCacheHeight = ViewConfiguration.get(mContext).getScaledMaximumDrawingCacheSize() / 4 / mPcWidth;
        if (DBG)
            Log.d(TAG, "getTallPicAndDrawToTexture. maxCacheHeight= " + maxCacheHeight);
        textView.setDrawingCacheEnabled(true);
        textView.layout(0, 0, mPcWidth, maxCacheHeight);

        Bitmap cacheBitmap;
        int[] content = new int[mPcWidth];
        int beginXinTexture = 0, beginYinTexture = 0, segments, remainAvailableTextureHeightPerCell = mTextureHeight, remainBitmapHeight;
        int count = mPcHeight / maxCacheHeight;
        if (mPcHeight % maxCacheHeight > 0)
            count++;

        if (DBG)
            Log.d(TAG, "count= " + count);
        for (int i = 0; i < count; i++){
            if (i > 0) {
                textView.destroyDrawingCache();
                textView.scrollBy(0, maxCacheHeight);//
//                textView.scrollTo(0, i * maxCacheHeight);
            }

            if (i == (count - 1) && mPcHeight < maxCacheHeight * count) {//latst cacheBitmap and bitmap width < maxCacheHeight
                if (DBG)
                    Log.d(TAG, "latst cacheBitmap and cacheBitmap width < maxCacheHeight, layout height= " + (mPcHeight - i * maxCacheHeight));
                textView.layout(0, 0, mPcWidth, mPcHeight - i * maxCacheHeight);
            }
            cacheBitmap = textView.getDrawingCache();
            remainBitmapHeight = cacheBitmap.getHeight();


            if (DBG)
                Log.d(TAG, "i= " + i + ", cacheBitmap= " + cacheBitmap);
            if (cacheBitmap != null){

                if (DBG)
                    Log.d(TAG, "cacheBitmap width= " + cacheBitmap.getWidth()
                            + ", height= " + cacheBitmap.getHeight()
                            + ", beginXinTexture= " + beginXinTexture
                             + ", beginYinTexture= " + beginYinTexture);
                if (beginYinTexture > 0) {//setpixels in remain space
                    remainAvailableTextureHeightPerCell = mTextureHeight - beginYinTexture;
                    if (DBG)
                        Log.d(TAG, "cacheBitmap height= " + cacheBitmap.getHeight() + ", remainAvailableTextureHeightPerCell= " + remainAvailableTextureHeightPerCell);
                    if (cacheBitmap.getHeight() >= remainAvailableTextureHeightPerCell) {
                        for (int k = 0; k < remainAvailableTextureHeightPerCell; k++) {
                            cacheBitmap.getPixels(content, 0, cacheBitmap.getWidth(), 0, k, mPcWidth, 1);
                            mBitmap.setPixels(content, 0, mBitmap.getWidth(), beginXinTexture, beginYinTexture + k, mPcWidth, 1);
                        }
                        beginXinTexture += mPcWidth;
                        beginYinTexture = 0;
                        remainBitmapHeight -= remainAvailableTextureHeightPerCell;
                        remainAvailableTextureHeightPerCell = mTextureHeight;

                    } else {//cacheBitmap.getWidth() < remainAvailableTextureHeightPerCell
                        for (int k = 0; k < cacheBitmap.getHeight(); k++) {
                            cacheBitmap.getPixels(content, 0, cacheBitmap.getWidth(), 0, k, cacheBitmap.getWidth(), 1);
                            mBitmap.setPixels(content, 0, mBitmap.getWidth(), beginXinTexture, beginYinTexture + k, cacheBitmap.getWidth(), 1);
                        }
                        beginYinTexture += cacheBitmap.getHeight();
                        continue;

                    }

                }

                //beginXinTexture = 0
                if (remainBitmapHeight <= 0)
                    continue;
                segments = remainBitmapHeight / mTextureHeight;
                if (remainBitmapHeight % mTextureHeight > 0)
                    segments++;
                if (DBG)
                    Log.d(TAG, "mPcWidth= " + mPcWidth + ", remainBitmapHeight= " + remainBitmapHeight
                            + ", segments= " + segments + ", beginXinTexture= " + beginXinTexture
                     + ", beginYinTexture= " + beginYinTexture);
                int beginYinCacheBitmap;
                for (int j = 0; j < segments; j++) {
                    beginYinCacheBitmap = cacheBitmap.getHeight() - remainBitmapHeight;
                    for (int k = 0; k <  Math.min(remainAvailableTextureHeightPerCell, remainBitmapHeight); k++) {
                        if (READ_DBG)
                            Log.d(TAG, "i= " + i + ", j= " + j + ", k= " + k + ", beginYinCacheBitmap= " + beginYinCacheBitmap);
                        cacheBitmap.getPixels(content, 0, cacheBitmap.getWidth(),
                                0, beginYinCacheBitmap + k, mPcWidth, 1);
                        mBitmap.setPixels(content, 0, mTextureWidth,
                                beginXinTexture, beginYinTexture + k, mPcWidth, 1);
                    }

                    if (DBG)
                        Log.d(TAG, "j= " + j + ", remainAvailableTextureHeightPerCell= " + remainAvailableTextureHeightPerCell
                         + ", remainBitmapHeight= " + remainBitmapHeight);
                    if ((j < segments - 1) || ((j == (segments - 1) && (remainAvailableTextureHeightPerCell == remainBitmapHeight)))) {
                        beginXinTexture += mPcWidth;
                    }
                    beginYinTexture += Math.min(remainAvailableTextureHeightPerCell, remainBitmapHeight);
                    if (beginYinTexture >= mTextureHeight)
                        beginYinTexture = 0;

                    if (j < segments - 1) {
                        remainBitmapHeight -= Math.min(remainAvailableTextureHeightPerCell, remainBitmapHeight);
                        remainAvailableTextureHeightPerCell = mTextureHeight - beginYinTexture;
                    }
                }

            }


        }



    }

    private void getFatPicAndDrawToTexture(TextView textView) {
        int maxCacheWidth = ViewConfiguration.get(mContext).getScaledMaximumDrawingCacheSize() / 4 / mPcHeight;
        if (DBG)
            Log.d(TAG, "getFatPicAndDrawToTexture. maxCacheWidth= " + maxCacheWidth);
        textView.setDrawingCacheEnabled(true);
        textView.layout(0, 0, maxCacheWidth, mPcHeight);
        Bitmap cacheBitmap;
        int[] content = new int[maxCacheWidth];
        int beginXinTexture = 0, beginYinTexture = 0, segments = 0, remainAvailableTextureWidthPerCell = mTextureWidth,
            remainBitmapWidth, readSize;
        int count = mPcWidth / maxCacheWidth;
        if (mPcWidth % maxCacheWidth > 0)
            count++;

        if (DBG)
            Log.d(TAG, "count= " + count);
        for (int i = 0; i < count; i++){
            if (i > 0) {
                textView.destroyDrawingCache();
                textView.scrollBy(maxCacheWidth, 0);//
            }

            if (i == (count - 1) && mPcWidth < maxCacheWidth * count) {//latst cacheBitmap and bitmap width < maxCacheWidth
                if (DBG)
                    Log.d(TAG, "latst cacheBitmap and cacheBitmap width < maxCacheWidth, layout width= " + (mPcWidth - i * maxCacheWidth));
                    textView.layout(0, 0, mPcWidth - i * maxCacheWidth, mPcHeight);
            }
            cacheBitmap = textView.getDrawingCache();
            remainBitmapWidth = cacheBitmap.getWidth();

            if (DBG)
                Log.d(TAG, "i= " + i + ", cacheBitmap= " + cacheBitmap);

            if (cacheBitmap != null){
                if (DBG)
                    Log.d(TAG, "cacheBitmap width= " + cacheBitmap.getWidth()
                            + ", height= " + cacheBitmap.getHeight()
                            + ", beginXinTexture= " + beginXinTexture
                            + ", beginYinTexture= " + beginYinTexture);

                if (beginXinTexture > 0) {//setpixels in remain space
                    remainAvailableTextureWidthPerCell = mTextureWidth - beginXinTexture;
                    if (DBG)
                        Log.d(TAG, "cacheBitmap width= " + cacheBitmap.getWidth() + ", remainAvailableTextureWidthPerCell= " + remainAvailableTextureWidthPerCell);
                    if (cacheBitmap.getWidth() >= remainAvailableTextureWidthPerCell) {
                        for (int k = 0; k < cacheBitmap.getHeight(); k++) {
                            cacheBitmap.getPixels(content, 0, cacheBitmap.getWidth(), 0, k, remainAvailableTextureWidthPerCell, 1);
                            mBitmap.setPixels(content, 0, mBitmap.getWidth(), beginXinTexture, beginYinTexture + k, remainAvailableTextureWidthPerCell, 1);
                        }
                        beginXinTexture = 0;
                        beginYinTexture += cacheBitmap.getHeight();
                        remainBitmapWidth -= remainAvailableTextureWidthPerCell;

                    } else {//cacheBitmap.getWidth() < remainAvailableTextureWidthPerCell
                        for (int k = 0; k < cacheBitmap.getHeight(); k++) {
                            cacheBitmap.getPixels(content, 0, cacheBitmap.getWidth(), 0, k, cacheBitmap.getWidth(), 1);
                            mBitmap.setPixels(content, 0, mBitmap.getWidth(), beginXinTexture, beginYinTexture + k, cacheBitmap.getWidth(), 1);
                        }
                        beginXinTexture += cacheBitmap.getWidth();
                        continue;

                    }

                }

                //beginXinTexture = 0
                if (remainBitmapWidth <= 0)
                    continue;
                segments = remainBitmapWidth / mTextureWidth;
                if (remainBitmapWidth % mTextureWidth > 0)
                    segments++;

                int beginXinCacheBitmap;
                if (DBG)
                    Log.d(TAG, "mPcWidth= " + mPcWidth + ", remainBitmapWidth= " + remainBitmapWidth + ", segments= " + segments);
                for (int j = 0; j < segments; j++) {
                    beginXinCacheBitmap = cacheBitmap.getWidth() - remainBitmapWidth;
                    readSize = Math.min(remainBitmapWidth - j * mTextureWidth, mTextureWidth);
                    for (int k = 0; k < cacheBitmap.getHeight(); k++) {
                        if (READ_DBG)
                            Log.d(TAG, "i= " + i + "j= " + j + ", k= " + k);
                        cacheBitmap.getPixels(content, 0, cacheBitmap.getWidth(),
                                beginXinCacheBitmap, k, readSize, 1);
                        mBitmap.setPixels(content, 0, mTextureWidth, 0, beginYinTexture + k, readSize, 1);
                    }
                    if (readSize == mTextureWidth)
                        beginXinTexture = 0;
                    else beginXinTexture = readSize;

                    if ((j < segments - 1) || (j == (segments - 1) && readSize == mTextureWidth))
                        beginYinTexture += cacheBitmap.getHeight();

                    if (j < (segments - 1))
                        remainBitmapWidth -= readSize;

                }

            }

        }

    }

    private void initTextView(TextView textView) {
        if (DBG)
            Log.d(TAG, "initTextView. mWidth= " + mWidth + ", mHeight= " + mHeight);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(mWidth, 1);
        textView.setLayoutParams(params);
        textView.setWidth(mWidth);
        textView.setHeight(1);

        textView.getPaint().setAntiAlias(AppController.getInstance().getCfg().isAntialias());
        textView.setTextColor(GraphUtils.parseColor(mItem.textColor));

        if (mItem.logfont != null) {
            textView.setLineSpacing(Integer.parseInt(mItem.logfont.lfHeight) / 4, 1.0f);

            // Size.
            if (mItem.logfont.lfHeight != null) {
                textView.getPaint().setTextSize(Integer.parseInt(mItem.logfont.lfHeight));
            }

            //style
            int style = Typeface.NORMAL;
            if ("1".equals(mItem.logfont.lfItalic)) {
                style = Typeface.ITALIC;
            }
            if ("700".equals(mItem.logfont.lfWeight)) {
                style |= Typeface.BOLD;
            }
            if ("1".equals(mItem.logfont.lfUnderline)) {
                textView.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
            }

            Typeface typeface = AppController.getInstance().getTypeface(mItem.logfont.lfFaceName);
            if (typeface == null)
                typeface = Typeface.defaultFromStyle(style);
            else
                typeface = Typeface.create(typeface, style);

            if (DBG)
                Log.d(TAG, "style= " + style + ", lfFaceName= " + mItem.logfont.lfFaceName + ", typeface= " + typeface);

            textView.setTypeface(typeface, style);
        }

    }

    private boolean drawBitmapToTexture(Bitmap bitmap, int segments) {

        if (DBG)
            Log.d(TAG, "drawBitmapToTexture. bitmap= " + bitmap);
        if (bitmap == null) {
            if (DBG)
                Log.d(TAG, "the drawing cache is null.");
            //TODO::draw text to bitmap

            return false;
        }

        int[] content;

        if (mPcWidth > mPcHeight){//fat

            content = new int[Math.min(mTextureWidth, mPcWidth)];
            if (DBG)
                Log.d(TAG, "segments= " + segments);

            for (int i = 0; i < segments; i++){
                int readSize = Math.min(mPcWidth - i * mTextureWidth, mTextureWidth);
                for (int j = 0; j < mPcHeight; j++){
                    if (RENDER_DBG)
                        Log.d(TAG, "i= " + i + "j= " + j + ", i * mPcHeight() + j= " + (i * mPcHeight + j));
                    bitmap.getPixels(content, 0, mTextureWidth, i * mTextureWidth, j, readSize, 1);
                    mBitmap.setPixels(content, 0, mTextureWidth, 0, i * mPcHeight + j, readSize, 1);
                }
            }

        } else {//tall
            if (DBG)
                Log.d(TAG, "tall. mPcWidth = " + mPcWidth + ", mPcHeight= " + mPcHeight);

            content = new int[mPcWidth];
            if (DBG)
                Log.d(TAG, "tall.segments= " + segments);

            for (int i = 0; i < segments; i++) {
                int readHeight = Math.min(mPcHeight - i * mTextureWidth, mTextureWidth);
                for (int j = 0; j <readHeight ; j++) {
                    bitmap.getPixels(content, 0, mTextureWidth, 0, i * mTextureWidth + j, mPcWidth, 1);
                    mBitmap.setPixels(content, 0, mTextureWidth, i * mPcWidth, j, mPcWidth, 1);
                }
            }
        }


        return true;

    }


    public void initUpdateTex(int textId) {
//        // Update tex.
//        final int texToUpload = Math.min(MAX_ACTIVE_TEX, mTexCount);
//        for (int i = 0; i < texToUpload; i++) {
//            updateTexIndexToTexId(i, i);
//        }
        updateTexIndexToTexId(textId, textId);
    }

    public int lastColumnHeight() {
        return ((mPcHeight - 1) % mTextureHeight) + 1;
    }

    public int lastRowWidth() {
        return ((mPcWidth - 1) % mTextureWidth) + 1;
    }


    /**
     * @param size
     * @param blockSize
     * @return Minimal blocks for containing the total size, each of a blockSize.
     */
    public int ceilingBlocks(int size, int blockSize) {
        int blocks = (size + blockSize - 1) / blockSize;
        if (DBG)
            Log.d(TAG, "ceilingBlocks. [blocks=" + blocks + ", size=" + size + ", blockSize=" + blockSize);
        return blocks;
    }

    //read fat multiline pic(width > height)
    private void readFatBlock(InputStream is, byte[] content, Bitmap bm, int targetStrip, int targetOffset, int readSrcWidth, int readRows,
                              int lastReadWidth) throws IOException {
        for (int j = 0; j < readRows; j++) {
            if (READ_DBG)
                Log.d(TAG, "readFatBlock. j= " + j + ", readRows= " + readRows);

            if (j == readRows - 1) {
                if (LAST_COLUM_DBG)
                    Log.d(TAG, "read last row.");
//                ByteStreams.readFully(is, content, targetOffset * 4 + targetStrip * (j) * 4, lastReadWidth * 1 * 4);
                ByteStreams.readFully(is, content, 0, lastReadWidth * 1 * 4);

//                GraphUtils.convertRGBFromPC(content);
                bm.setPixels(byteArray2intArray(content), 0, mTextureWidth, 0, targetOffset + j * mPcHeight, lastReadWidth, 1);
            } else {
                if (READ_DBG)
                    Log.d(TAG, "read previous rows.");
//                ByteStreams.readFully(is, content, targetOffset * 4 + targetStrip * j * 4, readSrcWidth * 1 * 4);
                ByteStreams.readFully(is, content, 0, readSrcWidth * 1 * 4);

//                GraphUtils.convertRGBFromPC(content);
                bm.setPixels(byteArray2intArray(content), 0, mTextureWidth, 0, targetOffset + j * mPcHeight, readSrcWidth, 1);

            }
        }
    }

    //read tall multiline pic(height > width)
    private void readTallBlock(InputStream is, byte[] content, Bitmap bm, int targetStrip, int targetOffset, int readSrcWidth, int readSrcHeight,
                               int srcWidth) throws IOException {
        for (int j = 0; j < readSrcHeight; j++) {
            if (READ_DBG)
                Log.d(TAG, "readTallBlock. j= " + j + ", readSrcHeight= " + readSrcHeight);

            // The picture is bigger than my width.
            // The maximum width the texture could contain is myWidth, so read myWidth, and skip the remaining pixels in the line.
            if (srcWidth > readSrcWidth) {// fat pic
                // 4 stands for RGBA.
                ByteStreams.readFully(is, content, targetOffset * 4 + targetStrip * j * 4, readSrcWidth * 1 * 4);
                ByteStreams.skipFully(is, (srcWidth - readSrcWidth) * 4);


            } else { // srcWidth == readSrcWidth, always read srcWidtgh >= readSrcWidth.
                // targetOffset * 4 should also *4.
//                ByteStreams.readFully(is, content, targetOffset * 4 + targetStrip * j * 4, readSrcWidth * 1 * 4);

                ByteStreams.readFully(is, content, 0, readSrcWidth * 1 * 4);
//                GraphUtils.convertRGBFromPC(content);

//                ByteBuffer bb = ByteBuffer.allocateDirect(4);
//                IntBuffer ib = bb.asIntBuffer();
//                ib.t

                bm.setPixels(byteArray2intArray(content), 0, mTextureWidth, targetOffset, j, readSrcWidth, 1);
            }
        }
    }


    private boolean isLastTex(int i) {
        return i == mTexCount - 1;
    }

    private void updateTexIndexToTexId(int texIndex, int texIdInx) {
        if (DBG)
            Log.d(TAG, "updateTexIndexToTexId. [texIndex=" + texIndex + ", texIdInx=" + texIdInx);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexIds[texIdInx]);
        // TextRenderer.checkGLError("glBindTexture");
        updateImage(texIndex);
    }

    private void updateImage(int picIndex) {
//        String keyImgId = mScrollpicinfo.filePath.MD5 + picIndex;
//        MyBitmap bitmapFromMemCache = AppController.getInstance().getBitmapFromMemCache(keyImgId);
//        if (bitmapFromMemCache != null)
//            // Assigns the OpenGL texture with the Bitmap
//            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmapFromMemCache.getBitmap(), 0);

//        if (DBG)
//            Log.d(TAG, "isETC1Supported= " + ETC1Util.isETC1Supported() + ", etc1Texture= " + etc1Texture);
//        if (ETC1Util.isETC1Supported() && etc1Texture != null) {
//            //GLES10.GL_TEXTURE_2D, 0, 0, GLES10.GL_RGB, GLES10.GL_UNSIGNED_SHORT_5_6_5, etc1Texture
//            GLES20.glCompressedTexImage2D(GLES10.GL_TEXTURE_2D, 0, ETC1.ETC1_RGB8_OES, etc1Texture.getWidth(), etc1Texture.getHeight(),
//                    0, etc1Texture.getData().remaining(), etc1Texture.getData());
//        } else {
//        if (bitmapFromMemCache != null)
////             Assigns the OpenGL texture with the Bitmap
//            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmapFromMemCache.getBitmap(), 0);
        if (DBG)
            Log.d(TAG, "updateImage. mBitmap= " + mBitmap);
        if (mBitmap != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mBitmap, 0);
            mBitmap.recycle();
            mBitmap = null;

            System.gc();
//                System.gc();
        }

//        }

    }

    /* [Draw Canvas To Texture] */
    protected short[] mIndices;

    protected void initShapes() {
        if (mQuadSegs == null) {
            Log.w(TAG, "initShapes. [no content.");
            return;
        }

        int quadsSize = mQuadSegs.length;
        if (DBG)
            Log.d(TAG, "quadsSize : " + quadsSize);
        // Initialize vertex Buffer for triangle
        final ByteBuffer vbb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                // 12 floats per quad. 12 = 3 (xyz coords) * 4 (vertex)
                quadsSize * 12 * 4);
        vbb.order(ByteOrder.nativeOrder()); // Use the device hardware's native byte order
        mQuadVB = vbb.asFloatBuffer(); // Create a floating point buffer from the ByteBuffer
        // mQuadVB.put(quadVerts); // Add the coordinates to the FloatBuffer
        for (int i = 0; i < mQuadSegs.length; i++) {
            mQuadVB.put(mQuadSegs[i].getQuadPos()); // Add the coordinates to the FloatBuffer
        }
        mQuadVB.position(0); // Set the buffer to read the first coordinate

        // float totalHeight = 0.0f;
        // for (int i = 0; i < mQuadSegs.length; i++) {
        // totalHeight += mQuadSegs[i].getOffset();
        // }
        // mTotalTextHeight = totalHeight;

        final ByteBuffer vbb_t = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                quadsSize * 12 * 4);
        vbb_t.order(ByteOrder.nativeOrder()); // Use the device hardware's native byte order
        mQuadTCB = vbb_t.asFloatBuffer(); // Create a floating point buffer from the ByteBuffer

        for (int i = 0; i < mQuadSegs.length; i++) {
            mQuadTCB.put(mQuadSegs[i].getCoords(mTextureWidth, mTextureHeight)); // Add the
            // coordinates
            // to the FloatBuffer
        }

        // mQuadTCB.put(quadCoords); // Add the coordinates to the FloatBuffer
        mQuadTCB.position(0); // Set the buffer to read the first coordinate

        mVertexcount = mQuadVB.capacity() / 3;
        mIndices = new short[mVertexcount];
        for (short i = 0; i < mVertexcount; i++) {
            mIndices[i] = i;
        }

        final int size = mIndices.length * 2; // 2 bytes per short.
        // Initialize vertex Buffer for triangle
        final ByteBuffer indicesbuffer = ByteBuffer.allocateDirect(size);
        indicesbuffer.order(ByteOrder.nativeOrder());
        indicesbuffer.asShortBuffer().put(mIndices).position(0);

        if (DBG)
            Log.d(TAG, "initShapes. [quadVB.capacity()=" + mQuadVB.capacity() + ", vbb capacity=" + vbb.capacity() + ", vertexcount="
                    + mVertexcount
                    + ", indicesbuffer=" + indicesbuffer.capacity());

        final int[] buffers = {0, 0, 0};
        GLES20.glGenBuffers(3, buffers, 0);
        final int vertexBufferId = buffers[0];
        final int textureBufferId = buffers[1];
        final int indexBufferId = buffers[2];

        createBuffer(GLES20.GL_ARRAY_BUFFER, vbb, vbb.capacity(), vertexBufferId);
        createBuffer(GLES20.GL_ARRAY_BUFFER, vbb_t, vbb_t.capacity(), textureBufferId);
        // createBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, sb, sb.capacity(), indexBufferId);
        createBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesbuffer, indicesbuffer.capacity(), indexBufferId);
    }

    static void createBuffer(int target, Buffer buf, int size, int bufferId) {
        GLES20.glBindBuffer(target, bufferId);
        GLES20.glBufferData(target, size, buf, GLES20.GL_STATIC_DRAW);

        // The index must be the last one, as we'd like to drawElements.
        if (target != GLES20.GL_ELEMENT_ARRAY_BUFFER)
            GLES20.glBindBuffer(target, 0);
    }

    private int loadShader(int type, String shaderCode) {
        // Create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // Add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        if (DBG) {
            // Print the shader info log to the Android log output.
            Log.v(TAG, "Results of compiling source:" + "\n" + shaderCode + "\n:"
                    + GLES20.glGetShaderInfoLog(shader));
        }

        return shader;
    }

    private int muMVPMatrixHandle;
    protected int muMMatrixHandle;
    private int muTextureHandle;
    /**
     * Texture dimension.
     */
    // private int muTexScaleHandle;

    private float[] mMVPMatrix = new float[16];
    protected float[] mMMatrix = new float[16];
    private float[] mVMatrix = new float[16];
    private float[] mProjMatrix = new float[16];

    protected int mProgram;
    protected int maPositionHandle;
    protected int maTexCoordsHandle;
    protected int[] mTexIds;
    /**
     * Vertex position attribute buffer.
     */
    protected FloatBuffer mQuadVB;
    /**
     * Vertex texture coordinate buffer.
     */
    protected FloatBuffer mQuadTCB;

    private final String vertexShaderCode =
            "precision highp float;  \n" +
                    // This matrix member variable provides a hook to manipulate
                    // the coordinates of the objects that use this vertex shader
                    "uniform mat4 uMVPMatrix;   \n" +
                    "uniform mat4 uMMatrix;   \n" +
                    "attribute vec4 vPosition;  \n" +
                    "attribute vec4 vTexCoord;  \n" +
                    "varying vec4 v_v4TexCoord; \n" +

                    "void main(){               \n" +

                    // The matrix must be included as a modifier of gl_Position
                    " v_v4TexCoord = vTexCoord; \n" +
                    " gl_Position = uMVPMatrix * uMMatrix * vPosition; \n" +

                    "}  \n";
    //
    private final String fragmentShaderCode =
            "precision highp float;  \n"
                    + "uniform sampler2D u_s2dTexture; \n"
                    // +
                    // "uniform vec2 uTexScale; \n"
                    + "varying vec4 v_v4TexCoord; \n"
                    + "void main(){              \n"
//
//                    // + "vec2 mult=vec2((2.0*v_v4TexCoord.x + 1.0)/(2.0*2048.0), (2.0*v_v4TexCoord.y + 1.0)/(2.0*4096.0));\n"
//                    // + "vec2 mult=vec2((2.0 * v_v4TexCoord.x + 1.0)/(2.0*1024.0), (2.0 * v_v4TexCoord.y + 1.0) / (2.0 * 1024.0));\n"
//                    // + "vec2 mult=vec2((2.0 * v_v4TexCoord.x + 1.0)/(2.0*1024.0), (2.0 * v_v4TexCoord.y + 1.0) / (2.0 * 1024.0));\n"
//                    // GOOD + "vec2 mult=vec2((v_v4TexCoord.x/1024.0), (v_v4TexCoord.y /1024.0));\n"
//
//                    // + "vec2 mult=vec2((v_v4TexCoord.x)/2048.0, (v_v4TexCoord.y)/4096.0);\n"
//
//                    // + "gl_FragColor = texture2D(u_s2dTexture, v_v4TexCoord.xy/1024.0);\n"
//                    // + "gl_FragColor = texture2D(u_s2dTexture, mult);\n"
//
//
////                    + " vec4 mult=(2.0*v_v4TexCoord + 1.0)/(2.0*1024.0);\n"
////                    + " gl_FragColor = texture2D(u_s2dTexture, mult.xy); \n"
//
                    + " gl_FragColor = texture2D(u_s2dTexture, v_v4TexCoord.xy); \n"
////                    + " gl_FragColor = texture2D(u_s2dTexture, v_v4TexCoord.xy) + vec4(v_v4TexCoord.y, 0.2, 0.2, 0.0); \n"
//
//                    // Vec4 RGBA. Transparent the extra pixel which is larger than the texture width/height.
//                    // + "if (v_v4TexCoord.x > 200.0/2048.0) { gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0); } \n"
//
//                    // " gl_FragColor = texture2D(u_s2dTexture, v_v4TexCoord.xy) + vec4(v_v4TexCoord.y, 0.2, 0.2, 1.0); \n"
//
//                    // " gl_FragColor = texture2D(u_s2dTexture, v_v4TexCoord.xy) + vec4(v_v4TexCoord.x, 0.0, 0.0, 1.0); \n"
//                    // // " gl_FragColor = vec4(0.0, 0.2, 1.0, 1.0); \n" +
//
//                    // "vec2 mult=vec2((2.0*v_v4TexCoord.x - 1.0)/(2.0*uTexScale.x), (2.0*v_v4TexCoord.y - 1.0)/(2.0*uTexScale.y)); \n"
//                    // +
//                    // "gl_FragColor = texture2D(u_s2dTexture, mult.xy); \n"
//                    // "gl_FragColor = texture2D(u_s2dTexture, mult.xy) + vec4((2.0*v_v4TexCoord.x - 1.0)/(2.0*uTexScale.x), 0.0, 0.0, 1.0); \n"
                    +

                    "}                         \n";

    protected int mWidth;
    protected int mHeight;
    private int mBackgroundColor;
    protected int mRepeatCount = 0;
    private ItemMLScrollMultipic2View mMultilinesScrollMultipic2View;
    protected QuadSegment[] mQuadSegs;
    protected int mLineHeight;
    protected int mVertexcount;
    protected HashCode mTextBitmapHash;
    private int mMaxSegmentsPerTexContain;

    public void setDimension(int width, int height) {
        mWidth = width;
        // TODO Auto-generated method stub
        mHeight = height;

    }

    public void setPixelPerFrame(float speedByFrame) {
        // moving left.
//        mPixelPerFrame = -speedByFrame;
        if (speedByFrame >= 1.0f) {
            mPixelPerFrame = Math.round(speedByFrame);
            mIsGreaterThanAPixelPerFrame = true;
        } else {
            mPixelPerFrame = speedByFrame;
            mIsGreaterThanAPixelPerFrame = false;
        }
    }

    public void setRepeatCount(int repeatCount) {
        mRepeatCount = repeatCount;
    }

    public void setView(ItemMLScrollMultipic2View view) {
        mMultilinesScrollMultipic2View = view;
    }

    public void setTextItemBitmapHash(HashCode textBitmapHash) {
        mTextBitmapHash = textBitmapHash;
        // TODO Auto-generated method stub

    }

    private void setRealSegmentsPerTex(int maxHeightPerTex) {

//        if (mIsTallPCPic){

        // tall multipic
        if (mPcHeight > maxHeightPerTex) {
            mRealSegmentsPerTex = mMaxSegmentsPerTexContain;
            isTallPCPicSurplus = true;
        } else {
            mRealSegmentsPerTex = ceilingBlocks(mPcHeight, mTextureHeight);
            isTallPCPicSurplus = false;
        }

//        } else {  // fat multipic
//            if (mPcWidth > maxHeightPerTex)
//                mRealSegmentsPerTex = mMaxSegmentsPerTexContain;
//            else
//                mRealSegmentsPerTex = ceilingBlocks(mPcWidth, mTextureWidth);
//        }

    }


    public static void readFatTextPic(InputStream is, Bitmap bm, int pcWidth, int pcHeight, int textureWidth) throws IOException{
        byte[] content = new byte[Math.min(pcWidth, textureWidth) * 4];
        int maxPicWidthPerTexture = textureWidth / pcHeight * textureWidth;
        int readWidth = Math.min(maxPicWidthPerTexture, pcWidth);
        int segments = readWidth / textureWidth;
        if (readWidth % textureWidth > 0)
            segments++;
        if (DBG)
            Log.d(TAG, "readFatTextPic. pcWidth= " + pcWidth + ", pcHeight= " + pcHeight + ", textureWidth= " + textureWidth
                    + ", maxPicWidthPerTexture= " + maxPicWidthPerTexture + ", readWidth= " + readWidth + ", segments= " + segments);

        for (int i = 0; i < pcHeight; i++) {
            for (int j = 0; j < segments; j++) {
                int readSize = Math.min(readWidth - j * textureWidth, textureWidth);
                if (READ_DBG)
                    Log.d(TAG, "i= " + i + ", j= " + j + ", readSize= " + readSize);
                if (readSize != 0) {
                    ByteStreams.readFully(is, content, 0, readSize * 4);
                    bm.setPixels(byteArray2intArray(content), 0, textureWidth, 0, i + j * pcHeight, readSize, 1);
                }
                //skip
                if ((j == segments - 1) && (readWidth < pcWidth))
                    ByteStreams.skipFully(is,( pcWidth - readWidth) * 4);
            }
        }
    }

    public static void readTallTextPic(InputStream is, Bitmap bm, int pcWidth, int pcHeight, int textureWidth) throws IOException{
        byte[] content = new byte[pcWidth * 4];
        int maxPicHeightPerTexture = textureWidth / pcWidth * textureWidth;
        int readHeight = Math.min(maxPicHeightPerTexture, pcHeight);
        int segments = readHeight / textureWidth;
        if (readHeight % textureWidth > 0)
            segments++;
        if (DBG)
            Log.d(TAG, "readTallTextPic. pcWidth= " + pcWidth + ", pcHeight= " + pcHeight + ", textureWidth= " + textureWidth
             + ", maxPicHeightPerTexture= " + maxPicHeightPerTexture + ", readHeight= " + readHeight + ", segments= " + segments);

        for (int i = 0; i < segments; i++) {
            int readSize = Math.min(readHeight - i * textureWidth, textureWidth);
            for (int j = 0; j <readSize ; j++) {
                    ByteStreams.readFully(is, content, 0, pcWidth * 4);
                    bm.setPixels(byteArray2intArray(content), 0, textureWidth, i * pcWidth, j, pcWidth, 1);

            }
        }
    }

    public static int[] byteArray2intArray(byte[] content) {

        int[] arys = new int[content.length / 4];
        for (int i = 0; i < arys.length; i++) {
            arys[i] = (content[i * 4] & 0xFF)
                    | (content[i * 4 + 1] & 0xFF) << 8
                    | (content[i * 4 + 2] & 0xFF) << 16
                    | (content[i * 4 + 3] & 0xFF) << 24;
        }
        return arys;
    }

    //memory free is enough or not
    public static boolean isMemoryEnough(int textureWidth) {

        long memNeed = (textureWidth * textureWidth * 4) / 1048576L + 10;//MB

        long memFree = getFreeMem();

        if (isMemFreeEnough(memFree, memNeed))
            return true;

        if (DBG)
            Log.d(TAG, "compare memFree and needMem in first time, memFree < needMem. memFree= " + memFree + ", needMem= " + memNeed);
        System.gc();
        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        memFree = getFreeMem();
        if (isMemFreeEnough(memFree, memNeed))
            return true;

        //memfree <= memneed
        if (DBG)
            Log.d(TAG, "memfree is not enough, abort.");
        return false;

    }

    //compare memFree & memNeed
    private static boolean isMemFreeEnough(long memFree, long memNeed) {
        Log.d(TAG, "isMemFreeEnough. memFree= " + memFree + ", memNeed= " + memNeed);
        if (memFree == 0L) {
            Log.e(TAG, "Bad mem info.");
            return false;
        }

        if (memFree > memNeed) {
            return true;
        }

        //memfree <= memneed
        return false;
    }

    private static long getFreeMem() {
        long memFree = 0L;
        RandomAccessFile reader = null;
        try {
            reader = new RandomAccessFile("/proc/meminfo", "r");
            if (reader == null) {
                Log.e(TAG, "Cannot open meminfo. Abort.");

                return memFree;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = PATTERN.matcher(line);
                if (m.find()) {

                    if (DBG)
                        Log.d(TAG, " name= " + m.group(1) + ", size= " + Long.parseLong(m.group(2)));

                    if ("MemFree".equalsIgnoreCase(m.group(1))) {
                        memFree = Long.parseLong(m.group(2)) / 1024L;//MB
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return memFree;
    }

    private void testAndroidMemoryInfo(long memNeed) {
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(info);

        // availMem = MemFree + Cached in /proc/meminfo.
        long availMem = info.availMem >> 10;

        long freeMem = info.availMem / 1048576L;

        Log.i(TAG, "系统剩余内存:" + (info.availMem >> 10) + "k" + ", availMem/1048576L= " + info.availMem / 1048576L + "M");
        Log.i(TAG, "系统是否处于低内存运行：" + info.lowMemory + ", total mem= " + info.totalMem + ",  Process.getFreeMemory()= " + Process.getFreeMemory());
        Log.i(TAG, "当系统剩余内存低于" + (info.threshold >> 10) + "k" + "时就看成低内存运行");
        Log.d(TAG, "needMem= " + memNeed + ",freeMem(MemFree + Cached)= " + freeMem);
    }

    //ETC
//    public void load() {
//        ETC1Util.loadTexture(GLES10.GL_TEXTURE_2D, 0, 0,
//                GLES10.GL_RGB, GLES10.GL_UNSIGNED_SHORT_5_6_5, etc1Texture);
//    }

//    public void convertContent(byte[] content) {
//        byte[] content2 = new byte[mPcWidth * 3];
//        int offset = 1, n = 0;
//        for (int i = 0; i < content2.length; i++) {
//            content2[i] = content[i + offset];
//            n++;
//            if (n == 3) {
//                n = 0;
//                offset++;
//            }
//        }
//    }

}
