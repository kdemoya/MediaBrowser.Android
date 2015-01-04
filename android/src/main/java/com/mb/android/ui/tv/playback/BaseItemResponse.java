package com.mb.android.ui.tv.playback;

import com.mb.android.ui.tv.playback.VideoPlayer;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;

/**
 * Crutch used to stop leaking context using the ApiClient.
 */
public class BaseItemResponse extends Response<BaseItemDto> {

    private VideoPlayer mVideoPlayer;

    public BaseItemResponse(VideoPlayer videoPlayer) {
        mVideoPlayer = videoPlayer;
    }

    @Override
    public void onResponse(BaseItemDto item) {
        mVideoPlayer.onBaseItemReceived(item);
        mVideoPlayer = null;
    }
    @Override
    public void onError(Exception ex) {
        mVideoPlayer = null;
    }
}
