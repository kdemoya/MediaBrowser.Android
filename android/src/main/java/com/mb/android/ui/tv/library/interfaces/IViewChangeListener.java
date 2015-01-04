package com.mb.android.ui.tv.library.interfaces;


import mediabrowser.model.dto.BaseItemDto;

public interface IViewChangeListener {

    public void onCoverflowSelected();

    public void onListSelected();

    public void onGridSelected();

    public void onThumbSelected();

    public void onStripSelected();
}
