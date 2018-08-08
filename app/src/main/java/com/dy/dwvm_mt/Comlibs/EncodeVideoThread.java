package com.dy.dwvm_mt.Comlibs;

import android.graphics.ImageFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.dy.dwvm_mt.MTLib;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

import java.nio.ByteBuffer;
import java.util.EventListener;
import java.util.concurrent.ArrayBlockingQueue;

public class EncodeVideoThread extends Thread {
    private MediaCodec mEncoder;
    private boolean isRuning = true;
    private boolean needEncoding = true;
    private byte[] input = null;
    private byte[] m_encodeFrameBuffer = null;

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
        LogUtils.w(String.format("EncodeVideoThread colorFormat: %s rawWidth: %s rawHeight: %s", colorFormat, rawWidth, rawHeight));
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

    /*private void NV21ToNV12(byte[] nv12, byte[] nv21, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int framesize = width * height;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (int i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }
        for (int j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }
        for (int j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }*/

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
            YUVQueue.add(buffer.clone());
        }
    }

    public final void endEncoder() {
        isRuning = false;
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (input != null) {
            input = null;
        }
        if (m_encodeFrameBuffer != null) {
            m_encodeFrameBuffer = null;
        }
    }

    @Override
    public void run() {
        try {
            mEncoder = MediaCodec.createEncoderByType(MTLib.CODEC_VIDEO_H264);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MTLib.CODEC_VIDEO_H264, m_iRawWidth, m_iRawHeight);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, m_iRawWidth * m_iRawHeight * 5); // 1024 kbps
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, m_framerate);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2); // 2 seconds
            //mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, ENCODE_INPUT_COLOR_TABLE[m_iColorFormatIndex]);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, m_iColorFormat);
            mediaFormat.setInteger(MediaFormat.KEY_ROTATION, 90);

            mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            if (mEncoder != null) {
                mEncoder = null;
            }
            Log.e("EncodeVideoThread", "Encoder create error: " + e.getMessage());
        }

        if (mEncoder == null) {
            Log.e("DecodeActivity", "Can't find video info!");
            return;
        }
        mEncoder.start();


        while (isRuning) {
            if (needEncoding == true) {
                try {
                    onSentEncoding();
                } catch (Exception es) {
                    LogUtils.e("EncodeVideoThread run error:" + es.toString());
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
    }

    protected void NV21_to_YUV420SP(byte[] image, int width, int height) {
        byte tmp = 0;
        int uvBegin = width * height;
        int uvBytes = width * height / 2;
        for (int i = 0; i < uvBytes; i += 2) {
            tmp = image[uvBegin + i];
            image[uvBegin + i] = image[uvBegin + i + 1];
            image[uvBegin + i + 1] = tmp;
        }
    }

    private void onSentEncoding2() {
        final int iInputSize = (m_iRawWidth * m_iRawHeight * m_iColorFormat / 8);
        if (YUVQueue.size() > 0) {
            input = YUVQueue.poll();
        }
        if (input != null) {
            if (input.length < iInputSize || iInputSize <= 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return;
            }

            // input to mEncoder
            try {
                ByteBuffer[] inputBuffers = mEncoder.getInputBuffers();
                int inputBufferIndex = mEncoder.dequeueInputBuffer(-1);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    if (inputBuffer.remaining() < iInputSize) {
                        LogUtils.e("Encode input: " + iInputSize + ", remain: " + inputBuffer.remaining());
                        return;
                    } else {
                        // if camera is NV12, and mEncoder is YUV420SP, need swap U & V color
                        if (m_iColorFormat == ImageFormat.NV21 &&
                                m_iColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                            NV21_to_YUV420SP(input, m_iRawWidth, m_iRawHeight);
                        }
                        byte[] yuv420sp = new byte[m_iRawWidth * m_iRawHeight * 3 / 2];
                        NV21ToNV12(input, yuv420sp, m_iRawWidth, m_iRawHeight);
                        input = yuv420sp;

                        inputBuffer.put(input, 0, iInputSize);
                        mEncoder.queueInputBuffer(inputBufferIndex, 0, iInputSize, 0, 0);
                    }
                }
            } catch (Exception e) {
                LogUtils.e("Encode input failed: " + e.getMessage());
            }

            // get mEncoder output
            try {
                ByteBuffer[] outputBuffers = mEncoder.getOutputBuffers();
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mEncoder.dequeueOutputBuffer(bufferInfo, 0);
                while (outputBufferIndex >= 0) {
                    final int iEncodeFrameSize = bufferInfo.size;
                    // copy to buffer
                    if (m_encodeFrameBuffer == null || m_encodeFrameBuffer.length < iEncodeFrameSize) {
                        m_encodeFrameBuffer = new byte[(iEncodeFrameSize + 0xFFF) & (~0xFFF)];
                    }
                    outputBuffers[outputBufferIndex].get(m_encodeFrameBuffer);

                    // unlock
                    mEncoder.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mEncoder.dequeueOutputBuffer(bufferInfo, 0);

                    // send to network
                    if (m_mtLib.isWorking()) {
                        m_mtLib.sendOneFrameToDevice(0, m_ldeviceID, 0, m_sremoteIPPort, MTLib.CODEC_VIDEO_H264,
                                m_encodeFrameBuffer, iEncodeFrameSize, MTLib.IMAGE_RESOLUTION_D1, m_iRawWidth, m_iRawHeight);
                    }
                }
            } catch (Exception e) {
                LogUtils.e("Encode output failed: " + e.getMessage());
            }
        } else {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void onSentEncoding() {

        if (YUVQueue.size() > 0) {
            input = YUVQueue.poll();
            // 上一层已经将数据转为YUV420SP(NV12)
            byte[] yuv420sp = new byte[m_iRawWidth * m_iRawHeight * 3 / 2];
            NV21ToNV12(input, yuv420sp, m_iRawWidth, m_iRawHeight);
            input = yuv420sp;
        }
        if (input != null) {
            try {
                ByteBuffer[] inputBuffers = mEncoder.getInputBuffers();
                ByteBuffer[] outputBuffers = mEncoder.getOutputBuffers();
                int inputBufferIndex = mEncoder.dequeueInputBuffer(-1);
                if (inputBufferIndex >= 0) {
                    pts = computePresentationTime(generateIndex);
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    inputBuffer.put(input);
                    mEncoder.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                    generateIndex += 1;
                }

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                while (outputBufferIndex >= 0) {
                    //Log.i("AvcEncoder", "Get H264 Buffer Success! flag = "+bufferInfo.flags+",pts = "+bufferInfo.presentationTimeUs+"");
                    final int iEncodeFrameSize = bufferInfo.size;
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                    byte[] tmp_encodeFrameBuffer = new byte[bufferInfo.size];
                    outputBuffer.get(tmp_encodeFrameBuffer);


                    mEncoder.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
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
    }


}