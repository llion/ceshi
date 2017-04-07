package com.color.home.widgets.clt_json;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.color.home.ProgramParser;
import com.color.home.widgets.singleline.QuadGenerator;
import com.color.home.widgets.singleline.QuadSegment;
import com.color.home.widgets.singleline.localscroll.TextObjectHeadTail;

/**
 * Created by Administrator on 2017/4/5.
 */

public class SLScrollCltJsonHeadTailObject extends SLScrollCltJsonObject {

    protected static final boolean DBG = false;
    protected final static String TAG = "SLScrollCltJsonHeadTail";

    public SLScrollCltJsonHeadTailObject(Context context, ProgramParser.Item item) {
        super(context, item);
    }

    @Override
    protected void genQuadSegs() {
        if (DBG)
            Log.d(TAG, "genQuadSegs. [");
        QuadGenerator qg = new QuadGenerator(mPcWidth, getPcHeight(), getTexDim(), mEvenedWidth);
        final int repeatedQuadsSize = qg.getRepeatedQuadsSize();
        mQuadSegs = new QuadSegment[repeatedQuadsSize];
        for (int i = 0; i < repeatedQuadsSize; i++) {
            mQuadSegs[i] = qg.getQuad(i);
        }
    }

    private float pixelTemp = 0.0f;
    @Override
    public void render() {

        if (mNeedChangeTexture) {

            if (DBG)
                Log.d(TAG, "need change texture.");

            mNeedChangeTexture = false;
            changeTexture();
        }

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
        final float overflow = mMMatrix[12] - (-mEvenedWidth - mRealReadPcWidth);
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
}
