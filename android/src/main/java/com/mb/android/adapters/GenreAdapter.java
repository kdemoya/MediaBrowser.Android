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
import mediabrowser.model.extensions.StringHelper;

import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Adapter that displays info from a DTOBaseItem
 */
public class GenreAdapter extends AbstractMediaAdapter {

    public boolean isFastScrolling;
    LayoutInflater li;
    ApiClient mApi;
    int mImageWidth;
    int mImageHeight;


    public GenreAdapter(List<BaseItemDto> baseItems, Context context) {
        super(baseItems);
        mApi = MB3Application.getInstance().API;
        try {
            li = LayoutInflater.from(context);

            DisplayMetrics dm = MB3Application.getInstance().getResources().getDisplayMetrics();

            int columns = MB3Application.getInstance().getResources().getInteger(R.integer.library_columns_poster);

            mImageWidth = (int)((float)dm.widthPixels - (columns * (int)(10 * dm.density))) / columns;

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
//                mImageHeight = (int) (mImageWidth / 0.666666666666667f);
                mImageHeight = (int) (mImageWidth / (combinedAspectRatio / count));

            else
                mImageHeight = (int)(((float)mImageWidth / 16) * 9);

            FileLogger.getFileLogger().Info("mImageWidth: " + String.valueOf(mImageWidth));
            FileLogger.getFileLogger().Info("mImageHeight: " + String.valueOf(mImageHeight));
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

            convertView = li.inflate(R.layout.widget_genre_button, parent, false);

            holder = new ViewHolder();
            holder.titleText = (TextView) convertView.findViewById(R.id.tvFolderButtonTitle);
            holder.imageView = (NetworkImageView) convertView.findViewById(R.id.ivFolderButtonImage);
            holder.watchedCountOverlay = (TextView) convertView.findViewById(R.id.tvOverlay);
            holder.missingEpisodeOverlay = (TextView) convertView.findViewById(R.id.tvMissingGenreOverlay);
            holder.playedProgress = (ProgressBar) convertView.findViewById(R.id.pbPlaybackProgress);

            holder.imageView.setLayoutParams(new RelativeLayout.LayoutParams(mImageWidth, mImageHeight));
            holder.imageView.setDefaultImageResId(R.drawable.blank_music_genre);

            convertView.setTag(holder);
        }

        // Set title text
        holder.titleText.setText(mBaseItems.get(position).getName());

        // Set poster image
        if (!isFastScrolling) {

            if (mBaseItems.get(position).getHasPrimaryImage()) {
                ImageOptions options = new ImageOptions();
                options.setImageType(ImageType.Primary);
                options.setWidth(mImageWidth);
                options.setEnableImageEnhancers(imageEnhancersEnabled);

                String imageUrl = mApi.GetImageUrl(mBaseItems.get(position), options);
                holder.imageView.setVisibility(View.VISIBLE);
                holder.imageView.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());

            } else {
                holder.imageView.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
                if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mBaseItems.get(position).getName())) {
                    holder.missingEpisodeOverlay.setText(mBaseItems.get(position).getName());
                    holder.missingEpisodeOverlay.setVisibility(View.VISIBLE);
                }
            }

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
