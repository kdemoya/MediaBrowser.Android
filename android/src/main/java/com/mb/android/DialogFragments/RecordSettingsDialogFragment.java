package com.mb.android.DialogFragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.mb.android.MB3Application;
import com.mb.android.R;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.model.livetv.BaseTimerInfoDto;
import mediabrowser.model.livetv.ProgramInfoDto;
import mediabrowser.model.livetv.SeriesTimerInfoDto;
import mediabrowser.model.livetv.TimerInfoDto;

import java.text.DateFormatSymbols;
import java.util.Locale;

/**
 * Created by Mark on 12/12/13.
 *
 * When a user enters a new address for an existing server (through the add new server button). This
 * dialog will show the user the existing addresses and confirm which one the user wants to overwrite.
 *
 * The user must select either the local or remote address before continuing.
 */
public class RecordSettingsDialogFragment extends DialogFragment implements CompoundButton.OnCheckedChangeListener {

    private LinearLayout recordSeriesContainer;
    private EditText prePaddingValue;
    private EditText postPaddingValue;
    private CheckBox recordSeriesCheckBox;
    private CheckBox sundayCheckBox;
    private CheckBox mondayCheckBox;
    private CheckBox tuesdayCheckBox;
    private CheckBox wednesdayCheckBox;
    private CheckBox thursdayCheckBox;
    private CheckBox fridayCheckBox;
    private CheckBox saturdayCheckBox;
    private CheckBox recordOnlyNewCheckBox;
    private CheckBox recordAnyTimeCheckBox;
    private CheckBox recordAllChannelsCheckBox;
    private CheckBox prePaddingCheckBox;
    private CheckBox postPaddingCheckBox;

    private BaseTimerInfoDto timer;
    private ProgramInfoDto program;
    private boolean isNewRecording;
    private boolean mIsInitialSetup = true;

    /**
     * Class Constructor
     */
    public RecordSettingsDialogFragment() {}

    public void setTimerInfo(BaseTimerInfoDto timerInfo) {
        this.timer = timerInfo;
    }

    public void setProgramInfo(ProgramInfoDto programinfo) {
        this.program = programinfo;
    }

    public void setIsNewRecording(boolean isNew) {
        isNewRecording = isNew;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View mDialogContent = inflater.inflate(R.layout.fragment_recording_settings, null);

        if (mDialogContent == null) return null;

        buildControls(mDialogContent);
        setInitialStates();
        mIsInitialSetup = false;

        builder.setTitle("Recording Settings")
                .setView(mDialogContent)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        if (isNewRecording) {
                            if (recordSeriesCheckBox.isChecked()) {
                                MB3Application.getInstance().API.CreateLiveTvSeriesTimerAsync((SeriesTimerInfoDto) timer, new EmptyResponse());
                            } else {
                                MB3Application.getInstance().API.CreateLiveTvTimerAsync(timer, new EmptyResponse());
                            }
                        } else {
                            if (recordSeriesCheckBox.isChecked()) {
                                MB3Application.getInstance().API.UpdateLiveTvSeriesTimerAsync((SeriesTimerInfoDto) timer, new EmptyResponse());
                            } else {
                                MB3Application.getInstance().API.UpdateLiveTvTimerAsync((TimerInfoDto)timer, new EmptyResponse());
                            }
                        }

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        return builder.create();
    }


    private void buildControls(View mDialogContent) {

        recordSeriesContainer = (LinearLayout) mDialogContent.findViewById(R.id.llRecordSeriesDetails);

        prePaddingValue = (EditText) mDialogContent.findViewById(R.id.etPrePaddingMinutes);
        prePaddingValue.addTextChangedListener(prePaddingTextWatcher);

        postPaddingValue = (EditText) mDialogContent.findViewById(R.id.etPostPaddingMinutes);
        postPaddingValue.addTextChangedListener(postPaddingTextWatcher);

        recordSeriesCheckBox = (CheckBox) mDialogContent.findViewById(R.id.chkRecordSeries);
        recordSeriesCheckBox.setOnCheckedChangeListener(this);

        DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
        String[] dayNames = symbols.getWeekdays();

        sundayCheckBox = (CheckBox) mDialogContent.findViewById(R.id.chkDaySunday);
        sundayCheckBox.setText(dayNames[1]);
        sundayCheckBox.setOnCheckedChangeListener(this);

        mondayCheckBox = (CheckBox) mDialogContent.findViewById(R.id.chkDayMonday);
        mondayCheckBox.setText(dayNames[2]);
        mondayCheckBox.setOnCheckedChangeListener(this);

        tuesdayCheckBox = (CheckBox) mDialogContent.findViewById(R.id.chkDayTuesday);
        tuesdayCheckBox.setText(dayNames[3]);
        tuesdayCheckBox.setOnCheckedChangeListener(this);

        wednesdayCheckBox = (CheckBox) mDialogContent.findViewById(R.id.chkDayWednesday);
        wednesdayCheckBox.setText(dayNames[4]);
        wednesdayCheckBox.setOnCheckedChangeListener(this);

        thursdayCheckBox = (CheckBox) mDialogContent.findViewById(R.id.chkDayThursday);
        thursdayCheckBox.setText(dayNames[5]);
        thursdayCheckBox.setOnCheckedChangeListener(this);

        fridayCheckBox = (CheckBox) mDialogContent.findViewById(R.id.chkDayFriday);
        fridayCheckBox.setText(dayNames[6]);
        fridayCheckBox.setOnCheckedChangeListener(this);

        saturdayCheckBox = (CheckBox) mDialogContent.findViewById(R.id.chkDaySaturday);
        saturdayCheckBox.setText(dayNames[7]);
        saturdayCheckBox.setOnCheckedChangeListener(this);

        recordOnlyNewCheckBox = (CheckBox) mDialogContent.findViewById(R.id.chkRecordNewOnly);
        recordOnlyNewCheckBox.setOnCheckedChangeListener(this);

        recordAnyTimeCheckBox = (CheckBox) mDialogContent.findViewById(R.id.chkRecordAnyTime);
        recordAnyTimeCheckBox.setOnCheckedChangeListener(this);

        recordAllChannelsCheckBox = (CheckBox) mDialogContent.findViewById(R.id.chkRecordAllChannels);
        recordAllChannelsCheckBox.setOnCheckedChangeListener(this);

        prePaddingCheckBox = (CheckBox) mDialogContent.findViewById(R.id.chkPrePaddingRequired);
        prePaddingCheckBox.setOnCheckedChangeListener(this);

        postPaddingCheckBox = (CheckBox) mDialogContent.findViewById(R.id.chkPostPaddingRequired);
        postPaddingCheckBox.setOnCheckedChangeListener(this);
    }


    private void setInitialStates() {

        if (timer == null) return;

        if (timer instanceof SeriesTimerInfoDto && !isNewRecording) {
            SeriesTimerInfoDto mSeriesTimer = (SeriesTimerInfoDto) timer;

            if (mSeriesTimer.getRecordNewOnly()) {
                recordOnlyNewCheckBox.setChecked(true);
            }

            if (mSeriesTimer.getRecordAnyTime()) {
                recordAnyTimeCheckBox.setChecked(true);
            }

            if (mSeriesTimer.getRecordAnyChannel()) {
                recordAllChannelsCheckBox.setChecked(true);
            }

            recordSeriesCheckBox.setChecked(true);

            if (mSeriesTimer.getDays() != null && !mSeriesTimer.getDays().isEmpty()) {
                for (String day : mSeriesTimer.getDays()) {

                    switch (day) {
                        case "Sunday":
                            sundayCheckBox.setChecked(true);
                            break;
                        case "Monday":
                            mondayCheckBox.setChecked(true);
                            break;
                        case "Tuesday":
                            tuesdayCheckBox.setChecked(true);
                            break;
                        case "Wednesday":
                            wednesdayCheckBox.setChecked(true);
                            break;
                        case "Thursday":
                            thursdayCheckBox.setChecked(true);
                            break;
                        case "Friday":
                            fridayCheckBox.setChecked(true);
                            break;
                        case "Saturday":
                            saturdayCheckBox.setChecked(true);
                            break;
                    }
                }
            }
        } else {
            if (program != null && program.getIsSeries() && isNewRecording) {
                // It's a series, but since it's a new recording hide all the series options until clicked
                recordSeriesCheckBox.setChecked(false);

            } else {
                recordSeriesCheckBox.setVisibility(View.GONE);
            }
        }

        if (timer.getIsPrePaddingRequired()) {
            prePaddingCheckBox.setChecked(true);
        }

        prePaddingValue.setText(String.valueOf(timer.getPrePaddingSeconds() / 60));

        if (timer.getIsPostPaddingRequired()) {
            postPaddingCheckBox.setChecked(true);
        }

        postPaddingValue.setText(String.valueOf(timer.getPostPaddingSeconds() / 60));
    }

    public TextWatcher prePaddingTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void afterTextChanged(Editable editable) {

            try {
                timer.setPrePaddingSeconds(Integer.valueOf(editable.toString()) * 60);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public TextWatcher postPaddingTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void afterTextChanged(Editable editable) {

            try {
                timer.setPostPaddingSeconds(Integer.valueOf(editable.toString()) * 60);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

        switch (compoundButton.getId()) {

            case R.id.chkRecordSeries:

                if (isChecked) {
                    recordSeriesContainer.setVisibility(View.VISIBLE);
                } else {
                    recordSeriesContainer.setVisibility(View.GONE);
                }

            case R.id.chkDaySunday:

                if (isChecked) {
                    if (!((SeriesTimerInfoDto) timer).getDays().contains("Sunday") && !mIsInitialSetup) {
                        ((SeriesTimerInfoDto) timer).getDays().add("Sunday");
                    }
                } else {
                    ((SeriesTimerInfoDto) timer).getDays().remove("Sunday");
                }

            case R.id.chkDayMonday:

                if (isChecked) {
                    if (!((SeriesTimerInfoDto) timer).getDays().contains("Monday") && !mIsInitialSetup) {
                        ((SeriesTimerInfoDto) timer).getDays().add("Monday");
                    }
                } else {
                    ((SeriesTimerInfoDto) timer).getDays().remove("Monday");
                }

            case R.id.chkDayTuesday:

                if (isChecked) {
                    Log.d("onCheckChanged", "Tuesday isChecked");
                    if (!((SeriesTimerInfoDto) timer).getDays().contains("Tuesday") && !mIsInitialSetup) {
                        ((SeriesTimerInfoDto) timer).getDays().add("Tuesday");
                    }
                } else {
                    Log.d("onCheckChanged", "Tuesday !isChecked");
                    ((SeriesTimerInfoDto) timer).getDays().remove("Tuesday");
                }

            case R.id.chkDayWednesday:

                if (isChecked) {
                    if (!((SeriesTimerInfoDto) timer).getDays().contains("Wednesday") && !mIsInitialSetup) {
                        ((SeriesTimerInfoDto) timer).getDays().add("Wednesday");
                    }
                } else {
                    ((SeriesTimerInfoDto) timer).getDays().remove("Wednesday");
                }

            case R.id.chkDayThursday:

                if (isChecked) {
                    if (!((SeriesTimerInfoDto) timer).getDays().contains("Thursday") && !mIsInitialSetup) {
                        ((SeriesTimerInfoDto) timer).getDays().add("Thursday");
                    }
                } else {
                    ((SeriesTimerInfoDto) timer).getDays().remove("Thursday");
                }

            case R.id.chkDayFriday:

                if (isChecked) {
                    if (!((SeriesTimerInfoDto) timer).getDays().contains("Friday") && !mIsInitialSetup) {
                        ((SeriesTimerInfoDto) timer).getDays().add("Friday");
                    }
                } else {
                    ((SeriesTimerInfoDto) timer).getDays().remove("Friday");
                }

            case R.id.chkDaySaturday:

                if (isChecked) {
                    if (!((SeriesTimerInfoDto) timer).getDays().contains("Saturday") && !mIsInitialSetup) {
                        ((SeriesTimerInfoDto) timer).getDays().add("Saturday");
                    }
                } else {
                    ((SeriesTimerInfoDto) timer).getDays().remove("Saturday");
                }

            case R.id.chkRecordNewOnly:

                if (!mIsInitialSetup) {
                    ((SeriesTimerInfoDto) timer).setRecordNewOnly(isChecked);
                }

            case R.id.chkRecordAnyTime:

                if (!mIsInitialSetup) {
                    ((SeriesTimerInfoDto) timer).setRecordAnyTime(isChecked);
                }

            case R.id.chkRecordAllChannels:

                if (!mIsInitialSetup) {
                    ((SeriesTimerInfoDto) timer).setRecordAnyChannel(isChecked);
                }

            case R.id.chkPrePaddingRequired:

                if (!mIsInitialSetup) {
                    timer.setIsPrePaddingRequired(isChecked);
                }

            case R.id.chkPostPaddingRequired:

                if (!mIsInitialSetup) {
                    timer.setIsPostPaddingRequired(isChecked);
                }
        }
    }
}
