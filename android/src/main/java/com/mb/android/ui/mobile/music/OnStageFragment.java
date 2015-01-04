package com.mb.android.ui.mobile.music;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.Playlist;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.activities.mobile.RemoteControlActivity;
import mediabrowser.apiinteraction.Response;
import com.mb.android.listeners.OverflowOnItemLongClickListener;
import com.mb.android.player.AudioService;
import com.mb.android.ui.mobile.playback.AudioPlaybackActivity;
import com.mb.android.adapters.GenericAdapterPosters;
import com.mb.android.listeners.AlbumOnItemClickListener;
import com.mb.android.listeners.SongOnItemClickListener;
import com.mb.android.logging.FileLogger;
import com.mb.android.ui.mobile.album.SongAdapter;
import com.mb.android.widget.nestedlistview.NestedListView;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.SimilarItemsQuery;
import mediabrowser.model.session.PlayCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Mark on 2014-07-15.
 *
 * Fragment that displays a selection of music based on recent events
 */
public class OnStageFragment extends Fragment {

    protected NetworkImageView mBackdropImage1;
    protected NetworkImageView mBackdropImage2;
    protected ViewSwitcher mBackdropSwitcher;
    private GridView newMusicGrid;
    private NestedListView mostPlayedListView;
    private TextView tvInstantMixArtists;
    private int mBackdropIndex = 0;
    private List<String> mBackdropUrls;
    private BaseItemDto[] mInstantMixItems;
    private String mInstantMixArtists = "";
    private MusicActivity mMusicActivity;

    private Runnable CycleBackdrops = new Runnable() {
        @Override
        public void run() {

            if (mBackdropIndex >= mBackdropUrls.size())
                mBackdropIndex = 0;

            setInstantMixImage(mBackdropUrls.get(mBackdropIndex));
            mBackdropIndex += 1;
            mBackdropSwitcher.postDelayed(this, 8000);
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity != null) {
            try {
                mMusicActivity = (MusicActivity) activity;
            } catch (ClassCastException e) {
                Log.d("ServerSelectionFragment", "onAttach: Exception casting activity");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {

        View view = layoutInflater.inflate(R.layout.fragment_on_stage, container, false);

        view.findViewById(R.id.ivPlayPause).setOnClickListener(onInstantMixPlayClick);
        mBackdropSwitcher = (ViewSwitcher) view.findViewById(R.id.vsBackdropImages);
        mBackdropImage1 = (NetworkImageView) view.findViewById(R.id.ivBackdropImage1);
        mBackdropImage2 = (NetworkImageView) view.findViewById(R.id.ivBackdropImage2);
        newMusicGrid = (GridView) view.findViewById(R.id.gvNewMusic);
        mostPlayedListView = (NestedListView) view.findViewById(R.id.lvMostPlayedMusic);
        tvInstantMixArtists = (TextView) view.findViewById(R.id.tvInstantMixArtists);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        requestOnStageContent();
        if (mBackdropUrls != null && mBackdropUrls.size() > 0) {
            enableInstantMixImage();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        disableInstantMixImage();
        if (newMusicGrid != null) {
            GenericAdapterPosters newMusicAdapter = (GenericAdapterPosters) newMusicGrid.getAdapter();
            if (newMusicAdapter != null) {
                newMusicAdapter.clearDataSet();
                newMusicAdapter = null;
            }
        }

        if (mostPlayedListView != null) {
            SongAdapter songAdapter = (SongAdapter) mostPlayedListView.getAdapter();
            if (songAdapter != null) {
                songAdapter.clearDataset();
                songAdapter = null;
            }
        }
    }

    @Override
    public void onDestroy() {
        disableInstantMixImage();
        mMusicActivity = null;

        super.onDestroy();
    }

    private void requestOnStageContent() {

        ItemQuery query = new ItemQuery();
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setLimit(1);
        query.setRecursive(true);
        query.setSortBy(new String[]{ItemSortBy.DatePlayed.toString()});
        query.setSortOrder(SortOrder.Descending);
        query.setFilters(new ItemFilter[]{ItemFilter.IsPlayed});
        query.setFields(new ItemFields[]{ItemFields.ParentId});
        query.setIncludeItemTypes(new String[]{"Audio"});

        MB3Application.getInstance().API.GetItemsAsync(query, getLastSongResponse);

        ItemQuery newMusicQuery = new ItemQuery();
        newMusicQuery.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        newMusicQuery.setLimit(3);
        newMusicQuery.setRecursive(true);
        newMusicQuery.setSortBy(new String[]{ItemSortBy.DateCreated.toString()});
        newMusicQuery.setSortOrder(SortOrder.Descending);
        newMusicQuery.setFields(new ItemFields[]{ItemFields.ParentId, ItemFields.PrimaryImageAspectRatio});
        newMusicQuery.setIncludeItemTypes(new String[]{"MusicAlbum"});

        MB3Application.getInstance().API.GetItemsAsync(newMusicQuery, getNewMusicResponse);

        ItemQuery mostPlayedQuery = new ItemQuery();
        mostPlayedQuery.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        mostPlayedQuery.setLimit(10);
        mostPlayedQuery.setRecursive(true);
        mostPlayedQuery.setSortBy(new String[]{ItemSortBy.PlayCount.toString()});
        mostPlayedQuery.setSortOrder(SortOrder.Descending);
        mostPlayedQuery.setFilters(new ItemFilter[]{ItemFilter.IsPlayed});
        mostPlayedQuery.setFields(new ItemFields[]{ItemFields.ParentId});
        mostPlayedQuery.setIncludeItemTypes(new String[]{"Audio"});

        MB3Application.getInstance().API.GetItemsAsync(mostPlayedQuery, getMostPlayedResponse);

    }

    //**********************************************************************************************
    // Instant-Mix methods
    //**********************************************************************************************

    private Response<ItemsResult> getLastSongResponse = new Response<ItemsResult>() {

        private boolean isRandomSongQuery = false;

        @Override
        public void onResponse(ItemsResult response) {

            if (!isRandomSongQuery && (response == null || response.getItems() == null || response.getItems().length < 1)) {

                ItemQuery query = new ItemQuery();
                query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
                query.setLimit(1);
                query.setRecursive(true);
                query.setSortBy(new String[]{ItemSortBy.Random.toString()});
                query.setSortOrder(SortOrder.Descending);
                query.setFields(new ItemFields[]{ItemFields.ParentId});
                query.setIncludeItemTypes(new String[]{"Audio"});

                isRandomSongQuery = true;
                MB3Application.getInstance().API.GetItemsAsync(query, this);

            } else if (response != null && response.getItems() != null && response.getItems().length > 0) {

                SimilarItemsQuery query = new SimilarItemsQuery();
                query.setId(response.getItems()[0].getId());
                query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
                query.setLimit(50);

                MB3Application.getInstance().API.GetInstantMixFromSongAsync(query, getInstantMixResponse);
            } else {
                // Couldn't get any music from the server.
                FileLogger.getFileLogger().Error("OnStage: Could not retrieve song for instant mix");
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private Response<ItemsResult> getInstantMixResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {

            if (response == null || response.getItems() == null) return;

            mInstantMixItems = response.getItems();

            int artistCount = 0;
            int backdropCount = 0;

            for (BaseItemDto song : mInstantMixItems) {

                if (song.getArtists() != null && song.getArtists().size() != 0 && !mInstantMixArtists.contains(song.getArtists().get(0))) {
                    if (artistCount < 5) {
                        if (!mInstantMixArtists.isEmpty()) {
                            mInstantMixArtists += ", ";
                        }
                        mInstantMixArtists += song.getArtists().get(0);
                        artistCount++;
                    }

                    if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(song.getParentBackdropItemId())) {
                        ImageOptions options = new ImageOptions();
                        options.setImageType(ImageType.Backdrop);
                        options.setImageIndex(0);

                        String imageUrl = MB3Application.getInstance().API.GetImageUrl(song.getParentBackdropItemId(), options);

                        if (mBackdropUrls == null)
                            mBackdropUrls = new ArrayList<>();

                        mBackdropUrls.add(imageUrl);
                        backdropCount++;
                    }
                }

                if (artistCount >= 5 && backdropCount >= 5) break;
            }

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mInstantMixArtists)) {
                tvInstantMixArtists.setText(mInstantMixArtists);
            }
            if (mBackdropUrls != null && mBackdropUrls.size() > 0) {
                setInstantMixImages(mBackdropUrls);
                enableInstantMixImage();
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private Response<ItemsResult> getNewMusicResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {

            if (response == null || response.getItems() == null) return;

            List<BaseItemDto> newMusic = Arrays.asList(response.getItems());

            newMusicGrid.setAdapter(new GenericAdapterPosters(newMusic, 3, mMusicActivity, null));
            newMusicGrid.setOnItemClickListener(new AlbumOnItemClickListener());
            newMusicGrid.setOnItemLongClickListener(new OverflowOnItemLongClickListener());
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private Response<ItemsResult> getMostPlayedResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {

            if (response == null || response.getItems() == null) return;

            mostPlayedListView.setAdapter(new SongAdapter(Arrays.asList(response.getItems()), mMusicActivity));
            mostPlayedListView.setOnItemClickListener(new SongOnItemClickListener());
            mostPlayedListView.setOnItemLongClickListener(new OverflowOnItemLongClickListener());
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private View.OnClickListener onInstantMixPlayClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (mInstantMixItems == null || mInstantMixItems.length == 0) return;

            MB3Application.getInstance().PlayerQueue = new Playlist();

            AudioService.PlayerState currentState = MB3Application.getAudioService().getPlayerState();
            if (currentState.equals(AudioService.PlayerState.PLAYING) || currentState.equals(AudioService.PlayerState.PAUSED)) {
                MB3Application.getAudioService().stopMedia();
            }

            if (MB3Application.getCastManager(MB3Application.getInstance()).isConnected()) {
                MB3Application.getCastManager(MB3Application.getInstance()).playItems(mInstantMixItems, PlayCommand.PlayNow, 0L);
                Intent intent = new Intent(mMusicActivity, RemoteControlActivity.class);
                mMusicActivity.startActivity(intent);
            } else {
                for (BaseItemDto song : mInstantMixItems) {
                    PlaylistItem playableItem = new PlaylistItem();
                    playableItem.Id = song.getId();
                    playableItem.Name = song.getName();
                    if (song.getArtists() != null) {
                        StringBuilder sb = new StringBuilder();
                        for (String artist : song.getArtists()) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(artist);
                        }
                        playableItem.SecondaryText = sb.toString();
                    }

                    playableItem.Type = song.getType();
                    playableItem.Runtime = song.getRunTimeTicks();

                    MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playableItem);
                }
                Intent intent = new Intent(mMusicActivity, AudioPlaybackActivity.class);
                mMusicActivity.startActivity(intent);
            }
        }
    };

    protected void setInstantMixImages(List<String> imageUrls) {

        mBackdropSwitcher.removeCallbacks(CycleBackdrops);
        mBackdropUrls = imageUrls;
        mBackdropIndex = 0;

        if (imageUrls.size() > 1)
            mBackdropSwitcher.post(CycleBackdrops);
        else
            setInstantMixImage(imageUrls.get(0));
    }

    private void setInstantMixImage(String imageUrl) {

        if (imageUrl == null || imageUrl.isEmpty()) {
            FileLogger.getFileLogger().Error("Error setting backdrop - imageUrl is null or empty");
            return;
        }

        if (mBackdropSwitcher.getDisplayedChild() == 0) {
            mBackdropImage2.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            mBackdropSwitcher.showNext();
        } else {
            mBackdropImage1.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            mBackdropSwitcher.showPrevious();
        }
    }

    protected void disableInstantMixImage() {

        if (mBackdropSwitcher == null) return;

        if (mBackdropUrls != null && mBackdropUrls.size() > 1)
            mBackdropSwitcher.removeCallbacks(CycleBackdrops);
    }

    protected void enableInstantMixImage() {

        if (mBackdropSwitcher == null) return;

        if (mBackdropUrls != null && mBackdropUrls.size() > 1)
            mBackdropSwitcher.post(CycleBackdrops);
    }
}
