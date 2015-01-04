package com.mb.android.ui.mobile.livetv;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.activities.mobile.ProgramDetailsActivity;
import com.mb.android.adapters.ScheduledRecordingsAdapter;
import com.mb.android.utils.Utils;
import mediabrowser.apiinteraction.Response;
import com.mb.android.livetv.IListing;
import com.mb.android.livetv.ListingData;
import com.mb.android.livetv.ListingHeader;
import mediabrowser.model.livetv.TimerInfoDto;
import mediabrowser.model.livetv.TimerQuery;
import mediabrowser.model.results.TimerInfoDtoResult;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Mark on 2014-06-01.
 *
 * Fragment that shows a list of media that the user has scheduled to record.
 */
public class ScheduledFragment extends Fragment {

    private ListView mScheduledItems;
    private ProgressBar mActivityIndicator;
    private TextView mErrorText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_scheduled_recordings, container, false);
        mScheduledItems = (ListView) view.findViewById(R.id.lvScheduledRecordings);
        mActivityIndicator = (ProgressBar) view.findViewById(R.id.pbActivityIndicator);
        mErrorText = (TextView) view.findViewById(R.id.tvErrorText);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mActivityIndicator.setVisibility(View.VISIBLE);
        MB3Application.getInstance().API.GetLiveTvTimersAsync(new TimerQuery(), new GetTimersResponse());
    }

    private class GetTimersResponse extends Response<TimerInfoDtoResult> {
        @Override
        public void onResponse(TimerInfoDtoResult result) {
            if (mActivityIndicator != null) {
                mActivityIndicator.setVisibility(View.GONE);
            }

            if (result == null) {
                mErrorText.setVisibility(View.VISIBLE);
                return;
            }

            if (result.getItems() == null || result.getItems().length == 0) {
                mErrorText.setText(MB3Application.getInstance().getResources().getString(R.string.no_scheduled_recordings));
                mErrorText.setVisibility(View.VISIBLE);
                if (mScheduledItems.getAdapter() != null) {
                    mScheduledItems.setAdapter(null);
                }
            } else {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                DateFormat dayFormat = new SimpleDateFormat("EEEE");
                String currentDate = "";

                final List<IListing> listings = new ArrayList<>();

                for (TimerInfoDto program : result.getItems()) {

                    Date date = Utils.convertToLocalDate(program.getStartDate());

                    String dateString = dateFormat.format(date);

                    if (!dateString.equals(currentDate)) {
                        currentDate = dateString;
                        ListingHeader header = new ListingHeader();
                        header.day = dayFormat.format(date);
                        header.date = dateString;
                        listings.add(header);
                    }

                    ListingData listing = new ListingData();
                    listing.timerInfoDto = program;
                    listings.add(listing);
                }

                mScheduledItems.setAdapter(new ScheduledRecordingsAdapter(listings));
                mScheduledItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                        if (listings.get(i) instanceof ListingData) {

                            String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(((ListingData) listings.get(i)).timerInfoDto);
                            Intent intent = new Intent(MB3Application.getInstance(), ProgramDetailsActivity.class);
                            intent.putExtra("timer", jsonData);

                            startActivity(intent);
                        }
                    }
                });
            }
        }
    }
}
