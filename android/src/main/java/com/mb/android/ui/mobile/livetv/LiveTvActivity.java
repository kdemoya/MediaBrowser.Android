package com.mb.android.ui.mobile.livetv;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;

import com.mb.android.MB3Application;
import com.mb.android.activities.BaseMbMobileActivity;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.R;
import com.mb.android.fragments.NavigationMenuFragment;

/**
 * Created by Mark on 28/01/14.
 *
 * This Activity will contain all the fragments used to provide a Live-TV UI. There should be a
 * fragment for each of the following
 *
 * a) The Guide (Will likely be the hardest facet to incorporate
 * b) Channels: Perhaps a grid/list of all channels
 * c) Recorded media
 * d) Scheduled Recordings: Just a list of what's going to be recorded in the near future
 * e) Series Recordings: Shows ongoing series recordings
 */
public class LiveTvActivity extends BaseMbMobileActivity {

    private ActionBarDrawerToggle mDrawerToggle;
    private boolean mIsFresh = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_tv);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawer.setFocusableInTouchMode(false);

        NavigationMenuFragment fragment = (NavigationMenuFragment) getSupportFragmentManager().findFragmentById(R.id.left_drawer);
        if (fragment != null && fragment.isInLayout()) {
            fragment.setDrawerLayout(drawer);
        }

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
            pager.setAdapter(new LiveTvAdapter(getSupportFragmentManager()));
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    public class LiveTvAdapter extends FragmentStatePagerAdapter {

        public LiveTvAdapter(FragmentManager fm) {
            super(fm);
        }

        /**
         * Return the Fragment associated with a specified position.
         *
         * @param position The index of the fragment to be returned
         */
        @Override
        public Fragment getItem(int position) {

            if (position == 0) {
                return new ChannelsFragment();
            } else if (position == 1) {
                return new RecordingsFragment();
            } else if (position == 2) {
                return new ScheduledFragment();
            }

            return new ScheduledSeriesFragment();
        }

        /**
         * Return the number of views available.
         */
        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            if (position == 0) {
                return getResources().getString(R.string.ltv_channels);
            } else if (position == 1) {
                return getResources().getString(R.string.ltv_recordings);
            } else if (position == 2) {
                return getResources().getString(R.string.ltv_scheduled_recordings);
            }
            return getResources().getString(R.string.ltv_scheduled_series);
        }
    }
}
