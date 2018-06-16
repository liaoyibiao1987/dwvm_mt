package com.dy.dwvm_mt;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.dy.dwvm_mt.Comlibs.BaseActivity;
import com.dy.dwvm_mt.Comlibs.EncodeVideoThread;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class MainActivity extends BaseActivity implements MTLib.MTLibCallback, Camera.PreviewCallback {
    // parameters for MTLib demo
    private static final long LOCAL_DEVICE_ID = 0x04000009;
    private static final int LOCAL_UDP_PORT = 5004;
    private static final String LOCAL_DEVICE_NAME = "MT-Demo-Android";
    private static final long REMOTE_DEVICE_ID = 0x04000000;
    private String REMOTE_DEVICE_IP = "";

    /*非公有的变量前面要加上小写m，
        静态变量前面加上小写s，
        其它变量以小写字母开头，
        静态变量全大写。
        除了protected外，其它的带有m的变量在子类中是无法访问的。*/
    // parameters for camera preview, capture, encode
    // === raw image resolution range: 640x360 ~ 720x576
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

    // MT Library
    private final MTLib m_mtLib = new MTLib();

    // Camera
    private Camera m_cam = null;
    private byte[] m_rawBuffer = null;
    private int m_iRawWidth = 0;
    private int m_iRawHeight = 0;

    // encoder
    private MediaCodec m_encoder = null;
    private byte[] m_encodeFrameBuffer = null;
    private boolean m_encoderPauseSend = false;

    // decoder
    private MediaCodec m_decoder = null;
    private boolean m_decoderCreateFailed = false;
    private boolean m_decoderValid = false;
    private String m_decoderCodecName = "";
    private int m_decoderWidth = 0;
    private int m_decoderHeight = 0;
    private boolean m_decodeWaitKeyFrame = true;

    // UI controls SurfaceView
    private EditText m_editText = null;
    private TextView m_txtLog = null;
    private Button m_btnStart = null;
    private Button m_btnStop = null;
    private Button m_btnSendOnePacket = null;
    private SurfaceView m_surfaceCameraPreview = null;
    private SurfaceView m_surfaceDecoderShow = null;

    private static Object m_yuvlocker = new Object();
    private static int m_yuvqueuesize = 10;
    public static ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<byte[]>(m_yuvqueuesize);
    private EncodeVideoThread mPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get UI controls
        m_txtLog = (TextView) findViewById(R.id.txtLog);
        m_btnStart = (Button) findViewById(R.id.btnStart);
        m_btnStop = (Button) findViewById(R.id.btnStop);
        m_editText = (EditText) findViewById(R.id.editText);
        m_btnSendOnePacket = (Button) findViewById(R.id.btnSendOnePacket);
        m_surfaceCameraPreview = (SurfaceView) findViewById(R.id.surfaceCameraPreview);
        m_surfaceDecoderShow = (SurfaceView) findViewById(R.id.surfaceDecoderShow);

        // initialize UI
        setTitle(LOCAL_DEVICE_NAME);
        m_btnStart.setEnabled(true);
        m_btnStop.setEnabled(false);
        m_btnSendOnePacket.setEnabled(false);
        showLog("MT demo");

        // listen buttons click event
        m_btnStart.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickBtnStart();
            }
        });
        m_btnStop.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickBtnStop();
            }
        });
        m_btnSendOnePacket.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickBtnSendOnePacket();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.e("TEST", "获取到摄像头的使用.");
            //init(barcodeScannerView, getIntent(), null);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);//1 can be another integer
        }
    }

    @Override
    protected void onDestroy() {
        onClickBtnStop();
        super.onDestroy();
    }

    // show the string on UI 'txtLog' viewer
    private void showLog(String sz) {
        if (m_txtLog != null) {
            Time currTime = new Time();
            currTime.setToNow();
            String szShow = currTime.format("[%H:%M:%S] ");
            szShow += sz;
            m_txtLog.setText(szShow);
        }
    }

    // btnStart onClick event
    private void onClickBtnStart() {
        REMOTE_DEVICE_IP = m_editText.getText().toString();
        // MTLib start
        if (!m_mtLib.isWorking()) {
            m_mtLib.installCallback(this);
            try {
                if (!m_mtLib.start(LOCAL_DEVICE_ID, LOCAL_UDP_PORT, 1024 * 1024, 0, 1, 1, "")) {
                    showLog("MTLib.start() failed !");
                    return;
                }
            } catch (Exception e) {
                showLog("MTLib.start() error: " + e.getMessage());
                return;
            }
            m_mtLib.setDeviceName(LOCAL_DEVICE_NAME);
        }

        // open camera
        if (!cameraStart()) {
            onClickBtnStop();
            return;
        }

        // open encoder
        if (!encoderStart()) {
            onClickBtnStop();
            Log.e("MT 编码：", "打开编码失败");
            Toast.makeText(this, "打开编码失败", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mPlayer == null) {
            mPlayer = new EncodeVideoThread(m_mtLib, m_iRawWidth, m_iRawHeight, ENCODE_INPUT_COLOR_TABLE[m_iColorFormatIndex]);
            mPlayer.ChangeRemoter(REMOTE_DEVICE_ID, REMOTE_DEVICE_IP);
            mPlayer.start();
        }
        // update UI
        m_btnStart.setEnabled(false);
        m_btnStop.setEnabled(true);
        m_btnSendOnePacket.setEnabled(true);
    }

    // btnStop onClick event
    private void onClickBtnStop() {
        // close decoder
        decoderStop();

        // close encoder
        encoderStop();

        // close camera
        cameraStop();

        // MTLib stop
        if (m_mtLib.isWorking()) {
            try {
                m_mtLib.stop();
            } catch (Exception e) {
                showLog("MTLib.stop() error: " + e.getMessage());
                return;
            }
        }

        // update UI
        m_btnStart.setEnabled(true);
        m_btnStop.setEnabled(false);
        m_btnSendOnePacket.setEnabled(false);
    }

    // btnSendOnPacket onClick event
    private void onClickBtnSendOnePacket() {
        if (m_mtLib.isWorking()) {
            String szSend = "MTLib-Android Test Send One UDP Packet.\n";
            byte[] dataBuffer = szSend.getBytes();
            long sendResult = m_mtLib.sendUdpPacketToDevice(900, 0, REMOTE_DEVICE_ID, REMOTE_DEVICE_IP, dataBuffer, dataBuffer.length);
            showLog("Test send udp: result=" + sendResult);
        }
        m_encoderPauseSend = !m_encoderPauseSend;
    }

    // camera start preview & capture
    private boolean cameraStart() {
        if (m_cam != null) {
            // already opened
            return true;
        }

        // check system camera number
        final int iCamNumber = Camera.getNumberOfCameras();
        if (iCamNumber <= 0) {
            showLog("Can not found camera !");
            return false;
        }

        // open first front camera
        try {
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
            if (iCamIndex < 0) {
                showLog("Can not find front-camera !");
                return false;
            }


            // open camera
            m_cam = Camera.open(iCamIndex);
            if (m_cam == null) {
                showLog("Failed to open camera #" + iCamIndex);
                return false;
            }
        } catch (Exception e) {
            if (m_cam != null) {
                m_cam.release();
                m_cam = null;
            }
            showLog("Camera error: " + e.getMessage());
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
                if (m_iColorFormatIndex < 0) {
                    showLog("Camera NOT support YUV color!");
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
            // malloc buffer
            m_rawBuffer = new byte[m_iRawWidth * m_iRawHeight * 4 + 4096];
        } catch (Exception e) {
            m_cam.release();
            m_cam = null;
            showLog("Camera set param failed: " + e.getMessage());
            return false;
        }

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
            // add callback buffer
            m_cam.addCallbackBuffer(m_rawBuffer);
            m_cam.setPreviewCallbackWithBuffer(this);
            // start preview
            m_cam.startPreview();
        } catch (Exception e) {
            m_cam.release();
            m_cam = null;
            showLog("Preview failed: " + e.getMessage());
        }

        return true;
    }

    // camera stop preview & capture
    private void cameraStop() {
        if (m_cam != null) {
            m_cam.setPreviewCallbackWithBuffer(null);
            m_cam.stopPreview();
            m_cam.release();
            m_cam = null;
        }
    }

    public void putYUVData(byte[] buffer) {
        if (YUVQueue.size() >= 10) {
            YUVQueue.poll();
        }
        synchronized (m_yuvlocker) {
            YUVQueue.add(buffer);
        }
    }

    private void savaPreviewFrame(byte[] data, Camera camera) {
        putYUVData(data);
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

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        savaPreviewFrame(data, camera);
        return;

        /*
        //
        // callback from Camera: YUV raw data
        //
        // YUV raw data bytes
        final int iInputSize = (m_iRawWidth * m_iRawHeight * RAW_IMAGE_BITS_TABLE[m_iColorFormatIndex] / 8);
        if (data.length < iInputSize || iInputSize <= 0) {
            return;
        }

        // encode to H264
        if (m_encoder != null && !m_encoderPauseSend) {
            // input to encoder
            try {
                ByteBuffer[] inputBuffers = m_encoder.getInputBuffers();
                //dequeueInputBuffer 的参数表示等待的时间（毫秒），-1表示一直等，0表示不等。按常理传-1就行，但实际上在很多机子上会挂掉，没办法，还是传0吧，丢帧总比挂掉好。当然也可以传一个具体的毫秒数，不过没什么大意思吧
                //https://blog.csdn.net/halleyzhang3/article/details/11473961
                int inputBufferIndex = m_encoder.dequeueInputBuffer(1000000);
                Log.e("mtapp", "1: inputBufferIndex -> " + inputBufferIndex);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    Log.e("mtapp", "2: iInputSize -> " + iInputSize);
                    if (inputBuffer.remaining() < iInputSize) {
                        showLog("Encode input: " + iInputSize + ", remain: " + inputBuffer.remaining());
                        return;
                    } else {
                        // if camera is NV12, and encoder is YUV420SP, need swap U & V color
                        if (RAW_IMAGE_COLOR_TABLE[m_iColorFormatIndex] == ImageFormat.NV21 &&
                                ENCODE_INPUT_COLOR_TABLE[m_iColorFormatIndex] == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                            NV21_to_YUV420SP(data, m_iRawWidth, m_iRawHeight);
                        }
                        inputBuffer.put(data, 0, iInputSize);
                        Log.e("mtapp", "3: Encode input -> " + iInputSize + ", remain: " + inputBuffer.remaining());
                        m_encoder.queueInputBuffer(inputBufferIndex, 0, iInputSize, 0, 0);
                    }
                }
            } catch (Exception e) {
                Log.e("mtapp", "E1: Encode input failed: " + e.getMessage());
                showLog("Encode input failed: " + e.getMessage());
            }

            // get encoder output
            try {
                ByteBuffer[] outputBuffers = m_encoder.getOutputBuffers();
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = m_encoder.dequeueOutputBuffer(bufferInfo, 1000);
                Log.e("mtapp", "4: outputBufferIndex -> " + outputBufferIndex);
                while (outputBufferIndex >= 0) {
                    final int iEncodeFrameSize = bufferInfo.size;
                    //showLog("enc len " + iEncodeFrameSize);

                    // copy to buffer
                    if (m_encodeFrameBuffer == null || m_encodeFrameBuffer.length < iEncodeFrameSize) {
                        m_encodeFrameBuffer = new byte[(iEncodeFrameSize + 0xFFF) & (~0xFFF)];
                    }
                    outputBuffers[outputBufferIndex].get(m_encodeFrameBuffer);

                    // unlock
                    m_encoder.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = m_encoder.dequeueOutputBuffer(bufferInfo, -1);

                    Log.e("开始编码：", "" + m_encodeFrameBuffer.length);
                    // send to network
                    if (m_mtLib.isWorking()) {
                        m_mtLib.sendOneFrameToDevice(0, REMOTE_DEVICE_ID, 0, REMOTE_DEVICE_IP, MTLib.CODEC_VIDEO_H264,
                                m_encodeFrameBuffer, iEncodeFrameSize, MTLib.IMAGE_RESOLUTION_D1, m_iRawWidth, m_iRawHeight);
                    }
                }
            } catch (Exception e) {
                Log.e("mtapp", "E2: Encode input failed: " + e.toString());
                showLog("Encode output failed: " + e.toString());
            }
        }

        // start next callback
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

        */
    }

    private boolean encoderStart() {
        // create encoder
        try {
            m_encoder = MediaCodec.createEncoderByType(MTLib.CODEC_VIDEO_H264);
        } catch (Exception e) {
            if (m_encoder != null) {
                m_encoder = null;
            }
            showLog("Encoder create error: " + e.getMessage());
            return false;
        }

        // setting encode parameters
        try {
            int iFrameRate = m_cam.getParameters().getPreviewFrameRate();
            if (iFrameRate <= 0) {
                iFrameRate = 30;
            }
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MTLib.CODEC_VIDEO_H264, m_iRawWidth, m_iRawHeight);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 512000); // 512 kbps
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, iFrameRate);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2); // 2 seconds
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, ENCODE_INPUT_COLOR_TABLE[m_iColorFormatIndex]);
            mediaFormat.setInteger(MediaFormat.KEY_ROTATION, 90);

            m_encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            m_encoder.start();
        } catch (Exception e) {
            if (m_encoder != null) {
                m_encoder = null;
            }
            showLog("Encoder config error: " + e.getMessage());
            return false;
        }

        return true;
    }

    private void encoderStop() {
        if (m_encoder != null) {
            m_encoder.release();
            m_encoder = null;
        }
        if (m_encodeFrameBuffer != null) {
            m_encodeFrameBuffer = null;
        }
    }

    private boolean decoderStart(String codecName, int width, int height) {
        if (m_decoder != null) {
            return false;
        }

        // get surface & holder
        if (m_surfaceDecoderShow == null) {
            return false;
        }
        SurfaceHolder holder;
        try {
            holder = m_surfaceDecoderShow.getHolder();
        } catch (Exception e) {
            Log.e("decoderStart", "Get holder error: " + e.getMessage());
            return false;
        }
        if (holder == null) {
            Log.e("decoderStart", "Get holder failed.");
            return false;
        }

        // create decoder
        try {
            m_decoder = MediaCodec.createDecoderByType(codecName);
        } catch (Exception e) {
            if (m_decoder != null) {
                m_decoder = null;
            }
            Log.e("decoderStart", "Decoder create error: " + e.getMessage());
            return false;
        }

        // bind to surface, and start
        try {
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(codecName, width, height);
            m_decoder.configure(mediaFormat, holder.getSurface(), null, 0);
            m_decoder.start();
        } catch (Exception e) {
            Log.e("decoderStart", "surfaceCreated(decoder) error: " + e.getMessage());
            return false;
        }

        m_decoderCodecName = codecName;
        m_decoderWidth = width;
        m_decoderHeight = height;
        m_decodeWaitKeyFrame = true;
        m_decoderValid = true;
        return true;
    }

    private void decoderStop() {
        m_decoderCreateFailed = true;
        m_decoderValid = false;
        if (m_decoder != null) {
            m_decoder.stop();
            m_decoder.release();
            m_decoder = null;
        }
        m_decoderCreateFailed = false;
    }

    private boolean decoderOneVideoFrame(String codecName, int width, int height, byte[] dataBuffer, int dataSize, long frameType) {
        // if no decoder, create it
        if (m_decoder == null) {
            // only try one to create decoder
            if (m_decoderCreateFailed) {
                return false;
            }
            if (!decoderStart(codecName, width, height)) {
                m_decoderCreateFailed = true;
                return false;
            }
            m_decoderCreateFailed = false;
        }
        if (!m_decoderValid) {
            return false;
        }

        // if codec or image-resolution changed, NOT process it
        // maybe, you can test: decoderStop() then decoderStart()
        if (!m_decoderCodecName.equalsIgnoreCase(codecName) || m_decoderWidth != width || m_decoderHeight != height) {
            return false;
        }

        // wait key-frame at first
        if (m_decodeWaitKeyFrame) {
            if (0 != frameType) //0- I frame, 1- P frame
            {
                return true;
            } else {
                m_decodeWaitKeyFrame = false;
            }
        }

        // decode frame
        try {
            ByteBuffer[] decodeInputBuffers = m_decoder.getInputBuffers();
            int decodeInputBufferIndex = m_decoder.dequeueInputBuffer(-1);
            if (decodeInputBufferIndex >= 0) {
                ByteBuffer inputBuffer = decodeInputBuffers[decodeInputBufferIndex];
                inputBuffer.clear();
                if (inputBuffer.remaining() < dataSize) {
                    Log.e("decoderStart", "Decode input: " + dataSize + ", remain: " + inputBuffer.remaining());
                    return false;
                }
                inputBuffer.put(dataBuffer, 0, dataSize);
                m_decoder.queueInputBuffer(decodeInputBufferIndex, 0, dataSize, System.currentTimeMillis() * 1000, 0);
            }

            MediaCodec.BufferInfo decodeOutBufferInfo = new MediaCodec.BufferInfo();
            int decodeOutputBufferIndex = m_decoder.dequeueOutputBuffer(decodeOutBufferInfo, 0);
            while (decodeOutputBufferIndex >= 0) {
                m_decoder.releaseOutputBuffer(decodeOutputBufferIndex, true);
                decodeOutputBufferIndex = m_decoder.dequeueOutputBuffer(decodeOutBufferInfo, 0);
            }
        } catch (Exception e) {
            Log.e("decoderStart", "Decode failed: " + e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public long onReceivedUdpPacket(
            long localDeviceId,
            String remoteDeviceIpPort,
            long remoteDeviceId,
            long packetCommandType,
            byte[] packetBuffer,
            int packetBytes) {
        //
        // callback from MTLib: on received one udp packet
        //
        String szLog = "received udp packet: type=" + packetCommandType + ", len=" + packetBytes + ", from " + remoteDeviceIpPort;
        Log.d("MTDemo", szLog);
        return 1;
    }

    @Override
    public long onReceivedAudioFrame(
            long localDeviceId,
            String remoteDeviceIpPort,
            long remoteDeviceId,
            int remoteEncoderChannelIndex,
            int localDecoderChannelIndex,
            String audioCodec,
            byte[] frameBuffer,
            int frameSize) {
        //
        // callback from MTLib: on received one audio frame
        //
        String szLog = "received audio frame, len=" + frameSize + ", from " + remoteDeviceIpPort + ", codec=" + audioCodec;
        Log.d("MTDemo", szLog);
        return 1;
    }

    @Override
    public long onReceivedVideoFrame(
            long localDeviceId,
            String remoteDeviceIpPort,
            long remoteDeviceId,
            int remoteEncoderChannelIndex,
            int localDecoderChannelIndex,
            long frameType,
            String videoCodec,
            int imageResolution,
            int width,
            int height,
            byte[] frameBuffer,
            int frameSize) {
        //
        // callback from MTLib: on received one video frame
        //
        decoderOneVideoFrame(videoCodec, width, height, frameBuffer, frameSize, frameType);
        return 1;
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
}
