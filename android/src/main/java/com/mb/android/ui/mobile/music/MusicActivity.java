package com.mb.android.ui.mobile.music;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mb.android.MB3Application;
import com.mb.android.activities.BaseMbMobileActivity;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.R;
import com.mb.android.fragments.NavigationMenuFragment;

/**
 * Created by Mark on 2014-07-12.
 *
 * Activity that is a landing page for everything music related
 */
public class MusicActivity extends BaseMbMobileActivity {

    private ActionBarDrawerToggle mDrawerToggle;
    private String mParentId;
    private boolean mIsFresh = true;
    private DrawerLayout mFilterSortDrawer;
    private FilterSortMenuFragment filterSortMenuFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_music_home);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawer.setFocusableInTouchMode(false);

        final NavigationMenuFragment fragment = (NavigationMenuFragment) getSupportFragmentManager().findFragmentById(R.id.left_drawer);
        if (fragment != null && fragment.isInLayout()) {
            fragment.setDrawerLayout(drawer);
        }
        filterSortMenuFragment = (FilterSortMenuFragment) getSupportFragmentManager().findFragmentById(R.id.right_drawer);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                drawer,
                R.string.abc_action_bar_home_description,
                R.string.abc_action_bar_up_description) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
//                getActionBar().setTitle(mTitle);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
//                getActionBar().setTitle(mDrawerTitle);
            }

        };

        drawer.setDrawerListener(mDrawerToggle);

        mParentId = getMb3Intent().getStringExtra("ParentId");

        mFilterSortDrawer = (DrawerLayout) findViewById(R.id.sort_filter_drawer);

        mMini = (MiniController) findViewById(R.id.miniController1);
        mCastManager.addMiniController(mMini);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            buildUi();
        }
    }
    @Override
    protected void onConnectionRestored() {
        buildUi();
    }

    private void buildUi() {
        if (mIsFresh) {
            ViewPager pager = (ViewPager) findViewById(R.id.vpViewPager);
            pager.setAdapter(new MusicAdapter(getSupportFragmentManager()));
            mIsFresh = false;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.add("filter/sort").setIcon(R.drawable.filter).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        /*
        Filter/Sort
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase("filter/sort")) {
            if (mFilterSortDrawer.isDrawerOpen(Gravity.RIGHT)) {
                mFilterSortDrawer.closeDrawer(Gravity.RIGHT);
            } else {
                mFilterSortDrawer.openDrawer(Gravity.RIGHT);
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    //**********************************************************************************************
    // Navigation
    //**********************************************************************************************

    public class MusicAdapter extends FragmentStatePagerAdapter {

        public MusicAdapter(FragmentManager fm) {
            super(fm);
        }

        /**
         * Return the Fragment associated with a specified position.
         *
         * @param position The index of the fragment to be returned
         */
        @Override
        public Fragment getItem(int position) {

            Fragment fragment;

            if (position == 0) {
                fragment = new OnStageFragment();
            } else {
                fragment = new MusicLibraryFragment();
                if (filterSortMenuFragment != null) {
                    filterSortMenuFragment.setLibraryFragment((MusicLibraryFragment)fragment);
                }
            }

            Bundle bundle = new Bundle();
            bundle.putString("ParentId", mParentId);
            fragment.setArguments(bundle);

            return fragment;
        }

        /**
         * Return the number of views available.
         */
        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            if (position == 0) {
                return getResources().getString(R.string.on_stage_string);
            } else {
                return getResources().getString(R.string.library_header);
            }
        }
    }

    // OH MY GOD this feels like a hack
    public void updateMusicLibraryFragmentReference(MusicLibraryFragment fragment) {
        if (filterSortMenuFragment == null) {
            filterSortMenuFragment = (FilterSortMenuFragment) getSupportFragmentManager().findFragmentById(R.id.right_drawer);
        }
        if (filterSortMenuFragment != null) {
            filterSortMenuFragment.setLibraryFragment(fragment);
        }
    }
}
