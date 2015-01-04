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

import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.utils.Utils;
import mediabrowser.apiinteraction.ApiClient;
import com.mb.android.logging.FileLogger;
import com.mb.android.widget.AnimatedNetworkImageView;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.LocationType;

import java.util.Date;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * BaseAdapter that shows media. The image preference will be Primary(if Episode)->Thumb->Backdrop.
 * Perhaps the class should be renamed.
 */
public class HorizontalAdapterBackdrops extends BaseAdapter implements SectionIndexer {

    Context mContext;
    List<BaseItemDto> mBaseItems;
    LayoutInflater li;
    ApiClient mApi;
    SharedPreferences mSharedPreferences;
    int mImageWidth;
    int mImageHeight;
//    private Animation fadeIn;
    private Integer mDefaultImageId;
    private boolean imageEnhancersEnabled;
    private String sections_ = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";


    public HorizontalAdapterBackdrops(Context context, List<BaseItemDto> baseItems, int gridHeight, int rows, Integer defaultImageId) {
        mContext = context;
        mBaseItems = baseItems;
        mApi = MB3Application.getInstance().API;
        mDefaultImageId = defaultImageId;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        try {
            li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            DisplayMetrics dm = mContext.getResources().getDisplayMetrics();

            mImageHeight = (gridHeight - (rows * (int)(8 * dm.density))) / rows;
            mImageWidth = (mImageHeight / 9) * 16;

            imageEnhancersEnabled = PreferenceManager
                    .getDefaultSharedPreferences(MB3Application.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true);

        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("Error in adapter initialization", e);
        }
    }

    public void addItems(List<BaseItemDto> items) {
        mBaseItems.addAll(items);
        notifyDataSetChanged();
    }


    public int getCount() {
        return mBaseItems.size();
    }


    @SuppressLint("SimpleDateFormat")
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {

            convertView = li.inflate(R.layout.widget_horizontal_library_tile_large, parent, false);

            holder = new ViewHolder();
            holder.imageView = (AnimatedNetworkImageView) convertView.findViewById(R.id.ivLibraryTilePrimaryImage);
            holder.overlay = (TextView) convertView.findViewById(R.id.tvOverlay);
            holder.playedProgress = (ProgressBar) convertView.findViewById(R.id.pbPlaybackProgress);
            holder.missingEpisodeOverlay = (TextView) convertView.findViewById(R.id.tvMissingEpisodeOverlay);
            holder.imageView.setLayoutParams(new RelativeLayout.LayoutParams(mImageWidth, mImageHeight));
            if (mDefaultImageId != null) {
                holder.imageView.setDefaultImageResId(mDefaultImageId);
            }

//            holder.imageView.setAnimation(fadeIn);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.position = position;

        String type = mBaseItems.get(position).getType();

        // Set tile image

        ImageOptions options = null;
        String imageUrl = null;

        if (!type.equalsIgnoreCase("Episode") && mBaseItems.get(position).getHasThumb()) {
            options = new ImageOptions();
            options.setImageType(ImageType.Thumb);
            options.setWidth(mImageWidth);
            options.setMaxHeight(mImageHeight);
            options.setEnableImageEnhancers(imageEnhancersEnabled);
            imageUrl = mApi.GetImageUrl(mBaseItems.get(position).getId(), options);

        } else if (!type.equalsIgnoreCase("Episode") && mBaseItems.get(position).getBackdropCount() > 0) {
            options = new ImageOptions();
            options.setImageType(ImageType.Backdrop);
            options.setWidth(mImageWidth);
            options.setMaxHeight(mImageHeight);
            options.setEnableImageEnhancers(imageEnhancersEnabled);
            options.setImageIndex(0);
            imageUrl = mApi.GetImageUrl(mBaseItems.get(position), options);

        } else if (mBaseItems.get(position).getHasPrimaryImage()) {
            options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setWidth(mImageWidth);
            options.setMaxHeight(mImageHeight);
            options.setEnableImageEnhancers(imageEnhancersEnabled);
            imageUrl = mApi.GetImageUrl(mBaseItems.get(position), options);

        } else if (mBaseItems.get(position).getType().equalsIgnoreCase("episode") &&
                mBaseItems.get(position).getParentThumbItemId() != null) {
            options = new ImageOptions();
            options.setImageType(ImageType.Thumb);
            options.setWidth(mImageWidth);
            options.setMaxHeight(mImageHeight);
            options.setEnableImageEnhancers(imageEnhancersEnabled);
            imageUrl = mApi.GetImageUrl(mBaseItems.get(position).getParentThumbItemId(), options);

        } else if (mBaseItems.get(position).getHasThumb()) {
            options = new ImageOptions();
            options.setImageType(ImageType.Thumb);
            options.setWidth(mImageWidth);
            options.setMaxHeight(mImageHeight);
            options.setEnableImageEnhancers(imageEnhancersEnabled);
            imageUrl = mApi.GetImageUrl(mBaseItems.get(position).getId(), options);

        } else {
            holder.imageView.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
        }

        if (options != null) {
            holder.imageView.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        }

        // Process top-right overlays

        if (mBaseItems.get(position).getLocationType().equals(LocationType.Virtual) && mBaseItems.get(position).getType().equalsIgnoreCase("episode")) {
            holder.overlay.setVisibility(View.INVISIBLE);

            if (mBaseItems.get(position).getPremiereDate() != null) {

                Date premiereDate = Utils.convertToLocalDate(mBaseItems.get(position).getPremiereDate());

                long premiereDateMs = premiereDate.getTime();
                long currentMs = new Date().getTime();

                if (premiereDateMs - currentMs > 0)
                    holder.missingEpisodeOverlay.setText(MB3Application.getInstance().getResources().getString(R.string.un_aired_overlay));
            }

            holder.missingEpisodeOverlay.setVisibility(TextView.VISIBLE);
        } else {
            holder.missingEpisodeOverlay.setVisibility(TextView.GONE);
            if (mBaseItems.get(position).getType().equalsIgnoreCase("boxset") ||
                    mBaseItems.get(position).getType().equalsIgnoreCase("season") ||
                    mBaseItems.get(position).getType().equalsIgnoreCase("series")) {
                if (mBaseItems.get(position).getUserData().getUnplayedItemCount() != null && mBaseItems.get(position).getUserData().getUnplayedItemCount() > 0) {
                    holder.overlay.setText(String.valueOf(mBaseItems.get(position).getUserData().getUnplayedItemCount()));
                    holder.overlay.setVisibility(View.VISIBLE);
                } else if (mBaseItems.get(position).getUserData().getUnplayedItemCount() != null && mBaseItems.get(position).getUserData().getUnplayedItemCount() == 0) {
                    holder.overlay.setText("\u2714");
                    holder.overlay.setVisibility(View.VISIBLE);
                } else {
                    holder.overlay.setVisibility(View.INVISIBLE);
                }
            } else {
                boolean watched = false;

                if (mBaseItems.get(position).getUserData() != null) {
                    watched = mBaseItems.get(position).getUserData().getPlayed();
                }

                if (watched) {
                    holder.overlay.setText("\u2714");
                    holder.overlay.setVisibility(View.VISIBLE);
                } else {
                    holder.overlay.setVisibility(View.INVISIBLE);
                }
            }
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

        public AnimatedNetworkImageView imageView;
        public TextView overlay;
        public ProgressBar playedProgress;
        public TextView missingEpisodeOverlay;
        public int position;
    }

}

