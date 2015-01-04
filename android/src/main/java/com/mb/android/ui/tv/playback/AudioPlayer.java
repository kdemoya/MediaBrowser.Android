package com.mb.android.ui.tv.playback;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.Playlist;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.interfaces.IWebsocketEventListener;
import com.mb.android.ui.mobile.album.BaseSongAdapter;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import com.mb.android.logging.FileLogger;
import com.mb.android.utils.BackdropSlideshow;
import com.mb.android.utils.Utils;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.session.PlayRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that handles audio playback
 */
public class AudioPlayer extends FragmentActivity implements MediaPlayer.OnCompletionListener, MediaPlayer.OnInfoListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, IWebsocketEventListener {

    private static final String TAG = "AudioPlayer";
    protected NetworkImageView mBackdropImage1;
    protected NetworkImageView mBackdropImage2;
    protected ViewSwitcher mBackdropSwitcher;
    private NetworkImageView mAlbumImage;
    private TextView mSongNameText;
    private TextView mArtistNameText;
    private TextView mAlbumNameText;
    private TextView mCurrentPositionText;
    private TextView mRuntimeText;
    private RelativeLayout mControls;
    private ImageView mPlayPauseButton;
    private ImageView mMuteButton;
    private ImageView mLikeButton;
    private ImageView mDislikeButton;
    private ImageView mFavoriteButton;
    private MediaPlayer audioPlayer;
    private boolean isFresh = true;
    private StreamInfo mStreamInfo;
    private int mCurrentlyPlayingIndex = 0;
    private boolean mIsPaused;
    private boolean mIsPrepared;
    private boolean mIsMuted;
    private boolean mControlUiVisible;
    private float mCurrentVolume = 1.0f;
    private long mLastProgressReport;
    private ListView mPlaylist;
    private boolean mPlaylistVisible;
    private BaseSongAdapter songAdapter;
    private Boolean mLikes;
    private boolean mIsFavorite;
    private BackdropSlideshow mSlideShow;
    private List<Integer> mDefaultImages;
    private AnimationSet asFadeout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.tv_activity_audio_player);
        setOverscanValues();

        mBackdropSwitcher = (ViewSwitcher) findViewById(R.id.vsBackdropImages);
        mBackdropImage1 = (NetworkImageView) findViewById(R.id.ivBackdropImage1);
        mBackdropImage2 = (NetworkImageView) findViewById(R.id.ivBackdropImage2);
        mSlideShow = new BackdropSlideshow(mBackdropSwitcher, mBackdropImage1, mBackdropImage2);
        mAlbumImage = (NetworkImageView) findViewById(R.id.ivAlbumCover);
        mAlbumImage.setDefaultImageResId(R.drawable.music_square_bg);
        mSongNameText = (TextView) findViewById(R.id.tvSongName);
        mArtistNameText = (TextView) findViewById(R.id.tvArtistName);
        mAlbumNameText = (TextView) findViewById(R.id.tvAlbumName);
        mCurrentPositionText = (TextView) findViewById(R.id.tvCurrentPosition);
        mRuntimeText = (TextView) findViewById(R.id.tvRuntime);
        mControls = (RelativeLayout) findViewById(R.id.llTransportControls);
        mPlayPauseButton = (ImageView) findViewById(R.id.ivPlayPause);
        mPlayPauseButton.setOnClickListener(onClickListener);
        ImageView mPreviousButton = (ImageView) findViewById(R.id.ivPrevious);
        ImageView mRewindButton = (ImageView) findViewById(R.id.ivRewind);
        mRewindButton.setOnClickListener(onClickListener);
        ImageView mFastForwardButton = (ImageView) findViewById(R.id.ivFastForward);
        mFastForwardButton.setOnClickListener(onClickListener);
        ImageView mNextButton = (ImageView) findViewById(R.id.ivNext);
        mPlaylist = (ListView) findViewById(R.id.lvPlayList);
        ImageView mPlaylistButton = (ImageView) findViewById(R.id.ivPlaylistSelection);
        mPlaylistButton.setOnClickListener(onClickListener);
        mMuteButton = (ImageView) findViewById(R.id.ivAudioMute);
        mMuteButton.setOnClickListener(onClickListener);
        mLikeButton = (ImageView) findViewById(R.id.ivLike);
        mLikeButton.setOnClickListener(onClickListener);
        mDislikeButton = (ImageView) findViewById(R.id.ivDislike);
        mDislikeButton.setOnClickListener(onClickListener);
        mFavoriteButton = (ImageView) findViewById(R.id.ivFavorite);
        mFavoriteButton.setOnClickListener(onClickListener);

        if (MB3Application.getInstance().PlayerQueue != null
                && MB3Application.getInstance().PlayerQueue.PlaylistItems != null
                && MB3Application.getInstance().PlayerQueue.PlaylistItems.size() > 1) {
            mPreviousButton.setOnClickListener(onClickListener);
            mPreviousButton.setVisibility(View.VISIBLE);
            mNextButton.setOnClickListener(onClickListener);
            mNextButton.setVisibility(View.VISIBLE);
        }

        audioPlayer = new MediaPlayer();
        audioPlayer.setScreenOnWhilePlaying(true);
        audioPlayer.setOnPreparedListener(this);
        audioPlayer.setOnInfoListener(this);
        audioPlayer.setOnErrorListener(this);
        audioPlayer.setOnCompletionListener(this);

        Animation growAnimation = AnimationUtils.loadAnimation(this, R.anim.screensaver_image_grow);
        Animation fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        AnimationSet animationSet = new AnimationSet(false);
        animationSet.addAnimation(fadeInAnimation);
        animationSet.addAnimation(growAnimation);

        mBackdropSwitcher.setInAnimation(animationSet);
        mBackdropSwitcher.setOutAnimation(fadeOutAnimation);

        songAdapter = new BaseSongAdapter(MB3Application.getInstance().PlayerQueue.PlaylistItems, this);
        mPlaylist.setAdapter(songAdapter);
        mPlaylist.setOnItemClickListener(onPlaylistItemClick);

        buildAnimationSets();
    }

    @Override
    public void onResume() {
        super.onResume();

        MB3Application.getInstance().setCurrentActivity(this);

        if (isFresh) {

            if (playerQueueIsEmpty()) {
                this.finish();
                return;
            }

            MB3Application.getInstance().API.GetItemAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    itemDtoResponse
            );

            mDefaultImages = new ArrayList<>();
            mDefaultImages.add(R.drawable.default_music_backdrop);
            mDefaultImages.add(R.drawable.default_music_backdrop1);
            mDefaultImages.add(R.drawable.default_music_backdrop2);
            isFresh = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        clearReferences();
    }

    @Override
    public void onStop() {
        if (mSlideShow != null) {
            mSlideShow.release();
            mSlideShow = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {

        clearReferences();
        if (audioPlayer != null) {
            if (audioPlayer.isPlaying()) {
                audioPlayer.release();
            }
            audioPlayer = null;
        }

        if (mSlideShow != null) {
            mSlideShow.release();
            mSlideShow = null;
        }

        if (mPlayPauseButton != null) {
            mPlayPauseButton.removeCallbacks(hideControlsRunnable);
        }

        super.onDestroy();
    }

    //******************************************************************************************************************
    // Button events
    //******************************************************************************************************************

    @Override
    public void onBackPressed() {
        if (!mControlUiVisible && !mPlaylistVisible) {
            super.onBackPressed();
            return;
        }
        if (mControlUiVisible) {
            hideControlsOverlay();
        }
        if (mPlaylistVisible) {
            hidePlaylist();
        }
    }

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
                onNextButton();
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                onRewindButton();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                onPreviousButton();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (mPlaylistVisible) {
                    break;
                } else if (!mControlUiVisible) {
                    showControlsOverlay();
                    mPlayPauseButton.requestFocus();
                    return true;
                } else {
                    showControlsOverlay();
                }
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (mPlaylistVisible) {
                    break;
                } else if (!mControlUiVisible) {
                    showControlsOverlay();
                    mPlayPauseButton.requestFocus();
                    return true;
                } else {
                    showControlsOverlay();
                }
            case KeyEvent.KEYCODE_DPAD_UP:
                if (mPlaylistVisible) {
                    break;
                } else if (!mControlUiVisible) {
                    showControlsOverlay();
                    mPlayPauseButton.requestFocus();
                    return true;
                } else {
                    showControlsOverlay();
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (mPlaylistVisible) {
                    break;
                } else if (!mControlUiVisible) {
                    showControlsOverlay();
                    mPlayPauseButton.requestFocus();
                    return true;
                } else {
                    showControlsOverlay();
                }
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (mPlaylistVisible) {
                    break;
                } else if (!mControlUiVisible) {
                    showControlsOverlay();
                    mPlayPauseButton.requestFocus();
                    return true;
                } else {
                    showControlsOverlay();
                }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void onPlayPauseButton() {
        if (mIsPaused) {
            onPlayButton();
        } else {
            onPauseButton();
        }
    }

    private void onPlayButton() {
        try {
            if (audioPlayer == null) return;
            if (!audioPlayer.isPlaying()) {
                audioPlayer.start();
                mPlayPauseButton.setImageResource(R.drawable.vp_pause_selector);
                mIsPaused = false;
            }
        } catch(IllegalStateException ex) {
            FileLogger.getFileLogger().Debug("Error toggling audio player state");
        }
    }

    private void onPauseButton() {
        try {
            if (audioPlayer == null) return;
            if (audioPlayer.isPlaying()) {
                audioPlayer.pause();
                mPlayPauseButton.setImageResource(R.drawable.vp_play_selector);
                mIsPaused = true;
            } else {
                onPlayButton();
            }
        } catch (IllegalStateException ex) {
            FileLogger.getFileLogger().Debug("Error toggling audio player state");
        }
    }

    private void onFastForwardButton() {
        try {
            if (audioPlayer == null) return;
            int currentPosition = audioPlayer.getCurrentPosition();
            if (mStreamInfo.getRunTimeTicks() != null
                    && ((long)currentPosition * 10000) + 300000000 <= mStreamInfo.getRunTimeTicks()) {
                audioPlayer.seekTo(currentPosition + 30000);
            } else if (hasMoreItemsToPlay()) {
                onNextButton();
            }
        } catch (IllegalStateException ex) {
            FileLogger.getFileLogger().Debug("Error fast-forwarding audio player");
        }
    }

    private void onRewindButton() {
        try {
            if (audioPlayer == null) return;
            int currentPosition = audioPlayer.getCurrentPosition();
            audioPlayer.seekTo((currentPosition - 30000 > 0) ? currentPosition - 30000 : 0);
        } catch (IllegalStateException ex) {
            FileLogger.getFileLogger().Debug("Error rewinding audio player");
        }
    }

    private void onNextButton() {
        try {
            if (audioPlayer == null) return;
            if (!hasMoreItemsToPlay()) return;

            mAlbumImage.removeCallbacks(onEverySecond);
            audioPlayer.reset();
            mCurrentlyPlayingIndex++;

            MB3Application.getInstance().API.GetItemAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(mCurrentlyPlayingIndex).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    itemDtoResponse);

        } catch(IllegalStateException ex) {
            FileLogger.getFileLogger().Debug("Error jumping to next item");
        }
    }

    private void onPreviousButton() {
        try {
            if (audioPlayer == null) return;
            if (mCurrentlyPlayingIndex == 0) return;

            mAlbumImage.removeCallbacks(onEverySecond);
            audioPlayer.reset();
            mCurrentlyPlayingIndex--;

            MB3Application.getInstance().API.GetItemAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(mCurrentlyPlayingIndex).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    itemDtoResponse);

        } catch(IllegalStateException ex) {
            FileLogger.getFileLogger().Debug("Error jumping to previous item");
        }
    }

    private void onMuteUnmuteButton() {
        if (audioPlayer == null) return;

        mIsMuted = !mIsMuted;
        audioPlayer.setVolume(
                mIsMuted ? 0 : mCurrentVolume,
                mIsMuted ? 0 : mCurrentVolume
        );
        mMuteButton.setImageResource(mIsMuted ? R.drawable.vp_unmute_selector : R.drawable.vp_mute_selector);
    }

    private void onPlaylistButton() {
        hideControlsOverlay();
        showPlaylist();
    }

    private void onLikeButton() {
        if (mLikes == null || !mLikes) {
            MB3Application.getInstance().API.UpdateUserItemRatingAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(mCurrentlyPlayingIndex).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    true,
                    new UpdateUserDataResponse()
            );
        } else {
            MB3Application.getInstance().API.ClearUserItemRatingAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(mCurrentlyPlayingIndex).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    new UpdateUserDataResponse()
            );
        }
    }

    private void onDislikeButton() {
        if (mLikes == null || mLikes) {
            MB3Application.getInstance().API.UpdateUserItemRatingAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(mCurrentlyPlayingIndex).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    false,
                    new UpdateUserDataResponse()
            );
        } else {
            MB3Application.getInstance().API.ClearUserItemRatingAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(mCurrentlyPlayingIndex).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    new UpdateUserDataResponse()
            );
        }
    }

    private void onFavoriteButton() {
        MB3Application.getInstance().API.UpdateFavoriteStatusAsync(
                MB3Application.getInstance().PlayerQueue.PlaylistItems.get(mCurrentlyPlayingIndex).Id,
                MB3Application.getInstance().API.getCurrentUserId(),
                !mIsFavorite,
                new UpdateUserDataResponse()
        );
    }


    private class UpdateUserDataResponse extends Response<UserItemDataDto> {
        @Override
        public void onResponse(UserItemDataDto data) {
            setRatingIcons(data);
        }
    }

    private void setRatingIcons(UserItemDataDto userItemDataDto) {

        if (userItemDataDto == null) {
            Log.d(TAG, "userItemDataDto is null");
            return;
        }
        mIsFavorite = userItemDataDto.getIsFavorite();
        mFavoriteButton.setImageResource(mIsFavorite ? R.drawable.vp_favorite_toggled_selector : R.drawable.vp_favorite_selector);

        if (userItemDataDto.getLikes() == null) {
            mLikeButton.setImageResource(R.drawable.vp_like_selector);
            mDislikeButton.setImageResource(R.drawable.vp_dislike_selector);
            mLikes = null;
        } else if (userItemDataDto.getLikes()) {
            mLikeButton.setImageResource(R.drawable.vp_like_toggled_selector);
            mDislikeButton.setImageResource(R.drawable.vp_dislike_selector);
            mLikes = true;
        } else {
            mLikeButton.setImageResource(R.drawable.vp_like_selector);
            mDislikeButton.setImageResource(R.drawable.vp_dislike_toggled_selector);
            mLikes = false;
        }
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
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
                case R.id.ivNext:
                    onNextButton();
                    break;
                case R.id.ivPlaylistSelection:
                    onPlaylistButton();
                    break;
                case R.id.ivDislike:
                    onDislikeButton();
                    break;
                case R.id.ivLike:
                    onLikeButton();
                    break;
                case R.id.ivFavorite:
                    onFavoriteButton();
                    break;
                case R.id.ivAudioMute:
                    onMuteUnmuteButton();
                    break;
            }
        }
    };

    AdapterView.OnItemClickListener onPlaylistItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            FileLogger.getFileLogger().Debug(TAG + ": playlist item clicked.");
            if (MB3Application.getInstance().PlayerQueue.PlaylistItems == null
                    || MB3Application.getInstance().PlayerQueue.PlaylistItems.size() <= position ) {
                FileLogger.getFileLogger().Debug(TAG + ": invalid index.");
                return;
            }

            if (audioPlayer != null) {
                mAlbumImage.removeCallbacks(onEverySecond);
                audioPlayer.reset();
            }
            FileLogger.getFileLogger().Debug(TAG + ": audio player killed");
            mCurrentlyPlayingIndex = position;
            MB3Application.getInstance().API.GetItemAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(position).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    itemDtoResponse
            );
            FileLogger.getFileLogger().Debug(TAG + ": finished playlist item click");
        }
    };


    //******************************************************************************************************************
    // MediaPlayer events
    //******************************************************************************************************************

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

        audioPlayer.start();
        mIsPrepared = true;
        mAlbumImage.postDelayed(onEverySecond, 1000);
        PlayerHelpers.sendPlaybackStartedToServer(mStreamInfo, 0L, mCurrentVolume, mIsMuted, mIsPaused, new EmptyResponse());
        mLastProgressReport = SystemClock.elapsedRealtime();
        songAdapter.setCurrentPlayingIndex(mCurrentlyPlayingIndex);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

        mAlbumImage.removeCallbacks(onEverySecond);
        PlayerHelpers.sendPlaybackStoppedToServer(mStreamInfo, (long)(audioPlayer.getCurrentPosition()) * 10000, new EmptyResponse());
        audioPlayer.reset();
        mIsPrepared = false;



        if (hasMoreItemsToPlay()) {
            mCurrentlyPlayingIndex++;

            MB3Application.getInstance().API.GetItemAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(mCurrentlyPlayingIndex).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    itemDtoResponse);

        } else {
            this.finish();
        }
    }

    private boolean hasMoreItemsToPlay() {
        return MB3Application.getInstance().PlayerQueue != null
                && MB3Application.getInstance().PlayerQueue.PlaylistItems != null
                && MB3Application.getInstance().PlayerQueue.PlaylistItems.size() > mCurrentlyPlayingIndex + 1;
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }


    private boolean playerQueueIsEmpty() {
        return MB3Application.getInstance().PlayerQueue == null
                || MB3Application.getInstance().PlayerQueue.PlaylistItems == null
                || MB3Application.getInstance().PlayerQueue.PlaylistItems.size() == 0;
    }

    private Response<BaseItemDto> itemDtoResponse = new Response<BaseItemDto>() {
        @Override
        public void onResponse(BaseItemDto baseItemDto) {
            if (baseItemDto == null) return;

            mStreamInfo = PlayerHelpers.buildStreamInfoAudio(baseItemDto.getId(), baseItemDto.getMediaSources(), 0L, null);

            if (baseItemDto.getUserData() != null) {
                mLikes = baseItemDto.getUserData().getLikes();
                mIsFavorite = baseItemDto.getUserData().getIsFavorite();
                setRatingIcons(baseItemDto.getUserData());
            } else {
                mLikes = null;
                mIsFavorite = false;
            }

            if (mStreamInfo != null) {
                loadStreamInfoIntoPlayer();
                showSongDetails(baseItemDto);
                getAlbumCover(baseItemDto);
                getBackdrops(baseItemDto);

                mCurrentPositionText.setText("0:00");
                if (baseItemDto.getRunTimeTicks() != null) {
                    int runtime = (int) (baseItemDto.getRunTimeTicks() / 10000);
                    mRuntimeText.setText(Utils.PlaybackRuntimeFromMilliseconds(runtime));
                } else {
                    mRuntimeText.setText("--:--");
                }
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private void loadStreamInfoIntoPlayer() {
        loadUrlIntoPlayer(mStreamInfo.ToUrl(MB3Application.getInstance().API.getApiUrl()));
    }

    private void loadUrlIntoPlayer(String url) {
        try {
            audioPlayer.setDataSource(url);
            audioPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showSongDetails(BaseItemDto item) {
        if (item == null) return;

        mSongNameText.setText(!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getName()) ? item.getName() : "");
        mArtistNameText.setText(getFirstArtistOrDefault(item.getArtists()));
        mAlbumNameText.setText(!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getAlbum()) ? item.getAlbum() : "");
    }

    private String getFirstArtistOrDefault(ArrayList<String> artists) {
        return (artists != null && artists.size() > 0) ? artists.get(0) : "";
    }

    private void getAlbumCover(BaseItemDto item) {

        if (mCurrentlyPlayingIndex != 0) {
            albumFadeOut();
        }
        if (item.getHasPrimaryImage()) {
            setAlbumImage(item.getId());
        } else if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getAlbumPrimaryImageTag())
                && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getAlbumId())) {
            setAlbumImage(item.getAlbumId());
        } else {
            mAlbumImage.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
        }
    }

    private void setAlbumImage(String itemId) {
        ImageOptions options = new ImageOptions();
        options.setMaxHeight(150 * 2);
        options.setMaxWidth(150 * 2);
        options.setImageType(ImageType.Primary);

        String url = MB3Application.getInstance().API.GetImageUrl(itemId, options);
        mAlbumImage.setImageUrl(url, MB3Application.getInstance().API.getImageLoader());
    }

    private void getBackdrops(BaseItemDto item) {

        if (item.getBackdropCount() > 0) {
            setBackdrops(item);
        } else if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getParentBackdropItemId())) {
            MB3Application.getInstance().API.GetItemAsync(
                    item.getParentBackdropItemId(),
                    MB3Application.getInstance().API.getCurrentUserId(),
                    getBackdropItemResponse);
        } else {
            mSlideShow.setBackdropResourceIds(mDefaultImages);
        }
    }

    private Response<BaseItemDto> getBackdropItemResponse = new Response<BaseItemDto>() {
        @Override
        public void onResponse(BaseItemDto item) {
            if (item == null) return;
            setBackdrops(item);
        }
    };

    private void setBackdrops(BaseItemDto item) {

        List<String> backdropUrls = new ArrayList<>();

        for (int i = 0; i < item.getBackdropCount(); i++) {
            ImageOptions options = new ImageOptions();
            options.setMaxHeight(1280);
            options.setMaxWidth(720);
            options.setImageType(ImageType.Backdrop);
            options.setImageIndex(i);

            String url = MB3Application.getInstance().API.GetImageUrl(item.getId(), options);
            backdropUrls.add(url);
        }

        if (backdropUrls.size() == 1) {
            mSlideShow.setBackdropUrls(backdropUrls);
        } else {
            mSlideShow.setBackdropUrls(backdropUrls);
        }
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


    /**
     * Runnable that fires once a second to update the various UI controls
     */
    private Runnable onEverySecond = new Runnable() {

        public void run() {

            if (audioPlayer == null) return;

            // Report current position to the server only every 5 seconds.
            if (mLastProgressReport > 0 && SystemClock.elapsedRealtime() - mLastProgressReport > 5000) {
                PlayerHelpers.sendPlaybackProgressToServer(mStreamInfo, (long)(audioPlayer.getCurrentPosition()) * 10000, mCurrentVolume, mIsMuted, mIsPaused, new EmptyResponse());
                mLastProgressReport = SystemClock.elapsedRealtime();
            }

            // No point going on. If playback is paused then the values haven't changed.
            if (mIsPrepared) {
                mCurrentPositionText.setText(Utils.PlaybackRuntimeFromMilliseconds(audioPlayer.getCurrentPosition()));
            }

            mAlbumImage.postDelayed(onEverySecond, 1000);
        }
    };

    @Override
    public void onTakeScreenshotRequest() {

    }

    @Override
    public void onRemotePlayRequest(PlayRequest request, String mediaType) {
        FileLogger.getFileLogger().Info(TAG + ": remote play request received");
        if ("audio".equalsIgnoreCase(mediaType)) {
            FileLogger.getFileLogger().Info(TAG + ": first item is audio.");
            if (audioPlayer != null) {
                audioPlayer.reset();
            }
            MB3Application.getInstance().PlayerQueue = new Playlist();
            addItemsToPlaylist(request.getItemIds());
            FileLogger.getFileLogger().Info(TAG + ": audio player killed");
            MB3Application.getInstance().API.GetItemAsync(
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Id,
                    MB3Application.getInstance().API.getCurrentUserId(),
                    itemDtoResponse
            );
            FileLogger.getFileLogger().Info(TAG + ": finished audio play request");
        } else if ("video".equalsIgnoreCase(mediaType)) {
            FileLogger.getFileLogger().Info(TAG + ": first item is video");
            if (audioPlayer != null) {
                audioPlayer.reset();
            }
            MB3Application.getInstance().PlayerQueue = new Playlist();
            addItemsToPlaylist(request.getItemIds());
            FileLogger.getFileLogger().Info(TAG + ": audio player killed");
            Intent intent = new Intent(this, VideoPlayer.class);
            startActivity(intent);
            this.finish();
            FileLogger.getFileLogger().Info(TAG + ": finished video play request");
        } else {
            FileLogger.getFileLogger().Info(TAG + ": unable to process play request. Unsupported media type");
        }
    }

    @Override
    public void onSeekCommand(Long seekPositionTicks) {

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

    private void showControlsOverlay() {
        if (!mControlUiVisible) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.grow_vertical);
            mControls.startAnimation(animation);
            mControls.setVisibility(View.VISIBLE);
            mControlUiVisible = true;
        }
        mPlayPauseButton.removeCallbacks(hideControlsRunnable);
        mPlayPauseButton.postDelayed(hideControlsRunnable, 6000);
    }

    private void hideControlsOverlay() {
        if (!mControlUiVisible) return;
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.shrink_vertical);
        mControls.startAnimation(animation);
        mControls.setVisibility(View.GONE);
        mControlUiVisible = false;
    }

    private void showPlaylist() {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.grow_horizontal);
        mPlaylist.startAnimation(animation);
        mPlaylist.setVisibility(View.VISIBLE);
        mPlaylist.setSelection(mCurrentlyPlayingIndex);
        mPlaylist.requestFocus();
        mPlaylistVisible = true;
    }

    private void hidePlaylist() {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.shrink_horizontal);
        mPlaylist.startAnimation(animation);
        mPlaylist.setVisibility(View.GONE);
        mPlaylistVisible = false;
    }

    private Runnable hideControlsRunnable = new Runnable() {
        @Override
        public void run() {
            hideControlsOverlay();
        }
    };

    private void albumFadeOut() {

        mAlbumImage.startAnimation(asFadeout);
    }



    private void buildAnimationSets() {
//        Animation fadeAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        Animation shrinkAnimation = AnimationUtils.loadAnimation(this, R.anim.image_shrink);
        shrinkAnimation.setDuration(500);
        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        Animation growAnimation = AnimationUtils.loadAnimation(this, R.anim.image_grow3);
        growAnimation.setDuration(500);
        growAnimation.setStartOffset(500);

        asFadeout = new AnimationSet(false);
        asFadeout.addAnimation(rotateAnimation);
        asFadeout.addAnimation(shrinkAnimation);
//        asFadeout.addAnimation(growAnimation);
    }
}
