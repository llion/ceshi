package com.color.home.program.sync;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.Constants;

public abstract class CLIntentService extends Service {
    private final static String TAG = "CLIntentService";
    private static final boolean DBG = false;
    private Handler mServiceHandler;
    private boolean mRedelivery;

    /**
     * Sets intent redelivery preferences. Usually called from the constructor with your preferred semantics.
     * 
     * <p>
     * If enabled is true, {@link #onStartCommand(Intent, int, int)} will return {@link Service#START_REDELIVER_INTENT}, so if this process
     * dies before {@link #onHandleIntent(Intent)} returns, the process will be restarted and the intent redelivered. If multiple Intents
     * have been sent, only the most recent one is guaranteed to be redelivered.
     * 
     * <p>
     * If enabled is false (the default), {@link #onStartCommand(Intent, int, int)} will return {@link Service#START_NOT_STICKY}, and if the
     * process dies, the Intent dies along with it.
     */
    public void setIntentRedelivery(boolean enabled) {
        mRedelivery = enabled;
    }

    @Override
    public void onCreate() {
        // TODO: It would be nice to have an option to hold a partial wakelock
        // during processing, and to have a static startService(Context, Intent)
        // method that would launch the service & hand off a wakelock.

        super.onCreate();
        mServiceHandler = AppController.getHandler();
    }

    @Override
    public void onStart(final Intent intent, final int startId) {
        mServiceHandler.post(new Runnable() {

            @Override
            public void run() {
                onHandleIntent(intent);
                stopSelf(startId);
            }
        });
    }

    /**
     * You should not override this method for your IntentService. Instead, override {@link #onHandleIntent}, which the system calls when
     * the IntentService receives a start request.
     * 
     * @see android.app.Service#onStartCommand
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return mRedelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DBG)
            Log.v(TAG, "onDestroy. [");

        // Don't call super.onDestory(), as we'd like to keep the "Looper".

        if (Constants.HTTP_SERVER_SUPPORT && AppController.getInstance().getConnectivity().hasServer())
            PollingUtils.startPollingService(getApplicationContext(), 6000, SyncService.class, Constants.ACTION_REFRESH);
    }

    /**
     * Unless you provide binding for your service, you don't need to implement this method, because the default implementation returns
     * null.
     * 
     * @see android.app.Service#onBind
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * This method is invoked on the worker thread with a request to process. Only one Intent is processed at a time, but the processing
     * happens on a worker thread that runs independently from other application logic. So, if this code takes a long time, it will hold up
     * other requests to the same IntentService, but it will not hold up anything else. When all requests have been handled, the
     * IntentService stops itself, so you should not call {@link #stopSelf}.
     * 
     * @param intent
     *            The value passed to {@link android.content.Context#startService(Intent)}.
     */
    protected abstract void onHandleIntent(Intent intent);
}
