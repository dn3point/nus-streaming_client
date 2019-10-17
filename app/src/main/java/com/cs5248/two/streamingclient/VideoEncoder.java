package com.cs5248.two.streamingclient;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

public class VideoEncoder {
    public static final String LOG_TAG = VideoEncoder.class.getSimpleName();

    private static final String FORMAT = "video/avc";
    private static final int KEY_FRAME = 1;
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
    private static final String DIR_NAME = "MediaCodec";
    private static final int TIMEOUT_USEC = 12000;

    private int mWidth;
    private int mHeight;
    private int mFps;
    private int mBitRate;

    private MediaCodec mediaCodec;
    private BufferedOutputStream outputStream;
    private byte[] configbyte;

    private boolean isRuning = false;

    public VideoEncoder(int width, int height, int fps, int bitRate) {
        mWidth = width;
        mHeight = height;
        mFps = fps;
        mBitRate = bitRate;
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(FORMAT, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFps);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, KEY_FRAME);
        try {
            mediaCodec = MediaCodec.createEncoderByType(FORMAT);
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
        createFile();
    }

    private void createFile() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), DIR_NAME);
        dir.mkdirs();
        if (dir.canWrite()) {
            File file = new File(dir, now() + ".mp4");
            Log.i(LOG_TAG, file.toString());
            if (file.exists()) {
                file.delete();
            }
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(file));
            } catch (FileNotFoundException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    }

    private String now() {
        final GregorianCalendar now = new GregorianCalendar();
        return SIMPLE_DATE_FORMAT.format(now.getTime());
    }

    private void stopEncoder() {
        try {
            mediaCodec.stop();
            mediaCodec.release();
        } catch (IllegalStateException e){
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public void stopThread(){
        isRuning = false;
        try {
            stopEncoder();
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public void startEncoderThread(){
        Thread encoderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                isRuning = true;
                byte[] input = null;
                long pts =  0;
                long generateIndex = 0;

                while (isRuning) {
                    if (MainActivity.YUVQueue.size() > 0) {
                        input = MainActivity.YUVQueue.poll();
                        byte[] yuv420sp = new byte[mWidth * mHeight * 3 / 2];
                        nv21ToNv12(input, yuv420sp, mWidth, mHeight);
                        input = yuv420sp;
                    }
                    if (input != null) {
                        try {
                            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                            if (inputBufferIndex >= 0) {
                                pts = computePresentationTime(generateIndex);
                                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                                inputBuffer.clear();
                                inputBuffer.put(input);
                                mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                                generateIndex += 1;
                            }

                            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                            while (outputBufferIndex >= 0) {
                                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                                byte[] outData = new byte[bufferInfo.size];
                                outputBuffer.get(outData);
                                if (bufferInfo.flags == 2) {
                                    configbyte = new byte[bufferInfo.size];
                                    configbyte = outData;
                                } else if (bufferInfo.flags == 1) {
                                    byte[] keyframe = new byte[bufferInfo.size + configbyte.length];
                                    System.arraycopy(configbyte, 0, keyframe, 0, configbyte.length);
                                    System.arraycopy(outData, 0, keyframe, configbyte.length, outData.length);

                                    outputStream.write(keyframe, 0, keyframe.length);
                                } else {
                                    outputStream.write(outData, 0, outData.length);
                                }

                                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                            }
                        } catch (Exception e) {
                            Log.e(LOG_TAG, e.getMessage());
                        }
                    } else {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Log.e(LOG_TAG, e.getMessage());
                        }
                    }
                }
            }
        });
        encoderThread.start();
    }

    private void nv21ToNv12(byte[] nv21, byte[] nv12, int width, int height){
        if (nv21 == null || nv12 == null) {
            return;
        }
        int frameSize = width * height;
        System.arraycopy(nv21, 0, nv12, 0, frameSize);
        for (int i = 0; i < frameSize; i++) {
            nv12[i] = nv21[i];
        }
        for (int j = 0; j < frameSize / 2; j += 2) {
            nv12[frameSize + j - 1] = nv21[j + frameSize];
        }
        for (int j = 0; j < frameSize / 2; j += 2) {
            nv12[frameSize + j] = nv21[j + frameSize - 1];
        }
    }

    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / mFps;
    }
}
