package com.dy.dwvm_mt.phones.calloperations;

import android.os.Build;
import android.support.annotation.RequiresApi;

import com.dy.dwvm_mt.phones.ICallAcceptor;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/10/25.
 * PS: Not easy to write code, please indicate.
 */
public class AboveAPI19CallAcceptor implements ICallAcceptor {

    public AboveAPI19CallAcceptor() {
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    /* renamed from: a */
    public void accept() {
        /*AudioManager audioManager = (AudioManager) AppEntry.m12850b().getSystemService("audio");
        KeyEvent keyEvent = new KeyEvent(0, 79);
        KeyEvent keyEvent2 = new KeyEvent(1, 79);
        audioManager.dispatchMediaKeyEvent(keyEvent);
        audioManager.dispatchMediaKeyEvent(keyEvent2);*/
    }

}
