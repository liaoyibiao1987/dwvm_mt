package com.dy.dwvm_mt.messagestructs;

import com.dy.javastruct.ForceLength;
import com.dy.javastruct.StructClass;
import com.dy.javastruct.StructField;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/26.
 * PS: Not easy to write code, please indicate.
 */
@StructClass
public class s_MT_Tel_ValidCode {
    /// <summary>
    /// //二级命令命令码
    /// </summary>
    @StructField(order = 0)
    public int CMD_Sub;

    /// <summary>
    /// 验证码
    /// </summary>
    @StructField(order = 1)
    public int Code;

    /// <summary>
    /// 被叫号码
    /// </summary>
    @StructField(order = 2)
    @ForceLength(forceLen = s_messageBase.WVM_MAX_TELPSTATE_CODE_LEN)
    public byte[] CalledNumber;
}
