package com.color.home.widgets.singleline_scroll;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.ProgramParser.Item;
import com.color.home.Texts;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.FinishObserver;
import com.color.home.widgets.multilines.MultiPicScrollObject;
import com.color.home.widgets.singleline.MovingTextUtils;
import com.color.home.widgets.singleline.QuadGenerator;
import com.color.home.widgets.singleline.QuadSegment;
import com.google.common.hash.HashCode;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * Created by Administrator on 2016/11/11.
 */
public class SinglineScrollObject {
    private final static String TAG = "SinglineScrollObject";
    private static final boolean DBG = false;
    private static final int MAX_DRAW_TEXT_WIDTH = 33000;
    protected boolean DBG_PNG = false;
    private static final boolean DBG_MATRIX = false;
    protected boolean DBG_READ = false;

    private ArrayList<Item> mItems;
    protected int mBeginXinTexture = 0;
    protected int mBeginYinTexture = 0;

    protected float mPixelPerFrame = -4.0f;
    protected int mCurrentRepeats = 0;
    protected Context mContext;
    protected int[] mTexIds;

    /* [Draw Canvas To Texture] */
    protected short[] mIndices;
    private int mQuadsCount;
    private int muMVPMatrixHandle;
    protected int muMMatrixHandle;
    protected int muTextureHandle;
    /**
     * Texture dimension.
     */
    protected int muTexScaleHandle;
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
    private float mSpeedByFrame;
    protected int mRepeatCount = 0;
    private FinishObserver mFinishObserver;
    protected QuadSegment[] mQuadSegs;
    //    protected int mLineHeight;
    protected int mVertexcount;
    protected HashCode mTextBitmapHash;

    protected int mPcWidth;
    protected int mPcHeight;
    protected int mRealReadPcWidth;

    protected int mEvenPcHeight;
    protected int mEvenPcWidth;
    protected int mTextSize;
    protected String mKey = "";
    protected int mTexDim = -1;


    private int getEvenPcHeight() {
        return mEvenPcHeight;
    }

    private int getEvenPcWidth() {
        return mEvenPcWidth;
    }

    public SinglineScrollObject(Context context, ArrayList<Item> items) {
        mContext = context;
        mItems = items;

    }

    public SinglineScrollObject(Context context) {
        mContext = context;
    }

    public boolean update() {
        genTexs();

        // Only one mem cache bitmap currently.
        synchronized (AppController.sLock) {
            AppController.MyBitmap texFromMemCache = texFromMemCache();
            if (DBG)
                Log.d(TAG, "texFromMemCache = " + texFromMemCache);
            if (texFromMemCache == null) {
                if (!prepareTexture()) {
                    return false;
                }
            } else {
                setPcWidth(texFromMemCache.mSingleLineWidth);
                setPcHeight(texFromMemCache.mSingleLineHeight);
                setTexDim(QuadGenerator.findClosestPOT(mPcWidth, getPcHeight()));

                if (!MultiPicScrollObject.isMemoryEnough(getTexDim()))
                    return false;

            }

            updatePageToTexId(0, 0);


            if (DBG)
                android.util.Log.i(TAG, "bmpSize[" + mPcWidth + ", " + getPcHeight() + "]");

            mRealReadPcWidth = getRealReadPcWidth(mPcWidth, mPcHeight, mTexDim);
            initShapes();

            setupMVP();

            Matrix.setIdentityM(mMMatrix, 0);
            // Matrix.translateM(mMMatrix, 0, thePosition.x, thePosition.y, thePosition.z);

            // GLES20.glUseProgram(mProgram);

            GLES20.glUniform2f(muTexScaleHandle, (float) mPcWidth, (float) getPcHeight());

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

            return true;
        }
    }

    protected void setupMVP() {
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
        if (DBG)
            Log.d(TAG, "getEvenPcHeight() =  " + getPcHeight() + ", mPcHeight = " + mPcHeight);
        Matrix.translateM(mVMatrix, 0, mEvenedWidth / 2.0f, getPcHeight() / 2.0f, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, mVMatrix, 0);

        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
    }

    protected void genTexs() {

        if (DBG)
            Log.d(TAG, "genTexs. [");

        mTexIds = new int[1];

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

    protected void initTexParam(int texId) {
        if (DBG) Log.d(TAG, "initTexParam, texId = " + texId);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexIds[texId]);
        SinglelineScrollRenderer.checkGLError("glBindTexture");
        // Setup texture parameters
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        SinglelineScrollRenderer.checkGLError("glTexParameterf:GL_TEXTURE_MAG_FILTER");
        // GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        SinglelineScrollRenderer.checkGLError("glTexParameterf:GL_TEXTURE_MIN_FILTER");

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        SinglelineScrollRenderer.checkGLError("glTexParameterf:GL_TEXTURE_WRAP_S");
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        SinglelineScrollRenderer.checkGLError("glTexParameterf:GL_TEXTURE_WRAP_T");
    }

    protected void updatePageToTexId(int pageTexIndex, int texId) {
        if (DBG)
            Log.d(TAG, "updatePageToTexId. texId= " + texId);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexIds[texId]);
        SinglelineScrollRenderer.checkGLError("glBindTexture");
        updateImage();
    }

    private void updateImage() {
        AppController.MyBitmap bitmapFromMemCache = texFromMemCache();
        if (DBG)
            Log.d(TAG, "updateImage. bitmapFromMemCache= " + bitmapFromMemCache);
        if (bitmapFromMemCache != null) {
            // Assigns the OpenGL texture with the Bitmap
            if (DBG)
                Log.d(TAG, "updateImage. bitmapFromMemCache.getBitmap()= " + bitmapFromMemCache.getBitmap());
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmapFromMemCache.getBitmap(), 0);

        }
    }

    /**
     * 1. Setup pcWidth and pcHeight.
     * 2. Setup the texture dimension to some POT dim.
     * 3. addBitmapToMemoryCache(getKeyImgId(0)
     */
    protected boolean prepareTexture() {

        try {
            Paint paint = new Paint();
            setupPaint(paint);
            calculatePcWidth(paint);

            if (!MultiPicScrollObject.isMemoryEnough(getTexDim()))
                return false;

            Bitmap textureBm = Bitmap.createBitmap(getTexDim(), getTexDim(), Bitmap.Config.ARGB_8888);
            textureBm.eraseColor(Color.argb(0, 255, 255, 255));

            Bitmap bitmap;
            String type, text;
            int backColor;
            int[] content = new int[textureBm.getWidth()];
            int maxPicWidthPerTexture = getTexDim() / mPcHeight * getTexDim();
            if (DBG)
                Log.d(TAG, "prepareTexture. maxPicWidthPerTexture= " + maxPicWidthPerTexture);

            for (Item item : mItems) {
                if (mBeginYinTexture > (getTexDim() - getPcHeight())) {
                    if (DBG)
                        Log.d(TAG, "prepareTexture. pcHeight= " + getPcHeight() + ",texture remain available height= "
                                + (getTexDim() - mBeginYinTexture) + "texture is not enough, stop read.");
                    break;
                }

                type = item.type;
                if (!TextUtils.isEmpty(type)) {

                    if ("2".equals(type)) {//bitmap

                        bitmap = getBitmap(item, mPcHeight);
                        if (DBG)
                            Log.d(TAG, "bitmap= " + bitmap);
                        //
                        if (bitmap != null) {
                            setTextureBmPixelsOfFatPic(bitmap, -1, (mPcHeight - bitmap.getHeight()) / 2, content, maxPicWidthPerTexture, textureBm);

                            if (DBG)
                                Log.d(TAG, "after setTextureBmPixelsOfFatPic, mBeginXinTexture= " + mBeginXinTexture
                                        + ", mBeginYinTexture= " + mBeginYinTexture);
                        }

                    } else if ("5".equals(type)) {//text
                        text = getText(item);
                        if (DBG)
                            Log.d(TAG, "text= " + text);

                        if (!TextUtils.isEmpty(text)) {
                            setupPaint(paint, item);
                            backColor = getBackColor(item);
                            drawTextBitmapAndSetTexPixels(content, text, backColor, paint,
                                    maxPicWidthPerTexture, textureBm);

                            if (DBG)
                                Log.d(TAG, "after drawTextBitmapAndSetTexPixels, mBeginXinTexture= " + mBeginXinTexture
                                        + ", mBeginYinTexture= " + mBeginYinTexture);
                        }
                    }
                }
            }

            if (DBG)
                Log.d(TAG, "mPcWidth= " + mPcWidth + ", mPcHeight= " + mPcHeight
                        + ", mBeginXinTexture= " + mBeginXinTexture + ", mBeginYinTexture= " + mBeginYinTexture
                 + ", mTexDim= " + getTexDim());

            if (DBG)
                Log.d(TAG, "addBitmapToMemoryCache. mKey= " + mKey);
            AppController.getInstance().addBitmapToMemoryCache(getKey(), new AppController.MyBitmap(textureBm, mPcWidth, getPcHeight()));

            if (DBG_PNG) {
                new File("/mnt/sdcard/mul").mkdir();
                QuadGenerator.toPng(textureBm, new File("/mnt/sdcard/mul/" + "singleline_scroll.png"));
            }

            return true;
        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }


    }

    //text bitmap background color
    private int getBackColor(Item item) {
        if (!TextUtils.isEmpty(item.backcolor) && !"0xFF000000".equals(item.backcolor)) {
            if ("0xFF010000".equals(item.backcolor)) {
                return GraphUtils.parseColor("0xFF000000");
            } else {
                return GraphUtils.parseColor(item.backcolor);
            }
        } else {
            return GraphUtils.parseColor("0x00000000");
        }
    }

    protected String getKey(){
        if (DBG)
            Log.d(TAG, "getKey. mKey= " + mKey);
        if (!TextUtils.isEmpty(mKey))
            return mKey;

        if (mItems != null){
            String text;
            for (Item item : mItems) {
                if ("2".equals(item.type) && (item.filesource != null)) {
                    if (!TextUtils.isEmpty(item.filesource.MD5))
                        mKey += ("_" + item.filesource.MD5);
                    else
                        mKey += ("_" + item.filesource.filepath);

                } else if ("5".equals(item.type)) {
                    text = getText(item);
                    if (!TextUtils.isEmpty(text))
                        mKey += ("_" + item.getSinglelineScrollTextBitmapHash(text));
                }
            }

            mKey += ("_" + mTextSize);
        }

        if (DBG)
            Log.d(TAG, "getKey. mKey= " + mKey);

        return mKey;
    }
    public AppController.MyBitmap texFromMemCache() {

        return AppController.getInstance().getBitmapFromMemCache(getKey());
    }

    // Load shaders, create vertices, texture coordinates etc.
    public void init() {
        setGLColor();

        // Initialize the triangle vertex array
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        SinglelineScrollRenderer.checkGLError("loadShaders");

        mProgram = GLES20.glCreateProgram(); // Create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader); // Add the vertex shader to program
        SinglelineScrollRenderer.checkGLError("glAttachShader:vert");
        GLES20.glAttachShader(mProgram, fragmentShader); // Add the fragment shader to program
        SinglelineScrollRenderer.checkGLError("glAttachShader:frag");
        GLES20.glLinkProgram(mProgram); // Creates OpenGL program executables
        SinglelineScrollRenderer.checkGLError("glLinkProgram");
        GLES20.glUseProgram(mProgram);
        SinglelineScrollRenderer.checkGLError("glUseProgram");

        // Get handle to the vertex shader's vPosition member
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        SinglelineScrollRenderer.checkGLError("glGetAttribLocation:vPosition");
        // Get handle to the vertex shader's vPosition member
        maTexCoordsHandle = GLES20.glGetAttribLocation(mProgram, "vTexCoord");
        SinglelineScrollRenderer.checkGLError("glGetAttribLocation:vTexCoord");
        // get handle to uniform parameter
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        SinglelineScrollRenderer.checkGLError("glGetUniformLocation:uMVPMatrix");

        // get handle to uniform parameter
        muMMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMMatrix");
        SinglelineScrollRenderer.checkGLError("glGetUniformLocation:uMMatrix");

        muTextureHandle = GLES20.glGetUniformLocation(mProgram, "u_s2dTexture");
        SinglelineScrollRenderer.checkGLError("glGetUniformLocation:u_s2dTexture");
        muTexScaleHandle = GLES20.glGetUniformLocation(mProgram, "uTexScale");
        SinglelineScrollRenderer.checkGLError("glGetUniformLocation:muTexScaleHandle");
    }

    protected void setGLColor() {
        // R G B A.
        // GLES20.glClearColor(1.0f, 0.5f, 0.5f, 1.0f);
        //TODO::背景颜色(透明)
        int backgroundColor = GraphUtils.parseColor("0x00000000");//
        final float red = Color.red(backgroundColor) / 255.0f;
        final float green = Color.green(backgroundColor) / 255.0f;
        final float blue = Color.blue(backgroundColor) / 255.0f;
        final float alpha = Color.alpha(backgroundColor) / 255.0f;
        if (DBG)
            Log.d(TAG, "init. [r=" + red + ", g=" + green + ", b=" + blue + ", alpha=" + alpha);
        GLES20.glClearColor(red, green, blue, alpha);
    }


    private float pixelTemp = 0.0f;
    public boolean mIsGreaterThanAPixelPerFrame = false;

    public void render() {

        if (mIsGreaterThanAPixelPerFrame)
            Matrix.translateM(mMMatrix, 0, mPixelPerFrame, 0.f, 0.f);
        else {
            pixelTemp += mPixelPerFrame;
            if (pixelTemp <= -1.0f) {
                Matrix.translateM(mMMatrix, 0, -1.0f, 0.f, 0.f);
                pixelTemp += 1;
            }
        }

        final float overflow = mMMatrix[12] - (-mEvenedWidth - mRealReadPcWidth);
        if (DBG_MATRIX)
            Log.d(TAG, "mMMatrix[12]= " + mMMatrix[12] + ", overflow= " + overflow
                    + ", mRepeatCount= " + mRepeatCount + ", mCurrentRepeats= " + mCurrentRepeats);
        if (overflow < 0) {

            Matrix.setIdentityM(mMMatrix, 0);
            // To the left edge.
            Matrix.translateM(mMMatrix, 0, -mEvenedWidth + overflow, 0.f, 0.f);
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
            mQuadTCB.put(mQuadSegs[i].getCoords(getTexDim(), getTexDim())); // Add the coordinates to the FloatBuffer
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

    protected void genQuadSegs() {
        if (DBG)
            Log.d(TAG, "genQuadSegs. [");
        QuadGenerator qg = new QuadGenerator(mPcWidth, getPcHeight(), getTexDim(), mEvenedWidth);
        final int repeatedQuadsSize = qg.getRepeatedQuadsSize();
        if (DBG)
            Log.d(TAG, "repeatedQuadsSize= " + repeatedQuadsSize);

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

    }


    public void setPixelPerFrame(float speedByFrame) {
        if (speedByFrame >= 1.0f) {
            mPixelPerFrame = Math.round(-speedByFrame);
            mIsGreaterThanAPixelPerFrame = true;
        } else {
            mPixelPerFrame = -speedByFrame;
            mIsGreaterThanAPixelPerFrame = false;
        }
    }


    public void setView(FinishObserver singleLineTextSurfaceView) {
        mFinishObserver = singleLineTextSurfaceView;
    }


    protected void setupPaint(Paint paint) {
        paint.setTextSize(mTextSize);
        paint.setAntiAlias(AppController.getInstance().getCfg().isAntialias());
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
    }

    private void setupPaint(Paint paint, Item item) {
        if (DBG)
            Log.d(TAG, "setupPaint. [paint antialis=" + paint.isAntiAlias()
                    + ", linear=" + paint.isLinearText() + ", textColor= " + item.textColor);
        paint.setColor(GraphUtils.parseColor(item.textColor));

        if (item.logfont != null && "1".equals(item.logfont.lfUnderline)) {
            paint.setFlags(Paint.UNDERLINE_TEXT_FLAG);
        } else
            paint.setFlags(0);

        setTypeface(paint, AppController.getInstance().getTypeface(item.logfont.lfFaceName), getStyle(item));
    }

    private int getStyle(Item item) {
        int style = Typeface.NORMAL;
        if (item.logfont != null) {
            if ("1".equals(item.logfont.lfItalic)) {
                style = Typeface.ITALIC;
            }
            if ("700".equals(item.logfont.lfWeight)) {
                style |= Typeface.BOLD;
            }
        }

        if (DBG) {
            Log.d(TAG, "style = " + style);
        }

        return style;
    }

    public void setTypeface(Paint paint, Typeface tf, int style) {

        if (tf == null) {
            tf = Typeface.defaultFromStyle(style);
        } else {
            tf = Typeface.create(tf, style);
        }

        if (style > 0) {
            setTypeface(paint, tf);
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            paint.setFakeBoldText((need & Typeface.BOLD) != 0);
            paint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            paint.setFakeBoldText(false);
            paint.setTextSkewX(0);
            setTypeface(paint, tf);
        }
    }

    public void setTypeface(Paint paint, Typeface tf) {
        if (DBG)
            Log.d(TAG, "setTypeface. paint old Typeface= " + paint.getTypeface() + ", new tf= " + tf);
        if (paint.getTypeface() != tf) {
            paint.setTypeface(tf);
            if (DBG)
                Log.d(TAG, "setTypeface. paint.getTypeface()= " + paint.getTypeface());
        }
    }

    public void setTextItemBitmapHash(HashCode textBitmapHash) {
        mTextBitmapHash = textBitmapHash;
        // TODO Auto-generated method stub

    }

    protected int getTexDim() {
        if (mTexDim < 0)
            mTexDim = QuadGenerator.findClosestPOT(mPcWidth, mPcHeight);

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

    protected int getRealReadPcWidth(int pcWidth, int pcHeight, int texDim) {
        if (DBG)
            Log.d(TAG, "getRealReadPcWidth. pcWidth= " + pcWidth + ", pcHeight= " + pcHeight + ", texDim= " + texDim);
        if (pcWidth > pcHeight) {
            int maxPcWidthPerTexture = texDim / pcHeight * texDim;
            if (pcWidth > maxPcWidthPerTexture)
                return maxPcWidthPerTexture;
            else
                return pcWidth;
        } else
            return pcWidth;
    }


    protected void setTextureBmPixelsOfFatPic(Bitmap bitmap, int textWidth, int bitmapTopToBeginY, int[] content, int maxPicWidthPerTexture, Bitmap textureBm) {

        if (DBG)
            Log.d(TAG, "setTextureBmPixelsOfFatPic. bitmap.width= " + bitmap.getWidth() + ", bitmap.height= " + bitmap.getHeight()
                    + ", textWidth= " + textWidth + ", mBeginXinTexture= " + mBeginXinTexture + ", mBeginYinTexture= " + mBeginYinTexture);

        if (mBeginYinTexture > (getTexDim() - getPcHeight())) {
            if (DBG)
                Log.d(TAG, "texture is not enough.");
            return;
        }

        int remainAvailableWidthPerLine = getTexDim() - mBeginXinTexture;
        int realDrawWidth = bitmap.getWidth();
        int alreadyReadWidth = 0;
        if (textWidth > 0) {
            realDrawWidth = textWidth;//draw text bitmap
        }

        int tempBeginYinTexture = mBeginYinTexture + bitmapTopToBeginY;

        if (remainAvailableWidthPerLine > 0) {//setpixels in remain space

            if (DBG)
                Log.d(TAG, "setTextureBmPixelsOfFatPic. realDrawWidth= " + realDrawWidth
                        + ", remainAvailableWidthPerLine= " + remainAvailableWidthPerLine
                        + ", tempBeginYinTexture= " + tempBeginYinTexture);
            if (realDrawWidth >= remainAvailableWidthPerLine) {
                if (DBG)
                    Log.d(TAG, "setTextureBmPixelsOfFatPic. realDrawWidth >= remainAvailableWidthPerLine.");

                for (int k = 0; k < bitmap.getHeight(); k++) {
                    bitmap.getPixels(content, 0, bitmap.getWidth(), 0, k, remainAvailableWidthPerLine, 1);
                    textureBm.setPixels(content, 0, getTexDim(), mBeginXinTexture, tempBeginYinTexture + k, remainAvailableWidthPerLine, 1);
                }
                alreadyReadWidth = remainAvailableWidthPerLine;
                realDrawWidth -= remainAvailableWidthPerLine;
                mBeginXinTexture = 0;
                mBeginYinTexture += mPcHeight;

            } else {//realDrawWidth < remainAvailableWidthPerLine
                if (DBG)
                    Log.d(TAG, "setTextureBmPixelsOfFatPic. realDrawWidth < remainAvailableWidthPerLine.");
                for (int k = 0; k < bitmap.getHeight(); k++) {
                    bitmap.getPixels(content, 0, bitmap.getWidth(), 0, k, realDrawWidth, 1);
                    textureBm.setPixels(content, 0, getTexDim(), mBeginXinTexture, tempBeginYinTexture + k, realDrawWidth, 1);
                }
                mBeginXinTexture += realDrawWidth;
                return;
            }

        }

        if (DBG)
            Log.d(TAG, "setTextureBmPixelsOfFatPic." + ", realDrawWidth= " + realDrawWidth);
        if (realDrawWidth == 0) {
            if (DBG)
                Log.d(TAG, "setTextureBmPixelsOfFatPic. all bitmap pixels always set into textureBm.");
            return;
        }

        if (mBeginYinTexture > (getTexDim() - getPcHeight())) {
            if (DBG)
                Log.d(TAG, "texture is not enough.");
            return;
        }

        //beginXinTexture = 0
        if (DBG)
            Log.d(TAG, "mBeginXinTexture= " + mBeginXinTexture + ", maxPicWidthPerTexture= " + maxPicWidthPerTexture);

        int readWidth = Math.min(maxPicWidthPerTexture - (mBeginYinTexture / mPcHeight * getTexDim() + mBeginXinTexture), realDrawWidth);
        int segments = readWidth / getTexDim();
        if (readWidth % getTexDim() > 0)
            segments++;

        if (DBG)
            Log.d(TAG, "maxPicWidthPerTexture= " + maxPicWidthPerTexture + ", readWidth= " + readWidth + ", segments= " + segments);

        int readSize = 0;
        for (int j = 0; j < segments; j++) {
            readSize = Math.min(readWidth - j * getTexDim(), getTexDim());
            tempBeginYinTexture = mBeginYinTexture + bitmapTopToBeginY;

            if (DBG)
                Log.d(TAG, " tempBeginYinTexture= " + tempBeginYinTexture);
            for (int k = 0; k < bitmap.getHeight(); k++) {
                if (DBG_READ)
                    Log.d(TAG, "j= " + j + ", k= " + k);
                bitmap.getPixels(content, 0, bitmap.getWidth(), alreadyReadWidth + j * getTexDim(), k, readSize, 1);
                textureBm.setPixels(content, 0, getTexDim(), 0, tempBeginYinTexture + k, readSize, 1);
            }
            if ((j < segments - 1) || (j == (segments - 1) && readSize == getTexDim()))
                mBeginYinTexture += getPcHeight();
        }

        mBeginXinTexture = readSize % getTexDim();

    }

    protected void drawTextBitmapAndSetTexPixels(int[] content, String originalText,
                                                 int backColor, Paint paint, int maxPicWidthPerTexture,
                                                 Bitmap textureBm) {

        if (DBG)
            Log.d(TAG, "drawTextBitmapAndSetTexPixels. "+ "backColor= " + backColor);

        float baseLine;
        Rect bounds = new Rect();
        paint.getTextBounds(originalText, 0, originalText.length(), bounds);
        baseLine = (getPcHeight() - bounds.height()) / 2 + Math.abs(bounds.top);
        if (DBG)
            Log.d(TAG, "drawTextBitmapAndSetTexPixels. original bounds= " + bounds
                    + ", mPcHeight= " + mPcHeight + ", baseLine= " + baseLine);

        float measureWidth = paint.measureText(originalText);
        int myw = (int) ensuredWidth(bounds, measureWidth);

        int saveBitmapWidth = myw, drawLength = originalText.length();
        if (DBG)
            Log.d(TAG, "drawTextBitmapAndSetTexPixels. original text myw= " + myw + ", drawLength= " + drawLength);
        if (myw > MAX_DRAW_TEXT_WIDTH) {
            saveBitmapWidth = MAX_DRAW_TEXT_WIDTH;
            drawLength = (MAX_DRAW_TEXT_WIDTH / mTextSize);
        }

        Bitmap savedBitmap = Bitmap.createBitmap(saveBitmapWidth, getPcHeight(), Bitmap.Config.ARGB_8888);
        savedBitmap.eraseColor(Color.argb(0, 255, 255, 255));
        // Creates a new canvas that will draw into a bitmap instead of rendering into the screen
        Canvas bitmapCanvas = new Canvas(savedBitmap);

        int drawTextWidth;
        String text;
        for (int i = 0; i < originalText.length(); i += drawLength) {

            if (DBG)
                Log.d(TAG, "i= " + i + ", drawLength= " + drawLength + ", (i + drawLength)= " + (i + drawLength) + ", originalText.length= " + originalText.length());

            if (mBeginYinTexture > (getTexDim() - getPcHeight())) {
                if (DBG)
                    Log.d(TAG, "drawTextBitmapAndSetTexPixels. texture is not enough, stop draw text.");
                break;
            }

            text = originalText.substring(i, Math.min(i + drawLength, originalText.length()));
            if (DBG)
                Log.d(TAG, "text= " + text);

            if (drawLength < originalText.length())
                paint.getTextBounds(text, 0, text.length(), bounds);

            drawTextWidth = (int) ensuredWidth(bounds, paint.measureText(text));
            bitmapCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            //background color
            bitmapCanvas.drawColor(backColor);
            bitmapCanvas.drawText(text, 0, text.length(), bounds.left < 0 ? -bounds.left : 0, baseLine, paint);

            if (DBG_PNG) {
                new File("/mnt/sdcard/mul").mkdir();
                QuadGenerator.toPng(savedBitmap, new File("/mnt/sdcard/mul/" + "singleline_scroll_text_" + i + ".png"));
            }
            setTextureBmPixelsOfFatPic(savedBitmap, drawTextWidth, (mPcHeight - savedBitmap.getHeight()) / 2, content, maxPicWidthPerTexture, textureBm);
        }

    }


    public void setRepeatCount(int repeatCount) {
        mRepeatCount = repeatCount;
    }

    public void setTextSize(int textSize) {
        mTextSize = textSize;
    }

    private void calculatePcWidth(Paint paint) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        String type, text = null;
        int myw, pcWidth = 0;
        Rect bounds = new Rect();
        float measureWidth;

        for (Item item : mItems) {

            type = item.type;
            if ("2".equals(type)) {
                BitmapFactory.decodeFile(item.getAbsFilePath(), options);
                pcWidth += options.outWidth / (options.outHeight / ((float)getPcHeight()));
                if (DBG)
                    Log.d(TAG, "calculatePcWidth. original bitmap width= " + options.outWidth + ", height= " + options.outHeight
                     + ", pcHeight= " + getPcHeight() + ", picture need width= " + options.outWidth / (options.outHeight / ((float)getPcHeight()))
                     + ", pcWidth= " + pcWidth);

            } else if ("5".equals(type)) {

                text = getText(item);
                if (TextUtils.isEmpty(text))
                    continue;

                setupPaint(paint, item);
                paint.getTextBounds(text, 0, text.length(), bounds);
                measureWidth = paint.measureText(text, 0, text.length());
                myw = (int) ensuredWidth(bounds, measureWidth);
                pcWidth += myw;

                if (DBG)
                    Log.d(TAG, "calculatePcWidth. text item myw= " + myw + ", pcWidth= " + pcWidth);

            }
        }

        if (DBG)
            Log.d(TAG, "calculatePcWidth. pcWidth= " + pcWidth);

        setPcWidth(pcWidth);
    }

    private String getText(Item item) {
        if (item.filesource != null && "1".equals(item.filesource.isrelative))
            return Texts.getStringFromFile(AppController.getPlayingRootPath() + "/" + item.filesource.filepath);
        else
            return item.text;
    }

    public Bitmap getBitmap(Item item, int pcHeight) {

        String filePath = item.getAbsFilePath();
        if (DBG)
            Log.d(TAG, "getBitmap. filePath= " + filePath);

        BitmapFactory.Options sBitmapOptionsCache = new BitmapFactory.Options();
        int sampleSize = 1;
        sBitmapOptionsCache.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, sBitmapOptionsCache);
        int nextHeight = sBitmapOptionsCache.outHeight >> 1;
        if (DBG)
            Log.d(TAG, "original bitmap width= " + sBitmapOptionsCache.outWidth + ", height= " + sBitmapOptionsCache.outHeight);
        while (nextHeight > pcHeight) {
            sampleSize <<= 1;
            nextHeight >>= 1;
        }

        sBitmapOptionsCache.inSampleSize = sampleSize;
        if (DBG)
            Log.d(TAG, "getBitmap. [sampleSize=" + sampleSize + ", nextHeight=" + nextHeight + ", pcHeight=" + pcHeight);

        sBitmapOptionsCache.inJustDecodeBounds = false;
        sBitmapOptionsCache.inPurgeable = true;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, sBitmapOptionsCache);

        if (DBG)
            Log.d(TAG, "getBitmap. after decode, bitmap.width= " + bitmap.getWidth() + ", bitmap.height= " + bitmap.getHeight());
        if (bitmap.getHeight() > getPcHeight()){
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            // 计算宽高缩放率
            float scaleWidth = getPcHeight() / ((float)bitmap.getHeight());
            float scaleHeight = scaleWidth;
            if (DBG)
                Log.d(TAG, "scaleWidth= " + scaleWidth);
            // 缩放图片动作
            matrix.postScale(scaleWidth, scaleHeight);
            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (DBG)
                Log.d(TAG, "getBitmap. newBitmap width= " + newBitmap.getWidth() + ", height= " + newBitmap.getHeight());
//            bitmap.recycle();
//            bitmap = null;
//            System.gc();
            return newBitmap;
        }

        return bitmap;

    }
}
