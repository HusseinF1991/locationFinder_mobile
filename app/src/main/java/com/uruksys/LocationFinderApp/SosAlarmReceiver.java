package com.uruksys.LocationFinderApp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Looper;
import android.os.PowerManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

public class SosAlarmReceiver extends BroadcastReceiver {

    FusedLocationProviderClient fusedLocationClient;
    LocationCallback locationCallback;
    Context myContext;
    double lng = 0;
    double lat = 0;
    int getLocationTryNumber = 1;
    PowerManager.WakeLock wl;

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        myContext = context;

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationFinderApp:SosWakeLock");
        wl.acquire();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        final LocationRequest locationRequest = new LocationRequest();
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

                    Log.d("GetLocationForSms", "get location "+location);
                    lng = location.getLongitude();
                    lat = location.getLatitude();

                }
                else {

                    SharedPreferences sharedPref = myContext.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                    lng = Double.valueOf(sharedPref.getString("LocationFinderLongtitudeSharedPrefereces", "0"));
                    lat = Double.valueOf(sharedPref.getString("LocationFinderLatitudeSharedPrefereces", "0"));

                }


                if (getLocationTryNumber == 1) {
                    getLocationTryNumber = 2;
                } else {

                    //msg format ($Coordinates: http://maps.google.com/maps?f=q&q=33.1234567,33.1234567&z=16)
                    String msg = "$SOS: http://maps.google.com/maps?f=q&q=" + lat + "," + lng + "&z=16";

                    SharedPreferences sharedPref = myContext.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                    int simId = sharedPref.getInt("LocationFinderSimIdSharedPrefereces", 0);
                    String num1 = sharedPref.getString("LocationFinderSosNum1SharedPrefereces", "");
                    String num2 = sharedPref.getString("LocationFinderSosNum2SharedPrefereces", "");
                    String num3 = sharedPref.getString("LocationFinderSosNum3SharedPrefereces", "");

                    SmsManager smsManager = SmsManager.getDefault();
                    if (num1 != "")
                        smsManager.getSmsManagerForSubscriptionId(simId).sendTextMessage(num1, null, msg, null, null);
                    if (num2 != "")
                        smsManager.getSmsManagerForSubscriptionId(simId).sendTextMessage(num2, null, msg, null, null);
                    if (num3 != "")
                        smsManager.getSmsManagerForSubscriptionId(simId).sendTextMessage(num3, null, msg, null, null);

                    Toast.makeText(myContext, "تم ارسال نداء استغاثة", Toast.LENGTH_LONG).show();
                    Log.d("SosAlarm", "sent");

                    fusedLocationClient.removeLocationUpdates(locationCallback);
                    getLocationTryNumber = 1;

                    wl.release();
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }
}
