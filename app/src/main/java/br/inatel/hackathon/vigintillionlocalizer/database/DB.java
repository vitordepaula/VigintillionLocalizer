package br.inatel.hackathon.vigintillionlocalizer.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.LinkedList;
import java.util.List;

import br.inatel.hackathon.vigintillionlocalizer.model.Beacon;

/**
 * Created by lucas on 27/08/2016.
 */
public class DB {
    private SQLiteDatabase db_detected;
    private SQLiteDatabase db_tracked;

    public DB(Context ctx){
        db_detected = new DBCoreDetectedBeacons(ctx).getWritableDatabase();
        db_tracked = new DBCoreTrackedBeacons(ctx).getWritableDatabase();
    }

    public void detected_insert(Beacon beacon){
        ContentValues values = new ContentValues();
        values.put("mac", beacon.getMac());
        values.put("rssi", beacon.getRssi());
        values.put("latitude", beacon.getLatitude());
        values.put("longitude", beacon.getLongitude());
        values.put("date", beacon.getDate());
        db_detected.insert("beacon", null, values);
    }

    public void detected_update(Beacon beacon){
        long idFound = detected_search(beacon.getMac());
        if(idFound != -1){
            ContentValues values = new ContentValues();
            values.put("mac", beacon.getMac());
            values.put("rssi", beacon.getRssi());
            values.put("latitude", beacon.getLatitude());
            values.put("longitude", beacon.getLongitude());
            values.put("date", beacon.getDate());
            db_detected.update("beacon", values, "_id = ?", new String[]{"" + idFound});
        }
    }

    public void detected_delete(Beacon beacon){
        db_detected.delete("beacon", "_id = " + beacon.getId(), null);
    }

    public long detected_search(String mac) {
        String[] columns = new String[]{"_id", "mac", "rssi", "latitude", "longitude", "date"};
        Cursor cursor = db_detected.query("beacon", columns, null, null, null, null, "date DESC");
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

    public void tracked_add(String beacon) {
        ContentValues values = new ContentValues();
        values.put("mac", beacon);
        db_tracked.insert("tracked_beacons", null, values);
    }

    public void tracked_delete(List<String> beacons) {
        for (String beacon: beacons)
            db_tracked.delete("tracked_beacons", "mac = \"" + beacon + "\"", null);
    }

    public List<String> tracked_get() {
        List<String> result = new LinkedList<>();
        String[] columns = new String[]{"_id", "mac"};
        Cursor cursor = db_tracked.query("tracked_beacons", columns, null, null, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                result.add(cursor.getString(1));
            } while (cursor.moveToNext());
        }
        return result;
    }
}

