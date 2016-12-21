package com.nodeart.raixur.recorder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.UUID;

public class CordovaRecorder extends CordovaPlugin {

    private static final int RECORDING_DEVICE = MediaRecorder.AudioSource.MIC;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int SAMPLE_RATE = 44100;

    private AudioRecorder recorder;


    private String outputPath;
    private int duration;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if(action.equals("record")) {
            initRecord(args);
            record(callbackContext);
            return true;
        }
        else if(action.equals("startRecord")) {
            initRecord(args);
            startRecord();
            return true;
        }
        else if(action.equals("stopRecord")) {
            stopRecord(callbackContext);
            return true;
        }
        return false;
    }

    private void record(final CallbackContext callbackContext) {
        recorder.start();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                recorder.release();
                callbackContext.success(outputPath);
            }
        }, duration);
    }

    private void startRecord() {
        recorder.start();
    }

    private void stopRecord(CallbackContext callbackContext) {
        recorder.stop();
        callbackContext.success(outputPath);
    }

    private void initRecord(JSONArray args) throws JSONException {
        Context context = cordova.getActivity().getApplicationContext();
        String outputPath;

        if (args.length() >= 1) {
            outputPath = args.getString(0);
        } else {
            outputPath = context.getExternalCacheDir().getAbsoluteFile() + "/" + UUID.randomUUID().toString() + ".wav";
        }

        if (args.length() >= 2) {
            duration = Integer.parseInt(args.getString(1));
        } else {
            duration = 0;
        }

        recorder = new AudioRecorder(RECORDING_DEVICE, SAMPLE_RATE,
                    CHANNEL_CONFIG, AUDIO_ENCODING);
        recorder.setOutputFile(outputPath);
        recorder.prepare();
    }
}
