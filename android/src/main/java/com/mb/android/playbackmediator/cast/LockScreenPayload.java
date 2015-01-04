package com.mb.android.playbackmediator.cast;

import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.BaseItemInfo;
import mediabrowser.model.extensions.StringHelper;

/**
 * Created by Mark on 2014-10-03.
 */
public class LockScreenPayload {

    public String backdropImageItemId;
    public String primaryImageItemId;
    public String title;
    public String secondaryText;
    public Long runtimeTicks;

    public LockScreenPayload(BaseItemDto baseItemDto) {
        runtimeTicks = 0L;
        secondaryText = "";

        if (null == baseItemDto) return;

        title = baseItemDto.getName();

        if (baseItemDto.getArtists() != null && baseItemDto.getArtists().size() > 0) {
            secondaryText += baseItemDto.getArtists().get(0);
        }
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(baseItemDto.getAlbum())) {
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(secondaryText))
                secondaryText += " / ";
            secondaryText += baseItemDto.getAlbum();
        }
        if (baseItemDto.getHasPrimaryImage()) {
            primaryImageItemId = baseItemDto.getId();
        } else if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(baseItemDto.getAlbumId()) && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(baseItemDto.getAlbumPrimaryImageTag())) {
            primaryImageItemId = baseItemDto.getAlbumId();
        }
        if (baseItemDto.getBackdropCount() > 0) {
            backdropImageItemId = baseItemDto.getId();
        } else if (baseItemDto.getParentBackdropImageTags() != null && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(baseItemDto.getParentBackdropItemId())) {
            backdropImageItemId = baseItemDto.getParentBackdropItemId();
        }

        if (baseItemDto.getRunTimeTicks() != null) {
            runtimeTicks = baseItemDto.getRunTimeTicks();
        }
    }

    public LockScreenPayload(BaseItemInfo baseItemInfo) {
        runtimeTicks = 0L;
        secondaryText = "";

        if (null == baseItemInfo) return;

        title = baseItemInfo.getName();

        if (baseItemInfo.getArtists() != null && baseItemInfo.getArtists().size() > 0) {
            secondaryText += baseItemInfo.getArtists().get(0);
        }
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(baseItemInfo.getAlbum())) {
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(secondaryText))
                secondaryText += " / ";
            secondaryText += baseItemInfo.getAlbum();
        }
        if (baseItemInfo.getHasPrimaryImage()) {
            primaryImageItemId = baseItemInfo.getId();
        } else if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(baseItemInfo.getPrimaryImageItemId())) {
            primaryImageItemId = baseItemInfo.getPrimaryImageItemId();
        }
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(baseItemInfo.getBackdropImageTag())) {
            backdropImageItemId = baseItemInfo.getId();
        } else if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(baseItemInfo.getBackdropItemId())) {
            backdropImageItemId = baseItemInfo.getBackdropItemId();
        }

        if (baseItemInfo.getRunTimeTicks() != null) {
            runtimeTicks = baseItemInfo.getRunTimeTicks();
        }
    }
}
