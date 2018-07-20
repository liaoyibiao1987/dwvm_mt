//
// Created by xy on 1/22/16.
//

#ifndef DWVM_MT_CMTLIBINSTANCE_H
#define DWVM_MT_CMTLIBINSTANCE_H

#include "DWVM/DwvmBase.h"
#include "Session.h"

class CJStringToChar
{
public:
    CJStringToChar(JNIEnv *env, jstring jsz)
    {
        m_env = env;
        m_jsz = jsz;
        if (m_env && m_jsz)
        {
            jboolean isCopy = JNI_FALSE;
            m_sz = env->GetStringUTFChars(jsz, &isCopy);
        }
        else
        {
            m_sz = NULL;
        }
    }

    ~CJStringToChar()
    {
        if (m_sz != NULL && m_env != NULL)
        {
            m_env->ReleaseStringUTFChars(m_jsz, m_sz);
            m_sz = NULL;
        }
    }

    operator const char *(void)
    {
        return m_sz;
    }

    JNIEnv *m_env;
    jstring m_jsz;
    const char *m_sz;
};

#define Char_to_JString(env, sz) env->NewStringUTF(sz)

class CJByteArrayToPointer
{
public:
    CJByteArrayToPointer(JNIEnv *env, jbyteArray jba)
    {
        m_env = env;
        m_jba = jba;
        if (m_env && m_jba)
        {
            jboolean isCopy = JNI_FALSE;
            m_ptr = env->GetByteArrayElements(jba, &isCopy);
        }
        else
        {
            m_ptr = NULL;
        }
    }

    ~CJByteArrayToPointer()
    {
        if (m_ptr != NULL && m_env != NULL)
        {
            m_env->ReleaseByteArrayElements(m_jba, m_ptr, 0);
        }
    }

    void *GetPointer()
    { return m_ptr; }

    int GetLength()
    { return (m_jba && m_env) ? m_env->GetArrayLength(m_jba) : (0); }

    JNIEnv *m_env;
    jbyteArray m_jba;
    jbyte *m_ptr;
};

#define New_JByteArray(env, len)                     env->NewByteArray(len)
#define Get_JByteArray_Len(env, jba)                 env->GetArrayLength(jba)
#define Copy_Buffer_To_JByteArray(env, jba, buf, len)  env->SetByteArrayRegion(jba, 0, len, (jbyte*) buf)


class CMTLibInstance
{
public:
    CMTLibInstance(JavaVM *javaEnv, jobject javaObj);

    ~CMTLibInstance();

    BOOL IsValid();

    BOOL Start(
        DWORD dwLocalDeviceId,
        WORD wUdpPort,
        int iUdpSocketBufferSize,
        int iNetEncryptMode,
        int iEncoderChannelNumber,
        int iDecoderChannelNumber,
        const char *szOptionText);

    void Stop();

    BOOL IsWorking();

    BOOL SetDeviceName(const char *szDeviceName);

    bool ResetDeviceID(int *m_dwLocalDeviceId);

    int SendUdpPacketToDevice(
        DWORD dwPacketType,
        DWORD dwNeedReplay,
        DWORD dwDestDeviceId,
        const char *szDestDeviceIpPort,
        BYTE *pDataBuffer,
        int iDataSize);

    int SendOneFrameToDevice(
        int iLocalEncoderChannelIndex,
        DWORD dwRemoteDeviceId,
        int iRemoteDeviceDecoderChannelIndex,
        const char *szRemoteDeviceIpPort,
        const char *szCodec, // CODEC_VIDEO_H264, CODEC_AUDIO_MP3
        BYTE *pFrameBuffer,
        int iFrameBytes,
        int iImageResolution, // IMAGE_RESOLUTION_CIF, IMAGE_RESOLUTION_D1, IMAGE_RESOLUTION_QCIF
        int iImageWidth,
        int iImageHeight);

protected:
    static int OnReceivedNetPacket(
        T_DWVM_JNI_ENV *pJava,
        SOCKET s,           // socket
        void *pCbContext,   // 回调上下文参数，对应于 DWVM_CreateNetThread() 函数的 pCbContext 参数
        DWORD dwFromIp,     // 网络包的来源IP
        WORD wFromPort,     // 网络包的来源PORT
        DWORD dwCmd,        // 网络包的命令类型
        void *pData,        // 网络包buffer
        int iLen)
    {
        return ((CMTLibInstance *) pCbContext)->OnReceivedNetPacket1(pJava, dwFromIp, wFromPort, dwCmd, pData, iLen);
    }

    int OnReceivedNetPacket1(T_DWVM_JNI_ENV *pJava, DWORD dwFromIp, WORD wFromPort, DWORD dwCmd, void *pData, int iLen);

    BOOL OnReceivedVideoAudioPacket(
        T_DWVM_JNI_ENV *pJava,
        T_WVM_VA_FRAME_HEADER *pFrameHdr,// IN  收到的音频视频小包: T_WVM_VA_FRAME_HEADER + T_WVM_VA_BLOCK_HEADER + frame_data
        DWORD dwRemoteDeviceIp,        // IN  发送方的ip
        WORD wRemotePort,                // IN  发送方的port
        DWORD dwRemoteDeviceId);        // IN  发送方的ID

    DWORD NotifyJavaCallbackMethod(
        T_DWVM_JNI_ENV *pJava,
        const char *szFuncName,
        const char *szRemoteDeviceIpPort,
        DWORD dwRemoteDeviceId,
        int iRemoteEncoderChannelIndex,
        DWORD dwLocalDeviceId,
        int iLocalDecoderChannelIndex,
        DWORD dwDataType,
        BYTE *pDataBuffer,
        int iDataBytes,
        const char *szCodecName,
        int iImageResolution,
        int iWidth,
        int iHeight);

    static THREAD_PROC(Thread_AutoDeleteSession, lp)
    {
        ((CMTLibInstance*)lp)->OnAutoDeleteSessionThread();
        return NULL;
    }
    void OnAutoDeleteSessionThread();

protected:
    // java env
    JavaVM *m_javaVM;
    jobject m_javaObj;

    // network socket
    SOCKET m_sock;
    DWORD m_dwLocalDeviceId;
    int m_iEncoderChannelNumber;
    int m_iDecoderChannelNumber;
    DWORD m_dwNetEncryptMode;

    // session
    CXSimpleList3 m_sessionList;
    THREAD_HANDLE m_hAutoDeleteThread;
    BOOL m_bToStopThread;

    // h264 header
    void* m_pCodecData[WVM_MAX_MT_ENCODER_NUMBER];
    int m_iCodecDataSize[WVM_MAX_MT_ENCODER_NUMBER];
};


#endif //DWVM_MT_CMTLIBINSTANCE_H
