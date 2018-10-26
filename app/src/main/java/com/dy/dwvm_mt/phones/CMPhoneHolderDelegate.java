package com.dy.dwvm_mt.phones;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/10/25.
 * PS: Not easy to write code, please indicate.
 */
public class CMPhoneHolderDelegate {
    private static CMPhoneHolder cmPhoneHolder;

    /* renamed from: c */
    private static void initCMPhoneHolder() {
        synchronized (CMPhoneHolderDelegate.class) {
            if (cmPhoneHolder == null) {
                cmPhoneHolder = new CMPhoneHolder();
            }
        }
    }

    /* renamed from: a */
    public static ICallAcceptor getAcceptor() {
        ICallAcceptor iCallAcceptor;
        synchronized (CMPhoneHolderDelegate.class) {
            CMPhoneHolderDelegate.initCMPhoneHolder();
            iCallAcceptor = cmPhoneHolder;
        }
        return iCallAcceptor;
    }

    /* renamed from: b */
    public static ICallRejector getRejector() {
        ICallRejector iCallRejector;
        synchronized (CMPhoneHolderDelegate.class) {
            CMPhoneHolderDelegate.initCMPhoneHolder();
            iCallRejector = cmPhoneHolder;
        }
        return iCallRejector;
    }

}
