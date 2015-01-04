package com.mb.android.activities.mobile;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.activities.BaseMbMobileActivity;
import mediabrowser.apiinteraction.Response;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.utils.Utils;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.entities.SortOrder;
import com.mb.android.logging.FileLogger;

/**
 * Created by Mark on 12/12/13.
 *
 * This activity shows the user a selected photo. It also allows browsing of other photos in the same directory
 */
public class PhotoDetailsActivity extends BaseMbMobileActivity {

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private static final String TAG = "PhotoDetailsActivity";
    private BaseItemDto mItem;
    private NetworkImageView imageView;
    private TextView photoName;
    private TextView cameraInfo;
    private BaseItemDto[] _photos;
    private int _currentIndex = 0;
    private String parentId;
    private String lastViewed;

    /**
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mActionBar != null) {
            mActionBar.hide();
        }
        setContentView(R.layout.activity_photo_details);

        final GestureDetector gdt = new GestureDetector(this, new GestureListener());
        imageView = (NetworkImageView) findViewById(R.id.ivPhotograph);
        photoName = (TextView) findViewById(R.id.tvPhotoName);
        cameraInfo = (TextView) findViewById(R.id.tvCameraInfo);

        LinearLayout photoDetailsHolder = (LinearLayout) findViewById(R.id.llPhotoDetails);
        photoDetailsHolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraInfo.setVisibility(
                        cameraInfo.getVisibility() == TextView.VISIBLE ? TextView.GONE : TextView.VISIBLE);
            }
        });

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                gdt.onTouchEvent(event);
                return true;
            }
        });

        parentId = getMb3Intent().getStringExtra("ParentId");
        String jsonData = getMb3Intent().getStringExtra("Item");
        mItem = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, BaseItemDto.class);
        lastViewed = mItem.getId();

        if (savedInstanceState != null
                && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(savedInstanceState.getString("LastViewed"))) {
            lastViewed = savedInstanceState.getString("LastViewed");
        }

        mMini = (MiniController) findViewById(R.id.miniController1);
        mCastManager.addMiniController(mMini);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("LastViewed", lastViewed);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            buildAndSendInitialQuery();
        }
    }

    @Override
    public void onPause() {
        mMini.removeOnMiniControllerChangedListener(mCastManager);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FileLogger.getFileLogger().Info("Photo Details Activity: onDestroy");
    }

    @Override
    protected void onConnectionRestored() {
        buildAndSendInitialQuery();
    }

    private void buildAndSendInitialQuery() {
        ItemQuery query = new ItemQuery();
        query.setParentId(parentId);
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setFields(new ItemFields[]{ItemFields.Overview});
        query.setSortBy(new String[]{ItemSortBy.SortName});
        query.setSortOrder(SortOrder.Ascending);

        MB3Application.getInstance().API.GetItemsAsync(query, getItemsResponse);
    }

    private void setImage(int index) {

        if (_photos == null ||_photos.length <= index) return;

        BaseItemDto photo = _photos[index];

        if (photo == null) return;

        lastViewed = photo.getId();

        ImageOptions options;
        options = new ImageOptions();
        options.setImageType(ImageType.Primary);
        options.setMaxWidth(getScreenWidth());
        options.setMaxHeight(getScreenHeight());

        String imageUrl = MB3Application.getInstance().API.GetImageUrl(_photos[index], options);
        imageView.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        photoName.setText(_photos[index].getName());

        setImageDetails(photo);

        Utils.LogMemoryUsage(this);
    }

    private void setImageDetails(BaseItemDto photo) {
        String info = "";
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(photo.getCameraMake())) {
            info += "Make: " + photo.getCameraMake() + " ";
        }
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(photo.getCameraModel())) {
            if (info.length() > 0) info += System.getProperty("line.separator");
            info += "Model: " + photo.getCameraModel();
        }
        if (photo.getWidth() != null && photo.getHeight() != null) {
            if (info.length() > 0) info += System.getProperty("line.separator");
            info += "Dimensions: " + String.valueOf(photo.getWidth()) + "x" + String.valueOf(photo.getHeight()) + " px";
        }
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(photo.getSoftware())) {
            if (info.length() > 0) info += System.getProperty("line.separator");
            info += "Software: " + photo.getSoftware();
        }
        if (photo.getExposureTime() != null) {
            if (info.length() > 0) info += System.getProperty("line.separator");
            info += "Exposure Time: " + String.valueOf(photo.getExposureTime());
        }
        if (photo.getFocalLength() != null) {
            if (info.length() > 0) info += System.getProperty("line.separator");
            info += "Focal Length: " + String.valueOf(photo.getFocalLength());
        }
        if (photo.getAperture() != null) {
            if (info.length() > 0) info += System.getProperty("line.separator");
            info += "Aperature: " + String.valueOf(photo.getAperture());
        }
        if (photo.getShutterSpeed() != null) {
            if (info.length() > 0) info += System.getProperty("line.separator");
            info += "Shutter Speed: " + String.valueOf(photo.getShutterSpeed());
        }
        if (photo.getIsoSpeedRating() != null) {
            if (info.length() > 0) info += System.getProperty("line.separator");
            info += "ISO Speed Rating: " + String.valueOf(photo.getIsoSpeedRating());
        }
        if (photo.getLatitude() != null) {
            if (info.length() > 0) info += System.getProperty("line.separator");
            info += "Latitude: " + String.valueOf(photo.getLatitude());
        }
        if (photo.getLongitude() != null) {
            if (info.length() > 0) info += System.getProperty("line.separator");
            info += "Longitude: " + String.valueOf(photo.getLongitude());
        }
        if (photo.getAltitude() != null) {
            if (info.length() > 0) info += System.getProperty("line.separator");
            info += "Altitude: " + String.valueOf(photo.getAltitude());
        }

        cameraInfo.setText(info);
    }

    private void showNextImage() {

        if (_currentIndex + 1 == _photos.length)
            _currentIndex = 0;
        else
            _currentIndex += 1;

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.grow_horizontal);
        imageView.startAnimation(animation);
        setImage(_currentIndex);
    }

    private void showPreviousImage() {

        if (_currentIndex == 0)
            _currentIndex = _photos.length - 1;
        else
            _currentIndex = _currentIndex - 1;

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.grow_horizontal_left);
        imageView.startAnimation(animation);
        setImage(_currentIndex);
    }

    /**
     * Callback function used in the API call GetItemsAsync
     */
    private Response<ItemsResult> getItemsResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {
            FileLogger.getFileLogger().Info(TAG + ": Get Items Callback");

            if (response != null && response.getItems() != null && response.getItems().length > 0) {
                FileLogger.getFileLogger().Info(TAG + " - Get Items Callback: " + response.getItems().length + " Items to process");
                FileLogger.getFileLogger().Info(TAG + " - Get Items Callback: Total Record Count = " + response.getTotalRecordCount());

                _photos = response.getItems();

                for (int i = 0; i < _photos.length; i++) {
                    try {

                        if (_photos[i].getId().equalsIgnoreCase(lastViewed)) {
                            _currentIndex = i;
                            setImage(_currentIndex);
                            break;
                        }

                    } catch (Exception e) {
                        FileLogger.getFileLogger().ErrorException("Error setting initial photo", e);
                    }
                }
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                showNextImage();
                return false; // Right to left
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                showPreviousImage();
                return false; // Left to right
            }

            return false;
        }
    }
}
