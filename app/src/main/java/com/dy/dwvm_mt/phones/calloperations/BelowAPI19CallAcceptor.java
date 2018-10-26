package com.dy.dwvm_mt.phones.calloperations;

import com.dy.dwvm_mt.phones.ICallAcceptor;

import java.io.IOException;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/10/25.
 * PS: Not easy to write code, please indicate.
 */
public class BelowAPI19CallAcceptor implements ICallAcceptor {

   public BelowAPI19CallAcceptor() {
    }

    /* renamed from: a */
    @Override
    public void accept() {
        try {
            Runtime runtime = Runtime.getRuntime();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("input keyevent ");
            stringBuilder.append(Integer.toString(79));
            runtime.exec(stringBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
