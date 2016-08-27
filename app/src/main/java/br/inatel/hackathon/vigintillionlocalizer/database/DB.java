package br.inatel.hackathon.vigintillionlocalizer.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import br.inatel.hackathon.vigintillionlocalizer.model.Beacon;

/**
 * Created by lucas on 27/08/2016.
 */
public class DB {
    private SQLiteDatabase db;

    public DB(Context ctx){
        DBCore auxDB = new DBCore(ctx);
        db = auxDB.getWritableDatabase();
    }

    public void insert(Beacon beacon){
        ContentValues values = new ContentValues();
        values.put("mac", beacon.getMac());
        values.put("rssi", beacon.getRssi());
        values.put("latitude", beacon.getLatitude());
        values.put("longitude", beacon.getLongitude());
        values.put("date", beacon.getDate());
        db.insert("beacon", null, values);
    }

    public void update(Beacon beacon){
        long idFound = search(beacon.getMac());
        if(idFound != -1){
            ContentValues values = new ContentValues();
            values.put("mac", beacon.getMac());
            values.put("rssi", beacon.getRssi());
            values.put("latitude", beacon.getLatitude());
            values.put("longitude", beacon.getLongitude());
            values.put("date", beacon.getDate());
            db.update("beacon", values, "_id = ?", new String[]{"" + idFound});
        }
    }

    public void delete(Beacon beacon){
        db.delete("beacon", "_id = " + beacon.getId(), null);
    }

    public long search(String mac) {
        String[] columns = new String[]{"_id", "mac", "rssi", "latitude", "longitude", "date"};
        Cursor cursor = db.query("beacon", columns, null, null, null, null, "date DESC");
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                String macFromDB = cursor.getString(1);
                if (macFromDB.equals(mac)) {
                    long id = cursor.getLong(0);
                    return id;
                }
            } while (cursor.moveToNext());
        }
        return -1;
    }

}

