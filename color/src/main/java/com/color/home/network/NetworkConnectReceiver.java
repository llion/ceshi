package com.color.home.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;


public class NetworkConnectReceiver extends BroadcastReceiver{
    public  static final String TAG ="NetworkConnectReceiver";
    public  static boolean DBG = false;

    public NetworkObserver mNetworkObserver;

    public NetworkConnectReceiver(NetworkObserver networkObserver) {
        if (DBG)
            Log.d(TAG, "networkObserver=" + networkObserver);
        this.mNetworkObserver = networkObserver;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if (DBG){
            Log.i(TAG,"--------onReceive, thread=" + Thread.currentThread());
        }

//      监听网络连接(包括WiFi连接和移动数据的打开和关闭)，以及连接连接上可用的连接都可以监听到
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())){
//                获取网路状态
            NetworkInfo info=intent
                    .getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (info!=null){
//                    如果当前网络连接成功并且网络连接可用
                if (NetworkInfo.State.CONNECTED == info.getState() && info.isAvailable()){
                    if (DBG){
                        Log.i(TAG,"---------Internet connected.");
                        Log.i(TAG,"InternetType:" + info.getType() + " connected.");
                    }
                    if (mNetworkObserver != null) {
                        if (DBG){
                            Log.i(TAG,"------mNetworkObserver.reloadContent");
                        }

                        mNetworkObserver.reloadContent();
                    }
                }
            }else{
                if (DBG){
                    Log.i(TAG,"---------Internet disconnected.");
                }
            }
        }else {
            if (DBG){
                Log.i(TAG,"---------There is no Internet");
            }
        }
    }


}


