package com.dy.dwvm_mt.commandmanager;

import com.dy.dwvm_mt.messagestructs.NetWorkCommand;

import java.util.EventObject;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/4.
 * PS: Not easy to write code, please indicate.
 */
public final class NWCommandEventArg extends EventObject {
    private static final long serialVersionUID = 1331423214321432154L;
    private NetWorkCommand eventArg;

    public NWCommandEventArg(Object source) {
        super(source);
        eventArg = ((NetWorkCommand) source);
    }

    public NetWorkCommand getEventArg() {
        return eventArg;
    }
}
