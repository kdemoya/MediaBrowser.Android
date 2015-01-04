package com.mb.android.utils;

import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mark on 2014-12-15.
 *
 * A Utility class to easily add backdrop cycling to the various activities in the client.
 */
public class BackdropSlideshow {

    private ViewSwitcher mBackdropSwitcher;
    private NetworkImageView mBackdropImage1;
    private NetworkImageView mBackdropImage2;
    private int mBackdropIndex = 0;
    private int mDisplayIntervalMs = 15000;
    private List<String> mBackdropUrls;
    private List<Integer> mBackdropResourceIds;


    /**
     * Instantiate a new instance of the BackdropSlideshow
     *
     * @param viewSwitcher The ViewSwitcher that contains the two NetworkImageView's to switch between
     * @param imageView1   The NetworkImageView that will display the images
     * @param imageView2   The NetworkImageView that will display the images
     */
    public BackdropSlideshow(ViewSwitcher viewSwitcher, NetworkImageView imageView1, NetworkImageView imageView2) {

        if (viewSwitcher == null) {
            throw new IllegalArgumentException("viewSwitcher");
        }
        if (imageView1 == null) {
            throw new IllegalArgumentException("imageView1");
        }
        if (imageView2 == null) {
            throw new IllegalArgumentException("imageView2");
        }

        mBackdropSwitcher = viewSwitcher;
        mBackdropImage1 = imageView1;
        mBackdropImage2 = imageView2;
    }


    /**
     * Set the interval in which the images should change. Images will only cycle if there is more than one image to
     * display.
     *
     * @param milliseconds The interval in milliseconds
     */
    public void setDisplayintervalMs(int milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("milliseconds must be an unsigned value");
        }
        mDisplayIntervalMs = milliseconds;
    }


    /**
     * Get the current interval between backdrop transitions
     *
     * @return The interval in milliseconds
     */
    public int getDisplayIntervalMs() {
        return mDisplayIntervalMs;
    }


    /**
     * Release all resources being used to display backdrops. Should be called in the onDestroy() or onStop() methods
     * of the Activity/Fragment.
     */
    public void release() {
        reset();
        mBackdropImage2 = null;
        mBackdropImage1 = null;
        mBackdropSwitcher = null;
    }


    /**
     * Sets the backdrop images that will be cycled.
     *
     * This method is identical to {@link #setBackdropUrls(java.util.List, int)} with a displayIntervalMs of 15000
     *
     * @param imageUrls A List of image URL's.
     */
    public void setBackdropUrls(List<String> imageUrls) {
        setBackdropUrls(imageUrls, 15000);
    }


    /**
     * Sets the backdrop images that will be cycled.
     *
     * @param imageUrls         A List of image URL's.
     * @param displayIntervalMs The interval in milliseconds
     */
    public void setBackdropUrls(List<String> imageUrls, int displayIntervalMs) {
        setDisplayintervalMs(displayIntervalMs);
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("imageResIds is null or empty");
        }
        reset();
        mBackdropUrls = imageUrls;
        mBackdropSwitcher.post(cycleBackdrops);
    }


    /**
     * Sets the backdrop images that will be cycled.
     *
     * This method is functionally identical to calling {@link #setBackdropResourceIds(java.util.List, int)} with a
     * displayIntervalMs of 15000;
     *
     * @param imageResIds A List of image Resource Identifiers
     */
    public void setBackdropResourceIds(List<Integer> imageResIds) {
        setBackdropResourceIds(imageResIds, 15000);
    }


    /**
     * Sets the backdrop images that will be cycled.
     *
     * @param imageResIds       A List of image Resource Identifiers
     * @param displayIntervalMs The interval in milliseconds
     */
    public void setBackdropResourceIds(List<Integer> imageResIds, int displayIntervalMs) {
        setDisplayintervalMs(displayIntervalMs);
        if (imageResIds == null || imageResIds.isEmpty()) {
            throw new IllegalArgumentException("imageResIds is null or empty");
        }
        reset();
        mBackdropResourceIds = imageResIds;
        mBackdropSwitcher.post(cycleBackdrops);
    }


    // Stop the current image cycling and reset the data structures to an empty state.
    private void reset() {
        mBackdropSwitcher.removeCallbacks(cycleBackdrops);
        mBackdropUrls = new ArrayList<>();
        mBackdropResourceIds = new ArrayList<>();
        mBackdropIndex = 0;
    }


    // This is the meat of the class. Runs on the mDisplayIntervalMs and calls the appropriate image setting method
    private Runnable cycleBackdrops = new Runnable() {
        @Override
        public void run() {

            if (mBackdropUrls != null && !mBackdropUrls.isEmpty()) {

                if (mBackdropIndex >= mBackdropUrls.size()) {
                    mBackdropIndex = 0;
                }

                setBackdropImage(mBackdropUrls.get(mBackdropIndex));
                mBackdropIndex++;

                if (mBackdropUrls.size() > 1) {
                    mBackdropSwitcher.postDelayed(this, mDisplayIntervalMs);
                }

            } else if (mBackdropResourceIds != null && !mBackdropResourceIds.isEmpty()) {

                if (mBackdropIndex >= mBackdropResourceIds.size()) {
                    mBackdropIndex = 0;
                }

                setbackdropImage(mBackdropResourceIds.get(mBackdropIndex));
                mBackdropIndex++;

                if (mBackdropResourceIds.size() > 1) {
                    mBackdropSwitcher.postDelayed(this, mDisplayIntervalMs);
                }
            }
        }
    };


    private void setBackdropImage(String imageUrl) {

        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        if (mBackdropSwitcher.getDisplayedChild() == 0) {
            mBackdropImage2.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            mBackdropSwitcher.showNext();
        } else {
            mBackdropImage1.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            mBackdropSwitcher.showPrevious();
        }
    }


    private void setbackdropImage(Integer resourceId) {

        if (resourceId == null) {
            return;
        }

        if (mBackdropSwitcher.getDisplayedChild() == 0) {
            mBackdropImage2.setImageResource(resourceId);
            mBackdropSwitcher.showNext();
        } else {
            mBackdropImage1.setImageResource(resourceId);
            mBackdropSwitcher.showPrevious();
        }
    }
}
