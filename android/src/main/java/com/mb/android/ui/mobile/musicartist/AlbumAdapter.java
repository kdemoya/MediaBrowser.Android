package com.mb.android.ui.mobile.musicartist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.PreferenceManager;
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
import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;

import java.util.List;

/**
 * Created by Mark on 12/12/13.
 */
public class AlbumAdapter extends BaseAdapter implements SectionIndexer {

    Context mContext;
    List<BaseItemDto> mBaseItems;
    LayoutInflater mLayoutInflater;
    ApiClient mApi;
    int mAlbumMaxHeight;
    int mAlbumMaxWidth;
    boolean imageEnhancersEnabled;

    private String sections_ = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public AlbumAdapter(Context c, List<BaseItemDto> baseItems, ApiClient apiClient, int albumMaxHeight, int albumMaxWidth) {
        mContext = c;
        mBaseItems = baseItems;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mApi = apiClient;

        mAlbumMaxHeight = albumMaxHeight;
        mAlbumMaxWidth = albumMaxWidth;

        try {
            imageEnhancersEnabled = PreferenceManager
                    .getDefaultSharedPreferences(MB3Application.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true);
        } catch (Exception e) {
            Log.d("AbstractMediaAdapter", "Error reading preferences");
        }
    }


    public int getCount() {
        return mBaseItems.size();
    }


    @SuppressLint("SimpleDateFormat")
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {

            convertView = mLayoutInflater.inflate(R.layout.widget_album_tile, parent, false);

            holder = new ViewHolder();
            holder.albumTitle = (TextView) convertView.findViewById(R.id.tvAlbumName);
            holder.artistName = (TextView) convertView.findViewById(R.id.tvArtistName);
            holder.albumYear = (TextView) convertView.findViewById(R.id.tvAlbumYear);
            holder.imageView = (NetworkImageView) convertView.findViewById(R.id.ivAlbumCover);
            holder.imageView.setDefaultImageResId(R.drawable.music_square_bg);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.position = position;
        holder.albumTitle.setText(mBaseItems.get(position).getName());

        String type = mBaseItems.get(position).getType();

        if (mBaseItems.get(position).getAlbumArtist() != null) {
            holder.artistName.setText(mBaseItems.get(position).getAlbumArtist());
        } else {
            holder.artistName.setText("");
        }

        if (mBaseItems.get(position).getProductionYear() != null) {
            holder.albumYear.setText(String.valueOf(mBaseItems.get(position).getProductionYear()));
        }

        if (type.equalsIgnoreCase("Boxset") || type.equalsIgnoreCase("Folder")) {
            holder.albumYear.setText(String.valueOf(mBaseItems.get(position).getChildCount()) + " Items");
        }

//        if (mBaseItems[position].CommunityRating != null) {
//            holder.starRating.setVisibility(ImageView.VISIBLE);
//            Utils.ShowStarRating(mBaseItems[position].CommunityRating, holder.starRating);
//        } else {
//            holder.starRating.setVisibility(ImageView.GONE);
//        }

        if (mBaseItems.get(position).getHasPrimaryImage()) {
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setMaxWidth(mAlbumMaxWidth);
            options.setMaxHeight(mAlbumMaxHeight);
            options.setEnableImageEnhancers(imageEnhancersEnabled);

            String imageUrl = mApi.GetImageUrl(mBaseItems.get(position), options);
            holder.imageView.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());

        } else {
            holder.imageView.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
        }

//        holder.isNewOverlay.setVisibility(ImageView.INVISIBLE);

//        if (mBaseItems[position].DateCreated != null) {
//            try {
//                DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
//
//                Date createdDate = new Date();
//                createdDate = inputFormat.parse(mBaseItems[position].DateCreated);
//                long createdMilliseconds = createdDate.getTime();
//
//                long currentMilliseconds = new Date().getTime();
//
//                if ((currentMilliseconds - createdMilliseconds) < (10 * 86400000 )) {
//                    holder.isNewOverlay.setVisibility(ImageView.VISIBLE);
//                }
//
//            } catch (Exception e) {
//                Log.e("Exception", "Error converting DateCreated", e);
//            }
//        }

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
        public TextView albumTitle;
        public TextView artistName;
        public TextView albumYear;
        public int position;
    }
}
