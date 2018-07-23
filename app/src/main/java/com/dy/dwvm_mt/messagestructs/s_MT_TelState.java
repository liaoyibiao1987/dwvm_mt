package com.dy.dwvm_mt.messagestructs;

import com.dy.javastruct.ForceLength;
import com.dy.javastruct.StructClass;
import com.dy.javastruct.StructField;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/23.
 * PS: Not easy to write code, please indicate.
 */
@StructClass
public class s_MT_TelState {
    /// <summary>
    /// //二级命令命令码
    /// </summary>
    @StructField(order = 0)
    public int CMD_Sub;	//二级命令命令码
    /// <summary>
    /// //会议ID没有则填0
    /// </summary>
    @StructField(order = 1)
    public int MeetingID;	//没有则填0
    /// <summary>
    /// 话机状态0:空闲；1:忙；2:为振铃；3:被叫号码；4:来电号码；5:主叫摘机；6:为被叫摘机；7:为挂机；
    /// </summary>
    @StructField(order =2)
    public int TelState;//	话机状态0:空闲；1:忙；2:为振铃；3:被叫号码；4:来电号码；5:主叫摘机；6:为被叫摘机；7:为挂机；8:为停止振铃；9:新的振铃
    /// <summary>
    /// 被叫号码/来电号码/其它信息(根据TelState类型赋值)，没有则填空字符串；
    /// </summary>
    @StructField(order = 3)
    @ForceLength(forceLen = s_messageBase.WVM_MAX_TELPSTATE_CODE_LEN)
    public byte[] Data = new byte[s_messageBase.WVM_MAX_TELPSTATE_CODE_LEN];//被叫号码/来电号码/被叫摘机时上报通道验证码/其它信息(根据TelState类型赋值)，没有则填空字符串；
}
