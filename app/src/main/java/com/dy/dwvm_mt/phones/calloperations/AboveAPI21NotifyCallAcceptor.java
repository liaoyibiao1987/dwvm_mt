package com.dy.dwvm_mt.phones.calloperations;

import android.os.Build;
import android.support.annotation.RequiresApi;

import com.dy.dwvm_mt.phones.ICallAcceptor;


/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/10/25.
 * PS: Not easy to write code, please indicate.
 */
public class AboveAPI21NotifyCallAcceptor implements ICallAcceptor {

    class api21Runer implements Runnable {
        api21Runer() {
        }

        public void run() {
            //FloatWindowManager.m17488b();
        }
    }

    public AboveAPI21NotifyCallAcceptor() {
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    /* renamed from: a */
    public void accept() {
        /*Handler a;
        Runnable c26181;
        try {
            for (MediaController mediaController : ((MediaSessionManager) AppEntry.m12850b().getSystemService("media_session")).getActiveSessions(new ComponentName(AppEntry.m12850b(), AboveAP121NotifyMonitorService.class))) {
                if ("com.android.server.telecom".equals(mediaController.getPackageName())) {
                    mediaController.dispatchMediaButtonEvent(new KeyEvent(1, 79));
                    HandlerUtils.m13006a().postDelayed(new api21Runer(), 200);
                    return;
                }
            }
            a = HandlerUtils.m13006a();
            c26181 = new api21Runer();
        } catch (SecurityException e) {
            e.printStackTrace();
            a = HandlerUtils.m13006a();
            c26181 = new api21Runer();
        } catch (Throwable th) {
            HandlerUtils.m13006a().postDelayed(new api21Runer(), 200);
        }
        a.postDelayed(c26181, 200);*/
    }

}
