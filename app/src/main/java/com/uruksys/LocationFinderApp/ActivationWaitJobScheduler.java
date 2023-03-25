package com.uruksys.LocationFinderApp;

import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ActivationWaitJobScheduler extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {

        SharedPreferences sharedPref = this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("LocationFinderWaitActivationSmsSharedPrefereces", false);
        editor.commit();

        Toast.makeText(this, "فشل في التفعيل , حاول مرة اخرى", Toast.LENGTH_SHORT).show();

        //send broadcast intent to the any receiver in application (MapsActivity)
        Intent intent = new Intent("WaitActivationConfirmSms");
        intent.putExtra("smsReceived" , false);
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
