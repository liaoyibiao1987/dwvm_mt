package com.dy.dwvm_mt.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.dy.dwvm_mt.Comlibs.BaseActivity;
import com.dy.dwvm_mt.Comlibs.EncodeVideoThread;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.MTLib;
import com.dy.dwvm_mt.R;
import com.dy.dwvm_mt.commandmanager.MTLibUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

import java.nio.ByteBuffer;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.dy.dwvm_mt.Comlibs.BaseActivity.MT_VP_PAGE_OPENTYPE;

public class HomeFragment extends Fragment implements Camera.PreviewCallback, MTLib.MTLibCallback {
    /*@BindView(R.id.viewpager)
    ViewPager viewPager;

    @BindView(R.id.tablayout)
    TabLayout tabLayout;*/

    @BindView(R.id.surfaceCameraPreview)
    protected SurfaceView m_surfaceCameraPreview;
    @BindView(R.id.surfaceDecoderShow)
    protected SurfaceView m_surfaceDecoderShow;

    // parameters for MTLib demo
    private static final long LOCAL_DEVICE_ID = 0x04000009;
    private static final int LOCAL_UDP_PORT = 5004;
    private static final String LOCAL_DEVICE_NAME = "MT-Demo-Android";
    private static final long REMOTE_DEVICE_ID = 0x04000000;
    private String REMOTE_DEVICE_IP = "172.16.0.144:5007";

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
    private I_MT_Prime m_mtoperator = null;
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

    private EncodeVideoThread encodeVideoThread = null;

    HomeFragment.AutoStartCamera receiver = null;
    IntentFilter intentFilter = null;
    private int m_pageOpenType = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragments_home, container, false);
        ButterKnife.bind(this, rootView);
        //baseActivity = (BaseActivity) getActivity();
        if (ContextCompat.checkSelfPermission(this.getActivity(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.e("TEST", "获取到摄像头的使用.");
            //init(barcodeScannerView, getIntent(), null);
        } else {
            ActivityCompat.requestPermissions(this.getActivity(),
                    new String[]{Manifest.permission.CAMERA}, 1);//1 can be another integer
        }

        m_surfaceDecoderShow.setZOrderOnTop(true);
        m_surfaceDecoderShow.getHolder().setFormat(PixelFormat.TRANSLUCENT);//设置画布  背景透明
        m_surfaceCameraPreview.setZOrderOnTop(true);
        m_surfaceCameraPreview.getHolder().setFormat(PixelFormat.TRANSLUCENT);//设置画布  背景透明

        /*TabsAdapter tabsAdapter = new TabsAdapter(getChildFragmentManager());
        tabsAdapter.addFragment(new DialTabFragment(1), "Favorite 1");
        tabsAdapter.addFragment(new DialTabFragment(2), "Favorite 2");
        viewPager.setAdapter(tabsAdapter);
        tabLayout.setupWithViewPager(viewPager);*/
        m_mtoperator = MTLibUtils.getBaseMTLib();

        if (getArguments() != null) {
            m_pageOpenType = getArguments().getInt(MT_VP_PAGE_OPENTYPE);
        }

        receiver = new HomeFragment.AutoStartCamera();//广播接受者实例
        intentFilter = new IntentFilter();
        intentFilter.addAction(BaseActivity.MT_AUTOSTARTCAMERA_ACTION);
        getActivity().registerReceiver(receiver, intentFilter);
        return rootView;
    }

    public int getOpenType() {
        return m_pageOpenType;
    }

    @Override
    public void onDestroy() {
        stopAll();
        super.onDestroy();
    }


    public void startAll() {
        setupM_mtLib();
        // open camera
        if (cameraStart() == false) {
            LogUtils.e("MT 打开摄像头：", "打开摄像头失败");
            //onClickBtnStop();
            return;
        }

        // open encoder
        if (encoderStart() == false) {
            //onClickBtnStop();
            LogUtils.e("MT 编码：", "打开编码失败");
            return;
        }
        if (encodeVideoThread == null) {
            encodeVideoThread = new EncodeVideoThread(m_mtoperator, m_iRawWidth, m_iRawHeight, ENCODE_INPUT_COLOR_TABLE[m_iColorFormatIndex]);
            encodeVideoThread.changeRemoter(REMOTE_DEVICE_ID, REMOTE_DEVICE_IP);
            encodeVideoThread.start();
        }
    }

    public void stopAll() {
        getActivity().unregisterReceiver(receiver);
        cameraStop();
        encoderStop();
        decoderStop();
    }

    private void setupM_mtLib() {
        if (!m_mtoperator.isWorking()) {
            m_mtoperator.installCallback(this);
            try {
                if (!m_mtoperator.start(LOCAL_DEVICE_ID, LOCAL_UDP_PORT, 1024 * 1024, 0, 1, 1, "")) {
                    LogUtils.e("MTLib.start() failed !");
                    return;
                }
            } catch (Exception e) {
                LogUtils.e("MTLib.start() error: " + e.getMessage());
                return;
            }
            m_mtoperator.setDeviceName(LOCAL_DEVICE_NAME);
        }
    }

    private boolean cameraStart() {
        if (m_cam != null) {
            // already opened
            return true;
        }

        // check system camera number
        final int iCamNumber = Camera.getNumberOfCameras();
        if (iCamNumber <= 0) {
            LogUtils.d("Can not found camera !");
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
            // malloc buffer
            m_rawBuffer = new byte[m_iRawWidth * m_iRawHeight * 4 + 4096];
        } catch (Exception e) {
            m_cam.release();
            m_cam = null;
            LogUtils.e("Camera set param failed: " + e.getMessage());
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
            LogUtils.e("Preview failed: " + e.getMessage());
        }

        return true;
    }

    private void cameraStop() {
        if (m_cam != null) {
            m_cam.setPreviewCallbackWithBuffer(null);
            m_cam.stopPreview();
            m_cam.release();
            m_cam = null;
        }
    }

    private boolean encoderStart() {
        // create encoder
        try {
            m_encoder = MediaCodec.createEncoderByType(MTLib.CODEC_VIDEO_H264);
        } catch (Exception e) {
            if (m_encoder != null) {
                m_encoder = null;
            }
            LogUtils.e("Encoder create error: " + e.getMessage());
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
            LogUtils.e("Encoder config error: " + e.getMessage());
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
            LogUtils.e("decoderStart", "Get holder error: " + e.getMessage());
            return false;
        }
        if (holder == null) {
            LogUtils.e("decoderStart", "Get holder failed.");
            return false;
        }

        // create decoder
        try {
            m_decoder = MediaCodec.createDecoderByType(codecName);
        } catch (Exception e) {
            if (m_decoder != null) {
                m_decoder = null;
            }
            LogUtils.e("decoderStart", "Decoder create error: " + e.getMessage());
            return false;
        }

        // bind to surface, and start
        try {
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(codecName, width, height);
            m_decoder.configure(mediaFormat, holder.getSurface(), null, 0);
            m_decoder.start();
        } catch (Exception e) {
            LogUtils.e("decoderStart", "surfaceCreated(decoder) error: " + e.getMessage());
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
    public void onPreviewFrame(byte[] data, Camera camera) {
        //
        // callback from Camera: YUV raw data
        //
        // YUV raw data bytes
        final int iInputSize = (m_iRawWidth * m_iRawHeight * RAW_IMAGE_BITS_TABLE[m_iColorFormatIndex] / 8);
        if (data.length < iInputSize || iInputSize <= 0) {
            return;
        } else {
            // if camera is NV12, and encoder is YUV420SP, need swap U & V color
            if (RAW_IMAGE_COLOR_TABLE[m_iColorFormatIndex] == ImageFormat.NV21 &&
                    ENCODE_INPUT_COLOR_TABLE[m_iColorFormatIndex] == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                NV21_to_YUV420SP(data, m_iRawWidth, m_iRawHeight);
            }
            savePreviewFrame(data, camera);
        }
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

    private void savePreviewFrame(byte[] data, Camera camera) {
        encodeVideoThread.addCallbackBuffer(data);
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
    public long onReceivedUdpPacket(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, long packetCommandType, byte[] packetBuffer, int packetBytes) {
        return 0;
    }

    @Override
    public long onReceivedVideoFrame(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, int remoteEncoderChannelIndex, int localDecoderChannelIndex, long frameType, String videoCodec, int imageResolution, int width, int height, byte[] frameBuffer, int frameSize) {
        decoderOneVideoFrame(videoCodec, width, height, frameBuffer, frameSize, frameType);
        return 1;
    }

    @Override
    public long onReceivedAudioFrame(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, int remoteEncoderChannelIndex, int localDecoderChannelIndex, String audioCodec, byte[] frameBuffer, int frameSize) {
        return 0;
    }


    public class AutoStartCamera extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (HomeFragment.this.getOpenType() == BaseActivity.MT_VIDEOPHONE_STARTUPTYPE_CALLING ||
                    HomeFragment.this.getOpenType() == BaseActivity.MT_VIDEOPHONE_STARTUPTYPE_OFFHOOK) {
                HomeFragment.this.startAll();
            }
        }
    }
}
