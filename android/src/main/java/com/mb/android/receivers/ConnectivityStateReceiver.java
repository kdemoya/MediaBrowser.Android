package com.mb.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.mb.android.MB3Application;
import com.mb.android.logging.FileLogger;
import com.mb.network.ConnectionState;

/**
 * Created by Mark on 2014-05-01.
 *
 * This BroadcastReceiver receives android.net.wifi.STATE_CHANGE messages from the system. It's purpose
 * is to initiate the chain of events that will update the API to the new server address.
 */
public class ConnectivityStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (!action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            return;
        }

        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

        if (networkInfo.isConnected()) {

            // Wifi is available
            MB3Application.getInstance().SetConnectionState(ConnectionState.CONNECTED_WIFI);
            FileLogger.getFileLogger().Info("WiFi is CONNECTED");

        } else {

            // This BroadcastReceiver seems to fire several times when disconnected from wifi.
            // So only test for cellular connectivity if we're not already connected to cellular or
            // set as disconnected.
            if (!MB3Application.getInstance().GetConnectionState().equals(ConnectionState.CONNECTED_CELLULAR) &&
                    !MB3Application.getInstance().GetConnectionState().equals(ConnectionState.DISCONNECTED)) {
                ConnectivityManager connectivityManager = (ConnectivityManager) MB3Application.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo cellularNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

                // Use Cellular if available
                if (cellularNetworkInfo != null && cellularNetworkInfo.isConnectedOrConnecting()) {
                    MB3Application.getInstance().SetConnectionState(ConnectionState.CONNECTED_CELLULAR);
                    FileLogger.getFileLogger().Info("Cellular is CONNECTED");
                } else {
                    MB3Application.getInstance().SetConnectionState(ConnectionState.DISCONNECTED);
                    FileLogger.getFileLogger().Info("DISCONNECTED");
                }
            }


        }
    }
}
