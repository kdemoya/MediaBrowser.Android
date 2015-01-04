package com.mb.android.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.search.SearchHint;
import mediabrowser.model.entities.ImageType;
import com.mb.android.logging.FileLogger;

import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Activity handles acquisition and display of search results.
 */
public class SearchResultsAdapter extends BaseAdapter implements SectionIndexer {

    public boolean isFastScrolling;
    Context mContext;
    List<SearchHint> mBaseItems;
    LayoutInflater li;
    ApiClient mApi;
    SharedPreferences mSharedPreferences;
    int mImageWidth;
    int mImageHeight;
    private boolean imageEnhancersEnabled;
    private String sections_ = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";


    public SearchResultsAdapter(Context context, List<SearchHint> baseItems, ApiClient apiClient) {
        mContext = context;
        mBaseItems = baseItems;
        mApi = apiClient;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        try {
            li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            int columns = mContext.getResources().getInteger(R.integer.library_columns_poster);
            DisplayMetrics dm = mContext.getResources().getDisplayMetrics();

            mImageHeight = mImageWidth = (dm.widthPixels - (columns * (int) (10 * dm.density))) / columns;

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


    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {

            convertView = li.inflate(R.layout.widget_folder_button, parent, false);

            if (convertView == null) return null;

            holder = new ViewHolder();
            holder.titleText = (TextView) convertView.findViewById(R.id.tvFolderButtonTitle);
            holder.secondaryText = (TextView) convertView.findViewById(R.id.tvFolderButtonYear);
            holder.imageView = (NetworkImageView) convertView.findViewById(R.id.ivFolderButtonImage);
            holder.imageHolder = (RelativeLayout) convertView.findViewById(R.id.rlFolderButtonImageHolder);
            holder.isNewOverlay = (ImageView) convertView.findViewById(R.id.ivOverlayImage);
            holder.playedProgress = (ProgressBar) convertView.findViewById(R.id.pbPlaybackProgress);
            holder.playedProgress.setVisibility(View.GONE);

            holder.imageHolder.setLayoutParams(new LinearLayout.LayoutParams(mImageWidth, mImageHeight));

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.position = position;

        String type = mBaseItems.get(position).getType();

        // Set primary text

        if (type.equalsIgnoreCase("Episode") &&
                mBaseItems.get(position).getIndexNumber() != null &&
                mBaseItems.get(position).getParentIndexNumber() != null) {

            String title = "";
            title += String.valueOf(mBaseItems.get(position).getParentIndexNumber()) + ".";

            title += mBaseItems.get(position).getIndexNumber().toString() + " - ";
            title += mBaseItems.get(position).getName();
            holder.titleText.setText(title);
            holder.secondaryText.setText(mBaseItems.get(position).getSeries());

        } else {
            holder.titleText.setText(mBaseItems.get(position).getName());
            holder.secondaryText.setText("");
        }

        // Set tile image

        if (!isFastScrolling) {

            // Download from server
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mBaseItems.get(position).getPrimaryImageTag())) {
                ImageOptions options = new ImageOptions();
                options.setImageType(ImageType.Primary);
                options.setMaxWidth(mImageWidth);
                options.setEnableImageEnhancers(imageEnhancersEnabled);

                holder.imageView.setVisibility(View.VISIBLE);

                String imageUrl = mApi.GetImageUrl(mBaseItems.get(position).getItemId(), options);
                holder.imageView.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());

            // Use placeholder image instead
            } else {
                if (mBaseItems.get(position).getType().equalsIgnoreCase("person"))
                    holder.imageView.setImageResource(R.drawable.default_actor);
                else if (mBaseItems.get(position).getType().equalsIgnoreCase("game")) {
                    holder.imageView.setImageResource(R.drawable.default_game_portrait);
                } else if (mBaseItems.get(position).getType().equalsIgnoreCase("video")) {
                    holder.imageView.setImageResource(R.drawable.default_video_portrait);
                } else if (mBaseItems.get(position).getType().equalsIgnoreCase("musicalbum")) {
                    holder.imageView.setImageResource(R.drawable.music_square_bg);
                } else if (mBaseItems.get(position).getType().equalsIgnoreCase("musicartist")) {
                    holder.imageView.setImageResource(R.drawable.default_artist);
                } else if (mBaseItems.get(position).getType().equalsIgnoreCase("audio")) {
                    holder.imageView.setImageResource(R.drawable.music_square_bg);
                } else {
                    holder.imageView.setVisibility(View.INVISIBLE);
                }
            }
        } else {
            holder.imageView.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }


    public Object getItem(int position) {

        return mBaseItems.get(position);
    }


    public long getItemId(int position) {
        return 0;
    }


    public int getPositionForSection(int section) {
        // If there is no item for current section, previous section will be selected
        for (int i = section; i >= 0; i--) {
            for (int j = 0; j < getCount(); j++) {
                if (i == 0) {
                    // For numeric section
                    for (int k = 0; k <= 9; k++) {
                        if (((SearchHint) getItem(j)).getName().charAt(0) == (char) k)
                            return j;
                    }
                } else {
                    if (((SearchHint) getItem(j)).getName().toUpperCase().charAt(0) == sections_.charAt(i))
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
        public ImageView isNewOverlay;
        public RelativeLayout imageHolder;
        public ProgressBar playedProgress;
        public int position;
    }

}
