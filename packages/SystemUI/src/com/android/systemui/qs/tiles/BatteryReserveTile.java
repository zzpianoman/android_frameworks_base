/*
 * Copyright (C) 2015 The CyanogenMod Project
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
import android.database.ContentObserver;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;

public class BatteryReserveTile extends QSTile<QSTile.BooleanState> {

    private static final String BATTERY_RESERVE_FILE = "/sys/battery_reserve/enabled";
    
    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick() {
        setBatteryReserveEnabled(!getBatteryReserveEnabled());
        refreshState();
    }

    @Override
    protected void handleLongClick() {
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        File file = new File(BATTERY_RESERVE_FILE);
        if (!file.exists()) {
            state.visible = false;
            return;
        }
        state.label = mContext.getString(R.string.quick_settings_battery_reserve_label);
        if (getBatteryReserveEnabled()) {
            state.visible = true;
            state.icon = ResourceIcon.get(R.drawable.ic_qs_battery_reserve_on);
        } else {
            state.visible = true;
            state.icon = ResourceIcon.get(R.drawable.ic_qs_battery_reserve_off);
        }
    }

    private boolean getBatteryReserveEnabled() {
        try {
            FileInputStream fis = new FileInputStream(BATTERY_RESERVE_FILE);
            int result = fis.read();
            fis.close();
            return (result != '0');
        } catch (Exception e) {
            return false;
        }
	
    }

    protected void setBatteryReserveEnabled(boolean on) {
        try {
            FileOutputStream fos = new FileOutputStream(BATTERY_RESERVE_FILE);
            byte[] bytes = new byte[2];
            bytes[0] = (byte)(on ? '1' : '0');
            bytes[1] = '\n';
            fos.write(bytes);
            fos.close();
        } catch (Exception e) {
           // fail silently
        }
    }

    public BatteryReserveTile(Host host) {
        super(host);
    }

    private ContentObserver mObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            refreshState();
        }
    };

    @Override
    public void destroy() {
        mContext.getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    public void setListening(boolean listening) {
       
    }

}
