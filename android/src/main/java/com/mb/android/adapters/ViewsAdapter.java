package com.mb.android.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mb.android.MB3Application;
import com.mb.android.MenuEntity;
import com.mb.android.R;

import java.util.List;


/**
 * Created by Mark on 12/12/13.
 *
 * Adapter used to populate the Drawer ListView on the home screen.
 */
public class ViewsAdapter extends BaseAdapter {

    private LayoutInflater mLayoutInflater;
    private List<MenuEntity> mLibraryFolders;

    public ViewsAdapter(List<MenuEntity> libraryFolders) {
        mLibraryFolders = libraryFolders;
        try {
            mLayoutInflater = (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getCount() {
        return mLibraryFolders.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int index, View convertView, ViewGroup viewGroup) {

        // I don't think the holder pattern is necessary here since the tiles won't be
        // scrolling off-screen
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.widget_view_tile, viewGroup, false);
        }

        TextView viewTitle = (TextView) convertView.findViewById(R.id.tvViewTitle);
        viewTitle.setText(mLibraryFolders.get(index).Name);

        ImageView viewImage = (ImageView) convertView.findViewById(R.id.ivViewImage);

        if (index == 0) {
            viewImage.setImageResource(R.drawable.home);
        } else if (mLibraryFolders.get(index).CollectionType != null) {
            if (mLibraryFolders.get(index).CollectionType.equalsIgnoreCase("books"))
                viewImage.setImageResource(R.drawable.books);

            else if (mLibraryFolders.get(index).CollectionType.equalsIgnoreCase("games"))
                viewImage.setImageResource(R.drawable.games);

            else if (mLibraryFolders.get(index).CollectionType.equalsIgnoreCase("movies"))
                viewImage.setImageResource(R.drawable.movies);

            else if (mLibraryFolders.get(index).CollectionType.equalsIgnoreCase("queue"))
                viewImage.setImageResource(R.drawable.folder);

            else if (mLibraryFolders.get(index).CollectionType.equalsIgnoreCase("boxsets"))
                viewImage.setImageResource(R.drawable.folder);

            else if (mLibraryFolders.get(index).CollectionType.equalsIgnoreCase("playlists"))
                viewImage.setImageResource(R.drawable.folder);

            else if (mLibraryFolders.get(index).CollectionType.equalsIgnoreCase("homevideos"))
                viewImage.setImageResource(R.drawable.homevideos);

            else if (mLibraryFolders.get(index).CollectionType.equalsIgnoreCase("music"))
                viewImage.setImageResource(R.drawable.music);

            else if (mLibraryFolders.get(index).CollectionType.equalsIgnoreCase("musicvideos"))
                viewImage.setImageResource(R.drawable.musicvideos);

            else if (mLibraryFolders.get(index).CollectionType.equalsIgnoreCase("photos"))
                viewImage.setImageResource(R.drawable.photos);

            else if (mLibraryFolders.get(index).CollectionType.equalsIgnoreCase("tvshows"))
                viewImage.setImageResource(R.drawable.tv);

            else if (mLibraryFolders.get(index).CollectionType.equalsIgnoreCase("livetv"))
                viewImage.setImageResource(R.drawable.tv);

            else if (mLibraryFolders.get(index).CollectionType.equalsIgnoreCase("channels"))
                viewImage.setImageResource(R.drawable.channels);
        }


        return convertView;
    }
}
