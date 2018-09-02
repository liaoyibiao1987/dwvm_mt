package com.dy.dwvm_mt.comlibs;

import com.dy.dwvm_mt.commandmanager.NWCommandEventArg;
import com.dy.dwvm_mt.messagestructs.s_MT_SetCodeStates;
import com.dy.dwvm_mt.messagestructs.s_messageBase;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.StringUtils;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/24.
 * PS: Not easy to write code, please indicate.
 */
public final class EncoderManager {
    private static I_MT_Prime primer;

    public static void Init(I_MT_Prime m_primer) {
        primer = m_primer;
    }

    public static void Manage(NWCommandEventArg arg) {
        try {
            s_MT_SetCodeStates states = arg.getEventArg().Param(s_MT_SetCodeStates.class);

        } catch (Exception es) {

        }
    }

    public static boolean OpenEncode(int streamType, int encodeChannel, int targetDeviceID, int decodeChannle, int imgType, String ip) {
        boolean retVal = true;
        if (StringUtils.isTrimEmpty(ip) || ip == "0.0.0.0:0") {
            LogUtils.w("EncodeManage收到IP地址不合法");
            retVal = false;
        }

        switch (streamType)
        {
            case s_messageBase.StreamType.VIDEO:
               /* retVal = primer.sendOneFrameToDevice(encodeChannel, targetDeviceID, decodeChannle, ip, imgType, 0);
                if (retVal == false)
                {
                    LogUtils.e("打开编码器错误 Quantity.StreamType.VIDEO");
                }*/
                break;
            case s_messageBase.StreamType.AUDIO:
               /* retVal = ComponentService.NetWorkService.EncoderStartSendAudio(encodeChannel, targetDeviceID, decodeChannle, ip);
                if (retVal == false)
                {
                    LogUtils.e("打开音频编码器错误 Quantity.StreamType.AUDIO");
                }*/
                break;
            case s_messageBase.StreamType.BOTH:
                //Trace.WriteLine("***======******* 编码通道: " + encodeChannel);
                /*retVal = ComponentService.NetWorkService.EncoderStartSendVideo(encodeChannel, targetDeviceID, decodeChannle, ip, imgType, 0);
                if (retVal == false)
                {
                    int ECode = 0;
                    LogUtils.e("打开编码器错误 Video: OpenEncode();ECode=" + ECode);
                }*/
                /*retVal = ComponentService.NetWorkService.EncoderStartSendAudio(encodeChannel, targetDeviceID, decodeChannle, ip);
                if (retVal == false)
                {
                    int ECode = 0;
                    LogUtils.e("打开编码器错误 Audio: OpenEncode();ECode=" + ECode);
                }*/
                break;
        }
        return retVal;
    }
}
