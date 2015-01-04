package com.mb.android.mediaroute;

/**
 * Created by Mark on 2014-09-01.
 *
 * Class containing the known commands for the Media Browser remote control API
 */
public final class MediaBrowserControlIntent {

    public static final String CATEGORY_MEDIA_BROWSER_COMMAND = "com.mb.android.CATEGORY_MEDIA_BROWSER_COMMAND";

    public static final String ACTION_MOVE_UP = "com.mb.android.ACTION_MOVE_UP";
    public static final String ACTION_MOVE_DOWN = "com.mb.android.ACTION_MOVE_DOWN";
    public static final String ACTION_MOVE_LEFT = "com.mb.android.ACTION_MOVE_LEFT";
    public static final String ACTION_MOVE_RIGHT = "com.mb.android.ACTION_MOVE_RIGHT";
    public static final String ACTION_PAGE_UP = "com.mb.android.ACTION_PAGE_UP";
    public static final String ACTION_PAGE_DOWN = "com.mb.android.ACTION_PAGE_DOWN";
    public static final String ACTION_PREVIOUS_LETTER = "com.mb.android.ACTION_PREVIOUS_LETTER";
    public static final String ACTION_NEXT_LETTER = "com.mb.android.ACTION_NEXT_LETTER";
    public static final String ACTION_TOGGLE_OSD = "com.mb.android.ACTION_TOGGLE_OSD";
    public static final String ACTION_TOGGLE_CONTEXT_MENU = "com.mb.android.ACTION_TOGGLE_CONTEXT_MENU";
    public static final String ACTION_SELECT = "com.mb.android.ACTION_SELECT";
    public static final String ACTION_BACK = "com.mb.android.ACTION_BACK";
    public static final String ACTION_TAKE_SCREENSHOT = "com.mb.android.ACTION_TAKE_SCREENSHOT";
    public static final String ACTION_SEND_KEY = "com.mb.android.ACTION_SEND_KEY";
    public static final String ACTION_SEND_STRING = "com.mb.android.ACTION_SEND_STRING";
    public static final String ACTION_GO_HOME = "com.mb.android.ACTION_GO_HOME";
    public static final String ACTION_GO_TO_SETTINGS = "com.mb.android.ACTION_GO_TO_SETTINGS";
    public static final String ACTION_VOLUME_UP = "com.mb.android.ACTION_VOLUME_UP";
    public static final String ACTION_VOLUME_DOWN = "com.mb.android.ACTION_VOLUME_DOWN";
    public static final String ACTION_MUTE = "com.mb.android.ACTION_MUTE";
    public static final String ACTION_UNMUTE = "com.mb.android.ACTION_UNMUTE";
    public static final String ACTION_TOGGLE_MUTE = "com.mb.android.ACTION_TOGGLE_MUTE";
    public static final String ACTION_SET_VOLUME = "com.mb.android.ACTION_SET_VOLUME";
    public static final String ACTION_SET_AUDIO_STREAM_INDEX = "com.mb.android.ACTION_SET_AUDIO_STREAM_INDEX";
    public static final String ACTION_SET_SUBTITLE_STREAM_INDEX = "com.mb.android.ACTION_SET_SUBTITLE_INDEX";
    public static final String ACTION_TOGGLE_FULLSCREEN = "com.mb.android.ACTION_TOGGLE_FULLSCREEN";
    public static final String ACTION_DISPLAY_CONTENT = "com.mb.android.ACTION_DISPLAY_CONTENT";
    public static final String ACTION_GO_TO_SEARCH = "com.mb.android.ACTION_GO_TO_SEARCH";
    public static final String ACTION_DISPLAY_MESSAGE = "com.mb.android.ACTION_DISPLAY_MESSAGE";

    public static final String ACTION_NEXT_TRACK = "com.mb.android.ACTION_NEXT_TRACK";
    public static final String ACTION_PREVIOUS_TRACK = "com.mb.android.ACTION_PREVIOUS_TRACK";

    public static final String CATEGORY_SUPPORTED_TYPES = "com.mb.android.CATEGORY_SUPPORTED_TYPES";
    public static final String EXTRA_SUPPORTS_AUDIO = "com.mb.android.EXTRA_SUPPORTS_AUDIO";
    public static final String EXTRA_SUPPORTS_BOOKS = "com.mb.android.EXTRA_SUPPORTS_BOOKS";
    public static final String EXTRA_SUPPORTS_GAMES = "com.mb.android.EXTRA_SUPPORTS_GAMES";
    public static final String EXTRA_SUPPORTS_PHOTOS = "com.mb.android.EXTRA_SUPPORTS_PHOTOS";
    public static final String EXTRA_SUPPORTS_VIDEO = "com.mb.android.EXTRA_SUPPORTS_VIDEO";

    public static final String CATEGORY_SUPPORTED_QUEUE_TYPES = "com.mb.android.CATEGORY_SUPPORTED_QUEUE_TYPES";
    public static final String EXTRA_SUPPORTS_QUEUED_AUDIO = "com.mb.android.EXTRA_SUPPORTS_QUEUED_AUDIO";
    public static final String EXTRA_SUPPORTS_QUEUED_BOOKS = "com.mb.android.EXTRA_SUPPORTS_QUEUED_BOOKS";
    public static final String EXTRA_SUPPORTS_QUEUED_GAMES = "com.mb.android.EXTRA_SUPPORTS_QUEUED_GAMES";
    public static final String EXTRA_SUPPORTS_QUEUED_PHOTOS = "com.mb.android.EXTRA_SUPPORTS_QUEUED_PHOTOS";
    public static final String EXTRA_SUPPORTS_QUEUED_VIDEO = "com.mb.android.EXTRA_SUPPORTS_QUEUED_VIDEO";
}
