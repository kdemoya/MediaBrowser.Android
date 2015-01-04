package com.mb.android.activities.mobile;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mb.android.activities.BaseMbMobileActivity;
import com.mb.android.adapters.ChannelsAdapter;
import mediabrowser.apiinteraction.Response;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.fragments.NavigationMenuFragment;
import mediabrowser.model.channels.ChannelItemQuery;
import mediabrowser.model.channels.ChannelQuery;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemsResult;
import tangible.DotNetToJavaStringHelper;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * This Activity typically displays a grid representation of a Channels contents.
 */
public class ChannelsActivity extends BaseMbMobileActivity {

    public String TAG = "ChannelsActivity";
    private ActionBarDrawerToggle mDrawerToggle;
    private String mChannelItemId;
    private String mChannelId;
    private List<BaseItemDto> mItems;
    private TextView mWarningText;
    private ProgressBar mBusyIndicator;
    private boolean mIsFresh = true;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.activity_channels);
        mWarningText = (TextView) findViewById(R.id.tvNoContentWarning);
        mBusyIndicator = (ProgressBar) findViewById(R.id.pbActivityIndicator);

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

        if (getMb3Intent() != null) {
            mChannelId = getMb3Intent().getStringExtra("ChannelId");
            mChannelItemId = getMb3Intent().getStringExtra("ChannelItemId");
        }

        mBusyIndicator.setVisibility(View.VISIBLE);

        mMini = (MiniController) findViewById(R.id.miniController1);
        mCastManager.addMiniController(mMini);
        mMini.setOnMiniControllerChangedListener(mCastManager);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            buildAndSendQuery();
        }
    }

    @Override
    protected void onConnectionRestored() {
        buildAndSendQuery();
    }

    private void buildAndSendQuery() {
        if (mIsFresh) {
            if (DotNetToJavaStringHelper.isNullOrEmpty(mChannelId)) {
                // This means we're displaying the root channels rather than the contents of a channel
                ChannelQuery query = new ChannelQuery();
                query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
                MB3Application.getInstance().API.GetChannels(query, new GetChannelsResponse());
            } else {
                // We're drilling down into a channel.
                ChannelItemQuery query = new ChannelItemQuery();
                query.setSortOrder(SortOrder.Ascending);
                query.setChannelId(mChannelId);
                query.setFolderId(mChannelItemId);
                query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
                MB3Application.getInstance().API.GetChannelItems(query, new GetChannelsResponse());
            }
            mIsFresh = false;
        }
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

    //**********************************************************************************************
    // Callback Classes
    //**********************************************************************************************

    private class GetChannelsResponse extends Response<ItemsResult> {
        @Override
        public void onResponse(ItemsResult itemsResult) {
            mBusyIndicator.setVisibility(View.GONE);

            if (itemsResult == null) {
                mWarningText.setText(getResources().getString(R.string.channels_server_error));
                mWarningText.setVisibility(View.VISIBLE);
                return;
            }
            if (itemsResult.getItems() == null || itemsResult.getItems().length == 0) {
                mWarningText.setText(getResources().getString(R.string.channels_no_content_warning));
                mWarningText.setVisibility(View.VISIBLE);
            }

            mItems = Arrays.asList(itemsResult.getItems());
            GridView channelsGrid = (GridView) findViewById(R.id.gvLibrary);
            channelsGrid.setAdapter(new ChannelsAdapter(ChannelsActivity.this, mItems, R.drawable.default_channel_landscape));
            channelsGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    if (mChannelId == null) {

                        Intent intent = new Intent(ChannelsActivity.this, ChannelsActivity.class);
                        intent.putExtra("ChannelId", mItems.get(i).getId());
                        startActivity(intent);

                    } else if (mItems.get(i).getIsFolder()) {

                        Intent intent = new Intent(ChannelsActivity.this, ChannelsActivity.class);
                        intent.putExtra("ChannelId", mChannelId);
                        intent.putExtra("ChannelItemId", mItems.get(i).getId());
                        startActivity(intent);

                    } else {

                        String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(mItems.get(i));

                        Intent intent = new Intent(ChannelsActivity.this, MediaDetailsActivity.class);
                        intent.putExtra("Item", jsonData);
                        startActivity(intent);
                    }
                }
            });
        }
    }
}
