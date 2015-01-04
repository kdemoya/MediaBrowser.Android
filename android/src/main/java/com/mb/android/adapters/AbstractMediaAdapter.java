package com.mb.android.adapters;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.utils.Utils;
import mediabrowser.model.dto.BaseItemDto;

import java.util.Date;
import java.util.List;

/**
 * Created by Mark on 2014-06-16.
 *
 * Abstract class that defines a group of common methods used by any adapter(s) that are displaying
 * media entities (Episodes, Movies, Songs).
 */
public abstract class AbstractMediaAdapter extends BaseAdapter implements SectionIndexer {

    protected static final String WATCHED_CHECKMARK = "\u2714";
    protected List<BaseItemDto> mBaseItems;
    protected boolean imageEnhancersEnabled;

    private String sections_ = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    //**********************************************************************************************
    // Constructors
    //**********************************************************************************************

    public AbstractMediaAdapter(List<BaseItemDto> items) {
        mBaseItems = items;

        try {
            imageEnhancersEnabled = PreferenceManager
                    .getDefaultSharedPreferences(MB3Application.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true);
        } catch (Exception e) {
            Log.d("AbstractMediaAdapter", "Error reading preferences");
        }
    }

    //**********************************************************************************************
    // Base Class Overrides
    //**********************************************************************************************

    @Override
    public int getCount() {
        return mBaseItems.size();
    }

    @Override
    public Object getItem(int i) {
        return mBaseItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    //**********************************************************************************************
    // Overlay Methods
    //**********************************************************************************************

    /**
     * Display the progress bar, watched indicator, or watched progress for each media item in the
     * grid.
     *
     * @param holder   The ViewHolder being affected
     * @param baseItem The item having it's watched state queried
     */
    protected void setWatchedState(ViewHolder holder, BaseItemDto baseItem) {

        if (baseItem.getUserData() != null && baseItem.getUserData().getPlaybackPositionTicks() > 1) {

            holder.playedProgress.setVisibility(View.VISIBLE);
            holder.playedProgress.setMax(100);
            double percentWatched =
                    (double) baseItem.getUserData().getPlaybackPositionTicks() / (double) baseItem.getRunTimeTicks();
            int roundedValue = (int) (percentWatched * 100);
            holder.playedProgress.setProgress(roundedValue);
        } else {
            holder.playedProgress.setVisibility(View.INVISIBLE);
        }

        if (baseItem.getType().equalsIgnoreCase("boxset") ||
                baseItem.getType().equalsIgnoreCase("season") ||
                baseItem.getType().equalsIgnoreCase("series")) {
            if (baseItem.getUserData().getUnplayedItemCount() > 0) {
                holder.watchedCountOverlay.setText(String.valueOf(baseItem.getUserData().getUnplayedItemCount()));
                holder.watchedCountOverlay.setVisibility(View.VISIBLE);
            } else if (baseItem.getUserData().getUnplayedItemCount() == 0) {
                holder.watchedCountOverlay.setText(WATCHED_CHECKMARK);
                holder.watchedCountOverlay.setVisibility(View.VISIBLE);
            }
        } else if (baseItem.getUserData() != null && baseItem.getUserData().getPlayed()) {
            holder.watchedCountOverlay.setText(WATCHED_CHECKMARK);
            holder.watchedCountOverlay.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sets thet top right overlay for an episode that is either missing or unaired.
     *
     * @param holder   The ViewHolder being affected
     * @param baseItem The item having it's watched state queried
     */
    protected void setMissingOrUnairedEpisodeState(ViewHolder holder, BaseItemDto baseItem) {

        if (baseItem.getPremiereDate() != null) {

            Date premiereDate = Utils.convertToLocalDate(baseItem.getPremiereDate());

            long premiereDateMs = premiereDate.getTime();
            long currentMs = new Date().getTime();

            if (premiereDateMs - currentMs > 0) {
                holder.missingEpisodeOverlay.setText("UNAIRED");
            }

            holder.missingEpisodeOverlay.setVisibility(TextView.VISIBLE);
        }
    }

    @Override
    public Object[] getSections() {
        String[] sections = new String[sections_.length()];
        for (int i = 0; i < sections_.length(); i++)
            sections[i] = String.valueOf(sections_.charAt(i));
        return sections;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        // If there is no item for current section, previous section will be selected
        for (int i = sectionIndex; i >= 0; i--) {
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

    @Override
    public int getSectionForPosition(int position) {
        return 0;
    }

    public class ViewHolder {

        public NetworkImageView imageView;
        public TextView titleText;
        public TextView episodeTitleText;
        public TextView watchedCountOverlay;
        public RelativeLayout imageHolder;
        public ProgressBar playedProgress;
        public TextView missingEpisodeOverlay;
    }

    public void clearDataSet() {
        mBaseItems = null;
        notifyDataSetChanged();
    }
}
