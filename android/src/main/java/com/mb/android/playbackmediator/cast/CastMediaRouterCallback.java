/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mb.android.playbackmediator.cast;

import android.content.Context;
import android.support.v7.media.MediaRouter;

import com.google.android.gms.cast.CastDevice;
import com.mb.android.MB3Application;
import com.mb.android.mediaroute.MediaBrowserRouteProvider;
import com.mb.android.playbackmediator.utils.LogUtils;
import com.mb.android.playbackmediator.utils.Utils;

import static com.mb.android.playbackmediator.utils.LogUtils.LOGD;

/**
 * Provides a handy implementation of {@link android.support.v7.media.MediaRouter.Callback}. When a {@link android.support.v7.media.MediaRouter.RouteInfo} is
 * selected by user from the list of available routes, this class will call the
 * {@link DeviceSelectionListener#setDevice(com.google.android.gms.cast.CastDevice))} of the listener that was passed to it in
 * the constructor. In addition, as soon as a non-default route is discovered, the
 * {@link DeviceSelectionListener#onCastDeviceDetected(com.google.android.gms.cast.CastDevice))} is called.
 * <p>
 * There is also logic in this class to help with the process of previous session recovery.
 */
public class CastMediaRouterCallback extends MediaRouter.Callback {
    private static final String TAG = LogUtils.makeLogTag(CastMediaRouterCallback.class);
    private final DeviceSelectionListener selectDeviceInterface;
    private final Context mContext;
    private int mRouteCount = 0;

    public CastMediaRouterCallback(DeviceSelectionListener callback, Context context) {
        this.selectDeviceInterface = callback;
        this.mContext = context;
    }

    @Override
    public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
        LOGD(TAG, "onRouteSelected: info=" + info);
        if (BaseCastManager.getCastManager().getReconnectionStatus()
                == BaseCastManager.ReconnectionStatus.FINALIZE) {
            BaseCastManager.getCastManager().setReconnectionStatus(BaseCastManager.ReconnectionStatus.INACTIVE);
            BaseCastManager.getCastManager().cancelReconnectionTask();
            return;
        }
        Utils.saveStringToPreference(mContext, BaseCastManager.PREFS_KEY_ROUTE_ID, info.getId());
        CastDevice device = CastDevice.getFromBundle(info.getExtras());
        BaseCastManager.getCastManager().setRouteInfo(info);
        if (null != device) {
            selectDeviceInterface.onDeviceSelected(device);
            LOGD(TAG, "onResult: mSelectedDevice=" + device.getFriendlyName());
        } else {
            BaseCastManager.getCastManager().setDevice(info);
            LOGD(TAG, "onResult: mSelectedDevice=" + info.getName());
        }
        MB3Application.getCastManager(MB3Application.getInstance()).startPulseCheck();
    }

    @Override
    public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
        LOGD(TAG, "onRouteUnselected: route=" + route);
        selectDeviceInterface.onDeviceSelected(null);
        MB3Application.getCastManager(MB3Application.getInstance()).terminateMb3Session();
    }

    @Override
    public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
        super.onRouteAdded(router, route);
        LOGD(TAG, "Route added: " + route.getName());
        if (!router.getDefaultRoute().equals(route)) {
            if (++mRouteCount == 1) {
                BaseCastManager.getCastManager().onCastAvailabilityChanged(true);
            }
            selectDeviceInterface.onCastDeviceDetected(route);
        }
        if (BaseCastManager.getCastManager().getReconnectionStatus()
                == BaseCastManager.ReconnectionStatus.STARTED) {
            String routeId = Utils.getStringFromPreference(mContext,
                    BaseCastManager.PREFS_KEY_ROUTE_ID);
            if (route.getId().equals(routeId)) {
                // we found the route, so lets go with that
                LOGD(TAG, "onRouteAdded: Attempting to recover a session with info=" + route);
                BaseCastManager.getCastManager().setReconnectionStatus(
                        BaseCastManager.ReconnectionStatus.IN_PROGRESS);

                CastDevice device = CastDevice.getFromBundle(route.getExtras());
                LOGD(TAG, "onRouteAdded: Attempting to recover a session with device: "
                        + device.getFriendlyName());
                selectDeviceInterface.onDeviceSelected(device);
            }
        }
    }

    @Override
    public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
        super.onRouteRemoved(router, route);
        LOGD(TAG, "onRouteRemoved: " + route);
        if (--mRouteCount == 0) {
            BaseCastManager.getCastManager().onCastAvailabilityChanged(false);
        }
    }

}
