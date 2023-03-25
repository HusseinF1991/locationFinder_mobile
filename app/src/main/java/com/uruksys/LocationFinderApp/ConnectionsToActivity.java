package com.uruksys.LocationFinderApp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConnectionsToActivity extends AppCompatActivity {

    private static final String TAG = ConnectionsToActivity.class.getSimpleName();

    private String myPhoneNumber;
    private String serverIp = "http://23.239.203.134:3000/LocationFinderApp";
    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();

    private AlertDialog addNewConnectionToDialog;
    private ArrayList<UserInfoModel_ConnectionsTo> connectionsToArrayList = new ArrayList();
    private ListView listConnectionsTo;
    private ArrayList<String> contactNumbers = new ArrayList<>();


    //for new connection dialog
    EditText etxtNewUserConnection, etxtNewPhoneConnection;
    int currentIndex = 0;

    public static Boolean isConnected_ConnectionToActivity = false;
    private Timer pingTimer = null;
    private TimerTask pingTimerTask = null;

    //private BroadcastReceiver networkStatusMessageReceiver = null;
    private BroadcastReceiver newConnectionMessageReceiver = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connections_to);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        listConnectionsTo = findViewById(R.id.listConnectionsTo);


        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_ConnectionsTo);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);


        //get the information of the user from the sharedPreferences
        Context context = this;
        SharedPreferences sharedPref = context.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        myPhoneNumber = sharedPref.getString("LocationFinderPhoneSharedPrefereces", "notRegistered");


        //ReceiveBroadcastNetConnectionStatus();
        ReceiveBroadcastNewConnection();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                newConnectionMessageReceiver, new IntentFilter("responseToConnectionReq"));

        //LocalBroadcastManager.getInstance(this).registerReceiver(
        //        networkStatusMessageReceiver, new IntentFilter("networkChangeReceiver"));

        //this.registerReceiver(new NetworkChangeReceiver(), new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));


        //timer to ping to google every 4 sec to check internet connection ( check if timer not null first)
        isConnected_ConnectionToActivity = false;
        pingTimer = new Timer();
        pingTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (isConnected_ConnectionToActivity != internetIsConnected()) {
                    isConnected_ConnectionToActivity = !isConnected_ConnectionToActivity;
                    Log.d("isThereInternet", "ConnectionToActivity / " + isConnected_ConnectionToActivity);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (isConnected_ConnectionToActivity) {

                                GetConnectionsTo();
                            } else {

                                if (addNewConnectionToDialog != null && addNewConnectionToDialog.isShowing()){
                                    addNewConnectionToDialog.dismiss();
                                    Toast.makeText(ConnectionsToActivity.this, "غير مرتبط بالانترنت", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
                }
            }
        };
        pingTimer.schedule(pingTimerTask, 0, 4000);
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


    //onReceiving broadcast of the connection response
    private void ReceiveBroadcastNewConnection() {
        newConnectionMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean newConnectionStatus = intent.getBooleanExtra("connectionsUpdated", true);

                if (newConnectionStatus) {
                    listConnectionsTo.setAdapter(null);
                    GetConnectionsTo();
                }
            }
        };
    }


    //onReceiving broadcast that the internet connection status changed
    /*private void ReceiveBroadcastNetConnectionStatus(){
        networkStatusMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                isConnected = intent.getBooleanExtra("isThereConnection" , false);

                if(isConnected){

                    GetConnectionsTo();
                }else {

                    listConnectionsTo.setAdapter(null);

                    if(addNewConnectionToDialog != null && addNewConnectionToDialog.isShowing())
                        addNewConnectionToDialog.dismiss();

                    Toast.makeText(ConnectionsToActivity.this , "غير مرتبط بالانترنت", Toast.LENGTH_SHORT).show();
                }

                Log.d(TAG , isConnected+"");
            }
        };
    }*/


    private void GetConnectionsTo() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("PhoneNumber", myPhoneNumber);
            new GetConnectionsToAsync().execute(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private class GetConnectionsToAsync extends AsyncTask<JSONObject, Void, String> {

        @Override
        protected String doInBackground(JSONObject... jsonObjects) {

            RequestBody requestBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/getUserConnectionsTo")
                    .post(requestBody)
                    .build();

            Response response = null;
            String s = null;
            try {
                response = client.newCall(request).execute();
                s = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return s;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);

            connectionsToArrayList.clear();
            try {
                JSONArray jsonArray = new JSONArray(response);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    //remove the seconds from the time that is fetched from the DB

                    DateFormat df=new SimpleDateFormat("HH:mm");
                    String connectTime_From= df.format(new SimpleDateFormat("HH:mm").parse(jsonObject.get("connectTime_From").toString()));
                    String connectTime_To= df.format(new SimpleDateFormat("HH:mm").parse(jsonObject.get("connectTime_To").toString()));

                    connectionsToArrayList.add(new UserInfoModel_ConnectionsTo
                            (
                                    jsonObject.get("accessToUserName").toString(),
                                    jsonObject.get("accessToPhone").toString(),
                                    jsonObject.get("tokenKey").toString(),
                                    jsonObject.get("connectionStatus").toString(),
                                    connectTime_From,
                                    connectTime_To
                            ));
                }

                ConnectionsToListAdapter connectionsToListAdapter = new ConnectionsToListAdapter(ConnectionsToActivity.this,
                        R.layout.connections_to_listview, connectionsToArrayList);
                listConnectionsTo.setAdapter(connectionsToListAdapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.optionmenu_newconnectionto, menu);
        return super.onCreateOptionsMenu(menu);
    }


    //on activityResult for fetched contact
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case (7):
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
                                etxtNewUserConnection.setText(TempNameHolder);
                                etxtNewPhoneConnection.setText(TempNumberHolder);

                                contactNumbers.add(TempNumberHolder);
                                currentIndex = contactNumbers.indexOf(TempNumberHolder);
                            }
                        }

                    }
                }
                break;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("checkOptionMenu" , "itemClicked");
        if (isConnected_ConnectionToActivity) {

            if (item.getItemId() == R.id.itemNewConnection_ConnectionsToActivity) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                LayoutInflater layoutInflater = getLayoutInflater();
                View dialogBuilderView = layoutInflater.inflate(R.layout.requestnew_connectiontodialog, null);
                builder.setView(dialogBuilderView);

                etxtNewUserConnection = dialogBuilderView.findViewById(R.id.etxtNewUserConnection);
                etxtNewPhoneConnection = dialogBuilderView.findViewById(R.id.etxtNewPhoneConnection);
                Button btnReqNewConnection = dialogBuilderView.findViewById(R.id.btnReqNewConnection);
                Button btnCancelNewConnectionDialog = dialogBuilderView.findViewById(R.id.btnCancelNewConnectionDialog);
                Button btnSearchContacts = dialogBuilderView.findViewById(R.id.btnSearchContacts);
                Button btnNextNumberForContact = dialogBuilderView.findViewById(R.id.btnNextNumberForContact);

                addNewConnectionToDialog = builder.create();
                addNewConnectionToDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                addNewConnectionToDialog.show();

                //cancel connection request dialog
                btnCancelNewConnectionDialog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addNewConnectionToDialog.dismiss();
                    }
                });

                //listener on requesting new connection by btn click
                btnReqNewConnection.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (etxtNewPhoneConnection.getText().toString().equals("") || etxtNewUserConnection.getText().toString().trim().equals("")) {
                            Toast.makeText(getBaseContext(), "أدخل المعلومات كاملة رجاءا", Toast.LENGTH_SHORT).show();
                        } else {
                            SendConnectionRequest(etxtNewUserConnection.getText().toString().trim(), etxtNewPhoneConnection.getText().toString());
                            addNewConnectionToDialog.dismiss();
                        }
                    }
                });

                //make listener on number changed in EditText to grap the contact name
                etxtNewPhoneConnection.addTextChangedListener(new TextWatcher() {
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
                            etxtNewUserConnection.setText(contactName);
                        } else {
                            etxtNewUserConnection.setText("");
                        }
                    }
                });


                //btn to view contacts and grab the number and name
                btnSearchContacts.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        contactNumbers.clear();
                        etxtNewPhoneConnection.setText("");
                        etxtNewUserConnection.setText("");

                        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                        startActivityForResult(intent, 7);
                    }
                });


                //view nextnum number for the grabed contact if exists
                btnNextNumberForContact.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (contactNumbers.size() > 1) {
                            if (contactNumbers.size() == currentIndex + 1) {
                                etxtNewPhoneConnection.setText(contactNumbers.get(0));
                                currentIndex = contactNumbers.indexOf(etxtNewPhoneConnection.getText().toString());
                            } else {
                                currentIndex++;
                                etxtNewPhoneConnection.setText(contactNumbers.get(currentIndex));
                            }
                        }
                    }
                });

            }
        }
        else{

            Toast.makeText(ConnectionsToActivity.this , "غير مرتبط بالانترنت", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
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


    private void SendConnectionRequest(String reqToUserName, String reqToPhone) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("receiverRequestUserName", reqToUserName);
            jsonObject.put("receiverRequestPhone", reqToPhone);
            jsonObject.put("senderRequestPhone", myPhoneNumber);

            new SendConnectionReqAsyn().execute(jsonObject);

            //make the query on sqlite db
            MySqliteDB mySqliteDB = new MySqliteDB(this);
            Cursor cursor = mySqliteDB.GetAllConnections(myPhoneNumber, reqToPhone);

            boolean result;
            if (cursor.getCount() > 0) {

                result = mySqliteDB.UpdateOnConnectionsTbl(myPhoneNumber, reqToPhone, "Pending" , "00:00" , "23:59");
                Log.d("updateSqlite", "completed");

            } else {
                result = mySqliteDB.InsertToConnectionsTbl(myPhoneNumber, null, reqToPhone, reqToUserName, "Pending" , "00:00" , "23:59");
                Log.d("insertSqlite", "completed");
            }

            if (!result) {
                Toast.makeText(this, "error query of sqlite insert/update connection", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private class SendConnectionReqAsyn extends AsyncTask<JSONObject, Void, String> {

        @Override
        protected String doInBackground(JSONObject... jsonObjects) {

            RequestBody requestBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/handleConnectionToReq")
                    .post(requestBody)
                    .build();

            Response response = null;
            String s = null;
            try {
                response = client.newCall(request).execute();
                s = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return s;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);


            try {
                Log.d("TAG1", response);
                JSONObject jsonObject = new JSONObject(response);
                String responseMsg = jsonObject.get("msg").toString();
                if (responseMsg.equals("تم ارسال طلب الارتباط")) {
                    GetConnectionsTo();
                }
                Toast.makeText(getBaseContext(), responseMsg, Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
                Log.d("TAG2", response);
            }
        }
    }
}


class UserInfoModel_ConnectionsTo {
    private String userName, phoneNumber, tokenKey, ConnectionStatus , connectTime_From , connectTime_To;

    public UserInfoModel_ConnectionsTo(String userName, String phoneNumber, String tokenKey, String ConnectionStatus , String connectTime_From , String connectTime_To) {
        this.phoneNumber = phoneNumber;
        this.tokenKey = tokenKey;
        this.userName = userName;
        this.ConnectionStatus = ConnectionStatus;
        this.connectTime_From = connectTime_From;
        this.connectTime_To = connectTime_To;
    }

    public String getConnectionStatus() {
        return ConnectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) { ConnectionStatus = connectionStatus;  }

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

    public String getConnectTime_From() { return connectTime_From; }

    public void setConnectTime_From(String connectTime_From) {  this.connectTime_From = connectTime_From;  }

    public String getConnectTime_To() {  return connectTime_To;  }

    public void setConnectTime_To(String connectTime_To) {  this.connectTime_To = connectTime_To; }
}