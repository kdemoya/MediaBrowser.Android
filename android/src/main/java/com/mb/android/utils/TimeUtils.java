package com.mb.android.utils;

import mediabrowser.model.extensions.StringHelper;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Mark on 2014-11-26.
 */
public class TimeUtils {

    public static long msToTicks(int milliseconds) {
        return (long)milliseconds * 10000;
    }

    public static long secondsToTicks(int seconds) {
        return (long)seconds * 10000000;
    }

    public static int secondsToMs(int seconds) {
        return seconds * 1000;
    }

    public static String timestampTofriendlyHoursMinutes(Date timestamp) {
        String timeString = "";
        if (timestamp == null) return timeString;

        Date formattedDate = Utils.convertToLocalDate(timestamp);
        DateFormat outputFormat = new SimpleDateFormat("hh:mm a");
        timeString = outputFormat.format(formattedDate);

        return timeString;
    }

    public static int elapsedMillisecondsSinceTimestamp(Date timestamp) {
        int elapsed = 0;

        if (timestamp == null) return elapsed;

        Date formattedDate = Utils.convertToLocalDate(timestamp);
        Date now = new Date();
        elapsed = (int)(now.getTime() - formattedDate.getTime());

        return elapsed;
    }
}
