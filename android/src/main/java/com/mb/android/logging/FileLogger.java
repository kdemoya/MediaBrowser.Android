package com.mb.android.logging;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.mb.android.MB3Application;
import com.mb.android.utils.Utils;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.logging.LogSeverity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Mark on 11/12/13.
 */
public class FileLogger implements ILogger {

    private static FileLogger mInstance;
    private String mExternalStoragePath = Environment.getExternalStorageDirectory().toString();
    private File mAppDirectory = new File(mExternalStoragePath + "/" + "Mb3AndroidData/Logs");
    private File mActiveLogFile;
    private boolean loggingEnabled;
    private int mLogLevel = 1;


    private FileLogger() {

        // Only proceed if the storage location exists and is accessible
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (!mAppDirectory.exists()) {
                mAppDirectory.mkdirs();
            }

            // Will generate a file/path naming structure like: sdcard/Mb3AndroidData/client-067e6162-3b6f-4ae2-a171-2470b63dff00.txt
            mActiveLogFile = new File(mAppDirectory.getPath() + "/client-" + UUID.randomUUID().toString() + ".txt");

            try {
                if (mActiveLogFile.createNewFile()) {
                    loggingEnabled = true;
                    Log.i("", "New file created at: " + mActiveLogFile.getPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
                loggingEnabled = false;
            }
            cleanUpLogFolder();
        } else {
            Log.i("", "Mediastate = " + Environment.getExternalStorageState());
        }

    }


    public static FileLogger getFileLogger() {

        if (mInstance == null) {
            mInstance = new FileLogger();
            try {
                mInstance.WriteLogHeader();
            } catch (Exception e) {

            }
        }

        return mInstance;
    }


    public void setLoggingLevel(int level) {
        mLogLevel = level;
    }


    public File GetActiveLogFile() {
        return mActiveLogFile;
    }


    public String LogFilePath() {
        if (mAppDirectory != null) {
            return mAppDirectory.getPath();
        }

        return "";
    }


    private void WriteLogHeader() {

        PackageInfo pInfo = null;
        try {
            pInfo = MB3Application.getInstance().getPackageManager().getPackageInfo(MB3Application.getInstance().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (pInfo != null) {
            mInstance.LogMessage("Application Version: " + pInfo.versionName, LogSeverity.Info);
        }

        if (Build.VERSION.RELEASE != null) {
            mInstance.Info("Android Version: " + Build.VERSION.RELEASE);
        }

        if (Build.MODEL != null && !Build.MODEL.isEmpty())
            mInstance.LogMessage("Device: " + Build.MODEL, LogSeverity.Info);

        try {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = (WindowManager) MB3Application.getInstance().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            display.getMetrics(metrics);
            mInstance.LogMessage("Screen Width: " + String.valueOf(metrics.widthPixels), LogSeverity.Info);
            mInstance.LogMessage("Screen Height: " + String.valueOf(metrics.heightPixels), LogSeverity.Info);
            mInstance.LogMessage("Density: " + String.valueOf(metrics.density), LogSeverity.Info);
            mInstance.LogMessage("DensityDpi: " + String.valueOf(metrics.densityDpi), LogSeverity.Info);
            mInstance.LogMessage("### ALWAYS PROVIDE FULL LOGS WHEN REPORTING ISSUES ###", LogSeverity.Info);
        } catch (Exception e) {

        }

        mInstance.LogMessage("Total Memory Available: " + Utils.TotalMemory(MB3Application.getInstance()), LogSeverity.Info);
        mInstance.LogMessage("Max Memory usable per Application: " + Utils.MaxApplicationMemory(), LogSeverity.Info);
    }

    private void cleanUpLogFolder() {
        if (mAppDirectory == null) return;

        Thread thread = new Thread() {
            @Override
            public void run() {
                String[] fileNames = mAppDirectory.list();
                if (fileNames == null || fileNames.length == 0) return;

                Long currentTime = new Date().getTime();

                for (String fileName : fileNames) {
                    File file = new File(mAppDirectory.getPath() + "/" + fileName);
                    if (!file.exists()) continue;
                    // Delete files older than 7 days.
                    if (currentTime - file.lastModified() >= 604800000) {
                        file.delete();
                    }
                }
            }
        };
        thread.run();
    }

    //******************************************************************************************************************
    // ILogger methods
    //******************************************************************************************************************

    @Override
    public void Info(String message, Object... paramList) {
        try {
            mInstance.LogMessage(String.format(message, paramList), LogSeverity.Info);
        } catch (Exception e) {
            mInstance.LogMessage(message, LogSeverity.Info);
        }
    }


    @Override
    public void Error(String message, Object... paramList) {
        mInstance.LogMessage(String.format(message, paramList), LogSeverity.Error);
    }


    @Override
    public void Warn(String message, Object... paramList) {
        mInstance.LogMessage(String.format(message, paramList), LogSeverity.Warn);
    }


    @Override
    public void Debug(String message, Object... paramList) {
            mInstance.LogMessage(String.format(message, paramList), LogSeverity.Debug);
    }


    @Override
    public void Fatal(String message, Object... paramList) {
        mInstance.LogMessage(String.format(message, paramList), LogSeverity.Fatal);
    }


    @Override
    public void FatalException(String message, Exception exception, Object... paramList) {
        mInstance.logException(String.format(message, paramList), exception, LogSeverity.Fatal);
    }


    @Override
    public void ErrorException(String message, Exception exception, Object... paramList) {
        mInstance.logException(String.format(message, paramList), exception, LogSeverity.Error);
    }

    //******************************************************************************************************************

    private void logException(String message, Exception exception, LogSeverity severity) {
        try {
            message += "\r" + stackTraceToString(exception);
            if (exception.getCause() != null) {
                message += "caused by " + stackTraceToString(exception.getCause());
            }
        } catch (Exception e) {
            Log.d("FileLogger", "failed to parse exception");
        }
        mInstance.LogMessage(message, severity);
    }


    private String stackTraceToString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString());
            sb.append("\r");
        }
        return sb.toString();
    }


    private void LogMessage(String message, LogSeverity severity) {

        if (logLevelIsSufficientToWriteMessage(severity)) {
            LogRow row = new LogRow();
            row.mMessage = message;
            row.mTime = new Date();
            row.mSeverity = severity;

            mInstance.LogMessage(row);
        }
    }


    private void LogMessage(LogRow row) {

        if (!loggingEnabled) return;
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(mActiveLogFile, true)));
            out.println(row.toString() + "\r");
            out.close();

        } catch (Exception e) {
            Log.i("", "Something went wrong writing file contents");
        }

    }


    private boolean logLevelIsSufficientToWriteMessage(LogSeverity severity) {

        int messageLogLevel = getLogLevel(severity);

        return messageLogLevel >= mLogLevel;
    }


    private int getLogLevel(LogSeverity severity) {

        switch (severity) {
            case Debug:
                return LogLevel.Debug;
            case Error:
                return LogLevel.Error;
            case Fatal:
                return LogLevel.Fatal;
            case Info:
                return LogLevel.Info;
            case Warn:
                return LogLevel.Warn;
            default:
                throw new IllegalArgumentException("Unknown LogSeverity");

        }

    }
}
