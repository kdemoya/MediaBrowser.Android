package com.mb.android.ui.mobile.library;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import mediabrowser.apiinteraction.Response;
import com.mb.android.interfaces.ICommandListener;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.ui.mobile.musicartist.ArtistActivity;
import com.mb.android.activities.mobile.BookDetailsActivity;
import com.mb.android.activities.mobile.MediaDetailsActivity;
import com.mb.android.ui.mobile.album.MusicAlbumActivity;
import com.mb.android.activities.mobile.PhotoDetailsActivity;
import com.mb.android.activities.mobile.SeriesViewActivity;
import com.mb.android.adapters.MediaAdapterBackdrops;
import com.mb.android.adapters.MediaAdapterPosters;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.querying.EpisodeQuery;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.library.PlayAccess;
import com.mb.android.logging.FileLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * This fragment shows a list of various items in a users library
 */
public class LibraryPresentationFragment extends Fragment implements ICommandListener{

    private static final String TAG = "LibraryPresentationFragment";
    private GridView mLibraryGrid;
    private BaseItemDto mItem;
    private ItemQuery mItemQuery;
    private EpisodeQuery mEpisodeQuery;
    private boolean mPosterViewEnabled;
    private ProgressBar mProgress;
    private List<BaseItemDto> mItems;
    private int mCurrentQueryStartIndex;
    private boolean mDisableIndexing;
    private View mView;
    private LibraryPresentationActivity mLibraryActivity;
    private Response mResponse;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FileLogger.getFileLogger().Info(TAG + ": onCreate");

        String jsonData = getArguments().getString("EpisodeQuery");
        if (jsonData != null) {
            mEpisodeQuery = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, EpisodeQuery.class);
        } else {
            jsonData = getArguments().getString("ItemQuery");
            if (jsonData != null) {
                mItemQuery = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, ItemQuery.class);
            } else {
                jsonData = getArguments().getString("Item");
                if (jsonData != null) {
                    mItem = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, BaseItemDto.class);
                }
            }
        }
        mDisableIndexing = getArguments().getBoolean("DisableIndexing", false);

        SharedPreferences mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mLibraryActivity);
        mPosterViewEnabled = mSharedPrefs.getBoolean("pref_prefer_posters", false);

        DisplayMetrics metrics = new DisplayMetrics();
        mLibraryActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        if (savedInstanceState != null) {
            mCurrentQueryStartIndex = 0;
            if (mItemQuery != null) {
                mItemQuery.setStartIndex(0);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        FileLogger.getFileLogger().Info("Library Presentation Fragment: onCreateView");

        if (mPosterViewEnabled) {
            if (mDisableIndexing) {
                mView = inflater.inflate(R.layout.fragment_library_presentation_poster, container, false);
            } else {
                mView = inflater.inflate(R.layout.fragment_library_presentation_poster_indexable, container, false);
            }
        } else {
            if (mDisableIndexing) {
                mView = inflater.inflate(R.layout.fragment_library_presentation, container, false);
            } else {
                mView = inflater.inflate(R.layout.fragment_library_presentation_indexable, container, false);
            }
        }

        if (mView == null)
            return null;

        mProgress = (ProgressBar) mView.findViewById(R.id.pbLibraryProgress);

        mLibraryGrid = (GridView) mView.findViewById(R.id.gvLibrary);
        mLibraryGrid.setOnItemClickListener(itemClickListener);

        FileLogger.getFileLogger().Info("Library Presentation Fragment: Requesting Items");
        mProgress.setVisibility(ProgressBar.VISIBLE);

        if (MB3Application.getInstance().getIsConnected()) {
            performInitialQueries();
        }

        return mView;
    }

    public void onConnectionRestored() {
        performInitialQueries();
    }

    private void performInitialQueries() {
        if (mItemQuery != null) {
            MB3Application.getInstance().API.GetItemsAsync(mItemQuery, getItemsResponse);
        } else if (mEpisodeQuery != null) {
            MB3Application.getInstance().API.GetEpisodesAsync(mEpisodeQuery, getItemsResponse);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity != null) {
            try {
                mLibraryActivity = (LibraryPresentationActivity) activity;
            } catch (ClassCastException e) {
                Log.d("ServerSelectionFragment", "onAttach: Exception casting activity");
            }
        }
    }

    private void InitializeLibraryView() {

        FileLogger.getFileLogger().Info(TAG + ": Populating Library View");

        if (mItems.size() > 0) {
            FileLogger.getFileLogger().Info(TAG + ": " + String.valueOf(mItems.size()) + " items to show");

            mLibraryGrid.setAdapter(mPosterViewEnabled
                    ? new MediaAdapterPosters(mItems, MB3Application.getInstance().getResources().getInteger(R.integer.library_columns_poster), MB3Application.getInstance().API, R.drawable.default_video_portrait)
                    : new MediaAdapterBackdrops(mLibraryActivity, mItems, MB3Application.getInstance().API, R.drawable.default_video_landscape)
            );
            mLibraryGrid.setFastScrollEnabled(true);
            mLibraryGrid.requestFocus();
        } else {
            FileLogger.getFileLogger().Info(TAG + ": No items in collection");
            Log.i("InitializeLibraryView", "No items in collection.");
        }
    }

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {

        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

            Log.i("ItemClickListener", "Position Clicked [" + position + "]");
            FileLogger.getFileLogger().Info("Library Presentation Fragment: Position Clicked " + position);

            mItem = mItems.get(position);
            String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(mItem);

            Intent intent;

            if (mItem.getType().equalsIgnoreCase("MusicArtist")) {

                FileLogger.getFileLogger().Info("Library Presentation Fragment: Item is MusicArtist");
                intent = new Intent(mLibraryActivity, ArtistActivity.class);
                intent.putExtra("ArtistId", mItem.getId());

            } else if (mItem.getType().equalsIgnoreCase("MusicAlbum")) {

                FileLogger.getFileLogger().Info("Library Presentation Fragment: Item is MusicAlbum");
                intent = new Intent(mLibraryActivity, MusicAlbumActivity.class);
                intent.putExtra("AlbumId", mItem.getId());

            } else if (mItem.getType().equalsIgnoreCase("Series")) {

                intent = new Intent(mLibraryActivity, SeriesViewActivity.class);
                intent.putExtra("Item", jsonData);

            } else if (mItem.getType().equalsIgnoreCase("Photo")) {

//                if (MB3Application.getCastManager(mLibraryActivity).isConnected()) {
//                    JSONObject data = new JSONObject();
//
//                    try {
//                        data.put("serverAddress",
//                                MB3Application.getInstance().API.getServerAddress());
//                        data.put("itemId", mMediaWrapper.Item.getId());
//                        data.put("userId", MB3Application.getInstance().API.getCurrentUserId());
//                        data.put("deviceId", MB3Application.getCastManager(mLibraryActivity).getCurrentDeviceId());
//                        data.put("deviceName", MB3Application.getCastManager(mLibraryActivity).getDeviceName());
//                        data.put("startTimeTicks", "0");
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//
//                    MediaInfo mediaInfo =
//                            Utils.ToMediaInfo(mMediaWrapper.Item, null, null, null, data);
//                    try {
//                        MB3Application.getCastManager(mLibraryActivity).loadMedia(mediaInfo, true, 0);
//                    } catch (TransientNetworkDisconnectionException e) {
//                        FileLogger.getFileLogger().ErrorException("Exception Handled: ", e);
//                    } catch (NoConnectionException e) {
//                        e.printStackTrace();
//                    }
//                } else {
                intent = new Intent(mLibraryActivity, PhotoDetailsActivity.class);
                intent.putExtra("Item", jsonData);
                if (mItemQuery != null && mItemQuery.getParentId() != null) {
                    intent.putExtra("ParentId", mItemQuery.getParentId());
                }
//                }

            } else if (mItem.getType().equalsIgnoreCase("Book")) {

                intent = new Intent(mLibraryActivity, BookDetailsActivity.class);
                intent.putExtra("Item", jsonData);

            } else if (mItem.getIsFolder()) {

                FileLogger.getFileLogger().Info("Library Presentation Fragment: Item is folder");
                intent = new Intent(mLibraryActivity, LibraryPresentationActivity.class);
                intent.putExtra("Item", jsonData);

            } else {

                FileLogger.getFileLogger().Info("Library Presentation Fragment: Item is media");
                intent = new Intent(mLibraryActivity, MediaDetailsActivity.class);
                intent.putExtra("currentIndex", position);
                intent.putExtra("Item", jsonData);
            }

            FileLogger.getFileLogger().Info("Calling StartActivity");
            startActivity(intent);
        }
    };

    public void PerformQuery(ItemQuery query) {

        mCurrentQueryStartIndex = 0;
        mItemQuery.setStartIndex(0);

        // perform the new query
        MB3Application.getInstance().API.GetItemsAsync(query, getItemsResponse);
        mLibraryGrid.setAdapter(null);
    }

    public void PerformQuery(ItemQuery query, Response response) {

        mResponse = response;
        PerformQuery(query);
    }


    public List<Integer> GetAvailableYears() {

        List<Integer> years = new ArrayList<>();

        for (BaseItemDto item : mItems) {

            if (item.getProductionYear() != null && !years.contains(item.getProductionYear()))
                years.add(item.getProductionYear());
        }

        Collections.sort(years);
        Collections.reverse(years);

        return years;
    }


    public List<String> GetAvailableGenres() {

        List<String> genres = new ArrayList<>();

        for (BaseItemDto item : mItems) {

            if (item.getGenres() != null) {
                for (String g : item.getGenres()) {
                    if (!genres.contains(g))
                        genres.add(g);
                }
            }
        }

        Collections.sort(genres);

        return genres;
    }


    public List<String> GetAvailableOfficialRatings() {

        List<String> ratings = new ArrayList<>();

        for (BaseItemDto item : mItems) {

            if (item.getOfficialRating() != null && !item.getOfficialRating().isEmpty() && !ratings.contains(item.getOfficialRating()))
                ratings.add(item.getOfficialRating());
        }

        return ratings;
    }

    private Response<ItemsResult> getItemsResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {
            FileLogger.getFileLogger().Info("Library Presentation Fragment: Get Items response");
            mProgress.setVisibility(ProgressBar.GONE);

            if (response == null) return;

            if (response.getItems() != null && response.getItems().length > 0) {
                FileLogger.getFileLogger().Info("Library Presentation Fragment - Get Items response: " + response.getItems().length + " Items to process");
                FileLogger.getFileLogger().Info("Library Presentation Fragment - Get Items response: Total Record Count = " + response.getTotalRecordCount());

                if (mLibraryGrid.getAdapter() == null) {
                    mLibraryActivity.SetPlayControlsEnabled(areItemsPlayable(response.getItems()));
                    mItems = new ArrayList<>();
                    mItems.addAll(Arrays.asList(response.getItems()));
                    InitializeLibraryView();
                    mCurrentQueryStartIndex = response.getItems().length;
                } else {
                    mItems.addAll(Arrays.asList(response.getItems()));

                    if (mPosterViewEnabled)
                        ((MediaAdapterPosters) mLibraryGrid.getAdapter()).notifyDataSetChanged();
                    else
                        ((MediaAdapterBackdrops) mLibraryGrid.getAdapter()).notifyDataSetChanged();

                    mCurrentQueryStartIndex += response.getItems().length;
                }

                if (response.getTotalRecordCount() > mCurrentQueryStartIndex + 1) {

                    mItemQuery.setStartIndex(mCurrentQueryStartIndex);
                    MB3Application.getInstance().API.GetItemsAsync(mItemQuery, this);
                } else {
                }


//                if (mMediaWrapper.Items[0].Type.equalsIgnoreCase("MusicArtist")) {
//                    FileLogger.getFileLogger(LibraryPresentationActivity.this).Info("Library Presentation Fragment - Get Items Callback: response contains MusicArtists, getting Albums");
//                    isProcessed = false;
//                    ItemQuery query = new ItemQuery();
//                    query.UserId = mPayload.User.Id;
//                    query.ParentId = mMediaWrapper.Item.Id;
//                    query.SortBy = new ItemSortBy[] { ItemSortBy.SortName };
//                    query.SortOrder = SortOrder.Ascending;
//                    query.Recursive = true;
//                    query.ExcludeItemTypes = new String[] { "MusicArtist", "Audio" };
//                    query.Fields = new ItemFields[] { ItemFields.UserData, ItemFields.ItemCounts, ItemFields.ParentId, ItemFields.Genres, ItemFields.Studios, ItemFields.SortName, ItemFields.AudioInfo, ItemFields.DateCreated };
//
//                    mApi.GetItemsAsync(query, null, new GetItemsCallback());
//
//                }
            } else {
                Log.i("GetItemCallback", "result or result.items is null");
                FileLogger.getFileLogger().Info("Library Presentation Fragment - Get Items Callback: No items or response was null");

                TextView noContentWarning = (TextView) mView.findViewById(R.id.tvNoContentWarning);
                noContentWarning.setVisibility(TextView.VISIBLE);
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };


    private boolean areItemsPlayable(BaseItemDto[] items) {
        return items != null
                && items.length > 0
                && items[0].getPlayAccess().equals(PlayAccess.Full)
                && ("audio".equalsIgnoreCase(items[0].getMediaType())
                    || "video".equalsIgnoreCase(items[0].getMediaType())
                    || "season".equalsIgnoreCase(items[0].getType())
                    || "series".equalsIgnoreCase(items[0].getType()));
    }

    @Override
    public void onPreviousButton() {

    }

    @Override
    public void onNextButton() {

    }

    @Override
    public void onPlayPauseButton() {

    }

    @Override
    public void onPlayButton() {

    }

    @Override
    public void onPauseButton() {

    }
}
