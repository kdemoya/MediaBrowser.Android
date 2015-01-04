package com.mb.android.listeners;

import android.view.View;
import android.widget.BaseAdapter;

import mediabrowser.model.dto.BaseItemDto;

/**
 * Created by Mark on 2014-07-21.
 *
 * Class that handles generation of the popup menu that appears when a user clicks on an overflow icon
 */
public class OverflowOnClickListener extends abstractOverflowClickListener implements View.OnClickListener {

    public OverflowOnClickListener(BaseItemDto item, BaseAdapter baseAdapter) {
        mItem = item;
        mBaseAdapter = baseAdapter;
    }

    @Override
    public void onClick(View v) {
        showMenu(v);
    }
}
