package com.dy.dwvm_mt.messagestructs;

import com.dy.javastruct.StructClass;
import com.dy.javastruct.StructField;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/23.
 * PS: Not easy to write code, please indicate.
 */

@StructClass
public class s_DDNS_StatesMsg {
    @StructField(order = 0)
    public int CMD;

    @StructField(order = 1)
    public int Types;
}
