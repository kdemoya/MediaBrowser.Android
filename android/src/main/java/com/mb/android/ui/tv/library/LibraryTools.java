package com.mb.android.ui.tv.library;

import com.jess.ui.TwoWayGridView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.utils.Utils;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.SeriesStatus;
import mediabrowser.model.extensions.StringHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Mark on 2014-11-28.
 */
public class LibraryTools {

    public static double calculateNormalizedAspectRatio(ArrayList<BaseItemDto> items) {
        if (items == null || items.size() == 0) {
            throw new IllegalArgumentException("items");
        }

        int count = 0;
        double combinedAspectRatio = 0d;
        for (BaseItemDto item : items) {

            if (item.getPrimaryImageAspectRatio() != null) {
                combinedAspectRatio += item.getPrimaryImageAspectRatio();
                count++;
            }
            if (count >= 5) {
                break;
            }
        }
        if (combinedAspectRatio > 0d) {
            return combinedAspectRatio / (double)count;
        } else {
            if ("episode".equalsIgnoreCase(items.get(0).getType())) {
                return 1.78;
            } else {
                return 0.66666666667;
            }
        }
    }

    public static int calculateGridHeightFromAspectRatio(double normalizedAspectRatio, TwoWayGridView gridView) {
        if (gridView == null) {
            throw new IllegalArgumentException("gridView");
        }

        int height = gridView.getHeight();
        if (normalizedAspectRatio < 0.9) {
            // Movie posters and other portrait content
            height = (int)((float)height * 1.55);
        } else if (normalizedAspectRatio < 1.1) {
            // Albums and other squarish content
            height = (int)((float)height * 1.1);
        } else {
            // backdrops, episode images and other landscape content
        }

        return height;
    }

    public static Integer getDefaultImageIdFromType(String type, double aspectRatio) {

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(type)) {
            return null;
        }

        Integer resourceId = null;

        switch (type.toLowerCase()) {

            case "musicalbum":
                resourceId = R.drawable.music_square_bg;
                break;
            case "musicartist":
                resourceId = R.drawable.default_artist;
                break;
            case "book":
                if (aspectRatio < 1.0d) {
                    resourceId = R.drawable.default_book_portrait;
                } else {
                    resourceId = R.drawable.default_book_landscape;
                }
                break;
            case "game":
                if (aspectRatio < 1.0d) {
                    resourceId = R.drawable.default_game_portrait;
                } else {
                    resourceId = R.drawable.default_game_landscape;
                }
                break;
            case "channel":
            case "channelfolderitem":
                if (aspectRatio < 1.0d) {
                    resourceId = R.drawable.default_channel_portrait;
                } else {
                    resourceId = R.drawable.default_channel_landscape;
                }
                break;
            case "movie":
            case "homemovie":
            default:
                if (aspectRatio < 1.0d) {
                    resourceId = R.drawable.default_video_portrait;
                } else {
                    resourceId = R.drawable.default_video_landscape;
                }
        }

        return resourceId;
    }

    public static String buildAiringInfoString(BaseItemDto item) {
        if ("series".equalsIgnoreCase(item.getType())) {
            return buildSeriesAiringInfoString(item);
        } else if ("episode".equalsIgnoreCase(item.getType())) {
            return buildEpisodeAiringInfoString(item);
        } else {
            return "";
        }
    }

    private static String buildSeriesAiringInfoString(BaseItemDto item) {
        String aInfo = "";

        if (item.getAirDays() != null && item.getAirDays().size() > 0) {
            String daysString = "";

            if (item.getAirDays().size() == 7) {
                daysString = "daily";
            } else {
                for (String day : item.getAirDays()) {
                    if (!daysString.isEmpty())
                        daysString += ", ";

                    daysString += day + "s";
                }
            }

            if (!daysString.isEmpty())
                aInfo += daysString;
        }

        if (item.getAirTime() != null && !item.getAirTime().isEmpty()) {
            aInfo += " " + MB3Application.getInstance().getResources().getString(R.string.at_string) + " ";
            aInfo += item.getAirTime();
        }

        if (item.getStudios() != null && item.getStudios().length > 0) {
            aInfo += " on ";
            aInfo += item.getStudios()[0].getName();
        }

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(aInfo)) {
            if (item.getStatus() != null && item.getStatus().equals(SeriesStatus.Ended)) {
                aInfo = MB3Application.getInstance().getResources().getString(R.string.aired_string) + " " + aInfo;
            } else {
                aInfo = MB3Application.getInstance().getResources().getString(R.string.airs_string) + " " + aInfo;
            }
        }

        return aInfo;
    }

    private static String buildEpisodeAiringInfoString(BaseItemDto item) {
        String aInfo = "";

        if (item.getPremiereDate() != null) {
            DateFormat outputFormat = new SimpleDateFormat("dd-MM-yyyy");

            Date premiereDate = Utils.convertToLocalDate(item.getPremiereDate());
            aInfo = outputFormat.format(premiereDate);

            if (premiereDate != null) {
                if (premiereDate.before(new Date())) {
                    aInfo = MB3Application.getInstance().getResources().getString(R.string.aired_string) + " " + aInfo;
                } else {
                    aInfo = MB3Application.getInstance().getResources().getString(R.string.airs_string) + " " + aInfo;
                }
            }
        }
        return aInfo;
    }

}
