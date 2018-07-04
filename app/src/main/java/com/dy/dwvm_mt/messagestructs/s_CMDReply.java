package com.dy.dwvm_mt.messagestructs;

import com.dy.javastruct.StructClass;
import com.dy.javastruct.StructField;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/4.
 * PS: Not easy to write code, please indicate.
 */
@StructClass
public class s_CMDReply {

    public int getDwSize() {
        return dwSize;
    }

    public void setDwSize(int dwSize) {
        this.dwSize = dwSize;
    }

    public int getDwSrcCmd() {
        return dwSrcCmd;
    }

    public void setDwSrcCmd(int dwSrcCmd) {
        this.dwSrcCmd = dwSrcCmd;
    }

    public int getDwSrcSeq() {
        return dwSrcSeq;
    }

    public void setDwSrcSeq(int dwSrcSeq) {
        this.dwSrcSeq = dwSrcSeq;
    }

    public int getDwSrcTick() {
        return dwSrcTick;
    }

    public void setDwSrcTick(int dwSrcTick) {
        this.dwSrcTick = dwSrcTick;
    }

    public int getDwSrcContext() {
        return dwSrcContext;
    }

    public void setDwSrcContext(int dwSrcContext) {
        this.dwSrcContext = dwSrcContext;
    }

    public s_CMDReply() {

    }

    public s_CMDReply(int dwsize, int dwsrcCmd, int dwsrcSeq, int dwsrcTick, int dwsrcContext) {
        dwSize = dwsize;
        dwSrcCmd = dwsrcCmd;
        dwSrcSeq = dwsrcSeq;
        dwSrcTick = dwsrcTick;
        dwSrcContext = dwsrcContext;
    }

    @StructField(order = 0)
    private int dwSize;

    @StructField(order = 1)
    private int dwSrcCmd;

    @StructField(order = 2)
    private int dwSrcSeq;

    @StructField(order = 3)
    private int dwSrcTick;

    @StructField(order = 4)
    private int dwSrcContext;
}
