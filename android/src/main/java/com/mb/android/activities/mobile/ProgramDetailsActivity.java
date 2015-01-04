package com.mb.android.activities.mobile;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.Playlist;
import com.mb.android.activities.BaseMbMobileActivity;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.DialogFragments.RecordSettingsDialogFragment;
import com.mb.android.MB3Application;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.fragments.NavigationMenuFragment;
import com.mb.android.logging.FileLogger;
import com.mb.android.player.AudioService;
import com.mb.android.ui.mobile.playback.PlaybackActivity;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.livetv.BaseTimerInfoDto;
import mediabrowser.model.livetv.ProgramAudio;
import mediabrowser.model.livetv.ProgramInfoDto;
import mediabrowser.model.livetv.RecordingInfoDto;
import mediabrowser.model.livetv.SeriesTimerInfoDto;
import mediabrowser.model.livetv.TimerInfoDto;
import com.mb.android.utils.Utils;
import mediabrowser.model.session.PlayCommand;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Mark on 2014-06-05.
 *
 * Activity that allows viewing and editing of a recording timer for a program
 */
public class ProgramDetailsActivity extends BaseMbMobileActivity implements View.OnClickListener {

    private ActionBarDrawerToggle mDrawerToggle;
    private BaseTimerInfoDto timer;
    private ProgramInfoDto program;
    private RecordingInfoDto recording;
    private NetworkImageView primaryImage;
    private ImageView starRating;
    private ImageView recordingIcon;
    private TextView programTitle;
    private TextView episodeTitle;
    private TextView programOverview;
    private TextView programGenres;
    private TextView recordingStatus;
    private TextView startDate;
    private TextView startTime;
    private TextView runTime;
    private TextView stationName;
    private TextView audioFormat;
    // Changing visibility of controls is causing the onCheckedChanged listener to fire.
    // Need to ignore during initial setup.
    private boolean mIsInitialSetup = true;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_program_details);

        String jsonData = getMb3Intent().getStringExtra("timer");
        if (jsonData != null) {
            timer = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, BaseTimerInfoDto.class);
        }
        jsonData = getMb3Intent().getStringExtra("program");
        if (jsonData != null) {
            program = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, ProgramInfoDto.class);
        }
        jsonData = getMb3Intent().getStringExtra("recording");
        if (jsonData != null) {
            recording = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, RecordingInfoDto.class);
        }

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawer.setFocusableInTouchMode(false);

        NavigationMenuFragment fragment = (NavigationMenuFragment) getSupportFragmentManager().findFragmentById(R.id.left_drawer);
        if (fragment != null && fragment.isInLayout()) {
            fragment.setDrawerLayout(drawer);
        }

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                drawer,
                R.string.abc_action_bar_home_description,
                R.string.abc_action_bar_up_description) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
//                getActionBar().setTitle(mTitle);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
//                getActionBar().setTitle(mDrawerTitle);
            }

        };

        drawer.setDrawerListener(mDrawerToggle);

        buildControls();

        mMini = (MiniController) findViewById(R.id.miniController1);
        mCastManager.addMiniController(mMini);

        mIsInitialSetup = false;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            buildUi();
        }
    }

    @Override
    protected void onConnectionRestored() {
        buildUi();
    }

    private void buildUi() {
        if (program != null) {
            setProgramDetails(program);
        }

        if (timer != null) {
            setProgramDetails(timer);
        }

        if (recording != null) {
            setProgramDetails(recording);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (recording != null) {
            menu.add(getResources().getString(R.string.play_action_bar_button))
                    .setIcon(R.drawable.play).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
//            menu.add(getResources().getString(R.string.ltv_delete_recording))
//                    .setIcon(R.drawable.abc_ic_clear).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        if (item.getTitle().equals(getResources().getString(R.string.play_action_bar_button))) {

            AudioService.PlayerState currentState = MB3Application.getAudioService().getPlayerState();
            if (currentState.equals(AudioService.PlayerState.PLAYING) || currentState.equals(AudioService.PlayerState.PAUSED)) {
                MB3Application.getAudioService().stopMedia();
            }
            MB3Application.getInstance().PlayerQueue = new Playlist();

            if (mCastManager.isConnected()) {
                if (recording != null) {
                    BaseItemDto baseItemDto = new BaseItemDto();
                    baseItemDto.setId(recording.getId());
                    baseItemDto.setName(recording.getName());
                    baseItemDto.setType(recording.getType());
                    baseItemDto.setMediaType(recording.getMediaType());
                    baseItemDto.setIsFolder(false);
                    mCastManager.playItem(baseItemDto, PlayCommand.PlayNow, 0L);
                }
            } else {
                PlaylistItem playableItem = new PlaylistItem();
                playableItem.Id = recording.getId();
                playableItem.Name = recording.getName();

                if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(recording.getEpisodeTitle())) {
                    playableItem.SecondaryText = recording.getEpisodeTitle();
                }

                playableItem.Type = recording.getType();

                MB3Application.getInstance().PlayerQueue.PlaylistItems = new ArrayList<>();
                MB3Application.getInstance().PlayerQueue.PlaylistItems.add(playableItem);

                Intent intent = new Intent(this, PlaybackActivity.class);
                startActivity(intent);
            }

        } else if (item.getTitle().equals(getResources().getString(R.string.ltv_delete_recording))) {

            MB3Application.getInstance().API.DeleteLiveTvRecordingAsync(recording.getId(), new EmptyResponse());

        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }


    private void setProgramDetails(RecordingInfoDto recording) {
        if (recording == null) return;
        if (recording.getImageTags() != null && recording.getImageTags().containsKey(ImageType.Primary)) {
            setPrimaryImageUsingItemId(recording.getId());
        }
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(recording.getSeriesTimerId())) {
            showSeriesRecordingIcon();
        }
        setStartDateAndTime(recording.getStartDate());
        setRuntime(recording.getRunTimeTicks());
        setChannelName(recording.getChannelName());
        setCommunityRating(recording.getCommunityRating());
        setGenres(recording.getGenres());
        setAudioFormat(recording.getAudio());
        programTitle.setText(recording.getName());
        setEpisodeTitle(recording.getEpisodeTitle());
        setOverview(recording.getOverview());
    }

    private void setProgramDetails(ProgramInfoDto programInfo) {
        if (programInfo == null) return;
        if (programInfo.getHasPrimaryImage()) {
            setPrimaryImageUsingItemId(programInfo.getId());
        }
        setStartDateAndTime(programInfo.getStartDate());
        setRuntime(programInfo.getRunTimeTicks());
        setChannelName(programInfo.getChannelName());
        setCommunityRating(programInfo.getCommunityRating());
        setGenres(programInfo.getGenres());
        setAudioFormat(programInfo.getAudio());
        programTitle.setText(programInfo.getName());
        setEpisodeTitle(programInfo.getEpisodeTitle());
        setOverview(programInfo.getOverview());
    }

    private void setProgramDetails(BaseTimerInfoDto timer) {
        if (timer == null) return;
        if (timer instanceof TimerInfoDto) {
            TimerInfoDto timerInfoDto = (TimerInfoDto) timer;
            setProgramDetails(timerInfoDto.getProgramInfo());
            setStartDateAndTime(timerInfoDto.getStartDate());
            setRuntime(timerInfoDto.getRunTimeTicks());
            setChannelName(timerInfoDto.getChannelName());
            if (timerInfoDto.getStatus() != null) {
                recordingStatus.setText("Status: " + timerInfoDto.getStatus().toString());
                recordingStatus.setVisibility(View.VISIBLE);
            }
            if (timerInfoDto.getSeriesTimerId() != null && !timerInfoDto.getSeriesTimerId().isEmpty()) {
                showSeriesRecordingIcon();
            } else if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(timerInfoDto.getId())) {
                showRecordingIcon();
            }
        } else if (timer instanceof SeriesTimerInfoDto) {
            SeriesTimerInfoDto seriesTimer = (SeriesTimerInfoDto) timer;
            if (seriesTimer.getHasPrimaryImage()) {
                setPrimaryImageUsingItemId(seriesTimer.getId());
            }
            if (seriesTimer.getDayPattern() != null) {
                startDate.setText(String.valueOf(seriesTimer.getDayPattern()));
                startDate.setVisibility(View.VISIBLE);
            }
            setChannelName(seriesTimer.getChannelName());
        }
        programTitle.setText(timer.getName());
        setOverview(timer.getOverview());
    }

    private void setRuntime(Long runtimeText) {
        if (runtimeText == null) return;
        runTime.setText(Utils.TicksToMinutesString(runtimeText));
        runTime.setVisibility(View.VISIBLE);

    }

    private void showRecordingIcon() {
        recordingIcon.setImageResource(R.drawable.record_icon);
        recordingIcon.setVisibility(View.VISIBLE);
    }

    private void showSeriesRecordingIcon() {
        recordingIcon.setImageResource(R.drawable.record_series_icon);
        recordingIcon.setVisibility(View.VISIBLE);
    }

    private void setChannelName(String text) {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(text)) return;
        stationName.setText(text);
        stationName.setVisibility(View.VISIBLE);
    }

    private void setPrimaryImageUsingItemId(String itemId) {
        ImageOptions options = new ImageOptions();
        options.setImageType(ImageType.Primary);
        options.setHeight(500);
        String imageUrl = MB3Application.getInstance().API.GetImageUrl(itemId, options);
        primaryImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
    }

    private void setCommunityRating(Float rating) {
        if (rating == null) return;
        Utils.ShowStarRating(rating, starRating);
        starRating.setVisibility(View.VISIBLE);
    }

    private void setGenres(List<String> genres) {
        if (genres == null || genres.size() == 0) return;
        String gText = "";
        for (String genre : genres) {
            if (!gText.isEmpty())
                gText += "<font color='Aqua'> &#149 </font>";
            gText += genre;
        }
        if (!gText.isEmpty()) {
            programGenres.setText(Html.fromHtml(gText), TextView.BufferType.SPANNABLE);
            programGenres.setVisibility(View.VISIBLE);
        }
    }

    private void setAudioFormat(ProgramAudio audio) {
        if (audio == null) return;
        audioFormat.setText(audio.toString());
        audioFormat.setVisibility(View.VISIBLE);
    }

    private void setEpisodeTitle(String text) {
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(text)) {
            episodeTitle.setText(text);
            episodeTitle.setVisibility(View.VISIBLE);
        }
    }

    private void setOverview(String text) {
        if (text == null) return;
        // Some providers stupidly put the show name as the overview
        if (programTitle == null || programTitle.getText() == null || !text.equalsIgnoreCase(programTitle.getText().toString())) {
            programOverview.setText(text);
            programOverview.setVisibility(View.VISIBLE);
        }
    }

    private void setStartDateAndTime(Date date) {
        if (startDate == null) return;
        // Set start time text
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat timeFormat = new SimpleDateFormat("hh:mm a");

        Date sDate = Utils.convertToLocalDate(date);
        String formattedDate = dateFormat.format(sDate);
        startDate.setText(formattedDate);
        startDate.setVisibility(View.VISIBLE);
        String formattedTime = timeFormat.format(sDate);
        if (formattedTime != null && formattedTime.startsWith("0")) {
            formattedTime = formattedTime.replaceFirst("0", " ");
        }
        startTime.setText(formattedTime);
        startTime.setVisibility(View.VISIBLE);
    }


    private void buildControls() {

        primaryImage = (NetworkImageView) findViewById(R.id.ivPrimaryImage);
        starRating = (ImageView) findViewById(R.id.ivStarRating);
        programTitle = (TextView) findViewById(R.id.tvMediaTitle);
        episodeTitle = (TextView) findViewById(R.id.tvEpisodeTitle);
        programOverview = (TextView) findViewById(R.id.tvMediaOverview);
        programGenres = (TextView) findViewById(R.id.tvGenreValues);
        startDate = (TextView) findViewById(R.id.tvStartDate);
        startTime = (TextView) findViewById(R.id.tvStartTime);
        runTime = (TextView) findViewById(R.id.tvRuntime);
        stationName = (TextView) findViewById(R.id.tvStationName);
        audioFormat = (TextView) findViewById(R.id.tvAudioFormat);
        recordingIcon = (ImageView) findViewById(R.id.ivRecordingImage);

        recordingStatus = (TextView) findViewById(R.id.tvRecordingStatus);

        Button configureRecordingTimer = (Button) findViewById(R.id.btnSaveRecording);
        configureRecordingTimer.setOnClickListener(this);
        Button cancelRecordingTimer = (Button) findViewById(R.id.btnCancelRecording);
        cancelRecordingTimer.setOnClickListener(this);

        if (timer != null) {

            if (program == null) {
                configureRecordingTimer.setText(getResources().getString(R.string.ltv_update_recording_button));
            }

            if (timer instanceof TimerInfoDto) {
                cancelRecordingTimer.setText(getResources().getString(R.string.ltv_cancel_recording));
            } else if (timer instanceof SeriesTimerInfoDto) {
                cancelRecordingTimer.setText(getResources().getString(R.string.ltv_cancel_series));
            }

            if (userCanManageLiveTV()) {
                cancelRecordingTimer.setVisibility(View.VISIBLE);
            }
        }

        if (recording != null || !userCanManageLiveTV()) {
            configureRecordingTimer.setVisibility(View.GONE);
        }

    }


    private boolean userCanManageLiveTV() {

        return MB3Application.getInstance().user != null
                && MB3Application.getInstance().user.getConfiguration() != null
                && MB3Application.getInstance().user.getConfiguration().getEnableLiveTvManagement();

    }


    @Override
    public void onClick(View view) {

        if (mIsInitialSetup) return;

        switch (view.getId()) {

            case R.id.btnSaveRecording:

//                if (timer instanceof SeriesTimerInfoDto) {
//                    MB3Application.getInstance().API.UpdateLiveTvSeriesTimerAsync((SeriesTimerInfoDto) timer, new UpdateTimerCallback());
//                } else {
//                    MB3Application.getInstance().API.UpdateLiveTvTimerAsync((TimerInfoDto) timer, new UpdateTimerCallback());
//                }
                if (timer == null && program != null) {
                    // We're browsing program info for a program in the guide, not a scheduled recording
                    MB3Application.getInstance().API.GetDefaultLiveTvTimerInfo(program.getId(), new GetDefaultTimerResponse());
                } else {
                    // It's a scheduled recording. The timer already exists on the server
                    RecordSettingsDialogFragment dialogFragment = new RecordSettingsDialogFragment();
                    dialogFragment.setTimerInfo(timer);
                    dialogFragment.setIsNewRecording(false);
                    if (program != null) {
                        dialogFragment.setProgramInfo(program);
                    }
                    dialogFragment.show(this.getSupportFragmentManager(), "RecordSettingsDialog");
                }

            case R.id.btnCancelRecording:

                if (timer == null) return;

                if (timer instanceof TimerInfoDto) {
                    FileLogger.getFileLogger().Info("Cancel Recording");
                    MB3Application.getInstance().API.CancelLiveTvTimerAsync(timer.getId(), new cancelRecordingResponse());

                } else if (timer instanceof SeriesTimerInfoDto) {
                    FileLogger.getFileLogger().Info("Cancel Series Recording");
                    MB3Application.getInstance().API.CancelLiveTvSeriesTimerAsync(timer.getId(), new cancelRecordingResponse());
                }

//                AlertDialog.Builder builder = new AlertDialog.Builder(this);
//
//                final AlertDialog dialog = builder
//                        .setMessage("Are are sure you want to delete this scheduled recording?")
//                        .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//
//                            }
//                        })
//                        .setNegativeButton(R.string.cancel_button, null)
//                        .create();
//
//                dialog.show();
        }
    }

    private class cancelRecordingResponse extends EmptyResponse {
        @Override
        public void onResponse() {
            ProgramDetailsActivity.this.finish();
        }
        @Override
        public void onError(Exception ex) {
            Toast.makeText(ProgramDetailsActivity.this, "Error cancelling scheduled recording", Toast.LENGTH_LONG).show();
            FileLogger.getFileLogger().ErrorException("Error cancelling scheduled recording", ex);
            ProgramDetailsActivity.this.finish();
        }
    }

    public class GetDefaultTimerResponse extends Response<SeriesTimerInfoDto> {
        @Override
        public void onResponse(SeriesTimerInfoDto seriesTimerInfoDto) {

            if (seriesTimerInfoDto == null) return;

            RecordSettingsDialogFragment dialogFragment = new RecordSettingsDialogFragment();
            dialogFragment.setTimerInfo(seriesTimerInfoDto);
            dialogFragment.setIsNewRecording(true);
            if (program != null) {
                dialogFragment.setProgramInfo(program);
            }
            dialogFragment.show(ProgramDetailsActivity.this.getSupportFragmentManager(), "RecordSettingsDialog");
        }
    }
}
