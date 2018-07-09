package com.dy.dwvm_mt;

import android.os.Bundle;
import android.service.carrier.CarrierService;
import android.view.View;
import android.widget.Button;

import com.dy.dwvm_mt.Comlibs.BaseActivity;
import com.dy.dwvm_mt.Comlibs.DataPackShell;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.commandmanager.AnalysingUtils;
import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.commandmanager.DY_onReceivedPackEventHandler;
import com.dy.dwvm_mt.commandmanager.MTLibUtils;
import com.dy.dwvm_mt.commandmanager.NWCommandEventArg;
import com.dy.dwvm_mt.commandmanager.NWCommandEventHandler;
import com.dy.dwvm_mt.messagestructs.NetWorkCommand;
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
public class TestActivity extends BaseActivity implements NWCommandEventHandler {

    @BindView(R.id.btn_test_login)
    Button btn_testlogin;
    private I_MT_Prime m_mtLib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        ButterKnife.bind(this);
        AnalysingUtils.addRecvedCommandListeners(this);
        btn_testlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ddnsIPAndPort = CommandUtils.DDNSIP + ":" + CommandUtils.DDNSPORT;
                CommandUtils.sendLoginData("L_MT6", "123", "3850203", "860756", ddnsIPAndPort);
            }
        });
    }

    @Override
    public void doHandler(NWCommandEventArg arg) {
        if (arg != null && arg.getEventArg() != null) {
            int cmd = arg.getEventArg().getCmd();
            switch (cmd) {
                case s_messageBase.DeviceCMD.WVM_CMD_DDNS_LOGIN_RESULT:
                    try {
                        s_loginResultDDNS loginResult =  arg.getEventArg().Param(s_loginResultDDNS.class);
                        LogUtils.d("Device ID：" + loginResult.getDwDeviceId());
                    } catch (Exception es) {
                        LogUtils.e("TestActivity : Analytic package error :" + es);
                    }
                    break;
            }

        }
    }
}
