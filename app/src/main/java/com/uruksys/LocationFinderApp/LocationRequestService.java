package com.uruksys.LocationFinderApp;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobScheduler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocationRequestService extends FirebaseMessagingService {


    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private double lat = 0;
    private double lng = 0;
    private String myPhoneNumber;
    private String serverIp = "http://23.239.203.134:3000/LocationFinderApp";
    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();

    Context myContext;

    private FusedLocationProviderClient fusedLocationClient;
    private Boolean flag = false;

    public LocationRequestService() {


    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("FCMService" , "created");

        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000); // two minute interval
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        CreateNotificationChannel();


        //get the information of the user from the sharedPreferences
        myContext = this;
        SharedPreferences sharedPref = myContext.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        myPhoneNumber = sharedPref.getString("LocationFinderPhoneSharedPrefereces", "notRegistered");
    }


    private void CreateNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel("channel1", "myChannel", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("ConnectionResponse");
            NotificationManager manager = (NotificationManager) getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }


    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        Log.d("TAG5", "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        setNewToken(token);
    }


    //set new token in DB
    private void setNewToken(String token) {

        SharedPreferences sharedPref = this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("LocationFinderTokenKeySharedPrefereces", token);
        editor.commit();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("tokenKey", token);
            jsonObject.put("phoneNumber", myPhoneNumber);
            new SetNewTokenByPost().execute(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private class SetNewTokenByPost extends AsyncTask<JSONObject, Void, Void> {

        @Override
        protected Void doInBackground(JSONObject... jsonObjects) {

            RequestBody rBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/setNewTokenKey")
                    .post(rBody)
                    .build();

            try {
                client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);


        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d("TAG1", "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {

            Log.d("TAG2", "Message data payload: " + remoteMessage.getData());

            for (Map.Entry<String, String> entry : remoteMessage.getData().entrySet()) {
                if (entry.getKey().equals("title") && entry.getValue().equals("abortConnectionTo")) {

                    makeNotification_AbortConnectionsTo(remoteMessage.getData());
                    break;
                } else if (entry.getKey().equals("title") && entry.getValue().equals("connectionReq")) {

                    makeNotification_NewConnReq(remoteMessage.getData());
                    break;
                } else if (entry.getKey().equals("title") && entry.getValue().equals("respondToConnectionReq")) {

                    onRespondToConnectionReq(remoteMessage.getData());
                    makeNotification_ConnReqResponse(remoteMessage.getData());
                    break;
                } else if (entry.getKey().equals("title") && entry.getValue().equals("locationReq")) {

                    //GetCurrentLocation(remoteMessage.getData());
                    break;
                } else if (entry.getKey().equals("title") && entry.getValue().equals("locationResponse")) {

                    //OnLocationResponse(remoteMessage.getData());
                    //makeNotification_LocReceived(remoteMessage.getData());
                    break;
                } else if (entry.getKey().equals("title") && entry.getValue().equals("GPS_Off")) {

                    //OnLocRequestFailed_GPSOff(remoteMessage.getData());
                    //makeNotification_LocRequestFailed(remoteMessage.getData());
                    break;
                }
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d("TAG3", "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }


    @SuppressLint({"MissingPermission"})
    private void SendCurrentLocation(final Map<String, String> map) {

        //starts the maps activity
        Log.d("fusedgettinglocation", "checking");
        /*Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClass(myContext, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);


        //wait until the activity starts then get the location
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        }, 1000);*/

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

                    //Log.d("fusedgettinglocation", "M1 : " + location);
                    Log.d("fusedgettinglocation", "M1 : lat :" + lat + ", lng :" + lng);
                } else {

                    SharedPreferences sharedPref = myContext.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                    lng = Double.valueOf(sharedPref.getString("LocationFinderLongtitudeSharedPrefereces", "0"));
                    lat = Double.valueOf(sharedPref.getString("LocationFinderLatitudeSharedPrefereces", "0"));

                    Log.d("fusedgettinglocation", "from history: lng " + lng + " / lat " + lat);
                }

                Log.d("fusedgettinglocation", "remove location updates");
                fusedLocationClient.removeLocationUpdates(locationCallback);

                String requesterPhoneNumber = map.get("requesterPhoneNumber");
                String responderPhoneNumber = map.get("responderPhoneNumber");

                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("requesterPhoneNumber", requesterPhoneNumber);
                    jsonObject.put("responderPhoneNumber", responderPhoneNumber);
                    jsonObject.put("lat", lat);
                    jsonObject.put("lng", lng);


                    Log.d("fusedgettinglocation", "send location by post");
                    new SendLocationByPost().execute(jsonObject);
                    //send broadcast to finish the maps Activity after getting the current location
                    //LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getBaseContext());
                    //localBroadcastManager.sendBroadcast(new Intent("activityStartedForLocReq"));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        Log.d("fusedgettinglocation", "request location updates");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.getApplicationContext());
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());


        //Intent getLocIntent = new Intent(myContext , GetLocationSevice.class);
        //PendingIntent pi = PendingIntent.getService(myContext, 987 , getLocIntent , 0);
        //fusedLocationClient.requestLocationUpdates(locationRequest , pi);


        /*fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {

                if(location != null){

                    //Log.d("fusedgettinglocation", "M2 : " + location);
                    lng = location.getLongitude();
                    lat = location.getLatitude();
                    Log.d("fusedgettinglocation", "M2 : lat :" + lat  + ", lng :" + lng);
                }
                else {

                    SharedPreferences sharedPref = myContext.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                    lng = Double.valueOf(sharedPref.getString("LocationFinderLongtitudeSharedPrefereces", "0"));
                    lat = Double.valueOf(sharedPref.getString("LocationFinderLatitudeSharedPrefereces", "0"));

                    Log.d("fusedgettinglocation", "from history: lng " +lng+ " / lat " + lat);
                }

                String requesterPhoneNumber = map.get("requesterPhoneNumber");
                String responderPhoneNumber = map.get("responderPhoneNumber");

                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("requesterPhoneNumber", requesterPhoneNumber);
                    jsonObject.put("responderPhoneNumber", responderPhoneNumber);
                    jsonObject.put("lat", lat);
                    jsonObject.put("lng", lng);

                    new SendLocationByPost().execute(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });*/
    }


    /*---- Method to Check GPS is enable or disable ----- */
    private Boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext().getContentResolver();
        boolean gpsStatus = Settings.Secure.isLocationProviderEnabled(contentResolver, LocationManager.GPS_PROVIDER);
        Log.d("TAG4", gpsStatus + "");
        if (gpsStatus) {
            return true;

        } else {
            return false;
        }
    }


    //get the current location method
    public void GetCurrentLocation(Map<String, String> map) {
        flag = displayGpsStatus(); //check gps is turned on / off


        //check if the sender is allowed to request location from this user(have connection) from sqlite DB
        Date connectTime_From = null;
        Date connectTime_To = null;
        String connectionStatus = null;
        Date myDate = Calendar.getInstance().getTime();

        MySqliteDB mySqliteDB = new MySqliteDB(myContext);
        Cursor c = mySqliteDB.GetAllConnections(map.get("requesterPhoneNumber"), map.get("responderPhoneNumber"));
        if (c.getCount() > 0) {

            connectionStatus = null;

            DateFormat df = new SimpleDateFormat("HH:mm");
            connectTime_From = null;
            connectTime_To = null;


            try {

                if (c.moveToFirst()) {
                    do {
                        connectionStatus = c.getString(c.getColumnIndex("ConnectionStatus"));
                        connectTime_From = new SimpleDateFormat("HH:mm").parse(c.getString(c.getColumnIndex("ConnectTime_From"))) ;
                        connectTime_To = new SimpleDateFormat("HH:mm").parse(c.getString(c.getColumnIndex("ConnectTime_To"))) ;

                    } while (c.moveToNext());
                }
                c.close();

                String myDateStr = df.format(myDate);
                myDate = df.parse(myDateStr);

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        Log.d("fusedgettinglocation" , "time interval : "+connectionStatus);
        Log.d("fusedgettinglocation" , "connection status : "+connectTime_From+ " to " + connectTime_To);
        Log.d("fusedgettinglocation" , "current time : "+myDate);

        if (flag && connectionStatus.equals("Connected") && myDate.before(connectTime_To) && myDate.after(connectTime_From)) {
            SendCurrentLocation(map);

        } else if(!flag){
            String requesterPhoneNumber = map.get("requesterPhoneNumber");
            String responderPhoneNumber = map.get("responderPhoneNumber");
            String requesterUserName = map.get("requesterUserName");

            makeNotification_GPS_Off(requesterUserName);

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("requesterPhoneNumber", requesterPhoneNumber);
                jsonObject.put("responderPhoneNumber", responderPhoneNumber);

                new GPS_Off_cantGetLocationByPost().execute(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    private class GPS_Off_cantGetLocationByPost extends AsyncTask<JSONObject, Void, Void> {

        @Override
        protected Void doInBackground(JSONObject... jsonObjects) {
            RequestBody rBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/GPS_Off_cantGetLocation")
                    .post(rBody)
                    .build();
            try {
                client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    private class SendLocationByPost extends AsyncTask<JSONObject, Void, Void> {

        @Override
        protected Void doInBackground(JSONObject... jsonObjects) {
            RequestBody rBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/returnRequestedLocation")
                    .post(rBody)
                    .build();
            try {
                Response response = client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    //when the requested location is received
    private void OnLocationResponse(Map<String, String> receivedMessage) {
        JobScheduler jobScheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(RegisterActivity.SmsJobSchedulerId);

        lat = Double.parseDouble(receivedMessage.get("lat"));
        lng = Double.parseDouble(receivedMessage.get("lng"));

        //send the location lat , lng as broadcast intent to be received by  MapsActivity
        Location l = new Location("google");
        l.setLatitude(lat);
        l.setLongitude(lng);
        Intent intent = new Intent("GPSLocationUpdates");
        Bundle b = new Bundle();
        b.putParcelable("Location", l);
        intent.putExtra("Location", b);
        intent.putExtra("responderUserName", receivedMessage.get("responderUserName"));
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
    }


    //make notification for the received location
    private void makeNotification_LocReceived(Map<String, String> receivedMsg) {
        Intent intent = new Intent(this, MapsActivity.class);
        Bundle b = new Bundle();
        Location l = new Location("google");
        l.setLatitude(lat);
        l.setLongitude(lng);
        b.putParcelable("Location", l);
        intent.putExtra("Location", b);
        intent.putExtra("responderUserName", receivedMsg.get("responderUserName"));
        PendingIntent pendingIntent = PendingIntent.getActivity
                (getBaseContext(), 100, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        String responderUserName = receivedMsg.get("responderUserName");
        String notificationTitle = "تم تحديد الموقع";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel1")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setContentTitle(notificationTitle)
                .setSmallIcon(R.drawable.locationfinderlogo)
                .setContentText(responderUserName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }


    //when response to connection request is received send it as broadcast and set it in sqlite db
    private void onRespondToConnectionReq(Map<String, String> receivedMsg) {

        //send  broadcast intent to be received by  MapsActivity
        Intent intent = new Intent("responseToConnectionReq");
        intent.putExtra("connectionsUpdated", true);
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);


        String AccessToPhone = receivedMsg.get("AccessToPhone");
        String connectionStatus = receivedMsg.get("ConnectionStatus");
        String connectTime_From = receivedMsg.get("ConnectTime_From");
        String connectTime_To = receivedMsg.get("ConnectTime_To");

        //make the query on sqlite db
        MySqliteDB mySqliteDB = new MySqliteDB(this);


        //get the information of the user from the sharedPreferences
        myContext = this;
        SharedPreferences sharedPref = myContext.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        myPhoneNumber = sharedPref.getString("LocationFinderPhoneSharedPrefereces", "notRegistered");

        boolean result = mySqliteDB.UpdateOnConnectionsTbl(myPhoneNumber, AccessToPhone, connectionStatus , connectTime_From , connectTime_To);
        Log.d("updateSqlite", "reject request completed");

        if (!result) {
            Toast.makeText(this, "error query of sqlite insert/update connection", Toast.LENGTH_SHORT).show();
        }
    }


    //make notification for the received requested_location
    private void makeNotification_ConnReqResponse(Map<String, String> receivedMsg) {

        String AccessToPhone = receivedMsg.get("AccessToPhone");
        String AccessToUserName = receivedMsg.get("AccessToUserName");
        String connectionStatus = receivedMsg.get("ConnectionStatus");
        String notificationContent = null;

        switch (connectionStatus) {
            case "Connected":
                notificationContent = "تم الموافقة على طلب ارتباطك مع " + AccessToUserName + " صاحب الرقم " + AccessToPhone;
                break;
            case "Rejected":
                notificationContent = "تم رفض طلب ارتباطك مع " + AccessToUserName + " صاحب الرقم " + AccessToPhone;
                break;
            case "Aborted":
                notificationContent = "تم الغاء ارتباطك مع " + AccessToUserName + " صاحب الرقم " + AccessToPhone;
                break;
        }


        //make notification click event
        Intent intent = new Intent(this, ConnectionsToActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity
                (getBaseContext(), 120, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        String notificationTitle = "رد على طلب الارتباط";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel1")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setContentTitle(notificationTitle)
                .setSmallIcon(R.drawable.locationfinderlogo)
                .setContentText(notificationContent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationContent))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }


    //make notification for new connection request received
    private void makeNotification_NewConnReq(Map<String, String> receivedMsg) {
        String notificationTitle = "طلب ارتباط جديد";
        String accessFromPhone = receivedMsg.get("accessFromPhone");
        String accessFromUserName = null;

        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;

        ContentResolver contentResolver = getContentResolver();

        // Query and loop for every phone number of the contact
        Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null, null, null, null);

        while (phoneCursor.moveToNext()) {
            String number = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER)).replace(" ", "");
            if
            (
                    accessFromPhone.equals(number) ||
                            ("00964" + accessFromPhone.substring(1, accessFromPhone.length())).equals(number) ||
                            ("+964" + accessFromPhone.substring(1, accessFromPhone.length())).equals(number)
            ) {
                accessFromUserName = phoneCursor.getString(phoneCursor.getColumnIndex(DISPLAY_NAME));
                break;
            }
        }
        phoneCursor.close();


        if (accessFromUserName == null) accessFromUserName = "غير معرف";

        //make notification click event
        Intent intent = new Intent(this, ConnsAndReqsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity
                (getBaseContext(), 110, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        String notificationContent = String.format("تم وصول طلب ارتباط من " + accessFromUserName + " صاحب الرقم " + accessFromPhone);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel1")
                .setContentTitle(notificationTitle)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.locationfinderlogo)
                .setContentText(notificationContent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationContent))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());


        //re get the information of the user from the sharedPreferences
        Context context = this;
        SharedPreferences sharedPref = context.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        myPhoneNumber = sharedPref.getString("LocationFinderPhoneSharedPrefereces", "notRegistered");
        Log.d("updateSqlite", "myPhoneNumber " + myPhoneNumber);

        //make the query on sqlite db
        MySqliteDB mySqliteDB = new MySqliteDB(this);
        Cursor cursor = mySqliteDB.GetAllConnections(accessFromPhone, myPhoneNumber);

        boolean result;
        if (cursor.getCount() > 0) {

            result = mySqliteDB.UpdateOnConnectionsTbl(accessFromPhone, myPhoneNumber, "Pending" , "00:00" , "23:59");
            Log.d("updateSqlite", "completed");

        } else {
            result = mySqliteDB.InsertToConnectionsTbl(accessFromPhone, accessFromUserName, myPhoneNumber, null, "Pending" , "00:00" , "23:59");
            Log.d("insertSqlite", "completed");
        }

        if (!result) {
            Toast.makeText(this, "error query of sqlite insert/update connection", Toast.LENGTH_SHORT).show();
        }


        Log.d("UserName", accessFromUserName);
        //save the username of the number in DB
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("accessFromPhone", accessFromPhone);
            jsonObject.put("accessToPhone", myPhoneNumber);
            jsonObject.put("accessFromUserName", accessFromUserName);

            new SendRequesterUserNameByPost().execute(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    //set the username of the connection request by post
    private class SendRequesterUserNameByPost extends AsyncTask<JSONObject, Void, Void> {

        @Override
        protected Void doInBackground(JSONObject... jsonObjects) {
            RequestBody rBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/setConnectionRequesterUserName")
                    .post(rBody)
                    .build();

            try {
                client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    //make notification for aborting connections to from conns&reqs
    private void makeNotification_AbortConnectionsTo(Map<String, String> receivedMsg) {

        String accessFromUserName = receivedMsg.get("accessFromUserName");
        String accessFromPhone = receivedMsg.get("accessFromPhone");

        //make the query on sqlite db
        MySqliteDB mySqliteDB = new MySqliteDB(this);

        //get the information of the user from the sharedPreferences
        myContext = this;
        SharedPreferences sharedPref = myContext.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        myPhoneNumber = sharedPref.getString("LocationFinderPhoneSharedPrefereces", "notRegistered");

        boolean result = mySqliteDB.UpdateOnConnectionsTbl(accessFromPhone, myPhoneNumber, "Aborted");
        Log.d("updateSqlite", "completed");

        if (!result) {
            Toast.makeText(this, "error query of sqlite insert/update connection", Toast.LENGTH_SHORT).show();
        }

        //making notification
        String notificationTitle = null;
        String notificationContent = null;

        if (receivedMsg.get("oldConnectionStatus").equals("Connected")) {
            notificationTitle = "الغاء الارتباط";
            notificationContent = String.format("لقد الغى " + accessFromUserName + " صاحب الرقم " + accessFromPhone + " ارتباطه بك ");
        } else if (receivedMsg.get("oldConnectionStatus").equals("Pending")) {
            notificationTitle = "الغاء طلب الارتباط";
            notificationContent = String.format("لقد الغى " + accessFromUserName + " صاحب الرقم " + accessFromPhone + " طلب ارتباطه بك");
        }


        //make notification click event
        Intent intent = new Intent(this, ConnectionsToActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity
                (getBaseContext(), 130, intent, PendingIntent.FLAG_CANCEL_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel1")
                .setContentTitle(notificationTitle)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.locationfinderlogo)
                .setContentText(notificationContent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationContent))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }


    //when target gps is off and location request failed
    private void OnLocRequestFailed_GPSOff(Map<String, String> receivedMsg) {

        //abort the notification for sending sms
        JobScheduler jobScheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(RegisterActivity.SmsJobSchedulerId);

        //send  broadcast intent to be received by  MapsActivity
        Intent intent1 = new Intent("TargetGPS_Off");
        intent1.putExtra("msg", receivedMsg.get("notificationContent"));
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent1);
    }


    //give notification that location request is failed due to GPS off in the target phone
    private void makeNotification_LocRequestFailed(Map<String, String> receivedMsg) {

        //make notification click event
        Intent intent = new Intent(this, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity
                (getBaseContext(), 130, intent, PendingIntent.FLAG_CANCEL_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel1")
                .setContentTitle(receivedMsg.get("notificationTitle"))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.locationfinderlogo)
                .setContentText(receivedMsg.get("notificationContent"))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(receivedMsg.get("notificationContent")))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }


    //give notification that GPS is off and coming location requests is failing
    private void makeNotification_GPS_Off(String requesterUserName) {

        String notificationContent = "فشل في ارسال موقعك الى " + requesterUserName + " بسبب عدم تفعيل خاصية الموقع";

        //make notification click event
        Intent intent = new Intent(this, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity
                (getBaseContext(), 130, intent, PendingIntent.FLAG_CANCEL_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel1")
                .setContentTitle("تنبيه")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.locationfinderlogo)
                .setContentText(notificationContent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationContent))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }
}