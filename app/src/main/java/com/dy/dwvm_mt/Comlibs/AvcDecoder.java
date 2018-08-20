package com.dy.dwvm_mt.Comlibs;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.SurfaceHolder;

import com.dy.dwvm_mt.utilcode.util.LogUtils;

import java.nio.ByteBuffer;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/8/11.
 * PS: Not easy to write code, please indicate.
 * YUV420有打包格式(Packed)，一如前文所述。同时还有平面格式(Planar)，即Y、U、V是分开存储的，每个分量占一块地方，其中Y为 width*height，而U、V合占Y的一半，该种格式每个像素占12比特。
 * 根据U、V的顺序，分出2种格式
 * <p>
 * U前V后即YUV420P，也叫 I420(标准420)
 * V前U后，叫YV12(YV表示Y后面跟着V，12表示12bit)。叫YVU420P_YV_12 = 叫YV12
 * <p>
 * 另外，还有一种半平面格式(Semi-planar)， 即Y单独占一块地 方，但其后U、V又紧挨着排在一起，根据U、V的顺序，
 * 又有2种，
 * U前V后叫NV12，在国内好像很多人叫它为YUV420SP格式；
 * V前U后叫 NV21。这种格式似乎比NV12稍受欢迎。
 * <p>
 * Image.Plane[] planes = image.getPlanes();
 * planes[0] 总是Y ,planes[1] 总是U(Cb)， planes[2]总是V(Cr)
 */


public class AvcDecoder {
    /**
     * 相机支持YV12（平面 YUV 4:2:0） 以及 NV21 （半平面 YUV 4:2:0），MediaCodec支持以下一个或多个：
     * .#19 COLOR_FormatYUV420Planar (I420)
     * .#20 COLOR_FormatYUV420PackedPlanar (also I420)
     * .#21 COLOR_FormatYUV420SemiPlanar (NV12)
     * .#39 COLOR_FormatYUV420PackedSemiPlanar (also NV12)
     * .#0x7f000100 COLOR_TI_FormatYUV420PackedSemiPlanar (also also NV12)
     * I420的数据布局相当于YV12，但是Cr和Cb却是颠倒的，就像NV12和NV21一样。所以如果你想要去处理相机拿到的YV12数据，可能会看到一些奇怪的颜色干扰
     */

    private MediaCodec m_decoder = null;
    private SurfaceHolder m_holder;
    private int m_width;
    private int m_height;
    private int m_rotation = 90;
    private final static int TIME_INTERNAL = 5;


    private boolean m_decoderCreateFailed = false;
    private boolean m_decoderValid = false;
    private String m_decoderCodecName = "";
    private boolean m_decodeWaitKeyFrame = true;

    private MediaCodec.BufferInfo decodeOutBufferInfo;
    private ByteBuffer[] decodeInputBuffers;
    private Object decoderLocker = new Object();

    public AvcDecoder(SurfaceHolder holder) {
        m_holder = holder;
    }

    private synchronized  boolean decoderStart(String codecName, int width, int height) {
        if (m_decoder != null) {
            return false;
        }

        if (m_holder == null) {
            LogUtils.e("decoderStart", "Get holder failed.");
            return false;
        }

        // create decoder
        try {
            m_decoder = MediaCodec.createDecoderByType(codecName);
        } catch (Exception e) {
            if (m_decoder != null) {
                m_decoder = null;
            }
            LogUtils.e("decoderStart", "Decoder create error: " + e.getMessage());
            return false;
        }

        // bind to surface, and start
        try {
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(codecName, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_ROTATION, m_rotation);
            m_decoder.configure(mediaFormat, m_holder.getSurface(), null, 0);
            m_decoder.start();
        } catch (Exception e) {
            LogUtils.e("decoderStart", "surfaceCreated(decoder) error: " + e.getMessage());
            return false;
        }

        m_decoderCodecName = codecName;
        m_decodeWaitKeyFrame = true;
        m_decoderValid = true;
        m_width = width;
        m_height = height;
        return true;
    }

    public void decoderStop() {
        m_decoderCreateFailed = true;
        m_decoderValid = false;
        if (m_decoder != null) {
            m_decoder.stop();
            m_decoder.release();
            m_decoder = null;
        }
        m_decoderCreateFailed = false;
    }

    int mCount = 0;
    public boolean decoderOneVideoFrame(String codecName, int width, int height, byte[] dataBuffer, int dataSize, long frameType) {
        // if no decoder, create it
        if (m_decoder == null) {
            // only try one to create decoder
            if (m_decoderCreateFailed) {
                return false;
            }
            if (decoderStart(codecName, width, height) == false) {
                m_decoderCreateFailed = true;
                return false;
            }
            m_decoderCreateFailed = false;
        }
        if (!m_decoderValid) {
            return false;
        }

        // if codec or image-resolution changed, NOT process it
        // maybe, you can test: decoderStop() then decoderStart()
        if (!m_decoderCodecName.equalsIgnoreCase(codecName) || m_width != width || m_height != height) {
            decoderStop();
            return false;
        }

        // wait key-frame at first
        if (m_decodeWaitKeyFrame) {
            if (0 != frameType) //0- I frame, 1- P frame
            {
                return true;
            } else {
                m_decodeWaitKeyFrame = false;
            }
        }
        synchronized (decoderLocker) {
            LogUtils.d(String.format("codecName %s, width %d, height %d, frameType %d", codecName, width, height, frameType));
            // decode frame
            try {
                decodeInputBuffers = m_decoder.getInputBuffers();
                int decodeInputBufferIndex = m_decoder.dequeueInputBuffer(100);
                if (decodeInputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = decodeInputBuffers[decodeInputBufferIndex];
                    inputBuffer.clear();
                    if (inputBuffer.remaining() < dataSize) {
                        Log.e("decoderStart", "Decode input: " + dataSize + ", remain: " + inputBuffer.remaining());
                        return false;
                    }
                    inputBuffer.put(dataBuffer, 0, dataSize);
                    m_decoder.queueInputBuffer(decodeInputBufferIndex, 0, dataSize, mCount * TIME_INTERNAL, 0);
                    mCount++;
                }
                decodeOutBufferInfo = new MediaCodec.BufferInfo();
                int decodeOutputBufferIndex = m_decoder.dequeueOutputBuffer(decodeOutBufferInfo, 100);
                while (decodeOutputBufferIndex >= 0) {
                    m_decoder.releaseOutputBuffer(decodeOutputBufferIndex, true);
                    decodeOutputBufferIndex = m_decoder.dequeueOutputBuffer(decodeOutBufferInfo, 0);
                }
            } catch (Exception e) {
                Log.e("decoderStart", "Decode failed: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

}
