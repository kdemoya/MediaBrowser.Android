package com.mb.android.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.logging.FileLogger;
import com.mb.android.utils.Utils;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.extensions.StringHelper;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by Mark on 12/12/13.
 *
 * BaseAdapter that shows media. The image preference will be Primary(if Episode)->Thumb->Backdrop.
 * Perhaps the class should be renamed.
 */
public class ChannelsAdapter extends BaseAdapter implements SectionIndexer {

    public boolean isFastScrolling;
    Context mContext;
    List<BaseItemDto> mBaseItems;
    LayoutInflater li;
    SharedPreferences mSharedPreferences;
    int mImageWidth;
    int mImageHeight;
//    private Animation fadeIn;
    private Integer mDefaultImageId;
    private boolean imageEnhancersEnabled;
    private String sections_ = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";


    public ChannelsAdapter(Context context, List<BaseItemDto> baseItems, Integer defaultImageId) {
        mContext = context;
        mBaseItems = baseItems;
        mDefaultImageId = defaultImageId;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        try {
            li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            int columns = mContext.getResources().getInteger(R.integer.library_columns);
            DisplayMetrics dm = mContext.getResources().getDisplayMetrics();

            mImageWidth = (dm.widthPixels - ((columns * 2) * (int) (4 * dm.density))) / columns;
            mImageHeight = (mImageWidth / 16) * 9;

            imageEnhancersEnabled = PreferenceManager
                    .getDefaultSharedPreferences(MB3Application.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true);

        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("Error in adapter initialization", e);
        }

//        fadeIn = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
    }


    public int getCount() {
        return mBaseItems.size();
    }


    @SuppressLint("SimpleDateFormat")
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {

            convertView = li.inflate(R.layout.widget_library_tile_large, parent, false);

            holder = new ViewHolder();
            holder.titleText = (TextView) convertView.findViewById(R.id.tvLibraryTileTitle);
            holder.secondaryText = (TextView) convertView.findViewById(R.id.tvLibraryTileSubTitle);
            holder.imageView = (NetworkImageView) convertView.findViewById(R.id.ivLibraryTilePrimaryImage);
            holder.overlay = (TextView) convertView.findViewById(R.id.tvOverlay);
            holder.playedProgress = (ProgressBar) convertView.findViewById(R.id.pbPlaybackProgress);
            holder.missingEpisodeOverlay = (TextView) convertView.findViewById(R.id.tvMissingEpisodeOverlay);

            holder.imageView.setLayoutParams(new RelativeLayout.LayoutParams(mImageWidth, mImageHeight));
            if (mDefaultImageId != null) {
                holder.imageView.setDefaultImageResId(mDefaultImageId);
            }

//            holder.imageView.setAnimation(fadeIn);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.position = position;

        String type = mBaseItems.get(position).getType();


        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(type) && !type.equalsIgnoreCase("Episode") && mBaseItems.get(position).getHasThumb()) {
            // Don't display the text fields
            holder.titleText.setVisibility(View.INVISIBLE);
            holder.secondaryText.setVisibility(View.INVISIBLE);
        } else {
            // Set primary text

            holder.titleText.setVisibility(View.VISIBLE);
            holder.secondaryText.setVisibility(View.VISIBLE);
            if (type.equalsIgnoreCase("Episode") && mBaseItems.get(position).getIndexNumber() != null) {
                String title = mBaseItems.get(position).getIndexNumber().toString();
                if (mBaseItems.get(position).getIndexNumberEnd() != null && !mBaseItems.get(position).getIndexNumberEnd().equals(mBaseItems.get(position).getIndexNumber()))
                    title += " - " + mBaseItems.get(position).getIndexNumberEnd() + ". ";
                else
                    title += ". ";
                title += mBaseItems.get(position).getName();
                holder.titleText.setText(title);
            } else {
                holder.titleText.setText(mBaseItems.get(position).getName());
            }

            // Set Secondary text

            if (type.equalsIgnoreCase("Boxset") || type.equalsIgnoreCase("Folder") || type.equalsIgnoreCase("Season") || type.equalsIgnoreCase("PhotoFolder")) {

                holder.secondaryText.setText(String.valueOf(mBaseItems.get(position).getChildCount()) + " Items");

            } else {

                String iText = "";

                if (type.equalsIgnoreCase("Episode") && mBaseItems.get(position).getPremiereDate() != null) {

                    DateFormat outputFormat = new SimpleDateFormat("dd-MM-yyyy");

                    Date premiereDate = Utils.convertToLocalDate(mBaseItems.get(position).getPremiereDate());
                    iText = outputFormat.format(premiereDate);



                } else {
                    if (mBaseItems.get(position).getProductionYear() != null && mBaseItems.get(position).getProductionYear() != 0) {
                        iText = String.valueOf(mBaseItems.get(position).getProductionYear());
                    }
                }

                if (mBaseItems.get(position).getOfficialRating() != null && !mBaseItems.get(position).getOfficialRating().equalsIgnoreCase("none")) {

                    if (!iText.isEmpty())
                        iText += "<font color='Aqua'> &#149 </font>";

                    iText += mBaseItems.get(position).getOfficialRating();
                }

                if (mBaseItems.get(position).getRunTimeTicks() != null && mBaseItems.get(position).getRunTimeTicks() > 0) {

                    if (!iText.isEmpty())
                        iText += "<font color='Aqua'> &#149 </font>";

                    iText += Utils.TicksToRuntimeString(mBaseItems.get(position).getRunTimeTicks());
                }

                holder.secondaryText.setText(Html.fromHtml(iText), TextView.BufferType.SPANNABLE);
            }
        }

        // Set tile image

        if (!isFastScrolling) {

            ImageOptions options = null;
            String imageUrl = null;

            if (mBaseItems.get(position).getHasThumb()) {
                options = new ImageOptions();
                options.setImageType(ImageType.Thumb);
                options.setWidth(mImageWidth);
                options.setMaxHeight(mImageHeight);
                options.setEnableImageEnhancers(imageEnhancersEnabled);
                imageUrl = MB3Application.getInstance().API.GetImageUrl(mBaseItems.get(position).getId(), options);

            } else if (mBaseItems.get(position).getHasPrimaryImage()) {
                options = new ImageOptions();
                options.setImageType(ImageType.Primary);
                options.setWidth(mImageWidth);
                options.setMaxHeight(mImageHeight);
                options.setEnableImageEnhancers(imageEnhancersEnabled);
                imageUrl = MB3Application.getInstance().API.GetImageUrl(mBaseItems.get(position), options);

            } else if (mBaseItems.get(position).getBackdropCount() > 0) {
                options = new ImageOptions();
                options.setImageType(ImageType.Backdrop);
                options.setWidth(mImageWidth);
                options.setMaxHeight(mImageHeight);
                options.setEnableImageEnhancers(imageEnhancersEnabled);
                options.setImageIndex(0);
                imageUrl = MB3Application.getInstance().API.GetImageUrl(mBaseItems.get(position), options);

            } else {
                holder.imageView.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
            }

            if (options != null) {
                holder.imageView.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            }
        }

        // Process top-right overlays

        if (mBaseItems.get(position).getLocationType().equals(LocationType.Virtual) && mBaseItems.get(position).getType().equalsIgnoreCase("episode")) {
            holder.overlay.setVisibility(View.INVISIBLE);

            if (mBaseItems.get(position).getPremiereDate() != null) {

                Date premiereDate = Utils.convertToLocalDate(mBaseItems.get(position).getPremiereDate());

                long premiereDateMs = premiereDate.getTime();
                long currentMs = new Date().getTime();

                if (premiereDateMs - currentMs > 0)
                    holder.missingEpisodeOverlay.setText("UNAIRED");
            }

            holder.missingEpisodeOverlay.setVisibility(TextView.VISIBLE);
        } else {
            holder.missingEpisodeOverlay.setVisibility(TextView.GONE);
            if (mBaseItems.get(position).getType().equalsIgnoreCase("boxset") ||
                    mBaseItems.get(position).getType().equalsIgnoreCase("season") ||
                    mBaseItems.get(position).getType().equalsIgnoreCase("series")) {
                if (mBaseItems.get(position).getUserData().getUnplayedItemCount() != null && mBaseItems.get(position).getUserData().getUnplayedItemCount() > 0) {
                    holder.overlay.setText(String.valueOf(mBaseItems.get(position).getUserData().getUnplayedItemCount()));
                    holder.overlay.setVisibility(View.VISIBLE);
                } else if (mBaseItems.get(position).getUserData().getUnplayedItemCount() != null && mBaseItems.get(position).getUserData().getUnplayedItemCount() == 0) {
                    holder.overlay.setText("\u2714");
                    holder.overlay.setVisibility(View.VISIBLE);
                } else {
                    holder.overlay.setVisibility(View.INVISIBLE);
                }
            } else {
                boolean watched = false;

                if (mBaseItems.get(position).getUserData() != null) {
                    watched = mBaseItems.get(position).getUserData().getPlayed();
                }

                if (watched) {
                    holder.overlay.setText("\u2714");
                    holder.overlay.setVisibility(View.VISIBLE);
                } else {
                    holder.overlay.setVisibility(View.INVISIBLE);
                }
            }
        }

        try {
            // Show the percentage watched for media
            if (mBaseItems.get(position).getUserData() != null && mBaseItems.get(position).getUserData().getPlaybackPositionTicks() > 1) {

                holder.playedProgress.setVisibility(View.VISIBLE);
                holder.playedProgress.setMax(100);
                double percentWatched = (double) mBaseItems.get(position).getUserData().getPlaybackPositionTicks() / (double) mBaseItems.get(position).getRunTimeTicks();
                int roundedValue = (int) (percentWatched * 100);
                holder.playedProgress.setProgress(roundedValue);

            } else {
                holder.playedProgress.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            holder.playedProgress.setVisibility(View.INVISIBLE);
            FileLogger.getFileLogger().ErrorException("Error setting progressbar value", e);
        }

        return convertView;
    }


    public Object getItem(int position) {

        return mBaseItems.get(position);
    }


    public long getItemId(int position) {
        return 0;
    }


    @SuppressLint("DefaultLocale")
    public int getPositionForSection(int section) {
        // If there is no item for current section, previous section will be selected
        for (int i = section; i >= 0; i--) {
            for (int j = 0; j < getCount(); j++) {
                if (i == 0) {
                    // For numeric section
                    for (int k = 0; k <= 9; k++) {
                        if (((BaseItemDto) getItem(j)).getSortName().charAt(0) == (char) k)
                            return j;
                    }
                } else {
                    if (((BaseItemDto) getItem(j)).getSortName().toUpperCase().charAt(0) == sections_.charAt(i))
                        return j;
                }
            }
        }
        return 0;
    }


    public int getSectionForPosition(int position) {
        return 0;
    }


    public Object[] getSections() {
        String[] sections = new String[sections_.length()];
        for (int i = 0; i < sections_.length(); i++)
            sections[i] = String.valueOf(sections_.charAt(i));
        return sections;
    }


    public class ViewHolder {

        public NetworkImageView imageView;
        public TextView titleText;
        public TextView secondaryText;
        public TextView overlay;
        public ProgressBar playedProgress;
        public TextView missingEpisodeOverlay;
        public int position;
    }

}

