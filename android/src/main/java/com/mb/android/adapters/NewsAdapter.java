package com.mb.android.adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mb.android.R;
import com.mb.android.utils.Utils;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.news.NewsItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Mark on 12/12/13.
 */
public class NewsAdapter extends BaseAdapter {

    private NewsItem[] mNewsItems;
    private Context mContext;
    private ApiClient mApi;
    private LayoutInflater mLayoutInflater;

    public NewsAdapter(NewsItem[] newsItems, Context context, ApiClient apiClient) {
        mNewsItems = newsItems;
        mContext = context;
        mApi = apiClient;

        try {
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        } catch (Exception e) {

        }
    }

    @Override
    public int getCount() {
        return mNewsItems.length;
    }

    @Override
    public Object getItem(int position) {
        return mNewsItems[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        NewsViewHolder holder;

        if (convertView == null) {

            holder = new NewsViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.widget_news_item, null);
            holder.titleText = (TextView) convertView.findViewById(R.id.tvNewsTitle);
            holder.contentText = (TextView) convertView.findViewById(R.id.tvNewsContent);
            holder.dateText = (TextView) convertView.findViewById(R.id.tvNewsDate);
            convertView.setTag(holder);

        } else {
            holder = (NewsViewHolder) convertView.getTag();
        }

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mNewsItems[position].getTitle()))
            holder.titleText.setText(mNewsItems[position].getTitle());


        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mNewsItems[position].getDescription()))
            holder.contentText.setText(Html.fromHtml(mNewsItems[position].getDescription()));

        if (mNewsItems[position].getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            Date date = Utils.convertToLocalDate(mNewsItems[position].getDate());
            holder.dateText.setText(sdf.format(date));
        }

        return convertView;
    }

    private class NewsViewHolder {
        public TextView titleText;
        public TextView contentText;
        public TextView dateText;
    }
}
