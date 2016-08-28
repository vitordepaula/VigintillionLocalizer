package br.inatel.hackathon.vigintillionlocalizer.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by lucas on 27/08/2016.
 */
public class DBCoreTrackedBeacons extends SQLiteOpenHelper {
    private static final String NAME_DB = "tracked_beacons";
    private static final int VERSION_DB = 1;

    public DBCoreTrackedBeacons(Context context) {
        super(context, NAME_DB, null, VERSION_DB);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table tracked_beacons" +
                "(_id integer primary key autoincrement, " +
                "mac text not null);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            db.execSQL("drop table tracked_beacons");
            onCreate(db);
        }
    }
}
