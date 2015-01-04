package com.mb.android.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.utils.Utils;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;
import mediabrowser.model.extensions.StringHelper;

import java.util.ArrayList;


public class TvStreamsAdapter extends BaseAdapter {

    private ArrayList<MediaStream> mStreams;
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private int currentIndex;

    public TvStreamsAdapter(ArrayList<MediaStream> streams, Integer currentStreamIndex, Context context) {
        mStreams = streams;
        mContext = context;
        currentIndex = currentStreamIndex != null ? currentStreamIndex : -33;

        try {
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        } catch (Exception e) {

        }
    }

    @Override
    public int getCount() {
        return mStreams.size();
    }

    @Override
    public Object getItem(int position) {
        return mStreams.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {

            holder = new ViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.widget_tv_stream_tile, parent, false);
            holder.streamLanguage = (TextView) convertView.findViewById(R.id.tvStreamLanguage);
            holder.streamText1 = (TextView) convertView.findViewById(R.id.tvStreamText1);
            holder.streamText2 = (TextView) convertView.findViewById(R.id.tvStreamText2);
            holder.streamText3 = (TextView) convertView.findViewById(R.id.tvStreamText3);
            holder.streamText4 = (TextView) convertView.findViewById(R.id.tvStreamText4);
            holder.streamImage = (ImageView) convertView.findViewById(R.id.ivStreamImage);
            holder.currentStream = (TextView) convertView.findViewById(R.id.tvCurrentStream);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        MediaStream stream = mStreams.get(position);

        if (stream.getType().equals(MediaStreamType.Audio)) {
            holder.streamImage.setImageResource(R.drawable.vp_audio);
            holder.streamLanguage.setText(Utils.getFullLanguageName(stream.getLanguage()));
            holder.streamText1.setText("Codec: " + (stream.getCodec() != null ? stream.getCodec() : ""));
            holder.streamText2.setText("Layout: " + (stream.getChannelLayout() != null ? stream.getChannelLayout() : ""));
            holder.streamText3.setText("Bitrate: " + getBitrateString(stream.getBitRate()));
            holder.streamText4.setText("Default: " + (stream.getIsDefault() ? "Yes" : "No"));
        } else if (stream.getType().equals(MediaStreamType.Subtitle)) {
            holder.streamImage.setImageResource(R.drawable.vp_subs);
            holder.streamLanguage.setText(Utils.getFullLanguageName(stream.getLanguage()));
            holder.streamText1.setText("Codec: " + (stream.getCodec() != null ? stream.getCodec() : ""));
            holder.streamText2.setText("Default: " + (stream.getIsDefault() ? "Yes" : "No"));
            holder.streamText3.setText("Forced: " + (stream.getIsForced() ? "Yes" : "No"));
            holder.streamText4.setText("External: " + (stream.getIsExternal() ? "Yes" : "No"));
        }

        if (stream.getIndex() == currentIndex) {
            holder.currentStream.setVisibility(View.VISIBLE);
        } else {
            holder.currentStream.setVisibility(View.GONE);
        }

        return convertView;
    }

    private class ViewHolder {
        public ImageView streamImage;
        public TextView streamLanguage;
        public TextView streamText1;
        public TextView streamText2;
        public TextView streamText3;
        public TextView streamText4;
        public TextView currentStream;
    }

    private String getBitrateString(Integer bitrate) {
        if (bitrate == null) {
            bitrate = 0;
        }
        bitrate = bitrate / 1024;
        return String.valueOf(bitrate) + " kbps";
    }
}
