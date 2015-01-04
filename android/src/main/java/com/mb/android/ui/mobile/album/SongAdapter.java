package com.mb.android.ui.mobile.album;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.ViewHolderSong;
import com.mb.android.logging.FileLogger;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.extensions.StringHelper;

import java.util.List;

/**
 * Created by Mark on 2014-07-13.
 *
 * Adapter that shows basic info about a group of songs
 */
public class SongAdapter extends BaseAdapter implements SectionIndexer {

    private LayoutInflater li;
    private List<BaseItemDto> mSongs;
    private int mWidth = 80;
    private boolean imageEnhancersEnabled;

    private String mSections = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public SongAdapter(List<BaseItemDto> songs, Context c) {
        mSongs = songs;
        li = c != null
                ? LayoutInflater.from(c)
                : (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        try {
            DisplayMetrics metrics = MB3Application.getInstance().getResources().getDisplayMetrics();
            mWidth = (int)(60 * metrics.density);
        } catch (Exception e) {
            Log.d("SongAdapter", "Error measuring width");
        }
        try {
            imageEnhancersEnabled = PreferenceManager
                    .getDefaultSharedPreferences(MB3Application.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true);
        } catch (Exception e) {
            Log.d("AbstractMediaAdapter", "Error reading preferences");
        }
    }

    public void clearDataset() {
        mSongs = null;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mSongs != null ? mSongs.size() : 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolderSong holder;

        if (convertView == null) {

            convertView = li.inflate(R.layout.widget_music_song_row, parent, false);

            if (convertView == null)
                return null;

            holder = new ViewHolderSong();
            holder.titleText = (TextView) convertView.findViewById(R.id.tvTrackTitle);
            holder.secondaryText = (TextView) convertView.findViewById(R.id.tvTrackArtist);
            holder.imageView = (NetworkImageView) convertView.findViewById(R.id.ivAlbumCover);
            holder.imageView.setDefaultImageResId(R.drawable.music_square_bg);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolderSong) convertView.getTag();
        }

        holder.position = position;
        holder.titleText.setText(mSongs.get(position).getName());

        if (mSongs.get(position).getHasPrimaryImage()) {
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setWidth(mWidth);
            options.setEnableImageEnhancers(imageEnhancersEnabled);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(mSongs.get(position).getId(), options);
            holder.imageView.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        } else if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mSongs.get(position).getAlbumPrimaryImageTag())) {
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setWidth(mWidth);
            options.setEnableImageEnhancers(imageEnhancersEnabled);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(mSongs.get(position).getAlbumId(), options);
            holder.imageView.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        } else {
            holder.imageView.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
        }

        if (position == 6) {
            FileLogger.getFileLogger().Info(new GsonJsonSerializer().SerializeToString(mSongs.get(position)));
        }

        if (mSongs.get(position).getArtists() != null) {
            StringBuilder sb = new StringBuilder();
            for (String artist : mSongs.get(position).getArtists()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(artist);
            }
            holder.secondaryText.setText(sb.toString());
        } else {
            holder.secondaryText.setText("");
        }

        return convertView;
    }

    @Override
    public Object getItem(int position) {

        if (mSongs == null || mSongs.size() == 0 || mSongs.size() <= position) return null;

        return mSongs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public Object[] getSections() {
        String[] sections = new String[mSections.length()];
        for (int i = 0; i < mSections.length(); i++)
            sections[i] = String.valueOf(mSections.charAt(i));
        return sections;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {

        // If there is no item for current section, previous section will be selected
        for (int i = sectionIndex; i >= 0; i--) {
            for (int j = 0; j < getCount(); j++) {
                if (i == 0) {
                    // For numeric section
                    for (int k = 0; k <= 9; k++) {
                        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mSongs.get(j).getName()) && mSongs.get(j).getName().charAt(0) == String.valueOf(k).charAt(0))
                            return j;
                    }
                } else {
                    if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mSongs.get(j).getName()) && mSongs.get(j).getName().charAt(0) == mSections.charAt(i))
                        return j;
                }
            }
        }
        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {
        return 0;
    }
}
