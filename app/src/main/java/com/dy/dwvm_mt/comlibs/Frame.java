package com.dy.dwvm_mt.comlibs;

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

    public int getMdataSize() {
        return mdataSize;
    }


    public Frame(byte[] data, int size, long frameType) {
        mData = new byte[size];
        System.arraycopy(data, 0, mData, 0, size);
        mdataSize = size;
        mFrameType = frameType;
    }

}
