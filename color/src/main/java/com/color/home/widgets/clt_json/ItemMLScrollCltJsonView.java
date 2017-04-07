package com.color.home.widgets.clt_json;

import android.content.Context;
import android.graphics.PixelFormat;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.ProgramParser;
import com.color.home.network.NetworkConnectReceiver;
import com.color.home.network.NetworkObserver;
import com.color.home.utils.GraphUtils;
import com.color.home.widgets.OnPlayFinishedListener;
import com.color.home.widgets.RegionView;
import com.color.home.widgets.multilines.ItemMLScrollMultipic2View;
import com.color.home.widgets.multilines.MultiPicScrollRenderer;
import com.color.home.widgets.singleline.MovingTextUtils;

/**
 * Created by Administrator on 2017/4/5.
 */

public class ItemMLScrollCltJsonView extends ItemMLScrollMultipic2View implements NetworkObserver {
    private final static String TAG = "ItemMLScrollCltJsonView";
    private static final boolean DBG = false;
    protected MultiPicScrollRenderer mRenderer;
    protected OnPlayFinishedListener mListener;
    protected ProgramParser.Item mItem;
    protected MLScrollCltJsonObject mTheTextObj;
    private NetworkConnectReceiver mNetworkConnectReceiver;

    public ItemMLScrollCltJsonView(Context context) {
        super(context);
    }

    public void setItem(RegionView regionView, ProgramParser.Item item) {
        mTheTextObj = new MLScrollCltJsonObject(mContext, item);

        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        mRenderer = new MultiPicScrollRenderer(mTheTextObj);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // getHolder().setFormat(PixelFormat.RGBA_8888);

        mListener = regionView;
        this.mItem = item;

        initDisplay(item);

        float pixelPerFrame = MovingTextUtils.getPixelPerFrame(item);
//        mTheTextObj.setPixelPerFrame(Math.max(1, Math.round(pixelPerFrame)));
        mTheTextObj.setPixelPerFrame(pixelPerFrame);

        // Total play length in milisec.
        long playLength = 300000;
        try {
            playLength = Long.parseLong(item.playLength);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        // int mDuration = Integer.parseInt(item.duration);

        boolean mIsScrollByTime = "1".equals(item.isscrollbytime);
        if (mIsScrollByTime) {
            removeCallbacks(this);
            postDelayed(this, playLength);
        } else {
            // Counts.
            int repeatCount = 1;
            try {
                repeatCount = Integer.parseInt(item.repeatcount);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if (DBG)
                Log.d(TAG, "setItem. [repeatCount=" + repeatCount);
            mTheTextObj.setRepeatCount(repeatCount);
            mTheTextObj.setView(this);
            // int mDuration = Integer.parseInt(item.duration);
        }


        mNetworkConnectReceiver = new NetworkConnectReceiver(this);

    }

    protected void initDisplay(ProgramParser.Item item) {
        int color = 0;
        if (!TextUtils.isEmpty(item.backcolor) && !"0xFF000000".equals(item.backcolor)) {
            if ("0xFF010000".equals(item.backcolor)) {
                color = GraphUtils.parseColor("0xFF000000");
            } else {
                color = GraphUtils.parseColor(item.backcolor);
            }
        } else {
            color = GraphUtils.parseColor("0x00000000");
        }

        if ("1".equals(item.invertClr)) {
            mTheTextObj.setBackgroundColor(GraphUtils.invertColor(color));
            if (DBG) {
                Log.d(TAG, "setItem. [invertClr, color=" + dumpColor(color) + ", invert color=" + GraphUtils.invertColor(color));
            }
        } else {
            mTheTextObj.setBackgroundColor(color);
        }
    }

    @Override
    public void run() {
        if (DBG)
            Log.i(TAG, "run. Finish item play due to play length time up");
        notifyPlayFinished();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mNetworkConnectReceiver != null)
            ItemMLPagedCltJsonView.registerNetworkConnectReceiver(mContext, mNetworkConnectReceiver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mTheTextObj != null)
            mTheTextObj.removeCltRunnable();

        if (mNetworkConnectReceiver != null)
            ItemMLPagedCltJsonView.unRegisterNetworkConnectReceiver(mContext, mNetworkConnectReceiver);
    }

    @Override
    public void notifyPlayFinished() {
//        mRenderer.finish();
        if (DBG)
            Log.i(TAG, "tellListener. Tell listener =" + mListener);

        if (mListener != null) {
            mRenderer.finish();
            mListener.onPlayFinished(this);
            removeListener(mListener);
        }
    }

    @Override
    public void setListener(OnPlayFinishedListener listener) {
        this.mListener = listener;
    }

    @Override
    public void removeListener(OnPlayFinishedListener listener) {
        this.mListener = null;
    }

    public MultiPicScrollRenderer getmRenderer() {
        return mRenderer;
    }

    @Override
    public void reloadContent() {
        if (DBG)
            Log.d(TAG, "reloadContent. mTheTextObj= " + mTheTextObj);
        if (mTheTextObj != null)
            mTheTextObj.reloadCltJson();
    }
}
