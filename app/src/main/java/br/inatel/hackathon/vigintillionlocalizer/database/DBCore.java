package br.inatel.hackathon.vigintillionlocalizer.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by lucas on 27/08/2016.
 */
public class DBCore extends SQLiteOpenHelper {
    private static final String NAME_DB = "beacon";
    private static final int VERSION_DB = 2;

    public DBCore(Context context) {
        super(context, NAME_DB, null, VERSION_DB);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table beacon" +
                "(_id integer primary key autoincrement, " +
                "mac text not null, " +
                "rssi integer not null, " +
                "latitude float not null, " +
                "longitude float not null, " +
                "date text not null);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table user");
        onCreate(db);
    }
}
