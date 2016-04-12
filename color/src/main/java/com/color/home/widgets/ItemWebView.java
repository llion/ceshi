package com.color.home.widgets;

import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.color.home.ProgramParser.Item;

public class ItemWebView extends WebView implements OnPlayFinishObserverable, Runnable, FinishObserver {
    private static final boolean DBG = false;
    // never public, so that another class won't be messed up.
    private final static String TAG = "ItemWebView";

    private boolean mIsDetached;
    private Item mItem;
    private OnPlayFinishedListener mListener;
    private int mDuration;

    public ItemWebView(Context context, AttributeSet attrs, int defStyle, Map<String, Object> javaScriptInterfaces, boolean privateBrowsing) {
        super(context, attrs, defStyle, javaScriptInterfaces, privateBrowsing);
        // TODO Auto-generated constructor stub
    }

    public ItemWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    public ItemWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public ItemWebView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public void setItem(RegionView regionView, Item item) {
        mListener = regionView;
        this.mItem = item;

        mDuration = Integer.parseInt(item.duration);

        removeCallbacks(this);
        postDelayed(this, mDuration);
        
        
        loadUrl(item.url);
        if (DBG)
            Log.i(TAG, "setItem. url=" + item.url);

        WebSettings webSettings = this.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setPluginsEnabled(true);
        webSettings.setPluginState(WebSettings.PluginState.ON);
        
        setWebViewClient(new MyWebViewClient());
        
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // if (Uri.parse(url).getHost().equals("www.example.com")) {
            // // This is my web site, so do not override; let my WebView load
            // the page
            // return false;
            // }
            // // Otherwise, the link is not for a page on my site, so launch
            // another Activity that handles URLs
            // Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            // startActivity(intent);
            // return true;
            if (DBG)
                Log.i(TAG, "shouldOverrideUrlLoading. url=" + url);
            
//            view.loadUrl(url);
//            return true;
            
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (DBG)
                Log.i(TAG, "onPageStarted. view, url, favicon");
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (DBG)
                Log.i(TAG, "onPageFinished. view, url");
            // TODO Auto-generated method stub
            super.onPageFinished(view, url);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            // TODO Auto-generated method stub
            if (DBG)
                Log.i(TAG, "onLoadResource. view, url");
            super.onLoadResource(view, url);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            // TODO Auto-generated method stub
            if (DBG)
                Log.i(TAG, "shouldInterceptRequest. view, url");
            return super.shouldInterceptRequest(view, url);
        }

        @Override
        @Deprecated
        public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
            // TODO Auto-generated method stub
            if (DBG)
                Log.i(TAG, "onTooManyRedirects. view, cancelMsg, continueMsg");
            super.onTooManyRedirects(view, cancelMsg, continueMsg);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            // TODO Auto-generated method stub
            if (DBG)
                Log.i(TAG, "onReceivedError. view, errorCode, description, failingUrl");
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onFormResubmission(WebView view, Message dontResend, Message resend) {
            // TODO Auto-generated method stub
            if (DBG)
                Log.i(TAG, "onFormResubmission. view, dontResend, resend");
            super.onFormResubmission(view, dontResend, resend);
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            // TODO Auto-generated method stub
            if (DBG)
                Log.i(TAG, "doUpdateVisitedHistory. view, url, isReload");
            super.doUpdateVisitedHistory(view, url, isReload);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            // TODO Auto-generated method stub
            if (DBG)
                Log.i(TAG, "onReceivedSslError. view, handler, error");
            super.onReceivedSslError(view, handler, error);
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
            // TODO Auto-generated method stub
            if (DBG)
                Log.i(TAG, "onReceivedHttpAuthRequest. view, handler, host, realm");
            super.onReceivedHttpAuthRequest(view, handler, host, realm);
        }

        @Override
        public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
            // TODO Auto-generated method stub
            if (DBG)
                Log.i(TAG, "shouldOverrideKeyEvent. view, event");
            return super.shouldOverrideKeyEvent(view, event);
        }

        @Override
        public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
            // TODO Auto-generated method stub
            if (DBG)
                Log.i(TAG, "onUnhandledKeyEvent. view, event");
            super.onUnhandledKeyEvent(view, event);
        }

        @Override
        public void onScaleChanged(WebView view, float oldScale, float newScale) {
            // TODO Auto-generated method stub
            if (DBG)
                Log.i(TAG, "onScaleChanged. view, oldScale, newScale");
            super.onScaleChanged(view, oldScale, newScale);
        }

        @Override
        public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
            // TODO Auto-generated method stub
            if (DBG)
                Log.i(TAG, "onReceivedLoginRequest. view, realm, account, args");
            super.onReceivedLoginRequest(view, realm, account, args);
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsDetached = true;

        boolean removeCallbacks = removeCallbacks(this);
        if (DBG)
            Log.i(TAG, "onDetachedFromWindow. Try to remove call back. result is removeCallbacks=" + removeCallbacks);
    }

    @Override
    public void notifyPlayFinished() {
        if (mListener != null) {
            if (DBG)
                Log.i(TAG, "tellListener. Tell listener =" + mListener);
            mListener.onPlayFinished(this);
        }
    }

    @Override
    public void run() {
        if (DBG)
            Log.i(TAG, "run. Finish item play due to play length time up = ");
        notifyPlayFinished();
    }
}