package com.uruksys.LocationFinderApp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String TAG = NetworkChangeReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean status = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        Log.e(TAG, "network connected = " + status + " " + activeNetwork);

        if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {

            Intent intent2 = new Intent("networkChangeReceiver");
            intent2.putExtra("isThereConnection", status);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent2);
        }
    }
}
