package com.mb.android.ui.tv.library.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.mb.android.MB3Application;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.adapters.ResumeDialogAdapter;
import com.mb.android.ui.tv.playback.VideoPlayer;
import mediabrowser.model.dto.BaseItemDto;

import java.util.ArrayList;


public class MediaResumeDialogFragment extends DialogFragment {

    private BaseItemDto mItem;

    public void setItem(BaseItemDto item) {
        mItem = item;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams")
        View layout = inflater.inflate(R.layout.widget_resume_dialog, null);
        ListView list = (ListView) layout.findViewById(R.id.lvResumeSelection);
        list.setAdapter(new ResumeDialogAdapter(mItem));
        list.setOnItemClickListener(onItemClickListener);

        builder.setView(layout);

        return builder.create();
    }


    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            MB3Application.getInstance().PlayerQueue.PlaylistItems = new ArrayList<>();
            PlaylistItem playableItem = new PlaylistItem();
            playableItem.Id = mItem.getId();
            playableItem.Name = mItem.getName();
            playableItem.startPositionTicks = (position == 1 && mItem.getUserData() != null ? mItem.getUserData().getPlaybackPositionTicks() : 0L );
            playableItem.Type = mItem.getType();

            if ("episode".equalsIgnoreCase(mItem.getType()))
                playableItem.SecondaryText = mItem.getSeriesName();

//            if (audioStreamIndex != null) {
//                playableItem.AudioStreamIndex = audioStreamIndex;
//            }
//            if (subtitleStreamIndex != null) {
//                playableItem.SubtitleStreamIndex = subtitleStreamIndex;
//            }
            MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playableItem);
            Intent intent = new Intent(MB3Application.getInstance(), VideoPlayer.class);
            startActivity(intent);
//            MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playableItem);
//            PlayerHelpers.playItem(getActivity(), mItem, position == 1, null, null); // only resumes if the second button is pressed
            MediaResumeDialogFragment.this.dismiss();
        }
    };
}
