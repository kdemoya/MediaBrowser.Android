package com.mb.android.player;

import mediabrowser.model.dto.BaseItemDto;

/**
 * Created by Mark on 2014-07-10.
 */
public interface AudioPlayerListener {

    public void onItemLoaded(BaseItemDto baseItemDto, int playlistPositionIndex);

    public void onPlaylistCreated();

    public void onPlaylistCompleted();

    public void onPlayPauseChanged(boolean paused);

    public void onVolumeChanged(boolean muted, float volume);

    public void onShuffleChanged(boolean isShuffling);

    public void onRepeatChanged(boolean isRepeating);
}
