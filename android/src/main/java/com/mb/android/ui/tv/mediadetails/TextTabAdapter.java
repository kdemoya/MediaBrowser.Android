package com.mb.android.ui.tv.mediadetails;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mb.android.MB3Application;
import com.mb.android.R;

import java.util.List;

/**
 * Created by Mark on 2014-11-02.
 */
public class TextTabAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private List<String> sections;
    private int selectedIndex = 0;

    public TextTabAdapter(Context context, List<String> sections) {

        inflater = LayoutInflater.from(context != null ? context : MB3Application.getInstance());
        this.sections = sections;
    }

    public void setSelectedIndex(int newIndex) {
        selectedIndex = newIndex;
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return sections != null ? sections.size() : 0;
    }

    @Override
    public Object getItem(int i) {
        return sections != null ? sections.get(i) : null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        Holder holder;

        if (view == null) {
            holder = new Holder();
            view = inflater.inflate(R.layout.widget_text_tile, viewGroup, false);
            holder.sectionName = (TextView) view.findViewById(R.id.tvViewTitle);
            holder.sectionName.setTextSize(24f);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        holder.sectionName.setText(sections.get(i));

        if (i == selectedIndex) {
            holder.sectionName.setTextColor(MB3Application.getInstance().getResources().getColor(R.color.white));
        } else {
            holder.sectionName.setTextColor(Color.parseColor("#70ffffff"));
        }

        return view;
    }


    private class Holder {
        public TextView sectionName;
    }
}
