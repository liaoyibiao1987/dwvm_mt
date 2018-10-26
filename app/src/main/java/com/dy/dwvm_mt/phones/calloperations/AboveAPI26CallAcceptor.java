package com.dy.dwvm_mt.phones.calloperations;

import android.annotation.SuppressLint;
import android.os.Build;

import com.dy.dwvm_mt.phones.ICallAcceptor;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/10/25.
 * PS: Not easy to write code, please indicate.
 */
public class AboveAPI26CallAcceptor implements ICallAcceptor {
    public AboveAPI26CallAcceptor() {
    }

    @SuppressLint({"MissingPermission"})
    @Override
    /* renamed from: a */
    public void accept() {
        if (Build.VERSION.SDK_INT >= 26) {
            //((TelecomManager) AppEntry.m12850b().getSystemService("telecom")).acceptRingingCall();
        }
    }

}
