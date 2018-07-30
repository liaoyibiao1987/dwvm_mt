package com.dy.dwvm_mt.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.dy.dwvm_mt.Comlibs.LocalSetting;
import com.dy.dwvm_mt.MTMainActivity;
import com.dy.dwvm_mt.R;
import com.dy.dwvm_mt.broadcasts.AutoStartReceiver;
import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.messagestructs.s_messageBase;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.PhoneUtils;
import com.dy.dwvm_mt.utilcode.util.StringUtils;

import java.lang.reflect.Method;

public class CallShowService extends Service {
    private static final int FOREGROUND_ID = 1;
    private static int NOTIFKEEPERIID = 0x111;

    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    public static boolean isRunning = false;//是否正在运行
    private int phoneState = TelephonyManager.CALL_STATE_IDLE;//收到的话机状态
    private boolean isEnable = true;//是否启用服务
    private boolean isFloatShown = false;

    private String commingTelNumber = "";
    private String outgoingTelNumber = "";

    private ConstraintLayout mFloatLayout;
    private WindowManager.LayoutParams wmParams;
    private WindowManager mWindowManager;
    private Button mFloatButton;

    @Override
    public void onCreate() {
        //android.os.Debug.waitForDebugger();
        if (isRunning == false) {
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
        if (intent != null && Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {
            //去电 根本行不通,大多数手机根本无法获取去电。
            outgoingTelNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            LogUtils.d("ACTION_NEW_OUTGOING_CALL : " + outgoingTelNumber);
           /* if (StringUtils.isTrimEmpty(outgoingTelNumber) == false && outgoingTelNumber.length() > 2) {
                CommandUtils.sendVerifyCode(outgoingTelNumber, outgoingTelNumber);
            }*/

        }

        NotificationWhenCommand();
        return super.onStartCommand(intent, START_STICKY, startId);
    }

    //手机休眠一段时间后（1-2小时），后台运行的服务被强行kill掉
    //Service通过调用startForeground方法来绑定一个前台的通知时，可以有效的提升进程的优先级。
    private void NotificationWhenCommand() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.drawable.videoico)
                .setContentTitle("电话提醒")
                .setContentText("点击打开视频界面.")
                .setContentInfo("东耀企业")
                .setWhen(System.currentTimeMillis());
        Intent activityIntent = new Intent(this, MTMainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();
        //Service通过调用startForeground方法来绑定一个前台的通知时，可以有效的提升进程的优先级
        if (Build.VERSION.SDK_INT < 18) {
            startForeground(NOTIFKEEPERIID, notification);
        } else {
            startForeground(NOTIFKEEPERIID, notification);
            startService(new Intent(this, InnerServer.class));
        }
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
            //设置window type[优先级]
            wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;//窗口的type类型决定了它的优先级，优先级越高显示越在顶层
            //设置图片格式，效果为背景透明
            wmParams.format = PixelFormat.RGBA_8888;
            //调整悬浮窗显示的停靠位置为顶部水平居中
            wmParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER;
           /* // 设置图片格式，效果为背景透明
            wmParams.format = PixelFormat.TRANSLUCENT;*/
            /*// 设置Window flag 系统级弹框 | 覆盖表层
            wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;*/
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
                LocalSetting.setCallState(state);
                int callState = s_messageBase.TelStates.Idle;
                if (isEnable) {//启用
                    switch (state) {
                        case TelephonyManager.CALL_STATE_IDLE://待机时（即无电话时,挂断时会调用）
                            LogUtils.d("PhoneStateListener CALL_STATE_IDLE");
                            outgoingTelNumber = "";
                            commingTelNumber = "";
                            LocalSetting.getCacheDoubleUtils().put(LocalSetting.Cache_Name_CallingTelNumber, "");
                            dismiss();//关闭来电秀
                            break;

                        case TelephonyManager.CALL_STATE_RINGING://响铃(来电)
                            commingTelNumber = incomingNumber;
                            callState = s_messageBase.TelStates.Ring;
                            LogUtils.d("PhoneStateListener CALL_STATE_RINGING incomingNumber ->" + incomingNumber);//来电号码
                           /* try {
                                String ddnsIPAndPort = CommandUtils.getDDNSIPPort();
                                //CommandUtils.sendLoginData("L_MT5", "123", "13411415574", "860756", ddnsIPAndPort);
                                callShow();//显示来电秀
                                LogUtils.d("PhoneStateListener onCallStateChanged: CALL_STATE_RINGING incomingNumber ->" + incomingNumber);//来电号码
                            } catch (Exception es) {
                                LogUtils.e("PhoneStateListener onCallStateChanged: .CALL_STATE_RINGING" + es);
                            }*/
                            //PhoneUtils.telcomInvok(getApplicationContext(),"endCall");
                            break;
                        case TelephonyManager.CALL_STATE_OFFHOOK://摘机（接听）
                            try {
                                if (StringUtils.isTrimEmpty(incomingNumber) == true) {
                                    callState = s_messageBase.TelStates.Offhook;
                                    Thread.sleep(1000);
                                    callShow();
                                } else {
                                    callState = s_messageBase.TelStates.CalledOffhook;
                                }
                                LogUtils.d("PhoneStateListener CALL_STATE_OFFHOOK", incomingNumber);
                            } catch (Exception es) {
                                LogUtils.e("PhoneStateListener CALL_STATE_OFFHOOK error" + es);
                            }

                            break;
                        default:
                            break;
                    }
                }
                CommandUtils.sendTelState(callState, LocalSetting.getMeetingID(), null);
            }

            private void callShow() {
                if (isFloatShown == false) {
                    SystemClock.sleep(500);// 睡0.5秒是为了让悬浮窗显示在360或别的悬浮窗口的上方
                    if (phoneState != TelephonyManager.CALL_STATE_IDLE) {
                        //添加mFloatLayout
                        mWindowManager.addView(mFloatLayout, wmParams);
                        //浮动窗口按钮
                        mFloatButton = mFloatLayout.findViewById(R.id.float_id);
                        //有值就赋上，没值就不显示该UI
                        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

                        mFloatButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    PhoneUtils.telcomInvok(getBaseContext(), "answerRingingCall");
                                    PhoneUtils.answerRingingCall(CallShowService.this);
                                    String calling = LocalSetting.getCacheDoubleUtils().getString(LocalSetting.Cache_Name_CallingTelNumber);
                                    LogUtils.d("PhoneStateListener callShow send VerifyCode :", calling);
                                    CommandUtils.sendVerifyCode(calling, calling);
                                   /* Intent dialogIntent = new Intent(getBaseContext(), DY_VideoPhoneActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            .putExtra(BaseActivity.MT_VP_PAGE_OPENTYPE, BaseActivity.MT_VIDEOPHONE_STARTUPTYPE_OFFHOOK);
                                    getApplication().startActivity(dialogIntent);*/
                                } catch (Exception es) {
                                    LogUtils.e("mFloatButton error: " + es);
                                } finally {
                                    dismiss();
                                }
                            }
                        });//mFloatView.setOnClickListener[结束服务，关闭悬浮窗]
                    }
                    isFloatShown = true;
                }

            }

            private void dismiss() {
                try {
                    if (isFloatShown == true) {
                        mWindowManager.removeView(mFloatLayout);
                    }
                    isFloatShown = false;
                } catch (Exception ex) {
                    LogUtils.e("dismiss error :" + ex.toString());
                }
            }
        };
        //设置监听器
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private static class InnerServer extends Service {
        @Override
        public void onCreate() {
            super.onCreate();
            try {
                startForeground(CallShowService.NOTIFKEEPERIID, new Notification());
            } catch (Exception es) {
                LogUtils.e("InnerServer -> onCreate: " + es);
            }
            stopSelf();
        }

        @Override
        public void onDestroy() {
            stopForeground(true);
            super.onDestroy();
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

}
