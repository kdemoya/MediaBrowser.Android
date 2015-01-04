package com.mb.android.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.logging.FileLogger;
import com.mb.android.utils.Utils;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.livetv.SeriesTimerInfoDto;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Mark on 12/12/13.
 *
 * Adapter that's used to populate a ListView with programs that are scheduled to be recorded
 */
public class ScheduledSeriesRecordingsAdapter extends BaseAdapter {

    List<SeriesTimerInfoDto> mBaseItems;
    LayoutInflater li;
    SharedPreferences mSharedPreferences;
    String[] mDayNames;
    private boolean imageEnhancersEnabled;


    public ScheduledSeriesRecordingsAdapter(List<SeriesTimerInfoDto> listings) {

        mBaseItems = listings;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(MB3Application.getInstance());
        try {
            li = (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            imageEnhancersEnabled = PreferenceManager
                    .getDefaultSharedPreferences(MB3Application.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true);
        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("Error in adapter initialization", e);
        }

        DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
        mDayNames = symbols.getShortWeekdays();
    }


    public int getCount() {
        return mBaseItems.size();
    }


    @SuppressLint("SimpleDateFormat")
    public View getView(int position, View convertView, ViewGroup parent) {

        View view;
        view = li.inflate(R.layout.widget_scheduled_item, parent, false);

        TextView titleText = (TextView) view.findViewById(R.id.tvListingTitle);
        TextView episodeText = (TextView) view.findViewById(R.id.tvListingEpisodeTitle);
        TextView recordingTimeframeText = (TextView) view.findViewById(R.id.tvScheduledRecordingTime);
        ImageView recordingIcon = (ImageView) view.findViewById(R.id.ivListingRecordingStatus);
        NetworkImageView primaryImage = (NetworkImageView) view.findViewById(R.id.ivRecordingImage);

        SeriesTimerInfoDto program = mBaseItems.get(position);

        // Set the image
        if (program.getHasPrimaryImage()) {
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setMaxWidth(400);
            options.setEnableImageEnhancers(imageEnhancersEnabled);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(program.getId(), options);
            primaryImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        }

        // Set title text
        titleText.setText(program.getName());

        String recordingDetails = "";

        // Set episode title text
        if (program.getDayPattern() != null) {
            recordingDetails += program.getDayPattern().toString();
        } else if (program.getDays() != null && !program.getDays().isEmpty()) {

            String daysString = "";

            for (String day : program.getDays()) {
                if (!daysString.isEmpty()) {
                    daysString += ", ";
                }
                if (day.equals("Sunday")) {
                    daysString += mDayNames[1];
                } else if (day.equals("Monday")) {
                    daysString += mDayNames[2];
                } else if (day.equals("Tuesday")) {
                    daysString += mDayNames[3];
                } else if (day.equals("Wednesday")) {
                    daysString += mDayNames[4];
                } else if (day.equals("Thursday")) {
                    daysString += mDayNames[5];
                } else if (day.equals("Friday")) {
                    daysString += mDayNames[6];
                } else if (day.equals("Saturday")) {
                    daysString += mDayNames[7];
                }
            }

            recordingDetails += daysString;
        }

        recordingDetails += " - ";

        if (program.getRecordAnyTime()) {
            recordingDetails += "Any time";
        } else {
            DateFormat printFormat = new SimpleDateFormat("hh:mm a");

            Date startDate = Utils.convertToLocalDate(program.getStartDate());
            String formattedDate = printFormat.format(startDate);
            if (formattedDate != null && formattedDate.startsWith("0")) {
                formattedDate = formattedDate.replaceFirst("0", " ");
            }
            recordingDetails += formattedDate;
        }

        episodeText.setText(recordingDetails);
        episodeText.setVisibility(View.VISIBLE);

        if (program.getRecordAnyChannel()) {
            recordingTimeframeText.setText("Any channel");
        } else {
            recordingTimeframeText.setText(program.getChannelName() != null ? program.getChannelName() : "");
        }

        recordingIcon.setVisibility(View.GONE);

        return view;
    }


    public Object getItem(int position) {

        return mBaseItems.get(position);
    }


    public long getItemId(int position) {
        return 0;
    }
}

