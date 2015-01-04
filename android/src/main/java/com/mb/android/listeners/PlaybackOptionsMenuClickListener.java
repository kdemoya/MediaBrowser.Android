package com.mb.android.listeners;

import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.PopupMenu;
import android.view.View;

import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.ui.mobile.playback.PlaybackActivity;

import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;

/**
 * Created by Mark on 2014-07-24.
 *
 * Class that displays a list of options when the user clicks the hamburger icon in the video
 * playback activity.
 */
public class PlaybackOptionsMenuClickListener implements View.OnClickListener {

    MediaSourceInfo mMediaSourceInfo;
    PlaybackActivity  mPlaybackActivity;


    public PlaybackOptionsMenuClickListener(MediaSourceInfo mediaSourceInfo, PlaybackActivity playbackActivity) {
        mMediaSourceInfo = mediaSourceInfo;
        mPlaybackActivity = playbackActivity;
    }

    @Override
    public void onClick(View v) {

        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
        int i = 0;

        int audioStreamCount = 0;
        int subtitleStreamCount = 0;

        if (mMediaSourceInfo != null && mMediaSourceInfo.getMediaStreams() != null) {
            for (MediaStream stream : mMediaSourceInfo.getMediaStreams()) {
                if (stream.getType() != null && stream.getType().equals(MediaStreamType.Audio)) {
                    audioStreamCount++;
                } else if (stream.getType() != null && stream.getType().equals(MediaStreamType.Subtitle)) {
                    subtitleStreamCount++;
                }
            }
        }

        // Only show the stream menu options if there's something to configure
        if (audioStreamCount > 1) {
            popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.audio_stream_selection));
            i++;
        }
        // Allow user to disable subtitles as well as pick them
        if (subtitleStreamCount > 0) {
            popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.subtitle_stream_selection));
            i++;
        }

        popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.bitrate_selection));

        popupMenu.setOnMenuItemClickListener(new PlaybackOptionsMenuItemClickListener((FragmentActivity)v.getContext(), mPlaybackActivity));
        popupMenu.show();
    }
}
