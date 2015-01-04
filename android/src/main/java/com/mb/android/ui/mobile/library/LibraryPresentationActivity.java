package com.mb.android.ui.mobile.library;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mb.android.activities.BaseMbMobileActivity;
import mediabrowser.apiinteraction.Response;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.MB3Application;
import com.mb.android.Playlist;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.fragments.NavigationMenuFragment;
import com.mb.android.player.AudioService;
import com.mb.android.ui.mobile.playback.AudioPlaybackActivity;
import com.mb.android.ui.mobile.playback.PlaybackActivity;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.EpisodeQuery;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemsByNameQuery;
import mediabrowser.model.entities.ParentalRating;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.library.PlayAccess;
import mediabrowser.model.entities.SortOrder;
import com.mb.android.logging.FileLogger;
import mediabrowser.model.session.PlayCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * This Activity typically displays a grid representation of a users library contents.
 */
public class LibraryPresentationActivity extends BaseMbMobileActivity {

    private static final String TAG = "LibraryPresentationActivity";
    private ActionBarDrawerToggle mDrawerToggle;
    private BaseItemDto mItem;
    private DrawerLayout mDrawerLayout;
    private LibraryPresentationFragment mLibraryView;
    private ListView mDrawerList;
    private List<String> mInitialGenreList;
    private List<String> mSelectedGenres;
    private int mCurrentSortIndex;
    private boolean mSortAscending = true;
    private List<String> mSortOptions;
    private ItemQuery mItemQuery;
    private EpisodeQuery mEpisodeQuery;
    private List<Integer> mYears;
    private List<Integer> mSelectedYears;
    private boolean mFilterUnPlayed;
    private List<String> mOfficialRatings;
    private int mSelectedOfficialRatingIndex;
    private boolean mDisableIndexing;
    private boolean mShowPlayControls;
    private boolean mIsFresh = true;
    private Bundle mSavedInstanceState;

    public void SetPlayControlsEnabled(boolean isEnabled) {
        mShowPlayControls = isEnabled;
        this.invalidateOptionsMenu();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSavedInstanceState = savedInstanceState;

        FileLogger.getFileLogger().Info(TAG + ": onCreate");
        setContentView(R.layout.activity_library);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawer.setFocusableInTouchMode(false);

        NavigationMenuFragment fragment = (NavigationMenuFragment) getSupportFragmentManager().findFragmentById(R.id.left_drawer);
        if (fragment != null && fragment.isInLayout()) {
            fragment.setDrawerLayout(drawer);
        }

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                drawer,
                R.string.abc_action_bar_home_description,
                R.string.abc_action_bar_up_description) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
//                getActionBar().setTitle(mTitle);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
//                getActionBar().setTitle(mDrawerTitle);
            }
        };

        drawer.setDrawerListener(mDrawerToggle);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.sort_filter_drawer);
        mDrawerList = (ListView) findViewById(R.id.right_drawer);
        mDisableIndexing = getMb3Intent().getBooleanExtra("DisableIndexing", false);
        String jsonData = getMb3Intent().getStringExtra("EpisodeQuery");
        if (jsonData != null) {
            mEpisodeQuery = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, EpisodeQuery.class);
        } else {
            jsonData = getMb3Intent().getStringExtra("ItemQuery");
            if (jsonData != null) {
                mItemQuery = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, ItemQuery.class);
            } else {
                jsonData = getMb3Intent().getStringExtra("Item");
                if (jsonData != null) {
                    mItem = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, BaseItemDto.class);
                }
            }
        }

        mMini = (MiniController) findViewById(R.id.miniController1);
        mCastManager.addMiniController(mMini);
        FileLogger.getFileLogger().Info(TAG + ": finish onCreate");
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (mShowPlayControls) {
            menu.add(getResources().getString(R.string.play_all_action_bar_button)).setIcon(R.drawable.play).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.add(getResources().getString(R.string.shuffle_action_bar_button)).setIcon(R.drawable.shuffle).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        if (mEpisodeQuery == null) {
            menu.add("filter/sort").setIcon(R.drawable.filter).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }


        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        /*
        Play All
         */
        if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.play_all_action_bar_button))) {

            FileLogger.getFileLogger().Info("Library Presentation Fragment: play all clicked");
            handlePlayRequest(false);

        /*
        Shuffle
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.shuffle_action_bar_button))) {

            FileLogger.getFileLogger().Info("Library Presentation Fragment: shuffle all clicked");
            handlePlayRequest(true);

        /*
        Filter/Sort
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase("filter/sort")) {
            if (mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
                mDrawerLayout.closeDrawer(Gravity.RIGHT);
            } else {
                mDrawerLayout.openDrawer(Gravity.RIGHT);
            }
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }


    private void handlePlayRequest(boolean shuffle) {

        AudioService.PlayerState currentState = MB3Application.getAudioService().getPlayerState();
        if (currentState.equals(AudioService.PlayerState.PLAYING) || currentState.equals(AudioService.PlayerState.PAUSED)) {
            MB3Application.getAudioService().stopMedia();
        }
        MB3Application.getInstance().StopMedia();

        MB3Application.getInstance().PlayerQueue.PlaylistItems = new ArrayList<>();

        if (mItem != null && mItem.getType().equalsIgnoreCase("playlist")) {
            if (mCastManager != null && mCastManager.isConnected()) {
                mCastManager.playItem(mItem, shuffle ? PlayCommand.PlayShuffle : PlayCommand.PlayNow, 0L);
            } else {
                if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mItem.getMediaType())) {
                    if (mItem.getMediaType().equalsIgnoreCase("audio")) {
                        getAudioPlaylistItems(shuffle);
                    } else if (mItem.getMediaType().equalsIgnoreCase("video")) {
                        getVideoPlaylistItems(shuffle);
                    } else {
                        Toast.makeText(this, mItem.getMediaType() + " playlists not supported", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "Could not determine Playlist type ", Toast.LENGTH_LONG).show();
                }
            }
        } else if (mEpisodeQuery != null) {
            EpisodeQuery query = new EpisodeQuery();
            query.setFields(mEpisodeQuery.getFields());
            query.setIsMissing(false);
            query.setIsVirtualUnaired(false);
            query.setSeasonId(mEpisodeQuery.getSeasonId());
            query.setSeasonNumber(mEpisodeQuery.getSeasonNumber());
            query.setSeriesId(mEpisodeQuery.getSeriesId());
            query.setUserId(mEpisodeQuery.getUserId());
            MB3Application.getInstance().API.GetEpisodesAsync(query, new PlayAllShuffleResponse(shuffle));
        } else {
            ItemQuery query = new ItemQuery();
            query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
            if (mItem != null) {
                query.setParentId(mItem.getId());
                query.setIncludeItemTypes(new String[]{"Audio", "Movie", "Episode", "MusicVideo"});
            } else {
                // TODO Fix this mess
//                if (mItemsCatagory.equalsIgnoreCase("games")) {
//                    FileLogger.getFileLogger().Info("Games are not yet supported");
//                } else if (mMediaWrapper.ItemsCatagory.equalsIgnoreCase("movies")) {
//                    query.setIncludeItemTypes(new String[]{"Movie"});
//                } else if (mMediaWrapper.ItemsCatagory.equalsIgnoreCase("music")) {
//                    query.setIncludeItemTypes(new String[]{"Audio"});
//                } else {
//                    query.setIncludeItemTypes(new String[]{"Episode"});
//                }
            }
            query.setSortBy(new String[]{ItemSortBy.SortName});
            query.setSortOrder(SortOrder.Ascending);
            query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.SortName, ItemFields.DateCreated});
            query.setRecursive(true);
            query.setIsMissing(false);
            query.setIsVirtualUnaired(false);

            MB3Application.getInstance().API.GetItemsAsync(query, new PlayAllShuffleResponse(shuffle));
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        FileLogger.getFileLogger().Info(TAG + ": onResume");
        if (MB3Application.getInstance().getIsConnected()) {
            FileLogger.getFileLogger().Info(TAG + ": is Connected");
            if (mSavedInstanceState == null) {
                if (mIsFresh) {
                    performUiSetup();
                    setContent();
                    mIsFresh = false;
                }
            } else {
                FileLogger.getFileLogger().Info(TAG + ": re-acquiring content fragment");
                mLibraryView = (LibraryPresentationFragment) getSupportFragmentManager().findFragmentByTag("library");
            }
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        try {
            mMini.removeOnMiniControllerChangedListener(mCastManager);
        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("Error handled removing Mini Controller changed listener ", e);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        FileLogger.getFileLogger().Info("Library Presentation Activity: onDestroy");
        Log.i("LibraryPresentationFragment", "onDestroy");
    }

    @Override
    protected void onConnectionRestored() {
        FileLogger.getFileLogger().Info(TAG + ": onConnection restored");
        performUiSetup();
        if (mSavedInstanceState == null) {
            setContent();
        } else {
            FileLogger.getFileLogger().Info(TAG + ": re-acquiring content fragment");
            mLibraryView = (LibraryPresentationFragment) getSupportFragmentManager().findFragmentByTag("library");
        }
        // Notify the fragment of the change
        if (mLibraryView != null) {
            mLibraryView.onConnectionRestored();
        }
    }

    private void performUiSetup() {

        FileLogger.getFileLogger().Info(TAG + ": parsing initial query information");
        if (MB3Application.getInstance().ParentalRatings.size() == 0) {
            MB3Application.getInstance().API.GetParentalRatingsAsync(getParentalRatingsResponse);
        }

        if (mItem != null) {
            mShowPlayControls = mItem.getPlayAccess().equals(PlayAccess.Full);
        }

        if (mEpisodeQuery == null && mItemQuery == null && mItem != null) {

            mItemQuery = new ItemQuery();
            mItemQuery.setUserId(MB3Application.getInstance().API.getCurrentUserId());
            mItemQuery.setParentId(mItem.getId());
            mItemQuery.setSortBy(new String[]{ItemSortBy.SortName});
            mItemQuery.setSortOrder(SortOrder.Ascending);
            mItemQuery.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.SortName, ItemFields.DateCreated, ItemFields.Genres});
            mItemQuery.setLimit(200);

            if (MB3Application.getInstance().user != null && MB3Application.getInstance().user.getConfiguration() != null) {
                if (MB3Application.getInstance().user.getConfiguration().getDisplayMissingEpisodes())
                    mItemQuery.setIsMissing(true);

                if (MB3Application.getInstance().user.getConfiguration().getDisplayUnairedEpisodes())
                    mItemQuery.setIsVirtualUnaired(true);

                if (MB3Application.getInstance().user.getConfiguration().getGroupMoviesIntoBoxSets())
                    mItemQuery.setCollapseBoxSetItems(true);
            }
        }

        if (mItemQuery != null) {
            mItemQuery = mItemQuery;
        } else if (mEpisodeQuery != null) {
            mEpisodeQuery = mEpisodeQuery;
        }  else {
            FileLogger.getFileLogger().Error("LibraryPresentationActivity: Nothing to show");
            return;
        }

        mSortOptions = new ArrayList<>();
        mSortOptions.add("Filter By");
        mSortOptions.add("Genre");
        mSortOptions.add("Year");
        mSortOptions.add("Official Rating");
        mSortOptions.add("Un-played");
        mSortOptions.add("Sort By");
        mSortOptions.add("Name");
        mSortOptions.add("Community Rating");
        mSortOptions.add("Content Rating");
        mSortOptions.add("Date Added");
        mSortOptions.add("Date Played");
        mSortOptions.add("Date Released");
        mSortOptions.add("Runtime");
        mSortOptions.add("Direction");
        mSortOptions.add("Ascending");
        mSortOptions.add("Descending");

        mCurrentSortIndex = mSortOptions.indexOf("Name");

        mDrawerList.setAdapter(new NavigationDrawerAdapter(LibraryPresentationActivity.this));
        mDrawerList.setOnItemClickListener(new DrawerSortItemClickListener());

        if (mEpisodeQuery != null)
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
            mDrawerLayout.closeDrawer(Gravity.RIGHT);
        } else {
            super.onBackPressed();
        }
    }


    private void setContent() {
        FileLogger.getFileLogger().Info(TAG + ": building content fragment");
        mLibraryView = new LibraryPresentationFragment();

        Bundle bundle = new Bundle();
        if (mItem != null) {
            String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(mItem);
            bundle.putSerializable("Item", jsonData);
        }
        if (mItemQuery != null) {
            String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(mItemQuery);
            bundle.putSerializable("ItemQuery", jsonData);
        }
        if (mEpisodeQuery != null) {
            String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(mEpisodeQuery);
            bundle.putSerializable("EpisodeQuery", jsonData);
        }
        bundle.putBoolean("DisableIndexing", mDisableIndexing);
        mLibraryView.setArguments(bundle);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, mLibraryView, "library")
                .commit();
    }


    private void requestGenres() {

        ItemsByNameQuery ibnQuery = new ItemsByNameQuery();
        if (mItemQuery.getParentId() != null && !mItemQuery.getParentId().isEmpty())
            ibnQuery.setParentId(mItemQuery.getParentId());
        else {
            ibnQuery.setIncludeItemTypes(mItemQuery.getIncludeItemTypes());
            ibnQuery.setRecursive(true);
        }
        ibnQuery.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        ibnQuery.setSortOrder(SortOrder.Ascending);
        ibnQuery.setSortBy(new String[]{"SortName"});

        MB3Application.getInstance().API.GetGenresAsync(ibnQuery, getGenresResponse);
    }

    private List<String> SortOfficialRatings(List<String> mOfficialRatings) {

        List<String> sortedList = new ArrayList<>();

        FileLogger.getFileLogger().Info("Begin Sorting Ratings");
        for (String rating : mOfficialRatings) {
            if (sortedList.size() == 0) {
                FileLogger.getFileLogger().Info("Add the initial rating");
                sortedList.add(rating);
                continue;
            }

            ParentalRating comparingRating = null;
            for (ParentalRating r : MB3Application.getInstance().ParentalRatings) {
                if (r.getName().equalsIgnoreCase(rating)) {
                    FileLogger.getFileLogger().Info("Matched: Setting comparingRating");
                    comparingRating = r;
                    break;
                }
            }

            if (comparingRating == null) continue;

            // Get the rating value of the rating at the specific index. If the rating is higher,
            // insert the new entity before it. Otherwise check the next index.
            for (int i = 0; i < sortedList.size(); i++) {

                // if were at the end of the list then there is no more rating to check. Add it
                if (i == sortedList.size() - 1) {
                    sortedList.add(rating);
                    break;
                }

                ParentalRating sortedComparingRating = null;
                for (ParentalRating r : MB3Application.getInstance().ParentalRatings) {
                    if (r.getName().equalsIgnoreCase(sortedList.get(i))) {
                        sortedComparingRating = r;
                        break;
                    }
                }

                if (sortedComparingRating == null) continue;

                if (sortedComparingRating.getValue() > comparingRating.getValue()) {
                    sortedList.add(i, rating);
                    break;
                }
            }
        }

        return sortedList;
    }

    private Response<ItemsResult> getGenresResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {

            mInitialGenreList = new ArrayList<>();
            mInitialGenreList.add("Filter: Genre");
            mInitialGenreList.add("-- BACK --");

            for (BaseItemDto i : response.getItems()) {
                if (i.getName() != null && !i.getName().isEmpty())
                    mInitialGenreList.add(i.getName());
            }

            mDrawerList.setAdapter(new NavigationDrawerGenreAdapter(LibraryPresentationActivity.this));
            mDrawerList.setOnItemClickListener(new DrawerGenreItemClickListener());
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    public class NavigationDrawerAdapter extends BaseAdapter {

        Context mContext;
        LayoutInflater mLayoutInflater;

        public NavigationDrawerAdapter(Context context) {

            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mSortOptions.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            if (view == null) {
                view = mLayoutInflater.inflate(R.layout.widget_navigation_drawer_clickable_item, null);
            }

            if (view == null) return null;

            TextView tv = (TextView) view.findViewById(R.id.tvClickableItem);
            tv.setText(mSortOptions.get(i));

            if (mSortOptions.get(i).equalsIgnoreCase("Filter By") || mSortOptions.get(i).equalsIgnoreCase("Sort By") || mSortOptions.get(i).equalsIgnoreCase("Direction")) {
                tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                tv.setGravity(Gravity.CENTER);
                tv.setTextColor(Color.parseColor("#00b4ff"));
                tv.setBackgroundColor(Color.parseColor("#30bbbbbb"));
                tv.setPadding(0, 10, 0, 10);

            } else {

                tv.setBackgroundColor(Color.TRANSPARENT);
                tv.setGravity(Gravity.NO_GRAVITY);
                tv.setPadding(10, 0, 0, 0);

                if (i == mCurrentSortIndex ||
                        (mSortOptions.get(i).equalsIgnoreCase("Ascending") && mSortAscending) ||
                        (mSortOptions.get(i).equalsIgnoreCase("Descending") && !mSortAscending) ||
                        (mSortOptions.get(i).equalsIgnoreCase("Un-played") && mFilterUnPlayed)) {
                    tv.setTextColor(Color.parseColor("#00b4ff"));
                    tv.setTextSize(20);
                } else {
                    tv.setTextColor(Color.WHITE);
                    tv.setTextSize(18);
                }

            }


            return view;
        }
    }

    /**
     * Called when the drawer is showing the sort/filter root menu and an item is clicked. Builds a new
     * ItemQuery and updates the selected items in the drawer
     */
    private class DrawerSortItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

            if (mCurrentSortIndex != position && mSortOptions.get(position).equalsIgnoreCase("Genre")) {

                requestGenres();

            } else if (mCurrentSortIndex != position && mSortOptions.get(position).equalsIgnoreCase("Year")) {

                mYears = mLibraryView.GetAvailableYears();

                if (mYears != null && mYears.size() > 0) {
                    mDrawerList.setAdapter(new NavigationDrawerYearAdapter(LibraryPresentationActivity.this));
                    mDrawerList.setOnItemClickListener(new DrawerYearItemClickListener());
                }

            } else if (mCurrentSortIndex != position && mSortOptions.get(position).equalsIgnoreCase("Official Rating")) {

                mOfficialRatings = mLibraryView.GetAvailableOfficialRatings();

                if (mOfficialRatings != null && mOfficialRatings.size() > 0) {
                    // Sort by rating first
                    mOfficialRatings = SortOfficialRatings(mOfficialRatings);

                    mOfficialRatings.add(0, "Filter: Official Rating");
                    mOfficialRatings.add(1, "-- BACK --");
                    mDrawerList.setAdapter(new NavigationDrawerOfficialRatingAdapter(LibraryPresentationActivity.this));
                    mDrawerList.setOnItemClickListener(new DrawerOfficialRatingItemClickListener());
                }

            } else if (mCurrentSortIndex != position && mSortOptions.get(position).equalsIgnoreCase("Un-played")) {

                if (!mFilterUnPlayed) {
                    mItemQuery.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
                    mFilterUnPlayed = true;
                } else {
                    mItemQuery.setFilters(null);
                    mFilterUnPlayed = false;
                }

                mLibraryView.PerformQuery(mItemQuery);
                ((NavigationDrawerAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();

            } else if (mCurrentSortIndex != position && (position != 0 && position != mSortOptions.size() - 2 && position != mSortOptions.size() - 1)) {

                mCurrentSortIndex = position;
                ((NavigationDrawerAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();

                if (mSortOptions.get(position).equalsIgnoreCase("Date Added"))
                    mItemQuery.setSortBy(new String[]{ItemSortBy.DateCreated.toString()});
                else if (mSortOptions.get(position).equalsIgnoreCase("Date Released"))
                    mItemQuery.setSortBy(new String[]{ItemSortBy.PremiereDate.toString()});
                else if (mSortOptions.get(position).equalsIgnoreCase("Community Rating"))
                    mItemQuery.setSortBy(new String[]{ItemSortBy.CommunityRating.toString()});
                else if (mSortOptions.get(position).equalsIgnoreCase("Content Rating"))
                    mItemQuery.setSortBy(new String[]{ItemSortBy.OfficialRating.toString()});
                else if (mSortOptions.get(position).equalsIgnoreCase("Date Played"))
                    mItemQuery.setSortBy(new String[]{ItemSortBy.DatePlayed.toString()});
                else if (mSortOptions.get(position).equalsIgnoreCase("Runtime"))
                    mItemQuery.setSortBy(new String[]{ItemSortBy.Runtime.toString()});
                else
                    mItemQuery.setSortBy(new String[]{ItemSortBy.SortName.toString()});

                mLibraryView.PerformQuery(mItemQuery);
            }

            if (position == mSortOptions.size() - 2 && !mSortAscending) {
                mItemQuery.setSortOrder(SortOrder.Ascending);
                mLibraryView.PerformQuery(mItemQuery);
                mSortAscending = true;
                ((NavigationDrawerAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();
            } else if (position == mSortOptions.size() - 1 && mSortAscending) {
                mItemQuery.setSortOrder(SortOrder.Descending);
                mLibraryView.PerformQuery(mItemQuery);
                mSortAscending = false;
                ((NavigationDrawerAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();
            }
        }
    }

    private class DrawerGenreItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            FileLogger.getFileLogger().Info("Genre Clicked");

            // ignore the user pressing on the header
            if (i == 0) return;

            // User is going "Up" to the parent menu
            if (i == 1) {

                mDrawerList.setAdapter(new NavigationDrawerAdapter(LibraryPresentationActivity.this));
                mDrawerList.setOnItemClickListener(new DrawerSortItemClickListener());
                mDrawerList.getOnItemClickListener().onItemClick(mDrawerList, null, mCurrentSortIndex, 0);

                // User clicked something in the current menu
            } else {

                if (mSelectedGenres == null)
                    mSelectedGenres = new ArrayList<>();

                if (mSelectedGenres.contains(mInitialGenreList.get(i))) {
                    mSelectedGenres.remove(mInitialGenreList.get(i));
                } else
                    mSelectedGenres.add(mInitialGenreList.get(i));

                mItemQuery.setAllGenres(mSelectedGenres.toArray(new String[mSelectedGenres.size()]));

                mLibraryView.PerformQuery(mItemQuery, new GenreQueryResponse());
            }
        }
    }


    private class GenreQueryResponse extends Response<ItemsResult> {
        @Override
        public void onResponse(ItemsResult result) {
            mInitialGenreList = mLibraryView.GetAvailableGenres();
            mInitialGenreList.add(0, "Filter: Genre");
            mInitialGenreList.add(1, "-- BACK --");
            ((NavigationDrawerGenreAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();
        }
    }


    public class NavigationDrawerYearAdapter extends BaseAdapter {

        private Context mContext;
        private LayoutInflater mLayoutInflater;

        public NavigationDrawerYearAdapter(Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // Account for the nav elements
            mYears.add(0, null);
            mYears.add(1, null);
        }

        @Override
        public int getCount() {
            return mYears.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            if (view == null) {
                view = mLayoutInflater.inflate(R.layout.widget_navigation_drawer_clickable_item, viewGroup, false);
            }

            if (view == null) return null;

            TextView tv = (TextView) view.findViewById(R.id.tvClickableItem);


            if (i == 0) {
                tv.setText("Filter: Year");
                tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                tv.setGravity(Gravity.CENTER);
                tv.setTextColor(Color.parseColor("#00b4ff"));
                tv.setBackgroundColor(Color.parseColor("#30bbbbbb"));
                tv.setPadding(0, 10, 0, 10);

            } else {

                if (i == 1)
                    tv.setText("-- BACK --");
                else
                    tv.setText(mYears.get(i).toString());

                tv.setPadding(10, 0, 0, 0);
                tv.setGravity(Gravity.NO_GRAVITY);
                tv.setBackgroundColor(Color.TRANSPARENT);

                if (mSelectedYears != null && mSelectedYears.contains(mYears.get(i))) {
                    tv.setTextColor(Color.parseColor("#00b4ff"));
                    tv.setTextSize(20);
                } else {
                    tv.setTextColor(Color.WHITE);
                    tv.setTextSize(18);
                }
            }

            return view;
        }
    }


    private class YearQueryREsponse extends Response<ItemsResult> {
        @Override
        public void onResponse(ItemsResult result) {
            ((NavigationDrawerYearAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();
        }
    }


    private class DrawerYearItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            // ignore the user pressing on the header
            if (i == 0) return;

            // User is going "Up" to the parent menu
            if (i == 1) {

                mDrawerList.setAdapter(new NavigationDrawerAdapter(LibraryPresentationActivity.this));
                mDrawerList.setOnItemClickListener(new DrawerSortItemClickListener());
                mDrawerList.getOnItemClickListener().onItemClick(mDrawerList, null, mCurrentSortIndex, 0);

                // User clicked something in the current menu
            } else {

                if (mSelectedYears == null)
                    mSelectedYears = new ArrayList<>();

                if (mSelectedYears.contains(mYears.get(i))) {
                    mSelectedYears.remove(mYears.get(i));
                } else
                    mSelectedYears.add(mYears.get(i));


                int[] years = new int[mSelectedYears.size()];
                for (int j = 0; j < years.length; j++) {
                    years[j] = mSelectedYears.get(j);
                }
                mItemQuery.setYears(years);

                mLibraryView.PerformQuery(mItemQuery, new YearQueryREsponse());
            }
        }
    }


    private class NavigationDrawerGenreAdapter extends BaseAdapter {

        private Context mContext;
        private LayoutInflater mLayoutInflater;

        public NavigationDrawerGenreAdapter(Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mInitialGenreList.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            if (view == null) {
                view = mLayoutInflater.inflate(R.layout.widget_navigation_drawer_clickable_item, null);
            }

            if (view == null) return null;

            TextView tv = (TextView) view.findViewById(R.id.tvClickableItem);

            tv.setText(mInitialGenreList.get(i));

            if (i == 0) {
                tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                tv.setGravity(Gravity.CENTER);
                tv.setTextColor(Color.parseColor("#00b4ff"));
                tv.setBackgroundColor(Color.parseColor("#30bbbbbb"));
                tv.setPadding(0, 10, 0, 10);

            } else {

                tv.setPadding(10, 0, 0, 0);
                tv.setGravity(Gravity.NO_GRAVITY);
                tv.setBackgroundColor(Color.TRANSPARENT);

                if (mSelectedGenres != null && mSelectedGenres.contains(mInitialGenreList.get(i))) {
                    tv.setTextColor(Color.parseColor("#00b4ff"));
                    tv.setTextSize(20);
                } else {
                    tv.setTextColor(Color.WHITE);
                    tv.setTextSize(18);
                }
            }
            return view;
        }
    }


    public class NavigationDrawerOfficialRatingAdapter extends BaseAdapter {

        private Context mContext;
        private LayoutInflater mLayoutInflater;

        public NavigationDrawerOfficialRatingAdapter(Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mOfficialRatings.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            if (view == null) {
                view = mLayoutInflater.inflate(R.layout.widget_navigation_drawer_clickable_item, null);
            }

            if (view == null) return null;

            TextView tv = (TextView) view.findViewById(R.id.tvClickableItem);

            tv.setText(mOfficialRatings.get(i));

            if (i == 0) {
                tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                tv.setGravity(Gravity.CENTER);
                tv.setTextColor(Color.parseColor("#00b4ff"));
                tv.setBackgroundColor(Color.parseColor("#30bbbbbb"));
                tv.setPadding(0, 10, 0, 10);

            } else {

                tv.setPadding(10, 0, 0, 0);
                tv.setGravity(Gravity.NO_GRAVITY);
                tv.setBackgroundColor(Color.TRANSPARENT);

                if (mSelectedOfficialRatingIndex == i) {
                    tv.setTextColor(Color.parseColor("#00b4ff"));
                    tv.setTextSize(20);
                } else {
                    tv.setTextColor(Color.WHITE);
                    tv.setTextSize(18);
                }
            }
            return view;
        }
    }


    private class DrawerOfficialRatingItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            // ignore the user pressing on the header
            if (i == 0) return;

            // User is going "Up" to the parent menu
            if (i == 1) {

                mDrawerList.setAdapter(new NavigationDrawerAdapter(LibraryPresentationActivity.this));
                mDrawerList.setOnItemClickListener(new DrawerSortItemClickListener());
                mDrawerList.getOnItemClickListener().onItemClick(mDrawerList, null, mCurrentSortIndex, 0);

                // User clicked something in the current menu
            } else {
                if (mSelectedOfficialRatingIndex != i) {
                    mItemQuery.setMaxOfficialRating(mOfficialRatings.get(i));
                    mSelectedOfficialRatingIndex = i;
                } else {
                    mItemQuery.setMaxOfficialRating(null);
                    mSelectedOfficialRatingIndex = 0;
                }


                mLibraryView.PerformQuery(mItemQuery);
                ((NavigationDrawerOfficialRatingAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();
            }
        }
    }


    private class PlayAllShuffleResponse extends Response<ItemsResult> {

        private boolean mShuffleResults;

        public PlayAllShuffleResponse(boolean shuffleResults) {
            mShuffleResults = shuffleResults;
        }

        @Override
        public void onResponse(ItemsResult response) {
            FileLogger.getFileLogger().Info("Library Presentation Fragment: play-all/shuffle response received");

            if (response == null) {
                FileLogger.getFileLogger().Info("Library Presentation Fragment: response was null");
                return;
            }

            if (response.getItems() == null) {
                FileLogger.getFileLogger().Info("Library Presentation Fragment: items is null");
                return;
            }

            if (response.getItems().length == 0) {
                FileLogger.getFileLogger().Info("Library Presentation Fragment: items is empty");
                return;
            }

            FileLogger.getFileLogger().Info("response.Items.length = " + String.valueOf(response.getItems().length));

            // Filter out unplayable items
            ArrayList<BaseItemDto> filteredItems = new ArrayList<>();
            for (BaseItemDto item : response.getItems()) {
                if (!item.getIsPlaceHolder() && !item.getLocationType().equals(LocationType.Virtual))
                    filteredItems.add(item);
            }
            response.setItems(filteredItems.toArray(new BaseItemDto[filteredItems.size()]));

            FileLogger.getFileLogger().Info("filtered result.Items.length = " + String.valueOf(response.getItems().length));

            if (mShuffleResults) {
                ArrayList<BaseItemDto> items = new ArrayList<>();
                Collections.addAll(items, response.getItems());
                Collections.shuffle(items);
                response.setItems(items.toArray(new BaseItemDto[items.size()]));

                FileLogger.getFileLogger().Info("shuffled result.Items.length = " + String.valueOf(response.getItems().length));
            }

            MB3Application.getInstance().PlayerQueue = new Playlist();

            FileLogger.getFileLogger().Info("Library Presentation Fragment: processing response");
            for (BaseItemDto item : response.getItems()) {
                PlaylistItem playableItem = new PlaylistItem();
                playableItem.Id = item.getId();
                playableItem.Name = item.getName();
                if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getAlbumArtist()))
                    playableItem.SecondaryText = item.getAlbumArtist();
                if (item.getType().equalsIgnoreCase("Episode") && item.getParentIndexNumber() != null && item.getIndexNumber() != null)
                    playableItem.SecondaryText = "Season " + String.valueOf(item.getParentIndexNumber()) + " | Episode " + String.valueOf(item.getIndexNumber());
                playableItem.Type = item.getType();

                MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playableItem);
            }

            if (MB3Application.getInstance().PlayerQueue.PlaylistItems.size() == 0) return;

            if ("audio".equalsIgnoreCase(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Type)) {
                Intent intent = new Intent(LibraryPresentationActivity.this, AudioPlaybackActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(LibraryPresentationActivity.this, PlaybackActivity.class);
                startActivity(intent);
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };


    private Response<ParentalRating[]> getParentalRatingsResponse = new Response<ParentalRating[]>() {

        @Override
        public void onResponse(ParentalRating[] ratings) {

            if (ratings != null && ratings.length > 0) {
                MB3Application.getInstance().ParentalRatings.addAll(Arrays.asList(ratings));
                FileLogger.getFileLogger().Info("ratings returned: " + String.valueOf(ratings.length));
            } else
                FileLogger.getFileLogger().Info("ratings response from server was null");
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private void getAudioPlaylistItems(final boolean shuffle) {

        ItemQuery query = new ItemQuery();
        query.setParentId(mItem.getId());
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());

        MB3Application.getInstance().API.GetItemsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {

                if (response == null || response.getItems() == null) return;

                MB3Application.getInstance().PlayerQueue = new Playlist();

                for (BaseItemDto item : response.getItems()) {

                    PlaylistItem playlistItem = new PlaylistItem();
                    playlistItem.Id = item.getId();
                    playlistItem.Name = String.valueOf(item.getIndexNumber()) + ". " + item.getName();
                    playlistItem.Type = item.getType();

                    MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playlistItem);
                }

                if (shuffle) {
                    Collections.shuffle(MB3Application.getInstance().PlayerQueue.PlaylistItems);
                }

                Intent intent = new Intent(LibraryPresentationActivity.this, AudioPlaybackActivity.class);
                startActivity(intent);
            }
        });

    }

    private void getVideoPlaylistItems(final boolean shuffle) {

        ItemQuery query = new ItemQuery();
        query.setParentId(mItem.getId());
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());

        MB3Application.getInstance().API.GetItemsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult result) {

                if (result == null || result.getItems() == null) return;

                MB3Application.getInstance().PlayerQueue = new Playlist();

                for (BaseItemDto item : result.getItems()) {

                    PlaylistItem playableItem = new PlaylistItem();
                    playableItem.Id = item.getId();
                    playableItem.Name = item.getName();
                    if (item.getType().equalsIgnoreCase("Episode") && item.getParentIndexNumber() != null && item.getIndexNumber() != null)
                        playableItem.SecondaryText = "Season " + String.valueOf(item.getParentIndexNumber()) + " | Episode " + String.valueOf(item.getIndexNumber());
                    playableItem.Type = item.getType();

                    MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playableItem);

                    if (shuffle) {
                        Collections.shuffle(MB3Application.getInstance().PlayerQueue.PlaylistItems);
                    }
                }

                Intent intent = new Intent(LibraryPresentationActivity.this, PlaybackActivity.class);
                startActivity(intent);
            }
        });
    }
}
