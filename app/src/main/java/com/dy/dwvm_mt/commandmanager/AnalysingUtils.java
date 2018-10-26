package com.dy.dwvm_mt.commandmanager;

import android.util.Log;

import com.dy.dwvm_mt.comlibs.DataPackShell;
import com.dy.dwvm_mt.comlibs.I_MT_Prime;
import com.dy.dwvm_mt.messagestructs.NetWorkCommand;
import com.dy.dwvm_mt.messagestructs.ReceivePackEntity;
import com.dy.dwvm_mt.messagestructs.s_CMDReply;
import com.dy.dwvm_mt.messagestructs.s_headPack;
import com.dy.dwvm_mt.messagestructs.s_messageBase;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.javastruct.JavaStruct;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/4.
 * PS: AnalysingUtils
 */
public class AnalysingUtils {
    private static int pollingCount;
    private static Object status;
    private static I_MT_Prime mtLib;
    private static Object locker = new Object();
    private static boolean isRunning = false;

    private static List<NetWorkCommand> receivedCommands = new ArrayList<>();
    private static Set<NWCommandEventHandler> eventListeners;


    public static void setupMTLib(I_MT_Prime mtlib) {
        mtLib = mtlib;
    }

    static {
        eventListeners = new LinkedHashSet<NWCommandEventHandler>();
        new Thread() {
            public void run() {
                while (true) {
                    if (isRunning == true) {
                        NetWorkCommand command = popCommand();
                        if (command != null) {
                            try {
                                NWCommandEventArg arg = new NWCommandEventArg(command);
                                notifieRecvedCommands(arg);
                            } catch (Exception es) {
                                LogUtils.e("AnalysingUtils Thread error:" + es);
                            }
                        }
                    }
                }
            }
        }.start();

    }

    /**
     * 封装好并解析过的数据包提供给应用程序使用
     *
     * @param handler NWCommandEvent Handler
     */
    public static void addReceivedCommandListeners(NWCommandEventHandler handler) {
        eventListeners.add(handler);
    }

    public static void removeReceivedCommandListeners(NWCommandEventHandler handler) {
        if (handler == null) {
            eventListeners.clear();
        } else {
            if (eventListeners.contains(handler) == false) {
                eventListeners.remove(handler);
            }
        }
    }

    protected static void notifieRecvedCommands(NWCommandEventArg arg) {
        if (eventListeners.size() > 0) {
            for (NWCommandEventHandler handler : eventListeners) {
                handler.doNWCommandHandler(arg);
            }
        }
    }

    public static boolean addCommand(NetWorkCommand command) {
        synchronized (locker) {
            receivedCommands.add(command);
            return true;
        }
    }

    public static NetWorkCommand popCommand() {
        synchronized (locker) {
            if (receivedCommands.size() > 0) {
                NetWorkCommand pop1 = receivedCommands.get(0);
                receivedCommands.remove(0);
                return pop1;
            } else {
                return null;
            }
        }
    }

    public static void startReviceData() {
        if (isRunning == false) {
            setReceiveFull();
        }
        isRunning = true;
    }

    /**
     * 调用setOnReceiveFullPacket来监听从DataPackShell收取到的所有经过组合解析的报文.
     * 将初略组合的ReceivePackEntity进一步分析为NetWorkCommand深度组合包.
     */
    private static void setReceiveFull() {
        DataPackShell.setOnReceiveFullPacket(new DataPackShell.OnReciveFullPacketListener() {
            @Override
            public void onReviced(DataPackShell.ReceivedPackEntity e) {
                try {
                    String srcIpPort = e.getszSrcIpPort();
                    if (srcIpPort.startsWith("127.0.0.1") == false) {
                        pollingCount = 0;
                        /*if (status != NetworkStatus.Normal)
                        {

                        }*/
                    }
                    Log.d("mt setReceiveFull", " 收到网络报：bagType -> " + e.getbagType());
                    //LogUtils.d("收到网络报：bagType -> " + e.getbagType());
                    ReceivePackEntity rp = new ReceivePackEntity(e.getbagBuffer(), e.getbagSize(), e.getbagType(), srcIpPort);
                    if (e.getbagType() != s_messageBase.DeviceCMD.WVM_CMD_REPLY) {              //不是回应包
                        if (e.getbagType() != 207) {
                            NetWorkCommand peercommand = new NetWorkCommand(rp);
                            addCommand(peercommand);
                        }

                        s_headPack s_head = new s_headPack();
                        JavaStruct.unpack(s_head, rp.getBagBuffer());

                        if (s_head.dwReplyContext != 0) {////发送到设备的对方ID
                            s_CMDReply s_re = new s_CMDReply(20, s_head.dwCmd, s_head.dwSeq, s_head.dwSendingTick, s_head.dwReplyContext);
                            byte[] Data = JavaStruct.pack(s_re);
                            mtLib.sendUdpPacketToDevice2(s_messageBase.DeviceCMD.WVM_CMD_REPLY, 0, s_head.dwSrcId, srcIpPort, Data, Data.length);
                        }
                    } else {
                        s_CMDReply s_reply = new s_CMDReply();                                  //回应包
                        try {
                            JavaStruct.unpack(s_reply, e.getbagBuffer());//获取到登录包结构数据
                            if (s_reply.getDwSrcCmd() != s_messageBase.DeviceCMD.WVM_CMD_POLLING) {
                                NetWorkCommand peercommand = new NetWorkCommand(rp);
                                addCommand(peercommand);

                                /*  移除多次发送的命令    m_devices.Lock(() = >
                                        {
                                int index = m_devices.FindIndex(x = > x.Seq == s_reply.dwSrcSeq && x.CMD == s_reply.dwSrcCmd && x.IsNeedReturn > 0)
                                ;
                                if (index > -1) m_devices.AnsycRemoveAt(index);
                                });*/
                            }
                        } catch (Exception ex) {
                            LogUtils.e("bufferhelper_event_ReceiceFullBuffer 发生异常：", ex.toString());
                        }
                    }

                } catch (Exception ee) {
                    LogUtils.e("网络包接收事件" + ee.toString());
                }
            }
        });
    }
}
