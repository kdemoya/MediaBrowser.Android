package com.mb.android.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.utils.Utils;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;

/**
 * Created by Mark on 2014-09-15.
 *
 * Small adapter that is used when a user has the choice of starting an item from the beginning and resuming from the
 * last playback check-in. The adapter will show the timestamp and an image.
 */
public class ResumeDialogAdapter extends BaseAdapter {

    private long mResumePositionTicks;
    private LayoutInflater inflater;
    private String startImageUrl;
    private String resumeImageUrl;

    public ResumeDialogAdapter(BaseItemDto baseItem) {
        mResumePositionTicks = baseItem.getUserData().getPlaybackPositionTicks();
        inflater = LayoutInflater.from(MB3Application.getInstance());

        if (baseItem.getChapters() == null || baseItem.getChapters().size() < 1) return;

        startImageUrl = buildChapterImageUrl(baseItem.getId(), 0);
        for (int index = baseItem.getChapters().size() - 1; index >= 0; index--) {
            if (baseItem.getChapters().get(index).getStartPositionTicks() <= mResumePositionTicks) {
                resumeImageUrl = buildChapterImageUrl(baseItem.getId(), index);
                break;
            }
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    // ViewHolder warning suppressed because there's only ever going to be two items in the list
    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        convertView = inflater.inflate(R.layout.tv_widget_root_menu_item, parent, false);
        TextView text = (TextView) convertView.findViewById(R.id.tvMenuItemText);
        NetworkImageView image = (NetworkImageView) convertView.findViewById(R.id.ivResumePointImage);
        image.setDefaultImageResId(R.drawable.chapters_extras_image);


        if (position == 0) {
            text.setText(MB3Application.getInstance().getResources().getString(R.string.play_from_beginning_string));
            image.setImageUrl(startImageUrl, MB3Application.getInstance().API.getImageLoader());
        } else {
            text.setText(String.format(MB3Application.getInstance().getResources().getString(R.string.popup_resume), Utils.PlaybackRuntimeFromMilliseconds(mResumePositionTicks / 10000)));
            image.setImageUrl(resumeImageUrl, MB3Application.getInstance().API.getImageLoader());
        }

        return convertView;
    }

    private String buildChapterImageUrl(String itemId, int chapterIndex) {

        ImageOptions options = new ImageOptions();
        options.setImageType(ImageType.Chapter);
        options.setImageIndex(chapterIndex);

        return MB3Application.getInstance().API.GetImageUrl(itemId, options);
    }
}
