package com.mb.android.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mb.android.R;
import com.mb.android.utils.Utils;

import mediabrowser.model.entities.ItemReview;
import mediabrowser.model.extensions.StringHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 */
public class TvReviewsAdapter extends BaseAdapter {

    private List<ItemReview> mReviews;
    private Context mContext;
    private LayoutInflater mLayoutInflater;

    public TvReviewsAdapter(List<ItemReview> reviews, Context context) {
        mReviews = reviews;
        mContext = context;

        try {
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        } catch (Exception e) {

        }
    }

    @Override
    public int getCount() {
        return mReviews.size();
    }

    @Override
    public Object getItem(int position) {
        return mReviews.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @SuppressLint("SimpleDateFormat")
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.tv_widget_rt_critic_review, null);
        }

        if (convertView == null)
            return null;

        if (!mReviews.get(position).getLikes()) {
            ImageView reviewerImage = (ImageView) convertView.findViewById(R.id.ivCriticFreshRottenImage);
            reviewerImage.setImageResource(R.drawable.rotten);
        }


        TextView reviewInfo = (TextView) convertView.findViewById(R.id.tvReviewInfo);

        String reviewDetails = mReviews.get(position).getReviewerName();

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mReviews.get(position).getPublisher())) {
            if (reviewDetails != null && !reviewDetails.isEmpty()) {
                reviewDetails += ", ";
            }
            reviewDetails += mReviews.get(position).getPublisher();
        }

        if (mReviews.get(position).getDate() != null) {
            if (reviewDetails != null && !reviewDetails.isEmpty()) {
                reviewDetails += ", ";
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            Date date = Utils.convertToLocalDate(mReviews.get(position).getDate());

            reviewDetails += sdf.format(date);
        }

        reviewInfo.setText(reviewDetails);

        TextView reviewerCaption = (TextView) convertView.findViewById(R.id.tvCriticReview);
        if (!mReviews.get(position).getCaption().isEmpty())
            reviewerCaption.setText(mReviews.get(position).getCaption());
        else
            reviewerCaption.setText(mContext.getResources().getString(R.string.click_full_review));


        return convertView;
    }
}
