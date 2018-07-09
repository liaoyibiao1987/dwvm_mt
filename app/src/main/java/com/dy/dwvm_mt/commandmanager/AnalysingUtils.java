package com.dy.dwvm_mt.commandmanager;

import com.dy.dwvm_mt.Comlibs.DataPackShell;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.messagestructs.NetWorkCommand;
import com.dy.dwvm_mt.messagestructs.ReceivePackEntity;
import com.dy.dwvm_mt.messagestructs.s_CMDReply;
import com.dy.dwvm_mt.messagestructs.s_headPack;
import com.dy.dwvm_mt.messagestructs.s_messageBase;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.javastruct.JavaStruct;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/4.
 * PS: Not easy to write code, please indicate.
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
                while (isRunning) {
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
        }.start();

    }

    /**
     * 封装好并解析过的数据包提供给应用程序使用
     * @param handler NWCommandEvent Handler
     */
    public static void addRecvedCommandListeners(NWCommandEventHandler handler) {
        eventListeners.add(handler);
    }

    public static void removeRecvedCommandListeners(NWCommandEventHandler handler) {
        eventListeners.remove(handler);
    }

    protected static void notifieRecvedCommands(NWCommandEventArg arg) {
        if (eventListeners.size() > 0) {
            for (NWCommandEventHandler handler : eventListeners) {
                handler.doHandler(arg);
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
                    ReceivePackEntity rp = new ReceivePackEntity(e.getbagBuffer(), e.getbagSize(), e.getbagType(), srcIpPort);
                    if (e.getbagType() != s_messageBase.DeviceCMD.WVM_CMD_REPLY) {              //不是回应包
                        if (e.getbagType() != 207) {
                            NetWorkCommand peercommand = new NetWorkCommand(rp);
                            addCommand(peercommand);
                        }

                        s_headPack s_head = new s_headPack();
                        JavaStruct.unpack(s_head, rp.getBagBuffer());
                        int SrcID = s_head.dwSrcId;//来源ID
                        int dwReplyContext = s_head.dwReplyContext;//发送给设备的对应ID
                        int dwSeq = s_head.dwSeq;

                        if (dwReplyContext != 0) {
                            s_CMDReply s_re = new s_CMDReply(20, s_head.dwCmd, s_head.dwSeq, s_head.dwSendingTick, s_head.dwReplyContext);
                            byte[] Data = (byte[]) JavaStruct.pack(s_re);
                            mtLib.sendUdpPacketToDevice2(s_messageBase.DeviceCMD.WVM_CMD_REPLY, 0, SrcID, srcIpPort, Data, Data.length);
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
