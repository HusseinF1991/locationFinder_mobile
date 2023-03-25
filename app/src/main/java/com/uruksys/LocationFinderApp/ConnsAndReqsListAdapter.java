package com.uruksys.LocationFinderApp;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConnsAndReqsListAdapter extends ArrayAdapter implements View.OnClickListener {

    private String myPhoneNumber;
    private String serverIp = "http://23.239.203.134:3000/LocationFinderApp";
    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();
    int listViewLayout;
    Context myContext;
    ArrayList<UserInfoModel_ConnsNReqs> connsNReqsArrayList;

    AlertDialog changeConnectorUserNameDialog;

    public ConnsAndReqsListAdapter(@NonNull Context context, int resource, @NonNull ArrayList<UserInfoModel_ConnsNReqs> objects) {
        super(context, resource, objects);

        this.connsNReqsArrayList = objects;
        this.listViewLayout = resource;
        this.myContext = context;


        //get the information of the user from the sharedPreferences
        SharedPreferences sharedPref = context.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        myPhoneNumber = sharedPref.getString("LocationFinderPhoneSharedPrefereces", "notRegistered");
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final View ItemView;
        UserInfoModel_ConnsNReqs userInfoModel_connsNReqs = connsNReqsArrayList.get(position);
        ViewHolder Holder;

        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(myContext);
            convertView = layoutInflater.inflate(listViewLayout, parent, false);
            Holder = new ViewHolder();


            //initialize the views objects
            Holder.txtUserName_ConnsNReqs = convertView.findViewById(R.id.txtUserName_ConnsNReqs);
            Holder.txtPhone_ConnsNReqs = convertView.findViewById(R.id.txtPhone_ConnsNReqs);
            Holder.txtStatus_ConnsNReqs = convertView.findViewById(R.id.txtStatus_ConnsNReqs);
            Holder.btnAccept_ConnsNReqs = convertView.findViewById(R.id.btnAccept_ConnsNReqs);
            Holder.btnReject_ConnsNReqs = convertView.findViewById(R.id.btnReject_ConnsNReqs);
            Holder.txtToTime_ConnsNReqs = convertView.findViewById(R.id.txtToTime_ConnsNReqs);
            Holder.txtFromTime_ConnsNReqs = convertView.findViewById(R.id.txtFromTime_ConnsNReqs);

            convertView.setTag(Holder);
            ItemView = convertView;

        } else {
            Holder = (ViewHolder) convertView.getTag();
            ItemView = convertView;
        }

        Holder.txtUserName_ConnsNReqs.setText(userInfoModel_connsNReqs.getUserName());
        Holder.txtPhone_ConnsNReqs.setText(userInfoModel_connsNReqs.getPhoneNumber());
        Holder.txtStatus_ConnsNReqs.setText(userInfoModel_connsNReqs.getConnectionStatus());
        Holder.txtFromTime_ConnsNReqs.setText(userInfoModel_connsNReqs.getConnectTime_From());
        Holder.txtToTime_ConnsNReqs.setText(userInfoModel_connsNReqs.getConnectTime_To());
        if (userInfoModel_connsNReqs.getConnectionStatus().equals("Connected")) {
            Holder.btnAccept_ConnsNReqs.setVisibility(View.INVISIBLE);
            Holder.btnReject_ConnsNReqs.setText("الغاء الارتباط");
            Holder.txtStatus_ConnsNReqs.setText("مرتبط");
            Holder.txtStatus_ConnsNReqs.setTextColor(Color.parseColor("#13A5bb"));

        } else if (userInfoModel_connsNReqs.getConnectionStatus().equals("Pending")) {
            Holder.btnReject_ConnsNReqs.setText("رفض");
            Holder.txtStatus_ConnsNReqs.setText("بأنتظار الرد");
            Holder.txtStatus_ConnsNReqs.setTextColor(Color.RED);

            Holder.btnAccept_ConnsNReqs.setVisibility(View.VISIBLE);
            Holder.btnAccept_ConnsNReqs.setTag(R.string.holderTagKey, Holder);
            Holder.btnAccept_ConnsNReqs.setTag(R.string.itemPositionInList, position);
            Holder.btnAccept_ConnsNReqs.setOnClickListener(this);

        } else if (userInfoModel_connsNReqs.getConnectionStatus().equals("Aborted")) {

            Holder.btnAccept_ConnsNReqs.setVisibility(View.INVISIBLE);
            Holder.btnReject_ConnsNReqs.setVisibility(View.INVISIBLE);
            Holder.txtStatus_ConnsNReqs.setText("ملغي");
            Holder.txtStatus_ConnsNReqs.setTextColor(Color.RED);
        }
        Holder.btnReject_ConnsNReqs.setTag(R.string.holderTagKey, Holder);
        Holder.btnReject_ConnsNReqs.setTag(R.string.itemPositionInList, position);
        Holder.btnReject_ConnsNReqs.setOnClickListener(this);

        //set listener on userName textView to change it
        Holder.txtUserName_ConnsNReqs.setTag(R.string.holderTagKey, Holder);
        Holder.txtUserName_ConnsNReqs.setTag(R.string.itemPositionInList, position);
        Holder.txtUserName_ConnsNReqs.setOnClickListener(this);

        return ItemView;
    }




    class ViewHolder {
        TextView txtUserName_ConnsNReqs, txtPhone_ConnsNReqs, txtStatus_ConnsNReqs, txtToTime_ConnsNReqs, txtFromTime_ConnsNReqs;
        Button btnAccept_ConnsNReqs, btnReject_ConnsNReqs;
    }


    @Override
    public void onClick(View v) {
        final int position = (int) v.getTag(R.string.itemPositionInList);
        final UserInfoModel_ConnsNReqs userInfoModel_connsNReqs = connsNReqsArrayList.get(position);

        final ViewHolder holder = (ViewHolder) v.getTag(R.string.holderTagKey);
        if(ConnsAndReqsActivity.isConnected_ConnsAndReqsActivity){
            switch (v.getId()) {
                case R.id.btnReject_ConnsNReqs:

                    if (holder.txtStatus_ConnsNReqs.getText().toString().equals("بأنتظار الرد")) {
                        userInfoModel_connsNReqs.setConnectionStatus("Rejected");
                        holder.txtStatus_ConnsNReqs.setText("مرفوض");
                    } else {
                        userInfoModel_connsNReqs.setConnectionStatus("Aborted");
                        holder.txtStatus_ConnsNReqs.setText("ملغي");
                    }
                    holder.txtStatus_ConnsNReqs.setTextColor(Color.RED);
                    holder.btnReject_ConnsNReqs.setVisibility(View.INVISIBLE);
                    holder.btnAccept_ConnsNReqs.setVisibility(View.INVISIBLE);

                    CreatePostReq(holder, position);
                    break;
                case R.id.btnAccept_ConnsNReqs:


                    AlertDialog.Builder myBuilder = new AlertDialog.Builder(myContext);

                    LayoutInflater myLayoutInflater = (LayoutInflater) myContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View myView = myLayoutInflater.inflate(R.layout.connection_to_time_dialog , null);
                    myBuilder.setView(myView);

                    Button btnConfirmConnectionToInterval = myView.findViewById(R.id.btnConfirmConnectionToInterval);
                    Button btnCancelConnectionToInterval = myView.findViewById(R.id.btnCancelConnectionToInterval);
                    final Button btnSelectFromTimeDialog = myView.findViewById(R.id.btnSelectFromTimeDialog);
                    final Button btnSelectToTimeDialog = myView.findViewById(R.id.btnSelectToTimeDialog);

                    final AlertDialog setConnectionTo_TimeAllowedDialog = myBuilder.create();
                    setConnectionTo_TimeAllowedDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    setConnectionTo_TimeAllowedDialog.show();

                    btnConfirmConnectionToInterval.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            userInfoModel_connsNReqs.setConnectionStatus("Connected");
                            holder.txtStatus_ConnsNReqs.setText("مرتبط");
                            holder.txtStatus_ConnsNReqs.setTextColor(Color.parseColor("#13A5bb"));

                            holder.btnReject_ConnsNReqs.setVisibility(View.INVISIBLE);
                            holder.btnAccept_ConnsNReqs.setVisibility(View.INVISIBLE);
                            holder.txtFromTime_ConnsNReqs.setText(btnSelectFromTimeDialog.getText().toString());
                            holder.txtToTime_ConnsNReqs.setText(btnSelectToTimeDialog.getText().toString());

                            CreatePostReq(holder, position , btnSelectFromTimeDialog.getText().toString() , btnSelectToTimeDialog.getText().toString());
                            setConnectionTo_TimeAllowedDialog.dismiss();
                        }
                    });

                    btnCancelConnectionToInterval.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            setConnectionTo_TimeAllowedDialog.dismiss();
                        }
                    });

                    btnSelectFromTimeDialog.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            TimePickerDialog timePickerDialog = new TimePickerDialog(myContext, new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                    try {
                                        Date Date1 = new SimpleDateFormat("HH:mm").parse(hourOfDay + ":" + minute);
                                        Date Date2 = new SimpleDateFormat("HH:mm").parse(btnSelectToTimeDialog.getText().toString());

                                        //check if user entered fromTime after ToTime
                                        if(Date1.after(Date2)){
                                            btnSelectFromTimeDialog.setText(Date2.getHours() + ":" + Date2.getMinutes());
                                        }else {
                                            btnSelectFromTimeDialog.setText(hourOfDay + ":" + minute);
                                        }
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } , 0 , 0 , true);
                            timePickerDialog.show();
                        }
                    });

                    btnSelectToTimeDialog.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            TimePickerDialog timePickerDialog = new TimePickerDialog(myContext, new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                    try {
                                        Date Date1 = new SimpleDateFormat("HH:mm").parse(btnSelectFromTimeDialog.getText().toString());
                                        Date Date2 = new SimpleDateFormat("HH:mm").parse(hourOfDay + ":" + minute);

                                        //check if user entered fromTime after ToTime
                                        if(Date1.after(Date2)){
                                            btnSelectToTimeDialog.setText(Date1.getHours() + ":" + Date1.getMinutes());
                                        }else {
                                            btnSelectToTimeDialog.setText(hourOfDay + ":" + minute);
                                        }
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } , 0 , 0 , true);
                            timePickerDialog.show();
                        }
                    });

                    break;
                case R.id.txtUserName_ConnsNReqs:

                    final String currentUserName = holder.txtUserName_ConnsNReqs.getText().toString();
                    final String connectorPhoneNumber = holder.txtPhone_ConnsNReqs.getText().toString();

                    AlertDialog.Builder builder = new AlertDialog.Builder(myContext);

                    LayoutInflater layoutInflater = (LayoutInflater) myContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
                    View view = layoutInflater.inflate(R.layout.connector_username_dialog , null);
                    builder.setView(view);

                    final EditText etxtConnectorUserName = view.findViewById(R.id.etxtConnectorUserName);
                    Button btnChangeConnectorUserName = view.findViewById(R.id.btnChangeConnectorUserName);
                    Button btnCancelUserNameChange = view.findViewById(R.id.btnCancelUserNameChange);

                    etxtConnectorUserName.setText(currentUserName);

                    changeConnectorUserNameDialog = builder.create();
                    changeConnectorUserNameDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    changeConnectorUserNameDialog.show();

                    btnChangeConnectorUserName.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String newUserName = etxtConnectorUserName.getText().toString().trim();
                            if(newUserName.trim().equals("")){
                                Toast.makeText(myContext , "أدخل اسم صحيح رجاءا", Toast.LENGTH_SHORT).show();
                            }
                            else if(newUserName.trim().equals(currentUserName)){

                                changeConnectorUserNameDialog.dismiss();
                            }
                            else{
                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put("accessFromPhone", connectorPhoneNumber);
                                    jsonObject.put("accessToPhone", myPhoneNumber);
                                    jsonObject.put("accessFromUserName", newUserName);

                                    userInfoModel_connsNReqs.setUserName(newUserName);

                                    new SendRequesterUserNameByPost().execute(jsonObject);

                                    changeConnectorUserNameDialog.dismiss();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

                    btnCancelUserNameChange.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            changeConnectorUserNameDialog.dismiss();
                        }
                    });
                    break;
            }
        }
        else{
            Toast.makeText(myContext, "غير مرتبط بالانترنت", Toast.LENGTH_SHORT).show();
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

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //changeConnectorUserNameDialog.dismiss();
        }
    }


    //method for rejecting connection request or aborting it
    private void CreatePostReq(ViewHolder holder, int itemPosition) {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("AccessFromPhone", holder.txtPhone_ConnsNReqs.getText().toString());
            jsonObject.put("AccessToPhone", myPhoneNumber);
            jsonObject.put("ConnectionStatus", connsNReqsArrayList.get(itemPosition).getConnectionStatus());
            jsonObject.put("TokenKey", connsNReqsArrayList.get(itemPosition).getTokenKey());

            new Accept_Rej_AbortReqAsync().execute(jsonObject);


            //make the query on sqlite db
            MySqliteDB mySqliteDB = new MySqliteDB(myContext);

            boolean result = mySqliteDB.UpdateOnConnectionsTbl(holder.txtPhone_ConnsNReqs.getText().toString(), myPhoneNumber, connsNReqsArrayList.get(itemPosition).getConnectionStatus());
            Log.d("updateSqlite", "completed");

            if (!result) {
                Toast.makeText(myContext, "error query of sqlite update connection", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    //method for Accepting connection request
    private void CreatePostReq(ViewHolder holder, int itemPosition , String FromTime , String ToTime) {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("AccessFromPhone", holder.txtPhone_ConnsNReqs.getText().toString());
            jsonObject.put("AccessToPhone", myPhoneNumber);
            jsonObject.put("ConnectionStatus", connsNReqsArrayList.get(itemPosition).getConnectionStatus());
            jsonObject.put("TokenKey", connsNReqsArrayList.get(itemPosition).getTokenKey());
            jsonObject.put("ConnectTime_From", FromTime);
            jsonObject.put("ConnectTime_To", ToTime);

            new Accept_Rej_AbortReqAsync().execute(jsonObject);


            //make the query on sqlite db
            MySqliteDB mySqliteDB = new MySqliteDB(myContext);

            boolean result = mySqliteDB.UpdateOnConnectionsTbl
                    (
                            holder.txtPhone_ConnsNReqs.getText().toString(),
                            myPhoneNumber,
                            connsNReqsArrayList.get(itemPosition).getConnectionStatus(),
                            FromTime,
                            ToTime
                    );
            Log.d("updateSqlite", "completed");

            if (!result) {
                Toast.makeText(myContext, "error query of sqlite update connection", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    private class Accept_Rej_AbortReqAsync extends AsyncTask<JSONObject, Void, String> {

        @Override
        protected String doInBackground(JSONObject... jsonObjects) {
            RequestBody requestBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/ResponseToConnectionReq")
                    .post(requestBody)
                    .build();

            Response response = null;
            String s = null;
            try {
                response = client.newCall(request).execute();
                s = response.body().string();
                JSONObject jsonObject = new JSONObject(s);
                s = jsonObject.get("msg").toString();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return s;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (!s.equals("Updated")) {
                Toast.makeText(myContext, s, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
