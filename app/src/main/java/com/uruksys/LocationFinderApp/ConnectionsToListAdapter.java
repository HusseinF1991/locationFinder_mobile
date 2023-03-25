package com.uruksys.LocationFinderApp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConnectionsToListAdapter extends ArrayAdapter implements View.OnClickListener {

    private String myPhoneNumber;
    private String serverIp = "http://23.239.203.134:3000/LocationFinderApp";
    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();
    private Context myContext;
    private ArrayList<UserInfoModel_ConnectionsTo> connectionsToArrayList;
    int resource;

    public ConnectionsToListAdapter(@NonNull Context context, int resource, @NonNull ArrayList<UserInfoModel_ConnectionsTo> objects) {
        super(context, resource, objects);
        this.myContext = context;
        this.resource = resource;
        this.connectionsToArrayList = objects;


        //get the information of the user from the sharedPreferences
        SharedPreferences sharedPref = myContext.getSharedPreferences("LocationFinderSharedPrefereces", Context.MODE_PRIVATE);
        myPhoneNumber = sharedPref.getString("LocationFinderPhoneSharedPrefereces", "notRegistered");
    }


    @Override
    public void onClick(View v) {
        int itemPosition = (int) v.getTag(R.string.itemPositionInList);
        ViewHolder holder = (ViewHolder) v.getTag(R.string.holderTagKey);

        if(ConnectionsToActivity.isConnected_ConnectionToActivity){

            AbortConnectionTo(itemPosition, holder.txtStatus_ConnectionsTo.getText().toString());

            holder.txtStatus_ConnectionsTo.setText("ملغي");
            holder.txtStatus_ConnectionsTo.setTextColor(Color.RED);
            holder.btnAbort_ConnectionsTo.setEnabled(false);
        }
        else{
            Toast.makeText(myContext, "غير مرتبط بالانترنت", Toast.LENGTH_SHORT).show();
        }

    }


    class ViewHolder {
        TextView txtUserName_ConnectionsTo, txtPhone_ConnectionsTo, txtStatus_ConnectionsTo , txtToTime_ConnectionsTo , txtFromTime_ConnectionsTo;
        Button btnAbort_ConnectionsTo;

    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View itemView;
        ViewHolder holder;
        UserInfoModel_ConnectionsTo userInfoModel_connectionsTo = connectionsToArrayList.get(position);

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(myContext);
            convertView = inflater.inflate(resource, parent, false);
            holder = new ViewHolder();

            holder.btnAbort_ConnectionsTo = convertView.findViewById(R.id.btnAbort_ConnectionsTo);
            holder.txtPhone_ConnectionsTo = convertView.findViewById(R.id.txtPhone_ConnectionsTo);
            holder.txtStatus_ConnectionsTo = convertView.findViewById(R.id.txtStatus_ConnectionsTo);
            holder.txtUserName_ConnectionsTo = convertView.findViewById(R.id.txtUserName_ConnectionsTo);
            holder.txtToTime_ConnectionsTo = convertView.findViewById(R.id.txtToTime_ConnectionsTo);
            holder.txtFromTime_ConnectionsTo = convertView.findViewById(R.id.txtFromTime_ConnectionsTo);


            itemView = convertView;
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
            itemView = convertView;
        }

        holder.txtUserName_ConnectionsTo.setText(userInfoModel_connectionsTo.getUserName());
        holder.txtStatus_ConnectionsTo.setText(userInfoModel_connectionsTo.getConnectionStatus());
        holder.txtPhone_ConnectionsTo.setText(userInfoModel_connectionsTo.getPhoneNumber());
        holder.txtToTime_ConnectionsTo.setText(userInfoModel_connectionsTo.getConnectTime_To());
        holder.txtFromTime_ConnectionsTo.setText(userInfoModel_connectionsTo.getConnectTime_From());
        if (userInfoModel_connectionsTo.getConnectionStatus().equals("Connected")){

            holder.btnAbort_ConnectionsTo.setText("الغاء الارتباط");
            holder.txtStatus_ConnectionsTo.setText("مرتبط");
            holder.txtStatus_ConnectionsTo.setTextColor(Color.parseColor("#13A5bb"));
        }
        else {

            holder.txtStatus_ConnectionsTo.setText("بأنتظار الرد");
            holder.txtStatus_ConnectionsTo.setTextColor(Color.RED);
            holder.btnAbort_ConnectionsTo.setText("الغاء الطلب");
        }

        holder.btnAbort_ConnectionsTo.setTag(R.string.holderTagKey, holder);
        holder.btnAbort_ConnectionsTo.setTag(R.string.itemPositionInList, position);
        holder.btnAbort_ConnectionsTo.setOnClickListener(this);
        return itemView;
    }


    private void AbortConnectionTo(int itemPosition, String oldConnectionStatus) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("accessToPhone", connectionsToArrayList.get(itemPosition).getPhoneNumber());
            jsonObject.put("accessFromPhone", myPhoneNumber);
            jsonObject.put("oldConnectionStatus", oldConnectionStatus);
            jsonObject.put("tokenKey", connectionsToArrayList.get(itemPosition).getTokenKey());

            new AbortConnectionToAsync().execute(jsonObject);


            //make the query on sqlite db
            MySqliteDB mySqliteDB = new MySqliteDB(myContext);

            boolean result;

            result = mySqliteDB.UpdateOnConnectionsTbl(myPhoneNumber, connectionsToArrayList.get(itemPosition).getPhoneNumber(), "Aborted");
            Log.d("updateSqlite", "completed");

            if (!result) {
                Toast.makeText(myContext, "error query of sqlite update connection", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private class AbortConnectionToAsync extends AsyncTask<JSONObject, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(JSONObject... jsonObjects) {

            RequestBody rBody = RequestBody.create(JSON, String.valueOf(jsonObjects[0]));
            Request request = new Request.Builder()
                    .url(serverIp + "/abortConnectionTo")
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
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }


        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);

            try {
                String responseMsg = jsonObject.get("msg").toString();
                Toast.makeText(myContext, responseMsg, Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
