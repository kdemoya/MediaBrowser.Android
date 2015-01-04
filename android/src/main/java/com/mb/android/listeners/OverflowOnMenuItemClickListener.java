package com.mb.android.listeners;

import android.content.Context;
import android.view.MenuItem;
import android.widget.BaseAdapter;

import com.mb.android.MB3Application;
import com.mb.android.Playlist;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import mediabrowser.apiinteraction.Response;
import com.mb.android.ui.mobile.musicartist.GetInstantMixResponse;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.SimilarItemsByNameQuery;
import mediabrowser.model.querying.SimilarItemsQuery;

import java.util.ArrayList;

/**
 * Created by Mark on 2014-07-21.
 *
 * Class that handles the click event that is generated when the user clicks an overflow menu item
 */
public class OverflowOnMenuItemClickListener
        implements android.support.v7.widget.PopupMenu.OnMenuItemClickListener {

    private BaseItemDto mItem;
    private Context mContext;
    private BaseAdapter mBaseAdapter;

    public OverflowOnMenuItemClickListener(BaseItemDto item, BaseAdapter baseAdapter, Context context) {
        mItem = item;
        mBaseAdapter = baseAdapter;
        mContext = context;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        String menuTitle = item.getTitle().toString();

        if (mContext.getResources().getString(R.string.playlist_remove_string).equalsIgnoreCase(menuTitle)) {

            // Remove from playlist
            removeItemFromPlaylist();

        } else if (mContext.getResources().getString(R.string.playlist_add_string).equalsIgnoreCase(menuTitle)) {

            // Add to playlist
            addItemToPlaylist();

        } else if (mContext.getResources().getString(R.string.instant_mix_string).equalsIgnoreCase(menuTitle)) {

            // Instant Mix
            requestInstantMix();

        } else if (mContext.getResources().getString(R.string.like_string).equalsIgnoreCase(menuTitle)) {

            // Like Item
            setItemLiked();

        } else if (mContext.getResources().getString(R.string.dislike_string).equalsIgnoreCase(menuTitle)) {

            // Dislike Item
            setItemDisliked();

        } else if (mContext.getResources().getString(R.string.clear_rating_string).equalsIgnoreCase(menuTitle)) {

            // Clear Item Rating
            clearItemRating();

        } else if (mContext.getResources().getString(R.string.favorite_action_bar_button).equalsIgnoreCase(menuTitle)) {

            // Set Item as Favorite
            setItemFavorite();

        } else if (mContext.getResources().getString(R.string.un_favorite_action_bar_button).equalsIgnoreCase(menuTitle)) {

            // Remove Item Favorite Rating
            removeItemFavorite();

        } else if (mContext.getResources().getString(R.string.un_played_action_bar_button).equalsIgnoreCase(menuTitle)) {

            // Set Item Unplayed
            setItemUnplayed();

        } else if (mContext.getResources().getString(R.string.played_action_bar_button).equalsIgnoreCase(menuTitle)) {

            // Set Item Played
            setItemPlayed();
        }
        return false;
    }

    /**
     * Adds the current item to the current player queue
     */
    private void addItemToPlaylist() {

        if (MB3Application.getInstance().PlayerQueue == null) {
            MB3Application.getInstance().PlayerQueue = new Playlist();
        }

        if (MB3Application.getInstance().PlayerQueue.PlaylistItems == null) {
            MB3Application.getInstance().PlayerQueue.PlaylistItems = new ArrayList<>();
        }

        if (mItem.getRecursiveItemCount() != null && mItem.getRecursiveItemCount() > 0) {

            ItemQuery query = new ItemQuery();
            query.setParentId(mItem.getId());
            query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
            query.setRecursive(true);
            query.setIncludeItemTypes(new String[] { "audio" });

            MB3Application.getInstance().API.GetItemsAsync(query, getAllRecursiveChildrenResponse);

        } else {
            addItemToPlaylistInternal(mItem);
        }
    }

    private void addItemToPlaylistInternal(BaseItemDto item) {

        PlaylistItem playlistItem = new PlaylistItem();
        playlistItem.Id = item.getId();
        playlistItem.Name = item.getName();
        playlistItem.Runtime = item.getRunTimeTicks();
        playlistItem.Type = item.getType();

        if (item.getType() != null && item.getType().equalsIgnoreCase("audio") && item.getArtists() != null) {
            playlistItem.SecondaryText = item.getArtists().get(0);
        }

        MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playlistItem);
    }

    /**
     * Remove the current item from the current player queue
     */
    private void removeItemFromPlaylist() {

        if (MB3Application.getInstance().PlayerQueue == null
                || MB3Application.getInstance().PlayerQueue.PlaylistItems == null) {
            return;
        }

        int index = -1;

        for (int i = 0; i < MB3Application.getInstance().PlayerQueue.PlaylistItems.size(); i++) {
            if (MB3Application.getInstance().PlayerQueue.PlaylistItems.get(i).Id.equalsIgnoreCase(mItem.getId())) {
                index = i;
                break;
            }
        }

        if (index == -1) return;

        MB3Application.getInstance().PlayerQueue.PlaylistItems.remove(index);
    }

    /**
     * Request an Instant Mix from the current Item. Item must be a song, album, artist or music genre
     */
    private void requestInstantMix() {

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(mItem.getType())) return;

        if (mItem.getType().equalsIgnoreCase("Audio")) {

            SimilarItemsQuery query = new SimilarItemsQuery();
            query.setId(mItem.getId());
            query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
            query.setLimit(50);
            MB3Application.getInstance().API.GetInstantMixFromSongAsync(query, new GetInstantMixResponse(mContext));

        } else if (mItem.getType().equalsIgnoreCase("MusicAlbum")) {

            SimilarItemsQuery query = new SimilarItemsQuery();
            query.setId(mItem.getId());
            query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
            query.setLimit(50);
            MB3Application.getInstance().API.GetInstantMixFromAlbumAsync(query, new GetInstantMixResponse(mContext));

        } else if (mItem.getType().equalsIgnoreCase("musicartist")) {

            SimilarItemsByNameQuery query = new SimilarItemsByNameQuery();
            query.setName(mItem.getName());
            query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
            query.setLimit(50);

            MB3Application.getInstance().API.GetInstantMixFromArtistAsync(query, new GetInstantMixResponse(mContext));

        } else if (mItem.getType().equalsIgnoreCase("musicgenre")) {

            SimilarItemsByNameQuery query = new SimilarItemsByNameQuery();
            query.setName(mItem.getName());
            query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
            query.setLimit(50);

            MB3Application.getInstance().API.GetInstantMixFromMusicGenreAsync(query, new GetInstantMixResponse(mContext));
        }
    }

    /**
     * Set the current item as Liked for the current logged in user
     */
    private void setItemLiked() {

        MB3Application.getInstance().API.UpdateUserItemRatingAsync(mItem.getId(),
                MB3Application.getInstance().API.getCurrentUserId(), true, new UpdateUserDataResponse());
    }

    /**
     * Set the current item as Disliked for the current logged in user
     */
    private void setItemDisliked() {

        MB3Application.getInstance().API.UpdateUserItemRatingAsync(mItem.getId(),
                MB3Application.getInstance().API.getCurrentUserId(), false, new UpdateUserDataResponse());
    }

    /**
     * Remove the Liked or Disliked status from the item for the current user
     */
    private void clearItemRating() {

        MB3Application.getInstance().API.ClearUserItemRatingAsync(mItem.getId(),
                MB3Application.getInstance().API.getCurrentUserId(), new UpdateUserDataResponse());
    }

    /**
     * Set current item as a favorite for the current user
     */
    private void setItemFavorite() {

        MB3Application.getInstance().API.UpdateFavoriteStatusAsync(mItem.getId(),
                MB3Application.getInstance().API.getCurrentUserId(), true, new UpdateUserDataResponse());
    }

    /**
     * Remove the current item as a favorite for the current user
     */
    private void removeItemFavorite() {

        MB3Application.getInstance().API.UpdateFavoriteStatusAsync(mItem.getId(),
                MB3Application.getInstance().API.getCurrentUserId(), false, new UpdateUserDataResponse());
    }

    /**
     * Set Item played for the current user
     */
    private void setItemPlayed() {

        MB3Application.getInstance().API.MarkPlayedAsync(
                mItem.getId(),
                MB3Application.getInstance().API.getCurrentUserId(),
                null,
                new UpdateUserDataResponse()
        );
    }

    /**
     * Set Item played for the current user
     */
    private void setItemUnplayed() {

        MB3Application.getInstance().API.MarkUnplayedAsync(
                mItem.getId(),
                MB3Application.getInstance().API.getCurrentUserId(),
                new UpdateUserDataResponse()
        );
    }

    //**********************************************************************************************
    // Callbacks
    //**********************************************************************************************

    /**
     * Callback that is triggered after the user updates the user rating
     */
    private class UpdateUserDataResponse extends Response<UserItemDataDto> {
        @Override
        public void onResponse(UserItemDataDto userItemDataDto) {

            if (userItemDataDto == null) return;

            mItem.setUserData(userItemDataDto);
            mBaseAdapter.notifyDataSetChanged();
        }
    }

    private Response<ItemsResult> getAllRecursiveChildrenResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {
            if (response == null || response.getItems() == null) return;

            for (BaseItemDto item : response.getItems()) {
                addItemToPlaylistInternal(item);
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };
}
