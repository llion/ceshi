package com.color.home.widgets;

import android.content.Context;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import com.color.home.ProgramParser;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.utils.Reflects;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * wolfnx
 * 2017/3/15
 */

public class ItemStreamView extends SurfaceView implements OnPlayFinishObserverable, MediaPlayer.OnBufferingUpdateListener,Runnable,FinishObserver,MediaPlayer.OnInfoListener,NetworkObserver,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener, SurfaceHolder.Callback, MediaPlayer.OnErrorListener,MediaPlayer.OnSeekCompleteListener{


    private static final boolean DBG = false;
    private final static String TAG = "ItemSurfaceView";
    private OnPlayFinishedListener mListener;
    private int mVideoWidth;
    private int mVideoHeight;
    public boolean mIsVideoReadyToBePlayed = false;
    public boolean mIsVideoSizeKnown = false;
    private String path;;
    public Context mContext;
    public SurfaceHolder holder;
    private boolean mKeepAsp;
    private boolean mSeekable;
    private boolean mIsLoop;
    private float mVolume;
    private int mStartOffset;
    private int mPlayLength;
    private int mDuration = 50000;
    private NetworkConnectReceiver mNetworkConnectReceiver;

    public MediaPlayer mMediaPlayer;

    private boolean mStarted;

    public ItemStreamView(Context context) {
        this(context,null);
    }

    public ItemStreamView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }


    public ItemStreamView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mContext=context;
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        checkSkipDraw();
    }

    @Override
    public void onPrepared(MediaPlayer mediaplayer) {
        if (DBG)
            Log.d(TAG, "onPrepared called, mIsVideoReadyToBePlayed=" + mIsVideoReadyToBePlayed
                    + ", mIsVideoSizeKnown=" + mIsVideoSizeKnown);
        mIsVideoReadyToBePlayed = true;

//        mMediaPlayer.setLooping(mSeekable && mIsLoop);

        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
            startVideoPlayback();

        }

    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (DBG)
            Log.d(TAG, "onBufferingUpdate percent:" + percent);
    }



    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (DBG)
            Log.e(TAG, "onError. mp, what, extra=" + mp + ", " + what + ", " + extra + ", Thread=" + Thread.currentThread());
        if(what == 1 && extra == -2147483648)
            releaseMediaPlayer();
        return true;
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
    public void surfaceCreated(SurfaceHolder holder) {
        if (DBG)
            Log.d(TAG, "surfaceCreated called");
        playVideo();

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (DBG)
            Log.d(TAG, "surfaceChanged called format=" + format + ", width=" + width + ", height=" + height);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (DBG)
            Log.d(TAG, "surfaceDestroyed called");

        releaseMediaPlayer();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (DBG) {
            Log.d(TAG, "onSeekComplete.....");
        }
        tellListener();
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DBG)
            Log.e(TAG, "onDetachedFromWindow. stopPlayback. this=" + this);

        if (mNetworkConnectReceiver != null)
            mContext.unregisterReceiver(mNetworkConnectReceiver);
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (DBG)
            Log.i(TAG, "onMeasure");
        {
            if (mKeepAsp) {

                int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
                int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
                if (DBG)
                    Log.d(TAG, "onMeasure. mVideoWidth= " + mVideoWidth + ", mVideoHeight= " + mVideoHeight);

                if (mVideoWidth > 0 && mVideoHeight > 0) {
                    if (mVideoWidth * height > width * mVideoHeight) {
                        // Log.i("@@@", "image too tall, correcting");
                        height = width * mVideoHeight / mVideoWidth;
                    } else if (mVideoWidth * height < width * mVideoHeight) {
                        // Log.i("@@@", "image too wide, correcting");
                        width = height * mVideoWidth / mVideoHeight;
                    }
                }

                if (DBG)
                    Log.d(TAG, "setMeasuredDimension. width= " + width + ", height= " + height);
                setMeasuredDimension(width, height);

                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height, Gravity.CENTER);
                this.setLayoutParams(layoutParams);
            }else{

                super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            }
        }

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
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    protected void playVideo() {
        if (DBG)
            Log.i(TAG, "Thread=" + Thread.currentThread());

        doCleanUp();

        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.reset();

            mMediaPlayer.setDataSource(mContext, Uri.parse(path));
            mMediaPlayer.setDisplay(holder);
            mMediaPlayer.prepareAsync();

            //setting volume
//            float leftVolume;
//            float rightVolume = leftVolume = mVolume;
//            mMediaPlayer.setVolume(leftVolume, rightVolume);

            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setOnInfoListener(this);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected void doCleanUp() {
        if (DBG)
            Log.i(TAG, "doCleanUp. , Thread=" + Thread.currentThread());
        mVideoWidth = 0;
        mVideoHeight = 0;
        mIsVideoReadyToBePlayed = false;
        mIsVideoSizeKnown = false;
        mStarted = false;
    }

    private void tellListener() {

        if (DBG)
            Log.i(TAG, "tellListener. Tell listener =" + mListener);

        if (mListener != null) {
            mListener.onPlayFinished(this);
            removeListener(mListener);
        }

    }

    public void releaseMediaPlayer() {
        if (DBG)
            Log.e(TAG, "releaseMediaPlayer. mMediaPlayer=" + mMediaPlayer);
        if (mMediaPlayer != null) {
            // Do not stop, otherwise it could halt the Main thread.
            mMediaPlayer.reset();
            // PlayerPool.getInst().release(mMediaPlayer);
            mMediaPlayer.release();
            mMediaPlayer = null;
            removeCallbacks(this);
        }
    }

    private void startVideoPlayback() {
        if (DBG)
            Log.v(TAG, "startVideoPlayback, mMediaPlayer=" + mMediaPlayer);
        holder.setFixedSize(mVideoWidth, mVideoHeight);
        if (!mStarted) {
            mStarted = true;

            mMediaPlayer.start();
            mNetworkConnectReceiver = new NetworkConnectReceiver(this);
            registerNetworkConnectReceiver();
        }
    }

    public void setItem(RegionView regionView, ProgramParser.Item item) {
        mListener = regionView;
//        mSeekable = ((ProgramParser.VideoItem) item).isSeekable();

        path=item.url;

        if (DBG)
        Log.i(TAG,"path:"+path);

        mKeepAsp = "1".equals(item.reserveAS);
        try {
            mDuration = Integer.parseInt(item.duration);
            postDelayed(this,mDuration);
        } catch (Exception e) {
            // ignore;
            e.printStackTrace();
        }

//        if (DBG)
//            Log.i(TAG, "setItem. SurfaceView, [absFilePath=" + path + ", isSeekable=" + mSeekable);
//        try {
////            mVolume = Float.parseFloat(item.volume);
////            mStartOffset = Integer.parseInt(item.inOffset);
////            mPlayLength = Integer.parseInt(item.playLength);
//        } catch (NumberFormatException nfe) {
//            Log.e(TAG, "Bad Number", nfe);
//        }

    }

    @Override
    public void run() {
        notifyPlayFinished();
    }

    @Override
    public void notifyPlayFinished() {
        if (DBG)
            Log.i(TAG, "tellListener. Tell listener =" + mListener);
        if (mListener != null) {
            mListener.onPlayFinished(this);
            removeListener(mListener);
        }

    }


    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {

        return true;
    }

    @Override
    public void reloadContent() {
      if(mMediaPlayer!=null){

          if(DBG)
              Log.i(TAG,"itemStreamView:-reloadContent");
          mMediaPlayer.start();

      }
    }

    public void registerNetworkConnectReceiver() {
        if (mNetworkConnectReceiver != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mNetworkConnectReceiver, filter);
        }
    }
}
