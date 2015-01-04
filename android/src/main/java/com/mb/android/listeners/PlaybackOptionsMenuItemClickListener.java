package com.mb.android.listeners;

import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

import com.mb.android.DialogFragments.AudioStreamSelectionDialogFragment;
import com.mb.android.DialogFragments.BitrateSelectionDialogFragment;
import com.mb.android.DialogFragments.SubtitleStreamSelectionDialogFragment;
import com.mb.android.R;
import com.mb.android.ui.mobile.playback.PlaybackActivity;

/**
 * Created by Mark on 2014-07-24.
 *
 * Listener that responds to individual item clicks in the hamburger menu of the media playback activity
 */
public class PlaybackOptionsMenuItemClickListener implements android.support.v7.widget.PopupMenu.OnMenuItemClickListener {

    private FragmentActivity mActivity;
    private PlaybackActivity mPlaybackActivity;

    public PlaybackOptionsMenuItemClickListener(FragmentActivity activity, PlaybackActivity playbackActivity) {
        mActivity = activity;
        mPlaybackActivity = playbackActivity;
    }
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        String menuTitle = menuItem.getTitle().toString();

        if (mActivity.getResources().getString(R.string.bitrate_selection).equalsIgnoreCase(menuTitle)) {
            BitrateSelectionDialogFragment df = new BitrateSelectionDialogFragment();
            df.show(mActivity.getSupportFragmentManager(), "BitrateSelection");
        } else if (mActivity.getResources().getString(R.string.audio_stream_selection).equalsIgnoreCase(menuTitle)) {
            AudioStreamSelectionDialogFragment df = new AudioStreamSelectionDialogFragment();
            df.show(mActivity.getSupportFragmentManager(), "AudioStreamSelection");
        } else if (mActivity.getResources().getString(R.string.subtitle_stream_selection).equalsIgnoreCase(menuTitle)) {
            SubtitleStreamSelectionDialogFragment df = new SubtitleStreamSelectionDialogFragment();
            df.show(mActivity.getSupportFragmentManager(), "SubtitleStreamSelection");
        }

        return false;
    }
}
