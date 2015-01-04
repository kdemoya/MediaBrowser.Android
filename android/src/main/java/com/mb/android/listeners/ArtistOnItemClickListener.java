package com.mb.android.listeners;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import com.mb.android.ui.mobile.musicartist.ArtistActivity;
import mediabrowser.model.dto.BaseItemDto;

/**
 * Created by Mark on 2014-07-21.
 *
 * Class that handles clicks on music artists
 */
public class ArtistOnItemClickListener implements AdapterView.OnItemClickListener  {

    protected BaseItemDto mItem;
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Activity activity = (Activity) view.getContext();
        mItem = (BaseItemDto) parent.getItemAtPosition(position);

        Intent intent = new Intent(activity, ArtistActivity.class);
        intent.putExtra("ArtistId", mItem.getId());
        activity.startActivity(intent);
    }
}
