package com.mb.android.tests;

import android.test.InstrumentationTestCase;

import com.mb.android.profiles.MbAndroidProfile;
import mediabrowser.model.dlna.EncodingContext;
import mediabrowser.model.dlna.StreamBuilder;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dlna.VideoOptions;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.dto.MediaSourceType;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;
import mediabrowser.model.entities.VideoType;
import mediabrowser.model.mediainfo.MediaProtocol;

import java.util.ArrayList;


public class StreamBuilderTest extends InstrumentationTestCase {

    public void testStreamBuilder() {

        MbAndroidProfile profile = new MbAndroidProfile(true, false);

        StreamBuilder builder = new StreamBuilder();

        MediaStream mediaStream1 = new MediaStream();
        mediaStream1.setCodec("H264");
        mediaStream1.setType(MediaStreamType.Video);
        mediaStream1.setProfile("HIGH");
        mediaStream1.setIsCabac(true);

        MediaStream mediaStream2 = new MediaStream();
        mediaStream2.setCodec("AC3");

        ArrayList<MediaStream> streams = new ArrayList<>();
        streams.add(mediaStream1);
        streams.add(mediaStream2);

        MediaSourceInfo sourceInfo = new MediaSourceInfo();
        sourceInfo.setBitrate(6200000);
        sourceInfo.setContainer("mkv");
        sourceInfo.setPath("\\server\\test.mkv");
        sourceInfo.setProtocol(MediaProtocol.File);
        sourceInfo.setRunTimeTicks(36000000000L);
        sourceInfo.setVideoType(VideoType.VideoFile);
        sourceInfo.setType(MediaSourceType.Default);
        sourceInfo.setMediaStreams(streams);

        ArrayList<MediaSourceInfo> mediaSources = new ArrayList<>();
        mediaSources.add(sourceInfo);

        VideoOptions options = new VideoOptions();
        options.setContext(EncodingContext.Streaming);
        options.setDeviceId("24d5239e8c375ee0e3e4434d660d0b64");
        options.setItemId("1b11682a6069315436bb02d9cc91e715");
        options.setProfile(profile);
        options.setMediaSources(mediaSources);

        StreamInfo streamInfo = builder.BuildVideoItem(options);

        String url = streamInfo.ToDlnaUrl("http://localhost:8096");


        // now for the actual tests
        boolean containsHighProfile = url.contains(";high;");
        boolean containsBaseline = url.contains(";baseline;");

        assertTrue(containsHighProfile);
        assertFalse(containsBaseline);

        boolean isHls = url.contains("master.m3u8?");

        assertTrue(isHls);
    }
}
