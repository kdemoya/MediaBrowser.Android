package com.mb.android.ui.mobile.album;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mb.android.MB3Application;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mark on 2014-07-11.
 */
public class BaseSongAdapter extends BaseAdapter {

    private List<PlaylistItem> mPlaylistItems;
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private int mCurrentPlayingIndex = 0;

    public BaseSongAdapter(List<PlaylistItem> playList, Context context) {

        mPlaylistItems = playList;
        mContext = context;
        mLayoutInflater = (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setCurrentPlayingIndex(int index) {
        mCurrentPlayingIndex = index;
        this.notifyDataSetChanged();
    }

    public void remove(PlaylistItem item) {

        mPlaylistItems.remove(item);
        this.notifyDataSetChanged();
    }

    public void insert(PlaylistItem item, int index) {

        mPlaylistItems.add(index, item);
        this.notifyDataSetChanged();
    }

    public void clearPlaylist() {
        mPlaylistItems = new ArrayList<>();
        this.notifyDataSetChanged();
    }

    public int getItemIndex(String itemId) {

        int index = -1;

        if (mPlaylistItems != null && !mPlaylistItems.isEmpty()) {
            for (int i = 0; i < mPlaylistItems.size(); i++) {
                if (mPlaylistItems.get(i).Id.equalsIgnoreCase(itemId)) {
                    index = i;
                    break;
                }
            }
        }

        Log.d("BaseSongAdapter", "getItemIndex() = " + String.valueOf(index));
        return index;
    }

    @Override
    public int getCount() {
        return mPlaylistItems.size();
    }

    @Override
    public Object getItem(int i) {
        return mPlaylistItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        if (view == null) {
            view = mLayoutInflater.inflate(R.layout.playlist_song, viewGroup, false);
        }

        if (view != null) {

            TextView title = (TextView) view.findViewById(R.id.tvSongTitle);
            TextView artist = (TextView) view.findViewById(R.id.tvSongArtist);
            ImageView dragHandle = (ImageView) view.findViewById(R.id.ivDragHandle);
            TextView runtime = (TextView) view.findViewById(R.id.tvSongRuntime);
//            ImageView overflow = (ImageView) view.findViewById(R.id.ivMediaOverflow);
//            overflow.setVisibility(View.GONE);

            title.setText(mPlaylistItems.get(i).Name);
            artist.setText(mPlaylistItems.get(i).SecondaryText);

            if (mPlaylistItems.get(i).Runtime != null) {
                runtime.setText(Utils.PlaybackRuntimeFromMilliseconds(mPlaylistItems.get(i).Runtime / 10000));
            } else {
                runtime.setText("0:00");
            }

            if (mCurrentPlayingIndex == i) {
                dragHandle.setImageResource(R.drawable.play);
                dragHandle.setAlpha(1f);
            } else {
                dragHandle.setImageResource(R.drawable.ic_dialog_dialer);
                dragHandle.setAlpha(.2f);
            }
        }

        return view;
    }
}
