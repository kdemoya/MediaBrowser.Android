package com.mb.android;

import android.app.Instrumentation;
import android.view.KeyEvent;

import com.mb.android.logging.FileLogger;
import com.mb.android.ui.mobile.playback.AudioPlaybackActivity;
import com.mb.android.ui.mobile.playback.PlaybackActivity;
import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.apiclient.RemoteLogoutReason;
import mediabrowser.model.apiclient.SessionUpdatesEventArgs;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.session.BrowseRequest;
import mediabrowser.model.session.GeneralCommand;
import mediabrowser.model.session.MessageCommand;
import mediabrowser.model.session.PlayRequest;
import mediabrowser.model.session.PlaystateRequest;
import mediabrowser.model.session.SessionInfoDto;
import mediabrowser.model.session.UserDataChangeInfo;

/**
 * Created by Mark on 2014-11-18.
 *
 * ApiEventListener implementation that passes events up to the visible activity
 */
public class MbApiEventListener extends ApiEventListener {

    public void onRemoteLoggedOut(ApiClient client, RemoteLogoutReason reason){
        if (reason == RemoteLogoutReason.ParentalControlRestriction) {

        } else if (reason == RemoteLogoutReason.GeneralAccesError) {

        }
    }

    public void onUserUpdated(ApiClient client, UserDto userDto){
        if (userDto != null && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(MB3Application.getInstance().API.getCurrentUserId())) {
            if (MB3Application.getInstance().API.getCurrentUserId().equalsIgnoreCase(userDto.getId())) {
                MB3Application.getInstance().user = userDto;
                if (MB3Application.getInstance().getCurrentActivity() != null) {
                    MB3Application.getInstance().getCurrentActivity().onUserDataUpdated();
                }
            }
        }
    }

    public void onUserConfigurationUpdated(ApiClient client, UserDto userDto){
        if (userDto != null) {
            MB3Application.getInstance().user = userDto;
        }
    }

    public void onBrowseCommand(ApiClient client, BrowseRequest command){
        if (command == null || tangible.DotNetToJavaStringHelper.isNullOrEmpty(command.getItemId())) return;
        FileLogger.getFileLogger().Info("Processing browse command");

        client.GetItemAsync(command.getItemId(), MB3Application.getInstance().API.getCurrentUserId(), browseItemResponse);
    }

    public void onPlayCommand(ApiClient client, PlayRequest command){
        if (client == null || command == null
                || command.getItemIds() == null || command.getItemIds().length  < 1) {
            return;
        }
        FileLogger.getFileLogger().Info("ApiEventListener: onPlayCommand");

        MB3Application.getInstance().API.GetItemAsync(command.getItemIds()[0], client.getCurrentUserId(), new GetPlayItemTypeResponse(command));
    }

    public void onPlaystateCommand(ApiClient client, PlaystateRequest command)
    {
        if (command == null) return;

        switch (command.getCommand()){
            case Stop:
                sendKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP);
                break;
            case Pause:
                sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE);
                break;
            case Unpause:
                sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE);
                break;
            case NextTrack:
                sendKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
                break;
            case PreviousTrack:
                sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                break;
            case Seek:
                sendSeekCommand(command.getSeekPositionTicks());
                break;
            case Rewind:
                sendKeyEvent(KeyEvent.KEYCODE_MEDIA_REWIND);
                break;
            case FastForward:
                sendKeyEvent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
                break;
        }
    }

    private void sendSeekCommand(Long seekPositionTicks) {
        if (MB3Application.getInstance().getCurrentActivity() != null) {
            MB3Application.getInstance().getCurrentActivity().onSeekCommand(seekPositionTicks);
        }
    }

    public void onMessageCommand(ApiClient client, MessageCommand command)
    {

    }

    public void onGeneralCommand(ApiClient client, GeneralCommand command)
    {
        switch(command.getName()) {
            case "MoveUp":
                sendKeyEvent(KeyEvent.KEYCODE_DPAD_UP);
                break;
            case "MoveDown":
                sendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN);
                break;
            case "MoveLeft":
                sendKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT);
                break;
            case "MoveRight":
                sendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT);
                break;
            case "PageUp":
                sendKeyEvent(KeyEvent.KEYCODE_PAGE_UP);
                break;
            case "PageDown":
                sendKeyEvent(KeyEvent.KEYCODE_PAGE_DOWN);
                break;
            case "PreviousLetter":
                break;
            case "NextLetter":
                break;
            case "ToggleOsd":
                break;
            case "ToggleContextMenu":
                break;
            case "Select":
                sendKeyEvent(KeyEvent.KEYCODE_ENTER);
                break;
            case "Back":
                sendKeyEvent(KeyEvent.KEYCODE_BACK);
                break;
            case "TakeScreenshot":
                takeScreenShot();
                break;
            case "SendKey":
                break;
            case "SendString":
                break;
            case "GoHome":
                goHome();
                break;
            case "GoToSettings":
                goToSettings();
                break;
            case "VolumeUp":
                sendKeyEvent(KeyEvent.KEYCODE_VOLUME_UP);
                break;
            case "VolumeDown":
                sendKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN);
                break;
            case "Mute":
                break;
            case "Unmute":
                break;
            case "ToggleMute":
                sendKeyEvent(KeyEvent.KEYCODE_MUTE);
                break;
            case "SetVolume":
                break;
            case "SetAudioStreamIndex":
                break;
            case "SetSubtitleStreamIndex":
                break;
            case "ToggleFullscreen":
                break;
            case "DisplayContent":
                break;
            case "GoToSearch":
                break;
            case "DisplayMessage":
                break;
        }
    }

    public void onSendStringCommand(ApiClient client, String value)
    {

    }

    public void onSetVolumeCommand(ApiClient client, int value)
    {

    }

    public void onSetAudioStreamIndexCommand(ApiClient client, int value) {
        if (MB3Application.getInstance().getCurrentActivity() instanceof PlaybackActivity) {
            // Mobile video player is active
        } else if (MB3Application.getInstance().getCurrentActivity() instanceof AudioPlaybackActivity) {
            // Mobile audio player is acive
        }

    }

    public void onSetSubtitleStreamIndexCommand(ApiClient client, int value)
    {

    }

    public void onUserDataChanged(ApiClient client, UserDataChangeInfo info)
    {

    }

    public void onSessionsUpdated(ApiClient client, SessionUpdatesEventArgs args)
    {

    }

    public void onPlaybackStart(ApiClient client, SessionInfoDto info)
    {

    }

    public void onPlaybackStopped(ApiClient client, SessionInfoDto info)
    {

    }

    public void onSessionEnded(ApiClient client, SessionInfoDto info)
    {

    }


    private Response<BaseItemDto> browseItemResponse = new Response<BaseItemDto>() {

        @Override
        public void onResponse(BaseItemDto item) {
            if (item == null) {
                FileLogger.getFileLogger().Info("browse item response: no item returned");
                return;
            }

            if (MB3Application.getInstance().getCurrentActivity() != null) {
                MB3Application.getInstance().getCurrentActivity().onRemoteBrowseRequest(item);
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private void sendKeyEvent(final int keycode) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Instrumentation inst = new Instrumentation();
                inst.sendKeyDownUpSync(keycode);
            }
        }).start();
    }

    private void takeScreenShot() {
        if (MB3Application.getInstance().getCurrentActivity() != null) {
            MB3Application.getInstance().getCurrentActivity().onTakeScreenshotRequest();
        }
    }

    private void goHome() {
        if (MB3Application.getInstance().getCurrentActivity() != null) {
            MB3Application.getInstance().getCurrentActivity().onGoHomeRequest();
        }
    }

    private void goToSettings() {
        if (MB3Application.getInstance().getCurrentActivity() != null) {
            MB3Application.getInstance().getCurrentActivity().onGoToSettingsRequest();
        }
    }


    private class GetPlayItemTypeResponse extends Response<BaseItemDto> {

        private PlayRequest command;

        public GetPlayItemTypeResponse(PlayRequest command) {
            this.command = command;
        }

        @Override
        public void onResponse(BaseItemDto item) {
            if (item == null) return;
            if (MB3Application.getInstance().getCurrentActivity() != null) {
                MB3Application.getInstance().getCurrentActivity().onRemotePlayRequest(command, item.getMediaType());
            }
        }
    }
}
