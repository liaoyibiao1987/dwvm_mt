package com.dy.dwvm_mt.Comlibs;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.dy.dwvm_mt.utilcode.util.FileIOUtils;
import com.dy.dwvm_mt.utilcode.util.FileUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.ThreadUtils;
import com.dy.dwvm_mt.utilcode.util.TimeUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

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


public class AvcDecoder extends Thread {
    /**
     * 相机支持YV12（平面 YUV 4:2:0） 以及 NV21 （半平面 YUV 4:2:0），MediaCodec支持以下一个或多个：
     * .#19 COLOR_FormatYUV420Planar (I420)
     * .#20 COLOR_FormatYUV420PackedPlanar (also I420)
     * .#21 COLOR_FormatYUV420SemiPlanar (NV12)
     * .#39 COLOR_FormatYUV420PackedSemiPlanar (also NV12)
     * .#0x7f000100 COLOR_TI_FormatYUV420PackedSemiPlanar (also also NV12)
     * I420的数据布局相当于YV12，但是Cr和Cb却是颠倒的，就像NV12和NV21一样。所以如果你想要去处理相机拿到的YV12数据，可能会看到一些奇怪的颜色干扰
     * <p>
     * 现今为止，知道华为hais970对h264 multi-slice 多线程编码支持度不好。
     */

    private MediaCodec m_decoder = null;
    private SurfaceView m_sufcaeViewer;
    private int m_width;
    private int m_height;
    private int m_rotation = 90;
    private final static int TIME_INTERNAL = 5;


    private boolean m_decoderCreateFailed = false;
    private boolean m_decoderValid = false;
    private String m_decoderCodecName = "";
    private boolean m_decodeWaitKeyFrame = true;
    private boolean isRunning = true;
    private boolean isPause = false;

    private static final int M_YUVQUEUESIZE = 50;
    private ArrayBlockingQueue<Frame> mFrmList = new ArrayBlockingQueue<>(M_YUVQUEUESIZE);

    public AvcDecoder(SurfaceView surfaceView) {
        m_sufcaeViewer = surfaceView;
        m_sufcaeViewer.setKeepScreenOn(true);
    }

    private synchronized boolean decoderStart(String codecName, int width, int height) {
        if (m_decoder != null) {
            return false;
        }

        if (m_sufcaeViewer == null) {
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
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width * height);
            mediaFormat.setInteger(MediaFormat.KEY_MAX_HEIGHT, height);
            mediaFormat.setInteger(MediaFormat.KEY_MAX_WIDTH, width);
            /*mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(mSps));
            mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(mPps));*/

            //*************************全面屏手机两次加载sufcaeViewer和强制拉伸，不能在onCreate去传入Holder和Surface************************/
            Log.w("DYMTTTTTTT", " m_sufcaeViewer.getHolder().getSurface()");
            m_decoder.configure(mediaFormat, m_sufcaeViewer.getHolder().getSurface(), null, 0);
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
        LogUtils.d("Decoder stop...");
        isRunning = false;
        seekFrames = 0;
        decoderRelease();
        SystemClock.sleep(20);
        mFrmList = null;
    }

    private void decoderRelease() {
        isPause = true;
        m_decoderCreateFailed = true;
        m_decoderValid = false;
        if (m_decoder != null) {
            try {
                m_decoder.stop();
                m_decoder.release();
                this.stop();
            } catch (Exception es) {
                LogUtils.e("decoderRelease error", es);
            } finally {
                m_decoder = null;
            }
        }
        isPause = false;
        m_decoderCreateFailed = false;
    }

    File file = new File(Environment.getExternalStorageDirectory() + "/DY", TimeUtils.getNowString(new SimpleDateFormat("yyyy_MM_dd_HH")) + ".h264");
    int seekFrames = 0;

    public boolean decoderOneVideoFrame(String codecName, int width, int height, byte[] dataBuffer, int dataSize, long frameType) {
        //LogUtils.d(String.format("codecName %s, width %d, height %d, frameType %d", codecName, width, height, frameType));
        // if no decoder, create it
        FileIOUtils.writeFileFromBytesByStream(file, dataBuffer, true);
        Log.d("writeFileFromBytes", TimeUtils.getNowString());
        return true;
        /*if (isRunning == true && isPause == false) {
            Log.d("MMMMMMMMMMMMM START : " + mFrmList.size(), TimeUtils.getNowString());
            if (m_decoder == null) {
                // only try one to create decoder
                if (m_decoderCreateFailed) {
                    return false;
                }
                LogUtils.d("Decoder renew...");
                if (decoderStart(codecName, width, height) == false) {
                    //m_decoder.flush();//刷新（Flushed） 第一帧只接受关键帧，flush自动生成关键帧 慎用，影响编码
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
            if (m_decoderCodecName.equalsIgnoreCase(codecName) == false || m_width != width || m_height != height) {
                decoderRelease();
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
            if (mFrmList.size() >= M_YUVQUEUESIZE) {
                mFrmList.poll();
            }
            seekFrames++;
            Log.d("DDDDDD seekFrames : " + seekFrames, TimeUtils.getNowString());
            Frame frame = new Frame(dataBuffer, dataSize, frameType);
            if (seekFrames > 30) {
                Log.d("YYYYYY offer : " + seekFrames, TimeUtils.getNowString());
                mFrmList.offer(frame);
            }
            Log.d("TTTTTTTTTTTTT END : " + mFrmList.size(), TimeUtils.getNowString());
        }
        return true;*/
    }
        /*synchronized (decoderLocker) {
            // decode frame
            try {
                ByteBuffer[] decodeInputBuffers = m_decoder.getInputBuffers();
                int decodeInputBufferIndex = m_decoder.dequeueInputBuffer(-1);
                if (decodeInputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = decodeInputBuffers[decodeInputBufferIndex];
                    inputBuffer.clear();
                    if (inputBuffer.remaining() < dataSize) {
                        Log.e("decoderStart", "Decode input: " + dataSize + ", remain: " + inputBuffer.remaining());
                        return false;
                    }
                    inputBuffer.put(dataBuffer, 0, dataSize);
                    m_decoder.queueInputBuffer(decodeInputBufferIndex, 0, dataSize, System.currentTimeMillis() * 1000, 0);
                }

                MediaCodec.BufferInfo decodeOutBufferInfo = new MediaCodec.BufferInfo();
                int decodeOutputBufferIndex = m_decoder.dequeueOutputBuffer(decodeOutBufferInfo, 0);
                while (decodeOutputBufferIndex >= 0) {
                    m_decoder.releaseOutputBuffer(decodeOutputBufferIndex, true);
                    decodeOutputBufferIndex = m_decoder.dequeueOutputBuffer(decodeOutBufferInfo, 0);
                }
                Thread.sleep(10);
            } catch (Exception e) {
                Log.e("decoderStart", "Decode failed: " + e.getMessage());
                return false;
            }

        }*/

    @Override
    public void run() {
        Frame frame = null;
        //存放目标文件的数据
        ByteBuffer byteBuffer = null;
        //解码后的数据，包含每一个buffer的元数据信息，例如偏差，在相关解码器中有效的数据大小
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while (isRunning) {
            if (isPause == false) {
                if (mFrmList.isEmpty()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                frame = mFrmList.poll();
                //Log.d("mmmmmmmmmmmmmmttt", Arrays.toString(frame.getmData()));
                try {
                    //1 准备填充器
                    int inIndex = m_decoder.dequeueInputBuffer(0);
                    //Log.w("DYMMMMMMMMMMMMMMMM", inIndex + " Input");
                    if (inIndex >= 0) {
                        //2 准备填充数据
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            byteBuffer = m_decoder.getInputBuffers()[inIndex];
                            byteBuffer.clear();
                        } else {
                            byteBuffer = m_decoder.getInputBuffer(inIndex);
                        }
                        if (byteBuffer == null) {
                            continue;
                        }
                        byteBuffer.put(frame.getmData(), 0, frame.getMdataSize());
                        /*int value = frame.getmData()[4] & 0x0f;
                        if (value == 7 || value == 8) {
                            m_decoder.queueInputBuffer(inIndex, 0, frame.getMdataSize(), 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                        } else if (value == 5) {
                            m_decoder.queueInputBuffer(inIndex, 0, frame.getMdataSize(), 0, MediaCodec.BUFFER_FLAG_KEY_FRAME);
                        } else {
                            m_decoder.queueInputBuffer(inIndex, 0, frame.getMdataSize(), 0, 0);
                        }*/
                        //3 把数据传给解码器
                        m_decoder.queueInputBuffer(inIndex, 0, frame.getMdataSize(), 0, 0);
                    } else {
                        continue;
                    }
                } catch (Exception es) {
                    isRunning = false;
                    isPause = true;
                    LogUtils.e("AvcDecoder dequeueInputBuffer error", es);
                }
                //这里可以根据实际情况调整解码速度
                /*long sleep = mFrmList.size() > 20 ? 10 : 20;
                SystemClock.sleep(sleep);*/
                //4 开始解码
                try {
                    int decodeOutputBufferIndex = m_decoder.dequeueOutputBuffer(info, 0);
                    //Log.w("DYTTTTTTTTTTTTTT", outIndex + " Output");
                    while (decodeOutputBufferIndex >= 0) {
                        //帧控制
                   /* while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }*/
                        //boolean doRender = (info.size != 0);
                        //对outputbuffer的处理完后，调用这个函数把buffer重新返回给codec类。
                        //调用这个api之后，SurfaceView才有图像
                        m_decoder.releaseOutputBuffer(decodeOutputBufferIndex, true);
                        decodeOutputBufferIndex = m_decoder.dequeueOutputBuffer(info, 0);
                    }
                    System.gc();
                } catch (Exception es) {
                    isRunning = false;
                    isPause = true;
                    LogUtils.e("AvcDecoder dequeueOutputBuffer error", es);
                    break;
                }
            }
        }
        mFrmList.clear();
        mFrmList = null;
        Log.i("MT android ", "===============stop DecodeThread==============");
    }
}
