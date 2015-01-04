package com.mb.android.fragments.tv.music;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.mb.android.MB3Application;
import com.mb.android.MenuEntity;
import com.mb.android.R;
import com.mb.android.adapters.GenericAdapterPosters;
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
public class MusicAlbumsFragment extends Fragment {

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
        query.setSortBy(new String[]{ItemSortBy.SortName.toString()});
        query.setSortOrder(SortOrder.Ascending);
        query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.ParentId});
        query.setIncludeItemTypes(new String[]{"MusicAlbum"});
        query.setExcludeLocationTypes(new LocationType[]{LocationType.Virtual});

        MB3Application.getInstance().API.GetItemsAsync(query, getItemsResponse);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.tv_fragment_series_actors, container, false);

        return mView;
    }

    private Response<ItemsResult> getItemsResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {

            if (response != null && response.getItems() != null && response.getItems().length > 0) {

                ArrayList<BaseItemDto> albums = new ArrayList<>();
                albums.addAll(Arrays.asList(response.getItems()));
                GridView albumsGrid = (GridView) mView.findViewById(R.id.gvSeriesActors);
                albumsGrid.setNumColumns(6);
                albumsGrid.setAdapter(new GenericAdapterPosters(albums, albumsGrid.getNumColumns(), mView.getContext(), null));
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };
}
