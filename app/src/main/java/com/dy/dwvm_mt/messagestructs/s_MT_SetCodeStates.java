package com.dy.dwvm_mt.messagestructs;

import com.dy.javastruct.ForceLength;
import com.dy.javastruct.StructClass;
import com.dy.javastruct.StructField;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/24.
 * PS: Not easy to write code, please indicate.
 */

@StructClass
public class s_MT_SetCodeStates {
    /// <summary>
    /// 二级命令命令码
    /// </summary>
    @StructField(order = 0)
    public int CMD_Sub;
    /// <summary>
    /// 编码通道号
    /// </summary>
    @StructField(order = 1)
    public int Channel;
    /// <summary>
    /// 图像分辨
    /// </summary>
    @StructField(order = 2)
    public int ImagerType;
    /// <summary>
    /// 视频帧模式: 是否仅仅发送视频关键帧
    /// </summary>
    @StructField(order = 3)
    public int StreamType;
    /// <summary>
    /// 编码时向此IP端口发送视频，
    /// </summary>
    @StructField(order = 4)
    @ForceLength(forceLen = s_messageBase.WVM_MAX_DESIPADDR_LEN)
    public char[] DesIPAddr;
    /// <summary>
    /// 流程ID，如果此命令需要返回值时，在回应包需要带上此字段，没有则填0
    /// </summary>
    @StructField(order = 5)
    public int FlowID;
    /// <summary>
    /// 目标设备ID
    /// </summary>
    @StructField(order = 6)
    public int DesPSID;

    // True-打开  False-关闭 （对应某个格式通道）
    @StructField(order = 7)
    public boolean Switch;

    @StructField(order = 8)
    public byte videoSoruceTypes;

    @StructField(order = 9)
    public byte DesChannel;
}
