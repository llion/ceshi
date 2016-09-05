package com.color.home.program.sync;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by hmh on 2016/2/25.
 */
public class FailedProgramChecker {

    public static final String TRYING_VSN = "color.trying_vsn";
    public static final String TRYING_TIME = "color.trying_time";
    public static final String BAD_VSNS = "color.BadVSNS";
    public static final String BADVSN_TOTAL = "color.badvsn_total";
    private static final String TAG = "FailedProgramChecker";
    private static final boolean  DBG = false;
    public static final String ACTION_VSN_CHECKER = "com.color.player.ACTION_VSN_CHECKER";
    private final Context mContext;
//
    public FailedProgramChecker(Context context) {
        mContext = context;
    }

    public FailedProgramChecker(Context context, File vsn) {
        mContext = context;

//        SystemProperties.PROP_VALUE_MAX
        if (DBG) Log.d(TAG, "FailedProgramChecker, vsn=" + vsn);
        if (DBG) new Exception().printStackTrace();

//        ArrayList<String> commands = new ArrayList<String>();
//        commands.add("setprop " + TRYING_VSN + " '" + normal + "'");
//        commands.add("setprop " + TRYING_TIME + " " + String.valueOf(SystemClock.uptimeMillis()));
//        commands.add("iwconfig wlan0 power off");
//        if (DBG)
//            Log.d(TAG, "setup =" + displayMode + " [commands 0=" + commands.get(0));


        //SystemProperties.set(TRYING_VSN, normal);
        //SystemProperties.set(TRYING_TIME, String.valueOf(SystemClock.uptimeMillis()));

        final Intent origIntent = context.registerReceiver(null, new IntentFilter(ACTION_VSN_CHECKER));

        Intent intent = new Intent(ACTION_VSN_CHECKER);
        intent.putExtra(TRYING_VSN, vsn.toString() + vsn.lastModified());
        intent.putExtra(TRYING_TIME, SystemClock.uptimeMillis());

        // SAVE badVSNs array.
        if (origIntent != null) {
            if (DBG)
                Log.d(TAG, "There was origIntent = " + origIntent);

            ArrayList<String> badVSNs = origIntent.getStringArrayListExtra(BAD_VSNS);
            if (DBG)
                Log.d(TAG, "badVSNs=" + badVSNs);
            if (badVSNs != null)
                intent.putStringArrayListExtra(BAD_VSNS, badVSNs);
        }

        context.sendStickyBroadcast(intent);

        if (DBG) {
            final Intent intent1 = context.registerReceiver(null, new IntentFilter(ACTION_VSN_CHECKER));
            if (intent1 != null) {
                Log.d(TAG, "Sent sticky TRYING_VSN=" + intent1.getStringExtra(TRYING_VSN)
                        + ", TRYING_TIME=" + intent1.getLongExtra(TRYING_TIME, 0L));
            }
        }

    }


    public void check() {
        final long uptimeMillis = SystemClock.uptimeMillis();
        final Intent origIntent = mContext.registerReceiver(null, new IntentFilter(ACTION_VSN_CHECKER));
        if (origIntent == null) {
            Log.e(TAG, "No stick broadcast set, abort check.!!!");
            return;
        }

        final long lastUp = origIntent.getLongExtra(TRYING_TIME, 0L);

        if (DBG) Log.d(TAG, "check");

        if (uptimeMillis - lastUp < 1200L) {
            //C
            Log.e(TAG, "", new Exception("Home has restarted without the device rebooting."));
            final String tryingVsn = origIntent.getStringExtra(TRYING_VSN);
            ArrayList<String> badVSNs = origIntent.getStringArrayListExtra(BAD_VSNS);

            if (DBG)
                Log.d(TAG, "bad vsn, TRYING_VSN=" + tryingVsn
                + " badVSNs=" + badVSNs);

//            ArrayList<String> commands = new ArrayList<String>();
//            commands.add("setprop " + BADVSN_TOTAL + " " + String.valueOf(badvsn_total));
//            commands.add("setprop " + BAD_VSNS + (badvsn_total - 1) + " \"" + SystemProperties.get(TRYING_VSN) + "\"");

            Intent intent = new Intent(ACTION_VSN_CHECKER);
            if (badVSNs == null)
                badVSNs = new ArrayList<String>(1);
            // Add the bad vsn.
            if (! badVSNs.contains(tryingVsn))
                badVSNs.add(tryingVsn);

            intent.putStringArrayListExtra(BAD_VSNS, badVSNs);
            mContext.sendStickyBroadcast(intent);

            if (DBG) {
                final Intent intent2 = mContext.registerReceiver(null, new IntentFilter(ACTION_VSN_CHECKER));
                if (intent2 != null) {
                    Log.d(TAG, "check sticky set TRYING_VSN=" + intent2.getStringExtra(TRYING_VSN)
                            + ", TRYING_TIME=" + intent2.getLongExtra(TRYING_TIME, 0L)
                    );

                    for (String vsn : intent.getStringArrayListExtra(BAD_VSNS))
                        Log.d(TAG, "[ BAD VSN ]=" + vsn);
                }
            }
        }

    }


    public boolean okToPlay(File vsn) {
        if (DBG)
            Log.e(TAG, "okToPlay checking = " + vsn);

        final Intent intent = mContext.registerReceiver(null, new IntentFilter(ACTION_VSN_CHECKER));
        if (intent == null) {
            Log.e(TAG, "okToPlay no sticky broadcast. return ok.");
            return true;
        }

        ArrayList<String> badVSNs = intent.getStringArrayListExtra(BAD_VSNS);
        if(badVSNs == null) {
            if (DBG)
                Log.d(TAG, "OK to play = " + vsn + ", as there is no badVSNs.");
            return true;
        }

        String normal = vsn.toString() + vsn.lastModified();
        if (DBG)
            Log.d(TAG, "normalized = " + normal + "");

        for (String badvsn : badVSNs) {
            if (DBG)
                Log.d(TAG, "Bad vsn=" + badvsn);

            if (badvsn != null && badvsn.equals(normal)) {
                if (DBG) Log.d(TAG, "Found bad vsn=" + badvsn);
                return false;
            }
        }

        return true;
    }

    public void clear() {
        Log.e(TAG, "No reason to clear the bad vsns, abort.!!!");

    }

}