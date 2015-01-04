package com.mb.android.ui.mobile.homescreen;

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

import com.mb.android.DialogFragments.LatestItemsDialogFragment;
import com.mb.android.MB3Application;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import mediabrowser.apiinteraction.Response;
import com.mb.android.interfaces.ICommandListener;
import com.mb.android.ui.mobile.musicartist.ArtistActivity;
import com.mb.android.activities.mobile.BookDetailsActivity;
import com.mb.android.ui.mobile.library.LibraryPresentationActivity;
import com.mb.android.activities.mobile.MediaDetailsActivity;
import com.mb.android.ui.mobile.album.MusicAlbumActivity;
import com.mb.android.activities.mobile.PhotoDetailsActivity;
import com.mb.android.adapters.HomeScreenItemsAdapter;
import com.mb.android.ui.mobile.playback.PlaybackActivity;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.querying.ItemFields;
import com.mb.android.logging.FileLogger;
import mediabrowser.model.querying.LatestItemsQuery;

import java.util.ArrayList;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment that shows all items recently added to a users library.
 */
public class NewItemsFragment extends Fragment implements ICommandListener {

    private static final String TAG = "NewItemsFragment";
    private BaseItemDto[] mItems;
    private ProgressBar mActivityIndicator;
    private GridView mNewItemsGrid;
    private TextView noContentText;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_homescreen_items, container, false);
        if (view != null) {
            mActivityIndicator = (ProgressBar) view.findViewById(R.id.pbActivityIndicator);
            mNewItemsGrid = (GridView) view.findViewById(R.id.gvUpNext);
            noContentText = (TextView) view.findViewById(R.id.tvNoContentWarning);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        FileLogger.getFileLogger().Info(TAG + "onResume");
        if (MB3Application.getInstance().API != null
                && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(MB3Application.getInstance().API.getCurrentUserId())) {
            LatestItemsQuery query = new LatestItemsQuery();
            query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
            query.setLimit(20);
            query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.ParentId});
            query.setIsPlayed(false);
            query.setGroupItems(true);
            MB3Application.getInstance().API.GetLatestItems(query, getNewItemsResponse);
        }
        FileLogger.getFileLogger().Info(TAG + "finish onResume");
    }

    private Response<BaseItemDto[]> getNewItemsResponse = new Response<BaseItemDto[]>() {
        @Override
        public void onResponse(BaseItemDto[] response) {
            FileLogger.getFileLogger().Info(TAG + ": GetNewItems");
            hideActivityIndicator();
            processResponse(response);
            toggleNoContentWarning();
            refreshOrInitializeGridContent();
            FileLogger.getFileLogger().Info(TAG + ": Finished GetNewItems");
        }
        @Override
        public void onError(Exception ex) {
            FileLogger.getFileLogger().Info(TAG + ": error getting new items");
        }
    };

    private void hideActivityIndicator() {
        if (null != mActivityIndicator) {
            mActivityIndicator.setVisibility(View.GONE);
        }
    }

    private void processResponse(BaseItemDto[] response) {
        if (null == response) {
            FileLogger.getFileLogger().Info(TAG + " - processResponse: Invalid response");
            return;
        }
        FileLogger.getFileLogger().Info(TAG + " - processResponse: " + String.valueOf(response.length) + " Items received.");
        mItems = response;
    }

    private void toggleNoContentWarning() {
        if (null == noContentText) return;
        boolean showWarning = null == mItems || mItems.length == 0;
        noContentText.setText(R.string.no_new_items_warning);
        noContentText.setVisibility(showWarning ? TextView.VISIBLE : TextView.INVISIBLE);
    }

    private void refreshOrInitializeGridContent() {
        if (mNewItemsGrid == null) return;
        HomeScreenItemsAdapter adapter = (HomeScreenItemsAdapter) mNewItemsGrid.getAdapter();
        if (adapter != null) {
            adapter.addNewDataset(mItems);
            adapter.notifyDataSetChanged();
        } else if (null != mItems) {
            mNewItemsGrid.setAdapter(new HomeScreenItemsAdapter(mItems));
            mNewItemsGrid.setOnItemClickListener(onItemClickListener);
        }
    }

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int index, long id) {
            if (mItems == null || mItems.length == 0) {
                return;
            }

            String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(mItems[index]);
            Intent intent;

            // New items are going to be pretty much anything... awesome.

            if (mItems[index].getType().equalsIgnoreCase("series")) {
                LatestItemsDialogFragment fragment = new LatestItemsDialogFragment();
                fragment.setSeries(mItems[index]);
                fragment.show(getFragmentManager(), "LatestItemsDialog");
                return;
            } else if (mItems[index].getType().equalsIgnoreCase("musicartist")) {
                intent = new Intent(MB3Application.getInstance(), ArtistActivity.class);
                intent.putExtra("ArtistId", mItems[index].getId());
            } else if (mItems[index].getType().equalsIgnoreCase("musicalbum")) {
                intent = new Intent(MB3Application.getInstance(), MusicAlbumActivity.class);
                intent.putExtra("AlbumId", mItems[index].getId());
            } else if (mItems[index].getType().equalsIgnoreCase("audio")) {
                MB3Application.getInstance().API.GetItemAsync(
                        mItems[index].getAlbumId(),
                        MB3Application.getInstance().API.getCurrentUserId(),
                        getAlbumResponse);
                return;
            } else if (mItems[index].getType().equalsIgnoreCase("photo")) {
                intent = new Intent(MB3Application.getInstance(), PhotoDetailsActivity.class);
                intent.putExtra("Item", jsonData);
            } else if (mItems[index].getType().equalsIgnoreCase("book")) {
                intent = new Intent(MB3Application.getInstance(), BookDetailsActivity.class);
                intent.putExtra("Item", jsonData);
            } else if (mItems[index].getIsFolder()) {
                intent = new Intent(MB3Application.getInstance(), LibraryPresentationActivity.class);
                intent.putExtra("Item", jsonData);
            } else {
                intent = new Intent(MB3Application.getInstance(), MediaDetailsActivity.class);
                intent.putExtra("Item", jsonData);
                intent.putExtra("LaunchedFromHomeScreen", true);
            }

            startActivity(intent);
        }
    };

    private Response<BaseItemDto> getAlbumResponse = new Response<BaseItemDto>() {

        @Override
        public void onResponse(BaseItemDto item) {

            if (item == null) return;

            Intent intent = new Intent(MB3Application.getInstance(), MusicAlbumActivity.class);
            intent.putExtra("AlbumId", item.getId());

            startActivity(intent);
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    @Override
    public void onStop() {
        super.onStop();
        mItems = null;
    }

    private void playRequest() {
        if (mItems == null || mItems.length == 0) return;

        BaseItemDto item = mItems[mNewItemsGrid.getSelectedItemPosition()];

        if ("series".equalsIgnoreCase(item.getType())) {
            handleSeriesPlayRequest(item.getId());
        } else {
            PlaylistItem playableItem = new PlaylistItem();
            playableItem.Id = item.getId();
            playableItem.Name = item.getName();
            playableItem.startPositionTicks = 0L;
            playableItem.Type = item.getType();

            if (item.getType().equalsIgnoreCase("episode"))
                playableItem.SecondaryText = item.getSeriesName();

            MB3Application.getInstance().PlayerQueue.PlaylistItems = new ArrayList<>();
            MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playableItem);

            Intent intent = new Intent(MB3Application.getInstance(), PlaybackActivity.class);
            startActivity(intent);
        }
    }

    private void handleSeriesPlayRequest(String seriesId) {
        LatestItemsQuery query = new LatestItemsQuery();
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.ParentId, ItemFields.DateCreated});
        query.setParentId(seriesId);
        query.setIncludeItemTypes(new String[] { "episode" });
        query.setIsPlayed(false);
        query.setGroupItems(false);

        MB3Application.getInstance().API.GetLatestItems(query, new getSeriesUnwatchedItemsResponse());
    }

    @Override
    public void onPreviousButton() {

    }

    @Override
    public void onNextButton() {

    }

    @Override
    public void onPlayPauseButton() {
        playRequest();
    }

    @Override
    public void onPlayButton() {
        playRequest();
    }

    @Override
    public void onPauseButton() {

    }

    private class getSeriesUnwatchedItemsResponse extends Response<BaseItemDto[]> {
        @Override
        public void onResponse(BaseItemDto[] episodes) {
            if (episodes == null || episodes.length == 0) return;

            MB3Application.getInstance().PlayerQueue.PlaylistItems = new ArrayList<>();

            for (BaseItemDto item : episodes) {
                PlaylistItem playableItem = new PlaylistItem();
                playableItem.Id = item.getId();
                playableItem.Name = item.getName();
                playableItem.startPositionTicks = 0L;
                playableItem.Type = item.getType();
                playableItem.SecondaryText = item.getSeriesName();

                MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playableItem);
            }

            Intent intent = new Intent(MB3Application.getInstance(), PlaybackActivity.class);
            startActivity(intent);
        }
    }
}
