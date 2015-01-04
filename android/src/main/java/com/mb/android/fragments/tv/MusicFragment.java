package com.mb.android.fragments.tv;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mb.android.MenuEntity;
import com.mb.android.R;
import com.mb.android.fragments.tv.music.MusicAlbumsFragment;
import com.mb.android.fragments.tv.music.MusicArtistsFragment;
import com.mb.android.fragments.tv.music.MusicGenresFragment;
import com.mb.android.fragments.tv.music.MusicSongsFragment;
import com.mb.android.fragments.tv.music.MusicUpNowFragment;
import com.mb.android.fragments.tv.music.MusicYearsFragment;
import com.mb.android.logging.FileLogger;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment used to show a users music library in various formats
 */
public class MusicFragment extends Fragment {

    public String TAG = "MusicFragment";
    private MenuEntity mMenuEntity;
    private View mView;


    /**
     * Class Constructor
     */
    public MusicFragment() {}

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mMenuEntity = (MenuEntity) getArguments().getSerializable("MenuEntity");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.tv_activity_details, container, false);

        PagerTabStrip tabStrip = (PagerTabStrip) mView.findViewById(R.id.pager_title_strip);
        tabStrip.setDrawFullUnderline(false);

        ViewPager mViewPager = (ViewPager) mView.findViewById(R.id.mediaPager);

        if (mViewPager != null) {
            FileLogger.getFileLogger().Info("MusicFragment: Initializing ViewPager");
            mViewPager.setAdapter(new MusicPagerAdapter(getActivity().getSupportFragmentManager()));
        }

        return mView;
    }


    private void LoadBackdrops() {

//        if (mItem.BackdropCount() > 0) {
//            List<String> backdropUrls = new ArrayList<>();
//
//            for (int i = 0; i < mItem.BackdropCount(); i++) {
//
//                ImageOptions imageOptions = new ImageOptions();
//                imageOptions.ImageType = ImageType.Backdrop;
//                imageOptions.MaxHeight = 720;
//                imageOptions.MaxWidth = 1280;
//                imageOptions.ImageIndex = i;
//
//                backdropUrls.add(MB3Application.getInstance().API.GetImageUrl(mItem, imageOptions));
//            }
//
//            if (backdropUrls.size() > 0) {
//                CoreActivity coreActivity = (CoreActivity) getActivity();
//                if (coreActivity != null) {
//                    coreActivity.SetBackdropImages(backdropUrls);
//                }
//            }
//
//        }
    }

    private void LoadLogoImage() {

//        String imageUrl = null;
//
//        ImageOptions options = new ImageOptions();
//        options.ImageType = ImageType.Logo;
//        options.MaxHeight = 300;
//        options.MaxWidth = 500;
//
//        if (mItem.HasLogo()) {
//            imageUrl = MB3Application.getInstance().API.GetImageUrl(mItem, options);
//        } else if (mItem.ParentLogoItemId != null) {
//            imageUrl = MB3Application.getInstance().API.GetImageUrl(mItem.ParentLogoItemId, options);
//        }
//
//        CoreActivity coreActivity = (CoreActivity) getActivity();
//        if (coreActivity != null) {
//            coreActivity.SetLogoImage(imageUrl);
//        }
    }

    //**********************************************************************************************
    // Callback Classes
    //**********************************************************************************************


    private class MusicPagerAdapter extends FragmentStatePagerAdapter {

        public MusicPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            Bundle args = new Bundle();

            switch (position) {

                case 0: // Up Now
                    MusicUpNowFragment mUpNowFragment = new MusicUpNowFragment();
                    args.putSerializable("MenuEntity", mMenuEntity);
                    mUpNowFragment.setArguments(args);
                    return mUpNowFragment;
                case 1: // Artists -- show albums
                    MusicArtistsFragment mArtistsFragment = new MusicArtistsFragment();
                    args.putSerializable("MenuEntity", mMenuEntity);
                    mArtistsFragment.setArguments(args);
                    return mArtistsFragment;
                case 2: // Albums -- show albums
                    MusicAlbumsFragment mAlbumsFragment = new MusicAlbumsFragment();
                    args.putSerializable("MenuEntity", mMenuEntity);
                    mAlbumsFragment.setArguments(args);
                    return mAlbumsFragment;
                case 3: // Songs -- show songs
                    MusicSongsFragment mSongsFragment = new MusicSongsFragment();
                    args.putSerializable("MenuEntity", mMenuEntity);
                    mSongsFragment.setArguments(args);
                    return mSongsFragment;
                case 4: // Genres -- show genres then albums
                    MusicGenresFragment mGenresFragment = new MusicGenresFragment();
                    args.putSerializable("MenuEntity", mMenuEntity);
                    mGenresFragment.setArguments(args);
                    return mGenresFragment;
                case 5: // Years -- show years then songs
                    MusicYearsFragment mYearsFragment = new MusicYearsFragment();
                    args.putSerializable("MenuEntity", mMenuEntity);
                    mYearsFragment.setArguments(args);
                    return mYearsFragment;
            }

            return null;
        }

        @Override
        public int getCount() {
            return 6;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            switch (position) {

                case 0:
                    return "Up Now";
                case 1:
                    return "Artists";
                case 2:
                    return "Albums";
                case 3:
                    return "Songs";
                case 4:
                    return "Genres";
                default:
                    return "Years";
            }
        }
    }
}
