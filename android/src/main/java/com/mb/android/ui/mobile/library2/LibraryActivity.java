package com.mb.android.ui.mobile.library2;

import android.os.Bundle;

import com.mb.android.MB3Application;
import com.mb.android.activities.BaseMbMobileActivity;

import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.EpisodeQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;


public class LibraryActivity extends BaseMbMobileActivity {

    private static final String TAG = "LibraryActivity";
    private BaseItemDto mParentItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String jsonData = getMb3Intent().getStringExtra("Item");
        mParentItem = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, BaseItemDto.class);
    }

    @Override
    protected void onConnectionRestored() {

    }


//    private EpisodeQuery buildEpisodeQuery() {
//        EpisodeQuery query = new EpisodeQuery();
//        query.setFields(mEpisodeQuery.getFields());
//        query.setIsMissing(false);
//        query.setIsVirtualUnaired(false);
//        query.setSeasonId(mEpisodeQuery.getSeasonId());
//        query.setSeasonNumber(mEpisodeQuery.getSeasonNumber());
//        query.setSeriesId(mEpisodeQuery.getSeriesId());
//        query.setUserId(mEpisodeQuery.getUserId());
//        return query;
//    }
//
//
//    private ItemQuery buildItemQuery() {
//        ItemQuery query = new ItemQuery();
//        query.setUserId(MB3Application.getInstance().API.getCurrentUserId());
//        query.setParentId(mItem.getId());
//        query.setIncludeItemTypes(new String[]{"Audio", "Movie", "Episode", "MusicVideo"});
//        query.setSortBy(new String[]{ItemSortBy.SortName});
//        query.setSortOrder(SortOrder.Ascending);
//        query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.SortName, ItemFields.DateCreated});
//        query.setRecursive(true);
//        query.setIsMissing(false);
//        query.setIsVirtualUnaired(false);
//        return query;
//    }
}
