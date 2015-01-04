package com.mb.android.search;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import com.mb.android.MB3Application;
import com.mb.android.R;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.model.search.SearchHint;
import mediabrowser.model.search.SearchHintResult;
import mediabrowser.model.search.SearchQuery;


public class SearchHintContentProvider extends ContentProvider {

    private static final String[] COLUMNS = {
            "_id",
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_ICON_1,
            SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA
    };
    private MatrixCursor cursor = new MatrixCursor(COLUMNS);
    private GsonJsonSerializer serializer = new GsonJsonSerializer();

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        if (selectionArgs[0].length() < 3) return cursor;

        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setSearchTerm(selectionArgs[0].trim());
        searchQuery.setLimit(20);
        searchQuery.setUserId(MB3Application.getInstance().API.getCurrentUserId());
        searchQuery.setIncludeArtists(true);
        searchQuery.setIncludeMedia(true);
        searchQuery.setIncludePeople(true);
        searchQuery.setIncludeStudios(false);
        searchQuery.setIncludeGenres(false);


        MB3Application.getInstance().API.GetSearchHintsAsync(searchQuery, new Response<SearchHintResult>() {
            @Override
            public void onResponse(SearchHintResult result) {
                if (result == null || result.getSearchHints() == null) return;

                for (int i = 0; i < result.getSearchHints().length; i++) {
                    SearchHint hint = result.getSearchHints()[i];
                    String json = serializer.SerializeToString(hint);
                    if ("episode".equalsIgnoreCase(hint.getType())) {
                        cursor.addRow(new Object[]{
                                i,
                                getFormattedEpisodeTitle(hint.getName(), hint.getParentIndexNumber(), hint.getIndexNumber()),
                                hint.getSeries() != null ? hint.getSeries() : "",
                                R.drawable.tv,
                                json
                        });
                    } else if ("audio".equalsIgnoreCase(hint.getType())) {
                        cursor.addRow(new Object[]{
                                i,
                                getFormattedSongTitle(hint.getName(), hint.getIndexNumber()),
                                getFormattedAlbumArtist(hint.getAlbum(), hint.getAlbumArtist()),
                                R.drawable.music,
                                json
                        });
                    } else if ("musicalbum".equalsIgnoreCase(hint.getType())) {
                        cursor.addRow(new Object[]{
                                i,
                                hint.getName(),
                                hint.getAlbumArtist() != null ? hint.getAlbumArtist() : "",
                                R.drawable.music,
                                json
                        });
                    } else if ("musicartist".equalsIgnoreCase(hint.getType())) {
                        cursor.addRow(new Object[]{ i, hint.getName(), "", R.drawable.music, json });
                    } else if ("person".equalsIgnoreCase(hint.getType())) {
                        cursor.addRow(new Object[]{ i, hint.getName(), "", null, json });
                    } else if ("series".equalsIgnoreCase(hint.getType())) {
                        cursor.addRow(new Object[]{ i, hint.getName(), "", R.drawable.tv, json });
                    } else if ("photo".equalsIgnoreCase(hint.getType())) {
                        cursor.addRow(new Object[]{ i, hint.getName(), "", R.drawable.photos, json });
                    } else if ("game".equalsIgnoreCase(hint.getType())) {
                        cursor.addRow(new Object[]{ i, hint.getName(), "", R.drawable.games, json });
                    } else if ("book".equalsIgnoreCase(hint.getType())) {
                        cursor.addRow(new Object[]{ i, hint.getName(), "", R.drawable.books, json });
                    } else {
                        cursor.addRow(new Object[]{ i, hint.getName(), "", R.drawable.movies, json });
                    }
                }
            }
        });

        MatrixCursor returnMatrix = cursor;
        cursor = new MatrixCursor(COLUMNS);

        return returnMatrix;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private String getFormattedEpisodeTitle(String name, Integer parentIndex, Integer index) {
        String string = name;

        if (index != null) {
            string = String.valueOf(index) + " - " + string;

            if (parentIndex != null) {
                string = String.valueOf(parentIndex) + "." + string;
            }
        }

        return string;
    }

    private String getFormattedSongTitle(String name, Integer index) {
        String string = name;

        if (index != null)
            string = String.valueOf(index) + ". " + string;

        return string;
    }

    private String getFormattedAlbumArtist(String albumName, String artistName) {
        String string = "";

        if (albumName != null) {
            string += albumName;
        }
        if (artistName != null) {
            if (string.length() > 0) {
                string = " / " + string;
            }
            string = artistName + string;
        }

        return string;
    }
}
