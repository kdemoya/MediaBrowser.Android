package com.mb.android.ui.mobile.album;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.activities.BaseMbMobileActivity;
import com.mb.android.activities.mobile.RemoteControlActivity;
import mediabrowser.apiinteraction.Response;
import com.mb.android.listeners.SongOnItemClickListener;
import com.mb.android.playbackmediator.cast.exceptions.NoConnectionException;
import com.mb.android.playbackmediator.cast.exceptions.TransientNetworkDisconnectionException;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.MB3Application;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.fragments.NavigationMenuFragment;
import com.mb.android.player.AudioService;
import com.mb.android.ui.mobile.playback.AudioPlaybackActivity;
import com.mb.android.ui.mobile.musicartist.ArtistActivity;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.library.PlayAccess;
import mediabrowser.model.entities.SortOrder;
import com.mb.android.logging.FileLogger;
import mediabrowser.model.session.PlayCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

/**
 * Created by Mark on 12/12/13.
 *
 * This activity displays the contents of an album. It also handles all playback scenarios that
 * will be encountered
 */
public class MusicAlbumActivity extends BaseMbMobileActivity {

    private ActionBarDrawerToggle mDrawerToggle;
    private BaseItemDto mArtist;
    private BaseItemDto mAlbum;
    private BaseItemDto[] mSongs;
    private TextView mArtistName;
    private String mAlbumId;
    private boolean mAddFavoriteMenuItemVisible;
    private boolean mRemoveFavoriteMenuItemVisible;
    private boolean mSetPlayedMenuItemVisible;
    private boolean mSetUnplayedMenuItemVisible;
    private boolean mIsFresh = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_music_album_details);

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

        mArtist = (BaseItemDto) getMb3Intent().getSerializableExtra("Artist");
        mAlbumId = getMb3Intent().getStringExtra("AlbumId");

//        if (mAlbum != null) {
//            TextView year = (TextView) findViewById(R.id.tvAlbumReleaseYearValue);
//            year.setText(String.valueOf((mAlbum.getProductionYear() != null ? mAlbum.getProductionYear() : "")));
//
//            TextView albumName = (TextView) findViewById(R.id.tvAlbumViewAlbumName);
//            albumName.setText(mAlbum.getName());
//
//            if (mAlbum.getGenres() != null) {
//                TextView genres = (TextView) findViewById(R.id.tvAlbumGenresValue);
//                String genreString = "";
//                for (String s : mAlbum.getGenres()) {
//                    if (!genreString.isEmpty())
//                        genreString += ", ";
//                    genreString += s;
//                }
//                genres.setText(genreString);
//            }
//        }

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

    @Override
    public void onPause() {
        mMini.removeOnMiniControllerChangedListener(mCastManager);
        super.onPause();
    }

    @Override
    protected void onConnectionRestored() {
        performInitialTasks();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            performInitialTasks();
        }
    }

    private void performInitialTasks() {
        if (mIsFresh) {
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mAlbumId)) {

                MB3Application.getInstance().API.GetItemAsync(mAlbumId, MB3Application.getInstance().API.getCurrentUserId(), getAlbumResponse);
                boolean isGenre = getMb3Intent().getBooleanExtra("isGenre", false);

                if (!isGenre) {
                    ItemQuery query = new ItemQuery();
                    query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
                    query.setParentId(mAlbumId);
                    query.setSortBy(new String[]{ItemSortBy.SortName});
                    query.setSortOrder(SortOrder.Ascending);
                    query.setFields(new ItemFields[]{ItemFields.ParentId});
                    query.setRecursive(true);

                    MB3Application.getInstance().API.GetItemsAsync(query, getSongsResponse);
                }
            }
            mIsFresh = false;
        }
    }

    private Response<BaseItemDto> getAlbumResponse = new Response<BaseItemDto>() {
        @Override
        public void onResponse(BaseItemDto item) {

            if (item == null) return;
            mAlbum = item;

            if ("musicgenre".equalsIgnoreCase(item.getType())) {
                ItemQuery query = new ItemQuery();
                query.setParentId(mAlbum.getParentId());
                query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
                query.setRecursive(true);
                query.setSortBy(new String[]{ItemSortBy.Name});
                query.setSortOrder(SortOrder.Ascending);
                query.setFields(new ItemFields[]{ItemFields.ParentId});
                query.setGenres(new String[] { mAlbum.getName()});
                query.setIncludeItemTypes(new String[]{"Audio"});
                MB3Application.getInstance().API.GetItemsAsync(query, getSongsResponse);
            }

            NetworkImageView albumCover = (NetworkImageView) findViewById(R.id.ivAlbumCoverLarge);

            if (mAlbum.getHasPrimaryImage()) {
                ImageOptions options = new ImageOptions();
                options.setImageType(ImageType.Primary);
                options.setMaxWidth((int) (150 * getScreenDensity()));
                options.setMaxHeight((int) (150 * getScreenDensity()));
                options.setEnableImageEnhancers(PreferenceManager
                        .getDefaultSharedPreferences(MB3Application.getInstance())
                        .getBoolean("pref_enable_image_enhancers", true));

                String albumCoverImageUrl = MB3Application.getInstance().API.GetImageUrl(mAlbum, options);
                albumCover.setImageUrl(albumCoverImageUrl, MB3Application.getInstance().API.getImageLoader());
            } else {
                albumCover.setDefaultImageResId(R.drawable.music_square_bg);
                albumCover.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
            }

            // Set the backdrop and title
            if (mAlbum.getBackdropCount() > 0) {
                LoadGenreBackdrop();
            } else if (mArtist != null) {
                LoadArtistBackdrop();
                mArtistName = (TextView) findViewById(R.id.tvAlbumViewArtistName);
                mArtistName.setText(mArtist.getName());
            } else {
                if (mAlbum.getParentId() != null) {
                    Log.i("MusicAlbumActivity", "Has ParentId");
                    MB3Application.getInstance().API.GetItemAsync(
                            mAlbum.getParentId(),
                            MB3Application.getInstance().API.getCurrentUserId(),
                            getItemResponse);
                }
            }

            updateFavoriteVisibleIcons();
            updatePlaystateVisibleIcons();

            if (com.mb.android.playbackmediator.utils.Utils.getBooleanFromPreference(MusicAlbumActivity.this, "CONTENT_MIRROR_ENABLED", false)) {
                try {
                    mCastManager.displayItem(mAlbum);
                } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                    e.printStackTrace();
                }
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (mAlbum != null && mAlbum.getPlayAccess().equals(PlayAccess.Full)) {
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

        if (mSetUnplayedMenuItemVisible)
            mSetUnplayedMenuItem.setVisible(true);
        else
            mSetUnplayedMenuItem.setVisible(false);

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

            MB3Application.getInstance().API.UpdateFavoriteStatusAsync(mAlbum.getId(), MB3Application.getInstance().API.getCurrentUserId(), true, new UpdateFavoriteResponse());

        /*
        Remove Favorite
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.un_favorite_action_bar_button))) {

            MB3Application.getInstance().API.UpdateFavoriteStatusAsync(mAlbum.getId(), MB3Application.getInstance().API.getCurrentUserId(), false, new UpdateFavoriteResponse());

        /*
        Set Played
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.played_action_bar_button))) {

            MB3Application.getInstance().API.MarkPlayedAsync(mAlbum.getId(), MB3Application.getInstance().API.getCurrentUserId(), null, new UpdatePlaystateReponse());

        /*
        Remove Played
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.un_played_action_bar_button))) {

            MB3Application.getInstance().API.MarkUnplayedAsync(mAlbum.getId(), MB3Application.getInstance().API.getCurrentUserId(), new UpdatePlaystateReponse());

        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void handlePlayOrShuffleRequest(boolean shuffleMedia) {
        AudioService.PlayerState currentState = MB3Application.getAudioService().getPlayerState();
        if (currentState.equals(AudioService.PlayerState.PLAYING) || currentState.equals(AudioService.PlayerState.PAUSED)) {
            MB3Application.getAudioService().stopMedia();
        }
        MB3Application.getInstance().PlayerQueue.PlaylistItems = new ArrayList<>();
        if (mCastManager.isConnected()) {
            mCastManager.playItem(mAlbum, shuffleMedia ? PlayCommand.PlayShuffle : PlayCommand.PlayNow, 0L);
            Intent intent = new Intent(this, RemoteControlActivity.class);
            startActivity(intent);
        } else {
            for (BaseItemDto song : mSongs) {
                PlaylistItem playableItem = new PlaylistItem();
                playableItem.Id = song.getId();
                playableItem.Name = String.valueOf(song.getIndexNumber()) + ". " + song.getName();
                playableItem.Type = song.getType();
                playableItem.Runtime = song.getRunTimeTicks();

                MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playableItem);
            }

            if (shuffleMedia) {
                long seed = System.nanoTime();
                Collections.shuffle(MB3Application.getInstance().PlayerQueue.PlaylistItems, new Random(seed));
            }
            Intent intent = new Intent(this, AudioPlaybackActivity.class);
            startActivity(intent);
        }
    }

    public void updateFavoriteVisibleIcons() {

        Log.i("", "updateFavoriteVisibleIcons called");
        FileLogger.getFileLogger().Info("Update favorite visible icons");

        if (mAlbum.getUserData() != null && mAlbum.getUserData().getIsFavorite()) {

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

    private void updatePlaystateVisibleIcons() {

        Log.i("", "updatePlaystateVisibleIcons called");
        FileLogger.getFileLogger().Info("Update playstate visible icons");

        if (mAlbum.getUserData() != null && mAlbum.getUserData().getPlayed()) {

            // only show the remove favorite
            mSetPlayedMenuItemVisible = false;
            mSetUnplayedMenuItemVisible = true;

        } else {

            // only show the add favorite
            mSetPlayedMenuItemVisible = true;
            mSetUnplayedMenuItemVisible = false;
        }

        this.invalidateOptionsMenu();
    }

    public void LoadGenreBackdrop() {
        Log.i("MusicAlbumActivity", "Load Genre Backdrop called");
        NetworkImageView backdrop = (NetworkImageView) findViewById(R.id.ivAlbumViewBackdrop);

        if (mAlbum.getBackdropCount() > 0) {
            Log.i("MusicAlbumActivity", "Has Backdrop");
            ImageOptions backOptions = new ImageOptions();
            backOptions.setImageType(ImageType.Backdrop);
            backOptions.setWidth(getScreenWidth() / 2);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(mAlbum, backOptions);
            backdrop.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        } else {
            backdrop.setLayoutParams(new RelativeLayout.LayoutParams(getScreenWidth(), (int) ((float) (getScreenWidth() / 16) * 5)));
        }
    }

    public void LoadArtistBackdrop() {
        Log.i("MusicAlbumActivity", "Load Artist Backdrop called");
        NetworkImageView backdrop = (NetworkImageView) findViewById(R.id.ivAlbumViewBackdrop);

        if (mArtist.getBackdropCount() > 0) {
            Log.i("MusicAlbumActivity", "Has Backdrop");
            ImageOptions backOptions = new ImageOptions();
            backOptions.setImageType(ImageType.Backdrop);
            backOptions.setWidth(getScreenWidth() / 2);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(mArtist, backOptions);
            backdrop.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        } else {
            backdrop.setLayoutParams(new RelativeLayout.LayoutParams(getScreenWidth(), (int) ((float) (getScreenWidth() / 16) * 5)));
        }
    }


    private class UpdateFavoriteResponse extends Response<UserItemDataDto> {
        @Override
        public void onResponse(UserItemDataDto response) {

            if (response == null) return;
            mAlbum.getUserData().setIsFavorite(response.getIsFavorite());
            updateFavoriteVisibleIcons();
        }
    }

    private class UpdatePlaystateReponse extends Response<UserItemDataDto> {
        @Override
        public void onResponse(UserItemDataDto response) {
            if (response == null) return;

            mAlbum.getUserData().setPlayed(response.getPlayed());
            updatePlaystateVisibleIcons();
        }
    }

    private Response<BaseItemDto> getItemResponse = new Response<BaseItemDto>() {

        @Override
        public void onResponse(BaseItemDto response) {
            Log.i("GetArtistCallback", "Setup Object");
            mArtist = response;

            if (mArtist != null) {

                LoadArtistBackdrop();

                mArtistName = (TextView) findViewById(R.id.tvAlbumViewArtistName);
                mArtistName.setText(mArtist.getName());
                mArtistName.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {

                        Intent intent = new Intent(MusicAlbumActivity.this, ArtistActivity.class);
                        intent.putExtra("ArtistId", mArtist.getId());

                        startActivity(intent);
                    }

                });

            } else {
                Log.i("GetSongsCallback", "songs_ is null");
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };


    private Response<ItemsResult> getSongsResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {
            Log.i("GetSongsCallback", "Setup Objects");

            if (response == null) {
                Log.d("MusicAlbumActivity", "Error processing response");
                return;
            }

            mSongs = response.getItems();

            if (mSongs != null) {

                ListView songList = (ListView) findViewById(R.id.lvTrackList);
                songList.setAdapter(new SongAdapter(Arrays.asList(mSongs), MusicAlbumActivity.this));
                songList.setOnItemClickListener(new SongOnItemClickListener());
//                songList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//                    @Override
//                    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
//
//                        remotePlaySingleItem = true;
//                        remotePlaySingleItemId = mSongs[i].Id;
//                        MB3Application.getInstance().API.GetClientSessionsAsync(new GetClientSessionsCallback());
//                        return true;
//                    }
//                });

                songList.requestFocus();

            } else {
                Log.i("GetSongsCallback", "mSongs is null");
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };
}
