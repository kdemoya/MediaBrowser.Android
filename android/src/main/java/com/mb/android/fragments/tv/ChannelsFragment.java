package com.mb.android.fragments.tv;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.mb.android.MB3Application;
import com.mb.android.R;
import mediabrowser.apiinteraction.Response;
import com.mb.android.adapters.GenericAdapterPosters;
import mediabrowser.model.channels.ChannelItemQuery;
import mediabrowser.model.channels.ChannelQuery;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemsResult;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment used to show a Grid of Channel entities.
 */
public class ChannelsFragment extends Fragment {

    public String TAG = "ChannelsFragment";
    private BaseItemDto mChannelItemDto;
    private BaseItemDto mChannelDto;
    private List<BaseItemDto> mItems;
    private View mView;

    /**
     * Class Constructor
     */
    public ChannelsFragment() {}

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Bundle args = getArguments();

        if (args != null) {
            mChannelDto = (BaseItemDto) args.getSerializable("Channel");
            mChannelItemDto = (BaseItemDto) args.getSerializable("ChannelItem");
        }

        if (mChannelDto == null) {
            // This means we're displaying the root channels rather than the contents of a channel
            ChannelQuery query = new ChannelQuery();
            query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
            MB3Application.getInstance().API.GetChannels(query, new GetChannelsResponse());
        } else {
            // We're drilling down into a channel.
            ChannelItemQuery query = new ChannelItemQuery();
            query.setChannelId(mChannelDto.getId());
//            query.setFolderId(mChannelItemDto != null && mChannelItemDto.getIsFolder() ? mChannelItemDto.getId() : null);
            query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
            query.setSortOrder(SortOrder.Ascending);
            MB3Application.getInstance().API.GetChannelItems(query, new GetChannelsResponse());
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.tv_fragment_series_actors, container, false);

        return mView;
    }

    //**********************************************************************************************
    // Callback Classes
    //**********************************************************************************************

    private class GetChannelsResponse extends Response<ItemsResult> {
        @Override
        public void onResponse(ItemsResult itemsResult) {

            if (itemsResult == null) return;

            mItems = Arrays.asList(itemsResult.getItems());
            GridView channelsGrid = (GridView) mView.findViewById(R.id.gvSeriesActors);
            channelsGrid.setNumColumns(4);
            channelsGrid.setAdapter(new GenericAdapterPosters(mItems, channelsGrid.getNumColumns(), mView.getContext(), null));
            channelsGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    Toast.makeText(getActivity(), "Item Type = " + mItems.get(i).getType(), Toast.LENGTH_LONG).show();
                    Toast.makeText(getActivity(), "Media Type = " + mItems.get(i).getMediaType(), Toast.LENGTH_LONG).show();
//                    CoreActivity coreActivity = (CoreActivity) getActivity();
//
//                    if (coreActivity == null) return;
//
//                    if (mChannelDto == null) {
//                        coreActivity.Navigate(mItems.get(i), (BaseItemDto)null);
//                    } else if (mItems.get(i).getIsFolder()) {
//                        coreActivity.Navigate(mChannelDto, mItems.get(i));
//                    } else {
//                        coreActivity.Navigate(mItems.get(i), i);
//                    }
                }
            });
        }
    }
}
