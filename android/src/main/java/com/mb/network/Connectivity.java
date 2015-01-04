package com.mb.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

import com.mb.android.logging.FileLogger;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.extensions.StringHelper;

import java.util.Formatter;

/**
 * Created by Mark on 11/12/13.
 */
public class Connectivity {
    private final static String[] typicalLanIps = {"192.", "10.", "127."};

    /**
     * Check if there is any connectivity
     *
     * @param context
     * @return
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }


    /**
     * Check if there is fast connectivity
     *
     * @param context
     * @return
     */
    public static boolean isConnectedFast(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected() && Connectivity.isConnectionFast(info.getType(), info.getSubtype()));
    }


    /**
     * Check if the connection is fast
     *
     * @param type
     * @param subType
     * @return
     */
    public static boolean isConnectionFast(int type, int subType) {
        if (type == ConnectivityManager.TYPE_WIFI) {
            System.out.println("CONNECTED VIA WIFI");
            return true;
        } else if (type == ConnectivityManager.TYPE_ETHERNET) {
            System.out.println("CONNECTED VIA ETHERNET");
            return true;
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            switch (subType) {
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return false; // ~ 14-64 kbps
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    return true; // ~ 400-1000 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    return true; // ~ 600-1400 kbps
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return false; // ~ 100 kbps
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    return true; // ~ 2-14 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    return true; // ~ 700-1700 kbps
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    return true; // ~ 1-23 Mbps
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return true; // ~ 400-7000 kbps
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                    return true; // ~ 1-2 Mbps
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    return true; // ~ 5 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return true; // ~ 10-20 Mbps
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return false; // ~25 kbps
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return true; // ~ 10+ Mbps
                // Unknown
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                default:
                    return false;
            }
        } else {
            return false;
        }
    }


    /**
     * Determine if the client can likely connect to the server in question
     *
     * @param server The server trying to be reached
     * @return true if possible, false otherwise
     */
    public static boolean CanConnect(Context context, ServerInfo server) {

        if (!isConnected(context)) {
            FileLogger.getFileLogger()
                    .Error("CanConnect: Device is not connected to any networks");
            return false;
        }

        if (server == null || tangible.DotNetToJavaStringHelper.isNullOrEmpty(server.getLocalAddress())) {

            FileLogger.getFileLogger()
                    .Error("CanConnect: Server is null, or misconfigured");
            return false;
        }

        if (isConnectedLAN(context)) {

            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            int ip = wifiInfo.getIpAddress();

            Formatter formatter = new Formatter();
            String ipString = formatter.format("%d.%d.%d.%d",
                    (ip & 0xff),
                    (ip >> 8 & 0xff),
                    (ip >> 16 & 0xff),
                    (ip >> 24 & 0xff)).toString();
            formatter.close();

            if (isPrivateIp(ipString)) {
                // Server needs to be a private IP and have the first octet match.
                // Or a public IP
                if (isPrivateIp(server.getLocalAddress())) {
                    if (!server.getLocalAddress()
                            .startsWith(ipString.substring(0, ipString.indexOf('.')))) {
                        return false;
                    }
                }
            } else {
                // Server needs to be a public IP
                if (isPrivateIp(server.getLocalAddress())) {
                    return false;
                }
            }

        } else {
            // Connected through cellular
            if (isPrivateIp(server.getLocalAddress())) {
                return false;
            }
        }

        return true;
    }


    public static boolean isConnectedLAN(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);


        NetworkInfo mNetInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mNetInfo != null && mNetInfo.isConnected())
            return true;

        mNetInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

        if (mNetInfo != null && mNetInfo.isConnected())
            return true;

        return false;
    }


    public static boolean isPrivateIp(String ipAddress) {

        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }

        for (String ipStart : typicalLanIps) {
            if (ipAddress.contains(ipStart))
                return true;
        }

        return false;
    }
}
