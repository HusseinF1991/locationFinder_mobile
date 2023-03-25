package com.uruksys.LocationFinderApp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PersonalInfoActivity extends AppCompatActivity {

    private static final String TAG = PersonalInfoActivity.class.getSimpleName();
    private String serverIp = "http://23.239.203.134:3000/LocationFinderApp";
    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();

    private String myPhoneNumber;
    private int simId;
    private int SmsRequest_WaitTime;
    private String userName;

    private EditText etxtSmsDelayTime;
    private TextView txtMyPhoneNumber;
    private EditText etxtMyUserName;
    private Spinner spinSimId;
    private Button btnSaveChanges;

    private EditText etxtSosInterval;

    private Button btnSearchContact1, btnSearchContact2, btnSearchContact3;
    private Button btnNextContact1, btnNextContact2, btnNextContact3;
    private EditText etxtSosNumber1, etxtSosNumber2, etxtSosNumber3;

    private ArrayList<String> contactNumbers1 = new ArrayList<>();
    int currentIndex1 = 0;
    private ArrayList<String> contactNumbers2 = new ArrayList<>();
    int currentIndex2 = 0;
    private ArrayList<String> contactNumbers3 = new ArrayList<>();
    int currentIndex3 = 0;

    //private BroadcastReceiver networkStatusMessageReceiver = null;

    private Boolean isConnected = false;
    private Timer pingTimer = null;
    private TimerTask pingTimerTask = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //disable starting soft keyboard when activity starts
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_PersonalInfo);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);

        txtMyPhoneNumber = findViewById(R.id.txtMyPhoneNumber);
        etxtMyUserName = findViewById(R.id.etxtMyUserName);
        etxtSmsDelayTime = findViewById(R.id.etxtSmsDelayTime);
        spinSimId = findViewById(R.id.spinSimId);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);

        btnSearchContact1 = findViewById(R.id.btnSearchContact1);
        btnSearchContact2 = findViewById(R.id.btnSearchContact2);
        btnSearchContact3 = findViewById(R.id.btnSearchContact3);
        etxtSosNumber1 = findViewById(R.id.etxtSosNumber1);
        etxtSosNumber2 = findViewById(R.id.etxtSosNumber2);
        etxtSosNumber3 = findViewById(R.id.etxtSosNumber3);
        btnNextContact1 = findViewById(R.id.btnNextContact1);
        btnNextContact2 = findViewById(R.id.btnNextContact2);
        btnNextContact3 = findViewById(R.id.btnNextContact3);
        etxtSosInterval = findViewById(R.id.etxtSosInterval);

        //get the information of the user from the sharedPreferences
        SharedPreferences sharedPref = this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        myPhoneNumber = sharedPref.getString("LocationFinderPhoneSharedPrefereces", "notRegistered");
        simId = sharedPref.getInt("LocationFinderSimIdSharedPrefereces", 0);
        SmsRequest_WaitTime = sharedPref.getInt("LocationFinderWaitToSendSmsSharedPrefereces", 60);
        userName = sharedPref.getString("LocationFinderUserNameSharedPrefereces", "notRegistered");

        etxtSosNumber1.setText(sharedPref.getString("LocationFinderSosNum1SharedPrefereces", ""));
        etxtSosNumber2.setText(sharedPref.getString("LocationFinderSosNum2SharedPrefereces", ""));
        etxtSosNumber3.setText(sharedPref.getString("LocationFinderSosNum3SharedPrefereces", ""));
        etxtSosInterval.setText(String.valueOf(sharedPref.getInt("LocationFinderSosIntervalsSharedPrefereces", 10)));

        etxtMyUserName.setText(userName);
        etxtSmsDelayTime.setText(String.valueOf(SmsRequest_WaitTime));
        txtMyPhoneNumber.setText(myPhoneNumber);


        GetSimCards();

        OnUpdatingPersonalInfo();

        SearchSosContact1();
        SearchSosContact2();
        SearchSosContact3();
        NextContactNumber1();
        NextContactNumber2();
        NextContactNumber3();

        //ReceiveBroadcastNetConnectionStatus();

        //LocalBroadcastManager.getInstance(this).registerReceiver(
        //        networkStatusMessageReceiver, new IntentFilter("networkChangeReceiver"));

        //this.registerReceiver(new NetworkChangeReceiver(), new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));


        //timer to ping to google every 4 sec to check internet connection ( check if timer not null first)
        pingTimer = new Timer();
        pingTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (isConnected != internetIsConnected()) {
                    isConnected = !isConnected;
                    Log.d("isThereInternet", "PersonalInfoActivity / " + isConnected);
                }
            }
        };
        pingTimer.schedule(pingTimerTask , 0, 4000);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        pingTimer.cancel();
        pingTimer.purge();
        pingTimerTask.cancel();
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


    //onReceiving broadcast that the internet connection status changed
    /*private void ReceiveBroadcastNetConnectionStatus() {
        networkStatusMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                boolean isConnected = intent.getBooleanExtra("isThereConnection", false);

                if (isConnected) {

                    btnSaveChanges.setEnabled(true);
                } else {

                    btnSaveChanges.setEnabled(false);

                    Toast.makeText(PersonalInfoActivity.this, "غير مرتبط بالانترنت", Toast.LENGTH_SHORT).show();
                }

                Log.d(TAG, isConnected + "");
            }
        };
    }*/


    //get phone sims then populate them in spinner
    @SuppressLint("MissingPermission")
    private void GetSimCards() {

        //get sim cards info
        String selectedSimInSpinner = null;
        final List<SimInfo> simInfoList = new ArrayList<>();
        Uri URI_TELEPHONY = Uri.parse("content://telephony/siminfo/");
        Cursor c = this.getContentResolver().query(URI_TELEPHONY, null, "sim_id != -1", null, null);

        SubscriptionManager subscriptionManager = SubscriptionManager.from(getApplicationContext());
        List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
            int subscriptionId = subscriptionInfo.getSubscriptionId();
            Log.d("apipas", "subscriptionId:" + subscriptionId);
        }


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
            simList.add(simInfo.getDisplay_name() + " " + (simInfo.getSim_id()));

            if (simId == simInfo.getSim_id()) {
                selectedSimInSpinner = simInfo.getDisplay_name() + " " + (simInfo.getSim_id());
            }
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
                    TV.setTextColor(Color.RED);
                }
                return view;
            }
        };

        AgeSpinnerAdapter.setDropDownViewResource(R.layout.spinner_layout);
        spinSimId.setAdapter(AgeSpinnerAdapter);

        //set selection for the spinner
        spinSimId.setSelection(AgeSpinnerAdapter.getPosition(selectedSimInSpinner));
    }


    //on clicking btn to save changes of the personal info
    private void OnUpdatingPersonalInfo() {
        btnSaveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isConnected){
                    Toast.makeText(PersonalInfoActivity.this, "غير مرتبط بالانترنت", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (etxtMyUserName.getText().toString().trim().equals("")) {
                    Toast.makeText(PersonalInfoActivity.this, "لا يمكن ادخال اسم مستخدم فارغ", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(etxtSosInterval.getText().toString().equals("") || Integer.valueOf(etxtSosInterval.getText().toString()) < 3){

                    Toast.makeText(PersonalInfoActivity.this, "الفترة بين الندائين يجب ان لا تقل عن 4 دقائق", Toast.LENGTH_SHORT).show();
                    return;
                }

                simId = Integer.parseInt(spinSimId.getSelectedItem().toString().substring(spinSimId.getSelectedItem().toString().length() - 1));
                userName = etxtMyUserName.getText().toString().trim();
                SmsRequest_WaitTime = Integer.parseInt(etxtSmsDelayTime.getText().toString());
                JSONObject jsonObject = new JSONObject();

                try {
                    jsonObject.put("userName", userName);
                    jsonObject.put("simId", simId);
                    jsonObject.put("phoneNumber", myPhoneNumber);

                    new UpdatePersonalInfoAsync().execute(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }



    private class UpdatePersonalInfoAsync extends AsyncTask<JSONObject, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(JSONObject... jsonObjects) {
            RequestBody requestBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/updatePersonalInfo")
                    .post(requestBody)
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

                Log.d(TAG, jsonObject + "");
                String s = jsonObject.get("msg").toString();

                if (s.equals("Updated")) {

                    SharedPreferences sharedPref = PersonalInfoActivity.this.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putInt("LocationFinderSimIdSharedPrefereces", simId);
                    editor.putString("LocationFinderUserNameSharedPrefereces", userName);
                    editor.putInt("LocationFinderWaitToSendSmsSharedPrefereces", SmsRequest_WaitTime);

                    editor.putString("LocationFinderSosNum1SharedPrefereces", etxtSosNumber1.getText().toString().trim());
                    editor.putString("LocationFinderSosNum2SharedPrefereces", etxtSosNumber2.getText().toString().trim());
                    editor.putString("LocationFinderSosNum3SharedPrefereces", etxtSosNumber3.getText().toString().trim());
                    editor.putInt("LocationFinderSosIntervalsSharedPrefereces", Integer.valueOf(etxtSosInterval.getText().toString()));
                    editor.commit();


                    Toast.makeText(PersonalInfoActivity.this, "تم تعديل المعلومات الشخصية", Toast.LENGTH_SHORT).show();
                } else {

                    Toast.makeText(PersonalInfoActivity.this, "خطأ في تعديل المعلومات , حاول مرة ثانية رجاءا", Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    //btn to view contacts and grab the number for Sos 1
    private void SearchSosContact1() {
        btnSearchContact1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contactNumbers1.clear();
                etxtSosNumber1.setText("");

                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, RegisterActivity.SosNumberCode1);
            }
        });
    }


    //btn to view contacts and grab the number for Sos 2
    private void SearchSosContact2() {
        btnSearchContact2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contactNumbers2.clear();
                etxtSosNumber2.setText("");

                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, RegisterActivity.SosNumberCode2);
            }
        });
    }


    //btn to view contacts and grab the number for Sos 3
    private void SearchSosContact3() {
        btnSearchContact3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contactNumbers3.clear();
                etxtSosNumber3.setText("");

                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, RegisterActivity.SosNumberCode3);
            }
        });
    }


    //nextnum contact number 1 from fetched name
    private void NextContactNumber1() {
        btnNextContact1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contactNumbers1.size() > 1) {
                    if (contactNumbers1.size() == currentIndex1 + 1) {
                        etxtSosNumber1.setText(contactNumbers1.get(0));
                        currentIndex1 = contactNumbers1.indexOf(etxtSosNumber1.getText().toString());
                    } else {
                        currentIndex1++;
                        etxtSosNumber1.setText(contactNumbers1.get(currentIndex1));
                    }
                }
            }
        });
    }


    //nextnum contact number 2 from fetched name
    private void NextContactNumber2() {
        btnNextContact2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contactNumbers2.size() > 1) {
                    if (contactNumbers2.size() == currentIndex2 + 1) {
                        etxtSosNumber2.setText(contactNumbers2.get(0));
                        currentIndex2 = contactNumbers2.indexOf(etxtSosNumber2.getText().toString());
                    } else {
                        currentIndex2++;
                        etxtSosNumber2.setText(contactNumbers2.get(currentIndex2));
                    }
                }
            }
        });
    }


    //nextnum contact number 3 from fetched name
    private void NextContactNumber3() {
        btnNextContact3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (contactNumbers3.size() > 1) {
                    if (contactNumbers3.size() == currentIndex3 + 1) {
                        etxtSosNumber3.setText(contactNumbers3.get(0));
                        currentIndex3 = contactNumbers3.indexOf(etxtSosNumber3.getText().toString());
                    } else {
                        currentIndex3++;
                        etxtSosNumber3.setText(contactNumbers3.get(currentIndex3));
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
            case (RegisterActivity.SosNumberCode1):
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
                                etxtSosNumber1.setText(TempNumberHolder);

                                contactNumbers1.add(TempNumberHolder);
                                currentIndex1 = contactNumbers1.indexOf(TempNumberHolder);
                            }
                        }
                    }
                }
                break;

            case (RegisterActivity.SosNumberCode2):
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
                                etxtSosNumber2.setText(TempNumberHolder);

                                contactNumbers2.add(TempNumberHolder);
                                currentIndex2 = contactNumbers2.indexOf(TempNumberHolder);
                            }
                        }
                    }
                }
                break;

            case (RegisterActivity.SosNumberCode3):
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
                                etxtSosNumber3.setText(TempNumberHolder);

                                contactNumbers3.add(TempNumberHolder);
                                currentIndex3 = contactNumbers3.indexOf(TempNumberHolder);
                            }
                        }
                    }
                }
                break;
        }
    }
}
