package com.dy.dwvm_mt.commandmanager;

import com.dy.dwvm_mt.Comlibs.DataPackShell;

import java.util.EventListener;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/4.
 * PS: 对原始MT 组件的一遍封装，对数据包进行进一步的解析。 (事件注册后销毁时必须反注册)
 */
public interface DY_onReceivedPackEventHandler extends EventListener {
    /**
     * 处理收到的非加工包数据
     * @param entity 收到的完整数据包，没有经过加工的数据(<区别于 NWCommandEventHandlerNWCommandEventArg 是已经加工过的数据格式>)
     * @return return 0 (事件注册后销毁时必须反注册)
     */
    long onReceivedUdpPacket(
            DataPackShell.ReceivedPackEntity entity
    );

    long onReceivedVideoFrame(
            long localDeviceId,
            String remoteDeviceIpPort,
            long remoteDeviceId,
            int remoteEncoderChannelIndex,
            int localDecoderChannelIndex,
            long frameType,
            String videoCodec,
            int imageResolution,
            int width,
            int height,
            byte[] frameBuffer,
            int frameSize
    );

    long onReceivedAudioFrame(
            long localDeviceId,
            String remoteDeviceIpPort,
            long remoteDeviceId,
            int remoteEncoderChannelIndex,
            int localDecoderChannelIndex,
            String audioCodec,
            byte[] frameBuffer,
            int frameSize
    );
}
