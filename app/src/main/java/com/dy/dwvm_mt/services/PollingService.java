package com.dy.dwvm_mt.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.dy.dwvm_mt.Comlibs.BaseActivity;
import com.dy.dwvm_mt.Comlibs.EncoderManager;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.Comlibs.LocalSetting;
import com.dy.dwvm_mt.Comlibs.LoginExtMessageDissector;
import com.dy.dwvm_mt.DY_VideoPhoneActivity;
import com.dy.dwvm_mt.TestActivity;
import com.dy.dwvm_mt.broadcasts.NetworkChangeReceiver;
import com.dy.dwvm_mt.commandmanager.AnalysingUtils;
import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.commandmanager.MTLibUtils;
import com.dy.dwvm_mt.commandmanager.NWCommandEventArg;
import com.dy.dwvm_mt.commandmanager.NWCommandEventHandler;
import com.dy.dwvm_mt.messagestructs.s_DDNS_StatesMsg;
import com.dy.dwvm_mt.messagestructs.s_loginResultDDNS;
import com.dy.dwvm_mt.messagestructs.s_messageBase;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.ToastUtils;

public class PollingService extends Service implements NWCommandEventHandler, NetworkChangeReceiver.OnNetWorkChange {

    private static int m_interval = 0;
    private static boolean m_isOnline = false;

    @Override
    public void onCreate() {
        //android.os.Debug.waitForDebugger();
        /*if (isRunning == false) {
            try {
                isRunning = true;
                initFloatView();
                initPhoneStateListener();
                Thread.sleep(1000);
            } catch (Exception es) {
                LogUtils.e("CallShowService onCreate error " + es.toString());
                isRunning = false;
                return;
            }
        }
*/
        AnalysingUtils.addRecvedCommandListeners(this);
        NetworkChangeReceiver.getInstance().setOnNetWorkChange(this);
        EncoderManager.Init(MTLibUtils.getBaseMTLib());
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (m_isOnline == true && m_interval > 0) {
                            CommandUtils.sendPolling();
                            //LogUtils.d("Send polling package.");
                            Thread.sleep(m_interval);
                        } else {
                            Thread.sleep(200);
                        }
                    } catch (Exception es) {
                        LogUtils.e("PollingService error :" + es);
                    }
                }
            }
        }).start();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        NetworkChangeReceiver.getInstance().delOnNetWorkChange(this);
        super.onDestroy();
    }

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (intent!=null){
                m_interval = intent.getIntExtra(CommandUtils.Str_Extra_Polling, -1);
                m_isOnline = intent.getBooleanExtra(CommandUtils.Str_Extra_Online, false);
            }else {
                LogUtils.w("PollingService -> onStartCommand (Intent is null)");
            }
        } catch (Exception es) {
            LogUtils.e("onStartCommand error", es.toString());
        }
        return super.onStartCommand(intent, START_STICKY, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static final void ReWriteInformation(final int ddnsID, final String ddnsIP, final LoginExtMessageDissector.LoginExtMessage loginExtMessage) {
        try {
            final int localDeviceID = loginExtMessage.getDeviceId();
            CommandUtils.setDDNSDEVICEID(ddnsID);
            CommandUtils.setDDNSIPPort(ddnsIP);
            I_MT_Prime mtlib = MTLibUtils.getBaseMTLib();
            mtlib.resetDeviceID(localDeviceID);

            LocalSetting.SetInformationByLoginResult(loginExtMessage);
        } catch (Exception es) {
            LogUtils.e("ReWriteInformation error:" + es);
        }
    }

    @Override
    public void doNWCommandHandler(NWCommandEventArg arg) {
        if (arg != null && arg.getEventArg() != null) {
            int cmd = arg.getEventArg().getCmd();
            switch (cmd) {
                case s_messageBase.DeviceCMD.WVM_CMD_USER_BASE:
                    onReceivedSubCMD(arg);
                    break;
                case s_messageBase.DeviceCMD.WVM_CMD_DDNS_LOGIN_RESULT:
                    try {
                        s_loginResultDDNS loginResult = arg.getEventArg().Param(s_loginResultDDNS.class);
                        LogUtils.d("Device ID：" + loginResult.getDwDeviceId());
                        if (loginResult.getDwErrorCode() == 0) {
                            ToastUtils.showShort("登录成功");
                            int ddnsID = arg.getEventArg().getHeader().dwSrcId;
                            String ddnsIP = arg.getEventArg().getIPPort();
                            LoginExtMessageDissector.LoginExtMessage loginExtMessage = LoginExtMessageDissector.getLoginExtMessage(loginResult);
                            ReWriteInformation(ddnsID, ddnsIP, loginExtMessage);
                        } else {
                            LocalSetting.ResetInformation();
                            ToastUtils.showShort("登录失败");
                        }
                    } catch (Exception es) {
                        LogUtils.e("PollingService WVM_CMD_DDNS_LOGIN_RESULT: Analytic package error :" + es);
                    }
                    break;
            }

        }
    }

    private void onReceivedSubCMD(NWCommandEventArg arg) {
        try {
            int subCMD = arg.getEventArg().getSubCmd();
            switch (subCMD) {
                case s_messageBase.DeviceCMD_Sub.DDNS_StatesMsg:
                    s_DDNS_StatesMsg s_statesMsg = arg.getEventArg().Param(s_DDNS_StatesMsg.class);
                    if (s_statesMsg.Types == s_messageBase.DDNS_StatesMsg.ReLogin) {
                        String loginID = LocalSetting.getLoginID();
                        String loginPSW = LocalSetting.getLoginPSW();
                        String telNumber = LocalSetting.getTelNumber();
                        CommandUtils.sendLoginData(loginID, loginPSW, telNumber, "", CommandUtils.getDDNSIPPort());
                        LogUtils.d(String.format("开始重新登录 ID: %s  PSW: %s  TEL: %s  ZONE: %s  DDNSIPPor: %S", loginID, loginPSW, telNumber, "", CommandUtils.getDDNSIPPort()));
                    } else if (s_statesMsg.Types == s_messageBase.DDNS_StatesMsg.LoginOffline) {
                        LogUtils.d("设备已在另一处登录,被迫下线。请检查电脑是否拥有多个网络地址!");
                    }
                    break;
                case s_messageBase.DeviceCMD_Sub.DDNS_MTInfo:
                    try {
                        Thread.sleep(2000);
                        LogUtils.d("通知设备进入视频电话.");
                        Intent intent = new Intent(getBaseContext(), DY_VideoPhoneActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .putExtra(BaseActivity.MT_VP_PAGE_OPENTYPE, BaseActivity.MT_VIDEOPHONE_STARTUPTYPE_OFFHOOK);
                        getApplication().startActivity(intent);
                    } catch (Exception es) {
                        LogUtils.e("PollingService onReceivedSubCMD error:" + es.toString());
                    }

                    break;
                case s_messageBase.DeviceCMD_Sub.DDNS_SetCoding://ddns通知MT打开或关闭编码器(打开只有在摘机的时候才能操作)
                    EncoderManager.Manage(arg);
                default:
                    break;
            }

        } catch (Exception es) {
            LogUtils.e("PollingService WVM_CMD_USER_BASE: Analytic package error :" + es);
        }

    }

    @Override
    public void onNetStateChange(int wifi, int mobile, int none, int oldStatus, int newStatus) {
        LogUtils.d("网络状态变化,重新登录.");
        String loginID = LocalSetting.getLoginID();
        String loginPSW = LocalSetting.getLoginPSW();
        String telNumber = LocalSetting.getTelNumber();
        if (newStatus == none) {
            //没有网络
        }
        if (newStatus == mobile) {
            //移动网络
            //CommandUtils.sendLoginData(loginID, loginPSW, telNumber, "", CommandUtils.getDDNSIPPort());
        }
        if (newStatus == wifi) {
            //CommandUtils.sendLoginData(loginID, loginPSW, telNumber, "", CommandUtils.getDDNSIPPort());
            //wifi网络
            if (oldStatus == mobile) {
                //从移动网络切换到wifi网络
            }
        }

    }
}
