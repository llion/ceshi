package com.color.home.widgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.color.home.Constants;
import com.color.home.ProgramParser;
import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.Region;
import com.color.home.utils.Reflects;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Other thoughts regarding loop for some video that doesn't support loop, such as MPEG2.
 * 1. Play as normal.
 * 2. After play finished, seekTo(0).
 * 3. Wait for onSeekComplete.
 * 4. If it cannot seek complete for 1 seconds, regard this video as unseekable.
 * 5. Unseekable video should go thru our normal (remove view and reinit the view) video play back.
 */
public class ItemVideoView extends SurfaceView implements OnPlayFinishObserverable, OnBufferingUpdateListener, OnCompletionListener,
        OnPreparedListener, OnVideoSizeChangedListener, SurfaceHolder.Callback, OnErrorListener, MediaPlayer.OnSeekCompleteListener {
    private static final boolean DBG = true;
    // never public, so that another class won't be messed up.
    private final static String TAG = "ItemVideoView";
    private OnPlayFinishedListener mListener;
    private Region mRegion;

    private int mVideoWidth;
    private int mVideoHeight;
    private MediaPlayer mMediaPlayer;
    private SurfaceHolder holder;
    private String path;
    private boolean mIsVideoSizeKnown = false;
    private boolean mIsVideoReadyToBePlayed = false;
    private boolean first = true;

    private boolean mIsLoop;
    private boolean mKeepAsp;
    private float mVolume;
    private int mStartOffset;
    private int mPlayLength;
    private boolean mSeekable;

    private Context mContext;

    public void setLoop(boolean loop) {
        mIsLoop = loop;

    }

    public ItemVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        // TODO Auto-generated constructor stub

        // setVisibility(INVISIBLE);
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // If Z order is on the top, the layer flag for the video is invalid.
        // for example, the text won't able to be shown on the top of the video.
        // setZOrderOnTop(true);

        checkSkipDraw();


    }

    private void checkSkipDraw() {
        try {
            Field fieldFrom = Reflects.getFieldFrom(View.class, "mPrivateFlags");
            int flag = fieldFrom.getInt(this);

            if (DBG) {
                Log.i(TAG, "ItemVideoView. value=" + flag);
            }

            fieldFrom.setInt(this, flag & ~0x00000080);
            if (DBG) {
                Log.i(TAG, "ItemVideoView. value new =" + fieldFrom.getInt(this));
            }

        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // checkSkipDraw();

        super.dispatchDraw(canvas);

    }

    public ItemVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        mContext = context;
        // TODO Auto-generated constructor stub
    }

    public ItemVideoView(Context context) {
        this(context, null);
        mContext = context;
        // TODO Auto-generated constructor stub
    }

    public void setItem(RegionView regionView, Item item) {
        mListener = regionView;
        mSeekable = ((ProgramParser.VideoItem) item).isSeekable();
        // TODO Auto-generated method stub
        path = item.getAbsFilePath();
        mKeepAsp = "1".equals(item.reserveAS);
        if (DBG)
            Log.i(TAG, "setItem. VideoView, [absFilePath=" + path + ", isSeekable=" + mSeekable);
        try {
            mVolume = Float.parseFloat(item.volume);
            mStartOffset = Integer.parseInt(item.inOffset);
            mPlayLength = Integer.parseInt(item.playLength);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Bad Number", nfe);
        }


        // setVideoPath(filePath);
        // setOnCompletionListener(new OnCompletionListener() {
        //
        // @Override
        // public void onCompletion(MediaPlayer mp) {
        // if (DBG)
        // Log.i(TAG, "onCompletion. tellListener mp=" + mp
        // + ", filePath" + filePath);
        // tellListener();
        // }
        // });
        // setOnPreparedListener(new OnPreparedListener() {
        // @Override
        // public void onPrepared(MediaPlayer mp) {
        // if (DBG)
        // Log.i(TAG, "onPrepared. mp=" + mp);
        // // mp.setLooping(true);
        //
        // setVisibility(VISIBLE);
        // start();
        // }
        // });
    }

    private void playVideo() {
        if (DBG)
            Log.i(TAG, "playVideo. mIsLoop=" + mIsLoop + ", Thread=" + Thread.currentThread());
        doCleanUp();


        // Create a new media player and set the listeners
        // mMediaPlayer = PlayerPool.getInst().getFree();
        // if (mMediaPlayer == null) {
        // if (DBG)
        // Log.e(TAG, "playVideo. no more MediaPlayer, Thread=" +
        // Thread.currentThread());
        // return;
        // }

        // mMediaPlayer.reset();

        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.setDisplay(holder);
            mMediaPlayer.prepareAsync();

            float leftVolume;
            float rightVolume = leftVolume = mVolume;
            mMediaPlayer.setVolume(leftVolume, rightVolume);

            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);

            mMediaPlayer.setOnSeekCompleteListener(this);


            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void onBufferingUpdate(MediaPlayer arg0, int percent) {
        if (DBG)
            Log.d(TAG, "onBufferingUpdate percent:" + percent);
    }

    @Override
    public void onCompletion(MediaPlayer arg0) {
        if (DBG)
            Log.i(TAG, "onCompletion. mIsLoop=" + mIsLoop + ", mMediaPlayer=" + mMediaPlayer);

        // No need to start, as with not seekable video, this view is destroyed.
//        if (mIsLoop) {
////            mMediaPlayer.seekTo(mStartOffset);
//            if (!mSeekable) {
//                // Only not seekable video need this.
//                // Otherwise, it's handled by setloop(true);
//                mMediaPlayer.start();
//            }
//
//        }

        // No onCompletion is called when looping.
        // Use onSeekComplete instead.
        tellListener();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        if (DBG)
            Log.v(TAG, "onVideoSizeChanged called");
        if (width == 0 || height == 0) {
            Log.e(TAG, "invalid video width(" + width + ") or height(" + height + ")");
            return;
        }
        mIsVideoSizeKnown = true;
        mVideoWidth = width;
        mVideoHeight = height;

        // Donnot call twice. HMH
        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
            startVideoPlayback();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaplayer) {
        if (DBG)
            Log.d(TAG, "onPrepared called, mIsVideoReadyToBePlayed=" + mIsVideoReadyToBePlayed
                    + ", mIsVideoSizeKnown=" + mIsVideoSizeKnown + ", seekable=" + mSeekable + ", loop=" + mIsLoop);
        mIsVideoReadyToBePlayed = true;
        // Can only set after perpare?

        mMediaPlayer.setLooping(mSeekable && mIsLoop);

        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
            startVideoPlayback();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
        if (DBG)
            Log.d(TAG, "surfaceChanged called i=" + i + ", j=" + j + ", k=" + k);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
        if (DBG)
            Log.d(TAG, "surfaceDestroyed called");

        releaseMediaPlayer();

        if(mSeekReceiver != null)
            mContext.unregisterReceiver(mSeekReceiver);
    }

    BroadcastReceiver mSeekReceiver = null;
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (DBG)
            Log.d(TAG, "surfaceCreated called");
        playVideo();

        mSeekReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(DBG)
                    Log.d(TAG, "program seek command received..");
                int seekPoint = intent.getIntExtra("seekTo", -1);
                if(mMediaPlayer.isPlaying() && seekPoint != -1 && seekPoint < mMediaPlayer.getDuration()){
                    mMediaPlayer.seekTo(seekPoint);
                }
            }
        };
        mContext.registerReceiver(mSeekReceiver, new IntentFilter(Constants.ACTION_PROGRAM_SEEK));

    }

    public void releaseMediaPlayer() {
        if (DBG)
            Log.i(TAG, "releaseMediaPlayer. mMediaPlayer=" + mMediaPlayer);
        if (mMediaPlayer != null) {
            // Do not stop, otherwise it could halt the Main thread.
            mMediaPlayer.reset();
            // PlayerPool.getInst().release(mMediaPlayer);
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (DBG)
            Log.i("@@@@", "onMeasure, mKeepAsp=" + mKeepAsp);

        if (mKeepAsp) {
            int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
            int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
            if (mVideoWidth > 0 && mVideoHeight > 0) {
                if (mVideoWidth * height > width * mVideoHeight) {
                    // Log.i("@@@", "image too tall, correcting");
                    height = width * mVideoHeight / mVideoWidth;
                } else if (mVideoWidth * height < width * mVideoHeight) {
                    // Log.i("@@@", "image too wide, correcting");
                    width = height * mVideoWidth / mVideoHeight;
                } else {
                    // Log.i("@@@", "aspect ratio is correct: " +
                    // width+"/"+height+"="+
                    // mVideoWidth+"/"+mVideoHeight);
                }
            }
            // Log.i("@@@@@@@@@@", "setting size: " + width + 'x' + height);
            setMeasuredDimension(width, height);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void doCleanUp() {
        if (DBG)
            Log.i(TAG, "doCleanUp. , Thread=" + Thread.currentThread());
        mVideoWidth = 0;
        mVideoHeight = 0;
        mIsVideoReadyToBePlayed = false;
        mIsVideoSizeKnown = false;
        mStarted = false;
    }

    private boolean mStarted;

    private void startVideoPlayback() {
        if (DBG)
            Log.v(TAG, "startVideoPlayback, mMediaPlayer=" + mMediaPlayer);
        holder.setFixedSize(mVideoWidth, mVideoHeight);
        // mMediaPlayer.setLooping(true);
        if (!mStarted) {
            mStarted = true;
//        mMediaPlayer.seekTo(mStartOffset);
            mMediaPlayer.start();
        }
    }

    // @Override
    // protected void onAttachedToWindow() {
    // super.onAttachedToWindow();
    //
    // start();
    // }

    @Override
    protected void onDetachedFromWindow() {
        // TODO Auto-generated method stub
        super.onDetachedFromWindow();

        if (DBG)
            Log.i(TAG, "onDetachedFromWindow. stopPlayback. this=" + this);

        releaseMediaPlayer();
        doCleanUp();
    }

    @Override
    public void setListener(OnPlayFinishedListener listener) {
        this.mListener = listener;
    }

    @Override
    public void removeListener(OnPlayFinishedListener listener) {
        this.mListener = null;
    }

    private void tellListener() {
        if (DBG)
            Log.i(TAG, "tellListener. , mListener=" + mListener);
        if (mListener != null) {
            if (DBG)
                Log.i(TAG, "tellListener. Tell listener =" + mListener);
            mListener.onPlayFinished(this);
        }
    }

    public void setRegion(Region mRegion) {
        this.mRegion = mRegion;
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (DBG)
            Log.e(TAG, "onError. mp, what, extra=" + mp + ", " + what + ", " + extra + ", Thread=" + Thread.currentThread());
        if(what == 1 && extra == -2147483648)
            releaseMediaPlayer();
        return false;
    }

    public void destroy() {
        if (DBG)
            Log.i(TAG, "destroy. mMediaPlayer=" + mMediaPlayer + ", Thread=" + Thread.currentThread());
        releaseMediaPlayer();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        if (DBG) {
            Log.d(TAG, "onSeekComplete.....");
        }

        tellListener();
    }

    public boolean ismSeekable() {
        return mSeekable;
    }

    public boolean ismIsLoop() {
        return mIsLoop;
    }
}
