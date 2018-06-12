package com.dy.dwvm_mt;

/**
 * Created on 2016.1.22
 * MT NDK Library
 */
public class MTLib
{
    static
    {
        System.loadLibrary("DwvmMTLib");
    }

    public static final String CODEC_VIDEO_H264 = "video/avc";
    public static final String CODEC_AUDIO_MP3 = "audio/mp3";

    public static final int IMAGE_RESOLUTION_CIF = 0;
    public static final int IMAGE_RESOLUTION_D1 = 1;
    public static final int IMAGE_RESOLUTION_QCIF = 2;

    public interface MTLibCallback
    {
        long onReceivedUdpPacket(
            long localDeviceId,
            String remoteDeviceIpPort,
            long remoteDeviceId,
            long packetCommandType,
            byte[] packetBuffer,
            int packetBytes
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

    private MTLibCallback m_callbackInstance = null;

    public void installCallback(MTLibCallback instance)
    {
        m_callbackInstance = instance;
    }

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
        int height)
    {
        // callback by JNI-MTLib, and dispatch to main app
        if (m_callbackInstance != null)
        {
            if (funcName.equalsIgnoreCase("onReceivedUdpPacket"))
            {
                return m_callbackInstance.onReceivedUdpPacket(
                    localDeviceId,
                    remoteDeviceIpPort,
                    remoteDeviceId,
                    dataType,
                    dataBuffer,
                    dataBytes);
            }
            else if (funcName.equalsIgnoreCase("onReceivedVideoFrame"))
            {
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
            }
            else if (funcName.equalsIgnoreCase("onReceivedAudioFrame"))
            {
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

    public native boolean start(
        long localDeviceId,
        int udpPort,
        int udpSocketBufferSize,
        int netEncryptMode,
        int encoderChannelNumber,
        int decoderChannelNumber,
        String optionText
    );

    public native void stop();

    public native boolean isWorking();

    public native boolean setDeviceName(String deviceName);

    public native int dataEncrypt(byte[] srcData, int srcSize, byte[] destData);

    public native int dataDecrypt(byte[] srcData, int srcSize, byte[] destData);

    public native String stringEncrypt(String srcText);

    public native String stringDecrypt(String srcText);

    public native int sendUdpPacketToDevice(
        long packetType,
        long needReplay,
        long destDeviceId,
        String destDeviceIpPort,
        byte[] dataBuffer,
        int dataSize
    );

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
