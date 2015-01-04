package com.mb.android.player;

/**
 * Created by Mark on 2014-05-16.
 *
 * Various callback methods that will allow the MediaPlayerSurfaceView to communicate with the
 * parent fragment or activity.
 */
public interface IMediaPlayerSurfaceViewCallbacks {

    public void onVolumeChanged(float newVolume);

    public void onMute();

    public void onPlayPauseChanged(boolean isPaused);

    public void onPositionUpdated(int newPosition);

    public void onPlaybackStarting();

    public void onPlaybackCompleted();
}
