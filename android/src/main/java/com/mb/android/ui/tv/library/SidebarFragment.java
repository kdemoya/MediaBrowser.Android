package com.mb.android.ui.tv.library;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.utils.Utils;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.extensions.StringHelper;

/**
 * Created by Mark on 2014-10-27.
 */
public class SidebarFragment extends Fragment {

    private TextView mMediaTitle;
    private TextView mRuntime;
    private TextView mOverview;
    private ImageView mStarRating;
    private ImageView mRtImage;
    private NetworkImageView mLogoImage;
    private TextView mReleaseYear;
    private TextView mOfficialRating;
    private TextView mRtRating;
    private TextView mMetaScore;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tv_sidebar, container, false);
        mMediaTitle = (TextView) view.findViewById(R.id.tvMediaTitleSide);
        mStarRating = (ImageView) view.findViewById(R.id.ivStarImageSide);
        mRtImage = (ImageView) view.findViewById(R.id.ivCriticFreshRottenImageSide);
        mRtRating = (TextView) view.findViewById(R.id.tvRtRatingSide);
        mMetaScore = (TextView) view.findViewById(R.id.tvMetaScoreSide);
        mLogoImage = (NetworkImageView) view.findViewById(R.id.ivLogoSide);
        mOverview = (TextView) view.findViewById(R.id.tvMediaOverviewSide);
        mRuntime = (TextView) view.findViewById(R.id.tvRuntimeSide);
        mReleaseYear = (TextView) view.findViewById(R.id.tvReleaseYearSide);
        mOfficialRating = (TextView) view.findViewById(R.id.tvRatingSide);

        return view;
    }

    public void setInfoFrom(BaseItemDto baseItem) {
        Toast.makeText(MB3Application.getInstance(), "setInfoFrom successfully called", Toast.LENGTH_LONG).show();
        populateItemInfo(baseItem);
    }

    private void populateItemInfo(BaseItemDto item) {

        mMediaTitle.setText(!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getName()) ? item.getName() : "");

        if (item.getCommunityRating() != null) {
            Utils.ShowStarRating(item.getCommunityRating(), mStarRating);
            mStarRating.setVisibility(View.VISIBLE);
        } else {
            mStarRating.setVisibility(View.GONE);
        }

        if (item.getCriticRating() != null) {
            if (item.getCriticRating() >= 60) {
                mRtImage.setImageResource(R.drawable.fresh);
            } else {
                mRtImage.setImageResource(R.drawable.rotten);
            }
            mRtImage.setVisibility(View.VISIBLE);

            mRtRating.setText(String.valueOf(item.getCriticRating().intValue()) + "%");
            mRtRating.setVisibility(View.VISIBLE);
        } else {
            mRtImage.setVisibility(View.GONE);
            mRtRating.setVisibility(View.GONE);
        }

        if (item.getMetascore() != null) {

            mMetaScore.setText(String.valueOf(item.getMetascore().intValue()));
            if (item.getMetascore() >= 60) {
                mMetaScore.setBackgroundColor(Color.parseColor("#7066cc33"));
            } else if (item.getMetascore() >= 40) {
                mMetaScore.setBackgroundColor(Color.parseColor("#70ffcc33"));
            } else {
                mMetaScore.setBackgroundColor(Color.parseColor("#70f00000"));
            }
            mMetaScore.setVisibility(View.VISIBLE);
        } else {
            mMetaScore.setVisibility(View.GONE);
        }

        if (mLogoImage != null) {
            if (item.getHasLogo()) {
                ImageOptions options = new ImageOptions();
                options.setImageType(ImageType.Logo);
                options.setMaxWidth(500);

                String imageUrl = MB3Application.getInstance().API.GetImageUrl(item, options);
                mLogoImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
                mLogoImage.setVisibility(View.VISIBLE);
                mMediaTitle.setVisibility(View.GONE);
            } else {
                mLogoImage.setVisibility(View.GONE);
                mMediaTitle.setVisibility(View.VISIBLE);
            }
        }

        if (mRuntime != null) {
            if ( !"series".equalsIgnoreCase(item.getType())) {
                mRuntime.setText(Utils.TicksToMinutesString(item.getCumulativeRunTimeTicks() != null ? item.getCumulativeRunTimeTicks() : item.getRunTimeTicks() != null ? item.getRunTimeTicks() : 0));
                mRuntime.setVisibility(View.VISIBLE);
            } else {
                mRuntime.setVisibility(View.GONE);
            }
        }

        if (mOverview != null) {
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getShortOverview())) {
                mOverview.setText(item.getShortOverview());
            } else {
                mOverview.setText(item.getOverview());
            }
        }

        if (mReleaseYear != null) {
            if (item.getProductionYear() != null) {
                mReleaseYear.setText(String.valueOf(item.getProductionYear()));
                mReleaseYear.setVisibility(View.VISIBLE);
            } else {
                mReleaseYear.setVisibility(View.GONE);
            }
        }

        if (mOfficialRating != null) {
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getOfficialRating())) {
                mOfficialRating.setText(item.getOfficialRating());
                mOfficialRating.setVisibility(View.VISIBLE);
            } else {
                mOfficialRating.setVisibility(View.GONE);
            }
        }
    }
}
