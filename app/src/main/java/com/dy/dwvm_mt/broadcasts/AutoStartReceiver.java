package com.dy.dwvm_mt.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dy.dwvm_mt.services.CallShowService;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

public class AutoStartReceiver extends BroadcastReceiver {
    public static final String AUTO_START_RECEIVER = "dwvm_mt.autostart_action";

    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.d("开启自动加载服务");

        if(!CallShowService.isRunning)
            startCallShowService(context, intent);
    }

    private void startCallShowService(Context context, Intent intent) {
        intent.setClass(context, CallShowService.class);
        context.startService(intent);
    }
}
