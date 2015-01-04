package com.mb.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.activities.mobile.SeriesViewActivity;
import com.mb.android.ui.tv.library.LibraryTools;

import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import com.mb.android.logging.FileLogger;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment that displays various information about the current Series.
 */
public class SeriesDetailsFragment extends Fragment {

    private View mView;
    private BaseItemDto mSeries;
    private NetworkImageView mBackdropImage;
    private int mImageIndex = 0;
    private String mTvdbBaseUrl = "http://thetvdb.com/index.php?tab=series&id=";
    private SeriesViewActivity mSeriesActivity;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity != null) {
            try {
                mSeriesActivity = (SeriesViewActivity) activity;
                mSeriesActivity.setSeriesDetailsFragment(this);
            } catch (ClassCastException e) {
                Log.d("ServerSelectionFragment", "onAttach: Exception casting activity");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        FileLogger.getFileLogger().Info("SeriesDetailsFragment: onCreateView");

        mView = inflater.inflate(R.layout.fragment_series_details, container, false);
        mBackdropImage = (NetworkImageView) mView.findViewById(R.id.ivSeriesImageHeader);

        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mBackdropImage != null) {
            mBackdropImage.removeCallbacks(CycleBackdrop);
        }
    }

    public void setSeries(BaseItemDto series) {
        mSeries = series;

        FileLogger.getFileLogger().Info("SeriesDetailsFragment: BuildImageQuery");

        if (mBackdropImage != null) {

            // lock the dimensions to stop the image from 'jumping'. Only an issue in portrait mode
            int width = mSeriesActivity.getScreenWidth();
            int height = (width / 16) * 9;
            mBackdropImage.setLayoutParams(new LinearLayout.LayoutParams(width, height));

            ImageOptions options = null;

            if (mSeries.getHasThumb()) {

                options = new ImageOptions();
                options.setImageType(ImageType.Thumb);
                options.setImageIndex(0);

                if (mSeries.getBackdropCount() > 1)
                    mBackdropImage.postDelayed(CycleBackdrop, 8000);

            } else if (mSeries.getBackdropCount() > 0) {

                options = new ImageOptions();
                options.setImageType(ImageType.Backdrop);
                options.setImageIndex(0);

                if (mSeries.getBackdropCount() > 1)
                    mBackdropImage.postDelayed(CycleBackdrop, 8000);
            }

            if (options != null) {

                DisplayMetrics metrics = new DisplayMetrics();
                mSeriesActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

                options.setWidth(metrics.widthPixels);

                String imageUrl = MB3Application.getInstance().API.GetImageUrl(mSeries, options);
                mBackdropImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            }
        } else {
            NetworkImageView primaryImageLandscape = (NetworkImageView) mView.findViewById(R.id.ivPrimaryImage);

            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setMaxWidth((int) (300 * mSeriesActivity.getScreenDensity()));
            options.setMaxHeight(mSeriesActivity.getScreenHeight() - 325);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(mSeries, options);
            primaryImageLandscape.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        }

        PopulateView();
    }

    private void PopulateView() {

        FileLogger.getFileLogger().Info("SeriesDetailsFragment: PopulateView");

        TextView titleText = (TextView) mView.findViewById(R.id.tvMediaTitle);
        titleText.setText(mSeries.getName());

        FileLogger.getFileLogger().Info("SeriesDetailsFragment: Setting Airing Info");
        TextView airingInfo = (TextView) mView.findViewById(R.id.tvSeriesViewAiringInfo);
        airingInfo.setText(LibraryTools.buildAiringInfoString(mSeries));

        FileLogger.getFileLogger().Info("SeriesDetailsFragment: Set Overview");
        TextView seriesOverview = (TextView) mView.findViewById(R.id.tvSeriesOverview);
        seriesOverview.setText(mSeries.getOverview());
        seriesOverview.setMovementMethod(new ScrollingMovementMethod());

        String gText = "";

        if (mSeries.getGenres() != null && !mSeries.getGenres().isEmpty()) {
            for (String genre : mSeries.getGenres()) {

                if (!gText.isEmpty())
                    gText += "<font color='#00b4ff'> &#149 </font>";

                gText += genre;
            }
        }

        TextView genreText = (TextView) mView.findViewById(R.id.tvSeriesGenre);
        genreText.setText(Html.fromHtml(gText), TextView.BufferType.SPANNABLE);

        FileLogger.getFileLogger().Info("SeriesDetailsFragment: Finished setting Genre(s)");

        if (mSeries.getProviderIds() != null && !mSeries.getProviderIds().isEmpty()) {
            final String tvdb = mSeries.getProviderIds().get("Tvdb");

            if (tvdb != null && !tvdb.isEmpty()) {

                TextView links = (TextView) mView.findViewById(R.id.tvLinks);
                links.setText("TheTVDB");
                links.setClickable(true);
                links.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {

                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mTvdbBaseUrl + tvdb));
                        startActivity(browserIntent);

                    }
                });

                FileLogger.getFileLogger().Info("SeriesDetailsFragment: Finished setting Links");
            }
        }
    }


    private Runnable CycleBackdrop = new Runnable() {

        @Override
        public void run() {

            if (mImageIndex >= mSeries.getBackdropCount())
                mImageIndex = 0;

            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Backdrop);
            options.setWidth(getResources().getDisplayMetrics().widthPixels);
            options.setImageIndex(mImageIndex);

            mImageIndex += 1;

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(mSeries, options);
            mBackdropImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());

            mBackdropImage.postDelayed(CycleBackdrop, 8000);
        }
    };
}
