package com.dy.dwvm_mt.Comlibs;

public final class Frame {
    private byte[] mData;
    private int mdataSize;
    private long mFrameType;

    public long getFrameType() {
        return mFrameType;
    }

    public void setFrameType(long frameType) {
        this.mFrameType = frameType;
    }

    public byte[] getmData() {
        return mData;
    }

    public void setmData(byte[] mData) {
        this.mData = mData;
    }

    public int getMdataSize() {
        return mdataSize;
    }

    public void setMdataSize(int mdataSize) {
        this.mdataSize = mdataSize;
    }


    public Frame(byte[] data, int size, long frameType) {
        mData = data;
        mdataSize = size;
        mFrameType = frameType;
    }

}
