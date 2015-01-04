package com.mb.android.ui.mobile.musicartist;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MB3Application;
import com.mb.android.R;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment that shows an artist or bands biography
 */
public class BioFragment extends Fragment {

    private ArtistActivity mArtistActivity;
    private TextView artistBio;
    private NetworkImageView artistLogoImage;
    private NetworkImageView backdrop;
    private String artistId;

    /**
     * Class Constructor
     */
    public BioFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_artist_bio, container, false);
        artistBio = (TextView) view.findViewById(R.id.tvArtistBio);
        artistLogoImage = (NetworkImageView) view.findViewById(R.id.ivArtistLogo);
        backdrop = (NetworkImageView) view.findViewById(R.id.ivArtistViewBackdrop);

        Bundle args = getArguments();

        artistId = args.getString("ArtistId");

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(artistId)) return;

        MB3Application.getInstance().API.GetItemAsync(
                artistId,
                MB3Application.getInstance().API.getCurrentUserId(),
                getItemResponse);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity != null) {
            try {
                mArtistActivity = (ArtistActivity) activity;
            } catch (ClassCastException e) {
                Log.d("ServerSelectionFragment", "onAttach: Exception casting activity");
            }
        }
    }

    private Response<BaseItemDto> getItemResponse = new Response<BaseItemDto>() {

        @Override
        public void onResponse(BaseItemDto item) {

            if (item != null) {
                // Set logo
                if (item.getHasLogo()) {
                    ImageOptions options = new ImageOptions();
                    options.setImageType(ImageType.Logo);
                    options.setMaxWidth(400);
                    options.setMaxHeight(250);

                    String imageUrl = MB3Application.getInstance().API.GetImageUrl(item, options);
                    artistLogoImage.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
                }

                // Set the backdrop
                if (item.getBackdropCount() > 0) {

                    DisplayMetrics metrics = new DisplayMetrics();
                    mArtistActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

                    if (backdrop != null) {
                        backdrop.setLayoutParams(new RelativeLayout.LayoutParams(metrics.widthPixels, (metrics.widthPixels / 16) * 9));
                        ImageOptions options = new ImageOptions();
                        options.setImageType(ImageType.Backdrop);
                        options.setMaxWidth(metrics.widthPixels);

                        String imageUrl = MB3Application.getInstance().API.GetImageUrl(item, options);
                        backdrop.setImageUrl(imageUrl, MB3Application.getInstance().API.getImageLoader());
                    }
                }

                // Set artist bio
                if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(item.getOverview())) {
                    artistBio.setText(Html.fromHtml(item.getOverview()));
                    artistBio.setMovementMethod(new ScrollingMovementMethod());
                }

            } else {
                Log.i("GetItemCallback", "item is null");
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };
}
