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
import com.mb.android.adapters.LiveTvChannelsAdapter;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.LiveTvChannelQuery;
import mediabrowser.model.results.ChannelInfoDtoResult;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Mark on 2014-06-01.
 *
 * Show a listing of all the channels available
 */
public class ChannelsFragment extends Fragment {

    private GridView mChannels;
    private ProgressBar mActivityIndicator;
    private TextView mErrorText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tv_fragment_live_tv_recordings, container, false);
        mChannels = (GridView) view.findViewById(R.id.gvRecordedMedia);
        mActivityIndicator = (ProgressBar) view.findViewById(R.id.pbActivityIndicator);
        mErrorText = (TextView) view.findViewById(R.id.tvErrorText);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mActivityIndicator.setVisibility(View.VISIBLE);

        LiveTvChannelQuery query = new LiveTvChannelQuery();
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());

        MB3Application.getInstance().API.GetLiveTvChannelsAsync(query, new GetChannelsResponse());
    }

    private class GetChannelsResponse extends Response<ChannelInfoDtoResult> {
        @Override
        public void onResponse(ChannelInfoDtoResult result) {
            if (mActivityIndicator != null) {
                mActivityIndicator.setVisibility(View.GONE);
            }

            if (result == null) {
                mErrorText.setVisibility(View.VISIBLE);
                return;
            }

            final ArrayList<ChannelInfoDto> channelsList = new ArrayList<>();
            channelsList.addAll(Arrays.asList(result.getItems()));

            if (channelsList.size() == 0) {
                mErrorText.setText(MB3Application.getInstance().getResources().getString(R.string.channels_no_content_warning));
                mErrorText.setVisibility(View.VISIBLE);
            }

            mChannels.setAdapter(new LiveTvChannelsAdapter(channelsList,MB3Application.getInstance().API));
            mChannels.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(channelsList.get(i));
                    Intent intent = new Intent(MB3Application.getInstance(), ChannelListingsActivity.class);
                    intent.putExtra("ChannelInfoDto", jsonData);

                    startActivity(intent);
                }
            });
        }
        @Override
        public void onError(Exception ex) {

        }
    }
}
