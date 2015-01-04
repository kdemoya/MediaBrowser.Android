package com.mb.android.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import mediabrowser.apiinteraction.ApiClient;
import com.mb.android.logging.FileLogger;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.extensions.StringHelper;

import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Adapter that displays info from a DTOBaseItem
 */
public class GenericAdapterPosters extends AbstractMediaAdapter {

    LayoutInflater li;
    ApiClient mApi;
    int mImageWidth;
    int mImageHeight;
    Integer mDefaultImageResId;


    public GenericAdapterPosters(List<BaseItemDto> baseItems, int columns, Context context, Integer defaultImageResId) {
        super(baseItems);
        mApi = MB3Application.getInstance().API;
        mDefaultImageResId = defaultImageResId;
        try {
            li = LayoutInflater.from(context);

            DisplayMetrics dm = MB3Application.getInstance().getResources().getDisplayMetrics();

            mImageWidth = (int)((float)dm.widthPixels - (columns * (int)(18 * dm.density))) / columns;

            int count = 0;
            double combinedAspectRatio = 0;

            for (BaseItemDto item : baseItems) {
                if (item.getHasPrimaryImage() && item.getPrimaryImageAspectRatio() != null && item.getPrimaryImageAspectRatio() > 0) {
                    FileLogger.getFileLogger().Info("PrimaryImageAspectRation: " + String.valueOf(item.getPrimaryImageAspectRatio()));
                    FileLogger.getFileLogger().Info("OriginalPrimaryImageAspectRation: " + String.valueOf(item.getOriginalPrimaryImageAspectRatio()));
                    combinedAspectRatio += item.getPrimaryImageAspectRatio();
                    count++;
                }

                if (count == 5) {
                    break;
                }
            }

            if (combinedAspectRatio > 0)
                mImageHeight = (int) (mImageWidth / (combinedAspectRatio / count));
            else
                mImageHeight = (int)(((float)mImageWidth / 16) * 9);

        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("Error in adapter initialization", e);
        }
    }

    @Override
    public int getCount() {
        return mBaseItems != null ? mBaseItems.size() : 0;
    }


    @SuppressLint("SimpleDateFormat")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView != null) {

            holder = (ViewHolder) convertView.getTag();
            holder.playedProgress.setVisibility(View.INVISIBLE);
            holder.watchedCountOverlay.setVisibility(View.INVISIBLE);
            holder.missingEpisodeOverlay.setVisibility(View.INVISIBLE);

        } else {

            convertView = li.inflate(R.layout.tv_widget_folder_button, parent, false);

            if (convertView == null) return null;

            holder = new ViewHolder();
            holder.titleText = (TextView) convertView.findViewById(R.id.tvFolderButtonTitle);
            holder.episodeTitleText = (TextView) convertView.findViewById(R.id.tvFolderButtonSeriesTitle);
            holder.imageHolder = (RelativeLayout) convertView.findViewById(R.id.rlFolderButtonImageHolder);
            holder.watchedCountOverlay = (TextView) convertView.findViewById(R.id.tvOverlay);
            holder.missingEpisodeOverlay = (TextView) convertView.findViewById(R.id.tvMissingEpisodeOverlay);
            holder.playedProgress = (ProgressBar) convertView.findViewById(R.id.pbPlaybackProgress);
            holder.imageView = (NetworkImageView) convertView.findViewById(R.id.ivFolderButtonImage);
            holder.imageView.setLayoutParams(new RelativeLayout.LayoutParams(mImageWidth, mImageHeight));

            if (mDefaultImageResId != null) {
                holder.imageView.setDefaultImageResId(mDefaultImageResId);
            }

            convertView.setTag(holder);
        }

        String type = mBaseItems.get(position).getType();

        if (type.equalsIgnoreCase("episode")) {
            // Set title text & episode title text
            if (mBaseItems.get(position).getSeriesName() != null) {
                holder.titleText.setText(mBaseItems.get(position).getSeriesName());
            }

            String title = "";

            if (mBaseItems.get(position).getParentIndexNumber() != null) {
                title += "S" + String.valueOf(mBaseItems.get(position).getParentIndexNumber());
            }
            if (mBaseItems.get(position).getIndexNumber() != null) {
                if (!title.isEmpty()) {
                    title +=", ";
                }
                title += "E" + String.valueOf(mBaseItems.get(position).getIndexNumber());

                if (mBaseItems.get(position).getIndexNumberEnd() != null && !mBaseItems.get(position).getIndexNumber().equals(mBaseItems.get(position).getIndexNumberEnd())) {
                    title += "-" + String.valueOf(mBaseItems.get(position).getIndexNumberEnd());
                }
            }

            if (!title.isEmpty()) {
                title += " - ";
            }
            title += mBaseItems.get(position).getName();

            holder.episodeTitleText.setText(title);
            holder.episodeTitleText.setVisibility(View.VISIBLE);

        } else {
            // Set title text
            holder.titleText.setText(mBaseItems.get(position).getName());

            if (type.equalsIgnoreCase("MusicAlbum")) {

                holder.episodeTitleText.setText(mBaseItems.get(position).getAlbumArtist());
                holder.episodeTitleText.setVisibility(View.VISIBLE);
            } else {
                holder.episodeTitleText.setVisibility(View.GONE);
            }
        }

        // Set poster image
        String imageUrl = "";

        ImageOptions options = new ImageOptions();
        options.setWidth(mImageWidth);

        if (mBaseItems.get(position).getHasThumb()) {
            options.setImageType(ImageType.Thumb);
            options.setEnableImageEnhancers(imageEnhancersEnabled);
            imageUrl = mApi.GetImageUrl(mBaseItems.get(position), options);
        } else if (mBaseItems.get(position).getHasPrimaryImage()) {
            options.setImageType(ImageType.Primary);
            options.setEnableImageEnhancers(imageEnhancersEnabled);
            imageUrl = mApi.GetImageUrl(mBaseItems.get(position), options);
        } else if (mBaseItems.get(position).getType().equalsIgnoreCase("episode")
                && mBaseItems.get(position).getParentThumbItemId() != null) {
            options.setImageType(ImageType.Thumb);
            options.setEnableImageEnhancers(imageEnhancersEnabled);
            imageUrl = mApi.GetImageUrl(mBaseItems.get(position).getParentThumbItemId(), options);
        } else if (mDefaultImageResId != null) {
            holder.imageView.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
        }

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(imageUrl)) {
            holder.imageView.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        }

        // Process top-right overlays
        if (type.equalsIgnoreCase("episode") &&
                mBaseItems.get(position).getLocationType().equals(LocationType.Virtual)) {
            setMissingOrUnairedEpisodeState(holder, mBaseItems.get(position));
        } else {
            setWatchedState(holder, mBaseItems.get(position));
        }

        return convertView;
    }

    @Override
    public Object getItem(int position) {

        if (mBaseItems == null
                || mBaseItems.size() == 0
                || mBaseItems.size() <= position) {
            return null;
        }

        return mBaseItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
