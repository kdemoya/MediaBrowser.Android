package com.mb.android.ui.tv.library.interfaces;

import mediabrowser.model.dto.UserItemDataDto;

/**
 * Created by Mark on 2014-11-01.
 */
public interface ILongPressDialogListener {

    /**
     * The User selected one of the long-press actions. The calling UI should update the userdata for that item.
     */
    public void onUserDataChanged(String itemId, UserItemDataDto userItemDataDto);
}
