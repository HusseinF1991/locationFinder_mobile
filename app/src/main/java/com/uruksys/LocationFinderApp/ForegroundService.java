package com.uruksys.LocationFinderApp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Timer;
import java.util.TimerTask;

public class ForegroundService extends Service {

    private BroadcastReceiver gpsStatusMessageReceiver = null;


    public ForegroundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //make wakelock for huawei phones only
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M && Build.MANUFACTURER.toLowerCase().equals("huawei")) {
            String tag = "LocationManagerService";
            @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
            wakeLock.acquire();
        }


        //starting foreground  service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel("channel3", "myChanne3", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("ConnectionResponse");
            NotificationManager manager = (NotificationManager) getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(this, MapsActivity.class);

        @SuppressLint("WrongConstant") final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);



        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                Notification notification = new NotificationCompat.Builder(ForegroundService.this, "channel3")
                        .setSmallIcon(R.drawable.locationfinderlogo)
                        .setContentTitle("موقعي")
                        .setContentText("البرنامج يعمل بصورة صحيحة")
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .build();

                startForeground((int) System.currentTimeMillis() , notification);
                Log.d("timerForeGround" , "reached");
            }
        } ,0 , 1800000);

        GPS_StatusReceiver gps_statusReceiver = new GPS_StatusReceiver();
        this.registerReceiver(gps_statusReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));


        gpsStatusMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                boolean gpsStatus = intent.getBooleanExtra("GpsStatus" , false);
                if(gpsStatus){

                    Notification notification = new NotificationCompat.Builder(context, "channel3")
                            .setSmallIcon(R.drawable.locationfinderlogo)
                            .setContentTitle("موقعي")
                            .setContentText("البرنامج يعمل بصورة صحيحة")
                            .setContentIntent(pendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .build();

                    startForeground((int) System.currentTimeMillis() , notification);
                }
                else{


                    Notification notification = new NotificationCompat.Builder(context, "channel3")
                            .setSmallIcon(R.drawable.locationfinderlogo)
                            .setContentTitle("موقعي")
                            .setContentText( "نظام تحديد الموقع لديك مغلق , يرجى الضغط على الاشعار")
                            .setContentIntent(pendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .build();

                    startForeground((int) System.currentTimeMillis() , notification);
                }
            }
        };


        LocalBroadcastManager.getInstance(this).registerReceiver(
                gpsStatusMessageReceiver, new IntentFilter("MyGpsStatusChanged"));

    }



}
