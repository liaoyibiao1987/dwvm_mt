package com.dy.dwvm_mt.phones;

import android.os.Build;
import android.util.Log;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/10/25.
 * PS: Not easy to write code, please indicate.
 */
public class UseSystemCallRejector implements ICallRejector{
    private static String TAG= "UseSystemCallRejector";
    public static boolean isNativeSystem() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("--- vivo = ");
        //stringBuilder.append(VivoHelper.m9001f());
        stringBuilder.append(", device = ");
        stringBuilder.append(Build.DEVICE);
        stringBuilder.append(", SDK_INT = ");
        stringBuilder.append(Build.VERSION.SDK_INT);
        Log.d(TAG, stringBuilder.toString());
       /* int i = 0;
        int i2 = (Build.MANUFACTURER.equals(Constant.f37638z) && TextUtils.equals(SystemProperties.m8985a("ro.vivo.os.build.display.id"), "Funtouch OS_3.1") && VERSION.SDK_INT == 25) ? 1 : 0;
        int i3 = (DeviceUtils.m8917v() && ((double) VivoHelper.m9001f()) == 2.0d && Build.DEVICE.equals("msm8974")) ? 1 : 0;
        i2 |= i3;
        if (DeviceUtils.m8917v() && Build.MODEL.equals("vivo Y75s")) {
            i = 1;
        }
        return i2 | i;*/
       return false;
    }

    /* renamed from: b */
    public boolean reject() {
        //FloatWindowManager.m17488b();
        return false;
    }

}
