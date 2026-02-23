package com.msra.avliveness.utils;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;


import com.msra.avliveness.utils.DeviceInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class DeviceQuery {
    private static final String TAG = "device";
    private Context context;

    public DeviceQuery(Context context) {
        this.context = context;
    }

    public List<DeviceInfo> getAvailableCameras() {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        List<DeviceInfo> cameraList = new ArrayList<>();
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
//                Log.d("Camera ID", cameraId);
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                int lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                String type = (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) ? "Front" : "Back";
                if (characteristics != null & type == "Front") {
                    cameraList.add(new DeviceInfo(type + " Camera", cameraId));
                    Log.d(TAG, "cameraID: " + cameraId);
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return cameraList;
    }

    public List<DeviceInfo> getAvailableSpeakers() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        List<DeviceInfo> speakerList = new ArrayList<>();
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        int deviceId = 0;
        for (AudioDeviceInfo device : devices) {
            if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                    device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.getType() == AudioDeviceInfo.TYPE_USB_DEVICE) {
                Log.d("device", device.toString());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Log.d("address",device.getAddress());
                }

                speakerList.add(new DeviceInfo(device.getProductName().toString(), String.valueOf(device.getId())));
            }
            deviceId += 1;
        }
        return speakerList;
    }

    public List<DeviceInfo> getAvailableMicrophones() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        List<DeviceInfo> microphoneList = new ArrayList<>();
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        int deviceId = 0;
        for (AudioDeviceInfo device : devices) {
            if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC ||
                    device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.getType() == AudioDeviceInfo.TYPE_USB_DEVICE) {
                Log.i("device", device.getProductName().toString());
                String address = "";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                     address = device.getAddress();
                     microphoneList.add(new DeviceInfo(address + " Microphone: " + device.getId(), String.valueOf(device.getId()) ) );


                }


            }
            deviceId += 1;
        }
        return microphoneList;
    }

    public static void listMicrophones(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);

        for (AudioDeviceInfo device : devices) {
            if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC) {

                int[] channels = device.getChannelCounts();
                int[] encodings = device.getEncodings();
                int[] sampleRates = device.getSampleRates();

                System.out.println("Microphone ID: " + device.getId());
                System.out.println("Channel Counts: " + Arrays.toString(channels));
                System.out.println("Encodings: " + Arrays.toString(encodings));
                System.out.println("Sample Rates: " + Arrays.toString(sampleRates));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    System.out.println("Location: " + device.getAddress());
                }
            }
        }
    }

}

