package com.mb.android.activities.mobile;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.activities.BaseMbMobileActivity;
import mediabrowser.apiinteraction.Response;

import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.fragments.NavigationMenuFragment;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ItemReview;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.results.ItemReviewsResult;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Mark on 12/12/13.
 *
 * This Activity shows the various Rotten Tomatoes ratings that are associated with the current movie.
 */
public class RottenTomatoesActivity extends BaseMbMobileActivity {

    private AsyncTask<String, Void, Bitmap> mPosterTask = null;
    private BaseItemDto mItem;
    private ItemReview[] mReviews;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotten_tomatoes);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawer.setFocusableInTouchMode(false);

        NavigationMenuFragment fragment = (NavigationMenuFragment) getSupportFragmentManager().findFragmentById(R.id.left_drawer);
        if (fragment != null && fragment.isInLayout()) {
            fragment.setDrawerLayout(drawer);
        }

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                drawer,
                R.string.abc_action_bar_home_description,
                R.string.abc_action_bar_up_description) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
//                getActionBar().setTitle(mTitle);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
//                getActionBar().setTitle(mDrawerTitle);
            }

        };

        drawer.setDrawerListener(mDrawerToggle);

        String jsonData = getMb3Intent().getStringExtra("Item");
        mItem = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, BaseItemDto.class);

        TextView movieTitle = (TextView) findViewById(R.id.tvRTMovieTitle);

        if (mItem != null) {
            movieTitle.setText(mItem.getName());

            int rtScore = (int) Math.ceil(mItem.getCriticRating());

            if (rtScore < 60) {
                ImageView freshLogo = (ImageView) findViewById(R.id.ivCertifiedFresh);
                freshLogo.setVisibility(ImageView.INVISIBLE);
            }

            TextView reviewScore = (TextView) findViewById(R.id.tvTomatometerScore);
            reviewScore.setText(String.valueOf(rtScore) + "%");

            ProgressBar rtScoreBar = (ProgressBar) findViewById(R.id.pbRtScore);
            rtScoreBar.setProgress(rtScore);

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mItem.getCriticRatingSummary())) {
                TextView reviewText = (TextView) findViewById(R.id.tvTomatometerReview);
                reviewText.setText(Html.fromHtml(mItem.getCriticRatingSummary()));
            }
        }

        mMini = (MiniController) findViewById(R.id.miniController1);
        mCastManager.addMiniController(mMini);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            buildUi();
        }
    }

    @Override
    public void onPause() {
        mMini.removeOnMiniControllerChangedListener(mCastManager);
        super.onPause();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mPosterTask != null) {
            mPosterTask.cancel(true);
            mPosterTask = null;
        }
    }

    @Override
    protected void onConnectionRestored() {
        buildUi();
    }

    private void buildUi() {
        if (mItem != null) {

            if (mItem.getBackdropCount() > 0) {

                NetworkImageView mBackdropImage = (NetworkImageView) findViewById(R.id.ivMediaBackdrop);

                ImageOptions backdropOptions = new ImageOptions();
                backdropOptions.setImageType(ImageType.Backdrop);
                backdropOptions.setImageIndex(0);
                backdropOptions.setMaxWidth(getScreenWidth() / 2);
                backdropOptions.setMaxHeight(getScreenHeight() / 2);

                String backdropImageUrl = MB3Application.getInstance().API.GetImageUrl(mItem, backdropOptions);
                mBackdropImage.setImageUrl(backdropImageUrl, MB3Application.getInstance().API.getImageLoader());

                ImageView mImageOverlay = (ImageView) findViewById(R.id.ivDetailsOverlay);
                mImageOverlay.setAlpha(0.8f);

            }

            MB3Application.getInstance().API.GetCriticReviews(mItem.getId(), 0, 25, new GetCriticReviewsResponse());

            NetworkImageView movieImage = (NetworkImageView) findViewById(R.id.ivRtMovieImage);
            movieImage.setDefaultImageResId(R.drawable.default_video_portrait);

            if (mItem.getHasPrimaryImage()) {

                ImageOptions options = new ImageOptions();
                options.setImageType(ImageType.Primary);
                options.setImageIndex(0);
                options.setMaxHeight((int) (240 * getScreenDensity()));
                options.setEnableImageEnhancers(PreferenceManager
                        .getDefaultSharedPreferences(MB3Application.getInstance())
                        .getBoolean("pref_enable_image_enhancers", true));

                String imageUrl = MB3Application.getInstance().API.GetImageUrl(mItem, options);
                movieImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            } else {
                movieImage.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
            }
        }
    }

    public class ReviewAdapter extends BaseAdapter {

        public int getCount() {

            if (mReviews != null)
                return mReviews.length;
            else
                return 0;
        }

        public Object getItem(int arg0) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        @SuppressLint("SimpleDateFormat")
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.widget_rt_critic_review, parent, false);
            }

            if (convertView == null)
                return null;

            if (!mReviews[position].getLikes()) {
                ImageView reviewerImage = (ImageView) convertView.findViewById(R.id.ivCriticFreshRottenImage);
                reviewerImage.setImageResource(R.drawable.rotten);
            }


            TextView reviewerName = (TextView) convertView.findViewById(R.id.tvCriticName);
            reviewerName.setText(mReviews[position].getReviewerName());

            TextView reviewerPublication = (TextView) convertView.findViewById(R.id.tvCriticPublication);
            reviewerPublication.setText(mReviews[position].getPublisher());

            TextView reviewerCaption = (TextView) convertView.findViewById(R.id.tvCriticReview);
            if (!mReviews[position].getCaption().isEmpty())
                reviewerCaption.setText(mReviews[position].getCaption());
            else
                reviewerCaption.setText(getResources().getString(R.string.click_full_review));

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            Date date = com.mb.android.utils.Utils.convertToLocalDate(mReviews[position].getDate());

            TextView reviewDate = (TextView) convertView.findViewById(R.id.tvReviewDate);
            reviewDate.setText(sdf.format(date));

            return convertView;
        }

    }


    private class GetCriticReviewsResponse extends Response<ItemReviewsResult> {
        @Override
        public void onResponse(ItemReviewsResult reviews) {
            if (reviews != null && reviews.getTotalRecordCount() > 0) {

                mReviews = reviews.getItems();

                GridView reviewList = (GridView) findViewById(R.id.gvRtCriticReviews);

                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ||
                        (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) <= Configuration.SCREENLAYOUT_SIZE_NORMAL)
                    reviewList.setNumColumns(1);
                else
                    reviewList.setNumColumns(2);

                reviewList.setAdapter(new ReviewAdapter());
                reviewList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long arg3) {

                        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mReviews[position].getUrl())) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mReviews[position].getUrl()));
                            startActivity(browserIntent);
                        }
                    }

                });

            }
        }
    }
}
