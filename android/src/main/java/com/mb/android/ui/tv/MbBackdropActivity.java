package com.mb.android.ui.tv;

import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.logging.FileLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mark on 2014-07-03.
 * Abstract Activity that other activities will inherit when using showing backdrops using a ViewSwitcher
 */
public abstract class MbBackdropActivity extends MbBaseActivity {

    protected NetworkImageView mBackdropImage1;
    protected NetworkImageView mBackdropImage2;
    protected ViewSwitcher mBackdropSwitcher;
    private int mBackdropIndex = 0;
    private Runnable CycleBackdrops = new Runnable() {
        @Override
        public void run() {

            if (mBackdropIndex >= mBackdropUrls.size())
                mBackdropIndex = 0;

            setBackdropImage(mBackdropUrls.get(mBackdropIndex));
            mBackdropIndex += 1;
            mBackdropSwitcher.postDelayed(this, 8000);
        }
    };
    private List<String> mBackdropUrls;


    @Override
    public void onResume() {
        super.onResume();
        enableBackdropImage();
    }


    @Override
    public void onPause() {
        super.onPause();
        disableBackdropImage();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        disableBackdropImage();
    }

    //**********************************************************************************************
    // Backdrop image methods
    //**********************************************************************************************


    protected void setBackdropImages(List<String> imageUrls) {

        if (imageUrls == null || imageUrls.size() == 0) {
            FileLogger.getFileLogger().Error("Error setting backdrops - imageUrls is null or empty");
            return;
        }
        mBackdropSwitcher.removeCallbacks(CycleBackdrops);
        mBackdropUrls = imageUrls;
        mBackdropIndex = 0;

        if (imageUrls.size() > 1)
            mBackdropSwitcher.post(CycleBackdrops);
        else
            setBackdropImage(imageUrls.get(0));
    }


    private void setBackdropImage(String imageUrl) {

        if (imageUrl == null || imageUrl.isEmpty()) {
            FileLogger.getFileLogger().Error("Error setting backdrop - imageUrl is null or empty");
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


    protected void setBackdropToDefaultImage() {

        mBackdropSwitcher.removeCallbacks(CycleBackdrops);
        mBackdropUrls = new ArrayList<>();
        mBackdropIndex = 0;

        if (mBackdropSwitcher.getDisplayedChild() == 0) {
            mBackdropImage2.setDefaultImageResId(R.drawable.default_backdrop);
            mBackdropImage2.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
            mBackdropSwitcher.showNext();
        } else {
            mBackdropImage1.setDefaultImageResId(R.drawable.default_backdrop);
            mBackdropImage1.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
            mBackdropSwitcher.showPrevious();
        }

    }


    protected void disableBackdropImage() {

        if (mBackdropSwitcher == null) return;

        if (mBackdropUrls != null && mBackdropUrls.size() > 1)
            mBackdropSwitcher.removeCallbacks(CycleBackdrops);

        mBackdropSwitcher.setVisibility(ViewSwitcher.INVISIBLE);
    }


    protected void enableBackdropImage() {

        if (mBackdropUrls != null && mBackdropUrls.size() > 1)
            mBackdropSwitcher.post(CycleBackdrops);

        mBackdropSwitcher.setVisibility(ViewSwitcher.VISIBLE);
    }
}
