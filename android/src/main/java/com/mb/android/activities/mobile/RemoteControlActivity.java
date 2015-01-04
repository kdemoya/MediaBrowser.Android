package com.mb.android.activities.mobile;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.mb.android.activities.BaseMbMobileActivity;
import com.mb.android.mediaroute.MediaBrowserControlIntent;
import com.mb.android.playbackmediator.cast.callbacks.IVideoCastConsumer;
import com.mb.android.playbackmediator.cast.callbacks.VideoCastConsumerImpl;
import com.mb.android.playbackmediator.cast.exceptions.CastException;
import com.mb.android.playbackmediator.cast.exceptions.NoConnectionException;
import com.mb.android.playbackmediator.cast.exceptions.TransientNetworkDisconnectionException;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.fragments.NavigationMenuFragment;
import com.mb.android.ui.mobile.homescreen.HomescreenActivity;
import com.mb.android.utils.Utils;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.BaseItemInfo;
import mediabrowser.model.entities.ImageType;
import com.mb.android.logging.FileLogger;

import mediabrowser.model.session.SessionInfoDto;

/**
 * Created by Mark on 12/12/13.
 *
 * Activity that presents a UI allowing the user to remotely control media playing back on another
 * client
 */
public class RemoteControlActivity extends BaseMbMobileActivity {

    private final String TAG = "RemoteControlActivity";
    private ActionBarDrawerToggle mDrawerToggle;
    private TextView remoteDevice;
    private NetworkImageView primaryImage;
    private NetworkImageView backdrop;
    private TextView titleText;
    private TextView subTitleText;
    private TextView currentTime;
    private TextView runtime;
    private ImageButton btnPrevious;
    private ImageButton btnRewind;
    private ImageButton btnPlay;
    private ImageButton btnStop;
    private ImageButton btnFastForward;
    private ImageButton btnNext;
    private ImageButton btnMute;
    private ImageButton btnVolUp;
    private ImageButton btnVolDown;
    private SeekBar sbProgress;
    private boolean playbackStarted = false;
    private int pulseInterval = 0;
    private long previousExtrapolatedPositionSeconds = 0;
    private long extrapolatedPositionSeconds = 0;
    // Once a seek has been performed. Contains the time offset used to calculate position
    private Long positionOffsetMilliseconds;
    private Integer mAudioStreamIndex = null;
    private Integer mSubtitleStreamIndex = null;
    private boolean initialCcSeekPerformed = true;
    private boolean isSeeking;
    private String itemId;
    private SessionInfoDto currentSession;
    private boolean isPaused;
    private boolean launchedFromNotification;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_remote_control);

        launchedFromNotification = getMb3Intent().getBooleanExtra("LAUNCHED_BY_NOTIFICATION", false);

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

        if (mActionBar != null) {
            mActionBar.setBackgroundDrawable(new ColorDrawable(0x70000000));
        }

        // If true then the screen was rotated. Take the last known values instead
        if (savedInstanceState != null) {
            extrapolatedPositionSeconds = savedInstanceState.getLong("extrapolatedPositionSeconds");
            positionOffsetMilliseconds = savedInstanceState.getLong("positionOffsetMilliseconds");
        }

        remoteDevice = (TextView) findViewById(R.id.tvRemoteDevice);
        titleText = (TextView) findViewById(R.id.tvRemoteItemName);
        subTitleText = (TextView) findViewById(R.id.tvTitleSubText);
        currentTime = (TextView) findViewById(R.id.tvRemoteCurrentTime);
        runtime = (TextView) findViewById(R.id.tvRemoteRuntime);
        primaryImage = (NetworkImageView) findViewById(R.id.ivRemotePrimaryImage);
        backdrop = (NetworkImageView) findViewById(R.id.ivRemoteBackdrop);
        btnPrevious = (ImageButton) findViewById(R.id.ibPrevious);
        btnRewind = (ImageButton) findViewById(R.id.ibRewind);
        btnPlay = (ImageButton) findViewById(R.id.ibPlay);
        btnStop = (ImageButton) findViewById(R.id.ibStop);
        btnFastForward = (ImageButton) findViewById(R.id.ibFastForward);
        btnNext = (ImageButton) findViewById(R.id.ibNext);
        sbProgress = (SeekBar) findViewById(R.id.sbRemoteprogressBar);
        btnMute = (ImageButton) findViewById(R.id.ibMute);
        btnVolUp = (ImageButton) findViewById(R.id.ibVolUp);
        btnVolDown = (ImageButton) findViewById(R.id.ibVolDown);

        // Set UI Listeners
        sbProgress.setOnSeekBarChangeListener(onSeekBarChanged);
        btnPrevious.setOnClickListener(onPreviousClick);
        btnRewind.setOnClickListener(onRewindClick);
        btnPlay.setOnClickListener(onPlayClick);
        btnStop.setOnClickListener(onStopClick);
        btnFastForward.setOnClickListener(onFastForwardClick);
        btnNext.setOnClickListener(onNextClick);
        btnVolDown.setOnClickListener(onVolumeDownClick);
        btnMute.setOnClickListener(onMuteClick);
        btnVolUp.setOnClickListener(onVolumeUpClick);

        // Update our CastConsumer reference
        mCastManager.removeVideoCastConsumer(mCastConsumer);
        mCastConsumer = castConsumer;
        mCastManager.addVideoCastConsumer(mCastConsumer);

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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onPause() {
        super.onPause();
        FileLogger.getFileLogger().Info("RemoteControlActivity: onPause");
        backdrop.removeCallbacks(onEverySecond);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            buildUi();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (launchedFromNotification) {
            Intent intent = new Intent(this, HomescreenActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            this.finish();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onConnectionRestored() {
        buildUi();
    }

    private void buildUi() {
        BaseItemDto item = (BaseItemDto) getMb3Intent().getSerializableExtra("DTOBaseItem");

        if (item != null) {
            BaseItemInfo baseItemInfo = new BaseItemInfo();
            baseItemInfo.setName(item.getName());
            baseItemInfo.setId(item.getId());
            baseItemInfo.setSeriesName(item.getSeriesName());
            if (item.getHasPrimaryImage()) {
                baseItemInfo.setPrimaryImageItemId(item.getId());
                baseItemInfo.setPrimaryImageTag("value_does_not_matter");
            }
            baseItemInfo.setBackdropItemId(item.getBackdropCount() > 0 ? item.getId() : item.getParentBackdropItemId() != null ? item.getParentBackdropItemId() : null);
            baseItemInfo.setRunTimeTicks(item.getRunTimeTicks());

            PopulateView(baseItemInfo);
            setVisibleControls(mCastManager.getRouteInfo());
        }
        backdrop.postDelayed(onEverySecond, 1000);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {

        bundle.putDouble("extrapolatedPositionSeconds", extrapolatedPositionSeconds);

        if (positionOffsetMilliseconds != null)
            bundle.putLong("positionOffsetMilliseconds", positionOffsetMilliseconds);

        super.onSaveInstanceState(bundle);
    }


    private void setPlayButtonImage(boolean paused) {
        if (paused) {
            btnPlay.setImageResource(R.drawable.vp_play_selector);
        } else {
            btnPlay.setImageResource(R.drawable.vp_pause_selector);
        }
    }

    private void setMuteButtonImage(boolean muted) {

        if (muted) {
            btnMute.setImageResource(R.drawable.vp_unmute_selector);
        } else {
            btnMute.setImageResource(R.drawable.vp_mute_selector);
        }
    }


    private void PopulateView(BaseItemInfo itemInfo) {

        if (itemInfo == null) return;

        // We've already processed this item. So return
        if (null != itemId && itemId.equalsIgnoreCase(itemInfo.getId())) return;

        itemId = itemInfo.getId();
        if ("episode".equalsIgnoreCase(itemInfo.getType())) {
            titleText.setText(itemInfo.getSeriesName() != null ? itemInfo.getSeriesName() : "");
            String subText = "";
            if (itemInfo.getParentIndexNumber() != null) {
                subText = "S" + String.valueOf(itemInfo.getParentIndexNumber());
            }
            if (itemInfo.getIndexNumber() != null) {
                subText += "E" + String.valueOf(itemInfo.getIndexNumber());
            }
            if (itemInfo.getIndexNumberEnd() != null && itemInfo.getIndexNumberEnd() != itemInfo.getIndexNumber()) {
                subText += "-" + String.valueOf(itemInfo.getIndexNumberEnd());
            }
            if (subText.length() > 0) {
                subText = subText + ": ";
            }
            subText += itemInfo.getName();
            subTitleText.setText(subText);
        } else {
            titleText.setText(itemInfo.getName());
            subTitleText.setText("");
        }

        currentTime.setText("0:00");
        sbProgress.setSecondaryProgress(0);

        if (itemInfo.getRunTimeTicks() == null || itemInfo.getRunTimeTicks() == 0) {
            sbProgress.setVisibility(SeekBar.INVISIBLE);
            runtime.setVisibility(TextView.INVISIBLE);
        } else {
            runtime.setText(Utils.PlaybackRuntimeFromMilliseconds(itemInfo.getRunTimeTicks() / 10000));
            sbProgress.setMax((int) (itemInfo.getRunTimeTicks() / 10000));
        }

        ImageOptions options = new ImageOptions();

        if (itemInfo.getHasPrimaryImage() && primaryImage != null) {

            int maxWidth = getScreenWidth() - 20;
            int maxHeight = getScreenHeight() - (int) (390 * getScreenDensity());

            options.setImageType(ImageType.Primary);
//            options.setMaxWidth(maxWidth);
            options.setHeight(maxHeight);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(itemInfo.getPrimaryImageItemId(), options);
            primaryImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        }

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(itemInfo.getBackdropItemId())) {

            options = new ImageOptions();
            options.setImageType(ImageType.Backdrop);
            options.setMaxHeight(getScreenHeight());
            options.setQuality(80);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(itemInfo.getBackdropItemId(), options);
            backdrop.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        }

        remoteDevice.setText(MB3Application.getInstance().getResources()
                .getString(R.string.casting_to_device, mCastManager.getDeviceName()));

    }


    //**********************************************************************************************
    // View Listeners
    //**********************************************************************************************

    /**
     * Previous Button
     */
    View.OnClickListener onPreviousClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };


    /**
     * Rewind Button
     */
    View.OnClickListener onRewindClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (currentSession == null || currentSession.getNowPlayingItem() == null) return;

            if (extrapolatedPositionSeconds - 30 >= 0) {
                int newPosition = ((int)extrapolatedPositionSeconds - 30) * 1000;
                try {
                    mCastManager.seek(newPosition);
                    btnFastForward.setEnabled(false);
                    btnRewind.setEnabled(false);
                } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    /**
     * Play Button
     */
    View.OnClickListener onPlayClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mCastManager.togglePlayback();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    /**
     * Stop Button
     */
    View.OnClickListener onStopClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                FileLogger.getFileLogger()
                        .Info("stop button pressed");
                mCastManager.stop();
                RemoteControlActivity.this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    /**
     * Fast Forward Button
     */
    View.OnClickListener onFastForwardClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (currentSession == null || currentSession.getNowPlayingItem() == null) return;

            if (currentSession.getNowPlayingItem().getRunTimeTicks() != null
                    && (extrapolatedPositionSeconds + 30) * 10000000 < currentSession.getNowPlayingItem().getRunTimeTicks()) {
                int newPosition = ((int)extrapolatedPositionSeconds + 30) * 1000;
                try {
                    mCastManager.seek(newPosition);
                    btnFastForward.setEnabled(false);
                    btnRewind.setEnabled(false);
                } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    /**
     * Next Button
     */
    View.OnClickListener onNextClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };


    /**
     * Volume Down Button
     */
    View.OnClickListener onVolumeDownClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mCastManager.incrementVolume(-MB3Application.VOLUME_INCREMENT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    /**
     * Mute Button
     */
    View.OnClickListener onMuteClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                boolean isMuted = mCastManager.isMute();
                mCastManager.setMute(!isMuted);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    /**
     * Volume Up Button
     */
    View.OnClickListener onVolumeUpClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mCastManager.incrementVolume(MB3Application.VOLUME_INCREMENT);
            } catch (CastException | NoConnectionException | TransientNetworkDisconnectionException e) {
                e.printStackTrace();
            }
        }
    };


    /**
     * Seek Bar (scrubber)
     */
    SeekBar.OnSeekBarChangeListener onSeekBarChanged = new SeekBar.OnSeekBarChangeListener() {
        int seekValue = -1;

        // called when the scrubber changes value either by touch or by code manipulation
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            seekValue = progress;

            // fromUser means we're responding to user touch specifically
            if (fromUser) {
                currentTime.setText(Utils.PlaybackRuntimeFromMilliseconds(seekValue));
            }
        }

        // called when the user starts touching the scrubber
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeeking = true;
        }

        // called when the user lifts their finger from the scrubber
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            isSeeking = false;
            try {
                Log.d("RemoteControlActivity", "seek");
                mCastManager.seek(seekValue);
            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                e.printStackTrace();
            }
        }
    };

    //**********************************************************************************************
    // Cast Consumer callback
    //**********************************************************************************************

    private IVideoCastConsumer castConsumer = new VideoCastConsumerImpl() {

        @Override
        public void onApplicationDisconnected(int errorCode) {
            RemoteControlActivity.this.finish();
        }

        @Override
        public void onRemoteMediaPlayerStatusUpdated() {

            currentSession = mCastManager.getCurrentSessionInfo();

            if (currentSession == null) return;

            if (currentSession.getNowPlayingItem() != null) {
                playbackStarted = true;
            }

            if (playbackStarted && currentSession.getNowPlayingItem() == null) {
                RemoteControlActivity.this.finish();
                return;
            }

            PopulateView(currentSession.getNowPlayingItem());

//            if (currentSession.getTranscodingInfo() != null) {
//                if (currentSession.getTranscodingInfo().getCompletionPercentage() != null) {
//                    sbProgress.setSecondaryProgress();
//                }
//            }

            if (currentSession.getPlayState() != null) {

                if (currentSession.getPlayState().getPositionTicks() != null) {
                    if (currentSession.getPlayState().getPositionTicks() / 10000000 != previousExtrapolatedPositionSeconds) {
                        previousExtrapolatedPositionSeconds = extrapolatedPositionSeconds = currentSession.getPlayState().getPositionTicks() / 10000000;
                    }
                }

                isPaused = currentSession.getPlayState().getIsPaused();
                setPlayButtonImage(isPaused);
                setMuteButtonImage(currentSession.getPlayState().getIsMuted());
                btnRewind.setEnabled(true);
                btnFastForward.setEnabled(true);
            }
        }

        @Override
        public void onDataMessageSendFailed(int errorCode) {
            Log.d(TAG, "onDataMessageSendFailed. Error Code: " + String.valueOf(errorCode));
        }

        @Override
        public void onDataMessageReceived(String message) {
//            Log.d(TAG, "onDataMessageReceived: " + message);
        }
    };

    //**********************************************************************************************
    // Utility Methods
    //**********************************************************************************************

    // only show controls that the route is capable of handling
    private void setVisibleControls(MediaRouter.RouteInfo routeInfo) {

        if (null == routeInfo) return;

        if (routeInfo.supportsControlCategory(CastMediaControlIntent.categoryForCast(MB3Application.getApplicationId()))) {
            // Show chromecast specific controls

        } else {

            if (!routeInfo.supportsControlAction(
                    MediaBrowserControlIntent.CATEGORY_MEDIA_BROWSER_COMMAND,
                    MediaBrowserControlIntent.ACTION_VOLUME_UP)) {
                btnVolUp.setVisibility(View.GONE);
            }
            if (!routeInfo.supportsControlAction(
                    MediaBrowserControlIntent.CATEGORY_MEDIA_BROWSER_COMMAND,
                    MediaBrowserControlIntent.ACTION_VOLUME_DOWN)) {
                btnVolDown.setVisibility(View.GONE);
            }
            if (!routeInfo.supportsControlAction(
                    MediaBrowserControlIntent.CATEGORY_MEDIA_BROWSER_COMMAND,
                    MediaBrowserControlIntent.ACTION_MUTE) &&
                    !routeInfo.supportsControlAction(
                            MediaBrowserControlIntent.CATEGORY_MEDIA_BROWSER_COMMAND,
                            MediaBrowserControlIntent.ACTION_TOGGLE_MUTE)) {
                btnMute.setVisibility(View.GONE);
            }

//            if (mCurrentSession == null) {
//                isChromecastSession = true;
//                btnFastForward.setVisibility(View.GONE);
//                btnRewind.setVisibility(View.GONE);
//                btnStop.setVisibility(View.GONE);
//            }
        }
    }

    private Runnable onEverySecond = new Runnable() {
        @Override
        public void run() {

            if (!isSeeking && playbackStarted && !isPaused) {
                extrapolatedPositionSeconds += 1;
                Log.d(TAG, "extrapolatedPositionSeconds: " + String.valueOf(extrapolatedPositionSeconds));

                sbProgress.setProgress((int) (extrapolatedPositionSeconds * 1000));
                currentTime.setText(Utils.PlaybackRuntimeFromMilliseconds(extrapolatedPositionSeconds * 1000));
            }
            backdrop.postDelayed(this, 1000);
        }
    };
}
