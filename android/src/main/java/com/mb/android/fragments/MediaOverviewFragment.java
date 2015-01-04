package com.mb.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.utils.Utils;
import com.mb.android.activities.mobile.MediaDetailsActivity;
import com.mb.android.activities.mobile.RottenTomatoesActivity;

import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.LocationType;
import com.mb.android.logging.FileLogger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment that shows the overview and other details about the current selected item
 */
public class MediaOverviewFragment extends Fragment {

    private View mView;
    private BaseItemDto mItem;
    private ImageView sRating;
    private NetworkImageView mPrimaryImage;
    private ViewSwitcher mBackdropSwitcher;
    private NetworkImageView mBackdropImage1;
    private NetworkImageView mBackdropImage2;
    private List<String> mBackdropUrls;
    private int mBackdropIndex = 0;
    private View miRuntime;
    private View miYear;
    private View miOfficialRating;
    private MediaDetailsActivity mMediaDetailsActivity;

    /**
     * Class Constructor
     */
    public MediaOverviewFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_media_overview, container, false);

        Bundle args = getArguments();

        String jsonData = args.getString("Item");
        mItem = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, BaseItemDto.class);

        sRating = (ImageView) mView.findViewById(R.id.ivDetailsStarRating);
        miRuntime = inflater.inflate(R.layout.widget_bordered_textview, container, false);
        miYear = inflater.inflate(R.layout.widget_bordered_textview, container, false);
        miOfficialRating = inflater.inflate(R.layout.widget_bordered_textview, container, false);

        return mView;
    }


    @Override
    public void onPause() {
        super.onPause();

        if (mBackdropSwitcher != null) {
            mBackdropSwitcher.removeCallbacks(CycleBackdrop);
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        PopulateDetailsView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity != null) {
            try {
                mMediaDetailsActivity = (MediaDetailsActivity) activity;
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
        }
    }

    private void PopulateDetailsView() {

        PopulateTitle();
        PopulateTagline();
        PopulatePrimaryImage();
        PopulateBackdropImage();
        PopulateTvInfo();
        PopulateOverview();
        PopulateMediaInfo();
        PopulateMissingItemOverlay();
    }

    private void PopulateTitle() {

        TextView title = (TextView) mView.findViewById(R.id.tvMediaTitle);

        if (title != null)
            title.setText(mItem.getName());
    }


    private void PopulateTagline() {

        TextView tagline = (TextView) mView.findViewById(R.id.tvTagline);

        if (mItem.getTaglines() != null && mItem.getTaglines().size() > 0) {
            tagline.setText(mItem.getTaglines().get(0));
        } else {
            tagline.setVisibility(LinearLayout.GONE);
        }
    }


    private void PopulatePrimaryImage() {

        mPrimaryImage = (NetworkImageView) mView.findViewById(R.id.ivPrimaryImage);

        if (mPrimaryImage == null)
            return;

        if (mItem.getHasPrimaryImage()) {
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setMaxWidth((int) (300 * mMediaDetailsActivity.getScreenDensity()));
            options.setMaxHeight(mMediaDetailsActivity.getScreenHeight() - 150);
            options.setEnableImageEnhancers(PreferenceManager
                    .getDefaultSharedPreferences(MB3Application.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true));

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(mItem, options);
            mPrimaryImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        } else {
            mPrimaryImage.setDefaultImageResId(R.drawable.default_video_portrait);
            mPrimaryImage.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
        }
    }


    private void PopulateBackdropImage() {

        mBackdropSwitcher = (ViewSwitcher) mView.findViewById(R.id.vsBackdropImages);

        // We're not in portrait view
        if (mBackdropSwitcher == null)
            return;

        // lock the dimensions to stop the image from 'jumping'. Only an issue in portrait mode
        int width = mMediaDetailsActivity.getScreenWidth();
        int height = (width / 16) * 9;
        mBackdropSwitcher.setLayoutParams(new RelativeLayout.LayoutParams(width, height));


        mBackdropImage1 = (NetworkImageView) mView.findViewById(R.id.ivMediaBackdrop1);
        mBackdropImage1.setDefaultImageResId(R.drawable.default_backdrop);
        mBackdropImage2 = (NetworkImageView) mView.findViewById(R.id.ivMediaBackdrop2);

        mBackdropUrls = new ArrayList<>();
        ImageOptions options;
        String imageUrl;

        if (mItem.getType().equalsIgnoreCase("episode") && mItem.getHasPrimaryImage()) {

            options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setEnableImageEnhancers(false);
            imageUrl = MB3Application.getInstance().API.GetImageUrl(mItem, options);
            setBackdropImage(imageUrl);

            if (mPrimaryImage != null) {
                mPrimaryImage.setVisibility(View.GONE);
            }

        } else {
            if (mItem.getBackdropCount() > 0) {
                for (int i = 0; i < mItem.getBackdropCount(); i++) {
                    options = new ImageOptions();
                    options.setImageType(ImageType.Backdrop);
                    options.setImageIndex(i);
                    options.setMaxWidth(mMediaDetailsActivity.getResources().getDisplayMetrics().widthPixels);
                    imageUrl = MB3Application.getInstance().API.GetImageUrl(mItem, options);

                    mBackdropUrls.add(imageUrl);
                }
            } else if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mItem.getParentBackdropItemId())) {
                options = new ImageOptions();
                options.setImageType(ImageType.Backdrop);

                imageUrl = MB3Application.getInstance().API.GetImageUrl(mItem.getParentBackdropItemId(), options);
                mBackdropUrls.add(imageUrl);
            }
        }

        if (mBackdropUrls != null && mBackdropUrls.size() > 0 ) {
            if (mBackdropUrls.size() == 1) {
                setBackdropImage(mBackdropUrls.get(0));
            } else {
                mBackdropSwitcher.post(CycleBackdrop);
            }
        } else {
            setBackdropImage(null);
        }
    }


    private void PopulateTvInfo() {

        LinearLayout tvInfo = (LinearLayout) mView.findViewById(R.id.llTvInfo);

        if (mItem.getType().equalsIgnoreCase("episode")) {

            TextView seriesName = (TextView) mView.findViewById(R.id.tvSeriesName);
            seriesName.setText(mItem.getSeriesName());

            TextView episodeNumber = (TextView) mView.findViewById(R.id.tvEpisodeNumber);

            try {
                String title = "";
                if (mItem.getIndexNumber() != null)
                    title += mItem.getIndexNumber().toString();
                if (mItem.getIndexNumberEnd() != null && !mItem.getIndexNumberEnd().equals(mItem.getIndexNumber())) {
                    title += " - " + mItem.getIndexNumberEnd();
                    episodeNumber.setText("Season " + String.valueOf(mItem.getParentIndexNumber()) + " Episodes " + title);
                } else {
                    episodeNumber.setText("Season " + String.valueOf(mItem.getParentIndexNumber()) + " Episode " + String.valueOf(mItem.getIndexNumber()));
                }
            } catch (Exception e) {
                FileLogger.getFileLogger().ErrorException("PopulateTvInfo - ", e);
                tvInfo.setVisibility(LinearLayout.GONE);
            }
        } else {
            tvInfo.setVisibility(LinearLayout.GONE);
        }

    }


    private void PopulateOverview() {

        TextView synopsis = (TextView) mView.findViewById(R.id.tvOverviewSynopsis);
        synopsis.setText(mItem.getOverview());
        //synopsis.setMovementMethod(new ScrollingMovementMethod());
    }


    private void PopulateMediaInfo() {

        LinearLayout miHolder = (LinearLayout) mView.findViewById(R.id.llMediaInfoContent);
        if (miHolder.getChildCount() > 0) {
            miHolder.removeAllViews();
        }

        if (mItem.getCriticRating() != null && mItem.getCriticRating() > 1) {

            LinearLayout rtInfo = (LinearLayout) mView.findViewById(R.id.llRtContainer);
            rtInfo.setOnClickListener(new View.OnClickListener() {

                public void onClick(View arg0) {

                    Intent rtIntent = new Intent(mMediaDetailsActivity, RottenTomatoesActivity.class);
                    String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(mItem);
                    rtIntent.putExtra("Item", jsonData);
                    startActivity(rtIntent);

                }

            });

            ImageView rtRatingImage = (ImageView) mView.findViewById(R.id.ivRtImage);
            TextView rtRatingText = (TextView) mView.findViewById(R.id.tvRtRating);

            Animation anim = AnimationUtils.loadAnimation(mMediaDetailsActivity, R.anim.image_grow);

            if (mItem.getCriticRating() >= 60) {
                rtRatingImage.setImageResource(R.drawable.fresh);
                if (anim != null)
                    rtRatingImage.startAnimation(anim);
            } else {
                rtRatingImage.setImageResource(R.drawable.rotten);
                if (anim != null)
                    rtRatingImage.startAnimation(anim);
            }

            try {
                rtRatingText.setText(String.valueOf((int) Math.ceil(mItem.getCriticRating())) + "%");
            } catch (Exception e) {
                FileLogger.getFileLogger().ErrorException("MediaOverviewFragment: Error setting RT Rating value ", e);
            }
            rtRatingImage.setVisibility(ImageView.VISIBLE);
            rtRatingText.setVisibility(TextView.VISIBLE);
        }

        if (mItem.getMetascore() != null) {
            TextView metaScore = (TextView) mView.findViewById(R.id.tvMetaScore);

            if (metaScore != null) {
                metaScore.setText(String.valueOf(mItem.getMetascore().intValue()));
                if (mItem.getMetascore() >= 60) {
                    metaScore.setBackgroundColor(Color.parseColor("#7066cc33"));
                } else if (mItem.getMetascore() >= 40) {
                    metaScore.setBackgroundColor(Color.parseColor("#70ffcc33"));
                } else {
                    metaScore.setBackgroundColor(Color.parseColor("#70f00000"));
                }
                metaScore.setVisibility(View.VISIBLE);
            }
        }

        if (mItem.getCommunityRating() != null) {
            sRating.setVisibility(ImageView.VISIBLE);
            Utils.ShowStarRating(mItem.getCommunityRating(), sRating);
        }

        if (mItem.getRunTimeTicks() != null && miRuntime != null) {

            TextView runtime = (TextView) miRuntime.findViewById(R.id.tvMediaInfoText);
            runtime.setText(Utils.TicksToRuntimeString(mItem.getRunTimeTicks()));
            miHolder.addView(miRuntime);
        }

        if (mItem.getProductionYear() != null && miYear != null) {

            TextView textYear = (TextView) miYear.findViewById(R.id.tvMediaInfoText);

            if (textYear != null) {
                textYear.setText(String.valueOf(mItem.getProductionYear()));
                miHolder.addView(miYear);
            }
        }

        if (mItem.getOfficialRating() != null
                && mItem.getOfficialRating().length() > 0
                && miOfficialRating != null) {

            TextView textOfficialRating = (TextView) miOfficialRating.findViewById(R.id.tvMediaInfoText);
            textOfficialRating.setText(String.valueOf(mItem.getOfficialRating()));
            miHolder.addView(miOfficialRating);
        }


        if (mItem.getGenres() != null && mItem.getGenres().size() > 0) {

            TextView genreList = (TextView) mView.findViewById(R.id.tvGenreValues);

            String genresTemp = "";

            for (String genre : mItem.getGenres()) {

                if (!genresTemp.isEmpty()) {
                    genresTemp += "<font color='#00b4ff'> &#149 </font>";
                }

                genresTemp += genre;
            }

            genreList.setText(Html.fromHtml(genresTemp), TextView.BufferType.SPANNABLE);
            genreList.setTextSize((float) 14);
        }
    }


    private void PopulateMissingItemOverlay() {

        if (!mItem.getLocationType().equals(LocationType.Virtual)) return;

        TextView missingItemOverlay = (TextView) mView.findViewById(R.id.tvMissingEpisodeOverlay);

        if (mItem.getPremiereDate() != null) {

            Date premiereDate = Utils.convertToLocalDate(mItem.getPremiereDate());

            long premiereDateMs = premiereDate.getTime();
            long currentMs = new Date().getTime();

            if (premiereDateMs - currentMs > 0)
                missingItemOverlay.setText("UNAIRED");
        }

        missingItemOverlay.setVisibility(TextView.VISIBLE);

    }

    private void setBackdropImage(String imageUrl) {

        if (imageUrl == null || imageUrl.isEmpty()) {
            FileLogger.getFileLogger().Error("Error setting backdrop - imageUrl is null or empty");
            return;
        }

        if (mBackdropSwitcher.getDisplayedChild() == 0) {
            mBackdropImage2.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            mBackdropSwitcher.showNext();
        } else {
            mBackdropImage1.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            mBackdropSwitcher.showPrevious();
        }
    }

    private Runnable CycleBackdrop = new Runnable() {

        @Override
        public void run() {

            if (mBackdropIndex >= mBackdropUrls.size())
                mBackdropIndex = 0;

            setBackdropImage(mBackdropUrls.get(mBackdropIndex));
            mBackdropIndex += 1;
            mBackdropSwitcher.postDelayed(this, 8000);
        }
    };
}
