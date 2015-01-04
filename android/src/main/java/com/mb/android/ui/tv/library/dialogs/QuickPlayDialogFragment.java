package com.mb.android.ui.tv.library.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.Playlist;
import com.mb.android.R;
import com.mb.android.activities.mobile.SeriesViewActivity;
import com.mb.android.ui.tv.library.interfaces.IQuickPlayDialogListener;
import mediabrowser.apiinteraction.Response;
import com.mb.android.logging.FileLogger;
import com.mb.android.ui.tv.playback.PlayerHelpers;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemsResult;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Mark on 12/12/13.
 *
 * When a user enters a new address for an existing server (through the add new server button). This
 * dialog will show the user the existing addresses and confirm which one the user wants to overwrite.
 *
 * The user must select either the local or remote address before continuing.
 */
public class QuickPlayDialogFragment extends DialogFragment {

    private BaseItemDto mParent;
    private NetworkImageView mHeaderImage;
    private ListView mLatestItemsList;
    private BaseItemDto mFirstUnplayedItem;
    private boolean isAudio;

    public void setData(BaseItemDto baseItemDto, boolean isAudio) {
        mParent = baseItemDto;
        this.isAudio = isAudio;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams")
        View mDialogContent = inflater.inflate(R.layout.fragment_latest_items_popup, null);
        mHeaderImage = (NetworkImageView) mDialogContent.findViewById(R.id.ivLatestItemsHeaderImage);
        mLatestItemsList = (ListView) mDialogContent.findViewById(R.id.lvLatestItems);

        setImage(mDialogContent);
        getListContent();

        builder.setView(mDialogContent);

        return builder.create();

    }


    private void setImage(View view) {

        if (mParent.getHasBanner()) {
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Banner);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(mParent.getId(), options);
            mHeaderImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            mHeaderImage.setOnClickListener(onHeaderClickListener);
        } else {
            view.findViewById(R.id.rlBannerContainer).setVisibility(View.GONE);
        }

    }


    private void getListContent() {

        if (!"CollectionFolder".equalsIgnoreCase(mParent.getType())
                && mParent.getUserData() != null
                && mParent.getUserData().getUnplayedItemCount() != null
                && mParent.getUserData().getUnplayedItemCount() > 0) {

            ItemQuery query = new ItemQuery();
            query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
            query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.ParentId, ItemFields.DateCreated});
            query.setSortBy(new String[] {"SortName"});
            query.setSortOrder(SortOrder.Ascending);
            query.setIsPlayed(false);
            query.setParentId(mParent.getId());
            query.setRecursive(true);
            query.setIsVirtualUnaired(false);
            query.setLimit(1);
            query.setMediaTypes(isAudio ? new String[] {"audio"} : new String[] {"video"});
            if ("series".equalsIgnoreCase(mParent.getType()) || "season".equalsIgnoreCase(mParent.getType())) {
                query.setIncludeItemTypes(new String[]{"episode"});
            }

            MB3Application.getInstance().API.GetItemsAsync(query, getUnplayedItemResponse);
        } else {
            // just show play-all & shuffle buttons
            mLatestItemsList.setAdapter(new LatestEpisodesAdapter());
            mLatestItemsList.setOnItemClickListener(onItemClickListener);
        }
    }


    private Response<ItemsResult> getUnplayedItemResponse = new Response<ItemsResult>() {
        @Override
        public void onResponse(ItemsResult result) {
            if (result == null || result.getItems() == null || result.getItems().length == 0) {
                Toast.makeText(getActivity(), "Error in onResponse", Toast.LENGTH_LONG).show();
                return;
            }
            mFirstUnplayedItem = result.getItems()[0];

            mLatestItemsList.setAdapter(new LatestEpisodesAdapter());
            mLatestItemsList.setOnItemClickListener(onItemClickListener);
        }
        @Override
        public void onError(Exception ex) {
            Toast.makeText(getActivity(), "Error getting item", Toast.LENGTH_LONG).show();
            mLatestItemsList.setAdapter(new LatestEpisodesAdapter());
            mLatestItemsList.setOnItemClickListener(onItemClickListener);
        }
    };


    private class LatestEpisodesAdapter extends BaseAdapter {

        private LayoutInflater mLayoutInflater;
        private float mDensity;


        public LatestEpisodesAdapter() {
            mLayoutInflater = (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            DisplayMetrics metrics = MB3Application.getInstance().getResources().getDisplayMetrics();
            mDensity = metrics.density;
        }

        @Override
        public int getCount() {
            return mFirstUnplayedItem != null ? 3 : 2;
        }

        @Override
        public Object getItem(int position) {
            return mFirstUnplayedItem;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            EpisodeHolder episodeHolder;

            if (convertView == null) {

                convertView = mLayoutInflater.inflate(R.layout.widget_quickplay_item, parent, false);

                episodeHolder = new EpisodeHolder();
                episodeHolder.addedDate = (TextView) convertView.findViewById(R.id.tvAddedDate);
                episodeHolder.episodeImage = (NetworkImageView) convertView.findViewById(R.id.ivEpisodeImage);
                episodeHolder.episodeTitle = (TextView) convertView.findViewById(R.id.tvEpisodeTitle);
                episodeHolder.actionText = (TextView) convertView.findViewById(R.id.tvActionText);

                convertView.setTag(episodeHolder);

            } else {
                episodeHolder = (EpisodeHolder) convertView.getTag();
            }

            if (position == 0) {
                episodeHolder.actionText.setText("Play All");
                episodeHolder.actionText.setVisibility(View.VISIBLE);
                episodeHolder.episodeTitle.setVisibility(View.GONE);
                episodeHolder.addedDate.setVisibility(View.GONE);
                episodeHolder.episodeImage.setDefaultImageResId(R.drawable.play);
                episodeHolder.episodeImage.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
            } else if (position == 1) {
                episodeHolder.actionText.setText("Shuffle");
                episodeHolder.actionText.setVisibility(View.VISIBLE);
                episodeHolder.episodeTitle.setVisibility(View.GONE);
                episodeHolder.addedDate.setVisibility(View.GONE);
                episodeHolder.episodeImage.setDefaultImageResId(R.drawable.shuffle);
                episodeHolder.episodeImage.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
            } else {
                episodeHolder.episodeTitle.setText("First Unplayed");
                if (mFirstUnplayedItem.getParentIndexNumber() != null && mFirstUnplayedItem.getIndexNumber() != null) {
                    try {
                        String epTitle = String.valueOf(mFirstUnplayedItem.getParentIndexNumber()) + "." +
                                String.valueOf(mFirstUnplayedItem.getIndexNumber());
                        if (mFirstUnplayedItem.getIndexNumberEnd() != null && !mFirstUnplayedItem.getIndexNumber().equals(mFirstUnplayedItem.getIndexNumberEnd())) {
                            epTitle += "-" + String.valueOf(mFirstUnplayedItem.getIndexNumberEnd());
                        }

                        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(epTitle)) {
                            epTitle += " - ";
                        }

                        epTitle += mFirstUnplayedItem.getName();
                        episodeHolder.addedDate.setText(epTitle);
                    } catch (Exception e) {
                        FileLogger.getFileLogger().ErrorException("Error setting episode text", e);
                        episodeHolder.addedDate.setText(mFirstUnplayedItem.getName());
                    }
                } else {
                    episodeHolder.addedDate.setText(mFirstUnplayedItem.getName());
                }

                if (mFirstUnplayedItem.getHasPrimaryImage()) {
                    ImageOptions options = new ImageOptions();
                    options.setImageType(ImageType.Primary);
                    options.setWidth((int) (140 * mDensity));

                    String imageUrl = MB3Application.getInstance().API.GetImageUrl(mFirstUnplayedItem.getId(), options);
                    episodeHolder.episodeImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
                }
            }
            return convertView;
        }
    }

    private View.OnClickListener onHeaderClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent intent = new Intent(MB3Application.getInstance(), SeriesViewActivity.class);
            intent.putExtra("SeriesId", mParent.getId());

            startActivity(intent);
            QuickPlayDialogFragment.this.dismiss();
        }
    };

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            IQuickPlayDialogListener activity = (IQuickPlayDialogListener)getActivity();
            if (activity == null) return;

            if (position == 0) {
                getAllChildren(mParent, isAudio, false);
            } else if (position == 1) {
                getAllChildren(mParent, isAudio, true);
            } else if (position == 2) {
                MB3Application.getInstance().PlayerQueue = new Playlist();
                PlayerHelpers.addToPlaylist(mFirstUnplayedItem, 0L, null, null);
                activity.onQuickPlaySelectionFinished();
                QuickPlayDialogFragment.this.dismiss();
            }
        }
    };

    private void getAllChildren(BaseItemDto item, boolean isAudio, boolean shuffle) {
        ItemQuery query = new ItemQuery();
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setSortBy(new String[]{"SortName"});
        query.setSortOrder(SortOrder.Ascending);
        query.setParentId(item.getId());
        query.setRecursive(true);
        query.setIsVirtualUnaired(false);
        query.setStartIndex(0);
        query.setLimit(200);
        query.setMediaTypes(isAudio ? new String[] {"audio"} : new String[] {"video"});

        MB3Application.getInstance().PlayerQueue = new Playlist();
        MB3Application.getInstance().API.GetItemsAsync(query, new getAllChildrenResponse(shuffle, query));
    }

    private class getAllChildrenResponse extends Response<ItemsResult> {

        private boolean mShuffle;
        private ItemQuery mQuery;

        public getAllChildrenResponse(boolean shuffle, ItemQuery query) {
            mShuffle = shuffle;
            mQuery = query;
        }

        @Override
        public void onResponse(ItemsResult result) {
            FileLogger.getFileLogger().Info("QuickPlayDialogFragment: get all children onResponse");
            ArrayList<BaseItemDto> items = new ArrayList<>();
            Collections.addAll(items, result.getItems());
            if (mShuffle) {
                Collections.shuffle(items);
            }
            PlayerHelpers.addToPlaylist(items);
            mQuery.setStartIndex(mQuery.getStartIndex() + items.size());
            if (result.getTotalRecordCount()  > mQuery.getStartIndex() + 1) {
                MB3Application.getInstance().API.GetItemsAsync(mQuery, this);
            } else {
                IQuickPlayDialogListener activity = (IQuickPlayDialogListener) getActivity();
                if (items.size() > 0 && activity != null) {
                    activity.onQuickPlaySelectionFinished();
                } else {
                    FileLogger.getFileLogger().Info("QuickPlayDialogFragment: no items to add to playlist");
                }
                QuickPlayDialogFragment.this.dismiss();
            }


        }
        @Override
        public void onError(Exception ex) {
            FileLogger.getFileLogger().Info("QuickPlayDialogFragment: get all children onError");
            QuickPlayDialogFragment.this.dismiss();
        }
    }

    private class EpisodeHolder {
        public TextView episodeTitle;
        public TextView addedDate;
        public TextView actionText;
        public NetworkImageView episodeImage;
    }
}
