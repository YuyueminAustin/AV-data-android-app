package com.msra.avliveness.services;

import android.annotation.SuppressLint;
import android.content.Context;

import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import android.util.Log;
import android.widget.Toast;

import com.msra.avliveness.utils.Utils;


public class AudioRecorderHelper {
    private static final String TAG = "AudioRecorder";
    private AudioRecord audioRecord;
    private RecordThread recordThread;
    private boolean isRecording = false;
    private int sampleRate = 48000;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
//    private RecordListener recordListener;

    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;

    private int bufferSize;

    private FileOutputStream mOutputStream;
    private File mTempRecordFile;
    private File mLastRecordFile;

    private String startTime;
    private String endTime;

    private RecordListener recordListener = new RecordListener() {
        @Override
        public void onStartRecord() {

            Log.i(TAG, "onStartRecord: ");

            startTime = Utils.getTimeStr();
        }

        @Override
        public void onRecordData(byte[] bytes) {
            try {
                if (mOutputStream != null) {
                    mOutputStream.write(bytes);
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException while writing data", e);
            }
        }

        @Override
        public void onStopRecord() {
            Log.i(TAG, "onStopRecord: ");
            endTime = Utils.getTimeStr();
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    if (mOutputStream != null) {
                        mOutputStream.flush();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IOException while flushing output stream", e);
                }

//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
//                Date date = new Date();
//                String path = mTempRecordFile.getPath();
                String fileName = mTempRecordFile.getAbsolutePath().replace(".pcm", ".wav");

                try {
                    convertPcmToWav(mTempRecordFile.getAbsolutePath(), fileName);
//                    mLastRecordFile = new File(fileName);
                    Log.i(TAG, "File Saved on: " + fileName);
                } catch (IOException e) {
                    Log.e(TAG, "IOException while converting to WAV", e);
                }
            });
        }
    };

    public String getAudioRecordStartTime() {
        return startTime;
    }

    public String getAudioRecordEndTime() {
        return endTime;
    }

    private static AudioDeviceInfo getMicrophoneById(Context context, int microphoneId) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);

        for (AudioDeviceInfo device : devices) {
            if (device.getId() == microphoneId) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Log.d(TAG, "Selected device: " + microphoneId + "/" + device.getAddress());
                }
                return device;
            }
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    public AudioRecorderHelper(int audioSource, Context context, int microphoneId){
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        bufferSize = minBufferSize;
        audioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, minBufferSize);
        // set device
        if (audioRecord.setPreferredDevice(getMicrophoneById(context, microphoneId))) {
            Toast.makeText(context, "Microphone set to " + microphoneId, Toast.LENGTH_LONG).show();
        }

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed");
            audioRecord = null;
        } else {
            Log.d(TAG, "AudioRecord initialized successfully");
        }
        Log.d(TAG, "state: " + audioRecord.getState());
    }

    public void startAudioRecording(Context context, String filePath) {

//        Toast.makeText(context, "Saved To " + filePath, Toast.LENGTH_LONG).show();
        if (audioRecord != null) {
            audioRecord.startRecording();
        } else {
            Log.e(TAG, "audioRecord is null.");
            return;
        }

        isRecording = true;



        try {
            mTempRecordFile = new File(filePath);
            if (!mTempRecordFile.getParentFile().exists()) {
                mTempRecordFile.getParentFile().mkdirs();
            }
            mOutputStream = new FileOutputStream(mTempRecordFile);
        } catch (IOException e) {
            Log.e(TAG, "Failed to create output stream", e);
            return;
        }
        Log.i(TAG, "File path: " + filePath);
        recordThread = new RecordThread("AudioRecordThread");
        recordThread.start();
        if (recordListener != null) {
            recordListener.onStartRecord();
        } else {
            Log.w(TAG, "start: recordListener is null");
        }


    }

    public void setRecordListener(RecordListener _recordListener){
        recordListener = _recordListener;
    }

    public void stopAudioRecording() {
        if (audioRecord != null) {
            isRecording = false;

            try{
                if (recordThread != null) {
                    recordThread.join();
                }

            } catch (InterruptedException e) {
                Log.d(TAG, "InterruptedException " + e.getMessage());
            } finally {
                if (recordListener != null) {
                    recordListener.onStopRecord();
                } else {
                    Log.d(TAG, "stop: recordListener is null");
                }
                audioRecord.stop();
//                audioRecord.release();
            }
//            recordListener = null;

        }
    }

    public void endSession(){
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        if (recordListener != null) {
            recordListener = null;
        }
    }

    class RecordThread extends Thread {

        public RecordThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            Log.v(TAG, "recording thread run");
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

            if (audioRecord == null || audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                Log.d(TAG, "AudioRecord is not initialized");
                return;
            }

            byte[] buffer = new byte[bufferSize];
            try {
                while (isRecording) {
                    int len = audioRecord.read(buffer, 0, buffer.length);
                    if (len > 0) {
                        if (recordListener != null) {
                            recordListener.onRecordData(buffer);
                        }
                    } else {
                        Log.e(TAG, "record read error: " + len);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during recording", e);
            } finally {
                try {
                    if (mOutputStream != null) {
                        mOutputStream.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IOException while closing file output stream", e);
                }
            }
            Log.v(TAG, "recording thread end");
        }
    }

    private void convertPcmToWav(String pcmFilePath, String wavFilePath) throws IOException {
        FileInputStream pcmInputStream = new FileInputStream(pcmFilePath);
        FileOutputStream wavOutputStream = new FileOutputStream(wavFilePath);
        long totalAudioLen = pcmInputStream.getChannel().size();
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = sampleRate;
        int channels = 1;
        long byteRate = 16 * sampleRate * channels / 8;

        byte[] header = new byte[44];
        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = 16;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        wavOutputStream.write(header, 0, 44);

        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        while ((bytesRead = pcmInputStream.read(buffer)) != -1) {
            wavOutputStream.write(buffer, 0, bytesRead);
        }

        pcmInputStream.close();
        wavOutputStream.close();
    }



}

