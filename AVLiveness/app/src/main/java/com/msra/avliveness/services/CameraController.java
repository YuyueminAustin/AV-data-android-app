package com.msra.avliveness.services;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.msra.avliveness.utils.Utils;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.VideoCodec;

import java.io.File;

public class CameraController {
    private static final String TAG = "CameraController";
    private CameraView camera;
    private Context context;

    private String startTime, endTime;

    private File currentVideoFile;

    public String currentVideoIndex;

    public CameraController(Context context, CameraView camera) {
        this.camera = camera;
        this.context = context;
        initializeCamera();
    }

    private void initializeCamera() {
        camera.setLifecycleOwner((AppCompatActivity) context);
        camera.addCameraListener(new CameraListener() {
            @Override
            public void onVideoTaken(VideoResult result) {
                super.onVideoTaken(result);
//                renameRecording();
//                Toast.makeText(context, "File saved at " + loc, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onVideoRecordingStart() {
                startTime = Utils.getTimeStr();
                Toast.makeText(context, "Recording Starts", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Video Recording status: " + camera.isTakingVideo());
            }

            @Override
            public void onVideoRecordingEnd() {
//                Log.d(TAG, "Video Recording status: " + camera.isTakingVideo());
                endTime = Utils.getTimeStr();
                Toast.makeText(context, "Recording Stops", Toast.LENGTH_LONG).show();
            }
        });

        camera.setMode(Mode.VIDEO);
        camera.setFacing(Facing.FRONT);
        camera.setVideoCodec(VideoCodec.H_264);
        camera.setAudio(Audio.OFF);
        camera.setVideoMaxDuration(300000); // 5-min
        camera.setPreviewFrameRate(30F);
    }

    public void startRecording(String folderName) {
//        Toast.makeText(context, "Folder Name " + folderName, Toast.LENGTH_SHORT).show();
        currentVideoFile = new File(folderName);
        camera.takeVideo(currentVideoFile);

    }

    public String getVideoRecordStartTime() {
        return startTime;
    }

    public String getVideoRecordEndTime() {
        return endTime;
    }

    public void stopRecording(String idx) {
        this.currentVideoIndex = idx;
//        Log.d(TAG, "Video Recording status: " + camera.isTakingVideo());
        if (camera.isTakingVideo()) {
            camera.stopVideo();
            // Rename the file here

        }
    }

    public void renameRecording(){
        String idx = this.currentVideoIndex;
        String newFileName = Utils.fileNameConstructor(idx, Utils.getDeviceName(), startTime, endTime, "v");
        File newFile = new File(currentVideoFile.getParent(), newFileName + ".mp4");
        if (currentVideoFile.renameTo(newFile)) {
            Toast.makeText(context, "File renamed to " + newFileName, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Failed to rename file", Toast.LENGTH_SHORT).show();
        }


    }

    public void resumeCamera() {
        camera.open();
    }

    public void pauseCamera() {
        camera.close();
    }

    public void destroyCamera() {
        camera.destroy();
    }
}