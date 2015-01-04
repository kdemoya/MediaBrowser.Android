package com.mb.android.ui.mobile.musicartist;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mb.android.activities.BaseMbMobileActivity;
import com.mb.android.activities.mobile.RemoteControlActivity;
import mediabrowser.apiinteraction.Response;
import com.mb.android.playbackmediator.cast.exceptions.NoConnectionException;
import com.mb.android.playbackmediator.cast.exceptions.TransientNetworkDisconnectionException;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.MB3Application;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.fragments.NavigationMenuFragment;
import com.mb.android.player.AudioService;
import com.mb.android.ui.mobile.playback.AudioPlaybackActivity;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.library.PlayAccess;
import mediabrowser.model.entities.SortOrder;
import com.mb.android.logging.FileLogger;
import mediabrowser.model.session.PlayCommand;

import java.util.ArrayList;

/**
 * Created by Mark on 12/12/13.
 *
 * This Activity displays a biography for the selected artist/band and a list of albums that the artist has released
 */
public class ArtistActivity extends BaseMbMobileActivity {

    private ActionBarDrawerToggle mDrawerToggle;
    private BioFragment           mBioFragment;
    private AlbumsFragment        mAlbumsFragment;
    private BaseItemDto           mArtist;
    private String                artistId;
    private boolean               mAddFavoriteMenuItemVisible;
    private boolean               mRemoveFavoriteMenuItemVisible;
    private boolean               mSetPlayedMenuItemVisible;
    private boolean               mSetUnPlayedMenuItemVisible;
    private boolean               mIsFresh = true;


    /**
     * @param savedInstanceState The bundle passed to the Activity at launch
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_artist_details);

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

        artistId = getMb3Intent().getStringExtra("ArtistId");

        ViewPager pager = (ViewPager) findViewById(R.id.artistPager);

        if (pager != null) {
            // Phone
            pager.setAdapter(new ArtistPagerAdapter(getSupportFragmentManager()));
        } else {
            // Tablet
            if (savedInstanceState == null) {

                Bundle args = new Bundle();
                args.putSerializable("ArtistId", artistId);
                args.putBoolean("IsTabletLayout", true);

                mBioFragment = new BioFragment();
                mBioFragment.setArguments(args);

                mAlbumsFragment = new AlbumsFragment();
                mAlbumsFragment.setArguments(args);

                FragmentManager fm = getSupportFragmentManager();

                FragmentTransaction fragmentTransaction = fm.beginTransaction();
                fragmentTransaction.replace(R.id.flArtistBioHolder, mBioFragment);
                fragmentTransaction.replace(R.id.flArtistAlbumsHolder, mAlbumsFragment);
                fragmentTransaction.commit();
            }
        }

        mMini = (MiniController) findViewById(R.id.miniController1);
        mCastManager.addMiniController(mMini);
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

    /**
     * @param menu The Menu instance that is being assembled
     * @return True is the OptionsMenu creation was handled
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (mArtist != null && mArtist.getPlayAccess().equals(PlayAccess.Full)) {
            menu.add(getResources().getString(R.string.play_all_action_bar_button)).setIcon(R.drawable.play).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.add(getResources().getString(R.string.shuffle_action_bar_button)).setIcon(R.drawable.shuffle).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        MenuItem mRemoveFavoriteMenuItem = menu.add(getResources().getString(R.string.un_favorite_action_bar_button));
        mRemoveFavoriteMenuItem.setIcon(R.drawable.nfav);
        mRemoveFavoriteMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        if (mRemoveFavoriteMenuItemVisible)
            mRemoveFavoriteMenuItem.setVisible(true);
        else
            mRemoveFavoriteMenuItem.setVisible(false);

        MenuItem mAddFavoriteMenuItem = menu.add(getResources().getString(R.string.favorite_action_bar_button));
        mAddFavoriteMenuItem.setIcon(R.drawable.fav);
        mAddFavoriteMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        if (mAddFavoriteMenuItemVisible)
            mAddFavoriteMenuItem.setVisible(true);
        else
            mAddFavoriteMenuItem.setVisible(false);

        MenuItem mSetPlayedMenuItem = menu.add(getResources().getString(R.string.played_action_bar_button));
        mSetPlayedMenuItem.setIcon(R.drawable.set_played);
        mSetPlayedMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        if (mSetPlayedMenuItemVisible)
            mSetPlayedMenuItem.setVisible(true);
        else
            mSetPlayedMenuItem.setVisible(false);

        MenuItem mSetUnplayedMenuItem = menu.add(getResources().getString(R.string.un_played_action_bar_button));
        mSetUnplayedMenuItem.setIcon(R.drawable.set_unplayed);
        mSetUnplayedMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        if (mSetUnPlayedMenuItemVisible)
            mSetUnplayedMenuItem.setVisible(true);
        else
            mSetUnplayedMenuItem.setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }


    /**
     * @param item The MenuItem that has been selected
     * @return True if the Item Selection was handled, False otherwise.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        /*
        Play all
         */
        if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.play_all_action_bar_button))) {
            handlePlayOrShuffleRequest(false);

        /*
        Shuffle
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.shuffle_action_bar_button))) {
            handlePlayOrShuffleRequest(true);

        /*
        Set Favorite
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.favorite_action_bar_button))) {

            MB3Application.getInstance().API.UpdateFavoriteStatusAsync(mArtist.getId(), MB3Application.getInstance().API.getCurrentUserId(), true, new UpdateFavoriteResponse());

        /*
        Remove Favorite
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.un_favorite_action_bar_button))) {

            MB3Application.getInstance().API.UpdateFavoriteStatusAsync(mArtist.getId(), MB3Application.getInstance().API.getCurrentUserId(), false, new UpdateFavoriteResponse());

        /*
        Set Played
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.played_action_bar_button))) {

            MB3Application.getInstance().API.MarkPlayedAsync(mArtist.getId(), MB3Application.getInstance().API.getCurrentUserId(), null, new UpdatePlaystateResponse());

        /*
        Remove Played
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.un_played_action_bar_button))) {

            MB3Application.getInstance().API.MarkUnplayedAsync(mArtist.getId(), MB3Application.getInstance().API.getCurrentUserId(), new UpdatePlaystateResponse());

        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void handlePlayOrShuffleRequest(boolean shuffleMedia) {
        ItemQuery query = new ItemQuery();
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setParentId(mArtist.getId());
        query.setSortBy(new String[]{ shuffleMedia ? ItemSortBy.Random : ItemSortBy.Album});
        query.setSortOrder(SortOrder.Ascending);
        query.setIncludeItemTypes(new String[]{"Audio"});
        query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.SortName, ItemFields.DateCreated});
        query.setRecursive(true);

        MB3Application.getInstance().API.GetItemsAsync(query, playAllShuffleResponse);
    }


    @Override
    public void onPause() {
        mMini.removeOnMiniControllerChangedListener(mCastManager);
        super.onPause();
    }


    @Override
    protected void onConnectionRestored() {
        buildUi();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            buildUi();
        }
    }

    private void buildUi() {
        if (mIsFresh) {
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(artistId)) {
                MB3Application.getInstance().API.GetItemAsync(artistId, MB3Application.getInstance().API.getCurrentUserId(), getArtistResponse);
            }
            mIsFresh = false;
        }
    }

    private Response<BaseItemDto> getArtistResponse = new Response<BaseItemDto>() {
        @Override
        public void onResponse(BaseItemDto item) {
            if (item == null) return;
            mArtist = item;
            if (mActionBar != null && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getName())) {
                mActionBar.setTitle(item.getName());
            }
            if (com.mb.android.playbackmediator.utils.Utils.getBooleanFromPreference(ArtistActivity.this, "CONTENT_MIRROR_ENABLED", false)) {
                try {
                    mCastManager.displayItem(item);
                } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                    e.printStackTrace();
                }
            }
            updateFavoriteVisibleIcons();
            updatePlaystateVisibleIcons();
        }
    };

    /**
     *
     */
    public void updateFavoriteVisibleIcons() {

        Log.i("", "updateFavoriteVisibleIcons called");
        FileLogger.getFileLogger().Info("Update favorite visible icons");
        if (mArtist == null) return;

        if (mArtist.getUserData() != null && mArtist.getUserData().getIsFavorite()) {

            FileLogger.getFileLogger().Info("Show remove favorite");
            // only show the remove favorite
            mAddFavoriteMenuItemVisible = false;
            mRemoveFavoriteMenuItemVisible = true;

        } else {
            FileLogger.getFileLogger().Info("Show add favorite");
            // only show the add favorite
            mAddFavoriteMenuItemVisible = true;
            mRemoveFavoriteMenuItemVisible = false;
        }

        this.invalidateOptionsMenu();
    }

    /**
     *
     */
    private void updatePlaystateVisibleIcons() {

        Log.i("", "updatePlaystateVisibleIcons called");
        FileLogger.getFileLogger().Info("Update playstate visible icons");

        if (mArtist == null) return;

        if (mArtist.getUserData() != null && mArtist.getUserData().getPlayed()) {

            // only show the set unplayed
            mSetPlayedMenuItemVisible = false;
            mSetUnPlayedMenuItemVisible = true;

        } else {

            // only show the set played
            mSetPlayedMenuItemVisible = true;
            mSetUnPlayedMenuItemVisible = false;
        }

        this.invalidateOptionsMenu();
    }

    private Response<ItemsResult> playAllShuffleResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {

            if (response == null ) return;

            AudioService.PlayerState currentState = MB3Application.getAudioService().getPlayerState();
            if (currentState.equals(AudioService.PlayerState.PLAYING) || currentState.equals(AudioService.PlayerState.PAUSED)) {
                MB3Application.getAudioService().stopMedia();
            }
            MB3Application.getInstance().PlayerQueue.PlaylistItems = new ArrayList<>();

            if (mCastManager.isConnected()) {
                mCastManager.playItem(mArtist, PlayCommand.PlayNow, 0L);
                Intent intent = new Intent(ArtistActivity.this, RemoteControlActivity.class);
                startActivity(intent);
            } else {
                for (BaseItemDto song : response.getItems()) {
                    PlaylistItem playableItem = new PlaylistItem();
                    playableItem.Id = song.getId();
                    playableItem.Name = song.getName();
                    StringBuilder builder = new StringBuilder();
                    for (String s : song.getArtists()) {
                        if (builder.length() > 0)
                            builder.append(", ");
                        builder.append(s);
                    }
                    playableItem.SecondaryText = builder.toString();
                    playableItem.Type = song.getType();
                    playableItem.Runtime = song.getRunTimeTicks();

                    MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playableItem);
                }

                Intent intent = new Intent(ArtistActivity.this, AudioPlaybackActivity.class);
                startActivity(intent);
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private class UpdateFavoriteResponse extends Response<UserItemDataDto> {
        @Override
        public void onResponse(UserItemDataDto data) {

            if (data == null || mArtist == null) return;

            mArtist.getUserData().setIsFavorite(data.getIsFavorite());
            updateFavoriteVisibleIcons();
        }
    }


    private class UpdatePlaystateResponse extends Response<UserItemDataDto> {
        @Override
        public void onResponse(UserItemDataDto data) {

            if (data == null || mArtist == null) return;

            mArtist.getUserData().setPlayed(data.getPlayed());
            updatePlaystateVisibleIcons();
        }
    }

    /**
     *
     */
    private class ArtistPagerAdapter extends FragmentPagerAdapter {

        public ArtistPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            Bundle args = new Bundle();
            args.putSerializable("ArtistId", artistId);

            switch (position) {

                case 0:
                    mBioFragment = new BioFragment();
                    mBioFragment.setArguments(args);
                    return mBioFragment;
                case 1:
                    mAlbumsFragment = new AlbumsFragment();
                    mAlbumsFragment.setArguments(args);
                    return mAlbumsFragment;

            }

            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            if (position == 0)
                return getResources().getString(R.string.bio_header);

            return getResources().getString(R.string.albums_header);
        }
    }
}
