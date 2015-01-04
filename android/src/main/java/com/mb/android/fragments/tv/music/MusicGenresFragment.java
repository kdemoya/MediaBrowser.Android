package com.mb.android.fragments.tv.music;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mb.android.MenuEntity;
import com.mb.android.R;

/**
 * Created by Mark on 2014-05-20.
 */
public class MusicGenresFragment extends Fragment {

    private MenuEntity mMenuEntity;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mMenuEntity = (MenuEntity) getArguments().getSerializable("MenuEntity");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View mView = inflater.inflate(R.layout.tv_fragment_series_actors, container, false);

        return mView;
    }
}
