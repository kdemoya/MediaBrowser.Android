package com.mb.android.adapters;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.ViewHolder;
import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.extensions.StringHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by Mark on 12/12/13.
 */
public class TvActorAdapter extends BaseAdapter {

    private BaseItemPerson[] mPeople;
    private ApiClient mApi;
    private LayoutInflater mLayoutInflater;
//    private int height;
//    private int width;
    private boolean imageEnhancersEnabled;

    public TvActorAdapter(BaseItemPerson[] people, int gridHeight, int rows, Context context) {
        mPeople = people;
        mApi = MB3Application.getInstance().API;

        if (context == null) {
            context = MB3Application.getInstance();
        }

        try {
            mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            DisplayMetrics dm = context.getResources().getDisplayMetrics();

//            height = ((int)((float)gridHeight * .65) - (rows * (int)(8 * dm.density))) / rows;
//            width = (int)((float)height  * .66);

            imageEnhancersEnabled = PreferenceManager
                    .getDefaultSharedPreferences(MB3Application.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true);
        } catch (Exception e) {

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
            convertView = mLayoutInflater.inflate(R.layout.tv_widget_actor_tile, parent, false);
            holder.titleText = (TextView) convertView.findViewById(R.id.tvActorName);
            holder.secondaryText = (TextView) convertView.findViewById(R.id.tvActorRole);
            holder.imageView = (NetworkImageView) convertView.findViewById(R.id.ivActorImage);
//            holder.imageView.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
            holder.imageView.setDefaultImageResId(R.drawable.default_actor);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (mPeople[position].getName() != null && !mPeople[position].getName().isEmpty())
            holder.titleText.setText(mPeople[position].getName());

        String role = "";
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mPeople[position].getRole())) {
            if ("actor".equalsIgnoreCase(mPeople[position].getType())) {
                role += "as ";
            }
            role += mPeople[position].getRole();
        }

        holder.secondaryText.setText(role);

        if (mPeople[position].getHasPrimaryImage()) {
            ImageOptions actorImageOptions = new ImageOptions();
//            actorImageOptions.setHeight(height);
            actorImageOptions.setImageType(ImageType.Primary);
            actorImageOptions.setEnableImageEnhancers(imageEnhancersEnabled);
                String actorImageUrl = mApi.GetPersonImageUrl(mPeople[position], actorImageOptions);
                holder.imageView.setImageUrl(actorImageUrl, MB3Application.getInstance().API.getImageLoader());
        } else {
            holder.imageView.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
        }

        return convertView;
    }
}
