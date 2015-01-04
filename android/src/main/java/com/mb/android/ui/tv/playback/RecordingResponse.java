package com.mb.android.ui.tv.playback;

import com.mb.android.ui.tv.playback.VideoPlayer;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.livetv.RecordingInfoDto;

/**
 * Crutch used to stop leaking context using the ApiClient.
 */
public class RecordingResponse extends Response<RecordingInfoDto> {

    private VideoPlayer mVideoPlayer;

    public RecordingResponse(VideoPlayer videoPlayer) {
        mVideoPlayer = videoPlayer;
    }

    @Override
    public void onResponse(RecordingInfoDto item) {
        mVideoPlayer.onRecordingReceived(item);
        mVideoPlayer = null;
    }
    @Override
    public void onError(Exception ex) {
        mVideoPlayer = null;
    }
}
