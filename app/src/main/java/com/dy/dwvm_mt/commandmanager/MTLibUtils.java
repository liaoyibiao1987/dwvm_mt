package com.dy.dwvm_mt.commandmanager;

import com.dy.dwvm_mt.Comlibs.DataPackShell;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.MTLib;
import com.dy.dwvm_mt.messagestructs.NetWorkCommand;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/6.
 * PS: Not easy to write code, please indicate.
 */
public class MTLibUtils {
    // MT Library
    private static I_MT_Prime m_mtLib = null;
    private static Set<DY_onReceivedPackEventHandler> eventListeners;

    public static I_MT_Prime getBaseMTLib() {
        return m_mtLib;
    }
    public static void Initialisation(){
        // 方法一定要调用一次
        // java语言的加载模式决定了static 静态构造方法不能主动加载。必须调用一次任意方法或者初始化类。
    }

    static {
        eventListeners = new LinkedHashSet();
        setupMTLib();
        try {
            Thread.sleep(1000);
            DataPackShell.setOnReceiveFullPacket(new DataPackShell.OnReciveFullPacketListener() {
                @Override
                public void onReviced(DataPackShell.ReceivedPackEntity entity) {
                    if (eventListeners.size() > 0) {
                        for (DY_onReceivedPackEventHandler handler : eventListeners) {
                            handler.onReceivedUdpPacket(entity);
                        }
                    }
                }
            });
            AnalysingUtils.setupMTLib(m_mtLib);
            AnalysingUtils.startReviceData();
            AnalysingUtils.addRecvedCommandListeners(new NWCommandEventHandler() {
                @Override
                public void doHandler(NWCommandEventArg arg) {
                    NetWorkCommand command = arg.getEventArg();
                    LogUtils.e("收到回应包了：" + command);
                }
            });
            CommandUtils.initSetupAdapter(m_mtLib);

        } catch (Exception es) {
            LogUtils.e("MTLibUtils error:" + es);
        }

    }

    public static void setupMTLib() {
        if (m_mtLib == null) {
            try {
                m_mtLib = new MTLib();
                if (m_mtLib.isWorking() == false) {
                    m_mtLib.installCallback(new I_MT_Prime.MTLibCallback() {
                        @Override
                        public long onReceivedUdpPacket(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, long packetCommandType, byte[] packetBuffer, int packetBytes) {
                            DataPackShell.ParseBuff(packetBuffer, (int) packetCommandType, remoteDeviceIpPort);
                            return packetBytes;
                        }

                        @Override
                        public long onReceivedVideoFrame(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, int remoteEncoderChannelIndex, int localDecoderChannelIndex, long frameType, String videoCodec, int imageResolution, int width, int height, byte[] frameBuffer, int frameSize) {
                            if (eventListeners.size() > 0) {
                                for (DY_onReceivedPackEventHandler handler : eventListeners) {
                                    handler.onReceivedVideoFrame(localDeviceId, remoteDeviceIpPort, remoteDeviceId, remoteEncoderChannelIndex, localDecoderChannelIndex, frameType, videoCodec, imageResolution, width, height, frameBuffer, frameSize);
                                }
                            }
                            return eventListeners.size();
                        }

                        @Override
                        public long onReceivedAudioFrame(long localDeviceId, String remoteDeviceIpPort, long remoteDeviceId, int remoteEncoderChannelIndex, int localDecoderChannelIndex, String audioCodec, byte[] frameBuffer, int frameSize) {
                            if (eventListeners.size() > 0) {
                                for (DY_onReceivedPackEventHandler handler : eventListeners) {
                                    handler.onReceivedAudioFrame(localDeviceId, remoteDeviceIpPort, remoteDeviceId, remoteEncoderChannelIndex, localDecoderChannelIndex, audioCodec, frameBuffer, frameSize);
                                }
                            }
                            return eventListeners.size();
                        }
                    });
                    if (!m_mtLib.start(0x04000009, CommandUtils.MTPORT, 1024 * 1024, 0, 1, 1, "")) {
                        LogUtils.e("MTLib.start() failed !");
                        return;
                    }
                    m_mtLib.setDeviceName("MT");
                } else {
                    LogUtils.d("MTLib is already started !");
                }
            } catch (Exception e) {
                LogUtils.e("MTLib.start() error: " + e.getMessage());
            }
            //m_mtLib.setDeviceName(LOCAL_DEVICE_NAME);
        } else {
            LogUtils.e("主件已经初始化过了.");
        }
    }

    public static void addRecvedCommandListeners(DY_onReceivedPackEventHandler handler) {
        eventListeners.add(handler);
    }

    public static void removeRecvedCommandListeners(DY_onReceivedPackEventHandler handler) {
        eventListeners.remove(handler);
    }

}
