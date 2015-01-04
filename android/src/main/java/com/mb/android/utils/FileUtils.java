package com.mb.android.utils;

import android.content.Context;

import com.mb.android.SavedSessionInfo;
import com.mb.android.exceptions.LoadFileException;
import com.mb.android.exceptions.SaveFileException;
import com.mb.android.logging.FileLogger;
import com.mb.network.ServerInformationWrapper;
import mediabrowser.model.apiclient.ServerInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.List;

/**
 * Created by Mark on 11/12/13.
 *
 * <p>A utility class that contains various methods used to save and load data to/from the filesystem
 */
public class FileUtils {

    // File names to use when saving/loading data
    private static final String LAST_CONNECTED = "last_connected";
    private static final String SERVER_SETTINGS = "server_settings";


    /**
     * Load the last connected server from the filesystem.
     *
     * @param context The {@link android.content.Context} to use for the load action
     * server, or null.
     * @throws LoadFileException
     */
    public static SavedSessionInfo LoadSavedSessionInfo(Context context) throws LoadFileException {
        if (context == null) {
            throw new NullPointerException("context");
        }
        try {

            File file = context.getFileStreamPath(LAST_CONNECTED);

            if (!file.exists())
                return null;

            FileLogger.getFileLogger().Info("Loading last connected server from app directory");

            FileInputStream fis = context.openFileInput(LAST_CONNECTED);
            ObjectInputStream is = new ObjectInputStream(fis);
            SavedSessionInfo savedSession;
            savedSession = (SavedSessionInfo) is.readObject();
            is.close();

            return savedSession;

        } catch (FileNotFoundException e) {
            throw new LoadFileException("File not found", e);
        } catch (StreamCorruptedException e) {
            throw new LoadFileException("Corrupt stream", e);
        } catch (IOException e) {
            throw new LoadFileException("IO exception", e);
        } catch (ClassNotFoundException e) {
            throw new LoadFileException("Class not found", e);
        } catch (ClassCastException e) {
            throw new LoadFileException("Class cast exception", e);

        }
    }


    /**
     * Save the last connected server to the file system
     *
     * @param context The Context to use for the save action
     * @param savedSession  The SavedSessionInfo object to save to the filesystem
     * @throws SaveFileException
     */
    public static void SaveSavedSessionInfo(Context context, SavedSessionInfo savedSession) throws SaveFileException {
        if (context == null) {
            throw new NullPointerException("context");
        }
        if (savedSession == null) {
            throw new NullPointerException("savedSession");
        }
        try {
            FileLogger.getFileLogger().Info("Saving last connected to app directory");

            FileOutputStream fos = context.openFileOutput(LAST_CONNECTED, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(savedSession);
            os.close();
        } catch (FileNotFoundException e) {
            throw new SaveFileException("File not Found", e);
        } catch (IOException e) {
            throw new SaveFileException("IO Exception", e);
        }
    }


    public static boolean DeleteLastConnected(Context context) {
        if (context == null) {
            throw new NullPointerException("context");
        }
        File file = context.getFileStreamPath(LAST_CONNECTED);

        return file.exists() && file.delete();

    }
    /**
     * Save the the list of known servers to the filesystem
     *
     * @param context    The context to use for the save action
     * @param serverList The ServerInformationWrapper containing the list of servers to save
     * @throws SaveFileException
     */
    public static void SaveServerList(Context context, ServerInformationWrapper serverList) throws SaveFileException {
        if (context == null) {
            throw new NullPointerException("context");
        }
        if (serverList == null) {
            throw new NullPointerException("serverList");
        }
        try {
            FileLogger.getFileLogger().Info("Saving server list");
            FileOutputStream fos = context.openFileOutput(SERVER_SETTINGS, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(serverList.Servers);
            os.close();
        } catch (FileNotFoundException e) {
            throw new SaveFileException("File not found exception", e);
        } catch (IOException e) {
            throw new SaveFileException("IO Exception", e);
        } catch (NullPointerException e) {
            throw new SaveFileException("Null pointer handled");
        }
    }


    /**
     * Load the stored server list from the devices file system
     *
     * @param context The context to use in the loading attempt
     * @return A ServerInformationWrapper object containing the stored servers
     * @throws LoadFileException
     */
    @SuppressWarnings("unchecked") // We know the List<ServerInformation> cast is correct. Move on.
    public static ServerInformationWrapper LoadServerList(Context context) throws LoadFileException {
        if (context == null) {
            throw new NullPointerException("context");
        }
        try {

            File file = context.getFileStreamPath(SERVER_SETTINGS);

            if (!file.exists())
                return null;

            FileLogger.getFileLogger().Info("Loading server list from app directory");

            FileInputStream fis = context.openFileInput(SERVER_SETTINGS);
            ObjectInputStream is = new ObjectInputStream(fis);
            ServerInformationWrapper settings = new ServerInformationWrapper();
            settings.Servers = (List<ServerInfo>) is.readObject();
            is.close();
            return settings;

        } catch (FileNotFoundException e) {
            throw new LoadFileException("File not found exception", e);
        } catch (StreamCorruptedException e) {
            throw new LoadFileException("Stream corrupt exception", e);
        } catch (IOException e) {
            throw new LoadFileException("IO exception", e);
        } catch (ClassNotFoundException e) {
            throw new LoadFileException("Class not found exception", e);
        }
    }
}
