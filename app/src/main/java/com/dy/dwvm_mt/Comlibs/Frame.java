package com.dy.dwvm_mt.Comlibs;

public final class Frame {
    public byte[] mData;
    public int mdataSize;
    public int mwidth;
    public int mheight;
    public long mframeType;
    public String mCodecName;

    public Frame(String codecName, byte[] data, int width, int height, int size, long frameType) {
        mCodecName = codecName;
        mData = data;
        this.mdataSize = size;
        mwidth = width;
        mheight = height;
        mframeType = frameType;
    }

}
