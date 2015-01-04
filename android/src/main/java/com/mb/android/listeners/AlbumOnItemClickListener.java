package com.mb.android.listeners;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import com.mb.android.ui.mobile.album.MusicAlbumActivity;
import mediabrowser.model.dto.BaseItemDto;

/**
 * Created by Mark on 2014-07-21.
 *
 * Class that handles an Album click.
 */
public class AlbumOnItemClickListener implements AdapterView.OnItemClickListener {

    protected BaseItemDto mAlbum;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Activity activity = (Activity) view.getContext();
        mAlbum = (BaseItemDto) parent.getItemAtPosition(position);

        Intent intent = new Intent(activity, MusicAlbumActivity.class);
        intent.putExtra("AlbumId", mAlbum.getId());

        activity.startActivity(intent);
    }
}
