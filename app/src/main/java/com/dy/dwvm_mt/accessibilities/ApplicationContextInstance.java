package com.dy.dwvm_mt.accessibilities;

import android.content.Context;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/10/25.
 * PS: Not easy to write code, please indicate.
 */
public class ApplicationContextInstance {
    private static ApplicationContextInstance instance;
    /* renamed from: b */
    private Context context = null;

    /* renamed from: a */
    public static synchronized ApplicationContextInstance getInstance() {
        ApplicationContextInstance applicationContextInstance;
        synchronized (ApplicationContextInstance.class) {
            if (instance == null) {
                instance = new ApplicationContextInstance();
            }
            applicationContextInstance = instance;
        }
        return applicationContextInstance;
    }

    /* renamed from: b */
    public Context getContext() {
        return this.context;
    }

    /* renamed from: a */
    public void setContext(Context context) {
        if (this.context == null) {
            this.context = context;
        }
    }

}
