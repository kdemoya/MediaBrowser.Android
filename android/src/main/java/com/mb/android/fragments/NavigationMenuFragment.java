package com.mb.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.MenuEntity;
import com.mb.android.Playlist;
import com.mb.android.R;
import com.mb.android.activities.mobile.ChannelsActivity;
import com.mb.android.ui.mobile.homescreen.HomescreenActivity;
import com.mb.android.ui.mobile.library.LibraryPresentationActivity;
import com.mb.android.activities.mobile.PlaylistActivity;
import com.mb.android.adapters.ViewsAdapter;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import com.mb.android.logging.FileLogger;
import com.mb.android.player.AudioService;
import com.mb.android.ui.mobile.livetv.LiveTvActivity;
import com.mb.android.ui.main.ConnectionActivity;
import com.mb.android.ui.mobile.music.MusicActivity;
import mediabrowser.model.channels.ChannelQuery;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.livetv.LiveTvInfo;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mark on 2014-08-09.
 *
 * Fragment that shows the navigation menu on every activity when the user presses the app icon
 */
public class NavigationMenuFragment extends Fragment {

    private static final String TAG = "Navigation Menu";
    private ListView mDrawerList;
    private NetworkImageView mAvatar;
    private TextView mUsername;
    private RelativeLayout mUserContainer;
    private DrawerLayout mDrawerLayout;
    private boolean liveTvEnabled = false;
    private boolean mChannelsEnabled = false;


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_navigation_menu, container, false);
        mDrawerList = (ListView) view.findViewById(R.id.lvMainMenu);
        mAvatar = (NetworkImageView) view.findViewById(R.id.ivUserAvatar);
        mUsername = (TextView) view.findViewById(R.id.tvUserName);
        mUserContainer = (RelativeLayout) view.findViewById(R.id.rlUserContainer);

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();

        // Don't start making api requests unless we have valid connection details
        if (ApiProperlyConfigured()) {
            if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(MB3Application.getInstance().user.getName())) {
                MB3Application.getInstance().API.GetUserAsync(MB3Application.getInstance().API.getCurrentUserId(), getUserResponse);
            } else {
                buildUserButton();
            }
            MB3Application.getInstance().API.GetLiveTvInfoAsync(liveTvInfoResponse);
        }
    }


    public void setDrawerLayout(DrawerLayout drawerLayout) {
        mDrawerLayout = drawerLayout;
    }


    private boolean ApiProperlyConfigured() {
        return MB3Application.getInstance().API != null
                && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(MB3Application.getInstance().API.getServerAddress())
                && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(MB3Application.getInstance().API.getCurrentUserId());
    }


    private Response<UserDto> getUserResponse = new Response<UserDto>() {
        @Override
        public void onResponse(UserDto result) {
            if (result == null) return;
            MB3Application.getInstance().user = result;
            buildUserButton();
        }
    };


    private void buildUserButton() {

        DisplayMetrics metrics = MB3Application.getInstance().getResources().getDisplayMetrics();

        if (MB3Application.getInstance().user != null) {
            if (MB3Application.getInstance().user.getHasPrimaryImage()) {

                ImageOptions options = new ImageOptions();
                options.setImageType(ImageType.Primary);
                options.setHeight((int) (50 * metrics.density));
                options.setWidth((int) (50 * metrics.density));
                options.setCropWhitespace(true);

                String imageUrl = MB3Application.getInstance().API.GetUserImageUrl(MB3Application.getInstance().user, options);
                mAvatar.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            }

            mUsername.setText(MB3Application.getInstance().user.getName());
        }

        mUserContainer.setOnClickListener(onUserClick);
    }


    private View.OnClickListener onUserClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            closeDrawer();
            terminateQueuedMedia();
            if (MB3Application.getInstance().API != null) {
                MB3Application.getInstance().API.Logout(new EmptyResponse());
            }
            showUserSelection();
        }
    };


    private void closeDrawer() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.START)) {
            mDrawerLayout.closeDrawer(Gravity.START);
        }
    }


    private void terminateQueuedMedia() {
        MB3Application.getInstance().PlayerQueue = new Playlist();
        AudioService.PlayerState currentState = MB3Application.getAudioService().getPlayerState();
        if (currentState.equals(AudioService.PlayerState.PLAYING) || currentState.equals(AudioService.PlayerState.PAUSED)) {
            MB3Application.getAudioService().stopMedia();
        }
    }


    private void showUserSelection() {
        Intent intent = new Intent(MB3Application.getInstance(), ConnectionActivity.class);
        intent.putExtra("show_users", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    private void buildNavigationMenu(BaseItemDto[] rootFolders) {

        final List<MenuEntity> menu = new ArrayList<>();

        MenuEntity homeMenuItem = new MenuEntity();
        homeMenuItem.CollectionType = "root";
        homeMenuItem.Name = MB3Application.getInstance().getResources().getString(R.string.menu_home);

        menu.add(homeMenuItem);

        if (queueHasItems()) {
            MenuEntity queueMenuItem = new MenuEntity();
            queueMenuItem.CollectionType = "queue";
            queueMenuItem.Name = MB3Application.getInstance().getResources().getString(R.string.queue_string);
            menu.add(queueMenuItem);
        }

        if (rootFolders != null) {
            for (BaseItemDto item : rootFolders) {

                MenuEntity menuItem = new MenuEntity();
                menuItem.CollectionType = item.getCollectionType();
                menuItem.Id = item.getId();
                menuItem.Name = item.getName();

                menu.add(menuItem);
            }
        }

        if (liveTvEnabled) {
            MenuEntity menuItem = new MenuEntity();
            menuItem.CollectionType = "livetv";
            menuItem.Name = MB3Application.getInstance().getResources().getString(R.string.live_tv_header);

            menu.add(menuItem);
        }

        if (mChannelsEnabled) {
            MenuEntity menuItem = new MenuEntity();
            menuItem.CollectionType = "channels";
            menuItem.Name = MB3Application.getInstance().getResources().getString(R.string.channels_header);

            menu.add(menuItem);
        }

        mDrawerList.setAdapter(new ViewsAdapter(menu));
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int index, long id) {

                if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.START)) {
                    mDrawerLayout.closeDrawer(Gravity.START);
                }

                if (index == 0) {
                    Intent intent = new Intent(MB3Application.getInstance(), HomescreenActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.finish();
                    }
                } else if (menu.get(index).CollectionType != null && menu.get(index).CollectionType.equalsIgnoreCase("queue")) {
                    Intent intent = new Intent(MB3Application.getInstance(), PlaylistActivity.class);
                    startActivity(intent);
                } else if (menu.get(index).CollectionType != null && menu.get(index).CollectionType.equalsIgnoreCase("channels")) {
                    Intent intent = new Intent(MB3Application.getInstance(), ChannelsActivity.class);
                    startActivity(intent);
                } else if (menu.get(index).CollectionType != null && menu.get(index).CollectionType.equalsIgnoreCase("livetv")) {
                    Intent intent = new Intent(MB3Application.getInstance(), LiveTvActivity.class);
                    startActivity(intent);
                } else if (menu.get(index).CollectionType != null && menu.get(index).CollectionType.equalsIgnoreCase("music")) {
                    Intent intent = new Intent(MB3Application.getInstance(), MusicActivity.class);
                    intent.putExtra("ParentId", menu.get(index).Id);
                    startActivity(intent);
                } else {
                    ItemQuery query = new ItemQuery();
                    query.setParentId(menu.get(index).Id);
                    query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
                    query.setSortBy(new String[]{ItemSortBy.SortName});
                    query.setSortOrder(SortOrder.Ascending);
                    query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.ParentId, ItemFields.SortName});
                    query.setLimit(200);

                    if (MB3Application.getInstance().user != null
                            && MB3Application.getInstance().user.getConfiguration() != null) {
                        if (MB3Application.getInstance().user.getConfiguration().getGroupMoviesIntoBoxSets())
                            query.setCollapseBoxSetItems(true);
                    }

                    String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(query);

                    Intent intent = new Intent(MB3Application.getInstance(), LibraryPresentationActivity.class);
                    intent.putExtra("ItemQuery", jsonData);
                    FileLogger.getFileLogger().Info("Starting Library Presentation Activity");
                    startActivity(intent);
                }
            }
        });

    }


    private boolean queueHasItems() {
        return MB3Application.getInstance().PlayerQueue != null
                && MB3Application.getInstance().PlayerQueue.PlaylistItems != null
                && MB3Application.getInstance().PlayerQueue.PlaylistItems.size() > 0;
    }


    private Response<LiveTvInfo> liveTvInfoResponse = new Response<LiveTvInfo>() {
        @Override
        public void onResponse(LiveTvInfo liveTvInfo) {
            FileLogger.getFileLogger().Info(TAG + ": Live TV info response received");

            if (liveTvInfo != null && liveTvInfo.getIsEnabled() && liveTvInfo.getEnabledUsers() != null) {
                for (String userId : liveTvInfo.getEnabledUsers()) {
                    if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(userId) && userId.equalsIgnoreCase(MB3Application.getInstance().API.getCurrentUserId())) {
                        liveTvEnabled = true;
                    }
                }
            }

            requestChannels();
        }
        @Override
        public void onError(Exception ex) {
            FileLogger.getFileLogger().Error(TAG + ": Error retrieving Live TV info");
            requestChannels();
        }
    };


    private void requestChannels() {
        ChannelQuery query = new ChannelQuery();
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());

        MB3Application.getInstance().API.GetChannels(query, getChannelsResponse);
    }


    private Response<ItemsResult> getChannelsResponse = new Response<ItemsResult>() {
        @Override
        public void onResponse(ItemsResult response) {
            FileLogger.getFileLogger().Info(TAG + ": Channel info response received");

//            if (response != null && response.getItems() != null && response.getItems().length > 0) {
            if (response != null && response.getTotalRecordCount() > 0) {
                FileLogger.getFileLogger().Info(TAG + ": Channels available");
                mChannelsEnabled = true;
            }

            getLibraryRoot();
        }
        @Override
        public void onError(Exception ex) {
            FileLogger.getFileLogger().Error(TAG + ": Error receiving Channel info");
            getLibraryRoot();
        }
    };


    private void getLibraryRoot() {
        ItemQuery query = new ItemQuery();
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setSortBy(new String[]{ItemSortBy.SortName});
        query.setSortOrder(SortOrder.Ascending);
        query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});

        MB3Application.getInstance().API.GetItemsAsync(query, getLibraryResponse);
    }


    Response<ItemsResult> getLibraryResponse = new Response<ItemsResult>() {
        @Override
        public void onResponse(ItemsResult response) {
            FileLogger.getFileLogger().Info(TAG + ": Get library response received");

            if (response != null && response.getTotalRecordCount() > 0) {
                FileLogger.getFileLogger().Info(TAG + ": library contents available");
                buildNavigationMenu(response.getItems());
            } else {
                FileLogger.getFileLogger().Info(TAG + ": no library contents in response");
                buildNavigationMenu(null);
            }
        }
        @Override
        public void onError(Exception ex) {
            FileLogger.getFileLogger().Error(TAG + ": Error getting library contents");
            buildNavigationMenu(null);
        }
    };
}
