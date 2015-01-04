package com.mb.android.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mb.android.R;
import mediabrowser.apiinteraction.ApiClient;
import com.mb.android.livetv.IListing;
import com.mb.android.livetv.ListingData;
import com.mb.android.livetv.ListingHeader;
import com.mb.android.logging.FileLogger;
import mediabrowser.model.livetv.ProgramInfoDto;
import com.mb.android.utils.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Adapter that's used to populate a ListView with programs that are airing on a given channel
 */
public class ChannelListingsAdapter extends BaseAdapter {

    Context mContext;
    List<IListing> mBaseItems;
    LayoutInflater li;
    ApiClient mApi;
    SharedPreferences mSharedPreferences;
    Date now = new Date();


    public ChannelListingsAdapter(Context context, List<IListing> listings, ApiClient apiClient) {

        mContext = context;
        mBaseItems = listings;
        mApi = apiClient;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        try {
            li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("Error in adapter initialization", e);
        }

    }

    public void refreshCurrentTime() {
        now = new Date();
        notifyDataSetChanged();
    }

    public int getCount() {
        return mBaseItems.size();
    }


    @SuppressLint("SimpleDateFormat")
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if (mBaseItems.get(position).isHeader()) {
            view = li.inflate(R.layout.widget_channel_listing_header, null);

            TextView dayText = (TextView) view.findViewById(R.id.tvHeaderDay);
            TextView dateText = (TextView) view.findViewById(R.id.tvHeaderDate);

            dayText.setText(((ListingHeader)mBaseItems.get(position)).day);
            dateText.setText(((ListingHeader)mBaseItems.get(position)).date);

        } else {
            view = li.inflate(R.layout.widget_channel_listing_item, null);

            TextView titleText = (TextView) view.findViewById(R.id.tvListingTitle);
            TextView startTimeText = (TextView) view.findViewById(R.id.tvListingStartTime);
            TextView newText = (TextView) view.findViewById(R.id.tvListingNew);
            TextView runtimeText = (TextView) view.findViewById(R.id.tvListingRuntime);
            RelativeLayout background = (RelativeLayout) view.findViewById(R.id.rlBackground);
            ImageView recordingIcon = (ImageView) view.findViewById(R.id.ivListingRecordingStatus);

            ProgramInfoDto program = ((ListingData) mBaseItems.get(position)).programInfoDto;

            // Set title text
            titleText.setText(program.getName());

            // Set start time text
            DateFormat printFormat = new SimpleDateFormat("hh:mm a");

            Date date = Utils.convertToLocalDate(program.getStartDate());
            String formattedDate = printFormat.format(date);
            if (formattedDate != null && formattedDate.startsWith("0")) {
                formattedDate = formattedDate.replaceFirst("0", " ");
            }
            startTimeText.setText(formattedDate);

            // Highlight the currently airing programs startTimeText

            boolean isCurrentlyAiring = false;

            if (date.before(now)) {
                if (mBaseItems.size() > position + 1 && !mBaseItems.get(position + 1).isHeader()) {
                    Date d = Utils.convertToLocalDate(((ListingData) mBaseItems.get(position + 1)).programInfoDto.getStartDate());

                    if (d.after(now)) {
                        isCurrentlyAiring = true;
                    }

                } else if (mBaseItems.size() > position + 2 && !mBaseItems.get(position + 2).isHeader()) {
                    Date d = Utils.convertToLocalDate(((ListingData) mBaseItems.get(position + 2)).programInfoDto.getStartDate());

                    if (d.after(now)) {
                        isCurrentlyAiring = true;
                    }
                }
            }

            if (isCurrentlyAiring) {
                startTimeText.setBackgroundColor(Color.parseColor("#008000"));
            } else {
                startTimeText.setBackgroundColor(Color.TRANSPARENT);
            }

            // set new text
            if (program.getIsPremiere()) {
                newText.setText("Premier");
                newText.setVisibility(View.VISIBLE);

            } else if (program.getIsSeries() && !program.getIsRepeat()) {
                newText.setText("New");
                newText.setVisibility(View.VISIBLE);
            } else {
                newText.setVisibility(View.GONE);
            }

            // set runtime text
            if (program.getRunTimeTicks() != null) {
                runtimeText.setText(Utils.TicksToMinutesString(program.getRunTimeTicks()));
            } else {
                runtimeText.setText("");
            }

            // set background
            if (program.getIsMovie()) {
                background.setBackgroundColor(Color.parseColor("#271A21"));
//            holder.underline.setBackgroundColor(Color.parseColor("#A43913"));
            } else if (program.getIsNews()) {
                background.setBackgroundColor(Color.parseColor("#211A32"));
//            holder.underline.setBackgroundColor(Color.parseColor("#523378"));
            } else if (program.getIsSports()) {
                background.setBackgroundColor(Color.parseColor("#0F2624"));
//            holder.underline.setBackgroundColor(Color.parseColor("#0A7C33"));
            } else if (program.getIsKids()) {
                background.setBackgroundColor(Color.parseColor("#092345"));
//            holder.underline.setBackgroundColor(Color.parseColor("#0B487D"));
            } else {
                background.setBackgroundColor(Color.TRANSPARENT);
//            holder.underline.setBackgroundColor(Color.TRANSPARENT);
            }

            // Show the recording overlay
            if (program.getSeriesTimerId() != null && !program.getSeriesTimerId().isEmpty()) {
                recordingIcon.setVisibility(View.VISIBLE);
                recordingIcon.setImageResource(R.drawable.record_series_icon);
            } else if (program.getTimerId() != null && !program.getTimerId().isEmpty()) {
                recordingIcon.setVisibility(View.VISIBLE);
                recordingIcon.setImageResource(R.drawable.record_icon);
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

