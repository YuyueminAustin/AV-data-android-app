package com.msra.avliveness.fragments;

import android.hardware.Sensor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.msra.avliveness.R;
import com.msra.avliveness.services.MotionSensorHelper;
import com.msra.avliveness.views.SensorDataView;

import java.io.File;

public class SensorDataFragment extends Fragment {

    private static final String ARG_SESSION_FOLDER = "session_folder";
    private SensorDataView sensorDataView;
    private MotionSensorHelper motionSensorHelper;

    private File sessionFolder;

    public static SensorDataFragment newInstance(String sessionFolderPath) {
        SensorDataFragment fragment = new SensorDataFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SESSION_FOLDER, sessionFolderPath);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the fragment layout
        View view = inflater.inflate(R.layout.fragment_sensor_data, container, false);

        // Initialize the SensorDataView
        sensorDataView = view.findViewById(R.id.sensorDataView);

        Log.d("SensorDataFragment", "sensorDataView initialized: " + (sensorDataView != null));

        if (getArguments() != null) {
            String sessionFolderPath = getArguments().getString(ARG_SESSION_FOLDER);
            sessionFolder = new File(sessionFolderPath);
            Log.d("SensorDataFragment", "Session folder: " + sessionFolder.getAbsolutePath());
            motionSensorHelper = new MotionSensorHelper(getContext(), sessionFolder);
            motionSensorHelper.setSensorDataView(sensorDataView);
        }

//        // Initialize the MotionSensorHelper
//        motionSensorHelper = new MotionSensorHelper(getContext());
//        motionSensorHelper.setSensorDataView(sensorDataView);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Start listening to sensor data and update the UI
        if (motionSensorHelper != null) {
            motionSensorHelper.startRecording();
        }
//        updateSensorData();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop listening to sensor data
        if (motionSensorHelper != null) {
            motionSensorHelper.stopRecording();
        }
    }

    private void updateSensorData() {
        Log.d("Sensor", "Fragment: UpdateSensorData");
        float[] latestAccData = motionSensorHelper.getSensorData(Sensor.TYPE_ACCELEROMETER);
        if (latestAccData != null) {
            sensorDataView.updateSensorData("Accelerometer", latestAccData);
        }

        float[] latestGyroData = motionSensorHelper.getSensorData(Sensor.TYPE_GYROSCOPE);
        if (latestGyroData != null) {
            sensorDataView.updateSensorData("Gyroscope", latestGyroData);
        }
    }



}

