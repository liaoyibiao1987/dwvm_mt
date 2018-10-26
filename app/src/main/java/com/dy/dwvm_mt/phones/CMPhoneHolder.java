package com.dy.dwvm_mt.phones;

import android.os.Build;
import com.dy.dwvm_mt.phones.calloperations.AboveAPI19CallAcceptor;
import com.dy.dwvm_mt.phones.calloperations.AboveAPI21NotifyCallAcceptor;
import com.dy.dwvm_mt.phones.calloperations.AboveAPI26CallAcceptor;
import com.dy.dwvm_mt.phones.calloperations.BelowAPI19CallAcceptor;
import com.dy.dwvm_mt.phones.calloperations.CommonCallRejector;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/10/25.
 * PS: Not easy to write code, please indicate.
 */
public class CMPhoneHolder implements ICallAcceptor, ICallRejector {
    /* renamed from: a */
    ICallRejector rejector;
    /* renamed from: b */
    ICallAcceptor acceptor;

    CMPhoneHolder() {
        setRejector();
        setAcceptor();
    }

    /* renamed from: c */
    private void setRejector() {
        if (UseSystemCallRejector.isNativeSystem()) {
            this.rejector = new UseSystemCallRejector();
        } else {
            this.rejector = new CommonCallRejector();
        }
    }

    /* renamed from: d */
    private void setAcceptor() {
        if (UseSystemCallAcceptor.isNativeSystem()) {
            this.acceptor = new UseSystemCallAcceptor();
        } else if (Build.VERSION.SDK_INT >= 26) {
            this.acceptor = new AboveAPI26CallAcceptor();
        } else if (Build.VERSION.SDK_INT >= 21) {
            this.acceptor = new AboveAPI21NotifyCallAcceptor();
        } else if (Build.VERSION.SDK_INT >= 19) {
            this.acceptor = new AboveAPI19CallAcceptor();
        } else {
            this.acceptor = new BelowAPI19CallAcceptor();
        }
    }

    @Override
    public void accept() {
       this.acceptor.accept();
    }

    @Override
    public boolean reject() {
        return this.rejector.reject();
    }
}