package com.mb.android.interfaces;

import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.session.PlayRequest;
import mediabrowser.model.session.PlaystateRequest;

/**
 * Created by Mark on 2014-11-13.
 */
public interface IWebsocketEventListener {

    void onTakeScreenshotRequest();
    void onRemotePlayRequest(PlayRequest command, String type);
    void onSeekCommand(Long seekPositionTicks);
    void onRemoteBrowseRequest(BaseItemDto baseItemDto);
    void onUserDataUpdated();
    void onGoHomeRequest();
    void onGoToSettingsRequest();

}
