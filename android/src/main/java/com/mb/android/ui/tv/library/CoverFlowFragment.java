package com.mb.android.ui.tv.library;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.jess.ui.TwoWayAdapterView;
import com.jess.ui.TwoWayGridView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.adapters.HorizontalAdapterPosters;
import com.mb.android.logging.FileLogger;
import com.mb.android.ui.tv.playback.PlayerHelpers;
import com.mb.android.utils.Utils;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Mark on 2014-11-12.
 *
 * Fragment that shows library content in a typical CoverFlow style
 */
public class CoverFlowFragment extends BaseLibraryFragment {

    private static final String TAG = "CoverFlowFragment";
    private ArrayList<BaseItemDto> mItems;
    private LibraryActivity mLibraryActivity;
    private TwoWayGridView contentGrid;
    private ScrollView overviewScroller;
    private TextView mediaTitle;
    private TextView episodeIndexes;
    private TextView airingInfo;
    private TextView mediaOverview;
    private ImageView mStarRating;
    private ImageView mRtImage;
    private TextView mRtRating;
    private TextView mMetaScore;
    private ImageView mOfficialRating;
    private TextView mOfficialRatingText;
    private boolean isFresh = true;
    private PlayerHelpers mPlayHelper;
    private LinearLayout mDetailsPane;
    private Animation fadeInAnimation;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity != null) {
            try {
                mLibraryActivity = (LibraryActivity) activity;
            } catch (ClassCastException e) {
                Log.d(TAG, "onAttach: Exception casting activity");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tv_fragment_library_coverflow, container, false);
        contentGrid = (TwoWayGridView) view.findViewById(R.id.gvContent);
        mediaTitle = (TextView) view.findViewById(R.id.tvMediaTitle);
        episodeIndexes = (TextView) view.findViewById(R.id.tvEpisodeIndexes);
        airingInfo = (TextView) view.findViewById(R.id.tvAiringInfo);
        mediaOverview = (TextView) view.findViewById(R.id.tvMediaOverview);
        mStarRating = (ImageView) view.findViewById(R.id.ivStarImage);
        mRtImage = (ImageView) view.findViewById(R.id.ivCriticFreshRottenImage);
        mRtRating = (TextView) view.findViewById(R.id.tvRtRating);
        mMetaScore = (TextView) view.findViewById(R.id.tvMetaScore);
        mOfficialRating = (ImageView) view.findViewById(R.id.ivOfficialRating);
        mOfficialRatingText = (TextView) view.findViewById(R.id.tvOfficialRating);
        overviewScroller = (ScrollView) view.findViewById(R.id.svOverviewScrollView);
        mDetailsPane = (LinearLayout) view.findViewById(R.id.llDetails);
        fadeInAnimation = AnimationUtils.loadAnimation(mLibraryActivity, R.anim.fade_in);
        fadeInAnimation.setDuration(2500);
        mItems = new ArrayList<>();
        mPlayHelper = new PlayerHelpers();
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (overviewScroller != null) {
            overviewScroller.removeCallbacks(textScroller);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            performInitialSetup();
        }
    }

    public void performInitialSetup() {
        if (isFresh) {
            isFresh = false;
        }
    }


    private TwoWayAdapterView.OnItemSelectedListener onItemSelectedListener = new TwoWayAdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(TwoWayAdapterView<?> parent, View view, int position, long id) {
            if (mItems == null || mItems.size() <= position) return;
            hideDetails();
            BaseItemDto item = mItems.get(position);
            if (item == null) return;
            mediaTitle.setText(item.getName());
            mediaOverview.setText(item.getOverview());
            setEpisodeIndexes(item);
            if (mLibraryActivity != null) {
                mLibraryActivity.populateBackdrops(item);
            }
            populateRatingInfo(item);
            populateAiringInfo(item);

            overviewScroller.removeCallbacks(textScroller);
            overviewScroller.scrollTo(0,0);
            overviewScroller.postDelayed(textScroller, 5000); // Wait 5 seconds before scrolling initially
            showDetails();
        }
        @Override
        public void onNothingSelected(TwoWayAdapterView<?> parent) {

        }
    };

    private TwoWayAdapterView.OnItemLongClickListener mOnItemLongClickListener = new TwoWayAdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(TwoWayAdapterView<?> parent, View view, int position, long id) {
            if (mItems == null || mItems.size() <= position) {
                return false;
            }
            if (mLibraryActivity == null) return false;

            mLibraryActivity.showLongPressDialog(mItems.get(position));

            return true;
        }
    };


    private void populateAiringInfo(BaseItemDto item) {
        String airingString = LibraryTools.buildAiringInfoString(item);
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(airingString)) {
            airingInfo.setText(airingString);
            airingInfo.setVisibility(View.VISIBLE);
        } else {
            airingInfo.setVisibility(View.GONE);
        }
    }


    private void populateRatingInfo(BaseItemDto item) {
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
                mMetaScore.setBackgroundResource(R.drawable.metacritic_good_underlay);
            } else if (item.getMetascore() >= 40) {
                mMetaScore.setBackgroundResource(R.drawable.metacritic_average_underlay);
            } else {
                mMetaScore.setBackgroundResource(R.drawable.metacritic_bad_underlay);
            }
            mMetaScore.setVisibility(View.VISIBLE);
        } else {
            mMetaScore.setVisibility(View.GONE);
        }
        mOfficialRatingText.setVisibility(View.GONE);
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getOfficialRating())) {
            mOfficialRating.setVisibility(View.VISIBLE);
            String rating = item.getOfficialRating();

            if (rating.endsWith("TV-G")) {
                mOfficialRating.setImageResource(R.drawable.tv_g);
            } else if (rating.endsWith("TV-Y")) {
                mOfficialRating.setImageResource(R.drawable.tv_y);
            } else if (rating.endsWith("TV-Y7")) {
                mOfficialRating.setImageResource(R.drawable.tv_y7);
            } else if (rating.endsWith("TV-14")) {
                mOfficialRating.setImageResource(R.drawable.tv_14);
            } else if (rating.endsWith("TV-PG")) {
                mOfficialRating.setImageResource(R.drawable.tv_pg);
            } else if (rating.endsWith("PG")) {
                mOfficialRating.setImageResource(R.drawable.mpaa_pg_small);
            } else if (rating.endsWith("G")) {
                mOfficialRating.setImageResource(R.drawable.mpaa_g_small);
            } else if (rating.endsWith("PG-13")) {
                mOfficialRating.setImageResource(R.drawable.mpaa_pg13_small);
            } else if (rating.endsWith("NC-17")) {
                mOfficialRating.setImageResource(R.drawable.mpaa_nc17_small);
            } else if (rating.endsWith("NR")) {
                mOfficialRating.setImageResource(R.drawable.mpaa_nr_small);
            } else if (rating.endsWith("R")) {
                mOfficialRating.setImageResource(R.drawable.mpaa_r_small);
            } else {
                mOfficialRating.setVisibility(View.GONE);
                mOfficialRatingText.setVisibility(View.VISIBLE);
                mOfficialRatingText.setText(rating);
            }

        } else {
            mOfficialRating.setVisibility(View.GONE);
        }
    }

    private void setEpisodeIndexes(BaseItemDto item) {
        if ("episode".equalsIgnoreCase(item.getType())) {
            episodeIndexes.setVisibility(View.VISIBLE);
            episodeIndexes.setText(Utils.getLongEpisodeIndexString(item));
        } else {
            episodeIndexes.setVisibility(View.GONE);
        }
    }

    int retries = 0;

    @Override
    public void addContent(final BaseItemDto[] items) {

        if (contentGrid == null) {

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (retries < 10) {
                        if (contentGrid == null) {
                            retries++;
                            handler.postDelayed(this, 100);
                        } else {
                            addContentInternal(items);
                        }
                    }
                }
            }, 100);
        } else {
            addContentInternal(items);
        }
    }


    @Override
    public BaseItemDto getCurrentItem() {
        if (contentGrid != null && contentGrid.getSelectedItemPosition() >= 0) {
            return (BaseItemDto)contentGrid.getSelectedItem();
        }
        return null;
    }

    @Override
    public void refreshData(BaseItemDto item) {
        FileLogger.getFileLogger().Debug("Refresh data requested");
        if (contentGrid != null) {
            HorizontalAdapterPosters adapter = (HorizontalAdapterPosters)contentGrid.getAdapter();
            FileLogger.getFileLogger().Debug("Trying to replace item in dataset");
            boolean insertSucceeded = Utils.insertIntoDataset(item, mItems);
            FileLogger.getFileLogger().Debug("Watched: " + String.valueOf(item.getUserData().getPlayed()));
            if (adapter != null && insertSucceeded) {
                FileLogger.getFileLogger().Debug("insert succeeded");
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onDpadLeftHandled() {
        if (contentGrid != null && contentGrid.getSelectedItemPosition() == 0) {
            if (contentGrid.getCount() > 0) {
                int targetIndex = contentGrid.getCount() - 1;
                contentGrid.smoothScrollToPosition(targetIndex);
                contentGrid.setSelection(targetIndex);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDpadRightHandled() {
        if (contentGrid != null && contentGrid.getCount() > 0) {
            if (contentGrid.getSelectedItemPosition() == contentGrid.getCount() - 1) {
                contentGrid.smoothScrollToPosition(0);
                contentGrid.setSelection(0);
                return true;
            }
        }
        return false;
    }


    private void addContentInternal(BaseItemDto[] items) {
        mItems.addAll(Arrays.asList(items));

        if (items.length == 0 || contentGrid.getAdapter() != null) {
            return;
        }

        double normalizedAspectRatio = LibraryTools.calculateNormalizedAspectRatio(mItems) ;
        int gridHeight = LibraryTools.calculateGridHeightFromAspectRatio(normalizedAspectRatio, contentGrid);
        Integer defaultImageResoureId = LibraryTools.getDefaultImageIdFromType(mItems.get(0).getType(), normalizedAspectRatio);

        ViewGroup.LayoutParams layoutParams = contentGrid.getLayoutParams();
        layoutParams.height = gridHeight;
        contentGrid.setLayoutParams(layoutParams);
        contentGrid.setRowHeight(gridHeight - 10);
        contentGrid.setAdapter(new HorizontalAdapterPosters(mItems, gridHeight, 1, defaultImageResoureId));
        Animation animation = AnimationUtils.loadAnimation(mLibraryActivity, R.anim.grow_vertical);
        contentGrid.startAnimation(animation);
        contentGrid.setVisibility(View.VISIBLE);
        contentGrid.requestFocus();
        contentGrid.setOnItemSelectedListener(onItemSelectedListener);
        contentGrid.setOnItemClickListener(new TwoWayAdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(TwoWayAdapterView<?> parent, View view, int position, long id) {
                if ("audio".equalsIgnoreCase(mItems.get(position).getType()) || "photo".equalsIgnoreCase(mItems.get(position).getType())) {
                    mPlayHelper.playItem(mLibraryActivity, mItems.get(position), 0L, null, null, null, true);
                } else {
                    if (mLibraryActivity != null) {
                        mLibraryActivity.navigate((BaseItemDto) parent.getSelectedItem(), null);
                    }
                }
            }
        });
        contentGrid.setOnItemLongClickListener(mOnItemLongClickListener);

        if (mItems != null && mItems.size() > 0) {
            View v = contentGrid.getSelectedView();
            onItemSelectedListener.onItemSelected(contentGrid, v, 0, contentGrid.getAdapter().getItemId(0));

            if ("episode".equalsIgnoreCase(mItems.get(0).getType()) && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(mItems.get(0).getParentBackdropItemId())) {
                MB3Application.getInstance().API.GetItemAsync(mItems.get(0).getParentBackdropItemId(), MB3Application.getInstance().API.getCurrentUserId(), new GetParentResponse());
            }
        }
    }

    private class GetParentResponse extends Response<BaseItemDto> {
        @Override
        public void onResponse(BaseItemDto item) {
            if (item == null || mLibraryActivity == null) return;
            mLibraryActivity.populateBackdrops(item);
        }
    }


    private Runnable textScroller = new Runnable() {

        int previousScrollY = -1;
        boolean postScrollDelayObserved = false;

        @Override
        public void run() {
            if (overviewScroller == null) {
                return;
            }
            if (postScrollDelayObserved) {
                overviewScroller.smoothScrollTo(0,0);
                postScrollDelayObserved = false;
            }
            if (overviewScroller.getScrollY() == 0 && previousScrollY != -1) {
                // We've just returned to the top from previous scroll
                previousScrollY = -1;
                overviewScroller.postDelayed(this, 5000);
            } else if (overviewScroller.getScrollY() != previousScrollY) {
                // We're scrolling
                previousScrollY = overviewScroller.getScrollY();
                overviewScroller.smoothScrollTo(0, overviewScroller.getScrollY() + 1);
                overviewScroller.postDelayed(this, 100);
            } else {
                // we can't scroll further
                overviewScroller.postDelayed(this, 5000);
                postScrollDelayObserved = true;
            }
        }
    };

    private void hideDetails() {
        if (mDetailsPane == null) return;
        mDetailsPane.setVisibility(View.INVISIBLE);
    }

    private void showDetails() {
        if (mDetailsPane == null || fadeInAnimation == null) return;
        mDetailsPane.startAnimation(fadeInAnimation);
        mDetailsPane.setVisibility(View.VISIBLE);
    }
}
