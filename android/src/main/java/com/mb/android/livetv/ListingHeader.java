package com.mb.android.livetv;

/**
 * Created by Mark on 2014-06-04.
 */
public class ListingHeader implements IListing {

    public String day;
    public String date;

    @Override
    public boolean isHeader() {
        return true;
    }
}
