/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2012 Android Open Kang Project
 * Copyright (C) 2013 The SlimRoms Project
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
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class QuickRecordTile extends QSTile<QSTile.BooleanState> {

    private static final String TAG = "QuickRecordTile";
    private static final String RECORDINGS_DIRECTORY = "QuickRecord";

    public static final int STATE_IDLE = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_RECORDING = 2;
    public static final int STATE_JUST_RECORDED = 3;
    public static final int STATE_NO_RECORDING = 4;
    public static final int BITRATE = 22000;
    // One hour maximum recording time
    public static final int MAX_RECORD_TIME = 3600000;

    private File mFile;
    private Handler mHandler;
    private MediaPlayer mPlayer = null;
    private MediaRecorder mRecorder = null;

    private static String mFileName = null;
    private static File mFolder = new File(Environment.getExternalStorageDirectory() 
                                                             + File.separator + RECORDINGS_DIRECTORY);

    private int mRecordingState = 0;

    public QuickRecordTile(Host host) {
        super(host);
        mHandler = new Handler();
        boolean success = true;
        if (!mFolder.exists()) {
            success = mFolder.mkdir();
        }
        if (success) {
            mFileName = getLastFileName();
        } else {
            Log.d(TAG, "Failed to create External Storage directory " + mFolder);
        }
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
    }

    @Override
    protected void handleClick() {
        mFileName = getLastFileName();
        if (mFileName == null) {
            mRecordingState = STATE_NO_RECORDING;
        }
        switch (mRecordingState) {
            case STATE_RECORDING:
                stopRecording();
                break;
            case STATE_NO_RECORDING:
                return;
            case STATE_IDLE:
            case STATE_JUST_RECORDED:
                startPlaying();
                break;
            case STATE_PLAYING:
                stopPlaying();
                break;
        }
    }

    @Override
    protected void handleLongClick() {
        mFileName = getLastFileName();
        if (mFileName == null) {
            mRecordingState = STATE_NO_RECORDING;
        }
        switch (mRecordingState) {
            case STATE_NO_RECORDING:
            case STATE_IDLE:
            case STATE_JUST_RECORDED:
                startRecording();
                break;
        }
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        int playStateName = 0;
        int playStateIcon = 0;
        state.visible = true;
        if (mFileName == null) {
            mRecordingState = STATE_NO_RECORDING;
        }
        switch (mRecordingState) {
            case STATE_PLAYING:
                playStateName = R.string.quick_settings_quick_record_play;
                playStateIcon = R.drawable.ic_qs_playing;
                break;
            case STATE_RECORDING:
                playStateName = R.string.quick_settings_quick_record_rec;
                playStateIcon = R.drawable.ic_qs_recording;
                break;
            case STATE_JUST_RECORDED:
                playStateName = R.string.quick_settings_quick_record_save;
                playStateIcon = R.drawable.ic_qs_saved;
                break;
            case STATE_NO_RECORDING:
                playStateName = R.string.quick_settings_quick_record_nofile;
                playStateIcon = R.drawable.ic_qs_quickrecord;
                break;
            default:
                playStateName = R.string.quick_settings_quick_record_def;
                playStateIcon = R.drawable.ic_qs_quickrecord;
        }
        final Resources r = mContext.getResources();
        state.icon = ResourceIcon.get(playStateIcon);
        state.label = r.getString(playStateName);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.DONT_TRACK_ME_BRO;
    }

    final Runnable delayTileRevert = new Runnable () {
        public void run() {
            if (mRecordingState == STATE_JUST_RECORDED) {
                mRecordingState = STATE_IDLE;
                refreshState();
            }
        }
    };


    final Runnable autoStopRecord = new Runnable() {
        public void run() {
            if (mRecordingState == STATE_RECORDING) {
                stopRecording();
            }
        }
    };

    final OnCompletionListener stoppedPlaying = new OnCompletionListener(){
        public void onCompletion(MediaPlayer mp) {
            mRecordingState = STATE_IDLE;
            refreshState();
        }
    };

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        mFileName = getLastFileName();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
            mRecordingState = STATE_PLAYING;
            refreshState();
            mPlayer.setOnCompletionListener(stoppedPlaying);
        } catch (IOException e) {
            Log.e(TAG, "QuickRecord prepare() failed on play: ", e);
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
        mRecordingState = STATE_IDLE;
        refreshState();
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mFileName = getTimestampFilename();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        mRecorder.setAudioEncodingBitRate(BITRATE);
        mRecorder.setAudioSamplingRate(16000);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setOutputFile(mFileName);
        try {
            mRecorder.prepare();
            mRecorder.start();
            mRecordingState = STATE_RECORDING;
            refreshState();
            mHandler.postDelayed(autoStopRecord, MAX_RECORD_TIME);
        } catch (IOException e) {
            Log.e(TAG, "QuickRecord prepare() failed on record: ", e);
        }
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        mRecordingState = STATE_JUST_RECORDED;
        refreshState();
        makeFileDiscoverable(mFileName);
        mHandler.postDelayed(delayTileRevert, 2000);
    }

    private String getTimestampFilename() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String currentTimeStamp = dateFormat.format(new Date());
        if (!mFolder.exists()) {
            mFolder.mkdir();
        }
        return (mFolder.getAbsolutePath() + File.separator + "Recording_" + currentTimeStamp + ".aac");
    }

    private String getLastFileName() {
        if (!mFolder.exists()) {
            mFolder.mkdir();
        }
        String[] files = mFolder.list();
        if(files.length == 0) {
            return null;
        }
        int fileIndex;
        for (fileIndex=(files.length - 1); fileIndex>=0; fileIndex--) {
            if (files[fileIndex].endsWith(".aac")) {
                return (mFolder.getAbsolutePath() + File.separator + files[fileIndex]);
            }
        }
        return null;
    }

    public void makeFileDiscoverable(String fileName) {
        MediaScannerConnection.scanFile(mContext, new String[]{fileName}, null, null);
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                                                Uri.parse(fileName)));
    }
}
