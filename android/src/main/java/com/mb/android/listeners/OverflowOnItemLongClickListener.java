package com.mb.android.listeners;

import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import mediabrowser.model.dto.BaseItemDto;

/**
 * Created by Mark on 2014-08-16.
 * Class that handles generation of the popup menu that appears when a user long clicks on a grid/list item
 */
public class OverflowOnItemLongClickListener extends abstractOverflowClickListener implements AdapterView.OnItemLongClickListener {

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        mBaseAdapter = (BaseAdapter)parent.getAdapter();
        mItem = (BaseItemDto) mBaseAdapter.getItem(position);
        showMenu(view);
        return true;
    }
}
