package com.mb.android.exceptions;

import com.mb.android.logging.FileLogger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by Mark on 2014-09-17.
 */
public class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler {

    public void uncaughtException(Thread thread, Throwable ex) {
        FileLogger.getFileLogger().Error("Uncaught Exception ");
        LogStackTrace(ex);
    }

    public void LogStackTrace(Throwable ex) {

        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        ex.printStackTrace(printWriter);
        FileLogger.getFileLogger().Error("Stack Trace: " + result.toString());
        if (ex.getCause() != null) {
            LogStackTrace(ex.getCause());
        }

        printWriter = null;
        result = null;
    }
}

