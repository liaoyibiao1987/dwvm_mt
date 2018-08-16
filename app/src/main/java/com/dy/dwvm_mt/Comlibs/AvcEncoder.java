package com.dy.dwvm_mt.Comlibs;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.dy.dwvm_mt.MTLib;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class AvcEncoder extends Thread implements Camera.PreviewCallback {
    private static final int RAW_IMAGE_WIDTH_MIN = 640;
    private static final int RAW_IMAGE_HEIGHT_MIN = 360;
    private static final int RAW_IMAGE_WIDTH_MAX = 720;
    private static final int RAW_IMAGE_HEIGHT_MAX = 576;
    // === color format mapping table: Raw <--> Encode
    private static final int[] RAW_IMAGE_COLOR_TABLE = {ImageFormat.YUY2, ImageFormat.NV21, ImageFormat.YV12};
    private static final int[] RAW_IMAGE_BITS_TABLE = {16, 12, 12};
    private static final int[] ENCODE_INPUT_COLOR_TABLE = {MediaCodecInfo.CodecCapabilities.COLOR_FormatYCbYCr,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar};
    // === current raw-format
    private int m_iColorFormatIndex = -1;
    private static final int M_YUVQUEUESIZE = 10;

    private MediaCodec mEncoder;
    private boolean isRuning = true;
    private boolean needEncoding = true;
    private byte[] input = null;
    private byte[] m_encodeFrameBuffer = null;

    private long pts = 0;
    private long generateIndex = 0;
    private int m_framerate = 20;
    private int TIMEOUT_USEC = 12000;


    private int m_iRawWidth;
    private int m_iRawHeight;

    private long m_ldeviceID;
    private String m_sremoteIPPort;


    private I_MT_Prime m_mtLib;
    //private final AutoResetEvent are = new AutoResetEvent(false); //Semaphore

    // Camera
    private Camera m_cam = null;
    private byte[] m_rawBuffer = null;

    private Object m_yuvlocker = new Object();
    public ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<byte[]>(M_YUVQUEUESIZE);

    public interface RemoteUpdateEventListener extends EventListener {
        void handleUpdateTargetEvent(long deviceID, String IPPort);
    }

    public interface ReceivedYUVDataEventListener extends EventListener {
        void handleYUVDataEvent(byte[] buffer);
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

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime(long frameIndex) {
        return frameIndex * 1000000 / m_framerate;
    }

    /**
     * NV21ToNV12 将NV21数据转为NV12
     *
     * @param nv21
     * @param nv12
     * @param width
     * @param height
     */
    private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int frameSize = width * height;
        System.arraycopy(nv21, 0, nv12, 0, frameSize);
        /*for (int i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }*/
        for (int j = 0; j < frameSize / 2; j += 2) {
            nv12[frameSize + j - 1] = nv21[j + frameSize];
        }
        for (int j = 0; j < frameSize / 2; j += 2) {
            nv12[frameSize + j] = nv21[j + frameSize - 1];
        }
    }

    public AvcEncoder() {
        LogUtils.w("AvcEncoder initialization");
    }

    /**
     * 设置了mtLib就会用mtLib去发送编码后的包，如果没设置就只是编码不发送.
     *
     * @param mtLib
     */
    public void setMTLib(I_MT_Prime mtLib) {
        if (mtLib == null) {
            m_mtLib = mtLib;
        } else {
            m_mtLib.stop();
            m_mtLib = mtLib;
        }
    }

    private int getFaceCamera() {
        // check system camera number
        final int iCamNumber = Camera.getNumberOfCameras();
        if (iCamNumber <= 0) {
            LogUtils.d("Can not found camera !");
            return -1;
        }
        // find front camera
        int iCamIndex = -1;
        Camera.CameraInfo ci = new Camera.CameraInfo();
        for (int i = 0; i < iCamNumber; i++) {
            Camera.getCameraInfo(i, ci);
            if (Camera.CameraInfo.CAMERA_FACING_FRONT == ci.facing) {
                iCamIndex = i;
                break;
            }
        }
        return iCamIndex;
    }

    public boolean cameraStart() {
        if (m_cam != null) {
            // already opened
            return true;
        }
        int iCamIndex = getFaceCamera();
        // open first front camera
        try {
            if (iCamIndex < 0) {
                LogUtils.e("Can not find front-camera !");
                return false;
            }
            // open camera
            m_cam = Camera.open(iCamIndex);
            if (m_cam == null) {
                LogUtils.e("Failed to open camera #" + iCamIndex);
                return false;
            }
        } catch (Exception e) {
            if (m_cam != null) {
                m_cam.release();
                m_cam = null;
            }
            LogUtils.e("Camera error: " + e.getMessage());
            return false;
        }

        // set camera options
        try {
            Camera.Parameters camParams = m_cam.getParameters();
            // enum all preview size, and set to VGA or D1
            List<Camera.Size> previewSizes = camParams.getSupportedPreviewSizes();
            for (int i = 0; i < previewSizes.size(); i++) {
                Camera.Size s = previewSizes.get(i);
                if (s.width >= RAW_IMAGE_WIDTH_MIN && s.width <= RAW_IMAGE_WIDTH_MAX &&
                        s.height >= RAW_IMAGE_HEIGHT_MIN && s.height <= RAW_IMAGE_HEIGHT_MAX) {
                    camParams.setPreviewSize(s.width, s.height);
                    camParams.setPictureSize(s.width, s.height);
                    break;
                }
            }
            // enum all preview format, and set to RAW_IMAGE_COLOR_TABLE (yuy2,nv21,yv12)
            List<Integer> previewFormats = camParams.getSupportedPreviewFormats();
            LogUtils.w(" Camera previewFormats: " + Arrays.toString(previewFormats.toArray()));
            if (previewFormats.size() > 0) {
                m_iColorFormatIndex = -1;
                for (int r = 0; r < RAW_IMAGE_COLOR_TABLE.length && m_iColorFormatIndex < 0; r++) {
                    for (int i = 0; i < previewFormats.size(); i++) {
                        Integer iFormat = previewFormats.get(i);
                        if (iFormat == RAW_IMAGE_COLOR_TABLE[r]) {
                            m_iColorFormatIndex = r;
                            camParams.setPreviewFormat(iFormat);
                            break;
                        }
                    }
                }
                LogUtils.w(" Camera previewFormats set: " + Arrays.toString(previewFormats.toArray()));
                if (m_iColorFormatIndex < 0) {
                    LogUtils.e("Camera NOT support YUV color!");
                    m_cam.release();
                    m_cam = null;
                    return false;
                }
            }
            // other
            camParams.setFlashMode("off");
            camParams.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            camParams.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            camParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            // setting
            m_cam.setParameters(camParams);
            m_cam.setDisplayOrientation(180);

            // get resolution
            Camera.Size res = camParams.getPreviewSize();
            m_iRawWidth = res.width;
            m_iRawHeight = res.height;
        } catch (Exception e) {
            m_cam.release();
            m_cam = null;
            LogUtils.e("Camera set param failed: " + e.getMessage());
            return false;
        }
        return true;
    }

    public boolean startPerViewer(SurfaceView m_surfaceCameraPreview) {
        // start preview & capture
        try {
            // bind to viewer
            SurfaceHolder rawSurfaceHolder = m_surfaceCameraPreview.getHolder();
            if (rawSurfaceHolder != null) {
                //rawSurfaceHolder.addCallback(this); // if call on onCreate(), add this line and implements SurfaceHolder.Callback
                //rawSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                m_cam.setPreviewDisplay(rawSurfaceHolder);
                m_cam.setDisplayOrientation(90);
            }
            // malloc buffer
            m_rawBuffer = new byte[m_iRawWidth * m_iRawHeight * 3 / 2];
            // add callback buffer
            m_cam.addCallbackBuffer(m_rawBuffer);
            m_cam.setPreviewCallbackWithBuffer(this);
            // start preview
            m_cam.startPreview();
            return true;
        } catch (Exception e) {
            m_cam.release();
            m_cam = null;
            LogUtils.e("Preview failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        //
        // callback from Camera: YUV raw data
        //
        // YUV raw data bytes
        final int iInputSize = (m_iRawWidth * m_iRawHeight * RAW_IMAGE_BITS_TABLE[m_iColorFormatIndex] / 8);
        if (bytes.length < iInputSize || iInputSize <= 0) {
            return;
        } else {
            // if camera is NV21(YUV420SP), and the encoder can only encode NV12, need swap U & V color
            if (m_iColorFormatIndex == ImageFormat.NV21) {  //ENCODE_INPUT_COLOR_TABLE[m_iColorFormatIndex] == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                byte[] yuv420sp = new byte[m_iRawWidth * m_iRawHeight * 3 / 2];
                //把待编码的视频帧转换为YUV420格式
                NV21ToNV12(bytes, yuv420sp, m_iRawWidth, m_iRawHeight);
                bytes = yuv420sp;
            }
            synAddImageBuffer(bytes);
            if (m_cam != camera) {
                if (m_cam != null) {
                    m_cam.release();
                    m_cam = null;
                }
                m_cam = camera;
            }
            if (m_cam != null && m_rawBuffer != null) {
                m_cam.addCallbackBuffer(m_rawBuffer);
            }
        }
    }

    public final void changeRemoter(long deviceID, String remoteIP) {
        RemoteUpdateEventListener ml = getListenerInfo().mOnUpdateRemoteListener;
        if (ml != null) {
            ml.handleUpdateTargetEvent(deviceID, remoteIP);
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

    private final void synAddImageBuffer(byte[] buffer) {
        if (YUVQueue.size() >= 10) {
            YUVQueue.poll();
        }
        synchronized (m_yuvlocker) {
            YUVQueue.add(buffer.clone());
        }
    }

    public final void endEncoder() {
        isRuning = false;
        needEncoding = false;
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
        if (YUVQueue != null) {
            YUVQueue.clear();
            YUVQueue = null;
        }
    }

    @Override
    public void run() {
        try {
            mEncoder = MediaCodec.createEncoderByType(MTLib.CODEC_VIDEO_H264);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MTLib.CODEC_VIDEO_H264, m_iRawWidth, m_iRawHeight);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, m_iRawWidth * m_iRawHeight); // 1024 kbps
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, m_framerate);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2); // 2 seconds
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, m_iColorFormatIndex);
            mediaFormat.setInteger(MediaFormat.KEY_ROTATION, 90);

            mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            if (mEncoder != null) {
                mEncoder = null;
            }
            Log.e("AvcEncoder", "Encoder create error: " + e.getMessage());
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
                    LogUtils.e("AvcEncoder run error:" + es.toString());
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

    private void onSentEncoding() {
        if (YUVQueue.size() > 0) {
            input = YUVQueue.poll();
        }
        if (input != null) {
            try {
                ByteBuffer[] inputBuffers = mEncoder.getInputBuffers();//获取到输入缓冲区
                ByteBuffer[] outputBuffers = mEncoder.getOutputBuffers();//获取到输出缓冲区
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
                    byte[] tmp_encodeFrameBuffer = new byte[iEncodeFrameSize];
                    outputBuffer.get(tmp_encodeFrameBuffer);


                    mEncoder.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                    // send to network
                    if (m_mtLib != null && m_mtLib.isWorking()) {
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