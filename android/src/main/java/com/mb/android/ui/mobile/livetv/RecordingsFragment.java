package com.mb.android.ui.mobile.livetv;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.activities.mobile.ProgramDetailsActivity;
import com.mb.android.adapters.RecordingsAdapterBackdrops;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.livetv.RecordingQuery;
import mediabrowser.model.results.RecordingInfoDtoResult;

import java.util.Arrays;

/**
 * Created by Mark on 2014-06-01.
 */
public class RecordingsFragment extends Fragment {

    private GridView mRecordings;
    private ProgressBar mActivityIndicator;
    private TextView mErrorText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tv_fragment_live_tv_recordings, container, false);
        mRecordings = (GridView) view.findViewById(R.id.gvRecordedMedia);
        mActivityIndicator = (ProgressBar) view.findViewById(R.id.pbActivityIndicator);
        mErrorText = (TextView) view.findViewById(R.id.tvErrorText);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mActivityIndicator.setVisibility(View.VISIBLE);
        RecordingQuery query = new RecordingQuery();
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        MB3Application.getInstance().API.GetLiveTvRecordingsAsync(query, new GetRecordingsResponse());
    }

    private class GetRecordingsResponse extends Response<RecordingInfoDtoResult> {
        @Override
        public void onResponse(final RecordingInfoDtoResult result) {

            if (mActivityIndicator != null) {
                mActivityIndicator.setVisibility(View.GONE);
            }

            if (result == null) {
                mErrorText.setVisibility(View.VISIBLE);
                return;
            }

            if (result.getItems() == null || result.getItems().length == 0) {
                mErrorText.setText(MB3Application.getInstance().getResources().getString(R.string.channels_no_content_warning));
                mErrorText.setVisibility(View.VISIBLE);
            } else {
                mRecordings.setAdapter(new RecordingsAdapterBackdrops(Arrays.asList(result.getItems())));
                mRecordings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(result.getItems()[i]);
                        Intent intent = new Intent(MB3Application.getInstance(), ProgramDetailsActivity.class);
                        intent.putExtra("recording", jsonData);
                        startActivity(intent);
                    }
                });
            }
        }
    }
}
