package com.uruksys.LocationFinderApp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();

    Intent foregroundServiceIntent;
    Intent FCMservice;

    LocationCallback locationCallback;
    private double lng = 0;
    private double lat = 0;
    private GoogleMap mMap;
    List<Marker> mapMarkersList = new ArrayList<>();
    int getLocationTryNumber = 1;


    private Date connectTime_From;
    private Date connectTime_To;
    private String myPhoneNumber;
    private int simId;
    private int SmsRequest_WaitTime;
    private String serverIp = "http://23.239.203.134:3000/LocationFinderApp";
    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();

    private ScrollView mScrollView;
    private TextView txtSelectedUserPhone, txtLat, txtLng, txtCityName;
    private Spinner spinConnectedUserNames;
    private Button btnGetLocation;
    private Button btnActivateSos;
    private Button btnSendMyLocation;
    private Button btnOpenGoogleMap;
    private ArrayList<UserInfoModel> connectionsList = new ArrayList();

    private Button btnOpenConnectionTo;
    private Button btnOpenPersonalInformation;
    private Button btnOpenConnsNReqs;

    private boolean sosStatus = false;
    private int sosIntervals = 10;

    private String selectedTokenKey = null;
    private String selectedPhone = null;
    private String selectedUser = null;

    private BroadcastReceiver locationMessageReceiver = null;
    private BroadcastReceiver newConnectionMessageReceiver = null;
    private BroadcastReceiver LocRequestFailedMessageReceiver = null;
    //private BroadcastReceiver networkStatusMessageReceiver = null;
    private BroadcastReceiver ProposeReqBySmsMessageReceiver = null;
    private BroadcastReceiver ConfirmActivationMessageReceiver = null;

    private GoogleApiClient googleApiClient;

    private CountDownTimer countDownTimer = null;
    private CountDownTimer sosStartsTimer = null;

    private Boolean isConnected = false;
    //private Boolean isConnected = null;
    //private Timer pingTimer = null;
    //private TimerTask pingTimerTask = null;

    private FusedLocationProviderClient fusedLocationClient;

    private AlertDialog LoadingDialog = null;
    private AlertDialog ActivationLoadingDialog = null;
    private AlertDialog ActivationDialog = null;

    //for send my location dialog
    private AlertDialog sendMyLocDialog;
    private ArrayList<String> contactNumbers = new ArrayList<>();
    int currentIndex = 0;
    private TextView txtContactName;
    private EditText etxtContactNumber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //make the activity screen always on
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        //get the information of the user from the sharedPreferences
        Context context = MapsActivity.this;
        final SharedPreferences sharedPref = context.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        myPhoneNumber = sharedPref.getString("LocationFinderPhoneSharedPrefereces", "notRegistered");
        simId = sharedPref.getInt("LocationFinderSimIdSharedPrefereces", 0);
        SmsRequest_WaitTime = sharedPref.getInt("LocationFinderWaitToSendSmsSharedPrefereces", 60);
        sosStatus = sharedPref.getBoolean("LocationFinderSosStatusSharedPrefereces", false);
        sosIntervals = sharedPref.getInt("LocationFinderSosIntervalsSharedPrefereces", 10);
        String expDateS = sharedPref.getString("LocationFinderExpDateSharedPrefereces", "2019-01-01");


        spinConnectedUserNames = findViewById(R.id.spinConnectedUserNames);
        txtSelectedUserPhone = findViewById(R.id.txtSelectedUserPhone);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnActivateSos = findViewById(R.id.btnActivateSos);

        btnOpenConnectionTo = findViewById(R.id.btnOpenConnectionTo);
        btnOpenConnsNReqs = findViewById(R.id.btnOpenConnsNReqs);
        btnOpenPersonalInformation = findViewById(R.id.btnOpenPersonalInformation);

        btnOpenGoogleMap = findViewById(R.id.btnOpenGoogleMap);
        btnSendMyLocation = findViewById(R.id.btnSendMyLocation);
        txtLng = findViewById(R.id.txtLng);
        txtLat = findViewById(R.id.txtLat);
        txtCityName = findViewById(R.id.txtCityName);

        mScrollView = findViewById(R.id.scrollMap);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionBar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);

        //change sos button background according to sos status
        if (!sosStatus) btnActivateSos.setBackground(getDrawable(R.drawable.sos_on));
        else if (sosStatus) btnActivateSos.setBackground(getDrawable(R.drawable.sos_off));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        WorkaroundMapFragment mapFragment = (WorkaroundMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);

            mapFragment.setListener(new WorkaroundMapFragment.OnTouchListener() {
                @Override
                public void onTouch() {
                    mScrollView.requestDisallowInterceptTouchEvent(true);
                }
            });
        }

        //change the map fragment dimensions relativly to the screen size
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        ViewGroup.LayoutParams params = mapFragment.getView().getLayoutParams();
        params.height = (int) (0.45 * height);
        mapFragment.getView().setLayoutParams(params);


        //check permissions if user not registered before
        //checkLocationPermission();
        //checkContactsPermission();
        //checkReadSMSPermission();
        //checkSendSMSPermission();
        //checkReceiveSMSPermission();
        //checkReadPhoneStatePermission();
        Check9Permissions();


        TurnGPS_On(); //promote the user to turn on gps if its closed
        OnSelectingUserNameInSpinner();
        OnRequestingLocation();
        //open dialog to send my current location to selected number
        SendMyLocation();
        //open received location in google map url
        OpenInGoogleMap();
        SendSos();

        OnStartingOtherActivities();

        //check the account imei and the phone number in server and the expiration date in server
        CheckAccountNActivation();


        //when user chooses to get target location by sms on clicking on the notification's action   ...deprecated(no getting loc by internet)
        Intent intent = getIntent();
        String targetPhone = intent.getStringExtra("targetPhoneForSMS");
        if (targetPhone != null) {
            GetLocationBySMS(targetPhone, "اختر الشبكة");
        }


        ReceiveBroadcastLocation();
        ReceiveBroadcastNewConnection();
        ReceiveBroadcastReqLocFailed();
        ReceiveBroadcastProposeReqBySms();
        ReceiveBroadcastActivationConfirmation();
        //ReceiveBroadcastNetConnectionStatus();


        //register receivers for the broadcast intent
        LocalBroadcastManager.getInstance(this).registerReceiver(
                locationMessageReceiver, new IntentFilter("GPSLocationUpdates"));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                newConnectionMessageReceiver, new IntentFilter("responseToConnectionReq"));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                LocRequestFailedMessageReceiver, new IntentFilter("TargetGPS_Off"));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                ProposeReqBySmsMessageReceiver, new IntentFilter("ProposeRequestBySms"));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                ConfirmActivationMessageReceiver, new IntentFilter("WaitActivationConfirmSms"));

        //LocalBroadcastManager.getInstance(this).registerReceiver(
        //        activityStartsForLocReqReceiver , new IntentFilter("activityStartedForLocReq"));

        //LocalBroadcastManager.getInstance(this).registerReceiver(
        //        networkStatusMessageReceiver, new IntentFilter("networkChangeReceiver"));

        //this.registerReceiver(new NetworkChangeReceiver(), new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));


        Date today = new Date();
        Log.d("ExpirationDate", expDateS);
        try {

            foregroundServiceIntent = new Intent(this, ForegroundService.class);
            FCMservice = new Intent(this, LocationRequestService.class);

            if (today.after(new SimpleDateFormat("yyyy-MM-dd",Locale.ENGLISH).parse(expDateS))) {

                Log.d("ExpirationDate", "expired");
                //stop services
                stopService(FCMservice);
                stopService(foregroundServiceIntent);

                //unregister receivers for the broadcast intent
                LocalBroadcastManager.getInstance(this).unregisterReceiver(locationMessageReceiver);
                LocalBroadcastManager.getInstance(this).unregisterReceiver(newConnectionMessageReceiver);
                LocalBroadcastManager.getInstance(this).unregisterReceiver(LocRequestFailedMessageReceiver);
                LocalBroadcastManager.getInstance(this).unregisterReceiver(ProposeReqBySmsMessageReceiver);


                Boolean WaitActivationSms = sharedPref.getBoolean("LocationFinderWaitActivationSmsSharedPrefereces", false);
                if (WaitActivationSms) {

                    LayoutInflater layoutInflater = getLayoutInflater();
                    View view = layoutInflater.inflate(R.layout.loading_dialog, null);

                    TextView txtMessageInDialog = view.findViewById(R.id.txtMessageInDialog);
                    txtMessageInDialog.setText("بانتظار تاكيد التفعيل");

                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MapsActivity.this);
                    builder1.setView(view);
                    ActivationLoadingDialog = builder1.create();
                    ActivationLoadingDialog.show();

                    ActivationLoadingDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if (sharedPref.getBoolean("LocationFinderWaitActivationSmsSharedPrefereces", false)) {
                                ActivationLoadingDialog.show();
                            }
                        }
                    });
                } else {

                    Toast.makeText(this, "البرنامج منتهي الصلاحية", Toast.LENGTH_SHORT).show();

                    ActivateApplication();
                    ActivationDialog.show();
                }
            } else {

                startService(FCMservice);

                //start service that works in foreground
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(foregroundServiceIntent);
                } else {
                    startService(foregroundServiceIntent);
                }

                //give guidance for huawei
                int appStartUpTimes = sharedPref.getInt("LocationFinderAppStartUpTimesSharedPrefereces", 1);
                if(appStartUpTimes < 4){
                    if (Build.MANUFACTURER.toLowerCase().equals("huawei")) {
                        String guidance = "لعمل البرنامج بصورة صحيحة , رجاءا اتبع التعليمات في الرابط ادناه :\n" +
                                "https://www.youtube.com/watch?v=GNb1DAHudZ4&list=PLvyQs_dnjfSav8iRPqb4D3Np_d-OsJ8dZ&index=32 ";
                        LayoutInflater inflater = getLayoutInflater();
                        View view  = inflater.inflate(R.layout.aboutus_dialog , null);
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setView(view);
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();

                        TextView t2 = view.findViewById(R.id.txtAboutUs);
                        t2.setMovementMethod(LinkMovementMethod.getInstance());
                        t2.setText(guidance);

                        appStartUpTimes++;
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putInt("LocationFinderAppStartUpTimesSharedPrefereces", appStartUpTimes);
                        editor.commit();
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();

            Log.d("ExpirationDate", "error converting");
        }


        GetConnectedUsers();
        //timer to ping to google every 4 sec to check internet connection ... deprecated
        /*pingTimer = new Timer();
        pingTimerTask = new TimerTask() {
            @Override
            public void run() {
                boolean currentPing = internetIsConnected();
                if (isConnected == null || isConnected != currentPing) {
                    isConnected = currentPing;
                    Log.d("isThereInternet", "MapsActivity / " + isConnected);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isConnected) {

                                Toast.makeText(MapsActivity.this, "غير مرتبط بالانترنت", Toast.LENGTH_SHORT).show();
                            }

                            GetConnectedUsers();
                        }
                    });
                }
            }
        };
        pingTimer.schedule(pingTimerTask, 0, 4000);*/
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        /*pingTimer.cancel();
        pingTimer.purge();
        pingTimerTask.cancel();*/
    }


    //check if there is internet connection by ping on google
    public boolean internetIsConnected() {
        try {
            String command = "ping -c 1 www.google.com";
            return (Runtime.getRuntime().exec(command).waitFor() == 0);
        } catch (Exception e) {
            return false;
        }
    }


    //when user chooses to get target location by sms on clicking on the notification's action
    @SuppressLint("MissingPermission")
    private void GetLocationBySMS(final String targetPhone, final String alertMsg) {

        //get sim cards info
        final List<SimInfo> simInfoList = new ArrayList<>();
        Uri URI_TELEPHONY = Uri.parse("content://telephony/siminfo/");
        Cursor c = this.getContentResolver().query(URI_TELEPHONY, null, "sim_id == " + simId, null, null);


        if (c.moveToFirst()) {
            do {
                int id = c.getInt(c.getColumnIndex("_id"));
                String display_name = c.getString(c.getColumnIndex("display_name"));
                String icc_id = c.getString(c.getColumnIndex("icc_id"));
                int sim_id = c.getInt(c.getColumnIndex("sim_id"));
                SimInfo simInfo = new SimInfo(id, display_name, icc_id, sim_id);
                Log.d("apipas_sim_info", simInfo.toString());
                simInfoList.add(simInfo);
            } while (c.moveToNext());
        }
        c.close();


        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this)
                .setMessage(alertMsg)
                .setPositiveButton(simInfoList.get(0).getDisplay_name(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.getSmsManagerForSubscriptionId(simId).sendTextMessage(targetPhone, null, "$LocationFinderNeedsLatAndLng$", null, null);
                        Toast.makeText(MapsActivity.this, "تم ارسال الرسالة", Toast.LENGTH_SHORT).show();

                        LayoutInflater layoutInflater = getLayoutInflater();
                        View view = layoutInflater.inflate(R.layout.loading_dialog, null);

                        AlertDialog.Builder builder1 = new AlertDialog.Builder(MapsActivity.this);
                        builder1.setView(view)
                                .setNegativeButton("أخفاء", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        LoadingDialog = builder1.create();
                        LoadingDialog.show();

                    }
                })
                .setNeutralButton("الغاء", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                });

        AlertDialog alertDialog = builder.create();
        try {
            alertDialog.show();
        } catch (WindowManager.BadTokenException e) {
            //use a log message
        }

    }


    //promote the user to turn on GPS (if closed) on the activity created
    private void TurnGPS_On() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(getApplicationContext()).addApi(LocationServices.API).build();
            googleApiClient.connect();

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(30 * 1000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
            // **************************
            builder.setAlwaysShow(true); // this is the key ingredient
            // **************************

            PendingResult result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(new ResultCallback() {

                @Override
                public void onResult(@NonNull Result result) {
                    final Status status = result.getStatus();
                    //final LocationSettingsStates state = result.getLocationSettingsStates();

                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:

                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {

                                status.startResolutionForResult(MapsActivity.this, 1000);

                            } catch (IntentSender.SendIntentException e) {

                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:

                            break;

                    }
                }
            });

        }
        //googleApiClient = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    //onReceiving broadcast that the internet connection status changed
    /*private void ReceiveBroadcastNetConnectionStatus() {
        networkStatusMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Log.d("isThereInternet", "MapsActivity / " + isConnected);

                if(isConnected != null) isConnected = internetIsConnected();
                else isConnected = !isConnected;

                GetConnectedUsers();
            }
        };
    }*/


    //onReceiving broadcast to close the mapsActivity after getting the location
    /*private void ReceiveBroadcastForClosingActivity(){
        activityStartsForLocReqReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {

                finish();
            }
        };
    }*/


    //onReceiving broadcast of the requested location is failed due to target gps is turned off
    private void ReceiveBroadcastReqLocFailed() {
        LocRequestFailedMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (countDownTimer != null) countDownTimer.cancel();
                if (LoadingDialog != null && LoadingDialog.isShowing()) LoadingDialog.dismiss();
                btnGetLocation.setEnabled(true);
                //btnGetLocation.setText("طلب الموقع");

                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setMessage(intent.getStringExtra("msg"));

                AlertDialog alertDialog = builder.create();
                try {
                    alertDialog.show();
                } catch (WindowManager.BadTokenException e) {
                    //use a log message
                }
            }
        };
    }


    //onReceiving broadcast of the connection response
    private void ReceiveBroadcastNewConnection() {
        newConnectionMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean newConnectionStatus = intent.getBooleanExtra("connectionsUpdated", true);

                GetConnectedUsers();
            }
        };
    }


    //onReceiving broadcast of the fetched location
    private void ReceiveBroadcastLocation() {
        locationMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extra data included in the Intent
                String responderUserName = intent.getStringExtra("responderUserName");
                Bundle b = intent.getBundleExtra("Location");
                Location lastKnownLoc = (Location) b.getParcelable("Location");
                if (lastKnownLoc != null) {
                    SetReceivedLocation(lastKnownLoc, responderUserName);

                    if (countDownTimer != null) countDownTimer.cancel();
                    if (LoadingDialog != null && LoadingDialog.isShowing()) LoadingDialog.dismiss();

                    btnGetLocation.setEnabled(true);
                    //btnGetLocation.setText("طلب الموقع");
                }
            }
        };
    }


    //receive broadcast that activation Sms received or not
    private void ReceiveBroadcastActivationConfirmation() {
        ConfirmActivationMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extra data included in the Intent
                Boolean smsReceived = intent.getBooleanExtra("smsReceived", false);

                if (smsReceived) {

                    final SharedPreferences sharedPref = context.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                    int amountTransferred = sharedPref.getInt("LocationFinderAmountTransferredToActivateSharedPrefereces", 0);
                    if (amountTransferred > 0) {

                        if (ActivationLoadingDialog != null && ActivationLoadingDialog.isShowing())
                            ActivationLoadingDialog.dismiss();

                        ActivateApplication();
                        try {
                            ActivationDialog.show();
                        } catch (WindowManager.BadTokenException e) {
                            //use a log message
                        }
                    } else {

                        String expDateS = sharedPref.getString("LocationFinderExpDateSharedPrefereces", "2019-01-01");

                        if (ActivationLoadingDialog != null && ActivationLoadingDialog.isShowing())
                            ActivationLoadingDialog.dismiss();
                        if (ActivationDialog != null && ActivationDialog.isShowing())
                            ActivationDialog.dismiss();

                        //register receivers for the broadcast intent
                        LocalBroadcastManager.getInstance(MapsActivity.this).registerReceiver(
                                locationMessageReceiver, new IntentFilter("GPSLocationUpdates"));

                        LocalBroadcastManager.getInstance(MapsActivity.this).registerReceiver(
                                newConnectionMessageReceiver, new IntentFilter("responseToConnectionReq"));

                        LocalBroadcastManager.getInstance(MapsActivity.this).registerReceiver(
                                LocRequestFailedMessageReceiver, new IntentFilter("TargetGPS_Off"));

                        LocalBroadcastManager.getInstance(MapsActivity.this).registerReceiver(
                                ProposeReqBySmsMessageReceiver, new IntentFilter("ProposeRequestBySms"));

                        //start FCM service
                        startService(FCMservice);

                        //start service that works in foreground
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(foregroundServiceIntent);
                        } else {
                            startService(foregroundServiceIntent);
                        }

                        Toast.makeText(MapsActivity.this, " تم التفعيل لغاية " + expDateS, Toast.LENGTH_SHORT).show();

                        //update the expiration date in the server
                        CheckAccountNActivation();
                    }

                } else {
                    if (ActivationLoadingDialog != null && ActivationLoadingDialog.isShowing())
                        ActivationLoadingDialog.dismiss();

                    ActivateApplication();
                    try {
                        ActivationDialog.show();
                    } catch (WindowManager.BadTokenException e) {
                        //use a log message
                    }

                }
            }
        };
    }


    //onReceiving broadcast notifying that request failed and proposing request by sms
    private void ReceiveBroadcastProposeReqBySms() {
        ProposeReqBySmsMessageReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extra data included in the Intent
                String targetPhone = intent.getStringExtra("targetPhoneForSMS");
                String msg = "فشل في تحديد موقع الرقم " + targetPhone + " بسبب عدم ارتباطه بالانترنت , هل ترغب بمعرفة الموقع عن طريق ارسال رسالة";

                GetLocationBySMS(targetPhone, msg);
            }
        };
    }


    //set the received location in the map and textViews
    private void SetReceivedLocation(Location lastKnownLoc, String responderUserName) {

        txtLat.setText(String.valueOf(lastKnownLoc.getLatitude()));
        txtLng.setText(String.valueOf(lastKnownLoc.getLongitude()));


        /*----------to get City-Name from coordinates ------------- */
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        List<Address> addresses;
        try {

            btnOpenGoogleMap.setEnabled(true);
            btnOpenGoogleMap.setBackgroundResource(R.drawable.googlemap);
            addresses = gcd.getFromLocation(lastKnownLoc.getLatitude(), lastKnownLoc.getLongitude(), 1);

            if (addresses.size() > 0) {
                String cityName = addresses.get(0).getLocality();
                txtCityName.setText(cityName);
                LatLng location = new LatLng(lastKnownLoc.getLatitude(), lastKnownLoc.getLongitude());

                //make the old markers icon color blue and the last one is red
                if (mapMarkersList.size() > 0) {
                    for (Marker marker : mapMarkersList) {

                        Log.d("markersOfMap", "changing the color of it");
                        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    }
                }

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(location)
                        .title(responderUserName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                Marker marker = mMap.addMarker(markerOptions);
                mapMarkersList.add(marker);

                mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
                mMap.setMinZoomPreference(12.0f);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //open google map application when clicking the btnOpenGoogleMap button
    private void OpenInGoogleMap() {
        btnOpenGoogleMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!txtLat.getText().toString().equals("") && !txtLng.getText().toString().equals("")) {

                    String googleUrl = "http://maps.google.com/maps?f=q&q=" + txtLat.getText().toString() + "," + txtLng.getText().toString() + "&z=16";

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleUrl));
                    startActivity(browserIntent);
                }
            }
        });
    }


    //open dialog to send my current location to selected number
    private void SendMyLocation() {
        btnSendMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                LayoutInflater layoutInflater = getLayoutInflater();
                View dialogBuilderView = layoutInflater.inflate(R.layout.sendloc_mapactivitydialog, null);
                builder.setView(dialogBuilderView);

                txtContactName = dialogBuilderView.findViewById(R.id.txtContactName);
                etxtContactNumber = dialogBuilderView.findViewById(R.id.etxtContactNumber);
                Button btnSendMyLocDialog = dialogBuilderView.findViewById(R.id.btnSendMyLocDialog);
                Button btnCancelSendLocDialog = dialogBuilderView.findViewById(R.id.btnCancelSendLocDialog);
                Button btnSearchContacts = dialogBuilderView.findViewById(R.id.btnSearchContacts);
                Button btnNextNumberForContact = dialogBuilderView.findViewById(R.id.btnNextNumberForContact);

                sendMyLocDialog = builder.create();
                sendMyLocDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                sendMyLocDialog.show();

                //cancel connection request dialog
                btnCancelSendLocDialog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendMyLocDialog.dismiss();
                    }
                });


                //listener on requesting new connection by btn click
                btnSendMyLocDialog.setOnClickListener(new View.OnClickListener() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onClick(View v) {
                        if (etxtContactNumber.getText().toString().equals("")) {
                            Toast.makeText(getBaseContext(), "أدخل رقم المستلم رجاءا", Toast.LENGTH_SHORT).show();
                        } else {

                            final String destinationAddress = etxtContactNumber.getText().toString();
                            Toast.makeText(MapsActivity.this, "سيتم ارسال موقعك الحالي...", Toast.LENGTH_SHORT).show();
                            sendMyLocDialog.dismiss();
                            btnSendMyLocation.setEnabled(false);

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

                                        Log.d("sendMyLocation", location + "");
                                        Log.d("sendMyLocation", "lat: " + location.getLatitude() + " lng: " + location.getLongitude());
                                        lng = location.getLongitude();
                                        lat = location.getLatitude();

                                    } else {

                                        SharedPreferences sharedPref = MapsActivity.this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                                        lng = Double.valueOf(sharedPref.getString("LocationFinderLongtitudeSharedPrefereces", "0"));
                                        lat = Double.valueOf(sharedPref.getString("LocationFinderLatitudeSharedPrefereces", "0"));

                                    }

                                    if (getLocationTryNumber == 1) {
                                        getLocationTryNumber = 2;
                                    } else {

                                        // SMS sent pending intent
                                        PendingIntent sentIntent = PendingIntent.getBroadcast(MapsActivity.this, RegisterActivity.CheckCurrentLocSentInSms_BroadcastCode,
                                                new Intent("SMS_SENT_ACTION"), 0);

                                        registerReceiver(new BroadcastReceiver() {
                                            @Override
                                            public void onReceive(Context arg0, Intent arg1) {
                                                int resultCode = getResultCode();
                                                switch (resultCode) {
                                                    case Activity.RESULT_OK:
                                                        Toast.makeText(MapsActivity.this, "تم ارسال الموقع", Toast.LENGTH_SHORT).show();
                                                        btnSendMyLocation.setEnabled(true);
                                                        break;
                                                    default:
                                                        Toast.makeText(MapsActivity.this, "فشل في ارسال الرسالة", Toast.LENGTH_SHORT).show();
                                                        btnSendMyLocation.setEnabled(true);
                                                        break;
                                                }
                                            }
                                        }, new IntentFilter("SMS_SENT_ACTION"));

                                        //msg format ($Coordinates: http://maps.google.com/maps?f=q&q=33.1234567,33.1234567&z=16)
                                        String msg = "$Coordinates: http://maps.google.com/maps?f=q&q=" + lat + "," + lng + "&z=16";
                                        SmsManager smsManager = SmsManager.getDefault();
                                        smsManager.getSmsManagerForSubscriptionId(simId).sendTextMessage(destinationAddress, null, msg, sentIntent, null);

                                        fusedLocationClient.removeLocationUpdates(locationCallback);
                                        getLocationTryNumber = 1;
                                    }
                                }
                            };
                            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                        }
                    }
                });

                //make listener on number changed in EditText to grap the contact name
                etxtContactNumber.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        String contactName = getContactUserName(s.toString());
                        if (contactName != null) {
                            txtContactName.setText(contactName);
                        } else {
                            txtContactName.setText("");
                        }
                    }
                });


                //btn to view contacts and grab the number and name
                btnSearchContacts.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        contactNumbers.clear();
                        etxtContactNumber.setText("");
                        txtContactName.setText("");

                        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                        startActivityForResult(intent, RegisterActivity.SendMyLoc_OpenContactsIntentId);
                    }
                });


                //view nextnum number for the grabed contact if exists
                btnNextNumberForContact.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (contactNumbers.size() > 1) {
                            if (contactNumbers.size() == currentIndex + 1) {
                                etxtContactNumber.setText(contactNumbers.get(0));
                                currentIndex = contactNumbers.indexOf(etxtContactNumber.getText().toString());
                            } else {
                                currentIndex++;
                                etxtContactNumber.setText(contactNumbers.get(currentIndex));
                            }
                        }
                    }
                });
            }
        });
    }


    //show dialog on long clicking on the map the send the clicked location
    private void SendClickedLocation(final LatLng latLng) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        LayoutInflater layoutInflater = getLayoutInflater();
        View dialogBuilderView = layoutInflater.inflate(R.layout.sendloc_mapactivitydialog, null);
        builder.setView(dialogBuilderView);

        txtContactName = dialogBuilderView.findViewById(R.id.txtContactName);
        etxtContactNumber = dialogBuilderView.findViewById(R.id.etxtContactNumber);
        Button btnSendMyLocDialog = dialogBuilderView.findViewById(R.id.btnSendMyLocDialog);
        Button btnCancelSendLocDialog = dialogBuilderView.findViewById(R.id.btnCancelSendLocDialog);
        Button btnSearchContacts = dialogBuilderView.findViewById(R.id.btnSearchContacts);
        Button btnNextNumberForContact = dialogBuilderView.findViewById(R.id.btnNextNumberForContact);

        sendMyLocDialog = builder.create();
        sendMyLocDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        sendMyLocDialog.show();

        //cancel connection request dialog
        btnCancelSendLocDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMyLocDialog.dismiss();
            }
        });


        //listener on requesting new connection by btn click
        btnSendMyLocDialog.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                if (etxtContactNumber.getText().toString().equals("")) {
                    Toast.makeText(getBaseContext(), "أدخل رقم المستلم رجاءا", Toast.LENGTH_SHORT).show();
                } else {
                    String destinationNumber = etxtContactNumber.getText().toString();

                    // SMS sent pending intent
                    PendingIntent sentIntent = PendingIntent.getBroadcast(MapsActivity.this, RegisterActivity.CheckSelectedLocSentInSms_BroadcastCode,
                            new Intent("SMS_SENT_ACTION"), 0);

                    registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context arg0, Intent arg1) {
                            int resultCode = getResultCode();
                            switch (resultCode) {
                                case Activity.RESULT_OK:
                                    Toast.makeText(MapsActivity.this, "تم ارسال الموقع", Toast.LENGTH_SHORT).show();
                                    break;
                                default:
                                    Toast.makeText(MapsActivity.this, "فشل في ارسال الرسالة", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }
                    }, new IntentFilter("SMS_SENT_ACTION"));


                    //msg format ($Coordinates: http://maps.google.com/maps?f=q&q=33.1234567,33.1234567&z=16)
                    String msg = "$Coordinates: http://maps.google.com/maps?f=q&q=" + latLng.latitude + "," + latLng.longitude + "&z=16";
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.getSmsManagerForSubscriptionId(simId).sendTextMessage(destinationNumber, null, msg, sentIntent, null);

                    sendMyLocDialog.dismiss();

                }

            }
        });

        //make listener on number changed in EditText to grap the contact name
        etxtContactNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String contactName = getContactUserName(s.toString());
                if (contactName != null) {
                    txtContactName.setText(contactName);
                } else {
                    txtContactName.setText("");
                }
            }
        });


        //btn to view contacts and grab the number and name
        btnSearchContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contactNumbers.clear();
                etxtContactNumber.setText("");
                txtContactName.setText("");

                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, 15);
            }
        });


        //view nextnum number for the grabed contact if exists
        btnNextNumberForContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contactNumbers.size() > 1) {
                    if (contactNumbers.size() == currentIndex + 1) {
                        etxtContactNumber.setText(contactNumbers.get(0));
                        currentIndex = contactNumbers.indexOf(etxtContactNumber.getText().toString());
                    } else {
                        currentIndex++;
                        etxtContactNumber.setText(contactNumbers.get(currentIndex));
                    }
                }
            }
        });
    }


    //Activate Sos
    private void SendSos() {
        btnActivateSos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences sharedPref = MapsActivity.this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                String num1 = sharedPref.getString("LocationFinderSosNum1SharedPrefereces", "");
                String num2 = sharedPref.getString("LocationFinderSosNum2SharedPrefereces", "");
                String num3 = sharedPref.getString("LocationFinderSosNum3SharedPrefereces", "");
                if (num1.equals("") && num2.equals("") && num3.equals("")) {

                    Toast.makeText(MapsActivity.this, "رجاءا اختر ارقام الأستغاثة اولا", Toast.LENGTH_LONG).show();
                } else {
                    Log.d("sos", "clicked");
                    if (!sosStatus) {

                        Log.d("sos", "enabled");
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("LocationFinderSosStatusSharedPrefereces", true);
                        editor.commit();
                        sosStatus = true;

                        sosStartsTimer = new CountDownTimer(5000, 200) {
                            public void onTick(long millisUntilFinished) {

                                //btnActivateSos.setText("بدء خلال : " + String.valueOf(millisUntilFinished / 1000));
                                if (btnActivateSos.getBackground().getConstantState().equals(getDrawable(R.drawable.sos_on).getConstantState()))
                                    btnActivateSos.setBackground(getDrawable(R.drawable.sos_on2));
                                else if (btnActivateSos.getBackground().getConstantState().equals(getDrawable(R.drawable.sos_on2).getConstantState()))
                                    btnActivateSos.setBackground(getDrawable(R.drawable.sos_on));
                            }

                            public void onFinish() {

                                Context context = MapsActivity.this;
                                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                                Intent SosIntent = new Intent(context, SosAlarmReceiver.class);
                                PendingIntent pi = PendingIntent.getBroadcast(context, 0, SosIntent, 0);
                                am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * sosIntervals, pi); // Millisec * Second * Minute

                                btnActivateSos.setBackground(getDrawable(R.drawable.sos_off));
                            }
                        }.start();
                    } else {

                        Log.d("sos", "disabled");
                        Context context = MapsActivity.this;
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("LocationFinderSosStatusSharedPrefereces", false);
                        editor.commit();
                        sosStatus = false;

                        Intent SosIntent = new Intent(context, SosAlarmReceiver.class);
                        PendingIntent sender = PendingIntent.getBroadcast(context, 0, SosIntent, 0);
                        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        alarmManager.cancel(sender);

                        if (sosStartsTimer != null) sosStartsTimer.cancel();
                        btnActivateSos.setBackgroundResource(R.drawable.sos_on);
                    }
                }
            }
        });
    }


    //on activityResult for fetched contact
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case (RegisterActivity.SendMyLoc_OpenContactsIntentId):
                if (resultCode == Activity.RESULT_OK) {

                    Uri uri;
                    Cursor cursor1, cursor2;
                    String TempNameHolder, TempNumberHolder, TempContactID, IDresult = "";
                    int IDresultHolder;

                    uri = data.getData();

                    cursor1 = getContentResolver().query(uri, null, null, null, null);

                    if (cursor1.moveToFirst()) {

                        TempNameHolder = cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                        TempContactID = cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts._ID));

                        IDresult = cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                        IDresultHolder = Integer.valueOf(IDresult);

                        if (IDresultHolder == 1) {

                            cursor2 = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + TempContactID, null, null);

                            while (cursor2.moveToNext()) {

                                TempNumberHolder = cursor2.getString(cursor2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                TempNumberHolder = TempNumberHolder.replace(" ", "");

                                //remove +964 from the prefix of the number
                                if (TempNumberHolder.startsWith("+964")) {

                                    TempNumberHolder = TempNumberHolder.substring(4, TempNumberHolder.length());
                                    TempNumberHolder = "0" + TempNumberHolder;
                                } else if (TempNumberHolder.startsWith("00964")) {

                                    TempNumberHolder = TempNumberHolder.substring(5, TempNumberHolder.length());
                                    TempNumberHolder = "0" + TempNumberHolder;
                                }
                                txtContactName.setText(TempNameHolder);
                                etxtContactNumber.setText(TempNumberHolder);

                                contactNumbers.add(TempNumberHolder);
                                currentIndex = contactNumbers.indexOf(TempNumberHolder);
                            }
                        }

                    }
                }
                break;
        }
    }


    //get the name of the number from the contacts
    private String getContactUserName(String phoneNumber) {

        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;

        ContentResolver contentResolver = getContentResolver();

        // Query and loop for every phone number of the contact
        Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null, null, null, null);

        while (phoneCursor.moveToNext()) {
            String number = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER)).replace(" ", "");
            if (phoneNumber.length() > 3) {
                if
                (
                        phoneNumber.equals(number) ||
                                ("00964" + phoneNumber.substring(1, phoneNumber.length())).equals(number) ||
                                ("+964" + phoneNumber.substring(1, phoneNumber.length())).equals(number)
                ) {
                    String contactName = phoneCursor.getString(phoneCursor.getColumnIndex(DISPLAY_NAME));
                    return contactName;
                }
            }
        }
        phoneCursor.close();

        return null;
    }


    //create option menu in the maps activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.optionmenu_mapsactivity, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.ActivateApp_MapsActivity) {
            ActivateApplication();
            ActivationDialog.show();
        }
        else if(item.getItemId() == R.id.AboutUs_MapsActivity){
            String aboutUsStr =
                    "نبذة عن الشركة:\n" +
                    " \n" +
                    "توفر شركة انظمة اوروك حلول ذكية لمختلف المشاكل التي تواجه القطاعات الحكومية أو القطاع الخاص باستخدام احدث الطرق التكنولوجية \n" +
                    "\n" +
                    "\n" +
                    "حول التطبيق:\n" +
                    " \n" +
                    " تطبيق موقعي وهو تطبيق يستخدم لتحديد المواقع لاستعلام وتحديد مواقع الاشخاص عبر هواتفهم الذكية \n,"+
                    "مميزات التطبيق\n" +
                    "1- يمكن تحديد موقع اي جهة من جهات الاتصال المخزونة لديك بعد ربط هذه الجهة بالتطبيق.\n" +
                    "2- يمكن ارسال موقعك او اي موقع معين يتم تحديده على الخارطة الى اي جهة اتصال حتى في حالة عدم امتلاكه التطبيق\n" +
                    "3- البرنامج يعمل بدون الحاجة للاتصال بالانترنت\n" +
                    "4- يمتلك التطبيق ميزة (SOS) وهي ميزة ارسال نداء استغاثة في حالة التعرض لخطر وذلك بضغطة زر\n" +
                    "5- توفر الشركة فترة تجريبية لمدة 10 ايام و بعدها يمكنك شراء التطبيق بأشتراك سنوي قيمته 5 الاف دينار عراقي تدفع عن طريق تحويل رصيد اتصال\n" +
                    "\n" +
                    "لمزيد من المعلومات مشاهدة الفديو التعليمي في الرابط ادناه:\n" +
                    "https://www.youtube.com/watch?v=gF69B5qlJi8&list=PLvyQs_dnjfSav8iRPqb4D3Np_d-OsJ8dZ\n" +
                    "\n" +
                    "معلومات التواصل - Contact info\n" +
                    "\n" +
                    "* رقم الهاتف:\n" +
                    "07710464594\n" +
                    "\n" +
                    "* الايميل\n" +
                    "info@uruksys.com\n" +
                    "\n" +
                    "*الموقع الرسمي:\n" +
                    "https://www.uruksys.com/\n" +
                    "\n" +
                    "* صفحة الفيس بوك:\n" +
                    "https://www.facebook.com/uruksystemfortechnologytransfer/\n" +
                    "\n" +
                    "* قناة اليوتيوب:\n" +
                    "https://www.youtube.com/channel/UC_uMYLLKHaeuwxhrFIyDcTg\n" +
                    "\n" +
                    "* العنوان:\n" +
                    "شارع فلسطين - ساحة بيروت - شارع الثورة - عمارة النور الطبي - الطابق الثالث\n";

            LayoutInflater inflater = getLayoutInflater();
            View view  = inflater.inflate(R.layout.aboutus_dialog , null);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(view);
            AlertDialog alertDialog = builder.create();
            alertDialog.show();

            TextView t2 = view.findViewById(R.id.txtAboutUs);
            t2.setMovementMethod(LinkMovementMethod.getInstance());
            t2.setText(aboutUsStr);
        }
        return super.onOptionsItemSelected(item);
    }


    //3 listeners on 3 buttons to start activities(ConnectionToActivity , ConnsNReqsActivity , PersonalInfoActivity)
    private void OnStartingOtherActivities() {
        btnOpenPersonalInformation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent personalInfoIntent = new Intent(MapsActivity.this, PersonalInfoActivity.class);
                startActivity(personalInfoIntent);
            }
        });

        btnOpenConnsNReqs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent connsNReqsIntent = new Intent(MapsActivity.this, ConnsAndReqsActivity.class);
                startActivity(connsNReqsIntent);
            }
        });

        btnOpenConnectionTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent connectionsToIntent = new Intent(MapsActivity.this, ConnectionsToActivity.class);
                startActivity(connectionsToIntent);
            }
        });
    }


    private void ActivateApplication() {
        final SharedPreferences sharedPref = this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        String currentExpDateS = sharedPref.getString("LocationFinderExpDateSharedPrefereces", "2019-01-01");


        try {
            Date currentExpDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(currentExpDateS);

            if (Calendar.getInstance(Locale.ENGLISH).getTime().after(currentExpDate)) {

                int amountTransferred = sharedPref.getInt("LocationFinderAmountTransferredToActivateSharedPrefereces", 0);
                String dialogMsg = null;
                if (amountTransferred == 0) {
                    dialogMsg = "سيتم تحويل 5000 دينار عراقي من رصيدك لغرض تفعيل البرنامج سنة كاملة لغاية ";
                } else if (amountTransferred > 0 && amountTransferred < 5000) {
                    int remainedAmount = 5000 - amountTransferred;
                    dialogMsg = "بقي " + remainedAmount + "دينار عراقي لغرض اكمال تفعيل البرنامج سنة كاملة , لغاية ";
                } else {
                    dialogMsg = " هناك خطأ , اتصال بخدمات الزبائن رجاءا لمعالجة الخطأ";
                }

                Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                cal.add(Calendar.MONTH, 12);
                String newExpDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.getTime());

                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setTitle("تفعيل البرنامج")
                        .setMessage(dialogMsg + newExpDate)
                        .setNegativeButton("الغاء", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton("تفعيل", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {


                                if (myPhoneNumber.startsWith("078")) {

                                    // SMS sent pending intent
                                    PendingIntent sentIntent = PendingIntent.getBroadcast(MapsActivity.this, 37,
                                            new Intent("SMS_SENT_ACTION"), 0);

                                    registerReceiver(new BroadcastReceiver() {
                                        @Override
                                        public void onReceive(Context arg0, Intent arg1) {
                                            int resultCode = getResultCode();
                                            switch (resultCode) {
                                                case Activity.RESULT_OK:
                                                    Toast.makeText(MapsActivity.this, "تم ارسال الرسالة", Toast.LENGTH_SHORT).show();

                                                    SharedPreferences.Editor editor = sharedPref.edit();
                                                    editor.putBoolean("LocationFinderWaitActivationSmsSharedPrefereces", true);
                                                    editor.commit();

                                                    JobScheduler jobScheduler = (JobScheduler) getApplicationContext()
                                                            .getSystemService(JOB_SCHEDULER_SERVICE);

                                                    ComponentName componentName = new ComponentName(MapsActivity.this, ActivationWaitJobScheduler.class);

                                                    JobInfo jobInfoObj = new JobInfo.Builder(RegisterActivity.ActivationWaitJobSchedulerId, componentName)
                                                            .setMinimumLatency(120 * 1000).setOverrideDeadline(120 * 1000).build();

                                                    jobScheduler.schedule(jobInfoObj);

                                                    break;
                                                default:

                                                    Toast.makeText(MapsActivity.this, "فشل في ارسال الرسالة", Toast.LENGTH_SHORT).show();

                                                    break;
                                            }
                                        }
                                    }, new IntentFilter("SMS_SENT_ACTION"));

                                    SmsManager smsManager = SmsManager.getDefault();
                                    smsManager.getSmsManagerForSubscriptionId(simId).sendTextMessage("21112", null, "07818650366 5000", sentIntent, null);


                                } else if (myPhoneNumber.startsWith("077")) {

                                    //maximum old transferred amount = 4000 , see mySmsReceiver class
                                    int oldTransferredAmount = sharedPref.getInt("LocationFinderAmountTransferredToActivateSharedPrefereces", 0);
                                    int requiredAmount = 5000 - oldTransferredAmount;
                                    String dialNumber = "*123*"+requiredAmount+"*07710464594#";

                                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", dialNumber , null)));
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putBoolean("LocationFinderWaitActivationSmsSharedPrefereces", true);
                                    editor.commit();

                                    JobScheduler jobScheduler = (JobScheduler) getApplicationContext()
                                            .getSystemService(JOB_SCHEDULER_SERVICE);

                                    ComponentName componentName = new ComponentName(MapsActivity.this, ActivationWaitJobScheduler.class);

                                    JobInfo jobInfoObj = new JobInfo.Builder(RegisterActivity.ActivationWaitJobSchedulerId, componentName)
                                            .setMinimumLatency(120 * 1000).setOverrideDeadline(120 * 1000).build();

                                    jobScheduler.schedule(jobInfoObj);

                                } else if (myPhoneNumber.startsWith("075")) {

                                    //maximum old transferred amount = 4000 , see mySmsReceiver class
                                    int oldTransferredAmount = sharedPref.getInt("LocationFinderAmountTransferredToActivateSharedPrefereces", 0);
                                    int requiredAmount = 5000 - oldTransferredAmount;
                                    String dialNumber = "*215*07519602547*"+requiredAmount+"#";

                                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", dialNumber , null)));
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putBoolean("LocationFinderWaitActivationSmsSharedPrefereces", true);
                                    editor.commit();

                                    JobScheduler jobScheduler = (JobScheduler) getApplicationContext()
                                            .getSystemService(JOB_SCHEDULER_SERVICE);

                                    ComponentName componentName = new ComponentName(MapsActivity.this, ActivationWaitJobScheduler.class);

                                    JobInfo jobInfoObj = new JobInfo.Builder(RegisterActivity.ActivationWaitJobSchedulerId, componentName)
                                            .setMinimumLatency(120 * 1000).setOverrideDeadline(120 * 1000).build();

                                    jobScheduler.schedule(jobInfoObj);
                                }
                            }
                        });

                ActivationDialog = builder.create();

                ActivationDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {

                        try {
                            Boolean WaitActivitionSms = sharedPref.getBoolean("LocationFinderWaitActivationSmsSharedPrefereces", false);

                            String expDateS = sharedPref.getString("LocationFinderExpDateSharedPrefereces", "2019-01-01");
                            Date expDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(expDateS);
                            if (!WaitActivitionSms && Calendar.getInstance().getTime().after(expDate)) {

                                ActivationDialog.show();
                            } else if (WaitActivitionSms && Calendar.getInstance().getTime().after(expDate)) {

                                LayoutInflater layoutInflater = getLayoutInflater();
                                View view = layoutInflater.inflate(R.layout.loading_dialog, null);

                                TextView txtMessageInDialog = view.findViewById(R.id.txtMessageInDialog);
                                txtMessageInDialog.setText("بانتظار تاكيد التفعيل");

                                AlertDialog.Builder builder1 = new AlertDialog.Builder(MapsActivity.this);
                                builder1.setView(view);
                                ActivationLoadingDialog = builder1.create();
                                ActivationLoadingDialog.show();

                                ActivationLoadingDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        if (sharedPref.getBoolean("LocationFinderWaitActivationSmsSharedPrefereces", false)) {
                                            ActivationLoadingDialog.show();
                                        }
                                    }
                                });
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                            Toast.makeText(MapsActivity.this, "error", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("تفعيل البرنامج")
                        .setMessage("تنتهي صلاحية البرنامج بتاريخ " + currentExpDateS)
                        .setNegativeButton("الغاء", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                ActivationDialog = builder.create();

            }
        } catch (ParseException e) {
            e.printStackTrace();

            Log.d("CheckUserActivation", "error");
        }


    }


    //check the account imei and the phone number in server and the expiration date in server
    @SuppressLint("MissingPermission")   //for getting device imei (permission already granted)
    private void CheckAccountNActivation() {
        JSONObject phoneNumberJson = new JSONObject();
        try {
            Context context = MapsActivity.this;
            final SharedPreferences sharedPref = context.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
            String expDateS = sharedPref.getString("LocationFinderExpDateSharedPrefereces", "2019-01-01");

            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(RegisterActivity.TELEPHONY_SERVICE);
            String imei;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                imei = telephonyManager.getImei();
            } else {
                imei = telephonyManager.getDeviceId();
            }

            phoneNumberJson.put("PhoneNumber", myPhoneNumber);
            phoneNumberJson.put("ExpirationDate", expDateS);
            phoneNumberJson.put("imei", imei);
            new CheckAccountNActivationByPost().execute(phoneNumberJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private class CheckAccountNActivationByPost extends AsyncTask<JSONObject, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(JSONObject... jsonObjects) {
            RequestBody rBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/CheckImeiNExpiration")
                    .post(rBody)
                    .build();

            Response response = null;
            JSONObject jsonObject = null;
            try {
                response = client.newCall(request).execute();
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

            try {
                String responseMsg = jsonObject.get("msg").toString();

                if (responseMsg.equals("wrong imei")) {

                    Toast.makeText(MapsActivity.this, "لقد نقل هذا الحساب الى جهاز اخر , سيتم حذف حسابك من هذا الهاتف", Toast.LENGTH_SHORT).show();

                    SharedPreferences sharedPref = MapsActivity.this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("LocationFinderPhoneSharedPrefereces", "notRegistered");
                    editor.putInt("LocationFinderSimIdSharedPrefereces", 0);
                    editor.putString("LocationFinderUserNameSharedPrefereces", "notRegistered");
                    editor.putInt("LocationFinderWaitToSendSmsSharedPrefereces", 60);
                    editor.putBoolean("LocationFinderSosStatusSharedPrefereces", false);
                    editor.putInt("LocationFinderSosIntervalsSharedPrefereces", 10);
                    editor.putString("LocationFinderSosNum1SharedPrefereces", "");
                    editor.putString("LocationFinderSosNum2SharedPrefereces", "");
                    editor.putString("LocationFinderSosNum3SharedPrefereces", "");
                    editor.putString("LocationFinderTokenKeySharedPrefereces", "notRegistered");
                    editor.commit();


                    MySqliteDB mySqliteDB = new MySqliteDB(MapsActivity.this);
                    mySqliteDB.DeleteAllConnections();


                    finish();
                    stopService(foregroundServiceIntent);
                    stopService(FCMservice);

                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);

                } else {
                    Log.d("checkingExp", "noProblem");
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */


    //testing code to show current location in the google map  ....part 1
    /*LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    FusedLocationProviderClient mFusedLocationClient;*/
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //check if the activity started with intent that carries retrieved location(started from notification)
        Intent intent = getIntent();
        String responderUserName = intent.getStringExtra("responderUserName");
        Bundle b = intent.getBundleExtra("Location");
        if (b != null) {
            Location lastKnownLoc = (Location) b.getParcelable("Location");
            SetReceivedLocation(lastKnownLoc, responderUserName);
        }

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                Log.d("currentLatLng", "lat: " + latLng.latitude + ", lng: " + latLng.longitude);
                SendClickedLocation(latLng);

            }
        });


        //testing code to show current location in the google map  ....part 2
        /*mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000); // two minute interval
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        else {
            fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }*/
    }

    //testing code to show current location in the google map  ....part 3
    /*LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.d("fusedgettinglocation", "MapsActivity/Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Current Position");
                mCurrLocationMarker = mMap.addMarker(markerOptions);

                //move map camera
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
            }
        }
    };*/


    private void AttachUserConnectionInSpinner() {
        ArrayList<String> connectedUserNamesList = new ArrayList<>();
        connectedUserNamesList.add("اختر الشخص المراد معرفة موقعه");
        for (UserInfoModel userInfoModel : connectionsList) {
            connectedUserNamesList.add(userInfoModel.getUserName());
        }

        ArrayAdapter<String> AgeSpinnerAdapter = new ArrayAdapter<String>(this, R.layout.spinner_layout, connectedUserNamesList) {
            @Override
            public boolean isEnabled(int position) {
                if (position == 0) {
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

                View view = super.getDropDownView(position, convertView, parent);
                TextView TV = (TextView) view;
                if (position == 0) {
                    TV.setTextColor(Color.GRAY);
                } else {
                    TV.setTextColor(Color.BLACK);
                }
                return view;
            }
        };

        AgeSpinnerAdapter.setDropDownViewResource(R.layout.spinner_layout);
        spinConnectedUserNames.setAdapter(AgeSpinnerAdapter);
    }


    //when selecting username in the spinner to get his/her location show their number in textView
    private void OnSelectingUserNameInSpinner() {
        spinConnectedUserNames.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedUser = spinConnectedUserNames.getSelectedItem().toString();

                if (selectedUser.equals("اختر الشخص المراد معرفة موقعه"))
                    return;

                for (UserInfoModel infoModel : connectionsList) {
                    if (infoModel.getUserName().equals(selectedUser)) {
                        txtSelectedUserPhone.setText(infoModel.getPhoneNumber());

                        selectedPhone = infoModel.getPhoneNumber();
                        selectedTokenKey = infoModel.getTokenKey();

                        try {
                            connectTime_From = new SimpleDateFormat("HH:mm").parse(infoModel.getConnectTime_From());
                            connectTime_To = new SimpleDateFormat("HH:mm").parse(infoModel.getConnectTime_To());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    //when user requested location of the selected user in spinner
    private void OnRequestingLocation() {
        btnGetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Date myDate = Calendar.getInstance().getTime();
                String myDateStr = new SimpleDateFormat("HH:mm").format(myDate);
                try {
                    myDate = new SimpleDateFormat("HH:mm").parse(myDateStr);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (selectedPhone != null && myDate.before(connectTime_To) && myDate.after(connectTime_From)) {
                    if (isConnected) {
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("responderPhoneNumber", selectedPhone);
                            jsonObject.put("tokenKey", selectedTokenKey);
                            jsonObject.put("requesterPhoneNumber", myPhoneNumber);

                            new GetRequestingLocationByPost().execute(jsonObject);

                            btnGetLocation.setEnabled(false);
                            countDownTimer = new CountDownTimer(SmsRequest_WaitTime * 1000, 1000) {
                                public void onTick(long millisUntilFinished) {
                                    btnGetLocation.setText(String.valueOf(millisUntilFinished / 1000));
                                    //here you can have your logic to set text to edit text
                                }

                                public void onFinish() {
                                    btnGetLocation.setEnabled(true);
                                    //btnGetLocation.setText("طلب الموقع");
                                }
                            }.start();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //String msg = "ليس لديك خدمة انترنت , هل ترغب بمعرفة الموقع عن طريق رسالة نصية";
                        String msg = "هل ترغب بمعرفة الموقع عن طريق رسالة نصية";
                        GetLocationBySMS(selectedPhone, msg);
                    }
                } else if (selectedPhone != null && (myDate.after(connectTime_To) || myDate.before(connectTime_From))) {
                    Toast.makeText(MapsActivity.this, "ليس لديك صلاحية معرفة موقع هذا الشخص في هذا الوقت", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    //send location request using post
    private class GetRequestingLocationByPost extends AsyncTask<JSONObject, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(JSONObject... jsonObjects) {
            RequestBody rBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/GetLocation")
                    .post(rBody)
                    .build();

            Response response = null;
            JSONObject jsonObject = null;
            try {
                response = client.newCall(request).execute();
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

            try {
                String responseMsg = jsonObject.get("msg").toString();
                Toast.makeText(MapsActivity.this, responseMsg, Toast.LENGTH_SHORT).show();

                //make scheduler for sending SMS in case target has no internet connection
                JobScheduler jobScheduler = (JobScheduler) getApplicationContext()
                        .getSystemService(JOB_SCHEDULER_SERVICE);

                ComponentName componentName = new ComponentName(MapsActivity.this, SmsJobScheduler.class);

                PersistableBundle pb = new PersistableBundle();
                pb.putString("targetPhone", selectedPhone);
                pb.putString("targetUserName", selectedUser);

                JobInfo jobInfoObj = new JobInfo.Builder(RegisterActivity.SmsJobSchedulerId, componentName)
                        .setMinimumLatency((SmsRequest_WaitTime - 2) * 1000).setOverrideDeadline((SmsRequest_WaitTime - 2) * 1000).setExtras(pb).build();

                jobScheduler.schedule(jobInfoObj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    //get connected users with the current user from the DB by node js (post)
    @SuppressLint("MissingPermission")   //for getting device imei (permission already granted)
    private void GetConnectedUsers() {
        //get connected users online if there is connection

        spinConnectedUserNames.setAdapter(null);
        txtSelectedUserPhone.setText("رقم الهاتف");
        selectedPhone = null;
        connectionsList.clear();

        if (isConnected) {
            JSONObject phoneNumberJson = new JSONObject();
            try {

                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(RegisterActivity.TELEPHONY_SERVICE);
                String imei;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    imei = telephonyManager.getImei();
                } else {
                    imei = telephonyManager.getDeviceId();
                }

                phoneNumberJson.put("PhoneNumber", myPhoneNumber);
                phoneNumberJson.put("imei", imei);
                new GetConnectionsByPost().execute(phoneNumberJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {  //get connected users offline if there is no connection

            MySqliteDB mySqliteDB = new MySqliteDB(this);
            Cursor c = mySqliteDB.GetConnectionsTo(myPhoneNumber);

            try {
                if (c.getCount() > 0) {
                    if (c.moveToFirst()) {
                        do {
                            connectionsList.add(new UserInfoModel
                                    (
                                            c.getString(c.getColumnIndex("AccessToUserName")),
                                            c.getString(c.getColumnIndex("AccessToPhone")),
                                            "0",
                                            c.getString(c.getColumnIndex("ConnectTime_From")),
                                            c.getString(c.getColumnIndex("ConnectTime_To"))
                                    ));
                        } while (c.moveToNext());
                    }
                    c.close();

                    AttachUserConnectionInSpinner();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    //get the connections using post
    private class GetConnectionsByPost extends AsyncTask<JSONObject, Void, String> {

        @Override
        protected String doInBackground(JSONObject... jsonObjects) {
            RequestBody requestBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/getConnectedUserConnectionTo")
                    .post(requestBody)
                    .build();

            Response response = null;
            String responseBody = null;
            try {
                response = client.newCall(request).execute();
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);

                responseBody = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseBody;
        }

        @Override
        protected void onPostExecute(String responseBody) {
            super.onPostExecute(responseBody);

            try {
                JSONArray jsonArray = new JSONArray(responseBody);
                JSONObject explrObject;
                for (int i = 0; i < jsonArray.length(); i++) {
                    explrObject = jsonArray.getJSONObject(i);
                    connectionsList.add(new UserInfoModel
                            (
                                    explrObject.get("AccessToUserName").toString(),
                                    explrObject.get("AccessToPhone").toString(),
                                    explrObject.get("TokenKey").toString(),
                                    explrObject.get("connectTime_From").toString(),
                                    explrObject.get("connectTime_To").toString()
                            ));
                }
                AttachUserConnectionInSpinner();
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    String responseMsg = jsonObject.get("msg").toString();

                    if (responseMsg.equals("wrong imei")) {

                        Toast.makeText(MapsActivity.this, "لقد نقل هذا الحساب الى جهاز اخر , سيتم حذف حسابك من هذا الهاتف", Toast.LENGTH_SHORT).show();

                        SharedPreferences sharedPref = MapsActivity.this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("LocationFinderPhoneSharedPrefereces", "notRegistered");
                        editor.putInt("LocationFinderSimIdSharedPrefereces", 0);
                        editor.putString("LocationFinderUserNameSharedPrefereces", "notRegistered");
                        editor.putInt("LocationFinderWaitToSendSmsSharedPrefereces", 60);
                        editor.putBoolean("LocationFinderSosStatusSharedPrefereces", false);
                        editor.putInt("LocationFinderSosIntervalsSharedPrefereces", 10);
                        editor.putString("LocationFinderSosNum1SharedPrefereces", "");
                        editor.putString("LocationFinderSosNum2SharedPrefereces", "");
                        editor.putString("LocationFinderSosNum3SharedPrefereces", "");
                        editor.putString("LocationFinderTokenKeySharedPrefereces", "notRegistered");
                        editor.commit();


                        MySqliteDB mySqliteDB = new MySqliteDB(MapsActivity.this);
                        mySqliteDB.DeleteAllConnections();


                        finish();
                        stopService(foregroundServiceIntent);
                        stopService(FCMservice);

                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(0);

                    } else {

                        Toast.makeText(MapsActivity.this, responseMsg, Toast.LENGTH_SHORT).show();
                    }

                    spinConnectedUserNames.setAdapter(null);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }

        }
    }


    //reset the shared preferences (simId , smsDelay) on activity starts, in case user changes personal information
    @Override
    protected void onStart() {
        super.onStart();

        //get the information of the user from the sharedPreferences
        Context context = MapsActivity.this;
        SharedPreferences sharedPref = context.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        simId = sharedPref.getInt("LocationFinderSimIdSharedPrefereces", 0);
        SmsRequest_WaitTime = sharedPref.getInt("LocationFinderWaitToSendSmsSharedPrefereces", 60);
        myPhoneNumber = sharedPref.getString("LocationFinderPhoneSharedPrefereces", "notRegistered");
        sosIntervals = sharedPref.getInt("LocationFinderSosIntervalsSharedPrefereces", 10);
    }


    //check if the application is granted access to location service
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("الوصول للموقع")
                        .setMessage("هذا البرنامج يحتاج الاذن للوصول الى الموقع")
                        .setPositiveButton("موافق", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this
                                        , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}
                                        , RegisterActivity.MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        RegisterActivity.MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }


    //check if the application is granted access to contacts
    private void checkContactsPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("الوصول للاسماء")
                        .setMessage("هذا البرنامج يحتاج الاذن للوصول الى سجل الاسماء")
                        .setPositiveButton("موافق", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this
                                        , new String[]{Manifest.permission.READ_CONTACTS}
                                        , RegisterActivity.MY_PERMISSION_REQUEST_READ_CONTACTS);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        RegisterActivity.MY_PERMISSION_REQUEST_READ_CONTACTS);
            }
        }
    }


    //check if the application is granted access to Read SMS
    private void checkReadSMSPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("الوصول للرسائل")
                        .setMessage("هذا البرنامج يحتاج الاذن للوصول الى الرسائل")
                        .setPositiveButton("موافق", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this
                                        , new String[]{Manifest.permission.READ_SMS}
                                        , RegisterActivity.MY_PERMISSION_REQUEST_Read_SMS);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_SMS},
                        RegisterActivity.MY_PERMISSION_REQUEST_Read_SMS);
            }
        }
    }


    //check if the application is granted access to send SMS
    public void checkSendSMSPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("ارسال الرسائل")
                        .setMessage("هذا البرنامج يحتاج الاذن لارسال رسائل")
                        .setPositiveButton("موافق", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this
                                        , new String[]{Manifest.permission.SEND_SMS}
                                        , RegisterActivity.MY_PERMISSION_REQUEST_SEND_SMS);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        RegisterActivity.MY_PERMISSION_REQUEST_SEND_SMS);
            }
        }
    }


    //check if the application is granted access to Read Phone State
    private void checkReadPhoneStatePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("حالة الهاتف")
                        .setMessage("هذا البرنامج يحتاج الاذن للوصول الى حالة الهاتف")
                        .setPositiveButton("موافق", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this
                                        , new String[]{Manifest.permission.READ_PHONE_STATE}
                                        , RegisterActivity.MY_PERMISSION_REQUEST_READ_PHONE_STATE);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        RegisterActivity.MY_PERMISSION_REQUEST_READ_PHONE_STATE);
            }
        }
    }


    //check if the application is granted access to receive SMS
    private void checkReceiveSMSPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_SMS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("استلام الرسائل")
                        .setMessage("هذا البرنامج يحتاج الاذن لاستلام رسائل")
                        .setPositiveButton("موافق", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this
                                        , new String[]{Manifest.permission.RECEIVE_SMS}
                                        , RegisterActivity.MY_PERMISSION_REQUEST_RECEIVE_SMS);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECEIVE_SMS},
                        RegisterActivity.MY_PERMISSION_REQUEST_RECEIVE_SMS);
            }
        }
    }


    private void Check9Permissions() {
        if
        (
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) ||
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) ||
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) ||
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) ||
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                        //(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) ||
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED) ||
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) ||
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.DISABLE_KEYGUARD) != PackageManager.PERMISSION_GRANTED)
        ) {

            // Should we show an explanation?
            if (
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_SMS) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) ||
                            //ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WAKE_LOCK) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.DISABLE_KEYGUARD)
            ) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(MapsActivity.this
                        , new String[]{
                                Manifest.permission.READ_SMS,
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.SEND_SMS,
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                //Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.DISABLE_KEYGUARD,
                                Manifest.permission.WAKE_LOCK,
                                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                                Manifest.permission.READ_PHONE_STATE}
                        , RegisterActivity.MY_PERMISSION_For_9_REQUESTS);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_SMS,
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.SEND_SMS,
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                //Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.DISABLE_KEYGUARD,
                                Manifest.permission.WAKE_LOCK,
                                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                                Manifest.permission.READ_PHONE_STATE}
                        , RegisterActivity.MY_PERMISSION_For_9_REQUESTS);
            }
        }
    }


    //if user didn't grant the application access to the GPS then close the application
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case RegisterActivity.MY_PERMISSIONS_REQUEST_LOCATION:   //checking location access permission
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return;
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("تنويه")
                            .setMessage("نعتذر , لا يمكنك استخدام البرنامج اذا لم تسمح له بالوصول الى الموقع")
                            .setPositiveButton("اغلاق البرنامج", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    finish();
                                    System.exit(0);
                                }
                            })
                            .create()
                            .show();
                }
                break;
            case RegisterActivity.MY_PERMISSION_REQUEST_READ_CONTACTS:   //checking contacts access permission
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return;
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("تنويه")
                            .setMessage("نعتذر , لا يمكنك استخدام البرنامج اذا لم تسمح له بالوصول الى الاسماء")
                            .setPositiveButton("اغلاق البرنامج", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    finish();
                                    System.exit(0);
                                }
                            })
                            .create()
                            .show();
                }
                break;
            case RegisterActivity.MY_PERMISSION_REQUEST_Read_SMS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return;
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("تنويه")
                            .setMessage("نعتذر , لا يمكنك استخدام البرنامج اذا لم تسمح له بالوصول الى الرسائل")
                            .setPositiveButton("اغلاق البرنامج", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    finish();
                                    System.exit(0);
                                }
                            })
                            .create()
                            .show();
                }
                break;
            case RegisterActivity.MY_PERMISSION_REQUEST_SEND_SMS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return;
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("تنويه")
                            .setMessage("نعتذر , لا يمكنك استخدام البرنامج اذا لم تسمح له بالوصول الى الرسائل")
                            .setPositiveButton("اغلاق البرنامج", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    finish();
                                    System.exit(0);
                                }
                            })
                            .create()
                            .show();
                }
                break;
            case RegisterActivity.MY_PERMISSION_REQUEST_RECEIVE_SMS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return;
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("تنويه")
                            .setMessage("نعتذر , لا يمكنك استخدام البرنامج اذا لم تسمح له بارسال الرسائل")
                            .setPositiveButton("اغلاق البرنامج", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    finish();
                                    System.exit(0);
                                }
                            })
                            .create()
                            .show();
                }
                break;
/*            case RegisterActivity.MY_PERMISSION_REQUEST_READ_PHONE_STATE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return;
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("تنويه")
                            .setMessage("نعتذر , لا يمكنك استخدام البرنامج اذا لم تسمح له بالوصول الى حالة الهاتف")
                            .setPositiveButton("اغلاق البرنامج", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    finish();
                                    System.exit(0);
                                }
                            })
                            .create()
                            .show();
                }
                break;*/
            case RegisterActivity.MY_PERMISSION_For_9_REQUESTS:
                for (int i = 0; i < permissions.length; i++) {
                    Log.d("myPermission", permissions[i] + " ," + grantResults[i]);
                    if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {

                    } else {

                        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this)
                                .setTitle("تنويه")
                                .setMessage("نعتذر , لا يمكنك استخدام البرنامج اذا لم توافق على طلبات النظام");

                        final AlertDialog alertDialog = builder.create();

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                alertDialog.dismiss();
                                finish();
                                System.exit(0);

                            }
                        }, 4000);

                        alertDialog.show();
                    }
                }
                break;
        }
    }
}


class UserInfoModel {
    private String userName, phoneNumber, tokenKey, connectTime_From, connectTime_To;

    public UserInfoModel(String userName, String phoneNumber, String tokenKey, String connectTime_From, String connectTime_To) {
        this.phoneNumber = phoneNumber;
        this.tokenKey = tokenKey;
        this.userName = userName;
        this.connectTime_From = connectTime_From;
        this.connectTime_To = connectTime_To;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getTokenKey() {
        return tokenKey;
    }

    public void setTokenKey(String tokenKey) {
        this.tokenKey = tokenKey;
    }

    public String getConnectTime_From() {
        return connectTime_From;
    }

    public void setConnectTime_From(String connectTime_From) {
        this.connectTime_From = connectTime_From;
    }

    public String getConnectTime_To() {
        return connectTime_To;
    }

    public void setConnectTime_To(String connectTime_To) {
        this.connectTime_To = connectTime_To;
    }
}


class SimInfo {
    private int id_;
    private String display_name;
    private String icc_id;
    private int sim_id;

    public SimInfo(int id_, String display_name, String icc_id, int sim_id) {
        this.id_ = id_;
        this.display_name = display_name;
        this.icc_id = icc_id;
        this.sim_id = sim_id;
    }

    public int getId_() {
        return id_;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public String getIcc_id() {
        return icc_id;
    }

    public int getSim_id() {
        return sim_id;
    }

    @Override
    public String toString() {
        return "SimInfo{" +
                "id_=" + id_ +
                ", display_name='" + display_name + '\'' +
                ", icc_id='" + icc_id + '\'' +
                ", sim_id= " + sim_id +
                '}';
    }
}