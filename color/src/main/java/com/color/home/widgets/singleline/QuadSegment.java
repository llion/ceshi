package com.color.home.widgets.singleline;

import android.util.Log;

public class QuadSegment {
    private final static String TAG = "QuadSegment";
    private static final boolean DBG = false;
    private int mQuadWidth;
    private int mQuadHeight;

    private int mTop;
    private int mLeft;

    private int mTopInTex;
    private int mLeftInTex;

    // private String mLine;
    public QuadSegment(int top, int left, int quadWidth, int quadHeight, int topInTex, int leftInTex) {
        mTop = top;
        mLeft = left;

        mTopInTex = topInTex;
        mLeftInTex = leftInTex;
        

        mQuadWidth = quadWidth;
        mQuadHeight = MovingTextUtils.evenIt(quadHeight);

        if (DBG)
            Log.d(TAG, "QuadSegment. [quadWidth=" + quadWidth + ", quadHeight=" + quadHeight + ", top=" + top + ", left=" + left
                    + ", topInTex=" + topInTex + ", leftInTex=" + leftInTex);
    }

    public float[] getQuadPos() {
        float bottomLeft = (-mQuadHeight) + mTop ;
//        float right = mLeft + mQuadWidth - 1.0f;
        float right = mLeft + mQuadWidth;
        float quadPos[] = {
                /*
                 * (1)(3)  (1) is the origin when left = 0, top = 0.
                 * ^ ^ 
                 * | | 
                 * (0)(2) 
                 */
                // X, Y, Z
                mLeft, bottomLeft, 0,
                mLeft, mTop , 0, // +1.0f
                right, bottomLeft, 0,
                right, mTop, 0 // + 1.0f
                // ^1---^3
                // |----|
                // |0---|2
                // The edge dir is UP.
        };
        
        if (DBG)
            Log.d(TAG, "getQuadPos. [top=" + mTop
                    + ", bottomLeft=" + bottomLeft);

        return quadPos;
    }

    public float[] getCoords(int textureWidth, int textureHeight) {
        if (DBG)
            Log.d(TAG, "getCoords. [textureWidth=" + textureWidth + ", textureHeight=" + textureHeight
                    + ", mQuadWidth=" + mQuadWidth
                    + ", mQuadHeight=" + mQuadHeight
                    + ", mLeftInTex + 0.5f=" + (mLeftInTex + 0.5f)
                    + ", mTopInTex=" + mTopInTex);
//        http://stackoverflow.com/questions/5879403/opengl-texture-coordinates-in-pixel-space/5879551#5879551
        // -1 the whole width, i.e., -0.5 the uper and +0.5f the lower is OK.
        float textureSlideWidth = (float) ((float)mQuadWidth) / (float) textureWidth;
        float textureSlideHeight = (float) ((float)mQuadHeight) / (float) textureHeight;
        float bottomLeftX = (float) ((float)mLeftInTex) / (float) textureWidth;
        float bottomLeftY = (float) ((float)mTopInTex + (float)mQuadHeight) / (float) textureHeight;
        float quadCoords[] = {
                bottomLeftX, bottomLeftY, 0,
                bottomLeftX, bottomLeftY - textureSlideHeight, 0,
                bottomLeftX + textureSlideWidth , bottomLeftY, 0,
                bottomLeftX + textureSlideWidth , bottomLeftY - textureSlideHeight, 0
        };

        return quadCoords;
    }

}