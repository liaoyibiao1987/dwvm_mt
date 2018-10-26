package com.dy.dwvm_mt.phones;

import android.os.Build;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/10/25.
 * PS: Not easy to write code, please indicate.
 */
public class UseSystemCallAcceptor implements ICallAcceptor{

    @Override
    public void accept() {
        //FloatWindowManager.m17488b();
    }

    /* renamed from: b */
    public static boolean isNativeSystem() {
        return Build.DEVICE.equals("Coolpad8675-A");
    }

}
