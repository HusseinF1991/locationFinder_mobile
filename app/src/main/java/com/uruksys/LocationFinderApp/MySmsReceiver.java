package com.uruksys.LocationFinderApp;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MySmsReceiver extends BroadcastReceiver {

    private static final String TAG = MySmsReceiver.class.getSimpleName();
    public static final String pdu_type = "pdus";

    private Boolean gps_Flag = false;

    LocationCallback locationCallback;
    private double lat = 0;
    private double lng = 0;

    int getLocationTryNumber = 1;

    private String myPhoneNumber;
    private int simId;
    private Context myContext;

    private FusedLocationProviderClient fusedLocationClient;


    @Override
    public void onReceive(Context context, Intent intent) {

        myContext = context;

        SharedPreferences sharedPref = context.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        myPhoneNumber = sharedPref.getString("LocationFinderPhoneSharedPrefereces", "notRegistered");
        simId = sharedPref.getInt("LocationFinderSimIdSharedPrefereces", 0);

        Log.d(TAG, "checkMsgReceived : true");

        // Get the SMS message.
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs;
        String strMessage_Sender = "";
        String strMessage_Body = "";
        String strMessage_DisplayedBody = "";
        String format = bundle.getString("format");


        // Retrieve the SMS message received.
        Object[] pdus = (Object[]) bundle.get(pdu_type);

        if (pdus != null) {

            //Telephony.Sms.Intents.getMessagesFromIntent(intent);

            // Fill the msgs array.
            msgs = new SmsMessage[pdus.length];
            for (int i = 0; i < msgs.length; i++) {
                // Check Android version and use appropriate createFromPdu.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // If Android version M or newer:
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                } else {
                    // If Android version L or older:
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }

                // Build the message to show.
                strMessage_Sender = msgs[i].getOriginatingAddress();
                strMessage_Body = strMessage_Body + msgs[i].getMessageBody();
                strMessage_DisplayedBody = strMessage_DisplayedBody + msgs[i].getDisplayMessageBody();

                Log.d(TAG, strMessage_Sender + " , messagebody : " + strMessage_DisplayedBody);
            }
            // Log and display the SMS message.
            Log.d(TAG, strMessage_Sender + " , messagebody : " + strMessage_Body);
            Log.d(TAG, strMessage_Sender + " , displayed body : " + strMessage_DisplayedBody);

            //trim the +964 prefix of the sender number
            if(strMessage_Sender.startsWith("+964")){

                strMessage_Sender = strMessage_Sender.substring(4, strMessage_Sender.length());
                strMessage_Sender = "0" + strMessage_Sender;

                Log.d(TAG, strMessage_Sender + " , " + strMessage_Body);
            }

            if (strMessage_Body.equals("$LocationFinderNeedsLatAndLng$")) {

                //check if the sender is allowed to request location from this user(have connection) from sqlite DB
                MySqliteDB mySqliteDB = new MySqliteDB(context);

                Cursor c = mySqliteDB.GetAllConnections(strMessage_Sender, myPhoneNumber);

                Log.d(TAG, "checkUsersConnected");
                if (c.getCount() > 0) {

                    Log.d(TAG, "UsersAreConnected");
                    String connectionStatus = null;
                    String accessToPhone = null;

                    DateFormat df = new SimpleDateFormat("HH:mm");
                    Date connectTime_From = null;
                    Date connectTime_To = null;
                    Date myDate = Calendar.getInstance().getTime();

                    try {

                        if (c.moveToFirst()) {
                            do {
                                connectionStatus = c.getString(c.getColumnIndex("ConnectionStatus"));
                                connectTime_From = new SimpleDateFormat("HH:mm").parse(c.getString(c.getColumnIndex("ConnectTime_From")));
                                connectTime_To = new SimpleDateFormat("HH:mm").parse(c.getString(c.getColumnIndex("ConnectTime_To")));
                                Log.d(TAG, connectionStatus);


                            } while (c.moveToNext());
                        }
                        c.close();

                        String myDateStr = df.format(myDate);
                        myDate = df.parse(myDateStr);
                        Log.d(TAG, myDateStr);

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    if (connectionStatus != null && connectionStatus.equals("Connected") && myDate.before(connectTime_To) && myDate.after(connectTime_From)) {

                        Log.d(TAG, "call_getCurrentLocation");
                        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
                        getCurrentLocation(strMessage_Sender, context);
                    }
                }
            } else if (strMessage_Body.startsWith("$Coordinates: ")) {

                //check connection with the sender
                MySqliteDB mySqliteDB = new MySqliteDB(context);
                Cursor c = mySqliteDB.GetAllConnections(myPhoneNumber, strMessage_Sender);

                if (c.getCount() > 0) {
                    String connectionStatus = null;
                    String accessToUserName = null;

                    if (c.moveToFirst()) {
                        do {
                            accessToUserName = c.getString(c.getColumnIndex("AccessToUserName"));
                            connectionStatus = c.getString(c.getColumnIndex("ConnectionStatus"));

                        } while (c.moveToNext());
                    }
                    c.close();

                    if (connectionStatus != null && connectionStatus.equals("Connected")) {


                        //msg format ($Coordinates: http://maps.google.com/maps?f=q&q=33.1234567,33.1234567&z=16)
                        String[] msgBody = strMessage_Body.split(",", 2);
                        String lat = "";
                        String lng = "";

                        int equalCounts = 0;
                        for (char ch : msgBody[0].toCharArray()) {
                            if (equalCounts == 2) {
                                lat = lat + ch;
                                continue;
                            }
                            if (ch == '=') equalCounts++;
                        }


                        boolean requireMore = true;
                        for (char ch : msgBody[1].toCharArray()) {
                            if (ch == '&') {
                                requireMore = false;
                            }
                            if (requireMore) lng = lng + ch;
                        }

                        Log.d(TAG, "lat " + lat + " lng " + lng);

                        makeNotification_LocReceived(Double.parseDouble(lat), Double.parseDouble(lng), accessToUserName);
                    }
                }
            } else if (strMessage_Body.equals("$نظام تحديد الموقع مغلق$")) {

                //check connection with the sender
                MySqliteDB mySqliteDB = new MySqliteDB(context);
                Cursor c = mySqliteDB.GetAllConnections(myPhoneNumber, strMessage_Sender);

                if (c.getCount() > 0) {
                    String connectionStatus = null;
                    String accessToUserName = null;

                    if (c.moveToFirst()) {
                        do {
                            accessToUserName = c.getString(c.getColumnIndex("AccessToUserName"));
                            connectionStatus = c.getString(c.getColumnIndex("ConnectionStatus"));

                        } while (c.moveToNext());
                    }
                    c.close();

                    if (connectionStatus != null && connectionStatus.equals("Connected")) {

                        //send  broadcast intent to be received by  MapsActivity
                        Intent intent1 = new Intent("TargetGPS_Off");
                        intent1.putExtra("msg", " نظام تحديد الموقع غير مفعل عند " + accessToUserName);
                        LocalBroadcastManager.getInstance(myContext).sendBroadcast(intent1);

                        makeNotification_LocRequestFailed(accessToUserName);
                    }
                }
            } else if (strMessage_DisplayedBody.startsWith("قمت بتحويل 5000دينار للرقم9647818650366بنجاح")) {

                if (!strMessage_Sender.startsWith("07") && !strMessage_Sender.startsWith("00964") && !strMessage_Sender.startsWith("964")) {

                    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                    cal.add(Calendar.MONTH, 12);
                    String newExpDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.getTime());

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("LocationFinderWaitActivationSmsSharedPrefereces", false);
                    editor.putString("LocationFinderExpDateSharedPrefereces", newExpDate);
                    editor.commit();

                    //send broadcast intent to the any receiver in application (MapsActivity)
                    Intent intent1 = new Intent("WaitActivationConfirmSms");
                    intent.putExtra("smsReceived", true);
                    LocalBroadcastManager.getInstance(myContext).sendBroadcast(intent1);

                    SetExpirationDate();

                    makeNotification_AppActivated(newExpDate);
                }

            } else if (strMessage_DisplayedBody.contains(" د ارسلت من رصيدك الى 7710464594 في")) {


                Log.d(TAG , "activationProcess : transfer confirmation received");
                if (!strMessage_Sender.startsWith("07") && !strMessage_Sender.startsWith("00964") && !strMessage_Sender.startsWith("964")) {

                    int amountTransferred = Integer.parseInt(strMessage_Body.substring(0 , 4));

                    int oldTransferredAmount = sharedPref.getInt("LocationFinderAmountTransferredToActivateSharedPrefereces", 0);
                    if(oldTransferredAmount + amountTransferred == 5000){

                        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                        cal.add(Calendar.MONTH, 12);
                        String newExpDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.getTime());

                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("LocationFinderWaitActivationSmsSharedPrefereces", false);
                        editor.putString("LocationFinderExpDateSharedPrefereces", newExpDate);
                        editor.putInt("LocationFinderAmountTransferredToActivateSharedPrefereces", 0);
                        editor.commit();

                        //send broadcast intent to the any receiver in application (MapsActivity)
                        Intent intent1 = new Intent("WaitActivationConfirmSms");
                        intent.putExtra("smsReceived", true);
                        LocalBroadcastManager.getInstance(myContext).sendBroadcast(intent1);

                        SetExpirationDate();

                        makeNotification_AppActivated( " تم التفعيل لغاية " + newExpDate);
                    }
                    else if(oldTransferredAmount + amountTransferred < 5000) {

                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("LocationFinderWaitActivationSmsSharedPrefereces", false);
                        editor.putInt("LocationFinderAmountTransferredToActivateSharedPrefereces", oldTransferredAmount + amountTransferred);
                        editor.commit();

                        //send broadcast intent to the any receiver in application (MapsActivity)
                        Intent intent1 = new Intent("WaitActivationConfirmSms");
                        intent.putExtra("smsReceived", true);
                        LocalBroadcastManager.getInstance(myContext).sendBroadcast(intent1);

                        int totalTransferred = oldTransferredAmount + amountTransferred;
                        int amountRemained = 5000 - (oldTransferredAmount + amountTransferred);
                        makeNotification_AppActivated( " تم تحويل  " + totalTransferred + " دينار عراقي , المتبقي " + amountRemained + " لاكمال عملية التفعيل");
                    }
                    else if(oldTransferredAmount + amountTransferred > 5000){

                        int changeAmount = (oldTransferredAmount + amountTransferred) - 5000;

                        Log.d(TAG , "activationProcess : amount transferred " +(oldTransferredAmount + amountTransferred));
                        int numOfMonths = ((changeAmount/1000) * 2) + 12;
                        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                        cal.add(Calendar.MONTH , numOfMonths);

                        Log.d(TAG , "activationProcess : new exp date " +cal.getTime());

                        String newExpDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.getTime());

                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("LocationFinderWaitActivationSmsSharedPrefereces", false);
                        editor.putString("LocationFinderExpDateSharedPrefereces", newExpDate);
                        editor.putInt("LocationFinderAmountTransferredToActivateSharedPrefereces", 0);
                        editor.commit();

                        //send broadcast intent to the any receiver in application (MapsActivity)
                        Intent intent1 = new Intent("WaitActivationConfirmSms");
                        intent.putExtra("smsReceived", true);
                        LocalBroadcastManager.getInstance(myContext).sendBroadcast(intent1);

                        SetExpirationDate();

                        makeNotification_AppActivated( " تم التفعيل لغاية " + newExpDate);

                    }
                }
            }
            else if(strMessage_DisplayedBody.contains("تم تعبئة رصيد 7519602547 ب")){

                Log.d(TAG , "activationProcess : transfer confirmation received");
                if (!strMessage_Sender.startsWith("07") && !strMessage_Sender.startsWith("00964") && !strMessage_Sender.startsWith("964")) {

                    int amountTransferred ;
                    if(strMessage_Body.contains("1000 د.ع")) amountTransferred = 1000;
                    else if(strMessage_Body.contains("2000 د.ع")) amountTransferred = 2000;
                    else if(strMessage_Body.contains("3000 د.ع")) amountTransferred = 3000;
                    else if(strMessage_Body.contains("4000 د.ع")) amountTransferred = 4000;
                    else if(strMessage_Body.contains("5000 د.ع")) amountTransferred = 5000;
                    else if(strMessage_Body.contains("6000 د.ع")) amountTransferred = 6000;
                    else if(strMessage_Body.contains("7000 د.ع")) amountTransferred = 7000;
                    else if(strMessage_Body.contains("8000 د.ع")) amountTransferred = 8000;
                    else amountTransferred = 9000;

                    int oldTransferredAmount = sharedPref.getInt("LocationFinderAmountTransferredToActivateSharedPrefereces", 0);
                    if(oldTransferredAmount + amountTransferred == 5000){

                        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                        cal.add(Calendar.MONTH, 12);
                        String newExpDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.getTime());

                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("LocationFinderWaitActivationSmsSharedPrefereces", false);
                        editor.putString("LocationFinderExpDateSharedPrefereces", newExpDate);
                        editor.putInt("LocationFinderAmountTransferredToActivateSharedPrefereces", 0);
                        editor.commit();

                        //send broadcast intent to the any receiver in application (MapsActivity)
                        Intent intent1 = new Intent("WaitActivationConfirmSms");
                        intent.putExtra("smsReceived", true);
                        LocalBroadcastManager.getInstance(myContext).sendBroadcast(intent1);

                        SetExpirationDate();

                        makeNotification_AppActivated( " تم التفعيل لغاية " + newExpDate);
                    }
                    else if(oldTransferredAmount + amountTransferred < 5000) {

                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("LocationFinderWaitActivationSmsSharedPrefereces", false);
                        editor.putInt("LocationFinderAmountTransferredToActivateSharedPrefereces", oldTransferredAmount + amountTransferred);
                        editor.commit();

                        //send broadcast intent to the any receiver in application (MapsActivity)
                        Intent intent1 = new Intent("WaitActivationConfirmSms");
                        intent.putExtra("smsReceived", true);
                        LocalBroadcastManager.getInstance(myContext).sendBroadcast(intent1);

                        int totalTransferred = oldTransferredAmount + amountTransferred;
                        int amountRemained = 5000 - (oldTransferredAmount + amountTransferred);
                        makeNotification_AppActivated( " تم تحويل  " + totalTransferred + " دينار عراقي , المتبقي " + amountRemained + " لاكمال عملية التفعيل");
                    }
                    else if(oldTransferredAmount + amountTransferred > 5000){

                        int changeAmount = (oldTransferredAmount + amountTransferred) - 5000;

                        Log.d(TAG , "activationProcess : amount transferred " +(oldTransferredAmount + amountTransferred));
                        int numOfMonths = ((changeAmount/1000) * 2) + 12;
                        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                        cal.add(Calendar.MONTH , numOfMonths);

                        Log.d(TAG , "activationProcess : new exp date " +cal.getTime());

                        String newExpDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.getTime());

                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("LocationFinderWaitActivationSmsSharedPrefereces", false);
                        editor.putString("LocationFinderExpDateSharedPrefereces", newExpDate);
                        editor.putInt("LocationFinderAmountTransferredToActivateSharedPrefereces", 0);
                        editor.commit();

                        //send broadcast intent to the any receiver in application (MapsActivity)
                        Intent intent1 = new Intent("WaitActivationConfirmSms");
                        intent.putExtra("smsReceived", true);
                        LocalBroadcastManager.getInstance(myContext).sendBroadcast(intent1);

                        SetExpirationDate();

                        makeNotification_AppActivated( " تم التفعيل لغاية " + newExpDate);

                    }
                }
            }
        }
    }


    //make notification and broadcast for the received location
    private void makeNotification_LocReceived(double lat, double lng, String userName) {


        Location l = new Location("google");
        l.setLatitude(lat);
        l.setLongitude(lng);
        Bundle b = new Bundle();
        b.putParcelable("Location", l);

        //send the location lat , lng as broadcast intent to be received by  MapsActivity
        Intent intent1 = new Intent("GPSLocationUpdates");
        intent1.putExtra("responderUserName", userName);
        intent1.putExtra("Location", b);
        LocalBroadcastManager.getInstance(myContext).sendBroadcast(intent1);

        //intent for notification pending intent
        Intent intent2 = new Intent(myContext, MapsActivity.class);
        intent2.putExtra("responderUserName", userName);
        intent2.putExtra("Location", b);
        PendingIntent pendingIntent = PendingIntent.getActivity
                (myContext, RegisterActivity.requestCode_LocationReceivedViaSms, intent2, PendingIntent.FLAG_CANCEL_CURRENT);

        String responderUserName = userName;
        String notificationTitle = "تم تحديد الموقع";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(myContext, "channel1")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setContentTitle(notificationTitle)
                .setSmallIcon(R.drawable.locationfinderlogo)
                .setContentText(responderUserName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(myContext);
        // notificationId is a unique int for each notification that you must define

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }


    @SuppressLint("MissingPermission")
    private void getCurrentLocation(final String reqPhoneNumber, Context context) {
        gps_Flag = displayGpsStatus(context); //check gps is turned on / off
        if (gps_Flag) {

            Log.d(TAG, "Gps_On_Get_Location");

            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setInterval(5000); // two minute interval
            locationRequest.setFastestInterval(5000);
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    List<Location> locationList = locationResult.getLocations();
                    if (locationList.size() > 0) {

                        Location location = locationList.get(locationList.size() - 1);
                        lat = location.getLatitude();
                        lng = location.getLongitude();

                        Log.d(TAG, "sendSmsCotainsLat&lng " + simId);
                        Log.d(TAG, "sendSmsCotainsLat&lng " + reqPhoneNumber);
                        Log.d(TAG, "sendSmsCotainsLat&lng");

                        if (getLocationTryNumber == 1) {
                            getLocationTryNumber = 2;
                        } else {
                            //msg format ($Coordinates: http://maps.google.com/maps?f=q&q=33.1234567,33.1234567&z=16)
                            String msg = "$Coordinates: http://maps.google.com/maps?f=q&q=" + lat + "," + lng + "&z=16";
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.getSmsManagerForSubscriptionId(simId).sendTextMessage(reqPhoneNumber, null, msg, null, null);

                            fusedLocationClient.removeLocationUpdates(locationCallback);
                            getLocationTryNumber = 1;
                        }

                    } else {

                        SharedPreferences sharedPref = myContext.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                        lng = Double.valueOf(sharedPref.getString("LocationFinderLongtitudeSharedPrefereces", "0"));
                        lat = Double.valueOf(sharedPref.getString("LocationFinderLatitudeSharedPrefereces", "0"));

                        Log.d("fusedgettinglocation", "from history: lng " + lng + " / lat " + lat);
                        fusedLocationClient.removeLocationUpdates(locationCallback);
                    }
                }
            };
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        } else {
            //send sms that gps is off
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.getSmsManagerForSubscriptionId(simId).sendTextMessage(reqPhoneNumber, null, "$نظام تحديد الموقع مغلق$", null, null);

            makeNotification_GPS_Off();
        }
    }


    /*---- Method to Check GPS is enable or disable ----- */
    private Boolean displayGpsStatus(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        boolean gpsStatus = Settings.Secure.isLocationProviderEnabled(contentResolver, LocationManager.GPS_PROVIDER);
        Log.d(TAG, gpsStatus + "");
        if (gpsStatus) {
            return true;

        } else {
            return false;
        }
    }


    //give notification that GPS is off and coming location requests is failing
    private void makeNotification_GPS_Off() {

        String notificationContent = "فشل في ارسال موقعك بسبب عدم تفعيل خاصية الموقع, يرجى تفعيل خاصية الموقع";

        //make notification click event
        Intent intent = new Intent(myContext, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity
                (myContext, RegisterActivity.requestCode_GPS_Off_Flag, intent, PendingIntent.FLAG_CANCEL_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(myContext, "channel1")
                .setContentTitle("تنبيه")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.locationfinderlogo)
                .setContentText(notificationContent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationContent))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(myContext);
        // notificationId is a unique int for each notification that you must define

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }


    //give notification that location request is failed due to GPS off in the target phone
    private void makeNotification_LocRequestFailed(String accessToUserName) {

        //make notification click event
        Intent intent = new Intent(myContext, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity
                (myContext, 131, intent, PendingIntent.FLAG_CANCEL_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(myContext, "channel1")
                .setContentTitle("فشل في تحديد الموقع")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.locationfinderlogo)
                .setContentText(" نظام تحديد الموقع غير مفعل عند " + accessToUserName)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(" نظام تحديد الموقع غير مفعل عند " + accessToUserName))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(myContext);
        // notificationId is a unique int for each notification that you must define

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }


    //give notification that GPS is off and coming location requests is failing
    private void makeNotification_AppActivated(String notificationContent) {

        //make notification click event
        Intent intent = new Intent(myContext, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity
                (myContext, RegisterActivity.requestCode_AppActivated, intent, PendingIntent.FLAG_CANCEL_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(myContext, "channel1")
                .setContentTitle("تفعيل")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.locationfinderlogo)
                .setContentText(notificationContent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationContent))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(myContext);
        // notificationId is a unique int for each notification that you must define

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }





    //set expiration date in server
    @SuppressLint("MissingPermission")   //for getting device imei (permission already granted)
    private void SetExpirationDate() {
        JSONObject phoneNumberJson = new JSONObject();
        try {
            final SharedPreferences sharedPref = myContext.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
            String expDateS = sharedPref.getString("LocationFinderExpDateSharedPrefereces", "2019-01-01");

            TelephonyManager telephonyManager = (TelephonyManager) myContext.getSystemService(RegisterActivity.TELEPHONY_SERVICE);
            String imei;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                imei = telephonyManager.getImei();
            } else {
                imei = telephonyManager.getDeviceId();
            }

            phoneNumberJson.put("PhoneNumber", myPhoneNumber);
            phoneNumberJson.put("ExpirationDate", expDateS);
            phoneNumberJson.put("imei", imei);
            new SetExpirationDateByPost().execute(phoneNumberJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private class SetExpirationDateByPost extends AsyncTask<JSONObject, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(JSONObject... jsonObjects) {
            RequestBody rBody = RequestBody.create(RegisterActivity.JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(RegisterActivity.serverIp + "/CheckImeiNExpiration")
                    .post(rBody)
                    .build();

            Response response = null;
            JSONObject jsonObject = null;
            try {
                response = RegisterActivity.client.newCall(request).execute();
                String s = response.body().string();
                jsonObject = new JSONObject(s);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return jsonObject;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
        }
    }
}
