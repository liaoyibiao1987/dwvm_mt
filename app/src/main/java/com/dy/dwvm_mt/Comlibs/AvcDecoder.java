package com.dy.dwvm_mt.Comlibs;

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
     .#19 COLOR_FormatYUV420Planar (I420)
     .#20 COLOR_FormatYUV420PackedPlanar (also I420)
     .#21 COLOR_FormatYUV420SemiPlanar (NV12)
     .#39 COLOR_FormatYUV420PackedSemiPlanar (also NV12)
     .#0x7f000100 COLOR_TI_FormatYUV420PackedSemiPlanar (also also NV12)
     I420的数据布局相当于YV12，但是Cr和Cb却是颠倒的，就像NV12和NV21一样。所以如果你想要去处理相机拿到的YV12数据，可能会看到一些奇怪的颜色干扰*/
}
