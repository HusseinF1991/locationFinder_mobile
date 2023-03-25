package com.uruksys.LocationFinderApp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent i = new Intent(context, LocationRequestService.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(i);


        //start service that works in foreground
        Intent foregroundServiceIntent = new Intent(context , ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(foregroundServiceIntent);
        }
        else{
            context.startService(foregroundServiceIntent);
        }

        //start SOS if it was enabled before phone was restarted
        SharedPreferences sharedPref = context.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        boolean sosStatus  = sharedPref.getBoolean("LocationFinderSosStatusSharedPrefereces", false);
        int sosIntervals = sharedPref.getInt("LocationFinderSosIntervalsSharedPrefereces", 10);
        if(sosStatus){

            AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            Intent SosIntent = new Intent(context, SosAlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, SosIntent, 0);
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * sosIntervals, pi); // Millisec * Second * Minute

        }
    }
}
