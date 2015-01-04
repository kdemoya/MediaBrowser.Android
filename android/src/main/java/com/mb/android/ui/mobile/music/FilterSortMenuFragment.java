package com.mb.android.ui.mobile.music;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mb.android.MB3Application;
import com.mb.android.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class FilterSortMenuFragment extends Fragment {

    private ListView categoryListView;
    private ListView filterSortOptionsListView;
    private ArrayList<String> categories;

    private WeakReference<MusicLibraryFragment> libraryFragmentWeakReference;

    public void setLibraryFragment(MusicLibraryFragment fragment) {
        libraryFragmentWeakReference = new WeakReference<>(fragment);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.music_filter_sort_fragment, container, false);
        categoryListView = (ListView) view.findViewById(R.id.lvContent);

        buildCategoriesList();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        CategoryAdapter adapter = (CategoryAdapter) categoryListView.getAdapter();
        if (adapter == null) {
            return;
        }
        RootCategory rootCategory = RootCategory.valueOf(PreferenceManager.getDefaultSharedPreferences(MB3Application.getInstance()).getString("pref_music_root", "artist"));
        switch(rootCategory) {
            case artist:
                adapter.setSelectedIndex(0);
                break;
            case albumArtist:
                adapter.setSelectedIndex(1);
                break;
            case album:
                adapter.setSelectedIndex(2);
                break;
            case song:
                adapter.setSelectedIndex(3);
                break;
            case genre:
                adapter.setSelectedIndex(4);
                break;
        }
    }

    private void buildCategoriesList() {

        categories = new ArrayList<>();
        categories.add(getResources().getString(R.string.artist_string));
        categories.add("Album Artists");
        categories.add(getResources().getString(R.string.albums_header));
        categories.add(getResources().getString(R.string.songs_string));
        categories.add(getResources().getString(R.string.genres_string));

        categoryListView.setAdapter(new CategoryAdapter());
        categoryListView.setOnItemClickListener(onCategoryClick);
    }

    class CategoryAdapter extends BaseAdapter {

        private LayoutInflater inflater;
        private int selectedIndex = 0;

        public CategoryAdapter() {
            inflater = LayoutInflater.from(MB3Application.getInstance());
        }

        public void setSelectedIndex(int newIndex) {
            selectedIndex = newIndex;
            this.notifyDataSetChanged();
        }

        public int getSelectedIndex() {
            return selectedIndex;
        }

        @Override
        public int getCount() {
            return categories.size();
        }

        @Override
        public Object getItem(int position) {
            return categories.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.widget_navigation_drawer_clickable_item, parent, false);
            }

            TextView text = (TextView) convertView.findViewById(R.id.tvClickableItem);
            text.setText(categories.get(position));
            text.setPadding(10, 0, 0, 0);

            if (position == selectedIndex) {
                text.setTextSize(20);
                text.setTextColor(Color.parseColor("#00b4ff"));
            } else {
                text.setTextSize(18);
                text.setTextColor(Color.WHITE);
            }

            return convertView;
        }
    }

    AdapterView.OnItemClickListener onCategoryClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (libraryFragmentWeakReference == null) {
                return;
            }

            MusicLibraryFragment musicLibraryFragment = libraryFragmentWeakReference.get();
            CategoryAdapter adapter = (CategoryAdapter) parent.getAdapter();

            if (musicLibraryFragment == null || adapter == null || adapter.getSelectedIndex() == position) {
                return;
            }

            adapter.setSelectedIndex(position);

            if (position == 0) {
                musicLibraryFragment.displayArtists();
            } else if (position == 1) {
                musicLibraryFragment.displayAlbumArtists();
            } else if (position == 2) {
                musicLibraryFragment.displayAlbums();
            } else if (position == 3) {
                musicLibraryFragment.displaySongs();
            } else {
                musicLibraryFragment.displayGenres();
            }
        }
    };
}
