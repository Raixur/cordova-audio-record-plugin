package com.nodeart.raixur.recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class AudioRecorder
{
    /**
     * INITIALIZING : recorder is initializing;
     * READY : recorder has been initialized, recorder not yet started
     * RECORDING : recording
     * ERROR : reconstruction needed
     * STOPPED: reset needed
     */
    public enum State {INITIALIZING, READY, RECORDING, ERROR, STOPPED}

    // The interval in which the recorded samples are output to the file
    private static final int TIMER_INTERVAL = 120;

    private AudioRecord 	 aRecorder = null;

    private String			 fPath = null;

    private State			 state;

    // File writer
    private RandomAccessFile fWriter;
    private FileChannel		 fChannel;

    // Number of channels, sample rate, sample size(size in bits),
    // buffer size, audio source, sample size
    private short 			 nChannels;
    private int				 sRate;
    private short			 bSamples;
    private int				 bufferSize;
    private int				 aSource;
    private int				 aFormat;

    // Number of frames written to file on each output(only in uncompressed mode)
    private int				 framePeriod;

    // Buffer for output(only in uncompressed mode)
    //private ShortBuffer		shBuffer;
    private ByteBuffer		bBuffer;

    // Number of bytes written to file after header(only in uncompressed mode)
    // after stop() is called, this size is written to the header/data chunk in the wave file
    private int				 payloadSize;

    /**
     *
     * Returns the state of the recorder in a RehearsalAudioRecord.State typed object.
     * Useful, as no exceptions are thrown.
     *
     * @return recorder state
     */
    public State getState() {
        return state;
    }

    /**
     * Method used for recording.
     */
    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener()
    {
        @Override
        public void onPeriodicNotification(AudioRecord recorder) {
            aRecorder.read(bBuffer, bBuffer.capacity()); // Fill buffer
            try {
                bBuffer.rewind();
                fChannel.write(bBuffer); // Write buffer to file
                payloadSize += bBuffer.capacity();
            } catch (IOException e) {
                stop();
            }
        }

        @Override
        public void onMarkerReached(AudioRecord recorder) {  }
    };

    /**
     * Default constructor
     *
     * Instantiates a new recorder, in case of compressed recording the parameters can be left as 0.
     * In case of errors, no exception is thrown, but the state is set to ERROR
     */
    public AudioRecorder(int audioSource, int sampleRate, int channelConfig,
                                  int audioFormat)
    {
        try {
            if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                bSamples = 16;
            } else {
                bSamples = 8;
            }

            if (channelConfig == AudioFormat.CHANNEL_IN_MONO) {
                nChannels = 1;
            } else {
                nChannels = 2;
            }

            aSource = audioSource;
            sRate   = sampleRate;
            aFormat = audioFormat;

            framePeriod = sampleRate * TIMER_INTERVAL / 1000;
            bufferSize = framePeriod * 2 * bSamples * nChannels / 8;

            if (bufferSize < AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)) {
                bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                framePeriod = bufferSize / ( 2 * bSamples * nChannels / 8 );
            }

            aRecorder = new AudioRecord(audioSource, sampleRate,
                    channelConfig, audioFormat, bufferSize);

            if (aRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new Exception("AudioRecord initialization failed");
            }

            aRecorder.setRecordPositionUpdateListener(updateListener);
            aRecorder.setPositionNotificationPeriod(framePeriod);

            fPath = null;
            state = State.INITIALIZING;

        } catch (Exception e) {
            state = State.ERROR;
        }
    }

    /**
     * Sets output file path, call directly after construction/reset.
     *
     * @param outputFile file path
     */
    public void setOutputFile(String outputFile) {
        if (state == State.INITIALIZING) {
            fPath = outputFile;
        }
    }

    /**
     * Prepares the recorder for recording, in case the recorder is not in the INITIALIZING state and the file path was not set
     * the recorder is set to the ERROR state, which makes a reconstruction necessary.
     * In case uncompressed recording is toggled, the header of the wave file is written.
     * In case of an exception, the state is changed to ERROR
     */
    public void prepare() {
        try {
            if (state == State.INITIALIZING) {
                if ((aRecorder.getState() == AudioRecord.STATE_INITIALIZED) & (fPath != null)) {
                    // write file header

                    fWriter = new RandomAccessFile(fPath, "rw");
                    fChannel= fWriter.getChannel();

                    fWriter.setLength(0); // Set file length to 0, to prevent unexpected behavior in case the file already existed
                    fWriter.writeBytes("RIFF");
                    fWriter.writeInt(0); // Final file size not known yet, write 0

                    fWriter.writeBytes("WAVE");
                    fWriter.writeBytes("fmt ");
                    fWriter.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
                    fWriter.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
                    fWriter.writeShort(Short.reverseBytes(nChannels));// Number of channels, 1 for mono, 2 for stereo
                    fWriter.writeInt(Integer.reverseBytes(sRate)); // Sample rate
                    fWriter.writeInt(Integer.reverseBytes(sRate*bSamples*nChannels/8)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
                    fWriter.writeShort(Short.reverseBytes((short)(nChannels*bSamples/8))); // Block align, NumberOfChannels*BitsPerSample/8
                    fWriter.writeShort(Short.reverseBytes(bSamples)); // Bits per sample

                    fWriter.writeBytes("data");
                    fWriter.writeInt(0); // Data chunk size not known yet, write 0

                    bBuffer = ByteBuffer.allocateDirect(framePeriod*bSamples/8*nChannels);
                    bBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    bBuffer.rewind();
                    //shBuffer = bBuffer.asShortBuffer();

                    state = State.READY;
                }
                else {
                    state = State.ERROR;
                }
            }
            else {
                release();
                state = State.ERROR;
            }
        }
        catch(Exception e)
        {
            state = State.ERROR;
        }
    }

    /**
     *  Releases the resources associated with this class, and removes the unnecessary files, when necessary
     */
    public void release() {
        if (state == State.RECORDING) {
            stop();
        }
        else if (state == State.READY) {
            try {
                fWriter.close(); // Remove prepared file
            } catch (IOException ignored) { }

            (new File(fPath)).delete();
        }

        if (aRecorder != null) {
            aRecorder.release();
        }
    }

    /**
     * Resets the recorder to the INITIALIZING state, as if it was just created.
     * In case the class was in RECORDING state, the recording is stopped.
     * In case of exceptions the class is set to the ERROR state.
     */
    public void reset() {
        try {
            if (state != State.ERROR) {
                release();
                fPath = null; // Reset file path

                aRecorder = new AudioRecord(aSource, sRate, nChannels+1, aFormat, bufferSize);

                state = State.INITIALIZING;
            }
        }
        catch (Exception e) {
            state = State.ERROR;
        }
    }

    /**
     * Starts the recording, and sets the state to RECORDING.
     * Call after prepare().
     */
    public void start() {
        if (state == State.READY) {
            payloadSize = 0;
            aRecorder.startRecording();
            aRecorder.read(bBuffer, bBuffer.capacity());
            bBuffer.rewind();

            state = State.RECORDING;
        }
        else {
            state = State.ERROR;
        }
    }

    /**
     *  Stops the recording, and sets the state to STOPPED.
     * In case of further usage, a reset is needed.
     * Also finalizes the wave file in case of uncompressed recording.
     */
    public void stop() {
        if (state == State.RECORDING) {
            aRecorder.stop();

            try {
                fWriter.seek(4); // Write size to RIFF header
                fWriter.writeInt(Integer.reverseBytes(36+payloadSize));

                fWriter.seek(40); // Write size to Subchunk2Size field
                fWriter.writeInt(Integer.reverseBytes(payloadSize));

                fWriter.close();
            } catch(IOException e) {
                state = State.ERROR;
            }

            state = State.STOPPED;
        }
        else {
            state = State.ERROR;
        }
    }
}