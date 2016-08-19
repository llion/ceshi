package com.color.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.color.home.keyboard.KeyBoardNav;
import com.color.home.program.sync.FailedProgramChecker;
import com.color.home.program.sync.SyncService;
import com.color.widgets.floating.FloatingLayout;

import java.io.File;

// GIF View.
//http://stackoverflow.com/questions/14482415/show-gif-with-android-graphics-movie
// Weather View.
// Clock.
// Word, PPT excel.
// Clock.
// adb shell am broadcast -a com.clt.broadcast.playProgram --es path /mnt/usb_storage/USB_DISK0/udisk0 --es file_name test-halt-onlastpage.vsn
// adb shell am broadcast -a com.clt.broadcast.playProgram --es path /mnt/usb_storage/USB_DISK0/udisk0 --es file_name test-multitext-halt.vsn

/**
 * @author zzjd7382
 *         <p>
 *         MainActivity:V
 */
public class MainActivity extends Activity {
    private static final boolean DBG = false;
    final static String TAG = "MainActivity";

    public ProgramsViewer mProgramsViewer;
    public ViewGroup mContentVG;
    private MainReceiver mReceiver;
    private ScreenStatusReceiver mScreenStatusReceiver;
    private CopyProgress mCp;
    private FloatingLayout mSyncInfolayout;
    private KeyBoardNav mKb;

    private class MainReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Constants.ACTION_USB_SYNC_START.equals(action)) {
                mCp.addProgress();

            } else if (Constants.ACTION_USB_SYNCED.equals(action)) {
                mCp.removeProgress();

                boolean result = intent.getBooleanExtra(Constants.EXTRA_USB_SYNC_RESULT, true);
                if (result) {
                    AppController.getInstance().toast(getApplicationContext(), "USB-SYNCED", Toast.LENGTH_LONG);
                } else {
                    AppController.getInstance().toast(getApplicationContext(), "FAILED USB-SYNC", Toast.LENGTH_LONG);
                    String reason = intent.getExtras().getString(Constants.EXTRA_REASON_FOR_SYNC_FAILURE, "");
                    if(!TextUtils.isEmpty(reason)){
                        displayUsbSyncInfo(reason);
                    }
                }
            } else if (Intent.ACTION_MEDIA_REMOVED.equals(action) && intent.getData().toString().endsWith("0")){
                mSyncInfolayout.hideLayout();
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mContentVG == null) {
            Log.d(TAG, "NULL content view group.");
            return;
        }

        mContentVG.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

//        if (DBG) {
//            Log.d(TAG, "onResume, isPause=" + isPaused);
//        }

//        if (isPaused) {
//            isPaused = false;
//
//            final File file = AppController.getInstance().getModel().getFile();
//            if (file != null) {
//
//                if (DBG)
//                    Log.d(TAG, "We have previously a file playing and paused, now resume. file=" + file);
//
//                startProgram(file);
//            }
//        }
    }

//    private boolean isPaused = false;

    @Override
    protected void onPause() {
        super.onPause();
        if (DBG) {
            Log.d(TAG, "onPause.");
        }

//        isPaused = true;

        //stopProgramInternal();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mReceiver = new MainReceiver();
        mKb = new KeyBoardNav(this);

        if (DBG)
            Log.d(TAG, "BUILD id=" + Build.ID);
        // if (!Constants.TEST_MOBILE_DEVICE_ID.equals(Build.ID)) {
        // if (DBG)
        // Log.i(TAG, "onCreate. setup usb otg for the box only.");
        // new USBOtg();
        // }

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getActionBar().hide();
        // Force to black.
//        getWindow().getDecorView().setBackgroundColor(0xFF000000);

        // This init must be the first init. Otherwise, the others calling
        // getInst() w/o the context will fail.

        mContentVG = (ViewGroup) findViewById(android.R.id.content);


        mContentVG.setBackgroundResource(R.drawable.background_empty_content);

        mProgramsViewer = new ProgramsViewer(this);

        if (DBG) {
            Log.i(TAG, "onCreate. externalStorageDirectory=" + Environment.getExternalStorageDirectory()
                    + ", Environment.getExternalStorageState()=" + Environment.getExternalStorageState());
        }
        // Boolean isInternalSDPresent = new File(Constants.MNT_SDCARD).isDirectory();
        // if (isInternalSDPresent) {
        // if (!checkAndStartUsbPrograms()) {
        // startNetPrograms();
        // }

        if (DBG)
            Log.i(TAG, "onCreate. Present, /mnt/sdcard isSDPresent=" + new File(Constants.MNT_SDCARD).isDirectory());
        // } else {
        // Log.e(TAG, "onCreate. isSDPresent=" + isInternalSDPresent);
        // }

        registerUsbSyncEvents();
        mCp = new CopyProgress(this);
        mSyncInfolayout = new FloatingLayout(getApplicationContext());

        sendBroadcast(new Intent(Constants.ACTION_COLOR_HOME_STARTED));

        // register screen_on/off broadcast receiver

        mScreenStatusReceiver = new ScreenStatusReceiver();
        IntentFilter receiverFilter = new IntentFilter();
        receiverFilter.addAction(Intent.ACTION_SCREEN_ON);
        receiverFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStatusReceiver, receiverFilter);
    }

    class ScreenStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DBG)
                Log.d(TAG, "status receiver action=" + intent.getAction());
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                stopProgramInternal();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                final File file = AppController.getInstance().getModel().getFile();
                if (file != null) {
                    if (DBG)
                        Log.d(TAG, "screen on action received , previous file : " + file);
                    startProgram(file);
                }
            }
        }
    }

    private void displayUsbSyncInfo(String reason) {
        if (DBG) {
            Log.d(TAG, "displayUsbSyncInfo");
        }

        mSyncInfolayout.addView(LayoutInflater.from(this).inflate(R.layout.layout_usb_sync_info, null));
        TextView showUpdateStatusTextView = (TextView) mSyncInfolayout.findViewById(R.id.usbSyncInfoTextView);
        showUpdateStatusTextView.setText(reason);
        mSyncInfolayout.showLayout();
    }

    private void registerUsbSyncEvents() {
        IntentFilter intentFilter = new IntentFilter(Constants.ACTION_USB_SYNC_START);
        intentFilter.addAction(Constants.ACTION_USB_SYNCED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = Intent.makeMainActivity(new ComponentName("com.android.settings", "com.android.settings.Settings"));
            startActivity(intent);
        } else if (item.getItemId() == R.id.action_apps) {
            startAllApps();
            if (DBG)
                Log.i(TAG, "onOptionsItemSelected. manual download, Thread=" + Thread.currentThread());
            // listIpAddress();
        }

        return super.onOptionsItemSelected(item);
    }

    private void startAllApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        // adb shell am start -n com.android.launcher3/com.android.launcher3.Launcher
        intent.setComponent(new ComponentName("com.color.android.home", "com.color.android.home.Home"));
        startActivity(intent);
    }

    private boolean mFakeHomeStarted;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mFakeHomeStarted = false;
            if (DBG)
                Log.i(TAG, "onTouchEvent. ACTION_DOWN, showNext.");
            if (mProgramsViewer != null)
                mProgramsViewer.showNext();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (!mFakeHomeStarted && event.getEventTime() - event.getDownTime() > 800) {
                mFakeHomeStarted = true;
                startAllApps();
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (DBG)
            Log.d(TAG, "onConfigurationChanged. [newConfig=" + newConfig);
        super.onConfigurationChanged(newConfig);
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (DBG)
            Log.i(TAG, "onNewIntent. getIntent=" + getIntent() + ", data=" + intent.getData());

        final String action = intent.getAction();
        if (Constants.ACTION_PLAY_PROGRAM.equals(action)) {

            final String path = intent.getStringExtra(Constants.EXTRA_PATH);
            final String fileName = intent.getStringExtra(Constants.EXTRA_FILE_NAME);
            if (DBG)
                Log.i(TAG, "onNewIntent. path=" + path + ", fileName=" + fileName);

            if (TextUtils.isEmpty(fileName)) {
                stopProgram();
            } else {
                startProgram(generateVsnFile(false, path, fileName));
//                if(DBG)
//                    Log.d(TAG, "after startProgram onNewIntent: isPause=" + isPaused);
//                isPaused = false;
            }

        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (DBG)
            Log.d(TAG, "onKeyDown. [keyCode=" + keyCode + ", event=" + event);

        mKb.onKeyDown(keyCode, event);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // Do not call super.
        if (DBG)
            Log.i(TAG, "onBackPressed. Show/hide actionbar.");

        if (getActionBar().isShowing()) {
            getActionBar().hide();
        } else {
            getActionBar().show();
        }

    }

    @Override
    protected void onDestroy() {
        if (DBG)
            Log.d(TAG, "onDestroy. [");

        unregisterReceiver(mReceiver);
        unregisterReceiver(mScreenStatusReceiver);
        super.onDestroy();
    }


    private void stopProgramInternal() {
        if (DBG) {
            Log.d(TAG, "stopProgramInternal.");
        }
        // Stop program
        if (mProgramsViewer != null) {
            mProgramsViewer.setPrograms(null);
            mProgramsViewer.removeProgramView();
        }
    }

    private void stopProgram() {
        if (DBG)
            Log.d(TAG, "stopProgram. [");
        stopProgramInternal();

        AppController.getInstance().getModel().setCurProgramPathFile("", "");

        AppController.getInstance().getModel().setPrograms(null);
        AppController.getInstance().markProgram(null, null);
        // But in fact, its stopped.
        SyncService.startService(getApplicationContext(), Uri.parse("content://com.color.home/play/"), Constants.ACTION_PROGRAM_STARTED);
    }

    private void startProgram(File vsn) {
        if (DBG)
            Log.i(TAG, "startProgram. programVsnFile=" + vsn);

        // Mark in the SystemProperties the time and the vsn file we are trying to playback.
        // If later the Home is restart, and the interval is quite short,
        // Maybe we could not playback this program.
        new FailedProgramChecker(this, vsn); // Mark

        mProgramsViewer.parsePrograms(vsn);
    }

    public void onProgramStarted(File vsn) {
        // Must be before startProgram. Otherwise, the item data source's path will be the previous one.
        // Check AppController.getPlayingRootPath()'s usage.
        AppController.getInstance().getModel().setCurProgramPathFile(vsn);

        AppController.getInstance().getModel().setPrograms(mProgramsViewer.getPrograms());
        AppController.getInstance().markProgram(vsn);
        SyncService.startService(getApplicationContext(), Uri.fromFile(vsn), Constants.ACTION_PROGRAM_STARTED);
    }


    File generateVsnFile(boolean isNetwork, String path, String fileName) {
        return new File(path + "/" + fileName);
    }

}
