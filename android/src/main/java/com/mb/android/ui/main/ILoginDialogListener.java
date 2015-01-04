package com.mb.android.ui.main;

import mediabrowser.model.dto.UserDto;

/**
 * Created by Mark on 2014-10-28.
 */
public interface ILoginDialogListener {

    public void onLoginDialogPositiveButtonClick(UserDto user, String password);
    public void onLoginDialogPositiveButtonClick(String username, String password);
    public void onLoginDialogNegativeButtonClick();
}
