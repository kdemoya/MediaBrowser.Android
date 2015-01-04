package com.mb.android.mediaroute;

import android.content.Context;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.provider.Settings;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteDescriptor;
import android.support.v7.media.MediaRouteDiscoveryRequest;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouteProviderDescriptor;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.mb.android.MB3Application;
import mediabrowser.apiinteraction.Response;
import com.mb.android.logging.FileLogger;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.querying.SessionQuery;
import mediabrowser.model.session.SessionInfoDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mark on 2014-04-21.
 *
 * This class will be used to define a Media Route representing each available client connected to
 * the Media Browser Server.
 */
public final class MediaBrowserRouteProvider extends MediaRouteProvider {

    public static final String CATEGORY_MEDIA_BROWSER_ROUTE = "com.mb.android.CATEGORY_MEDIA_BROWSER_ROUTE";

    public MediaBrowserRouteProvider(Context context) {
        super(context);

        getRoutes();
    }

    @Override
    public RouteController onCreateRouteController(String routeId) {
        return new MediaBrowserRouteController();
    }

    @Override
    public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest request) {
        if (request == null) return;
        getRoutes();
    }

    /**
     * Request the available Sessions from the Media Browser Server
     */
    private void getRoutes() {

        if (null == MB3Application.getInstance().API || tangible.DotNetToJavaStringHelper.isNullOrEmpty(MB3Application.getInstance().API.getServerAddress())) {
            return;
        }
        SessionQuery query = new SessionQuery();
        MB3Application.getInstance().API.GetClientSessionsAsync(query, new Response<SessionInfoDto[]>() {
            @Override
            public void onResponse(SessionInfoDto[] remoteSessions) {

                if (remoteSessions == null) {
                    Log.i("GetClientSessionsCallback", "sessions is null");
                    return;
                }

                if (remoteSessions.length == 0) {
                    Log.i("GetClientSessionsCallback", "sessions is empty");
                    return;
                }

                List<SessionInfoDto> validSessions = new ArrayList<>();

                String deviceId =
                        Settings.Secure.getString(getContext().getContentResolver(),
                                Settings.Secure.ANDROID_ID);

                for (SessionInfoDto session : remoteSessions) {
                    // Don't show this device, or MediaBrowser Chromecast sessions in the list
                    if (session.getDeviceId().equalsIgnoreCase(deviceId) || "Chromecast".equalsIgnoreCase(session.getClient()))
                        continue;

                    if (MB3Application.getInstance().API.getCurrentUserId().equals(session.getUserId()) || userCanControlOtherSessions()) {
                        validSessions.add(session);
                    }
                }

                if (validSessions.isEmpty()) {
                    return;
                }

                FileLogger.getFileLogger().Info(String.valueOf(validSessions.size()) + " sessions available");
                publishRoutes(validSessions);
            }
        });
    }

    private boolean userCanControlOtherSessions() {
        return MB3Application.getInstance().user != null
                && MB3Application.getInstance().user.getConfiguration() != null
                && MB3Application.getInstance().user.getConfiguration().getEnableRemoteControlOfOtherUsers();
    }

    /**
     * Take the known sessions and create a MediaRouteDescriptor for each. Then publish them to the
     * framework.
     *
     * @param validSessions The List of known sessions
     */
    private void publishRoutes(List<SessionInfoDto> validSessions) {

        FileLogger.getFileLogger().Info("MediaBrowserRouteProvider: Build RouteDescriptors");
        List<MediaRouteDescriptor> routes = new ArrayList<>();

        for (SessionInfoDto session : validSessions) {

            // Create the route descriptor using previously created IntentFilters
            MediaRouteDescriptor routeDescriptor = new MediaRouteDescriptor.Builder(
                    session.getId(),
                    session.getDeviceName())
                    .setDescription(session.getClient())
                    .addControlFilters(getControlFiltersFromSessionInfo(session))
                    .setPlaybackStream(AudioManager.STREAM_MUSIC)
                    .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                    .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                    .setVolumeMax(0)
                    .setVolume(0)
                    .build();

            if (routeDescriptor != null) {
                FileLogger.getFileLogger().Info("MediaBrowserRouteProvider: Adding RouteDescriptor");
                routes.add(routeDescriptor);
            } else {
                FileLogger.getFileLogger().Info("MediaBrowserRouteProvider: Error building RouteDescriptor");
            }
        }

        FileLogger.getFileLogger().Info("MediaBrowserRouteProvider: " + String.valueOf(routes.size()) + " routes added");

        // Add the route descriptor to the provider descriptor
        MediaRouteProviderDescriptor providerDescriptor =
                new MediaRouteProviderDescriptor.Builder()
                .addRoutes(routes)
                .build();

        if (providerDescriptor == null) {
            FileLogger.getFileLogger().Info("MediaBrowserRouteProvider: Error building ProviderDescriptor");
        }

        // Publish the descriptor to the framework
        setDescriptor(providerDescriptor);

        FileLogger.getFileLogger().Info("MediaBrowserRouteProvider: ProviderDescriptor published");
    }

    /**
     * The route descriptor should only contain control intents that the client is able to
     * handle. This will allow RouteInfo.supportsControlCategory() to weed out unsupported requests
     *
     * The basic playback controls are added automatically and then additional remote control
     * commands are added based on the values reported in SessionInfoDto.SupportedCommands.
     *
     * @param session The SessionInfoDto to extract supported control intents from
     * @return An ArrayList containing all the commands this session supports.
     */
    private ArrayList<IntentFilter> getControlFiltersFromSessionInfo(SessionInfoDto session) {

        IntentFilter playControls = new IntentFilter();
        playControls.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        playControls.addCategory(CATEGORY_MEDIA_BROWSER_ROUTE);
        playControls.addAction(MediaControlIntent.ACTION_PLAY);
        playControls.addAction(MediaControlIntent.ACTION_SEEK);
        playControls.addAction(MediaControlIntent.ACTION_PAUSE);
        playControls.addAction(MediaControlIntent.ACTION_RESUME);
        playControls.addAction(MediaControlIntent.ACTION_STOP);
        playControls.addAction(MediaBrowserControlIntent.ACTION_NEXT_TRACK);
        playControls.addAction(MediaBrowserControlIntent.ACTION_PREVIOUS_TRACK);

        ArrayList<IntentFilter> controlFilters = new ArrayList<>();
        controlFilters.add(playControls);

        if (null != session) {

            if (null != session.getSupportedCommands() && !session.getSupportedCommands().isEmpty()) {

                IntentFilter supportedCommands = new IntentFilter();
                supportedCommands.addCategory(MediaBrowserControlIntent.CATEGORY_MEDIA_BROWSER_COMMAND);

                for (String command : session.getSupportedCommands()) {

                    switch (command) {

                        case "MoveUp":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_MOVE_UP);
                            break;
                        case "MoveDown":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_MOVE_DOWN);
                            break;
                        case "MoveLeft":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_MOVE_LEFT);
                            break;
                        case "MoveRight":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_MOVE_RIGHT);
                            break;
                        case "PageUp":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_PAGE_UP);
                            break;
                        case "PageDown":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_PAGE_DOWN);
                            break;
                        case "PreviousLetter":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_PREVIOUS_LETTER);
                            break;
                        case "NextLetter":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_NEXT_LETTER);
                            break;
                        case "ToggleOsd":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_TOGGLE_OSD);
                            break;
                        case "ToggleContextMenu":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_TOGGLE_CONTEXT_MENU);
                            break;
                        case "Select":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_SELECT);
                            break;
                        case "Back":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_BACK);
                            break;
                        case "TakeScreenshot":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_TAKE_SCREENSHOT);
                            break;
                        case "SendKey":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_SEND_KEY);
                            break;
                        case "SendString":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_SEND_STRING);
                            break;
                        case "GoHome":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_GO_HOME);
                            break;
                        case "GoToSettings":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_GO_TO_SETTINGS);
                            break;
                        case "VolumeUp":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_VOLUME_UP);
                            break;
                        case "VolumeDown":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_VOLUME_DOWN);
                            break;
                        case "Mute":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_MUTE);
                            break;
                        case "Unmute":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_UNMUTE);
                            break;
                        case "ToggleMute":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_TOGGLE_MUTE);
                            break;
                        case "SetVolume":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_SET_VOLUME);
                            break;
                        case "SetAudioStreamIndex":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_SET_AUDIO_STREAM_INDEX);
                            break;
                        case "SetSubtitleStreamIndex":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_SET_SUBTITLE_STREAM_INDEX);
                            break;
                        case "ToggleFullscreen":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_TOGGLE_FULLSCREEN);
                            break;
                        case "DisplayContent":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_DISPLAY_CONTENT);
                            break;
                        case "GoToSearch":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_GO_TO_SEARCH);
                            break;
                        case "DisplayMessage":
                            supportedCommands.addAction(MediaBrowserControlIntent.ACTION_DISPLAY_MESSAGE);
                            break;
                    }
                }

                controlFilters.add(supportedCommands);
            }

            if (session.getPlayableMediaTypes() != null && session.getPlayableMediaTypes().size() > 0) {
                IntentFilter supportedTypes = new IntentFilter();
                supportedTypes.addCategory(MediaBrowserControlIntent.CATEGORY_SUPPORTED_TYPES);

                for (String playableMediaType : session.getPlayableMediaTypes()) {

                    if ("audio".equalsIgnoreCase(playableMediaType)) {
                        supportedTypes.addAction(MediaBrowserControlIntent.EXTRA_SUPPORTS_AUDIO);
                    } else if ("video".equalsIgnoreCase(playableMediaType)) {
                        supportedTypes.addAction(MediaBrowserControlIntent.EXTRA_SUPPORTS_VIDEO);
                    } else if ("game".equalsIgnoreCase(playableMediaType)) {
                        supportedTypes.addAction(MediaBrowserControlIntent.EXTRA_SUPPORTS_GAMES);
                    } else if ("photo".equalsIgnoreCase(playableMediaType)) {
                        supportedTypes.addAction(MediaBrowserControlIntent.EXTRA_SUPPORTS_PHOTOS);
                    } else if ("book".equalsIgnoreCase(playableMediaType)) {
                        supportedTypes.addAction(MediaBrowserControlIntent.EXTRA_SUPPORTS_BOOKS);
                    }
                }

                controlFilters.add(supportedTypes);
            }

            if (session.getQueueableMediaTypes() != null && session.getQueueableMediaTypes().size() > 0) {
                IntentFilter supportedQueueTypes = new IntentFilter();
                supportedQueueTypes.addCategory(MediaBrowserControlIntent.CATEGORY_SUPPORTED_QUEUE_TYPES);

                for (String queueableMediaType : session.getQueueableMediaTypes()) {

                    if ("audio".equalsIgnoreCase(queueableMediaType)) {
                        supportedQueueTypes.addAction(MediaBrowserControlIntent.EXTRA_SUPPORTS_QUEUED_AUDIO);
                    } else if ("video".equalsIgnoreCase(queueableMediaType)) {
                        supportedQueueTypes.addAction(MediaBrowserControlIntent.EXTRA_SUPPORTS_QUEUED_VIDEO);
                    } else if ("game".equalsIgnoreCase(queueableMediaType)) {
                        supportedQueueTypes.addAction(MediaBrowserControlIntent.EXTRA_SUPPORTS_QUEUED_GAMES);
                    } else if ("photo".equalsIgnoreCase(queueableMediaType)) {
                        supportedQueueTypes.addAction(MediaBrowserControlIntent.EXTRA_SUPPORTS_QUEUED_PHOTOS);
                    } else if ("book".equalsIgnoreCase(queueableMediaType)) {
                        supportedQueueTypes.addAction(MediaBrowserControlIntent.EXTRA_SUPPORTS_QUEUED_BOOKS);
                    }
                }

                controlFilters.add(supportedQueueTypes);
            }
        }




        return controlFilters;
    }
}