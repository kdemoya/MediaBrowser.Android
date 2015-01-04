package com.mb.android.displaypreferences;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mb.android.MB3Application;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.extensions.StringHelper;

/**
 * Created by Mark on 2014-11-22.
 */
public class DisplayPreferenceManager {

    GsonJsonSerializer serializer;
    PreferencesOpenHelper mPreferncesHelper;

    public DisplayPreferenceManager() {
        serializer = new GsonJsonSerializer();
        mPreferncesHelper = new PreferencesOpenHelper(MB3Application.getInstance());
    }

    /**
     * Save a DisplayPreference for an item
     * @param item
     * @param displayPreference
     */
    public void saveDisplayPreference(BaseItemDto item, DisplayPreference displayPreference) {

        if (item == null) {
            throw new IllegalArgumentException("item");
        }
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getType())) {
            throw new IllegalArgumentException("item.value");
        }
        if (displayPreference == null) {
            throw new IllegalArgumentException("displayPreference");
        }

        ContentValues values = new ContentValues();
        values.put(mPreferncesHelper.KEY_ID, item.getType());
        values.put(mPreferncesHelper.KEY_VALUES, serializer.SerializeToString(displayPreference));

        SQLiteDatabase db = mPreferncesHelper.getWritableDatabase();
        db.replace(mPreferncesHelper.DISPLAY_PREFERENCES_TABLE_NAME, null, values);
    }

    /**
     * Retrieve a saved display preference for an item. If a DisplayPreference doesn't exist then a default on will be
     * created.
     *
     * @param item The item to retrieve the DisplayPreference for
     * @return
     */
    public DisplayPreference retrieveDisplayPreference(BaseItemDto item) {

        if (item == null) {
            throw new IllegalArgumentException("item");
        }
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getType())) {
            throw new IllegalArgumentException("item.value");
        }

        DisplayPreference preference = readDisplayPreferenceFromDatabase(item.getType());

        if (preference == null) {
            preference = getDefaultPreference();
        }

        return preference;
    }

    private DisplayPreference readDisplayPreferenceFromDatabase(String itemId) {

        SQLiteDatabase db = mPreferncesHelper.getReadableDatabase();

        Cursor c = db.query(
                mPreferncesHelper.DISPLAY_PREFERENCES_TABLE_NAME,
                new String[]{mPreferncesHelper.KEY_VALUES},
                mPreferncesHelper.KEY_ID + " = \"" + itemId + "\"",
                null,
                null,
                null,
                null
        );

        DisplayPreference preference = null;

        if (c.moveToFirst()) {
            String json = c.getString(c.getColumnIndex(mPreferncesHelper.KEY_VALUES));
            preference = serializer.DeserializeFromString(json, DisplayPreference.class);
        }
        c.close();

        return preference;
    }


    private DisplayPreference getDefaultPreference() {
        return new DisplayPreference();
    }
}
