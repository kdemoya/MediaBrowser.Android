package com.mb.android.ui.mobile.musicartist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.mb.android.MB3Application;
import com.mb.android.R;
import mediabrowser.apiinteraction.Response;
import com.mb.android.ui.mobile.album.MusicAlbumActivity;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.entities.SortOrder;
import com.mb.android.logging.FileLogger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment that shows the albums in a users library that are released by the current artist
 */
public class AlbumsFragment extends Fragment {

    private View mView;
    private String artistId;
    private List<BaseItemDto> mAlbums;
    private ArtistActivity mArtistActivity;

    /**
     * Class Constructor
     */
    public AlbumsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_artist_albums, container, false);

        Bundle args = getArguments();

        artistId = args.getString("ArtistId");

        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();

        ItemQuery query = new ItemQuery();
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setParentId(artistId);
        query.setSortBy(new String[]{ItemSortBy.ProductionYear});
        query.setSortOrder(SortOrder.Descending);
        query.setIncludeItemTypes(new String[]{"MusicAlbum"});
        query.setRecursive(true);
        query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.Studios, ItemFields.Genres});

        MB3Application.getInstance().API.GetItemsAsync(query, getItemsResponse);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity != null) {
            try {
                mArtistActivity = (ArtistActivity) activity;
            } catch (ClassCastException e) {
                Log.d("ServerSelectionFragment", "onAttach: Exception casting activity");
            }
        }
    }


    private Response<ItemsResult> getItemsResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {
            Log.i("GetAlbumsCallback", "Setup Objects");

            if (response != null && response.getItems() != null) {
                mAlbums = Arrays.asList(response.getItems());

                if (mAlbums.size() == 0) {
                    Log.i("GetAlbumsCallback", "mAlbums is null or empty");
                    FileLogger.getFileLogger().Error("mAlbums is null or empty");
                    return;
                }

                DisplayMetrics metrics = new DisplayMetrics();
                mArtistActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

                GridView albumsList = (GridView) mView.findViewById(R.id.gvArtistAlbumList);
                albumsList.setAdapter(new AlbumAdapter(mArtistActivity, mAlbums, MB3Application.getInstance().API, (int) (125 * metrics.density), (int) (125 * metrics.density)));
                albumsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Intent intent = new Intent(mArtistActivity, MusicAlbumActivity.class);
                        intent.putExtra("AlbumId", mAlbums.get(i).getId());

                        startActivity(intent);
                    }
                });

                // For TV's. Bring the focus off the home icon and into the content
                albumsList.requestFocus();
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };
}
