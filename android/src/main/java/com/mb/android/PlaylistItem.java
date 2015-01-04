package com.mb.android;

/**
 * Created by Mark on 11/12/13.
 */
public class PlaylistItem {

    /**
     * The GUID of the item to play
     */
    public String Id;

    /**
     * The name of the item to play
     */
    public String Name;

    /**
     *
     */
    public String SecondaryText;

    public Long Runtime;
    /**
     * The type of file being played. Song, Video etc
     */
    public String Type;

    /**
     * Resume the item from the playback position ticks of the items userdata object. Only usable
     * if a single item is being played
     */
    public Long startPositionTicks;

    /**
     * Used when an item is added that has children. IE: a folder or Album
     */
    public boolean Recursive;

    /**
     * The audio stream chosen from the advanced play dialog
     */
    public Integer AudioStreamIndex;

    /**
     * The subtitle stream chosen from the advanced play dialog
     */
    public Integer SubtitleStreamIndex;
}
