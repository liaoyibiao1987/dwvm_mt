package com.dy.dwvm_mt.messagestructs;

import com.dy.javastruct.StructClass;
import com.dy.javastruct.StructField;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/4.
 * PS: Not easy to write code, please indicate.
 */
@StructClass
public class s_headPack {
    /// <summary>
    /// 命令开始码，必须等于0x4D565744
    /// </summary>
    @StructField(order = 0)
    public int dwStartCode;
    /// <summary>
    /// 本结构的长度
    /// </summary>
    @StructField(order = 1)
    public int dwSize;
    /// <summary>
    /// 命令类型
    /// </summary>
    @StructField(order = 2)
    public int dwCmd;
    /// <summary>
    /// 本数据包的序号。最高位为1的数据包由模块内部处理，为0的数据包由应用软件处理。
    /// </summary>
    @StructField(order = 3)
    public int dwSeq;
    /// <summary>
    /// 发送时刻的时间戳，配合应答包可以统计网络延时时间。
    /// </summary>
    @StructField(order = 4)
    public int dwSendingTick;
    /// <summary>
    /// 0表示接收方不需要应答，否则接收方需要回复应答包，并将该字段在应答包中返回
    /// </summary>
    @StructField(order = 5)
    public int dwReplyContext;
    /// <summary>
    /// 发送者的ID
    /// </summary>
    @StructField(order = 6)
    public int dwSrcId;
    /// <summary>
    /// 目标接收者的ID
    /// </summary>
    @StructField(order = 7)
    public int dwDestId;
    /// <summary>
    /// 结构体后跟着的数据长度
    /// </summary>
    @StructField(order = 8)
    public int dwDataSize;
    /// <summary>
    /// 结构体后跟着的数据的字节和. 发送时如果有加密，则先加密、再求和
    /// </summary>
    @StructField(order = 9)
    public int dwDataByteSum;
    /// <summary>
    /// 数据是否加密。最高4bit表示加密模式，0表示无加密。其他28bit为加密因子。包头（即本结构）不加密。
    /// </summary>
    @StructField(order = 10)
    public int dwEncrypt;
}
