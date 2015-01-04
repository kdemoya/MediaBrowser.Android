package com.mb.android.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.utils.Utils;
import com.mb.android.ViewHolder;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.extensions.StringHelper;

/**
 * Created by Mark on 12/12/13.
 */
public class TvScenesAdapter extends BaseAdapter {

    private BaseItemDto mBaseItem;
    private Context mContext;
    private LayoutInflater mLayoutInflater;

    public TvScenesAdapter(BaseItemDto baseItem, Context context) {
        mBaseItem = baseItem;
        mContext = context;

        try {
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        } catch (Exception e) {

        }
    }

    @Override
    public int getCount() {
        return mBaseItem.getChapters().size();
    }

    @Override
    public Object getItem(int position) {
        return mBaseItem.getChapters().get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {

            holder = new ViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.widget_tv_chapters_extras_tile, parent, false);
            holder.titleText = (TextView) convertView.findViewById(R.id.tvChapterExtraTitle);
            holder.secondaryText = (TextView) convertView.findViewById(R.id.tvChapterExtraTime);
            holder.imageView = (NetworkImageView) convertView.findViewById(R.id.ivChapterExtraImage);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mBaseItem.getChapters().get(position).getName()))
            holder.titleText.setText(mBaseItem.getChapters().get(position).getName());

        holder.secondaryText.setText(Utils.PlaybackRuntimeFromMilliseconds(mBaseItem.getChapters().get(position).getStartPositionTicks() / 10000));


        ImageOptions sceneImageOptions = new ImageOptions();
        sceneImageOptions.setWidth(400);
        sceneImageOptions.setImageType(ImageType.Chapter);
        sceneImageOptions.setImageIndex(position);

        String sceneImageUrl = MB3Application.getInstance().API.GetImageUrl(mBaseItem, sceneImageOptions);

        holder.imageView.setDefaultImageResId(R.drawable.chapters_extras_image);
        holder.imageView.setImageUrl(sceneImageUrl, MB3Application.getInstance().API.getImageLoader());

        return convertView;
    }
}
