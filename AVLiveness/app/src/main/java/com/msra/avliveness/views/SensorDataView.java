package com.msra.avliveness.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.widget.AppCompatTextView;

public class SensorDataView extends AppCompatTextView {

    private String accelerometerData = "Accelerometer ---\tNo data available";
    private String gyroscopeData = "Gyroscope ---\tNo data available";

    public SensorDataView(Context context) {
        super(context);
    }

    public SensorDataView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SensorDataView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // Method to update the sensor data
    public void updateSensorData(String sensorType, float[] values) {
        Log.d("SensorData", "View: Update Sensor Data");
        if (values != null && values.length >= 3) {
            String formattedData = String.format("%s ---\tX: %.2f\tY: %.2f\tZ: %.2f",
                    sensorType, values[0], values[1], values[2]);

            if (sensorType.equals("Accelerometer")) {
                accelerometerData = formattedData;
            } else if (sensorType.equals("Gyroscope")) {
                gyroscopeData = formattedData;
            }

            // Update the display to show both types of data
            setText(accelerometerData + "\n\n" + gyroscopeData);
        }
    }
}

