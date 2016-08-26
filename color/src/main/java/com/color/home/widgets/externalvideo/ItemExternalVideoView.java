package com.color.home.widgets.externalvideo;

import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.Region;
import com.color.home.widgets.ItemData;
import com.color.home.widgets.OnPlayFinishObserverable;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;

import java.io.IOException;

/**
 * Created by Administrator on 2016/8/25.
 */
public class ItemExternalVideoView extends SurfaceView implements ItemData, OnPlayFinishObserverable, Runnable {

    private static final String TAG = "ItemExternalVideoView";
    private static final boolean DBG = false;

    public SurfaceHolder surfaceHolder;
    public Camera camera;
    SurfaceCallback callback;
    private CameraConnectReceiver receiver;

    private OnPlayFinishedListener mListener;
    private long mDuration = 500l;

    public boolean isConnect = false;
    private boolean preview; // 是否正在预览


    public ItemExternalVideoView(Context context) {
        super(context);
    }

    public ItemExternalVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemExternalVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    public void setRegion(Region region) {

    }

    @Override
    public void setItem(RegionView regionView, Item item) {
        if (DBG)
            Log.i(TAG, "setItem. duration= " + item.duration);

        mListener = regionView;
        try {
            mDuration = Long.parseLong(item.duration);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        surfaceHolder = this.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        callback = new SurfaceCallback();
        surfaceHolder.addCallback(callback);
        if (DBG)
            Log.i(TAG,"surfaceHolder= " + surfaceHolder + ", callback= " + callback);

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DBG)
            Log.i(TAG, "onAttachedToWindow.mDuration= " + mDuration + ", " + Camera.getNumberOfCameras());
        removeCallbacks(this);
        postDelayed(this, mDuration);

        //register broadcast
        receiver = new CameraConnectReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        filter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        getContext().registerReceiver(receiver, filter);

    }

    @Override
    public void run() {
        if (DBG)
            Log.i(TAG, "run. mDuration= " + mDuration);
        tellListener();
    }

    private final class SurfaceCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            if (DBG) {
                Log.i(TAG, "surfaceChanged: holder=" + holder
                        + ", format= " + format
                        + ", width= " + width + ", height= " + height + ", thread= " + Thread.currentThread());
            }
// 如果预览无法更改或旋转，注意此处的事件确保在缩放或重排时停止预览
//            if (surfaceHolder.getSurface() == null) {
//                // 预览surface不存在
//                return;
//            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {

            if (DBG) {
                Log.i(TAG, "surfaceCreated: holder=" + holder + ", thread= " + Thread.currentThread());
            }

            openCamera();
            setPreviewDisplay(holder);
            startPreview();

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (DBG)
                Log.d(TAG, "surfaceDestroyed, holder= " + holder + ", camera= " + camera);

            clear();
        }
    }

    //open camera
    public void openCamera() {
        if (DBG)
            Log.i(TAG, "openCamera.");
        try {
            camera = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
            clear();
        }
    }

    //set preview display
    public void setPreviewDisplay(SurfaceHolder holder) {
        if (DBG)
            Log.i(TAG, "setPreviewDisplay.");
        try {
            if (camera != null)
                camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
            clear();
        }
    }

    // start preview
    public void startPreview() {
        if (DBG)
            Log.i(TAG, "startPreview.");
        try {
            if (camera != null) {
                camera.startPreview();
                preview = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            clear();
        }
    }

    private void tellListener() {
        if (mListener != null) {
            if (DBG)
                Log.i(TAG, "tellListener. Tell listener =" + mListener);
            mListener.onPlayFinished(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DBG)
            Log.i(TAG, "onDetachedFromWindow. camera= " + camera);
        clear();
        if (receiver != null)
            getContext().unregisterReceiver(receiver);
        removeCallbacks(this);
    }

    @Override
    public void setListener(OnPlayFinishedListener listener) {
        this.mListener = listener;
    }

    @Override
    public void removeListener(OnPlayFinishedListener listener) {
        this.mListener = null;
    }

    public void clear() {
        if (DBG)
            Log.d(TAG, "clear. surfaceHolder= " + surfaceHolder + ", callback= " + callback + ", camera= " + camera + ", isConnect= " + isConnect);

        if (camera != null) {

            if (preview) {
                camera.stopPreview();
                preview = false;
            }
            camera.release();
            camera = null;

        }

        if (DBG)
            Log.d(TAG, "clear finish. surfaceHolder= " + surfaceHolder + ", callback= " + callback + ", camera= " + camera + ", isConnect= " + isConnect);

    }

    public SurfaceHolder getSurfaceHolder() {
        return surfaceHolder;
    }

    private boolean hasCamera(){
        PackageManager pm = getContext().getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)//是否有后置相机
                || pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)//是否有前置相机
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD
                || Camera.getNumberOfCameras() > 0;
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
}