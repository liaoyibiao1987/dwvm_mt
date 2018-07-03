package com.dy.dwvm_mt;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.Button;

import com.dy.dwvm_mt.Comlibs.BaseActivity;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.commandmanager.commandUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/6/28.
 * PS: Not easy to write code, please indicate.
 */
public class TestActivity extends BaseActivity implements I_MT_Prime.MTLibCallback {

    @BindView(R.id.btn_test_login)
    Button btn_testlogin;
    private I_MT_Prime m_mtLib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        ButterKnife.bind(this);
        StartMTLib();

        btn_testlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ddnsIPAndPort = commandUtils.DDNSIP + ":" + commandUtils.DDNSPORT;
                commandUtils.sendLoginData("L_MT6", "123", "3850203", "860756", ddnsIPAndPort);
            }
        });
    }

    private void StartMTLib() {
        try {
            m_mtLib = getM_mtLib();
            if (m_mtLib.isWorking() == false) {
                m_mtLib.installCallback(this);
                if (!m_mtLib.start(0x04000009, commandUtils.MTPORT, 1024 * 1024, 0, 1, 1, "")) {
                    LogUtils.e("MTLib.start() failed !");
                    return;
                }
                m_mtLib.setDeviceName("MT");
            } else {
                LogUtils.d("MTLib is already started !");
            }
            commandUtils.initSetupAdapter(m_mtLib);

        } catch (Exception e) {
            LogUtils.e("MTLib.start() error: " + e.getMessage());
            return;
        }
        //m_mtLib.setDeviceName(LOCAL_DEVICE_NAME);
    }

    @Override
    public long onReceivedUdpPacket(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, long packetCommandType, byte[] packetBuffer, int packetBytes) {
        return 0;
    }

    @Override
    public long onReceivedVideoFrame(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, int remoteEncoderChannelIndex, int localDecoderChannelIndex, long frameType, String videoCodec, int imageResolution, int width, int height, byte[] frameBuffer, int frameSize) {
        return 0;
    }

    @Override
    public long onReceivedAudioFrame(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, int remoteEncoderChannelIndex, int localDecoderChannelIndex, String audioCodec, byte[] frameBuffer, int frameSize) {
        return 0;
    }
}