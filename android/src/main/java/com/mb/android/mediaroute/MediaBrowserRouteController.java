package com.mb.android.mediaroute;

import android.content.Intent;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.mb.android.MB3Application;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.querying.SessionQuery;
import mediabrowser.model.session.PlayRequest;
import mediabrowser.model.session.PlaystateCommand;
import mediabrowser.model.session.PlaystateRequest;
import mediabrowser.model.session.SessionInfoDto;

import java.util.Set;

/**
 * Created by Mark on 2014-04-21.
 *
 * Class defines how a MediaBrowser route should handle control requests.
 */
public class MediaBrowserRouteController extends MediaRouteProvider.RouteController {

    private String mCurrentSessionId;

    @Override
    public boolean onControlRequest(Intent intent, MediaRouter.ControlRequestCallback callback) {

        Log.d("MediaBrowserRouteController", "onControlRequest");
        boolean success = false;
        String action = intent.getAction();
        Set<String> categories = intent.getCategories();

        if (categories.contains(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)) {

            mCurrentSessionId = intent.getStringExtra("SessionId");

            switch (action) {
                case MediaControlIntent.ACTION_PLAY:
                    success = handlePlayAction(intent);
                    break;
                case MediaControlIntent.ACTION_PAUSE:
                    success = handlePauseAction();
                    break;
                case MediaControlIntent.ACTION_SEEK:
                    Log.d("MediaBrowserRouteController", "seek");
                    success = handleSeekAction(intent);
                    break;
                case MediaControlIntent.ACTION_STOP:
                    success = handleStopAction();
                    break;
                case MediaControlIntent.ACTION_RESUME:
                    success = handleResumeAction();
                    break;
                case MediaControlIntent.ACTION_GET_SESSION_STATUS:
                    success = handleGetSessionStatusAction();
                    break;
                case MediaBrowserControlIntent.ACTION_NEXT_TRACK:
                    success = handleNextTrackAction();
                    break;
                case MediaBrowserControlIntent.ACTION_PREVIOUS_TRACK:
                    success = handlePreviousTrackAction();
                    break;
            }

            return success;
        } else if (categories.contains(MediaBrowserControlIntent.CATEGORY_MEDIA_BROWSER_COMMAND)) {

            mCurrentSessionId = intent.getStringExtra("SessionId");

            switch (action) {

                case MediaBrowserControlIntent.ACTION_MOVE_UP:
                    break;
                case MediaBrowserControlIntent.ACTION_MOVE_DOWN:
                    break;
                case MediaBrowserControlIntent.ACTION_MOVE_LEFT:
                    break;
                case MediaBrowserControlIntent.ACTION_MOVE_RIGHT:
                    break;
                case MediaBrowserControlIntent.ACTION_PAGE_UP:
                    break;
                case MediaBrowserControlIntent.ACTION_PAGE_DOWN:
                    break;
                case MediaBrowserControlIntent.ACTION_PREVIOUS_LETTER:
                    break;
                case MediaBrowserControlIntent.ACTION_NEXT_LETTER:
                    break;
                case MediaBrowserControlIntent.ACTION_TOGGLE_OSD:
                    break;
                case MediaBrowserControlIntent.ACTION_TOGGLE_CONTEXT_MENU:
                    break;
                case MediaBrowserControlIntent.ACTION_SELECT:
                    break;
                case MediaBrowserControlIntent.ACTION_BACK:
                    break;
                case MediaBrowserControlIntent.ACTION_TAKE_SCREENSHOT:
                    break;
                case MediaBrowserControlIntent.ACTION_SEND_KEY:
                    break;
                case MediaBrowserControlIntent.ACTION_SEND_STRING:
                    break;
                case MediaBrowserControlIntent.ACTION_GO_HOME:
                    break;
                case MediaBrowserControlIntent.ACTION_GO_TO_SETTINGS:
                    break;
                case MediaBrowserControlIntent.ACTION_VOLUME_UP:
                    break;
                case MediaBrowserControlIntent.ACTION_VOLUME_DOWN:
                    break;
                case MediaBrowserControlIntent.ACTION_MUTE:
                    break;
                case MediaBrowserControlIntent.ACTION_UNMUTE:
                    break;
                case MediaBrowserControlIntent.ACTION_TOGGLE_MUTE:
                    break;
                case MediaBrowserControlIntent.ACTION_SET_VOLUME:
                    break;
                case MediaBrowserControlIntent.ACTION_SET_AUDIO_STREAM_INDEX:
                    break;
                case MediaBrowserControlIntent.ACTION_SET_SUBTITLE_STREAM_INDEX:
                    break;
                case MediaBrowserControlIntent.ACTION_TOGGLE_FULLSCREEN:
                    break;
                case MediaBrowserControlIntent.ACTION_DISPLAY_CONTENT:
                    success = handleNavigateToAction(
                            intent.getStringExtra("ItemName"),
                            intent.getStringExtra("ItemId"),
                            intent.getStringExtra("ItemType"));
                    break;
                case MediaBrowserControlIntent.ACTION_GO_TO_SEARCH:
                    break;
                case MediaBrowserControlIntent.ACTION_DISPLAY_MESSAGE:
                    break;
            }
        }

        return success;
    }

    //**********************************************************************************************
    // Media Methods
    //**********************************************************************************************

    private boolean handlePlayAction(Intent intent) {

        String jsonData = intent.getStringExtra("PlayRequest");
        PlayRequest playRequest = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, PlayRequest.class);

        MB3Application.getInstance().API.SendPlayCommandAsync(mCurrentSessionId, playRequest, new EmptyResponse());
        handleGetSessionStatusAction();
        return true;
    }

    private boolean handlePauseAction() {

        PlaystateRequest playstateRequest = new PlaystateRequest();
        playstateRequest.setCommand(PlaystateCommand.Pause);

        MB3Application.getInstance().API.SendPlaystateCommandAsync(mCurrentSessionId, playstateRequest, new EmptyResponse());
        handleGetSessionStatusAction();
        return true;
    }

    private boolean handleSeekAction(Intent intent) {

        Long seekPosition = intent.getLongExtra("seekPosition", -1L);

        if (seekPosition == -1L) return false;

        Log.d("MediaBrowserRouteController", "valid seek value");
        PlaystateRequest request = new PlaystateRequest();
        request.setSeekPositionTicks(seekPosition);
        request.setCommand(PlaystateCommand.Seek);

        MB3Application.getInstance().API.SendPlaystateCommandAsync(mCurrentSessionId, request, new EmptyResponse());
        handleGetSessionStatusAction();
        return true;
    }

    private boolean handleStopAction() {

        PlaystateRequest request = new PlaystateRequest();
        request.setCommand(PlaystateCommand.Stop);

        MB3Application.getInstance().API.SendPlaystateCommandAsync(mCurrentSessionId, request, new EmptyResponse());
        handleGetSessionStatusAction();
        return true;
    }

    private boolean handleResumeAction() {

        PlaystateRequest playstateRequest = new PlaystateRequest();
        playstateRequest.setCommand(PlaystateCommand.Unpause);

        MB3Application.getInstance().API.SendPlaystateCommandAsync(mCurrentSessionId, playstateRequest, new EmptyResponse());
        handleGetSessionStatusAction();
        return true;
    }

    private boolean handleNextTrackAction() {

        return true;
    }

    private boolean handlePreviousTrackAction() {

        return true;
    }

    //**********************************************************************************************
    // Session Methods
    //**********************************************************************************************

    private boolean handleGetSessionStatusAction() {

        SessionQuery query = new SessionQuery();
        MB3Application.getInstance().API.GetClientSessionsAsync(query, new Response<SessionInfoDto[]>() {
            @Override
            public void onResponse(SessionInfoDto[] remoteSessions) {

                if (remoteSessions == null || remoteSessions.length == 0) {
                    Log.i("GetClientSessionsCallback", "sessions is null or empty");
                    return;
                }

                SessionInfoDto sessionInfo = null;

                for (SessionInfoDto session : remoteSessions) {
                    if (session.getId().equalsIgnoreCase(mCurrentSessionId)) {
                        sessionInfo = session;
                        break;
                    }
                }

                if (sessionInfo != null) {
                    // We have our session
                    MB3Application.getCastManager(MB3Application.getInstance()).setCurrentSessionInfo(sessionInfo);
                }

            }
        });

        return true;
    }


    //**********************************************************************************************
    // Navigation Methods
    //**********************************************************************************************

    private boolean handleNavigateToAction(String itemName, String itemId, String itemType) {
        MB3Application.getInstance().API.SendBrowseCommandAsync(mCurrentSessionId, itemId, itemName, itemType, new EmptyResponse());
        return true;
    }
}
