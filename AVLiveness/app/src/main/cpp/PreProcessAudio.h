//
// Created by ethan on 15/7/2024.
//

#ifndef PREPROCESSAUDIO_H
#define PREPROCESSAUDIO_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

extern "C" JNIEXPORT jdoubleArray Java_com_msra_avliveness_PreProcessAudio_readWavFile(JNIEnv *env, jobject thiz, jstring file_path);
// Declare other JNI functions here...

#ifdef __cplusplus
}
#endif

#endif // PREPROCESSAUDIO_H
