package com.mb.android.adapters;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mb.android.MenuEntity;
import com.mb.android.R;

import java.util.List;

/**
 * Created by Mark on 2014-04-24.
 */
public class TvRootMenuAdapter extends BaseAdapter {

    private List<MenuEntity> mItems;
    private LayoutInflater mLayoutInflater;
    private Integer mSelectedIndex = null;

    public TvRootMenuAdapter(List<MenuEntity> items, LayoutInflater layoutInflater) {
        mItems = items;
        mLayoutInflater = layoutInflater;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int i) {
        return mItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        if (view == null) {
            view = mLayoutInflater.inflate(R.layout.tv_widget_root_menu_item, viewGroup, false);
        }

        TextView text = (TextView) view.findViewById(R.id.tvMenuItemText);
        text.setText(mItems.get(i).Name);
        if (mSelectedIndex != null && i == mSelectedIndex) {
            text.setTextColor(Color.parseColor("#ff6600"));
            text.setTextSize(26);
            Shader textShader=new LinearGradient(0, 0, 0, 20,
                    new int[]{Color.parseColor("#10aaff"),Color.parseColor("#00bbff")},
                    new float[]{0, 1}, Shader.TileMode.CLAMP);
            text.getPaint().setShader(textShader);
        } else {
            text.setTextColor(Color.parseColor("#ffffff"));
            text.setTextSize(20);
            text.getPaint().setShader(null);
        }


        return view;
    }

    public void setCurrentIndex(int index) {
        mSelectedIndex = index;
        this.notifyDataSetChanged();
    }
}
