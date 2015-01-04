package com.mb.android.fragments.tv;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.mb.android.ItemListWrapper;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.adapters.MediaAdapterPosters;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment that shows a generic grid of items
 */
public class GenericContentFragment extends Fragment {

    public String TAG = "GenericContentFragment";
    private ItemListWrapper mItems;

    /**
     * Class Constructor
     */
    public GenericContentFragment() {}

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mItems = (ItemListWrapper) getArguments().getSerializable("Media");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tv_fragment_series_actors, container, false);

        if (view != null && mItems != null && mItems.Items != null && mItems.Items.size() > 0) {
            GridView mGenericItemsGrid = (GridView) view.findViewById(R.id.gvSeriesActors);
            mGenericItemsGrid.setNumColumns(5);
            mGenericItemsGrid.setAdapter(new MediaAdapterPosters(mItems.Items, MB3Application.getInstance().getResources().getInteger(R.integer.tv_library_columns_poster), MB3Application.getInstance().API, null));
            mGenericItemsGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                }
            });
        }

        return view;
    }
}
