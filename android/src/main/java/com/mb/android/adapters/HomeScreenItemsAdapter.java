package com.mb.android.adapters;

import android.content.Context;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import com.mb.android.logging.FileLogger;

/**
 * Created by Mark on 12/12/13.
 *
 * Adapter that will show subsets of the users library in the various tabs of the home screen
 */
public class HomeScreenItemsAdapter extends BaseAdapter {

    private BaseItemDto[] mItems;
    private LayoutInflater mLayoutInflater;
    private int mWidth;
    private int mHeight;
    private boolean imageEnhancersEnabled;

    public HomeScreenItemsAdapter(BaseItemDto[] items) {
        mItems = items;

        try {
            mLayoutInflater = (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            int mScreenWidth = MB3Application.getInstance().getResources().getDisplayMetrics().widthPixels;

            int mColumns = MB3Application.getInstance().getResources().getInteger(R.integer.homescreen_item_columns);

            mWidth = (mScreenWidth - ((mColumns * 2) * (int) (4 * MB3Application.getInstance().getResources().getDisplayMetrics().density))) / mColumns;
            mHeight = (mWidth / 16) * 9;

            imageEnhancersEnabled = PreferenceManager
                    .getDefaultSharedPreferences(MB3Application.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true);
        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("error initializing adapter properties", e);
        }
    }

    public void addNewDataset(BaseItemDto[] items) {
        mItems = items;
    }

    @Override
    public int getCount() {
        return mItems.length;
    }

    @Override
    public Object getItem(int i) {
        return mItems[i];
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {

        ViewHolder holder;

        if (convertView == null) {

            convertView = mLayoutInflater.inflate(R.layout.widget_library_tile_large, viewGroup, false);

            holder = new ViewHolder();

            if (convertView != null) {

                holder.CollectionImage = (NetworkImageView) convertView.findViewById(R.id.ivLibraryTilePrimaryImage);
                holder.CollectionName = (TextView) convertView.findViewById(R.id.tvLibraryTileTitle);
                holder.CollectionSecondaryText = (TextView) convertView.findViewById(R.id.tvLibraryTileSubTitle);
                holder.CollectionProgress = (ProgressBar) convertView.findViewById(R.id.pbPlaybackProgress);
                holder.UnwatchedCount = (TextView) convertView.findViewById(R.id.tvOverlay);

                holder.CollectionImage.setLayoutParams(new RelativeLayout.LayoutParams(mWidth, mHeight));
                holder.CollectionImage.setDefaultImageResId(R.drawable.default_video_landscape);

                convertView.setTag(holder);
            }

        } else {
            holder = (ViewHolder) convertView.getTag();
            holder.UnwatchedCount.setVisibility(View.INVISIBLE);
        }

        if (mItems[i].getType().equalsIgnoreCase("episode") &&
                mItems[i].getParentIndexNumber() != null &&
                mItems[i].getIndexNumber() != null) {
            try {
                holder.CollectionName.setText(
                        String.valueOf(mItems[i].getParentIndexNumber()) + "." +
                                String.valueOf(mItems[i].getIndexNumber()) + " - " +
                                mItems[i].getName()
                );
            } catch (Exception e) {
                FileLogger.getFileLogger().ErrorException("Error setting episode text", e);
                holder.CollectionName.setText(mItems[i].getName());
            }
        } else {
            holder.CollectionName.setText(mItems[i].getName());
        }

        if (mItems[i].getType().equalsIgnoreCase("episode") || mItems[i].getType().equalsIgnoreCase("season"))
            holder.CollectionSecondaryText.setText(mItems[i].getSeriesName());
        else if (mItems[i].getProductionYear() != null && mItems[i].getProductionYear() != 0)
            holder.CollectionSecondaryText.setText(String.valueOf(mItems[i].getProductionYear()));
        else
            holder.CollectionSecondaryText.setText("");

        if (mItems[i].getType() != null && (mItems[i].getType().equalsIgnoreCase("series")
                || mItems[i].getType().equalsIgnoreCase("season") || mItems[i].getType().equalsIgnoreCase("boxset"))) {
            if (mItems[i].getUserData() != null && mItems[i].getUserData().getUnplayedItemCount() != null && mItems[i].getUserData().getUnplayedItemCount() > 0) {
                holder.UnwatchedCount.setText(String.valueOf(mItems[i].getUserData().getUnplayedItemCount()));
                holder.UnwatchedCount.setVisibility(View.VISIBLE);
            }
        }

        if (mItems[i].getHasThumb()) {
            holder.CollectionName.setVisibility(View.INVISIBLE);
            holder.CollectionSecondaryText.setVisibility(View.INVISIBLE);
        } else {
            holder.CollectionName.setVisibility(View.VISIBLE);
            holder.CollectionSecondaryText.setVisibility(View.VISIBLE);
        }

        try {

            String imageUrl = "";
            ImageOptions options = new ImageOptions();

            if ((mItems[i].getType().equalsIgnoreCase("episode") || mItems[i].getType().equalsIgnoreCase("photo")) && mItems[i].getHasPrimaryImage()) {
                options.setImageType(ImageType.Primary);
                options.setEnableImageEnhancers(imageEnhancersEnabled);

                if (mItems[i].getPrimaryImageAspectRatio() > 1.777) {
                    options.setHeight(mHeight);
                } else {
                    options.setWidth(mWidth);
                }

                imageUrl = MB3Application.getInstance().API.GetImageUrl(mItems[i], options);
            } else if (mItems[i].getHasThumb()) {
                options.setImageType(ImageType.Thumb);
                options.setWidth(mWidth);
                options.setEnableImageEnhancers(imageEnhancersEnabled);
                imageUrl = MB3Application.getInstance().API.GetImageUrl(mItems[i], options);
            } else if (mItems[i].getBackdropCount() > 0) {
                options.setImageType(ImageType.Backdrop);
                options.setWidth(mWidth);
                imageUrl = MB3Application.getInstance().API.GetImageUrl(mItems[i], options);
            } else if (mItems[i].getParentBackdropImageTags() != null && mItems[i].getParentBackdropImageTags().size() > 0) {
                options.setImageType(ImageType.Backdrop);
                options.setWidth(mWidth);
                imageUrl = MB3Application.getInstance().API.GetImageUrl(mItems[i].getParentBackdropItemId(), options);
            } else if (mItems[i].getHasPrimaryImage()) {
                options.setImageType(ImageType.Primary);
                options.setEnableImageEnhancers(imageEnhancersEnabled);

                if (mItems[i].getPrimaryImageAspectRatio() != null && mItems[i].getPrimaryImageAspectRatio() > 1.777) {
                    options.setHeight(mHeight);
                } else {
                    options.setWidth(mWidth);
                }
                imageUrl = MB3Application.getInstance().API.GetImageUrl(mItems[i], options);
            }

            if (options.getImageType() != null) {
                holder.CollectionImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            } else {
                holder.CollectionImage.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
            }

        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("Error setting image: ", e);
        }


        try {
            if (mItems[i].getUserData() != null && mItems[i].getUserData().getPlaybackPositionTicks() > 0) {
                holder.CollectionProgress.setVisibility(View.VISIBLE);
                holder.CollectionProgress.setMax(100);
                double percentWatched = (double) mItems[i].getUserData().getPlaybackPositionTicks() / (double) mItems[i].getRunTimeTicks();
                int roundedValue = (int) (percentWatched * 100);
                holder.CollectionProgress.setProgress(roundedValue);
            } else {
                holder.CollectionProgress.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            holder.CollectionProgress.setVisibility(View.INVISIBLE);
            FileLogger.getFileLogger().ErrorException("Error setting progressbar value", e);
        }

        return convertView;
    }

    private class ViewHolder {
        NetworkImageView CollectionImage;
        TextView CollectionName;
        TextView CollectionSecondaryText;
        TextView UnwatchedCount;
        ProgressBar CollectionProgress;
    }
}
