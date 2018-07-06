package com.dy.dwvm_mt.messagestructs;

import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.javastruct.JavaStruct;

import java.util.Arrays;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/4.
 * PS: Not easy to write code, please indicate.
 */
public class ReceivePackEntity {
    public byte[] getBagBuffer() {
        return bagBuffer;
    }

    public void setBagBuffer(byte[] bagBuffer) {
        this.bagBuffer = bagBuffer;
    }

    public int getBagSize() {
        return bagSize;
    }

    public void setBagSize(int bagSize) {
        this.bagSize = bagSize;
    }

    public int getBagType() {
        return bagType;
    }

    public void setBagType(int bagType) {
        this.bagType = bagType;
    }

    public String getSzSrcIpPort() {
        return szSrcIpPort;
    }

    public void setSzSrcIpPort(String szSrcIpPort) {
        this.szSrcIpPort = szSrcIpPort;
    }

    public s_headPack getHeadPack() {
        s_headPack header = new s_headPack();
        byte[] data = new byte[44];
        System.arraycopy(getBagBuffer(), 0, data, 0, 44);
        try {
            JavaStruct.unpack(header, data);
        } catch (Exception es) {
            LogUtils.e("ReceivePackEntity getHeadPack error :" + es);
        }
        return header;
    }

    public ReceivePackEntity() {

    }

    public ReceivePackEntity(byte[] bagbuffer, int bagsize, int bagtype, String szsrcIPPort) {
        bagBuffer = bagbuffer;
        bagSize = bagsize;
        bagType = bagtype;
        szSrcIpPort = szsrcIPPort;
    }

    /// <summary>
    /// 收到的数据
    /// </summary>
    private byte[] bagBuffer;
    /// <summary>
    /// 数据大小
    /// </summary>
    private int bagSize;
    /// <summary>
    /// 一级命令
    /// </summary>
    private int bagType;
    /// <summary>
    /// 来源IP地址
    /// </summary>
    private String szSrcIpPort;
}
