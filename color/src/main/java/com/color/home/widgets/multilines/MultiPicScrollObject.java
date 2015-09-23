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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.AppController.MyBitmap;
import com.color.home.ProgramParser.ScrollPicInfo;
import com.color.home.widgets.ItemsAdapter;
import com.color.home.widgets.singleline.QuadGenerator;
import com.color.home.widgets.singleline.QuadSegment;
import com.color.home.widgets.singleline.localscroll.TextRenderer;
import com.color.home.widgets.singleline.pcscroll.SLPCTextObject;
import com.google.common.hash.HashCode;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class MultiPicScrollObject {
    // private static final int MAX_TEXTURE_WIDTH_HEIGHT = 4096;
    private final static String TAG = "MultiPicScrollObject";
    private static final boolean DBG = false;
    private static final boolean PNG_DBG = false;
    // private static final String MNT_SDCARD_PNGS = "/mnt/sdcard/pngs";
    private static final int MAX_TEXTURE_WIDTH_HEIGHT = 2048;
    /**
     * Must be >= 2. Because there could be two quads w/ different texs.
     */
    // private static final int MAX_ACTIVE_TEX = 64;
    private static final int MAX_ACTIVE_TEX = 4;
    protected int mPcWidth;
    /**
     * Full bitmap. e.g., 38498.
     */
    protected int mPcHeight;
    protected float mPixelPerFrame = -4.0f;
    private int mColor;
    protected int mCurrentRepeats = 0;
    private Context mContext;
    private ScrollPicInfo mScrollpicinfo;
    /**
     * We split the whole big bitmap into how many pieces.
     */
    private int mTexCount;

    // Constructor initialize all necessary members
    public MultiPicScrollObject(Context context, ScrollPicInfo scrollpicinfo) {
        mContext = context;
        mScrollpicinfo = scrollpicinfo;
        mPcWidth = 1;
        mPcHeight = 1;

        // mPicCount = Integer.parseInt(mScrollpicinfo.picCount);
    }

    void update() {
        if (DBG)
            Log.d(TAG, "update.");
        /* [Update Text Size] */

        // 3. Generate texture with the new evaluated font size
        drawCanvasToTexture();

        initUpdateTex();

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

        // isUpdateNeeded = false;
    }

    public void resetPos() {
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

    public void render() {
        // // Add program to OpenGL environment
        float[] modelMat = mMMatrix;
        Matrix.translateM(modelMat, 0, 0.f, -mPixelPerFrame, 0.f);

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

        if (modelMat[13] > mHeight + mPcHeight) {
            if (DBG)
                Log.d(TAG, "render. [modelMat[13]=" + modelMat[13]);
            resetPos();
            mSr.resetPage();
            // if repeat count == 0, infinite loop.
            if (mRepeatCount != 0) {
                if (++mCurrentRepeats >= mRepeatCount) {
                    notifyFinish();
                }
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

            if (offset >= MAX_TEXTURE_WIDTH_HEIGHT * (mCurQuadIndex + 1) && mCurQuadIndex <= (mQuadSegs.length - 1)) {
                mCurQuadIndex++; // NOTE: mCurQuadIndex could be mQuadSegs.length
                if (DBG)
                    Log.d(TAG, "render. [mCurQuadIndex=" + mCurQuadIndex);

                if (isAnotherTex(mCurQuadIndex) && mQuadSegs.length != mCurQuadIndex) {
                    if (DBG)
                        Log.d(TAG, "render. [isAnotherTex.");
                    int texIndex = mCurQuadIndex / mMaxColsPerTexContain;
                    if (texIndex >= MAX_ACTIVE_TEX) { // Prepare the next tex. NOTE: The 0 .. MAX_ACTIVE_TEX-1 were pre-set.
                        MultiPicScrollObject.this.updateTexIndexToTexId(texIndex, texIndex % MAX_ACTIVE_TEX); // 0 for page = 2
                    }
                }
            }

            if (mCurQuadIndex - 1 >= 0 && offset >= MAX_TEXTURE_WIDTH_HEIGHT * mCurQuadIndex
                    && offset <= mWindowHeight + MAX_TEXTURE_WIDTH_HEIGHT * mCurQuadIndex) { // Always draw the prev quad.
                drawQuad(mCurQuadIndex - 1);
            }

            if (mCurQuadIndex <= mQuadSegs.length - 1) {
                drawQuad(mCurQuadIndex);
            }

        }

        /**
         * Even and odd page draw w/ even/odd texture from the textids.
         * 
         * @param quadIndex
         */
        private void drawQuad(int quadIndex) {
            int texIndex = quadIndex / mMaxColsPerTexContain;
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexIds[texIndex % MAX_ACTIVE_TEX]);
            if (DBG)
                TextRenderer.checkGLError("glBindTexture");
            // 4 vertex indices per quad, 2 (unsigned short) bytes per index.

            // offset(indices here)'s unit is char* !!!!!!!!!!! not in the GL_UNSIGNED_SHORT.
            // so must use the (vertext count *2) for the offset.!!!!
            // void glDrawElements(GLenum mode, GLsizei count, GLenum type, const GLvoid *indices)
            GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, 4, GLES20.GL_UNSIGNED_SHORT, quadIndex * 4 * 2);
        }

        public boolean isAnotherTex(int quadIndex) {
            return quadIndex % mMaxColsPerTexContain == 0;
        }

    }

    private SegRender mSr;

    protected void notifyFinish() {
        if (DBG)
            Log.d(TAG, "notifyFinish. [mSingleLineTextSurfaceView=" + mMultilinesScrollMultipic2View);
        if (mMultilinesScrollMultipic2View != null)
            mMultilinesScrollMultipic2View.notifyPlayFinished();
    }

    private final int mTextureWidth = MAX_TEXTURE_WIDTH_HEIGHT;
    private final int mTextureHeight = MAX_TEXTURE_WIDTH_HEIGHT;

    /* [Draw Canvas To Texture] */
    private void drawCanvasToTexture() {

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
                return ;
            }

            ByteStreams.skipFully(is, 20);
            ByteStreams.readFully(is, head, 0, 8);

            ByteBuffer bb = ByteBuffer.wrap(head);
            bb.order(ByteOrder.LITTLE_ENDIAN); // if you want little-endian

            if (DBG)
                Log.i(TAG, "drawCanvasToTexture. [position=" + bb.position());

            mPcWidth = bb.getInt();
            mPcHeight = bb.getInt();

            mMaxColsPerTexContain = MAX_TEXTURE_WIDTH_HEIGHT / mPcWidth;
            if (mMaxColsPerTexContain == 0) {
                if (DBG)
                    Log.d(TAG, "drawCanvasToTexture. [pc width is more than 2048, abort.");
                return;
            }

            int heightConsumedPerTex = mMaxColsPerTexContain * MAX_TEXTURE_WIDTH_HEIGHT;
            // final int heightRemaining = mPcHeight % heightPerTex;
            mTexCount = ceilingBlocks(mPcHeight, heightConsumedPerTex);

            // Now the pic count is ready.
            genTexs();

            if (DBG)
                Log.i(TAG, "onCreate. [mBitmapWidth=" + mPcWidth + ", mBitmapHeight=" + mPcHeight + ", mHeight=" + mHeight
                        + ", mPicCount="
                        + mTexCount);

            // we skipped 20 read 8 = 28.
            ByteStreams.skipFully(is, 1024 - 28);

            // byte[] converted = new byte[mWidth * mHeight * 4];
            for (int i = 0; i < mTexCount; i++) {
                String keyImgId = mScrollpicinfo.filePath.MD5 + i;

                // Image already exist, offset to next image.
                if (AppController.getInstance().getBitmapFromMemCache(keyImgId) != null) {
                    if (DBG)
                        Log.d(TAG, "drawCanvasToTexture. [getBitmapFromMemCache exist for =" + keyImgId);
                    // MAX_TEXTURE_WIDTH_HEIGHT is inaccurate, but OK for the lastest item.
                    ByteStreams.skipFully(is, mPcWidth * MAX_TEXTURE_WIDTH_HEIGHT * mMaxColsPerTexContain * 4);

                    continue;
                }

                // Always square.
                byte[] content = new byte[mTextureWidth * mTextureHeight * 4];

                int lastTexReadHeight = ((mPcHeight - 1) % heightConsumedPerTex) + 1;
                int columns = isLastTex(i) ? ceilingBlocks(lastTexReadHeight, MAX_TEXTURE_WIDTH_HEIGHT) : mMaxColsPerTexContain;
                for (int j = 0; j < columns; j++) {
                    // last column in last texture => (lastTex && j == columns - 1)
                    int readSrcHeight = ((isLastTex(i) && j == columns - 1) ? lastColumnHeight() : MAX_TEXTURE_WIDTH_HEIGHT);
                    // int targetStrip = MAX_TEXTURE_WIDTH_HEIGHT;
                    // int targetOffset = j * mPcWidth;
                    // int readSrcWidth = Math.min(mPcWidth, MAX_TEXTURE_WIDTH_HEIGHT);
                    // // last column in last texture => (lastTex && j == columns - 1)
                    // int readSrcHeight = ((lastTex && j == columns - 1) ? ((mLastTexReadHeight - 1) % MAX_TEXTURE_WIDTH_HEIGHT + 1) :
                    // MAX_TEXTURE_WIDTH_HEIGHT);
                    readBlock(is, content, MAX_TEXTURE_WIDTH_HEIGHT, j * mPcWidth, Math.min(mPcWidth, MAX_TEXTURE_WIDTH_HEIGHT),
                            readSrcHeight, mPcWidth);
                }

                SLPCTextObject.convertRGBFromPC(content);

                // Now put these nice RGBA pixels into a Bitmap object

                Bitmap bm = Bitmap.createBitmap(mTextureWidth, mTextureHeight, Bitmap.Config.ARGB_8888);
                // bm.setPremultiplied(false);
                bm.copyPixelsFromBuffer(ByteBuffer.wrap(content, 0, mTextureWidth * mTextureHeight * 4));
                AppController.getInstance().addBitmapToMemoryCache(keyImgId, new MyBitmap(bm, mPcWidth, mPcHeight));

                if (PNG_DBG) {
                    new File("/mnt/sdcard/mul").mkdir();
                    QuadGenerator.toPng(bm, new File("/mnt/sdcard/mul/" + keyImgId + ".png"));
                }

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // Do nothing here.
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } finally {
            if (streamResolver != null) {
                streamResolver.close();
            }
        }

        // setImageBitmap(resultBm);
        final int quadSize = ceilingBlocks(mPcHeight, MAX_TEXTURE_WIDTH_HEIGHT);
        if (DBG)
            Log.d(TAG, "drawCanvasToTexture. [quadSize=" + quadSize);
        mQuadSegs = new QuadSegment[quadSize];
        for (int i = 0; i < quadSize; i++) {
            // The last quad.
            // if (isLastTex(i)) {
            // // GOOD. mQuadSegs[i] = new QuadSegment(1024, 2048);
            // mQuadSegs[i] = new QuadSegment(mPcWidth, mLastTexReadHeight, -mWidth / 2);
            // } else {
            // mQuadSegs[i] = new QuadSegment(mPcWidth, mTextureHeight, -mWidth / 2);
            // }

            int top = -i * MAX_TEXTURE_WIDTH_HEIGHT;
            int quadHeight = (i == quadSize - 1) ? lastColumnHeight() : MAX_TEXTURE_WIDTH_HEIGHT;
            mQuadSegs[i] = new QuadSegment(top, 0, mPcWidth, quadHeight, 0, (i % mMaxColsPerTexContain) * mPcWidth);
        }

    }

    public void initUpdateTex() {
        // Update tex.
        final int texToUpload = Math.min(MAX_ACTIVE_TEX, mTexCount);
        for (int i = 0; i < texToUpload; i++) {
            updateTexIndexToTexId(i, i);
        }

    }

    public int lastColumnHeight() {
        return ((mPcHeight - 1) % MAX_TEXTURE_WIDTH_HEIGHT) + 1;
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

    // readBlock(is, content, MAX_TEXTURE_WIDTH_HEIGHT, j * mPcWidth, Math.min(mPcWidth, MAX_TEXTURE_WIDTH_HEIGHT),
    // readSrcHeight, mPcWidth);
    private void readBlock(InputStream is, byte[] content, int targetStrip, int targetOffset, int readSrcWidth, int readSrcHeight,
            int srcWidth) throws IOException {
        for (int j = 0; j < readSrcHeight; j++) {
            // The picture is bigger than my width.
            // The maximum width the texture could contain is myWidth, so read myWidth, and skip the remaining pixels in the line.
            if (srcWidth > readSrcWidth) {
                // 4 stands for RGBA.
                ByteStreams.readFully(is, content, targetOffset * 4 + targetStrip * j * 4, readSrcWidth * 1 * 4);
                ByteStreams.skipFully(is, (srcWidth - readSrcWidth) * 4);



            } else { // srcWidth == readSrcWidth, always read srcWidtgh >= readSrcWidth.
                // targetOffset * 4 should also *4.
                ByteStreams.readFully(is, content, targetOffset * 4 + targetStrip * j * 4, readSrcWidth * 1 * 4);
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
        String keyImgId = mScrollpicinfo.filePath.MD5 + picIndex;
        MyBitmap bitmapFromMemCache = AppController.getInstance().getBitmapFromMemCache(keyImgId);
        if (bitmapFromMemCache != null)
            // Assigns the OpenGL texture with the Bitmap
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmapFromMemCache.getBitmap(), 0);
    }

    /* [Draw Canvas To Texture] */
    protected short[] mIndices;

    protected void initShapes() {
        if (mQuadSegs == null) {
            Log.w(TAG, "initShapes. [no content.");
            return;
        }

        int quadsSize = mQuadSegs.length;

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
            mQuadTCB.put(mQuadSegs[i].getCoords(MAX_TEXTURE_WIDTH_HEIGHT, MAX_TEXTURE_WIDTH_HEIGHT)); // Add the
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

        final int[] buffers = { 0, 0, 0 };
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

    private int loadShader(int type, String shaderCode)
    {
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

    private final String fragmentShaderCode =
            "precision highp float;  \n"
                    + "uniform sampler2D u_s2dTexture; \n"
                    // +
                    // "uniform vec2 uTexScale; \n"
                    + "varying vec4 v_v4TexCoord; \n"
                    + "void main(){              \n"

                    // + "vec2 mult=vec2((2.0*v_v4TexCoord.x + 1.0)/(2.0*2048.0), (2.0*v_v4TexCoord.y + 1.0)/(2.0*4096.0));\n"
                    // + "vec2 mult=vec2((2.0 * v_v4TexCoord.x + 1.0)/(2.0*1024.0), (2.0 * v_v4TexCoord.y + 1.0) / (2.0 * 1024.0));\n"
                    // + "vec2 mult=vec2((2.0 * v_v4TexCoord.x + 1.0)/(2.0*1024.0), (2.0 * v_v4TexCoord.y + 1.0) / (2.0 * 1024.0));\n"
                    // GOOD + "vec2 mult=vec2((v_v4TexCoord.x/1024.0), (v_v4TexCoord.y /1024.0));\n"

                    // + "vec2 mult=vec2((v_v4TexCoord.x)/2048.0, (v_v4TexCoord.y)/4096.0);\n"

                    // + "gl_FragColor = texture2D(u_s2dTexture, v_v4TexCoord.xy/1024.0);\n"
                    // + "gl_FragColor = texture2D(u_s2dTexture, mult);\n"
                    
                    
//                    + " vec4 mult=(2.0*v_v4TexCoord + 1.0)/(2.0*1024.0);\n"
//                    + " gl_FragColor = texture2D(u_s2dTexture, mult.xy); \n"
                    
                                        + " gl_FragColor = texture2D(u_s2dTexture, v_v4TexCoord.xy); \n"
//                    + " gl_FragColor = texture2D(u_s2dTexture, v_v4TexCoord.xy) + vec4(v_v4TexCoord.y, 0.2, 0.2, 0.0); \n"

                    // Vec4 RGBA. Transparent the extra pixel which is larger than the texture width/height.
                    // + "if (v_v4TexCoord.x > 200.0/2048.0) { gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0); } \n"

                    // " gl_FragColor = texture2D(u_s2dTexture, v_v4TexCoord.xy) + vec4(v_v4TexCoord.y, 0.2, 0.2, 1.0); \n"

                    // " gl_FragColor = texture2D(u_s2dTexture, v_v4TexCoord.xy) + vec4(v_v4TexCoord.x, 0.0, 0.0, 1.0); \n"
                    // // " gl_FragColor = vec4(0.0, 0.2, 1.0, 1.0); \n" +

                    // "vec2 mult=vec2((2.0*v_v4TexCoord.x - 1.0)/(2.0*uTexScale.x), (2.0*v_v4TexCoord.y - 1.0)/(2.0*uTexScale.y)); \n"
                    // +
                    // "gl_FragColor = texture2D(u_s2dTexture, mult.xy); \n"
                    // "gl_FragColor = texture2D(u_s2dTexture, mult.xy) + vec4((2.0*v_v4TexCoord.x - 1.0)/(2.0*uTexScale.x), 0.0, 0.0, 1.0); \n"
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
    private int mMaxColsPerTexContain;

    public void setDimension(int width, int height) {
        mWidth = width;
        // TODO Auto-generated method stub
        mHeight = height;

    }

    public void setPixelPerFrame(float speedByFrame) {
        // moving left.
        mPixelPerFrame = -speedByFrame;
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

}
