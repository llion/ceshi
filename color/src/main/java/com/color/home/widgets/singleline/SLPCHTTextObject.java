/*
 * This proprietary software may be used only as
 * authorised by a licensing agreement from ARM Limited
 * (C) COPYRIGHT 2013 ARM Limited
 * ALL RIGHTS RESERVED
 * The entire notice above must be reproduced on all authorised
 * copies and copies may only be made to the extent permitted
 * by a licensing agreement from ARM Limited.
 */

package com.color.home.widgets.singleline;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.AppController.MyBitmap;
import com.color.home.ProgramParser.ScrollPicInfo;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.ItemsAdapter;
import com.color.home.widgets.multilines.MultiPicScrollObject;
import com.color.home.widgets.multilines.StreamResolver;
import com.color.home.widgets.singleline.localscroll.TextRenderer;
import com.google.common.hash.HashCode;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 
 * Head Tail marquee single line view.
 * 
 * @author zzjd7382
 *
 */
public class SLPCHTTextObject {
    // private static final String MNT_SDCARD_PNGS = "/mnt/sdcard/pngs";
    private static final int MAX_TEXTURE_WIDTH_HEIGHT = 4096;
    private final static String TAG = "SLPCHTTextObject";
    private static final boolean DBG = false;
    private static final boolean DBG_FPS = false;
    private static final boolean DBG_MATRIX = false;

    protected float mPixelPerFrame = -4.0f;
    protected int mCurrentRepeats = 0;
    private Context mContext;
    private ScrollPicInfo mScrollpicinfo;
    private int[] mTexIds;
    private final int mTexCount = 1;

    private int muMVPMatrixHandle;
    protected int muMMatrixHandle;
    private int muTextureHandle;
    /**
     * Texture dimension.
     */
//    private int muTexScaleHandle;
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

    protected int mWidth;
    protected int mHeight;
    private int mBackgroundColor;
    protected Paint mPaint = new Paint();
    private float mSpeedByFrame;
    protected int mRepeatCount = 0;
    private SLPCHTSurfaceView mTextSurfaceView;
    protected QuadSegment[] mQuadSegs;
    protected int mLineHeight;
    protected int mVertexcount;
    protected HashCode mTextBitmapHash;
    private int mPcWidth;
    private int mPcHeight;
    private int mEvenPcHeight;
    private int mEvenPcWidth;
    
    private int mTexWidth;
    private int mTexHeight;
    private int mColor;

    protected short[] mIndices;

    // Constructor initialize all necessary members
    public SLPCHTTextObject(Context context, ScrollPicInfo scrollpicinfo) {
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

    void update() throws Exception {
        // Now the pic count is ready.
        genTexs();
        
        if (!normalizTexToMemCache())
            return;
        updatePageToTexId(0, 0);

        if (DBG)
            android.util.Log.i(TAG, "bmpSize[" + mPcWidth + ", " + mPcHeight + "]");

        initShapes();

        setupMVP();

        Matrix.setIdentityM(mMMatrix, 0);
        // Matrix.translateM(mMMatrix, 0, thePosition.x, thePosition.y, thePosition.z);

        // GLES20.glUseProgram(mProgram);

//        GLES20.glUniform2f(muTexScaleHandle, (float) mPcWidth, (float) mPcHeight);

        // theAnimRotZ += 0.5f;

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

    private void setupMVP() {
        final float halfWidth = mWidth / 2.0f;
        final float halfHeight = mHeight / 2.0f;
        Matrix.orthoM(mMVPMatrix, 0, -halfWidth, halfWidth, -halfHeight, halfHeight, -1, 1);

        // http://www.learnopengles.com/tag/modelview-matrix/
        // Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        // Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        Matrix.setIdentityM(mVMatrix, 0);
        // Move the view (content/coordination origin) up.
        if(DBG)
            Log.d(TAG, "Even height : " + getEvenPcHeight());
        Matrix.translateM(mVMatrix, 0, mWidth / 2.0f, getEvenPcHeight() / 2.0f, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, mVMatrix, 0);

        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
    }

    private int getEvenPcHeight() {
        return mEvenPcHeight;
    }

    private int getEvenPcWidth() {
        return mEvenPcWidth;
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
        String keyImgId = getBitmapKeyImgId(picIndex);
        MyBitmap bitmapFromMemCache = AppController.getInstance().getBitmapFromMemCache(keyImgId);
        if (bitmapFromMemCache != null)
            // Assigns the OpenGL texture with the Bitmap
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmapFromMemCache.getBitmap(), 0);
    }

    protected String getBitmapKeyImgId(int picIndex) {
        return mScrollpicinfo.filePath.MD5 + picIndex;
    }

    protected boolean normalizTexToMemCache() throws Exception {
        final byte[] head = new byte[8];

        StreamResolver streamResolver = null;
        final String absFilePath = ItemsAdapter.getAbsFilePathByFileSource(mScrollpicinfo.filePath);
        try {
            streamResolver = new StreamResolver(absFilePath).resolve();
            InputStream readFromIs = streamResolver.getReadFromIs();
            if (readFromIs == null) {
                Log.e(TAG, "Bad file.absFilePath=" + absFilePath);
                return false;
            }

            if (DBG) Log.d(TAG, "skip fully.");
            ByteStreams.skipFully(readFromIs, 20);
            ByteStreams.readFully(readFromIs, head, 0, 8);

            ByteBuffer bb = ByteBuffer.wrap(head);
            bb.order(ByteOrder.LITTLE_ENDIAN); // if you want little-endian

            if (DBG)
                Log.i(TAG, "normalizTexToMemCache. [position=" + bb.position());

            mPcWidth = bb.getInt();
            mPcHeight = bb.getInt();
            if (mPcWidth <= 0 || mPcHeight <= 0)
                return false;
            setNormarizedEvenPcHeight(MovingTextUtils.evenIt(mPcHeight));
            setNormarizedEvenPcWidth(MovingTextUtils.evenIt(mPcWidth));

            final int closePOT = QuadGenerator.findClosestPOT(getEvenPcWidth(), getEvenPcHeight());
            if (DBG)
                Log.d(TAG, "normalizTexToMemCache. [pcWidth=" + mPcWidth + ", pcHeight=" + mPcHeight + ", closePOT=" + closePOT
                        + ", square=" + closePOT * closePOT
                        + ", mEvenPcHeight=" + getEvenPcHeight()
                        + ", event Pc width=" + MovingTextUtils.evenIt(mPcWidth)
                );


            // we skipped 20 read 8 = 28.
            ByteStreams.skipFully(readFromIs, 1024 - 28);
            // byte[] converted = new byte[mWidth * mHeight * 4];

            mTexHeight = mTexWidth = closePOT;
            // mTexCount is 1, for now.
            for (int picIndex = 0; picIndex < mTexCount; picIndex++) {
                String keyImgId = getBitmapKeyImgId(picIndex);

                // Image already exist, offset to next image.
                if (AppController.getInstance().getBitmapFromMemCache(keyImgId) != null) {
                    if (DBG)
                        Log.d(TAG, "normalizTexToMemCache. [getBitmapFromMemCache exist for =" + keyImgId);
                    if (picIndex == mTexCount - 1) {
                        // TODO: to test, we have to create a program with a large paragraph.
                        if (DBG)
                            Log.d(TAG, "normalizTexToMemCache. [do not skip full size in the file the last texture, as it overflows.");
                        continue;
                    }
                    ByteStreams.skipFully(readFromIs, mTexWidth * mTexHeight * 4);
                    continue;
                }

                // Always square.
                Bitmap bm = Bitmap.createBitmap(mTexWidth, mTexHeight, Bitmap.Config.ARGB_8888);
//                byte[] content = new byte[mTexWidth * mTexHeight * 4];
//                byte[] content;
                // IT'S presumed, the single line picture's width > height.
//                int readHeight = mPcHeight;
                // There could be empty heights.
                if (mPcWidth >= mPcHeight) {
                    if (DBG)
                        Log.d(TAG, "checkSinglePic. [mPcWidth > mPcHeight.");
                    MultiPicScrollObject.readFatTextPic(readFromIs, bm, mPcWidth, mPcHeight, mTexWidth);
//                    content = new byte[Math.min(mPcWidth, mTexWidth) * 4];
//                    for (int i = 0; i < readHeight; i++) {
//                        // + 1 because, / removes the tail.
//                        // There could be an empty move, if the '%' is 0. So exclude the empty.
//                        for (int j = 0; j < mPcWidth / mTexWidth + 1; j++) {
//                            int readSize = Math.min(mPcWidth - j * mTexWidth, mTexWidth);
//                            if (readSize != 0) {
//                                // 4 stands for RGBA.
//                                // i * texWidth is offset into the block (texWidth * pcHeight)
//                                // (texWidth * pcHeight) * j is which block.
////                                ByteStreams.readFully(readFromIs, content, i * mTexWidth * 4 + (mTexWidth * getEvenPcHeight()) * j * 4, readSize * 1 * 4);
//                                ByteStreams.readFully(readFromIs, content, 0, readSize * 4);
//                                bm.setPixels(MultiPicScrollObject.byteArray2intArray(content), 0, mTexWidth, 0, i + j * mPcHeight, readSize, 1);
//                            }
//                            // else {
//                            // is.read(content, i * texWidth * 4 + (texWidth * pcHeight) * j * 4, (pcWidth - j * texWidth) * 1 * 4);
//                            // }
//
//                        }
//                        // Skip what ???? Do not skip the pixels in the file!!!
//                        // It's shorter in width already.
//                        // is.skip((myWidth - mBitmapWidth) * 4);
//                    }
                } else {
                    if (DBG)
                        Log.d(TAG, "checkSinglePic. [mPcWidth < mPcHeight, only one line in my texture.");
                    MultiPicScrollObject.readTallTextPic(readFromIs, bm, mPcWidth, mPcHeight, mTexWidth);
//                    content = new byte[mPcWidth * 4];
//                    readHeight = Math.min(mPcHeight, mTexWidth);
//                    for (int i = 0; i < readHeight; i++) {
//                        ByteStreams.readFully(readFromIs, content, i * mTexWidth * 4, mPcWidth * 1 * 4);
//                    }
                }

                // mBitmapWidth = myWidth;
                // mBitmapHeight = myHeight;
                // texHeight = myHeight;
                // mLastQuadHeight = myHeight;

                // byte[] content = new byte[mBitmapWidth * texHeight * 4];
                // // 0 in the content.
                // int read = is.read(content, 0, mBitmapWidth * texHeight * 4);

                // if (DBG)
                // Log.i(TAG, "drawCanvasToTexture. [read size=" + read);

//                GraphUtils.convertRGBFromPC(content);
                // Now put these nice RGBA pixels into a Bitmap object

                // bm.setPremultiplied(false);
//                bm.copyPixelsFromBuffer(ByteBuffer.wrap(content, 0, mTexWidth * mTexHeight * 4));
                AppController.getInstance().addBitmapToMemoryCache(keyImgId, new MyBitmap(bm, mPcWidth, mPcHeight));

                if (DBG) {
                    new File("/mnt/sdcard/mul").mkdir();
                    QuadGenerator.toPng(bm, new File("/mnt/sdcard/mul/" + keyImgId + ".png"));
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "checkSinglePic. [exception:", e);
            return false;
        } finally {
            if (streamResolver != null) {
                streamResolver.close();
            }
        }
        return true;
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

//        muTexScaleHandle = GLES20.glGetUniformLocation(mProgram, "uTexScale");
//        TextRenderer.checkGLError("glGetUniformLocation:muTexScaleHandle");
    }

    private float pixelTemp = 0.0f;


    private long mLastRealtime = 0L;

    private boolean mIsGreaterThanAPixelPerFrame = false;

    public void render() {
        //if (false) {


//            if(mPixelPerFrame < -1.0f){
//                mPixelPerFrame = Math.round(mPixelPerFrame);
//            }
//            pixelTemp += mPixelPerFrame;
//
//            if(pixelTemp <= -1.0f) {
//                Matrix.translateM(mMMatrix, 0, (int)pixelTemp, 0.f, 0.f);
//                pixelTemp += Math.abs((int)pixelTemp);
//            }
        //}
        if (mIsGreaterThanAPixelPerFrame)
            Matrix.translateM(mMMatrix, 0, mPixelPerFrame, 0.f, 0.f);
        else {
            pixelTemp += mPixelPerFrame;
            if(pixelTemp <= -1.0f) {
                Matrix.translateM(mMMatrix, 0, -1.0f, 0.f, 0.f);
                pixelTemp += 1;
            }
        }

        if(DBG_MATRIX) {
            Log.d(TAG, "matrix[12] = " + mMMatrix[12]);
            Log.d(TAG, "pixelTemp = " + pixelTemp);
        }

        if (DBG_FPS) {

            long now = SystemClock.elapsedRealtimeNanos();
            Log.d(TAG, "Interval=" + (now - mLastRealtime) + ", pos=" + mMMatrix[12]);
            mLastRealtime = now;
        }

        // Matrix.translateM(mMMatrix, 0, -1.0f, 0.f, 0.f);

//        Matrix.translateM(mMMatrix, 0, mPixelPerFrame, 0.f, 0.f);
        // 09-08 23:04:05.580: D/TextObject(6052): render. [fl=639.0, i=12
        float overflow = mMMatrix[12] - (-mWidth - mPcWidth);
        if (overflow < 0) {
            // if (isFirstRun)
            // isFirstRun = false;

            Matrix.setIdentityM(mMMatrix, 0);
            // To the left edge.
            Matrix.translateM(mMMatrix, 0, -mWidth + overflow, 0.f, 0.f);
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
            Log.d(TAG, "notifyFinish. [mSingleLineTextSurfaceView=" + mTextSurfaceView);
        if (mTextSurfaceView != null)
            mTextSurfaceView.notifyPlayFinished();
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
            mQuadTCB.put(mQuadSegs[i].getCoords(mTexWidth, mTexHeight)); // Add the coordinates to the FloatBuffer
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
        QuadGenerator qg = new QuadGenerator(mPcWidth, mPcHeight, mTexWidth, mWidth);
        final int repeatedQuadsSize = qg.getRepeatedQuadsSize();
        mQuadSegs = new QuadSegment[repeatedQuadsSize];
        for (int i = 0; i < repeatedQuadsSize; i++) {
            mQuadSegs[i] = qg.getQuad(i);
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
//                    "uniform vec2 uTexScale; \n"
//                    +
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
        mWidth = width;
        // TODO Auto-generated method stub
        mHeight = height;

    }

    public void setTextColor(int parseColor) {
        mColor = parseColor;
        // TODO Auto-generated method stub
    }

    public void setBackgroundColor(int parseColor) {
        mBackgroundColor = parseColor;
    }

    public Paint getPaint() {
        return mPaint;
    }

    public void setPixelPerFrame(float speedByFrame) {
        if (DBG)
            Log.d(TAG, "setPixelPerFrame. [speedByFrame=" + speedByFrame);
        // moving left.
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

    public void setView(SLPCHTSurfaceView singleLineTextSurfaceView) {
        mTextSurfaceView = singleLineTextSurfaceView;
    }

    public void setTextItemBitmapHash(HashCode textBitmapHash) {
        mTextBitmapHash = textBitmapHash;
        // TODO Auto-generated method stub

    }

    private void setNormarizedEvenPcHeight(int evenPcHeight) {
        mEvenPcHeight = evenPcHeight;
    }

    private void setNormarizedEvenPcWidth(int evenPcWidth) {
        mEvenPcWidth = evenPcWidth;
    }
}
