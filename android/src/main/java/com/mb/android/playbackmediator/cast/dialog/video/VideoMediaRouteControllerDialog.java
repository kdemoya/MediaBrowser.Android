/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mb.android.playbackmediator.cast.dialog.video;

import static com.mb.android.playbackmediator.utils.LogUtils.LOGD;
import static com.mb.android.playbackmediator.utils.LogUtils.LOGE;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.mediaroute.MediaBrowserControlIntent;
import com.mb.android.playbackmediator.cast.VideoCastManager;
import com.mb.android.playbackmediator.cast.callbacks.VideoCastConsumerImpl;
import com.mb.android.playbackmediator.cast.exceptions.CastException;
import com.mb.android.playbackmediator.cast.exceptions.NoConnectionException;
import com.mb.android.playbackmediator.cast.exceptions.TransientNetworkDisconnectionException;
import com.mb.android.playbackmediator.utils.LogUtils;
import com.mb.android.playbackmediator.utils.Utils;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.session.SessionInfoDto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.MediaRouteControllerDialog;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.URL;

/**
 * A custom {@link MediaRouteControllerDialog} that provides an album art, a play/pause button and
 * the ability to take user to the target activity when the album art is tapped.
 */
public class VideoMediaRouteControllerDialog extends MediaRouteControllerDialog {

    private static final String TAG =
            LogUtils.makeLogTag(VideoMediaRouteControllerDialog.class);

    private ImageView mIcon;
    private ImageView mPausePlay;
    private TextView mTitle;
    private TextView mSubTitle;
    private TextView mEmptyText;
    private ProgressBar mLoading;
    private CheckBox mContentMirroring;
    private Uri mIconUri;
    private VideoCastManager mCastManager;
    protected int mState;
    private VideoCastConsumerImpl castConsumerImpl;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private Drawable mStopDrawable;
    private Context mContext;
    private boolean mClosed;
    private View mIconContainer;
    private View mTextContainer;

    private int mStreamType;

    public VideoMediaRouteControllerDialog(Context context, int theme) {
        super(context, theme);
    }

    @Override
    protected void onStop() {
        if (null != mCastManager) {
            mCastManager.removeVideoCastConsumer(castConsumerImpl);
        }
        super.onStop();
    }

    /**
     * Creates a new VideoMediaRouteControllerDialog in the given context.
     */
    public VideoMediaRouteControllerDialog(Context context) {
        super(context, R.style.CastDialog);
        try {
            this.mContext = context;
            mCastManager = VideoCastManager.getInstance();
            mState = mCastManager.getPlaybackStatus();
            castConsumerImpl = new VideoCastConsumerImpl() {

                @Override
                public void onRemoteMediaPlayerStatusUpdated() {
                    mState = mCastManager.getPlaybackStatus();

                    if (mState == MediaStatus.PLAYER_STATE_PAUSED) {
                        LOGD(TAG, "Player state is paused");
                    } else if (mState == MediaStatus.PLAYER_STATE_PLAYING) {
                        LOGD(TAG, "Player state is playing");
                    } else {
                        LOGD(TAG, "Player state is not what expected");
                    }

                    updatePlayPauseState(mState);
                }

                /*
                 * (non-Javadoc)
                 * @see com.google.sample.castcompanionlibrary.cast.VideoCastConsumerImpl
                 * #onMediaChannelMetadataUpdated()
                 */
                @Override
                public void onRemoteMediaPlayerMetadataUpdated() {
                    updateMetadata();
                }

            };
            mCastManager.addVideoCastConsumer(castConsumerImpl);
            mPauseDrawable = context.getResources().getDrawable(R.drawable.ic_av_pause_sm_dark);
            mPlayDrawable = context.getResources().getDrawable(R.drawable.ic_av_play_sm_dark);
            mStopDrawable = context.getResources().getDrawable(R.drawable.ic_av_stop_sm_dark);
        } catch (CastException e) {
            LOGE(TAG, "Failed to update the content of dialog", e);
        } catch (IllegalStateException e) {
            LOGE(TAG, "Failed to update the content of dialog", e);
        }
    }

    /*
     * Hides/show the icon and metadata and play/pause if there is no media
     */
    private void hideControls(boolean hide, int resId) {
        int visibility = hide ? View.GONE : View.VISIBLE;
        mIcon.setVisibility(visibility);
        mIconContainer.setVisibility(visibility);
        mTextContainer.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
        mEmptyText.setText(resId == 0 ? R.string.no_media_info : resId);
        mEmptyText.setVisibility(hide ? View.VISIBLE : View.GONE);
        if (hide) mPausePlay.setVisibility(visibility);
    }

    private void updateMetadata() {
        SessionInfoDto sessionInfo = mCastManager.getCurrentSessionInfo();

        if (null == sessionInfo || null == sessionInfo.getNowPlayingItem()) {
            hideControls(true, R.string.no_media_info);
            return;
        }
        mStreamType = MediaInfo.STREAM_TYPE_BUFFERED;
        hideControls(false, 0);
        mTitle.setText(sessionInfo.getNowPlayingItem().getName());
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(sessionInfo.getNowPlayingItem().getSeriesName())) {
            mSubTitle.setText(sessionInfo.getNowPlayingItem().getSeriesName());
        }
        if (sessionInfo.getNowPlayingItem().getHasPrimaryImage()) {
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setMaxWidth(400);
            String imageUrl = MB3Application.getInstance().API.GetImageUrl(sessionInfo.getNowPlayingItem().getId(), options);
            setIcon(Uri.parse(imageUrl));
        } else {
            setIcon(null);
        }
    }

    public void setIcon(Uri uri) {
        if (null != mIconUri && mIconUri.equals(uri)) {
            return;
        }
        mIconUri = uri;
        if (null == uri) {
            Bitmap bm = BitmapFactory.decodeResource(
                    mContext.getResources(), R.drawable.video_placeholder_200x200);
            mIcon.setImageBitmap(bm);
            return;
        }

        new Thread(new Runnable() {
            Bitmap bm = null;

            @Override
            public void run() {
                try {
                    URL imgUrl = new URL(mIconUri.toString());
                    bm = BitmapFactory.decodeStream(imgUrl.openStream());
                } catch (Exception e) {
                    LOGE(TAG, "setIcon(): Failed to load the image with url: " +
                            mIconUri + ", using the default one", e);
                    bm = BitmapFactory.decodeResource(
                            mContext.getResources(), R.drawable.video_placeholder_200x200);
                }
                if (mClosed) {
                    return;
                }
                mIcon.post(new Runnable() {

                    @Override
                    public void run() {
                        mIcon.setImageBitmap(bm);
                    }
                });

            }
        }).start();
    }

    private void updatePlayPauseState(int state) {
        if (null != mPausePlay) {
            switch (state) {
                case MediaStatus.PLAYER_STATE_PLAYING:
                    mPausePlay.setImageDrawable(getPauseStopButton());
                    adjustControlsVisibility(true);
                    break;
                case MediaStatus.PLAYER_STATE_PAUSED:
                    mPausePlay.setImageDrawable(mPlayDrawable);
                    adjustControlsVisibility(true);
                    break;
                case MediaStatus.PLAYER_STATE_IDLE:
                    mPausePlay.setVisibility(View.INVISIBLE);
                    setLoadingVisibility(false);

                    if (mState == MediaStatus.PLAYER_STATE_IDLE
                            && mCastManager.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED) {
                        hideControls(true, R.string.no_media_info);
                    } else {
                        switch (mStreamType) {
                            case MediaInfo.STREAM_TYPE_BUFFERED:
                                mPausePlay.setVisibility(View.INVISIBLE);
                                setLoadingVisibility(false);
                                break;
                            case MediaInfo.STREAM_TYPE_LIVE:
                                int idleReason = mCastManager.getIdleReason();
                                if (idleReason == MediaStatus.IDLE_REASON_CANCELED) {
                                    mPausePlay.setImageDrawable(mPlayDrawable);
                                    adjustControlsVisibility(true);
                                } else {
                                    mPausePlay.setVisibility(View.INVISIBLE);
                                    setLoadingVisibility(false);
                                }
                                break;
                        }
                    }
                    break;
                case MediaStatus.PLAYER_STATE_BUFFERING:
                    adjustControlsVisibility(false);
                    break;
                default:
                    mPausePlay.setVisibility(View.INVISIBLE);
                    setLoadingVisibility(false);
            }
        }
    }

    private Drawable getPauseStopButton() {
        switch (mStreamType) {
            case MediaInfo.STREAM_TYPE_BUFFERED:
                return mPauseDrawable;
            case MediaInfo.STREAM_TYPE_LIVE:
                return mStopDrawable;
            default:
                return mPauseDrawable;
        }
    }

    private void setLoadingVisibility(boolean show) {
        mLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void adjustControlsVisibility(boolean showPlayPlause) {
        int visible = showPlayPlause ? View.VISIBLE : View.INVISIBLE;
        mPausePlay.setVisibility(visible);
        setLoadingVisibility(!showPlayPlause);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (null != castConsumerImpl) {
            mCastManager.removeVideoCastConsumer(castConsumerImpl);
        }
        mClosed = true;
    }

    /**
     * Initializes this dialog's set of playback buttons and adds click listeners.
     */
    @Override
    public View onCreateMediaControlView(Bundle savedInstanceState) {
        LayoutInflater inflater = getLayoutInflater();
        View controls = inflater.inflate(R.layout.custom_media_route_controller_controls_dialog,
                null);

        loadViews(controls);
        mState = mCastManager.getPlaybackStatus();
        updateMetadata();
        updatePlayPauseState(mState);
        updateContentMirrorEnabled();
        setupCallbacks();
        return controls;
    }

    private void updateContentMirrorEnabled() {

        MediaRouter.RouteInfo info = mCastManager.getRouteInfo();

        boolean enable = info != null && (
                info.supportsControlCategory(CastMediaControlIntent.categoryForCast(
                        MB3Application.getApplicationId()))
                || info.supportsControlAction(
                        MediaBrowserControlIntent.CATEGORY_MEDIA_BROWSER_COMMAND,
                        MediaBrowserControlIntent.ACTION_DISPLAY_CONTENT));

        if (enable) {
            // only show the option if the route supports it
            mContentMirroring.setVisibility(View.VISIBLE);
            boolean mirrorEnabled
                    = Utils.getBooleanFromPreference(MB3Application.getInstance(), "CONTENT_MIRROR_ENABLED", false);
            Log.d(TAG, "MIRROR ENABLED CURRENTLY SET TO " + String.valueOf(mirrorEnabled));

            if (null != mContentMirroring) {
                mContentMirroring.setChecked(mirrorEnabled);
            }
        } else {
            // If the route doesn't support it, explicitly set it to false just in case
            Utils.saveBooleanToPreference(MB3Application.getInstance(), "CONTENT_MIRROR_ENABLED", false);
        }
    }

    private void setupCallbacks() {

        mPausePlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (null == mCastManager) {
                    return;
                }
                try {
                    adjustControlsVisibility(false);
                    mCastManager.togglePlayback();
                } catch (CastException e) {
                    adjustControlsVisibility(true);
                    LOGE(TAG, "Failed to toggle playback", e);
                } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                    adjustControlsVisibility(true);
                    LOGE(TAG, "Failed to toggle playback due to network issues", e);
                }
            }
        });

        mIcon.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showTargetActivity();
            }

        });

        mTextContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showTargetActivity();
            }

        });

        mContentMirroring.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                Log.d(TAG, "Mirror content being set to " + String.valueOf(isChecked ? true : null));
                Utils.saveBooleanToPreference(MB3Application.getInstance(), "CONTENT_MIRROR_ENABLED", isChecked ? true : null);
            }
        });
    }

    private void showTargetActivity() {
        if (null != mCastManager
                && null != mCastManager.getTargetActivity()) {
            try {
                mCastManager.onTargetActivityInvoked(mContext);
            } catch (TransientNetworkDisconnectionException e) {
                LOGE(TAG, "Failed to start the target activity due to network issues", e);
            } catch (NoConnectionException e) {
                LOGE(TAG, "Failed to start the target activity due to network issues", e);
            }
            cancel();
        }
    }

    private void loadViews(View controls) {
        mIcon = (ImageView) controls.findViewById(R.id.iconView);
        mIconContainer = controls.findViewById(R.id.iconContainer);
        mTextContainer = controls.findViewById(R.id.textContainer);
        mPausePlay = (ImageView) controls.findViewById(R.id.playPauseView);
        mTitle = (TextView) controls.findViewById(R.id.titleView);
        mSubTitle = (TextView) controls.findViewById(R.id.subTitleView);
        mLoading = (ProgressBar) controls.findViewById(R.id.loadingView);
        mEmptyText = (TextView) controls.findViewById(R.id.emptyView);
        mContentMirroring = (CheckBox) controls.findViewById(R.id.contentMirroring);
    }
}
