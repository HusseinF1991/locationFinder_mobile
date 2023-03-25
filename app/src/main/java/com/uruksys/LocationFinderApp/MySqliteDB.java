package com.uruksys.LocationFinderApp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class MySqliteDB extends SQLiteOpenHelper {

    private static final String sqliteDbName = "locationfinderapp";

    public MySqliteDB(@Nullable Context context) {
        super(context, sqliteDbName, null, 1);


        Log.d("sqlite_db", "called");

        SQLiteDatabase mySqlite = this.getWritableDatabase();

        //add columns on the tbl if not exists
        String query = "select DISTINCT tbl_name from sqlite_master where tbl_name = 'connections'";
        try (Cursor cursor = mySqlite.rawQuery(query, null)) {
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    boolean isExist = false;
                    SQLiteDatabase db = this.getWritableDatabase();
                    Cursor res = db.rawQuery("PRAGMA table_info(connections)", null);
                    res.moveToFirst();
                    do {
                        String currentColumn = res.getString(1);
                        if (currentColumn.equals("ConnectTime_From")) {
                            isExist = true;
                        }
                    } while (res.moveToNext());

                    if (!isExist) {

                        mySqlite.execSQL("ALTER TABLE `connections` ADD COLUMN ConnectTime_From Time DEFAULT '00:00'");
                        mySqlite.execSQL("ALTER TABLE `connections` ADD COLUMN ConnectTime_To Time DEFAULT '23:59'");
                    }
                }
            }
        }
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("sqlite_db", "Created");

        db.execSQL("CREATE TABLE IF NOT EXISTS `connections` (" +
                "Id INTEGER NOT NULL primary key AUTOINCREMENT , " +
                "AccessFromPhone varchar(25) NOT NULL, " +
                "AccessFromUserName varchar(25) DEFAULT NULL, " +
                "AccessToPhone varchar(25) NOT NULL, " +
                "AccessToUserName varchar(25) DEFAULT NULL, " +
                "ConnectTime_From Time DEFAULT '00:00', " +
                "ConnectTime_To Time DEFAULT '23:59', " +
                "ConnectionStatus varchar(20) NOT NULL)");


        db.execSQL("CREATE TABLE IF NOT EXISTS `locations`(" +
                "Id INTEGER NOT NULL primary key AUTOINCREMENT , " +
                "lat double(10,7) NOT NULL, " +
                "lng double(10,7) NOT NULL, " +
                "loc_Date DateTime NOT NULL) ");
    }



    //reinsert all the old connections when the application is reinstalled
    public void ReinsertOldConnections(ArrayList<MyOldConnectionsClassModel> myOldConnections){

        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        //delete all records if exists
        sqLiteDatabase.execSQL("delete from `connections`");

        //reinsert the connections
        for (MyOldConnectionsClassModel myOldConnectionsClassModel: myOldConnections) {

            Log.d("sqlite_db", "InsertToConnectionsTbl_started");

            ContentValues contentValues = new ContentValues();
            contentValues.put("Id", myOldConnectionsClassModel.getId());
            contentValues.put("AccessFromPhone", myOldConnectionsClassModel.getAccessFromPhone());
            contentValues.put("AccessFromUserName", myOldConnectionsClassModel.getAccessFromUserName());
            contentValues.put("AccessToPhone", myOldConnectionsClassModel.getAccessToPhone());
            contentValues.put("AccessToUserName", myOldConnectionsClassModel.getAccessToUserName());
            contentValues.put("ConnectTime_From", myOldConnectionsClassModel.getConnectTime_From());
            contentValues.put("ConnectTime_To", myOldConnectionsClassModel.getConnectTime_To());
            contentValues.put("ConnectionStatus", myOldConnectionsClassModel.getConnectionStatus());

            Long result = sqLiteDatabase.insert("Connections", null, contentValues);

            Log.d("sqlite_db", "InsertToConnectionsTbl_completed " + result);
        }
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("sqlite_db", "Upgraded");

        //db.execSQL("DROP TABLE IF EXISTS connections");
        //onCreate(db);
    }


    public boolean InsertToConnectionsTbl(String accessFromPhone, String accessFromUserName, String accessToPhone, String accessToUserName, String connectionStatus, String connectTime_From, String connectTime_To) {

        Log.d("sqlite_db", "InsertToConnectionsTbl_started");

        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("AccessFromPhone", accessFromPhone);
        contentValues.put("AccessFromUserName", accessFromUserName);
        contentValues.put("AccessToPhone", accessToPhone);
        contentValues.put("AccessToUserName", accessToUserName);
        contentValues.put("ConnectTime_From", connectTime_From);
        contentValues.put("ConnectTime_To", connectTime_To);
        contentValues.put("ConnectionStatus", connectionStatus);

        Long result = sqLiteDatabase.insert("Connections", null, contentValues);

        Log.d("sqlite_db", "InsertToConnectionsTbl_completed " + result);

        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }


    public boolean UpdateOnConnectionsTbl(String accessFromPhone, String accessToPhone, String connectionStatus) {

        Log.d("sqlite_db", "UpdateOnConnectionsTbl_Started");

        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("ConnectionStatus", connectionStatus);

        int result = sqLiteDatabase.update("connections", contentValues, "AccessFromPhone = ? AND AccessToPhone = ?", new String[]{accessFromPhone, accessToPhone});


        Log.d("sqlite_db", "UpdateOnConnectionsTbl_Completed");

        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }


    //overload method of (UpdateOnConnectionsTbl) accepting connection request
    public boolean UpdateOnConnectionsTbl(String accessFromPhone, String accessToPhone, String connectionStatus, String connectTime_From, String connectTime_To) {

        Log.d("sqlite_db", "UpdateOnConnectionsTbl_Started");

        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("ConnectionStatus", connectionStatus);
        contentValues.put("ConnectTime_From", connectTime_From);
        contentValues.put("ConnectTime_To", connectTime_To);

        int result = sqLiteDatabase.update("connections", contentValues, "AccessFromPhone = ? AND AccessToPhone = ?", new String[]{accessFromPhone, accessToPhone});


        Log.d("sqlite_db", "UpdateOnConnectionsTbl_Completed");

        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }


    public Cursor GetAllConnections(String accessFromPhone, String accessToPhone) {
        Log.d("sqlite_db", "GetallConections_Started");

        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM connections WHERE AccessFromPhone =  ? AND AccessToPhone = ?", new String[]{accessFromPhone, accessToPhone});

        //sqLiteDatabase.close();
        Log.d("sqlite_db", "row:" + cursor.getCount());
        Log.d("sqlite_db", "GetallConections_Completed");

        return cursor;
    }


    public Cursor GetConnectionsTo(String accessFromPhone) {

        Log.d("sqlite_db", "GetConectionsTo_Started");

        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM connections WHERE AccessFromPhone =  ? AND ConnectionStatus = 'Connected'", new String[]{accessFromPhone});

        //sqLiteDatabase.close();
        Log.d("sqlite_db", "row:" + cursor.getCount());
        Log.d("sqlite_db", "GetConectionsTo_Completed");

        return cursor;
    }


    public void DeleteAllConnections(){

        Log.d("sqlite_db", "deleteAllConnectionsTbl_Started");

        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();

        sqLiteDatabase.execSQL("delete from `connections`");

        Log.d("sqlite_db", "DeleteAllConnectionsTbl_Completed");
    }

}
