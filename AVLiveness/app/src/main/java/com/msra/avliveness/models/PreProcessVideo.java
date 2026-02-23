//package com.msra.avliveness.models;
//
//import android.graphics.Bitmap;
//import android.media.MediaCodec;
//import android.media.MediaExtractor;
//import android.media.MediaFormat;
//import android.util.Log;
//
//import org.opencv.core.CvType;
//import org.opencv.core.Mat;
//import org.opencv.core.Size;
//import org.opencv.imgproc.Imgproc;
//import org.opencv.videoio.VideoCapture;
//import org.opencv.videoio.Videoio;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//
//
//public class PreProcessVideo {
//
//    private static final String TAG = "PreProcessVideo";
////    public int[][][][] extractFrames(String videoPath, int numFrames, Size frameSize) {
////        Log.d(TAG, "videoPath: " + videoPath);
////        File videoFile = new File(videoPath);
////        if (!videoFile.exists()) {
////            Log.e(TAG, "Video file does not exist: " + videoPath);
////            return new int[0][0][0][0];
////        }
////        VideoCapture cap = new VideoCapture(videoPath);
////        if (!cap.isOpened()) {
////            return new int[0][0][0][0];
////        }
////
////        int totalFrames = (int) cap.get(Videoio.CAP_PROP_FRAME_COUNT);
////        Log.d(TAG, "total frames: " + totalFrames);
////        int interval = (numFrames == 0) ? 1 : Math.max(totalFrames / numFrames, 1);
////        Log.d(TAG, "interval: " + interval);
////
////        int frameWidth = (int) frameSize.width;
////        int frameHeight = (int) frameSize.height;
////        int[][][][] frames = new int[numFrames][frameHeight][frameWidth][4]; // 4 for ARGB channels
////
////        int frameIndex = 0;
////        for (int i = 0; i < totalFrames && frameIndex < numFrames; i += interval) {
////            cap.set(Videoio.CAP_PROP_POS_FRAMES, i);
////            Mat frame = new Mat();
////            if (!cap.read(frame)) {
////                break;
////            }
////            Imgproc.resize(frame, frame, frameSize);
////            frames[frameIndex] = matToArray(frame);
////            frameIndex++;
////        }
////
////        cap.release();
////        return frames;
////    }
//public int[][][][] extractFrames(String videoPath, int numFrames, Size frameSize) throws IOException {
//    Log.d(TAG, "videoPath: " + videoPath);
//    MediaExtractor extractor = new MediaExtractor();
//    extractor.setDataSource(videoPath);
//
//    int videoTrackIndex = -1;
//    for (int i = 0; i < extractor.getTrackCount(); i++) {
//        MediaFormat format = extractor.getTrackFormat(i);
//        String mime = format.getString(MediaFormat.KEY_MIME);
//        if (mime.startsWith("video/")) {
//            videoTrackIndex = i;
//            break;
//        }
//    }
//
//    if (videoTrackIndex == -1) {
//        Log.e(TAG, "No video track found in file");
//        return new int[0][0][0][0];
//    }
//
//    extractor.selectTrack(videoTrackIndex);
//    MediaFormat format = extractor.getTrackFormat(videoTrackIndex);
//    int width = format.getInteger(MediaFormat.KEY_WIDTH);
//    int height = format.getInteger(MediaFormat.KEY_HEIGHT);
//    long duration = format.getLong(MediaFormat.KEY_DURATION);
//    int frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
//
//    int totalFrames = (int) (duration / 1000000 * frameRate);
//
//
//    Log.d(TAG, "width: " + width);
//    Log.d(TAG, "height: " + height);
//    Log.d(TAG, "duration: " + duration);
//    Log.d(TAG, "framerate: " + frameRate);
//    Log.d(TAG, "total frames: " + totalFrames);
//
//    MediaCodec codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
//    codec.configure(format, null, null, 0);
//    codec.start();
//
//    int frameWidth = (int) frameSize.width;
//    int frameHeight = (int) frameSize.height;
//    int[][][][] frames = new int[totalFrames][frameHeight][frameWidth][4]; // 4 for ARGB channels
//
//    int frameIndex = 0;
//    int interval = (int) (duration / (totalFrames * 1000000 / frameRate));
//    ByteBuffer[] inputBuffers = codec.getInputBuffers();
//    ByteBuffer[] outputBuffers = codec.getOutputBuffers();
//    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//
//    while (frameIndex < numFrames) {
//        int inputBufferIndex = codec.dequeueInputBuffer(10000);
//        if (inputBufferIndex >= 0) {
//            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
//            int sampleSize = extractor.readSampleData(inputBuffer, 0);
//            if (sampleSize < 0) {
//                codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                break;
//            } else {
//                codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.getSampleTime(), 0);
//                extractor.advance();
//            }
//        }
//
//        int outputBufferIndex = codec.dequeueOutputBuffer(info, 10000);
//        if (outputBufferIndex >= 0) {
//            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
//            byte[] frameData = new byte[info.size];
//            outputBuffer.get(frameData);
//            Mat frame = new Mat(height, width, CvType.CV_8UC3);
//            frame.put(0, 0, frameData);
//
//            // Resize frame to the desired size
//            Imgproc.resize(frame, frame, frameSize);
//            frames[frameIndex] = matToArray(frame);
//
//            frameIndex++;
//            codec.releaseOutputBuffer(outputBufferIndex, false);
//
//            if (frameIndex % interval != 0) {
//                extractor.advance();
//            }
//        }
//    }
//
//    codec.stop();
//    codec.release();
//    extractor.release();
//
//    return frames;
//}
//
//
//    private int[][][] matToArray(Mat mat) {
//        int width = mat.cols();
//        int height = mat.rows();
//        int[][][] array = new int[height][width][3]; // 3 for RGB channels
//        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        org.opencv.android.Utils.matToBitmap(mat, bmp);
//
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                int pixel = bmp.getPixel(x, y);
//                array[y][x][0] = (pixel >> 16) & 0xFF; // Red
//                array[y][x][1] = (pixel >> 8) & 0xFF;  // Green
//                array[y][x][2] = pixel & 0xFF;         // Blue
//            }
//        }
//        return array;
//    }
//
//}
