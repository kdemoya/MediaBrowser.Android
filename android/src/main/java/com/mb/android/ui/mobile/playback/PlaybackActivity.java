package com.mb.android.ui.mobile.playback;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.Playlist;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.SubtitleDownloader;
import com.mb.android.activities.BaseMbMobileActivity;
import com.mb.android.listeners.PlaybackOptionsMenuClickListener;
import com.mb.android.logging.FileLogger;
import com.mb.android.profiles.MbAndroidProfile;
import com.mb.android.subtitles.Caption;
import com.mb.android.subtitles.FatalParsingException;
import com.mb.android.subtitles.FormatSRT;
import com.mb.android.subtitles.TimedTextFileFormat;
import com.mb.android.subtitles.TimedTextObject;
import com.mb.android.ui.tv.playback.PlayerHelpers;
import com.mb.android.utils.Utils;
import com.mb.network.Connectivity;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.profiles.AndroidProfile;
import mediabrowser.model.dlna.DeviceProfile;
import mediabrowser.model.dlna.DirectPlayProfile;
import mediabrowser.model.dlna.DlnaProfileType;
import mediabrowser.model.dlna.StreamBuilder;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dlna.SubtitleDeliveryMethod;
import mediabrowser.model.dlna.SubtitleProfile;
import mediabrowser.model.dlna.SubtitleStreamInfo;
import mediabrowser.model.dlna.VideoOptions;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.livetv.RecordingInfoDto;
import mediabrowser.model.session.PlayMethod;
import mediabrowser.model.session.PlayRequest;
import mediabrowser.model.session.PlaybackProgressInfo;
import mediabrowser.model.session.PlaybackStartInfo;
import mediabrowser.model.session.PlaybackStopInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Activity that handled video playback
 */
public class PlaybackActivity
        extends BaseMbMobileActivity
        implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        SurfaceHolder.Callback, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener {

    private static final String TAG = "PlaybackActivity";
    private static final int SUBTITLE_DISPLAY_INTERVAL = 100;
    private ImageView mPlayPauseButton;
    private ImageView mMuteUnMuteButton;
    private ImageView mOptionsMenu;
    private ListView mPlayList;
    private NetworkImageView mLoadingSplashScreen;
    private NetworkImageView mediaImage;
    private ProgressBar mBufferingIndicator;
    private RelativeLayout mMediaControlsOverlay;
    private SeekBar mPlaybackProgress;
    private TextView mRuntimeText;
    private TextView mCurrentPositionText;
    private TextView mMediaTitle;
    private TextView mMediaSubTitle;
    private TextView mNowPlayingText;
    private TextView mSecondaryText;
    private TextView mStreamDetails;
    private TextView subtitlesText;
    private boolean isFresh = true;
    private boolean mIsPaused;
    private boolean mResume;
    private boolean mMuted = false;
    private boolean mIsDirectStreaming;
    private boolean mIsStreamingHls;
    private boolean isPrepared = false;
    private boolean isSeeking;
    private float mCurrentVolume = 1.0f;
    private int currentPlayingIndex = 0;
    private int mWidth;
    private int mHeight;
    private int mPreviousPosition = 0;
    private int mTruePlayerPosition = 0;
    private int mPlayerPositionOffset = 0;
    private int mRuntime;
    private int currentlyPlayingIndex = 0;
    private long mLastActionTime = 0L;
    private long mLastProgressReport = 0L;
    private BaseItemDto mMediaItem;
    private DisplayMetrics mMetrics;
    private Handler subtitleDisplayHandler;
    private MediaPlayer mPlayer;
    private RecordingInfoDto mRecording;
    private StreamInfo mStreamInfo;
    private SurfaceView mSurface;
    private SurfaceHolder mHolder;
    private TimedTextFileFormat ttff;
    private TimedTextObject tto;

    public StreamInfo getStreamInfo() {
        return mStreamInfo;
    }

    @SuppressLint("InflateParams")
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_media_playback);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Just in case the TV Theme is still playing
        MB3Application.getInstance().StopMedia();

        if (MB3Application.getInstance().PlayerQueue == null
                || MB3Application.getInstance().PlayerQueue.PlaylistItems == null
                || MB3Application.getInstance().PlayerQueue.PlaylistItems.size() == 0) {

            Toast.makeText(this, "Nothing to play", Toast.LENGTH_LONG).show();
            finish();
        }

        mMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

//        if (!MB3Application.getInstance().isDolbyAvailable()) {
//
//            if (MB3Application.getInstance().createDolbyAudioProcessing()) {
//                Toast.makeText(mPlaybackActivity, "Dolby capable", Toast.LENGTH_LONG).show();
//            }
//        }

        // acquire UI elements
        mMediaControlsOverlay = (RelativeLayout) findViewById(R.id.rlControlOverlay);
        mPlaybackProgress = (SeekBar) findViewById(R.id.sbPlaybackProgress);
        mBufferingIndicator = (ProgressBar) findViewById(R.id.pbVideoLoading);
        mPlayPauseButton = (ImageView) findViewById(R.id.ivPlayPause);
        ImageView mPreviousButton = (ImageView) findViewById(R.id.ivPrevious);
        ImageView mNextButton = (ImageView) findViewById(R.id.ivNext);
        ImageView mRewindButton = (ImageView) findViewById(R.id.ivRewind);
        ImageView mFastForwardButton = (ImageView) findViewById(R.id.ivFastForward);
        mMuteUnMuteButton = (ImageView) findViewById(R.id.ivAudioMute);
        mSurface = (SurfaceView) findViewById(R.id.svPlaybackSurface);
        mLoadingSplashScreen = (NetworkImageView) findViewById(R.id.ivMusicScreenSaver);
        mMediaTitle = (TextView) findViewById(R.id.tvNowLoadingTitle);
        mMediaSubTitle = (TextView) findViewById(R.id.tvNowLoadingSubTitle);
        mRuntimeText = (TextView) findViewById(R.id.tvRuntime);
        mCurrentPositionText = (TextView) findViewById(R.id.tvCurrentPosition);
        mNowPlayingText = (TextView) findViewById(R.id.tvPlaybackPrimaryText);
        mSecondaryText = (TextView) findViewById(R.id.tvPlaybackSecondaryText);
        mediaImage = (NetworkImageView) findViewById(R.id.ivPlaybackMediaImage);
        mediaImage.setDefaultImageResId(R.drawable.default_video_portrait);
        mStreamDetails = (TextView) findViewById(R.id.tvStreamInfo);
        mOptionsMenu = (ImageView) findViewById(R.id.ivOptionsMenu);
        mPlayList = (ListView) findViewById(R.id.playlist_drawer);
        mPlayList.setEmptyView(getLayoutInflater().inflate(R.layout.widget_playlist_empty_view, null));

        if (MB3Application.getInstance().PlayerQueue.PlaylistItems.size() > 1) {
            mPreviousButton.setVisibility(ImageView.VISIBLE);
            mNextButton.setVisibility(ImageView.VISIBLE);
        }

        // set transport control event handlers
        mPlayPauseButton.setOnClickListener(onPlayPauseClick);
        mMuteUnMuteButton.setOnClickListener(onMuteUnmuteClick);
        mPreviousButton.setOnClickListener(onPreviousClick);
        mNextButton.setOnClickListener(onNextClick);
        mRewindButton.setOnClickListener(onRewindClick);
        mFastForwardButton.setOnClickListener(onFastForwardClick);

        mRuntimeText.setText("0:00:00");
        mCurrentPositionText.setText("0:00:00");

        mPlaybackProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progressValue = -1;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (fromUser) {
                    progressValue = progress;
                    mCurrentPositionText.setText(Utils.PlaybackRuntimeFromMilliseconds(progress));
                }
            }

            public void onStartTrackingTouch(SeekBar arg0) {
                isSeeking = true;
            }

            public void onStopTrackingTouch(SeekBar seekBar) { handleSeek(progressValue);  }
        });

        // Setup surface
        mSurface.setOnClickListener(onSurfaceClick);
        mHolder = mSurface.getHolder();
        if (mHolder != null)
            mHolder.addCallback(this);

        if ("Recording".equalsIgnoreCase(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Type)) {
            MB3Application.getInstance().API.GetLiveTvRecordingAsync(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Id, MB3Application.getInstance().API.getCurrentUserId(), new GetRecordingResponse());
        } else if ("tvChannel".equalsIgnoreCase(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Type)) {
            MB3Application.getInstance().API.GetItemAsync(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Id, MB3Application.getInstance().API.getCurrentUserId(), getItemResponse);
            findViewById(R.id.llTransportControls).setVisibility(View.GONE);
            mRuntimeText.setVisibility(View.GONE);
            mCurrentPositionText.setVisibility(View.GONE);
        } else {
            MB3Application.getInstance().API.GetItemAsync(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Id, MB3Application.getInstance().API.getCurrentUserId(), getItemResponse);
        }

        hidePanels(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("PlaybackActivity", "onResume");
//        if (MB3Application.getInstance().getIsConnected()) {
            buildUi();
//        }
        hideSystemUi();
        mSurface.postDelayed(onEverySecond, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        FileLogger.getFileLogger().Info("Playback Activity: onPause");
        PlayerPause();
        mIsPaused = true;
        Log.i(TAG, "onPause");
        mSurface.removeCallbacks(onEverySecond);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("PlaybackActivity", "onStop");
        FileLogger.getFileLogger().Info("Playback Activity: onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FileLogger.getFileLogger().Info("Playback Activity: onDestroy");
        MB3Application.getInstance().PlayerQueue = new Playlist();
        try {
            sendPlaybackStoppedToServer((long) mTruePlayerPosition * 10000);
        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("Error sending playback stopped ", e);
        }

        if (mIsStreamingHls)
            MB3Application.getInstance().API.StopTranscodingProcesses(
                    MB3Application.getInstance().API.getDeviceId(),
                    new EmptyResponse()
            );
        if (subtitleDisplayHandler != null) {
            subtitleDisplayHandler.removeCallbacks(processSubtitles);
            subtitleDisplayHandler = null;
        }

        if (mPlayer != null) {
            try {
                if (isPrepared) {
                    FileLogger.getFileLogger().Info(TAG + ": calling mPlayer.release");
                    mPlayer.release();
                    FileLogger.getFileLogger().Info(TAG + ": mPlayer.release called");
                }
            } catch (Exception e) {
                FileLogger.getFileLogger().Info(TAG + ": mPlayer.stop/release error");
                e.printStackTrace();
            } finally {
                mPlayer = null;
            }
        }

        MB3Application.getInstance().releaseDolbyAudioProcessing();
    }

    @Override
    protected void onConnectionRestored() {

    }

//    @Override
//    protected void onConnectionRestored() {
//        buildUi();
//    }

    private void buildUi() {
        if (isFresh) {

            mPlayList.setAdapter(new PlaylistDrawerAdapter(MB3Application.getInstance().PlayerQueue.PlaylistItems, this));
            mPlayList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    if (i != currentPlayingIndex) {
                        PlayerStop();

                        if (mIsStreamingHls)
                            MB3Application.getInstance().API.StopTranscodingProcesses(
                                    MB3Application.getInstance().API.getDeviceId(),
                                    new EmptyResponse()
                            );

                        sendPlaybackStoppedToServer((long) mTruePlayerPosition * 10000);

                        PlayerReset();
                        // Make sure the activity knows to update the playlist
                        UpdateCurrentPlayingIndex(i);

                        MB3Application.getInstance().API.GetItemAsync(
                                MB3Application.getInstance().PlayerQueue.PlaylistItems.get(i).Id,
                                MB3Application.getInstance().API.getCurrentUserId(),
                                getItemResponse);
                    }
                }
            });
            isFresh = false;
        }
    }

    public void UpdateCurrentPlayingIndex(int newIndex) {

        if (newIndex < 0 || newIndex >= MB3Application.getInstance().PlayerQueue.PlaylistItems.size())
            return;

        currentPlayingIndex = newIndex;
        ((PlaylistDrawerAdapter) mPlayList.getAdapter()).notifyDataSetChanged();
    }

    private class PlaylistDrawerAdapter extends BaseAdapter {

        List<PlaylistItem> mPlaylistItems;
        Context mContext;
        LayoutInflater mLayoutInflater;

        public PlaylistDrawerAdapter(List<PlaylistItem> playList, Context context) {

            mPlaylistItems = playList;
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mPlaylistItems.size();
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
                view = mLayoutInflater.inflate(R.layout.widget_playlist_drawer_clickable_item, viewGroup, false);
            }

            if (view != null) {

                TextView tv = (TextView) view.findViewById(R.id.tvClickableItem);
                TextView st = (TextView) view.findViewById(R.id.tvSecondaryText);
                st.setTextColor(Color.GRAY);

                tv.setText(mPlaylistItems.get(i).Name);
                st.setText(mPlaylistItems.get(i).SecondaryText);

                if (currentPlayingIndex == i) {
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_MEDIA_STOP == event.getKeyCode()) {
            finish();
            return true;
        } else if (KeyEvent.KEYCODE_MEDIA_PLAY == event.getKeyCode() ||
                KeyEvent.KEYCODE_MEDIA_PAUSE == event.getKeyCode() ||
                KeyEvent.KEYCODE_MEDIA_PREVIOUS == event.getKeyCode() ||
                KeyEvent.KEYCODE_MEDIA_REWIND == event.getKeyCode() ||
                KeyEvent.KEYCODE_MEDIA_NEXT == event.getKeyCode() ||
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD == event.getKeyCode() ||
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE == event.getKeyCode() ||
                KeyEvent.KEYCODE_VOLUME_MUTE == event.getKeyCode()) {
            ProcessMediaButtonPress(event.getKeyCode());
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * A button press has been received and passed into this fragment. We process it here
     *
     * @param keyCode An int representing the KeyCode that is to be processed
     */
    public void ProcessMediaButtonPress(int keyCode) {

        if (KeyEvent.KEYCODE_MEDIA_PLAY == keyCode) {
            if (mIsPaused)
                onPlayPauseClick.onClick(null);
            else
                onSurfaceClick.onClick(null);
        } else if (KeyEvent.KEYCODE_MEDIA_PAUSE == keyCode) {
            if (!mIsPaused)
                onPlayPauseClick.onClick(null);
            else
                onSurfaceClick.onClick(null);
        } else if (KeyEvent.KEYCODE_MEDIA_PREVIOUS == keyCode) {
            onPreviousClick.onClick(null);
        } else if (KeyEvent.KEYCODE_MEDIA_REWIND == keyCode) {
            onRewindClick.onClick(null);
        } else if (KeyEvent.KEYCODE_MEDIA_NEXT == keyCode) {
            onNextClick.onClick(null);
        } else if (KeyEvent.KEYCODE_MEDIA_FAST_FORWARD == keyCode) {
            onFastForwardClick.onClick(null);
        } else if (KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE == keyCode) {
            onPlayPauseClick.onClick(null);
        } else if (KeyEvent.KEYCODE_VOLUME_MUTE == keyCode) {
            onMuteUnmuteClick.onClick(null);
        }
    }


    @Override
    public void onRemotePlayRequest(PlayRequest request, String mediaType) {
        FileLogger.getFileLogger().Info(TAG + ": remote play request received");
        if ("audio".equalsIgnoreCase(mediaType)) {
            FileLogger.getFileLogger().Info(TAG + ": first item is audio.");
            MB3Application.getInstance().PlayerQueue = new Playlist();
            addItemsToPlaylist(request.getItemIds());
            FileLogger.getFileLogger().Info(TAG + ": video player killed");
            Intent intent = new Intent(this, AudioPlaybackActivity.class);
            startActivity(intent);
            this.finish();
            FileLogger.getFileLogger().Info(TAG + ": finished audio play request");
        } else if ("video".equalsIgnoreCase(mediaType)) {
            PlayerReset();
            MB3Application.getInstance().PlayerQueue = new Playlist();
            addItemsToPlaylist(request.getItemIds());
            FileLogger.getFileLogger().Info(TAG + ": video player killed");
            MB3Application.getInstance().API.GetItemAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    getItemResponse
            );
            FileLogger.getFileLogger().Info(TAG + ": finished video play request");
        } else {
            FileLogger.getFileLogger().Info(TAG + ": unable to process play request. Unsupported media type");
        }
    }

    @Override
    public void onRemoteBrowseRequest(BaseItemDto baseItemDto) {
        FileLogger.getFileLogger().Info(TAG + ": ignoring remote browse request due to media playback");
    }


    @Override
    public void onGoToSettingsRequest() {

    }

    @Override
    public void onSeekCommand(Long seekPositionTicks) {
        if (seekPositionTicks == null) return;
        handleSeek((int)(seekPositionTicks / 10000));
    }

    /**
     * Runnable that fires once a second to update the various UI controls
     */
    private Runnable onEverySecond = new Runnable() {

        public void run() {

            if (mLastActionTime > 0 && SystemClock.elapsedRealtime() - mLastActionTime > 5000 && !isSeeking) {
                hidePanels(true);
            }

            if (mPlayer == null) return;

            // Report current position to the server only every 5 seconds.
            if (mLastProgressReport > 0 && SystemClock.elapsedRealtime() - mLastProgressReport > 5000) {
                sendPlaybackProgressToServer((long) mPlaybackProgress.getProgress() * 10000);
                mLastProgressReport = SystemClock.elapsedRealtime();
            }

            // No point going on. If playback is paused then the values haven't changed.
            if (mIsPaused) return;

            /*
             Need to employ offset calculations to account for resuming because the player sees
             the stream as shorter than the actual runtime.
             */

            mTruePlayerPosition = mPlayer.getCurrentPosition() + mPlayerPositionOffset;

            mPreviousPosition = mTruePlayerPosition;

            if (isPrepared && !isSeeking) {
                mPlaybackProgress.setProgress(mTruePlayerPosition);
                mCurrentPositionText.setText(Utils.PlaybackRuntimeFromMilliseconds(mTruePlayerPosition));
            }

            mSurface.postDelayed(onEverySecond, 1000);
        }
    };

    private Runnable processSubtitles = new Runnable() {
        @Override
        public void run() {

            if (mPlayer != null && mPlayer.isPlaying()) {

                if (tto == null) return;
                // We have subs to process
                Collection<Caption> subtitles = tto.captions.values();
                for (Caption caption : subtitles) {
                    if (mTruePlayerPosition >= caption.start.getMilliseconds() && mTruePlayerPosition <= caption.end.getMilliseconds()) {
                        onTimedText(caption);
                        break;
                    } else if (mTruePlayerPosition > caption.end.getMilliseconds()) {
                        onTimedText(null);
                    }
                }
            }

            subtitleDisplayHandler.postDelayed(this, SUBTITLE_DISPLAY_INTERVAL);
        }
    };

    /**
     * Callback method invoked when the SurfaceView is changed
     *
     * @param surfaceHolder The SurfaceHolder whose surface has changed.
     * @param format        The new PixelFormat of the surface.
     * @param width         The new width of the surface.
     * @param height        The new height of the surface.
     */
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");
        FileLogger.getFileLogger().Info(TAG + ": surfaceChanged");

    }

    /**
     * Callback method invoked when the SurfaceHolder is created
     *
     * @param holder The SurfaceHolder whose surface is being created.

     */
    public void surfaceCreated(SurfaceHolder holder) {

        Log.i(TAG, "begin surfaceCreated");
        FileLogger.getFileLogger().Info(TAG + ": surfaceCreated");
        Log.i(TAG, "end surfaceCreated");
    }

    /**
     * Callback method invoked when the SurfaceHolder is destroyed
     *
     * @param holder The SurfaceHolder whose surface is being destroyed.
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        FileLogger.getFileLogger().Info(TAG + ": surfaceDestroyed");
        Log.i(TAG, "surfaceDestroyed");

    }

    /**
     * Callback method that is triggered when the MediaPlayer has fully initialized and is ready to play
     *
     * @param mediaPlayer The MediaPlayer instance
     */
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

        Log.i(TAG, "begin onPrepared");
        FileLogger.getFileLogger().Info(TAG + ": onPrepared");

        if (mWidth != 0 && mHeight != 0) {
            SetSurfaceDimensions();
        }

        if (mMediaItem != null && (mMediaItem.getType().equalsIgnoreCase("VodCastVideo") ||
                mMediaItem.getType().equalsIgnoreCase("PodCastAudio"))) {

            if (mediaPlayer.getDuration() != -1)
                mPlaybackProgress.setMax(mediaPlayer.getDuration());

        } else if (mRuntime != 0) {
            mPlaybackProgress.setMax(mRuntime);

            if (mResume && !mIsDirectStreaming) {
                if (mMediaItem != null) {
                    mPlaybackProgress.setProgress((int) (mMediaItem.getUserData().getPlaybackPositionTicks() / 10000));
                } else if (mRecording != null) {
                    mPlaybackProgress.setProgress((int) (mRecording.getUserData().getPlaybackPositionTicks() / 10000));
                }
            } else {
                mPlaybackProgress.setProgress(0);
            }
        } else {
            mPlaybackProgress.setVisibility(ProgressBar.INVISIBLE);
        }

        if (!mResume && mStreamInfo.getSubtitleDeliveryMethod().equals(SubtitleDeliveryMethod.External)) {
            mStreamInfo.setSubtitleFormat("srt");
            final List<SubtitleStreamInfo> subtitles = mStreamInfo.GetExternalSubtitles(MB3Application.getInstance().API.getApiUrl(), false);

            if (subtitles != null && subtitles.size() > 0) {
                new SubtitleDownloader(new Response<File>() {
                    @Override
                    public void onResponse(File subFile) {
                        FileLogger.getFileLogger().Info("Subtitle Downloader: onResponse");
                        if (subFile != null) {
                            ttff = new FormatSRT();

                            try {
                                InputStream is = new FileInputStream(subFile);
                                tto = ttff.parseFile(subFile.getName(), is);
                                if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(tto.warnings)) {
                                    FileLogger.getFileLogger().Info(tto.warnings);
                                }
                                if (tto.captions == null || tto.captions.size() == 0) {
                                    FileLogger.getFileLogger().Info("Subtitle Downloader: Subtitle file parsed. Nothing to display");
                                } else {
                                    FileLogger.getFileLogger().Info("tto caption count" + String.valueOf(tto.captions.size()));
                                    FileLogger.getFileLogger().Info("tto caption 1" + tto.captions.firstEntry().getValue().content);
                                    FileLogger.getFileLogger().Info("Subtitle Downloader: Create display handler");
                                    subtitleDisplayHandler = new Handler();
                                    subtitleDisplayHandler.post(processSubtitles);
                                    FileLogger.getFileLogger().Info("Subtitle Downloader: Finished!");
                                }
                            } catch (FatalParsingException | IOException e) {
                                e.printStackTrace();
                            } finally {
                                if (!subFile.delete()) {
                                    FileLogger.getFileLogger().Info("Subtitle Downloader: Error deleting subtitle file");
                                }
                            }
                        } else {
                            FileLogger.getFileLogger().Info("Subtitle Downloader: Unable to retrieve physical file");
                        }
                    }
                    @Override
                    public void onError(Exception ex) {
                        FileLogger.getFileLogger().Error("Error downloading subtitle file");
                    }
                }).execute(subtitles.get(0).getUrl());

                subtitlesText = (TextView) findViewById(R.id.txtSubtitles);

                try {
                    String subSize = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_subtitle_size", "18");
                    subtitlesText.setTextSize(Float.valueOf(subSize));
                } catch (Exception e) {
                    subtitlesText.setTextSize(18);
                }

            } else {
                FileLogger.getFileLogger().Info("onPrepared: StreamInfo returned no subtitles");
            }
        }

        mPlayer.start();
        isPrepared = true;

        if (mResume) {
            if ((mIsDirectStreaming || mIsStreamingHls)) {
                if (mMediaItem != null) {
                    mPlayer.seekTo((int) (mMediaItem.getUserData().getPlaybackPositionTicks() / 10000));
                } else if (mRecording != null) {
                    mPlayer.seekTo((int) (mRecording.getUserData().getPlaybackPositionTicks() / 10000));
                }
            } else {
                // set the progress bar to the correct value
                mPlayerPositionOffset = mMediaItem.getUserData() != null
                        ? (int) (mMediaItem.getUserData().getPlaybackPositionTicks() / 10000)
                        : 0;
            }
        }
        // Let the server know we started
        if (mMediaItem != null) {
            sendPlaybackStartedToServer((long) mTruePlayerPosition * 10000);
        }
        mLastProgressReport = mLastActionTime = SystemClock.elapsedRealtime();

        mPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                mPlaybackProgress.setSecondaryProgress((int) (mRuntime * ((float) percent / 100)));

            }
        });

//        if (mMediaItem != null)
        HideInitialLoadingImage();

        mSurface.post(onEverySecond);
        Log.i(TAG, "end onPrepared");
    }


    /**
     * Callback that is triggered when the MediaPlayer completes playback in a normal manor
     *
     * @param mp The mediaPlayer instance
     */
    public void onCompletion(MediaPlayer mp) {
        FileLogger.getFileLogger().Info(TAG + ": onCompletion");
        Log.i(TAG, "onCompletion");

        if (mIsStreamingHls)
            MB3Application.getInstance().API.StopTranscodingProcesses(MB3Application.getInstance().API.getDeviceId(), new EmptyResponse());

        // kill subtitles
        if (subtitleDisplayHandler != null) {
            subtitleDisplayHandler.removeCallbacks(processSubtitles);
            tto = null;
            ttff = null;
        }

        if (MB3Application.getInstance().PlayerQueue.PlaylistItems.size() > currentlyPlayingIndex + 1) {
            currentlyPlayingIndex += 1;
            PlayerStop();

            sendPlaybackStoppedToServer((long) mTruePlayerPosition * 10000);

            isPrepared = false;
            PlayerReset();
            // Make sure the activity knows to update the playlist
            UpdateCurrentPlayingIndex(currentlyPlayingIndex);

            MB3Application.getInstance().API.GetItemAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    getItemResponse);
        } else {
            mIsPaused = true;
            isPrepared = false;

            finish();
        }
    }

    //**********************************************************************************************
    // On-Screen controls
    //**********************************************************************************************

    /**
     * Play/Pause button
     */
    private View.OnClickListener onPlayPauseClick = new View.OnClickListener() {

        public void onClick(View v) {

            if (mPlayer != null) {
                try {
                    if (mPlayer.isPlaying()) {
                        mPlayer.pause();
                        mPlayPauseButton.setImageResource(R.drawable.vp_play_selector);
                    } else {
                        mPlayer.start();
                        mPlayPauseButton.setImageResource(R.drawable.vp_pause_selector);
                    }
                } catch (IllegalStateException e) {
                    FileLogger.getFileLogger().ErrorException("Exception handled trying to play/pause player" , e);
                }
            }

        }
    };

    /**
     * Mute/UnMute button
     */
    private View.OnClickListener onMuteUnmuteClick = new View.OnClickListener() {

        public void onClick(View v) {

            if (mPlayer != null) {
                if (mMuted) {
                    mMuteUnMuteButton.setImageResource(R.drawable.vp_mute_selector);
                    mPlayer.setVolume(mCurrentVolume, mCurrentVolume);
                    mMuted = false;
                } else {
                    mMuteUnMuteButton.setImageResource(R.drawable.vp_unmute_selector);
                    mPlayer.setVolume(0, 0);
                    mMuted = true;

                }
            }

        }
    };

    /**
     * Previous button
     */
    private View.OnClickListener onPreviousClick = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            if (MB3Application.getInstance().PlayerQueue.PlaylistItems.size() == 1)
                return;

            if (currentlyPlayingIndex - 1 < 0) {
                currentlyPlayingIndex = MB3Application.getInstance().PlayerQueue.PlaylistItems.size() - 1;
            } else {
                currentlyPlayingIndex -= 1;
            }

            PlayerStop();

            if (mIsStreamingHls)
                MB3Application.getInstance().API.StopTranscodingProcesses(MB3Application.getInstance().API.getDeviceId(), new EmptyResponse());

            sendPlaybackStoppedToServer((long) mTruePlayerPosition * 10000 );

            isPrepared = false;
            PlayerReset();
            // Make sure the activity knows to update the playlist
            UpdateCurrentPlayingIndex(currentlyPlayingIndex);

            MB3Application.getInstance().API.GetItemAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    getItemResponse);
        }
    };

    /**
     * Rewind button
     */
    private View.OnClickListener onRewindClick = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            // subtracting 30 seconds still has the current item in progress
            if ((mTruePlayerPosition * 10000) - 300000000 > 0) {

                mTruePlayerPosition = mTruePlayerPosition - 30000;

                if (mIsDirectStreaming || mIsStreamingHls) {
                    mPlayer.seekTo(mTruePlayerPosition);
                    mPlayerPositionOffset = 0;
                } else {

                    // In case the user is spazzing on the ff/rw button
                    if (!isPrepared) return;

                    PlayerStop();

                    isPrepared = false;
                    mPlayer.reset();

                    mStreamInfo.setStartPositionTicks(mTruePlayerPosition);
                    mPlayerPositionOffset = mTruePlayerPosition;

                    try {
                        mPlayer.setDataSource(mStreamInfo.ToUrl(MB3Application.getInstance().API.getApiUrl()));
                        mPlayer.prepareAsync();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // restart the current item
            } else {

                mTruePlayerPosition = 0;

                if (mIsDirectStreaming || mIsStreamingHls) {
                    mPlayer.seekTo(mTruePlayerPosition);
                } else {
                    mPlayer.stop();
                    isPrepared = false;
                    mPlayer.reset();

                    mStreamInfo.setStartPositionTicks(mTruePlayerPosition);

                    try {
                        mPlayer.setDataSource(mStreamInfo.ToUrl(MB3Application.getInstance().API.getApiUrl()));
                        mPlayer.prepareAsync();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    /**
     * Fast Forward button
     */
    private View.OnClickListener onFastForwardClick = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            if (mMediaItem != null && ("VodCastVideo".equalsIgnoreCase(mMediaItem.getType()) ||
                    "PodCastAudio".equalsIgnoreCase(mMediaItem.getType()))) {

                mPlayer.seekTo(mPlayer.getCurrentPosition() + 30000);

            } else {

                long mediaRuntime =
                        mStreamInfo != null && mStreamInfo.getMediaSource() != null && mStreamInfo.getMediaSource().getRunTimeTicks() != null
                                ? mStreamInfo.getMediaSource().getRunTimeTicks()
                                : mMediaItem != null && mMediaItem.getRunTimeTicks() != null
                                ? mMediaItem.getRunTimeTicks()
                                : mRecording != null && mRecording.getRunTimeTicks() != null
                                ? mRecording.getRunTimeTicks()
                                : 0;

                // Current position + 30 seconds is still less than the total runtime of the media
                if ((mTruePlayerPosition * 10000) + 300000000 < mediaRuntime) {
                    int seekTarget = mTruePlayerPosition + 30000;

                    if (mIsDirectStreaming || mIsStreamingHls) {
                        mPlayer.seekTo(seekTarget);
                        mPlayerPositionOffset = 0;
                    } else {

                        // In case the user is spazzing on the ff/rw button
                        if (!isPrepared) return;

                        PlayerStop();
                        isPrepared = false;
                        PlayerReset();

                        mStreamInfo.setStartPositionTicks((long)seekTarget * 10000);
                        mPlayerPositionOffset = seekTarget;

                        try {
                            String url = mStreamInfo.ToUrl(MB3Application.getInstance().API.getApiUrl());
                            FileLogger.getFileLogger().Info("Fast Forward");
                            FileLogger.getFileLogger().Info("new url: " + url);
                            mPlayer.setDataSource(url);
                            mPlayer.prepareAsync();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    // there isn't 30 seconds remaining in the current item. Move to the next item if one
                    // exists
                } else {
                    if (mIsStreamingHls) {
                        MB3Application.getInstance().API.StopTranscodingProcesses(MB3Application.getInstance().API.getDeviceId(), new EmptyResponse());
                    }
                    if (MB3Application.getInstance().PlayerQueue.PlaylistItems.size() > 1)
                        onNextClick.onClick(view);
                }
            }
        }
    };

    /**
     * Next button
     */
    private View.OnClickListener onNextClick = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            if (MB3Application.getInstance().PlayerQueue.PlaylistItems.size() == 1)
                return;

            if (MB3Application.getInstance().PlayerQueue.PlaylistItems.size() > currentlyPlayingIndex + 1) {
                currentlyPlayingIndex += 1;
            } else {
                currentlyPlayingIndex = 0;
            }

            mPlayer.stop();

            if (mIsStreamingHls)
                MB3Application.getInstance().API.StopTranscodingProcesses(
                        MB3Application.getInstance().API.getDeviceId(),
                        new EmptyResponse()
                );

            sendPlaybackStoppedToServer((long) mTruePlayerPosition * 10000);

            isPrepared = false;
            mPlayer.reset();
            // Make sure the activity knows to update the playlist
            UpdateCurrentPlayingIndex(currentlyPlayingIndex);

            MB3Application.getInstance().API.GetItemAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    getItemResponse);
        }
    };

    private View.OnClickListener onSurfaceClick = new View.OnClickListener() {

        public void onClick(View v) {

            mLastActionTime = SystemClock.elapsedRealtime();
            hidePanels(mMediaControlsOverlay.getVisibility() == View.VISIBLE);

        }
    };

    /**
     * Method that will show or hide the panels that contain the transport controls, media info, and now playing info
     *
     * @param hide True if the panels should be hidden, False if they are to be shown
     */
    private void hidePanels(boolean hide) {

        try {
            if (hide) {
                mMediaControlsOverlay.setVisibility(View.GONE);
                hideSystemUi();
            } else {
                mMediaControlsOverlay.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("hidePanels - ", e);
        }
    }

    private void hideSystemUi() {
        if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);
        } else if (Build.VERSION.SDK_INT >= 16) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    private void SetNowPlayingInfo(RecordingInfoDto recording) {
        setCurrentItemImage(recording.getHasPrimaryImage() ? recording.getId() : null);
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(recording.getEpisodeTitle())) {
            setPrimaryText(recording.getEpisodeTitle());
            setSecondaryText(recording.getName());
        } else {
            setPrimaryText(recording.getName());
            setSecondaryText("");
        }
    }

    /**
     * Set the image and text in the bottom-left corner of the playback window
     *
     * @param _mediaItem The item being played
     */
    private void SetNowPlayingInfo(BaseItemDto _mediaItem) {

        FileLogger.getFileLogger().Info(TAG + ": SetNowPlayingInfo");
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(_mediaItem.getSeriesPrimaryImageTag()) && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(_mediaItem.getSeriesId())) {
            setCurrentItemImage(_mediaItem.getSeriesId());
        } else if (_mediaItem.getHasPrimaryImage()) {
            setCurrentItemImage(_mediaItem.getId());
        } else if (_mediaItem.getParentId() != null) {
            // Get the parents primary image. PITA.
            MB3Application.getInstance().API.GetItemAsync(
                    _mediaItem.getParentId(),
                    MB3Application.getInstance().API.getCurrentUserId(),
                    getItemResponse);
        } else {
            mediaImage.setVisibility(View.GONE);
        }
        setPrimaryText(_mediaItem.getName());

        if (_mediaItem.getType() != null) {
            if (_mediaItem.getType().equalsIgnoreCase("Episode")) {
                setPrimaryText(String.valueOf(_mediaItem.getParentIndexNumber()) + "." + String.valueOf(_mediaItem.getIndexNumber()) + " - " + _mediaItem.getName());
                setSecondaryText(_mediaItem.getSeriesName());
            } else if (_mediaItem.getType().equalsIgnoreCase("Movie")) {
                mSecondaryText.setText((_mediaItem.getProductionYear() != null ? String.valueOf(_mediaItem.getProductionYear()) : ""));
            }
        }
    }

    private void setCurrentItemImage(String imageItemId) {
        if (imageItemId == null) {
            mediaImage.setVisibility(View.GONE);
        } else {
            mediaImage.setVisibility(ImageView.VISIBLE);

            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setWidth((int) (150 * mMetrics.density));
            options.setEnableImageEnhancers(PreferenceManager
                    .getDefaultSharedPreferences(MB3Application.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true));

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(imageItemId, options);
            mediaImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        }
    }

    private void setPrimaryText(String text) {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(text)) {
            mNowPlayingText.setVisibility(View.GONE);
        } else {
            mNowPlayingText.setVisibility(View.VISIBLE);
            mNowPlayingText.setText(text);
        }
    }

    private void setSecondaryText(String text) {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(text)) {
            mSecondaryText.setVisibility(View.GONE);
        } else {
            mSecondaryText.setVisibility(View.VISIBLE);
            mSecondaryText.setText(text);
        }
    }

    /**
     * Generate the Video URL to be requested from MB Server
     *
     * @param id                  The ID of the item to be played
     * @param mediaSources        The available MediaSourceInfo's for the item being played
     * @param startPositionTicks  The position in ticks that playback should commence from.
     * @param audioStreamIndex    Integer representing the media stream index to use for audio.
     * @param subtitleStreamIndex Integer representing the media stream index to use for subtitles.
     * @return A String containing the formed URL.
     */
    private boolean buildStreamInfo(String id,
                                    ArrayList<MediaSourceInfo> mediaSources,
                                    Long startPositionTicks,
                                    String mediaSourceId,
                                    Integer audioStreamIndex,
                                    Integer subtitleStreamIndex) {

        mIsDirectStreaming = false;
        mIsStreamingHls = false;
        mStreamDetails.setVisibility(TextView.VISIBLE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String bitrate;

        if (Connectivity.isConnectedLAN(this)) {
            bitrate = prefs.getString("pref_local_bitrate", "1800000");
        } else {
            bitrate = prefs.getString("pref_cellular_bitrate", "450000");
        }

        boolean hlsEnabled = prefs.getBoolean("pref_enable_hls", true);
//        boolean h264StrictModeEnabled = prefs.getBoolean("pref_h264_strict", true);

        DeviceProfile androidProfile = new MbAndroidProfile(hlsEnabled, false);

//        DirectPlayProfile directMp4 = new DirectPlayProfile();
//        directMp4.setContainer("mp4");
//        directMp4.setType(DlnaProfileType.Video);
//
//        DirectPlayProfile directMkv = new DirectPlayProfile();
//        directMkv.setContainer("mkv");
//        directMkv.setType(DlnaProfileType.Video);
//
//        ArrayList<DirectPlayProfile> directPlayProfiles = new ArrayList<>();
//
//        Collections.addAll(directPlayProfiles, androidProfile.getDirectPlayProfiles());
//
//        directPlayProfiles.add(directMkv);
//        directPlayProfiles.add(directMp4);
//
//        DirectPlayProfile[] directArray = new DirectPlayProfile[directPlayProfiles.size()];
//        directPlayProfiles.toArray(directArray);
//
//        androidProfile.setDirectPlayProfiles(directArray);
//
//        SubtitleProfile srtSubs = new SubtitleProfile();
//        srtSubs.setFormat("srt");
//        srtSubs.setMethod(SubtitleDeliveryMethod.External);
//
//        androidProfile.setSubtitleProfiles(new SubtitleProfile[] { srtSubs });

        String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(androidProfile);
        FileLogger.getFileLogger().Info(jsonData);

        FileLogger.getFileLogger().Info("Create VideoOptions");
        VideoOptions options = new VideoOptions();
        options.setItemId(id);
        options.setMediaSources(mediaSources);
        options.setProfile(androidProfile);
        options.setDeviceId(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        options.setMaxBitrate(Integer.valueOf(bitrate));
//        options.setMaxAudioChannels(5);

        if (audioStreamIndex != null) {
            options.setAudioStreamIndex(audioStreamIndex);
            options.setMediaSourceId(mediaSourceId);
        }
        if (subtitleStreamIndex != null) {
            options.setSubtitleStreamIndex(subtitleStreamIndex);
            options.setMediaSourceId(mediaSourceId);
        }

        FileLogger.getFileLogger().Info("Create StreamInfo");
        mStreamInfo = new StreamBuilder().BuildVideoItem(options);

        if (mStreamInfo == null) {
            FileLogger.getFileLogger().Info("streamInfo is null");
            return false;
        }

        mStreamInfo.setMaxWidth(1920);
        mStreamInfo.setMaxHeight(1080);
        mStreamInfo.setMaxFramerate(30.0f);
        mIsStreamingHls = mStreamInfo.getProtocol() != null && mStreamInfo.getProtocol().equalsIgnoreCase("hls");
        mIsDirectStreaming = mStreamInfo.getIsDirectStream();

        if (mStreamInfo.getProtocol() == null || !mStreamInfo.getProtocol().equalsIgnoreCase("hls")) {
            mStreamInfo.setStartPositionTicks(startPositionTicks);
        }

        return true;
    }


    /**
     * Display a backdrop image for the currently loading video
     *
     * @param item The item to show the backdrop for
     */
    private void SetInitialLoadingSplashscreen(BaseItemDto item) {

        ImageOptions options;

        if (item.getType().equalsIgnoreCase("Episode")) {

            if (item.getParentBackdropImageTags() != null &&
                    item.getParentBackdropImageTags().size() > 0) {

                options = new ImageOptions();
                options.setImageType(ImageType.Backdrop);

                String imageUrl = MB3Application.getInstance().API.GetImageUrl(item.getParentBackdropItemId(), options);
                mLoadingSplashScreen.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            }

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getSeriesName())) {
                mMediaTitle.setText(item.getSeriesName());
                mMediaSubTitle.setText(item.getName());
                mMediaSubTitle.setVisibility(View.VISIBLE);
            }

        } else {

            if (item.getBackdropCount() > 0) {

                options = new ImageOptions();
                options.setImageType(ImageType.Backdrop);
                options.setWidth(getResources().getDisplayMetrics().widthPixels);

                String imageUrl = MB3Application.getInstance().API.GetImageUrl(item, options);
                mLoadingSplashScreen.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            }

            mMediaTitle.setText(item.getName());
        }

        mMediaTitle.setVisibility(View.VISIBLE);
        mLoadingSplashScreen.setVisibility(View.VISIBLE);
    }

    /**
     *  Hide the backdrop since the video has now loaded
     */
    private void HideInitialLoadingImage() {

        mLoadingSplashScreen.setVisibility(View.GONE);
        mMediaTitle.setVisibility(View.GONE);
        mMediaSubTitle.setVisibility(View.GONE);
        mBufferingIndicator.setVisibility(View.GONE);
    }

    /**
     *  Callback function for when the MediaPlayer object encounters an error
     *
     * @param mp    The MediaPlayer object that encountered an error
     * @param what  The error code that was thrown
     * @param extra The extra code that further defines the error
     * @return A boolean value stating that the error was handled
     */
    public boolean onError(MediaPlayer mp, int what, int extra) {

        if (what == MediaPlayer.MEDIA_ERROR_IO) {
            FileLogger.getFileLogger().Error("Playback Error: Media Error IO");
        } else if (what == MediaPlayer.MEDIA_ERROR_MALFORMED) {
            FileLogger.getFileLogger().Error("Playback Error: Media Error Malformed");
        } else if (what == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
            FileLogger.getFileLogger().Error("Playback Error: Media Error Not Valid For Progressive Playback");
        } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            FileLogger.getFileLogger().Error("Playback Error: Media Error Server Died");
        } else if (what == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
            FileLogger.getFileLogger().Error("Playback Error: Media Error Timed Out");
        } else if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
            FileLogger.getFileLogger().Error("Playback Error: Media Error Unknown");
        } else if (what == MediaPlayer.MEDIA_ERROR_UNSUPPORTED) {
            FileLogger.getFileLogger().Error("Playback Error: Media Error Unsupported");
        } else {
            FileLogger.getFileLogger().Error("Playback Error: Unknown Error");
        }

        if (extra == -1004)
            FileLogger.getFileLogger().Error("Playback Error: -1004");
        else if (extra == -1007)
            FileLogger.getFileLogger().Error("Playback Error: -1007");
        else if (extra == -1010)
            FileLogger.getFileLogger().Error("Playback Error: -1010");
        else if (extra == -110)
            FileLogger.getFileLogger().Error("Playback Error: -110");
        else
            FileLogger.getFileLogger().Error("Playback Error: " + PlayerHelpers.PlayerStatusFromExtra(extra));

//        if (mStreamInfo == null) return false;
//
//        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mStreamInfo.Protocol) && mStreamInfo.Protocol.equalsIgnoreCase("hls")) {
//
//            FileLogger.getFileLogger().Info("Playback failed: Trying again with fragmented mp4");
//            Log.d(TAG, "Playback failed: Trying again with fragmented mp4");
//
//            mStreamInfo.Protocol = "";
//            mStreamInfo.Container = "mp4";
//
//            loadStreamInfoIntoPlayer();
//            mStreamDetails.setText(StreamDetailsFromStreamInfo());
//
//            return true;
//
//        } else if (mStreamInfo.Container.equalsIgnoreCase("mp4")) {
//
//            FileLogger.getFileLogger().Info("Playback failed: Trying again with webm");
//            Log.d(TAG, "Playback failed: Trying again with webm");
//
//            mStreamInfo.Container = "webm";
//            mStreamInfo.VideoCodec = "vpx";
//            mStreamInfo.AudioCodec = "vorbis";
//
//            loadStreamInfoIntoPlayer();
//            mStreamDetails.setText(StreamDetailsFromStreamInfo());
//
//            return true;
//        }

        return false;
    }

    /**
     *
     */
    private void SetSurfaceDimensions() {

        FileLogger.getFileLogger().Info("SetSurfaceDimensions: " + String.valueOf(mWidth) + "x" + String.valueOf(mHeight));
        Log.i("SetSurfaceDimensions", "media Height= " + String.valueOf(mHeight) + " media Width= " + String.valueOf(mWidth));

        ViewGroup.LayoutParams lp = mSurface.getLayoutParams();

        if (lp != null) {
            lp.width = mWidth;
            lp.height = mHeight;
            mSurface.setLayoutParams(lp);

            Log.i("SetSurfaceDimensions", "LP.Height=" + String.valueOf(lp.height) + " LP.Width=" + String.valueOf(lp.width));
        }
    }

    /**
     * Compare the source video dimensions with the physical screen dimensions and determine what
     * dimensions the video can be scaled to and occupy the most screen space
     *
     * @param stream The MediaStream that represents the video to be played
     */
    private void calculateScaledVideoDimensions(MediaStream stream) {

        float newWidth;
        float newHeight;
        int screenHeight;
        int screenWidth;

        screenWidth = getScreenWidth();
        screenHeight = getScreenHeight();

        FileLogger.getFileLogger().Info("calculateScaledVideoDimensions");
        FileLogger.getFileLogger().Info("Screen Width: " + screenWidth);
        FileLogger.getFileLogger().Info("Screen Height: " + screenHeight);
        FileLogger.getFileLogger().Info("Reported Video Width: " + stream.getWidth());
        FileLogger.getFileLogger().Info("Reported Video Height: " + stream.getHeight());

        // The media info has an aspect ratio, means we can account for anamorphic video in the calculations
        if (stream.getAspectRatio() != null && !stream.getAspectRatio().isEmpty()) {
            String widthString = stream.getAspectRatio().substring(0, stream.getAspectRatio().indexOf(':'));
            FileLogger.getFileLogger().Info("A/R width: " + widthString);
            String heightString = stream.getAspectRatio().substring(stream.getAspectRatio().indexOf(':') + 1);
            FileLogger.getFileLogger().Info("A/R height: " + heightString);
            double arHeight = Float.valueOf(heightString);
            double arWidth = Float.valueOf(widthString);

            newHeight = ((float) screenWidth / (float) arWidth) * (float) arHeight;

            if (newHeight <= screenHeight) {
                FileLogger.getFileLogger().Info("Scaling based on screen width");
                mWidth = screenWidth;
                mHeight = (int) newHeight;
            } else {
                FileLogger.getFileLogger().Info("Scaling based on screen height");
                newWidth = ((float) screenHeight / (float) arHeight) * (float) arWidth;
                mWidth = (int) newWidth;
                mHeight = screenHeight;
            }
            // The media info has no aspect ratio but does have height and width. We can still scale to fit the screen
        } else if (stream.getWidth() != null && stream.getHeight() != null) {
            FileLogger.getFileLogger().Info("MediaInfo missing Aspect Ratio, working from physical dimensions");

            newHeight = ((float) screenWidth / (float) stream.getWidth()) * (float) stream.getHeight();

            if (newHeight <= screenHeight) {
                FileLogger.getFileLogger().Info("Scaling based on screen width");
                mWidth = screenWidth;
                mHeight = (int) newHeight;
            } else {
                FileLogger.getFileLogger().Info("Scaling based on screen height");
                newWidth = ((float) screenHeight / (float) stream.getHeight()) * (float) stream.getWidth();
                mWidth = (int) newWidth;
                mHeight = screenHeight;
            }
            // Video will be scaled to screen size with no regard for aspect ratio
        } else {
            FileLogger.getFileLogger().Info("MediaInfo missing physical dimensions, attempting to fill screen");
        }

        FileLogger.getFileLogger().Info("Video scaled to: " + String.valueOf(mWidth) + "x" + String.valueOf(mHeight));
    }

    private void loadStreamInfoIntoPlayer() {

        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
            mPlayer.setVolume(mCurrentVolume, mCurrentVolume);
            mPlayer.setDisplay(mHolder);
            mPlayer.setScreenOnWhilePlaying(true);
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnInfoListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnCompletionListener(this);
        }
        if (mPlayer != null) {
            loadUrlIntoPlayer(mStreamInfo.ToUrl(MB3Application.getInstance().API.getApiUrl()));
        }
    }


    private void loadUrlIntoPlayer(String url) {

        FileLogger.getFileLogger().Info(TAG + ": attempting to play - " + url);
        try {
            mPlayer.setDataSource(url);
            mPlayer.prepareAsync();
        } catch (IOException e) {
            FileLogger.getFileLogger().ErrorException("Exception handled: ", e);
        }
    }

    //**********************************************************************************************
    // Hamburger menu callbacks
    //**********************************************************************************************

    public void onBitrateSelected() {
        reloadMediaDueToParamChange();
    }

    public void onAudioStreamSelected(int index) {
        MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).AudioStreamIndex = index != -1 ? index : null;
        reloadMediaDueToParamChange();
    }

    public void onSubtitleStreamSelected(int index) {
        MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).SubtitleStreamIndex = index != -1 ? index : null;
        reloadMediaDueToParamChange();
    }

    private void reloadMediaDueToParamChange() {

        PlayerStop();
        PlayerReset();

        if (subtitleDisplayHandler != null) {
            subtitleDisplayHandler.removeCallbacks(processSubtitles);
            subtitleDisplayHandler = null;
            ttff = null;
            tto = null;
        }

        mResume = true;

        if (mIsStreamingHls) {
            MB3Application.getInstance().API.StopTranscodingProcesses(MB3Application.getInstance().API.getDeviceId(), new EmptyResponse() {
                @Override
                public void onResponse() {
                    reloadMediaInternal();
                }
            });
        } else {
            reloadMediaInternal();
        }
    }

    private void reloadMediaInternal() {
        buildStreamInfo(
                mMediaItem.getId(),
                mMediaItem.getMediaSources(),
                (long) mPreviousPosition,
                mMediaItem.getMediaSources().get(0).getId(),
                MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).AudioStreamIndex,
                MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).SubtitleStreamIndex);

        if (mStreamInfo != null) {
            loadStreamInfoIntoPlayer();
        }
    }

    public void onTimedText(Caption text) {

        if (text == null || tangible.DotNetToJavaStringHelper.isNullOrEmpty(text.content)) {
            subtitlesText.setVisibility(View.INVISIBLE);
            return;
        }
        FileLogger.getFileLogger().Info("onTimedText");

        subtitlesText.setText(Html.fromHtml(text.content));
        subtitlesText.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING) {
            FileLogger.getFileLogger().Info("MEDIA_INFO_BAD_INTERLEAVING");
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            FileLogger.getFileLogger().Info("MEDIA_INFO_BUFFERING_START");
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            FileLogger.getFileLogger().Info("MEDIA_INFO_BUFFERING_END");
        } else if (what == MediaPlayer.MEDIA_INFO_METADATA_UPDATE) {
            FileLogger.getFileLogger().Info("MEDIA_INFO_METADATA_UPDATE");
        } else if (what == MediaPlayer.MEDIA_INFO_NOT_SEEKABLE) {
            FileLogger.getFileLogger().Info("MEDIA_INFO_NOT_SEEKABLE");
        } else if (what == MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT) {
            FileLogger.getFileLogger().Info("MEDIA_INFO_SUBTITLE_TIMED_OUT");
        } else if (what == MediaPlayer.MEDIA_INFO_UNKNOWN) {
            FileLogger.getFileLogger().Info("MEDIA_INFO_UNKNOWN");
        } else if (what == MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE) {
            FileLogger.getFileLogger().Info("MEDIA_INFO_UNSUPPORTED_SUBTITLE");
        } else if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            FileLogger.getFileLogger().Info("MEDIA_INFO_VIDEO_RENDERING_START");
        } else if (what == MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING) {
            FileLogger.getFileLogger().Info("MEDIA_INFO_VIDEO_TRACK_LAGGING");
        }
        return false;
    }


    //**********************************************************************************************
    // Callback Methods
    //**********************************************************************************************

    private class GetRecordingResponse extends Response<RecordingInfoDto> {
        @Override
        public void onResponse(RecordingInfoDto recording) {
            mRecording = recording;
            mMediaItem = null;

            if (mRecording == null) return;

            if (mRecording.getRunTimeTicks() != null) {
                mRuntime = (int) (mRecording.getRunTimeTicks() / 10000);
                mRuntimeText.setText(Utils.PlaybackRuntimeFromMilliseconds(mRuntime));

            }

            buildStreamInfo(mRecording.getId(),
                    new ArrayList<>(mRecording.getMediaSources()),
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).startPositionTicks != null
                            ? MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).startPositionTicks
                            : 0L,
                    mRecording.getMediaSources().get(0).getId(),
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).AudioStreamIndex,
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).SubtitleStreamIndex);

            if (mStreamInfo == null) return;

            SetNowPlayingInfo(mRecording);
            loadStreamInfoIntoPlayer();
            mOptionsMenu.setOnClickListener(new PlaybackOptionsMenuClickListener(mStreamInfo.getMediaSource(), PlaybackActivity.this));

            if (mRecording.getRunTimeTicks() != null)
                mRuntime = (int) (mRecording.getRunTimeTicks() / 10000);
        }
    }

    private Response<BaseItemDto> getItemResponse = new Response<BaseItemDto>() {

        @Override
        public void onResponse(BaseItemDto response) {
            mMediaItem = response;
            mRecording = null;

            if (mMediaItem == null) {

                Toast.makeText(PlaybackActivity.this, "Error communicating with server", Toast.LENGTH_LONG).show();
                PlaybackActivity.this.finish();
                return;
            }

            SetNowPlayingInfo(mMediaItem);

            SetInitialLoadingSplashscreen(mMediaItem);

            if (mMediaItem.getRunTimeTicks() != null) {
                mRuntime = (int) (mMediaItem.getRunTimeTicks() / 10000);
                mRuntimeText.setText(Utils.PlaybackRuntimeFromMilliseconds(mRuntime));

            }

            mWidth = mMetrics.widthPixels;
            mHeight = mMetrics.heightPixels;

            if (mMediaItem.getMediaStreams() != null) {
                for (MediaStream stream : mMediaItem.getMediaStreams()) {
                    if (stream.getHeight() != null && stream.getWidth() != null) {

                        calculateScaledVideoDimensions(stream);
                        break;
                    }
                }
            }

            if (mMediaItem.getType().equalsIgnoreCase("PodCastAudio")
                    || mMediaItem.getType().equalsIgnoreCase("ChannelAudioItem")) {
//                String mUrl = mMediaItem.getPath();
//
//                mStreamDetails.setText("");
//                mStreamDetails.setText("");
//
//                if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mUrl)) {
//                    loadUrlIntoPlayer(mUrl);
//                }

                mStreamInfo = PlayerHelpers.buildStreamInfoAudio(mMediaItem.getId(), mMediaItem.getMediaSources(), 0L, null);
                if (mStreamInfo != null) {
                    loadStreamInfoIntoPlayer();

                    mStreamDetails.setText(StreamDetailsFromStreamInfo());
                    mOptionsMenu.setOnClickListener(new PlaybackOptionsMenuClickListener(mStreamInfo.getMediaSource(), PlaybackActivity.this));
                }
            } else {
                mResume = currentlyPlayingIndex == 0
                        && MB3Application.getInstance().PlayerQueue != null
                        && MB3Application.getInstance().PlayerQueue.PlaylistItems != null
                        && MB3Application.getInstance().PlayerQueue.PlaylistItems.size() > 0
                        && MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).startPositionTicks != null
                        && MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).startPositionTicks > 0L;

                buildStreamInfo(mMediaItem.getId(),
                        mMediaItem.getMediaSources(),
                        mResume
                                ? mMediaItem.getUserData() != null
                                ? mMediaItem.getUserData().getPlaybackPositionTicks()
                                : 0
                                : 0,
                        mMediaItem.getMediaSources() != null && !mMediaItem.getMediaSources().isEmpty() ? mMediaItem.getMediaSources().get(0).getId() : null,
                        MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).AudioStreamIndex,
                        MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).SubtitleStreamIndex);

                if (mStreamInfo != null) {
                    loadStreamInfoIntoPlayer();

                    mStreamDetails.setText(StreamDetailsFromStreamInfo());
                    mOptionsMenu.setOnClickListener(new PlaybackOptionsMenuClickListener(mStreamInfo.getMediaSource(), PlaybackActivity.this));
                } else {

                    if (MB3Application.getInstance().PlayerQueue.PlaylistItems.size() > currentlyPlayingIndex + 1) {
                        currentlyPlayingIndex += 1;

                        isPrepared = false;
                        // Make sure the activity knows to update the playlist
                        UpdateCurrentPlayingIndex(currentlyPlayingIndex);

                        MB3Application.getInstance().API.GetItemAsync(
                                MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).Id,
                                MB3Application.getInstance().API.getCurrentUserId(),
                                this);
                        Toast.makeText(PlaybackActivity.this, MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex - 1).Name + " was skipped due to missing media info", Toast.LENGTH_LONG).show();
                    } else {
                        mIsPaused = true;
                        isPrepared = false;

                        Toast.makeText(PlaybackActivity.this, MB3Application.getInstance().PlayerQueue.PlaylistItems.get(currentlyPlayingIndex).Name + " was skipped due to missing media info", Toast.LENGTH_LONG).show();
                        PlaybackActivity.this.finish();
                    }
                }
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private void PlayerStop() {
        try {
            mPlayer.stop();
        } catch (IllegalStateException e) {
            FileLogger.getFileLogger().ErrorException("Exception handled ", e);
        }
    }

    private void PlayerReset() {
        try {
            mPlayer.reset();
        } catch (IllegalStateException e) {
            FileLogger.getFileLogger().ErrorException("Exception handled ", e);
        }
    }

    private void PlayerPause() {
        try {
            if (mPlayer != null && mPlayer.isPlaying()) {
                mPlayer.pause();
            }
        } catch (IllegalStateException e) {
            Log.d(TAG, "Error pausing player");
        }
    }

    private void PlayerSeekTo(int mSec) {
        try {
            mPlayer.seekTo(mSec);
        } catch (IllegalStateException e) {
            FileLogger.getFileLogger().ErrorException("Exception handled ", e);
        }
    }

    private void sendPlaybackStartedToServer(Long position) {
        if (mStreamInfo == null) return;
        PlaybackStartInfo info = new PlaybackStartInfo();
        info.setQueueableMediaTypes(new ArrayList<String>() {{ add("Audio"); add("Video"); }});
        info.setPositionTicks(position);
        info.setAudioStreamIndex(mStreamInfo.getAudioStreamIndex());
        info.setCanSeek(true);
        info.setIsMuted(mMuted);
        info.setIsPaused(mIsPaused);
        info.setItemId(mStreamInfo.getItemId());
        info.setMediaSourceId(mStreamInfo.getMediaSourceId());
        info.setPlayMethod(mStreamInfo.getIsDirectStream() ? PlayMethod.DirectStream : PlayMethod.Transcode);
        info.setSubtitleStreamIndex(mStreamInfo.getSubtitleStreamIndex());
        info.setVolumeLevel((int) mCurrentVolume * 100);

        MB3Application.getInstance().API.ReportPlaybackStartAsync(info, new EmptyResponse());
    }

    private void sendPlaybackProgressToServer(Long position) {
        if (mStreamInfo == null) return;
        PlaybackProgressInfo progressInfo = new PlaybackProgressInfo();
        progressInfo.setPositionTicks(position);
        progressInfo.setAudioStreamIndex(mStreamInfo.getAudioStreamIndex());
        progressInfo.setCanSeek(true);
        progressInfo.setIsMuted(mMuted);
        progressInfo.setIsPaused(mIsPaused);
        progressInfo.setItemId(mStreamInfo.getItemId());
        progressInfo.setMediaSourceId(mStreamInfo.getMediaSourceId());
        progressInfo.setPlayMethod(mStreamInfo.getIsDirectStream() ? PlayMethod.DirectStream : PlayMethod.Transcode);
        progressInfo.setSubtitleStreamIndex(mStreamInfo.getSubtitleStreamIndex());
        progressInfo.setVolumeLevel((int) mCurrentVolume * 100);

        MB3Application.getInstance().API.ReportPlaybackProgressAsync(progressInfo, new EmptyResponse());
    }

    private void sendPlaybackStoppedToServer(Long position) {
        if (mStreamInfo == null) return;
        PlaybackStopInfo stopInfo = new PlaybackStopInfo();
        stopInfo.setItemId(mStreamInfo.getItemId());
        stopInfo.setMediaSourceId(mStreamInfo.getMediaSourceId());
        stopInfo.setPositionTicks(position);

        MB3Application.getInstance().API.ReportPlaybackStoppedAsync(stopInfo, new EmptyResponse());
    }

    private String StreamDetailsFromStreamInfo() {
        String streamType = (mStreamInfo.getIsDirectStream() ? "DirectStream" : "Transcode") + ": ";

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mStreamInfo.getProtocol())) {
            streamType += mStreamInfo.getProtocol() + ", ";
        }

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mStreamInfo.getContainer())) {
            streamType += mStreamInfo.getContainer() + ", ";
        }

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mStreamInfo.getVideoCodec())) {
            streamType += mStreamInfo.getVideoCodec() + ", ";
        }

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mStreamInfo.getAudioCodec())) {
            streamType += mStreamInfo.getAudioCodec();
        }

        return streamType;
    }

    protected void addItemsToPlaylist(String[] itemIds) {
        for (String id : itemIds) {
            PlaylistItem item = new PlaylistItem();
            item.Id = id;
            MB3Application.getInstance().PlayerQueue.PlaylistItems.add(item);
        }
    }

    private void handleSeek(int seekPositionMs) {
        mSurface.removeCallbacks(onEverySecond);
        isSeeking = false;
        try {
            if (seekPositionMs == -1) return;

            if (mMediaItem != null) {
                mMediaItem.getUserData().setPlaybackPositionTicks((long) seekPositionMs * 10000);
            } else if (mRecording != null) {
                mRecording.getUserData().setPlaybackPositionTicks((long) seekPositionMs * 10000);
            }

            if (mMediaItem != null && (mMediaItem.getType().equalsIgnoreCase("VodCastVideo") ||
                    mMediaItem.getType().equalsIgnoreCase("PodCastAudio"))) {

                PlayerSeekTo(seekPositionMs);
                mSurface.post(onEverySecond);

            } else if (mIsDirectStreaming || mIsStreamingHls) {

                mPlayerPositionOffset = 0;
                PlayerSeekTo(seekPositionMs);
                mSurface.post(onEverySecond);

            } else {

                mBufferingIndicator.setVisibility(ProgressBar.VISIBLE);

                PlayerStop();
                isPrepared = false;
                PlayerReset();

                mPlayerPositionOffset = seekPositionMs;
                mStreamInfo.setStartPositionTicks((long)seekPositionMs * 10000);

                String url = mStreamInfo.ToUrl(MB3Application.getInstance().API.getApiUrl());
                FileLogger.getFileLogger().Info("Seek performed");
                FileLogger.getFileLogger().Info("new url: " + url);

                mPlayer.setDataSource(url);
                mPlayer.prepareAsync();
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
        } catch (IOException e) {
            FileLogger.getFileLogger().ErrorException("onStopTrackingTouch: IOException", e);
            e.printStackTrace();
        }
    }
}
