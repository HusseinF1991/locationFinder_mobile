package com.uruksys.LocationFinderApp;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PersistableBundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class RegCodeAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        final SharedPreferences sharedPref = context.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        int minRemained = sharedPref.getInt("LocationFinderRegCodeWaitSharedPrefereces", 0);

        SharedPreferences.Editor editor = sharedPref.edit();
        if (minRemained > 0) {

            editor.putInt("LocationFinderRegCodeWaitSharedPrefereces", minRemained - 1);
            editor.commit();
        } else {

            editor.putInt("LocationFinderRegCodeWaitSharedPrefereces", 0);
            editor.commit();
        }

        //send broadcast intent to the any receiver in application (RegisterActivity)
        Intent intent1 = new Intent("ResetRegCodeTries");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent1);
    }
}
