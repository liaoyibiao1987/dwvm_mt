package com.dy.dwvm_mt;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.dy.dwvm_mt.comlibs.AvcDecoder;
import com.dy.dwvm_mt.comlibs.AvcEncoder;
import com.dy.dwvm_mt.comlibs.BaseActivity;
import com.dy.dwvm_mt.comlibs.LocalSetting;
import com.dy.dwvm_mt.commandmanager.AnalysingUtils;
import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.commandmanager.DY_AVPacketEventHandler;
import com.dy.dwvm_mt.commandmanager.MTLibUtils;
import com.dy.dwvm_mt.commandmanager.NWCommandEventArg;
import com.dy.dwvm_mt.commandmanager.NWCommandEventHandler;
import com.dy.dwvm_mt.messagestructs.s_loginResultDDNS;
import com.dy.dwvm_mt.messagestructs.s_messageBase;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/6/28.
 * PS: Not easy to write code, please indicate.
 */
public class TestActivity extends BaseActivity implements NWCommandEventHandler, View.OnClickListener, DY_AVPacketEventHandler, SurfaceHolder.Callback {

    @BindView(R.id.btn_test_login)
    Button btn_testlogin;

    @BindView(R.id.btn_test_reportstate)
    Button btn_testreportstate;

    @BindView(R.id.btn_test_opencamare)
    Button btn_testopencamare;

    @BindView(R.id.btn_test_encoder)
    Button btn_testencoder;

    @BindView(R.id.btn_test_decoder)
    protected Button btn_testdecoder;

    @BindView(R.id.surface_test_encoder)
    protected SurfaceView m_surfacetestencoder;

    @BindView(R.id.surface_test_decoder)
    protected TextureView m_surfacetestdecoder;

    @BindView(R.id.btn_setSpeakerON1)
    protected Button btnsetSpeakerON1;

    @BindView(R.id.btn_setSpeakerON2)
    protected Button btnsetSpeakerON2;

    @BindView(R.id.btn_setSpeakerON3)
    protected Button btnsetSpeakerON3;

    @BindView(R.id.btn_test_appendBuffs)
    protected Button btntestappendBuffs;


    private AvcEncoder avcEncoder = null;
    private AvcDecoder avcDecoder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        ButterKnife.bind(this);
        AnalysingUtils.addRecvedCommandListeners(this);
        btn_testlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ddnsIPAndPort = CommandUtils.getDDNSIPPort();
                CommandUtils.sendLoginData("L_MT5", "123", "3850203", "860756", ddnsIPAndPort);
            }
        });
        avcEncoder = new AvcEncoder();
        btn_testreportstate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommandUtils.sendTelState(s_messageBase.TelStates.CalledOffhook, 0, null);
            }
        });

        btn_testdecoder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDecoder();
            }
        });

        btnsetSpeakerON1.setOnClickListener(this);
        btnsetSpeakerON2.setOnClickListener(this);
        btnsetSpeakerON3.setOnClickListener(this);
        btntestappendBuffs.setOnClickListener(this);

        m_surfacetestencoder.getHolder().addCallback(this);
        //m_surfacetestdecoder.getHolder().addCallback(this);

        btn_testopencamare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                avcEncoder.setMTLib(MTLibUtils.getBaseMTLib());
                avcEncoder.changeRemoter(LocalSetting.getDeviceId(), "127.0.0.1:" + CommandUtils.MTPORT);
                //avcEncoder.changeRemoter(0x04000000, "172.16.0.144:5004");
                avcEncoder.cameraStart();
            }
        });
        btn_testencoder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                avcEncoder.startPerViewer(m_surfacetestencoder);
                avcEncoder.start();
            }
        });
    }

    public void startDecoder() {
        avcDecoder = new AvcDecoder(m_surfacetestdecoder.getSurfaceTexture());
        avcDecoder.start();
        MTLibUtils.addRecvedAVFrameListeners(this);
    }

    @Override
    public void doNWCommandHandler(NWCommandEventArg arg) {
        if (arg != null && arg.getEventArg() != null) {
            int cmd = arg.getEventArg().getCmd();
            switch (cmd) {
                case s_messageBase.DeviceCMD.WVM_CMD_DDNS_LOGIN_RESULT:
                    try {
                        s_loginResultDDNS loginResult = arg.getEventArg().Param(s_loginResultDDNS.class);
                        LogUtils.d("Device ID：" + loginResult.getDwDeviceId());
                    } catch (Exception es) {
                        LogUtils.e("TestActivity : Analytic package error :" + es);
                    }
                    break;
            }

        }
    }

    @Override
    public void onClick(View v) {
        final AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        if (v.getId() == R.id.btn_setSpeakerON1) {
            LogUtils.d("设置手机外放", am.isSpeakerphoneOn());
            am.setMode(AudioManager.MODE_IN_CALL);
            am.setSpeakerphoneOn(!am.isSpeakerphoneOn());
        } else if (v.getId() == R.id.btn_setSpeakerON2) {
            LogUtils.d("设置手机外放2", am.isSpeakerphoneOn());
            final Handler mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    am.setMode(AudioManager.MODE_IN_CALL);
                    am.setSpeakerphoneOn(true);
                    LogUtils.d("设置手机外放2", am.isSpeakerphoneOn());
                }
            }, 500);
        } else if (v.getId() == R.id.btn_setSpeakerON3) {
            LogUtils.d("设置手机外放3", am.isSpeakerphoneOn());
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getMode() != AudioManager.MODE_IN_CALL) {
                audioManager.setMode(AudioManager.MODE_IN_CALL);
            }
            try {
                Class clazz = Class.forName("android.media.AudioSystem");
                Method m = clazz.getMethod("setForceUse", new Class[]{int.class, int.class});
                m.setAccessible(true);
                m.invoke(null, 1, 1);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            if (!audioManager.isSpeakerphoneOn()) {
                audioManager.setSpeakerphoneOn(true);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (avcEncoder != null) {
            avcEncoder.endEncoder();
            avcEncoder = null;
        }
        if (avcDecoder != null) {
            avcDecoder.decoderStop();
            avcDecoder = null;
        }
        AnalysingUtils.removeRecvedCommandListeners(this);
        super.onDestroy();
    }

    @Override
    public long onReceivedVideoFrame(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, int remoteEncoderChannelIndex, int localDecoderChannelIndex, long frameType, String videoCodec, int imageResolution, int width, int height, byte[] frameBuffer, int frameSize) {
        if (localDecoderChannelIndex == 0 && avcDecoder != null) {
            avcDecoder.decoderOneVideoFrame(videoCodec, width, height, frameBuffer, frameSize, frameType);
        }
        return 1;
    }

    @Override
    public long onReceivedAudioFrame(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, int remoteEncoderChannelIndex, int localDecoderChannelIndex, String audioCodec, byte[] frameBuffer, int frameSize) {
        return 1;
    }

    @Override
    public void onBackPressed() {
        if (avcEncoder != null) {
            avcEncoder.endEncoder();
            avcEncoder = null;
        }
        if (avcDecoder != null) {
            MTLibUtils.removeRecvedAVFrameListeners(null);
            avcDecoder.decoderStop();
            avcDecoder = null;
        }
        super.onBackPressed();
        System.out.println("按下了back键   onBackPressed()");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
       /* if (holder == (m_surfacetestdecoder.getHolder())) {
            if (avcDecoder != null) {
                avcDecoder.decoderStop();
                avcDecoder = null;
            }

        } else*/ if (holder == (m_surfacetestencoder.getHolder())) {
            if (avcEncoder != null) {
                avcEncoder.endEncoder();
                avcEncoder = null;
            }
        }
    }
}
