package com.mb.android.listeners;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import com.mb.android.playbackmediator.cast.VideoCastManager;
import com.mb.android.MB3Application;
import com.mb.android.PlaylistItem;
import com.mb.android.player.AudioService;
import com.mb.android.ui.mobile.playback.AudioPlaybackActivity;
import com.mb.android.activities.mobile.RemoteControlActivity;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.session.PlayCommand;

import java.util.ArrayList;

/**
 * Created by Mark on 2014-07-21.
 *
 * Class that handles the required actions when a song is clicked in one of the views
 */
public class SongOnItemClickListener implements AdapterView.OnItemClickListener {

    protected BaseItemDto mSong;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        VideoCastManager mCastManager = MB3Application.getCastManager(parent.getContext());
        Activity activity = (Activity) view.getContext();
        mSong = (BaseItemDto) parent.getItemAtPosition(position);

        AudioService.PlayerState currentState = MB3Application.getAudioService().getPlayerState();
        if (currentState.equals(AudioService.PlayerState.PLAYING) || currentState.equals(AudioService.PlayerState.PAUSED)) {
            MB3Application.getAudioService().stopMedia();
        }

        // Just in case the TV Theme is still playing
        MB3Application.getInstance().StopMedia();

        if (mCastManager.isConnected()) {
            mCastManager.playItem(mSong, PlayCommand.PlayNow, 0L);

            Intent intent = new Intent(activity, RemoteControlActivity.class);
            activity.startActivity(intent);
        } else {
            PlaylistItem playableItem = new PlaylistItem();
            playableItem.Id = mSong.getId();
            playableItem.Name = mSong.getName();
            playableItem.SecondaryText = mSong.getAlbumArtist();
            playableItem.Runtime = mSong.getRunTimeTicks();
            playableItem.Type = mSong.getType();

            MB3Application.getInstance().PlayerQueue.PlaylistItems = new ArrayList<>();
            MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playableItem);

            Intent intent = new Intent(activity, AudioPlaybackActivity.class);
            activity.startActivity(intent);
        }
    }
}
