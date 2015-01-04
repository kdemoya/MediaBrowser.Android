package com.mb.android.ui.tv.playback;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.livetv.ChannelInfoDto;

/**
 * Crutch used to stop leaking context using the ApiClient.
 */
public class TvChannelResponse extends Response<ChannelInfoDto> {

    private VideoPlayer mVideoPlayer;

    public TvChannelResponse(VideoPlayer videoPlayer) {
        mVideoPlayer = videoPlayer;
    }

    @Override
    public void onResponse(ChannelInfoDto item) {
        mVideoPlayer.onTvChannelReceived(item);
        mVideoPlayer = null;
    }
    @Override
    public void onError(Exception ex) {
        mVideoPlayer = null;
    }
}
