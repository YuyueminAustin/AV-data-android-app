package com.msra.avliveness.utils;

import android.os.Build;

import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Grid;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.WhiteBalance;
import com.otaliastudios.cameraview.filter.Filters;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {



    public static void setFlashMode(CameraView cameraView, String mode) {
        if (mode == null) {
            // Log an error or set a default mode
            cameraView.setFlash(Flash.AUTO); // default setting
            return;
        }
        switch (mode) {
            case "OFF":
                cameraView.setFlash(Flash.OFF);
                break;
            case "ON":
                cameraView.setFlash(Flash.ON);
                break;
            case "AUTO":
                cameraView.setFlash(Flash.AUTO);
                break;
            case "TORCH":
                cameraView.setFlash(Flash.TORCH);
                break;
        }
    }

    public static void setGridMode(CameraView cameraView, String mode) {
        if (mode == null) {
            // Log an error or set a default mode
            cameraView.setGrid(Grid.OFF); // default setting
            return;
        }
        switch (mode) {
            case "OFF":
                cameraView.setGrid(Grid.OFF);
                break;
            case "ON":
                cameraView.setGrid(Grid.DRAW_PHI);
                break;
        }
    }

    public static void setHDRMode(CameraView cameraView, String mode) {
        if (mode == null) {
            // Log an error or set a default mode
            cameraView.setHdr(Hdr.OFF); // default setting
            return;
        }
        switch (mode) {
            case "OFF":
                cameraView.setHdr(Hdr.OFF);
                break;
            case "ON":
                cameraView.setHdr(Hdr.ON);
                break;
        }
    }

    public static void setWhiteBalance(CameraView cameraView, String mode) {
        if (mode == null) {
            // Log an error or set a default mode
            cameraView.setWhiteBalance(WhiteBalance.AUTO);
            return;
        }
        switch (mode) {
            case "AUTO":
                cameraView.setWhiteBalance(WhiteBalance.AUTO);
                break;
            case "INCANDESCENT":
                cameraView.setWhiteBalance(WhiteBalance.INCANDESCENT);
                break;
            case "FLUORESCENT":
                cameraView.setWhiteBalance(WhiteBalance.FLUORESCENT);
                break;
            case "DAYLIGHT":
                cameraView.setWhiteBalance(WhiteBalance.DAYLIGHT);
                break;
            case "CLOUDY":
                cameraView.setWhiteBalance(WhiteBalance.CLOUDY);
                break;
        }
    }

    public static void setFilter(CameraView cameraView, String filter) {
        if (filter == null) {
            // Log an error or set a default mode
            cameraView.setFilter(Filters.NONE.newInstance());
            return;
        }
        switch (filter) {
            case "NONE":
                cameraView.setFilter(Filters.NONE.newInstance());
                break;
            case "AUTO_FIX":
                cameraView.setFilter(Filters.AUTO_FIX.newInstance());
                break;
            case "BLACK_AND_WHITE":
                cameraView.setFilter(Filters.BLACK_AND_WHITE.newInstance());
                break;
            case "BRIGHTNESS":
                cameraView.setFilter(Filters.BRIGHTNESS.newInstance());
                break;
            case "CONTRAST":
                cameraView.setFilter(Filters.CONTRAST.newInstance());
                break;
            case "CROSS_PROCESS":
                cameraView.setFilter(Filters.CROSS_PROCESS.newInstance());
                break;
            case "DOCUMENTARY":
                cameraView.setFilter(Filters.DOCUMENTARY.newInstance());
                break;
            case "DUOTONE":
                cameraView.setFilter(Filters.DUOTONE.newInstance());
                break;
            case "FILL_LIGHT":
                cameraView.setFilter(Filters.FILL_LIGHT.newInstance());
                break;
            case "GAMMA":
                cameraView.setFilter(Filters.GAMMA.newInstance());
                break;
            case "GRAIN":
                cameraView.setFilter(Filters.GRAIN.newInstance());
                break;
            case "GRAYSCALE":
                cameraView.setFilter(Filters.GRAYSCALE.newInstance());
                break;
            case "HUE":
                cameraView.setFilter(Filters.HUE.newInstance());
                break;
            case "INVERT_COLORS":
                cameraView.setFilter(Filters.INVERT_COLORS.newInstance());
                break;
            case "LOMOISH":
                cameraView.setFilter(Filters.LOMOISH.newInstance());
                break;
            case "POSTERIZE":
                cameraView.setFilter(Filters.POSTERIZE.newInstance());
                break;
            case "SATURATION":
                cameraView.setFilter(Filters.SATURATION.newInstance());
                break;
            case "SEPIA":
                cameraView.setFilter(Filters.SEPIA.newInstance());
                break;
            case "SHARPNESS":
                cameraView.setFilter(Filters.SHARPNESS.newInstance());
                break;
            case "TEMPERATURE":
                cameraView.setFilter(Filters.TEMPERATURE.newInstance());
                break;
            case "TINT":
                cameraView.setFilter(Filters.TINT.newInstance());
                break;
            case "VIGNETTE":
                cameraView.setFilter(Filters.VIGNETTE.newInstance());
                break;
        }
    }





    public static String getTimeStr() {
        // Format the date up to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");

        // Get the current date and time
        Date now = new Date();

        // Format the current date and time
        String formattedDate = sdf.format(now);

        // Get nanoseconds using System.nanoTime
        long nanoTime = System.nanoTime() % 1_000_000; // Extract the nanoseconds part

        // Combine formatted date and nanoseconds
        return formattedDate + String.format("%06d", nanoTime); // add nanoseconds
    }


    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String deviceName;
        if (model.startsWith(manufacturer)) {
            deviceName = capitalize(model);
        } else {
            deviceName = capitalize(manufacturer) + "-" + model;
        }

        // Replace all spaces and underscores with hyphens
        return deviceName.replace(" ", "-").replace("_", "-");
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        char[] chars = str.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    public static String fileNameConstructor(String idx, String deviceName, String startTime, String endTime, String identifier) {
        return String.format("%s_%s_%s", identifier, startTime, endTime);
    }

    public static String fileNameConstructor(String idx, String deviceName, String startTime) {
        if (idx == null) {
            idx = "-1";
        }
        return String.format("%s_%s_%s", idx, deviceName, startTime);
    }


    public static int getIdx(File baseDir) {
        File[] files = baseDir.listFiles();
        int maxIndex = -1;
        Pattern pattern = Pattern.compile("^(\\d+)_");
        for (File file : files) {
            if (file.isDirectory()) {
                Matcher matcher = pattern.matcher(file.getName());
                if (matcher.find()) {
                    int index = Integer.parseInt(matcher.group(1));
                    if (index > maxIndex) {
                        maxIndex = index;
                    }
                }
            }
        }
        return maxIndex + 1;
    }
}
