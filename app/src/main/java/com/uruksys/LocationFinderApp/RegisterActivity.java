package com.uruksys.LocationFinderApp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public static final int MY_PERMISSION_REQUEST_READ_CONTACTS = 98;
    public static final int MY_PERMISSION_REQUEST_Read_SMS = 97;
    public static final int MY_PERMISSION_REQUEST_SEND_SMS = 96;
    public static final int MY_PERMISSION_REQUEST_READ_PHONE_STATE = 95;
    public static final int MY_PERMISSION_REQUEST_RECEIVE_SMS = 94;
    public static final int MY_PERMISSION_For_9_REQUESTS = 93;

    public static final int RegCodeDelayAlarmManagerId = 112;
    public static final int SmsJobSchedulerId = 1;
    public static final int ActivationWaitJobSchedulerId = 17;

    public static final int SendMyLoc_OpenContactsIntentId = 15;

    public static final int requestCode_AppActivated = 312;
    public static final int requestCode_GPS_Off_Flag = 313;
    public static final int requestCode_LocationReceivedViaSms = 314;

    public final static int SosNumberCode1 = 45;
    public final static int SosNumberCode2 = 46;
    public final static int SosNumberCode3 = 47;

    public static final int CheckCurrentLocSentInSms_BroadcastCode = 62;
    public static final int CheckSelectedLocSentInSms_BroadcastCode = 63;

    private double lat = 0;
    private double lng = 0;
    private String tokenKey = null;
    private String userName = null;
    private String phoneNumber = null;
    private int simId = 0;

    public static String serverIp = "http://localhost:3000/LocationFinderApp";
    public static String serverIp_ForRegister = "http://localhost:3001/LocationFinderApp";
    public static MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static OkHttpClient client = new OkHttpClient();


    private EditText etxtPhoneNumber = null;
    private Spinner simIdSpinner = null;
    private EditText etxtUserName = null;
    private Button btnRegister = null;
    private ProgressBar pb = null;

    //views for registration code dialog
    private TextView txtDialogRegTitle = null, txtTimerDelay = null, txtReqCode_Tries = null;
    private Button btnCancelRegCode = null;
    private Button btnSubmitRegCode = null;
    private Button btnRequestRegCode = null;
    private EditText etxtDialogRegCode = null;
    AlertDialog registrationCodeDialog = null;
    private int timesOfCodeRequested = 3;
    private CountDownTimer waitNextCodeReqTimer = null;

    private BroadcastReceiver ResetRegCodeTriesMessageReceiver = null;

    private static final String TAG = RegisterActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        pb = (ProgressBar) findViewById(R.id.progressBar1);
        pb.setVisibility(View.INVISIBLE);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        etxtUserName = (EditText) findViewById(R.id.etxtUserName);
        etxtPhoneNumber = (EditText) findViewById(R.id.etxtPhoneNumber);
        simIdSpinner = (Spinner) findViewById(R.id.simIdSpinner);

        //start intro gif image of the application
        Intent intent2 = new Intent(getBaseContext(), IntroScreenActivity.class);
        startActivity(intent2);

        //cancel alarm manager
        SharedPreferences sharedPref = RegisterActivity.this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        if (sharedPref.getInt("LocationFinderRegCodeWaitSharedPrefereces", 0) == 0) {

            //cancel alarm manager
            Intent intent = new Intent(this, RegCodeAlarmReceiver.class);
            PendingIntent sender = PendingIntent.getBroadcast(this, RegCodeDelayAlarmManagerId, intent, 0);
            AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(sender);
        }
    }


    @Override
    protected void onRestart() {
        super.onRestart();

        if (!CheckUserRegistered()) {

            //check permissions if user not registered before
            //checkLocationPermission();
            //checkContactsPermission();
            //checkReadSMSPermission();
            //checkSendSMSPermission();
            //checkReceiveSMSPermission();
            //checkReadPhoneStatePermission();
            Check9Permissions();

            GetSimCards();
            GetNewToken();
            RegisterNewUserProcess();

            ReceiveBroadcastResetRegTries();

            LocalBroadcastManager.getInstance(this).registerReceiver(
                    ResetRegCodeTriesMessageReceiver, new IntentFilter("ResetRegCodeTries"));
        }
    }


    //onReceiving broadcast to reset tries of requesting registration code
    private void ReceiveBroadcastResetRegTries() {
        ResetRegCodeTriesMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                SharedPreferences sharedPref = RegisterActivity.this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                int waitTime = sharedPref.getInt("LocationFinderRegCodeWaitSharedPrefereces", 0);
                if (waitTime > 0) {
                    if (btnRequestRegCode != null) {

                        btnRequestRegCode.setAlpha((float) 0.5);
                        btnRequestRegCode.setEnabled(false);
                    }
                    if (txtReqCode_Tries != null) {

                        String msg1 = "لقد استنفذت جميع المحاولات ";
                        String msg2 = "انتظر " + waitTime + " دقائق...";
                        txtReqCode_Tries.setText(msg1 + "\n" + msg2);
                    }
                } else {

                    timesOfCodeRequested = 3;
                    if (txtReqCode_Tries != null) {

                        txtReqCode_Tries.setText("عدد المحاولات المتبقية : " + timesOfCodeRequested);
                    }
                    if (btnRequestRegCode != null) {

                        btnRequestRegCode.setAlpha(1);
                        btnRequestRegCode.setEnabled(true);
                    }

                    //cancel alarm manager
                    Intent intent2 = new Intent(RegisterActivity.this, RegCodeAlarmReceiver.class);
                    PendingIntent sender = PendingIntent.getBroadcast(RegisterActivity.this, RegCodeDelayAlarmManagerId, intent2, 0);
                    AlarmManager alarmManager = (AlarmManager) RegisterActivity.this.getSystemService(Context.ALARM_SERVICE);
                    alarmManager.cancel(sender);
                }
            }
        };
    }


    //check user registered to the application before
    private boolean CheckUserRegistered() {
        Context context = RegisterActivity.this;
        SharedPreferences sharedPref = context.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        phoneNumber = sharedPref.getString("LocationFinderPhoneSharedPrefereces", "notRegistered");
        simId = sharedPref.getInt("LocationFinderSimIdSharedPrefereces", 0);
        userName = sharedPref.getString("LocationFinderUserNameSharedPrefereces", "notRegistered");
        tokenKey = sharedPref.getString("LocationFinderTokenKeySharedPrefereces", "notRegistered");
        // sharedPref.edit().clear().commit();

        if (! phoneNumber.equals("notRegistered")) {   //if phoneNumber equals "notRegistered" means the user is not registered
            this.finish();
            Intent intent = new Intent(getBaseContext(), MapsActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }


    //get sim cards in the phone and populate them in spinner
    private void GetSimCards() {

        //get sim cards info
        final List<SimInfo> simInfoList = new ArrayList<>();
        Uri URI_TELEPHONY = Uri.parse("content://telephony/siminfo/");
        Cursor c = this.getContentResolver().query(URI_TELEPHONY, null, "sim_id != -1", null, null);


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

        ArrayList<String> simList = new ArrayList<>();
        simList.add("اختر الشريحة");
        for (SimInfo simInfo : simInfoList) {
            simList.add(simInfo.getDisplay_name() + " " + simInfo.getSim_id());
        }

        ArrayAdapter<String> AgeSpinnerAdapter = new ArrayAdapter<String>(this, R.layout.spinner_layout, simList) {
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
        simIdSpinner.setAdapter(AgeSpinnerAdapter);
    }


    //create new token to the user
    private void GetNewToken() {
        Log.d("GetNewToken", "getNewTokenKey");
        if (tokenKey.equals("notRegistered")) {
            FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                @Override
                public void onComplete(@NonNull Task<InstanceIdResult> task) {
                    if (!task.isSuccessful()) {
                        Log.d("GetNewToken", "failed!!!!!");
                        return;
                    }
                    // Get new Instance ID token
                    tokenKey = task.getResult().getToken();
                    Log.d(TAG, tokenKey);

                    SharedPreferences sharedPref = RegisterActivity.this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("LocationFinderTokenKeySharedPrefereces", tokenKey);
                    editor.commit();
                }
            });
        }
    }


    //register new user
    private void RegisterNewUserProcess() {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (
                        etxtPhoneNumber.getText().toString().equals("")
                                || (simIdSpinner.getSelectedItem().toString().equals("اختر الشريحة"))
                                || etxtUserName.getText().toString().equals("")
                ) {
                    Toast.makeText(RegisterActivity.this, "أدخل الحقول كاملة رجاءا", Toast.LENGTH_SHORT).show();
                } else if (tokenKey.equals("notRegistered")) {
                    Toast.makeText(RegisterActivity.this, "فشل في التسجيل حاول مرة اخرى", Toast.LENGTH_SHORT).show();
                    GetNewToken();
                } else {
                    pb.setVisibility(View.VISIBLE);

                    phoneNumber = etxtPhoneNumber.getText().toString();
                    simId = Integer.parseInt(simIdSpinner.getSelectedItem().toString().substring(simIdSpinner.getSelectedItem().toString().length() - 1));
                    userName = etxtUserName.getText().toString().trim();

                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("phoneNumber", phoneNumber);

                        new CheckUserExistsAsync().execute(jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
    }


    //async check user has old account for this number
    private class CheckUserExistsAsync extends AsyncTask<JSONObject, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(JSONObject... jsonObjects) {
            RequestBody requestBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/checkUserExist")
                    .post(requestBody)
                    .build();

            JSONObject json = null;
            try {
                Response response = client.newCall(request).execute();
                String s = response.body().string();
                json = new JSONObject(s);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return json;
        }


        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);

            pb.setVisibility(View.INVISIBLE);
            try {

                String response = jsonObject.get("msg").toString();

                AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                View view = inflater.inflate(R.layout.registeration_code_entry, null);

                builder.setView(view);

                txtDialogRegTitle = view.findViewById(R.id.txtDialogRegTitle);
                btnCancelRegCode = view.findViewById(R.id.btnCancelRegCode);
                btnSubmitRegCode = view.findViewById(R.id.btnSubmitRegCode);
                btnRequestRegCode = view.findViewById(R.id.btnRequestRegCode);
                txtReqCode_Tries = view.findViewById(R.id.txtReqCode_Tries);
                etxtDialogRegCode = view.findViewById(R.id.etxtDialogRegCode);
                txtTimerDelay = view.findViewById(R.id.txtTimerDelay);


                final SharedPreferences sharedPref = RegisterActivity.this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                int waitTime = sharedPref.getInt("LocationFinderRegCodeWaitSharedPrefereces", 0);
                if (waitTime > 0) {
                    btnRequestRegCode.setAlpha((float) 0.5);
                    btnRequestRegCode.setEnabled(false);

                    String msg1 = "لقد استنفذت جميع المحاولات ";
                    String msg2 = "انتظر " + waitTime + " دقائق...";
                    txtReqCode_Tries.setText(msg1 + "\n" + msg2);
                } else {

                    txtReqCode_Tries.setText("عدد المحاولات المتبقية : " + timesOfCodeRequested);
                    if (waitNextCodeReqTimer != null) {

                        btnRequestRegCode.setEnabled(false);
                        btnRequestRegCode.setAlpha((float) 0.5);
                    }
                }


                if (response.equals("Phone_number_existed")) {      //user registered on another phone

                    txtDialogRegTitle.setText("هذا الرقم مسجل بحساب ثاني لتحديث الحساب على هذا الهاتف اطلب الرمز ليصلك برسالة نصية");

                    registrationCodeDialog = builder.create();
                    registrationCodeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    registrationCodeDialog.show();

                } else if (response.equals("New_user")) {

                    txtDialogRegTitle.setText("لاكمال التسجيل اطلب رمز التفعيل ليصلك برسالة نصية");

                    registrationCodeDialog = builder.create();
                    registrationCodeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    registrationCodeDialog.show();

                } else {
                    Toast.makeText(RegisterActivity.this, "حصل خطأ , حاول مرة ثانية رجاءا", Toast.LENGTH_SHORT).show();

                    Log.d(TAG, "1");
                }


                //listener on the registration code alert dialog buttons
                //cancel the dialog
                btnCancelRegCode.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        registrationCodeDialog.dismiss();
                    }
                });

                //make registration
                btnSubmitRegCode.setOnClickListener(new View.OnClickListener() {
                    @SuppressLint("MissingPermission")  //for getting imei of the phone
                    @Override
                    public void onClick(View v) {

                        String enteredCode = etxtDialogRegCode.getText().toString();
                        if (enteredCode.equals("") || Integer.parseInt(enteredCode) < 55555 || Integer.parseInt(enteredCode) > 100000) {
                            Toast.makeText(RegisterActivity.this, "ادخل رمز صحيح رجاءا", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(RegisterActivity.TELEPHONY_SERVICE);
                        String imei;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            imei = telephonyManager.getImei();
                        } else {
                            imei = telephonyManager.getDeviceId();
                        }

                        phoneNumber = etxtPhoneNumber.getText().toString();
                        userName = etxtUserName.getText().toString().trim();
                        tokenKey = sharedPref.getString("LocationFinderTokenKeySharedPrefereces", "notRegistered");

                        JSONObject data = new JSONObject();
                        try {
                            btnSubmitRegCode.setEnabled(false);
                            btnSubmitRegCode.setAlpha((float) 0.5);
                            Date today = new Date();
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
                            String dateToStr = format.format(today);


                            data.put("phoneNumber", phoneNumber);
                            data.put("simId", simId);
                            data.put("userName", userName);
                            data.put("currentTime", dateToStr);
                            data.put("currentLocation_Lng", lng);
                            data.put("currentLocation_Lat", lat);
                            data.put("tokenKey", tokenKey);
                            data.put("imei", imei);
                            data.put("regCode", etxtDialogRegCode.getText().toString());

                            new completeUserRegistrationByPost().execute(data);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                //request registration code
                btnRequestRegCode.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        //generate random code
                        final int regCode = new Random().nextInt(44445) + 55555;   // [0 - 44444] + 55555 = [55555 - 99999]

                        JSONObject data = new JSONObject();
                        try {
                            phoneNumber = etxtPhoneNumber.getText().toString();
                            userName = etxtUserName.getText().toString().trim();
                            btnRequestRegCode.setEnabled(false);
                            btnRequestRegCode.setAlpha((float) 0.5);

                            Date today = new Date();
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
                            String dateToStr = format.format(today);

                            data.put("phoneNumber", phoneNumber);
                            data.put("regDate", dateToStr);
                            data.put("regCode", regCode);

                            new requestRegCodeByPost().execute(data);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();

                Log.d(TAG, "in catch thrown");
                Toast.makeText(RegisterActivity.this, "حصل خطأ , حاول مرة ثانية رجاءا", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private class requestRegCodeByPost extends AsyncTask<JSONObject, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(JSONObject... jsonObjects) {
            RequestBody requestBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .post(requestBody)
                    .url(serverIp_ForRegister + "/requestRegCode_users")
                    .build();

            JSONObject jsonObject = null;
            try {
                Response response = client.newCall(request).execute();
                String s = response.body().string();
                jsonObject = new JSONObject(s);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }


        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);

            try {
                String response = jsonObject.get("msg").toString();
                if (response.equals("errorInRequestingCode")) {
                    btnRequestRegCode.setEnabled(true);
                    btnRequestRegCode.setAlpha(1);
                    Toast.makeText(RegisterActivity.this, "فشل في طلب الرمز , حاول مرة ثانية رجاءا", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RegisterActivity.this, "تم ارسال طلب رمز التفعيل", Toast.LENGTH_SHORT).show();
                    timesOfCodeRequested--;

                    if (timesOfCodeRequested > 0) {
                        txtReqCode_Tries.setText("عدد المحاولات المتبقية : " + timesOfCodeRequested);
                        waitNextCodeReqTimer = new CountDownTimer(60000, 1000) {
                            public void onTick(long millisUntilFinished) {
                                txtTimerDelay.setText(String.valueOf(millisUntilFinished / 1000));
                                //here you can have your logic to set text to edit text
                            }

                            public void onFinish() {
                                btnRequestRegCode.setEnabled(true);
                                btnRequestRegCode.setAlpha(1);
                                txtTimerDelay.setText("");
                                waitNextCodeReqTimer = null;
                            }
                        }.start();
                    } else {
                        String msg1 = "لقد استنفذت جميع المحاولات";
                        String msg2 = "انتظر 10 دقائق...";
                        txtReqCode_Tries.setText(msg1 + "\n" + msg2);

                        final SharedPreferences sharedPref = RegisterActivity.this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putInt("LocationFinderRegCodeWaitSharedPrefereces", 10);
                        editor.commit();


                        AlarmManager am = (AlarmManager) RegisterActivity.this.getSystemService(Context.ALARM_SERVICE);
                        Intent SosIntent = new Intent(RegisterActivity.this, RegCodeAlarmReceiver.class);
                        PendingIntent pi = PendingIntent.getBroadcast(RegisterActivity.this, RegCodeDelayAlarmManagerId, SosIntent, 0);
                        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60, pi); // Millisec * Second
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                btnRequestRegCode.setEnabled(true);
                btnRequestRegCode.setAlpha(1);
                Toast.makeText(RegisterActivity.this, "فشل في طلب الرمز , حاول مرة ثانية رجاءا", Toast.LENGTH_SHORT).show();
            }
        }
    }


    //async task class for completing registering new user(check reg code are identical)
    private class completeUserRegistrationByPost extends AsyncTask<JSONObject, Void, String> {

        @Override
        protected String doInBackground(JSONObject... jsonObjects) {
            RequestBody rBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/completeRegistrationProcess")
                    .post(rBody)
                    .build();

            Response response = null;

            String s = null;
            try {
                response = client.newCall(request).execute();
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);

                s = response.body().string();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return s;
        }


        @Override
        protected void onPostExecute(String str) {
            super.onPostExecute(str);

            Log.d("oldSqlite", str);
            btnSubmitRegCode.setEnabled(true);
            btnSubmitRegCode.setAlpha(1);
            String responseMsg;
            try {
                JSONObject s = new JSONObject(str);
                responseMsg = s.get("msg").toString();
                if (responseMsg.equals("registeredSuccessfully")) {

                    SharedPreferences sharedPref = RegisterActivity.this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("LocationFinderPhoneSharedPrefereces", phoneNumber);
                    editor.putInt("LocationFinderSimIdSharedPrefereces", simId);
                    editor.putString("LocationFinderUserNameSharedPrefereces", userName);
                    editor.putInt("LocationFinderWaitToSendSmsSharedPrefereces", 60);

                    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                    cal.add(Calendar.DAY_OF_MONTH, 60);
                    Log.d("expDate", "activationProcess : new exp date " + cal.getTime());
                    String newExpDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.getTime());

                    //int newExpDay = Integer.parseInt(android.text.format.DateFormat.format("dd", cal.getTime()).toString());
                    //int newExpMonth = Integer.parseInt(android.text.format.DateFormat.format("MM", cal.getTime()).toString());
                    //int newExpYear = Integer.parseInt(android.text.format.DateFormat.format("yyyy", cal.getTime()).toString());
                    //String newExpDate = newExpYear + "-" + newExpMonth + "-" + newExpDay;

                    Log.d("expDate", "activationProcess : new exp date " + newExpDate);
                    editor.putString("LocationFinderExpDateSharedPrefereces", newExpDate);

                    editor.commit();

                    Toast.makeText(RegisterActivity.this, "تم التسجيل بنجاح", Toast.LENGTH_SHORT).show();

                    //cancel alarm manager of the registration code delay
                    Intent intent2 = new Intent(RegisterActivity.this, RegCodeAlarmReceiver.class);
                    PendingIntent sender = PendingIntent.getBroadcast(RegisterActivity.this, RegCodeDelayAlarmManagerId, intent2, 0);
                    AlarmManager alarmManager = (AlarmManager) RegisterActivity.this.getSystemService(Context.ALARM_SERVICE);
                    alarmManager.cancel(sender);

                    //create the sqlite database
                    new MySqliteDB(RegisterActivity.this);

                    Log.d("sqlite_db", "CreationCompleted");

                    finish();
                    Intent intent = new Intent(getBaseContext(), MapsActivity.class);
                    startActivity(intent);
                } else if (responseMsg.startsWith("ExpirationDate=")) {

                    SharedPreferences sharedPref = RegisterActivity.this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("LocationFinderPhoneSharedPrefereces", phoneNumber);
                    editor.putInt("LocationFinderSimIdSharedPrefereces", simId);
                    editor.putString("LocationFinderUserNameSharedPrefereces", userName);
                    editor.putInt("LocationFinderWaitToSendSmsSharedPrefereces", 60);

                    try {

                        Date dd = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(responseMsg.substring(15));
                        String expDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(dd);
                        editor.putString("LocationFinderExpDateSharedPrefereces", expDateStr);
                    } catch (ParseException e1) {
                        e1.printStackTrace();

                        Log.d("ExpirationDate", "error converting");
                    }

                    editor.commit();

                    Toast.makeText(RegisterActivity.this, "تم التسجيل بنجاح", Toast.LENGTH_SHORT).show();

                    //cancel alarm manager of the registration code delay
                    Intent intent2 = new Intent(RegisterActivity.this, RegCodeAlarmReceiver.class);
                    PendingIntent sender = PendingIntent.getBroadcast(RegisterActivity.this, RegCodeDelayAlarmManagerId, intent2, 0);
                    AlarmManager alarmManager = (AlarmManager) RegisterActivity.this.getSystemService(Context.ALARM_SERVICE);
                    alarmManager.cancel(sender);

                    //create the sqlite database
                    new MySqliteDB(RegisterActivity.this);

                    Log.d("sqlite_db", "CreationCompleted");

                    finish();
                    Intent intent = new Intent(getBaseContext(), MapsActivity.class);
                    startActivity(intent);

                } else if (responseMsg.equals("wrongRegCode")) {

                    Toast.makeText(RegisterActivity.this, "الرمز المستخدم غير صحيح , يرجى التأكد من صحة الرمز", Toast.LENGTH_SHORT).show();
                } else {

                    Toast.makeText(RegisterActivity.this, "فشل في اكمال عملية التسجيل , حاول مرة ثانية رجاءا", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                e.printStackTrace();


                Log.d("oldSqlite", "1");
                ArrayList<MyOldConnectionsClassModel> oldConnectionsList = new ArrayList<>();
                try {
                    String expDate = null;
                    JSONArray jsonArray = new JSONArray(str);
                    JSONObject explrObject;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        explrObject = jsonArray.getJSONObject(i);
                        oldConnectionsList.add(new MyOldConnectionsClassModel
                                (
                                        explrObject.getInt("Id"),
                                        explrObject.getString("AccessFromPhone"),
                                        explrObject.getString("AccessFromUserName"),
                                        explrObject.getString("AccessToPhone"),
                                        explrObject.getString("AccessToUserName"),
                                        explrObject.getString("ConnectTime_From"),
                                        explrObject.getString("ConnectTime_To"),
                                        explrObject.getString("ConnectionStatus")
                                ));

                        expDate = explrObject.getString("ExpirationDate");
                    }

                    try {

                        Date dd = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(expDate);
                        expDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(dd);
                    } catch (ParseException e1) {
                        e1.printStackTrace();

                        Log.d("ExpirationDate", "error converting");
                    }

                    SharedPreferences sharedPref = RegisterActivity.this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("LocationFinderPhoneSharedPrefereces", phoneNumber);
                    editor.putInt("LocationFinderSimIdSharedPrefereces", simId);
                    editor.putString("LocationFinderUserNameSharedPrefereces", userName);
                    editor.putInt("LocationFinderWaitToSendSmsSharedPrefereces", 60);
                    editor.putString("LocationFinderExpDateSharedPrefereces", expDate);
                    editor.commit();

                    //create the sqlite database
                    MySqliteDB mySqliteDB = new MySqliteDB(RegisterActivity.this);
                    mySqliteDB.ReinsertOldConnections(oldConnectionsList);


                    Log.d("sqlite_db", "CreationCompleted");

                    Toast.makeText(RegisterActivity.this, "تم التسجيل بنجاح واسترجاع الارتباطات السابقة", Toast.LENGTH_SHORT).show();

                    finish();
                    Intent intent = new Intent(getBaseContext(), MapsActivity.class);
                    startActivity(intent);

                } catch (JSONException e1) {
                    e1.printStackTrace();
                    Log.d("oldSqlite", e1.getMessage());
                    Toast.makeText(RegisterActivity.this, "فشل في اكمال عملية التسجيل , حاول مرة ثانية رجاءا", Toast.LENGTH_SHORT).show();
                }
            }
        }
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
                                ActivityCompat.requestPermissions(RegisterActivity.this
                                        , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}
                                        , MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
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
                                ActivityCompat.requestPermissions(RegisterActivity.this
                                        , new String[]{Manifest.permission.READ_CONTACTS}
                                        , MY_PERMISSION_REQUEST_READ_CONTACTS);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        MY_PERMISSION_REQUEST_READ_CONTACTS);
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
                                ActivityCompat.requestPermissions(RegisterActivity.this
                                        , new String[]{Manifest.permission.READ_SMS}
                                        , MY_PERMISSION_REQUEST_Read_SMS);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_SMS},
                        MY_PERMISSION_REQUEST_Read_SMS);
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
                                ActivityCompat.requestPermissions(RegisterActivity.this
                                        , new String[]{Manifest.permission.SEND_SMS}
                                        , MY_PERMISSION_REQUEST_SEND_SMS);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSION_REQUEST_SEND_SMS);
            }
        }
    }


    //check if the application is granted access to Read Phone State    ......deprecated
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
                                ActivityCompat.requestPermissions(RegisterActivity.this
                                        , new String[]{Manifest.permission.READ_PHONE_STATE}
                                        , MY_PERMISSION_REQUEST_READ_PHONE_STATE);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        MY_PERMISSION_REQUEST_READ_PHONE_STATE);
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
                                ActivityCompat.requestPermissions(RegisterActivity.this
                                        , new String[]{Manifest.permission.RECEIVE_SMS}
                                        , MY_PERMISSION_REQUEST_RECEIVE_SMS);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECEIVE_SMS},
                        MY_PERMISSION_REQUEST_RECEIVE_SMS);
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
                            //ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)||
                            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WAKE_LOCK) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.DISABLE_KEYGUARD)
            ) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(RegisterActivity.this
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
                        , MY_PERMISSION_For_9_REQUESTS);


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
                                Manifest.permission.READ_PHONE_STATE
                        }
                        , MY_PERMISSION_For_9_REQUESTS);
            }
        }
    }


    //if user didn't grant the application access to the GPS then close the application
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION:   //checking location access permission
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
            case MY_PERMISSION_REQUEST_READ_CONTACTS:   //checking contacts access permission
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
            case MY_PERMISSION_REQUEST_Read_SMS:
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
            case MY_PERMISSION_REQUEST_SEND_SMS:
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
            case MY_PERMISSION_REQUEST_RECEIVE_SMS:
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
/*            case MY_PERMISSION_REQUEST_READ_PHONE_STATE:
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
            case MY_PERMISSION_For_9_REQUESTS:
                for (int i = 0; i < permissions.length; i++) {
                    Log.d("myPermission", permissions[i] + " ," + grantResults[i]);
                    if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {

                    } else {

                        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this)
                                .setTitle("تنويه")
                                .setMessage("نعتذر , لا يمكنك استخدام البرنامج اذا لم توافق على طلبات الوصول النظام");

                        final AlertDialog alertDialog = builder.create();
                        alertDialog.show();

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                alertDialog.dismiss();
                                finish();
                                System.exit(0);
                            }
                        }, 4000);
                    }
                }
                break;
        }
    }
}


class MyOldConnectionsClassModel {

    private int Id;
    private String AccessFromPhone, AccessFromUserName, AccessToPhone, AccessToUserName, ConnectTime_From, ConnectTime_To, ConnectionStatus;

    public MyOldConnectionsClassModel(int id, String accessFromPhone, String accessFromUserName, String accessToPhone, String accessToUserName, String connectTime_From, String connectTime_To, String connectionStatus) {
        Id = id;
        AccessFromPhone = accessFromPhone;
        AccessFromUserName = accessFromUserName;
        AccessToPhone = accessToPhone;
        AccessToUserName = accessToUserName;
        ConnectTime_From = connectTime_From;
        ConnectTime_To = connectTime_To;
        ConnectionStatus = connectionStatus;
    }

    public int getId() {
        return Id;
    }

    public String getAccessFromPhone() {
        return AccessFromPhone;
    }

    public String getAccessFromUserName() {
        return AccessFromUserName;
    }

    public String getAccessToPhone() {
        return AccessToPhone;
    }

    public String getAccessToUserName() {
        return AccessToUserName;
    }

    public String getConnectTime_From() {
        return ConnectTime_From;
    }

    public String getConnectTime_To() {
        return ConnectTime_To;
    }

    public String getConnectionStatus() {
        return ConnectionStatus;
    }
}