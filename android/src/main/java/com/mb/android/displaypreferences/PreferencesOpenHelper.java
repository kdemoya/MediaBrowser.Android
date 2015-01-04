package com.mb.android.displaypreferences;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Mark on 2014-11-22.
 */
public class PreferencesOpenHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "preferences";
    private static final int DATABASE_VERSION = 1;
    public static final String DISPLAY_PREFERENCES_TABLE_NAME = "display_preferences";
    public static final String KEY_ID = "PREFERENCE_ID";
    public static final String KEY_VALUES = "PREFERENCE_DATA";

    private static final String DISPLAY_PREFERENCE_TABLE_CREATE =
            "CREATE TABLE " + DISPLAY_PREFERENCES_TABLE_NAME + " (" + KEY_ID + " TEXT PRIMARY KEY, " + KEY_VALUES + " TEXT);";


    public PreferencesOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DISPLAY_PREFERENCE_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
