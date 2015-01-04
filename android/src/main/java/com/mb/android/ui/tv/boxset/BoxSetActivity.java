package com.mb.android.ui.tv.boxset;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;
import com.jess.ui.TwoWayAdapterView;
import com.jess.ui.TwoWayGridView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.adapters.HorizontalAdapterBackdrops;
import com.mb.android.adapters.HorizontalAdapterPosters;
import com.mb.android.adapters.HorizontalAdapterTitledBackdrops;
import com.mb.android.adapters.HorizontalAdapterTitledPosters;
import com.mb.android.ui.tv.library.dialogs.MediaResumeDialogFragment;
import com.mb.android.ui.tv.library.dialogs.QuickPlayDialogFragment;
import com.mb.android.ui.tv.library.interfaces.ILongPressDialogListener;
import com.mb.android.ui.tv.library.interfaces.IQuickPlayDialogListener;
import mediabrowser.apiinteraction.Response;
import com.mb.android.logging.FileLogger;
import com.mb.android.ui.tv.MbBackdropActivity;
import com.mb.android.ui.tv.library.dialogs.LongPressDialogFragment;
import com.mb.android.ui.tv.playback.PlayerHelpers;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.SeasonQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment used to show a Details Screen for several media types. Movies, Episodes, Games
 */
public class BoxSetActivity extends MbBackdropActivity implements IQuickPlayDialogListener, ILongPressDialogListener {

    public String TAG = "BoxSetActivity";
    private BaseItemDto mParent;
    private List<BaseItemDto> mChildren;
    private String mParentCollectionType;
    private TextView mTitle;
    private NetworkImageView mLogo;
    private TwoWayGridView mBoxSetGrid;
    private NetworkImageView mPrimaryImage;
    private TextView overview;
    private boolean mIsFresh = true;
    private ProgressBar mActivityIndicator;


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        FileLogger.getFileLogger().Info(TAG + ": creating boxset view");
        setContentView(R.layout.tv_activity_boxset);
        inflateViews();
        setOverscanValues();

        String jsonData = getIntent().getStringExtra("CurrentBaseItemDTO");
        mParent = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, BaseItemDto.class);
        mParentCollectionType = getIntent().getStringExtra("CollectionType");
        FileLogger.getFileLogger().Info(TAG + ": finish creating view");
    }


    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            getParentItem();
        }
    }

    @Override
    public void onConnectionRestored() {
        getParentItem();
    }

    @Override
    protected void onUserDataUpdated(String itemId, UserItemDataDto userItemDataDto) {
        MB3Application.getInstance().API.GetItemAsync(
                mParent.getId(),
                MB3Application.getInstance().API.getCurrentUserId(),
                new GetParentItemResponse(true)
        );
        getChildren();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (mBoxSetGrid != null && mBoxSetGrid.getSelectedItemPosition() == 0) {
                    if (mBoxSetGrid.getAdapter() != null && mBoxSetGrid.getAdapter().getCount() > 0) {
                        int targetPosition = mBoxSetGrid.getAdapter().getCount() - 1;
                        mBoxSetGrid.smoothScrollToPosition(targetPosition);
                        mBoxSetGrid.setSelection(targetPosition);
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (mBoxSetGrid.getAdapter() != null && mBoxSetGrid.getAdapter().getCount() > 0) {
                    if (mBoxSetGrid.getSelectedItemPosition() == mBoxSetGrid.getAdapter().getCount() - 1) {
                        mBoxSetGrid.smoothScrollToPosition(0);
                        mBoxSetGrid.setSelection(0);
                        return true;
                    }
                }
                break;
        }

        return super.onKeyDown(keyCode, event);
    }

    //******************************************************************************************************************
    // button events
    //******************************************************************************************************************

    @Override
    protected void onPlayButton() {
        BaseItemDto item = (BaseItemDto) mBoxSetGrid.getSelectedItem();
        if (item != null) {
//            Toast.makeText(this, "Type: " + item.getType(), Toast.LENGTH_LONG).show();
            if ("season".equalsIgnoreCase(item.getType())) {
                if (LocationType.Virtual.equals(item.getLocationType())) {
                    Toast.makeText(BoxSetActivity.this, "Nothing to play", Toast.LENGTH_LONG).show();
                } else {
                    showPlayAllShuffleDialog(item);
                }
            } else if (item.getUserData() != null && item.getUserData().getPlaybackPositionTicks() > 0) {
                showResumeDialog(item);
            } else {
                PlayerHelpers helper = new PlayerHelpers();
                helper.playItem(BoxSetActivity.this, item, 0L, null, null, null, false);
            }
        }

    }

    @Override
    protected void onFastForwardButton() {

    }

    @Override
    protected void onRewindButton() {

    }

    @Override
    protected void onMenuButton() {

    }


    private void showPlayAllShuffleDialog(BaseItemDto item) {

        QuickPlayDialogFragment quickPlayDialogFragment = new QuickPlayDialogFragment();
        quickPlayDialogFragment.setData(item, false);
        quickPlayDialogFragment.show(getSupportFragmentManager(), "QuickPlayDialog");
    }


    private void showResumeDialog(BaseItemDto item) {

        MediaResumeDialogFragment resumeDialog = new MediaResumeDialogFragment();
        resumeDialog.setItem(item);
        resumeDialog.show(getSupportFragmentManager(), "resumeDialog");
    }


    private void getParentItem() {
        if (mIsFresh && mParent != null) {
            FileLogger.getFileLogger().Info(TAG + ": Requesting full info for parent.");
            MB3Application.getInstance().API.GetItemAsync(
                    mParent.getId(),
                    MB3Application.getInstance().API.getCurrentUserId(),
                    new GetParentItemResponse(false));

            loadBoxsetImage();
            getChildren();
            mIsFresh = false;
        }
    }

    private void getChildren() {
        if ("series".equalsIgnoreCase(mParent.getType())) {
            if (null == mParent.getSeasonCount() || mParent.getSeasonCount() > 1) {
                performSeriesQuery();
            } else {
                performEpisodesQuery();
            }
        } else {
            performItemsQuery();
        }
    }

    private void loadBoxsetImage() {
        if (mParent.getHasPrimaryImage()) {
            FileLogger.getFileLogger().Info(TAG + ": Parent has primary image");
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Primary);
            options.setMaxHeight(600);
            options.setMaxWidth(450);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(mParent, options);
            mPrimaryImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
        } else {
            FileLogger.getFileLogger().Info(TAG + ": Parent has no primary image");
            mPrimaryImage.setVisibility(View.INVISIBLE);
        }
    }

    private void performSeriesQuery() {
        FileLogger.getFileLogger().Info(TAG + ": Parent is a series, requesting seasons");
        SeasonQuery query = new SeasonQuery();
        query.setSeriesId(mParent.getId());
        query.setUserId(MB3Application.getInstance().user.getId());
        query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.SortName});

        MB3Application.getInstance().API.GetSeasonsAsync(query, getItemsResponse);
    }

    private void performEpisodesQuery() {
        FileLogger.getFileLogger().Info(TAG + ": Parent is series, requesting episodes");
        ItemQuery query = new ItemQuery();
        query.setParentId(mParent.getId());
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setSortBy(new String[]{ItemSortBy.SortName});
        query.setSortOrder(SortOrder.Ascending);
        query.setRecursive(true);
        query.setMediaTypes(new String[]{"video"});
        query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.SortName, ItemFields.DateCreated, ItemFields.Genres});

        MB3Application.getInstance().API.GetItemsAsync(query, getItemsResponse);
    }

    private void performItemsQuery() {
        FileLogger.getFileLogger().Info(TAG + ": Parent is boxset, requesting children");
        ItemQuery query = new ItemQuery();
        query.setParentId(mParent.getId());
        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        query.setSortBy(new String[]{ItemSortBy.ProductionYear});
        query.setSortOrder(SortOrder.Ascending);
        query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.SortName, ItemFields.DateCreated, ItemFields.Genres});

        MB3Application.getInstance().API.GetItemsAsync(query, getItemsResponse);
    }


    private void LoadTitle() {

        if (mParent.getHasLogo()) {
            FileLogger.getFileLogger().Info(TAG + ": Setting logo image");
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Logo);
            options.setMaxHeight(300);
            options.setMaxWidth(500);

            String imageUrl = MB3Application.getInstance().API.GetImageUrl(mParent, options);
            mLogo.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
            mTitle.setVisibility(View.GONE);
            mLogo.setVisibility(View.VISIBLE);
        } else {
            FileLogger.getFileLogger().Info(TAG + ": Setting title text");
            mTitle.setText(mParent.getName());
            mTitle.setVisibility(View.VISIBLE);
            mLogo.setVisibility(View.GONE);
        }
    }


    private void LoadBackdrops() {

        if (mParent.getBackdropCount() > 0) {
            List<String> backdropUrls = new ArrayList<>();
            FileLogger.getFileLogger().Info(TAG + ": Building backdrop image urls");

            for (int i = 0; i < mParent.getBackdropCount(); i++) {

                ImageOptions imageOptions = new ImageOptions();
                imageOptions.setImageType(ImageType.Backdrop);
                imageOptions.setMaxHeight(720);
                imageOptions.setMaxWidth(1280);
                imageOptions.setImageIndex(i);

                backdropUrls.add(MB3Application.getInstance().API.GetImageUrl(mParent, imageOptions));
            }

            if (backdropUrls.size() > 0) {
                FileLogger.getFileLogger().Info(TAG + ": setting backdrop(s)");
                setBackdropImages(backdropUrls);
            }
        }
    }

    private View.OnClickListener onPlayButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showPlayAllShuffleDialog(mParent);
        }
    };

    private View.OnClickListener onOptionsButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showOptionsMenuForItem(mParent);
        }
    };

    private void showOptionsMenuForItem(BaseItemDto item) {
        if (item == null) return;

        LongPressDialogFragment itemOptionsFragment = new LongPressDialogFragment();
        itemOptionsFragment.setItem(item);
        itemOptionsFragment.show(getSupportFragmentManager(), "OptionsFragment");
    }

    //**********************************************************************************************
    // Callback Classes
    //**********************************************************************************************

    private class GetParentItemResponse extends Response<BaseItemDto> {

        private boolean updateActivityResult;

        public GetParentItemResponse(boolean updateActivityResult) {
            this.updateActivityResult = updateActivityResult;
        }

        @Override
        public void onResponse(BaseItemDto response) {
            if (response == null) {
                FileLogger.getFileLogger().Info(TAG + ": No item returned from initial query");
                return;
            }
            FileLogger.getFileLogger().Info(TAG + ": valid response returned from initial query");
            mParent = response;

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mParent.getOverview())) {
                overview.setText(mParent.getOverview());
            }

            LoadTitle();
            LoadBackdrops();

            if (updateActivityResult) {
                sendActivityResult();
            }
        }
        @Override
        public void onError(Exception ex) {
            FileLogger.getFileLogger().Info(TAG + ": Error reported during initial query");
        }
    }


    private Response<ItemsResult> getItemsResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {

            mActivityIndicator.setVisibility(View.GONE);

            if (response == null || response.getItems() == null) {
                FileLogger.getFileLogger().Info(TAG + ": No results returned in query");
                return;
            }

            FileLogger.getFileLogger().Info(TAG + ": Children returned. Building Grid");
            mChildren = Arrays.asList(response.getItems());

            if (isThumbMajority(mChildren)) {
                mBoxSetGrid.setAdapter(new HorizontalAdapterTitledBackdrops(BoxSetActivity.this, mChildren, mBoxSetGrid.getHeight() -32, 1, R.drawable.default_video_landscape));
                mBoxSetGrid.setOnItemSelectedListener(onItemSelectedListener);
            } else {
                mBoxSetGrid.setAdapter(new HorizontalAdapterTitledPosters(mChildren, mBoxSetGrid.getHeight() - 32, 1, R.drawable.default_video_portrait));
                mBoxSetGrid.setOnItemSelectedListener(onItemSelectedListener);
            }
            mBoxSetGrid.setOnItemClickListener(new TwoWayAdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(TwoWayAdapterView<?> parent, View view, int position, long id) {
                    navigate(mChildren.get(position), mParentCollectionType);
                }
            });
            mBoxSetGrid.setOnItemLongClickListener(new TwoWayAdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(TwoWayAdapterView<?> parent, View view, int position, long id) {
                    if (mChildren.size() < position) {
                        return false;
                    }
                    showOptionsMenuForItem(mChildren.get(position));

                    return true;
                }
            });
            mBoxSetGrid.requestFocus();
        }
        @Override
        public void onError(Exception ex) {
            FileLogger.getFileLogger().Info(TAG + ": Error getting child items");
        }
    };

    private TwoWayAdapterView.OnItemSelectedListener onItemSelectedListener = new TwoWayAdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(TwoWayAdapterView<?> parent, View view, int position, long id) {
            if (parent.getAdapter() instanceof HorizontalAdapterTitledBackdrops) {
                ((HorizontalAdapterTitledBackdrops) parent.getAdapter()).setSelectedIndex(position);
            } else if (parent.getAdapter() instanceof HorizontalAdapterTitledPosters) {
                ((HorizontalAdapterTitledPosters) parent.getAdapter()).setSelectedIndex(position);
            }
        }

        @Override
        public void onNothingSelected(TwoWayAdapterView<?> parent) {

        }
    };

    private boolean isThumbMajority(List<BaseItemDto> items) {
        if (items == null || items.size() == 0) return false;

        int thumbImageCount = 0;

        for (BaseItemDto item : items) {
            if (item.getHasThumb()) {
                thumbImageCount++;
            }
        }

        return (float)thumbImageCount / (float)items.size() > .70f;
    }

    private void inflateViews() {
        mBackdropSwitcher = (ViewSwitcher) findViewById(R.id.vsBackdropImages);
        mBackdropImage1 = (NetworkImageView) findViewById(R.id.ivBackdropImage1);
        mBackdropImage2 = (NetworkImageView) findViewById(R.id.ivBackdropImage2);
        mTitle = (TextView) findViewById(R.id.tvMediaTitle);
        mLogo = (NetworkImageView) findViewById(R.id.ivLogoImage);
        mPrimaryImage = (NetworkImageView) findViewById(R.id.ivPrimaryImage);
        ImageButton optionMenuButton = (ImageButton) findViewById(R.id.ibOptionsMenu);
        optionMenuButton.setOnClickListener(onOptionsButtonClick);
        Button playButton = (Button) findViewById(R.id.btnPlay);
        playButton.setOnClickListener(onPlayButtonClick);
        mBoxSetGrid = (TwoWayGridView) findViewById(R.id.gridview);
        overview = (TextView) findViewById(R.id.tvMediaOverview);
        mActivityIndicator = (ProgressBar) findViewById(R.id.pbActivityIndicator);
    }

    @Override
    public void onQuickPlaySelectionFinished() {
        new PlayerHelpers().playItems(this);
    }

    @Override
    public void onUserDataChanged(String itemId, UserItemDataDto userItemDataDto) {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(itemId) || userItemDataDto == null) return;

        if (itemId.equalsIgnoreCase(mParent.getId())) {
            mParent.setUserData(userItemDataDto);
            // user updated the userdata for the boxset/series rather than it's children. Easier just to re-request the
            // children
            getChildren();
            sendActivityResult();

        } else {
            // find the child that was updated
            for (BaseItemDto item : mChildren) {
                if (itemId.equalsIgnoreCase(item.getId())) {
                    item.setUserData(userItemDataDto);
                    ((BaseAdapter) mBoxSetGrid.getAdapter()).notifyDataSetChanged();
                    break;
                }
            }

            MB3Application.getInstance().API.GetItemAsync(
                    mParent.getId(),
                    MB3Application.getInstance().API.getCurrentUserId(),
                    new GetParentItemResponse(true));
        }
    }

    private void sendActivityResult() {

        String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(mParent.getUserData());

        Intent resultIntent = new Intent();
        resultIntent.putExtra("Id", mParent.getId());
        resultIntent.putExtra("UserData", jsonData);

        setResult(Activity.RESULT_OK, resultIntent);
    }
}
