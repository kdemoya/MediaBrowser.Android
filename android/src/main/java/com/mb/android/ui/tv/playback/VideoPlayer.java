package com.mb.android.ui.tv.playback;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.toolbox.NetworkImageView;
import com.dolby.dap.DolbyAudioProcessing;
import com.dolby.dap.OnDolbyAudioProcessingEventListener;
import com.jess.ui.TwoWayAdapterView;
import com.jess.ui.TwoWayGridView;
import com.mb.android.MB3Application;
import com.mb.android.Playlist;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.adapters.TvScenesAdapter;
import com.mb.android.adapters.TvStreamsAdapter;
import com.mb.android.interfaces.IWebsocketEventListener;
import com.mb.android.ui.mobile.album.BaseSongAdapter;
import com.mb.android.utils.TimeUtils;
import com.mb.android.utils.Utils;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import com.mb.android.logging.FileLogger;
import com.mb.android.subtitles.Caption;
import com.mb.android.subtitles.TimedTextObject;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ChapterInfoDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.ProgramInfoDto;
import mediabrowser.model.livetv.RecordingInfoDto;
import mediabrowser.model.querying.SessionQuery;
import mediabrowser.model.session.PlayRequest;
import mediabrowser.model.session.SessionInfoDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Created by Mark on 2014The -10-31.
 * Video player UI geared towards living room use with a hand held remote
 */
public class VideoPlayer extends FragmentActivity
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
        IWebsocketEventListener, OnDolbyAudioProcessingEventListener {

    private static final String TAG = "VideoPlayer";
    private VideoView mVideoView;
    private NetworkImageView mSplashScreen;
    private TextView mLoadingTitle;
    private TextView mLoadingSubTitle;
    private ProgressBar mActivityIndicator;
    private boolean mIsPaused;
    private boolean mIsDirectStreaming;
    private boolean mIsStreamingHls;
    private boolean mIsPrepared;
    private boolean mIsMuted;
    private float mCurrentVolume = 1.0f;
    private StreamInfo mStreamInfo;
    private long mLastProgressReport;
    private int mTruePlayerPositionMs;
    private int mPlayerPositionOffsetMs;
    private TimedTextObject mTimedTextObject;
    private Handler mSubtitleDisplayHandler;
    private static final int SUBTITLE_DISPLAY_INTERVAL = 100;
    private TextView mSubtitlesText;
    private int mCurrentlyPlayingIndex;
    private boolean performInitialResumeSeek;
    private long runtimeTicks;
    private LinearLayout mPlayerUi;
    private RelativeLayout mTransportControls;
    private boolean mPopUpVisible = false;
    private boolean mGridVisible = false;
    private NetworkImageView mNowPlayingImage;
    private TextView mNowPlayingTitle;
    private TextView mNowPlayingSecondaryText;
    private TextView mNowPlayingTertiaryText;
    private TextView mOverview;
    private TextView mCurrentPosition;
    private TextView mRuntime;
    private ProgressBar mSeekBar;
    private ImageView mPlayPauseButton;
    private ImageView mPreviousButton;
    private ImageView mRewindButton;
    private ImageView mFastForwardButton;
    private ImageView mNextButton;
    private ImageView mPlaylistButton;
    private ImageView mAudioButton;
    private ImageView mSubtitleButton;
    private ImageView mMuteButton;
    private ImageView mChapterButton;
    private MediaPlayer mPlayer;
    private ArrayList<MediaStream> audioStreams;
    private ArrayList<MediaStream> subtitleStreams;
    private TwoWayGridView selectionGrid;
    private TvScenesAdapter chapterAdapter;
    private ListView mPlaylist;
    private boolean mPlaylistVisible;
    private BaseSongAdapter playlistAdapter;
    private String tvProgramId;


    //******************************************************************************************************************
    // Base Class Overrides
    //******************************************************************************************************************

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_tv_video_player);

        mSplashScreen = (NetworkImageView) findViewById(R.id.ivBackdropImage1);
        mSplashScreen.setDefaultImageResId(R.drawable.default_backdrop);
        mLoadingTitle = (TextView) findViewById(R.id.tvNowLoadingTitle);
        mLoadingSubTitle = (TextView) findViewById(R.id.tvNowLoadingSubTitle);
        mActivityIndicator = (ProgressBar) findViewById(R.id.pbActivityIndicator);
        mVideoView = (VideoView) findViewById(R.id.vvPlaybackSurface);
        mSubtitlesText = (TextView) findViewById(R.id.tvSubtitles);
        mPlayerUi = (LinearLayout) findViewById(R.id.rlPlayerUi);
        mNowPlayingImage = (NetworkImageView) findViewById(R.id.ivNowPlayingImage);
        mNowPlayingImage.setDefaultImageResId(R.drawable.default_video_portrait);
        mNowPlayingTitle = (TextView) findViewById(R.id.tvPlaybackPrimaryText);
        mNowPlayingSecondaryText = (TextView) findViewById(R.id.tvPlaybackSecondaryText);
        mNowPlayingTertiaryText = (TextView) findViewById(R.id.tvPlaybackTertiaryText);
        mCurrentPosition = (TextView) findViewById(R.id.tvCurrentPosition);
        mRuntime = (TextView) findViewById(R.id.tvRuntime);
        mSeekBar = (ProgressBar) findViewById(R.id.pbPlaybackProgress);
        mTransportControls = (RelativeLayout) findViewById(R.id.llTransportControls);
        mOverview = (TextView) findViewById(R.id.tvMediaOverview);
        mPlaylist = (ListView) findViewById(R.id.lvPlayList);
        mPlayPauseButton = (ImageView) findViewById(R.id.ivPlayPause);
        mPlayPauseButton.setOnClickListener(onButtonClick);
        mPreviousButton = (ImageView) findViewById(R.id.ivPrevious);
        mPreviousButton.setOnClickListener(onButtonClick);
        mRewindButton = (ImageView) findViewById(R.id.ivRewind);
        mRewindButton.setOnClickListener(onButtonClick);
        mFastForwardButton = (ImageView) findViewById(R.id.ivFastForward);
        mFastForwardButton.setOnClickListener(onButtonClick);
        mNextButton = (ImageView) findViewById(R.id.ivNext);
        mNextButton.setOnClickListener(onButtonClick);
        mPlaylistButton = (ImageView) findViewById(R.id.ivPlaylistSelection);
        mPlaylistButton.setOnClickListener(onButtonClick);
        mAudioButton = (ImageView) findViewById(R.id.ivAudioSelection);
        mAudioButton.setOnClickListener(onButtonClick);
        mSubtitleButton = (ImageView) findViewById(R.id.ivSubtitleSelection);
        mSubtitleButton.setOnClickListener(onButtonClick);
        mChapterButton = (ImageView) findViewById(R.id.ivChapterSelection);
        mChapterButton.setOnClickListener(onButtonClick);
        mMuteButton = (ImageView) findViewById(R.id.ivAudioMute);
        mMuteButton.setOnClickListener(onButtonClick);
        selectionGrid = (TwoWayGridView) findViewById(R.id.gvSelectionGrid);

        mVideoView.setOnPreparedListener(VideoPlayer.this);
        mVideoView.setOnErrorListener(VideoPlayer.this);
        mVideoView.setOnCompletionListener(VideoPlayer.this);

        setOverscanValues();

        if (MB3Application.getInstance().PlayerQueue == null
                || MB3Application.getInstance().PlayerQueue.PlaylistItems == null
                || MB3Application.getInstance().PlayerQueue.PlaylistItems.size() == 0) {
            this.finish();
        }

//        Toast.makeText(this, "TYPE: " + String.valueOf(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Type), Toast.LENGTH_LONG).show();

        getItemAtPosition(0);

        DolbyAudioProcessing mDolbyAudioProcessing = DolbyAudioProcessing.getDolbyAudioProcessing(this, DolbyAudioProcessing.PROFILE.GAME, this);

        if (mDolbyAudioProcessing == null) {
            FileLogger.getFileLogger().Debug("Dolby NOT available");
        } else {
            FileLogger.getFileLogger().Debug("Dolby available!!!!!");
        }

        playlistAdapter = new BaseSongAdapter(MB3Application.getInstance().PlayerQueue.PlaylistItems, this);
        mPlaylist.setAdapter(playlistAdapter);
        mPlaylist.setOnItemClickListener(onPlaylistItemClick);

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(systemUiVisibilityChangeListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        FileLogger.getFileLogger().Info(TAG +": onResume");
        MB3Application.getInstance().setCurrentActivity(this);
//        mVideoView.postDelayed(onEverySecond, 1000);
        hideSystemUi();
    }

    /**
     *
     */
    @Override
    public void onPause() {
        super.onPause();
        FileLogger.getFileLogger().Info(TAG + ": onPause");
        playerPause();
        mIsPaused = true;
        clearReferences();
//        mVideoView.removeCallbacks(onEverySecond);
    }

    /**
     *
     */
    @Override
    public void onDestroy() {
        FileLogger.getFileLogger().Info(TAG + ": onDestroy");
        try {
            PlayerHelpers.sendPlaybackStoppedToServer(mStreamInfo, (long) mTruePlayerPositionMs * 10000, new EmptyResponse());
        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("Error sending playback stopped ", e);
        }

        clearReferences();

        if (mIsStreamingHls)
            MB3Application.getInstance().API.StopTranscodingProcesses(MB3Application.getInstance().API.getDeviceId(), new EmptyResponse());

        if (mSubtitleDisplayHandler != null) {
            mSubtitleDisplayHandler.removeCallbacks(processSubtitles);
            mSubtitleDisplayHandler = null;
        }

        if (mVideoView != null) {
            try {
                if (mIsPrepared) {
                    FileLogger.getFileLogger().Info(TAG + ": calling mPlayer.release");
                    mVideoView.stopPlayback();
                    FileLogger.getFileLogger().Info(TAG + ": mPlayer.release called");
                }
            } catch (Exception e) {
                FileLogger.getFileLogger().Error(TAG + ": Error releasing player");
                e.printStackTrace();
            } finally {
                mVideoView.clearFocus();
                mVideoView = null;
            }
        }

        if (mLoadingTitle != null) {
            mLoadingTitle.removeCallbacks(hidePlayerUiRunnable);
        }

        MB3Application.getInstance().releaseDolbyAudioProcessing();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!mGridVisible && !mPopUpVisible && !mPlaylistVisible) {
            super.onBackPressed();
        } else {
            if (mGridVisible) {
                hideSelectionGrid();
            }
            if (mPopUpVisible) {
                hidePlayerUi();
            }
            if (mPlaylistVisible) {
                hidePlaylist();
            }
        }
    }

    //******************************************************************************************************************
    //
    //******************************************************************************************************************

    private void getItemAtPosition(int position) {
        if ("recording".equalsIgnoreCase(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Type)) {
            MB3Application.getInstance().API.GetLiveTvRecordingAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(position).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    new RecordingResponse(this));
        } else if ("tvchannel".equalsIgnoreCase(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Type)) {
            MB3Application.getInstance().API.GetLiveTvChannelAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(position).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    new TvChannelResponse(this));
        } else {
            MB3Application.getInstance().API.GetItemAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(position).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    new BaseItemResponse(this));
        }
    }

    protected void onRecordingReceived(RecordingInfoDto recording) {
        if (recording == null) {
            Toast.makeText(VideoPlayer.this, "Error communicating with server", Toast.LENGTH_LONG).show();
            VideoPlayer.this.finish();
            return;
        }
        processReceivedItemData(recording.getId(), recording.getMediaSources());
        updateStreamCounts();
        selectionGrid.setAdapter(null);
        chapterAdapter = null;
        setVisibleControls(true);

        runtimeTicks = recording.getRunTimeTicks() != null
                ? recording.getRunTimeTicks()
                : 0;

        loadCurrentPlayingInfo(
                recording.getName(),
                recording.getEpisodeTitle(),
                recording.getChannelName(),
                recording.getHasPrimaryImage() ? recording.getId() : "",
                recording.getOverview()
        );
        setStartEndTimes(0, (int)(runtimeTicks / 10000));
    }

    protected void onTvChannelReceived(ChannelInfoDto item) {
        if (item == null) {
            Toast.makeText(VideoPlayer.this, "Error communicating with server", Toast.LENGTH_LONG).show();
            VideoPlayer.this.finish();
            return;
        }

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(tvProgramId)) {
            processReceivedItemData(item.getId(), item.getMediaSources());
            updateStreamCounts();
            selectionGrid.setAdapter(null);
            chapterAdapter = null;
            setVisibleControls(false);
        }

        ProgramInfoDto program = item.getCurrentProgram();

        if (program != null) {

            tvProgramId = program.getId();

            loadCurrentPlayingInfo(
                    program.getName(),
                    program.getEpisodeTitle(),
                    program.getChannelName(),
                    program.getHasPrimaryImage() ? program.getId() : item.getHasPrimaryImage() ? item.getId() : "",
                    program.getOverview()
            );
            setStartEndTimes(program.getStartDate(), program.getEndDate());
            if (program.getRunTimeTicks() != null) {
                mSeekBar.setMax((int)(program.getRunTimeTicks() / 10000));
                mPlayerPositionOffsetMs = TimeUtils.elapsedMillisecondsSinceTimestamp(program.getStartDate());
            }
        }
    }

    protected void onBaseItemReceived(BaseItemDto item) {
        if (item == null) {
            Toast.makeText(VideoPlayer.this, "Error communicating with server", Toast.LENGTH_LONG).show();
            VideoPlayer.this.finish();
            return;
        }

        if ("VodCastVideo".equalsIgnoreCase(item.getType())
                || "PodCastAudio".equalsIgnoreCase(item.getType())) {
            String mUrl = item.getPath();

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mUrl)) {
                loadUrlIntoPlayer(mUrl);
            }

        } else {
            setInitialLoadingSplashScreen(item);
            processReceivedItemData(item.getId(), item.getMediaSources());
            updateStreamCounts();
            if (item.getChapters() != null && !item.getChapters().isEmpty()) {
                chapterAdapter = new TvScenesAdapter(item, VideoPlayer.this);
            } else {
                chapterAdapter = null;
            }
            setVisibleControls(true);

            runtimeTicks = (mStreamInfo.getMediaSource() != null && mStreamInfo.getMediaSource().getRunTimeTicks() != null)
                    ? mStreamInfo.getMediaSource().getRunTimeTicks()
                    : 0;

            if ("episode".equalsIgnoreCase(item.getType())) {
                loadCurrentPlayingInfo(
                        item.getSeriesName(),
                        item.getName(),
                        Utils.getLongEpisodeIndexString(item),
                        item.getSeriesPrimaryImageTag() != null ? item.getSeriesId() : "",
                        item.getOverview()
                );
            } else {
                loadCurrentPlayingInfo(
                        item.getName(),
                        null,
                        null,
                        item.getHasPrimaryImage() ? item.getId() : "",
                        item.getOverview()
                );
            }
            setStartEndTimes(0, (int)(runtimeTicks / 10000));
        }
    }

    private void processReceivedItemData(String itemId, ArrayList<MediaSourceInfo> mediaSources) {

        // necessary evils for non-hls transcoding
        mPlayerPositionOffsetMs = 0;
        mTruePlayerPositionMs = 0;

        boolean resume = mCurrentlyPlayingIndex == 0 && MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).startPositionTicks != null && MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).startPositionTicks > 0L;

        mIsDirectStreaming = false;
        mIsStreamingHls = false;

        mStreamInfo = PlayerHelpers.buildStreamInfoVideo(
                itemId,
                mediaSources,
                resume ? MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).startPositionTicks : 0L,
                null,
                mCurrentlyPlayingIndex == 0 ? MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).AudioStreamIndex : null,
                mCurrentlyPlayingIndex == 0 ? MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).SubtitleStreamIndex : null
        );

        if (mStreamInfo != null) {
            mIsStreamingHls = mStreamInfo.getProtocol() != null && mStreamInfo.getProtocol().equalsIgnoreCase("hls");
            mIsDirectStreaming = mStreamInfo.getIsDirectStream();
            loadStreamInfoIntoPlayer();

            if (resume) {
                if (mIsStreamingHls) {
                    performInitialResumeSeek = true;
                } else {
                    mPlayerPositionOffsetMs = (int)(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).startPositionTicks / 10000);
                }
            }
        }
    }

    //******************************************************************************************************************
    // SplashScreen methods
    //******************************************************************************************************************

    /**
     * Display a backdrop image for the currently loading video
     *
     * @param item The item to show the backdrop for
     */
    private void setInitialLoadingSplashScreen(BaseItemDto item) {

        ImageOptions options;

        if (item.getType().equalsIgnoreCase("Episode")) {

            if (item.getParentBackdropImageTags() != null &&
                    item.getParentBackdropImageTags().size() > 0) {

                options = new ImageOptions();
                options.setImageType(ImageType.Backdrop);

                String imageUrl = MB3Application.getInstance().API.GetImageUrl(item.getParentBackdropItemId(), options);
                mSplashScreen.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            } else {
                mSplashScreen.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
            }

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getSeriesName())) {
                mLoadingTitle.setText(item.getSeriesName());
                mLoadingSubTitle.setText(item.getName());
                mLoadingSubTitle.setVisibility(View.VISIBLE);
            }

        } else if (item.getBackdropCount() > 0) {

            options = new ImageOptions();
            options.setImageType(ImageType.Backdrop);
            options.setWidth(getResources().getDisplayMetrics().widthPixels);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(item, options);
            mSplashScreen.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());

            mLoadingTitle.setText(item.getName());
        } else {
            mSplashScreen.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
        }

        mLoadingTitle.setVisibility(View.VISIBLE);
        mSplashScreen.setVisibility(View.VISIBLE);
    }

    /**
     *  Hide the backdrop since the video has now loaded
     */
    private void hideInitialLoadingImage() {

        mSplashScreen.setVisibility(View.GONE);
        mLoadingTitle.setVisibility(View.GONE);
        mActivityIndicator.setVisibility(View.GONE);
        mLoadingSubTitle.setVisibility(View.GONE);
    }

    //******************************************************************************************************************
    // Input methods
    //******************************************************************************************************************

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                onPlayPauseButton();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                onPlayButton();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                onPauseButton();
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                onFastForwardButton();
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                this.finish();
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                onRewindButton();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (mGridVisible || mPlaylistVisible) {
                    break;
                } else if (mPopUpVisible) {
                    showPlayerUi(); // we're just calling this to reset the timer before it vanishes
                    break;
                } else {
                    showPlayerUi();
                    mPlayPauseButton.requestFocus();
                    return true;
                }
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (mGridVisible || mPlaylistVisible) {
                    break;
                } else if (mPopUpVisible) {
                    showPlayerUi(); // we're just calling this to reset the timer before it vanishes
                    break;
                } else {
                    showPlayerUi();
                    mPlayPauseButton.requestFocus();
                    return true;
                }
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (mGridVisible || mPlaylistVisible) {
                    break;
                } else if (mPopUpVisible) {
                    showPlayerUi(); // we're just calling this to reset the timer before it vanishes
                    break;
                } else {
                    showPlayerUi();
                    mPlayPauseButton.requestFocus();
                    return true;
                }
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (mGridVisible || mPlaylistVisible) {
                    break;
                } else if (mPopUpVisible) {
                    showPlayerUi(); // we're just calling this to reset the timer before it vanishes
                    break;
                } else {
                    showPlayerUi();
                    mPlayPauseButton.requestFocus();
                    return true;
                }
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (mGridVisible || mPlaylistVisible) {
                    break;
                } else if (mPopUpVisible) {
                    showPlayerUi(); // we're just calling this to reset the timer before it vanishes
                    break;
                } else {
                    showPlayerUi();
                    mPlayPauseButton.requestFocus();
                    return true;
                }
            case KeyEvent.KEYCODE_VOLUME_UP:
                onVolumeUp();
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                onVolumeDown();
                return true;
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                onMuteUnmuteButton();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    View.OnClickListener onButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.ivPlayPause:
                    onPlayPauseButton();
                    break;
                case R.id.ivPrevious:
                    onPreviousButton();
                    break;
                case R.id.ivRewind:
                    onRewindButton();
                    break;
                case R.id.ivFastForward:
                    onFastForwardButton();
                    break;
                case R.id.ivChapterSelection:
                    onChapterSelection();
                    break;
                case R.id.ivAudioSelection:
                    onAudioSelection();
                    break;
                case R.id.ivSubtitleSelection:
                    onSubtitleSelection();
                    break;
                case R.id.ivNext:
                    onNextButton();
                    break;
                case R.id.ivPlaylistSelection:
                    onPlaylistSelection();
                    break;
//                case R.id.ivDislike:
//                    onDislikeButton();
//                    break;
//                case R.id.ivLike:
//                    onLikeButton();
//                    break;
//                case R.id.ivFavorite:
//                    onFavoriteButton();
//                    break;
                case R.id.ivAudioMute:
                    onMuteUnmuteButton();
                    break;
            }
        }
    };

    /**
     * Jump forward 30 seconds if possible. If the remaining play time of a video is less than the seek amount then the
     * next video in the queue will be started
     */
    private void onFastForwardButton() {
        if (mVideoView == null) {
            return;
        }

        int seekTargetMs = mTruePlayerPositionMs + TimeUtils.secondsToMs(30);

        if (TimeUtils.msToTicks(seekTargetMs) < runtimeTicks) {
            if (mIsStreamingHls || mIsDirectStreaming) {
                mVideoView.seekTo(seekTargetMs);
            } else {
                // In case the user is hammering on the ff/rw button
                if (!mIsPrepared) return;

                mVideoView.stopPlayback();
                mStreamInfo.setStartPositionTicks(seekTargetMs);
                mPlayerPositionOffsetMs = seekTargetMs;

                mIsPrepared = false;

                loadStreamInfoIntoPlayer();
            }
            showPlayerUi();
        } else {
            onNextButton();
        }
    }

    /**
     * Jump back 30 seconds if possible.
     */
    private void onRewindButton() {
        if (mVideoView == null) {
            return;
        }
        int seekTargetMs = mTruePlayerPositionMs - TimeUtils.secondsToMs(30);
        if (seekTargetMs < 0) {
            seekTargetMs = 0;
        }

        if (mIsStreamingHls || mIsDirectStreaming) {
            mVideoView.seekTo(seekTargetMs);
        } else {
            // In case the user is hammering on the ff/rw button
            if (!mIsPrepared) return;

            mIsPrepared = false;
            mVideoView.stopPlayback();

            mStreamInfo.setStartPositionTicks(TimeUtils.msToTicks(seekTargetMs));
            mPlayerPositionOffsetMs = seekTargetMs;
            loadStreamInfoIntoPlayer();
        }

        showPlayerUi();
    }

    private void onNextButton() {

    }

    private void onPreviousButton() {

    }

    private void onPlayPauseButton() {
        FileLogger.getFileLogger().Info(TAG + ": onPlayPauseButton");
        if (mIsPaused) {
            onPlayButton();
        } else {
            onPauseButton();
        }
        showPlayerUi();
    }

    private void onPlayButton() {
        FileLogger.getFileLogger().Info(TAG + ": onPlayButton");
        mPlayPauseButton.setImageResource(R.drawable.vp_pause_selector);
        playerStart();
        showPlayerUi();
    }

    private void onPauseButton() {
        FileLogger.getFileLogger().Info(TAG + ": onPauseButton");
        if (mIsPaused) {
            onPlayButton();
        } else {
            mPlayPauseButton.setImageResource(R.drawable.vp_play_selector);
            playerPause();
        }
        showPlayerUi();
    }

    private void onMuteUnmuteButton() {
        if (mPlayer == null) return;

        mIsMuted = !mIsMuted;
        mMuteButton.setImageResource(mIsMuted ? R.drawable.vp_unmute_selector : R.drawable.vp_mute_selector);
        mPlayer.setVolume(
                mIsMuted ? 0 : mCurrentVolume,
                mIsMuted ? 0 : mCurrentVolume
        );
    }

    private void onAudioSelection() {
        hidePlayerUi();
        selectionGrid.setAdapter(new TvStreamsAdapter(audioStreams, mStreamInfo.getAudioStreamIndex(), VideoPlayer.this));
        selectionGrid.setOnItemClickListener(onStreamSelected);
        selectionGrid.setVisibility(View.VISIBLE);
        selectionGrid.requestFocus();
        mGridVisible = true;
    }

    private void onSubtitleSelection() {
        hidePlayerUi();
        selectionGrid.setAdapter(new TvStreamsAdapter(subtitleStreams, mStreamInfo.getSubtitleStreamIndex(), VideoPlayer.this));
        selectionGrid.setOnItemClickListener(onStreamSelected);
        selectionGrid.setVisibility(View.VISIBLE);
        selectionGrid.requestFocus();
        mGridVisible = true;
    }

    private void onChapterSelection() {
        hidePlayerUi();
        selectionGrid.setAdapter(chapterAdapter);
        selectionGrid.setOnItemClickListener(onChapterSelected);
        selectionGrid.setVisibility(View.VISIBLE);
        selectionGrid.requestFocus();
        mGridVisible = true;
    }

    private void onPlaylistSelection() {
        hidePlayerUi();
        showPlaylist();
    }


    TwoWayAdapterView.OnItemClickListener onChapterSelected = new TwoWayAdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(TwoWayAdapterView<?> parent, View view, int position, long id) {
            try {
                ChapterInfoDto chapterInfoDto = (ChapterInfoDto) parent.getItemAtPosition(position);
                if (chapterInfoDto == null) return;
                onSeekCommand(chapterInfoDto.getStartPositionTicks());
            } catch (ClassCastException ex) {
                FileLogger.getFileLogger().Error("Something went wrong determining chapter position");
            }
            selectionGrid.setVisibility(View.GONE);
            mGridVisible = false;
        }
    };

    TwoWayAdapterView.OnItemClickListener onStreamSelected = new TwoWayAdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(TwoWayAdapterView<?> parent, View view, int position, long id) {
            try {
                MB3Application.getInstance().PlayerQueue.PlaylistItems.get(mCurrentlyPlayingIndex).startPositionTicks = (long)mTruePlayerPositionMs * 10000;
                MediaStream stream = (MediaStream) parent.getItemAtPosition(position);
                if (stream == null) return;

                ArrayList<MediaSourceInfo> mediaSources = new ArrayList<>();
                mediaSources.add(mStreamInfo.getMediaSource());

                if (stream.getType().equals(MediaStreamType.Audio)) {
                    mStreamInfo.setAudioStreamIndex(stream.getIndex());
                } else if (stream.getType().equals(MediaStreamType.Subtitle)) {
                    mStreamInfo.setSubtitleStreamIndex(stream.getIndex() != -33 ? stream.getIndex() : null);
                }
                mStreamInfo = PlayerHelpers.buildStreamInfoVideo(
                        mStreamInfo.getItemId(),
                        mediaSources,
                        (long)mTruePlayerPositionMs * 10000,
                        mStreamInfo.getMediaSourceId(),
                        mStreamInfo.getAudioStreamIndex(),
                        mStreamInfo.getSubtitleStreamIndex()
                );
                if (mStreamInfo != null) {
                    mVideoView.stopPlayback();
                    mIsStreamingHls = mStreamInfo.getProtocol() != null && mStreamInfo.getProtocol().equalsIgnoreCase("hls");
                    mIsDirectStreaming = mStreamInfo.getIsDirectStream();
                    loadStreamInfoIntoPlayer();

                    if (mIsStreamingHls) {
                        performInitialResumeSeek = true;
                    } else {
                        mPlayerPositionOffsetMs = (int)(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).startPositionTicks / 10000);
                    }
                }


            } catch (ClassCastException ex) {
                FileLogger.getFileLogger().Error("Something went wrong determining stream index");
            }
            selectionGrid.setVisibility(View.GONE);
            mGridVisible = false;
        }
    };

    AdapterView.OnItemClickListener onPlaylistItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (MB3Application.getInstance().PlayerQueue.PlaylistItems.size() <= position) {
                FileLogger.getFileLogger().Debug("Invalid playlist index");
                return;
            }
            if (mVideoView != null) {
                mVideoView.stopPlayback();
                FileLogger.getFileLogger().Info(TAG + ": video player killed");
            }
            mCurrentlyPlayingIndex = position;
            if (mIsStreamingHls) {
                MB3Application.getInstance().API.StopTranscodingProcesses(
                        MB3Application.getInstance().API.getDeviceId(), new EmptyResponse() {
                    @Override
                    public void onResponse() {
                        getItemAtPosition(mCurrentlyPlayingIndex);
                    }
                    @Override
                    public void onError(Exception ex) {
                        getItemAtPosition(mCurrentlyPlayingIndex);
                    }
                });
            } else {
                getItemAtPosition(position);
            }
            hidePlaylist();
        }
    };


    private void onVolumeUp() {
        if (mCurrentVolume < 1 && mPlayer != null) {
            mCurrentVolume += .05;
            mPlayer.setVolume(mCurrentVolume, mCurrentVolume);
        }
    }

    private void onVolumeDown() {
        if (mCurrentVolume > 0 && mPlayer != null) {
            mCurrentVolume -= .05;
            mPlayer.setVolume(mCurrentVolume, mCurrentVolume);
        }
    }


    //******************************************************************************************************************
    // MediaPlayer callbacks
    //******************************************************************************************************************

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }


    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

        playlistAdapter.setCurrentPlayingIndex(mCurrentlyPlayingIndex);
        mPlayer = mediaPlayer;
        mIsPrepared = true;
        hideInitialLoadingImage();
        PlayerHelpers.sendPlaybackStartedToServer(mStreamInfo, 0L, mCurrentVolume, mIsMuted, mIsPaused, new EmptyResponse());
        mLastProgressReport = SystemClock.elapsedRealtime();
        mVideoView.post(onEverySecond);
        PlayerHelpers.downloadSubtitles(mStreamInfo, subtitleResponse);
        if (MB3Application.getInstance().PlayerQueue != null && MB3Application.getInstance().PlayerQueue.PlaylistItems.size() == 1) {
            setResult(RESULT_OK);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

        PlayerHelpers.sendPlaybackStoppedToServer(mStreamInfo, (long)mediaPlayer.getCurrentPosition() * 10000, new EmptyResponse());
        mVideoView.removeCallbacks(onEverySecond);
        mVideoView.stopPlayback();
        mCurrentlyPlayingIndex++;

        if (noMoreItemsToPlay()) {
            this.finish();
            return;
        }

        getItemAtPosition(mCurrentlyPlayingIndex);
    }

    private boolean noMoreItemsToPlay() {
        return MB3Application.getInstance().PlayerQueue == null
                || MB3Application.getInstance().PlayerQueue.PlaylistItems == null
                || MB3Application.getInstance().PlayerQueue.PlaylistItems.size() <= mCurrentlyPlayingIndex;
    }

    //******************************************************************************************************************
    // MediaPlayer wrapper methods
    //******************************************************************************************************************

    private void playerStart() {
        try {
            if (mVideoView != null && !mVideoView.isPlaying()) {
                mVideoView.start();
            }
            mIsPaused = false;
        } catch (IllegalStateException e) {
            Log.d(TAG, "Error pausing player");
        }
    }

    private void playerPause() {
        try {
            if (mVideoView != null && mVideoView.isPlaying()) {
                mVideoView.pause();
            }
            mIsPaused = true;
        } catch (IllegalStateException e) {
            Log.d(TAG, "Error pausing player");
        }
    }

    private void loadStreamInfoIntoPlayer() {
        if (mVideoView != null) {
            loadUrlIntoPlayer(mStreamInfo.ToUrl(MB3Application.getInstance().API.getApiUrl()));
        }
    }


    private void loadUrlIntoPlayer(String url) {

        FileLogger.getFileLogger().Info(TAG + ": attempting to play - " + url);
        try {
            mVideoView.setVideoPath(url);
            mVideoView.start();
        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("Exception handled: ", e);
        }
    }


    /**
     * Runnable that fires once a second to update the various UI controls
     */
    private Runnable onEverySecond = new Runnable() {

        public void run() {

            if (mVideoView == null) return;

            if (mVideoView.getCurrentPosition() > 0 && performInitialResumeSeek) {
                performInitialResumeSeek = false;
                mVideoView.seekTo((int)(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(mCurrentlyPlayingIndex).startPositionTicks / 10000));
            }

            // Report current position to the server only every 5 seconds.
            if (mLastProgressReport > 0 && SystemClock.elapsedRealtime() - mLastProgressReport > 5000) {
                PlayerHelpers.sendPlaybackProgressToServer(
                        mStreamInfo,
                        TimeUtils.msToTicks(mTruePlayerPositionMs),
                        mCurrentVolume,
                        mIsMuted,
                        mIsPaused,
                        new EmptyResponse()
                );
                mLastProgressReport = SystemClock.elapsedRealtime();
                if (mSeekBar.getSecondaryProgress() != mSeekBar.getMax()) {
                    // TODO fix session info
//                    MB3Application.getInstance().API.GetCurrentSessionAsync(getCurrentSessionResponse);
                    SessionQuery query = new SessionQuery();
                    MB3Application.getInstance().API.GetClientSessionsAsync(query, getCurrentSessionResponse);
                }
            }

            // No point going on. If playback is paused then the values haven't changed.
            if (mIsPrepared && !mIsPaused) {
                /*
                For non-hls playback we need to employ offset calculations because the player will see the stream as
                shorter than the actual runtime when seeking.
                */
                mTruePlayerPositionMs = mVideoView.getCurrentPosition() + mPlayerPositionOffsetMs;
                updateCurrentPosition(mTruePlayerPositionMs,
                        "tvchannel".equalsIgnoreCase(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(mCurrentlyPlayingIndex).Type));
            }

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(tvProgramId)) {
                // This means we're playing a TV channel and the player is initialized.
                if (mSeekBar.getProgress() >= mSeekBar.getMax()) {
                    MB3Application.getInstance().API.GetLiveTvChannelAsync(
                            MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Id,
                            MB3Application.getInstance().API.getCurrentUserId(),
                            new TvChannelResponse(VideoPlayer.this));
                }
            }

            mVideoView.postDelayed(onEverySecond, 1000);
        }
    };


    private Response<SessionInfoDto[]> getCurrentSessionResponse = new Response<SessionInfoDto[]>() {
        @Override
        public void onResponse(SessionInfoDto[] sessionInfoDto) {

            if (sessionInfoDto == null || sessionInfoDto.length == 0) {
                return;
            }
            for (SessionInfoDto info : sessionInfoDto) {
                if (MB3Application.getInstance().API.getDeviceId().equalsIgnoreCase(info.getDeviceId())) {
                    if (info.getTranscodingInfo() != null && info.getTranscodingInfo().getCompletionPercentage() != null) {
                        mSeekBar.setSecondaryProgress((int) (mSeekBar.getMax() * (info.getTranscodingInfo().getCompletionPercentage() / 100)));
                    }
                    break;
                }
            }
        }
    };

    private Response<TimedTextObject> subtitleResponse = new Response<TimedTextObject>() {
        @Override
        public void onResponse(TimedTextObject timedTextObject) {
            if (timedTextObject == null) return;
            mTimedTextObject = timedTextObject;

            try {
                String subSize = PreferenceManager.getDefaultSharedPreferences(VideoPlayer.this).getString("pref_subtitle_size", "18");
                mSubtitlesText.setTextSize(Float.valueOf(subSize));
            } catch (Exception e) {
                mSubtitlesText.setTextSize(18);
            }
            FileLogger.getFileLogger().Info("Subtitle Downloader: Create display handler");
            mSubtitleDisplayHandler = new Handler();
            mSubtitleDisplayHandler.post(processSubtitles);
            FileLogger.getFileLogger().Info("Subtitle Downloader: Finished!");
        }
        @Override
        public void onError(Exception ex) {

        }
    };


    private Runnable processSubtitles = new Runnable() {
        @Override
        public void run() {

            if (mVideoView != null && mVideoView.isPlaying()) {

                if (mTimedTextObject == null) return;
                // We have subs to process
                Collection<Caption> subtitles = mTimedTextObject.captions.values();
                for (Caption caption : subtitles) {
                    if (mTruePlayerPositionMs >= caption.start.getMilliseconds() && mTruePlayerPositionMs <= caption.end.getMilliseconds()) {
                        onTimedText(caption);
                        break;
                    } else if (mTruePlayerPositionMs > caption.end.getMilliseconds()) {
                        onTimedText(null);
                    }
                }
            }

            mSubtitleDisplayHandler.postDelayed(this, SUBTITLE_DISPLAY_INTERVAL);
        }
    };


    public void onTimedText(Caption text) {

        if (text == null) {
            mSubtitlesText.setVisibility(View.INVISIBLE);
            return;
        }
        mSubtitlesText.setText(Html.fromHtml(text.content));
        mSubtitlesText.setVisibility(View.VISIBLE);
    }

    //******************************************************************************************************************


    private void setVisibleControls(boolean canSeek) {

        mFastForwardButton.setVisibility(canSeek ? View.VISIBLE : View.INVISIBLE);
        mRewindButton.setVisibility(canSeek ? View.VISIBLE : View.INVISIBLE);

        int playlistItemCount = MB3Application.getInstance().PlayerQueue != null && MB3Application.getInstance().PlayerQueue.PlaylistItems != null
                ? MB3Application.getInstance().PlayerQueue.PlaylistItems.size()
                : 0;

        mNextButton.setVisibility(playlistItemCount > 1 ? View.VISIBLE : View.INVISIBLE);
        mPreviousButton.setVisibility(playlistItemCount > 1 ? View.VISIBLE : View.INVISIBLE);
        mPlaylistButton.setVisibility(playlistItemCount > 1 ? View.VISIBLE : View.GONE);

        mAudioButton.setVisibility(audioStreams != null && audioStreams.size() > 1 ? View.VISIBLE : View.GONE);
        mSubtitleButton.setVisibility(subtitleStreams != null && subtitleStreams.size() > 1 ? View.VISIBLE : View.GONE);
        mChapterButton.setVisibility(chapterAdapter != null ? View.VISIBLE : View.GONE);
    }

    private void showPlayerUi() {
        if (!mPopUpVisible) {
            mPlayerUi.setVisibility(View.VISIBLE);
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.grow_vertical);
            mTransportControls.startAnimation(animation);
            mTransportControls.setVisibility(View.VISIBLE);
            mPopUpVisible = true;
        }
        mLoadingTitle.removeCallbacks(hidePlayerUiRunnable);
        mLoadingTitle.postDelayed(hidePlayerUiRunnable, 5000);
    }

    private Runnable hidePlayerUiRunnable = new Runnable() {
        @Override
        public void run() {
            hidePlayerUi();
        }
    };

    private void hidePlayerUi() {
        if (!mPopUpVisible) return;
        mPlayerUi.setVisibility(View.GONE);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.shrink_vertical);
        mTransportControls.startAnimation(animation);
        mTransportControls.setVisibility(View.INVISIBLE);
        mPopUpVisible = false;
    }

    private void showPlaylist() {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.grow_horizontal);
        mPlaylist.startAnimation(animation);
        mPlaylist.setVisibility(View.VISIBLE);
        mPlaylist.requestFocus();
        mPlaylistVisible = true;
    }

    private void hidePlaylist() {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.shrink_horizontal);
        mPlaylist.startAnimation(animation);
        mPlaylist.setVisibility(View.GONE);
        mPlaylistVisible = false;
    }

    private void hideSelectionGrid() {
        selectionGrid.setVisibility(View.GONE);
        mGridVisible = false;
    }


    private void setOverscanValues() {
        RelativeLayout overscanLayout = (RelativeLayout) findViewById(R.id.rlOverscanPadding);

        if (overscanLayout == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int left = prefs.getInt("overscan_left", 0);
        int top = prefs.getInt("overscan_top", 0);
        int right = prefs.getInt("overscan_right", 0);
        int bottom = prefs.getInt("overscan_bottom", 0);

        ViewGroup.MarginLayoutParams overscanMargins = (ViewGroup.MarginLayoutParams) overscanLayout.getLayoutParams();
        overscanMargins.setMargins(left, top, right, bottom);
        overscanLayout.requestLayout();
    }

    @Override
    public void onTakeScreenshotRequest() {

    }

    @Override
    public void onRemotePlayRequest(PlayRequest request, String mediaType) {
        FileLogger.getFileLogger().Info(TAG + ": remote play request received");
        if ("audio".equalsIgnoreCase(mediaType)) {
            FileLogger.getFileLogger().Info(TAG + ": first item is audio.");
            if (mVideoView != null) {
                mVideoView.stopPlayback();
            }
            MB3Application.getInstance().PlayerQueue = new Playlist();
            addItemsToPlaylist(request.getItemIds());
            FileLogger.getFileLogger().Info(TAG + ": video player killed");
            Intent intent = new Intent(this, AudioPlayer.class);
            startActivity(intent);
            this.finish();
            FileLogger.getFileLogger().Info(TAG + ": finished audio play request");
        } else if ("video".equalsIgnoreCase(mediaType)) {
            FileLogger.getFileLogger().Info(TAG + ": first item is video");
            if (mVideoView != null) {
                mVideoView.stopPlayback();
            }
            MB3Application.getInstance().PlayerQueue = new Playlist();
            addItemsToPlaylist(request.getItemIds());
            FileLogger.getFileLogger().Info(TAG + ": video player killed");
            MB3Application.getInstance().API.GetItemAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    new BaseItemResponse(this)
            );
            FileLogger.getFileLogger().Info(TAG + ": finished video play request");
        } else {
            FileLogger.getFileLogger().Info(TAG + ": unable to process play request. Unsupported media type");
        }
    }

    @Override
    public void onSeekCommand(Long seekPositionTicks) {
        if (seekPositionTicks != null) {
            seek((int)(seekPositionTicks / 10000));
        }
    }

    @Override
    public void onRemoteBrowseRequest(BaseItemDto baseItemDto) {
        FileLogger.getFileLogger().Info(TAG + ": ignoring remote browse request due to media playback");
    }

    @Override
    public void onUserDataUpdated() {

    }

    @Override
    public void onGoHomeRequest() {

    }

    @Override
    public void onGoToSettingsRequest() {

    }

    protected void addItemsToPlaylist(String[] itemIds) {
        for (String id : itemIds) {
            PlaylistItem item = new PlaylistItem();
            item.Id = id;
            MB3Application.getInstance().PlayerQueue.PlaylistItems.add(item);
        }
    }

    private void clearReferences(){
        IWebsocketEventListener currActivity = MB3Application.getInstance().getCurrentActivity();
        if (currActivity != null && currActivity.equals(this))
            MB3Application.getInstance().setCurrentActivity(null);
    }

    @Override
    public void onDolbyAudioProcessingClientConnected() {

    }

    @Override
    public void onDolbyAudioProcessingClientDisconnected() {

    }

    @Override
    public void onDolbyAudioProcessingEnabled(boolean b) {

    }

    @Override
    public void onDolbyAudioProcessingProfileSelected(DolbyAudioProcessing.PROFILE profile) {

    }

    private void seek(int seekPositionMs) {
        mVideoView.removeCallbacks(onEverySecond);
        try {
            if (seekPositionMs == -1) return;

            if (mIsDirectStreaming || mIsStreamingHls) {

                mPlayerPositionOffsetMs = 0;
                mVideoView.seekTo(seekPositionMs);
                mVideoView.post(onEverySecond);

            } else {

                mVideoView.stopPlayback();
                mIsPrepared = false;

                mPlayerPositionOffsetMs = seekPositionMs;
                mStreamInfo.setStartPositionTicks((long)seekPositionMs * 10000);

                loadStreamInfoIntoPlayer();
            }
        } catch (IllegalArgumentException e) {
            FileLogger.getFileLogger().ErrorException("onStopTrackingTouch: IllegalArgumentException", e);
            e.printStackTrace();
        } catch (SecurityException e) {
            FileLogger.getFileLogger().ErrorException("onStopTrackingTouch: SecurityException", e);
            e.printStackTrace();
        } catch (IllegalStateException e) {
            FileLogger.getFileLogger().ErrorException("onStopTrackingTouch: IllegalStateException", e);
            e.printStackTrace();
        }
    }

    private void loadCurrentPlayingInfo(String itemName, String secondaryText, String tertiaryText, String primaryImageId, String overview) {
        if (mNowPlayingImage == null || mNowPlayingTitle == null) return;

        mNowPlayingTitle.setText(itemName);
        mOverview.setText(overview != null ? overview : "");

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(primaryImageId)) {
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setMaxWidth(300);
            String imageUrl = MB3Application.getInstance().API.GetImageUrl(primaryImageId, options);
            mNowPlayingImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            mNowPlayingImage.setVisibility(View.VISIBLE);
        } else {
            mNowPlayingImage.setVisibility(View.GONE);
        }
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(secondaryText)) {
            mNowPlayingSecondaryText.setText(secondaryText);
            mNowPlayingSecondaryText.setVisibility(View.VISIBLE);
        } else {
            mNowPlayingSecondaryText.setVisibility(View.GONE);
        }
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(tertiaryText)) {
            mNowPlayingTertiaryText.setText(tertiaryText);
            mNowPlayingTertiaryText.setVisibility(View.VISIBLE);
        } else {
            mNowPlayingTertiaryText.setVisibility(View.GONE);
        }
    }

    private void updateCurrentPosition(int positionMilliseconds, boolean staticCurrentPosition) {
        if (mCurrentPosition == null || mSeekBar == null) return;

        mSeekBar.setProgress(positionMilliseconds);
        if (!staticCurrentPosition) {
            mCurrentPosition.setText(Utils.PlaybackRuntimeFromMilliseconds(positionMilliseconds));
        }
    }

    private void setStartEndTimes(int startingPositionMilliseconds, int runtimeMilliseconds) {
        if (mCurrentPosition == null || mSeekBar == null || mRuntime == null) return;

        mSeekBar.setMax(runtimeMilliseconds);
        mSeekBar.setProgress(startingPositionMilliseconds);
        mSeekBar.setSecondaryProgress(0);
        mCurrentPosition.setText(Utils.PlaybackRuntimeFromMilliseconds(startingPositionMilliseconds));
        setCurrentPositionWidthFromRuntime(runtimeMilliseconds);
        mRuntime.setText(Utils.PlaybackRuntimeFromMilliseconds(runtimeMilliseconds));
    }

    private void setStartEndTimes(Date startDate, Date endDate) {
        if (mCurrentPosition == null || mSeekBar == null || mRuntime == null) return;

        mCurrentPosition.setText(TimeUtils.timestampTofriendlyHoursMinutes(startDate));
        mRuntime.setText(TimeUtils.timestampTofriendlyHoursMinutes(endDate));
        mSeekBar.setSecondaryProgress(0);
    }

    private void setCurrentPositionWidthFromRuntime(int runtime) {
        String runtimeString = "0:00:00";
        if (runtime > 0) {
            runtimeString = Utils.PlaybackRuntimeFromMilliseconds(runtime);
        }
        float measureText = mCurrentPosition.getPaint().measureText(runtimeString);
        mCurrentPosition.setWidth(mCurrentPosition.getPaddingLeft() + mCurrentPosition.getPaddingRight() + (int) measureText);
    }

    private void updateStreamCounts() {
        audioStreams = new ArrayList<>();
        subtitleStreams = new ArrayList<>();

        MediaStream dummySubStream = new MediaStream();
        dummySubStream.setLanguage("NONE");
        dummySubStream.setIndex(-33);
        dummySubStream.setType(MediaStreamType.Subtitle);
        subtitleStreams.add(dummySubStream);

        if (mStreamInfo == null
                || mStreamInfo.getMediaSource() == null
                || mStreamInfo.getMediaSource().getMediaStreams() == null) {
            return;
        }

        for (MediaStream stream : mStreamInfo.getMediaSource().getMediaStreams()) {
            if (stream.getType().equals(MediaStreamType.Audio)) {
                audioStreams.add(stream);
            } else if (stream.getType().equals(MediaStreamType.Subtitle)) {
                subtitleStreams.add(stream);
            }
        }

    }

    View.OnSystemUiVisibilityChangeListener systemUiVisibilityChangeListener = new View.OnSystemUiVisibilityChangeListener() {
        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                mPlayPauseButton.postDelayed(hideSystemUiRunnable, 3000);
            }
        }
    };

    private Runnable hideSystemUiRunnable = new Runnable() {
        @Override
        public void run() {
            hideSystemUi();
        }
    };

    private void hideSystemUi() {
        if (Build.VERSION.SDK_INT >= 16) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }
}
