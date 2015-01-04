package com.mb.android.ui.mobile.homescreen;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.activities.mobile.MediaDetailsActivity;
import com.mb.android.adapters.HomeScreenItemsAdapter;
import mediabrowser.apiinteraction.Response;
import com.mb.android.interfaces.ICommandListener;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.entities.SortOrder;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment that shows items the use has started watching but not completed
 */
public class ResumableFragment extends Fragment implements ICommandListener {

    private BaseItemDto[] mItems;
    private ProgressBar mActivityIndicator;
    private GridView mResumableItemsGrid;
    private TextView noContentText;

    /**
     * Class Constructor
     */
    public ResumableFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_homescreen_items, container, false);

        if (view != null) {
            mActivityIndicator = (ProgressBar) view.findViewById(R.id.pbActivityIndicator);
            mResumableItemsGrid = (GridView) view.findViewById(R.id.gvUpNext);
            noContentText = (TextView) view.findViewById(R.id.tvNoContentWarning);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (MB3Application.getInstance().API != null
                && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(MB3Application.getInstance().API.getCurrentUserId())) {

            if (mActivityIndicator != null)
                mActivityIndicator.setVisibility(View.VISIBLE);

            ItemQuery query = new ItemQuery();
            query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
            query.setSortBy(new String[]{ItemSortBy.SortName});
            query.setSortOrder(SortOrder.Ascending);
            query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
            query.setFilters(new ItemFilter[]{ ItemFilter.IsResumable });
            query.setRecursive(true);
            query.setLimit(20);
            query.setExcludeLocationTypes(new LocationType[]{LocationType.Offline, LocationType.Virtual});

            // Retrieve the root items.
            MB3Application.getInstance().API.GetItemsAsync(query, getItemsResponse);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mItems = null;
    }

    private Response<ItemsResult> getItemsResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {
            mActivityIndicator.setVisibility(View.GONE);
            Log.i("GetResumableItemsCallback", "data received");

            if (response == null) return;

            if (response.getItems() != null && response.getItems().length > 0) {
                Log.i("GetResumableItemsCallback", "data is not null");
                mItems = response.getItems();

                if (mResumableItemsGrid == null) return;

                mResumableItemsGrid.setAdapter(new HomeScreenItemsAdapter(mItems));
                mResumableItemsGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View v, int index, long id) {
                        // Resumable items are only going to be episodes and movies
                        String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(mItems[index]);
                        Intent intent = new Intent(MB3Application.getInstance(), MediaDetailsActivity.class);
                        intent.putExtra("Item", jsonData);
                        intent.putExtra("LaunchedFromHomeScreen", true);
                        startActivity(intent);
                    }
                });

                if (noContentText.getVisibility() == TextView.VISIBLE) {
                    noContentText.setVisibility(TextView.INVISIBLE);
                }

            } else {
                Log.i("GetResumableItemsCallback", "data is null or empty");

                // Show a label informing the user there is no new items.
                noContentText.setText(R.string.no_resumable_items_warning);
                noContentText.setVisibility(TextView.VISIBLE);


                if (mResumableItemsGrid == null) return;

                try {
                    HomeScreenItemsAdapter adapter = (HomeScreenItemsAdapter)mResumableItemsGrid.getAdapter();
                    if (adapter != null) {
                        // The grid has been initialized which should indicate that it was
                        // previously populated. Refresh it.
                        adapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    Log.d("ResumableFragment", "Error casting adapter");
                }
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    @Override
    public void onPreviousButton() {

    }

    @Override
    public void onNextButton() {

    }

    @Override
    public void onPlayPauseButton() {

    }

    @Override
    public void onPlayButton() {

    }

    @Override
    public void onPauseButton() {

    }
}
