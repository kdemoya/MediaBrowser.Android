package com.mb.android.listeners;

import android.support.v7.widget.PopupMenu;
import android.view.View;
import android.widget.BaseAdapter;

import com.mb.android.MB3Application;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import mediabrowser.model.dto.BaseItemDto;

/**
 * Created by Mark on 2014-08-16.
 */
public abstract class abstractOverflowClickListener {

    protected BaseItemDto mItem;
    protected BaseAdapter mBaseAdapter;

    // Method that will show the popup menu
    public void showMenu(View v) {

        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
        int i = 0;

        // Determine whether the item should be added or removed from the playlist
        if (MB3Application.getInstance().PlayerQueue != null
                && MB3Application.getInstance().PlayerQueue.PlaylistItems != null) {

            boolean exists = false;

            for (PlaylistItem plItem : MB3Application.getInstance().PlayerQueue.PlaylistItems) {
                if (plItem.Id.equalsIgnoreCase(mItem.getId())) {
                    exists = true;
                    break;
                }
            }

            if (exists) {
                popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.playlist_remove_string));
            } else {
                popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.playlist_add_string));
            }
            i++;
        }

        if (mItem.getType() != null
                && (mItem.getType().equalsIgnoreCase("Audio")
                || mItem.getType().equalsIgnoreCase("MusicAlbum")
                || mItem.getType().equalsIgnoreCase("MusicArtist")
                || mItem.getType().equalsIgnoreCase("MusicGenre"))) {
            popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.instant_mix_string));
            i++;
        }

        if (mItem.getUserData() != null) {
            if (mItem.getUserData().getLikes() == null) {
                popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.like_string));
                i++;
                popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.dislike_string));
            } else if (mItem.getUserData().getLikes()) {
                popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.dislike_string));
                i++;
                popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.clear_rating_string));
            } else {
                popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.like_string));
                i++;
                popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.clear_rating_string));
            }
            i++;

            if (mItem.getUserData().getIsFavorite()) {
                popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.un_favorite_action_bar_button));
            } else {
                popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.favorite_action_bar_button));
            }
            i++;

            if (mItem.getUserData().getPlayed()) {
                popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.un_played_action_bar_button));
            } else {
                popupMenu.getMenu().add(i, i, 0, MB3Application.getInstance().getResources().getString(R.string.played_action_bar_button));
            }
        }

        popupMenu.setOnMenuItemClickListener(new OverflowOnMenuItemClickListener(mItem, mBaseAdapter, v.getContext()));
        popupMenu.show();

    }
}
