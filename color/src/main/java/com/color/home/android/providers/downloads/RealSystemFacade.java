/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.color.home.android.providers.downloads;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

class RealSystemFacade implements SystemFacade {
    // never public, so that another class won't be messed up.
    private final static String TAG = "RealSystemFacade";
    private static final boolean DBG = false;
    private Context mContext;

    public RealSystemFacade(Context context) {
        mContext = context;
    }

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public NetworkInfo getActiveNetworkInfo(int uid) {
        ConnectivityManager connectivity = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            Log.w(Constants.TAG, "couldn't get connectivity manager");
            return null;
        }

        final NetworkInfo activeInfo = connectivity.getActiveNetworkInfo();
        // final NetworkInfo activeInfo =
        // connectivity.getActiveNetworkInfoForUid(uid);
        if (activeInfo == null && Constants.LOGVV) {
            Log.v(Constants.TAG, "network is not available");
        }

        if (DBG)
            Log.i(TAG, "getActiveNetworkInfo. activeInfo=" + activeInfo);

        return activeInfo;
    }

    //
    // @Override
    // public boolean isActiveNetworkMetered() {
    // final ConnectivityManager conn = ConnectivityManager.from(mContext);
    // return conn.isActiveNetworkMetered();
    // }
    //
    // @Override
    // public boolean isNetworkRoaming() {
    // ConnectivityManager connectivity =
    // (ConnectivityManager)
    // mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    // if (connectivity == null) {
    // Log.w(Constants.TAG, "couldn't get connectivity manager");
    // return false;
    // }
    //
    // NetworkInfo info = connectivity.getActiveNetworkInfo();
    // boolean isMobile = (info != null && info.getType() ==
    // ConnectivityManager.TYPE_MOBILE);
    // boolean isRoaming = isMobile &&
    // TelephonyManager.getDefault().isNetworkRoaming();
    // if (Constants.LOGVV && isRoaming) {
    // Log.v(Constants.TAG, "network is roaming");
    // }
    // return isRoaming;
    // }

    @Override
    public Long getMaxBytesOverMobile() {
        return Long.MAX_VALUE;
    }

    @Override
    public Long getRecommendedMaxBytesOverMobile() {
        return Long.MAX_VALUE;
    }

    @Override
    public void sendBroadcast(Intent intent) {
        mContext.sendBroadcast(intent);
    }

    @Override
    public boolean userOwnsPackage(int uid, String packageName) throws NameNotFoundException {
        return mContext.getPackageManager().getApplicationInfo(packageName, 0).uid == uid;
    }

    // @Override
    // public NetworkInfo getActiveNetworkInfo(int uid) {
    // // TODO Auto-generated method stub
    // return null;
    // }

    @Override
    public boolean isActiveNetworkMetered() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isNetworkRoaming() {
        // TODO Auto-generated method stub
        return false;
    }
}
