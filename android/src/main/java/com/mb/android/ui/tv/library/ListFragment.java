package com.mb.android.ui.tv.library;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.ui.tv.playback.PlayerHelpers;
import com.mb.android.utils.Utils;
import com.mb.android.widget.AnimatedNetworkImageView;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.LocationType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by Mark on 2014-11-12.
 *
 * Fragment that shows the users library content in a standard List View style
 */
public class ListFragment extends BaseLibraryFragment {

    private static final String TAG = "ListFragment";
    private ArrayList<BaseItemDto> mItems;
    private LibraryActivity mLibraryActivity;
    private LinearLayout listContainer;
    private AnimatedNetworkImageView banner;
    private ListView contentList;
    private TextView mediaTitle;
    private TextView episodeIndexes;
    private TextView airingInfo;
    private TextView runtime;
    private TextView mediaOverview;
    private TextView hiddenOverview;
    private ScrollView overviewScroller;
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
    private BaseItemDto parent;

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
        View view = inflater.inflate(R.layout.tv_fragment_library_list, container, false);

        listContainer = (LinearLayout) view.findViewById(R.id.llListContainer);
        banner = (AnimatedNetworkImageView) view.findViewById(R.id.ivBannerImage);
        contentList = (ListView) view.findViewById(R.id.lvContent);
        mediaTitle = (TextView) view.findViewById(R.id.tvMediaTitle);
        episodeIndexes = (TextView) view.findViewById(R.id.tvEpisodeIndexes);
        airingInfo = (TextView) view.findViewById(R.id.tvAiringInfo);
        runtime = (TextView) view.findViewById(R.id.tvRuntime);
        mediaOverview = (TextView) view.findViewById(R.id.tvMediaOverview);
        hiddenOverview = (TextView) view.findViewById(R.id.tvMediaOverviewHidden);
        overviewScroller = (ScrollView) view.findViewById(R.id.svOverviewScrollView);
        mStarRating = (ImageView) view.findViewById(R.id.ivStarImage);
        mRtImage = (ImageView) view.findViewById(R.id.ivCriticFreshRottenImage);
        mRtRating = (TextView) view.findViewById(R.id.tvRtRating);
        mMetaScore = (TextView) view.findViewById(R.id.tvMetaScore);
        mOfficialRating = (ImageView) view.findViewById(R.id.ivOfficialRating);
        mOfficialRatingText = (TextView) view.findViewById(R.id.tvOfficialRating);
        mDetailsPane = (LinearLayout) view.findViewById(R.id.llDetails);
        mItems = new ArrayList<>();
        mPlayHelper = new PlayerHelpers();
        fadeInAnimation = AnimationUtils.loadAnimation(mLibraryActivity, R.anim.fade_in);
        fadeInAnimation.setDuration(2500);
        return view;
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
            if (parent == null || !parent.getHasBanner()) {
                banner.setVisibility(View.GONE);
            } else {
                ImageOptions options = new ImageOptions();
                options.setImageType(ImageType.Banner);
                banner.setImageUrl(
                        MB3Application.getInstance().API.GetImageUrl(parent.getId(), options),
                        MB3Application.getInstance().API.getImageLoader()
                );
                banner.setVisibility(View.VISIBLE);
            }
        }
    }

    private AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (mItems == null || mItems.size() <= position) return;
            hideDetails();
            BaseItemDto item = mItems.get(position);
            if (item == null) return;
            mediaTitle.setText(item.getName());
            mediaOverview.setText(item.getOverview() != null ? item.getOverview() : "");
            hiddenOverview.setText(item.getOverview() != null ? item.getOverview() : "");
            if (mLibraryActivity != null) {
                mLibraryActivity.populateBackdrops(item);
            }
            setEpisodeIndexes(item);
            populateRatingInfo(item);
            populateAiringInfo(item);

            if (item.getRunTimeTicks() != null) {
                runtime.setText(Utils.TicksToMinutesString(item.getRunTimeTicks()));
            }

            overviewScroller.removeCallbacks(textScroller);
            overviewScroller.scrollTo(0,0);
            overviewScroller.postDelayed(textScroller, 5000); // Wait 5 seconds before scrolling initially
            showDetails();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private AdapterView.OnItemLongClickListener onItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (mItems == null || mItems.size() <= position) {
                return false;
            }
            if (mLibraryActivity == null) return false;

            mLibraryActivity.showLongPressDialog(mItems.get(position));

            return true;
        }
    };


    private void setEpisodeIndexes(BaseItemDto item) {
        if ("episode".equalsIgnoreCase(item.getType())) {
            episodeIndexes.setVisibility(View.VISIBLE);
            episodeIndexes.setText(Utils.getLongEpisodeIndexString(item));
        } else {
            episodeIndexes.setVisibility(View.GONE);
        }
    }

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

    int retries = 0;

    public void setParent(BaseItemDto parent) {
        this.parent = parent;
    }

    @Override
    public void addContent(final BaseItemDto[] items) {

        if (contentList == null) {

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (retries < 10) {
                        if (contentList == null) {
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
        if (contentList != null && contentList.getSelectedItemPosition() >= 0) {
            return (BaseItemDto)contentList.getSelectedItem();
        }
        return null;
    }

    @Override
    public void refreshData(BaseItemDto item) {
        if (contentList != null) {
            TextListAdapter adapter = (TextListAdapter) contentList.getAdapter();
            boolean insertSucceeded = Utils.insertIntoDataset(item, mItems);
            if (adapter != null && insertSucceeded) {
                adapter.notifyDataSetChanged();
            }
        }
    }


    @Override
    public boolean onDpadUpHandled() {
        if (contentList != null && contentList.getSelectedItemPosition() == 0) {
            if (contentList.getCount() > 0) {
                int targetIndex = contentList.getCount() - 1;
                contentList.smoothScrollToPosition(targetIndex);
                contentList.setSelection(targetIndex);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDpadDownHandled() {
        if (contentList != null && contentList.getCount() > 0) {
            if (contentList.getSelectedItemPosition() == contentList.getCount() - 1) {
                contentList.smoothScrollToPosition(0);
                contentList.setSelection(0);
                return true;
            }
        }
        return false;
    }

    private void addContentInternal(BaseItemDto[] items) {
        mItems.addAll(Arrays.asList(items));

        if (contentList.getAdapter() == null) {
            contentList.setAdapter(new TextListAdapter());
            Animation animation = AnimationUtils.loadAnimation(mLibraryActivity, R.anim.grow_horizontal_left);
            listContainer.startAnimation(animation);
            listContainer.setVisibility(View.VISIBLE);

            contentList.setOnItemSelectedListener(onItemSelectedListener);
            contentList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if ("audio".equalsIgnoreCase(mItems.get(position).getType()) || "photo".equalsIgnoreCase(mItems.get(position).getType())) {
                        mPlayHelper.playItem(mLibraryActivity, mItems.get(position), 0L, null, null, null, true);
                    } else {
                        if (mLibraryActivity != null) {
                            mLibraryActivity.navigate((BaseItemDto) parent.getSelectedItem(), null);
                        }
                    }
                }
            });
            contentList.setOnItemLongClickListener(onItemLongClickListener);
            contentList.requestFocus();

            if (mItems != null && mItems.size() > 0) {
                View v = contentList.getSelectedView();
                onItemSelectedListener.onItemSelected(contentList, v, 0, contentList.getAdapter().getItemId(0));

                if ("episode".equalsIgnoreCase(mItems.get(0).getType()) && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(mItems.get(0).getParentBackdropItemId())) {
                    MB3Application.getInstance().API.GetItemAsync(mItems.get(0).getParentBackdropItemId(), MB3Application.getInstance().API.getCurrentUserId(), new GetParentResponse());
                }
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

    private class TextListAdapter extends BaseAdapter {

        private LayoutInflater inflater;

        public TextListAdapter() {
            inflater = (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.widget_banner_text_tile, parent, false);
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.tvViewTitle);
                holder.banner = (AnimatedNetworkImageView) convertView.findViewById(R.id.ivBannerImage);
                holder.watchedStatusOverlay = (TextView) convertView.findViewById(R.id.tvOverlay);
                holder.missingEpisodeOverlay = (TextView) convertView.findViewById(R.id.tvMissingEpisodeOverlay);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (mItems.get(position).getHasBanner()) {
                ImageOptions options = new ImageOptions();
                options.setImageType(ImageType.Banner);
                options.setWidth(700);
                String url = MB3Application.getInstance().API.GetImageUrl(mItems.get(position), options);
                holder.banner.setImageUrl(url, MB3Application.getInstance().API.getImageLoader());
                holder.banner.setVisibility(View.VISIBLE);
                holder.title.setVisibility(View.INVISIBLE);
            } else {
                holder.title.setText(mItems.get(position).getName());
                holder.title.setVisibility(View.VISIBLE);
                holder.banner.setVisibility(View.INVISIBLE);
            }

            // Process top-right overlays

            if (mItems.get(position).getLocationType().equals(LocationType.Virtual) && mItems.get(position).getType().equalsIgnoreCase("episode")) {
                holder.watchedStatusOverlay.setVisibility(View.INVISIBLE);

                if (mItems.get(position).getPremiereDate() != null) {

                    Date premiereDate = Utils.convertToLocalDate(mItems.get(position).getPremiereDate());

                    long premiereDateMs = premiereDate.getTime();
                    long currentMs = new Date().getTime();

                    if (premiereDateMs - currentMs > 0)
                        holder.missingEpisodeOverlay.setText("UNAIRED");
                }

                holder.missingEpisodeOverlay.setVisibility(TextView.VISIBLE);
            } else {
                holder.missingEpisodeOverlay.setVisibility(TextView.GONE);
                if (mItems.get(position).getType().equalsIgnoreCase("boxset") ||
                        mItems.get(position).getType().equalsIgnoreCase("season") ||
                        mItems.get(position).getType().equalsIgnoreCase("series")) {
                    if (mItems.get(position).getUserData().getUnplayedItemCount() != null && mItems.get(position).getUserData().getUnplayedItemCount() > 0) {
                        holder.watchedStatusOverlay.setText(String.valueOf(mItems.get(position).getUserData().getUnplayedItemCount()));
                        holder.watchedStatusOverlay.setVisibility(View.VISIBLE);
                    } else if (mItems.get(position).getUserData().getUnplayedItemCount() != null && mItems.get(position).getUserData().getUnplayedItemCount() == 0) {
                        holder.watchedStatusOverlay.setText("\u2714");
                        holder.watchedStatusOverlay.setVisibility(View.VISIBLE);
                    } else {
                        holder.watchedStatusOverlay.setVisibility(View.INVISIBLE);
                    }
                } else {
                    boolean watched = false;

                    if (mItems.get(position).getUserData() != null) {
                        watched = mItems.get(position).getUserData().getPlayed();
                    }

                    if (watched) {
                        holder.watchedStatusOverlay.setText("\u2714");
                        holder.watchedStatusOverlay.setVisibility(View.VISIBLE);
                    } else {
                        holder.watchedStatusOverlay.setVisibility(View.INVISIBLE);
                    }
                }
            }

            return convertView;
        }
    }

    private class ViewHolder {
        public TextView title;
        public TextView watchedStatusOverlay;
        public TextView missingEpisodeOverlay;
        public AnimatedNetworkImageView banner;
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
