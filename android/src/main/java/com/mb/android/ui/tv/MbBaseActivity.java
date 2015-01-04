package com.mb.android.ui.tv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.mb.android.MB3Application;
import com.mb.android.Playlist;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import com.mb.android.ui.main.SettingsActivity;
import com.mb.android.ui.tv.playback.AudioPlayer;
import com.mb.android.ui.tv.playback.VideoPlayer;
import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.AndroidApiClient;
import com.mb.android.interfaces.IWebsocketEventListener;
import com.mb.android.logging.FileLogger;
import com.mb.android.ui.main.ConnectionActivity;
import com.mb.android.ui.tv.boxset.BoxSetActivity;
import com.mb.android.ui.tv.homescreen.HomeScreenActivity;
import com.mb.android.ui.tv.library.LibraryActivity;
import com.mb.android.ui.tv.mediadetails.MediaDetailsActivity;
import com.mb.android.ui.tv.person.ActorDetailsActivity;
import mediabrowser.model.apiclient.ConnectionState;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.livetv.RecordingInfoDto;
import mediabrowser.model.session.PlayRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;


public abstract class MbBaseActivity extends FragmentActivity implements IWebsocketEventListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (MB3Application.getInstance().API == null) {
            MB3Application.getInstance().setIsConnected(false);
            Thread thread = new Thread() {
                @Override
                public void run() {
                    MB3Application.getInstance().getConnectionManager().Connect(connectionResult);
                }
            };
            thread.start();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MB3Application.getInstance().setCurrentActivity(this);
        if (Build.VERSION.SDK_INT >= 16) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    public void onPause() {
        clearReferences();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        clearReferences();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ActivityResults.USER_DATA_UPDATED && resultCode == RESULT_OK) {
            if (data != null) {
                FileLogger.getFileLogger().Debug("onActivityResult with data");
                String jsonData = data.getStringExtra("UserData");
                UserItemDataDto userData = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, UserItemDataDto.class);
                onUserDataUpdated(data.getStringExtra("Id"), userData);
            } else {
                FileLogger.getFileLogger().Debug("onActivityResult without data");
                onUserDataUpdated(null, null);
            }
        } else if (requestCode == ActivityResults.PLAYBACK_COMPLETED && resultCode == RESULT_OK) {
            onUserDataUpdated(null, null);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                onPlayButton();
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                onFastForwardButton();
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                onRewindButton();
                return true;
            case KeyEvent.KEYCODE_MENU:
                onMenuButton();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }


    //**********************************************************************************************
    // Navigation
    //**********************************************************************************************

    public void navigate(BaseItemDto item, String collectionType) {

        Intent intent = null;

        if ("season".equalsIgnoreCase(item.getType())) {
            intent = new Intent(this, LibraryActivity.class);
        } else if ("series".equalsIgnoreCase(item.getType())) {
            intent = new Intent(this, BoxSetActivity.class);
        } else if (item.getType().equalsIgnoreCase("boxset")) {
            intent = new Intent(this, BoxSetActivity.class);
        } else if ("person".equalsIgnoreCase(item.getType())) {
            intent = new Intent(this, ActorDetailsActivity.class);
        } else if (item.getIsFolder()) {
            intent = new Intent(this, LibraryActivity.class);
        } else {
            intent = new Intent(this, MediaDetailsActivity.class);
        }

        String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(item);
        intent.putExtra("CurrentBaseItemDTO", jsonData);
        intent.putExtra("CollectionType", collectionType);

        startActivityForResult(intent, ActivityResults.USER_DATA_UPDATED);
    }


    public void navigate(BaseItemPerson item, String collectionType) {

        String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(item);

        Intent intent = new Intent(MB3Application.getInstance(), ActorDetailsActivity.class);
        intent.putExtra("CurrentBaseItemDTO", jsonData);
        intent.putExtra("CollectionType", collectionType);
        startActivity(intent);
    }

    protected void navigate(RecordingInfoDto item) {
        if (item != null) {
            MB3Application.getInstance().API.GetItemAsync(item.getId(), MB3Application.getInstance().API.getCurrentUserId(), new Response<BaseItemDto>() {
                @Override
                public void onResponse(BaseItemDto itemDto) {
                    if (itemDto != null) {
                        navigate(itemDto, null);
                    }
                }
            });
        }
    }


    protected void setOverscanValues() {
        RelativeLayout overscanLayout = (RelativeLayout) findViewById(R.id.rlOverscanPadding);

        if (overscanLayout == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int left = prefs.getInt("overscan_left", 0);
        int top = prefs.getInt("overscan_top", 0);
        int right = prefs.getInt("overscan_right", 0);
        int bottom = prefs.getInt("overscan_bottom", 0);

        ViewGroup.MarginLayoutParams overscanMargins = (ViewGroup.MarginLayoutParams) overscanLayout.getLayoutParams();
        overscanMargins.setMargins(left, top, right, bottom);
        overscanLayout.requestLayout();
    }


    private Response<ConnectionResult> connectionResult = new Response<ConnectionResult>() {
        @Override
        public void onResponse(ConnectionResult result) {

            if (ConnectionState.SignedIn.equals(result.getState())) {
                // A server was found and the user has been signed in using previously saved credentials.
                // Ready to browse using result.ApiClient
                FileLogger.getFileLogger().Info("**** SIGNED IN ****");
                MB3Application.getInstance().API = (AndroidApiClient)result.getApiClient();
                MB3Application.getInstance().user = new UserDto();
                MB3Application.getInstance().user.setId(MB3Application.getInstance().API.getCurrentUserId());
                MB3Application.getInstance().setIsConnected(true);
                onConnectionRestored();
            } else {
                returnToConnectionActivity();
            }
        }

        @Override
        public void onError(Exception ex) {
            returnToConnectionActivity();
        }
    };


    private void returnToConnectionActivity() {
        FileLogger.getFileLogger().Info("Failed to recover session after crash");
        Intent intent = new Intent(MB3Application.getInstance(), ConnectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        this.finish();
    }


    /*
    If an Activity crashes, then this method is called after ConnectionManager has rebuilt the ApiClient
     */
    protected abstract void onConnectionRestored();

    /*
    This method is called from onActivityResult. It is to allow this activity to update it's content to reflect changes
    that have happened in the child activity.
     */
    protected abstract void onUserDataUpdated(String itemId, UserItemDataDto userItemDataDto);


//    protected abstract void onBaseItemReceived(BaseItemDto baseItemDto);

    protected abstract void onPlayButton();

    protected abstract void onFastForwardButton();

    protected abstract void onRewindButton();

    protected abstract void onMenuButton();


    private void clearReferences(){
        IWebsocketEventListener currActivity = MB3Application.getInstance().getCurrentActivity();
        if (currActivity != null && currActivity.equals(this))
            MB3Application.getInstance().setCurrentActivity(null);
    }


    //******************************************************************************************************************
    // IWebSocketEventListener callbacks
    //******************************************************************************************************************

    @Override
    public void onTakeScreenshotRequest() {
        // image naming and path  to include sd card  appending name you choose for file
        String mPath = Environment.getExternalStorageDirectory().toString() + "/Pictures/" + String.valueOf(new Date().getTime() + ".jpg");

        // create bitmap screen capture
        Bitmap bitmap;
        View v1 = getWindow().getDecorView().getRootView();
        v1.setDrawingCacheEnabled(true);
        bitmap = Bitmap.createBitmap(v1.getDrawingCache());
        v1.setDrawingCacheEnabled(false);

        OutputStream fout = null;
        File imageFile = new File(mPath);

        try {
            fout = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fout);
            fout.flush();
            fout.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        playShutterSound();
    }

    @Override
    public void onRemotePlayRequest(PlayRequest request, String mediaType) {

        if ("audio".equalsIgnoreCase(mediaType)) {
            MB3Application.getInstance().PlayerQueue = new Playlist();
            addItemsToPlaylist(request.getItemIds());
            Intent intent = new Intent(this, AudioPlayer.class);
            startActivity(intent);
        } else if ("video".equalsIgnoreCase(mediaType)) {
            MB3Application.getInstance().PlayerQueue = new Playlist();
            addItemsToPlaylist(request.getItemIds());
            Intent intent = new Intent(this, VideoPlayer.class);
            startActivity(intent);
        }
    }

    protected void addItemsToPlaylist(String[] itemIds) {
        for (String id : itemIds) {
            PlaylistItem item = new PlaylistItem();
            item.Id = id;
            MB3Application.getInstance().PlayerQueue.PlaylistItems.add(item);
        }
    }

    @Override
    public void onRemoteBrowseRequest(BaseItemDto item) {
        if ("video".equalsIgnoreCase(item.getMediaType())) {
            browseToVideoDetails(item);
        } else if ("audio".equalsIgnoreCase(item.getMediaType())) {
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getAlbumId())) {

            }
        } else if ("book".equalsIgnoreCase(item.getMediaType())) {
            browseToBookDetails(item);
        } else if ("game".equalsIgnoreCase(item.getMediaType())) {
            browseToVideoDetails(item);
        } else if ("photo".equalsIgnoreCase(item.getMediaType())) {
            browseToPhotoDetails(item);
        } else if ("musicalbum".equalsIgnoreCase(item.getType())) {
            browseToAlbumDetails(item);
        } else if ("musicartist".equalsIgnoreCase(item.getType())) {
            browseToArtistDetails(item);
        }
    }

    @Override
    public void onSeekCommand(Long seekPositionTicks) {

    }

    @Override
    public void onUserDataUpdated() {

    }

    @Override
    public void onGoHomeRequest() {
        Intent intent = new Intent(this, HomeScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onGoToSettingsRequest() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private MediaPlayer mShutterMediaPlayer = null;

    private void playShutterSound() {
        AudioManager audioManager = (AudioManager) MB3Application.getInstance().getSystemService(Context.AUDIO_SERVICE);
        int volume = audioManager.getStreamVolume( AudioManager.STREAM_NOTIFICATION);

        if (volume != 0)
        {
            if (mShutterMediaPlayer == null)
                mShutterMediaPlayer = MediaPlayer.create(MB3Application.getInstance(), Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            if (mShutterMediaPlayer != null)
                mShutterMediaPlayer.start();
        }
    }

    private void browseToVideoDetails(BaseItemDto item) {

        String jsonData = MB3Application.getInstance().getJsonSerializer().SerializeToString(item);

        Intent intent = new Intent(this, MediaDetailsActivity.class);
        intent.putExtra("CurrentBaseItemDTO", jsonData);
        startActivity(intent);
    }

    private void browseToBookDetails(BaseItemDto item) {

    }

    private void browseToPhotoDetails(BaseItemDto item) {

    }

    private void browseToAlbumDetails(BaseItemDto item) {

    }

    private void browseToArtistDetails(BaseItemDto item) {

    }
}
