package com.mb.android.ui.mobile.musicartist;

import android.content.Context;
import android.content.Intent;

import com.mb.android.MB3Application;
import com.mb.android.Playlist;
import com.mb.android.PlaylistItem;
import mediabrowser.apiinteraction.Response;
import com.mb.android.ui.mobile.playback.AudioPlaybackActivity;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.querying.ItemsResult;

import java.util.ArrayList;

/**
 * Created by Mark on 2014-07-12.
 */
public class GetInstantMixResponse extends Response<ItemsResult> {

    private Context mContext;
    public GetInstantMixResponse(Context context) {
        mContext = context;
    }

    @Override
    public void onResponse(ItemsResult response) {

        if (response == null || response.getItems() == null) return;

        MB3Application.getInstance().PlayerQueue = new Playlist();
        MB3Application.getInstance().PlayerQueue.PlaylistItems = new ArrayList<>();

        for (BaseItemDto song : response.getItems()) {
            PlaylistItem playableItem = new PlaylistItem();
            playableItem.Id = song.getId();
            playableItem.Name = song.getName();
            if (song.getArtists() != null) {
                StringBuilder sb = new StringBuilder();
                for (String artist : song.getArtists()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(artist);
                }

                playableItem.SecondaryText = sb.toString();
            }

            playableItem.Type = song.getType();
            playableItem.Runtime = song.getRunTimeTicks();

            MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playableItem);
        }

        Intent intent = new Intent(mContext, AudioPlaybackActivity.class);
        mContext.startActivity(intent);

    }
}
