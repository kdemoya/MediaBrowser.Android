package com.mb.android.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment that shows a users root media libraries
 */
public class CollectionAdapter extends BaseAdapter {

    private BaseItemDto[] mItems;
    private LayoutInflater mLayoutInflater;
    private ApiClient mApi;
    private int mWidth;
    private int mHeight;
    private boolean mShowTitle = true;
    private boolean imageEnhancersEnabled;

    public CollectionAdapter(BaseItemDto[] items, Context context, ApiClient api) {
        mItems = items;
        mApi = api;
        try {
            mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            int mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;

            int mColumns = context.getResources().getInteger(R.integer.homescreen_item_columns);

            mWidth = (mScreenWidth - (mColumns * 2 * context.getResources().getDimensionPixelSize(R.dimen.grid_item_inner_padding))) / mColumns;
            mHeight = (mWidth / 16) * 9;

            imageEnhancersEnabled = PreferenceManager
                    .getDefaultSharedPreferences(MB3Application.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs == null) return;

        mShowTitle = prefs.getBoolean("pref_show_name", true);
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

            convertView = mLayoutInflater.inflate(R.layout.widget_collection_tile, viewGroup, false);

            holder = new ViewHolder();
            holder.CollectionName = (TextView) convertView.findViewById(R.id.tvCollectionName);
            holder.CollectionImage = (NetworkImageView) convertView.findViewById(R.id.ivCollectionImage);
            holder.CollectionImage.setLayoutParams(new RelativeLayout.LayoutParams(mWidth, mHeight));

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.CollectionName.setText(mItems[i].getName());

        ImageOptions options = null;

        if (mItems[i].getHasPrimaryImage()) {

            options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setEnableImageEnhancers(imageEnhancersEnabled);

        } else if (mItems[i].getHasThumb()) {

            options = new ImageOptions();
            options.setImageType(ImageType.Thumb);
            options.setEnableImageEnhancers(imageEnhancersEnabled);

        } else if (mItems[i].getBackdropCount() > 0) {

            options = new ImageOptions();
            options.setImageType(ImageType.Backdrop);
        }

        if (options != null) {
            options.setMaxWidth(mWidth);
            options.setMaxHeight(mHeight);

            String imageUrl = mApi.GetImageUrl(mItems[i], options);
            holder.CollectionImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            holder.CollectionImage.setVisibility(View.VISIBLE);
        } else {
            holder.CollectionImage.setVisibility(View.INVISIBLE);
        }

        if (!mShowTitle)
            holder.CollectionName.setVisibility(View.GONE);

        return convertView;
    }

    private class ViewHolder {
        NetworkImageView CollectionImage;
        TextView CollectionName;
    }
}
