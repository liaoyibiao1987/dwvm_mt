package com.dy.dwvm_mt;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.dy.dwvm_mt.comlibs.AvcDecoder;
import com.dy.dwvm_mt.comlibs.AvcEncoder;
import com.dy.dwvm_mt.comlibs.BaseActivity;
import com.dy.dwvm_mt.comlibs.EnumPageState;
import com.dy.dwvm_mt.comlibs.I_MT_Prime;
import com.dy.dwvm_mt.comlibs.LocalSetting;
import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.commandmanager.DY_AVPacketEventHandler;
import com.dy.dwvm_mt.commandmanager.MTLibUtils;
import com.dy.dwvm_mt.userview.DYImageButton;
import com.dy.dwvm_mt.utilcode.util.ActivityUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.PhoneUtils;
import com.dy.dwvm_mt.utilcode.util.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/6/29.
 * PS: Not easy to write code, please indicate.
 */
public class DY_VideoPhoneActivity extends BaseActivity implements  DY_AVPacketEventHandler, TextureView.SurfaceTextureListener {

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    private class PhoneStateReceive extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                TelephonyManager tManager = (TelephonyManager) context
                        .getSystemService(Service.TELEPHONY_SERVICE);
                switch (tManager.getCallState()) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        DY_VideoPhoneActivity.this.m_btn_endcall.callOnClick();
                        break;
                }

            } catch (Exception es) {
                LogUtils.e("DY_VideoPhoneActivity PhoneStateReceive", es);
            }
        }
    }

    @BindView(R.id.surfaceCameraPreview)
    protected SurfaceView m_surfaceCameraPreview;
    @BindView(R.id.surfaceDecoderShow)
    protected TextureView m_surfaceDecoderShow;
    @BindView(R.id.btn_freehand)
    protected DYImageButton m_btn_freehand;
    @BindView(R.id.btn_endcall)
    protected ImageButton m_btn_endcall;

    private PhoneStateReceive mReceiver;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videophone);
        ButterKnife.bind(this);
        //setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.e("TEST", "获取到摄像头的使用.");
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);//1 can be another integer
        }
        registReceiver();

        m_mtoperator = MTLibUtils.getBaseMTLib();
        startAll();

        //m_surfaceDecoderShow.setZOrderOnTop(true);
        m_surfaceDecoderShow.setSurfaceTextureListener(this);

       /* m_surfaceDecoderShow.getHolder().setFormat(PixelFormat.TRANSLUCENT);//设置画布  背景透明
        m_surfaceDecoderShow.getHolder().addCallback(this);*/
        //m_surfaceDecoderShow.getHolder().setFixedSize(480, 680);
        m_surfaceCameraPreview.setZOrderOnTop(true);
        m_surfaceCameraPreview.getHolder().setFormat(PixelFormat.TRANSLUCENT);//设置画布  背景透明
        //m_surfaceCameraPreview.getHolder().addCallback(this);

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
                if (decodeVideoThread != null) {
                    decodeVideoThread.testswitch();
                }
                //PhoneUtils.setSpeakerphoneOn(DY_VideoPhoneActivity.this, m_btn_freehand.isSelected());
            }
        });
    }

    private void registReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        mReceiver = new PhoneStateReceive();
        this.registerReceiver(mReceiver, filter);
    }

    /**
     * 这个方法是当这个activity没有销毁的时候，人为的按下锁屏键，然后再启动这个Activity的时候会去调用
     *
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        /*if (!pm.isScreenOn()) {
            String msg = intent.getStringExtra("msg");
            textview.setText("又收到消息:" + msg);
            //点亮屏幕
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP |
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
            wl.acquire();
            wl.release();*/
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
            decodeVideoThread = new AvcDecoder(m_surfaceDecoderShow.getSurfaceTexture());
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
            MTLibUtils.removeRecvedAVFrameListeners(null);
        }

    }

    @Override
    protected void onPause() {
        if (isFinishing() == true) {
            unregisterReceiver(mReceiver);
            CommandUtils.PageState = EnumPageState.Normal;
            stopAll();
        }
        super.onPause();
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
    public long onReceivedAudioFrame(long localDeviceId, String remoteDeviceIpPort,
                                     long remoteDeviceId, int remoteEncoderChannelIndex, int localDecoderChannelIndex, String
                                             audioCodec, byte[] frameBuffer, int frameSize) {
        return 1;
    }
}
