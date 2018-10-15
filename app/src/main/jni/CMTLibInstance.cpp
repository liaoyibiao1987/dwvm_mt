//
// Created by xy on 1/22/16.
//

#include "CMTLibInstance.h"
#include "DWVM/dwvm_global.h"
#include "DWVM/DwvmBase.h"
#include "Session.h"
#include "DWVM/Net.h"


class m_sock;
CNet *FindNetFromSocket(SOCKET s);

CMTLibInstance::CMTLibInstance(JavaVM *javaVM, jobject javaObj) :
    m_sessionList()
{
    m_javaVM = javaVM;
    m_javaObj = javaObj;

    m_sock = INVALID_SOCKET;
    m_dwLocalDeviceId = 0;
    m_iEncoderChannelNumber = 0;
    m_iDecoderChannelNumber = 0;
    m_dwNetEncryptMode = 0;
    memset(&m_resTestSpeed,0,sizeof(m_resTestSpeed));

    m_hAutoDeleteThread = INVALID_THREAD_HANDLE;
    m_bToStopThread = FALSE;

    for(int i=0; i < WVM_MAX_MT_ENCODER_NUMBER; i++)
    {
        m_pCodecData[i] = NULL;
        m_iCodecDataSize[i] = 0;
    }
}

CMTLibInstance::~CMTLibInstance()
{
    Stop();

    for(int i=0; i < WVM_MAX_MT_ENCODER_NUMBER; i++)
    {
        if(m_pCodecData[i])
        {
            free(m_pCodecData[i]);
            m_pCodecData[i] = NULL;
        }
        m_iCodecDataSize[i] = 0;
    }
}

BOOL CMTLibInstance::Start(
    DWORD dwLocalDeviceId,
    WORD wUdpPort,
    int iUdpSocketBufferSize,
    int iNetEncryptMode,
    int iEncoderChannelNumber,
    int iDecoderChannelNumber,
    const char *szOptionText)
{
    //
    // check input parameters
    //
    if (!IsValid())
    {
        xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::Start() failed: is not VALID.\n");
        return FALSE;
    }
    if (IsWorking())
    {
        xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::Start() failed: is working.\n");
        return FALSE;
    }
    if (0 == dwLocalDeviceId || GET_DEVICE_TYPE(dwLocalDeviceId) != WVM_DEVICE_TYPE_MT)
    {
        xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::Start() failed: invalidate localDeviceId: 0x%08X\n", dwLocalDeviceId);
        return FALSE;
    }
    if (0 == wUdpPort)
    {
        xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::Start() failed: invalidate udpPort: %u\n", wUdpPort);
        return FALSE;
    }
    if (iNetEncryptMode < 0)
    {
        xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::Start() failed: invalidate netEncryptMode: %d\n", iNetEncryptMode);
        return FALSE;
    }
    if ((iEncoderChannelNumber <= 0 && iDecoderChannelNumber <= 0) ||
        iEncoderChannelNumber > WVM_MAX_MT_ENCODER_NUMBER ||
        iDecoderChannelNumber > WVM_MAX_MT_DECODER_NUMBER)
    {
        xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::Start() failed: invalidate channel-number: enc=%d, dec=%d\n",
             iEncoderChannelNumber, iDecoderChannelNumber);
        return FALSE;
    }

    //
    // alloc session list
    //
    if (!m_sessionList.Create(sizeof(T_SESSION), MAX_SESSION_NUMBER, 0, sizeof(T_SESSION_ID)))
    {
        xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::Start() failed: can not create session-list\n");
        Stop();
        return FALSE;
    }

    //
    // create network resource
    //
    m_sock = DWVM_CreateNetThread(dwLocalDeviceId, wUdpPort, 0, iUdpSocketBufferSize, OnReceivedNetPacket, this, 1, 0);
    if (INVALID_SOCKET == m_sock)
    {
        xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::Start() failed: can not create net-resource\n");
        Stop();
        return FALSE;
    }

    m_dwLocalDeviceId = dwLocalDeviceId;
    m_iEncoderChannelNumber = (iEncoderChannelNumber < 0) ? (0) : (iEncoderChannelNumber);
    m_iDecoderChannelNumber = (iDecoderChannelNumber < 0) ? (0) : (iDecoderChannelNumber);
    m_dwNetEncryptMode = (DWORD) iNetEncryptMode;

    //
    // create thread
    //
    m_bToStopThread = FALSE;
    THREAD_CREATE(m_hAutoDeleteThread, Thread_AutoDeleteSession, this);
    THREAD_SET_NAME(m_hAutoDeleteThread, "DWVM_LibInstance_AutoDeleteSession");

    return TRUE;
}

BOOL CMTLibInstance::IsValid()
{
    return (m_javaVM != NULL && m_javaObj != NULL);
}

void CMTLibInstance::Stop()
{
    //
    // destroy network resource
    //
    if (INVALID_SOCKET != m_sock)
    {
        DWVM_DestroyNetThread(m_sock);
        m_sock = INVALID_SOCKET;
    }

    //
    // free thread
    //
    m_bToStopThread = TRUE;
    THREAD_CLOSE(m_hAutoDeleteThread);

    //
    // destroy session list
    //
    if (m_sessionList.IsValid())
    {
        const int iCount = m_sessionList.GetItemCount();
        for (int i = iCount - 1; i >= 0; i--)
        {
            T_SESSION *pSession = (T_SESSION *) m_sessionList.GetItem(i);
            m_sessionList.Remove(i, false);
            if (pSession && pSession->pSessionObj)
            {
                try
                {
                    delete pSession->pSessionObj;
                }
                catch (...)
                {
                }
                pSession->pSessionObj = NULL;
            }
        }
        m_sessionList.Destroy();
    }
}

BOOL CMTLibInstance::IsWorking()
{
    return (m_sock != INVALID_SOCKET && m_dwLocalDeviceId != 0);
}

BOOL CMTLibInstance::SetDeviceName(const char *szDeviceName)
{
    if (NULL == szDeviceName)
    {
        return FALSE;
    }

    return TRUE;
}

BOOL CMTLibInstance::SetDeviceId(DWORD dwLocalDeviceId)
{
    if (0 == dwLocalDeviceId || GET_DEVICE_TYPE(dwLocalDeviceId) != WVM_DEVICE_TYPE_MT)
    {
        xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::SetDeviceId() failed: invalidate localDeviceId: 0x%08X\n", dwLocalDeviceId);
        return FALSE;
    }

    DWVM_SetLocalDeviceId(m_sock, dwLocalDeviceId);
    m_dwLocalDeviceId = dwLocalDeviceId;

    return TRUE;
}

int CMTLibInstance::SendUdpPacketToDevice(
    DWORD dwPacketType,
    DWORD dwNeedReplay,
    DWORD dwDestDeviceId,
    const char *szDestDeviceIpPort,
    BYTE *pDataBuffer,
    int iDataSize)
{
    //
    // check working status & input parameters
    //
    if (!IsWorking())
    {
        return 0;
    }
    if (NULL == szDestDeviceIpPort)
    {
        return 0;
    }

    //
    // get ip & port from string
    //
    DWORD dwIp = 0;
    WORD wPort = 0;
    XGetIpPortFromString(szDestDeviceIpPort, &dwIp, &wPort);
    if (0 == dwIp || 0 == wPort)
    {
        return 0;
    }

    //
    // send
    //
    return (int) DWVM_SendNetPacket(FALSE, m_sock, dwPacketType, dwNeedReplay, dwDestDeviceId, dwIp, wPort, pDataBuffer,
                                    iDataSize, m_dwNetEncryptMode);
}

int CMTLibInstance::SendOneFrameToDevice(
    int iLocalEncoderChannelIndex,
    DWORD dwRemoteDeviceId,
    int iRemoteDeviceDecoderChannelIndex,
    const char *szRemoteDeviceIpPort,
    const char *szCodec, // CODEC_VIDEO_H264, CODEC_AUDIO_MP3
    BYTE *pFrameBuffer,
    int iFrameBytes,
    int iImageResolution, // IMAGE_RESOLUTION_CIF, IMAGE_RESOLUTION_D1, IMAGE_RESOLUTION_QCIF
    int iImageWidth,
    int iImageHeight)
{
    //
    // check working status & input parameters
    //
    if (!IsWorking())
    {
        xlog(XLOG_LEVEL_ERROR, "%s(): is NOT working.", __FUNCTION__);
        return 0;
    }
    if (NULL == szRemoteDeviceIpPort || NULL == pFrameBuffer || iFrameBytes <= 0)
    {
        xlog(XLOG_LEVEL_ERROR, "%s(): param NULL.", __FUNCTION__);
        return 0;
    }
    if (iLocalEncoderChannelIndex < 0 || iLocalEncoderChannelIndex >= m_iEncoderChannelNumber)
    {
        xlog(XLOG_LEVEL_ERROR, "%s(): encoderChannelIndex error: %d", __FUNCTION__, iLocalEncoderChannelIndex);
        return 0;
    }
    if (dwRemoteDeviceId == 0 || iRemoteDeviceDecoderChannelIndex < 0)
    {
        xlog(XLOG_LEVEL_ERROR, "%s(): dest device param error: id=%08X, decoderChannel=%d", __FUNCTION__, dwRemoteDeviceId,
             iRemoteDeviceDecoderChannelIndex);
        return 0;
    }
    if (NULL == szCodec || strlen(szCodec) <= 6)
    {
        xlog(XLOG_LEVEL_ERROR, "%s(): codecName error.", __FUNCTION__);
        return 0;
    }
    if (iImageResolution < 0 || iImageResolution >= WVM_MAX_IMAGERES_NUM)
    {
        xlog(XLOG_LEVEL_ERROR, "%s(): imageRes error: %d", __FUNCTION__, iImageResolution);
        return 0;
    }
    if (iImageWidth < 0 || iImageHeight < 0)
    {
        xlog(XLOG_LEVEL_ERROR, "%s(): image size error: %d x %d", __FUNCTION__, iImageWidth, iImageHeight);
        return 0;
    }

    //
    // get ip & port from string
    //
    DWORD dwIp = 0;
    WORD wPort = 0;
    XGetIpPortFromString(szRemoteDeviceIpPort, &dwIp, &wPort);
    if (0 == dwIp || 0 == wPort)
    {
        xlog(XLOG_LEVEL_ERROR, "%s(): invalid ip:port (%s)", __FUNCTION__, szRemoteDeviceIpPort);
        return 0;
    }

    //
    // make session id
    //
    T_SESSION_ID sessionId;
    memset(&sessionId, 0, sizeof(sessionId));
    sessionId.eDir = SESSION_SENDER;
    sessionId.iLocalChannelIndex = iLocalEncoderChannelIndex;
    sessionId.iLocalImageResolution = iImageResolution;
    sessionId.dwRemoteDeviceId = dwRemoteDeviceId;
    sessionId.iRemoteChannelIndex = iRemoteDeviceDecoderChannelIndex;
    // media type of this frame
    if (0 == memcmp(szCodec, "video/", 6))
    {
        sessionId.eMedia = SESSION_VIDEO;
    }
    else if (0 == memcmp(szCodec, "audio/", 6))
    {
        sessionId.eMedia = SESSION_AUDIO;
    }
    else
    {
        xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::%s(): invalid codecName: (%s)", __FUNCTION__, szCodec);
        return 0;
    }

    //
    // if session NOT existed, new the session, and add to list
    //
    T_SESSION *pSession = (T_SESSION *) m_sessionList.GetItem(&sessionId);
    if (pSession == NULL)
    {
        T_SESSION s = {sessionId, NULL};
        if (m_sessionList.Add(&s) < 0)
        {
            xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::%s(): session list overflow: num=%d", __FUNCTION__,
                 m_sessionList.GetItemCount());
            return 0;
        }
        pSession = (T_SESSION *) m_sessionList.GetItem(&sessionId);
        if (pSession == NULL)
        {
            xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::%s(): Can not find session, list num=%d", __FUNCTION__,
                 m_sessionList.GetItemCount());
            return 0;
        }
    }
    if (pSession->pSessionObj == NULL)
    {
        try
        {
            if (0 == strcmp(szCodec, CODEC_VIDEO_H264))
            {
                pSession->pSessionObj = new CSessionSenderH264(sessionId, szCodec, m_sock, m_dwNetEncryptMode, m_dwLocalDeviceId);
            }
            else if (0 == strcmp(szCodec, CODEC_AUDIO_MP3))
            {
                pSession->pSessionObj = new CSessionSender(sessionId, szCodec, m_sock, m_dwNetEncryptMode, m_dwLocalDeviceId);
            }
            else
            {
                xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::%s(): not-supported codecName: (%s)", __FUNCTION__, szCodec);
                return 0;
            }
        }
        catch (...)
        {
            pSession->pSessionObj = NULL;
            xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::%s(): new sender codec(%s) error.", __FUNCTION__, szCodec);
            return 0;
        }

        const int iChannel = iLocalEncoderChannelIndex;
        if(m_pCodecData[iChannel] && m_iCodecDataSize[iChannel] > 0)
        {
            pSession->pSessionObj->SetCodecInitData(m_pCodecData[iChannel], m_iCodecDataSize[iChannel]);
        }
    }

    //
    // send
    //
    return pSession->pSessionObj->SendFrame(dwIp, wPort, pFrameBuffer, iFrameBytes, iImageWidth, iImageHeight);
}

//
// find callback function 'callbackFromJNI' on java class 'com.dy.dwvm_mt.MTLib'
//
static bool FindCallbackMethodOnJava(T_DWVM_JNI_ENV *pJava)
{
    if (NULL == pJava || NULL == pJava->javaEnv || NULL == pJava->javaObj)
    {
        return false;
    }

    // find java class 'com.dy.dwvm_mt.MTLib'
    pJava->javaMTLib = pJava->javaEnv->GetObjectClass(pJava->javaObj);
    if (pJava->javaEnv->ExceptionCheck())
    {
        pJava->javaEnv->ExceptionClear();
        xlog(XLOG_LEVEL_ERROR, "FindCallbackMethodOnJava()::FindClass() error.\n");
        return false;
    }
    if (NULL == pJava->javaMTLib)
    {
        xlog(XLOG_LEVEL_ERROR, "FindCallbackMethodOnJava()::FindClass() failed.\n");
        return false;
    }

    // find callback method 'callbackFromJNI'
    pJava->javaMTLibCallbackMethod = pJava->javaEnv->GetMethodID(
        pJava->javaMTLib,
        "callbackFromJNI",
        "(Ljava/lang/String;Ljava/lang/String;JIJIJ[BILjava/lang/String;III)J");
    if (pJava->javaEnv->ExceptionCheck())
    {
        pJava->javaEnv->ExceptionClear();
        pJava->javaEnv->DeleteLocalRef(pJava->javaMTLib);
        xlog(XLOG_LEVEL_ERROR, "FindCallbackMethodOnJava()::GetMethodID() error.\n");
        return false;
    }
    if (NULL == pJava->javaMTLibCallbackMethod)
    {
        xlog(XLOG_LEVEL_ERROR, "FindCallbackMethodOnJava()::GetMethodID() failed.\n");
        pJava->javaEnv->DeleteLocalRef(pJava->javaMTLib);
        return false;
    }

    return true;
}

int CMTLibInstance::OnReceivedNetPacket1(T_DWVM_JNI_ENV *pJava, DWORD dwFromIp, WORD wFromPort, DWORD dwCmd,
                                         void *pData, int iLen)
{
    //
    // initialize / un-initialize java-env
    //
    if (NULL == pJava)
    {
        return 0;
    }
    if (1 == pJava->todo)
    {
        // attach current thread to java VM, and find callback-method on java class
        if (m_javaVM && m_javaObj)
        {
            pJava->javaVM = m_javaVM;
            pJava->javaObj = m_javaObj;
            pJava->javaEnv = NULL;
            pJava->javaVM->AttachCurrentThread(&pJava->javaEnv, NULL);
            if (pJava->javaEnv)
            {
                if (!FindCallbackMethodOnJava(pJava))
                {
                    pJava->javaVM->DetachCurrentThread();
                    pJava->javaVM = NULL;
                    pJava->javaObj = NULL;
                    pJava->javaEnv = NULL;
                }
            }
        }
        return (pJava->javaMTLibCallbackMethod != NULL);
    }
    else if (2 == pJava->todo)
    {
        // detach current thread from java VM
        if (pJava->javaVM && pJava->javaEnv)
        {
            pJava->javaVM->DetachCurrentThread();
        }
        return 1;
    }

    //
    // 先自己处理，不能处理的再交给APP
    //
    T_WVM_PACKET_HEADER *hdr = (T_WVM_PACKET_HEADER *) pData;
    if (WVM_CMD_PS_VA_RESEND == dwCmd)
    {
        // 对方请求重发视频、音频包
        T_WVM_VA_RESEND *r = (T_WVM_VA_RESEND *) (((char *) pData) + hdr->dwSize);
        T_SESSION_ID sessionId;
        memset(&sessionId, 0, sizeof(sessionId));
        sessionId.eMedia = (WVM_FRAMETYPE_AUDIO == r->Session.dwFrameType) ? SESSION_AUDIO : SESSION_VIDEO;
        sessionId.eDir = SESSION_SENDER;
        sessionId.iLocalChannelIndex = (int) r->Session.dwSrcDeviceEncoderChannelIndex;
        sessionId.iLocalImageResolution = r->Session.dwImageResolution;
        sessionId.dwRemoteDeviceId = hdr->dwSrcId;
        sessionId.iRemoteChannelIndex = r->Session.dwDestDeviceDecoderChannelIndex;
        T_SESSION *pSession = (T_SESSION *) m_sessionList.GetItem(&sessionId);
        if (pSession && pSession->pSessionObj)
        {
            CFrameSender *pSender = pSession->pSessionObj->GetSender();
            if (pSender)
            {
                pSender->Resend(m_sock, m_dwNetEncryptMode, hdr->dwSrcId, dwFromIp, wFromPort, r);
            }
        }
        return 0;
    }
    else if (WVM_CMD_PS_VA_POLLING == dwCmd)
    {
        // 对方请求统计信息
        T_WVM_VA_POLLING *r = (T_WVM_VA_POLLING *) (((char *) pData) + hdr->dwSize);
        T_SESSION_ID sessionId;
        memset(&sessionId, 0, sizeof(sessionId));
        sessionId.eMedia = (WVM_FRAMETYPE_AUDIO == r->Session.dwFrameType) ? SESSION_AUDIO : SESSION_VIDEO;
        sessionId.eDir = SESSION_SENDER;
        sessionId.iLocalChannelIndex = (int) r->Session.dwSrcDeviceEncoderChannelIndex;
        sessionId.iLocalImageResolution = r->Session.dwImageResolution;
        sessionId.dwRemoteDeviceId = hdr->dwSrcId;
        sessionId.iRemoteChannelIndex = r->Session.dwDestDeviceDecoderChannelIndex;
        T_SESSION *pSession = (T_SESSION *) m_sessionList.GetItem(&sessionId);
        if (pSession && pSession->pSessionObj)
        {
            CFrameSender *pSender = pSession->pSessionObj->GetSender();
            if (pSender)
            {
                pSender->Reply(m_sock, m_dwNetEncryptMode, (DWORD) hdr->dwSrcId, dwFromIp, wFromPort, (DWORD) hdr->dwDestId, r);
            }
        }
        return 0;
    }
    else if (WVM_CMD_PS_VA_FRAME == dwCmd)
    {
        // 视频、音频包，组合成完整帧
        T_WVM_VA_FRAME_HEADER *pFrameHeader = (T_WVM_VA_FRAME_HEADER *) (((char *) pData) + hdr->dwSize);
        OnReceivedVideoAudioPacket(pJava, pFrameHeader, dwFromIp, wFromPort, hdr->dwSrcId);
        return 0;
    }
    else if (WVM_CMD_PS_VA_REPLY == dwCmd)
    {
        // 接收端回馈来的统计信息
        T_WVM_VA_REPLY *pReply = (T_WVM_VA_REPLY *) (((char *) pData) + hdr->dwSize);
        T_SESSION_ID sessionId;
        memset(&sessionId, 0, sizeof(sessionId));
        sessionId.eMedia = (WVM_FRAMETYPE_AUDIO == pReply->Polling.Session.dwFrameType) ? SESSION_AUDIO : SESSION_VIDEO;
        sessionId.eDir = SESSION_RECEIVER;
        sessionId.iLocalChannelIndex = (int) pReply->Polling.Session.dwSrcDeviceEncoderChannelIndex;
        sessionId.iLocalImageResolution = pReply->Polling.Session.dwImageResolution;
        sessionId.dwRemoteDeviceId = hdr->dwSrcId;
        sessionId.iRemoteChannelIndex = pReply->Polling.Session.dwDestDeviceDecoderChannelIndex;
        T_SESSION *pSession = (T_SESSION *) m_sessionList.GetItem(&sessionId);
        if (pSession && pSession->pSessionObj)
        {
            CFrameReceiver *pReceiver = pSession->pSessionObj->GetReceiver();
            if (pReceiver)
            {
                pReceiver->OnReply(pReply);
            }
        }
    }
    else if (WVM_CMD_REMOTE_FILE_OPEN == dwCmd)
    {
        return 0;
    }
    else if (WVM_CMD_REMOTE_FILE_COMMAND == dwCmd)
    {
        return 0;
    }
    else if (WVM_CMD_REMOTE_FILE_CLOSE == dwCmd)
    {
        return 0;
    }
    else if (WVM_CMD_REMOTE_FILE_INFO == dwCmd)
    {
        return 0;
    }
    else if (WVM_CMD_REMOTE_FILE_FRAME == dwCmd)
    {
        return 0;
    }
    else if (WVM_CMD_BIG_CUSTOM_DATA == dwCmd)
    {
        return 0;
    }

    //
    // 交给APP处理
    //
    char szIp[64] = {""};
    XMakeIpPortString(dwFromIp, wFromPort, szIp);
    NotifyJavaCallbackMethod(pJava, "onReceivedUdpPacket", szIp, hdr->dwSrcId, 0, m_dwLocalDeviceId, 0, hdr->dwCmd,
                             (BYTE *) pData, iLen, NULL, 0, 0, 0);

    return 0;
}

DWORD CMTLibInstance::NotifyJavaCallbackMethod(
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
    int iHeight)
{
    if (pJava == NULL || pJava->javaEnv == NULL || pJava->javaObj == NULL || pJava->javaMTLibCallbackMethod == NULL)
    {
        return 0;
    }
    if (NULL == szFuncName || NULL == szRemoteDeviceIpPort)
    {
        return 0;
    }
    if (NULL == pDataBuffer || iDataBytes <= 0)
    {
        return 0;
    }

    jbyteArray jbaData = New_JByteArray(pJava->javaEnv, iDataBytes);
    if (NULL == jbaData)
    {
        return 0;
    }
    Copy_Buffer_To_JByteArray(pJava->javaEnv, jbaData, pDataBuffer, iDataBytes);

    jstring jszFuncName = Char_to_JString(pJava->javaEnv, szFuncName);

    jstring jszRemoteIpPort = Char_to_JString(pJava->javaEnv, szRemoteDeviceIpPort);

    jstring jszCodecName = Char_to_JString(pJava->javaEnv, (NULL == szCodecName) ? "" : szCodecName);

    jlong jlResult = pJava->javaEnv->CallLongMethod(
        pJava->javaObj,
        pJava->javaMTLibCallbackMethod,
        jszFuncName,
        jszRemoteIpPort,
        (jlong) dwRemoteDeviceId,
        (jint) iRemoteEncoderChannelIndex,
        (jlong) dwLocalDeviceId,
        (jint) iLocalDecoderChannelIndex,
        (jlong) dwDataType,
        jbaData,
        (jint) iDataBytes,
        jszCodecName,
        (jint) iImageResolution,
        (jint) iWidth,
        (jint) iHeight);

    pJava->javaEnv->DeleteLocalRef(jbaData);
    pJava->javaEnv->DeleteLocalRef(jszFuncName);
    pJava->javaEnv->DeleteLocalRef(jszRemoteIpPort);
    pJava->javaEnv->DeleteLocalRef(jszCodecName);

    return (DWORD) (jlResult & 0x00000000FFFFFFFF);
}

BOOL CMTLibInstance::OnReceivedVideoAudioPacket(
    T_DWVM_JNI_ENV *pJava,
    T_WVM_VA_FRAME_HEADER *pFrameHdr,// IN  收到的音频视频小包: T_WVM_VA_FRAME_HEADER + T_WVM_VA_BLOCK_HEADER + frame_data
    DWORD dwRemoteDeviceIp,        // IN  发送方的ip
    WORD wRemotePort,                // IN  发送方的port
    DWORD dwRemoteDeviceId)        // IN  发送方的ID
{
    //
    // make session id
    //
    T_SESSION_ID sessionId;
    memset(&sessionId, 0, sizeof(sessionId));
    if (WVM_FRAMETYPE_VIDEO_P == pFrameHdr->Session.dwFrameType || WVM_FRAMETYPE_VIDEO_I == pFrameHdr->Session.dwFrameType)
    {
        sessionId.eMedia = SESSION_VIDEO;
    }
    else if (WVM_FRAMETYPE_AUDIO == pFrameHdr->Session.dwFrameType)
    {
        sessionId.eMedia = SESSION_AUDIO;
    }
    else
    {
        return FALSE;
    }
    sessionId.eDir = SESSION_RECEIVER;
    sessionId.dwRemoteDeviceId = dwRemoteDeviceId;
    sessionId.iRemoteChannelIndex = (int) pFrameHdr->Session.dwSrcDeviceEncoderChannelIndex;
    sessionId.iLocalChannelIndex = (int) pFrameHdr->Session.dwDestDeviceDecoderChannelIndex;
    sessionId.iLocalImageResolution = (int) pFrameHdr->Session.dwImageResolution;

    //
    // if session NOT existed, new the session, and add to list
    //
    T_SESSION *pSession = (T_SESSION *) m_sessionList.GetItem(&sessionId);
    if (pSession == NULL)
    {
        T_SESSION s = {sessionId, NULL};
        if (m_sessionList.Add(&s) < 0)
        {
            xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::%s(): session list overflow: num=%d", __FUNCTION__,
                 m_sessionList.GetItemCount());
            return FALSE;
        }
        pSession = (T_SESSION *) m_sessionList.GetItem(&sessionId);
        if (pSession == NULL)
        {
            xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::%s(): Can not find session, list num=%d", __FUNCTION__,
                 m_sessionList.GetItemCount());
            return FALSE;
        }
    }
    if (pSession->pSessionObj == NULL)
    {
        try
        {
            pSession->pSessionObj = new CSessionReceiver(sessionId, NULL, m_sock, m_dwNetEncryptMode, m_dwLocalDeviceId);
        }
        catch (...)
        {
            pSession->pSessionObj = NULL;
            xlog(XLOG_LEVEL_ERROR, "CMTLibInstance::%s(): new receiver codec(%s) error.", __FUNCTION__);
            return FALSE;
        }
    }

    //
    // input packet to receiver-buffer
    //
    if (!pSession->pSessionObj->ReceivePacket(pFrameHdr, dwRemoteDeviceIp, wRemotePort))
    {
        return FALSE;
    }

    //
    // get whole-frames from receiver-buffer, and notify to java callback method
    //
    void *pFrame = NULL;
    int iFrameSize = 0;
    int iImageResolution = 0;
    int iWidth = 0;
    int iHeight = 0;
    DWORD dwFrameType = 0;
    while (pSession->pSessionObj->GetReceivedFrame(&pFrame, &iFrameSize, &iImageResolution, &iWidth, &iHeight, &dwFrameType))
    {
        // make ip:port string
        char szIpPort[64] = {""};
        XMakeIpPortString(dwRemoteDeviceIp, wRemotePort, szIpPort);

        // callback
        if (sessionId.eMedia == SESSION_AUDIO)
        {
            NotifyJavaCallbackMethod(pJava, "onReceivedAudioFrame", szIpPort, sessionId.dwRemoteDeviceId,
                                     sessionId.iRemoteChannelIndex, m_dwLocalDeviceId, sessionId.iLocalChannelIndex, dwFrameType,
                                     (BYTE *) pFrame, iFrameSize, pSession->pSessionObj->GetCodecName(), iImageResolution, iWidth,
                                     iHeight);
        }
        else
        {
            NotifyJavaCallbackMethod(pJava, "onReceivedVideoFrame", szIpPort, sessionId.dwRemoteDeviceId,
                                     sessionId.iRemoteChannelIndex, m_dwLocalDeviceId, sessionId.iLocalChannelIndex, dwFrameType,
                                     (BYTE *) pFrame, iFrameSize, pSession->pSessionObj->GetCodecName(), iImageResolution, iWidth,
                                     iHeight);
        }
    }

    return TRUE;
}

void CMTLibInstance::OnAutoDeleteSessionThread()
{
    xlog(XLOG_LEVEL_NORMAL, "thread func [%s] tid [%lu]\n", __func__, gettid());
    while(1)
    {
        // 10 seconds timer
        for(int i=0; i<1000; i++)
        {
            if(m_bToStopThread)
            {
                break;
            }
            MY_SLEEP(10);
        }
        if(m_bToStopThread)
        {
            break;
        }

        // check all session, delete some if session timeout (30 seconds no any data)
        if(!m_sessionList.IsValid())
        {
            break;
        }
        const int iCount = m_sessionList.GetItemCount();
        if(iCount > 0)
        {
            const time_t tmCurrTime = time(NULL);
            for (int i = iCount-1; i >= 0; i--)
            {
                T_SESSION *pSession = (T_SESSION *) m_sessionList.GetItem(i);
                if (pSession && pSession->pSessionObj)
                {
                    const time_t tmSessionLastTime = pSession->pSessionObj->GetLastPacketTime();
                    if(tmSessionLastTime != 0 && tmCurrTime > tmSessionLastTime && (tmCurrTime - tmSessionLastTime) >= 30)
                    {
                        xlog(XLOG_LEVEL_NORMAL, "delete timeout session: %s %s",
                             pSession->Id.eMedia == SESSION_AUDIO ? "audio" : "video",
                             pSession->Id.eDir == SESSION_SENDER ? "sender" : "receiver");
                        // save codec data, like H264-SPS-PPS
                        const int iChannel = pSession->Id.iLocalChannelIndex;
                        if(pSession->Id.eDir == SESSION_SENDER && iChannel >= 0 && iChannel < WVM_MAX_MT_ENCODER_NUMBER)
                        {
                            const int iCodecSize = pSession->pSessionObj->GetCodecInitDataLength();
                            if(iCodecSize > 0)
                            {
                                if(m_pCodecData[iChannel])
                                {
                                    free(m_pCodecData[iChannel]);
                                    m_pCodecData[iChannel] = NULL;
                                    m_iCodecDataSize[iChannel] = 0;
                                }
                                m_pCodecData[iChannel] = malloc((size_t) iCodecSize);
                                if(m_pCodecData[iChannel])
                                {
                                    pSession->pSessionObj->GetCodecInitData(m_pCodecData[iChannel], iCodecSize, &m_iCodecDataSize[iChannel]);
                                }
                            }
                        }
                        // delete object
                        try
                        {
                            delete pSession->pSessionObj;
                        }
                        catch (...)
                        {
                        }
                        pSession->pSessionObj = NULL;
                        // remove session from list
                        m_sessionList.Remove(i, false);
                    }
                }
            }
        }
    }
}

T_DWVM_TEST_NET_RESULT* CMTLibInstance::TestNetSpeed(
    DWORD dwRemoteDeviceId,
    const char* szRemoteDeviceIpPort,
    int iTestSeconds)
{
    if(0 == dwRemoteDeviceId || NULL == szRemoteDeviceIpPort || iTestSeconds < 1 || iTestSeconds > 60)
    {
        xlog(XLOG_LEVEL_ERROR, "TestNetSpeed(): invalidate parameters");
        return NULL;
    }

    //
    // get ip & port from string
    //
    DWORD dwIp = 0;
    WORD wPort = 0;
    XGetIpPortFromString(szRemoteDeviceIpPort, &dwIp, &wPort);
    if (0 == dwIp || 0 == wPort)
    {
        xlog(XLOG_LEVEL_ERROR, "TestNetSpeed(): get ip string failed.");
        return NULL;
    }

    if (INVALID_SOCKET == m_sock)
    {
        xlog(XLOG_LEVEL_ERROR, "TestNetSpeed(): socket not created.");
        return NULL;
    }
    CNet *pNet = FindNetFromSocket(m_sock);
    if (NULL == pNet)
    {
        xlog(XLOG_LEVEL_ERROR, "TestNetSpeed(): invalidate socket.");
        return NULL;
    }

    if(!pNet->StartTest(dwRemoteDeviceId, dwIp, wPort, (WORD)iTestSeconds))
    {
        xlog(XLOG_LEVEL_ERROR, "TestNetSpeed(): failed to call StartTest()");
        return NULL;
    }

    // wait for test finished
    while(true)
    {
        MY_SLEEP(1000);
        m_resTestSpeed.dwSize = sizeof(m_resTestSpeed);
        if(!pNet->GetTestStatus(&m_resTestSpeed))
        {
            xlog(XLOG_LEVEL_ERROR, "TestNetSpeed(): failed to get test-result");
            return NULL;
        }
        if(m_resTestSpeed.wStep == 0)
        {
            break;
        }
    }

    return &m_resTestSpeed;
}
