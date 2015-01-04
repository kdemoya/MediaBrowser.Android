package com.mb.android.ui.tv.library.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.ui.tv.library.LibraryActivity;

import java.util.ArrayList;
import java.util.List;


public class MenuDialogFragment extends DialogFragment {

    private String currentLayout;
    private List<String> mActions;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        currentLayout = getArguments().getString("CurrentView", "Poster");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams")
        View mDialogContent = inflater.inflate(R.layout.fragment_latest_items_popup, null);

        mDialogContent.findViewById(R.id.rlBannerContainer).setVisibility(View.GONE);
        getListContent();

        ListView actionsList = (ListView) mDialogContent.findViewById(R.id.lvLatestItems);
        actionsList.setAdapter(new ViewChangeAdapter());
        actionsList.setOnItemClickListener(onViewOptionsItemClickListener);

        builder.setView(mDialogContent);

        return builder.create();

    }


    private void getListContent() {

        mActions = new ArrayList<>();
        mActions.add("Cover Flow");
        mActions.add("List");
        mActions.add("Grid");
    }


    private class ViewChangeAdapter extends BaseAdapter {

        private LayoutInflater mLayoutInflater;


        public ViewChangeAdapter() {
            mLayoutInflater = (LayoutInflater) MB3Application.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mActions.size();
        }

        @Override
        public Object getItem(int position) {
            return mActions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ActionHolder actionHolder;

            if (convertView == null) {

                convertView = mLayoutInflater.inflate(R.layout.widget_long_press_item, parent, false);

                actionHolder = new ActionHolder();
                actionHolder.actionImage = (NetworkImageView) convertView.findViewById(R.id.ivActionImage);
                actionHolder.actionText = (TextView) convertView.findViewById(R.id.tvActionText);

                convertView.setTag(actionHolder);

            } else {
                actionHolder = (ActionHolder) convertView.getTag();
            }

            actionHolder.actionText.setText(mActions.get(position));
            switch (position) {
                case 0:
                    actionHolder.actionImage.setDefaultImageResId(R.drawable.view_coverflow);
                    break;
                case 1:
                    actionHolder.actionImage.setDefaultImageResId(R.drawable.view_list);
                    break;
                case 2:
                    actionHolder.actionImage.setDefaultImageResId(R.drawable.view_poster);
                    break;
            }

            return convertView;
        }
    }


    private AdapterView.OnItemClickListener onViewOptionsItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            LibraryActivity activity = (LibraryActivity) getActivity();

            switch(mActions.get(position)) {
                case "Cover Flow":
                    if (currentLayout.equalsIgnoreCase("Cover Flow")) return;
                    if (activity != null) {
                        activity.onCoverflowSelected();
                    }
                    break;
                case "List":
                    if (currentLayout.equalsIgnoreCase("List")) return;
                    if (activity != null) {
                        activity.onListSelected();
                    }
                    break;
                case "Grid":
                    if (currentLayout.equalsIgnoreCase("Grid")) return;
                    if (activity != null) {
                        activity.onGridSelected();
                    }
                    break;
                case "Thumb":

                    break;
                case "Strip":

                    break;
            }
            MenuDialogFragment.this.dismiss();
        }
    };



    private class ActionHolder {
        public TextView actionText;
        public NetworkImageView actionImage;
    }
}
