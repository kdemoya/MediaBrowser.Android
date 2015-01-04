package com.mb.android.adapters;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.ViewHolder;
import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;

/**
 * Created by Mark on 12/12/13.
 *
 * BaseAdapter that shows all the people for a given media item.
 */
public class ActorAdapter extends BaseAdapter {

    private BaseItemPerson[] mPeople;
    private ApiClient mApi;
    private LayoutInflater mLayoutInflater;
    private boolean imageEnhancersEnabled;

    public ActorAdapter(BaseItemPerson[] people, Context context, ApiClient apiClient) {
        mPeople = people;
        mApi = apiClient;

        try {
            mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            imageEnhancersEnabled = PreferenceManager
                    .getDefaultSharedPreferences(MB3Application.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true);
        } catch (Exception e) {
            Log.d("ActorAdapter", "Error getting layout inflater");
        }
    }

    @Override
    public int getCount() {
        return mPeople.length;
    }

    @Override
    public Object getItem(int position) {
        return mPeople[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {

            holder = new ViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.widget_actor_tile, parent, false);
            holder.titleText = (TextView) convertView.findViewById(R.id.tvActorName);
            holder.secondaryText = (TextView) convertView.findViewById(R.id.tvActorRole);
            holder.imageView = (NetworkImageView) convertView.findViewById(R.id.ivActorImage);
            holder.imageView.setDefaultImageResId(R.drawable.default_actor);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (mPeople[position].getName() != null && !mPeople[position].getName().isEmpty())
            holder.titleText.setText(mPeople[position].getName());


        if (mPeople[position].getRole() != null && !mPeople[position].getRole().isEmpty())
            holder.secondaryText.setText(mPeople[position].getRole());
        else {
            holder.secondaryText.setText("");
        }

        if (mPeople[position].getHasPrimaryImage()) {
            ImageOptions actorImageOptions = new ImageOptions();
            actorImageOptions.setMaxWidth(93);
            actorImageOptions.setMaxHeight(140);
            actorImageOptions.setImageType(ImageType.Primary);
            actorImageOptions.setEnableImageEnhancers(imageEnhancersEnabled);

            String actorImageUrl = mApi.GetImageUrl(mPeople[position].getId(), actorImageOptions);
            holder.imageView.setImageUrl(actorImageUrl, MB3Application.getInstance().API.getImageLoader());
        } else {
            holder.imageView.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
        }
        return convertView;
    }
}
