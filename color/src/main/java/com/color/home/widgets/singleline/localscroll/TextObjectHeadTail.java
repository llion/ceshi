/*
 * This proprietary software may be used only as
 * authorised by a licensing agreement from ARM Limited
 * (C) COPYRIGHT 2013 ARM Limited
 * ALL RIGHTS RESERVED
 * The entire notice above must be reproduced on all authorised
 * copies and copies may only be made to the extent permitted
 * by a licensing agreement from ARM Limited.
 */

package com.color.home.widgets.singleline.localscroll;

import com.color.home.widgets.singleline.QuadGenerator;
import com.color.home.widgets.singleline.QuadSegment;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class TextObjectHeadTail extends TextObject {
    private final static String TAG = "TextObjectHeadTail";
    private static final boolean DBG = false;

    public TextObjectHeadTail(Context context) {
        super(context);
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
    
    @Override
    public void render() {
        Matrix.translateM(mMMatrix, 0, mPixelPerFrame, 0.f, 0.f);
        // 09-08 23:04:05.580: D/TextObject(6052): render. [fl=639.0, i=12
        final float overflow = mMMatrix[12] - (-mEvenedWidth - mPcWidth);
        if (overflow < 0) {
            // if (isFirstRun)
            // isFirstRun = false;

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
