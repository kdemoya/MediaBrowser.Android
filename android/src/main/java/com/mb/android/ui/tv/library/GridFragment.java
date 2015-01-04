package com.mb.android.ui.tv.library;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jess.ui.TwoWayAdapterView;
import com.jess.ui.TwoWayGridView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.adapters.HorizontalAdapterBackdrops;
import com.mb.android.adapters.HorizontalAdapterPosters;
import com.mb.android.ui.tv.playback.PlayerHelpers;
import com.mb.android.utils.Utils;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.extensions.StringHelper;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Mark on 2014-11-15.
 *
 * Fragment that shows the contents of the users library in a poster wall layout
 */
public class GridFragment extends BaseLibraryFragment {

    private static final String TAG = "PosterFragment";
    private ArrayList<BaseItemDto> mItems;
    private PlayerHelpers mPlayHelper;
    private LibraryActivity mLibraryActivity;
    private TwoWayGridView mItemsGrid;
    private TextView mEpisodeIndexes;
    private TextView mMediaTitle;
    private TextView mRuntime;
    private ImageView mStarRating;
    private ImageView mRtImage;
    private TextView mReleaseYear;
    private TextView mOfficialRating;
    private TextView mRtRating;
    private TextView mMetaScore;
    private TextView mCurrentItemIndex;
    private TextView mTotalRecordCount;
    private int maxTitleWidthDp;
    private int totalItemCount;
    private int mSavedPosition = -1;
    private boolean mIsFresh = true;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics dm = MB3Application.getInstance().getResources().getDisplayMetrics();
        if (dm != null) {
            maxTitleWidthDp = (int) ((float)dm.widthPixels * .60);
        }
        mPlayHelper = new PlayerHelpers();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tv_fragment_library_poster, container, false);
        mItemsGrid = (TwoWayGridView) view.findViewById(R.id.gridview);
        mEpisodeIndexes = (TextView) view.findViewById(R.id.tvEpisodeIndexes);
        mMediaTitle = (TextView) view.findViewById(R.id.tvMediaTitle);
        mMediaTitle.setMaxWidth(maxTitleWidthDp);
        mStarRating = (ImageView) view.findViewById(R.id.ivStarImage);
        mRtImage = (ImageView) view.findViewById(R.id.ivCriticFreshRottenImage);
        mRtRating = (TextView) view.findViewById(R.id.tvRtRating);
        mMetaScore = (TextView) view.findViewById(R.id.tvMetaScore);
        mCurrentItemIndex = (TextView) view.findViewById(R.id.tvCurrentItemIndex);
        mTotalRecordCount = (TextView) view.findViewById(R.id.tvTotalRecordCount);
        mRuntime = null; // (TextView) view.findViewById(R.id.tvRuntimeSide);
        mReleaseYear = null;
        mOfficialRating = null;
//        mSavedPosition = getArguments().getInt("SavedPosition", -1);

        mItems = new ArrayList<>();


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            performInitialTasks();
        }
    }

    private void performInitialTasks() {
    }

    private TwoWayAdapterView.OnItemClickListener mOnItemClickListener = new TwoWayAdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(TwoWayAdapterView<?> parent, View view, int position, long id) {
            if (mItems == null || mItems.size() <= position) return;
            if ("audio".equalsIgnoreCase(mItems.get(position).getType()) || "photo".equalsIgnoreCase(mItems.get(position).getType())) {
                mPlayHelper.playItem(mLibraryActivity, mItems.get(position), 0L, null, null, null, true);
            } else {
//                Toast.makeText(mLibraryActivity, "Type: " + String.valueOf(mItems.get(position).getType()), Toast.LENGTH_LONG).show();
                if (mLibraryActivity != null) {
                    mLibraryActivity.navigate(mItems.get(position), null);
                }
            }
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

    private TwoWayAdapterView.OnItemSelectedListener mOnItemSelectedListener = new TwoWayAdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(TwoWayAdapterView<?> parent, View view, int position, long id) {
            if (mLibraryActivity != null) {
                mLibraryActivity.populateBackdrops(mItems.get(position));
            }
            populateItemInfo(mItems.get(position));
            setCurrentIndex(position + 1);
        }

        @Override
        public void onNothingSelected(TwoWayAdapterView<?> parent) {

        }
    };

    private void populateItemInfo(BaseItemDto item) {

        if ("episode".equalsIgnoreCase(item.getType())) {
            mEpisodeIndexes.setVisibility(View.VISIBLE);
            mEpisodeIndexes.setText(Utils.getShortEpisodeIndexString(item));
        } else {
            mEpisodeIndexes.setVisibility(View.GONE);
        }
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

        if (mRuntime != null) {
            if ( !"series".equalsIgnoreCase(item.getType())) {
                mRuntime.setText(Utils.TicksToMinutesString(item.getCumulativeRunTimeTicks() != null
                                ? item.getCumulativeRunTimeTicks()
                                : item.getRunTimeTicks() != null
                                ? item.getRunTimeTicks()
                                : 0)
                );
                mRuntime.setVisibility(View.VISIBLE);
            } else {
                mRuntime.setVisibility(View.GONE);
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

    private void setCurrentIndex(int index) {
        mCurrentItemIndex.setText(String.valueOf(index));
    }

    private void setTotalRecordCount(int count) {
        mTotalRecordCount.setText(String.valueOf(count));
    }

    int retries = 0;

    @Override
    public void addContent(final BaseItemDto[] items) {
        if (mItemsGrid == null) {

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (retries < 10) {
                        if (mItemsGrid == null) {
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
        if (mItemsGrid != null && mItemsGrid.getSelectedItemPosition() >= 0) {
            return (BaseItemDto)mItemsGrid.getSelectedItem();
        }
        return null;
    }

    @Override
    public void refreshData(BaseItemDto item) {
        if (mItemsGrid != null) {
            HorizontalAdapterBackdrops adapter = (HorizontalAdapterBackdrops)mItemsGrid.getAdapter();
            boolean insertSucceeded = Utils.insertIntoDataset(item, mItems);
            if (adapter != null && insertSucceeded) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onDpadLeftHandled() {
        if (mItemsGrid != null && mItemsGrid.getCount() > 0 && mItemsGrid.getSelectedItemPosition() <= 2) {
            int targetIndex = mItemsGrid.getCount() - 1;
            mItemsGrid.smoothScrollToPosition(targetIndex);
            mItemsGrid.setSelection(targetIndex);
            return true;
        }
        return false;
    }

    @Override
    public boolean onDpadRightHandled() {
        if (mItemsGrid != null && mItemsGrid.getCount() > 0) {
            if (mItemsGrid.getSelectedItemPosition() + mItemsGrid.getNumRows() > mItemsGrid.getCount()) {
                mItemsGrid.smoothScrollToPosition(0);
                mItemsGrid.setSelection(0);
                return true;
            }
        }
        return false;
    }

    private void addContentInternal(BaseItemDto[] items) {
        mItems.addAll(Arrays.asList(items));

        if (mItems.size() > 0 && mItemsGrid.getAdapter() == null) {
            DisplayMetrics dm = MB3Application.getInstance().getResources().getDisplayMetrics();
            int rows = 3;
            if (dm != null) {
                maxTitleWidthDp = (int) (dm.widthPixels * .65);
                mItemsGrid.setRowHeight((int) (mItemsGrid.getHeight() - (rows * (dm.density * 4))) / rows);
            }
            int defaultImageResId = LibraryTools.getDefaultImageIdFromType(mItems.get(0).getType(), 1.6);
            mItemsGrid.setAdapter(new HorizontalAdapterBackdrops(mLibraryActivity, mItems, mItemsGrid.getHeight(), rows, defaultImageResId));
            mItemsGrid.setOnItemClickListener(mOnItemClickListener);
            mItemsGrid.setOnItemLongClickListener(mOnItemLongClickListener);
            mItemsGrid.setOnItemSelectedListener(mOnItemSelectedListener);

            if (mItems.size() > 0) {
                View v = mItemsGrid.getSelectedView();
                mOnItemSelectedListener.onItemSelected(mItemsGrid, v, 0, mItemsGrid.getAdapter().getItemId(0));

                if ("episode".equalsIgnoreCase(mItems.get(0).getType()) && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(mItems.get(0).getParentBackdropItemId())) {
                    MB3Application.getInstance().API.GetItemAsync(mItems.get(0).getParentBackdropItemId(), MB3Application.getInstance().API.getCurrentUserId(), new GetParentResponse());
                }
            }
        }
        totalItemCount = mItems.size();
        setTotalRecordCount(totalItemCount);
    }

    private class GetParentResponse extends Response<BaseItemDto> {
        @Override
        public void onResponse(BaseItemDto item) {
            if (item == null || mLibraryActivity == null) return;
            mLibraryActivity.populateBackdrops(item);
        }
    }
}
