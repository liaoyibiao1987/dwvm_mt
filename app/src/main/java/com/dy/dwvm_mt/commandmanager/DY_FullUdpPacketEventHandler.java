package com.dy.dwvm_mt.commandmanager;

import com.dy.dwvm_mt.comlibs.DataPackShell;

import java.util.EventListener;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/4.
 * PS: 对原始MT 组件的一遍封装，对数据包进行进一步的解析。 (事件注册后销毁时必须反注册)
 */
public interface DY_FullUdpPacketEventHandler extends EventListener {
    /**
     * 处理收到的非加工包数据
     * @param entity 收到的完整数据包，没有经过加工的数据(<区别于 NWCommandEventHandlerNWCommandEventArg 是已经加工过的数据格式>)
     * @return return 0 (事件注册后销毁时必须反注册)
     */
    long onReceivedFullUdpPacket(
            DataPackShell.ReceivedPackEntity entity
    );


}
