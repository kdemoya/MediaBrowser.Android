package com.mb.android.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.logging.FileLogger;
import mediabrowser.model.dto.BaseItemDto;
import com.mb.android.utils.Utils;

import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Adapter that displays info from a DTOBaseItem
 */
public class TvSongsAdapter extends BaseAdapter implements SectionIndexer {

    public boolean isFastScrolling;
    private List<BaseItemDto> mSongs;
    private LayoutInflater li;
    private String sections_ = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private int selectedIndex = 0;


    public TvSongsAdapter(List<BaseItemDto> baseItems) {

        mSongs = baseItems;

        try {
            li = (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("Error in adapter initialization", e);
        }
    }

    public void setSelectedIndex(int index) { selectedIndex = index; }

    public int getCount() {
        return mSongs.size();
    }


    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {

            convertView = li.inflate(R.layout.tv_widget_song, parent, false);

            if (convertView == null) return null;

            holder = new ViewHolder();
            holder.songNumber = (TextView) convertView.findViewById(R.id.tvSongIndexNumber);
            holder.songTitle = (TextView) convertView.findViewById(R.id.tvSongTitle);
            holder.songRuntime = (TextView) convertView.findViewById(R.id.tvSongRuntime);
            holder.playButton = (Button) convertView.findViewById(R.id.btnPlay);
            holder.queueButton = (Button) convertView.findViewById(R.id.btnQueue);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Set song info
        if (mSongs.get(position).getIndexNumber() != null) {
            holder.songNumber.setText(String.valueOf(mSongs.get(position).getIndexNumber()));
        }

        holder.songTitle.setText(mSongs.get(position).getName());

        if (mSongs.get(position).getRunTimeTicks() != null) {
            holder.songRuntime.setText(Utils.PlaybackRuntimeFromMilliseconds(mSongs.get(position).getRunTimeTicks() / 10000));
        }

        if (position == selectedIndex) {
            holder.playButton.setVisibility(View.VISIBLE);
            holder.queueButton.setVisibility(View.VISIBLE);
        } else {
            holder.playButton.setVisibility(View.INVISIBLE);
            holder.queueButton.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }


    public Object getItem(int position) {

        return mSongs.get(position);
    }


    public long getItemId(int position) {
        return 0;
    }


    @SuppressLint("DefaultLocale")
    public int getPositionForSection(int section) {
        // If there is no item for current section, previous section will be selected
        for (int i = section; i >= 0; i--) {
            for (int j = 0; j < getCount(); j++) {
                if (i == 0) {
                    // For numeric section
                    for (int k = 0; k <= 9; k++) {
                        if (((BaseItemDto) getItem(j)).getSortName().charAt(0) == (char) k)
                            return j;
                    }
                } else {
                    if (((BaseItemDto) getItem(j)).getSortName().toUpperCase().charAt(0) == sections_.charAt(i))
                        return j;
                }
            }
        }
        return 0;
    }


    public int getSectionForPosition(int position) {
        return 0;
    }


    public Object[] getSections() {
        String[] sections = new String[sections_.length()];
        for (int i = 0; i < sections_.length(); i++)
            sections[i] = String.valueOf(sections_.charAt(i));
        return sections;
    }


    public class ViewHolder {

        public TextView songNumber;
        public TextView songTitle;
        public TextView songRuntime;
        public Button playButton;
        public Button queueButton;
    }

    public class SongWrapper {

        public BaseItemDto Song;
        public boolean IsSelected = false;
    }


//    public class AsyncProcessOverlay extends AsyncTask<Void, Void, String> {
//
//        private TextView mTextView;
//        private DTOBaseItem mItem;
//
//        public AsyncProcessOverlay(TextView textView, DTOBaseItem item) {
//            mTextView = textView;
//            mItem = item;
//        }
//
//        @Override
//        protected String doInBackground(Void... voids) {
//
//            boolean isNew = false;
//
//            if (mItem.DateCreated != null) {
//                try {
//                    DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//                    inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
//
//                    Date createdDate = inputFormat.parse(mItem.DateCreated);
//                    long createdMilliseconds = createdDate.getTime();
//
//                    long currentMilliseconds = new Date().getTime();
//
//                    if ((currentMilliseconds - createdMilliseconds) < (10 * 86400000)) {
//                        isNew = true;
//                    }
//
//                } catch (Exception e) {
//                    Log.e("Exception", "Error resolving date", e);
//                }
//            }
//
//            if (mItem.UserData != null) {
//
//                if (mItem.UserData.Played) {
//
//                    return "watched";
//
//                } else if (isNew || (mItem.RecentlyAddedItemCount != null && mItem.RecentlyAddedItemCount > 0)) {
//
//                    return "new";
//
//                } else {
//
//                    return "unwatched";
//                }
//            }
//            return "";
//        }
//
//        @Override
//        protected void onPostExecute(String string) {
//
//
//            if (string.equalsIgnoreCase("watched")) {
//
//                mTextView.setText("\u2714");
//                mTextView.setVisibility(View.VISIBLE);
//
//            } else {
//
//                mTextView.setVisibility(View.INVISIBLE);
//            }
//
//        }
//    }

}
