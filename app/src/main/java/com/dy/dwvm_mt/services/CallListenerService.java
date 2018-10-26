package com.dy.dwvm_mt.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.dy.dwvm_mt.R;
import com.dy.dwvm_mt.broadcasts.AutoStartReceiver;
import com.dy.dwvm_mt.comlibs.LocalSetting;
import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.messagestructs.s_messageBase;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.StringUtils;

import static com.dy.dwvm_mt.utilcode.util.StringUtils.formatPhoneNumber;

/**
 * author: aJIEw
 * description: 电话状态变化后运行的服务
 * 这里我在服务中启动了一个系统级弹窗，在通话的时候就显示，
 * 然后在其中放了一个按钮用于打开 PhoneCallApp
 */
public class CallListenerService extends Service {

    private View phoneCallView;
    private TextView tvCallNumber;
    private Button btnOpenApp;

    private WindowManager windowManager;
    private WindowManager.LayoutParams params;

    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    private String callNumber;
    private boolean hasShown;
    private boolean isCallingIn;

    private int m_DDNSCallState = s_messageBase.TelStates.Idle;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            initPhoneStateListener();
            initPhoneCallView();
            Thread.sleep(1000);
        } catch (Exception es) {
            LogUtils.e("CallListenerService onCreate error: " + es.toString());
            return;
        }
    }

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, START_STICKY, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 初始化来电状态监听器
     */
    private void initPhoneStateListener() {
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                LocalSetting.getDYSPUtil().put(LocalSetting.Cache_Name_CallingTelNumber, incomingNumber);
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE: // 待机，即无电话时，挂断时触发
                        LocalSetting.getDYSPUtil().put(LocalSetting.Cache_Name_CallingTelNumber, "");
                        m_DDNSCallState = s_messageBase.TelStates.Idle;
                        //dismiss();
                        break;

                    case TelephonyManager.CALL_STATE_RINGING: // 响铃，来电时触发
                        isCallingIn = true;
                        m_DDNSCallState = s_messageBase.TelStates.Ring;
                        //updateUI();
                        break;

                    case TelephonyManager.CALL_STATE_OFFHOOK: // 摘机，接听或打电话时触发
                        if (m_DDNSCallState == s_messageBase.TelStates.Ring) {
                            m_DDNSCallState = s_messageBase.TelStates.Offhook;
                        } else {
                            m_DDNSCallState = s_messageBase.TelStates.CalledOffhook;
                        }
                        //updateUI();
                        //show();
                        break;
                    default:
                        break;

                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };
        LocalSetting.setCallState(m_DDNSCallState);
        LogUtils.d("当前电话状态", m_DDNSCallState);
        CommandUtils.sendTelState(m_DDNSCallState, LocalSetting.getMeetingID(), null);
        // 设置来电监听器
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        sendValidCode();
    }

    private void sendValidCode() {
        if (m_DDNSCallState == s_messageBase.TelStates.CalledOffhook) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    //被叫发送验证码,ddns模拟成主叫
                    String caller = LocalSetting.getDYSPUtil().getString(LocalSetting.Cache_Name_CallingTelNumber);
                    CommandUtils.sendVerifyCode(caller, caller);
                    LogUtils.d("PhoneStateListener callShow send VerifyCode :", caller);
                } catch (Exception es) {
                    LogUtils.e("PhoneStateListener sendVerifyCode error" + es);
                }
            }).start();
        }
    }

    private void initPhoneCallView() {
        windowManager = (WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        int width = windowManager.getDefaultDisplay().getWidth();
        int height = windowManager.getDefaultDisplay().getHeight();

        params = new WindowManager.LayoutParams();
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        params.width = width;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        // 设置图片格式，效果为背景透明
        params.format = PixelFormat.TRANSLUCENT;
        // 设置 Window flag 为系统级弹框 | 覆盖表层
        params.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        // 不可聚集（不响应返回键）| 全屏
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        // API 19 以上则还可以开启透明状态栏与导航栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            params.flags = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        }

        FrameLayout interceptorLayout = new FrameLayout(this) {

            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {

                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

                        return true;
                    }
                }

                return super.dispatchKeyEvent(event);
            }
        };

        phoneCallView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.view_phone_call, interceptorLayout);
        tvCallNumber = phoneCallView.findViewById(R.id.tv_call_number);
        btnOpenApp = phoneCallView.findViewById(R.id.btn_open_app);
        btnOpenApp.setOnClickListener(v -> {
            /*Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            CallListenerService.this.startActivity(intent);*/
        });
    }

    /**
     * 显示顶级弹框展示通话信息
     */
    private void show() {
        if (!hasShown) {
            windowManager.addView(phoneCallView, params);
            hasShown = true;
        }
    }

    /**
     * 取消显示
     */
    private void dismiss() {
        if (hasShown) {
            windowManager.removeView(phoneCallView);
            isCallingIn = false;
            hasShown = false;
        }
    }

    private void updateUI() {
        tvCallNumber.setText(formatPhoneNumber(callNumber));

        int callTypeDrawable = isCallingIn ? R.drawable.ic_phone_call_in : R.drawable.ic_phone_call_out;
        tvCallNumber.setCompoundDrawablesWithIntrinsicBounds(null, null,
                getResources().getDrawable(callTypeDrawable), null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            sendBroadcast(new Intent(AutoStartReceiver.AUTO_START_RECEIVER));
        } catch (Exception es) {
            LogUtils.e("CallListenerService onDestroy error " + es.toString());
        } finally {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

    }
}
