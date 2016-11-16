package com.nodeart.raixur.recorder;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.os.Environment;
import android.content.Context;

import java.util.UUID;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;

public class AudioRecorder extends CordovaPlugin {

    private MediaRecorder myRecorder;
    private String outputFile;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Context context = cordova.getActivity().getApplicationContext();

        System.out.println("Entered...");
        if (action.equals("record")) {
            System.out.println("Recording...");
            if (args.length() >= 1) {
                outputFile = args.getString(0);
            } else {
                outputFile = UUID.randomUUID().toString();
            }

            String outputDir = context.getExternalCacheDir().getAbsoluteFile() + "/" + outputFile + ".m4a";

            System.out.println("Path: " + outputFile);
            myRecorder = new MediaRecorder();
            myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            myRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            myRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            myRecorder.setAudioSamplingRate(44100);
            myRecorder.setAudioChannels(1);
            myRecorder.setAudioEncodingBitRate(32000);
            myRecorder.setOutputFile(outputFile);

            try {
                System.out.println("Preparing...");
                myRecorder.prepare();
                System.out.println("Prepared for recording.");
                myRecorder.start();
                System.out.println("Started.");
            } catch (final Exception e) {
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        callbackContext.error(e.getMessage());
                    }
                });
                return false;
            }
            return true;
        }

        if (action.equals("stop")) {
            System.out.println("Stopped...");
            stopRecord(callbackContext);
            return true;
        }

        return false;
    }

    private void stopRecord(final CallbackContext callbackContext) {
        myRecorder.stop();
        myRecorder.release();
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                callbackContext.success(outputFile);
            }
        });
    }

}
