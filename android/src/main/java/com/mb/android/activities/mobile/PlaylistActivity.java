package com.mb.android.activities.mobile;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.activities.BaseMbMobileActivity;
import com.mb.android.fragments.NavigationMenuFragment;
import com.mb.android.logging.FileLogger;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.ui.mobile.playback.AudioPlaybackActivity;
import com.mb.android.ui.mobile.playback.PlaybackActivity;
import com.mb.android.utils.Utils;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;

/**
 * Created by Mark on 2014-10-01.
 */
public class PlaylistActivity extends BaseMbMobileActivity {

    private static final String TAG = "PlaylistActivity";
    private ActionBarDrawerToggle mDrawerToggle;
    private PlaylistAdapter mPlaylistAdapter;
    private TextView clearPlaylistButton;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_playlist);

        FileLogger.getFileLogger().Info(TAG + ": onCreate");

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

        DragSortListView playList = (DragSortListView) findViewById(R.id.playlist);
        DragSortController controller = buildController(playList);
        playList.setFloatViewManager(controller);
        playList.setOnTouchListener(controller);
        playList.setDragEnabled(true);
        playList.setDropListener(onDrop);
        playList.setRemoveListener(onRemove);
        playList.setEmptyView(getLayoutInflater().inflate(R.layout.widget_playlist_empty_view, null));
        mPlaylistAdapter = new PlaylistAdapter();
        playList.setAdapter(mPlaylistAdapter);

        clearPlaylistButton = (TextView) findViewById(R.id.tvClearPlaylist);
        if (clearPlaylistButton != null) {
            clearPlaylistButton.setOnClickListener(onClearPlaylistClick);
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
    public void onResume() {
        super.onResume();
        toggleClearPlaylistButtonVisibility(MB3Application.getInstance().PlayerQueue.PlaylistItems != null
                && MB3Application.getInstance().PlayerQueue.PlaylistItems.size() > 0);
    }

    @Override
    protected void onConnectionRestored() {

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (MB3Application.getInstance().PlayerQueue != null
                && MB3Application.getInstance().PlayerQueue.PlaylistItems != null
                && MB3Application.getInstance().PlayerQueue.PlaylistItems.size() > 0) {

            menu.add(getResources().getString(R.string.play_action_bar_button))
                    .setIcon(R.drawable.play)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.play_action_bar_button))) {
            if ("audio".equalsIgnoreCase(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Type)) {
                Intent intent = new Intent(this, AudioPlaybackActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(MB3Application.getInstance(), PlaybackActivity.class);
                startActivity(intent);
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    private class PlaylistAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return MB3Application.getInstance().PlayerQueue.PlaylistItems.size();
        }

        @Override
        public Object getItem(int position) {
            return MB3Application.getInstance().PlayerQueue.PlaylistItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(PlaylistActivity.this).inflate(R.layout.playlist_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.primaryImage = (NetworkImageView) convertView.findViewById(R.id.ivMediaImage);
                viewHolder.primaryText = (TextView) convertView.findViewById(R.id.tvSongTitle);
                viewHolder.secondaryText = (TextView) convertView.findViewById(R.id.tvSongArtist);
                viewHolder.dragHandle = (ImageView) convertView.findViewById(R.id.ivDragHandle);
                viewHolder.runtime = (TextView) convertView.findViewById(R.id.tvRuntime);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            PlaylistItem item = MB3Application.getInstance().PlayerQueue.PlaylistItems.get(position);

            viewHolder.primaryText.setText(item.Name);
            viewHolder.secondaryText.setText(item.SecondaryText);
            if (item.Runtime != null) {
                viewHolder.runtime.setText(Utils.TicksToRuntimeString(item.Runtime));
            } else {
                viewHolder.runtime.setText("");
            }


            return convertView;
        }
    }

    private class ViewHolder {

        public ImageView dragHandle;
        public NetworkImageView primaryImage;
        public TextView primaryText;
        public TextView secondaryText;
        public TextView runtime;
    }

    //**********************************************************************************************
    // DragSortListView Methods
    //**********************************************************************************************

    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    if (from != to) {
                        PlaylistItem item = MB3Application.getInstance().PlayerQueue.PlaylistItems.get(from);
                        MB3Application.getInstance().PlayerQueue.PlaylistItems.remove(item);
                        MB3Application.getInstance().PlayerQueue.PlaylistItems.add(to, item);
                        mPlaylistAdapter.notifyDataSetChanged();
                    }
                }
            };

    private DragSortListView.RemoveListener onRemove =
            new DragSortListView.RemoveListener() {
                @Override
                public void remove(int which) {
                    MB3Application.getInstance().PlayerQueue.PlaylistItems.remove(which);
                    mPlaylistAdapter.notifyDataSetChanged();
                    if (MB3Application.getInstance().PlayerQueue.PlaylistItems.size() == 0) {
                        invalidateOptionsMenu();
                        toggleClearPlaylistButtonVisibility(false);
                    }
                }
            };

    private void toggleClearPlaylistButtonVisibility(boolean showButton) {
        if (clearPlaylistButton != null) {
            clearPlaylistButton.setVisibility(showButton ? View.VISIBLE : View.GONE);
        }
    }

    private View.OnClickListener onClearPlaylistClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MB3Application.getInstance().PlayerQueue.PlaylistItems = new ArrayList<>();
            MB3Application.getAudioService().stopMedia();
            toggleClearPlaylistButtonVisibility(false);
        }
    };

    /**
     * Called in onCreateView. Override this to provide a custom
     * DragSortController.
     */
    public DragSortController buildController(DragSortListView dslv) {
        // defaults are
        //   dragStartMode = onDown
        //   removeMode = flingRight
        DragSortController controller = new DragSortController(dslv);
        controller.setDragHandleId(R.id.ivDragHandle);
//        controller.setClickRemoveId(R.id.click_remove);
        controller.setRemoveEnabled(true);
        controller.setSortEnabled(true);
        controller.setDragInitMode(DragSortController.ON_DOWN);
        controller.setRemoveMode(DragSortController.FLING_REMOVE);
        return controller;
    }
}
