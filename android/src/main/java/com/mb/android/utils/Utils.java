package com.mb.android.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.http.HttpResponseCache;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.util.Log;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.ImageView;

import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.profiles.MbAndroidProfile;
import com.mb.network.Connectivity;
import mediabrowser.model.dlna.StreamBuilder;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dlna.VideoOptions;
import mediabrowser.model.dto.BaseItemDto;
import com.mb.android.logging.FileLogger;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.extensions.StringHelper;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Mark on 11/12/13.
 *
 * Class contains various helper methods
 */
public class Utils {

    public static String TicksToRuntimeString(long ticks) {

        long hours = ticks / 36000000000L;
        long remainder = ticks % 36000000000L;
        long mins = remainder / 600000000L;

        String runtime = "";

        if (hours > 0) {
            runtime = String.valueOf(hours) + "h ";
        }

        runtime += String.valueOf(mins) + "m";

        return runtime;
    }


    public static String TicksToMinutesString(long ticks) {

        long mins = ticks / 600000000L;

        String runtime = "";

        runtime += String.valueOf(mins) + " min";

        return runtime;
    }


    /**
     * This will display a runtime in numerical format displaying as few leading 0's as possible
     *
     * Output Example:
     * 1:03:00
     *    8:12
     *
     * @param milliseconds The milliseconds to display
     * @return The milliseconds formatted to a standard time value
     */
    public static String PlaybackRuntimeFromMilliseconds(long milliseconds) {

        long hours = 0;
        long minutes = 0;
        long seconds = 0;

        if (milliseconds >= (1000 * 60 * 60)) {
            hours = milliseconds / (1000 * 60 * 60);
        }
        if (milliseconds >= (1000 * 60)) {
            minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        }
        if (milliseconds >= 1000) {
            seconds = ((milliseconds % (1000 * 60 * 60)) % (1000 * 60)) / 1000;
        }

        String runtime = "";

        if (hours > 0) {
            runtime = String.valueOf(hours) + ":";
        }
        if (minutes > 0) {
            if (minutes < 10 && hours > 0) {
                runtime += "0";
            }
            runtime += String.valueOf(minutes) + ":";
        } else {
            if (hours > 0) {
                runtime += "00:";
            }
        }

        if (seconds > 0) {
            if (seconds < 10) {
                runtime += "0";
            }
            runtime += String.valueOf(seconds);
            if (hours < 1 && minutes < 1) {
                runtime = "0:" + runtime;
            }
        } else {
            if (hours < 1 && minutes < 1) {
                runtime += "0:00";
            } else {
                runtime += "00";
            }
        }

        return runtime;
    }


    public static String getFriendlyTimeString(long delta) {

        long SECOND = 1000;
        long MINUTE = 60 * SECOND;
        long HOUR = 60 * MINUTE;
        long DAY = 24 * HOUR;
        long MONTH = 30 * DAY;

        if (delta < 0) {
            return "not yet";
        }
        if (delta < 2 * MINUTE) {
            return "a minute ago";
        }
        if (delta < 45 * MINUTE) {
            return delta / MINUTE + " minutes ago";
        }
        if (delta < 90 * MINUTE) {
            return "an hour ago";
        }
        if (delta < 24 * HOUR) {
            return delta / HOUR + " hours ago";
        }
        if (delta < 48 * HOUR) {
            return "yesterday";
        }
        if (delta < 30 * DAY) {
            return delta / DAY + " days ago";
        }
        if (delta < 12 * MONTH) {
            int months = (int) (delta / MONTH);
            return months <= 1 ? "one month ago" : months + " months ago";
        } else {
            int years = (int) (delta / (MONTH * 12));
            return years <= 1 ? "one year ago" : years + " years ago";
        }
    }

    public static String convertToLocalDateFormat(Date date) {

        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(MB3Application.getInstance());
        return dateFormat.format(date);
    }



    public static String TotalMemory(Context context) {

        ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        long totalMemory = memInfo.availMem;

        totalMemory = (totalMemory / 1024) / 1024;

        return String.valueOf(totalMemory) + " MB";
    }

    public static String MaxApplicationMemory() {
        long maxmemory = Runtime.getRuntime().maxMemory();
        maxmemory = (maxmemory / 1024) / 1024;

        return String.valueOf(maxmemory) + " MB";
    }

    public static void LogMemoryUsage(Context context) {

        long maxmemory = Runtime.getRuntime().maxMemory();
        long allocated = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        allocated = (allocated / 1024) / 1024;
        maxmemory = (maxmemory / 1024) / 1024;

        String usageInfo = "Memory Usage: ";
        usageInfo += String.valueOf(allocated) + " MB";

        usageInfo += " Maximum: " + String.valueOf(maxmemory) + " MB";


        HttpResponseCache theCache = HttpResponseCache.getInstalled();

        if (theCache != null) {
            Long cachesize = theCache.size();

            if (cachesize != null) {
                usageInfo += " Cache Size: ";
                if (cachesize >= 1024) {
                    cachesize = cachesize / 1024;

                    if (cachesize >= 1024) {
                        usageInfo += String.valueOf(cachesize / 1024) + " MB";
                    } else {
                        usageInfo += String.valueOf(cachesize) + " KB";
                    }
                } else {
                    usageInfo += String.valueOf(cachesize) + " B";
                }

                usageInfo += " Maximum: " + String.valueOf(HttpResponseCache.getInstalled().maxSize() / 1024 / 1024) + " MB";
            }
        }

        if (usageInfo.length() > 0) {
            FileLogger.getFileLogger().Info(usageInfo);
        }

    }


    public static void ShowStarRating(Float rating, ImageView imageView) {

        if (rating == null || imageView == null)
            return;

        if (rating >= 9.5)
            imageView.setImageResource(R.drawable.star10);
        else if (rating >= 8.5)
            imageView.setImageResource(R.drawable.star9);
        else if (rating >= 7.5)
            imageView.setImageResource(R.drawable.star8);
        else if (rating >= 6.5)
            imageView.setImageResource(R.drawable.star7);
        else if (rating >= 5.5)
            imageView.setImageResource(R.drawable.star6);
        else if (rating >= 4.5)
            imageView.setImageResource(R.drawable.star5);
        else if (rating >= 3.5)
            imageView.setImageResource(R.drawable.star4);
        else if (rating >= 2.5)
            imageView.setImageResource(R.drawable.star3);
        else if (rating >= 1.5)
            imageView.setImageResource(R.drawable.star2);
        else if (rating >= 0.5)
            imageView.setImageResource(R.drawable.star1);
        else
            imageView.setImageResource(R.drawable.star0);

    }




    public static String buildSubtitleDisplayString(MediaStream stream) {

        String desc;

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(stream.getLanguage())) {
            Locale local = new Locale(stream.getLanguage());
            desc = local.getDisplayLanguage();
        } else {
            desc = MB3Application.getInstance().getResources().getString(R.string.unknown_language);
        }

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(stream.getCodec())) {
            desc += " (" + stream.getCodec().toUpperCase() + ")";
        }

        return desc;
    }

    public static String buildAudioDisplayString(MediaStream stream) {

        String desc = "";

        if (stream.getCodec() != null && stream.getChannelLayout() != null) {
            desc = getFullLanguageName(stream.getLanguage());
            desc += " (" + (stream.getCodec().equalsIgnoreCase("dca") ? stream.getProfile().toUpperCase() : stream.getCodec().toUpperCase());
            desc += " " + stream.getChannelLayout() + ")";
        }

        return desc;
    }

    public static String getFullLanguageName(String shortLanguage) {
        if (shortLanguage != null) {
            Locale local = new Locale(shortLanguage);
            return local.getDisplayLanguage(local);
        } else {
            return "Unknown";
        }
    }

    public static boolean isAddressValid(EditText address) {

        Editable editable = address.getText();

        if (editable == null) return false;

        String addressString = editable.toString();

        int colonCount = 0;

        for (int i = 0; i < addressString.length(); i++) {
            if (addressString.charAt(i) == ':') {
                colonCount++;
            }
        }

        // Port is in the address. We don't want that
        if (colonCount == 2) return false;

        // Port is in the address. We don't want that
        if (colonCount == 1 && !addressString.startsWith("http")) return false;

        // Address appears valid
        return true;
    }

    public static boolean isPortValid(EditText port) {

        Editable editable = port.getText();

        if (editable == null) return false;

        String portString = editable.toString();

        try {
            int portInt = Integer.valueOf(portString);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Generate the Video URL to be requested from MB Server
     *
     * @return A String containing the formed URL.
     */
    public static String buildPlaybackUrl(BaseItemDto item,
                                    Long startPositionTicks,
                                    String mediaSourceId,
                                    Integer audioStreamIndex,
                                    Integer subtitleStreamIndex) {


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MB3Application.getInstance());
        String bitrate;

        if (Connectivity.isConnectedLAN(MB3Application.getInstance())) {
            bitrate = prefs.getString("pref_local_bitrate", "1800000");
        } else {
            bitrate = prefs.getString("pref_cellular_bitrate", "450000");
        }

        boolean hlsEnabled = prefs.getBoolean("pref_enable_hls", true);
        boolean h264StrictModeEnabled = prefs.getBoolean("pref_h264_strict", true);

        FileLogger.getFileLogger().Info("Create VideoOptions");
        VideoOptions options = new VideoOptions();
        options.setItemId(item.getId());
        options.setMediaSources(item.getMediaSources());
        options.setProfile(new MbAndroidProfile(hlsEnabled, false)
        );
        options.setDeviceId(Settings.Secure.getString(MB3Application.getInstance().getContentResolver(), Settings.Secure.ANDROID_ID));
        options.setMaxBitrate(Integer.valueOf(bitrate));

        if (audioStreamIndex != null) {
            options.setAudioStreamIndex(audioStreamIndex);
            options.setMediaSourceId(mediaSourceId);
        }
        if (subtitleStreamIndex != null) {
            options.setSubtitleStreamIndex(subtitleStreamIndex);
            options.setMediaSourceId(mediaSourceId);
        }

        FileLogger.getFileLogger().Info("Create StreamInfo");
        StreamInfo mStreamInfo;
        if (item.getType() != null && item.getType().equalsIgnoreCase("audio")) {
            mStreamInfo = new StreamBuilder().BuildAudioItem(options);
        } else {
            mStreamInfo = new StreamBuilder().BuildVideoItem(options);
        }

        if (mStreamInfo == null) {
            FileLogger.getFileLogger().Info("streamInfo is null");
            return "";
        }

        mStreamInfo.setMaxWidth(1920);
        mStreamInfo.setMaxHeight(1080);
        mStreamInfo.setMaxFramerate(30.0f);

        if (mStreamInfo.getProtocol() == null || !mStreamInfo.getProtocol().equalsIgnoreCase("hls")) {
            mStreamInfo.setStartPositionTicks(startPositionTicks);
        }

        return mStreamInfo.ToUrl(MB3Application.getInstance().API.getApiUrl());
    }

    public static Date convertToLocalDate(Date utcDate) {

        TimeZone timeZone = TimeZone.getDefault();
        Date convertedDate = new Date( utcDate.getTime() + timeZone.getRawOffset() );

        if ( timeZone.inDaylightTime(convertedDate) ) {
            Date dstDate = new Date( convertedDate.getTime() + timeZone.getDSTSavings() );

            if (timeZone.inDaylightTime( dstDate )) {
                convertedDate = dstDate;
            }
        }

        return convertedDate;
    }

    public static boolean insertIntoDataset(BaseItemDto item, ArrayList<BaseItemDto> dataset) {
        if (item == null) {
            return false;
        }
        if (dataset == null) {
            return false;
        }

        Integer index = null;

        for (int i = 0; i < dataset.size(); i++) {
            if (item.getId().equalsIgnoreCase(dataset.get(i).getId())) {
                index = i;
                break;
            }
        }

        // SUCCESS
        if (index != null) {
            dataset.set(index,item);
            return true;
        }

        return false;
    }

    /**
     * Returns a formatted string that shows the episode indexes in a readable manor.
     *
     * Output Examples:
     *  Season 1, Episode 12
     *  Season 3, Episodes 18 - 20
     *
     * @param item The Episode to parse
     *
     * @return The formatted String
     */
    public static String getLongEpisodeIndexString(BaseItemDto item) {
        String title = "";
        if (item == null) {
            return title;
        }
        try {
            if (item.getIndexNumber() != null)
                title += item.getIndexNumber().toString();
            if (item.getIndexNumberEnd() != null && !item.getIndexNumberEnd().equals(item.getIndexNumber())) {
                title += " - " + item.getIndexNumberEnd();
                title = "Season " + String.valueOf(item.getParentIndexNumber()) + ", Episodes " + title;
            } else {
                title = "Season " + String.valueOf(item.getParentIndexNumber()) + ", Episode " + title;
            }
        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("PopulateTvInfo - ", e);
        }
        return title;
    }

    /**
     * Returns a formatted string that shows the episode indexes in a readable manor.
     *
     * Output Examples:
     *  01:12
     *  03:18-20
     *
     * @param item The Episode to parse
     *
     * @return The formatted String
     */
    public static String getShortEpisodeIndexString(BaseItemDto item) {
        String title = "";
        if (item == null) {
            return title;
        }
        String episodeIndex = "";
        String episodeEndIndex = "";
        String parentIndex = "";

        try {
            if (item.getIndexNumber() != null) {
                episodeIndex = item.getIndexNumber().toString();
                if (episodeIndex.length() == 1) {
                    episodeIndex = "0" + episodeIndex;
                }
            }
            if (item.getIndexNumberEnd() != null && !item.getIndexNumberEnd().equals(item.getIndexNumber())) {
                episodeEndIndex = item.getIndexNumberEnd().toString();
                if (episodeEndIndex.length() == 1) {
                    episodeEndIndex = 0 + episodeEndIndex;
                }
            }
            if (item.getParentIndexNumber() != null) {
                parentIndex = item.getParentIndexNumber().toString();
                if (parentIndex.length() == 1) {
                    parentIndex = 0 + parentIndex;
                }
            }
            title = "s" + parentIndex + "e" + episodeIndex;
            if (episodeEndIndex.length() > 0) {
                title += "-" + episodeEndIndex;
            }
        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("PopulateTvInfo - ", e);
        }
        return title;
    }

    public static int convertDpToPixels(float dp){
        Resources resources = MB3Application.getInstance().getResources();
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                resources.getDisplayMetrics()
        );
    }
}
