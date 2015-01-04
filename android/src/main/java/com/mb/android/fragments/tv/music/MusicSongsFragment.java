package com.mb.android.fragments.tv.music;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.mb.android.MB3Application;
import com.mb.android.MenuEntity;
import com.mb.android.R;
import com.mb.android.adapters.TvSongsAdapter;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Mark on 2014-05-20.
 */
public class MusicSongsFragment extends Fragment {

    private MenuEntity mMenuEntity;
    private View mView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mMenuEntity = (MenuEntity) getArguments().getSerializable("MenuEntity");

        ItemQuery query = new ItemQuery();
        query.setParentId(mMenuEntity.Id);
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setRecursive(true);
        query.setSortBy(new String[]{ItemSortBy.Name.toString()});
        query.setSortOrder(SortOrder.Ascending);
        query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.ParentId});
        query.setIncludeItemTypes(new String[]{"Audio"});
        query.setExcludeLocationTypes(new LocationType[]{LocationType.Virtual});

        MB3Application.getInstance().API.GetItemsAsync(query, getItemsResponse);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.tv_fragment_songs, container, false);

        return mView;
    }

    private Response<ItemsResult> getItemsResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {

            if (response != null && response.getItems() != null && response.getItems().length > 0) {

                ArrayList<BaseItemDto> songs = new ArrayList<>();
                songs.addAll(Arrays.asList(response.getItems()));
                Log.d("Song Count", String.valueOf(songs.size()));
                final ListView songsGrid = (ListView) mView.findViewById(R.id.lvSongList);
                songsGrid.setAdapter(new TvSongsAdapter(songs));
                songsGrid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                        ((TvSongsAdapter)songsGrid.getAdapter()).setSelectedIndex(i);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };
}
