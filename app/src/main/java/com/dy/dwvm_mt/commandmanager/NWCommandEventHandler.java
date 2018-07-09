package com.dy.dwvm_mt.commandmanager;

import java.util.EventListener;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/4.
 * PS: 加工过实体事件handler
 */
public interface NWCommandEventHandler extends EventListener {
    void doHandler(NWCommandEventArg arg);
}
