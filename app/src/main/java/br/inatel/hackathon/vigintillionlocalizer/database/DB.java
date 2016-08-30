package br.inatel.hackathon.vigintillionlocalizer.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.maps.model.LatLng;

import java.util.LinkedList;
import java.util.List;

import br.inatel.hackathon.vigintillionlocalizer.model.Beacon;
import br.inatel.hackathon.vigintillionlocalizer.model.TrackedBeacon;

/**
 * Created by lucas on 27/08/2016.
 */
public class DB {
    private SQLiteDatabase db_detected;
    private SQLiteDatabase db_tracked;

    public DB(Context ctx) {
        db_detected = new DBCoreDetectedBeacons(ctx).getWritableDatabase();
        db_tracked = new DBCoreTrackedBeacons(ctx).getWritableDatabase();
    }

    public void detected_insert(Beacon beacon){
        ContentValues values = new ContentValues();
        values.put("id", beacon.getId());
        values.put("rssi", beacon.getRssi());
        values.put("latitude", beacon.getLocation().latitude);
        values.put("longitude", beacon.getLocation().longitude);
        values.put("timestamp", beacon.getTimestamp());
        db_detected.insert("beacon", null, values);
    }

    public void detected_update(Beacon beacon){
        String id = beacon.getId();
        Beacon b = detected_search(id);
        if (b != null) {
            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("rssi", beacon.getRssi());
            values.put("latitude", beacon.getLocation().latitude);
            values.put("longitude", beacon.getLocation().longitude);
            values.put("timestamp", beacon.getTimestamp());
            db_detected.update("beacon", values, "id = ?", new String[]{id});
        }
    }

    public void detected_delete(Beacon beacon){
        db_detected.delete("beacon", "_id = " + beacon.getId(), null);
    }

    public Beacon detected_search(String id) {
        String[] columns = new String[]{"_id", "id", "rssi", "latitude", "longitude", "timestamp"};
        Cursor cursor = db_detected.query("beacon", columns, null, null, null, null, "timestamp DESC");
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                String dbId = cursor.getString(1);
                if (dbId.equals(id)) {
                    return new Beacon()
                            .setId(id)
                            .setRssi(cursor.getInt(2))
                            .setLocation(new LatLng(cursor.getFloat(3),cursor.getFloat(4)))
                            .setTimestamp(cursor.getLong(5));
                }
            } while (cursor.moveToNext());
        }
        return null;
    }

    public void tracked_add(String beacon, int color_id) {
        ContentValues values = new ContentValues();
        values.put("mac", beacon);
        values.put("color", color_id);
        db_tracked.insert("tracked_beacons", null, values);
    }

    public void tracked_delete(List<String> beacons) {
        for (String beacon: beacons)
            db_tracked.delete("tracked_beacons", "mac = \"" + beacon + "\"", null);
    }

    public static List<String> getIds(List<TrackedBeacon> beacons) {
        List<String> result = new LinkedList<>();
        for (TrackedBeacon b: beacons) {
            result.add(b.beacon_id);
        }
        return result;
    }

    public List<TrackedBeacon> tracked_get() {
        List<TrackedBeacon> result = new LinkedList<>();
        String[] columns = new String[]{"_id", "mac", "color"};
        Cursor cursor = db_tracked.query("tracked_beacons", columns, null, null, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                Integer c = cursor.getInt(2);
                int color = (c == null ? 0 : c);
                result.add(new TrackedBeacon(cursor.getString(1), color));
            } while (cursor.moveToNext());
        }
        return result;
    }
}

