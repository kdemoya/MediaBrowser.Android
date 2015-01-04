package com.mb.android.ui.mobile.music;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.adapters.GenericAdapterPosters;
import com.mb.android.adapters.GenreAdapter;
import com.mb.android.listeners.AlbumOnItemClickListener;
import com.mb.android.listeners.SongOnItemClickListener;
import com.mb.android.ui.mobile.album.MusicAlbumActivity;
import com.mb.android.ui.mobile.album.SongAdapter;
import mediabrowser.apiinteraction.Response;
import com.mb.android.listeners.ArtistOnItemClickListener;
import com.mb.android.listeners.OverflowOnItemLongClickListener;
import com.mb.android.logging.FileLogger;
import com.mb.android.widget.indexablegridview.IndexableGridView;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ArtistsQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.ItemsByNameQuery;
import mediabrowser.model.querying.ItemsResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Mark on 2014-07-15.
 *
 * Fragment that shows all artists in the music library
 */
public class MusicLibraryFragment extends Fragment {

    private static final String TAG = "ArtistsFragment";
    private MusicActivity mMusicActivity;
    private IndexableGridView mContentGrid;
    private List<BaseItemDto> mItems;
    private String mParentId;
    private boolean isFresh = true;
    private RootCategory rootCategory = RootCategory.artist;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceStage) {
        FileLogger.getFileLogger().Info(TAG + ": onCreate");

        View view = inflater.inflate(R.layout.fragment_library_presentation_indexable, parent, false);
//        View view = inflater.inflate(R.layout.fragment_music_library, parent, false);
        mContentGrid = (IndexableGridView) view.findViewById(R.id.gvLibrary);
        mParentId = getArguments().getString("ParentId");

        FileLogger.getFileLogger().Info(TAG + ": End onCreate");
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity != null) {
            try {
                mMusicActivity = (MusicActivity) activity;
                mMusicActivity.updateMusicLibraryFragmentReference(this);
            } catch (ClassCastException e) {
                Log.d("ServerSelectionFragment", "onAttach: Exception casting activity");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        FileLogger.getFileLogger().Info(TAG + ": onResume");
        if (isFresh) {
            rootCategory = RootCategory.valueOf(PreferenceManager.getDefaultSharedPreferences(MB3Application.getInstance()).getString("pref_music_root", "artist"));
            switch(rootCategory) {
                case artist:
                    displayArtists();
                    break;
                case albumArtist:
                    displayAlbumArtists();
                    break;
                case album:
                    displayAlbums();
                    break;
                case song:
                    displaySongs();
                    break;
                case genre:
                    displayGenres();
                    break;
            }
            isFresh = false;
        }
        FileLogger.getFileLogger().Info(TAG + ": end onResume");
    }

    @Override
    public void onPause() {
        super.onPause();

        mItems = new ArrayList<>();
    }

    @Override
    public void onDestroy() {

        mMusicActivity = null;

        super.onDestroy();
    }

    //******************************************************************************************************************
    // Methods called from the filter/sort menu
    //******************************************************************************************************************


    public void displayArtists() {
        rootCategory = RootCategory.artist;
        writeCategoryToPreferences(rootCategory);
        mItems = new ArrayList<>();
        mContentGrid.setAdapter(null);

        FileLogger.getFileLogger().Info(TAG + ": Build artists query");
        ArtistsQuery query = new ArtistsQuery();
        query.setParentId(mParentId);
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setRecursive(true);
        query.setSortBy(new String[]{ItemSortBy.SortName});
        query.setSortOrder(SortOrder.Ascending);
        query.setFields(new ItemFields[]{ItemFields.ParentId, ItemFields.PrimaryImageAspectRatio, ItemFields.SortName});
        query.setStartIndex(0);
        query.setLimit(200);

        MB3Application.getInstance().API.GetArtistsAsync(query, new GetArtistsResponse(query, false));
    }

    public void displayAlbumArtists() {
        rootCategory = RootCategory.albumArtist;
        writeCategoryToPreferences(rootCategory);
        mItems = new ArrayList<>();
        mContentGrid.setAdapter(null);

        FileLogger.getFileLogger().Info(TAG + ": Build album artists query");
        ArtistsQuery query = new ArtistsQuery();
        query.setParentId(mParentId);
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setRecursive(true);
        query.setSortBy(new String[]{ItemSortBy.SortName});
        query.setSortOrder(SortOrder.Ascending);
        query.setFields(new ItemFields[]{ItemFields.ParentId, ItemFields.PrimaryImageAspectRatio, ItemFields.SortName});
        query.setStartIndex(0);
        query.setLimit(200);

        MB3Application.getInstance().API.GetAlbumArtistsAsync(query, new GetArtistsResponse(query, true));
    }

    public void displayAlbums() {
        rootCategory = RootCategory.album;
        writeCategoryToPreferences(rootCategory);
        mItems = new ArrayList<>();
        mContentGrid.setAdapter(null);

        FileLogger.getFileLogger().Info(TAG + ": Build albums query");
        ItemQuery query = new ItemQuery();
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setRecursive(true);
        query.setSortBy(new String[]{ItemSortBy.SortName});
        query.setSortOrder(SortOrder.Ascending);
        query.setFields(new ItemFields[]{ItemFields.ParentId, ItemFields.SortName, ItemFields.PrimaryImageAspectRatio});
        query.setIncludeItemTypes(new String[]{"MusicAlbum"});
        query.setStartIndex(0);
        query.setLimit(200);

        MB3Application.getInstance().API.GetItemsAsync(query, new GetAlbumsResponse(query));

    }

    public void displaySongs() {
        rootCategory = RootCategory.song;
        writeCategoryToPreferences(rootCategory);
        mItems = new ArrayList<>();
        mContentGrid.setAdapter(null);

        FileLogger.getFileLogger().Info(TAG + ": Build songs query");
        ItemQuery query = new ItemQuery();
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setRecursive(true);
        query.setSortBy(new String[]{ItemSortBy.Name});
        query.setSortOrder(SortOrder.Ascending);
        query.setFields(new ItemFields[]{ItemFields.ParentId, ItemFields.SortName});
        query.setIncludeItemTypes(new String[]{"Audio"});
        query.setStartIndex(0);
        query.setLimit(200);

        MB3Application.getInstance().API.GetItemsAsync(query, new GetSongsResponse(query));
    }

    public void displayGenres() {
        rootCategory = RootCategory.genre;
        writeCategoryToPreferences(rootCategory);
        mItems = new ArrayList<>();
        mContentGrid.setAdapter(null);

        FileLogger.getFileLogger().Info(TAG + ": Build genres query");
        ItemsByNameQuery query = new ItemsByNameQuery();
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setParentId(mParentId);
        query.setRecursive(true);
        query.setSortBy(new String[]{ItemSortBy.Name});
        query.setSortOrder(SortOrder.Ascending);
        query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.SortName});

        MB3Application.getInstance().API.GetMusicGenresAsync(query, new GetGenresResponse());
    }

    private void writeCategoryToPreferences(RootCategory category) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MB3Application.getInstance());
        prefs.edit()
                .putString("pref_music_root", category.name())
                .apply();
    }

    //******************************************************************************************************************
    // GridView initialization
    //******************************************************************************************************************

    private void initializeArtistsGrid() {
        FileLogger.getFileLogger().Info(TAG + ": initialize artists grid");
        mContentGrid.setNumColumns(MB3Application.getInstance().getResources().getInteger(R.integer.library_columns_poster));
        mContentGrid.setVerticalSpacing(10);
        mContentGrid.setAdapter(new GenericAdapterPosters(mItems, MB3Application.getInstance().getResources().getInteger(R.integer.library_columns_poster), mMusicActivity, R.drawable.default_artist));
        mContentGrid.setOnItemClickListener(new ArtistOnItemClickListener());
        mContentGrid.setOnItemLongClickListener(new OverflowOnItemLongClickListener());
    }

    private void initializeAlbumsGrid() {
        FileLogger.getFileLogger().Info(TAG + ": initialize albums grid");
        mContentGrid.setNumColumns(MB3Application.getInstance().getResources().getInteger(R.integer.library_columns_poster));
        mContentGrid.setVerticalSpacing(10);
        mContentGrid.setAdapter(new GenericAdapterPosters(mItems, getResources().getInteger(R.integer.library_columns_poster), mMusicActivity, R.drawable.music_square_bg));
        mContentGrid.setOnItemClickListener(new AlbumOnItemClickListener());
        mContentGrid.setOnItemLongClickListener(new OverflowOnItemLongClickListener());
    }

    private void initilizeSongsGrid() {
        FileLogger.getFileLogger().Info(TAG + ": initialize songs list");
        mContentGrid.setNumColumns(1);
        mContentGrid.setVerticalSpacing(0);
        mContentGrid.setAdapter(new SongAdapter(mItems, mMusicActivity));
        mContentGrid.setOnItemClickListener(new SongOnItemClickListener());
        mContentGrid.setOnItemLongClickListener(new OverflowOnItemLongClickListener());
    }

    private void initializeGenresGrid() {
        FileLogger.getFileLogger().Info(TAG + ": initialize genres grid");
        mContentGrid.setNumColumns(getResources().getInteger(R.integer.library_columns_poster));
        mContentGrid.setVerticalSpacing(10);
        mContentGrid.setAdapter(new GenreAdapter(mItems, mMusicActivity));
        mContentGrid.setOnItemClickListener(onGenreClickListener);
        mContentGrid.setOnItemLongClickListener(new OverflowOnItemLongClickListener());
    }

    //******************************************************************************************************************
    // API Responses
    //******************************************************************************************************************

    private class GetArtistsResponse extends Response<ItemsResult> {

        private ArtistsQuery mQuery;
        private boolean mIsAlbumArtistQuery;

        public GetArtistsResponse(ArtistsQuery query, boolean isAlbumArtistQuery) {
            mQuery = query;
            mIsAlbumArtistQuery = isAlbumArtistQuery;
        }

        @Override
        public void onResponse(ItemsResult result) {

            FileLogger.getFileLogger().Info(TAG + ": onResponse");
            if (result == null || result.getItems() == null) {
                FileLogger.getFileLogger().Info(TAG + ": nothing to show");
                return;
            }

            // Ignore the response since the user has changed layouts
            if ((mIsAlbumArtistQuery && !rootCategory.equals(RootCategory.albumArtist))
                    || (!mIsAlbumArtistQuery && !rootCategory.equals(RootCategory.artist))) {
                return;
            }

            if (mItems == null) {
                mItems = new ArrayList<>();
            }

            mItems.addAll(Arrays.asList(result.getItems()));

            GenericAdapterPosters adapter = (GenericAdapterPosters)mContentGrid.getAdapter();

            if (adapter != null) {
                FileLogger.getFileLogger().Info(TAG + ": add additional content");
                adapter.notifyDataSetChanged();
            } else {
                initializeArtistsGrid();
            }

            if (result.getTotalRecordCount() > mQuery.getStartIndex() + 200) {
                FileLogger.getFileLogger().Info(TAG + ": more items to retrieve");
                mQuery.setStartIndex(mQuery.getStartIndex() + 200);
                if (mIsAlbumArtistQuery) {
                    MB3Application.getInstance().API.GetAlbumArtistsAsync(mQuery, this);
                } else {
                    MB3Application.getInstance().API.GetArtistsAsync(mQuery, this);
                }
            }
        }
        @Override
        public void onError(Exception e) {
            FileLogger.getFileLogger().Info(TAG + ": error getting artists");
        }
    }

    private class GetAlbumsResponse extends Response<ItemsResult> {

        private ItemQuery mQuery;

        public GetAlbumsResponse(ItemQuery query) {
            mQuery = query;
        }

        @Override
        public void onResponse(ItemsResult result) {

            FileLogger.getFileLogger().Info(TAG + ": onResponse");
            if (result == null || result.getItems() == null) {
                FileLogger.getFileLogger().Info(TAG + ": nothing to show");
                return;
            }

            // Ignore the response since the user has changed layouts
            if (!rootCategory.equals(RootCategory.album)) {
                return;
            }

            if (mItems == null) {
                mItems = new ArrayList<>();
            }

            mItems.addAll(Arrays.asList(result.getItems()));

            GenericAdapterPosters adapter = (GenericAdapterPosters)mContentGrid.getAdapter();

            if (adapter != null) {
                FileLogger.getFileLogger().Info(TAG + ": add additional content");
                adapter.notifyDataSetChanged();
            } else {
                initializeAlbumsGrid();
            }

            if (result.getTotalRecordCount() > mQuery.getStartIndex() + 200) {
                FileLogger.getFileLogger().Info(TAG + ": more items to retrieve");
                mQuery.setStartIndex(mQuery.getStartIndex() + 200);
                MB3Application.getInstance().API.GetItemsAsync(mQuery, this);
            }
        }
        @Override
        public void onError(Exception e) {
            FileLogger.getFileLogger().Info(TAG + ": error getting albums");
        }
    }

    private class GetSongsResponse extends Response<ItemsResult> {

        private ItemQuery mQuery;

        public GetSongsResponse(ItemQuery query) {
            mQuery = query;
        }

        @Override
        public void onResponse(ItemsResult result) {

            FileLogger.getFileLogger().Info(TAG + ": onResponse");
            if (result == null || result.getItems() == null) {
                FileLogger.getFileLogger().Info(TAG + ": nothing to show");
                return;
            }

            // Ignore the response since the user has changed layouts
            if (!rootCategory.equals(RootCategory.song)) {
                return;
            }

            if (mItems == null) {
                mItems = new ArrayList<>();
            }

            mItems.addAll(Arrays.asList(result.getItems()));

            SongAdapter adapter = (SongAdapter) mContentGrid.getAdapter();

            if (adapter != null) {
                FileLogger.getFileLogger().Info(TAG + ": add additional content");
                adapter.notifyDataSetChanged();
            } else {
                initilizeSongsGrid();
            }

            if (result.getTotalRecordCount() > mQuery.getStartIndex() + 200) {
                FileLogger.getFileLogger().Info(TAG + ": more items to retrieve");
                mQuery.setStartIndex(mQuery.getStartIndex() + 200);
                MB3Application.getInstance().API.GetItemsAsync(mQuery, this);
            }
        }
        @Override
        public void onError(Exception e) {
            FileLogger.getFileLogger().Info(TAG + ": error getting songs");
        }
    }

    private class GetGenresResponse extends Response<ItemsResult> {

        @Override
        public void onResponse(ItemsResult result) {

            FileLogger.getFileLogger().Info(TAG + ": onResponse");
            if (result == null || result.getItems() == null) {
                FileLogger.getFileLogger().Info(TAG + ": nothing to show");
                return;
            }

            // Ignore the response since the user has changed layouts
            if (!rootCategory.equals(RootCategory.genre)) {
                return;
            }

            if (mItems == null) {
                mItems = new ArrayList<>();
            }

            mItems.addAll(Arrays.asList(result.getItems()));

            GenreAdapter adapter = (GenreAdapter) mContentGrid.getAdapter();

            if (adapter != null) {
                FileLogger.getFileLogger().Info(TAG + ": add additional content");
                adapter.notifyDataSetChanged();
            } else {
                initializeGenresGrid();
            }
        }
        @Override
        public void onError(Exception e) {
            FileLogger.getFileLogger().Info(TAG + ": error getting genres");
        }
    }

    AdapterView.OnItemClickListener onGenreClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (mItems == null || mItems.size() <= position) return;


            mItems.get(position).setParentId(mParentId);

            Intent intent = new Intent(MB3Application.getInstance(), MusicAlbumActivity.class);
            intent.putExtra("AlbumId", mItems.get(position).getId());
            intent.putExtra("isGenre", true);
            startActivity(intent);
        }
    };
}
