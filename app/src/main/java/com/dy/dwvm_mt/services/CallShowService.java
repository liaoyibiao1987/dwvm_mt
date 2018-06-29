package com.dy.dwvm_mt.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.dy.dwvm_mt.Comlibs.BaseActivity;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.DY_VideoPhoneActivity;
import com.dy.dwvm_mt.MTLib;
import com.dy.dwvm_mt.R;
import com.dy.dwvm_mt.broadcasts.AutoStartReceiver;
import com.dy.dwvm_mt.commandmanager.commandUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.PhoneUtils;

public class CallShowService extends Service implements I_MT_Prime.MTLibCallback {
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    public static boolean isRunning = false;//是否正在运行
    private int phoneState = TelephonyManager.CALL_STATE_IDLE;//收到的话机状态
    private boolean isEnable = true;//是否启用服务
    private boolean isCalling = false;//是否主动拨号

    private String phoneNumber = "";

    private ConstraintLayout mFloatLayout;
    private WindowManager.LayoutParams wmParams;
    private WindowManager mWindowManager;
    private Button mFloatButton;

    private I_MT_Prime m_mtLib;

    @Override
    public void onCreate() {
        //android.os.Debug.waitForDebugger();
        if (isRunning == false) {
            try {
                isRunning = true;
                initPhoneStateListener();
                initFloatView();
                //StartMTLib();
                Thread.sleep(1000);
            } catch (Exception es) {
                LogUtils.e("CallShowService onCreate error " + es.toString());
                isRunning = false;
            }
        }
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        try {
            sendBroadcast(new Intent(AutoStartReceiver.AUTO_START_RECEIVER));
        } catch (Exception es) {
            LogUtils.e("CallShowService onDestroy error " + es.toString());
        } finally {
            isRunning = false;
            isEnable = false;
        }
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
     * 创建悬浮窗
     */
    private void initFloatView() {
        try {
            wmParams = new WindowManager.LayoutParams();
            //获取WindowManagerImpl.CompatModeWrapper
            mWindowManager = (WindowManager) getApplication().getSystemService(getApplication().WINDOW_SERVICE);
            /*//设置window type[优先级]
            wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;//窗口的type类型决定了它的优先级，优先级越高显示越在顶层*/
            /*//设置图片格式，效果为背景透明
            wmParams.format = PixelFormat.RGBA_8888;*/
            //调整悬浮窗显示的停靠位置为顶部水平居中
            wmParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER;
            // 设置图片格式，效果为背景透明
            wmParams.format = PixelFormat.TRANSLUCENT;
            // 设置Window flag 系统级弹框 | 覆盖表层
            wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            // 不可聚集（不让返回键） | 全屏 | 透明标状态栏
            wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

            // 以屏幕左上角为原点，设置x、y初始值
            wmParams.x = 0;
            wmParams.y = 200;
            /*// 设置悬浮窗口长宽数据
            wmParams.width = 200;
            wmParams.height = 80;*/

            //设置悬浮窗口长宽数据
            wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

            LayoutInflater inflater = LayoutInflater.from(getApplication());
            //获取浮动窗口视图所在布局
            mFloatLayout = (ConstraintLayout) inflater.inflate(R.layout.floatview, null);
        } catch (Exception es) {
            LogUtils.e(es.toString());
        }

    }//createFloatView()//创建悬浮窗

    /**
     * 初始化电话状态监听
     */
    private void initPhoneStateListener() {
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                phoneState = state;
                LogUtils.d("phoneState :" + phoneState + "\t incomingNumber:" + incomingNumber);
                if (isEnable) {//启用
                    switch (state) {
                        case TelephonyManager.CALL_STATE_IDLE://待机时（即无电话时,挂断时会调用）
                            LogUtils.d("CallShowService -> PhoneStateListener: CALL_STATE_IDLE");
                            isCalling = false;
                            //dismiss();//关闭来电秀
                            break;
                        case TelephonyManager.CALL_STATE_OFFHOOK://摘机（接听）
                            try {
                                Intent dialogIntent = new Intent(getBaseContext(), DY_VideoPhoneActivity.class);
                                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                dialogIntent.putExtra(BaseActivity.MT_VP_PAGE_OPENTYPE, BaseActivity.MT_VIDEOPHONE_STARTUPTYPE_OFFHOOK);
                                Thread.sleep(3000);
                                getApplication().startActivity(dialogIntent);
                                //callShow();//显示来电秀
                                LogUtils.d("CallShowService -> PhoneStateListener: CALL_STATE_OFFHOOK");
                            } catch (Exception es) {
                                LogUtils.e("CallShowService -> PhoneStateListener: CALL_STATE_OFFHOOK error" + es);
                            }

                            break;
                        case TelephonyManager.CALL_STATE_RINGING://响铃(来电)
                            isCalling = false;
                            phoneNumber = incomingNumber;
                            try {
                                String ddnsIPAndPort = commandUtils.DDNSIP + commandUtils.DDNSPORT;
                                commandUtils.sendLoginData("L_MT10", "123", "13411415574", "0756", ddnsIPAndPort);
                                LogUtils.d("CallShowService -> PhoneStateListener: CALL_STATE_RINGING incomingNumber ->" + incomingNumber);//来电号码
                            } catch (Exception es) {
                                LogUtils.e("CallShowService -> PhoneStateListener: .CALL_STATE_RINGING" + es);
                            }
                            //callShow();//显示来电秀
                            break;
                        default:
                            break;
                    }
                }

            }

            private void callShow() {
                SystemClock.sleep(1000);// 睡0.5秒是为了让悬浮窗显示在360或别的悬浮窗口的上方
                if (phoneState != TelephonyManager.CALL_STATE_IDLE) {
                    //添加mFloatLayout
                    mWindowManager.addView(mFloatLayout, wmParams);
                    //浮动窗口按钮
                    mFloatButton = mFloatLayout.findViewById(R.id.float_id);
                    //有值就赋上，没值就不显示该UI
                    mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                            View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                            .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

                    mFloatButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            PhoneUtils.answerRingingCall(CallShowService.this);
                            dismiss();
                        }
                    });//mFloatView.setOnClickListener[结束服务，关闭悬浮窗]
                }
            }

            private void dismiss() {
                try {
                    mWindowManager.removeView(mFloatLayout);
                } catch (Exception ex) {
                    LogUtils.e("dismiss error :" + ex.toString());
                }
            }
        };
        //设置监听器
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }


    private void StartMTLib() {
        try {
            m_mtLib = new MTLib();
            if (m_mtLib.isWorking() == false) {
                m_mtLib.installCallback(this);
                if (!m_mtLib.start(0x04000009, commandUtils.MTPORT, 1024 * 1024, 0, 1, 1, "")) {
                    LogUtils.e("MTLib.start() failed !");
                    return;
                }
            } else {
                LogUtils.d("MTLib is already started !");
            }

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
