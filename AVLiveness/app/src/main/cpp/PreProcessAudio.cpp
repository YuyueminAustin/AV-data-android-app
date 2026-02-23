//
// Created by ethan on 15/7/2024.
//

#include "PreProcessAudio.h"
#include <jni.h>
#include <string>
#include <vector>
#include <cmath>
#include <iostream>
#include <fstream>
#include <android/log.h>

#define LOG_TAG "PreProcessAudio"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

class PreProcessAudio {
public:
    std::vector<double> readWavFile(const std::string &filePath) {
        std::ifstream file(filePath, std::ios::binary);
        if (!file) {
            throw std::runtime_error("Cannot open file");
        }

        file.seekg(0, std::ios::end);
        size_t fileSize = file.tellg();
        file.seekg(0, std::ios::beg);

        std::vector<char> wavHeader(44);
        file.read(wavHeader.data(), 44);

        int sampleRate = *reinterpret_cast<int *>(wavHeader.data() + 24);
        int bitsPerSample = *reinterpret_cast<short *>(wavHeader.data() + 34);
        int byteRate = *reinterpret_cast<int *>(wavHeader.data() + 28);

        LOGD("Sample Rate: %d", sampleRate);
        LOGD("Bits Per Sample: %d", bitsPerSample);
        LOGD("Byte Rate: %d", byteRate);

        std::vector<char> audioBytes(fileSize - 44);
        file.read(audioBytes.data(), fileSize - 44);
        file.close();

        int totalFrames = audioBytes.size() / 2;
        std::vector<double> audioSamples(totalFrames);
        for (int i = 0; i < totalFrames; ++i) {
            short sample = *reinterpret_cast<short *>(audioBytes.data() + i * 2);
            audioSamples[i] = sample / 32768.0;
        }

        return audioSamples;
    }

    std::vector<double> highpassFilter(const std::vector<double> &signal, double cutoff, double sampleRate) {
        // Implement highpass filter
        // Placeholder implementation, replace with actual highpass filter logic
        return signal;
    }

    std::vector<double> generatePivotSignal(double duration, double frequency, double sampleRate) {
        int length = static_cast<int>(duration * sampleRate);
        std::vector<double> pivotSignal(length);
        for (int i = 0; i < length; ++i) {
            double t = i / sampleRate;
            pivotSignal[i] = sin(2 * M_PI * frequency * t);
        }
        return pivotSignal;
    }

    std::vector<double> syncSignal(const std::vector<double> &signal, const std::vector<double> &pivotSignal) {
        // Implement signal synchronization
        // Placeholder implementation, replace with actual synchronization logic
        return signal;
    }

    // Define additional methods as needed...
};

extern "C" JNIEXPORT jdoubleArray
Java_com_msra_avliveness_PreProcessAudio_readWavFile(JNIEnv *env, jobject thiz, jstring file_path) {
    const char *path = env->GetStringUTFChars(file_path, 0);
    PreProcessAudio processor;
    std::vector<double> audioSamples = processor.readWavFile(std::string(path));
    env->ReleaseStringUTFChars(file_path, path);

    jdoubleArray result = env->NewDoubleArray(audioSamples.size());
    env->SetDoubleArrayRegion(result, 0, audioSamples.size(), audioSamples.data());
    return result;
}

// Define JNI functions for other methods...
