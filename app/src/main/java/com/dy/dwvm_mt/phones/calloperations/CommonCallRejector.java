package com.dy.dwvm_mt.phones.calloperations;

import com.dy.dwvm_mt.phones.ICallRejector;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/10/25.
 * PS: Not easy to write code, please indicate.
 */
public class CommonCallRejector implements ICallRejector {
   public CommonCallRejector() {
    }

    /* renamed from: b */
    @Override
    public boolean reject() {
       /* try {
            IResultReceiver.Stub.asInterface((IBinder) ReflectUtils.m13185a(Class.forName("android.os.ServiceManager"), "getService", String.class).invoke(null, new Object[]{"phone"})).endCall();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            FloatWindowManager.m17488b();
            return false;
        }*/
       return  false;
    }

}
