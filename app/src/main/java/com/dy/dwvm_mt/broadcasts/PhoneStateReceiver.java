package com.dy.dwvm_mt.broadcasts;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.dy.dwvm_mt.services.CallShowService;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

public class PhoneStateReceiver extends BroadcastReceiver {
    public static final String PHONE_STATE = TelephonyManager.ACTION_PHONE_STATE_CHANGED;
    private Intent intentService = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(PHONE_STATE) == true) {
            //String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            LogUtils.d(context, "PhoneStateReceiver 收到消息.");

            /** 隐式启动service
            intentService = new Intent();
            intentService.setClassName("com.dy.dwvm_mt",  "com.dy.dwvm_mt.services.CallShowService");
            */
            //** 显式启动service
            intentService = new Intent(context, CallShowService.class);
            context.startService(intentService);

            /*if (intent.getAction().equals("android.media.AUDIO_BECOMING_NOISY")) {
             *//* 服务开机自启动 *//*
                Intent service = new Intent(context, CallShowService.class);
                context.startService(service);
            } else {
                intentService = new Intent(context, CallShowService.class);
                context.startService(intentService);
            }*/

        }
    }
}
