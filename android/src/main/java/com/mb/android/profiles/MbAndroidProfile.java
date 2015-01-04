package com.mb.android.profiles;

import android.os.Build;

import mediabrowser.apiinteraction.android.profiles.Api16Builder;
import mediabrowser.apiinteraction.android.profiles.Api21Builder;
import mediabrowser.model.dlna.CodecProfile;
import mediabrowser.model.dlna.CodecType;
import mediabrowser.model.dlna.DirectPlayProfile;
import mediabrowser.model.dlna.DlnaProfileType;
import mediabrowser.model.dlna.EncodingContext;
import mediabrowser.model.dlna.ProfileCondition;
import mediabrowser.model.dlna.ProfileConditionType;
import mediabrowser.model.dlna.ProfileConditionValue;
import mediabrowser.model.dlna.profiles.DefaultProfile;
import mediabrowser.model.dlna.SubtitleDeliveryMethod;
import mediabrowser.model.dlna.SubtitleProfile;
import mediabrowser.model.dlna.TranscodingProfile;


public class MbAndroidProfile extends DefaultProfile
{
    public MbAndroidProfile()
    {
        this(true, true);
    }

    public MbAndroidProfile(boolean supportsHls, boolean supportsMpegDash)
    {
        setName("Android");

        // Adds a lot of weight and not needed in this context
        setProtocolInfo(null);

        boolean enableSubs = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            enableSubs = true;
        }

        buildTranscodeProfiles(supportsHls, supportsMpegDash);
        buildDirectPlayProfiles();
        buildCodecProfiles();
        buildDynamicProfiles();
        buildSubtitleProfiles(enableSubs);
    }

    private void buildTranscodeProfiles(boolean supportsHls, boolean supportsMpegDash) {

        java.util.ArrayList<TranscodingProfile> transcodingProfiles = new java.util.ArrayList<TranscodingProfile>();

        TranscodingProfile mp3Transcode = new TranscodingProfile();
        mp3Transcode.setContainer("mp3");
        mp3Transcode.setAudioCodec("mp3");
        mp3Transcode.setType(DlnaProfileType.Audio);
        transcodingProfiles.add(mp3Transcode);

        if (supportsMpegDash)
        {

        }
        if (supportsHls)
        {
            TranscodingProfile hlsH264Transcode = new TranscodingProfile();
            hlsH264Transcode.setProtocol("hls");
            hlsH264Transcode.setContainer("ts");
            hlsH264Transcode.setVideoCodec("h264");
            hlsH264Transcode.setAudioCodec("aac");
            hlsH264Transcode.setType(DlnaProfileType.Video);
            hlsH264Transcode.setContext(EncodingContext.Streaming);
            transcodingProfiles.add(hlsH264Transcode);
        }
        TranscodingProfile h264Transcode = new TranscodingProfile();
        h264Transcode.setContainer("mp4");
        h264Transcode.setVideoCodec("h264");
        h264Transcode.setAudioCodec("aac");
        h264Transcode.setType(DlnaProfileType.Video);
        h264Transcode.setContext(EncodingContext.Static);
        transcodingProfiles.add(h264Transcode);

        TranscodingProfile mkvTranscode = new TranscodingProfile();
        mkvTranscode.setContainer("webm");
        mkvTranscode.setVideoCodec("vpx");
        mkvTranscode.setAudioCodec("vorbis");
        mkvTranscode.setType(DlnaProfileType.Video);
        mkvTranscode.setContext(EncodingContext.Streaming);
        transcodingProfiles.add(mkvTranscode);

        setTranscodingProfiles(transcodingProfiles.toArray(new TranscodingProfile[0]));
    }

    private void buildDirectPlayProfiles() {

        DirectPlayProfile mp4DirectPlay = new DirectPlayProfile();
        mp4DirectPlay.setContainer("mp4");
        mp4DirectPlay.setVideoCodec("h264,mpeg4");
        mp4DirectPlay.setAudioCodec("aac");
        mp4DirectPlay.setType(DlnaProfileType.Video);

        DirectPlayProfile aacDirectPlay = new DirectPlayProfile();
        aacDirectPlay.setContainer("mp4,aac");
        aacDirectPlay.setAudioCodec("aac");
        aacDirectPlay.setType(DlnaProfileType.Audio);

        DirectPlayProfile mp3DirectPlay = new DirectPlayProfile();
        mp3DirectPlay.setContainer("mp3");
        mp3DirectPlay.setAudioCodec("mp3");
        mp3DirectPlay.setType(DlnaProfileType.Audio);

        DirectPlayProfile flacDirectPlay = new DirectPlayProfile();
        flacDirectPlay.setContainer("flac");
        flacDirectPlay.setAudioCodec("flac");
        flacDirectPlay.setType(DlnaProfileType.Audio);

        DirectPlayProfile oggDirectPlay = new DirectPlayProfile();
        oggDirectPlay.setContainer("ogg");
        oggDirectPlay.setAudioCodec("vorbis");
        oggDirectPlay.setType(DlnaProfileType.Audio);

        DirectPlayProfile imagesDirectPlay = new DirectPlayProfile();
        imagesDirectPlay.setContainer("jpeg,png,gif,bmp");
        imagesDirectPlay.setType(DlnaProfileType.Photo);

        setDirectPlayProfiles(new DirectPlayProfile[] {mp4DirectPlay, aacDirectPlay, mp3DirectPlay, flacDirectPlay, oggDirectPlay, imagesDirectPlay});
    }

    private void buildCodecProfiles() {

        CodecProfile h264VideoProfile = new CodecProfile();
        h264VideoProfile.setType(CodecType.Video);
        h264VideoProfile.setCodec("h264");
        h264VideoProfile.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.EqualsAny, ProfileConditionValue.VideoProfile, "baseline|constrained baseline"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.Width, "1920"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.Height, "1080"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoBitDepth, "8"),
                        new ProfileCondition(ProfileConditionType.NotEquals, ProfileConditionValue.IsAnamorphic, "true"),
                        new ProfileCondition(ProfileConditionType.Equals, ProfileConditionValue.IsCabac, "true")
                });

        CodecProfile videoProfile = new CodecProfile();
        videoProfile.setType(CodecType.Video);
        videoProfile.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.Width, "1920"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.Height, "1080"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoBitDepth, "8"),
                        new ProfileCondition(ProfileConditionType.NotEquals, ProfileConditionValue.IsAnamorphic, "true")
                });

        CodecProfile aacVideoProfile = new CodecProfile();
        aacVideoProfile.setType(CodecType.VideoAudio);
        aacVideoProfile.setCodec("aac");
        aacVideoProfile.setConditions(new ProfileCondition[] {new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "2")});

        CodecProfile aacAudioProfile = new CodecProfile();
        aacAudioProfile.setType(CodecType.Audio);
        aacAudioProfile.setCodec("aac");
        aacAudioProfile.setConditions(new ProfileCondition[] {new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "2")});

        CodecProfile mp3AudioProfile = new CodecProfile();
        mp3AudioProfile.setType(CodecType.Audio);
        mp3AudioProfile.setCodec("mp3");
        mp3AudioProfile.setConditions(new ProfileCondition[]
                {
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "2"),
                        new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioBitrate, "320000")
                });

        CodecProfile vorbisVideoProfile = new CodecProfile();
        vorbisVideoProfile.setType(CodecType.VideoAudio);
        vorbisVideoProfile.setCodec("vorbis");
        vorbisVideoProfile.setConditions(new ProfileCondition[] {new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "2")});

        CodecProfile vorbisAudioProfile = new CodecProfile();
        vorbisAudioProfile.setType(CodecType.Audio);
        vorbisAudioProfile.setCodec("vorbis");
        vorbisAudioProfile.setConditions(new ProfileCondition[] {new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "2")});

        setCodecProfiles(new CodecProfile[] {h264VideoProfile, videoProfile, aacVideoProfile, aacAudioProfile, mp3AudioProfile, vorbisVideoProfile, vorbisAudioProfile});
    }

    private void buildDynamicProfiles(){

        if (Build.VERSION.SDK_INT >= 21){
            new Api21Builder().buildProfiles(this);
        }
        else if (Build.VERSION.SDK_INT >= 16){
            new Api16Builder().buildProfiles(this);
        }
    }

    private void buildSubtitleProfiles(boolean enableSubs) {

        if (!enableSubs) return;

        SubtitleProfile srtSubs = new SubtitleProfile();
        srtSubs.setFormat("srt");
        srtSubs.setMethod(SubtitleDeliveryMethod.External);

        setSubtitleProfiles(new SubtitleProfile[] { srtSubs });
    }

//	public MbAndroidProfile(boolean supportsHls, boolean supportsMpegDash, String[] supportedH264Profiles)
//	{
//        buildTranscodingProfiles(supportsHls, supportsMpegDash);
//        buildDirectPlayProfiles();
//        buildCodecProfiles(supportedH264Profiles);
//        buildSubtitleProfiles(enableSubs);
//	}
//
//    private void buildTranscodingProfiles(boolean enableHls, boolean enableDash) {
//
//        List<TranscodingProfile> tProfiles = new ArrayList<>();
//
//        if (enableHls /* && Build.VERSION.SDK_INT != Build.VERSION_CODES.KITKAT */ ) {
//            TranscodingProfile hlsProfile = new TranscodingProfile();
//            hlsProfile.setProtocol("hls");
//            hlsProfile.setContainer("ts");
//            hlsProfile.setVideoCodec("h264");
//            hlsProfile.setAudioCodec("aac");
//            hlsProfile.setType(DlnaProfileType.Video);
//            hlsProfile.setContext(EncodingContext.Streaming);
//            tProfiles.add(hlsProfile);
//        }
//
//        if (enableDash) {
//
//        }
//
//        TranscodingProfile mkvProfile = new TranscodingProfile();
//        mkvProfile.setContainer("webm");
//        mkvProfile.setVideoCodec("vpx");
//        mkvProfile.setAudioCodec("vorbis");
//        mkvProfile.setType(DlnaProfileType.Video);
//        mkvProfile.setContext(EncodingContext.Streaming);
//        tProfiles.add(mkvProfile);
//
//        TranscodingProfile mp3Profile = new TranscodingProfile();
//        mp3Profile.setContainer("mp3");
//        mp3Profile.setAudioCodec("mp3");
//        mp3Profile.setType(DlnaProfileType.Audio);
//        mp3Profile.setContext(EncodingContext.Streaming);
//        tProfiles.add(mp3Profile);
//
//        setTranscodingProfiles(tProfiles.toArray(new TranscodingProfile[tProfiles.size()]));
//    }
//
//    private void buildDirectPlayProfiles() {
//
//        DirectPlayProfile mp4Profile = new DirectPlayProfile();
//        mp4Profile.setContainer("mp4,ts,mkv");
//        mp4Profile.setVideoCodec("h264");
//        mp4Profile.setAudioCodec("aac,ac3");
//        mp4Profile.setType(DlnaProfileType.Video);
//
//        DirectPlayProfile mkvProfile = new DirectPlayProfile();
//        mkvProfile.setContainer("mkv");
//        mkvProfile.setVideoCodec("vpx");
//        mkvProfile.setAudioCodec("vorbis");
//        mkvProfile.setType(DlnaProfileType.Video);
//
//        DirectPlayProfile threegpProfile = new DirectPlayProfile();
//        threegpProfile.setContainer("3gp");
//        threegpProfile.setVideoCodec("mpeg4,h264");
//        threegpProfile.setAudioCodec("aac,amr");
//        threegpProfile.setType(DlnaProfileType.Video);
//
//        DirectPlayProfile mp3Profile = new DirectPlayProfile();
//        mp3Profile.setContainer("mp3");
//        mp3Profile.setAudioCodec("mp3");
//        mp3Profile.setType(DlnaProfileType.Audio);
//
//        DirectPlayProfile vorbisProfile = new DirectPlayProfile();
//        vorbisProfile.setContainer("ogg");
//        vorbisProfile.setAudioCodec("vorbis");
//        vorbisProfile.setType(DlnaProfileType.Audio);
//
//        DirectPlayProfile flacProfile = new DirectPlayProfile();
//        flacProfile.setContainer("flac");
//        flacProfile.setAudioCodec("flac");
//        flacProfile.setType(DlnaProfileType.Audio);
//
//        DirectPlayProfile jpegProfile = new DirectPlayProfile();
//        jpegProfile.setContainer("jpg,jpeg");
//        jpegProfile.setType(DlnaProfileType.Photo);
//
//        DirectPlayProfile gifProfile = new DirectPlayProfile();
//        gifProfile.setContainer("gif");
//        gifProfile.setType(DlnaProfileType.Photo);
//
//        DirectPlayProfile pngProfile = new DirectPlayProfile();
//        pngProfile.setContainer("png");
//        pngProfile.setType(DlnaProfileType.Photo);
//
//        DirectPlayProfile webpProfile = new DirectPlayProfile();
//        webpProfile.setContainer("webp");
//        webpProfile.setType(DlnaProfileType.Photo);
//
//        setDirectPlayProfiles(new DirectPlayProfile[] {
//                mp4Profile,
//                mkvProfile,
//                threegpProfile,
//                mp3Profile,
//                vorbisProfile,
//                flacProfile,
//                jpegProfile,
//                gifProfile,
//                pngProfile,
//                webpProfile
//        });
//    }
//
//    private void buildCodecProfiles(String[] supportedH264Profiles) {
//
//        /*
//        H264
//         */
//        CodecProfile h264Profile = new CodecProfile();
//        h264Profile.setType(CodecType.Video);
//        h264Profile.setCodec("h264");
//        h264Profile.setConditions(new ProfileCondition[] {
//                new ProfileCondition(ProfileConditionType.EqualsAny, ProfileConditionValue.VideoProfile, tangible.DotNetToJavaStringHelper.join("|", supportedH264Profiles)),
//                new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.Width, "1920"),
//                new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.Height, "1080"),
//                new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.VideoBitDepth, "8"),
//                new ProfileCondition(ProfileConditionType.NotEquals, ProfileConditionValue.IsAnamorphic, "true")
//        });
//
//        /*
//        AAC
//         */
//        CodecProfile aacProfile = new CodecProfile();
//        aacProfile.setType(CodecType.VideoAudio);
//        aacProfile.setCodec("aac");
//        aacProfile.setConditions(new ProfileCondition[] {
//                new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "2")
//        });
//
//        CodecProfile aacProfile2 = new CodecProfile();
//        aacProfile2.setType(CodecType.Audio);
//        aacProfile2.setCodec("aac");
//        aacProfile2.setConditions(new ProfileCondition[] {
//                new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "2")
//        });
//
//        /*
//        FLAC
//         */
//        CodecProfile flacProfile = new CodecProfile();
//        flacProfile.setType(CodecType.Audio);
//        flacProfile.setCodec("flac");
//        flacProfile.setConditions(new ProfileCondition[] {
//                new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "2")
//        });
//
//        /*
//        MP3
//         */
//        CodecProfile mp3Profile = new CodecProfile();
//        mp3Profile.setType(CodecType.Audio);
//        mp3Profile.setCodec("mp3");
//        mp3Profile.setConditions(new ProfileCondition[] {
//                new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "2")
//        });
//
//        /*
//        Vorbis
//         */
//        CodecProfile vorbisProfile = new CodecProfile();
//        vorbisProfile.setType(CodecType.VideoAudio);
//        vorbisProfile.setCodec("vorbis");
//        vorbisProfile.setConditions(new ProfileCondition[] {
//                new ProfileCondition(ProfileConditionType.LessThanEqual, ProfileConditionValue.AudioChannels, "2")
//        });
//
//        setCodecProfiles(new CodecProfile[] { h264Profile, aacProfile, aacProfile2, flacProfile, mp3Profile, vorbisProfile });
//    }


}