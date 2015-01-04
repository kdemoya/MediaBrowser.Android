package com.mb.android.DialogFragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.logging.FileLogger;
import com.mb.android.utils.Utils;
import com.mb.network.Connectivity;

import mediabrowser.apiinteraction.android.profiles.AndroidProfile;
import mediabrowser.model.dlna.StreamBuilder;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dlna.VideoOptions;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;

/**
 * Created by Mark on 12/12/13.
 *
 * Show a dialog allowing the user to choose what audio and or subtitle stream should be used during playback
 */
public class StreamSelectionDialogFragment extends DialogFragment {

    private RadioGroup mAudioStreamRadioGroup;
    private RadioGroup mSubtitleStreamRadioGroup;
    private StreamSelectionDialogListener mListener;
    private StreamInfo mInfo;
    private BaseItemDto mMedia;
    private int mSelectedAudioStream;
    private int mSelectedSubtitleStream;

    /**
     * Class Constructor
     */
    public StreamSelectionDialogFragment() {}

    public void setItem(BaseItemDto item) {
        mMedia = item;
    }

    public void setStreams(int audioStream, int subtitleStream) {
        mSelectedAudioStream = audioStream;
        mSelectedSubtitleStream = subtitleStream;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (StreamSelectionDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement StreamSelectionDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (mMedia != null) {
            mInfo = buildStreamInfo(mMedia);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getResources().getString(R.string.stream_selection_title_string));

        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams") View dialogContent = inflater.inflate(R.layout.fragment_playback_options, null);

        mAudioStreamRadioGroup = (RadioGroup) dialogContent.findViewById(R.id.rgAudioStreams);
        mSubtitleStreamRadioGroup = (RadioGroup) dialogContent.findViewById(R.id.rgSubtitleStreams);

        RadioButton rButton;
        if (mInfo != null && mInfo.getMediaSource() != null && mInfo.getMediaSource().getMediaStreams() != null) {
            for (MediaStream ms : mInfo.getMediaSource().getMediaStreams()) {

                if (ms.getType() == MediaStreamType.Audio) {
                    rButton = new RadioButton(dialogContent.getContext());
                    rButton.setText(Utils.buildAudioDisplayString(ms));
                    rButton.setTag(String.valueOf(ms.getIndex()));
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        rButton.setButtonDrawable(R.drawable.mediabrowser_btn_radio_holo_dark);
                    }
                    mAudioStreamRadioGroup.addView(rButton);
                } else if (ms.getType() == MediaStreamType.Subtitle) {
                    rButton = new RadioButton(dialogContent.getContext());
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        rButton.setButtonDrawable(R.drawable.mediabrowser_btn_radio_holo_dark);
                    }
                    rButton.setText(Utils.buildSubtitleDisplayString(ms));
                    rButton.setTag(String.valueOf(ms.getIndex()));
                    mSubtitleStreamRadioGroup.addView(rButton);
                }
            }

            setAudioStreamsInitialCheckedState(mInfo);
            setSubtitleStreamsInitialCheckedState(mInfo);
        }

        builder.setView(dialogContent)
                .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        int rbId = mAudioStreamRadioGroup.getCheckedRadioButtonId();
                        RadioButton rb = (RadioButton) mAudioStreamRadioGroup.findViewById(rbId);
                        int selectedAudioStreamIndex = Integer.valueOf((String) rb.getTag());

                        rbId = mSubtitleStreamRadioGroup.getCheckedRadioButtonId();
                        rb = (RadioButton) mSubtitleStreamRadioGroup.findViewById(rbId);
                        int selectedSubtitleStreamIndex = Integer.valueOf((String) rb.getTag());

                        mListener.onDialogPositiveClick(selectedAudioStreamIndex, selectedSubtitleStreamIndex, mInfo.getMediaSourceId());
                    }
                })
                .setNegativeButton(getResources().getString(R.string.cancel_button), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onDialogNegativeClick(StreamSelectionDialogFragment.this);
                    }
                });

        return builder.create();
    }

    private void setSubtitleStreamsInitialCheckedState(StreamInfo info) {
        if (mSubtitleStreamRadioGroup == null) return;

        int targetStream = (mSelectedSubtitleStream != -1) ? mSelectedSubtitleStream : info.getSubtitleStreamIndex() != null ? info.getSubtitleStreamIndex() : -1;

        if (mSubtitleStreamRadioGroup.getChildCount() > 0) {
            if (targetStream != -1) {
                for (int i = 0; i < mSubtitleStreamRadioGroup.getChildCount(); i++) {
                    View view = mSubtitleStreamRadioGroup.getChildAt(i);
                    if (view instanceof RadioButton && view.getTag().equals(String.valueOf(targetStream))) {
                        ((RadioButton)view).setChecked(true);
                    }
                }
            } else {
                ((RadioButton)mSubtitleStreamRadioGroup.getChildAt(0)).setChecked(true);
            }
        }
    }

    private void setAudioStreamsInitialCheckedState(StreamInfo info) {
        if (mAudioStreamRadioGroup == null) return;

        int targetStream = mSelectedAudioStream != -1 ? mSelectedAudioStream : info.getAudioStreamIndex() != null ? info.getAudioStreamIndex() : -1;

        if (mAudioStreamRadioGroup.getChildCount() > 0) {
            if (targetStream != -1) {
                for (int i = 0; i < mAudioStreamRadioGroup.getChildCount(); i++) {
                    View view = mAudioStreamRadioGroup.getChildAt(i);
                    if (view instanceof RadioButton && view.getTag().equals(String.valueOf(targetStream))) {
                        ((RadioButton)view).setChecked(true);
                    }
                }
            } else {
                ((RadioButton)mAudioStreamRadioGroup.getChildAt(0)).setChecked(true);
            }
        }
    }

    private StreamInfo buildStreamInfo(BaseItemDto baseItemDto) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MB3Application.getInstance());
        if (prefs == null) return null;

        String bitrate;

        if (Connectivity.isConnectedLAN(MB3Application.getInstance())) {
            bitrate = prefs.getString("pref_local_bitrate", "1800000");
        } else {
            bitrate = prefs.getString("pref_cellular_bitrate", "450000");
        }

        boolean hlsEnabled = prefs.getBoolean("pref_enable_hls", true);
        boolean h264StrictModeEnabled = prefs.getBoolean("pref_h264_strict", true);

        FileLogger.getFileLogger().Info("Create VideoOptions");
        VideoOptions options = new VideoOptions();
        options.setItemId(baseItemDto.getId());
        options.setMediaSources(baseItemDto.getMediaSources());
        options.setProfile(new AndroidProfile(hlsEnabled, false));
        options.setDeviceId(Settings.Secure.getString(MB3Application.getInstance().getContentResolver(), Settings.Secure.ANDROID_ID));
        options.setMaxBitrate(Integer.valueOf(bitrate));

        FileLogger.getFileLogger().Info("Create StreamInfo");
        StreamInfo streamInfo = new StreamBuilder().BuildVideoItem(options);

        if (streamInfo == null) {
            FileLogger.getFileLogger().Info("streamInfo is null");
        }
        return streamInfo;
    }

    public interface StreamSelectionDialogListener {
        public void onDialogPositiveClick(int audioStreamIndex, int subtitleStreamIndex, String mediaSourceId);

        public void onDialogNegativeClick(DialogFragment dialog);
    }
}
