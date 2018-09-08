#include "DwvmBase.h"

//
// 大客户数据的发送和接收
//

//===============
// 发送方
//===============

static LONG g_lSenderFrameNumber = 1;

typedef struct
{
    DWORD dwTimeStamp;
    DWORD dwCustomID;
    SOCKET sock;
    DWORD dwDestDeviceId;
    DWORD dwDestIp;
    WORD wDestPort;
} T_SENDER_OPTION;

CBigCustomDataSender::CBigCustomDataSender() :
    m_fifo()
{
    m_hThread = INVALID_THREAD_HANDLE;
    m_bToExit = TRUE;

    m_sock = INVALID_SOCKET;
    m_dwDestDeviceId = 0;
    m_dwDestIp = 0;
    m_wDestPort = 0;

    m_cbProc = NULL;
    m_cbContext = NULL;
}

CBigCustomDataSender::~CBigCustomDataSender()
{
    m_bToExit = TRUE;
    WAIT_AND_TERMINATE_THREAD(m_hThread, 100, "DwvmBase::BigCustomDataSender");

    void *pData = NULL;
    int iDataLen = 0;
    void *pContext = NULL;
    while (m_fifo.Peek(&pData, &iDataLen, &pContext))
    {
        m_fifo.Pop(TRUE);
        if (pContext)
        {
            free(pContext);
            pContext = NULL;
        }
    }
    m_fifo.UnInit();
}

void CBigCustomDataSender::Setup(pfnDwvmBigCustomDataSendFinished cbProc, void *cbContext, int iMaxBufferNumber)
{
    m_cbProc = cbProc;
    m_cbContext = cbContext;

    if (!m_fifo.IsInited())
    {
        m_fifo.Init(iMaxBufferNumber, 16);
    }
}

BOOL CBigCustomDataSender::Push(void *pData, int iDataLen, DWORD dwCustomID, SOCKET sock, DWORD dwDestDeviceId,
                                DWORD dwDestIp, WORD wDestPort)
{
    // copy需要发送的数据到buffer，等待Thread_Sender线程去发送
    T_SENDER_OPTION *opt = (T_SENDER_OPTION *) malloc(sizeof(T_SENDER_OPTION));
    if (NULL == opt)
    {
        return FALSE;
    }
    opt->dwTimeStamp = timeGetTime();
    opt->dwCustomID = dwCustomID;
    opt->sock = sock;
    opt->dwDestDeviceId = dwDestDeviceId;
    opt->dwDestIp = dwDestIp;
    opt->wDestPort = wDestPort;
    //
    if (!m_fifo.Push(pData, iDataLen, opt))
    {
        free(opt);
        return FALSE;
    }

    // 创建线程去发送
    if (INVALID_THREAD_HANDLE == m_hThread)
    {
        m_bToExit = FALSE;
        THREAD_CREATE(m_hThread, Thread_Sender, this);
        if (INVALID_THREAD_HANDLE == m_hThread)
        {
            return FALSE;
        }
        THREAD_SET_NAME(m_hThread, "DWVM_BigCustomData_Sender");
    }

    return TRUE;
}

// 发送线程
void CBigCustomDataSender::OnSenderThread()
{
    BYTE PacketBuffer[WVM_MTU];
    memset(PacketBuffer, 0, sizeof(PacketBuffer));

    const int iMaxPktLen = min(1024, WVM_MTU) - sizeof(T_WVM_PACKET_HEADER) - sizeof(T_WVM_BIG_CUSTOM_DATA_HEADER);

    xlog(XLOG_LEVEL_NORMAL, "thread func [%s] tid [%lu]\n", __func__, gettid());

    while (!m_bToExit)
    {
        void *pData = NULL;
        int iDataLen = 0;
        void *pContext = NULL;
        if (m_fifo.Peek(&pData, &iDataLen, &pContext))
        {
            // 参数
            if (NULL == pContext || NULL == pData || iDataLen <= 0)
            {
                continue;
            }
            T_SENDER_OPTION opt;
            memcpy(&opt, pContext, sizeof(T_SENDER_OPTION));
            free(pContext);

            // 如果目标改变，重新设置socket参数
            //if(m_sock != opt.sock||
            //  m_dwDestDeviceId != opt.dwDestDeviceId ||
            //   m_dwDestIp != opt.dwDestIp ||
            //   m_wDestPort != opt.wDestPort)
            // 每次都重新设置参数，使其重新计算平均应答时间
            {
                for (int i = 0; i < MULTI_SENDING; i++)
                {
                    if (!m_tcp[i].Setup(opt.sock, opt.dwDestDeviceId, opt.dwDestIp, opt.wDestPort))
                    {
                        xlog(XLOG_LEVEL_WARNING, "CBigCustomDataSender: tcp setup failed !\n");
                    }
                }
                m_sock = opt.sock;
                m_dwDestDeviceId = opt.dwDestDeviceId;
                m_dwDestIp = opt.dwDestIp;
                m_wDestPort = opt.wDestPort;
            }

            // 全局帧序号
            const DWORD dwFrameSeq = (DWORD) (++g_lSenderFrameNumber);//InterlockedIncrement(&g_lSenderFrameNumber);
            // CRC校验码
            const WORD wCrcVerify = XMakeCrc(pData, iDataLen);

            // 分拆成小包，逐个发送
            const int iPktNumber = (iDataLen + iMaxPktLen - 1) / iMaxPktLen;
            BOOL bSucceed = TRUE; // 发送是否成功
            BYTE *pRemainData = (BYTE *) pData;
            int iRemainLen = iDataLen;
            for (int i = 0; i < iPktNumber; i++)
            {
                // !!! 需要确保第一个包被第一个收到、最后一个包被最后收到，其它包可以并行发送
                const BOOL bIsKeyPacket = (0 == i || (iPktNumber - 1) == i);

                const int iSendDataLen = min(iRemainLen, iMaxPktLen);
                T_WVM_BIG_CUSTOM_DATA_HEADER *hdr = (T_WVM_BIG_CUSTOM_DATA_HEADER *) PacketBuffer;
                hdr->dwFrameSeq = dwFrameSeq;
                hdr->dwFrameBytes = (DWORD) iDataLen;
                hdr->wPktNumber = (WORD) iPktNumber;
                hdr->wPktSeq = (WORD) i;
                hdr->wPktBytes = (WORD) iSendDataLen;
                hdr->dwPktOffset = (DWORD) (pRemainData - (BYTE *) pData);
                hdr->wCrcVerify = wCrcVerify;
                hdr->dwCustomID = opt.dwCustomID;
                memcpy(PacketBuffer + sizeof(T_WVM_BIG_CUSTOM_DATA_HEADER), pRemainData, (size_t) iSendDataLen);

                int iIdleTcpIndex = -1;
                while (1)
                {
                    BOOL bAllIdle = TRUE;
                    for (int j = 0; j < MULTI_SENDING; j++)
                    {
                        if (m_tcp[j].IsSending())
                        {
                            bAllIdle = FALSE;
                            const int iResult = m_tcp[j].AsyncCheckAndResend(0); // 返回值：0 正在发送中, 1 发送完成, -1 超时, -2 发送失败
                            if (iResult == 1)
                            {
                                iIdleTcpIndex = j;
                            }
                            else if (iResult < 0)
                            {
                                bSucceed = FALSE;
                                break;
                            }
                        }
                        else
                        {
                            iIdleTcpIndex = j;
                        }
                    }
                    if (iIdleTcpIndex < 0 || (bIsKeyPacket && !bAllIdle))
                    {
                        MY_SLEEP(1);
                    }
                    else
                    {
                        break;
                    }
                }
                if (!bSucceed)
                {
                    break;
                }

                if (bIsKeyPacket)
                {
                    for (int t = 0; t < 3; t++) // 尝试3次
                    {
                        bSucceed = m_tcp[iIdleTcpIndex].SendAndWait(WVM_CMD_BIG_CUSTOM_DATA, PacketBuffer,
                                                                    sizeof(T_WVM_BIG_CUSTOM_DATA_HEADER) + iSendDataLen,
                                                                    0);
                        if (bSucceed)
                        {
                            break;
                        }
                    }
                }
                else
                {
                    bSucceed = m_tcp[iIdleTcpIndex].AsyncSend(WVM_CMD_BIG_CUSTOM_DATA, PacketBuffer,
                                                              sizeof(T_WVM_BIG_CUSTOM_DATA_HEADER) + iSendDataLen, 0);
                }
                if (!bSucceed)
                {
                    break;
                }

                iRemainLen -= iSendDataLen;
                pRemainData += iSendDataLen;
            }

            // 释放buffer
            m_fifo.Pop(TRUE);

            // 回调，告知发送完毕
            try
            {
                if (m_cbProc)
                {
                    m_cbProc(m_cbContext, bSucceed, pData, iDataLen, opt.dwCustomID, timeGetTime() - opt.dwTimeStamp);
                }
            }
            catch (...)
            {
                xlog(XLOG_LEVEL_WARNING, "CBigCustomDataSender callback exception !\n");
            }
        }

        MY_SLEEP(10);
    }
}

//===============
// 接收方
//===============

CBigCustomDataReceiver::CBigCustomDataReceiver()
{
    m_cbProc = NULL;
    m_cbContext = NULL;
    m_iBigBufferSize = 0;
    m_pBigBuffer = NULL;
    m_dwCurrFrameSeq = 0;
}

CBigCustomDataReceiver::~CBigCustomDataReceiver()
{
    if (m_pBigBuffer)
    {
        free(m_pBigBuffer);
        m_pBigBuffer = NULL;
    }
    m_iBigBufferSize = 0;
}

void CBigCustomDataReceiver::Setup(pfnDwvmBigCustomDataReceived cbProc, void *cbContext)
{
    m_cbProc = cbProc;
    m_cbContext = cbContext;
}

// 收到WVM_CMD_BIG_CUSTOM_DATA命令后，调用这个函数处理
BOOL CBigCustomDataReceiver::PushPkt(DWORD dwFromIp, WORD wFromPort, DWORD dwSrcDeviceId, void *pPkt, int iPktSize)
{
    if (NULL == pPkt || iPktSize <= sizeof(T_WVM_BIG_CUSTOM_DATA_HEADER))
    {
        return FALSE;
    }

    // 数据尺寸不对
    T_WVM_BIG_CUSTOM_DATA_HEADER *bcd = (T_WVM_BIG_CUSTOM_DATA_HEADER *) pPkt;
    if ((int) (sizeof(T_WVM_BIG_CUSTOM_DATA_HEADER) + bcd->wPktBytes) < iPktSize)
    {
        return FALSE;
    }

    // 新的一帧
    if (0 == bcd->wPktSeq)
    {
        // 新的帧序号
        m_dwCurrFrameSeq = bcd->dwFrameSeq;

        // 分配buffer
        if (m_iBigBufferSize < (int) bcd->dwFrameBytes)
        {
            if (m_pBigBuffer)
            {
                free(m_pBigBuffer);
                m_pBigBuffer = NULL;
            }
        }
        if (NULL == m_pBigBuffer)
        {
            m_pBigBuffer = (BYTE *) malloc(bcd->dwFrameBytes + 4096);
            if (NULL == m_pBigBuffer)
            {
                return FALSE;
            }
            m_iBigBufferSize = bcd->dwFrameBytes + 4096;
        }
        memset(m_pBigBuffer, 0, (size_t) m_iBigBufferSize);
    }

    // 不接收老数据
    if (bcd->dwFrameSeq != m_dwCurrFrameSeq)
    {
        return FALSE;
    }

    // 分配buffer失败
    if (NULL == m_pBigBuffer)
    {
        return FALSE;
    }
    // 小包位置不对
    if ((bcd->dwPktOffset + bcd->wPktBytes) > (DWORD) m_iBigBufferSize)
    {
        return FALSE;
    }
    // 数据放到正确的位置
    memcpy(m_pBigBuffer + bcd->dwPktOffset, &bcd[1], bcd->wPktBytes);

    // 如果是最后一个包，校验CRC并回调整个buffer给高层
    if (bcd->wPktSeq == (bcd->wPktNumber - 1))
    {
        const WORD wCrcVerify = XMakeCrc(m_pBigBuffer, bcd->dwFrameBytes);
        if (wCrcVerify != bcd->wCrcVerify)
        {
            xlog(XLOG_LEVEL_WARNING, "CBigCustomDataReceiver::PushPkt() crc fail, calc(%04X) != src(%04X)\n",
                 wCrcVerify, bcd->wCrcVerify);
        }
        else if (m_cbProc)
        {
            try
            {
                m_cbProc(m_cbContext, m_pBigBuffer, bcd->dwFrameBytes, bcd->dwCustomID, dwFromIp, wFromPort,
                         dwSrcDeviceId);
            }
            catch (...)
            {
            }
        }

        // 清除缓存的数据
        m_dwCurrFrameSeq = 0;
    }

    return TRUE;
}
