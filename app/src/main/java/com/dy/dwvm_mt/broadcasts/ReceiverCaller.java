package com.dy.dwvm_mt.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.dy.dwvm_mt.services.ServiceCallerShow;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

public class ReceiverCaller extends BroadcastReceiver {

    private Intent intent2 = null;
    public final static String B_PHONE_STATE = TelephonyManager.ACTION_PHONE_STATE_CHANGED;

    @Override
    public void onReceive(Context context, Intent intent) {
        //获取来电是的电话号码
        String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        String action = intent.getAction();
        //呼入电话
        if (action.equals(B_PHONE_STATE)) {
            LogUtils.e(context, "ReceiverCaller.B_PHONE_STATE()");
            //将需要的信息赋给Service中的两个静态成员
            ServiceCallerShow.context = context;
            ServiceCallerShow.phoneNumBerStr = phoneNumber;

            doReceivePhone(context, intent);//进行细致监听，[来电ing][听话ing][挂断]
        }

    }//onReceive

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
                SystemClock.sleep(2000);// 睡2秒是为了让悬浮窗显示在360或别的悬浮窗口的上方
                intent2 = new Intent(context, ServiceCallerShow.class);
                context.startService(intent2);//打开Service
                break;
            case TelephonyManager.CALL_STATE_IDLE://电话挂断
                //context.stopService(intent2);//关闭Service中的悬浮窗口【关闭来电显示】
                //貌似用mWindowManager.removeView(mFloatLayout);比stopService快？但是还没测试过
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK://通话中
                //context.stopService(intent2);
                break;

        }

    }//doReceivePhone处理电话广播[来电ing][接通ing][挂断]
}
