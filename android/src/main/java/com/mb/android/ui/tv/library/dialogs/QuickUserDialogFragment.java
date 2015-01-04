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
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.Playlist;
import com.mb.android.R;
import com.mb.android.activities.mobile.SeriesViewActivity;
import com.mb.android.logging.FileLogger;
import com.mb.android.player.AudioService;
import com.mb.android.ui.main.ConnectionActivity;
import com.mb.android.ui.main.UserAdapter;
import com.mb.android.ui.tv.library.interfaces.IQuickPlayDialogListener;
import com.mb.android.ui.tv.playback.PlayerHelpers;

import java.util.ArrayList;
import java.util.Collections;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.session.SessionInfoDto;
import mediabrowser.model.session.SessionUserInfo;

/**
 * Created by Mark on 12/12/13.
 *
 * When a user enters a new address for an existing server (through the add new server button). This
 * dialog will show the user the existing addresses and confirm which one the user wants to overwrite.
 *
 * The user must select either the local or remote address before continuing.
 */
public class QuickUserDialogFragment extends DialogFragment {

    private BaseItemDto mParent;
    private TextView mUserName;
    private NetworkImageView mHeaderImage;
    private ListView mLatestItemsList;
    private BaseItemDto mFirstUnplayedItem;
    private boolean isAudio;
    private SessionInfoDto mCurrentSession;


    public void setCurrentSessionInfo(SessionInfoDto session) {
        mCurrentSession = session;
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
        View mDialogContent = inflater.inflate(R.layout.fragment_switch_users_popup, null);
        mHeaderImage = (NetworkImageView) mDialogContent.findViewById(R.id.ivLatestItemsHeaderImage);
        mLatestItemsList = (ListView) mDialogContent.findViewById(R.id.lvLatestItems);
        mUserName = (TextView) mDialogContent.findViewById(R.id.tvUserName);
        mDialogContent.findViewById(R.id.btnLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MB3Application.getInstance().PlayerQueue = new Playlist();
                AudioService.PlayerState currentState = MB3Application.getAudioService().getPlayerState();
                if (currentState.equals(AudioService.PlayerState.PLAYING) || currentState.equals(AudioService.PlayerState.PAUSED)) {
                    MB3Application.getAudioService().stopMedia();
                }
                if (MB3Application.getInstance().API != null) {
                    MB3Application.getInstance().API.Logout(new EmptyResponse());
                }
                Intent intent = new Intent(MB3Application.getInstance(), ConnectionActivity.class);
                intent.putExtra("show_users", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                QuickUserDialogFragment.this.dismiss();
            }
        });

        setUserInfo();

        getListContent();

        builder.setView(mDialogContent);

        return builder.create();

    }


    private void setUserInfo() {

        if (MB3Application.getInstance().user.getHasPrimaryImage()) {
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setMaxHeight(170);
            options.setMaxWidth(170);

            String imageUrl = MB3Application.getInstance().API.GetUserImageUrl(MB3Application.getInstance().user.getId(), options);
            mHeaderImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
//            mHeaderImage.setOnClickListener(onHeaderClickListener);
        }
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(MB3Application.getInstance().user.getName())) {
            mUserName.setText(MB3Application.getInstance().user.getName());
        }

    }


    private void getListContent() {

        MB3Application.getInstance().API.GetPublicUsersAsync(getUsersResponse);
    }


    private Response<UserDto[]> getUsersResponse = new Response<UserDto[]>() {
        @Override
        public void onResponse(UserDto[] result) {
            if (result == null || result.length == 0) {
                Toast.makeText(getActivity(), "Error in onResponse", Toast.LENGTH_LONG).show();
                return;
            }

            mLatestItemsList.setAdapter(new SwitchUserAdapter(result, mCurrentSession));
            mLatestItemsList.setOnItemClickListener(onUserClickListener);
        }
        @Override
        public void onError(Exception ex) {
            Toast.makeText(getActivity(), "Error getting item", Toast.LENGTH_LONG).show();
        }
    };


    private class SwitchUserAdapter extends BaseAdapter {

        private ArrayList<UserDto> mUsers;
        private LayoutInflater mLayoutInflater;
        private float mDensity;
        private ArrayList<String> includedUsers;


        public SwitchUserAdapter(UserDto[] users, SessionInfoDto currentSession) {
            mLayoutInflater = (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            DisplayMetrics metrics = MB3Application.getInstance().getResources().getDisplayMetrics();
            mDensity = metrics.density;
            mUsers = new ArrayList<>();
            if (users != null) {
                for (UserDto user : users) {
                    if (!user.getId().equalsIgnoreCase(MB3Application.getInstance().API.getCurrentUserId())) {
                        mUsers.add(user);
                    }
                }
            }
            includedUsers = new ArrayList<>();
            if (currentSession != null && currentSession.getAdditionalUsers() != null && !currentSession.getAdditionalUsers().isEmpty()) {
                for (SessionUserInfo userInfo : currentSession.getAdditionalUsers()) {
                    includedUsers.add(userInfo.getUserId());
                }
            }
        }

        @Override
        public int getCount() {
            return mUsers.size();
        }

        @Override
        public Object getItem(int position) {
            return mUsers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            Holder holder;

            if (convertView == null) {

                convertView = mLayoutInflater.inflate(R.layout.widget_switch_user_tile, parent, false);

                holder = new Holder();
                holder.avatar = (NetworkImageView) convertView.findViewById(R.id.ivUserTileImage);
                holder.username = (TextView) convertView.findViewById(R.id.tvUserTileName);
                holder.alsoHere = (TextView) convertView.findViewById(R.id.tvAlsoHere);

                convertView.setTag(holder);

            } else {
                holder = (Holder) convertView.getTag();
            }

            holder.username.setText(mUsers.get(position).getName());

            if (mUsers.get(position).getHasPrimaryImage()) {
                ImageOptions imageOptions = new ImageOptions();
                imageOptions.setImageType(ImageType.Primary);
                imageOptions.setMaxWidth((int)(65 * mDensity));
                imageOptions.setMaxHeight((int)(65 * mDensity));

                String imageUrl = MB3Application.getInstance().API.GetUserImageUrl(mUsers.get(position), imageOptions);
                holder.avatar.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            }

            if (includedUsers.contains(mUsers.get(position).getId())) {
                holder.alsoHere.setVisibility(View.VISIBLE);
            } else {
                holder.alsoHere.setVisibility(View.INVISIBLE);
            }


            return convertView;
        }

        public void toggleSelected(int position) {
            if (includedUsers.contains(mUsers.get(position).getId())) {
                includedUsers.remove(mUsers.get(position).getId());
            } else {
                includedUsers.add(mUsers.get(position).getId());
            }
            notifyDataSetChanged();
        }

        public String getUserId(int position) {
            return mUsers.get(position).getId();
        }
    }

//    private View.OnClickListener onHeaderClickListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//
//            Intent intent = new Intent(MB3Application.getInstance(), SeriesViewActivity.class);
//            intent.putExtra("SeriesId", mParent.getId());
//
//            startActivity(intent);
//            QuickUserDialogFragment.this.dismiss();
//        }
//    };

    private AdapterView.OnItemClickListener onUserClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            IQuickPlayDialogListener activity = (IQuickPlayDialogListener)getActivity();
            if (activity == null) return;

            ((SwitchUserAdapter)parent.getAdapter()).toggleSelected(position);
        }
    };



    private class Holder {
        public TextView username;
        public TextView alsoHere;
        public NetworkImageView avatar;
    }
}
