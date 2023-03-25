package com.uruksys.LocationFinderApp;

import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.PersistableBundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SmsJobScheduler extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {

        Log.d("smsjobScheduler" , "Sms job started");

        PersistableBundle pb =  params.getExtras();
        String targetPhone = (String) pb.get("targetPhone");
        String targetUserName = (String) pb.get("targetUserName");

        String notificationMsg = targetUserName+  " غير مرتبط بالانترنت هل ترغب بطلب الموقع عن طريق رسالة نصية؟"  ;

        //send broadcast intent to the any receiver in application (MapsActivity)
        Intent intent1 = new Intent("ProposeRequestBySms");
        intent1.putExtra("targetPhoneForSMS" , targetPhone);
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent1);


        //create notification
        Intent intent = new Intent(this , MapsActivity.class);
        intent.putExtra("targetPhoneForSMS" , targetPhone);
        PendingIntent pendingIntent = PendingIntent.getActivity
                (this , 100 , intent , PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel1")
                .setContentTitle("فشل في طلب الموقع")
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.locationfinderlogo)
                .setContentText(notificationMsg)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationMsg))
                .addAction(R.drawable.locationfinderlogo, "ارسل رسالة", pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());

        return false;
    }


    @Override
    public boolean onStopJob(JobParameters params) {

        Log.d("smsjobScheduler" , "Sms job stopped");
        return false;
    }
}
