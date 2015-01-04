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
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.NextUpQuery;
import mediabrowser.model.querying.ItemFields;
import com.mb.android.logging.FileLogger;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment shows a list of TV episodes that are "up next" based on user viewing habits. List content is
 * generated server-side.
 */
public class UpNextFragment extends Fragment implements ICommandListener {

    private static final String TAG = "UpNextFragment";
    private ProgressBar mActivityIndicator;
    private GridView upNextGrid;
    private TextView noContentText;
    private BaseItemDto[] mItems;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FileLogger.getFileLogger().Info(TAG + ": onCreateView");
        View view = inflater.inflate(R.layout.fragment_homescreen_items, container, false);

        if (view != null) {
            mActivityIndicator = (ProgressBar) view.findViewById(R.id.pbActivityIndicator);
            upNextGrid = (GridView) view.findViewById(R.id.gvUpNext);
            noContentText = (TextView) view.findViewById(R.id.tvNoContentWarning);
        }

        FileLogger.getFileLogger().Info(TAG + "Finish onCreateView");
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        FileLogger.getFileLogger().Info(TAG + "onResume");
        if (MB3Application.getInstance().API != null
                && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(MB3Application.getInstance().API.getCurrentUserId())) {

            mActivityIndicator.setVisibility(View.VISIBLE);

            NextUpQuery query = new NextUpQuery();
            query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
            query.setLimit(12);
            query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.ParentId});

            MB3Application.getInstance().API.GetNextUpEpisodesAsync(query, getNextUpResponse);
        }
        FileLogger.getFileLogger().Info(TAG + "finish onResume");
    }

    @Override
    public void onStop() {
        super.onStop();
        mItems = null;
    }

    private Response<ItemsResult> getNextUpResponse = new Response<ItemsResult>() {
        @Override
        public void onResponse(ItemsResult response) {
            mActivityIndicator.setVisibility(View.GONE);
            if (response == null || response.getItems() == null || response.getItems().length == 0) {

                // Show a label informing the user there is no new items.
                noContentText.setText(R.string.no_next_up_items_warning);
                noContentText.setVisibility(TextView.VISIBLE);
                return;
            }

            if (upNextGrid == null) return;

            mItems = response.getItems();

            Log.d("UpNextFragment", "setAdapter");
            upNextGrid.setAdapter(new HomeScreenItemsAdapter(response.getItems()));
            upNextGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View v, int index, long id) {
                    String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(mItems[index]);

                    Intent intent = new Intent(MB3Application.getInstance(), MediaDetailsActivity.class);
                    intent.putExtra("Item", jsonData);
                    intent.putExtra("LaunchedFromHomeScreen", true);
                    startActivity(intent);
                }
            });
        }
        @Override
        public void onError(Exception ex) {
            FileLogger.getFileLogger().Info("********* ON ERROR *********");
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
