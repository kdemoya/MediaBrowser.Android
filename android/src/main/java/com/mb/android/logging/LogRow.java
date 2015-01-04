package com.mb.android.logging;

import mediabrowser.model.logging.LogSeverity;

import java.util.Date;

/**
 * Created by Mark on 11/12/13.
 */
public class LogRow {

    public String mMessage;
    public Date mTime;
    public LogSeverity mSeverity;


    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(mTime.toString());
        builder.append("] , ");
        builder.append(mSeverity.toString());
        builder.append(" , ");
        builder.append(mMessage);
        builder.append(" , ");
        builder.append(String.valueOf(Thread.currentThread().getId()));
        builder.append(" , ");
        builder.append(Thread.currentThread().getName());

        return builder.toString();
    }
}
