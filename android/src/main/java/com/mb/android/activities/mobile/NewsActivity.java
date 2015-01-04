package com.mb.android.activities.mobile;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.mb.android.activities.BaseMbMobileActivity;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.adapters.NewsAdapter;
import com.mb.android.fragments.NavigationMenuFragment;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.results.NewsItemsResult;

/**
 * Created by Mark on 18/01/14.
 *
 * This Activity shows all the recent announcements that have been published.
 */
public class NewsActivity extends BaseMbMobileActivity {

    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_news);

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
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            getNewsItems();
        }
    }

    @Override
    public void onPause() {
        mMini.removeOnMiniControllerChangedListener(mCastManager);
        super.onPause();
    }

    @Override
    protected void onConnectionRestored() {
        getNewsItems();
    }

    private void getNewsItems() {
        MB3Application.getInstance().API.GetNewsItems(new GetNewsItemsResponse());
    }

    private class GetNewsItemsResponse extends Response<NewsItemsResult> {
        @Override
        public void onResponse(final NewsItemsResult result) {
            ListView newsList = (ListView) findViewById(R.id.lvNewsList);
            newsList.setAdapter(new NewsAdapter(result.getItems(), NewsActivity.this, MB3Application.getInstance().API));
            newsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.getItems()[i].getLink()));
                    startActivity(browserIntent);
                }
            });
        }
    }
}
