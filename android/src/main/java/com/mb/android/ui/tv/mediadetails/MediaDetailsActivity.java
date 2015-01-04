package com.mb.android.ui.tv.mediadetails;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;
import com.jess.ui.TwoWayAdapterView;
import com.jess.ui.TwoWayGridView;
import com.mb.android.DialogFragments.StreamSelectionDialogFragment;
import com.mb.android.ItemListWrapper;
import com.mb.android.ItemReviewsWrapper;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.adapters.HorizontalAdapterBackdrops;
import com.mb.android.adapters.TvActorAdapter;
import com.mb.android.adapters.TvReviewsAdapter;
import com.mb.android.adapters.TvScenesAdapter;
import mediabrowser.apiinteraction.Response;
import com.mb.android.logging.FileLogger;
import com.mb.android.ui.tv.MbBackdropActivity;
import com.mb.android.ui.tv.library.interfaces.ILongPressDialogListener;
import com.mb.android.ui.tv.library.dialogs.LongPressDialogFragment;
import com.mb.android.ui.tv.playback.PlayerHelpers;
import com.mb.android.utils.Utils;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.ChapterInfoDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.ItemReview;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.library.PlayAccess;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.SimilarItemsQuery;
import mediabrowser.model.results.ItemReviewsResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment used to show a Details Screen for several media types. Movies, Episodes, Games
 */
public class MediaDetailsActivity extends MbBackdropActivity implements ILongPressDialogListener, StreamSelectionDialogFragment.StreamSelectionDialogListener {

    public String TAG = "MediaDetailsActivity";
    private BaseItemDto mItem;
    private String mParentCollectionType;
    private NetworkImageView mLogo;
    private ProgressBar mActivityIndicator;
    private ItemListWrapper mSpecials;
    private ItemReviewsWrapper mReviews;
    private boolean mSpecialsCallbackCompleted = false;
    private boolean mReviewsCallbackCompleted = false;
    private boolean mQuickPlayCallbackCompleted = false;
    private boolean mQuickPlayEnabled;
    private List<String> mFragmentTypeList;
    private NetworkImageView mPosterImage;
    private NetworkImageView mDiscImage;
    private TextView mTitle;
    private TextView mSeriesTitle;
    private TextView mSeasonEpisodeNumbers;
    private TextView mOverview;
    private TextView mReleaseYear;
    private TextView mRuntime;
    private TextView mOfficialRating;
    private TextView mGenres;
    private ImageView mStarRating;
    private TextView mMetaScore;
    private ImageView mRtImage;
    private TextView mRtValue;
    private ImageButton streamSelectionButton;
    private ImageButton optionsButton;
    private ProgressBar playProgress;
    private Integer mSelectedAudioStream;
    private Integer mSelectedSubtitleStream;
    private String mSelectedMediaSourceId;
    private TextView mWatchedOverlay;
    private TwoWayGridView contentGrid;
    private TwoWayGridView sectionsGrid;
    private TextTabAdapter adapter;
    private LinearLayout overviewTab;
    private List<BaseItemDto> mSimilarItems;
    private boolean mIsFresh = true;
    private PlayerHelpers mPlayHelper;
    private float mDensity = 1.0f;



    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.tv_activity_details);

        String jsonData = getIntent().getStringExtra("CurrentBaseItemDTO");
        mItem = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, BaseItemDto.class);
        mParentCollectionType = getIntent().getStringExtra("CollectionType");

        inflateControls();
        attachEventListeners();
        setOverscanValues();

        mPlayHelper = new PlayerHelpers();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDensity = metrics.density;

        FileLogger.getFileLogger().Info("DetailsOverviewFragment: finish onCreateView");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            getItemData();
        }
    }

    @Override
    public void onConnectionRestored() {
        getItemData();
    }

    @Override
    protected void onUserDataUpdated(String itemId, UserItemDataDto userItemDataDto) {
        MB3Application.getInstance().API.GetItemAsync(
                mItem.getId(),
                MB3Application.getInstance().API.getCurrentUserId(),
                new GetItemResponse(true));
    }

    @Override
    protected void onPlayButton() {

    }

    @Override
    protected void onFastForwardButton() {

    }

    @Override
    protected void onRewindButton() {

    }

    @Override
    protected void onMenuButton() {

    }

    private void getItemData() {
        if (mIsFresh && mItem != null) {
            MB3Application.getInstance().API.GetItemAsync(
                    mItem.getId(),
                    MB3Application.getInstance().API.getCurrentUserId(),
                    new GetItemResponse(false));

            MB3Application.getInstance().API.GetSpecialFeaturesAsync(
                    MB3Application.getInstance().API.getCurrentUserId(),
                    mItem.getId(),
                    new GetSpecialFeaturesResponse()
            );

            MB3Application.getInstance().API.GetCriticReviews(mItem.getId(), 0, 25, new GetCriticReviewsResponse());

            if ("movie".equalsIgnoreCase(mItem.getType()) || "episode".equalsIgnoreCase(mItem.getType())) {
                MB3Application.getInstance().API.GetIntrosAsync(mItem.getId(), MB3Application.getInstance().API.getCurrentUserId(), new GetIntrosResponse());
            } else {
                mQuickPlayCallbackCompleted = true;
                mQuickPlayEnabled = false;
                BuildSectionsList(mItem);
            }
            mIsFresh = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }



    private void RenderRatingsValues() {

        if (mItem.getCommunityRating() != null) {
            Utils.ShowStarRating(mItem.getCommunityRating(), mStarRating);
            mStarRating.setVisibility(View.VISIBLE);

        }

        if (mItem.getMetascore() != null) {
            mMetaScore.setText(String.valueOf(mItem.getMetascore().intValue()));
            if (mItem.getMetascore() >= 60) {
                mMetaScore.setBackgroundColor(Color.parseColor("#7066cc33"));
            } else if (mItem.getMetascore() >= 40) {
                mMetaScore.setBackgroundColor(Color.parseColor("#70ffcc33"));
            } else {
                mMetaScore.setBackgroundColor(Color.parseColor("#70f00000"));
            }
            mMetaScore.setVisibility(View.VISIBLE);

        }

        if (mItem.getCriticRating() != null) {
            if (mItem.getCriticRating() >= 60) {
                mRtImage.setImageResource(R.drawable.fresh);
            } else {
                mRtImage.setImageResource(R.drawable.rotten);
            }
            mRtImage.setVisibility(View.VISIBLE);

            mRtValue.setText(String.valueOf(mItem.getCriticRating().intValue()) + "%");
            mRtValue.setVisibility(View.VISIBLE);

        }

    }


    private void BuildSectionsList(BaseItemDto item) {

        if (mSpecialsCallbackCompleted && mReviewsCallbackCompleted && mQuickPlayCallbackCompleted) {

            mFragmentTypeList = new ArrayList<>();

            if (item.getPlayAccess().equals(PlayAccess.Full)) {
                if (item.getUserData() != null && item.getUserData().getPlaybackPositionTicks() != 0) {
                    mFragmentTypeList.add("resume");
                }

                if (!item.getIsFolder() && item.getLocationType() != LocationType.Virtual) {
                    mFragmentTypeList.add("play");
                }

                if (mQuickPlayEnabled) {
                    mFragmentTypeList.add("quick play");
                }

                if (item.getLocalTrailerCount() != null && item.getLocalTrailerCount() > 0) {
                    mFragmentTypeList.add("play trailer");
                }
            } else {
                mFragmentTypeList.add("overview");
            }

            boolean hasActors = false;

            if (mItem.getPeople() != null) {
//                for (BaseItemPerson person : mItem.getPeople()) {
//                    if (person.getType() != null && person.getType().equalsIgnoreCase("actor")) {
                        hasActors = true;
//                        break;
//                    }
//                }
            }

            if ("series".equalsIgnoreCase(mItem.getType())) {
                mFragmentTypeList.add("seasons");
            }

            if (hasActors) {
                mFragmentTypeList.add("actors");
            }

            if (mItem.getChapters() != null && mItem.getChapters().size() > 0) {
                mFragmentTypeList.add("chapters");
            }

            if (mSpecials != null && mSpecials.Items != null && mSpecials.Items.size() > 0) {
                mFragmentTypeList.add("specials");
            }

            if (mReviews != null && mReviews.ItemReviews != null && mReviews.ItemReviews.size() > 0) {
                mFragmentTypeList.add("reviews");
            }

            if (mItem.getType().equalsIgnoreCase("movie")) {
                mFragmentTypeList.add("similar");
            }


            adapter = new TextTabAdapter(this, mFragmentTypeList);
            sectionsGrid.setAdapter(adapter);
            sectionsGrid.setOnItemSelectedListener(new TwoWayAdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(TwoWayAdapterView<?> parent, View view, int position, long id) {
                    adapter.setSelectedIndex(position);

                    switch (mFragmentTypeList.get(position)) {
                        case "resume":
                        case "play":
                        case "quick play":
                        case "play trailer":
                        case "overview":
                            contentGrid.setVisibility(View.GONE);
                            contentGrid.setOnItemClickListener(null);
                            overviewTab.setVisibility(View.VISIBLE);
                            break;
                        case "actors":
                            overviewTab.setVisibility(View.GONE);
                            contentGrid.setVisibility(View.VISIBLE);
                            contentGrid.setNumRows(1);
                            contentGrid.setRowHeight((int)((float)240 * mDensity));
                            contentGrid.setAdapter(new TvActorAdapter(mItem.getPeople(), contentGrid.getHeight(), 1, MediaDetailsActivity.this));
                            contentGrid.setOnItemClickListener(onNavigatiableItemClick);
                            break;
                        case "chapters":
                            overviewTab.setVisibility(View.GONE);
                            contentGrid.setRowHeight((int)((float)160 * mDensity));
                            contentGrid.setNumRows(2);
                            contentGrid.setAdapter(new TvScenesAdapter(mItem, MediaDetailsActivity.this));
                            contentGrid.setOnItemClickListener(onPlayableItemClick);
                            contentGrid.setVisibility(View.VISIBLE);
                            break;
                        case "specials":
                            overviewTab.setVisibility(View.GONE);
                            contentGrid.setRowHeight((int)((float)160 * mDensity));
                            contentGrid.setNumRows(2);
//                            contentGrid.setAdapter();
                            contentGrid.setOnItemClickListener(onPlayableItemClick);
                            contentGrid.setVisibility(View.VISIBLE);
                            break;
                        case "reviews":
                            overviewTab.setVisibility(View.GONE);
                            contentGrid.setRowHeight((int)((float)160 * mDensity));
                            contentGrid.setNumRows(2);
                            contentGrid.setAdapter(new TvReviewsAdapter(mReviews.ItemReviews, MediaDetailsActivity.this));
                            contentGrid.setOnItemClickListener(null);
                            contentGrid.setVisibility(View.VISIBLE);
                            break;
                        case "similar":
                            overviewTab.setVisibility(View.GONE);
                            contentGrid.setRowHeight((int)((float)160 * mDensity));
                            contentGrid.setNumRows(2);
                            contentGrid.setAdapter(new HorizontalAdapterBackdrops(MediaDetailsActivity.this, mSimilarItems, contentGrid.getHeight(), 2, R.drawable.default_video_landscape));
                            contentGrid.setOnItemClickListener(onNavigatiableItemClick);
                            contentGrid.setVisibility(View.VISIBLE);
                            break;
                        default:
                            overviewTab.setVisibility(View.GONE);
                            contentGrid.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onNothingSelected(TwoWayAdapterView<?> parent) {

                }
            });
            sectionsGrid.setOnItemClickListener(new TwoWayAdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(TwoWayAdapterView<?> parent, View view, int position, long id) {
                    switch (mFragmentTypeList.get(position)) {
                        case "resume":
                            mPlayHelper.playItem(
                                    MediaDetailsActivity.this,
                                    mItem,
                                    mItem.getUserData() != null ? mItem.getUserData().getPlaybackPositionTicks() : 0L,
                                    mSelectedAudioStream,
                                    mSelectedSubtitleStream,
                                    mSelectedMediaSourceId,
                                    true
                            );
                            break;
                        case "play":
                            mPlayHelper.playItem(MediaDetailsActivity.this, mItem, 0L, mSelectedAudioStream, mSelectedSubtitleStream, mSelectedMediaSourceId, false);
                            break;
                        case "quick play":
                            mPlayHelper.playItem(MediaDetailsActivity.this, mItem, 0L, mSelectedAudioStream, mSelectedSubtitleStream, mSelectedMediaSourceId, true);
                            break;
                        case "play trailer":
                            MB3Application.getInstance().API.GetLocalTrailersAsync(
                                    MB3Application.getInstance().API.getCurrentUserId(), mItem.getId(),
                                    getLocalTrailersResponse);
                            break;
                    }
                }
            });
            sectionsGrid.requestFocus();
        }
    }


    private void hideProgressIndicator() {
        mActivityIndicator.setVisibility(View.GONE);
    }

    private void loadDetails(BaseItemDto item) {
        if (mItem == null) {
            return;
        }

        mTitle.setText(item.getName());

        if ("episode".equalsIgnoreCase(item.getType())) {
            String title = "";
            if (item.getIndexNumber() != null)
                title += item.getIndexNumber().toString();
            if (item.getIndexNumberEnd() != null && !item.getIndexNumberEnd().equals(item.getIndexNumber())) {
                title += " - " + item.getIndexNumberEnd();
                mSeasonEpisodeNumbers.setText("Season " + String.valueOf(item.getParentIndexNumber()) + ", Episodes " + title);
            } else {
                mSeasonEpisodeNumbers.setText("Season " + String.valueOf(item.getParentIndexNumber()) + ", Episode " + String.valueOf(item.getIndexNumber()));
            }
            mSeasonEpisodeNumbers.setVisibility(View.VISIBLE);
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getSeriesName())) {
                mSeriesTitle.setText(item.getSeriesName());
                mSeriesTitle.setVisibility(View.VISIBLE);
            }
        }
        mOverview.setText(item.getOverview());

        if (item.getHasPrimaryImage()) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MB3Application.getInstance());
            boolean enableImageEnhancers = true;
            if (sharedPrefs != null) {
                enableImageEnhancers = sharedPrefs.getBoolean("pref_enable_image_enhancers", true);
            }
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setMaxWidth(400);
            options.setMaxHeight(600);
            options.setEnableImageEnhancers(enableImageEnhancers);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(item, options);

            mPosterImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());

        }

        if (item.getGenres() != null && item.getGenres().size() > 0) {
            String genreList = "";

            for (String genre : item.getGenres()) {
                if (!genreList.isEmpty())
                    genreList += "  |  ";

                genreList += genre;
            }

            mGenres.setText(genreList);
        }

        if (item.getProductionYear() != null) {
            mReleaseYear.setText(String.valueOf(item.getProductionYear()));
        }
        if (item.getOfficialRating() != null && !item.getOfficialRating().isEmpty()) {
            mOfficialRating.setText(item.getOfficialRating());
        }
        if (item.getRunTimeTicks() != null) {
            mRuntime.setText(Utils.TicksToMinutesString(item.getRunTimeTicks()));
        }
        if (item.getUserData() != null && item.getUserData().getPlayedPercentage() != null && item.getUserData().getPlayedPercentage() > 0) {
            playProgress.setMax(100);
            playProgress.setProgress(item.getUserData().getPlayedPercentage().intValue());
            playProgress.setVisibility(View.VISIBLE);
        } else {
            playProgress.setVisibility(View.INVISIBLE);
        }

//        RenderRatingsValues();
    }

    private void showDiscAnimation() {
        if (mItem.getHasDiscImage()) {
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Disc);
            options.setMaxWidth(225);
            options.setMaxHeight(225);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(mItem, options);

            mDiscImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            mDiscImage.setVisibility(View.VISIBLE);

            Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
            Animation slideAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_slow);
            Animation fadeAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);

            AnimationSet animationSet = new AnimationSet(false);
            animationSet.addAnimation(rotateAnimation);
            animationSet.addAnimation(slideAnimation);
            animationSet.addAnimation(fadeAnimation);

            mDiscImage.setAnimation(animationSet);
        }
        mDiscImage.setVisibility(View.VISIBLE);
    }

    private void LoadBackdrops(BaseItemDto item) {

        if (item.getBackdropCount() > 0) {
            List<String> backdropUrls = new ArrayList<>();

            for (int i = 0; i < item.getBackdropCount(); i++) {

                ImageOptions imageOptions = new ImageOptions();
                imageOptions.setImageType(ImageType.Backdrop);
                imageOptions.setMaxHeight(720);
                imageOptions.setMaxWidth(1280);
                imageOptions.setImageIndex(i);

                backdropUrls.add(MB3Application.getInstance().API.GetImageUrl(item, imageOptions));
            }

            setBackdropImages(backdropUrls);
        }
        else if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getParentBackdropItemId())) {
            MB3Application.getInstance().API.GetItemAsync(item.getParentBackdropItemId(), MB3Application.getInstance().API.getCurrentUserId(), new Response<BaseItemDto>() {
                @Override
                public void onResponse(BaseItemDto parentItem) {
                    if (parentItem == null) return;
                    LoadBackdrops(parentItem);
                }
            });
        }
    }

    private void LoadLogo() {

        if (mItem.getHasLogo()) {

            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Logo);
            options.setWidth(900);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(mItem, options);

            mLogo.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            mLogo.setVisibility(View.VISIBLE);

        } else {
            mLogo.setVisibility(View.INVISIBLE);
        }

    }


    //**********************************************************************************************
    // Callback Classes
    //**********************************************************************************************

    private class GetItemResponse extends Response<BaseItemDto> {

        private boolean setActivityResult;

        public GetItemResponse(boolean setActivityResult) {
            this.setActivityResult = setActivityResult;
        }

        @Override
        public void onResponse(BaseItemDto response) {

            mItem = response;
            getSimilarItems();


            hideProgressIndicator();
            loadDetails(response);
            LoadBackdrops(response);
            LoadLogo();
            toggleStreamSelectionButtonVisibility(response);
            BuildSectionsList(response);
            if (mItem.getUserData() != null) {
                mWatchedOverlay.setVisibility(response.getUserData().getPlayed() ? View.VISIBLE : View.INVISIBLE);
            }
            if (setActivityResult) {
                setActivityResult();
            }
        }

        @Override
        public void onError(Exception ex) {

        }
    };

    private void toggleStreamSelectionButtonVisibility(BaseItemDto item) {
        if (item == null || streamSelectionButton == null) return;

        streamSelectionButton.setVisibility(item.getMediaSourceCount() != null && item.getMediaSourceCount()  > 0 ? View.VISIBLE: View.GONE);
    }

    @Override
    public void onUserDataChanged(String itemId, UserItemDataDto userItemDataDto) {
        if (mItem != null && userItemDataDto != null && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(itemId)) {
            mItem.setUserData(userItemDataDto);
            mWatchedOverlay.setVisibility(mItem.getUserData().getPlayed() ? View.VISIBLE: View.INVISIBLE);
            setActivityResult();
        }
    }

    // User has requested new audio/subtitle streams
    @Override
    public void onDialogPositiveClick(int audioStreamIndex, int subtitleStreamIndex, String selectedMediaSourceId) {
        mSelectedAudioStream = audioStreamIndex;
        mSelectedSubtitleStream = subtitleStreamIndex;
        mSelectedMediaSourceId = selectedMediaSourceId;
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {

    }


    public class GetSpecialFeaturesResponse extends Response<BaseItemDto[]> {
        @Override
        public void onResponse(BaseItemDto[] specials) {
            if (specials != null && specials.length > 0) {
                mSpecials = new ItemListWrapper();
                mSpecials.Items = new ArrayList<>();

                Collections.addAll(mSpecials.Items, specials);
            }

            mSpecialsCallbackCompleted = true;
            BuildSectionsList(mItem);
        }
    }


    public class GetCriticReviewsResponse extends Response<ItemReviewsResult> {
        @Override
        public void onResponse(ItemReviewsResult results) {
            if (results != null && results.getTotalRecordCount() > 0) {
                mReviews = new ItemReviewsWrapper();
                mReviews.ItemReviews = new ArrayList<>();

                for (ItemReview review : results.getItems()) {
                    if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(review.getCaption())) {
                        mReviews.ItemReviews.add(review);
                    }
                }
            }

            mReviewsCallbackCompleted = true;
            BuildSectionsList(mItem);
        }
    }

    public class GetIntrosResponse extends Response<ItemsResult> {
        @Override
        public void onResponse(ItemsResult result) {
            mQuickPlayEnabled = result != null && result.getItems() != null && result.getItems().length > 0;
            mQuickPlayCallbackCompleted = true;
            BuildSectionsList(mItem);
        }
        @Override
        public void onError(Exception ex) {
            mQuickPlayCallbackCompleted = true;
            mQuickPlayEnabled = false;
            BuildSectionsList(mItem);
        }
    }

    private class UpdateUserDataResponse extends Response<UserItemDataDto> {
        @Override
        public void onResponse(UserItemDataDto data) {

        }
    }


    private Response<BaseItemDto[]> getLocalTrailersResponse = new Response<BaseItemDto[]>() {

        @Override
        public void onResponse(BaseItemDto[] trailers) {
            if (trailers != null && trailers.length > 0) {

                Log.i("GetInitialItemCallback", "Trailers found");
                Log.i("GetInitialItemCallback", trailers[0].getId());

                mPlayHelper.playItem(MediaDetailsActivity.this, trailers[0], 0L, null, null, null, true);

            } else {
                if (trailers == null) {
                    Log.i("GetItemsCallback", "result is null or no trailers");
                    FileLogger.getFileLogger()
                            .Error("Error getting trailers");
                } else {
                    FileLogger.getFileLogger()
                            .Error("Empty list returned for trailers");
                }
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private void setActivityResult() {
        Intent intent = new Intent();
        intent.putExtra("Id", mItem.getId());
        String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(mItem.getUserData());
        intent.putExtra("UserData", jsonData);
        setResult(RESULT_OK, intent);
    }


    private void inflateControls() {
        mTitle = (TextView) findViewById(R.id.tvMediaTitle);
        mLogo = (NetworkImageView) findViewById(R.id.ivLogoImage);
        mBackdropSwitcher = (ViewSwitcher) findViewById(R.id.vsBackdropImages);
        mBackdropImage1 = (NetworkImageView) findViewById(R.id.ivBackdropImage1);
        mBackdropImage2 = (NetworkImageView) findViewById(R.id.ivBackdropImage2);
        mActivityIndicator = (ProgressBar) findViewById(R.id.pbActivityIndicator);
        mPosterImage = (NetworkImageView) findViewById(R.id.ivPosterImage);
        mDiscImage = (NetworkImageView) findViewById(R.id.ivDiscImage);
        mTitle = (TextView) findViewById(R.id.tvMediaTitle);
        mSeriesTitle = (TextView) findViewById(R.id.tvSeriesTitle);
        mSeasonEpisodeNumbers = (TextView) findViewById(R.id.tvSeasonEpisodeNumbers);
        mOverview = (TextView) findViewById(R.id.tvMediaOverview);
        mOverview.setMovementMethod(new ScrollingMovementMethod());
        mReleaseYear = (TextView) findViewById(R.id.tvYearValue);
        mRuntime = (TextView) findViewById(R.id.tvRuntimeValue);
        mOfficialRating = (TextView) findViewById(R.id.tvRatingValue);
        mGenres = (TextView) findViewById(R.id.tvGenresValue);
        mStarRating = (ImageView) findViewById(R.id.ivStarImage);
        mMetaScore = (TextView) findViewById(R.id.tvMetaScore);
        mRtImage = (ImageView) findViewById(R.id.ivCriticFreshRottenImage);
        mRtValue = (TextView) findViewById(R.id.tvRtRating);
        streamSelectionButton = (ImageButton) findViewById(R.id.ibStreamMenu);
        optionsButton = (ImageButton) findViewById(R.id.ibOptionsMenu);
        mWatchedOverlay = (TextView) findViewById(R.id.tvWatchedOverlay);
        sectionsGrid = (TwoWayGridView) findViewById(R.id.gvSectionsGrid);
        contentGrid = (TwoWayGridView) findViewById(R.id.gridview);
        overviewTab = (LinearLayout) findViewById(R.id.llOverviewTabContainer);
        playProgress = (ProgressBar) findViewById(R.id.pbPlaybackProgress);
    }

    private void attachEventListeners() {

        streamSelectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mItem == null) return;
                StreamSelectionDialogFragment dialog = new StreamSelectionDialogFragment();
                dialog.setItem(mItem);
                dialog.show(getSupportFragmentManager(), "StreamSelectionDialog");
            }
        });

        optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mItem == null) return;
                LongPressDialogFragment itemOptionsFragment = new LongPressDialogFragment();
                itemOptionsFragment.setItem(mItem);
                itemOptionsFragment.show(getSupportFragmentManager(), "OptionsFragment");
            }
        });



        mPosterImage.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {

            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

                // This feels like such a hack, but it works. The disc image will only appear AFTER the poster has
                // downloaded
                if (top != oldTop && mPosterImage.getDrawable() != null) {
                    layoutCalls++;
                    if (layoutCalls == 3) {
                        showDiscAnimation();
                    }

                }
            }
        });
    }

    int layoutCalls = 0;

    private void getSimilarItems() {
        mSimilarItems = new ArrayList<>();
        SimilarItemsQuery query = new SimilarItemsQuery();
        query.setId(mItem.getId());
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setLimit(24);
        query.setFields(new ItemFields[]{ ItemFields.PrimaryImageAspectRatio, ItemFields.SortName });

        if (mItem.getType().equalsIgnoreCase("movie")) {
            MB3Application.getInstance().API.GetSimilarMoviesAsync(query, new GetSimilarItemsResponse());
        } else if (mItem.getType().equalsIgnoreCase("series")) {
            MB3Application.getInstance().API.GetSimilarSeriesAsync(query, new GetSimilarItemsResponse());
        }
    }

    public class GetSimilarItemsResponse extends Response<ItemsResult> {
        @Override
        public void onResponse(ItemsResult result) {
            if (result != null && result.getTotalRecordCount() > 0) {

                mSimilarItems = Arrays.asList(result.getItems());

                if (contentGrid == null) {
                    Log.d("DetailsSimilarFragment", "mSimilarItemsGrid is null");
                    return;
                }

                ListAdapter adapter = contentGrid.getAdapter();
                if (adapter != null) {
                    if (adapter instanceof HorizontalAdapterBackdrops) {
                        ((HorizontalAdapterBackdrops)adapter).notifyDataSetChanged();
                    }
                }
            }
        }
    }

    private TwoWayGridView.OnItemClickListener onPlayableItemClick = new TwoWayAdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(TwoWayAdapterView<?> parent, View view, int position, long id) {

            if (parent.getSelectedItem() instanceof ChapterInfoDto) {
                ChapterInfoDto chapter = (ChapterInfoDto)parent.getSelectedItem();
                mPlayHelper.playItem(MediaDetailsActivity.this, mItem, chapter.getStartPositionTicks(), mSelectedAudioStream, mSelectedSubtitleStream, mSelectedMediaSourceId, true);
            }
        }
    };

    private TwoWayGridView.OnItemClickListener onNavigatiableItemClick = new TwoWayAdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(TwoWayAdapterView<?> parent, View view, int position, long id) {
            Object item = contentGrid.getSelectedItem();
            if (item != null) {
                if (item instanceof BaseItemDto) {
                    navigate((BaseItemDto) item, mParentCollectionType);
                } else if (item instanceof BaseItemPerson) {
                    navigate((BaseItemPerson) item, mParentCollectionType);
                }
            }
        }
    };
}
