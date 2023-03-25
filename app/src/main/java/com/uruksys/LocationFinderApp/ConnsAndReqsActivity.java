package com.uruksys.LocationFinderApp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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

public class ConnsAndReqsActivity extends AppCompatActivity {

    private static final String TAG = ConnsAndReqsActivity.class.getSimpleName();

    private String myPhoneNumber;
    private String serverIp = "http://localhost:3000/LocationFinderApp";
    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();

    private ListView listConnsNReqs;
    ArrayList<UserInfoModel_ConnsNReqs> connsNReqsList = new ArrayList<>();

    public static Boolean isConnected_ConnsAndReqsActivity = false;
    private   Timer pingTimer = null;
    private TimerTask pingTimerTask = null;

    //private BroadcastReceiver networkStatusMessageReceiver = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conns_reqs);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        listConnsNReqs = findViewById(R.id.listConnsNReqs);

        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_ConnsNReqs);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);


        //get the information of the user from the sharedPreferences
        Context context = this;
        SharedPreferences sharedPref = context.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        myPhoneNumber = sharedPref.getString("LocationFinderPhoneSharedPrefereces", "notRegistered");


        //ReceiveBroadcastNetConnectionStatus();

        //LocalBroadcastManager.getInstance(this).registerReceiver(
        //       networkStatusMessageReceiver, new IntentFilter("networkChangeReceiver"));

        //this.registerReceiver(new NetworkChangeReceiver(), new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));


        //timer to ping to google every 4 sec to check internet connection ( check if timer not null first)
        isConnected_ConnsAndReqsActivity = false;
        pingTimer = new Timer();
        pingTimerTask = new TimerTask() {
            @Override
            public void run() {
                boolean currentPing = internetIsConnected();
                if (isConnected_ConnsAndReqsActivity == null || isConnected_ConnsAndReqsActivity != currentPing) {
                    isConnected_ConnsAndReqsActivity = currentPing;
                    Log.d("isThereInternet", "ConnsAndReqsActivity / " + isConnected_ConnsAndReqsActivity);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (isConnected_ConnsAndReqsActivity) {

                                GetConnsNReqs();
                            } else {

                                //listConnsNReqs.setAdapter(null);
                                //Toast.makeText(ConnsAndReqsActivity.this, "غير مرتبط بالانترنت", Toast.LENGTH_SHORT).show();
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


    //onReceiving broadcast that the internet connection status changed
    /*private void ReceiveBroadcastNetConnectionStatus(){
        networkStatusMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                boolean isConnected = intent.getBooleanExtra("isThereConnection" , false);

                if(isConnected){

                    GetConnsNReqs();
                }else {

                    listConnsNReqs.setAdapter(null);

                    Toast.makeText(ConnsAndReqsActivity.this , "غير مرتبط بالانترنت", Toast.LENGTH_SHORT).show();
                }

                Log.d(TAG , isConnected+"");
            }
        };
    }*/


    private void GetConnsNReqs() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("PhoneNumber", myPhoneNumber);
            new GetConnsNReqsAsync().execute(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    class GetConnsNReqsAsync extends AsyncTask<JSONObject, Void, String> {

        @Override
        protected String doInBackground(JSONObject... jsonObjects) {
            RequestBody requestBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/getConnsNReqs")
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
            try {
                connsNReqsList.clear();
                JSONArray jsonArray = new JSONArray(response);
                JSONObject explrObject;
                for (int i = 0; i < jsonArray.length(); i++) {
                    explrObject = jsonArray.getJSONObject(i);

                    //remove the seconds from the time that is fetched from the DB
                    DateFormat df=new SimpleDateFormat("HH:mm");
                    String connectTime_From= df.format(new SimpleDateFormat("HH:mm").parse(explrObject.get("ConnectTime_From").toString()));
                    String connectTime_To= df.format(new SimpleDateFormat("HH:mm").parse(explrObject.get("ConnectTime_To").toString()));

                    connsNReqsList.add(new UserInfoModel_ConnsNReqs
                            (
                                    explrObject.get("AccessFromUserName").toString(),
                                    explrObject.get("AccessFromPhone").toString(),
                                    explrObject.get("TokenKey").toString(),
                                    explrObject.get("ConnectionStatus").toString(),
                                    connectTime_From,
                                    connectTime_To
                            ));
                }
                ConnsAndReqsListAdapter connsAndReqsListAdapter = new ConnsAndReqsListAdapter
                        (ConnsAndReqsActivity.this, R.layout.conns_reqs_listview, connsNReqsList);

                listConnsNReqs.setAdapter(connsAndReqsListAdapter);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String responseMsg = jsonObject.get("msg").toString();
                    Toast.makeText(ConnsAndReqsActivity.this, responseMsg, Toast.LENGTH_SHORT).show();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            super.onPostExecute(response);
        }
    }
}


class UserInfoModel_ConnsNReqs {
    private String userName, phoneNumber, tokenKey, ConnectionStatus , connectTime_From , connectTime_To;

    public UserInfoModel_ConnsNReqs(String userName, String phoneNumber, String tokenKey, String ConnectionStatus, String connectTime_From , String connectTime_To) {
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

    public void setConnectionStatus(String connectionStatus) {
        ConnectionStatus = connectionStatus;
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