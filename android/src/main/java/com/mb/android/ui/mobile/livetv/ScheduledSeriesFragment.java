package com.mb.android.ui.mobile.livetv;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
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
import com.mb.android.adapters.ScheduledSeriesRecordingsAdapter;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.livetv.SeriesTimerInfoDto;
import mediabrowser.model.livetv.SeriesTimerQuery;
import mediabrowser.model.results.SeriesTimerInfoDtoResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Mark on 2014-06-01.
 *
 * Fragment that shows all the series recordings the user has scheduled
 */
public class ScheduledSeriesFragment extends Fragment {

    private ListView mRecordings;
    private ProgressBar mActivityIndicator;
    private TextView mErrorText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_scheduled_recordings, container, false);
        mRecordings = (ListView) view.findViewById(R.id.lvScheduledRecordings);
        mActivityIndicator = (ProgressBar) view.findViewById(R.id.pbActivityIndicator);
        mErrorText = (TextView) view.findViewById(R.id.tvErrorText);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mActivityIndicator.setVisibility(View.VISIBLE);

        SeriesTimerQuery query = new SeriesTimerQuery();
        query.setSortOrder(SortOrder.Ascending);
        MB3Application.getInstance().API.GetLiveTvSeriesTimersAsync(query, new GetSeriesTimersResponse());
    }

    private class GetSeriesTimersResponse extends Response<SeriesTimerInfoDtoResult> {
        @Override
        public void onResponse(SeriesTimerInfoDtoResult result) {

            if (mActivityIndicator != null) {
                mActivityIndicator.setVisibility(View.GONE);
            }

            if (result == null) {
                mErrorText.setVisibility(View.VISIBLE);
                return;
            }

            final List<SeriesTimerInfoDto> timers = new ArrayList<>();
            timers.addAll(Arrays.asList(result.getItems()));

            if (timers.size() == 0) {
                mErrorText.setText(MB3Application.getInstance().getResources().getString(R.string.no_scheduled_recordings));
                mErrorText.setVisibility(View.VISIBLE);
            } else {
                mRecordings.setAdapter(new ScheduledSeriesRecordingsAdapter(timers));
                mRecordings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                        String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(timers.get(i));
                        Intent intent = new Intent(MB3Application.getInstance(), ProgramDetailsActivity.class);
                        intent.putExtra("timer", jsonData);

                        if (timers.get(i).getId() == null) {
                            Log.d("SSF", "Id is null");
                        } else {
                            Log.d("SSF", "Id = " + timers.get(i).getId());
                        }

                        startActivity(intent);
                    }
                });
            }
        }
    }
}
