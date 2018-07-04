package com.dy.dwvm_mt.commandmanager;

import java.util.EventListener;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/4.
 * PS: Not easy to write code, please indicate.
 */
public interface NWCommandEventHandler extends EventListener {
    void doHandler(NWCommandEventArg arg);
}
