package com.mb.android.ui.tv.homescreen;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.logging.FileLogger;
import com.mb.android.widget.AnimatedNetworkImageView;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.extensions.StringHelper;

import org.w3c.dom.Text;


public class NextUpCardAdapter extends BaseAdapter {

    private BaseItemDto[] mItems;
    private LayoutInflater mInflater;

    public NextUpCardAdapter(BaseItemDto[] items, Context context) {

        mItems = items;
        mInflater = LayoutInflater.from(context);
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
    public View getView(int index, View view, ViewGroup parent) {

        ViewHolder holder;

        if (view == null) {
            view = mInflater.inflate(R.layout.widget_tv_next_up_card, parent, false);

            holder = new ViewHolder();
            holder.primaryImage = (AnimatedNetworkImageView) view.findViewById(R.id.ivPrimaryImage);
            holder.episodeTitle = (TextView) view.findViewById(R.id.tvEpisodeTitle);
            holder.overview = (TextView) view.findViewById(R.id.tvEpisodeOverview);
            holder.seriesTitle = (TextView) view.findViewById(R.id.tvSeriesTitle);

            view.setTag(holder);
        } else {
            holder = (ViewHolder)view.getTag();
        }

        holder.episodeTitle.setText(mItems[index].getName());
        holder.seriesTitle.setText(mItems[index].getSeriesName());

        try {
            String title = "";
            if (mItems[index].getIndexNumber() != null)
                title += mItems[index].getIndexNumber().toString();
            if (mItems[index].getIndexNumberEnd() != null && !mItems[index].getIndexNumberEnd().equals(mItems[index].getIndexNumber())) {
                title += " - " + mItems[index].getIndexNumberEnd();
                holder.overview.setText("Season " + String.valueOf(mItems[index].getParentIndexNumber()) + ", Episodes " + title);
            } else {
                holder.overview.setText("Season " + String.valueOf(mItems[index].getParentIndexNumber()) + ", Episode " + String.valueOf(mItems[index].getIndexNumber()));
            }
        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("PopulateTvInfo - ", e);
//            tvInfo.setVisibility(LinearLayout.GONE);
        }

        if (mItems[index].getHasPrimaryImage()) {
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setWidth(400);
            options.setEnableImageEnhancers(false);

            String url = MB3Application.getInstance().API.GetImageUrl(mItems[index], options);
            holder.primaryImage.setImageUrl(url, MB3Application.getInstance().API.getImageLoader());
        }

        return view;
    }


    private class ViewHolder {
        public AnimatedNetworkImageView primaryImage;
        public TextView episodeTitle;
        public TextView seriesTitle;
        public TextView overview;
    }
}
