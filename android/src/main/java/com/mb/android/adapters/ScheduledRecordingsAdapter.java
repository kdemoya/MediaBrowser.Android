package com.mb.android.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
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
import com.mb.android.livetv.IListing;
import com.mb.android.livetv.ListingData;
import com.mb.android.livetv.ListingHeader;
import com.mb.android.logging.FileLogger;
import com.mb.android.utils.Utils;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.livetv.TimerInfoDto;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Adapter that's used to populate a ListView with programs that are scheduled to be recorded
 */
public class ScheduledRecordingsAdapter extends BaseAdapter {

    List<IListing> mBaseItems;
    LayoutInflater li;
    private boolean imageEnhancersEnabled;

    public ScheduledRecordingsAdapter(List<IListing> listings) {

        mBaseItems = listings;
        try {
            li = (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            imageEnhancersEnabled = PreferenceManager
                    .getDefaultSharedPreferences(MB3Application.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true);
        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("Error in adapter initialization", e);
        }
    }


    public int getCount() {
        return mBaseItems.size();
    }


    @SuppressLint("SimpleDateFormat")
    public View getView(int position, View convertView, ViewGroup parent) {

        View view;

        if (mBaseItems.get(position).isHeader()) {
            view = li.inflate(R.layout.widget_channel_listing_header, parent, false);

            TextView dayText = (TextView) view.findViewById(R.id.tvHeaderDay);
            TextView dateText = (TextView) view.findViewById(R.id.tvHeaderDate);

            dayText.setText(((ListingHeader)mBaseItems.get(position)).day);
            dateText.setText(((ListingHeader)mBaseItems.get(position)).date);

        } else {
            view = li.inflate(R.layout.widget_scheduled_item, parent, false);

            TextView titleText = (TextView) view.findViewById(R.id.tvListingTitle);
            TextView episodeText = (TextView) view.findViewById(R.id.tvListingEpisodeTitle);
            TextView recordingTimeframeText = (TextView) view.findViewById(R.id.tvScheduledRecordingTime);
            ImageView recordingIcon = (ImageView) view.findViewById(R.id.ivListingRecordingStatus);
            NetworkImageView primaryImage = (NetworkImageView) view.findViewById(R.id.ivRecordingImage);

            TimerInfoDto program = ((ListingData) mBaseItems.get(position)).timerInfoDto;

            primaryImage.setDefaultImageResId(R.drawable.tv);

            // Set the image
            if (program.getProgramInfo() != null && program.getProgramInfo().getHasPrimaryImage()) {
                ImageOptions options = new ImageOptions();
                options.setImageType(ImageType.Primary);
                options.setMaxWidth(400);
                options.setEnableImageEnhancers(imageEnhancersEnabled);

                String imageUrl = MB3Application.getInstance().API.GetImageUrl(program.getProgramInfo().getId(), options);
                primaryImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            } else {
                primaryImage.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
            }

            // Set title text
            titleText.setText(program.getName());

            // Set episode title text
            if (program.getProgramInfo() != null && program.getProgramInfo().getEpisodeTitle() != null && !program.getProgramInfo().getEpisodeTitle().isEmpty()) {
                episodeText.setText(program.getProgramInfo().getEpisodeTitle());
                episodeText.setVisibility(View.VISIBLE);
            } else {
                episodeText.setVisibility(View.GONE);
            }

            // Set start time text
            DateFormat printFormat = new SimpleDateFormat("hh:mm a");

            Date startDate = Utils.convertToLocalDate(program.getStartDate());
            String formattedDate = printFormat.format(startDate);
            if (formattedDate != null && formattedDate.startsWith("0")) {
                formattedDate = formattedDate.replaceFirst("0", " ");
            }

            Date endDate = Utils.convertToLocalDate(program.getEndDate());
            String fDate = printFormat.format(endDate);
            if (fDate != null && fDate.startsWith("0")) {
                fDate = fDate.replaceFirst("0", " ");
            }

            recordingTimeframeText.setText(formattedDate + " - " + fDate);

            // Show the recording overlay
            if (program.getSeriesTimerId() != null && !program.getSeriesTimerId().isEmpty()) {
                recordingIcon.setVisibility(View.VISIBLE);
                recordingIcon.setImageResource(R.drawable.record_series_icon);
            } else {
                recordingIcon.setVisibility(View.GONE);
            }
        }

        return view;
    }


    public Object getItem(int position) {

        return mBaseItems.get(position);
    }


    public long getItemId(int position) {
        return 0;
    }
}

