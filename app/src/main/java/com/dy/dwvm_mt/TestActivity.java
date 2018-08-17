package com.dy.dwvm_mt;

import android.os.Bundle;
import android.service.carrier.CarrierService;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.dy.dwvm_mt.Comlibs.AvcDecoder;
import com.dy.dwvm_mt.Comlibs.AvcEncoder;
import com.dy.dwvm_mt.Comlibs.BaseActivity;
import com.dy.dwvm_mt.Comlibs.DataPackShell;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.Comlibs.LocalSetting;
import com.dy.dwvm_mt.commandmanager.AnalysingUtils;
import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.commandmanager.DY_onReceivedPackEventHandler;
import com.dy.dwvm_mt.commandmanager.MTLibUtils;
import com.dy.dwvm_mt.commandmanager.NWCommandEventArg;
import com.dy.dwvm_mt.commandmanager.NWCommandEventHandler;
import com.dy.dwvm_mt.messagestructs.s_loginResultDDNS;
import com.dy.dwvm_mt.messagestructs.s_messageBase;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.javastruct.JavaStruct;
import com.dy.javastruct.StructClass;

import java.lang.reflect.Array;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/6/28.
 * PS: Not easy to write code, please indicate.
 */
public class TestActivity extends BaseActivity implements NWCommandEventHandler, I_MT_Prime.MTLibReceivedVideoHandler {

    @BindView(R.id.btn_test_login)
    Button btn_testlogin;

    @BindView(R.id.btn_test_reportstate)
    Button btn_testreportstate;

    @BindView(R.id.btn_test_opencamare)
    Button btn_testopencamare;

    @BindView(R.id.btn_test_encoder)
    Button btn_testencoder;

    @BindView(R.id.surface_test_encoder)
    protected SurfaceView m_surfacetestencoder;

    @BindView(R.id.surface_test_decoder)
    protected SurfaceView m_surfacetestdecoder;

    private AvcEncoder avcEncoder = null;
    private AvcDecoder avcDncoder = null;

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

        m_surfacetestencoder.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                avcEncoder.setMTLib(MTLibUtils.getBaseMTLib());
                avcEncoder.changeRemoter(LocalSetting.getDeviceId(), "127.0.0.1:5008");
                avcEncoder.cameraStart();
                avcEncoder.startPerViewer(m_surfacetestencoder);
                avcEncoder.start();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        m_surfacetestdecoder.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                startDecoder(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        btn_testopencamare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                avcEncoder.setMTLib(MTLibUtils.getBaseMTLib());
                avcEncoder.changeRemoter(0x04000000, "172.16.0.144:5004");
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

    public void startDecoder(SurfaceHolder holder) {
      /*  MTLibUtils.getBaseMTLib().addReceivedVideoHandler(this);
        avcDncoder = new AvcDecoder(holder);*/
    }

    @Override
    public void onReceivedVideoFrames(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, int remoteEncoderChannelIndex, int localDecoderChannelIndex, long frameType, String videoCodec, int imageResolution, int width, int height, byte[] frameBuffer, int frameSize) {
        if (localDecoderChannelIndex == 0) {
            avcDncoder.decoderOneVideoFrame(videoCodec, width, height, frameBuffer, frameSize, frameType);
        }
    }

    @Override
    public void doHandler(NWCommandEventArg arg) {
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
}
