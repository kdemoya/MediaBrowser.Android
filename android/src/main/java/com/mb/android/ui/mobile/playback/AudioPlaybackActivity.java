package com.mb.android.ui.mobile.playback;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.Playlist;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.activities.BaseMbMobileActivity;
import mediabrowser.apiinteraction.Response;
import com.mb.android.logging.FileLogger;
import com.mb.android.player.AudioPlayerListener;
import com.mb.android.player.AudioService;
import com.mb.android.ui.mobile.album.BaseSongAdapter;
import com.mb.android.utils.Utils;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.session.PlayRequest;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;

/**
 * Created by Mark on 12/12/13.
 *
 * UI that is displayed to the user when playing audio
 */
public class AudioPlaybackActivity extends BaseMbMobileActivity implements AudioPlayerListener {

    private static final String TAG = "AudioPlaybackActivity";
    private static final String AVRCP_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    private static final String AVRCP_META_CHANGED = "com.android.music.metachanged";
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private SeekBar mPlaybackProgress;
    private ImageView mPlayPauseButton;
    private ImageView mMuteUnMuteButton;
    private ImageButton mShuffleButton;
    private ImageButton mRepeatButton;
    private ImageButton mFavoriteButton;
    private ImageButton mLikeButton;
    private ImageButton mDislikeButton;
    private TextView mCurrentPositionText;
    private TextView mRuntimeText;
    private NetworkImageView mMusicScreenSaver;
    private TextView mNowPlayingText;
    private TextView mSecondaryText;
    private NetworkImageView mediaImage;
    private DragSortListView mPlayList;
    private ImageView mPanelDirection;
    private int currentPlayingIndex = 0;
    private boolean mIsFavorite = false;
    private Boolean mLikes;
    private BaseSongAdapter songAdapter;
    private BaseItemDto mItem;
    private AudioService mAudioService;
    private boolean mPlaylistVisible;


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.activity_audio_playback);

        if (mActionBar != null) {
            mActionBar.hide();
        }

        // Just in case the TV Theme is still playing
        MB3Application.getInstance().StopMedia();

        // acquire UI elements
        slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        slidingUpPanelLayout.setPanelSlideListener(panelSlideListener);
        mPanelDirection = (ImageView) findViewById(R.id.ivPanelDirectionIndicator);
        mPlayList = (DragSortListView) findViewById(R.id.playlist_drawer);
        DragSortController controller = buildController(mPlayList);
        mPlayList.setFloatViewManager(controller);
        mPlayList.setOnTouchListener(controller);
        mPlayList.setDragEnabled(true);
        mPlayList.setDropListener(onDrop);
        mPlayList.setRemoveListener(onRemove);
        mPlayList.setEmptyView(getLayoutInflater().inflate(R.layout.widget_playlist_empty_view, null));
        mPlaybackProgress = (SeekBar) findViewById(R.id.sbPlaybackProgress);
        mPlayPauseButton = (ImageView) findViewById(R.id.ivPlayPause);
        ImageButton mPreviousButton = (ImageButton) findViewById(R.id.ivPrevious);
        ImageButton mNextButton = (ImageButton) findViewById(R.id.ivNext);
        mFavoriteButton = (ImageButton) findViewById(R.id.ivFavorite);
        mLikeButton = (ImageButton) findViewById(R.id.ivLike);
        mDislikeButton = (ImageButton) findViewById(R.id.ivDislike);
        mShuffleButton = (ImageButton) findViewById(R.id.ivShuffle);
        mRepeatButton = (ImageButton) findViewById(R.id.ivRepeat);
        mMuteUnMuteButton = (ImageView) findViewById(R.id.ivAudioMute);
        mMusicScreenSaver = (NetworkImageView) findViewById(R.id.ivMusicScreenSaver);
        mCurrentPositionText = (TextView) findViewById(R.id.tvCurrentPosition);
        mRuntimeText = (TextView) findViewById(R.id.tvRuntime);
        mNowPlayingText = (TextView) findViewById(R.id.tvPlaybackPrimaryText);
        mSecondaryText = (TextView) findViewById(R.id.tvPlaybackSecondaryText);
        mediaImage = (NetworkImageView) findViewById(R.id.ivPlaybackMediaImage);
        mediaImage.setDefaultImageResId(R.drawable.music_square_bg);
        TextView clearPlaylistText = (TextView) findViewById(R.id.tvClearPlaylist);

        // set event handlers
        mPlayPauseButton.setOnClickListener(onPlayPauseClick);
        mMuteUnMuteButton.setOnClickListener(onMuteUnmuteClick);
        mDislikeButton.setOnClickListener(onDislikeClick);
        mLikeButton.setOnClickListener(onLikeClick);
        mPreviousButton.setOnClickListener(onPreviousClick);
        mNextButton.setOnClickListener(onNextClick);
        mShuffleButton.setOnClickListener(onShuffleClick);
        mRepeatButton.setOnClickListener(onRepeatClick);
        mFavoriteButton.setOnClickListener(onFavoriteClick);
        clearPlaylistText.setOnClickListener(onClearPlaylistClick);
        mPlaybackProgress.setOnSeekBarChangeListener(onSeekBarChangeListener);
        if (MB3Application.getInstance().PlayerQueue.PlaylistItems.size() > 1) {
            mPreviousButton.setEnabled(true);
            mNextButton.setEnabled(true);
        }

        mAudioService = MB3Application.getAudioService();
        mAudioService.addAudioPlayerListener(AudioPlaybackActivity.this);
        if (mCastManager != null) {
            Log.d(TAG, "Adding Audio player listener");
            mAudioService.addAudioPlayerListener(mCastManager);
        }
        songAdapter = new BaseSongAdapter(MB3Application.getInstance().PlayerQueue.PlaylistItems, this);
        mPlayList.setAdapter(songAdapter);
        mPlayList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i != currentPlayingIndex) {
                    mAudioService.loadItemAtIndex(i);
                }
            }
        });
        if (mAudioService.getPlayerState().equals(AudioService.PlayerState.PLAYING) ||
                mAudioService.getPlayerState().equals(AudioService.PlayerState.PAUSED)) {
            Log.d(TAG, "Player is playing - Setting Now Playing Info");
            mItem = mAudioService.getCurrentItem();
            setNowPlayingInfo(mItem);
            onPlayPauseChanged(mAudioService.getPlayerState().equals(AudioService.PlayerState.PAUSED));
        } else {
            Log.d(TAG, "Play Media");
            mAudioService.playMedia();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FileLogger.getFileLogger().Info(TAG + ": onDestroy");
        mPlayPauseButton.removeCallbacks(onEverySecond);
        if (mAudioService != null) {
            mAudioService.removeAudioPlayerListener(this);
        }
    }

    @Override
    protected void onConnectionRestored() {

    }

    @Override
    public void onBackPressed() {
        if (mPlaylistVisible) {
            slidingUpPanelLayout.collapsePanel();
        } else {
            super.onBackPressed();
        }
    }

    private void updateCurrentPlayingIndex(int newIndex) {

        if (newIndex < 0 || newIndex >= MB3Application.getInstance().PlayerQueue.PlaylistItems.size())
            return;

        currentPlayingIndex = newIndex;
        songAdapter.setCurrentPlayingIndex(currentPlayingIndex);
    }

    @Override
    public void onItemLoaded(BaseItemDto baseItemDto, int playlistPositionIndex) {
        mItem = baseItemDto;
        setNowPlayingInfo(mItem);
        setRatingIcons(mItem);
        updateCurrentPlayingIndex(playlistPositionIndex);
        bluetoothNotifyChange(AVRCP_META_CHANGED);
    }

    @Override
    public void onPlaylistCreated() {

    }

    @Override
    public void onPlaylistCompleted() {
        this.finish();
    }

    @Override
    public void onPlayPauseChanged(boolean paused) {
        bluetoothNotifyChange(AVRCP_PLAYSTATE_CHANGED);
        mPlayPauseButton.setImageResource(paused ? R.drawable.ap_play : R.drawable.ap_pause);

    }

    @Override
    public void onVolumeChanged(boolean muted, float volume) {

        if (muted) {
            mMuteUnMuteButton.setImageResource(R.drawable.ap_mute_active);
        } else {
            mMuteUnMuteButton.setImageResource(R.drawable.ap_mute);
        }
    }

    @Override
    public void onShuffleChanged(boolean isShuffling) {

        if (isShuffling) {
            mShuffleButton.setSelected(true);
        } else {
            mShuffleButton.setSelected(false);
        }
    }

    @Override
    public void onRepeatChanged(boolean isRepeating) {

        if (isRepeating) {
            mRepeatButton.setSelected(true);
        } else {
            mRepeatButton.setSelected(false);
        }
    }

    private void setNowPlayingInfo(BaseItemDto mediaItem) {
        FileLogger.getFileLogger().Info(TAG + ": SetNowPlayingInfo");
        if (mediaItem == null) return;
        setTrackAndTitleText(mediaItem.getIndexNumber(), mediaItem.getName());
        setArtistAlbumTextFromBaseItem(mediaItem);
        setDurationAndProgressValues();
        startPositionUpdatePulseCheck();
        updateCurrentPlayingIndex(mAudioService.getCurrentlyPlayingIndex());
        // Need the parent item to display a backdrop image
        MB3Application.getInstance().API.GetItemAsync(
                mediaItem.getParentId(),
                MB3Application.getInstance().API.getCurrentUserId(),
                getAlbumResponse);
    }

    private void setTrackAndTitleText(Integer trackIndex, String trackName) {
        mNowPlayingText.setText((trackIndex != null ? String.valueOf(trackIndex) : "00") + ". "
                + (trackName != null ? trackName : ""));
    }

    private void setArtistAlbumTextFromBaseItem(BaseItemDto song) {
        String artistAlbumText = song.getArtists() != null && song.getArtists().size() > 0 ? song.getArtists().get(0) : "";
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(song.getAlbum())) {
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(artistAlbumText)) {
                artistAlbumText += " / ";
            }
            artistAlbumText += song.getAlbum();
        }
        mSecondaryText.setText(artistAlbumText);
    }

    private void setDurationAndProgressValues() {
        if (mAudioService.getDuration() > 0) {
            mPlaybackProgress.setMax(mAudioService.getDuration());
            mPlaybackProgress.setProgress(mAudioService.getCurrentPosition());
            mPlaybackProgress.setVisibility(ProgressBar.VISIBLE);
            mRuntimeText.setText(Utils.PlaybackRuntimeFromMilliseconds(mAudioService.getDuration()));
        } else {
            mPlaybackProgress.setVisibility(ProgressBar.INVISIBLE);
        }
    }

    private void startPositionUpdatePulseCheck() {
        mPlayPauseButton.removeCallbacks(onEverySecond);
        mPlayPauseButton.postDelayed(onEverySecond, 1000);
    }

    private void setRatingIcons(BaseItemDto baseItemDto) {

        if (baseItemDto == null || baseItemDto.getUserData() == null) return;

        if (baseItemDto.getUserData().getIsFavorite()) {
            mFavoriteButton.setSelected(true);
            mIsFavorite = true;
        } else {
            mFavoriteButton.setSelected(false);
            mIsFavorite = false;
        }

        if (baseItemDto.getUserData().getLikes() == null) {
            mLikeButton.setSelected(false);
            mDislikeButton.setSelected(false);
            mLikes = null;
        } else if (baseItemDto.getUserData().getLikes()) {
            mLikeButton.setSelected(true);
            mDislikeButton.setSelected(false);
            mLikes = true;
        } else {
            mLikeButton.setSelected(false);
            mDislikeButton.setSelected(true);
            mLikes = false;
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
                mAudioService.togglePause();
        }
    };

    /**
     * Mute/UnMute button
     */
    private View.OnClickListener onMuteUnmuteClick = new View.OnClickListener() {

        public void onClick(View v) {
                mAudioService.toggleMute();

        }
    };

    /**
     * Dislike button
     */
    private View.OnClickListener onDislikeClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (mItem == null) return;
            if (mLikes == null || mLikes) {
                MB3Application.getInstance().API.UpdateUserItemRatingAsync(mItem.getId(),
                        MB3Application.getInstance().API.getCurrentUserId(), false, new UpdateUserDataResponse());
            } else {
                MB3Application.getInstance().API.ClearUserItemRatingAsync(mItem.getId(),
                        MB3Application.getInstance().API.getCurrentUserId(), new UpdateUserDataResponse());
            }
        }
    };

    /**
     * Like button
     */
    private View.OnClickListener onLikeClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (mItem == null) return;
            if (mLikes == null || !mLikes) {
                MB3Application.getInstance().API.UpdateUserItemRatingAsync(mItem.getId(),
                        MB3Application.getInstance().API.getCurrentUserId(), true, new UpdateUserDataResponse());
            } else {
                MB3Application.getInstance().API.ClearUserItemRatingAsync(mItem.getId(),
                        MB3Application.getInstance().API.getCurrentUserId(), new UpdateUserDataResponse());
            }
        }
    };

    /**
     * Shuffle
     */
    private View.OnClickListener onShuffleClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            MB3Application.getAudioService().toggleShuffle();
        }
    };

    /**
     * Repeat
     */
    private View.OnClickListener onRepeatClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            MB3Application.getAudioService().toggleRepeat();
        }
    };

    /**
     * Previous button
     */
    private View.OnClickListener onPreviousClick = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            mAudioService.previous();
        }
    };


    /**
     * Next button
     */
    private View.OnClickListener onNextClick = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            mAudioService.next();
        }
    };

    /**
     * Favorite Button
     */
    private View.OnClickListener onFavoriteClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (mItem == null) return;
            MB3Application.getInstance().API.UpdateFavoriteStatusAsync(mItem.getId(),
                        MB3Application.getInstance().API.getCurrentUserId(), !mIsFavorite, new UpdateUserDataResponse());
        }
    };

    /**
     * Clear Playlist Button
     */
    private View.OnClickListener onClearPlaylistClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            MB3Application.getInstance().PlayerQueue.PlaylistItems = new ArrayList<>();
            songAdapter.clearPlaylist();
        }
    };

    //**********************************************************************************************
    // IApiCallback methods
    //**********************************************************************************************

    private Response<BaseItemDto> getAlbumResponse = new Response<BaseItemDto>() {

        @Override
        public void onResponse(BaseItemDto item) {

            if (item != null) {
                MB3Application.getInstance().API.GetItemAsync(
                        item.getParentId(),
                        MB3Application.getInstance().API.getCurrentUserId(),
                        getArtistResponse);

                if (item.getHasPrimaryImage()) {
                    Log.i(TAG, "Has Primary Image");

                    ImageOptions options = new ImageOptions();
                    options.setImageType(ImageType.Primary);
                    options.setHeight(750);
                    try {
                        options.setEnableImageEnhancers(PreferenceManager
                                .getDefaultSharedPreferences(MB3Application.getInstance())
                                .getBoolean("pref_enable_image_enhancers", true));
                    } catch (Exception e) {
                        Log.d("AbstractMediaAdapter", "Error reading preferences");
                    }
                    String imageUrl = MB3Application.getInstance().API.GetImageUrl(item, options);
                    mediaImage.setDefaultImageResId(R.drawable.music_square_bg);
                    mediaImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());

                } else {
                    mediaImage.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
                }
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private Response<BaseItemDto> getArtistResponse = new Response<BaseItemDto>() {

        @Override
        public void onResponse(BaseItemDto currentArtist) {

            if (currentArtist != null && currentArtist.getBackdropCount() > 0) {

                ImageOptions backdropOptions = new ImageOptions();
                backdropOptions.setImageType(ImageType.Backdrop);
                backdropOptions.setImageIndex(0);
                backdropOptions.setMaxHeight(Math.min(getScreenHeight(), 720));

                String backdropImageUrl = MB3Application.getInstance().API.GetImageUrl(currentArtist, backdropOptions);
                mMusicScreenSaver.setImageUrl(backdropImageUrl, MB3Application.getInstance().API.getImageLoader());
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private class UpdateUserDataResponse extends Response<UserItemDataDto> {
        @Override
        public void onResponse(UserItemDataDto data) {

            if (data == null) {
                Log.d(TAG, "response or response.data is null");
                return;
            }

            mItem.setUserData(data);
            setRatingIcons(mItem);
        }
    }


    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        int progressValue = -1;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            if (fromUser) {
                progressValue = progress;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            if (progressValue == -1) return;

            if (mAudioService != null) {
                mAudioService.playerSeekTo(progressValue);
            }

        }
    };

    /**
     *
     */
    private Runnable onEverySecond = new Runnable() {

        public void run() {
            mPlaybackProgress.setProgress(mAudioService.getCurrentPosition());
            mCurrentPositionText.setText(Utils.PlaybackRuntimeFromMilliseconds(mAudioService.getCurrentPosition()));
            mPlayPauseButton.postDelayed(onEverySecond, 1000);
        }
    };

    SlidingUpPanelLayout.PanelSlideListener panelSlideListener = new SlidingUpPanelLayout.PanelSlideListener() {
        @Override
        public void onPanelSlide(View panel, float slideOffset) { }

        @Override
        public void onPanelCollapsed(View panel) {
            mPanelDirection.setImageResource(R.drawable.ic_action_collapse);
            mPlaylistVisible = false;
        }

        @Override
        public void onPanelExpanded(View panel) {
            mPanelDirection.setImageResource(R.drawable.ic_action_expand);
            mPlaylistVisible = true;
        }

        @Override
        public void onPanelAnchored(View panel) { }

        @Override
        public void onPanelHidden(View panel) { }
    };

    //**********************************************************************************************
    // DragSortListView Methods
    //**********************************************************************************************

    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    if (from != to) {
                        PlaylistItem item = (PlaylistItem) songAdapter.getItem(from);
                        songAdapter.remove(item);
                        songAdapter.insert(item, to);

                        if (mAudioService == null) return;

                        if (mAudioService.getCurrentlyPlayingIndex() == from) {
                            // The user is moving the item being played
                            mAudioService.setCurrentlyPlayingIndex(to);
                            songAdapter.setCurrentPlayingIndex(to);
                        } else if (from < mAudioService.getCurrentlyPlayingIndex() || to <= mAudioService.getCurrentlyPlayingIndex()) {
                            // The user has moved the item to earlier in the list than the item
                            // being played. Need to offset the playing indexes to account for it.
                            int newIndex = songAdapter.getItemIndex(mAudioService.getCurrentItem().getId());
                            if (newIndex == -1) {
                                Log.d(TAG, "newIndex is -1");
                                return;
                            }

                            mAudioService.setCurrentlyPlayingIndex(newIndex);
                            songAdapter.setCurrentPlayingIndex(newIndex);
                        }
                    }
                }
            };

    private DragSortListView.RemoveListener onRemove =
            new DragSortListView.RemoveListener() {
                @Override
                public void remove(int which) {

                    songAdapter.remove((PlaylistItem)songAdapter.getItem(which));

                    if (mAudioService == null) return;

                    if (mAudioService.getCurrentlyPlayingIndex() == which) {
                        // The user is removing the currently playing item
                        if (mAudioService.getQueueItemCount() == 1) {
                            // Only one item. Stop the player
                            mAudioService.stopMedia();
                        } else {
                            mAudioService.playMediaAt(which);
                        }
                    } else if (mAudioService.getCurrentlyPlayingIndex() > which) {
                        // The user removed an item prior to the currently playing item. Need to
                        // update the currently playing index or else weirdness will happen
                        mAudioService.setCurrentlyPlayingIndex(mAudioService.getCurrentlyPlayingIndex() - 1);
                    }
                }
            };


    /**
     * Called in onCreateView. Override this to provide a custom
     * DragSortController.
     */
    public DragSortController buildController(DragSortListView dslv) {
        // defaults are
        //   dragStartMode = onDown
        //   removeMode = flingRight
        DragSortController controller = new DragSortController(dslv);
        controller.setDragHandleId(R.id.ivDragHandle);
//        controller.setClickRemoveId(R.id.click_remove);
        controller.setRemoveEnabled(true);
        controller.setSortEnabled(true);
        controller.setDragInitMode(DragSortController.ON_DOWN);
        controller.setRemoveMode(DragSortController.FLING_REMOVE);
        return controller;
    }

    private void bluetoothNotifyChange(String what) {
        Intent i = new Intent(what);
        i.putExtra("id", 1L);
        i.putExtra("artist", getArtistName());
        i.putExtra("album",getAlbumName());
        i.putExtra("track", getTrackName());
        i.putExtra("playing", isPlaying());
        i.putExtra("ListSize", getQueue());
        i.putExtra("duration", duration());
        i.putExtra("position", position());
        sendBroadcast(i);
    }

    private String getArtistName() {
        String artistString = "";
        if (mItem != null && mItem.getArtists() != null && mItem.getArtists().size() > 0) {
            for (String artist : mItem.getArtists()) {
                if (artistString.length() > 0) {
                    artistString += ", ";
                }
                artistString += artist;
            }
        }
        return artistString;
    }

    private String getAlbumName() {
        if (mItem != null && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(mItem.getAlbum())) {
            return mItem.getAlbum();
        }
        return "";
    }

    private String getTrackName() {
        if (mItem != null && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(mItem.getName())) {
            return mItem.getName();
        }
        return "";
    }

    private boolean isPlaying() {
        return MB3Application.getAudioService().getPlayerState().equals(AudioService.PlayerState.PLAYING);
    }

    private int getQueue() {
        return (MB3Application.getInstance().PlayerQueue != null
                && MB3Application.getInstance().PlayerQueue.PlaylistItems != null)
                ? MB3Application.getInstance().PlayerQueue.PlaylistItems.size()
                : 0;
    }

    private int duration() {
        return MB3Application.getAudioService().getDuration();
    }

    private int position() {
        return MB3Application.getAudioService().getCurrentPosition();
    }

    @Override
    public void onRemotePlayRequest(PlayRequest request, String mediaType) {
        FileLogger.getFileLogger().Info(TAG + ": remote play request received");
        if ("audio".equalsIgnoreCase(mediaType)) {
            FileLogger.getFileLogger().Info(TAG + ": first item is audio.");
            if (mAudioService != null) {
                mAudioService.stopMedia();
                MB3Application.getInstance().PlayerQueue = new Playlist();
                addItemsToPlaylist(request.getItemIds());
                FileLogger.getFileLogger().Info(TAG + ": audio service stopped");
                mAudioService.playMedia();
            }
            FileLogger.getFileLogger().Info(TAG + ": finished audio play request");
        } else if ("video".equalsIgnoreCase(mediaType)) {
            FileLogger.getFileLogger().Info(TAG + ": first item is video");
            if (mAudioService != null) {
                mAudioService.stopMedia();
                FileLogger.getFileLogger().Info(TAG + ": audio service killed");
            }
            MB3Application.getInstance().PlayerQueue = new Playlist();
            addItemsToPlaylist(request.getItemIds());
            Intent intent = new Intent(this, PlaybackActivity.class);
            startActivity(intent);
            this.finish();
            FileLogger.getFileLogger().Info(TAG + ": finished video play request");
        } else {
            FileLogger.getFileLogger().Info(TAG + ": unable to process play request. Unsupported media type");
        }
    }

    @Override
    public void onRemoteBrowseRequest(BaseItemDto baseItemDto) {
        FileLogger.getFileLogger().Info(TAG + ": ignoring remote browse request due to media playback");
    }
}