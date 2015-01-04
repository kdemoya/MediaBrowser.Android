package com.mb.android.activities.mobile;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.DialogFragments.SyncDialog;
import com.mb.android.activities.BaseMbMobileActivity;
import com.mb.android.adapters.ResumeDialogAdapter;
import mediabrowser.apiinteraction.Response;
import com.mb.android.playbackmediator.cast.exceptions.NoConnectionException;
import com.mb.android.playbackmediator.cast.exceptions.TransientNetworkDisconnectionException;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.DialogFragments.StreamSelectionDialogFragment;
import com.mb.android.MB3Application;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.fragments.NavigationMenuFragment;
import com.mb.android.player.AudioService;
import com.mb.android.ui.mobile.homescreen.HomescreenActivity;
import com.mb.android.ui.mobile.playback.PlaybackActivity;
import com.mb.android.ui.tv.playback.PlayerHelpers;
import com.mb.android.utils.Utils;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ThemeMediaResult;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.library.PlayAccess;
import com.mb.android.fragments.MediaActorsFragment;
import com.mb.android.fragments.MediaOverviewFragment;
import com.mb.android.logging.FileLogger;
import mediabrowser.model.session.PlayCommand;
import mediabrowser.model.sync.SyncJobRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * This Activity shows all the details for the currently selected Media item. Currently only Episodes and Movies
 * are shown here.
 */
public class MediaDetailsActivity extends BaseMbMobileActivity
        implements StreamSelectionDialogFragment.StreamSelectionDialogListener {

    private BaseItemDto mItem;
    private boolean mAddFavoriteMenuItemVisible;
    private boolean mRemoveFavoriteMenuItemVisible;
    private boolean mSetPlayedMenuItemVisible;
    private boolean mSetUnplayedMenuItemVisible;
    private boolean mFullDetailsDownloaded;
    private ViewPager mViewPager;
    private ViewSwitcher mBackdropSwitcher;
    private NetworkImageView mBackdropImage1;
    private NetworkImageView mBackdropImage2;
    private List<String> mBackdropUrls;
    private int mBackdropIndex = 0;
    private boolean mDying = false;
    private boolean mLaunchedFromHomeScreen;
    private ActionBarDrawerToggle mDrawerToggle;
    private int mSelectedAudioStreamIndex = -1;
    private int mSelectedSubtitleStreamIndex = -1;
    private String mSelectedMediaSourceId;
    private boolean isFresh = true;
    private Bundle mSavedInstanceState;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSavedInstanceState = savedInstanceState;

        FileLogger.getFileLogger().Info("Media Details Activity: onCreate");
        setContentView(R.layout.activity_media_details);

        PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_title_strip);
        if (tabStrip != null) tabStrip.setDrawFullUnderline(false);

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

        if (getMb3Intent() == null) {
            Log.i("MediaDetailsActivity", "intent is null");
            FileLogger.getFileLogger().Error("MediaDetailsActivity: Intent is null");
        }

        String jsonData = getMb3Intent().getStringExtra("Item");
        mItem = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, BaseItemDto.class);

        mLaunchedFromHomeScreen = getMb3Intent().getBooleanExtra("LaunchedFromHomeScreen", false);

        mViewPager = (ViewPager) findViewById(R.id.mediaPager);
        mMini = (MiniController) findViewById(R.id.miniController1);
        mCastManager.addMiniController(mMini);
        mMini.setOnMiniControllerChangedListener(mCastManager);
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

        if (mItem == null) {
            FileLogger.getFileLogger().Error("mMediaWrapper.Item is null");
        } else {
            if (mItem.getLocationType() != LocationType.Virtual && mItem.getPlayAccess().equals(PlayAccess.Full)) {

                if (!mItem.getType().equalsIgnoreCase("game")) {
                    menu.add(getResources().getString(R.string.play_action_bar_button))
                            .setIcon(R.drawable.play)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    menu.add(getResources().getString(R.string.language_action_bar_button))
                            .setIcon(R.drawable.pp)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }
            }
            if (mItem.getLocalTrailerCount() != null && mItem.getLocalTrailerCount() > 0) {
                menu.add(getResources().getString(R.string.trailer_action_bar_button))
                        .setIcon(R.drawable.trailers)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        }
        if (MB3Application.getInstance().getSyncEnabled()) {
            if (MB3Application.getInstance().user != null
                    && MB3Application.getInstance().user.getPolicy() != null
                    && MB3Application.getInstance().user.getPolicy().getEnableSync()) {
                menu.add("Sync").setIcon(R.drawable.refresh).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        }
        MenuItem mRemoveFavoriteMenuItem = menu.add(getResources().getString(R.string.un_favorite_action_bar_button));
        mRemoveFavoriteMenuItem.setIcon(R.drawable.nfav);
        mRemoveFavoriteMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        if (mRemoveFavoriteMenuItemVisible)
            mRemoveFavoriteMenuItem.setVisible(true);
        else
            mRemoveFavoriteMenuItem.setVisible(false);

        MenuItem mAddFavoriteMenuItem = menu.add(getResources().getString(R.string.favorite_action_bar_button));
        mAddFavoriteMenuItem.setIcon(R.drawable.fav);
        mAddFavoriteMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        if (mAddFavoriteMenuItemVisible)
            mAddFavoriteMenuItem.setVisible(true);
        else
            mAddFavoriteMenuItem.setVisible(false);

        MenuItem mSetPlayedMenuItem = menu.add(getResources().getString(R.string.played_action_bar_button));
        mSetPlayedMenuItem.setIcon(R.drawable.set_played);
        mSetPlayedMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        if (mSetPlayedMenuItemVisible)
            mSetPlayedMenuItem.setVisible(true);
        else
            mSetPlayedMenuItem.setVisible(false);

        MenuItem mSetUnplayedMenuItem = menu.add(getResources().getString(R.string.un_played_action_bar_button));
        mSetUnplayedMenuItem.setIcon(R.drawable.set_unplayed);
        mSetUnplayedMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        if (mSetUnplayedMenuItemVisible)
            mSetUnplayedMenuItem.setVisible(true);
        else
            mSetUnplayedMenuItem.setVisible(false);


        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.i("MediaDetailsActivity", "Options item clicked");
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        /*
        Play
         */
        if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.play_action_bar_button))) {

            /*
            Media is offline, can't be played anywhere
             */
            if (mItem.getLocationType() == LocationType.Offline
                    || (mItem.getIsPlaceHolder() != null && mItem.getIsPlaceHolder())) {
                ShowOfflineWarning();
                return true;
            }

            /*
            It's a game, we'll need to pass the media to an emulator
             */
//            if (mMediaWrapper.Item.Type.equalsIgnoreCase("Game")) {
//                FileLogger.getFileLogger(this).Info("Building Game intent");
//
//                String current_ime = Settings.Secure.getString(getContentResolver(),
//                        Settings.Secure.DEFAULT_INPUT_METHOD);
//
//                Intent intent = new Intent(this, NativeActivity.class);
//                intent.setAction(Intent.ACTION_VIEW);
//                intent.addCategory(Intent.CATEGORY_LAUNCHER);
//                intent.putExtra("ROM", mMediaWrapper.Item.Path);
//                intent.putExtra("LIBRETRO", mApp.LibretroNativeLibraryPath);
//                intent.putExtra("CONFIGFILE", getDefaultConfigPath());
//                intent.putExtra("IME", current_ime);
//
//
//                FileLogger.getFileLogger(this).Info("Start Activity");
//                // TODO figure out how to make this work
//                try {
//                    startActivity(intent);
//                } catch (Exception e) {
//                    Toast.makeText(this, "Error launching game", Toast.LENGTH_LONG).show();
//                }
//            } else {

                if (mItem.getUserData() != null && mItem.getUserData().getPlaybackPositionTicks() > 0) {
                    showResumeDialog();
                } else {
                    handlePlayRequest(mItem, false, false);
                }
//            }

        /*
        Audio and Subtitle settings
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.language_action_bar_button))) {

            if (mItem.getLocationType() == LocationType.Offline || mItem.getIsPlaceHolder()) {
                ShowOfflineWarning();
                return true;
            }

            if (mFullDetailsDownloaded)
                ShowStreamSelectionDialog();

        /*
        Trailer
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.trailer_action_bar_button))) {

            if (mItem.getLocationType() == LocationType.Offline) {
                ShowOfflineWarning();
                return true;
            }

            MB3Application.getInstance().API.GetLocalTrailersAsync(
                    MB3Application.getInstance().API.getCurrentUserId(),
                    mItem.getId(),
                    getLocalTrailersResponse
            );

        /*
        Home
         */
        } else if (item.getItemId() == android.R.id.home) {

            Intent intent = new Intent(this, HomescreenActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
            this.finish();

        /*
        Sync
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase("sync")) {
            SyncDialog dialog = new SyncDialog();
//            dialog.setItem(mItem);
//            dialog.setStreams(mSelectedAudioStreamIndex, mSelectedSubtitleStreamIndex);
            dialog.show(getSupportFragmentManager(), "SyncDialog");
        /*
        Set Favorite
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.favorite_action_bar_button))) {

            MB3Application.getInstance().API.UpdateFavoriteStatusAsync(
                    mItem.getId(),
                    MB3Application.getInstance().API.getCurrentUserId(),
                    true,
                    new UpdateFavoriteResponse()
            );

        /*
        Remove Favorite
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.un_favorite_action_bar_button))) {

            MB3Application.getInstance().API.UpdateFavoriteStatusAsync(mItem.getId(), MB3Application.getInstance().API.getCurrentUserId(), false,
                    new UpdateFavoriteResponse());

        /*
        Set Played
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.played_action_bar_button))) {

            MB3Application.getInstance().API.MarkPlayedAsync(
                    mItem.getId(),
                    MB3Application.getInstance().API.getCurrentUserId(),
                    new Date(),
                    new UpdatePlaystateResponse()
            );

        /*
        Remove Played
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.un_played_action_bar_button))) {

            MB3Application.getInstance().API.MarkUnplayedAsync(
                    mItem.getId(),
                    MB3Application.getInstance().API.getCurrentUserId(),
                    new UpdatePlaystateResponse());

        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }


    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            buildUi();
        }
        if (com.mb.android.playbackmediator.utils.Utils.getBooleanFromPreference(this, "CONTENT_MIRROR_ENABLED", false)) {
            try {
                mCastManager.displayItem(mItem);
            } catch (TransientNetworkDisconnectionException | NoConnectionException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        if (MB3Application.getInstance().user != null
                && MB3Application.getInstance().user.getPolicy().getEnableSync()) {
        }

        FileLogger.getFileLogger().Info("Media Details Activity: onResume");
        mDying = false;
    }

    private boolean shouldPlayThemeSong() {
        if (mSavedInstanceState != null) return false;
        if (mItem == null) return false;
        if ((mLaunchedFromHomeScreen && "Episode".equalsIgnoreCase(mItem.getType()))
                || !"Episode".equalsIgnoreCase(mItem.getType())) {

            // Only request the theme song if the feature is enabled in settings
            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_play_theme", false))
                if (!MB3Application.getAudioService().getPlayerState().equals(AudioService.PlayerState.PLAYING)) {
                    return true;
                }
        }
        return false;
    }


    @Override
    public void onPause() {
        FileLogger.getFileLogger().Info("Media Details Activity: onPause");
        mDying = true;
        super.onPause();
    }


    @Override
    public void onDestroy() {

        FileLogger.getFileLogger().Info("Media Details Activity: onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onConnectionRestored() {
        buildUi();
    }

    private void buildUi() {
        if (isFresh) {
            if (mItem != null) {

                MB3Application.getInstance().API.GetItemAsync(
                        mItem.getId(),
                        MB3Application.getInstance().API.getCurrentUserId(),
                        getItemResponse);

                if (shouldPlayThemeSong()) {
                    MB3Application.getInstance().API.GetThemeSongsAsync(
                            MB3Application.getInstance().API.getCurrentUserId(), mItem.getId(), true, getThemeSongsResponse);
                }
            }
            isFresh = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (mItem != null) {
            // We want episodes theme media to keep playing if it was started in the SeriesView activity
            if (mLaunchedFromHomeScreen || !"episode".equalsIgnoreCase(mItem.getType())) {
                MB3Application.getInstance().StopMedia();
            }
        }
        super.onBackPressed();
    }

    private void showResumeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        @SuppressLint("InflateParams") View layout = getLayoutInflater().inflate(R.layout.widget_resume_dialog, null);
        builder.setTitle(getResources().getString(R.string.popup_resume_title));
        builder.setView(layout);
        final AlertDialog dialog = builder.create();
        ListView list = (ListView) layout.findViewById(R.id.lvResumeSelection);
        list.setAdapter(new ResumeDialogAdapter(mItem));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                handlePlayRequest(mItem, position == 1, false);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void handlePlayRequest(BaseItemDto item, boolean resume, boolean isTrailer) {
        // Kill audio playback
        AudioService.PlayerState currentState = MB3Application.getAudioService().getPlayerState();
        if (currentState.equals(AudioService.PlayerState.PLAYING) || currentState.equals(AudioService.PlayerState.PAUSED)) {
            MB3Application.getAudioService().stopMedia();
        }
        // Just in case the TV Theme is still playing
        MB3Application.getInstance().StopMedia();

        /*
        Playback is to commence on a ChromeCast device.
        */
        if (mCastManager.isConnected()) {
            FileLogger.getFileLogger().Info("Play requested: Remote player detected");

            mCastManager.playItem(item, PlayCommand.PlayNow, resume ? item.getUserData().getPlaybackPositionTicks() : 0);

            Intent intent = new Intent(MediaDetailsActivity.this, RemoteControlActivity.class);
            intent.putExtra("startTimeTicks", resume ? item.getUserData().getPlaybackPositionTicks() : 0);
            startActivity(intent);

        /*
        Playback is to commence on an external player
        */
        } else if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("pref_enable_external_player", false)) {

            FileLogger.getFileLogger().Info("Play requested: External player");

//            String url = MB3Application.getInstance().API.getApiUrl() + "/Videos/" + item.getId() + "/stream?static=true";
            StreamInfo info = PlayerHelpers.buildExternalPlayerStreamInfo(
                    item.getId(),
                    item.getMediaSources(),
                    resume ? item.getUserData().getPlaybackPositionTicks() : 0L,
                    mSelectedMediaSourceId,
                    mSelectedAudioStreamIndex,
                    mSelectedSubtitleStreamIndex);
            String url = info.ToUrl(MB3Application.getInstance().API.getApiUrl());
            FileLogger.getFileLogger().Info("External player URL: " + url);
            Log.d("External Player url", url);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//            intent.setDataAndType(Uri.parse(url), "video/x-msvideo");
            startActivity(intent);
        /*
        Playback is to commence on the internal player
        */
        } else {
            FileLogger.getFileLogger().Info("Play requested: Internal player");
            MB3Application.getInstance().PlayerQueue.PlaylistItems = new ArrayList<>();

            if (isTrailer || resume) {
                addToPlaylist(item, resume, null, null);
                Intent intent = new Intent(this, PlaybackActivity.class);
                startActivity(intent);
            } else {
                MB3Application.getInstance().API.GetIntrosAsync(item.getId(), MB3Application.getInstance().API.getCurrentUserId(), new GetIntrosResponse(item, mSelectedAudioStreamIndex, mSelectedSubtitleStreamIndex));
            }
        }
    }

    private class GetIntrosResponse extends Response<ItemsResult> {

        private BaseItemDto mainFeature;
        private Integer audioStream;
        private Integer subtitleStream;

        public GetIntrosResponse(BaseItemDto item, Integer audioStreamIndex, Integer subtitleStreamIndex) {
            mainFeature = item;
            audioStream = audioStreamIndex;
            subtitleStream = subtitleStreamIndex;
        }
        @Override
        public void onResponse(ItemsResult result) {
            if (result != null && result.getItems() != null) {
                for (BaseItemDto rItem : result.getItems()) {
                    addToPlaylist(rItem, false, null, null);
                }
            }
            addToPlaylist(mainFeature, false, audioStream, subtitleStream);
            Intent intent = new Intent(MediaDetailsActivity.this, PlaybackActivity.class);
            startActivity(intent);
        }
        @Override
        public void onError(Exception ex) {
            addToPlaylist(mainFeature, false, audioStream, subtitleStream);
            Intent intent = new Intent(MediaDetailsActivity.this, PlaybackActivity.class);
            startActivity(intent);
        }
    }

    private void addToPlaylist(BaseItemDto item, boolean resume, Integer audioStreamIndex, Integer subtitleStreamIndex) {
        PlaylistItem playableItem = new PlaylistItem();
        playableItem.Id = item.getId();
        playableItem.Name = item.getName();
        playableItem.startPositionTicks = resume && item.getUserData() != null ? item.getUserData().getPlaybackPositionTicks() : 0L;
        playableItem.Type = item.getType();

        if (item.getType().equalsIgnoreCase("episode"))
            playableItem.SecondaryText = mItem.getSeriesName();

        if (audioStreamIndex != null && audioStreamIndex != -1) {
            playableItem.AudioStreamIndex = audioStreamIndex;
        }
        if (subtitleStreamIndex != null && subtitleStreamIndex != -1) {
            playableItem.SubtitleStreamIndex = subtitleStreamIndex;
        }
        MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playableItem);
    }


    public void updateFavoriteVisibleIcons() {

        Log.i("", "updateFavoriteVisibleIcons called");
        FileLogger.getFileLogger().Info("Update favorite visible icons");

        if (mItem.getUserData() != null && mItem.getUserData().getIsFavorite()) {

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

    private void ShowStreamSelectionDialog() {

        StreamSelectionDialogFragment dialog = new StreamSelectionDialogFragment();
        dialog.setItem(mItem);
        dialog.setStreams(mSelectedAudioStreamIndex, mSelectedSubtitleStreamIndex);
        dialog.show(getSupportFragmentManager(), "StreamSelectionDialog");
    }

    public void onDialogPositiveClick(int audioStreamIndex, int subtitleStreamIndex, String mediaSourceId) {

        mSelectedAudioStreamIndex = audioStreamIndex;
        mSelectedSubtitleStreamIndex = subtitleStreamIndex;
        mSelectedMediaSourceId = mediaSourceId;
    }

    public void onDialogNegativeClick(DialogFragment dialog) {

        Log.i("", "Cancel button pressed");
    }

//    private void populateDirectors(BaseItemPerson[] people) {
//
//        String directors = "";
//
//        for (BaseItemPerson person : people) {
//            if (person.Type.equalsIgnoreCase("director")) {
//                if (!directors.isEmpty())
//                    directors += ", ";
//
//                directors += person.Name;
//            }
//        }
//
//        if (!directors.isEmpty()) {
//            LinearLayout DirectorInfo = (LinearLayout) findViewById(R.id.llDirectorInfo);
//            TextView DirectorTV = (TextView) findViewById(R.id.tvDirectorValue);
//            DirectorTV.setText(directors);
//            DirectorInfo.setVisibility(LinearLayout.VISIBLE);
//        }
//    }
//
//    private void populateWriters(BaseItemPerson[] people) {
//
//        String writers = "";
//
//        for (BaseItemPerson person : people) {
//            if (person.Type.equalsIgnoreCase("writer")) {
//                if (!writers.isEmpty())
//                    writers += ", ";
//
//                writers += person.Name;
//            }
//        }
//
//        if (!writers.isEmpty()) {
//            LinearLayout WriterInfo = (LinearLayout) findViewById(R.id.llWriterInfo);
//            TextView WriterTV = (TextView) findViewById(R.id.tvWriterValue);
//            WriterTV.setText(writers);
//            WriterInfo.setVisibility(LinearLayout.VISIBLE);
//        }
//    }

    private void ShowOfflineWarning() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Offline Media");
        builder.setMessage("This item is offline and cannot be played.");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                // Do nothing but close the dialog

                dialog.dismiss();
            }

        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    private void updatePlaystateVisibleIcons() {

        Log.i("", "updatePlaystateVisibleIcons called");
        FileLogger.getFileLogger().Info("Update playstate visible icons");

        if (mItem.getUserData() != null && mItem.getUserData().getPlayed()) {

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

//    private String getDefaultConfigPath() {
//        String internal = System.getenv("INTERNAL_STORAGE");
//        String external = System.getenv("EXTERNAL_STORAGE");
//
//        if (external != null) {
//            String confPath = external + File.separator + "retroarch.cfg";
//            if (new File(confPath).exists())
//                return confPath;
//        } else if (internal != null) {
//            String confPath = internal + File.separator + "retroarch.cfg";
//            if (new File(confPath).exists())
//                return confPath;
//        } else {
//            String confPath = "/mnt/extsd/retroarch.cfg";
//            if (new File(confPath).exists())
//                return confPath;
//        }
//
//        if (internal != null && new File(internal + File.separator + "retroarch.cfg").canWrite())
//            return internal + File.separator + "retroarch.cfg";
//        else if (external != null && new File(internal + File.separator + "retroarch.cfg")
//                .canWrite())
//            return external + File.separator + "retroarch.cfg";
//        else {
//            File file = getCacheDir();
//
//            if (file != null)
//                return file.getAbsolutePath() + File.separator + "retroarch.cfg";
//        }
//
//        return null;
//    }

    private Response<BaseItemDto> getItemResponse = new Response<BaseItemDto>() {

        @Override
        public void onResponse(BaseItemDto item) {
            if (item != null) {

                mItem = item;
                mFullDetailsDownloaded = true;

                updateFavoriteVisibleIcons();
                updatePlaystateVisibleIcons();

                if (mViewPager != null) {
                    PagerAdapter adapter = mViewPager.getAdapter();
                    if (adapter == null) {
                        FileLogger.getFileLogger().Info("MediaDetailsActivity: initializing viewpager");
                        mViewPager.setAdapter(new MediaPagerAdapter(getSupportFragmentManager()));
                    } else {
                        FileLogger.getFileLogger().Info("MediaDetailsActivity: viewpager already exists");
                        if (isFresh && adapter.getCount() == 1) {
                            PagerTitleStrip titleStrip = (PagerTitleStrip) findViewById(R.id.pager_title_strip);
                            titleStrip.setVisibility(View.GONE);
                        }
                    }
                }

                mBackdropSwitcher = (ViewSwitcher) findViewById(R.id.vsBackdropImages);

                // Means we are in landscape view.
                if (mBackdropSwitcher != null) {

                    mBackdropImage1 = (NetworkImageView) findViewById(R.id.ivMediaBackdrop1);
                    mBackdropImage1.setDefaultImageResId(R.drawable.default_backdrop);
                    mBackdropImage2 = (NetworkImageView) findViewById(R.id.ivMediaBackdrop2);

                    ImageOptions options;
                    mBackdropUrls = new ArrayList<>();

                    if (mItem.getType().equalsIgnoreCase("Episode")) {
                        if (mItem.getParentBackdropImageTags() != null &&
                                mItem.getParentBackdropImageTags().size() > 0) {

                            // TODO cycle parent backdrops
                            options = new ImageOptions();
                            options.setImageType(ImageType.Backdrop);
                            //mBackdropImage1.postDelayed(CycleBackdrop, 8000);

                            String imageUrl = MB3Application.getInstance().API.GetImageUrl(
                                    mItem.getParentBackdropItemId(), options);
                            mBackdropUrls.add(imageUrl);
                        }
                    } else {
                        if (mItem.getBackdropCount() > 0) {
                            for (int i = 0; i < mItem.getBackdropCount(); i++) {
                                options = new ImageOptions();
                                options.setImageType(ImageType.Backdrop);
                                options.setMaxWidth(720);
                                options.setImageIndex(i);

                                String imageUrl = MB3Application.getInstance().API.GetImageUrl(mItem, options);
                                mBackdropUrls.add(imageUrl);
                            }
                        }
                    }

                    if (mBackdropUrls != null && mBackdropUrls.size() > 0) {
                        if (mBackdropUrls.size() == 1) {
                            mBackdropImage1.setImageUrl(mBackdropUrls.get(0),
                                    MB3Application.getInstance().API.getImageLoader());
                        } else if (mBackdropUrls.size() > 1) {
                            mBackdropSwitcher.post(CycleBackdrop);
                        }
                    } else {
                        mBackdropImage1.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
                    }
                }

            } else {
                Log.i("GetInitialItemCallback", "result is null");
                FileLogger.getFileLogger().Error("MediaDetailsActivity: GetItemCallback - Result is null");
            }
            isFresh = false;
        }
        @Override
        public void onError(Exception ex) {

        }
    };


    private class MediaPagerAdapter extends FragmentPagerAdapter {

        public MediaPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            Bundle args = new Bundle();
            args.putSerializable("Item", MB3Application.getInstance().getJsonSerializer().SerializeToString(mItem));

            switch (position) {

                case 0:
                    MediaOverviewFragment mOverviewFragment = new MediaOverviewFragment();
                    mOverviewFragment.setArguments(args);
                    return mOverviewFragment;
                case 1:
                    MediaActorsFragment mActorsFragment = new MediaActorsFragment();
                    mActorsFragment.setArguments(args);
                    return mActorsFragment;

            }

            return null;
        }

        @Override
        public int getCount() {

            if (mItem != null && mItem.getPeople() != null && mItem.getPeople().length > 0) {
                return 2;
            } else {
                return 1;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {

            if (position == 0)
                return getResources().getString(R.string.overview_header);

            return getResources().getString(R.string.actors_header);
        }
    }

    private Response<BaseItemDto[]> getLocalTrailersResponse = new Response<BaseItemDto[]>() {

        @Override
        public void onResponse(BaseItemDto[] trailers) {

            if (trailers != null && trailers.length > 0) {

                Log.i("GetInitialItemCallback", "Trailers found");
                Log.i("GetInitialItemCallback", trailers[0].getId());

                // Just in case the TV Theme is still playing
                MB3Application.getInstance().StopMedia();

                handlePlayRequest(trailers[0], false, true);

            } else {
                if (trailers == null) {
                    Log.i("GetItemsCallback", "result is null or no trailers");
                    FileLogger.getFileLogger()
                            .Error("Error getting trailers");
                } else {
                    FileLogger.getFileLogger()
                            .Error("Empty list returned for trailers");
                }
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private class UpdateFavoriteResponse extends Response<UserItemDataDto> {
        @Override
        public void onResponse(UserItemDataDto userItemData) {

            if (userItemData == null) return;

            mItem.getUserData().setIsFavorite(userItemData.getIsFavorite());
            updateFavoriteVisibleIcons();
        }
    }

    private class UpdatePlaystateResponse extends Response<UserItemDataDto> {
        @Override
        public void onResponse(UserItemDataDto userItemData) {
            if (userItemData == null) return;
            mItem.getUserData().setPlayed(userItemData.getPlayed());
            updatePlaystateVisibleIcons();
        }
    }


    private Response<ThemeMediaResult> getThemeSongsResponse = new Response<ThemeMediaResult>() {

        @Override
        public void onResponse(ThemeMediaResult themeSongs) {

            if (themeSongs == null || themeSongs.getItems() == null || themeSongs.getItems().length < 1)
                return;

            String url = Utils.buildPlaybackUrl(themeSongs.getItems()[0], 0L, null, null, null);
            MB3Application.getInstance().PlayMedia(url);
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private void setBackdropImage(String imageUrl) {

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

    private Runnable CycleBackdrop = new Runnable() {

        @Override
        public void run() {

            if (mDying)
                return;

            if (mBackdropIndex >= mBackdropUrls.size())
                mBackdropIndex = 0;

            setBackdropImage(mBackdropUrls.get(mBackdropIndex));
            mBackdropIndex += 1;
            mBackdropSwitcher.postDelayed(this, 8000);
        }
    };


}
