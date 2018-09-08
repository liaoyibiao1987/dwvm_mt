package com.dy.dwvm_mt;

import com.dy.dwvm_mt.comlibs.DataPackShell;
import com.dy.dwvm_mt.comlibs.I_MT_Prime;

import java.util.List;

/**
 * Created on 2016.1.22
 * MT NDK Library
 */
public class MTLib implements I_MT_Prime {
    static {
        System.loadLibrary("DwvmMTLib");
    }

    public static final String CODEC_VIDEO_H264 = "video/avc";
    public static final String CODEC_AUDIO_MP3 = "audio/mp3";

    public static final int IMAGE_RESOLUTION_CIF = 0;
    public static final int IMAGE_RESOLUTION_D1 = 1;
    public static final int IMAGE_RESOLUTION_QCIF = 2;

    private MTLibCallback m_callbackInstance = null;

    @Override
    public void installCallback(MTLibCallback instance) {
        m_callbackInstance = instance;
    }

    @Override
    public long callbackFromJNI(
            String funcName,
            String remoteDeviceIpPort,
            long remoteDeviceId,
            int remoteEncoderChannelIndex,
            long localDeviceId,
            int localDecoderChannelIndex,
            long dataType,
            byte[] dataBuffer,
            int dataBytes,
            String codecName,
            int imageResolution,
            int width,
            int height) {
        // callback by JNI-MTLib, and dispatch to main app
        if (m_callbackInstance != null) {
            if (funcName.equalsIgnoreCase("onReceivedUdpPacket")) {
                return m_callbackInstance.onReceivedUdpPacket(
                        localDeviceId,
                        remoteDeviceIpPort,
                        remoteDeviceId,
                        dataType,
                        dataBuffer,
                        dataBytes);
                /*return DataPackShell.ParseBuff(dataBuffer,(int)dataType, remoteDeviceIpPort);*/
            } else if (funcName.equalsIgnoreCase("onReceivedVideoFrame")) {
                return m_callbackInstance.onReceivedVideoFrame(
                        localDeviceId,
                        remoteDeviceIpPort,
                        remoteDeviceId,
                        remoteEncoderChannelIndex,
                        localDecoderChannelIndex,
                        dataType,
                        codecName,
                        imageResolution,
                        width,
                        height,
                        dataBuffer,
                        dataBytes);
            } else if (funcName.equalsIgnoreCase("onReceivedAudioFrame")) {
                return m_callbackInstance.onReceivedAudioFrame(
                        localDeviceId,
                        remoteDeviceIpPort,
                        remoteDeviceId,
                        remoteEncoderChannelIndex,
                        localDecoderChannelIndex,
                        codecName,
                        dataBuffer,
                        dataBytes);
            }
        }
        return 0;
    }

    @Override
    public native boolean start(
            long localDeviceId,
            int udpPort,
            int udpSocketBufferSize,
            int netEncryptMode,
            int encoderChannelNumber,
            int decoderChannelNumber,
            String optionText
    );

    @Override
    public native void stop();

    @Override
    public native boolean isWorking();

    @Override
    public native boolean setDeviceName(String deviceName);

    @Override
    public native boolean setDeviceId(long deviceID);

    @Override
    public native int dataEncrypt(byte[] srcData, int srcSize, byte[] destData);

    @Override
    public native int dataDecrypt(byte[] srcData, int srcSize, byte[] destData);

    @Override
    public native String stringEncrypt(String srcText);

    @Override
    public native String stringDecrypt(String srcText);

    @Override
    public native int sendUdpPacketToDevice(
            long packetType,
            long needReplay,
            long destDeviceId,
            String destDeviceIpPort,
            byte[] dataBuffer,
            int dataSize
    );

    @Override
    public int sendUdpPacketToDevice2(long packetType,
                                      long needReplay,
                                      long destDeviceId,
                                      String destDeviceIpPort,
                                      byte[] dataBuffer,
                                      int dataSize
    ) {
        List<byte[]> datas = DataPackShell.GetSendBuff(dataBuffer);
        int ret = 0;
        if (datas.size() > 0) {
            for (byte[] x : datas) {
                //byte[] v_x = new byte[x.length];
                //System.arraycopy(v_x, 0, x, 0, x.length);
                ret += sendUdpPacketToDevice(packetType, needReplay, destDeviceId, destDeviceIpPort, x, x.length);
            }
        }
        return ret;
    }

    @Override
    public native int sendOneFrameToDevice(
            int localEncoderChannelIndex,
            long remoteDeviceId,
            int remoteDeviceDecoderChannelIndex,
            String remoteDeviceIpPort,
            String Codec, // CODEC_VIDEO_H264, CODEC_AUDIO_MP3
            byte[] frameBuffer,
            int frameBytes,
            int imageResolution, // IMAGE_RESOLUTION_CIF, IMAGE_RESOLUTION_D1, IMAGE_RESOLUTION_QCIF
            int imageWidth,
            int imageHeight
    );
}
