package com.color.widgets.floating;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.TextView;

public class ToastTextView extends TextView {

    Handler mHandler;
    public ToastTextView(Context context, int duration) {
        super(context);
        mHandler = new Handler(Looper.getMainLooper());
        removeSelf(duration);
    }

    public ToastTextView(Context context, Handler handler, int duration) {
        super(context);
        mHandler = handler;
        removeSelf(duration);
    }

    private void removeSelf(int duration){
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((ViewGroup) ToastTextView.this.getParent()).removeView(ToastTextView.this);
            }
        }, duration);

    }



}
