package com.mb.android.ui.tv.playback;

import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.logging.FileLogger;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mark on 2014-12-09.
 * Activity that will be used to display photos
 */
public class PhotoPlayer extends FragmentActivity {

    private NetworkImageView mBackdropImage1;
    private NetworkImageView mBackdropImage2;
    private ViewSwitcher mBackdropSwitcher;
    private ImageView mPlayPauseButton;
    private LinearLayout controls;
    private TextView mPhotoName;
    private int mPhotoIndex = 0;
    private List<String> mPhotoUrls;
    private boolean mControlsVisible;

    private int mHeight;
    private int mWidth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tv_activity_photo_player);
        mBackdropSwitcher = (ViewSwitcher) findViewById(R.id.vsBackdropImages);
        mBackdropImage1 = (NetworkImageView) findViewById(R.id.ivBackdropImage1);
        mBackdropImage2 = (NetworkImageView) findViewById(R.id.ivBackdropImage2);
        mPhotoName = (TextView) findViewById(R.id.tvPhotoName);
        controls = (LinearLayout) findViewById(R.id.llControls);
        mPlayPauseButton = (ImageView) findViewById(R.id.ivPlayPause);
        mPlayPauseButton.setOnClickListener(onClickListener);
        ImageView fastForwardButton = (ImageView) findViewById(R.id.ivFastForward);
        fastForwardButton.setOnClickListener(onClickListener);
        ImageView rewindButton = (ImageView) findViewById(R.id.ivRewind);
        rewindButton.setOnClickListener(onClickListener);

        if (MB3Application.getInstance().PlayerQueue == null
                || MB3Application.getInstance().PlayerQueue.PlaylistItems == null
                || MB3Application.getInstance().PlayerQueue.PlaylistItems.isEmpty()) {
            Toast.makeText(this, "No images to display", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mPhotoIndex = getIntent().getIntExtra("Index", 0);

        setOverscanValues();

        measureScreen();
        createImageUrls();

        if (mPhotoUrls != null && mPhotoUrls.size() > 0) {
            loadImage();
            loadImageInfo();
        }
    }

    @Override
    public void onBackPressed() {
        if (mControlsVisible) {
            hidePlayerUi();
        } else {
            super.onBackPressed();
        }
    }

    private void measureScreen() {

        WindowManager w = getWindowManager();
        Display d = w.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        d.getMetrics(metrics);

        // since SDK_INT = 1;
        mWidth = metrics.widthPixels;
        mHeight = metrics.heightPixels;
        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17)
            try {
                mWidth = (Integer) Display.class.getMethod("getRawWidth").invoke(d);
                mHeight = (Integer) Display.class.getMethod("getRawHeight").invoke(d);
            } catch (Exception ignored) {
            }
        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 17)
            try {
                Point realSize = new Point();
                Display.class.getMethod("getRealSize", Point.class).invoke(d, realSize);
                mWidth = realSize.x;
                mHeight = realSize.y;
            } catch (Exception ignored) {
            }
    }

    private void createImageUrls() {

        mPhotoUrls = new ArrayList<>();
        ImageOptions options = new ImageOptions();
        options.setImageType(ImageType.Primary);
        options.setEnableImageEnhancers(false);
        options.setMaxHeight(mHeight);
        options.setMaxWidth(mWidth);

        for (PlaylistItem item : MB3Application.getInstance().PlayerQueue.PlaylistItems) {
            if ("photo".equalsIgnoreCase(item.Type)) {
                String url = MB3Application.getInstance().API.GetImageUrl(item.Id, options);
                mPhotoUrls.add(url);
            }
        }
    }

    private void loadImage() {
        if (mPhotoUrls == null || mPhotoUrls.isEmpty() || mPhotoUrls.size() <= mPhotoIndex) {
            FileLogger.getFileLogger().Error("Error setting image - mPhotoUrls is null or empty");
            return;
        }

        if (mBackdropSwitcher.getDisplayedChild() == 0) {
            mBackdropImage2.setImageUrl(mPhotoUrls.get(mPhotoIndex), MB3Application.getInstance().API.getImageLoader());
            mBackdropSwitcher.showNext();
        } else {
            mBackdropImage1.setImageUrl(mPhotoUrls.get(mPhotoIndex), MB3Application.getInstance().API.getImageLoader());
            mBackdropSwitcher.showPrevious();
        }
    }

    private void loadImageInfo() {
        mPhotoName.setText(MB3Application.getInstance().PlayerQueue.PlaylistItems.get(0).Name);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                onPlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                onPlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                onPlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                onFastForward();
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                this.finish();
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                onRewind();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (mControlsVisible) {
                    showPlayerUi(); // we're just calling this to reset the timer before it vanishes
                } else {
                    showPlayerUi();
                    mPlayPauseButton.requestFocus();
                    return true;
                }
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (mControlsVisible) {
                    showPlayerUi(); // we're just calling this to reset the timer before it vanishes
                } else {
                    showPlayerUi();
                    mPlayPauseButton.requestFocus();
                    return true;
                }
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (mControlsVisible) {
                    showPlayerUi(); // we're just calling this to reset the timer before it vanishes
                } else {
                    showPlayerUi();
                    mPlayPauseButton.requestFocus();
                    return true;
                }
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (mControlsVisible) {
                    showPlayerUi(); // we're just calling this to reset the timer before it vanishes
                } else {
                    showPlayerUi();
                    mPlayPauseButton.requestFocus();
                    return true;
                }
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (mControlsVisible) {
                    showPlayerUi(); // we're just calling this to reset the timer before it vanishes
                } else {
                    showPlayerUi();
                    mPlayPauseButton.requestFocus();
                    return true;
                }
        }
        return super.onKeyDown(keyCode, event);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            switch (v.getId()) {

                case R.id.ivPlayPause:
                    onPlayPause();
                    break;
                case R.id.ivRewind:
                    onRewind();
                    break;
                case R.id.ivFastForward:
                    onFastForward();
                    break;
                case R.id.ivDislike:
                    break;
                case R.id.ivLike:
                    break;
                case R.id.ivFavorite:
                    break;
            }
        }
    };

    private void onFastForward() {
        if (mPhotoUrls == null || mPhotoUrls.size() <= 1) {
            return;
        }
        if (mPhotoUrls.size() <= mPhotoIndex + 1) {
            mPhotoIndex++;
        } else {
            mPhotoIndex = 0;
        }
        loadImage();
        loadImageInfo();
    }

    private void onRewind() {
        if (mPhotoUrls == null || mPhotoUrls.size() <= 1) {
            return;
        }
        if ( mPhotoIndex > 0) {
            mPhotoIndex--;
        } else {
            mPhotoIndex = mPhotoUrls.size() - 1;
        }
        loadImage();
        loadImageInfo();
    }

    private void onPlayPause() {

    }

    private void showPlayerUi() {
        controls.setVisibility(View.VISIBLE);
        mControlsVisible = true;
        mPlayPauseButton.removeCallbacks(hidePlayerUiRunnable);
        mPlayPauseButton.postDelayed(hidePlayerUiRunnable, 6000);
    }

    private Runnable hidePlayerUiRunnable = new Runnable() {
        @Override
        public void run() {
            hidePlayerUi();
        }
    };

    private void hidePlayerUi() {
        controls.setVisibility(View.INVISIBLE);
        mControlsVisible = false;
    }

    protected void setOverscanValues() {
        RelativeLayout overscanLayout = (RelativeLayout) findViewById(R.id.rlOverscanPadding);

        if (overscanLayout == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int left = prefs.getInt("overscan_left", 0);
        int top = prefs.getInt("overscan_top", 0);
        int right = prefs.getInt("overscan_right", 0);
        int bottom = prefs.getInt("overscan_bottom", 0);

        ViewGroup.MarginLayoutParams overscanMargins = (ViewGroup.MarginLayoutParams) overscanLayout.getLayoutParams();
        overscanMargins.setMargins(left, top, right, bottom);
        overscanLayout.requestLayout();
    }
}
