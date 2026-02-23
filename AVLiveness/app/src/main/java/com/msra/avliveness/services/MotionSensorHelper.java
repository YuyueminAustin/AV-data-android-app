package com.msra.avliveness.services;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.msra.avliveness.activities.MainActivity;
import com.msra.avliveness.utils.Utils;
import com.msra.avliveness.views.SensorDataView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MotionSensorHelper implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    public String save_root;

    private Context context;

    private boolean isRecording = false;

    private List<float[]> accData = new ArrayList<>();
    private List<float[]> gyroData = new ArrayList<>();

    private SensorDataView sensorDataView;

    private File accFile;
    private File gyroFile;
    private FileWriter accWriter;
    private FileWriter gyroWriter;

    public MotionSensorHelper(Context context, File sessionFolder) {
        this.context = context;
        sensorManager = (SensorManager) (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Initialize files and writers
        try {
            accFile = new File(sessionFolder, "accelerometer_data.csv");
            gyroFile = new File(sessionFolder, "gyroscope_data.csv");
            accWriter = new FileWriter(accFile, true);
            gyroWriter = new FileWriter(gyroFile, true);

            // Write headers with timestamp
            accWriter.write("Timestamp,X,Y,Z\n");
            gyroWriter.write("Timestamp,X,Y,Z\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
//        sensorDataView = new SensorDataView(context);
    }

    public void startRecording() {
        accData.clear();
        gyroData.clear();
        isRecording = true;
        if (accelerometer != null && gyroscope != null) {
            sensorManager.registerListener(this, accelerometer, 10000);
            sensorManager.registerListener(this, gyroscope, 10000);
            Log.d("Sensor", "Sensor Start Recording");
        } else {
            Toast.makeText(context, "Sensors not available", Toast.LENGTH_LONG).show();
        }
    }

    public void stopRecording() {
        isRecording = false;
        sensorManager.unregisterListener(this);
        try {
            if (accWriter != null) {
                accWriter.close();
            }
            if (gyroWriter != null) {
                gyroWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
//        saveSensorData();
    }

    private void writeDataToFile(FileWriter writer, String timestamp, float[] values) {
        try {
            writer.write(timestamp + "," + values[0] + "," + values[1] + "," + values[2] + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d("Sensor", "onSensorChanged");
        if (isRecording) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accData.add(event.values.clone());
                writeDataToFile(accWriter, Utils.getTimeStr(), event.values);
                Log.d("Sensor", "accNewData: " + Arrays.toString(event.values));
                updateUI("Accelerometer", event.values);
            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                gyroData.add(event.values.clone());
                writeDataToFile(gyroWriter, Utils.getTimeStr(), event.values);
                updateUI("Gyroscope", event.values);
            }
        }
    }

    private void updateUI(final String sensorType, final float[] values) {
        Log.d("SensorData", "updateUI called with values: " + Arrays.toString(values));
        Log.d("SensorData", "sensorDataView is " + (sensorDataView != null ? "not null" : "null"));

        // Ensure sensorDataView is not null before posting the update
        if (sensorDataView != null) {
            sensorDataView.post(new Runnable() {
                @Override
                public void run() {
                    Log.d("SensorData", "Calling updateSensorData on sensorDataView");
                    sensorDataView.updateSensorData(sensorType, values);
                }
            });
        } else {
            Log.d("SensorData", "sensorDataView is null, cannot update UI");
        }
    }

    public float[] getSensorData(int sensorType) {
        Log.d("Sensor", "Helper:: Get Sensor Data");
        Log.d("Sensor", "Helper::accData: " + accData);
        if (sensorType == Sensor.TYPE_ACCELEROMETER && !accData.isEmpty()) {
            Log.d("Sensor", "Helper::accData" + accData.get(accData.size() - 1));
            return accData.get(accData.size() - 1); // Return the most recent accelerometer data
        } else if (sensorType == Sensor.TYPE_GYROSCOPE && !gyroData.isEmpty()) {
            return gyroData.get(gyroData.size() - 1); // Return the most recent gyroscope data
        } else {
            return null; // If data is not available for the requested sensor type
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void setSensorDataView(SensorDataView sensorDataView) {
        this.sensorDataView = sensorDataView;
    }
}
