package com.mb.android.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
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
import com.mb.android.utils.Utils;
import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.LocationType;
import com.mb.android.logging.FileLogger;

import java.util.Date;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Adapter that displays info from a DTOBaseItem
 */
public class MediaAdapterPosters extends BaseAdapter implements SectionIndexer {

    List<BaseItemDto> mBaseItems;
    LayoutInflater li;
    ApiClient mApi;
    SharedPreferences mSharedPreferences;
    int mImageWidth;
    int mImageHeight;
    Integer mDefaultImageId;
    private boolean imageEnhancersEnabled;
    private String sections_ = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";


    public MediaAdapterPosters(List<BaseItemDto> baseItems, int columns, ApiClient apiClient, Integer defaultImageId) {
        mBaseItems = baseItems;
        mApi = apiClient;
        mDefaultImageId = defaultImageId;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(MB3Application.getInstance());
        try {
            li = (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            DisplayMetrics dm = MB3Application.getInstance().getResources().getDisplayMetrics();

            mImageWidth = (dm.widthPixels - (columns * (int) (10 * dm.density))) / columns;

            int count = 0;
            double combinedAspectRatio = 0;

            for (BaseItemDto item : baseItems) {
                if (item.getHasPrimaryImage() && item.getPrimaryImageAspectRatio() != null && item.getPrimaryImageAspectRatio() > 0) {
                    combinedAspectRatio += item.getPrimaryImageAspectRatio();
                    count++;
                }

                if (count == 5) {
                    break;
                }
            }

            if (combinedAspectRatio > 0) {
                mImageHeight = (int) (mImageWidth / (combinedAspectRatio / count));
                Log.d("CAR","greater than 0");
            } else {
                mImageHeight = (int)Math.round(mImageWidth * 1.5);
            }
            Log.d("Height", String.valueOf(mImageHeight));
            Log.d("Width", String.valueOf(mImageWidth));

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

            convertView = li.inflate(R.layout.widget_folder_button, parent, false);

            if (convertView == null) return null;

            holder = new ViewHolder();
            holder.titleText = (TextView) convertView.findViewById(R.id.tvFolderButtonTitle);
            holder.secondaryText = (TextView) convertView.findViewById(R.id.tvFolderButtonYear);
            holder.imageView = (NetworkImageView) convertView.findViewById(R.id.ivFolderButtonImage);
            holder.isNewOverlay = (TextView) convertView.findViewById(R.id.tvOverlay);
            holder.missingEpisodeOverlay = (TextView) convertView.findViewById(R.id.tvMissingEpisodeOverlay);
            holder.playedProgress = (ProgressBar) convertView.findViewById(R.id.pbPlaybackProgress);

            holder.imageView.setLayoutParams(new RelativeLayout.LayoutParams(mImageWidth, mImageHeight));

            if (mDefaultImageId != null) {
                holder.imageView.setDefaultImageResId(mDefaultImageId);
            }

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.position = position;

        String type = mBaseItems.get(position).getType();

        // Set primary text

        if (type.equalsIgnoreCase("Episode") && mBaseItems.get(position).getIndexNumber() != null) {

            if (mBaseItems.get(position).getAirsBeforeSeasonNumber() != null ||
                    mBaseItems.get(position).getAirsAfterSeasonNumber() != null ||
                    mBaseItems.get(position).getAirsBeforeEpisodeNumber() != null) {

                holder.titleText.setText(mBaseItems.get(position).getName());

            } else {
                String title = mBaseItems.get(position).getIndexNumber().toString();
                if (mBaseItems.get(position).getIndexNumberEnd() != null && !mBaseItems.get(position).getIndexNumberEnd().equals(mBaseItems.get(position).getIndexNumber()))
                    title += " - " + mBaseItems.get(position).getIndexNumberEnd() + ". ";
                else
                    title += ". ";
                title += mBaseItems.get(position).getName();
                holder.titleText.setText(title);
            }

        } else {
            holder.titleText.setText(mBaseItems.get(position).getName());
        }

        // Set Secondary text

        if (type.equalsIgnoreCase("Boxset") || type.equalsIgnoreCase("Folder") || type.equalsIgnoreCase("Season") || type.equalsIgnoreCase("PhotoFolder")) {

            holder.secondaryText.setText(String.valueOf(mBaseItems.get(position).getChildCount()) + " Items");

        } else {

            if (mBaseItems.get(position).getProductionYear() != null && mBaseItems.get(position).getProductionYear() != 0) {
                holder.secondaryText.setText(String.valueOf(mBaseItems.get(position).getProductionYear()));
            }
        }

        // Set poster image
        String imageUrl = "";

        ImageOptions options = null;

        if (mBaseItems.get(position).getHasPrimaryImage()) {
            options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setWidth(mImageWidth);
            options.setEnableImageEnhancers(imageEnhancersEnabled);
            imageUrl = mApi.GetImageUrl(mBaseItems.get(position), options);

        } else if (mBaseItems.get(position).getType().equalsIgnoreCase("episode")
                && mBaseItems.get(position).getParentThumbItemId() != null) {
            options = new ImageOptions();
            options.setImageType(ImageType.Thumb);
            options.setMaxWidth(mImageWidth);
            options.setEnableImageEnhancers(imageEnhancersEnabled);
            imageUrl = mApi.GetImageUrl(mBaseItems.get(position).getParentThumbItemId(), options);
        } else if (mDefaultImageId != null) {
            holder.imageView.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
        }

        if (options != null) {
            holder.imageView.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        }

        // Process top-right overlays
        if (mBaseItems.get(position).getLocationType().equals(LocationType.Virtual) && mBaseItems.get(position).getType().equalsIgnoreCase("episode")) {
            holder.isNewOverlay.setVisibility(View.INVISIBLE);

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
                    holder.isNewOverlay.setText(String.valueOf(mBaseItems.get(position).getUserData().getUnplayedItemCount()));
                    holder.isNewOverlay.setVisibility(View.VISIBLE);
                } else if (mBaseItems.get(position).getUserData().getUnplayedItemCount() != null && mBaseItems.get(position).getUserData().getUnplayedItemCount() == 0) {
                    holder.isNewOverlay.setText("\u2714");
                    holder.isNewOverlay.setVisibility(View.VISIBLE);
                } else {
                    holder.isNewOverlay.setVisibility(View.GONE);
                }
            } else {
                new AsyncProcessOverlay(holder.isNewOverlay, mBaseItems.get(position)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }


        try {

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
        public TextView isNewOverlay;
        public ProgressBar playedProgress;
        public TextView missingEpisodeOverlay;
        public int position;
    }


    public class AsyncProcessOverlay extends AsyncTask<Void, Void, String> {

        private TextView mTextView;
        private BaseItemDto mItem;

        public AsyncProcessOverlay(TextView textView, BaseItemDto item) {
            mTextView = textView;
            mItem = item;
        }

        @Override
        protected String doInBackground(Void... voids) {

            boolean isNew = false;

            if (mItem.getDateCreated() != null) {

                Date createdDate = Utils.convertToLocalDate(mItem.getDateCreated());

                long createdMilliseconds = createdDate.getTime();
                long currentMilliseconds = new Date().getTime();

                if ((currentMilliseconds - createdMilliseconds) < (10 * 86400000)) {
                    isNew = true;
                }
            }

            if (mItem.getUserData() != null) {

                if (mItem.getUserData().getPlayed()) {
                    return "watched";
                } else {
                    return "unwatched";
                }
            }
            return "";
        }

        @Override
        protected void onPostExecute(String string) {


            if (string.equalsIgnoreCase("watched")) {

                mTextView.setText("\u2714");
                mTextView.setVisibility(View.VISIBLE);

            } else {

                mTextView.setVisibility(View.INVISIBLE);
            }

        }
    }

}
