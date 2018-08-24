package com.dy.dwvm_mt.commandmanager;

import java.util.EventListener;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/8/24.
 * PS: Not easy to write code, please indicate.
 */
public interface DY_AVPacketEventHandler extends EventListener {
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
