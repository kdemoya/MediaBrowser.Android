package com.mb.android.ui.tv.person;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;
import com.jess.ui.TwoWayAdapterView;
import com.jess.ui.TwoWayGridView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import mediabrowser.apiinteraction.Response;

import com.mb.android.adapters.HorizontalAdapterTitledBackdrops;
import com.mb.android.ui.tv.MbBackdropActivity;
import com.mb.android.ui.tv.library.dialogs.LongPressDialogFragment;
import com.mb.android.ui.tv.library.interfaces.ILongPressDialogListener;
import com.mb.android.utils.Utils;

import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment used to show a Details Screen for several media types. Movies, Episodes, Games
 */
public class ActorDetailsActivity extends MbBackdropActivity implements ILongPressDialogListener {

    public String TAG = "ActorDetailsActivity";
    private BaseItemPerson mPerson;
    private BaseItemDto mPersonDto;
    private ProgressBar mActivityIndicator;
    private TwoWayGridView mActorMediaGrid;
    private NetworkImageView mPersonImage;
    private ScrollView overviewScroller;
    private TextView mPersonBio;
    private TextView mActorName;
    private TextView mBirthday;
    private TextView mBirthPlace;
    private TextView mDateOfDeath;

    /**
     * Class Constructor
     */
    public ActorDetailsActivity() {}

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.tv_activity_actors);
        mActorName = (TextView) findViewById(R.id.tvMediaTitle);
        mBirthday = (TextView) findViewById(R.id.tvDateOfBirth);
        mBirthPlace = (TextView) findViewById(R.id.tvBirthPlace);
        mDateOfDeath = (TextView) findViewById(R.id.tvDateOfDeath);
        mActivityIndicator = (ProgressBar) findViewById(R.id.pbActivityIndicator);
        mBackdropSwitcher = (ViewSwitcher) findViewById(R.id.vsBackdropImages);
        mBackdropImage1 = (NetworkImageView) findViewById(R.id.ivBackdropImage1);
        mBackdropImage2 = (NetworkImageView) findViewById(R.id.ivBackdropImage2);
        mActorMediaGrid = (TwoWayGridView) findViewById(R.id.gridview);
        mPersonImage = (NetworkImageView) findViewById(R.id.ivActorImage);
        mPersonBio = (TextView) findViewById(R.id.tvActorBio);
        overviewScroller = (ScrollView) findViewById(R.id.svOverviewScrollView);
        findViewById(R.id.ibOptionsMenu).setOnClickListener(onOptionsClick);

        String jsonData = getIntent().getStringExtra("CurrentBaseItemDTO");
        mPerson = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, BaseItemPerson.class);

        setOverscanValues();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            getActorDto();
        }
    }

    @Override
    public void onDestroy() {
        overviewScroller.removeCallbacks(textScroller);
        super.onDestroy();
    }

    @Override
    public void onConnectionRestored() {
        getActorDto();
    }

    @Override
    protected void onUserDataUpdated(String itemId, UserItemDataDto userItemDataDto) {

    }


    private void getActorDto() {
        if (mPerson != null) {

            // Need to call GetPersonAsync to retrieve the actor bio and backdrops if any
            MB3Application.getInstance().API.GetItemAsync(mPerson.getId(), MB3Application.getInstance().API.getCurrentUserId(), getActorResponse);

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mPerson.getName())) {

                mActorName.setText(mPerson.getName());

                if (mPerson.getHasPrimaryImage()) {

                    ItemQuery query = new ItemQuery();
                    query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
                    query.setSortBy(new String[]{ItemSortBy.PremiereDate});
                    query.setSortOrder(SortOrder.Descending);
                    query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
                    query.setRecursive(true);
                    query.setPerson(mPerson.getName());

                    MB3Application.getInstance().API.GetItemsAsync(query, getActorMediaResponse);
                }
            }
        } else {
            hideActivityIndicator();
        }
    }


    //**********************************************************************************************
    // Callback Classes
    //**********************************************************************************************


    private Response<BaseItemDto> getActorResponse = new Response<BaseItemDto>() {

        @Override
        public void onResponse(BaseItemDto item) {

            if (item == null) {
                return;
            }

            mPersonDto = item;

            if (item.getPremiereDate() != null) {
                mBirthday.setText("Born:  " + Utils.convertToLocalDateFormat(item.getPremiereDate()));
                mBirthday.setVisibility(View.VISIBLE);
            }

            if (item.getProductionLocations() != null && !item.getProductionLocations().isEmpty()) {
                mBirthPlace.setText("Birthplace:  " + item.getProductionLocations().get(0));
                mBirthPlace.setVisibility(View.VISIBLE);
            }

            if (item.getEndDate() != null) {
                mDateOfDeath.setText("Died:  " + Utils.convertToLocalDateFormat(item.getEndDate()));
                mDateOfDeath.setVisibility(View.VISIBLE);
            }

            if (item.getBackdropCount() > 0) {
                List<String> backdropUrls = new ArrayList<>();

                for (int i = 0; i < item.getBackdropCount(); i++) {

                    ImageOptions imageOptions = new ImageOptions();
                    imageOptions.setImageType(ImageType.Backdrop);
                    imageOptions.setMaxHeight(720);
                    imageOptions.setMaxWidth(1280);
                    imageOptions.setImageIndex(i);

                    backdropUrls.add(MB3Application.getInstance().API.GetImageUrl(item, imageOptions));
                }

                if (backdropUrls.size() > 0) {
                    setBackdropImages(backdropUrls);
                }
            }

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getOverview())) {
                mPersonBio.setText(Html.fromHtml(item.getOverview()));
                overviewScroller.removeCallbacks(textScroller);
                overviewScroller.scrollTo(0,0);
                overviewScroller.postDelayed(textScroller, 5000); // Wait 5 seconds before scrolling initially
            }

            if (item.getHasPrimaryImage()) {
                int actorImageMaxHeight = (int) (360 * 1.5);
                int actorImageMaxWidth = (int) (360 * 1.5);

                // Get the Actor image
                ImageOptions actorImageOptions = new ImageOptions();
                actorImageOptions.setMaxHeight(actorImageMaxHeight);
                actorImageOptions.setMaxWidth(actorImageMaxWidth);
                actorImageOptions.setImageType(ImageType.Primary);

                String imageUrl = MB3Application.getInstance().API.GetPersonImageUrl(mPerson, actorImageOptions);
                mPersonImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());

            } else {
                mPersonImage.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };


    private Response<ItemsResult> getActorMediaResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {

            hideActivityIndicator();
            if (response != null && response.getItems() != null && response.getItems().length > 0) {

                mActorMediaGrid.setOnItemSelectedListener(new TwoWayAdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(TwoWayAdapterView<?> parent, View view, int position, long id) {
                        if (parent.getAdapter() != null && parent.getAdapter() instanceof HorizontalAdapterTitledBackdrops) {
                            ((HorizontalAdapterTitledBackdrops)parent.getAdapter()).setSelectedIndex(position);
                        }
                    }

                    @Override
                    public void onNothingSelected(TwoWayAdapterView<?> parent) {

                    }
                });

                mActorMediaGrid.setAdapter(new HorizontalAdapterTitledBackdrops(ActorDetailsActivity.this, Arrays.asList(response.getItems()), mActorMediaGrid.getHeight() - 32, 1, R.drawable.default_video_landscape));
                mActorMediaGrid.requestFocus();

            }
        }
        @Override
        public void onError(Exception ex) {
            hideActivityIndicator();
        }
    };


    private void hideActivityIndicator() {
        mActivityIndicator.setVisibility(View.GONE);
    }

    private Runnable textScroller = new Runnable() {

        int previousScrollY = -1;
        boolean postScrollDelayObserved = false;

        @Override
        public void run() {
            if (overviewScroller == null) {
                return;
            }
            if (postScrollDelayObserved) {
                overviewScroller.smoothScrollTo(0,0);
                postScrollDelayObserved = false;
            }
            if (overviewScroller.getScrollY() == 0 && previousScrollY != -1) {
                // We've just returned to the top from previous scroll
                previousScrollY = -1;
                overviewScroller.postDelayed(this, 5000);
            } else if (overviewScroller.getScrollY() != previousScrollY) {
                // We're scrolling
                previousScrollY = overviewScroller.getScrollY();
                overviewScroller.smoothScrollTo(0, overviewScroller.getScrollY() + 1);
                overviewScroller.postDelayed(this, 100);
            } else {
                // we can't scroll further
                overviewScroller.postDelayed(this, 5000);
                postScrollDelayObserved = true;
            }
        }
    };

    //******************************************************************************************************************
    // Button events
    //******************************************************************************************************************

    @Override
    protected void onPlayButton() {

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

    View.OnClickListener onOptionsClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPersonDto == null) return;
            LongPressDialogFragment itemOptionsFragment = new LongPressDialogFragment();
            itemOptionsFragment.setItem(mPersonDto);
            itemOptionsFragment.show(getSupportFragmentManager(), "OptionsFragment");
        }
    };

    @Override
    public void onUserDataChanged(String itemId, UserItemDataDto userItemDataDto) {

    }
}
