package com.android.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.view.KeyEvent;

import com.dy.dwvm_mt.utilcode.util.LogUtils;

import java.lang.reflect.Method;

public class PhoneUtils {

    public static com.android.internal.telephony.ITelephony getITelephony() throws Exception {
        Class clazz = Class.forName("android.os.ServiceManager");
        Method getServiceMethod = clazz.getMethod("getService", String.class);
        IBinder iBinder = (IBinder) getServiceMethod.invoke(null, Context.TELEPHONY_SERVICE);
        ITelephony iTelephony = ITelephony.Stub.asInterface(iBinder);
        return ITelephony.Stub.asInterface(iBinder);
    }

    /**
     * 接听电话
     *
     * @param context
     */
    public static void answerRingingCall(Context context) {
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {// android 2.3以上
                try {
                    Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);
                    buttonUp.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
                    context.sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");
                } catch (Exception e) {
                    Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);
                    buttonUp.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
                    context.sendOrderedBroadcast(buttonUp, null);
                }
            } else {
                getITelephony().answerRingingCall();
            }
        } catch (Exception e) {
            LogUtils.e(e);
        }
    }

    /**
     * 挂机
     */
    public static void endRingingCall() {
        try {
            getITelephony().endCall();
        } catch (Exception e) {
            LogUtils.e(e);
        }
    }
}
