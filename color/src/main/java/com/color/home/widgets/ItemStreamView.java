package com.color.home.widgets;

import android.content.Context;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import com.color.home.ProgramParser;
import com.color.home.network.ExpandNetworkObserver;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.utils.Reflects;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * wolfnx
 * 2017/3/15
 */

public class ItemStreamView extends SurfaceView implements OnPlayFinishObserverable, Runnable,FinishObserver,ExpandNetworkObserver,
          SurfaceHolder.Callback{


    private static final boolean DBG = false;
    private final static String TAG = "ItemStreamView";
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
    private boolean mIsNetConnectted;
    private Handler mHandler;

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
    public void surfaceCreated(SurfaceHolder holder) {
        if (DBG)
            Log.d(TAG, "surfaceCreated called");
        mNetworkConnectReceiver = new NetworkConnectReceiver(this);
        registerNetworkConnectReceiver();
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
        if(mHandler != null)
            mHandler.removeMessages(0);
        releaseMediaPlayer();
        removeCallbacks(this);
        unRegisterNetworkConnectReceiver();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(mHandler != null)
            mHandler.removeMessages(0);
        if (DBG)
            Log.e(TAG, "onDetachedFromWindow. stopPlayback. this=" + this);
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

    private void checkSkipDraw() {
        try {
            Field fieldFrom = Reflects.getFieldFrom(View.class, "mPrivateFlags");
            int flag = fieldFrom.getInt(this);

            if (DBG) {
                Log.i(TAG, "ItemSurfaceView. value=" + flag);
            }

            fieldFrom.setInt(this, flag & ~0x00000080);
            if (DBG) {
                Log.i(TAG, "ItemSurfaceView. value new =" + fieldFrom.getInt(this));
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

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.reset();

        mHandler = new StreamHandler();
        new Thread(){
            @Override
            public void run() {
                try {
                    Log.d(TAG, "setDataSource..");
                    if(mMediaPlayer != null)
                         mMediaPlayer.setDataSource(mContext, Uri.parse(path));
                    if(mHandler != null)
                        mHandler.sendEmptyMessage(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public class StreamHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            try {
                if(mMediaPlayer != null){

                    mMediaPlayer.setDisplay(holder);
                    Log.d(TAG, "prepareAsync..");
                    mMediaPlayer.prepareAsync();
                    Log.d(TAG, "setOnBufferingUpdateListener..");

                    mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                        @Override
                        public void onBufferingUpdate(MediaPlayer mp, int percent) {
                            if (DBG)
                                Log.d(TAG, "onBufferingUpdate percent:" + percent);
                        }
                    });

                    mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            if (DBG)
                                Log.e(TAG, "onError. mp, what, extra=" + mp + ", " + what + ", " + extra + ", Thread=" + Thread.currentThread());
                            if(what == 1 && extra == -2147483648)
                                releaseMediaPlayer();
                            return false;
                        }
                    });

                    mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            if (DBG)
                                Log.d(TAG, "onPrepared called, mIsVideoReadyToBePlayed=" + mIsVideoReadyToBePlayed
                                        + ", mIsVideoSizeKnown=" + mIsVideoSizeKnown);

                            mIsVideoReadyToBePlayed = true;
                            if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
                                startVideoPlayback();
                            }
                        }
                    });

                    mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
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
                    });
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
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

    public synchronized void releaseMediaPlayer() {
        if (DBG)
            Log.e(TAG, "releaseMediaPlayer. mMediaPlayer=" + mMediaPlayer);

        if (mMediaPlayer != null) {
            if(mMediaPlayer.isPlaying()){
                mMediaPlayer.stop();
            }
            new Thread(){
                @Override
                public void run() {
                    if(DBG)
                        Log.i(TAG, "releaseMediaPlayer. reset=start");
                    // Do not stop, otherwise it could halt the Main thread.
                    mMediaPlayer.reset();
                    // PlayerPool.getInst().release(mMediaPlayer);
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }
            }.start();
        }
    }

    private void startVideoPlayback() {
        if (DBG)
            Log.v(TAG, "startVideoPlayback, mMediaPlayer=" + mMediaPlayer);
        holder.setFixedSize(mVideoWidth, mVideoHeight);

        if (!mStarted) {
            mStarted = true;
            if(mMediaPlayer != null)
                mMediaPlayer.start();
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
    public void reloadContent() {
        networkConnectLogic();
    }

    @Override
    public void networkDisconnect() {
        networkDisconnectLogic();
    }

    private synchronized void networkConnectLogic(){
        if(!mIsNetConnectted){
            if(DBG)
                Log.e(TAG,"NetWork_Connected_broadcast_receive");
            mIsNetConnectted = true;
            if (mMediaPlayer != null) {

                if(mMediaPlayer.isPlaying()){
                    mMediaPlayer.stop();
                }
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
            playVideo();
        }
    }

    private synchronized void networkDisconnectLogic(){
        if(mIsNetConnectted){
            if (DBG)
            Log.i(TAG,"NetWork_Disconnected_broadcast_receive");
            mIsNetConnectted = false;
        }
    }

    public void registerNetworkConnectReceiver() {
        if (mNetworkConnectReceiver != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mNetworkConnectReceiver, filter);
        }
    }

    public  void unRegisterNetworkConnectReceiver() {
        if (DBG)
            Log.i(TAG, "mNetworkConnectReceiver-unregisterReceiver-started");
        if (mNetworkConnectReceiver != null)
            mContext.unregisterReceiver(mNetworkConnectReceiver);
    }
}
