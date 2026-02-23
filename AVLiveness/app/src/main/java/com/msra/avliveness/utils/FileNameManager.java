package com.msra.avliveness.utils;
import android.content.Context;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileNameManager {
    private int currentIndex;

    public FileNameManager() {
        currentIndex = 0;
    }

    private int getMaxIndex(File baseDir) {
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
        return maxIndex;
    }
    public File createNewSessionFolder(Context context, String experiment_name) {

        File baseDir = new File(context.getExternalFilesDir(null).getPath());
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        currentIndex = getMaxIndex(baseDir) + 1;
        Toast.makeText(context,"Experiment Index: " + currentIndex, Toast.LENGTH_LONG).show();

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date());
        String folderName = currentIndex + "_" + experiment_name + "_" + timeStamp;



        File sessionFolder = new File(baseDir, folderName);
        if (!sessionFolder.exists()) {
            sessionFolder.mkdirs();
        }


        return sessionFolder;
    }

    public int getIdx() {
        return currentIndex;
    }

    public String generateAudioFileName(File sessionFolder) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date());
        return new File(sessionFolder, currentIndex + "_audio_" + timeStamp + ".pcm").getAbsolutePath();
    }

    public String generateVideoFileName(File sessionFolder) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date());
        return new File(sessionFolder, currentIndex + "_video_" + timeStamp + ".mp4").getAbsolutePath();
    }
}
