package com.uruksys.LocationFinderApp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class GPS_StatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("GPSstatusChecking", "started");
        final LocationManager manager = (LocationManager) context.getSystemService( Context.LOCATION_SERVICE );
        if (manager.isProviderEnabled( LocationManager.GPS_PROVIDER) ) {



            //send  broadcast intent to be received by  ForegroundService
            Intent broadcastIntent = new Intent("MyGpsStatusChanged");
            broadcastIntent.putExtra("GpsStatus", true);
            LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
        }
        else
        {
            //makeNotification_GPS_Off(context);



            //send  broadcast intent to be received by  ForegroundService
            Intent broadcastIntent = new Intent("MyGpsStatusChanged");
            broadcastIntent.putExtra("GpsStatus", false);
            LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
        }
    }


    //give notification that GPS is off
    private void makeNotification_GPS_Off(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel("channel2", "myChanne2", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("gpsOff");
            NotificationManager manager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        String notificationContent = "نظام تحديد الموقع لديك مغلق , يرجى الضغط على الاشعار";

        //make notification click event
        Intent intent = new Intent(context, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity
                (context, 103, intent, PendingIntent.FLAG_CANCEL_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "channel2")
                .setContentTitle("تنبيه")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.locationfinderlogo)
                .setContentText(notificationContent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationContent))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // notificationId is a unique int for each notification that you must define

        //unique notification id that if the notification exist it will be updated not repeated
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }
}
