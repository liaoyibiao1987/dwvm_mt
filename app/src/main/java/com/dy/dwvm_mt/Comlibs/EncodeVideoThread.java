package com.dy.dwvm_mt.Comlibs;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.dy.dwvm_mt.MTLib;

import java.nio.ByteBuffer;
import java.util.EventListener;
import java.util.concurrent.ArrayBlockingQueue;

public class EncodeVideoThread extends Thread {
    private MediaCodec encoder;
    private boolean isRuning = true;
    private boolean needEncoding = true;
    private byte[] input = null;
    private long pts = 0;
    private long generateIndex = 0;
    private int m_framerate = 30;
    private int TIMEOUT_USEC = 12000;


    private int m_iRawWidth;
    private int m_iRawHeight;
    private int m_iColorFormat;

    private long m_ldeviceID;
    private String m_sremoteIPPort;

    private I_MT_Prime m_mtLib;
    private final AutoResetEvent are = new AutoResetEvent(false);

    private Object m_yuvlocker = new Object();
    private int m_yuvqueuesize = 10;
    public ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<byte[]>(m_yuvqueuesize);


    public EncodeVideoThread(I_MT_Prime primeService, int rawWidth, int rawHeight, int colorFormat) {
        m_mtLib = primeService;
        m_iRawWidth = rawWidth;
        m_iRawHeight = rawHeight;
        m_iColorFormat = colorFormat;
    }

    private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int framesize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / m_framerate;
    }

    public interface RemoteUpdateEventListener extends EventListener {
        void handleEvent(long deviceID, String IPPort);
    }

    public interface ReceivedYUVDataEventListener extends EventListener {
        void handleEvent(byte[] buffer);
    }

    static class ListenerInfo {
        protected RemoteUpdateEventListener mOnUpdateRemoteListener;
        protected ReceivedYUVDataEventListener mOnReceivedYUVDataListener;
    }

    private ListenerInfo mListenerInfo;

    private ListenerInfo getListenerInfo() {
        if (mListenerInfo != null) {
            return mListenerInfo;
        }
        mListenerInfo = new ListenerInfo();
        return mListenerInfo;
    }

    public void setOnUpdateRemoteListener(RemoteUpdateEventListener l) {
        getListenerInfo().mOnUpdateRemoteListener = l;
    }

    public void removeOnUpdateRemoteListener() {
        getListenerInfo().mOnUpdateRemoteListener = null;
    }

    public void setOnReceivedYUVDataListener(ReceivedYUVDataEventListener l) {
        getListenerInfo().mOnReceivedYUVDataListener = l;
    }

    public void removeOnReceivedYUVDataListener() {
        getListenerInfo().mOnReceivedYUVDataListener = null;
    }

    public final void changeRemoter(long deviceID, String remoteIP) {
        RemoteUpdateEventListener ml = getListenerInfo().mOnUpdateRemoteListener;
        if (ml != null) {
            ml.handleEvent(deviceID, remoteIP);
        }
        if (remoteIP == null || remoteIP == null || remoteIP.trim().isEmpty() == true
                || remoteIP.trim().isEmpty() == true || remoteIP.trim().startsWith("0.0.0.0") == true) {
            needEncoding = false;
        } else {
            needEncoding = true;
        }
        m_ldeviceID = deviceID;
        m_sremoteIPPort = remoteIP;
    }

    public final void addCallbackBuffer(byte[] buffer) {
        if (YUVQueue.size() >= 10) {
            YUVQueue.poll();
        }
        synchronized (m_yuvlocker) {
            YUVQueue.add(buffer);
        }
    }

    @Override
    public void run() {
        try {
            encoder = MediaCodec.createEncoderByType(MTLib.CODEC_VIDEO_H264);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MTLib.CODEC_VIDEO_H264, m_iRawWidth, m_iRawHeight);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1024000); // 1024 kbps
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, m_framerate);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2); // 2 seconds
            //mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, ENCODE_INPUT_COLOR_TABLE[m_iColorFormatIndex]);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, m_iColorFormat);
            mediaFormat.setInteger(MediaFormat.KEY_ROTATION, 180);

            encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            if (encoder != null) {
                encoder = null;
            }
            Log.e("EncodeVideoThread", "Encoder create error: " + e.getMessage());
        }

        if (encoder == null) {
            Log.e("DecodeActivity", "Can't find video info!");
            return;
        }
        encoder.start();


        while (isRuning) {
            if (needEncoding == true) {
                if (YUVQueue.size() > 0) {
                    input = YUVQueue.poll();
                    byte[] yuv420sp = new byte[m_iRawWidth * m_iRawHeight * 3 / 2];
                    NV21ToNV12(input, yuv420sp, m_iRawWidth, m_iRawHeight);
                    input = yuv420sp;
                }
                if (input != null) {
                    try {
                        long startMs = System.currentTimeMillis();
                        ByteBuffer[] inputBuffers = encoder.getInputBuffers();
                        ByteBuffer[] outputBuffers = encoder.getOutputBuffers();
                        int inputBufferIndex = encoder.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            pts = computePresentationTime(generateIndex);
                            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                            inputBuffer.clear();
                            inputBuffer.put(input);
                            encoder.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                            generateIndex += 1;
                        }

                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        int outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                        while (outputBufferIndex >= 0) {
                            //Log.i("AvcEncoder", "Get H264 Buffer Success! flag = "+bufferInfo.flags+",pts = "+bufferInfo.presentationTimeUs+"");
                            final int iEncodeFrameSize = bufferInfo.size;
                            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                            byte[] tmp_encodeFrameBuffer = new byte[bufferInfo.size];
                            outputBuffer.get(tmp_encodeFrameBuffer);


                            encoder.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                            // send to network
                            if (m_mtLib.isWorking()) {
                                m_mtLib.sendOneFrameToDevice(0, m_ldeviceID, 0, m_sremoteIPPort, MTLib.CODEC_VIDEO_H264,
                                        tmp_encodeFrameBuffer, iEncodeFrameSize, MTLib.IMAGE_RESOLUTION_D1, m_iRawWidth, m_iRawHeight);
                            }
                        }

                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


/*
            ByteBuffer[] inputBuffers = decoder.getInputBuffers();
            ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean isEOS = false;
            long startMs = System.currentTimeMillis();
            while (!Thread.interrupted()) {
                if (!isEOS) {
                    int inIndex = decoder.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = inputBuffers[inIndex];
                        int sampleSize = extractor.readSampleData(buffer, 0);
                        if (sampleSize < 0) {
                            // We shouldn't stop the playback at this point, just pass the EOS
                            // flag to decoder, we will get it again from the
                            // dequeueOutputBuffer
                            Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }

                int outIndex = decoder.dequeueOutputBuffer(info, 10000);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                        outputBuffers = decoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d("DecodeActivity", "New format " + decoder.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        ByteBuffer buffer = outputBuffers[outIndex];
                        Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);

                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        decoder.releaseOutputBuffer(outIndex, true);
                        break;
                }

                // All decoded frames have been rendered, we can stop playing now
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
            }
            */

        encoder.stop();
        encoder.release();
    }
}