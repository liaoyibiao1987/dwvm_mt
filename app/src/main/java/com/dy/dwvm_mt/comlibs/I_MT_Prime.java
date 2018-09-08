package com.dy.dwvm_mt.comlibs;

public interface I_MT_Prime {
    void installCallback(MTLibCallback instance);

    long callbackFromJNI(
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
            int height);

    boolean start(
            long localDeviceId,
            int udpPort,
            int udpSocketBufferSize,
            int netEncryptMode,
            int encoderChannelNumber,
            int decoderChannelNumber,
            String optionText
    );

    void stop();

    boolean isWorking();

    boolean setDeviceName(String deviceName);

    boolean setDeviceId(long deviceID);

    int dataEncrypt(byte[] srcData, int srcSize, byte[] destData);

    int dataDecrypt(byte[] srcData, int srcSize, byte[] destData);

    String stringEncrypt(String srcText);

    String stringDecrypt(String srcText);

    @Deprecated()
    @SuppressWarnings({"deprecation", "removal"})
    int sendUdpPacketToDevice(
            long packetType,
            long needReplay,
            long destDeviceId,
            String destDeviceIpPort,
            byte[] dataBuffer,
            int dataSize
    );

    int sendUdpPacketToDevice2(
            long packetType,
            long needReplay,
            long destDeviceId,
            String destDeviceIpPort,
            byte[] dataBuffer,
            int dataSize
    );


    int sendOneFrameToDevice(
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

    public interface MTLibCallback {
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


    public  interface MTLibReceivedVideoHandler {
        void onReceivedVideoFrames(
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
    }

}
