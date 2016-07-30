/*
 * This proprietary software may be used only as
 * authorised by a licensing agreement from ARM Limited
 * (C) COPYRIGHT 2013 ARM Limited
 * ALL RIGHTS RESERVED
 * The entire notice above must be reproduced on all authorised
 * copies and copies may only be made to the extent permitted
 * by a licensing agreement from ARM Limited.
 */

package com.color.home.widgets.singleline.pcscroll;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.AppController.MyBitmap;
import com.color.home.ProgramParser.ScrollPicInfo;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.FinishObserver;
import com.color.home.widgets.ItemsAdapter;
import com.color.home.widgets.multilines.StreamResolver;
import com.color.home.widgets.singleline.MovingTextUtils;
import com.color.home.widgets.singleline.QuadGenerator;
import com.color.home.widgets.singleline.QuadSegment;
import com.color.home.widgets.singleline.localscroll.TextRenderer;
import com.google.common.hash.HashCode;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class SLPCTextObject {
    final static String TAG = "SLPCTextObject";
    static final boolean DBG = false;
    private static final boolean DBG_FPS = false;
    private static final boolean DBG_MATRIX = false;

    protected float mPixelPerFrame = -4.0f;
    protected int mCurrentRepeats = 0;
    protected Context mContext;
    private ScrollPicInfo mScrollpicinfo;
    private int[] mTexIds;
    private int mTexCount = 1;

    /* [Draw Canvas To Texture] */
    protected short[] mIndices;
    private int mQuadsCount;
    private int muMVPMatrixHandle;
    protected int muMMatrixHandle;
    private int muTextureHandle;
    /**
     * Texture dimension.
     */
    private int muTexScaleHandle;
    private float[] mMVPMatrix = new float[16];
    protected float[] mMMatrix = new float[16];
    private float[] mVMatrix = new float[16];
    private float[] mProjMatrix = new float[16];
    protected int mProgram;
    protected int maPositionHandle;
    protected int maTexCoordsHandle;
    /**
     * Vertex position attribute buffer.
     */
    protected FloatBuffer mQuadVB;
    /**
     * Vertex texture coordinate buffer.
     */
    protected FloatBuffer mQuadTCB;

    protected int mEvenedWidth;
    protected int mEvenedHeight;
    private int mBackgroundColor;
    protected Paint mPaint = new Paint();
    private float mSpeedByFrame;
    protected int mRepeatCount = 0;
    private FinishObserver mFinishObserver;
    protected QuadSegment[] mQuadSegs;
    protected int mLineHeight;
    protected int mVertexcount;
    protected HashCode mTextBitmapHash;
    
    protected int mPcWidth;
    private int mPcHeight;
    
    protected int mEvenPcHeight;
    protected int mEvenPcWidth;
    private int getEvenPcHeight() {
        return mEvenPcHeight;
    }
    private int getEvenPcWidth() {
        return mEvenPcWidth;
    }

    private int mTexDim = -1;

    private static class SingleLineMeta {
        public int mWidth;
        public int mHeight;

        public SingleLineMeta(int width, int height) {
            mWidth = width;
            mHeight = height;
        }
    }

    // Constructor initialize all necessary members
    public SLPCTextObject(Context context, ScrollPicInfo scrollpicinfo) {
        mContext = context;
        mScrollpicinfo = scrollpicinfo;
    }

    // public void setRelPos(float aX, float aY, float aZ) {
    // setPosition(thePosition.x + aX,
    // thePosition.y + aY,
    // thePosition.z + aZ);
    //
    // // Preventing going too far-from or close-to the screen
    // if (thePosition.z > 0.9f)
    // thePosition.z = 0.9f;
    // if (thePosition.z < -4.0f)
    // thePosition.z = -4.0f;
    // }

    public void update() {
        genTexs();

        // Only one mem cache bitmap currently.
        MyBitmap texFromMemCache = texFromMemCache();
        if (DBG)
            Log.d(TAG,"texFromMemCache = " + texFromMemCache);
        if (texFromMemCache == null)
            prepareTexture();
        else {
            setPcWidth(texFromMemCache.mSingleLineWidth);
            setPcHeight(texFromMemCache.mSingleLineHeight);
            setTexDim(QuadGenerator.findClosestPOT(mPcWidth, getEvenPcHeight()));
        }

        updatePageToTexId(0, 0);

        if (DBG)
            android.util.Log.i(TAG, "bmpSize[" + mPcWidth + ", " + getPcHeight() + "]");

        initShapes();

        setupMVP();

        Matrix.setIdentityM(mMMatrix, 0);
        // Matrix.translateM(mMMatrix, 0, thePosition.x, thePosition.y, thePosition.z);

        // GLES20.glUseProgram(mProgram);

        GLES20.glUniform2f(muTexScaleHandle, (float) mPcWidth, (float) getEvenPcHeight());

        // Apply a ModelView Projection transformation
        // Matrix.setIdentityM(mMMatrix, 0);
        // Matrix.scaleM(mMMatrix, 0, (float)bitmapWidth / (float)bitmapHeight, (float)1.0f, 1.0f);
        // Matrix.translateM(mMMatrix, 0, thePosition.x, thePosition.y, thePosition.z);
        //
        // Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
        // Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

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

    private void setupMVP() {
        if (DBG)
            Log.d(TAG, "setupMVP. [mWidth=" + mEvenedWidth 
                    + ", mHeight=" + mEvenedHeight);
        final float halfWidth = mEvenedWidth / 2.0f;
        final float halfHeight = mEvenedHeight / 2.0f;
        Matrix.orthoM(mMVPMatrix, 0, -halfWidth, halfWidth, -halfHeight, halfHeight, -1, 1);

        // http://www.learnopengles.com/tag/modelview-matrix/
        // Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        // Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        Matrix.setIdentityM(mVMatrix, 0);
        // Move the view (content/coordination origin) up.
        if(DBG)
            Log.d(TAG, "getEvenPcHeight() =  " + getEvenPcHeight() + ", mPcHeight = " + mPcHeight);
        Matrix.translateM(mVMatrix, 0, mEvenedWidth / 2.0f, getEvenPcHeight() / 2.0f , 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, mVMatrix, 0);

        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
    }

    private void genTexs() {

        if (DBG)
            Log.d(TAG, "genTexs. [");

        mTexIds = new int[1];

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        TextRenderer.checkGLError("glActiveTexture");
        GLES20.glGenTextures(mTexIds.length, mTexIds, 0);
        TextRenderer.checkGLError("glGenTextures");
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(muTextureHandle, 0);
        TextRenderer.checkGLError("glUniform1i");

        // As byproduct, the 1 text is binded.
        for (int i = 0; i < mTexIds.length; i++) {
            initTexParam(i);
        }
    }

    private void initTexParam(int texId) {
        if (DBG) Log.d(TAG, "initTexParam, texId = " + texId);
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

    private void updatePageToTexId(int pageTexIndex, int texId) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexIds[texId]);
        TextRenderer.checkGLError("glBindTexture");
        updateImage(pageTexIndex);
    }

    private void updateImage(int picIndex) {
        String keyImgId = getKeyImgId(picIndex);
        MyBitmap bitmapFromMemCache = AppController.getInstance().getBitmapFromMemCache(keyImgId);
        if (bitmapFromMemCache != null)
            // Assigns the OpenGL texture with the Bitmap
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmapFromMemCache.getBitmap(), 0);
    }

    /**
     * 1. Setup pcWidth and pcHeight.
     * 2. Setup the texture dimension to some POT dim.
     * 3. addBitmapToMemoryCache(getKeyImgId(0)
     */
    protected void prepareTexture() {
        final byte[] head = new byte[8];

        StreamResolver streamResolver = null;
        final String absFilePath = ItemsAdapter.getAbsFilePathByFileSource(mScrollpicinfo.filePath);
        if (DBG)
            Log.d(TAG,"absFilePath = " + absFilePath);
        try {
            streamResolver = new StreamResolver(absFilePath).resolve();
            InputStream readFromIs = streamResolver.getReadFromIs();
            if (readFromIs == null) {
                Log.e(TAG, "Bad file.absFilePath=" + absFilePath);
                return;
            }

            if (DBG) Log.d(TAG, "skip fully.");
            ByteStreams.skipFully(readFromIs, 20);
            ByteStreams.readFully(readFromIs, head, 0, 8);
            
            // we skipped 20 read 8 = 28. Here is the content.
            ByteStreams.skipFully(readFromIs, 1024 - 28);

            ByteBuffer bb = ByteBuffer.wrap(head);
            bb.order(ByteOrder.LITTLE_ENDIAN); // if you want little-endian
            
            if (DBG)
                Log.i(TAG, "drawCanvasToTexture. [position=" + bb.position());
            setPcWidth(bb.getInt());
            setPcHeight(bb.getInt());
            // In case the pc width or height is set too late.
            setTexDim(QuadGenerator.findClosestPOT(mPcWidth, getEvenPcHeight()));

            // Always square.
            byte[] content = new byte[getTexDim() * getTexDim() * 4];
            if (DBG)
                Log.i(TAG, "getTexDim = " + getTexDim());

            int readHeight = getPcHeight();
            // There could be empty heights.
            if (mPcWidth >= getPcHeight()) {
                for (int i = 0; i < readHeight; i++) {
                    // + 1 because, / removes the tail.
                    // There could be an empty move, if the '%' is 0. So exclude the empty with the 'readSize' check.
                    for (int j = 0; j < mPcWidth / getTexDim() + 1; j++) {
                        int readSize = Math.min(mPcWidth - j * getTexDim(), getTexDim());
                        if (readSize != 0) {
                            // 4 stands for RGBA.
                            // i * texWidth is offset into the block (texWidth * pcHeight)
                            // (texWidth * pcHeight) * j is which block.
                            ByteStreams.readFully(readFromIs, content, i * getTexDim() * 4 + (getTexDim() * getEvenPcHeight()) * j * 4, readSize * 1 * 4);
                        }
                    }
                }
            } else {
                if (DBG)
                    Log.d(TAG, "checkSinglePic. [mPcWidth < mPcHeight, only one line in my texture.");
                for (int i = 0; i < readHeight; i++) {
                    ByteStreams.readFully(readFromIs, content, i * getTexDim() * 4, mPcWidth * 1 * 4);
                }
            }

            GraphUtils.convertRGBFromPC(content);

            final Bitmap bm = Bitmap.createBitmap(getTexDim(), getTexDim(), Bitmap.Config.ARGB_8888);
            bm.copyPixelsFromBuffer(ByteBuffer.wrap(content, 0, getTexDim() * getTexDim() * 4));
            AppController.getInstance().addBitmapToMemoryCache(getKeyImgId(0), new MyBitmap(bm, mPcWidth, getPcHeight()));

            if (DBG) {
                new File("/mnt/sdcard/mul").mkdir();
                QuadGenerator.toPng(bm, new File("/mnt/sdcard/mul/" + getKeyImgId(0) + ".png"));
            }

        } catch (Exception e) {
            Log.e(TAG, "checkSinglePic. [exception:", e);
        } finally {
            if (streamResolver != null) {
                streamResolver.close();
            }
        }
    }

    public MyBitmap texFromMemCache() {
        return AppController.getInstance().getBitmapFromMemCache(getKeyImgId(0));
    }

    protected String getKeyImgId(int picIndex) {
        return mScrollpicinfo.filePath.MD5 + picIndex;
    }

    // Load shaders, create vertices, texture coordinates etc.
    public void init() {
        // R G B A.
        // GLES20.glClearColor(1.0f, 0.5f, 0.5f, 1.0f);
        // mBackgroundColor
        final float red = Color.red(mBackgroundColor) / 255.0f;
        final float green = Color.green(mBackgroundColor) / 255.0f;
        final float blue = Color.blue(mBackgroundColor) / 255.0f;
        final float alpha = Color.alpha(mBackgroundColor) / 255.0f;
        if (DBG)
            Log.d(TAG, "init. [r=" + red + ", g=" + green + ", b=" + blue + ", alpha=" + alpha);
        GLES20.glClearColor(red, green, blue, alpha);

        // Initialize the triangle vertex array
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        TextRenderer.checkGLError("loadShaders");

        mProgram = GLES20.glCreateProgram(); // Create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader); // Add the vertex shader to program
        TextRenderer.checkGLError("glAttachShader:vert");
        GLES20.glAttachShader(mProgram, fragmentShader); // Add the fragment shader to program
        TextRenderer.checkGLError("glAttachShader:frag");
        GLES20.glLinkProgram(mProgram); // Creates OpenGL program executables
        TextRenderer.checkGLError("glLinkProgram");
        GLES20.glUseProgram(mProgram);
        TextRenderer.checkGLError("glUseProgram");

        // Get handle to the vertex shader's vPosition member
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        TextRenderer.checkGLError("glGetAttribLocation:vPosition");
        // Get handle to the vertex shader's vPosition member
        maTexCoordsHandle = GLES20.glGetAttribLocation(mProgram, "vTexCoord");
        TextRenderer.checkGLError("glGetAttribLocation:vTexCoord");
        // get handle to uniform parameter
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        TextRenderer.checkGLError("glGetUniformLocation:uMVPMatrix");

        // get handle to uniform parameter
        muMMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMMatrix");
        TextRenderer.checkGLError("glGetUniformLocation:uMMatrix");

        muTextureHandle = GLES20.glGetUniformLocation(mProgram, "u_s2dTexture");
        TextRenderer.checkGLError("glGetUniformLocation:u_s2dTexture");
        muTexScaleHandle = GLES20.glGetUniformLocation(mProgram, "uTexScale");
        TextRenderer.checkGLError("glGetUniformLocation:muTexScaleHandle");
    }


    private float pixelTemp = 0.0f;
    public boolean mIsGreaterThanAPixelPerFrame = false;
    public void render() {
//        if(DBG)
//            Log.d(TAG, "singleline render");
        // // Add program to OpenGL environment
//        if(DBG)
//            Log.d(TAG, "pixelperframe: " + mPixelPerFrame);
////        Matrix.translateM(mMMatrix, 0, -1.5f, 0.f, 0.f);
//
//        if(Math.abs(mPixelPerFrame) > 1.0f){
//            mPixelPerFrame = Math.round(mPixelPerFrame);
//        }
//        if(DBG)
//            Log.d(TAG, "pixelPerFrame :" + mPixelPerFrame);
//
//        if(DBG)
//            Log.d(TAG, "pixelTemp:" + pixelTemp);
//
//        pixelTemp += mPixelPerFrame;
//
//        if(pixelTemp <= -1.0f) {
//            Matrix.translateM(mMMatrix, 0, (int)pixelTemp, 0.f, 0.f);
//            pixelTemp += Math.abs((int)pixelTemp);
//        }
        if (mIsGreaterThanAPixelPerFrame)
            Matrix.translateM(mMMatrix, 0, mPixelPerFrame, 0.f, 0.f);
        else {
            pixelTemp += mPixelPerFrame;
            if(pixelTemp <= -1.0f) {
                Matrix.translateM(mMMatrix, 0, -1.0f, 0.f, 0.f);
                pixelTemp += 1;
            }
        }

        if(DBG_MATRIX)
            Log.d(TAG, "mMMatrix [12]  = " + mMMatrix[12]);

        if (mMMatrix[12] < -mEvenedWidth - mPcWidth) {
            Matrix.setIdentityM(mMMatrix, 0);
            // if repeat count == 0, infinite loop.
            if (mRepeatCount != 0) {
                if (++mCurrentRepeats >= mRepeatCount) {
                    notifyFinish();
                }
            }
        }

        GLES20.glUniformMatrix4fv(muMMatrixHandle, 1, false, mMMatrix, 0);
        // GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        // if (DBG)
        // Log.d(TAG, "render. [mIndices.length=" + mIndices.length);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, mVertexcount, GLES20.GL_UNSIGNED_SHORT, 0);
    }

    protected void notifyFinish() {
        if (DBG)
            Log.d(TAG, "notifyFinish. [mSingleLineTextSurfaceView=" + mFinishObserver);
        if (mFinishObserver != null)
            mFinishObserver.notifyPlayFinished();
    }

    public boolean isLastQuad(int quadIndex) {
        return quadIndex == mQuadsCount - 1;
    }

    protected void initShapes() {
        genQuadSegs();

        int quadsSize = mQuadSegs.length;

        // Initialize vertex Buffer for triangle
        final ByteBuffer vbb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                // 3 is the x, y, z.
                // first '2' is how many vertex for a new quad.
                // the second 2 is the basic 2 vertex to fullfill quads serial.
                // (quadsSize * 2 + 2) * 3 * 4);
                (quadsSize * 4) * 3 * 4);
        vbb.order(ByteOrder.nativeOrder()); // Use the device hardware's native byte order
        mQuadVB = vbb.asFloatBuffer(); // Create a floating point buffer from the ByteBuffer
        // mQuadVB.put(quadVerts); // Add the coordinates to the FloatBuffer
        for (int i = 0; i < quadsSize; i++) {
            // The first quad must specify the left column.
            // if (i == 0) {
            if (DBG)
                Log.d(TAG, "initShapes. [mQuadSegs[0].getQuadPos() length=" + mQuadSegs[0].getQuadPos().length
                        + ", quadsSize=" + quadsSize);
            mQuadVB.put(mQuadSegs[i].getQuadPos()); // Add the coordinates to the FloatBuffer
            // } else {
            // // 3 is the x, y, z.
            // // 2 is the coordinates to skip.
            // mQuadVB.put(mQuadSegs[i].getQuadPos(), 3 * 2, 3 * 2); // Add the coordinates to the FloatBuffer
            // }
            // mQuadVB.put(mQuadSegs[i].getQuadPos()); // Add the coordinates to the FloatBuffer
        }
        mQuadVB.position(0); // Set the buffer to read the first coordinate

        final ByteBuffer vbb_t = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                (quadsSize * 4) * 3 * 4);
        // (quadsSize * 2 + 2) * 3 * 4);
        vbb_t.order(ByteOrder.nativeOrder()); // Use the device hardware's native byte order
        mQuadTCB = vbb_t.asFloatBuffer(); // Create a floating point buffer from the ByteBuffer

        for (int i = 0; i < quadsSize; i++) {
            // mQuadTCB.put(mQuadSegs[i].getCoords(mTexWidth, mTexHeight)); // Add the coordinates to the FloatBuffer

            // The first quad must specify the left column.
            // if (i == 0) {
            // if (DBG)
            // Log.d(TAG, "initShapes. [mQuadSegs[0].getQuadPos() length=" + mQuadSegs[0].getQuadPos().length
            // + ", quadsSize=" + quadsSize);
            mQuadTCB.put(mQuadSegs[i].getCoords(getTexDim(), getTexDim())); // Add the coordinates to the FloatBuffer
            // } else {
            // // 3 is the x, y, z.
            // // 2 is the coordinates to skip.
            // mQuadTCB.put(mQuadSegs[i].getCoords(mTexWidth, mTexHeight), 3 * 2, 3 * 2); // Add the coordinates to the FloatBuffer
            // }
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

    protected void genQuadSegs() {
        final int widthRemaining = mPcWidth % getTexDim();
        mQuadsCount = mPcWidth / getTexDim() + (widthRemaining == 0 ? 0 : 1);
        final int lastQuadWidth = (widthRemaining == 0 ? getTexDim() : widthRemaining);

        mQuadSegs = new QuadSegment[mQuadsCount];
        int top = 0;
        int left = 0;
        for (int i = 0; i < mQuadsCount; i++) {
            int texTop, texLeft;
            texTop = i * getEvenPcHeight();
            texLeft = 0;

            // The last quad.
            if (isLastQuad(i)) {
                // GOOD. mQuadSegs[i] = new QuadSegment(1024, 2048);
                mQuadSegs[i] = new QuadSegment(top, left, lastQuadWidth, getEvenPcHeight(), texTop, texLeft);
            } else {
                mQuadSegs[i] = new QuadSegment(top, left, getTexDim(), getEvenPcHeight(), texTop, texLeft);
            }
            left += getTexDim();
        }
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

    private final String vertexShaderCode =
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
            "precision mediump float;  \n"
                    +
                    "uniform sampler2D u_s2dTexture; \n"
                    +
                    "uniform vec2 uTexScale; \n"
                    +
                    "varying vec4 v_v4TexCoord; \n"
                    +
                    "void main(){              \n"
                    +
                    // // " gl_FragColor = texture2D(u_s2dTexture, v_v4TexCoord.xy) + vec4(0.3, 0.2, 0.2, 1.0); \n" +
                    " gl_FragColor = texture2D(u_s2dTexture, v_v4TexCoord.xy); \n"
                    // " gl_FragColor = texture2D(u_s2dTexture, v_v4TexCoord.xy) + vec4(v_v4TexCoord.x, 0.0, 0.0, 1.0); \n"
                    // // " gl_FragColor = vec4(0.0, 0.2, 1.0, 1.0); \n" +

                    // "vec2 mult=vec2((2.0*v_v4TexCoord.x - 1.0)/(2.0*uTexScale.x), (2.0*v_v4TexCoord.y - 1.0)/(2.0*uTexScale.y)); \n"
                    // +
                    // "gl_FragColor = texture2D(u_s2dTexture, mult.xy); \n"
                    // "gl_FragColor = texture2D(u_s2dTexture, mult.xy) + vec4((2.0*v_v4TexCoord.x - 1.0)/(2.0*uTexScale.x), 0.0, 0.0, 1.0); \n"
                    +

                    "}                         \n";

    public void setDimension(int width, int height) {
        if (DBG)
            Log.d(TAG, "setDimension. [width=" + width
                    + ", height=" + height);
        
        mEvenedWidth = MovingTextUtils.evenIt(width);
        mEvenedHeight = MovingTextUtils.evenIt(height);
//        mWidth = width;
//        mHeight = height;

    }

    public void setBackgroundColor(int parseColor) {
        mBackgroundColor = parseColor;
    }

    public Paint getPaint() {
        return mPaint;
    }

    public void setPixelPerFrame(float speedByFrame) {
        // moving left.
//        mPixelPerFrame = -speedByFrame;
        if(speedByFrame >= 1.0f) {
            mPixelPerFrame = Math.round(-speedByFrame);
            mIsGreaterThanAPixelPerFrame = true;
        }else {
            mPixelPerFrame = -speedByFrame;
            mIsGreaterThanAPixelPerFrame = false;
        }
    }

    public void setRepeatCount(int repeatCount) {
        mRepeatCount = repeatCount;
    }

    public void setView(FinishObserver singleLineTextSurfaceView) {
        mFinishObserver = singleLineTextSurfaceView;
    }

    public void setStyle(int style) {
        getPaint().setTypeface(Typeface.defaultFromStyle(style));
    }

    public void setTypeface(String fontName, int style) {
        if (DBG)
            Log.d(TAG, "setFont. [fontName=" + fontName);

        // getPaint().setTypeface(AppController.getInstance().getTypeface(fontName));
        setTypeface(AppController.getInstance().getTypeface(fontName), style);
    }

    public void setTypeface(Typeface tf, int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }

            setTypeface(tf);
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            mPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
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
        // TODO Auto-generated method stub

    }

    protected int getTexDim() {
        if (mTexDim < 0)
            mTexDim = QuadGenerator.findClosestPOT(mPcWidth, getEvenPcHeight());

        return mTexDim;
    }

    protected void setTexDim(int texDim) {
        mTexDim = texDim;
    }

    protected int getPcHeight() {
        return mPcHeight;
    }

    protected void setPcHeight(int pcHeight) {
        mPcHeight = pcHeight;
        mEvenPcHeight = MovingTextUtils.evenIt(pcHeight);
    }

    protected void setPcWidth(int pcWidth) {
        mPcWidth = pcWidth;
        mEvenPcWidth = MovingTextUtils.evenIt(pcWidth);
    }

}
