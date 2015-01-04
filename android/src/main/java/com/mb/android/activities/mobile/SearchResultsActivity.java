package com.mb.android.activities.mobile;

import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.mb.android.activities.BaseMbMobileActivity;
import com.mb.android.ui.mobile.library.LibraryPresentationActivity;
import com.mb.android.ui.mobile.person.ActorBioActivity;
import mediabrowser.apiinteraction.Response;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.adapters.SearchResultsAdapter;
import com.mb.android.fragments.NavigationMenuFragment;
import com.mb.android.ui.mobile.album.MusicAlbumActivity;
import com.mb.android.ui.mobile.musicartist.ArtistActivity;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.search.SearchHint;
import mediabrowser.model.search.SearchHintResult;
import com.mb.android.logging.FileLogger;
import mediabrowser.model.search.SearchQuery;

import java.util.Arrays;

/**
 * Created by Mark on 12/12/13.
 *
 * Show the user a series of results based on the search criteria
 */
public class SearchResultsActivity extends BaseMbMobileActivity {

    private GridView mLibraryGrid;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        mLibraryGrid = (GridView) findViewById(R.id.gvLibrary);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawer.setFocusableInTouchMode(false);

        NavigationMenuFragment fragment = (NavigationMenuFragment) getSupportFragmentManager().findFragmentById(R.id.left_drawer);
        if (fragment != null && fragment.isInLayout()) {
            fragment.setDrawerLayout(drawer);
        }

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                drawer,
                R.string.abc_action_bar_home_description,
                R.string.abc_action_bar_up_description) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
//                getActionBar().setTitle(mTitle);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
//                getActionBar().setTitle(mDrawerTitle);
            }

        };

        drawer.setDrawerListener(mDrawerToggle);

        mMini = (MiniController) findViewById(R.id.miniController1);
        mCastManager.addMiniController(mMini);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            handleIntent(getIntent());
        }
    }
    @Override
    public void onPause() {
        mMini.removeOnMiniControllerChangedListener(mCastManager);
        super.onPause();
    }

    @Override
    protected void onConnectionRestored() {
        handleIntent(getIntent());
    }


    @Override
    protected void onNewIntent(Intent intent) {

        handleIntent(intent);
    }


    private void handleIntent(Intent intent) {

        String json = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(json)) {
            handleSearchSuggestionIntent(json);
        } else {
            handleStandardQueryIntent(intent);
        }
    }

    private void handleSearchSuggestionIntent(String json) {
        SearchHint hint = new GsonJsonSerializer().DeserializeFromString(json, SearchHint.class);
        processSearchHint(hint);
    }

    private void handleStandardQueryIntent(Intent intent) {
        String query = intent.getStringExtra(SearchManager.QUERY);

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(query)) {
            return;
        }

        if (mActionBar != null) {
            mActionBar.setTitle("Search results for '" + query + "'");
        }

        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        searchQuery.setSearchTerm(query);

        MB3Application.getInstance().API.GetSearchHintsAsync(searchQuery, new SearchResultsResponse());
    }

    private class SearchResultsResponse extends Response<SearchHintResult> {
        @Override
        public void onResponse(final SearchHintResult results) {
            if (results == null)
                Toast.makeText(SearchResultsActivity.this, "Results is null", Toast.LENGTH_LONG).show();
            else {
                mLibraryGrid.setAdapter(new SearchResultsAdapter(SearchResultsActivity.this, Arrays.asList(results.getSearchHints()), MB3Application.getInstance().API));
                mLibraryGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        processSearchHint(results.getSearchHints()[i]);
                    }
                });
            }
        }
    }


    private void processSearchHint(SearchHint hint) {
        if (hint == null) return;

        Intent intent;

        if (hint.getType().equalsIgnoreCase("series")) {
            intent = new Intent(SearchResultsActivity.this, SeriesViewActivity.class);
        } else if (hint.getType().equalsIgnoreCase("musicartist")
                || hint.getType().equalsIgnoreCase("musicalbum")
                || hint.getType().equalsIgnoreCase("audio")) {
            MB3Application.getInstance().API.GetItemAsync(
                    hint.getItemId(),
                    MB3Application.getInstance().API.getCurrentUserId(),
                    getItemResponse);
            return;
        } else if (hint.getType().equalsIgnoreCase("photo")) {
            intent = new Intent(SearchResultsActivity.this, PhotoDetailsActivity.class);
        } else if (hint.getType().equalsIgnoreCase("book")) {
            intent = new Intent(SearchResultsActivity.this, BookDetailsActivity.class);
        } else if (hint.getType().equalsIgnoreCase("folder")
                || hint.getType().equalsIgnoreCase("boxset")) {
            intent = new Intent(SearchResultsActivity.this, LibraryPresentationActivity.class);
        } else if (hint.getType().equalsIgnoreCase("movie")
                || hint.getType().equalsIgnoreCase("episode")) {
            intent = new Intent(SearchResultsActivity.this, MediaDetailsActivity.class);
        } else if (hint.getType().equalsIgnoreCase("person")) {
            intent = new Intent(SearchResultsActivity.this, ActorBioActivity.class);
            intent.putExtra("ActorName", hint.getName());
            intent.putExtra("ActorId", hint.getItemId());
        } else {
            Toast.makeText(SearchResultsActivity.this, "Type is: " + hint.getType(), Toast.LENGTH_LONG).show();
            intent = null;
        }

        if (intent != null) {
            BaseItemDto item = new BaseItemDto();
            item.setId(hint.getItemId());
            item.setName(hint.getName());
            item.setType(hint.getType());

            String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(item);
            intent.putExtra("Item", jsonData);

            startActivity(intent);
            this.finish();
        } else {
            FileLogger.getFileLogger().Info("intent is null");
        }
    }


    private Response<BaseItemDto> getItemResponse = new Response<BaseItemDto>() {

        @Override
        public void onResponse(BaseItemDto item) {

            if (item == null) return;

            Intent intent;
            if (item.getType().equalsIgnoreCase("musicartist")) {
                intent = new Intent(SearchResultsActivity.this, ArtistActivity.class);
                intent.putExtra("ArtistId", item.getId());
            } else if (item.getType().equalsIgnoreCase("musicalbum")) {
                intent = new Intent(SearchResultsActivity.this, MusicAlbumActivity.class);
                intent.putExtra("AlbumId", item.getId());
            } else { // It's a song
                MB3Application.getInstance().API.GetItemAsync(
                        item.getParentId(),
                        MB3Application.getInstance().API.getCurrentUserId(),
                        getAlbumResponse);
                return;
            }
            startActivity(intent);
            SearchResultsActivity.this.finish();
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private Response<BaseItemDto> getAlbumResponse = new Response<BaseItemDto>() {

        @Override
        public void onResponse(BaseItemDto album) {

            Intent intent = new Intent(SearchResultsActivity.this, MusicAlbumActivity.class);
            intent.putExtra("AlbumId", album.getId());
            startActivity(intent);
            SearchResultsActivity.this.finish();
        }
        @Override
        public void onError(Exception ex) {

        }
    };
}
