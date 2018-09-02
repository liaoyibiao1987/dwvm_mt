package com.dy.dwvm_mt.comlibs;

import com.dy.dwvm_mt.utilcode.constant.TimeConstants;
import com.dy.dwvm_mt.utilcode.util.ConvertUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.TimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/6/28.
 * PS: 收发数据包的加壳，解壳程序 (DDNS用了一套超大数据包的加壳解壳流程)
 */
public class DataPackShell {

    public interface OnReciveFullPacketListener {
        void onReviced(ReceivedPackEntity databuff);
    }


    /**
     * 收到一个完整包事件
     */
    private static OnReciveFullPacketListener receiveFullPacketHandler;

    /**
     * 完整包事件代理(包没有解析过，是只单纯的完整包数据 不同于 AnalysingUtils <CommandListeners>)
     *
     * @param listener
     */
    public static void setOnReceiveFullPacket(OnReciveFullPacketListener listener) {
        receiveFullPacketHandler = listener;
    }


    private static ArrayList<CachData> listCachData = new ArrayList<CachData>();
    private static Random rd = new Random(Integer.MAX_VALUE);
    public static Object listlocker = new Object();

    /**
     * 获取待发送
     *
     * @param buffer
     * @return
     */
    public static final ArrayList<byte[]> GetSendBuff(byte[] buffer) {
        ArrayList<byte[]> list = new ArrayList();
        int UniqueID = GetUniqueID();
//            拆成1024个字节
//             * UniqueID 4
//             * PackCount 4
//             * PackIndex 4
//             * DataLength 4
//             * Data  1024
//
        int packcount = buffer.length / 1024;
        if ((buffer.length % 1024) > 0) {
            packcount++;
        }
        for (int i = 0; i < packcount; i++) {


            if (i == packcount - 1) {
                int datalen = buffer.length % 1024;
                //最后一个包
                byte[] sendbuff = new byte[datalen + 16];
                System.arraycopy(ConvertUtils.int2byte(UniqueID), 0, sendbuff, 0, 4);
                System.arraycopy(ConvertUtils.int2byte(packcount), 0, sendbuff, 4, 4);
                System.arraycopy(ConvertUtils.int2byte(i), 0, sendbuff, 8, 4);
                System.arraycopy(ConvertUtils.int2byte(datalen), 0, sendbuff, 12, 4);

                System.arraycopy(buffer, i * 1024, sendbuff, 16, datalen);
                list.add(sendbuff);
            } else {
                byte[] sendbuff = new byte[1024 + 16];

                System.arraycopy(ConvertUtils.int2byte(UniqueID), 0, sendbuff, 0, 4);
                System.arraycopy(ConvertUtils.int2byte(packcount), 0, sendbuff, 4, 4);
                System.arraycopy(ConvertUtils.int2byte(i), 0, sendbuff, 8, 4);
                System.arraycopy(ConvertUtils.int2byte(1024), 0, sendbuff, 12, 4);
                System.arraycopy(buffer, i * 1024, sendbuff, 16, 1024);
                list.add(sendbuff);
            }

        }
        return list;
    }

    /**
     * 收到的小包组合成一个后会通过事件receiveFullBuffer反馈
     *
     * @param buffer      数据
     * @param bagType     1级命令
     * @param szSrcIpPort 来源ip端口
     */
    public static final void ParseBuff(byte[] buffer, int bagType, String szSrcIpPort) {
        try {
            //LogUtils.d("收到包" + StringUtils.toHexBinary(buffer), " datasize :" + buffer.length);
            byte[] newbuff = new byte[1084];
            if (buffer.length != 1084) {
                if (buffer.length < 16) {
                    return;
                } else {
                    System.arraycopy(buffer, 0, newbuff, 0, buffer.length);
                }
            } else {
                newbuff = buffer;
            }

            byte[] bufferWithNohead = new byte[1040];
            System.arraycopy(newbuff, 44, bufferWithNohead, 0, 1040);
            int UniqueID = ConvertUtils.byte2int(bufferWithNohead, 0);
            int PackCount = ConvertUtils.byte2int(bufferWithNohead, 4);
            int PackIndex = ConvertUtils.byte2int(bufferWithNohead, 8);
            int DataLength = ConvertUtils.byte2int(bufferWithNohead, 12);

            if (PackCount == 0) {
                //polling
                ReceivedPackEntity reEntity = new ReceivedPackEntity();
                reEntity.setbagBuffer(new byte[44]);
                System.arraycopy(buffer, 0, reEntity.getbagBuffer(), 0, 44);
                reEntity.setbagSize(44);
                reEntity.setbagType(bagType);
                reEntity.setszSrcIpPort(szSrcIpPort);
                if (receiveFullPacketHandler != null) {
                    receiveFullPacketHandler.onReviced(reEntity);
                }
            } else {
                byte[] rdata = new byte[DataLength];
                System.arraycopy(bufferWithNohead, 16, rdata, 0, DataLength);
                synchronized (listlocker) {
                    CachData entity = null;
                    for (Iterator<CachData> ite = listCachData.iterator(); ite.hasNext(); ) {
                        CachData data = ite.next();
                        if (data.getUniqueID() == UniqueID) {
                            entity = data;
                            break;
                        }
                    }
                    if (entity == null) {
                        for (Iterator<CachData> ite = listCachData.iterator(); ite.hasNext(); ) {
                            CachData data = ite.next();
                            if (TimeUtils.getTimeSpan(new Date(), data.getR_times(), TimeConstants.MIN) > 1) {
                                ite.remove();
                            }
                        }
                        entity = new CachData();
                        entity.setUniqueID(UniqueID);
                        entity.setReceivePackageCount(1);
                        entity.setReceiveDataLength(DataLength);
                        entity.setR_times(new Date());
                        entity.setDataList(new ArrayList<byte[]>());
                        for (int i = 0; i < PackCount; i++) {
                            entity.getDataList().add(new byte[1024]);
                        }
                        entity.getDataList().set(PackIndex, rdata);
                        entity.setbagType(bagType);
                        entity.setszSrcIpPort(szSrcIpPort);
                        listCachData.add(entity);

                    } else {
                        entity.setReceivePackageCount(entity.getReceivePackageCount() + 1);
                        entity.setReceiveDataLength(entity.getReceiveDataLength() + DataLength);
                        entity.getDataList().set(PackIndex, rdata);
                    }

                    if (entity.getReceivePackageCount() == PackCount) {
                        //收到完整包
                        byte[] ReData = new byte[entity.getReceiveDataLength() + 44];
                        System.arraycopy(newbuff, 0, ReData, 0, 44);

                        for (int i = 0; i < entity.getDataList().size(); i++) {
                            if (i == entity.getDataList().size() - 1) {
                                int len = entity.getReceiveDataLength() % 1024;
                                //最后一个包长度可能不是1024的
                                System.arraycopy(entity.getDataList().get(i), 0, ReData, i * 1024 + 44, len);
                            } else {
                                //1024个字节
                                System.arraycopy(entity.getDataList().get(i), 0, ReData, i * 1024 + 44, 1024);
                            }
                        }
                        ReceivedPackEntity reEntity = new ReceivedPackEntity();
                        reEntity.setbagBuffer(ReData);
                        reEntity.setbagSize(entity.getReceiveDataLength() + 44);
                        reEntity.setbagType(entity.getbagType());
                        reEntity.setszSrcIpPort(entity.getszSrcIpPort());
                        if (receiveFullPacketHandler != null) {
                            //LogUtils.d("收到包2" + StringUtils.toHexBinary(reEntity.getbagBuffer()), " getbagBuffer size :" + reEntity.getbagBuffer());
                            receiveFullPacketHandler.onReviced(reEntity);
                        }
                        for (Iterator<CachData> ite = listCachData.iterator(); ite.hasNext(); ) {
                            CachData data = ite.next();
                            if (data.getUniqueID() == entity.getUniqueID()) {
                                ite.remove();
                            }
                        }
                    }
                }
            }

        } catch (java.lang.Exception e) {
            LogUtils.e("receiver error :" + e);
        }
    }

    //随机不重复数
    private static int GetUniqueID() {
        return rd.nextInt(Integer.MAX_VALUE);
    }

    public static class ReceivedPackEntity {
        /**
         * 收到的数据
         */
        private byte[] privatebagBuffer;

        public final byte[] getbagBuffer() {
            return privatebagBuffer;
        }

        public final void setbagBuffer(byte[] value) {
            privatebagBuffer = value;
        }

        /**
         * 数据大小
         */
        private int privatebagSize;

        public final int getbagSize() {
            return privatebagSize;
        }

        public final void setbagSize(int value) {
            privatebagSize = value;
        }

        /**
         * 一级命令
         */
        private int privatebagType;

        public final int getbagType() {
            return privatebagType;
        }

        public final void setbagType(int value) {
            privatebagType = value;
        }

        /**
         * 来源IP地址
         */
        private String privateszSrcIpPort;

        public final String getszSrcIpPort() {
            return privateszSrcIpPort;
        }

        public final void setszSrcIpPort(String value) {
            privateszSrcIpPort = value;
        }
    }

    private static class CachData {
        private int privateUniqueID;

        public final int getUniqueID() {
            return privateUniqueID;
        }

        public final void setUniqueID(int value) {
            privateUniqueID = value;
        }

        private Date privateR_times = new Date(0);

        public final Date getR_times() {
            return privateR_times;
        }

        public final void setR_times(Date value) {
            privateR_times = value;
        }

        private int privateReceivePackageCount;

        public final int getReceivePackageCount() {
            return privateReceivePackageCount;
        }

        public final void setReceivePackageCount(int value) {
            privateReceivePackageCount = value;
        }

        private int privateReceiveDataLength;

        public final int getReceiveDataLength() {
            return privateReceiveDataLength;
        }

        public final void setReceiveDataLength(int value) {
            privateReceiveDataLength = value;
        }

        private ArrayList<byte[]> privateDataList;

        public final ArrayList<byte[]> getDataList() {
            return privateDataList;
        }

        public final void setDataList(ArrayList<byte[]> value) {
            privateDataList = value;
        }

        /**
         * 一级命令
         */
        private int privatebagType;

        public final int getbagType() {
            return privatebagType;
        }

        public final void setbagType(int value) {
            privatebagType = value;
        }

        /**
         * 来源IP地址
         */
        private String privateszSrcIpPort;

        public final String getszSrcIpPort() {
            return privateszSrcIpPort;
        }

        public final void setszSrcIpPort(String value) {
            privateszSrcIpPort = value;
        }
    }
}
