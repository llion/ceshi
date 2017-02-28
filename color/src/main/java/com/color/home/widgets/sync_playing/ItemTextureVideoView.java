/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.color.home.widgets.sync_playing;

import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.color.home.ProgramParser;
import com.color.home.utils.Reflects;
import com.color.home.widgets.RegionView;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Play a movie from a file on disk.  Output goes to a TextureView.
 * <p>
 * Currently video-only.
 * <p>
 * Contrast with PlayMovieSurfaceActivity, which uses a SurfaceView.  Much of the code is
 * the same, but here we can handle the aspect ratio adjustment with a simple matrix,
 * rather than a custom layout.
 * <p>
 * TODO: investigate crash when screen is rotated while movie is playing (need
 *       to have onPause() wait for playback to stop)
 */
public class ItemTextureVideoView extends TextureView implements TextureView.SurfaceTextureListener, MoviePlayer.PlayerFeedback {
    private static final String TAG = "ItemTextureVideoView";
    private static final boolean VERBOSE = true;

    private Context mContext;
//    private TextureView mTextureView;

    private boolean mShowStopLabel;
    private MoviePlayer.PlayTask mPlayTask;
    private boolean mSurfaceTextureReady = false;

    private final Object mStopper = new Object();   // used to signal stop


    private ItemTextureVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        // setVisibility(INVISIBLE);
        setSurfaceTextureListener(this);
        // If Z order is on the top, the layer flag for the video is invalid.
        // for example, the text won't able to be shown on the top of the video.
        // setZOrderOnTop(true);

        //TODO for what?
        checkSkipDraw();

    }

    private ItemTextureVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        mContext = context;
    }

    public ItemTextureVideoView(Context context) {
        this(context, null);
        mContext = context;
    }

    private void checkSkipDraw() {
        try {
            Field fieldFrom = Reflects.getFieldFrom(View.class, "mPrivateFlags");
            int flag = fieldFrom.getInt(this);

            Log.i(TAG, "ItemSyncVideoView. value=" + flag);

            fieldFrom.setInt(this, flag & ~0x00000080);
            Log.i(TAG, "ItemSyncVideoView. value new =" + fieldFrom.getInt(this));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


//    @Override
//    protected void onPause() {
//        if(VERBOSE)
//            Log.d(TAG, "PlayMovieActivity onPause");
//        super.onPause();
//        // We're not keeping track of the state in static fields, so we need to shut the
//        // playback down.  Ideally we'd preserve the state so that the player would continue
//        // after a device rotation.
//        //
//        // We want to be sure that the player won't continue to send frames after we pause,
//        // because we're tearing the view down.  So we wait for it to stop here.
//        if (mPlayTask != null) {
//            stopPlayback();
//            mPlayTask.waitForStop();
//        }
//    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
        // There's a short delay between the start of the activity and the initialization
        // of the SurfaceTexture that backs the TextureView.  We don't want to try to
        // send a video stream to the TextureView before it has initialized, so we disable
        // the "play" button until this callback fires.
        Log.d(TAG, "SurfaceTexture ready (" + width + "x" + height + ")");
        mSurfaceTextureReady = true;
//        updateControls();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (mShowStopLabel) {
            if(VERBOSE)
                Log.d(TAG, "stopping movie");
            stopPlayback();
            // Don't update the controls here -- let the task thread do it after the movie has
            // actually stopped.
            //mShowStopLabel = false;
            //updateControls();
        } else {
            if(VERBOSE)
                Log.d(TAG, "starting movie");
            startPlayBack();
        }
    }

    private void startPlayBack() {
        if (mPlayTask != null) {
            Log.w(TAG, "movie already playing");
//            return;
            mPlayTask = null;
        }
        if(VERBOSE)
            Log.d(TAG, "starting movie");
        SpeedControlCallback callback = new SpeedControlCallback();
//            if (((CheckBox) findViewById(R.id.locked60fps_checkbox)).isChecked()) {
//                // TODO: consider changing this to be "free running" mode
//
//            }
        SurfaceTexture st1 = getSurfaceTexture();
        Surface surface = new Surface(st1);
        MoviePlayer player = null;
        try {
            player = new MoviePlayer(new File(mFilePath), surface, callback);
        } catch (IOException ioe) {
            Log.e(TAG, "Unable to play movie", ioe);
            surface.release();
            return;
        }
        adjustAspectRatio(player.getVideoWidth(), player.getVideoHeight());

        mPlayTask = new MoviePlayer.PlayTask(player, this);

        mShowStopLabel = true;
//            updateControls();
        mPlayTask.setLoopMode(mLoop);
        mPlayTask.execute();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture st, int width, int height) {
        // ignore
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
        mSurfaceTextureReady = false;
        // assume activity is pausing, so don't need to update controls
        Log.d(TAG, "Texture Destroyed.");
        mContext.sendBroadcast(new Intent("com.clt.intent.syncProgramStop"));

        stopPlayback();
        return true;    // caller should release ST
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // ignore
    }

    /**
     * Requests stoppage if a movie is currently playing.  Does not wait for it to stop.
     */
    private void stopPlayback() {
        if (mPlayTask != null) {
            mPlayTask.requestStop();
        }
    }

    @Override   // MoviePlayer.PlayerFeedback
    public void playbackStopped() {
        Log.d(TAG, "playback stopped");
        mShowStopLabel = false;
        mPlayTask = null;
//        updateControls();
    }

    /**
     * Sets the TextureView transform to preserve the aspect ratio of the video.
     */
    private void adjustAspectRatio(int videoWidth, int videoHeight) {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        Log.v(TAG, "video=" + videoWidth + "x" + videoHeight +
                " view=" + viewWidth + "x" + viewHeight +
                " newView=" + newWidth + "x" + newHeight +
                " off=" + xoff + "," + yoff);

        Matrix txform = new Matrix();
        getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff, yoff);
        setTransform(txform);
    }

    private ProgramParser.Region mRegion;

    public void setRegion(ProgramParser.Region mRegion) {
        this.mRegion = mRegion;

    }

    private boolean mLoop;
    private String mFilePath;
    private RegionView mListener;

    public void setItem(RegionView regionView, ProgramParser.Item item) {
        mListener = regionView;
        mFilePath = item.getAbsFilePath();
        mLoop = "1".equals(item.loop);
        Log.i(TAG, "setItem. VideoView, [absFilePath=" + mFilePath);
//        try {
//            mVolume = Float.parseFloat(item.volume);
//            mStartOffset = Integer.parseInt(item.inOffset);
//            mPlayLength = Integer.parseInt(item.playLength);
//        } catch (NumberFormatException nfe) {
//            Log.e(TAG, "Bad Number", nfe);
//        }

    }

}
