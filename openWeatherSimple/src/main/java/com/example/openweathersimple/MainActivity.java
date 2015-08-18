package com.example.openweathersimple;

import ru.gelin.android.weather.notification.AppUtils;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;

public class MainActivity extends Activity {
    private final static String TAG = "MainActivity";
    private static final boolean DBG = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (DBG)
            Log.i(TAG, "onTouchEvent. event, Thread=" + Thread.currentThread());

        if (MotionEvent.ACTION_DOWN == event.getAction())
            AppUtils.startUpdateService(getApplicationContext(), true, true);
        return true;
    }

}
