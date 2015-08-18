package com.color.home;

import java.security.spec.MGF1ParameterSpec;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class TestProgramIndex extends ActivityInstrumentationTestCase2<MainActivity> {

    public TestProgramIndex() {
        super(MainActivity.class);
    }

    private Handler mBgHandler;
    private Looper mBgLooper;

    public void onCreateHandlerThread() {
        HandlerThread ht = new HandlerThread(TAG + " bg thread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        ht.start();
        mBgLooper = ht.getLooper();
        mBgHandler = new Handler(mBgLooper) {
            @Override
            public void handleMessage(Message msg) {
                int commandType = msg.what;
                Intent intent = (Intent) msg.obj;
                Log.d(TAG, "handleMessage, msg.what=" + commandType);
                // force using application context to be safer.
                // mMacro.execute(AppWidgetService.this.getApplicationContext(),
                // commandType, intent);

            }
        };
    }

    private final static String TAG = "TestProgramIndex";
    private static final boolean DBG = false;

    int mIndex = 0;
    int mReceivedIndex = 0;

    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
                int indexSticky = intent.getIntExtra(MainActivity.EXTRA_INDEX, 0);
                Log.i(TAG,
                        "onReceive. context, index=" + indexSticky + ", Thread="
                                + Thread.currentThread());

                mReceivedIndex = indexSticky;
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final MainActivity a = getActivity();
        onCreateHandlerThread();
    }

    public void testProgramIndex() throws Exception {
        IntentFilter filter = new IntentFilter(MainActivity.ACTION_CURRENT_PROG_INDEX);
        getActivity().registerReceiver(receiver, filter, null, mBgHandler);

        Thread.sleep(3000);

        if (DBG)
            Log.i(TAG, "testProgramIndextestProgramIndex. , Thread=" + Thread.currentThread());

        mIndex = 1;
        changeProgram();
        Thread.sleep(2000);
        assertEquals(mIndex, mReceivedIndex);

        mIndex = 0;
        changeProgram();
        Thread.sleep(2000);
        assertEquals(mIndex, mReceivedIndex);

        mIndex = 2;
        changeProgram();
        Thread.sleep(2000);
        assertEquals(mIndex, mReceivedIndex);

        getActivity().unregisterReceiver(receiver);
    }

    private void changeProgram() {
        Intent change = new Intent("com.clt.broadcast.playProgram");
        change.putExtra(SDCardInsertReceiver.EXTRA_CONTENT, mIndex);
        getActivity().sendBroadcast(change);
    }

    @Override
    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        super.tearDown();

        if (mBgLooper != null)
            mBgLooper.quit();

    }
}
