package com.mb.android.ui.mobile.person;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;

import com.mb.android.MB3Application;
import com.mb.android.activities.BaseMbMobileActivity;
import com.mb.android.logging.FileLogger;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.R;
import com.mb.android.fragments.NavigationMenuFragment;

/**
 * Created by Mark on 12/12/13.
 *
 * This Activity displays an actors biography as well as a list of media that the actor has been involved in.
 */
public class ActorBioActivity extends BaseMbMobileActivity {

    private static final String TAG = "ActorBioActivity";
    private ViewPager mViewPager;
    private ActionBarDrawerToggle mDrawerToggle;
    private String mActorName;
    private String mActorId;
    private ActorBioFragment mActorBioFragment;
    private ActorLibraryFragment mActorLibraryFragment;
    private boolean mIsFresh = true;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actor_details);

        mViewPager = (ViewPager) findViewById(R.id.actorPager);

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

        mActorName = getMb3Intent().getStringExtra("ActorName");
        mActorId = getMb3Intent().getStringExtra("ActorId");
        // TODO: move name into content area
        if (mActionBar != null) {
            mActionBar.setTitle(mActorName);
        }

        mMini = (MiniController) findViewById(R.id.miniController1);
        mCastManager.addMiniController(mMini);

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

    @Override
    public void onPause() {
        mMini.removeOnMiniControllerChangedListener(mCastManager);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            buildUI();
        }
    }

    @Override
    protected void onConnectionRestored() {
        buildUI();
    }

    private void buildUI() {
        if (mIsFresh) {
            if (mViewPager != null) {
                FileLogger.getFileLogger().Info(TAG + ": building ViewPager");
                mViewPager.setAdapter(new ActorPagerAdapter(getSupportFragmentManager()));
            } else {
                FileLogger.getFileLogger().Info(TAG + ": building UI Components");
                Bundle args = new Bundle();
                args.putString("ActorName", mActorName);
                args.putString("ActorId", mActorId);
                args.putBoolean("IsTabletLayout", true);

                mActorBioFragment = new ActorBioFragment();
                mActorBioFragment.setArguments(args);

                mActorLibraryFragment = new ActorLibraryFragment();
                mActorLibraryFragment.setArguments(args);

                FragmentManager fm = getSupportFragmentManager();

                FragmentTransaction fragmentTransaction = fm.beginTransaction();
                fragmentTransaction.replace(R.id.fActorBioContainer, mActorBioFragment);
                fragmentTransaction.replace(R.id.fActorMediaListContainer, mActorLibraryFragment);
                fragmentTransaction.commit();
            }
            mIsFresh = false;
            FileLogger.getFileLogger().Info(TAG + ": finished building UI");
        }
    }

    private class ActorPagerAdapter extends FragmentPagerAdapter {

        public ActorPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            Bundle args = new Bundle();
            args.putString("ActorName", mActorName);
            args.putString("ActorId", mActorId);

            switch (position) {
                case 0:
                    mActorBioFragment = new ActorBioFragment();
                    mActorBioFragment.setArguments(args);
                    return mActorBioFragment;
                case 1:
                    mActorLibraryFragment = new ActorLibraryFragment();
                    mActorLibraryFragment.setArguments(args);
                    return mActorLibraryFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            if (position == 0)
                return getResources().getString(R.string.bio_header);

            return getResources().getString(R.string.media_header);
        }
    }
}
