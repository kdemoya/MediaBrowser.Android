package com.mb.android.utils;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;

import com.mb.android.logging.FileLogger;
import mediabrowser.model.extensions.StringHelper;

/**
 * Created by Mark on 2014-11-25.
 */
public class CodecUtils {

    public static void logCodecInfo() {

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            FileLogger.getFileLogger().Info("Lollipop Specific Info");
            MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            for (MediaCodecInfo codecInfo : codecList.getCodecInfos()) {
                if (codecInfo.isEncoder()) continue;

                FileLogger.getFileLogger().Info("\rName: " + codecInfo.getName());
                FileLogger.getFileLogger().Info("Supported Types: " + tangible.DotNetToJavaStringHelper.join(",", codecInfo.getSupportedTypes()));
                for (String type : codecInfo.getSupportedTypes()) {
                    MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(type);
                    if (capabilities != null) {
                        if (capabilities.profileLevels != null) {
                            for (MediaCodecInfo.CodecProfileLevel profileLevel : capabilities.profileLevels) {
                                FileLogger.getFileLogger().Info("Profile: " + profileOrLevelToString(profileLevel.profile));
                                FileLogger.getFileLogger().Info("Level: " + profileOrLevelToString(profileLevel.level));
                            }
                        }
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

        }
    }

    private static String profileOrLevelToString(int profileOrLevel) {

        String stringValue = String.valueOf(profileOrLevel);

//        switch (profileOrLevel) {
//            case 39:
//                stringValue = "AACObjectELD";
//                break;
//            case 17:
//                stringValue = "AACObjectERLC";
//                break;
//            case 5:
//                stringValue = "AACObjectHE";
//                break;
//            case 29:
//                stringValue = "AACObjectHE_PS";
//                break;
//            case 2:
//                stringValue = "AACObjectLC | AVClevel1b | AVCProfileMain | H263Level20 | H263ProfileH320Encoding";
//                break;
//            case 23:
//                stringValue = "AACObjectLD";
//                break;
//            case 4:
//                stringValue = "AACObjectLTP | AVCLevel11 | AVCProfileExtended | H263Level30 | H263ProfileBackwardCompatible";
//                break;
//            case 1:
//                stringValue = "AACObjectMain | AVCLevel1 | AVCProfileBaseline | H263Level10 | H263ProfileBaseline";
//                break;
//            case 3:
//                stringValue = "AACObjectSSR";
//                break;
//            case 6:
//                stringValue = "AACObjectScalable";
//                break;
//            case 8:
//                stringValue = "AVCLevel12 | AVCLevel12 | AVCProfileHigh | H263Level40 | H263ProfileISWV2";
//                break;
//            case 16:
//                stringValue = "AVCLevel13 | AVCProfileHigh10 | H263Level45 | H263ProfileISWV3";
//                break;
//            case 32:
//                stringValue = "AVCLevel2 | AVCProfileHigh422 | H263Level50 | H263ProfileHighCompression";
//                break;
//            case 64:
//                stringValue = "AVCLevel21 | AVCProfileHigh444 | H263Level60 | H263ProfileInternet";
//                break;
//            case 128:
//                stringValue = "AVCLevel22 | H263Level70 | H263ProfileInterlace";
//                break;
//            case 256:
//                stringValue = "AVCLevel3 | H263ProfileHighLatency";
//                break;
//            case 512:
//                stringValue = "AVCLevel31";
//                break;
//            case 1024:
//                stringValue = "AVCLevel32";
//                break;
//            case 2048:
//                stringValue = "AVCLevel4";
//                break;
//            case 4096:
//                stringValue = "AVCLevel41";
//                break;
//            case 8192:
//                stringValue = "AVCLevel42";
//                break;
//            case 16384:
//                stringValue = "AVCLevel5";
//                break;
//            case 32768:
//                stringValue = "AVCLevel51";
//                break;
//            case 65536:
//                stringValue = "AVCLevel52";
//                break;
//        }


        return stringValue;
    }
}
