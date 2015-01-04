package com.mb.android.interfaces;

import mediabrowser.model.apiclient.ServerInfo;

/**
 * Created by Mark on 12/12/13.
 */
public interface IServerDialogClickListener {

    public void onOkClick(String address);

    public void onEditOkClick(ServerInfo serverInfo);

    public void onCancelClick();
}
