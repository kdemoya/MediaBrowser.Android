package com.mb.android.DialogFragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.mb.android.R;
import com.mb.android.ui.mobile.playback.PlaybackActivity;
import com.mb.network.Connectivity;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Mark on 2014-07-25.
 *
 * Dialog fragment that allows the user to adjust the current bitrate
 */
public class BitrateSelectionDialogFragment extends DialogFragment {

    private List<String> mBitrateNames;
    private List<String> mBitrateValues;
    private String mBitrate;
    private SharedPreferences mPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] bitrateEntries = getResources().getStringArray(R.array.pref_bitrate_entries);
        mBitrateNames = Arrays.asList(bitrateEntries);
        String[] bitrateValues = getResources().getStringArray(R.array.pref_bitrate_values);
        mBitrateValues = Arrays.asList(bitrateValues);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (Connectivity.isConnectedLAN(getActivity())) {
            mBitrate = mPrefs.getString("pref_local_bitrate", "1800000");
        } else {
            mBitrate = mPrefs.getString("pref_cellular_bitrate", "450000");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = LayoutInflater.from(getActivity());

        View contentView = inflater.inflate(R.layout.widget_bitrate_selection, null, false);

        RadioGroup radioGroup = (RadioGroup) contentView.findViewById(R.id.rgBitrates);
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        for (int i = 0; i < mBitrateNames.size() && i < mBitrateValues.size(); i++) {
            RadioButton radioButton = new RadioButton(getActivity());
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                radioButton.setButtonDrawable(R.drawable.mediabrowser_btn_radio_holo_dark);
            }
            radioButton.setText(mBitrateNames.get(i));
            radioButton.setTag(mBitrateValues.get(i));
            if (mBitrateValues.get(i).equalsIgnoreCase(mBitrate)) {
                radioButton.setChecked(true);
            } else {
                radioButton.setChecked(false);
            }
            radioButton.setOnCheckedChangeListener(onCheckedChangeListener);
            radioGroup.addView(radioButton);
        }

        builder.setTitle(getResources().getString(R.string.bitrate_selection));
        builder.setView(contentView);

        return builder.create();
    }

    private RadioButton.OnCheckedChangeListener onCheckedChangeListener
            = new RadioButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            mBitrate = (String)buttonView.getTag();

            if (Connectivity.isConnectedLAN(getActivity())) {
                mPrefs.edit().putString("pref_local_bitrate", mBitrate).apply();
            } else {
                mPrefs.edit().putString("pref_cellular_bitrate", mBitrate).apply();
            }
            try {
                Activity activity = getActivity();
                if (activity != null) {
                    ((PlaybackActivity) activity).onBitrateSelected();
                }
            } catch (Exception ex) {

            }
            BitrateSelectionDialogFragment.this.dismiss();
        }
    };
}
