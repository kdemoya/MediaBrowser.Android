package com.mb.android.ui.tv.library;

import android.support.v4.app.Fragment;

import mediabrowser.model.dto.BaseItemDto;

import java.util.ArrayList;

/**
 * Created by Mark on 2014-11-15.
 */
public abstract class BaseLibraryFragment extends Fragment {

    public abstract void addContent(BaseItemDto[] items);

    public abstract BaseItemDto getCurrentItem();

    public abstract void refreshData(BaseItemDto item);

    public boolean onDpadLeftHandled() { return false; }

    public boolean onDpadRightHandled() { return false; }

    public boolean onDpadUpHandled() { return false; }

    public boolean onDpadDownHandled() { return false; }
}
