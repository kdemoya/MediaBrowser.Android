package com.mb.android.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.logging.FileLogger;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.livetv.RecordingInfoDto;

import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * A BaseAdapter that shows all media that the user has recorded using a live-tv server plugin
 */
public class RecordingsAdapterBackdrops extends BaseAdapter implements SectionIndexer {

    public boolean isFastScrolling;
    List<RecordingInfoDto> mBaseItems;
    LayoutInflater li;
    SharedPreferences mSharedPreferences;
    int mImageWidth;
    int mImageHeight;
    private boolean imageEnhancersEnabled;
    private String sections_ = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";


    public RecordingsAdapterBackdrops(List<RecordingInfoDto> recordings) {
        mBaseItems = recordings;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(MB3Application.getInstance());
        try {
            li = (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            int columns = MB3Application.getInstance().getResources().getInteger(R.integer.library_columns);
            DisplayMetrics dm = MB3Application.getInstance().getResources().getDisplayMetrics();

            mImageWidth = (dm.widthPixels - ((columns * 2) * (int) (4 * dm.density))) / columns;
            mImageHeight = (mImageWidth / 16) * 9;

            imageEnhancersEnabled = PreferenceManager
                    .getDefaultSharedPreferences(MB3Application.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true);

        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("Error in adapter initialization", e);
        }

    }


    public int getCount() {
        return mBaseItems.size();
    }


    @SuppressLint("SimpleDateFormat")
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {

            convertView = li.inflate(R.layout.widget_library_tile_large, parent, false);

            holder = new ViewHolder();
            holder.titleText = (TextView) convertView.findViewById(R.id.tvLibraryTileTitle);
            holder.secondaryText = (TextView) convertView.findViewById(R.id.tvLibraryTileSubTitle);
            holder.imageView = (NetworkImageView) convertView.findViewById(R.id.ivLibraryTilePrimaryImage);
            holder.overlay = (TextView) convertView.findViewById(R.id.tvOverlay);
            holder.playedProgress = (ProgressBar) convertView.findViewById(R.id.pbPlaybackProgress);
            holder.missingEpisodeOverlay = (TextView) convertView.findViewById(R.id.tvMissingEpisodeOverlay);

            holder.imageView.setLayoutParams(new RelativeLayout.LayoutParams(mImageWidth, mImageHeight));

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.position = position;

        // Set primary text
        holder.titleText.setText(mBaseItems.get(position).getName());

        // Set Secondary text
        holder.secondaryText.setText(mBaseItems.get(position).getEpisodeTitle() != null && !mBaseItems.get(position).getEpisodeTitle().isEmpty() ? mBaseItems.get(position).getEpisodeTitle() : "");

        // Set tile image

        if (!isFastScrolling) {

            ImageOptions options = null;
            String imageUrl = null;

            if (mBaseItems.get(position).getImageTags() != null) {
                if (mBaseItems.get(position).getHasPrimaryImage()) {

                    options = new ImageOptions();
                    options.setImageType(ImageType.Primary);
                    options.setMaxWidth(mImageWidth);
                    options.setMaxHeight(mImageHeight);
                    options.setEnableImageEnhancers(imageEnhancersEnabled);
                    imageUrl = MB3Application.getInstance().API.GetImageUrl(mBaseItems.get(position).getId(), options);

                } else if (mBaseItems.get(position).getImageTags().containsKey(ImageType.Thumb)) {

                    options = new ImageOptions();
                    options.setImageType(ImageType.Thumb);
                    options.setMaxWidth(mImageWidth);
                    options.setMaxHeight(mImageHeight);
                    options.setEnableImageEnhancers(imageEnhancersEnabled);
                    imageUrl = MB3Application.getInstance().API.GetImageUrl(mBaseItems.get(position).getId(), options);
                }
            }

            if (options != null) {
                // TODO Do something about this for scaling, place in values

                /*
                    There usually is a better method than invoking DisplayMetrics. In this situation where we
                    want exact pixels, is it not better to go this route? Only issue I see right away is that
                    we're not accounting for changes in widget_library_tile padding values.
                */

                holder.imageView.setVisibility(View.VISIBLE);
                holder.imageView.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            } else {
                holder.imageView.setVisibility(View.INVISIBLE);
            }
        } else {
            holder.imageView.setVisibility(View.INVISIBLE);
        }

        try {
            // Show the percentage watched for media
            if (mBaseItems.get(position).getUserData() != null && mBaseItems.get(position).getUserData().getPlaybackPositionTicks() > 1) {

                holder.playedProgress.setVisibility(View.VISIBLE);
                holder.playedProgress.setMax(100);
                double percentWatched = (double) mBaseItems.get(position).getUserData().getPlaybackPositionTicks() / (double) mBaseItems.get(position).getRunTimeTicks();
                int roundedValue = (int) (percentWatched * 100);
                holder.playedProgress.setProgress(roundedValue);

            } else {
                holder.playedProgress.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            holder.playedProgress.setVisibility(View.INVISIBLE);
            FileLogger.getFileLogger().ErrorException("Error setting progressbar value", e);
        }

        return convertView;
    }


    public Object getItem(int position) {

        return mBaseItems.get(position);
    }


    public long getItemId(int position) {
        return 0;
    }


    @SuppressLint("DefaultLocale")
    public int getPositionForSection(int section) {
        // If there is no item for current section, previous section will be selected
        for (int i = section; i >= 0; i--) {
            for (int j = 0; j < getCount(); j++) {
                if (i == 0) {
                    // For numeric section
                    for (int k = 0; k <= 9; k++) {
                        if (((BaseItemDto) getItem(j)).getSortName().charAt(0) == (char) k)
                            return j;
                    }
                } else {
                    if (((BaseItemDto) getItem(j)).getSortName().toUpperCase().charAt(0) == sections_.charAt(i))
                        return j;
                }
            }
        }
        return 0;
    }


    public int getSectionForPosition(int position) {
        return 0;
    }


    public Object[] getSections() {
        String[] sections = new String[sections_.length()];
        for (int i = 0; i < sections_.length(); i++)
            sections[i] = String.valueOf(sections_.charAt(i));
        return sections;
    }


    public class ViewHolder {

        public NetworkImageView imageView;
        public TextView titleText;
        public TextView secondaryText;
        public TextView overlay;
        public ProgressBar playedProgress;
        public TextView missingEpisodeOverlay;
        public int position;
    }
}

