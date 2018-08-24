package com.dy.dwvm_mt;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;

import com.dy.dwvm_mt.Comlibs.AvcDecoder;
import com.dy.dwvm_mt.Comlibs.AvcEncoder;
import com.dy.dwvm_mt.Comlibs.BaseActivity;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.Comlibs.LocalSetting;
import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.commandmanager.DY_AVPacketEventHandler;
import com.dy.dwvm_mt.commandmanager.MTLibUtils;
import com.dy.dwvm_mt.fragments.HomeFragment;
import com.dy.dwvm_mt.userview.DYImageButton;
import com.dy.dwvm_mt.utilcode.constant.PermissionConstants;
import com.dy.dwvm_mt.utilcode.util.ActivityUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.PhoneUtils;
import com.dy.dwvm_mt.utilcode.util.ScreenUtils;
import com.dy.dwvm_mt.utilcode.util.ToastUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/6/29.
 * PS: Not easy to write code, please indicate.
 */
public class DY_VideoPhoneActivity extends BaseActivity implements SurfaceHolder.Callback, DY_AVPacketEventHandler {

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
    private boolean m_IsDecoderStart = false;
    private boolean m_IsEncoderStart = false;

    // MT Library
    private I_MT_Prime m_mtoperator = null;

    // encoder
    private AvcEncoder encodeVideoThread = null;
    // decoder
    private AvcDecoder decodeVideoThread = null;

   /* IntentFilter intentFilter = null;
    HomeFragment.AutoStartCamera receiver = null;
    private int m_pageOpenType = 0;*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videophone);
        ButterKnife.bind(this);
        //setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.e("TEST", "获取到摄像头的使用.");
            //init(barcodeScannerView, getIntent(), null);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);//1 can be another integer
        }

        m_mtoperator = MTLibUtils.getBaseMTLib();
        startAll();

        //m_surfaceDecoderShow.setZOrderOnTop(true);
        m_surfaceDecoderShow.getHolder().setFormat(PixelFormat.TRANSLUCENT);//设置画布  背景透明
        m_surfaceDecoderShow.getHolder().addCallback(this);
        //m_surfaceDecoderShow.getHolder().setFixedSize(480, 680);
        m_surfaceCameraPreview.setZOrderOnTop(true);
        m_surfaceCameraPreview.getHolder().setFormat(PixelFormat.TRANSLUCENT);//设置画布  背景透明
        m_surfaceCameraPreview.getHolder().addCallback(this);

        m_btn_endcall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Class<TelephonyManager> clazz = TelephonyManager.class;//得到方法
                try {
                    stopAll();
                    PhoneUtils.telcomInvok(DY_VideoPhoneActivity.this, "endCall");
                    Intent intent = new Intent(DY_VideoPhoneActivity.this, MTMainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    ActivityUtils.startActivity(intent);
                } catch (Exception ex) {
                    LogUtils.e("initPhoneStateListener" + ex.toString());
                }
            }
        });
        PhoneUtils.setSpeakerphoneOn(DY_VideoPhoneActivity.this, m_btn_freehand.isSelected());
        m_btn_freehand.setOnCheckedChangeListener(new DYImageButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(DYImageButton buttonView, boolean isChecked) {
                PhoneUtils.setSpeakerphoneOn(DY_VideoPhoneActivity.this, m_btn_freehand.isSelected());
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isInit = false;
        stopAll();
    }

    public void startAll() {
        if (isInit == false) {
            setup_mtLib();
            // open camera
            isInit = true;
        }
    }

    public void stopAll() {
        try {
            //getActivity().unregisterReceiver(receiver);
            encoderStop();
            decoderStop();
        } catch (Exception es) {
            LogUtils.e("HomeFragment stopAll() error :" + es.toString());
        } finally {
            isInit = false;
        }
    }

    private void setup_mtLib() {
        if (m_mtoperator == null || m_mtoperator.isWorking() == false) {
          /*  try {
                if (!m_mtoperator.start(LocalSetting.getDeviceId(), CommandUtils.MTPORT, 1024 * 1024, 0, 1, 1, "")) {
                    LogUtils.e("MTLib.start() failed !");
                    return;
                }
                LogUtils.e("setup_mtLib .start !");
            } catch (Exception e) {
                LogUtils.e("MTLib.start() error: " + e.getMessage());
                return;
            }
            m_mtoperator.setDeviceName(LOCAL_DEVICE_NAME);
            m_mtoperator.addReceivedVideoHandler(this);*/

            ToastUtils.showLong("请首先运行程序并登陆.");
        }
    }

    private boolean encoderStart(SurfaceView surfaceView) {
        // create encoder
        LogUtils.d("encoderStart begining...");
        try {
            if (encodeVideoThread == null) {
                encodeVideoThread = new AvcEncoder();
                encodeVideoThread.setMTLib(m_mtoperator);
                encodeVideoThread.changeRemoter(REMOTE_DEVICE_ID, REMOTE_DEVICE_IP);
                encodeVideoThread.cameraStart();
                encodeVideoThread.startPerViewer(surfaceView);
                encodeVideoThread.start();
            }
            return true;
        } catch (Exception es) {
            LogUtils.e("encoderStart error", es.toString());
            return false;
        }
    }

    private void encoderStop() {
        try {
            if (encodeVideoThread != null) {
                LogUtils.d("encoderStop.....");
                encodeVideoThread.endEncoder();
                encodeVideoThread = null;
            }
        } catch (Exception es) {
            LogUtils.e("encoderStop error", es.toString());
        }
    }

    private boolean decoderStart() {
        try {
            LogUtils.d("decoderStart begining...");
            decodeVideoThread = new AvcDecoder(m_surfaceDecoderShow);
            decodeVideoThread.start();
            MTLibUtils.addRecvedAVFrameListeners(this);
            return true;
        } catch (Exception es) {
            LogUtils.e("decoderStart error", es.toString());
            return false;
        } finally {
        }
    }

    private void decoderStop() {
        try {
            if (decodeVideoThread != null) {
                LogUtils.d("decoderStop.....");
                decodeVideoThread.decoderStop();
                decodeVideoThread = null;
            }
        } catch (Exception es) {
            LogUtils.e("decoderStop error", es.toString());
        } finally {
            MTLibUtils.removeRecvedAVFrameListeners(this);
        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        LogUtils.d("surfaceCreated");
        if (holder == (m_surfaceDecoderShow.getHolder())) {
            if (decoderStart() == false) {
                LogUtils.e("MT 打开解码：", "打开解码失败");
                return;
            } else {
                m_IsDecoderStart = true;
                LogUtils.e("MT 打开解码成功");
            }
        } else if (holder == (m_surfaceCameraPreview.getHolder())) {
            if (encoderStart(m_surfaceCameraPreview) == false) {
                LogUtils.e("MT 打开摄像头、编码：", "打开摄像头、编码失败");
                return;
            } else {
                m_IsEncoderStart = true;
                LogUtils.e("MT 打开摄像头、编码成功");
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public long onReceivedVideoFrame(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, int remoteEncoderChannelIndex, int localDecoderChannelIndex, long frameType, String videoCodec, int imageResolution, int width, int height, byte[] frameBuffer, int frameSize) {
        /* Log.d("mmmmmmmmmmmmmmttt", Arrays.toString(frameBuffer));*/
        //Log.d("onReceivedVideoFrames", localDecoderChannelIndex + videoCodec + width + height + "//" + frameSize + "//" + frameType + "//" + m_IsDecoderStart + "//" + isInit);
        if (localDecoderChannelIndex == 0 && m_IsDecoderStart == true && isInit == true) {
            decodeVideoThread.decoderOneVideoFrame(videoCodec, width, height, frameBuffer, frameSize, frameType);
        }
        return 1;
    }

    @Override
    public long onReceivedAudioFrame(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, int remoteEncoderChannelIndex, int localDecoderChannelIndex, String audioCodec, byte[] frameBuffer, int frameSize) {
        return 1;
    }
}
