package com.mb.android.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.logging.FileLogger;
import com.mb.android.utils.Utils;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Mark on 12/12/13.
 *
 * BaseAdapter that shows all the available users for a given MB server instance
 */
public class UserAdapter extends BaseAdapter {

    private LayoutInflater mLayoutInflater;
    private UserDto[] mUsers;
    private int mMaxPixels = 150;


    public UserAdapter(UserDto[] users) {
        mUsers = users;
        mLayoutInflater = (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        try {
            WindowManager windowManager = (WindowManager) MB3Application.getInstance().getSystemService(Context.WINDOW_SERVICE);
            Display d = windowManager.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            d.getMetrics(metrics);
            mMaxPixels = (int)(mMaxPixels * metrics.density);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public int getCount() {
        return mUsers.length;
    }


    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.widget_user_tile, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.UserName = (TextView) convertView.findViewById(R.id.tvUserTileName);
            viewHolder.LastSeen = (TextView) convertView.findViewById(R.id.tvLastSeenValue);
            viewHolder.UserImage = (NetworkImageView) convertView.findViewById(R.id.ivUserTileImage);
            viewHolder.UserImage.setDefaultImageResId(R.drawable.default_user);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.UserName.setText(mUsers[position].getName());
        viewHolder.LastSeen.setText(getLastSeenString(mUsers[position]));
        viewHolder.UserImage.setImageUrl(getImageUrl(mUsers[position]), MB3Application.getInstance().API.getImageLoader());

        return convertView;
    }


    public Object getItem(int position) {
        if (mUsers != null && mUsers.length > position) {
            return mUsers[position];
        }
        return null;
    }


    public long getItemId(int position) {
        return 0;
    }

    private class ViewHolder {

        public TextView UserName;
        public TextView LastSeen;
        public NetworkImageView UserImage;

    }

    @SuppressLint("SimpleDateFormat")
    private String getLastSeenString(UserDto user) {

        if (user.getLastLoginDate() != null) {

            Date date = Utils.convertToLocalDate(user.getLastActivityDate());

            long lastSeenMs = date.getTime();
            long currentMs = new Date().getTime();

            return MB3Application.getInstance().getResources().getString(R.string.last_seen) + " " + Utils.getFriendlyTimeString(currentMs - lastSeenMs);
        } else {
            return MB3Application.getInstance().getResources().getString(R.string.last_seen) + " " + MB3Application.getInstance().getResources().getString(R.string.unknown);
        }
    }

    private String getImageUrl(UserDto user) {
        if (user.getHasPrimaryImage()) {
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setMaxHeight(mMaxPixels);
            options.setMaxWidth(mMaxPixels);
            return MB3Application.getInstance().API.GetUserImageUrl(user, options);
        } else {
            return null;
        }
    }
}
