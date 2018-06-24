package com.dy.dwvm_mt.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.dy.dwvm_mt.broadcasts.PhoneStateReceiver;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

public class CallShowService extends Service {

    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    public static boolean isRunning = false;
    private int phoneState;
    private boolean isEnable = true;
    private boolean isCalling = false;

    private String phoneNumber = "";

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        initPhoneStateListener();
        //callShowView = CallShowView.getInstance();
    }
    @Override
    public void onDestroy() {
        isRunning = false;
        sendBroadcast(new Intent(PhoneStateReceiver.AUTO_START_RECEIVER));
        super.onDestroy();
    }


    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {//去电
            phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            LogUtils.d("Calling..." + phoneNumber);
            isCalling = true;
        }
        return super.onStartCommand(intent, START_STICKY, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 初始化电话状态监听
     */
    private void initPhoneStateListener() {
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {

                super.onCallStateChanged(state, incomingNumber);
                phoneState = state;

                if (isEnable) {//启用
                    switch (state) {
                        case TelephonyManager.CALL_STATE_IDLE://待机时（即无电话时,挂断时会调用）
                            LogUtils.d("CALL_STATE_IDLE");
                            dismiss();//关闭来电秀
                            break;
                        case TelephonyManager.CALL_STATE_OFFHOOK://摘机（接听）
                            LogUtils.d("CALL_STATE_OFFHOOK");
                            callShow();//显示来电秀

                            break;
                        case TelephonyManager.CALL_STATE_RINGING://响铃(来电)
                            LogUtils.d("CALL_STATE_RINGING");
                            isCalling = false;
                            phoneNumber = incomingNumber;
                            LogUtils.d("incomingNumber:" + incomingNumber);//来电号码
                            callShow();//显示来电秀

                            break;

                        default:
                            break;
                    }
                }

            }

            private void callShow() {
            }

            private void dismiss() {
            }
        };
        //--------------------------

        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        //设置监听器
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

}
