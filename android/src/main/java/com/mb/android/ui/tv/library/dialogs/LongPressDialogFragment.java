package com.mb.android.ui.tv.library.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.activities.mobile.SeriesViewActivity;
import com.mb.android.ui.tv.library.interfaces.ILongPressDialogListener;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;

import java.util.ArrayList;
import java.util.List;


public class LongPressDialogFragment extends DialogFragment {

    private NetworkImageView mHeaderImage;
    private BaseItemDto mItem;
    private List<String> mActions;

    public void setItem(BaseItemDto item) {
        mItem = item;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams")
        View mDialogContent = inflater.inflate(R.layout.fragment_latest_items_popup, null);
        mHeaderImage = (NetworkImageView) mDialogContent.findViewById(R.id.ivLatestItemsHeaderImage);

        setImage(mDialogContent);
        getListContent();

        ListView actionsList = (ListView) mDialogContent.findViewById(R.id.lvLatestItems);
        actionsList.setAdapter(new ActionAdapter());
        actionsList.setOnItemClickListener(onItemClickListener);

        builder.setView(mDialogContent);

        return builder.create();

    }


    private void setImage(View view) {

        if (mItem.getHasBanner()) {
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Banner);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(mItem.getId(), options);
            mHeaderImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            mHeaderImage.setOnClickListener(onHeaderClickListener);
        } else {
            view.findViewById(R.id.rlBannerContainer).setVisibility(View.GONE);
        }

    }


    private void getListContent() {

        if (mItem == null) return;

        mActions = new ArrayList<>();

        if (mItem.getUserData() != null) {

            if (!"person".equalsIgnoreCase(mItem.getType())) {
                mActions.add(mItem.getUserData().getPlayed() ? "Set Unplayed" : "Set Played");
            }

            if (mItem.getUserData().getLikes() == null) {
                mActions.add("Like");
                mActions.add("Dislike");
            } else if (!mItem.getUserData().getLikes()) {
                mActions.add("Like");
                mActions.add("Clear Rating");
            } else {
                mActions.add("Dislikes");
                mActions.add("Clear Rating");
            }
            mActions.add(mItem.getUserData().getIsFavorite() ? "Clear Favorite" : "Set Favorite");
        }

    }


    private class ActionAdapter extends BaseAdapter {

        private LayoutInflater mLayoutInflater;


        public ActionAdapter() {
            mLayoutInflater = (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mActions.size();
        }

        @Override
        public Object getItem(int position) {
            return mActions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ActionHolder actionHolder;

            if (convertView == null) {

                convertView = mLayoutInflater.inflate(R.layout.widget_long_press_item, parent, false);

                actionHolder = new ActionHolder();
                actionHolder.actionImage = (NetworkImageView) convertView.findViewById(R.id.ivActionImage);
                actionHolder.actionText = (TextView) convertView.findViewById(R.id.tvActionText);

                convertView.setTag(actionHolder);

            } else {
                actionHolder = (ActionHolder) convertView.getTag();
            }

            actionHolder.actionText.setText(mActions.get(position));

            switch(mActions.get(position)) {
                case "Like":
                    actionHolder.actionImage.setDefaultImageResId(R.drawable.like_off);
                    break;
                case "Dislike":
                    actionHolder.actionImage.setDefaultImageResId(R.drawable.dislike_off);
                    break;
                case "Clear Rating":
                    actionHolder.actionImage.setDefaultImageResId(R.drawable.like_off);
                    break;
                case "Set Unplayed":
                    actionHolder.actionImage.setDefaultImageResId(R.drawable.set_unplayed);
                    break;
                case "Set Played":
                    actionHolder.actionImage.setDefaultImageResId(R.drawable.set_played);
                    break;
                case "Set Favorite":
                    actionHolder.actionImage.setDefaultImageResId(R.drawable.favorite_off);
                    break;
                case "Clear Favorite":
                    actionHolder.actionImage.setDefaultImageResId(R.drawable.favorite_off);
                    break;
            }

            return convertView;
        }
    }

    private View.OnClickListener onHeaderClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent intent = new Intent(MB3Application.getInstance(), SeriesViewActivity.class);
            intent.putExtra("SeriesId", mItem.getId());

            startActivity(intent);
            LongPressDialogFragment.this.dismiss();
        }
    };

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            switch(mActions.get(position)) {
                case "Like":
                    MB3Application.getInstance().API.UpdateUserItemRatingAsync(mItem.getId(),
                            MB3Application.getInstance().API.getCurrentUserId(), true, updateUserDataResponse);
                    break;
                case "Dislike":
                    MB3Application.getInstance().API.UpdateUserItemRatingAsync(mItem.getId(),
                            MB3Application.getInstance().API.getCurrentUserId(), false, updateUserDataResponse);
                    break;
                case "Clear Rating":
                    MB3Application.getInstance().API.ClearUserItemRatingAsync(mItem.getId(),
                            MB3Application.getInstance().API.getCurrentUserId(), updateUserDataResponse);
                    break;
                case "Set Unplayed":
                    MB3Application.getInstance().API.MarkUnplayedAsync(mItem.getId(),
                            MB3Application.getInstance().API.getCurrentUserId(), updateUserDataResponse);
                    break;
                case "Set Played":
                    MB3Application.getInstance().API.MarkPlayedAsync(mItem.getId(),
                            MB3Application.getInstance().API.getCurrentUserId(), null, updateUserDataResponse);
                    break;
                case "Set Favorite":
                    MB3Application.getInstance().API.UpdateFavoriteStatusAsync(mItem.getId(),
                            MB3Application.getInstance().API.getCurrentUserId(), true, updateUserDataResponse);
                    break;
                case "Clear Favorite":
                    MB3Application.getInstance().API.UpdateFavoriteStatusAsync(mItem.getId(),
                            MB3Application.getInstance().API.getCurrentUserId(), false, updateUserDataResponse);
                    break;
            }
        }
    };

    private Response<UserItemDataDto> updateUserDataResponse = new Response<UserItemDataDto>() {
        @Override
        public void onResponse(UserItemDataDto itemDataDto) {

            ILongPressDialogListener listener = (ILongPressDialogListener) getActivity();
            if (listener != null) {
                listener.onUserDataChanged(mItem.getId(), itemDataDto);
            }
            LongPressDialogFragment.this.dismiss();
        }
        @Override
        public void onError(Exception ex) {
            LongPressDialogFragment.this.dismiss();
        }
    };

    private class ActionHolder {
        public TextView actionText;
        public NetworkImageView actionImage;
    }
}
