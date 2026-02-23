package com.msra.avliveness.models;

import com.github.psambit9791.jdsp.filter.Butterworth;
import com.github.psambit9791.jdsp.signal.CrossCorrelation;
import com.github.psambit9791.jdsp.transform.ShortTimeFourier;
import com.github.psambit9791.jdsp.windows.Hamming;
import com.github.psambit9791.jdsp.windows._Window;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.nfc.Tag;
import android.util.Log;

import java.io.FileInputStream;
import java.util.Arrays;

public class PreProcessAudio {

    private static final String TAG = "PreProcessAudio";
    public final int[] TARGET_CENTER_FREQ_LIST = {17250, 18000, 18750, 19500, 20250, 21000, 21750, 22500};
    public ExtractSignalOffset processAudio(String filePath, double remove_first, double remove_last) {
        ExtractSignalOffset extractedSignalOffset = null;
        try {
            double[] audioBuffer = readWavFile(filePath);
            Log.d(TAG, "audio_buffer Length: " + audioBuffer.length);

            // Example signal processing tasks
            double[] filteredSignal = highpassFilter(audioBuffer, 12000, 48000);
            Log.d(TAG, "filtered signal length: " + filteredSignal.length);
            double[] pivotSignal = generatePivotSignal(0.5, 15000, 48000);
            Log.d(TAG, "pivot signal length: " + pivotSignal.length);
            double[] synchronizedSignal = syncSignal(audioBuffer, pivotSignal);
            Log.d(TAG, "sync signal length: " + synchronizedSignal.length);
            extractedSignalOffset = extractSignal(synchronizedSignal, 48000, pivotSignal.length, 8000, remove_first, remove_last);
            Log.d(TAG, "extracted signal offset: " + extractedSignalOffset);

            // Further processing and analysis
        } catch (IOException e) {
            e.printStackTrace();
        }
        return extractedSignalOffset;
    }

    public DopplerArrayResult processDoppler(double[] extractedSignal, int fs, int nperseg, int noverlap, int nfft, int freq_offset) {
        double[][][] magnitudesArray = new double[TARGET_CENTER_FREQ_LIST.length][][];
        double[][] freqArray = new double[TARGET_CENTER_FREQ_LIST.length][];
        double[][] shiftArray = new double[TARGET_CENTER_FREQ_LIST.length][];
        for (int i = 0; i < TARGET_CENTER_FREQ_LIST.length; i++) {
            int targetFreq = TARGET_CENTER_FREQ_LIST[i];
            Log.d(TAG, "extractedSignal length: " + extractedSignal.length);
            DopplerResult result = computeSpectrogram(extractedSignal, fs, nperseg, noverlap, nfft, freq_offset, targetFreq);
            // Save the results into arrays
            magnitudesArray[i] = result.filteredMagnitudes;
            freqArray[i] = result.filteredFrequencies;
            shiftArray[i] = result.dopplerShift;
        }
        return new DopplerArrayResult(freqArray, shiftArray, magnitudesArray);

    }

    private double[] highpassFilter(double[] signal, double cutoff, double sampleRate) {
        Butterworth butterworth = new Butterworth((int) sampleRate);
        return butterworth.highPassFilter(signal,5, cutoff);
    }

    private double[] generatePivotSignal(double duration, double frequency, double sampleRate) {
        int length = (int) (duration * sampleRate);
        double[] t = new double[length];
        for (int i = 0; i < length; i++) {
            t[i] = i / sampleRate;
        }
        double[] pivotSignal = new double[length];
        for (int i = 0; i < length; i++) {
            pivotSignal[i] = Math.sin(2 * Math.PI * frequency * t[i]);
        }
        double[] pivotSignalFloat = new double[length];
        for (int i = 0; i < length; i++) {
            pivotSignalFloat[i] = (float) pivotSignal[i];
        }
        return pivotSignalFloat;
    }

    private double[] syncSignal(double[] signal, double[] pivotSignal) {
        CrossCorrelation cc = new CrossCorrelation(signal, pivotSignal);
        double[] corr = cc.crossCorrelate();
        int lag = findLag(corr, pivotSignal.length);
        double[] synchronizedSignal = new double[signal.length];
        if (lag < 0) {
            System.arraycopy(signal, 0, synchronizedSignal, -lag, signal.length + lag);
        } else {
            System.arraycopy(signal, lag, synchronizedSignal, 0, signal.length - lag);
        }
        return synchronizedSignal;
    }

    private ExtractSignalOffset extractSignal(double[] signal, double sampleRate, int pivotLength, int offsetSamples, double remove_start, double remove_last ) {
        int startIndex = pivotLength + offsetSamples + (int) (sampleRate * remove_start);
        int endIndex = signal.length - (int) (sampleRate * remove_last);
        double[] extractedSignal = new double[endIndex - startIndex];
        System.arraycopy(signal, startIndex, extractedSignal, 0, extractedSignal.length);
        return new ExtractSignalOffset(extractedSignal, startIndex);
    }

    public static class ExtractSignalOffset {
        public double[] extractedSignal;
        public int startIndex;
        ExtractSignalOffset(double[] extractedSignal, int startIndex){
            this.extractedSignal = extractedSignal;
            this.startIndex = startIndex;
        }
    }

    public DopplerResult computeSpectrogram(double[] extractedSignal, int fs, int nperseg, int noverlap, int nfft,
                                         double freqOffset, double targetFreq) {
        _Window window = new Hamming(nfft);
        Log.d(TAG, "nperseg: " + nperseg);
        Log.d(TAG, "noverlap: " + noverlap);
        Log.d(TAG, "window: " + window);
        ShortTimeFourier stft = new ShortTimeFourier(extractedSignal, nperseg, noverlap, window);
        stft.transform();
        double[][] magnitude = stft.getMagnitude(false);
        double[] frequencies = stft.getFrequencyAxis(false);
        double[] times = stft.getTimeAxis();
        double targetFreqMin = targetFreq - freqOffset;
        double targetFreqMax = targetFreq + freqOffset;

        // Identify indices for the target frequency range
        int[] targetIndices = Arrays.stream(frequencies)
                .filter(f -> f >= targetFreqMin && f <= targetFreqMax)
                .mapToInt(f -> Arrays.binarySearch(frequencies, f))
                .toArray();

        // Extract the target frequency range
        double[] targetFrequencies = new double[targetIndices.length];
        double[][] targetZxx = new double[targetIndices.length][times.length];
        for (int i = 0; i < targetIndices.length; i++) {
            targetFrequencies[i] = frequencies[targetIndices[i]];
            for (int j = 0; j < times.length; j++) {
                targetZxx[i][j] = magnitude[targetIndices[i]][j];
            }
        }

        // Drop the central frequency bin and its adjacent 2 bins
        int centralBin = findClosestIndex(targetFrequencies, targetFreq);
        int[] binsToDrop = {centralBin - 1, centralBin, centralBin + 1};

        double[] filteredFrequencies = new double[targetFrequencies.length - binsToDrop.length];
        double[][] filteredZxx = new double[targetFrequencies.length - binsToDrop.length][times.length];
        int index = 0;
        for (int i = 0; i < targetFrequencies.length; i++) {
            int finalI = i;
            if (Arrays.stream(binsToDrop).noneMatch(x -> x == finalI)) {
                filteredFrequencies[index] = targetFrequencies[i];
                for (int j = 0; j < times.length; j++) {
                    filteredZxx[index][j] = targetZxx[i][j];
                }
                index++;
            }
        }

        // Shift the frequencies to Doppler shift
        double[] dopplerShift = new double[filteredFrequencies.length];
        for (int i = 0; i < filteredFrequencies.length; i++) {
            dopplerShift[i] = filteredFrequencies[i] - targetFreq;
        }

        double[][] filteredMagnitudes = filteredZxx;

        return new DopplerResult(filteredFrequencies, dopplerShift, filteredMagnitudes);
    }

    private static int findClosestIndex(double[] array, double value) {
        int closestIndex = -1;
        double closestDistance = Double.MAX_VALUE;
        for (int i = 0; i < array.length; i++) {
            double distance = Math.abs(array[i] - value);
            if (distance < closestDistance) {
                closestIndex = i;
                closestDistance = distance;
            }
        }
        return closestIndex;
    }

    private double[] readWavFile(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        byte[] wavHeader = new byte[44]; // WAV header size
        fis.read(wavHeader);

        int sampleRate = ByteBuffer.wrap(wavHeader, 24, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        int bitsPerSample = ByteBuffer.wrap(wavHeader, 34, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        int byteRate = ByteBuffer.wrap(wavHeader, 28, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();

        Log.d("Process", "Sample Rate: " + sampleRate);
        Log.d("Process", "Bits Per Sample: " + bitsPerSample);
        Log.d("Process", "Byte Rate: " + byteRate);

//        if (bitsPerSample != 16) {
//            throw new UnsupportedOperationException("Only 16-bit WAV files are supported.");
//        }

        byte[] audioBytes = new byte[(int) (file.length() - 44)];
        fis.read(audioBytes);
        fis.close();

        int totalFrames = audioBytes.length / 2;
        double[] audioSamples = new double[totalFrames];
        ByteBuffer buffer = ByteBuffer.wrap(audioBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < totalFrames; i++) {
            audioSamples[i] = buffer.getShort() / 32768.0f; // Normalize to [-1, 1]
        }

        return audioSamples;
    }

    private static int findLag(double[] correlation, int pivotSignalLength) {
        int maxIndex = 0;
        double max = Math.abs(correlation[0]);

        for (int i = 1; i < correlation.length; i++) {
            if (Math.abs(correlation[i]) > max) {
                max = Math.abs(correlation[i]);
                maxIndex = i;
            }
        }

        return maxIndex - pivotSignalLength + 1;
    }

    public static class DopplerResult {
        public final double[] filteredFrequencies;
        public final double[] dopplerShift;
        public final double[][] filteredMagnitudes;

        public DopplerResult(double[] filteredFrequencies, double[] dopplerShift, double[][] filteredMagnitudes) {
            this.filteredFrequencies = filteredFrequencies;
            this.dopplerShift = dopplerShift;
            this.filteredMagnitudes = filteredMagnitudes;
        }
    }

    public static class DopplerArrayResult {
        public final double[][] filteredFrequenciesArray;
        public final double[][] dopplerShiftArray;
        public final double[][][] filteredMagnitudesArray;

        public DopplerArrayResult(double[][] filteredFrequenciesArray, double[][] dopplerShiftArray, double[][][] filteredMagnitudesArray) {
            this.filteredFrequenciesArray = filteredFrequenciesArray;
            this.dopplerShiftArray = dopplerShiftArray;
            this.filteredMagnitudesArray = filteredMagnitudesArray;
        }
    }




}
