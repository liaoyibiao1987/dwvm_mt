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
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.dy.dwvm_mt.Comlibs.AvcDecoder;
import com.dy.dwvm_mt.Comlibs.AvcEncoder;
import com.dy.dwvm_mt.Comlibs.BaseActivity;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.Comlibs.LocalSetting;
import com.dy.dwvm_mt.MTMainActivity;
import com.dy.dwvm_mt.R;
import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.commandmanager.MTLibUtils;
import com.dy.dwvm_mt.userview.DYImageButton;
import com.dy.dwvm_mt.utilcode.util.ActivityUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.PhoneUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.dy.dwvm_mt.Comlibs.BaseActivity.MT_VP_PAGE_OPENTYPE;

public class HomeFragment extends Fragment implements Camera.PreviewCallback, I_MT_Prime.MTLibReceivedVideoHandler {
    /*@BindView(R.id.viewpager)
    ViewPager viewPager;

    @BindView(R.id.tablayout)
    TabLayout tabLayout;*/

    @BindView(R.id.surfaceCameraPreview)
    protected SurfaceView m_surfaceCameraPreview;
    @BindView(R.id.surfaceDecoderShow)
    protected SurfaceView m_surfaceDecoderShow;
    @BindView(R.id.btn_freehand)
    protected DYImageButton m_btn_freehand;
    @BindView(R.id.btn_endcall)
    protected ImageButton m_btn_endcall;

    // parameters for MTLib demo
    private static final String LOCAL_DEVICE_NAME = "MT-Android";
    /* private static final long REMOTE_DEVICE_ID = 0x2000006;
     private String REMOTE_DEVICE_IP = "112.91.151.186:5001";*/
    private static final long REMOTE_DEVICE_ID = 0x2000000;
    private String REMOTE_DEVICE_IP = LocalSetting.getPSIPPort();
    private boolean isInit = false;

    /*非公有的变量前面要加上小写m，
        静态变量前面加上小写s，
        其它变量以小写字母开头，
        静态变量全大写。
        除了protected外，其它的带有m的变量在子类中是无法访问的。*/
    // parameters for camera preview, capture, encode
    // === raw image resolution range: 640x360 ~ 720x576


    // MT Library
    private I_MT_Prime m_mtoperator = null;
    // Camera
    private Camera m_cam = null;
    private byte[] m_rawBuffer = null;
    private int m_iRawWidth = 0;
    private int m_iRawHeight = 0;

    // encoder
    private AvcEncoder encodeVideoThread = null;
    private AvcDecoder decodeVideoThread = null;

    // decoder
    private MediaCodec m_decoder = null;
    private boolean m_decoderCreateFailed = false;
    private boolean m_decoderValid = false;
    private String m_decoderCodecName = "";
    private int m_decoderWidth = 0;
    private int m_decoderHeight = 0;
    private boolean m_decodeWaitKeyFrame = true;

    HomeFragment.AutoStartCamera receiver = null;
    IntentFilter intentFilter = null;
    private int m_pageOpenType = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragments_home, container, false);
        ButterKnife.bind(this, rootView);
        if (ContextCompat.checkSelfPermission(this.getActivity(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.e("TEST", "获取到摄像头的使用.");
            //init(barcodeScannerView, getIntent(), null);
        } else {
            ActivityCompat.requestPermissions(this.getActivity(),
                    new String[]{Manifest.permission.CAMERA}, 1);//1 can be another integer
        }

        //m_surfaceDecoderShow.setZOrderOnTop(true);
        m_surfaceDecoderShow.getHolder().setFormat(PixelFormat.TRANSLUCENT);//设置画布  背景透明
        m_surfaceCameraPreview.setZOrderOnTop(true);
        m_surfaceCameraPreview.getHolder().setFormat(PixelFormat.TRANSLUCENT);//设置画布  背景透明

        m_btn_endcall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Class<TelephonyManager> clazz = TelephonyManager.class;//得到方法
                try {
                    PhoneUtils.telcomInvok(getContext(), "endCall");
                    Intent intent = new Intent(getContext(), MTMainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    ActivityUtils.startActivity(intent);
                } catch (Exception ex) {
                    LogUtils.e("initPhoneStateListener" + ex.toString());
                }
            }
        });
        PhoneUtils.setSpeakerphoneOn(getContext(), m_btn_freehand.isSelected());
        m_btn_freehand.setOnCheckedChangeListener(new DYImageButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(DYImageButton buttonView, boolean isChecked) {
                PhoneUtils.setSpeakerphoneOn(getContext(), m_btn_freehand.isSelected());
            }
        });
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
        isInit = false;
        m_mtoperator.removeReceivedVideoHandler(this);
        super.onDestroy();
    }


    public void startAll() {
        if (isInit == false) {
            setupM_mtLib();
            // open camera
            if (cameraStart() == false) {
                LogUtils.e("MT 打开摄像头：", "打开摄像头失败");
                return;
            }
            // open encoder
            encoderStart();

            isInit = true;
        }

    }

    public void stopAll() {
        try {
            getActivity().unregisterReceiver(receiver);
            cameraStop();
            encoderStop();
            decoderStop();
        } catch (Exception es) {
            LogUtils.e("HomeFragment stopAll() error :" + es.toString());
        }
    }

    private void setupM_mtLib() {
        if (m_mtoperator.isWorking() == false) {
            try {
                if (!m_mtoperator.start(LocalSetting.getDeviceId(), CommandUtils.MTPORT, 1024 * 1024, 0, 1, 1, "")) {
                    LogUtils.e("MTLib.start() failed !");
                    return;
                }
                LogUtils.e("setupM_mtLib .start !");
            } catch (Exception e) {
                LogUtils.e("MTLib.start() error: " + e.getMessage());
                return;
            }
            m_mtoperator.setDeviceName(LOCAL_DEVICE_NAME);
        }
        m_mtoperator.addReceivedVideoHandler(this);
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
        if (encodeVideoThread == null) {
            LogUtils.d("MT 编码：", "正在打开编码.");
            encodeVideoThread = new AvcEncoder(m_cam, m_iRawWidth, m_iRawHeight, ENCODE_INPUT_COLOR_TABLE[m_iColorFormatIndex]);
            encodeVideoThread.setMTLib(m_mtoperator);
            encodeVideoThread.changeRemoter(REMOTE_DEVICE_ID, REMOTE_DEVICE_IP);
            encodeVideoThread.start();
        }
        return true;
    }

    private void encoderStop() {
        encodeVideoThread.endEncoder();
        encodeVideoThread = null;
    }

    private boolean decoderStart(String codecName, int width, int height) {
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
        if (decodeVideoThread == null) {
            LogUtils.d("MT 解码：" + "正在打开解码");
            decodeVideoThread = new AvcDecoder(holder);
        }
        if (decodeVideoThread.decoderStart(codecName, width, height) == true) {
            decodeVideoThread.
        } else {
            return false;
        }
    }

    private void decoderStop() {
        if (decodeVideoThread != null) {
            decodeVideoThread.decoderStop();
        }
    }

    private MediaCodec.BufferInfo decodeOutBufferInfo;
    private ByteBuffer[] decodeInputBuffers;
    private Object decoderLocker = new Object();

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
            decoderStop();
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
            synchronized (decoderLocker) {
                decodeInputBuffers = m_decoder.getInputBuffers();
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
                decodeOutBufferInfo = new MediaCodec.BufferInfo();
                int decodeOutputBufferIndex = m_decoder.dequeueOutputBuffer(decodeOutBufferInfo, 0);
                while (decodeOutputBufferIndex >= 0) {
                    m_decoder.releaseOutputBuffer(decodeOutputBufferIndex, true);
                    decodeOutputBufferIndex = m_decoder.dequeueOutputBuffer(decodeOutBufferInfo, 0);
                }
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
            // if camera is NV21(YUV420SP), and the encoder can only encode NV12, need swap U & V color
            if (RAW_IMAGE_COLOR_TABLE[m_iColorFormatIndex] == ImageFormat.NV21) {  //ENCODE_INPUT_COLOR_TABLE[m_iColorFormatIndex] == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                byte[] yuv420sp = new byte[m_iRawWidth * m_iRawHeight * 3 / 2];
                //把待编码的视频帧转换为YUV420格式
                NV21ToNV12(data, yuv420sp, m_iRawWidth, m_iRawHeight);
                data = yuv420sp;
            }

            //data = yuv_rotate90(data, m_iRawWidth, m_iRawHeight);
            savePreviewFrame(data, camera);
        }
    }

    private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
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
    public void onReceivedVideoFrames(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, int remoteEncoderChannelIndex, int localDecoderChannelIndex, long frameType, String videoCodec, int imageResolution, int width, int height, byte[] frameBuffer, int frameSize) {
        if (localDecoderChannelIndex == 0) {
            decoderOneVideoFrame(videoCodec, width, height, frameBuffer, frameSize, frameType);
        }
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
