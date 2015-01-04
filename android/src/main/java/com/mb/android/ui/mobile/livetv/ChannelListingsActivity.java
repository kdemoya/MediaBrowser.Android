package com.mb.android.ui.mobile.livetv;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.mb.android.Playlist;
import com.mb.android.activities.BaseMbMobileActivity;
import com.mb.android.logging.FileLogger;
import com.mb.android.ui.tv.playback.PlayerHelpers;
import com.mb.android.utils.Utils;
import mediabrowser.apiinteraction.Response;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.MB3Application;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.activities.mobile.ProgramDetailsActivity;
import com.mb.android.adapters.ChannelListingsAdapter;
import com.mb.android.fragments.NavigationMenuFragment;
import com.mb.android.livetv.IListing;
import com.mb.android.livetv.ListingData;
import com.mb.android.livetv.ListingHeader;
import com.mb.android.player.AudioService;
import com.mb.android.ui.mobile.playback.PlaybackActivity;

import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.ProgramInfoDto;
import mediabrowser.model.livetv.ProgramQuery;
import mediabrowser.model.results.ProgramInfoDtoResult;
import mediabrowser.model.session.PlayCommand;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by Mark on 2014-06-01.
 *
 * Activity that shows a channel name and logo and a list of all known listings.
 */
public class ChannelListingsActivity extends BaseMbMobileActivity {

    private ActionBarDrawerToggle mDrawerToggle;
    private ChannelInfoDto mChannel;
    private ListView mChannelsListView;
    private ProgressBar mActivityIndicator;
    private TextView mErrorText;
    private Button mWatchNowButton;
    private boolean mIsFresh = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tv_activity_live_tv_channel_listings);
        mWatchNowButton = (Button) findViewById(R.id.btnPlay);
        mWatchNowButton.setOnClickListener(onPlayClick);

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

        String jsonData = getMb3Intent().getStringExtra("ChannelInfoDto");
        mChannel = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, ChannelInfoDto.class);

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
    public void onPause() {
        super.onPause();

        if (mChannelsListView != null) {
            mChannelsListView.removeCallbacks(onEveryMinute);
        }
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
            if (mChannel != null) {

                TextView title = (TextView) findViewById(R.id.tvNetworkName);
                title.setText(mChannel.getNumber() + " " + mChannel.getName());

                ImageOptions options = null;

                if (mChannel.getImageTags() != null && mChannel.getImageTags().containsKey(ImageType.Thumb)) {
                    options = new ImageOptions();
                    options.setImageType(ImageType.Thumb);
                } else if (mChannel.getHasPrimaryImage()) {
                    options = new ImageOptions();
                    options.setImageType(ImageType.Primary);
                }

                NetworkImageView networkLogo = (NetworkImageView) findViewById(R.id.ivNetworkLogo);
                networkLogo.setDefaultImageResId(R.drawable.default_tv_channel);

                if (options != null) {
                    options.setMaxWidth(500);
                    String imageUrl = MB3Application.getInstance().API.GetImageUrl(mChannel.getId(), options);
                    networkLogo.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
                } else {
                    networkLogo.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
                }

                mActivityIndicator = (ProgressBar) findViewById(R.id.pbActivityIndicator);
                mActivityIndicator.setVisibility(View.VISIBLE);
                mErrorText = (TextView) findViewById(R.id.tvErrorText);

                ProgramQuery query = new ProgramQuery();
                query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
                query.setChannelIdList(new String[]{ mChannel.getId() });

                MB3Application.getInstance().API.GetLiveTvProgramsAsync(query, new GetProgramsResponse());

                if (mCastManager.isConnected()) {
                    MediaRouter.RouteInfo routeInfo = mCastManager.getRouteInfo();
                    if (routeInfo == null || !routeInfo.supportsControlCategory(CastMediaControlIntent.categoryForCast(MB3Application.getApplicationId()))) {
                        mWatchNowButton.setVisibility(View.GONE);
                    }
                }
            }

            if (mChannelsListView != null) {
                mChannelsListView.post(onEveryMinute);
            }
            mIsFresh = false;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    private class GetProgramsResponse extends Response<ProgramInfoDtoResult> {
        @Override
        public void onResponse(ProgramInfoDtoResult result) {

            if (mActivityIndicator != null) {
                mActivityIndicator.setVisibility(View.GONE);
            }

            if (result == null) {
                mErrorText.setVisibility(View.VISIBLE);
                return;
            }

            if (result.getItems() == null || result.getItems().length == 0) {
                mErrorText.setText(MB3Application.getInstance().getResources().getString(R.string.no_listings_available));
                mErrorText.setVisibility(View.VISIBLE);
            } else {

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                DateFormat dayFormat = new SimpleDateFormat("EEEE");
                String currentDate = "";

                final List<IListing> listings = new ArrayList<>();

                for (ProgramInfoDto program : result.getItems()) {

                    Date date = Utils.convertToLocalDate(program.getStartDate());

                    String dateString = dateFormat.format(date);

                    if (!dateString.equals(currentDate)) {
                        currentDate = dateString;
                        ListingHeader header = new ListingHeader();
                        header.day = dayFormat.format(date);
                        header.date = dateString;
                        listings.add(header);
                    }

                    ListingData listing = new ListingData();
                    listing.programInfoDto = program;
                    listings.add(listing);
                }

                mChannelsListView = (ListView) findViewById(R.id.lvChannelListings);
                mChannelsListView.setAdapter(new ChannelListingsAdapter(ChannelListingsActivity.this, listings, MB3Application.getInstance().API));
                mChannelsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        if (listings.get(i) instanceof ListingData) {

                            ListingData data = (ListingData) listings.get(i);
                            String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(data.programInfoDto);

                            Intent intent = new Intent(ChannelListingsActivity.this, ProgramDetailsActivity.class);
                            intent.putExtra("program", jsonData);

                            startActivity(intent);
                        }
                    }
                });
                mChannelsListView.postDelayed(onEveryMinute, 60000);
            }
        }
    }

    View.OnClickListener onPlayClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AudioService.PlayerState currentState = MB3Application.getAudioService().getPlayerState();
            if (currentState.equals(AudioService.PlayerState.PLAYING) || currentState.equals(AudioService.PlayerState.PAUSED)) {
                MB3Application.getAudioService().stopMedia();
            }
            MB3Application.getInstance().PlayerQueue = new Playlist();
            if (mCastManager.isConnected()) {
                mCastManager.playItem(mChannel, PlayCommand.PlayNow, 0L);
//            } else if (PreferenceManager.getDefaultSharedPreferences(ChannelListingsActivity.this)
//                    .getBoolean("pref_enable_external_player", false)) {
//
//                FileLogger.getFileLogger().Info("Play requested: External player");
//
//                String url = MB3Application.getInstance().API.getApiUrl() + "/Video/" + mChannel.getId() + "/stream.wtv?static=true";
//
////                String url = info.ToUrl(MB3Application.getInstance().API.getApiUrl());
//                FileLogger.getFileLogger().Info("External player URL: " + url);
//                Log.d("External Player url", url);
//
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//                startActivity(intent);
            } else {
                PlaylistItem playableItem = new PlaylistItem();
                playableItem.Id = mChannel.getId();
                playableItem.Name = mChannel.getNumber() + " " + mChannel.getName();
                playableItem.startPositionTicks = 0L;
                playableItem.Type = "TvChannel";

                MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playableItem);

                Intent intent = new Intent(ChannelListingsActivity.this, PlaybackActivity.class);
                startActivity(intent);
            }
        }
    };

    Runnable onEveryMinute = new Runnable() {
        @Override
        public void run() {

            // TODO: Find a way to only update the dataset if the time crosses over to the next program
            ChannelListingsAdapter adapter = (ChannelListingsAdapter) mChannelsListView.getAdapter();
            if (adapter != null)
                adapter.refreshCurrentTime();
            mChannelsListView.postDelayed(this, 60000);
        }
    };
}
