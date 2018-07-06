package com.dy.dwvm_mt.commandmanager;

import com.dy.dwvm_mt.Comlibs.DataPackShell;

import java.util.EventListener;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/4.
 * PS: Not easy to write code, please indicate.
 */
public interface DY_onReceivedPackEventHandler extends EventListener {
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
