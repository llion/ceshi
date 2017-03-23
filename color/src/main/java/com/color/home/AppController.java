package com.color.home;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.util.LruCache;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.color.home.netplay.Config;
import com.color.home.netplay.ConfigAPI;
import com.color.home.netplay.Connectivity;
import com.color.home.program.sync.PairedProgramFile;
import com.color.home.program.sync.PollingUtils;
import com.color.home.program.sync.Strategy;
import com.color.home.program.sync.SyncService;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

public class AppController extends Application {

    private final static String TAG = "AppController";
    private static final boolean  DBG = false;
    public static String sCharset = "GBK";
    public final static Object sLock = new Object();

    /**
     * Global request queue for Volley
     */
    private static Handler sHandler;
    static {
        final HandlerThread thread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        sHandler = new Handler(thread.getLooper());
    }

    public static class MyBitmap {
        private Bitmap mBitmap;

        public Bitmap getBitmap() {
            return mBitmap;
        }

        public int mSingleLineWidth;
        public int mSingleLineHeight;

        public MyBitmap(Bitmap bitmap, int singleLineWidth, int singleLineHeight) {
            mBitmap = bitmap;
            mSingleLineWidth = singleLineWidth;
            mSingleLineHeight = singleLineHeight;
        }

    }

    // Get max available VM memory, exceeding this amount will throw an
    // OutOfMemory exception. Stored in kilobytes as LruCache takes an
    // int in its constructor.
    static final int sMaxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    static {
        if (DBG)
            Log.d(TAG, "sMaxMemory=. [" + sMaxMemory);
    }
    // Use 1/3th of the available memory for this memory cache.
    // static final int sCacheSize = 500;
    static final int sCacheSize = sMaxMemory / 5;

    LruCache<String, MyBitmap> sMemoryCache = new LruCache<String, MyBitmap>(sCacheSize) {
        @Override
        protected int sizeOf(String key, MyBitmap mybitmap) {
            // The cache size will be measured in kilobytes rather than
            // number of items.
            return mybitmap.mBitmap.getByteCount() / 1024;
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, MyBitmap oldValue, MyBitmap newValue) {
            // Donot recycle here, as a bitmap could be using it.
            // if (oldValue != null)
            // oldValue.recycle();

            if (DBG)
                Log.d(TAG,
                        "entryRemoved. [reqest GC, oldValue=" + oldValue + ", newValue=" + newValue + ", Thread=" + Thread.currentThread());
            System.gc();

            super.entryRemoved(evicted, key, oldValue, newValue);
        }
    };
    /**
     * A singleton instance of the application class for easy access in other places
     */
    private static AppController sInstance;

    private Config mCfg;
    private Locale locale = null;

    private Connectivity mConnectivity;


    private Model mModel;
    private Strategy mStrategy;

    private File mDownloadDataDir;
    private SharedPreferences mSettings;

    private HashMap<String, Typeface> mTypefaces = new HashMap<String, Typeface>();

    public SharedPreferences getSettings() {
        return mSettings;
    }

    public Typeface generateTypeface(String fontName) {
        try {
            if(DBG)
                Log.d(TAG, "generate typeface by fontName:" + fontName);
            if (Constants.FONT_KAI.equals(fontName)) {
                return Typeface.createFromFile(Constants.FONT_PATH + "simkai.ttf");
            } else if (Constants.FONT_HEI.equals(fontName)) {
                return Typeface.createFromFile(Constants.FONT_PATH + "simhei.ttf");
            } else if (Constants.FONT_FANGSONG.equals(fontName)) {
                return Typeface.createFromFile(Constants.FONT_PATH + "simfang.ttf");
            } else if (Constants.FONT_SONG.equals(fontName)) {
                return Typeface.createFromFile(Constants.FONT_PATH + "simsun.ttf");
            } else if (Constants.FONT_LISHU.equals(fontName)) {
                return Typeface.createFromFile(Constants.FONT_PATH + "simli.ttf");
            }
        } catch (Exception e) {
            Log.e(TAG, "generateTypeface. [fontName=" + fontName, e);
        }
        return null;
    }

    public void addBitmapToMemoryCache(String key, MyBitmap bitmap) {
        if (DBG)
            Log.d(TAG, "addBitmapToMemoryCache. [key=" + key + ", bitmap=" + bitmap);
        if (getBitmapFromMemCache(key) == null) {
            sMemoryCache.put(key, bitmap);
        }
    }

    public MyBitmap getBitmapFromMemCache(String key) {
        if (DBG)
            Log.d(TAG, "getBitmapFromMemCache. [key=" + key + ", bitmap=" + sMemoryCache.get(key));

        return sMemoryCache.get(key);
    }

    public void markProgram(File vsn) {
        markProgram(vsn.getParentFile().getAbsolutePath(), vsn.getName());
    }

    public void markProgram(String path, String fileName) {
        PairedProgramFile file = new PairedProgramFile(path, fileName);

        Intent intent = new Intent(Constants.ACTION_CURRENT_PROG_INDEX);
        intent.putExtra(Constants.EXTRA_INDEX, 999);
        intent.putExtra(Constants.EXTRA_PATH, file.getPath());
        intent.putExtra(Constants.EXTRA_FILE_NAME, file.getFilename());
        sendStickyBroadcast(intent);

        Editor edit = getSettings().edit();
        edit.putString(Constants.EXTRA_PATH, file.getPath());
        edit.putString(Constants.EXTRA_FILE_NAME, file.getFilename());
        edit.apply();

//        ContentValues values = new ContentValues();
//        values.put(ColorContract.COLUMN_PROGRAM_SOURCE, "updated path");
//        values.put(ColorContract.COLUMN_PROGRAM_SOURCE, Constants.absFolderToSourceType(pathNorm));
//        values.put(ColorContract.COLUMN_PROGRAM_PATH, pathNorm);
//        values.put(ColorContract.COLUMN_PROGRAM_FILENAME, fileNameNorm);
//        int update = getContentResolver()
//                .update(Uri.withAppendedPath(ColorContract.PROGRAM_CONTENT_URI, "playing"), values, null, null);
//        if (DBG)
//            Log.i(TAG, "markProgram. [PROGRAM_CONTENT_URI update count=" + update);

    }

    public static final String LOG_TYPE_PROGRAM_RES_MISSING = "log.type.program.missing_resource";
    public static final String LOG_TYPE_UPDATING = "log.type.failed_updating";
    public static final String LOG_TYPE_CONNECTIVITY = "log.type.connectivity";
    public static final String LOG_TYPE_DEVICE = "log.type.device";
    public static final String LOG_TYPE_BAD_OPERATION = "log.type.operation.bad_operation";
    public static final String LOG_TYPE_START_PLAYING = "log.type.program.start_playing";
    public static final String LOG_TYPE_ETHERNET_CONFIGURED = "log.type.connectivity.lan.ethernet_configured";
    public static final String LOG_TYPE_WIFI_CONFIGURED = "log.type.connectivity.wifi.wifi_configured";

    public void reportInternetLog(String log_type, String description, int level, String others, String... args){
        Intent intent = new Intent(Constants.ACTION_LOG_REPORTING);
        intent.putExtra("log_type", log_type);
        intent.putExtra("description", description);
        intent.putExtra("level", level);
        intent.putExtra("others", others);
        intent.putExtra("args", args);

        sendBroadcast(intent);
    }

    public static String normPathNameInternalSdToSdcard(String pathNorm) {
        return pathNorm.replace("/mnt/internal_sd/", "/mnt/sdcard/");
    }

    public Typeface getTypeface(String fontName) {
        Typeface tf = null;
        if (mTypefaces.containsKey(fontName)) {
            tf = mTypefaces.get(fontName);
        } else {
            tf = generateTypeface(fontName);
            if (tf != null)
                mTypefaces.put(fontName, tf);
        }
        return tf;
    }

    public Connectivity getConnectivity() {
        return mConnectivity;
    }

    public boolean isProgramToastShowing;
    @Override
    public void onCreate() {
        super.onCreate();

        if (DBG)
            Log.d(TAG, "onCreate. [sMaxMemory=" + sMaxMemory);
        // initialize the singleton
        sInstance = this;
//        initToastGroup();

        toast(this, BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")", Toast.LENGTH_SHORT);

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration config = getBaseContext().getResources().getConfiguration();
        String lang = mSettings.getString(ConfigAPI.ATTR_LOCALE, "");
        sCharset = mSettings.getString(ConfigAPI.ATTR_TEXT_CHARSET, "GBK");

        if (!"".equals(lang) && !config.locale.getLanguage().equals(lang))
        {
            locale = new Locale(lang);
            Locale.setDefault(locale);
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        }

        sHandler.postAtFrontOfQueue(new Runnable() {

            @Override
            public void run() {
                if (DBG)
                    Log.d(TAG, "run what's posted at front. [postAtFrontOfQueue.");

                mCfg = new Config(AppController.this, Constants.FOLDER_USB_0);
                mConnectivity = new Connectivity(AppController.this);

            }
        });

        ensureFtpServer();


        mModel = new Model();
        mStrategy = new Strategy();

        mDownloadDataDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (mDownloadDataDir != null) {
            mDownloadDataDir.mkdirs();
        } else {
            Log.d(TAG, "onCreate. [mDownloadDataDir is NULL.");
        }

        isProgramToastShowing = Settings.Global.getInt(getContentResolver(), "showToast", 1) == 1;
    }

    public void ensureFtpServer() {
        scheduleEnsureFTP(mFtpFacilities, 3000);
    }

    private void scheduleEnsureFTP(Runnable runnable, int delayMillis) {
        sHandler.removeCallbacks(runnable);
        sHandler.postDelayed(runnable, delayMillis);
    }

    private Runnable mFtpFacilities = new Runnable() {
        @Override
        public void run() {
            if (!ensureFtpRootAndProgramPath()) {
                Log.w(TAG, "Schedule another scheduleEnsureFTP.");
                scheduleEnsureFTP(this, 5000);
            } else {
                // SystemProperties.set("ftpd.reset", "1");
                // Need not reset ftpd, as the FTPD'll only validate the path on connected.
                Log.d(TAG, "Ensured FTP path.");
            }
        }
    };

    private boolean ensureFtpRootAndProgramPath() {
        Log.d(TAG, "ensureFtpRootAndProgramPath.");

        File ftpDir = new File(Constants.FTP_PROGRAM_PATH);
        if (!ftpDir.isDirectory()) {
            if (!ftpDir.mkdirs()) {
                Log.e(TAG, "FtpServer. [Cannot make dir:" + ftpDir);

                return false;
            }
            Log.e(TAG, "FtpServer. [Not exist, FTP dir:" + ftpDir);
            return false;
        }

        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (locale != null)
        {
            newConfig.locale = locale;
            Locale.setDefault(locale);
            getBaseContext().getResources().updateConfiguration(newConfig, getBaseContext().getResources().getDisplayMetrics());
        }
    }

    public Strategy getStrategy() {
        return mStrategy;
    }

    public File getDownloadDataDir() {
        return mDownloadDataDir;
    }

    public Model getModel() {
        return mModel;
    }

    /**
     * @return ApplicationController singleton instance
     */
    public static synchronized AppController getInstance() {
        return sInstance;
    }



    public Config getCfg() {
        return mCfg;
    }

    public static Handler getHandler() {
        return sHandler;
    }

    @Override
    public void onTerminate() {
        if (DBG)
            Log.d(TAG, "onTerminate. [");

        if (com.color.home.Constants.HTTP_SERVER_SUPPORT) {
            PollingUtils.stopPollingService(this, SyncService.class, Constants.ACTION_REFRESH);
        }
        super.onTerminate();
    }

    public static String getPlayingRootPath() {
        return getInstance().getModel().getPath();
    }

    private boolean mActivityStarted;

    public synchronized void setStarted() {
        mActivityStarted = true;
    }

    public synchronized boolean isStarted() {
        return mActivityStarted;
    }

    public void toast(final Context context, final String text, final int duration) {
        if(!isProgramToastShowing)
            return;
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            toastMe(context, text, duration);
        } else {
            sHandler.post(new Runnable() {

                @Override
                public void run() {
                    toastMe(context, text, duration);
                }
            });
        }
    }

    public void toast(final Context context, final String text, final int duration, final int textColor) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            toastMe(context, text, duration, textColor);
        } else {
            sHandler.post(new Runnable() {
                @Override
                public void run() {
                    toastMe(context, text, duration, textColor);
                }
            });
        }
    }

    private void toastMe(Context context, String text, final int duration) {
        toastMe(context, text, duration, Color.WHITE);
    }

    private void toastMe(Context context, String text, final int duration, final int textColor) {
        Toast cheatSheet = new Toast(context);
        TextView textView = new TextView(context);
        textView.getPaint().setAntiAlias(false);
        textView.setBackgroundColor(Color.BLACK);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(textColor);
        cheatSheet.setView(textView);
        cheatSheet.setDuration(duration);
        cheatSheet.setGravity(Gravity.TOP | Gravity.LEFT, 0, 0);
        cheatSheet.show();
    }

//    LinearLayout mToastGroup;
//    private void toastMe(Context context, String text, int duration) {
//        duration = duration == Toast.LENGTH_LONG ? 3500 : 2000;
//        if(DBG)
//            Log.d(TAG, "toast text : " + text + ", duration : " + duration );
//        ToastTextView toastTextView = new ToastTextView(context, duration);
//        toastTextView.setBackgroundColor(Color.BLACK);
//        toastTextView.setText(text);
//        toastTextView.setTextSize(16);
//        toastTextView.setTextColor(Color.WHITE);
//
//        mToastGroup.addView(toastTextView);
//
//    }
//
//    private void toastMe(Context context, String text, int duration, final int textColor) {
//        duration = duration == Toast.LENGTH_LONG ? 3500 : 2000;
//        if(DBG)
//            Log.d(TAG, "toast text : " + text + ", duration : " + duration + ", textColor : " + textColor);
//        ToastTextView toastTextView = new ToastTextView(context, duration);
//        toastTextView.setBackgroundColor(Color.BLACK);
//        toastTextView.setText(text);
//        toastTextView.setTextSize(16);
//        toastTextView.setTextColor(textColor);
//
//        mToastGroup.addView(toastTextView);
//    }

//    private void initToastGroup() {
//        WindowManager.LayoutParams lp =
//                new WindowManager.LayoutParams();
//
//        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
//        lp.format = PixelFormat.TRANSPARENT;
//
//        lp.gravity = Gravity.LEFT | Gravity.TOP;
//        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
//        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
//        lp.x = 0;
//        lp.y = 0;
//
//        lp.flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
//
//        mToastGroup = new LinearLayout(getApplicationContext());
//        setupTransition(mToastGroup);
////        mToastGroup.setLayoutParams(lp);
////        mToastGroup.setVisibility(View.VISIBLE);
////        mToastGroup.setGravity(Gravity.TOP | Gravity.START);
//        mToastGroup.setOrientation(LinearLayout.VERTICAL);
//
//        if(DBG)
//            Log.d(TAG, "is Toast Group shown : " + mToastGroup.isShown() +
//                    ", layout visibility : " + mToastGroup.getVisibility());
//
//        WindowManager wm =
//                (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//        if (wm == null) {
//            return;
//        }
//
//        wm.addView(mToastGroup, lp);
//    }
//
//    private void setupTransition(ViewGroup viewGroup){
//        final LayoutTransition transition = new LayoutTransition();
//        if(DBG)
//            Log.d(TAG, "APPEARING " + transition.isTransitionTypeEnabled(LayoutTransition.APPEARING)
//                     + ", DISAPPEARING " + transition.isTransitionTypeEnabled(LayoutTransition.DISAPPEARING)
//                     + ", CHANGE_APPEARING " + transition.isTransitionTypeEnabled(LayoutTransition.CHANGE_APPEARING)
//                     + ", CHANGE_DISAPPEARING " + transition.isTransitionTypeEnabled(LayoutTransition.CHANGE_DISAPPEARING)
//            );
//        transition.enableTransitionType(LayoutTransition.APPEARING);
//        transition.enableTransitionType(LayoutTransition.DISAPPEARING);
//        transition.enableTransitionType(LayoutTransition.CHANGE_APPEARING);
//        transition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
//        viewGroup.setLayoutTransition(transition);
//        transition.setAnimator(LayoutTransition.APPEARING, transition.getAnimator(LayoutTransition.APPEARING));
//        transition.setAnimator(LayoutTransition.DISAPPEARING, transition.getAnimator(LayoutTransition.DISAPPEARING));
//        transition.setAnimator(LayoutTransition.CHANGE_APPEARING, transition.getAnimator(LayoutTransition.CHANGE_APPEARING));
//        transition.setAnimator(LayoutTransition.CHANGE_DISAPPEARING, transition.getAnimator(LayoutTransition.CHANGE_DISAPPEARING));
//    }

}
