package com.mb.android.activities.mobile;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.activities.BaseMbMobileActivity;
import mediabrowser.apiinteraction.Response;
import com.mb.android.playbackmediator.widgets.MiniController;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.fragments.NavigationMenuFragment;
import com.mb.android.utils.Utils;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.library.PlayAccess;
import com.mb.android.logging.FileLogger;

import java.io.File;

/**
 * Created by Mark on 12/12/13.
 *
 * This Activity shows all the relevant info for a book.
 */
public class BookDetailsActivity extends BaseMbMobileActivity {

    private ActionBarDrawerToggle mDrawerToggle;
    private BaseItemDto mItem;
    private boolean mAddFavoriteMenuItemVisible;
    private boolean mRemoveFavoriteMenuItemVisible;
    private File mDownloadingFile;
    private boolean isFresh = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_book_details);

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


        String jsonData = getMb3Intent().getStringExtra("Item");
        mItem = MB3Application.getInstance().getJsonSerializer().DeserializeFromString(jsonData, BaseItemDto.class);

        mMini = (MiniController) findViewById(R.id.miniController1);
        mCastManager.addMiniController(mMini);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (mItem.getPlayAccess().equals(PlayAccess.Full) && !mCastManager.isConnected()) {
            menu.add(getResources().getString(R.string.play_action_bar_button)).setIcon(R.drawable.play).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        MenuItem mRemoveFavoriteMenuItem = menu.add(getResources().getString(R.string.un_favorite_action_bar_button));
        mRemoveFavoriteMenuItem.setIcon(R.drawable.nfav);
        mRemoveFavoriteMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        if (mRemoveFavoriteMenuItemVisible)
            mRemoveFavoriteMenuItem.setVisible(true);
        else
            mRemoveFavoriteMenuItem.setVisible(false);

        MenuItem mAddFavoriteMenuItem = menu.add(getResources().getString(R.string.favorite_action_bar_button));
        mAddFavoriteMenuItem.setIcon(R.drawable.fav);
        mAddFavoriteMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        if (mAddFavoriteMenuItemVisible)
            mAddFavoriteMenuItem.setVisible(true);
        else
            mAddFavoriteMenuItem.setVisible(false);


        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        /*
        Play
         */
        if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.play_action_bar_button))) {

            String fileName = mItem.getPath();
            fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);

            // If these are equal it means Android couldn't determine the separator used.
            if (fileName.length() == mItem.getPath().length())
                fileName = mItem.getPath().substring(mItem.getPath().lastIndexOf('\\') + 1);

            fileName = fileName.replace('#', ' ');
            fileName = fileName.replace("  ", " ");

            File file = new File(Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DOWNLOADS, fileName);

            if (!file.exists()) {
                downloadFile(fileName, file);
            } else {
                openFile(file);
            }

        /*
        Set Favorite
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.favorite_action_bar_button))) {

            MB3Application.getInstance().API.UpdateFavoriteStatusAsync(
                    mItem.getId(),
                    MB3Application.getInstance().API.getCurrentUserId(),
                    true,
                    new UpdateFavoriteResponse()
            );

        /*
        Remove Favorite
         */
        } else if (((String) item.getTitle()).equalsIgnoreCase(getResources().getString(R.string.un_favorite_action_bar_button))) {

            MB3Application.getInstance().API.UpdateFavoriteStatusAsync(
                    mItem.getId(),
                    MB3Application.getInstance().API.getCurrentUserId(),
                    false,
                    new UpdateFavoriteResponse()
            );

        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MB3Application.getInstance().getIsConnected()) {
            getInitialItem();
        }
    }

    @Override
    public void onPause() {
        mMini.removeOnMiniControllerChangedListener(mCastManager);
        super.onPause();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onConnectionRestored() {
        getInitialItem();
    }

    private void getInitialItem() {
        if (isFresh) {
            if (mItem != null && MB3Application.getInstance().API.getCurrentUserId() != null) {
                MB3Application.getInstance().API.GetItemAsync(
                        mItem.getId(),
                        MB3Application.getInstance().API.getCurrentUserId(),
                        getItemResponse);
            }
            isFresh = false;
        }
    }

    private void downloadFile(String fileName, File file) {
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        mDownloadingFile = file;
        DownloadManager.Request request = buildDownloadRequest(mItem.getId(), fileName);
        manager.enqueue(request);
    }

    // TODO Attempt to create an ApiClient super class that contains this mehtod so we don't have to copy
    // getAuthorizationScheme() and getAuthorizationParameter()
    private DownloadManager.Request buildDownloadRequest(String id, String fileName) {

        String url = MB3Application.getInstance().API.getApiUrl() + "/items/" + id + "/File";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.addRequestHeader("X-MediaBrowser-Token", MB3Application.getInstance().API.getAccessToken());
        request.addRequestHeader("Authorization", getAuthorizationScheme() + " " + getAuthorizationParameter());
        request.setDescription("e-book download");
        request.setTitle(fileName);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        return request;
    }

    private void openFile(File file) {

        Intent myIntent = new Intent(Intent.ACTION_VIEW);
        String extension = mItem.getPath().substring(mItem.getPath().lastIndexOf('.') + 1);

        if (extension.equalsIgnoreCase("pdf"))
            myIntent.setDataAndType(Uri.fromFile(file), "application/pdf");
        else if (extension.equalsIgnoreCase("epub"))
            myIntent.setDataAndType(Uri.fromFile(file), "application/epub+zip");
        else if (extension.equalsIgnoreCase("cbr") || extension.equalsIgnoreCase("cbz"))
            myIntent.setDataAndType(Uri.fromFile(file), "application/x-cdisplay");
        else
            myIntent.setDataAndType(Uri.fromFile(file), "application/x-mobipocket-ebook");

        myIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        try {
            startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No Application available to view this file", Toast.LENGTH_LONG).show();
        }
    }

    public void updateFavoriteVisibleIcons() {

        Log.i("", "updateFavoriteVisibleIcons called");
        FileLogger.getFileLogger().Info("Update favorite visible icons");

        if (mItem.getUserData() != null && mItem.getUserData().getIsFavorite()) {

            FileLogger.getFileLogger().Info("Show remove favorite");
            // only show the remove favorite
            mAddFavoriteMenuItemVisible = false;
            mRemoveFavoriteMenuItemVisible = true;

        } else {
            FileLogger.getFileLogger().Info("Show add favorite");
            // only show the add favorite
            mAddFavoriteMenuItemVisible = true;
            mRemoveFavoriteMenuItemVisible = false;
        }

        this.invalidateOptionsMenu();
    }

    BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            BookDetailsActivity.this.openFile(mDownloadingFile);
            unregisterReceiver(onDownloadComplete);
        }
    };


    private class UpdateFavoriteResponse extends Response<UserItemDataDto> {

        @Override
        public void onResponse(UserItemDataDto userItemData) {
            if (userItemData == null) return;

            mItem.getUserData().setIsFavorite(userItemData.getIsFavorite());
            updateFavoriteVisibleIcons();
        }

    }


    private Response<BaseItemDto> getItemResponse = new Response<BaseItemDto>() {

        @Override
        public void onResponse(BaseItemDto item) {

            if (item != null) {

                mItem = item;
                updateFavoriteVisibleIcons();

                TextView bookTitle = (TextView) findViewById(R.id.tvBookTitle);
                TextView bookGenres = (TextView) findViewById(R.id.tvBookGenresValue);
                TextView bookSeries = (TextView) findViewById(R.id.tvBookSeriesValue);
                TextView bookAuthor = (TextView) findViewById(R.id.tvBookAuthor);
                TextView bookOverview = (TextView) findViewById(R.id.tvBookOverview);
                NetworkImageView bookCover = (NetworkImageView) findViewById(R.id.ivBookCoverLarge);
                NetworkImageView bookBackground = (NetworkImageView) findViewById(R.id.ivBookDetailsBackdrop);
                bookBackground.setLayoutParams(new RelativeLayout.LayoutParams(getScreenWidth(), (getScreenWidth() / 16) * 9));
                ImageView bookRating = (ImageView) findViewById(R.id.ivBookStarRating);

                bookCover.setDefaultImageResId(R.drawable.default_book_portrait);
                bookBackground.setDefaultImageResId(R.drawable.default_backdrop);

                bookTitle.setText(item.getName());

                String indexText = "";

                if (item.getIndexNumber() != null)
                    indexText = "Book " + ToRoman(item.getIndexNumber()) + " of ";

                if (item.getSeriesName() != null)
                    bookSeries.setText(Html.fromHtml(indexText + "<i><b>" + item.getSeriesName() + "</b></i>"), TextView.BufferType.SPANNABLE);
                else
                    bookSeries.setVisibility(TextView.GONE);

                if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getOverview())) {
                    bookOverview.setText(Html.fromHtml(item.getOverview()));
                }

                ImageOptions options = new ImageOptions();

                if (item.getHasPrimaryImage()) {
                    options.setImageType(ImageType.Primary);
                    options.setHeight((int) (250 * getScreenDensity()));
                    options.setEnableImageEnhancers(PreferenceManager
                            .getDefaultSharedPreferences(MB3Application.getInstance())
                            .getBoolean("pref_enable_image_enhancers", true));

                    String bookCoverImageUrl = MB3Application.getInstance().API.GetImageUrl(item, options);
                    bookCover.setImageUrl(bookCoverImageUrl, MB3Application.getInstance().API.getImageLoader());
                } else {
                    bookCover.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
                }

                if (item.getBackdropCount() > 0) {
                    options.setImageType(ImageType.Backdrop);
                    options.setMaxWidth(getScreenWidth());
                    options.setMaxHeight(null);

                    String bookBackgroundUrl = MB3Application.getInstance().API.GetImageUrl(item, options);
                    bookBackground.setImageUrl(bookBackgroundUrl, MB3Application.getInstance().API.getImageLoader());
                } else if (item.getParentBackdropItemId() != null) {
                    options.setImageType(ImageType.Backdrop);
                    options.setMaxWidth(getScreenWidth());
                    options.setMaxHeight(null);

                    String bookBackgroundUrl = MB3Application.getInstance().API.GetImageUrl(item.getParentId(), options);
                    bookBackground.setImageUrl(bookBackgroundUrl, MB3Application.getInstance().API.getImageLoader());
                } else {
                    bookBackground.setImageUrl(null, MB3Application.getInstance().API.getImageLoader());
                }

                if (item.getPeople() != null && item.getPeople().length > 0) {
                    for (BaseItemPerson person : item.getPeople()) {
                        if ("Author".equalsIgnoreCase(person.getType())) {
                            bookAuthor.setText(person.getName());
                            break;
                        }
                    }
                }

                if (item.getTags() != null && item.getTags().size() > 0) {

                    String tagsTemp = "";

                    if (item.getTags() != null) {
                        for (String tag : item.getTags()) {

                            if (!tagsTemp.isEmpty()) {
                                tagsTemp += "<font color='Aqua'> &#149 </font>";
                            }

                            tagsTemp += tag;
                        }

                        bookGenres.setText(Html.fromHtml(tagsTemp), TextView.BufferType.SPANNABLE);
                        bookGenres.setTextSize((float) 14);
                    }
                }

                bookRating.setVisibility(ImageView.VISIBLE);
                Utils.ShowStarRating(item.getCommunityRating(), bookRating);
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    private String ToRoman(int number) {
        if ((number < 0) || (number > 3999))
            throw new IllegalArgumentException("insert value between 1 and 3999");
        if (number < 1) return "";
        if (number >= 1000) return "M" + ToRoman(number - 1000);
        if (number >= 900) return "CM" + ToRoman(number - 900);
        if (number >= 500) return "D" + ToRoman(number - 500);
        if (number >= 400) return "CD" + ToRoman(number - 400);
        if (number >= 100) return "C" + ToRoman(number - 100);
        if (number >= 90) return "XC" + ToRoman(number - 90);
        if (number >= 50) return "L" + ToRoman(number - 50);
        if (number >= 40) return "XL" + ToRoman(number - 40);
        if (number >= 10) return "X" + ToRoman(number - 10);
        if (number >= 9) return "IX" + ToRoman(number - 9);
        if (number >= 5) return "V" + ToRoman(number - 5);
        if (number >= 4) return "IV" + ToRoman(number - 4);
        if (number >= 1) return "I" + ToRoman(number - 1);

        throw new IllegalArgumentException("something bad happened");
    }

    protected final String getAuthorizationScheme()
    {
        return "MediaBrowser";
    }

    /**
     Gets the authorization header parameter.

     <value>The authorization header parameter.</value>
     */
    protected final String getAuthorizationParameter()
    {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(MB3Application.getInstance().API.getClientName())
                && tangible.DotNetToJavaStringHelper.isNullOrEmpty(MB3Application.getInstance().API.getDeviceId())
                && tangible.DotNetToJavaStringHelper.isNullOrEmpty(MB3Application.getInstance().API.getDeviceName()))
        {
            return "";
        }

        String header = String.format("Client=\"%1$s\", DeviceId=\"%2$s\", Device=\"%3$s\", Version=\"%4$s\"",
                MB3Application.getInstance().API.getClientName(),
                MB3Application.getInstance().API.getDeviceId(),
                MB3Application.getInstance().API.getDeviceName(),
                MB3Application.getInstance().API.getApplicationVersion()
        );

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(MB3Application.getInstance().API.getCurrentUserId()))
        {
            header += String.format(", UserId=\"%1$s\"", MB3Application.getInstance().API.getCurrentUserId());
        }

        return header;
    }

}
