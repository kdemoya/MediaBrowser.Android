package com.mb.android.ui.tv.homescreen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;
import com.jess.ui.TwoWayAdapterView;
import com.jess.ui.TwoWayGridView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.adapters.RecordingsAdapterBackdrops;
import com.mb.android.ui.main.SettingsActivity;
import com.mb.android.adapters.HorizontalAdapterPosters;
import com.mb.android.ui.tv.library.LibraryTools;
import mediabrowser.apiinteraction.Response;
import com.mb.android.logging.FileLogger;
import com.mb.android.ui.tv.MbBackdropActivity;
import com.mb.android.ui.tv.library.dialogs.QuickUserDialogFragment;
import com.mb.android.ui.tv.library.interfaces.IQuickPlayDialogListener;
import com.mb.android.ui.tv.library.dialogs.QuickPlayDialogFragment;
import com.mb.android.ui.tv.mediadetails.TextTabAdapter;
import com.mb.android.ui.tv.playback.PlayerHelpers;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.livetv.RecordingInfoDto;
import mediabrowser.model.livetv.RecordingQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.LatestItemsQuery;
import mediabrowser.model.querying.SessionQuery;
import mediabrowser.model.results.RecordingInfoDtoResult;
import mediabrowser.model.session.SessionInfoDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mark on 11/01/14.
 *
 * This Activity represents the first view that the user sees after logging in. It should provide
 * several ways for the user to consume media that is in their library. Anything from recommended
 * media to recently added and more
 */
public class HomeScreenActivity extends MbBackdropActivity implements IQuickPlayDialogListener {

    private TwoWayGridView collectionsGrid;
    private TwoWayGridView ralGrid;
    private List<BaseItemDto> mItems;
    private boolean mIsFresh = true;
    private PlayerHelpers mPlayHelper;
    private NetworkImageView userImage;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tv_activity_homescreen);
        mBackdropSwitcher = (ViewSwitcher) findViewById(R.id.vsBackdropImages);
        mBackdropImage1 = (NetworkImageView) findViewById(R.id.ivBackdropImage1);
        mBackdropImage2 = (NetworkImageView) findViewById(R.id.ivBackdropImage2);
        collectionsGrid = (TwoWayGridView) findViewById(R.id.gvSectionsGrid);
        ralGrid = (TwoWayGridView) findViewById(R.id.gvRalGrid);
        userImage = (NetworkImageView) findViewById(R.id.btnUser);
        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SessionQuery query = new SessionQuery();
                MB3Application.getInstance().API.GetClientSessionsAsync(query, getSessionsResponse);

            }
        });

        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeScreenActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        mPlayHelper = new PlayerHelpers();
    }

    @Override
    public void onResume() {
        super.onResume();
        setOverscanValues();
        if (MB3Application.getInstance().getIsConnected()) {
            getHomescreenItems();
            if (MB3Application.getInstance().user != null) {
                MB3Application.getInstance().API.GetUserAsync(MB3Application.getInstance().API.getCurrentUserId(), getUserResponse);
            }
        }

    }

    @Override
    public void onDestroy() {
        Log.d("HomeScreenFragment", "onDestroy called");
        super.onDestroy();
    }

    private Response<SessionInfoDto[]> getSessionsResponse = new Response<SessionInfoDto[]>() {

        @Override
        public void onResponse(SessionInfoDto[] sessions) {

            QuickUserDialogFragment quickUserDialogFragment = new QuickUserDialogFragment();

            if (sessions != null) {
                for (SessionInfoDto session : sessions) {
                    if (MB3Application.getInstance().API.getDeviceId().equalsIgnoreCase(session.getDeviceId())) {
                        quickUserDialogFragment.setCurrentSessionInfo(session);
                    }
                }
            }

            quickUserDialogFragment.show(getSupportFragmentManager(), "QuickPlayDialog");
        }
        @Override
        public void onError(Exception ex) {
            QuickUserDialogFragment quickUserDialogFragment = new QuickUserDialogFragment();
            quickUserDialogFragment.show(getSupportFragmentManager(), "QuickPlayDialog");
        }
    };

    private Response<UserDto> getUserResponse = new Response<UserDto>() {
        @Override
        public void onResponse(UserDto user) {
            if (user == null) return;
            MB3Application.getInstance().user = user;
            if (user.getHasPrimaryImage()) {
                ImageOptions options = new ImageOptions();
                options.setImageType(ImageType.Primary);
                options.setMaxHeight(43);
                options.setMaxWidth(43);
                userImage.setImageUrl(
                        MB3Application.getInstance().API.GetUserImageUrl(user, options),
                        MB3Application.getInstance().API.getImageLoader()
                );
            } else {
                userImage.setImageResource(R.drawable.default_user);
            }

        }
    };

    @Override
    public void onConnectionRestored() {
        getHomescreenItems();
    }

    @Override
    protected void onUserDataUpdated(String itemId, UserItemDataDto userItemDataDto) {

    }

    private void getHomescreenItems() {
        if (!mIsFresh) return;
        MB3Application.getInstance().API.GetUserViews(MB3Application.getInstance().user.getId(), getItemsResponse);
        mIsFresh = false;
    }


    @Override
    protected void onPlayButton() {

        if (collectionsGrid == null || mItems == null || mItems.size() <= collectionsGrid.getSelectedItemPosition()) return;

        BaseItemDto selectedItem = mItems.get(collectionsGrid.getSelectedItemPosition());

        if (PlayerHelpers.isCollectionPlayableAsAudio(selectedItem.getCollectionType())) {
            showQuickPlayDialog(selectedItem, true);
        } else if (PlayerHelpers.isCollectionPlayableAsVideo(selectedItem.getCollectionType())) {
            showQuickPlayDialog(selectedItem, false);
        } else {
            Toast.makeText(this, "No Quick-play options for this folder type", Toast.LENGTH_LONG).show();
        }


    }

    @Override
    protected void onFastForwardButton() {

    }

    @Override
    protected void onRewindButton() {

    }

    @Override
    protected void onMenuButton() {

    }

    private Response<ItemsResult> getItemsResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {

            if (response != null && response.getItems() != null && response.getItems().length > 0) {
                mItems = Arrays.asList(response.getItems());

                List<String> sectionNames = new ArrayList<>();
                for (BaseItemDto item : mItems) {
                    sectionNames.add(item.getName());
                }

                collectionsGrid.setAdapter(new TextTabAdapter(HomeScreenActivity.this, sectionNames));
                collectionsGrid.setOnItemClickListener(new TwoWayAdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(TwoWayAdapterView<?> parent, View view, int position, long id) {
                        if (mItems != null && mItems.size() > position) {
                            navigate(mItems.get(position), mItems.get(position).getCollectionType());
                        }
                    }
                });
                collectionsGrid.setOnItemSelectedListener(mOnItemSelectedListener);
                collectionsGrid.requestFocus();
                View v = collectionsGrid.getSelectedView();
                mOnItemSelectedListener.onItemSelected(collectionsGrid, v, 0, collectionsGrid.getAdapter().getItemId(0));

            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };


//    private void GetResumableItems() {
//
//        ItemQuery itemsQuery = new ItemQuery();
//        itemsQuery.UserId = MB3Application.getInstance().Payload.User.Id;
//        itemsQuery.Limit = 8;
//        itemsQuery.Recursive = true;
//        itemsQuery.SortBy = new String[]{ItemSortBy.DateCreated.toString()};
//        itemsQuery.SortOrder = SortOrder.Descending;
//        itemsQuery.Filters = new ItemFilter[]{ItemFilter.IsResumable};
//        itemsQuery.Fields = new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.SortName, ItemFields.DateCreated, ItemFields.Genres};
//        itemsQuery.ExcludeItemTypes = new String[]{"Trailer", "CollectionFolder", "TrailerCollectionFolder", "Season", "Photo"};
//        itemsQuery.ExcludeLocationTypes = new LocationType[]{LocationType.Virtual};
//
//        MB3Application.getInstance().API.GetItemsAsync(itemsQuery, null, new ResumableItemsCallback());
//
//    }
//
//    private class ResumableItemsCallback implements IApiCallback {
//
//        @Override
//        public void Execute(Object data) {
//
//            ApiResponse response = (ApiResponse) data;
//            ItemsResult resumeItems = (ItemsResult) response.data;
//
//            if (resumeItems.Items == null || resumeItems.Items.length == 0) return;
//
//            ItemListWrapper items = new ItemListWrapper();
//            Collections.addAll(items.Items, resumeItems.Items);
//
//            Bundle args = new Bundle();
//            args.putSerializable("Items", items);
//            args.putString("Title", "Resume");
//            args.putBoolean("UseBackdrops", true);
//
//            ActivitySliceFragment fragment = new ActivitySliceFragment();
//            fragment.setArguments(args);
//
//            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
//            ft.add(R.id.flHomeScreenSlice1, fragment, "resumableItems");
//            ft.commit();
//        }
//    }
//
//
//    private void GetFavorites() {
//
//        ItemQuery query = new ItemQuery();
//        query.UserId = MB3Application.getInstance().Payload.User.Id;
//        query.Limit = 8;
//        query.Recursive = true;
//        query.SortBy = new String[]{ItemSortBy.SortName.toString()};
//        query.SortOrder = SortOrder.Descending;
//        query.Filters = new ItemFilter[]{ItemFilter.IsFavorite};
//        query.Fields = new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.ParentId};
//        query.ExcludeItemTypes = new String[]{"CollectionFolder", "TrailerCollectionFolder"};
//
//        MB3Application.getInstance().API.GetItemsAsync(query, null, new FavoritesCallback());
//    }
//
//    private class FavoritesCallback implements IApiCallback {
//
//        @Override
//        public void Execute(Object data) {
//
//            ApiResponse response = (ApiResponse) data;
//            ItemsResult favoriteItems = (ItemsResult) response.data;
//
//            if (favoriteItems.Items == null || favoriteItems.Items.length == 0) return;
//
//            ItemListWrapper items = new ItemListWrapper();
//            Collections.addAll(items.Items, favoriteItems.Items);
//
//            Bundle args = new Bundle();
//            args.putSerializable("Items", items);
//            args.putString("Title", "Favorites");
//
//            ActivitySliceFragment fragment = new ActivitySliceFragment();
//            fragment.setArguments(args);
//
//            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
//            ft.add(R.id.flHomeScreenSlice3, fragment, "favoriteItems");
//            ft.commit();
//        }
//    }
//


    private TwoWayAdapterView.OnItemSelectedListener mOnItemSelectedListener = new TwoWayAdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(TwoWayAdapterView<?> parent, View view, int position, long id) {

            ((TextTabAdapter) collectionsGrid.getAdapter()).setSelectedIndex(position);
            if (mItems == null || mItems.size() <= position) {
                onNothingSelected(parent);
                return;
            }
            ralGrid.setAdapter(null);
            setBackdropsForItem(mItems.get(position));
            if ("livetv".equalsIgnoreCase(mItems.get(position).getCollectionType())) {
                getRecentRecordings();
            } else {
                getRecentlyAddedForItem(mItems.get(position));
            }
//            getNextUpForItem(mItems.get(position));
        }

        @Override
        public void onNothingSelected(TwoWayAdapterView<?> parent) {
            setBackdropToDefaultImage();
        }
    };


    private void setBackdropsForItem(BaseItemDto item) {
        if (item.getBackdropCount() > 0) {
            List<String> imageUrls = new ArrayList<>();

            for (int j = 0; j < item.getBackdropCount(); j++) {

                ImageOptions options = new ImageOptions();
                options.setImageType(ImageType.Backdrop);
                options.setImageIndex(j);
                options.setMaxHeight(720);

                String imageUrl = MB3Application.getInstance().API.GetImageUrl(item.getId(), options);
                imageUrls.add(imageUrl);
            }

            setBackdropImages(imageUrls);
        } else {
            setBackdropToDefaultImage();
        }
    }

    private void setBackdropForItem(BaseItemDto item) {

        String backdropItemId = null;
        if (item.getBackdropCount() > 0) {
            backdropItemId = item.getId();
        } else if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getParentBackdropItemId())) {
            backdropItemId = item.getParentBackdropItemId();
        }

        if (backdropItemId != null) {
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Backdrop);
            options.setMaxHeight(720);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(backdropItemId, options);
            List<String> imageUrls = new ArrayList<>();
            imageUrls.add(imageUrl);
            setBackdropImages(imageUrls);
        }
    }

    private void getRecentlyAddedForItem(BaseItemDto item) {
        LatestItemsQuery recentItemsQuery = new LatestItemsQuery();
        recentItemsQuery.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        recentItemsQuery.setParentId(item.getId());
        recentItemsQuery.setLimit(10);
        recentItemsQuery.setIsPlayed(false);
        recentItemsQuery.setGroupItems(true);
        recentItemsQuery.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.ParentId});
        MB3Application.getInstance().API.GetLatestItems(recentItemsQuery, recentItemsCallback);
    }

    private Response<BaseItemDto[]> recentItemsCallback = new Response<BaseItemDto[]>() {

        @Override
        public void onResponse(BaseItemDto[] result) {
            if (result == null || result.length == 0) return;

            ArrayList<BaseItemDto> itemsList = new ArrayList<>();
            Collections.addAll(itemsList, result);
//            DisplayMetrics dm = getResources().getDisplayMetrics();
//            int rows = 2;
//            int rowHeight = (int)(ralGrid.getHeight() - (rows * (dm.density * 4))) / rows;
            int defaultImageId = itemsList.get(0) != null ? LibraryTools.getDefaultImageIdFromType(itemsList.get(0).getType(), 0.6) : R.drawable.default_video_portrait;
            ralGrid.setAdapter(new HorizontalAdapterPosters(itemsList, ralGrid.getHeight(), 1, defaultImageId ));
            ralGrid.setOnItemSelectedListener(onTileSelected);
            ralGrid.setOnItemClickListener(onTileClick);
        }
    };

    private void getRecentRecordings() {
        RecordingQuery query = new RecordingQuery();
        query.setLimit(10);
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        MB3Application.getInstance().API.GetLiveTvRecordingsAsync(query, new GetRecordingsResponse());
    }

    private class GetRecordingsResponse extends Response<RecordingInfoDtoResult> {
        @Override
        public void onResponse(final RecordingInfoDtoResult result) {

            if (result == null) {
                return;
            }

            if (result.getItems() == null || result.getItems().length == 0) {
                FileLogger.getFileLogger().Debug("No recordings returned");
            } else {
                ralGrid.setAdapter(new RecordingsAdapterBackdrops(Arrays.asList(result.getItems())));
                ralGrid.setOnItemSelectedListener(onTileSelected);
                ralGrid.setOnItemClickListener(onTileClick);
            }
        }
    }

//    private void getNextUpForItem(BaseItemDto item) {
//        if (!"tvshows".equalsIgnoreCase(item.getCollectionType())) {
//            upNextGrid.setVisibility(View.GONE);
//            return;
//        }
//        upNextGrid.setVisibility(View.VISIBLE);
//        NextUpQuery query = new NextUpQuery();
//        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
//        query.setParentId(item.getId());
//        query.setLimit(8);
//
//        MB3Application.getInstance().API.GetNextUpEpisodesAsync(query, nextUpResponse);
//    }
//
//    private Response<ItemsResult> nextUpResponse = new Response<ItemsResult>() {
//
//        @Override
//        public void onResponse(ItemsResult result) {
//            if (result == null || result.getItems() == null) return;
//            ArrayList<BaseItemDto> items = new ArrayList<>();
//            Collections.addAll(items, result.getItems());
////            upNextGrid.setAdapter(new NextUpCardAdapter(result.getItems(), HomeScreenActivity.this));
//            upNextGrid.setAdapter(new HorizontalAdapterBackdrops(HomeScreenActivity.this, items, upNextGrid.getHeight(), 1, R.drawable.default_video_landscape));
//            upNextGrid.setOnItemSelectedListener(onTileSelected);
//            upNextGrid.setOnItemClickListener(onTileClick);
//        }
//    };

    private TwoWayGridView.OnItemClickListener onTileClick = new TwoWayGridView.OnItemClickListener() {

        @Override
        public void onItemClick(TwoWayAdapterView<?> parent, View view, int position, long id) {
            if (parent.getItemAtPosition(position) instanceof BaseItemDto) {
                navigate((BaseItemDto) parent.getItemAtPosition(position), ((BaseItemDto) parent.getItemAtPosition(position)).getCollectionType());
            } else {
                navigate((RecordingInfoDto) parent.getItemAtPosition(position));
            }
        }
    };


    TwoWayGridView.OnItemSelectedListener onTileSelected = new TwoWayGridView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(TwoWayAdapterView<?> parent, View view, int position, long id) {
            if (parent.getSelectedItem() instanceof BaseItemDto) {
                setBackdropForItem((BaseItemDto) parent.getSelectedItem());
            }
        }

        @Override
        public void onNothingSelected(TwoWayAdapterView<?> parent) {
        }
    };


    //******************************************************************************************************************
    // Quick play dialog methods
    //******************************************************************************************************************

    private void showQuickPlayDialog(BaseItemDto item, boolean isAudio) {

        QuickPlayDialogFragment quickPlayDialogFragment = new QuickPlayDialogFragment();
        quickPlayDialogFragment.setData(item, isAudio);
        quickPlayDialogFragment.show(getSupportFragmentManager(), "QuickPlayDialog");
    }

    @Override
    public void onQuickPlaySelectionFinished() {
        FileLogger.getFileLogger().Info("HomeScreenActivity: onQuickPlaySelectionFinished");
        mPlayHelper.playItems(this);
    }
}
