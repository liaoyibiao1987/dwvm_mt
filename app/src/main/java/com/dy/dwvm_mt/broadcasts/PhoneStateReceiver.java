package com.dy.dwvm_mt.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.telephony.TelephonyManager;

import com.dy.dwvm_mt.services.CallShowService;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

public class PhoneStateReceiver extends BroadcastReceiver {
    public static final String PHONE_STATE = "android.intent.action.PHONE_STATE";
    public static final String B_PHONE_STATE = TelephonyManager.ACTION_PHONE_STATE_CHANGED;
    public static final String AUTO_START_RECEIVER = "dwvm_mt.autostart_action";
    private Intent intentService = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        LogUtils.d(intent.getAction());
        SystemClock.sleep(500);// 睡0.5秒是为了让悬浮窗显示在360或别的悬浮窗口的上方
        if (intent.getAction().equals("android.media.AUDIO_BECOMING_NOISY")) {
            /* 服务开机自启动 */
            Intent service = new Intent(context, CallShowService.class);
            context.startService(service);
        }else {
            intentService = new Intent(context, CallShowService.class);
            context.startService(intentService);
        }

    }

    /**
     * 处理电话广播.
     *
     * @param context
     * @param intent
     */

    public void doReceivePhone(Context context, Intent intent) {

        //获取来电时的状态：[来电ing][接通ing][挂断]

        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int state = telephony.getCallState();
        switch (state) {

            case TelephonyManager.CALL_STATE_RINGING://等待接电话
                break;
            case TelephonyManager.CALL_STATE_IDLE://电话挂断
                context.stopService(intentService);//关闭Service中的悬浮窗口【关闭来电显示】
                //貌似用mWindowManager.removeView(mFloatLayout);比stopService快？但是还没测试过
                break;

            case TelephonyManager.CALL_STATE_OFFHOOK://通话中
                context.stopService(intentService);
                break;
        }

    }//doReceivePhone处理电话广播[来电ing][接通ing][挂断]

}
