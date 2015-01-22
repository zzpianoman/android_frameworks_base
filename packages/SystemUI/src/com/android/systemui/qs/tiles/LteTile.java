/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.Phone;

import android.os.SystemProperties;

import com.android.systemui.qs.QSTile;
import com.android.systemui.R;

/**
 * Lazy Lte Tile
 * Created by Adnan on 1/21/15.
 *
 * Adapted to M and removed dependencies on
 * framework modifications by Justin M.
 * 2/14/2016.
 */
public class LteTile extends QSTile<QSTile.BooleanState> {

    TelephonyManager tm = (TelephonyManager)
                mContext.getSystemService(Context.TELEPHONY_SERVICE);

    public LteTile(Host host) {
        super(host);
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleLongClick() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.phone",
             "com.android.phone.MobileNetworkSettings");
        mHost.startActivityDismissingKeyguard(intent);
    }

    @Override
    protected void handleClick() {
        toggleLTE();
        refreshState();
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        // Hide the tile if device doesn't support LTE
        if (!deviceSupportsLte()) {
            state.visible = false;
            return;
        }
        state.label = mContext.getString(R.string.quick_settings_lte_tile_title);

        switch (getCurrentPreferredNetworkMode()) {
            case Phone.NT_MODE_GLOBAL:
            case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
            case Phone.NT_MODE_LTE_GSM_WCDMA:
            case Phone.NT_MODE_LTE_ONLY:
            case Phone.NT_MODE_LTE_WCDMA:
            case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                state.visible = true;
                state.icon = ResourceIcon.get(R.drawable.ic_qs_lte_on);
                break;
            default:
                state.visible = true;
                state.icon = ResourceIcon.get(R.drawable.ic_qs_lte_off);
                break;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.DONT_TRACK_ME_BRO;
    }

    private void toggleLTE() {
        int network = getCurrentPreferredNetworkMode();

        switch (network) {
        // GSM Devices
        case Phone.NT_MODE_WCDMA_PREF:
        case Phone.NT_MODE_GSM_UMTS:
            network = Phone.NT_MODE_LTE_GSM_WCDMA;
            break;
        case Phone.NT_MODE_LTE_GSM_WCDMA:
            network = Phone.NT_MODE_WCDMA_PREF;
            break;
        // GSM and CDMA devices
        case Phone.NT_MODE_GLOBAL:
            // Wtf to do here?
            network = Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
            break;
        // CDMA Devices
        case Phone.NT_MODE_CDMA:
            network = Phone.NT_MODE_LTE_CDMA_AND_EVDO;
            break;
        case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
            network = Phone.NT_MODE_CDMA;
            break;
        }
        int subId = SubscriptionManager.getDefaultSubId();
        tm.setPreferredNetworkType(subId, network);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.PREFERRED_NETWORK_MODE, network);
    }

    private int getCurrentPreferredNetworkMode() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE, -1);
    }

    private boolean deviceSupportsLte() {
        return SystemProperties.getBoolean("telephony.lteOnCdmaDevice", false) ||
               SystemProperties.getBoolean("telephony.lteOnGsmDevice", false);
    }

    private boolean isCdmaDevice() {
        return SystemProperties.getBoolean("telephony.lteOnCdmaDevice", false);
    }

    @Override
    public void setListening(boolean listening) {

    }
}
