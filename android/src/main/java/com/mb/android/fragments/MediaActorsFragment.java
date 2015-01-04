package com.mb.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.ui.mobile.person.ActorBioActivity;
import com.mb.android.activities.mobile.MediaDetailsActivity;
import com.mb.android.adapters.ActorAdapter;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment that shows a list of actors for a given item
 */
public class MediaActorsFragment extends Fragment {

    private BaseItemDto mItem;
    private GridView mCastList;
    private MediaDetailsActivity mMediaDetailsActivity;

    /**
     * Class Constructor
     */
    public MediaActorsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_media_actors, container, false);
        mCastList = (GridView) view.findViewById(R.id.gvMediaActors);

        Bundle args = getArguments();
        String jsonData = args.getString("Item");
        mItem = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, BaseItemDto.class);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCastList != null) {
            // If it's an episode we load people after the series cast
            if (mItem.getPeople() != null && mItem.getPeople().length > 0) {
                if (!mItem.getType().equalsIgnoreCase("episode")) {
                    populateActors(mItem.getPeople());
                } else {
                    if (mItem.getSeriesId() != null) {
                        Log.i("", "Requesting Series: " + mItem.getSeriesId());
                        MB3Application.getInstance().API.GetItemAsync(mItem.getSeriesId(),
                                MB3Application.getInstance().API.getCurrentUserId(),
                                getSeriesItemResponse);
                    }
                }
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity != null) {
            try {
                mMediaDetailsActivity = (MediaDetailsActivity) activity;
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
        }
    }

    private void populateActors(BaseItemPerson[] people) {

        mCastList.setAdapter(new ActorAdapter(people, mMediaDetailsActivity, MB3Application.getInstance().API));
        mCastList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                BaseItemPerson person = ((BaseItemPerson) adapterView.getAdapter().getItem(position));
                if (person.getType() != null && person.getType().equalsIgnoreCase("actor")) {
                    Intent intent = new Intent(mMediaDetailsActivity, ActorBioActivity.class);
                    intent.putExtra("ActorName", person.getName());
                    intent.putExtra("ActorId", person.getId());
                    startActivity(intent);
                }
            }
        });

        mCastList.requestFocus();
    }

    private Response<BaseItemDto> getSeriesItemResponse = new Response<BaseItemDto>() {

        @Override
        public void onResponse(BaseItemDto series) {
// The final list of people were going to pass to PopulateActors
            List<BaseItemPerson> consolidatedActors = new ArrayList<>();

            // An intermediary list that will contain non-duplicates of episode-level actors
            List<BaseItemPerson> usablePeople = new ArrayList<>();

            if (series != null) {
                /**
                 * Populate the episode cast first as it's guest stars, but remove actors also contained in the series
                 * object. Done because the series object actors have roles whereas the episode actors don't.
                 */
                if (mItem.getPeople() != null) {
                    boolean matched = false;

                    for (BaseItemPerson person : mItem.getPeople()) {
                        for (BaseItemPerson series_person : series.getPeople()) {

                            if (person.getName().equalsIgnoreCase(series_person.getName())) {
                                matched = true;
                                break;
                            }
                        }

                        if (!matched) {
                            usablePeople.add(person);
                        }
                    }
                }

            } else {
                Log.i("GetItemCallback", "series is null");
            }

            if (series != null && series.getPeople() != null) {
                consolidatedActors.addAll(Arrays.asList(series.getPeople()));
            }
            // Populate the guest stars at the end of the list.
            if (!usablePeople.isEmpty()) {
                consolidatedActors.addAll(usablePeople);
            }

            if (!consolidatedActors.isEmpty()) {
                populateActors(consolidatedActors.toArray(new BaseItemPerson[consolidatedActors.size()]));
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };
}
