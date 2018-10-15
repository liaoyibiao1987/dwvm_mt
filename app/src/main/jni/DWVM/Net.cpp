// Net.cpp: implementation of the CNet class.
//
//////////////////////////////////////////////////////////////////////

#include "XLib.h"
#include "Net.h"
#include "DwvmBase.h"

#define IP_UDP_SIZE    (28) // OS会为每个包自动添加：IP头20字节, UDP头8字节

extern CXSimpleList g_listTcpTunnelOverUdp;

//#define TEST_STAT_SEND //DEBUG: 统计发送包并每隔5秒显示一次
#ifdef TEST_STAT_SEND
static DWORD m_dwStatBeginTime = 0;
static DWORD m_dwStatSendQueryBytes = 0;
static DWORD m_dwStatSendQueryNumber = 0;
static DWORD m_dwStatSendActualBytes = 0;
static DWORD m_dwStatSendActualNumber = 0;
#endif

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

CNet::CNet() :
    m_RecvFifo(),
    m_SendFifo()
{
    m_dwLocalDeviceId = 0;
    m_sock = INVALID_SOCKET;
    m_wLocalPort = 0;
    m_iSocketBufferSize = WVM_DEFAULT_SOCKET_BUFFER_SIZE;
    m_pfnCbProc = NULL;
    m_pCbContext = NULL;
    m_hRecvThread = INVALID_THREAD_HANDLE;
    m_hSendThread = INVALID_THREAD_HANDLE;

    m_dwCbThreadNumber = 1;
    m_dwCbThreadStackSize = 0;
    m_lCbThreadWorkingCnt = 0;
    m_lCbTotalCnt = 0;
    for (int j = 0; j < MAX_CB_THREAD_NUMBER; j++)
    {
        m_hCbThread[j] = INVALID_THREAD_HANDLE;
    }

    m_bTestMode = FALSE;
    m_dbStepNeedTime = 0.0;
    m_dbSendTick = 0.0;
    m_dwSendPktSeq = 0x80000000L; // 最高bit为1
    m_dbSendElapse = 0.0;
    m_dwRecvAckSeq = 0;
    for (int i = 0; i < 3; i++)
    {
        m_dbTestBeginTime[i] = 0.0;
    }
    memset(&m_Test, 0, sizeof(m_Test));
    m_Test.dwSize = sizeof(m_Test);
}

CNet::~CNet()
{
    Destroy();
}

BOOL CNet::Create(
    DWORD dwLocalDeviceId,            // 本设备的ID
    WORD wLocalPort,                // 本地端口号
    DWORD dwBindLocalIp,            // 绑定本机的IP地址
    int iSocketBufferSize,            // socket buffer尺寸. 小于等于0时，用默认尺寸 WVM_DEFAULT_SOCKET_BUFFER_SIZE (256000)
    pfnDwvmNetReceive pfnCbProc,    // 接收网络包的回调函数指针
    void *pCbContext,                // 网络包回调函数的上下文参数. 会在回调函数 pfnCbProc 的 pCbContext 参数中原样返回
    DWORD dwCbThreadNumber,            // 网络包回调线程的数量。<=1 表示用单个线程回调，>1 表示用多个线程回调
    DWORD dwCbThreadStackSize)        // 网络包回调线程的堆栈尺寸，单位：字节。0 表示使用进程默认尺寸
{
    if (INVALID_SOCKET != m_sock)
    {
        return FALSE;
    }
    if (NULL == pfnCbProc)
    {
        return FALSE;
    }

    if (dwCbThreadNumber > MAX_CB_THREAD_NUMBER)
    {
        dwCbThreadNumber = MAX_CB_THREAD_NUMBER;
    }
    if (iSocketBufferSize <= 0)
    {
        iSocketBufferSize = WVM_DEFAULT_SOCKET_BUFFER_SIZE;
    }
    else if (iSocketBufferSize > MAX_SOCKET_BUFFER_SIZE)
    {
        iSocketBufferSize = MAX_SOCKET_BUFFER_SIZE;
    }
    m_iSocketBufferSize = iSocketBufferSize;
    m_dwLocalDeviceId = dwLocalDeviceId;
    m_wLocalPort = wLocalPort;
    m_pfnCbProc = pfnCbProc;
    m_pCbContext = pCbContext;
    m_dwCbThreadNumber = dwCbThreadNumber;
    m_dwCbThreadStackSize = dwCbThreadStackSize;
    m_lCbThreadWorkingCnt = 0;
    m_lCbTotalCnt = 0;

    // socket
    if (INVALID_SOCKET == (m_sock = udp_create()) ||
        !udp_bind(m_sock, wLocalPort, dwBindLocalIp))
    {
        return FALSE;
    }
    socket_setrecvbuf(m_sock, iSocketBufferSize);
    socket_setsendbuf(m_sock, iSocketBufferSize);

    // fifo
    if (!m_RecvFifo.Init(WVM_MTU, iSocketBufferSize / WVM_MTU) ||
        !m_SendFifo.Init(WVM_MTU, iSocketBufferSize / WVM_MTU))
    {
        Destroy();
        return FALSE;
    }

    // threads
    THREAD_CREATE(m_hRecvThread, Thread_Receiver, this);
    THREAD_CREATE(m_hSendThread, Thread_Sender, this);
    if (INVALID_THREAD_HANDLE == m_hRecvThread || INVALID_THREAD_HANDLE == m_hSendThread)
    {
        Destroy();
        return FALSE;
    }
    // cb threads
    for (int j = 0; j < (int) m_dwCbThreadNumber; j++)
    {
        THREAD_CREATE(m_hCbThread[j], Thread_Callback, this);
        if (INVALID_THREAD_HANDLE == m_hCbThread[j])
        {
            break;
        }
    }
    if (INVALID_THREAD_HANDLE == m_hCbThread[0])
    {
        Destroy();
        return FALSE;
    }

    return TRUE;
}

void CNet::Destroy()
{
    if (INVALID_SOCKET == m_sock)
    {
        return;
    }
    udp_destroy(m_sock);
    m_sock = INVALID_SOCKET;

    m_RecvFifo.UnInit();
    m_SendFifo.UnInit();

    WAIT_AND_TERMINATE_THREAD(m_hRecvThread, 100, "DwvmBase::CNet::Recv");
    WAIT_AND_TERMINATE_THREAD(m_hSendThread, 100, "DwvmBase::CNet::Send");

    for (int j = 0; j < MAX_CB_THREAD_NUMBER; j++)
    {
        WAIT_AND_TERMINATE_THREAD(m_hCbThread[j], 100, "DwvmBase::CNet::Callback");
    }

    m_pfnCbProc = NULL;
}

DWORD CNet::Send(BOOL bInner, DWORD dwSrcDeviceId, DWORD dwCmd, DWORD dwNeedReply, DWORD dwDestDeviceId, DWORD dwDestIp,
                 WORD wDestPort, void *pData, int iDataSize, DWORD dwEncrypt, DWORD dwPktSeq)
{
    static DWORD s_dwSeq = 0;

    if (0 == dwDestIp || 0 == wDestPort || (NULL == pData && iDataSize > 0) ||
        iDataSize > (WVM_MTU - sizeof(T_WVM_PACKET_HEADER)))
    {
        return 0;
    }

    // 因为序号的最高位要用来标示是否内部包，所以需要限制序号的最大值
    if (s_dwSeq & WVM_SEQ_INNER_FLAG)
    {
        s_dwSeq = 0;
    }

    const DWORD dwSeq = (0 == dwPktSeq) ? (++s_dwSeq) : (dwPktSeq); // 如果没有指定包序号，生成一个新序号
    BYTE Buffer[WVM_MTU];
    T_WVM_PACKET_HEADER *hdr = (T_WVM_PACKET_HEADER *) Buffer;

    // 填充头信息
    memset(hdr, 0, sizeof(T_WVM_PACKET_HEADER));
    hdr->dwStartCode = WVM_START_CODE;
    hdr->dwSize = sizeof(T_WVM_PACKET_HEADER);
    hdr->dwCmd = dwCmd;
    hdr->dwSeq = bInner ? (dwSeq | WVM_SEQ_INNER_FLAG) : (dwSeq & (~WVM_SEQ_INNER_FLAG));
    hdr->dwSendingTick = timeGetTime();
    hdr->dwReplyContext = dwNeedReply;
    hdr->dwSrcId = dwSrcDeviceId;
    hdr->dwDestId = dwDestDeviceId;
    hdr->dwDataSize = (DWORD) (iDataSize > 0 ? iDataSize : 0);
    // 复制数据
    if (iDataSize > 0)
    {
        memcpy(Buffer + hdr->dwSize, pData, (size_t) iDataSize);
    }
    // 加密
    if ((1L << 28) == dwEncrypt)
    {
        hdr->dwEncrypt = dwEncrypt;
        DWVM_Encrypt(Buffer + hdr->dwSize, iDataSize);
    }
    else
    {
        hdr->dwEncrypt = 0;
    }
    // 校验和
    hdr->dwDataByteSum = 0;
    for (int i = 0; i < iDataSize; i++)
    {
        hdr->dwDataByteSum += *(Buffer + hdr->dwSize + i);
    }

#ifdef TEST_STAT_SEND
    // 统计
    m_dwStatSendQueryNumber ++;
    m_dwStatSendQueryBytes += hdr->dwSize + hdr->dwDataSize;
#endif

    // 添加到FIFO
    if (!m_SendFifo.Push(Buffer, hdr->dwSize + hdr->dwDataSize, (long) dwDestIp, (long) wDestPort))
    {
        xlog(XLOG_LEVEL_ERROR, "Push NetPacket to sende-list FAIL! len=%d, init=%d, cache=%d",
             hdr->dwSize + hdr->dwDataSize, m_SendFifo.IsInited(), m_SendFifo.GetCount());
        return 0;
    }

    return dwSeq;
}

void CNet::OnRecvThread()
{
    BYTE RecvBuffer[WVM_MTU + 1024];
    int iRecvLen = 0;
    DWORD dwFromIp = 0;
    WORD wFromPort = 0;
    DWORD dwSum = 0;
    int i = 0;
    T_WVM_PACKET_HEADER *hdr = (T_WVM_PACKET_HEADER *) RecvBuffer;

    while (INVALID_SOCKET != m_sock &&
           (iRecvLen = udp_receive(m_sock, RecvBuffer, sizeof(RecvBuffer), &dwFromIp, &wFromPort, TRUE)) > 0)
    {
        // 检查数据长度是否有效
        if (iRecvLen > WVM_MTU || iRecvLen < (int) (hdr->dwSize + hdr->dwDataSize))
        {
            xlog(XLOG_LEVEL_WARNING,
                 "Recv NetPacket len invalid! from %s:%u, len %d, head %lu, data %lu",
                 socket_getstring(dwFromIp), wFromPort, iRecvLen, hdr->dwSize, hdr->dwDataSize);
            continue;
        }

        // 检查包头的开始码是否有效
        if (WVM_START_CODE != hdr->dwStartCode)
        {
            xlog(XLOG_LEVEL_WARNING, "Recv NetPacket start-code invalid: 0x%08X, from %s:%u",
                 hdr->dwStartCode, socket_getstring(dwFromIp), wFromPort);
            continue;
        }

        // 检查数据的校验和
        dwSum = 0;
        for (i = 0; i < (int) hdr->dwDataSize; i++)
        {
            dwSum += *(RecvBuffer + hdr->dwSize + i);
        }
        if (dwSum != hdr->dwDataByteSum)
        {
            xlog(XLOG_LEVEL_WARNING, "Recv NetPacket sum invalid: calc=%lu, hdr=%lu, from %s:%u", dwSum,
                 hdr->dwDataByteSum, socket_getstring(dwFromIp), wFromPort);
            continue;
        }

        // 解密数据
        if ((1L << 28) == hdr->dwEncrypt)
        {
            DWVM_Decrypt(RecvBuffer + hdr->dwSize, (int) hdr->dwDataSize);
        }

        // 去掉内部标志，恢复正常序号
        const BOOL bInner = (hdr->dwSeq & WVM_SEQ_INNER_FLAG) ? TRUE : FALSE;
        if (bInner)
        {
            hdr->dwSeq &= ~WVM_SEQ_INNER_FLAG;
        }

        // 应答: 发送端需要应答、并且是内部包、并且设备ID匹配
        if (0 != hdr->dwReplyContext && bInner && (0 == m_dwLocalDeviceId || hdr->dwDestId == m_dwLocalDeviceId))
        {
            T_WVM_REPLY r;
            memset(&r, 0, sizeof(r));
            r.dwSize = sizeof(r);
            r.dwSrcCmd = hdr->dwCmd;
            r.dwSrcSeq = hdr->dwSeq;
            r.dwSrcTick = hdr->dwSendingTick;
            r.dwSrcContext = hdr->dwReplyContext;
            this->Send(TRUE, m_dwLocalDeviceId, WVM_CMD_REPLY, FALSE, hdr->dwSrcId, dwFromIp, wFromPort, &r, sizeof(r),
                       0, 0);
        }

        if (WVM_CMD_TEST_NET == hdr->dwCmd)
        {
            // 远端发来的测试包，不需要交给其它模块处理
            continue;
        }
        else if (WVM_CMD_REPLY == hdr->dwCmd)
        {
            // 应答包
            T_WVM_REPLY *pr = (T_WVM_REPLY *) (RecvBuffer + hdr->dwSize);
            if (WVM_CMD_TEST_NET == pr->dwSrcCmd)
            {
                // 远端对我发出的测试包的应答，交给测试模块处理
                Test_OnRecvThread(pr);
                continue;
            }
            else
            {
                // TCP over UDP 应答包，内部处理
                CTcpSenderOverUdp *pTcpSender = (CTcpSenderOverUdp *) g_listTcpTunnelOverUdp.GetItem(
                    (DWORD) pr->dwSrcContext);
                if (pTcpSender)
                {
                    try
                    {
                        pTcpSender->CheckAck(pr);
                    }
                    catch (...)
                    {
                    }
                    continue;
                }
            }
        }

        // 放入FIFO, 由回调线程处理
        if (!m_RecvFifo.Push(RecvBuffer, iRecvLen, (long) dwFromIp, (long) wFromPort))
        {
            xlog(XLOG_LEVEL_ERROR,
                 "Push NetPacket to recv-list FAIL! len=%d, init=%d, cache=%d, thread_run=%d, cb_total=%d",
                 iRecvLen, m_RecvFifo.IsInited(), m_RecvFifo.GetCount(), m_lCbThreadWorkingCnt, m_lCbTotalCnt);
        }

        // 输出Debug信息: 每5秒一次
        {
            static LONG lCbTotal = m_lCbTotalCnt;
            static DWORD dwStartTime = timeGetTime();
            const DWORD dwCurrTime = timeGetTime();
            const DWORD dwTime = dwCurrTime - dwStartTime;
            if (dwTime >= 5000)
            {
                xlog(XLOG_LEVEL_NORMAL, "CNet recv-list: cache=%d, thread_run=%d, cb_total=%d (%d/s)",
                     m_RecvFifo.GetCount(), m_lCbThreadWorkingCnt, m_lCbTotalCnt - lCbTotal,
                     (m_lCbTotalCnt - lCbTotal) * 1000 / dwTime);
                dwStartTime = dwCurrTime;
                lCbTotal = m_lCbTotalCnt;
            }
        }
    }
}

void CNet::OnCallbackThread()
{
    BYTE Buffer[WVM_MTU + 1024];
    int iLen = 0;
    DWORD dwFromIp = 0;
    DWORD dwFromPort = 0;
    T_WVM_PACKET_HEADER *hdr = (T_WVM_PACKET_HEADER *) Buffer;

    T_DWVM_JNI_ENV javaTodo;
    memset(&javaTodo, 0, sizeof(T_DWVM_JNI_ENV));
    // initialize java thread-env
    if (m_pfnCbProc)
    {
        javaTodo.todo = 1;
        m_pfnCbProc(&javaTodo, m_sock, m_pCbContext, 0, 0, 0, NULL, 0);
        if (javaTodo.javaMTLibCallbackMethod == NULL)
        {
            xlog(XLOG_LEVEL_ERROR, "FAILED TO INITIALIZE java-env ON CALLBACK THREAD!\n");
            return;
        }
    }
    javaTodo.todo = 0;

    while (m_pfnCbProc && m_RecvFifo.IsInited())
    {
        MY_SLEEP(2);
        while (m_RecvFifo.Pop(Buffer, sizeof(Buffer), &iLen, (long *) &dwFromIp, (long *) &dwFromPort))
        {
            m_lCbTotalCnt++; //InterlockedIncrement(&m_lCbTotalCnt);
            m_lCbThreadWorkingCnt++; //InterlockedIncrement(&m_lCbThreadWorkingCnt);

            m_pfnCbProc(
                &javaTodo,
                m_sock,
                m_pCbContext,
                dwFromIp,
                (WORD) dwFromPort,
                hdr->dwCmd,
                Buffer,
                iLen);

            m_lCbThreadWorkingCnt--; //InterlockedDecrement(&m_lCbThreadWorkingCnt);
        }
    }

    // free java thread-env
    if (m_pfnCbProc)
    {
        javaTodo.todo = 2;
        m_pfnCbProc(&javaTodo, m_sock, m_pCbContext, 0, 0, 0, NULL, 0);
    }
}

static void test_sleep_ms(DWORD dwSleepMs, int iTestCount = 100)
{
    const DWORD dwTime1 = timeGetTime();
    for(int i=0; i<iTestCount; i++)
    {
        MY_SLEEP(dwSleepMs);
    }
    const DWORD dwUseMs = timeGetTime() - dwTime1;
    xlog(XLOG_LEVEL_NORMAL, "%s(%u ms,%d times) use time %u ms avg %u ms", __func__, dwSleepMs, iTestCount, dwUseMs, dwUseMs/iTestCount);
}

void CNet::OnSendThread()
{
    BYTE Buffer[WVM_MTU + 1024];
    int iLen = 0;
    DWORD dwDestIp = 0;
    DWORD dwDestPort = 0;
    T_WVM_PACKET_HEADER *hdr = (T_WVM_PACKET_HEADER *) Buffer;

	// 限制发送的频率: 每 2~15 毫秒发送1个包
	const double net_send_limit_PktInterlaceMsMin = 2.0;
    const double net_send_limit_PktInterlaceMsMax = 15.0;
	double dbPrevPktSendingMs = 0;
    double dbInterlaceMs = 0;
    double dbCurrMs = 0;
    double dbStatMinInterlaceMs = net_send_limit_PktInterlaceMsMax;
    //xlog(XLOG_LEVEL_NORMAL, "[%s] limit send (%.1f ~ %.1f) ms/pkt",__func__, net_send_limit_PktInterlaceMsMin,net_send_limit_PktInterlaceMsMax);

    //DEBUG: 测试 MY_SLEEP() 的实际时间
    //for(DWORD dwTestMs=1;dwTestMs<10;dwTestMs++)
    //{
    //    test_sleep_ms(dwTestMs);
    //}

    while (m_SendFifo.IsInited())
    {
        if(m_SendFifo.GetCount() > 0)
        {
            // 编码为12fps，即每80毫秒一帧
            // 这里限定在70ms内平均发送完所有包
            // 如果修改了编码帧率，需要同步修改这个参数 (70.0) //DEBUG
            dbInterlaceMs = 70.0 / m_SendFifo.GetCount();
            if(dbInterlaceMs < net_send_limit_PktInterlaceMsMin) dbInterlaceMs = net_send_limit_PktInterlaceMsMin;
            if(dbInterlaceMs > net_send_limit_PktInterlaceMsMax) dbInterlaceMs = net_send_limit_PktInterlaceMsMax;
            // 限制发送的频率，误差0.5毫秒
            if((GetCpuMs() - dbPrevPktSendingMs) >= (dbInterlaceMs-0.5) &&
                m_SendFifo.Pop(Buffer, sizeof(Buffer), &iLen, (long *) &dwDestIp, (long *) &dwDestPort))
            {
                hdr->dwSendingTick = timeGetTime();
                if (udp_send(m_sock, Buffer, iLen, dwDestIp, (WORD) dwDestPort) <= 0)
                {
                    xlog(XLOG_LEVEL_ERROR, "Send NetPacket FAIL! len=%d, dest=%s:%lu", iLen, socket_getstring(dwDestIp), dwDestPort);
                }
                else
                {
                    dbCurrMs = GetCpuMs();
                    dbInterlaceMs = dbCurrMs - dbPrevPktSendingMs;
                    dbPrevPktSendingMs = dbCurrMs;
                    //DEBUG: 显示实际的最小间隔时间
                    if(dbInterlaceMs < dbStatMinInterlaceMs)
                    {
                        xlog(XLOG_LEVEL_NORMAL, "[%s] min-send-interlace-ms: %1.f -> %1.f", __func__, dbStatMinInterlaceMs, dbInterlaceMs);
                        dbStatMinInterlaceMs = dbInterlaceMs;
                    }
                    #ifdef TEST_STAT_SEND
                    // 统计
                    m_dwStatSendActualNumber ++;
                    m_dwStatSendActualBytes += iLen;
                    #endif
                }
            }
        }

        if (m_bTestMode)
        {
            Test_OnSendThread(Buffer);
        }
        else if(m_SendFifo.GetCount() <= 0)
        {
            usleep(2000);
        }
        else if(net_send_limit_PktInterlaceMsMin >= 1.0)
        {
            usleep(500);
        }

#ifdef TEST_STAT_SEND
        // 统计
        const DWORD dwTick = timeGetTime();
        if(0 == m_dwStatBeginTime)
        {
            m_dwStatBeginTime = dwTick;
        }
        const DWORD dwMs = dwTick - m_dwStatBeginTime;
        if(dwMs >= 5000)
        {
            if(m_dwStatSendQueryNumber != 0 || m_dwStatSendActualNumber != 0)
            {
                xlog(XLOG_LEVEL_NORMAL,
                     "NET SEND STAT: query %u Kbps %u pps %u Bpp, actual %u Kbps %u pps %u Bpp\n",
                     m_dwStatSendQueryBytes * 8 / dwMs,
                     m_dwStatSendQueryNumber * 1000 / dwMs,
                     (0==m_dwStatSendQueryNumber) ? 0 : (m_dwStatSendQueryBytes / m_dwStatSendQueryNumber),
                     m_dwStatSendActualBytes * 8 / dwMs,
                     m_dwStatSendActualNumber * 1000 / dwMs,
                     (0==m_dwStatSendActualNumber) ? 0 : (m_dwStatSendActualBytes / m_dwStatSendActualNumber));
            }
            m_dwStatBeginTime = dwTick;
            m_dwStatSendQueryNumber = 0;
            m_dwStatSendQueryBytes = 0;
            m_dwStatSendActualNumber = 0;
            m_dwStatSendActualBytes = 0;
        }
#endif
    }
}

BOOL CNet::StartTest(DWORD dwDestDeviceId, DWORD dwIp, WORD wPort, WORD wTestSecond)
{
    if (m_bTestMode)
    {
        return FALSE;
    }
    if (0 == dwDestDeviceId || 0 == dwIp || 0 == wPort)
    {
        return FALSE;
    }

    if (wTestSecond < 5)
    {
        wTestSecond = 5;
    }
    else if (wTestSecond > 60)
    {
        wTestSecond = 60;
    }

    m_Test.dwSize = sizeof(m_Test);
    m_Test.dwDestDeviceId = dwDestDeviceId;
    m_Test.dwDestIp = dwIp;
    m_Test.wDestPort = wPort;
    m_Test.wStep = 0;
    m_Test.wTestSecond = wTestSecond;
    for (int i = 0; i < 3; i++)
    {
        m_Test.Stat[i].iPktSendCnt = 0;
        m_Test.Stat[i].iPktAckCnt = 0;
        m_Test.Stat[i].iPktSize = 0;
        m_Test.Stat[i].dbResponse = 0.0;
        m_Test.Stat[i].dbRunTime = 0.0;
        m_dbTestBeginTime[i] = 0.0;
    }
    m_Test.Stat[0].iPktSize = IP_UDP_SIZE + sizeof(T_WVM_PACKET_HEADER);
    m_Test.Stat[1].iPktSize = IP_UDP_SIZE + WVM_MTU;
    m_Test.Stat[2].iPktSize = IP_UDP_SIZE + WVM_MTU;

    m_dbStepNeedTime = 0.0;
    m_dbSendTick = 0.0;
    m_dbSendElapse = 0.0;
    m_dwRecvAckSeq = 0;

    // 开始测试
    m_bTestMode = TRUE;

    // 等待Send线程开始测试
    const DWORD dwEndTick = timeGetTime() + 50;
    while (0 == m_Test.wStep && timeGetTime() < dwEndTick)
    {
        MY_SLEEP(0);
    }

    return TRUE;
}

void CNet::StopTest()
{
    if (!m_bTestMode)
    {
        return;
    }

    m_bTestMode = FALSE;

    // 等待Send线程停止测试过程，最多50毫秒
    const DWORD dwEndTick = timeGetTime() + 50;
    while (0 != m_Test.wStep && timeGetTime() < dwEndTick)
    {
        MY_SLEEP(0);
    }
    m_Test.wStep = 0;
}

BOOL CNet::GetTestStatus(T_DWVM_TEST_NET_RESULT *pResult)
{
    if (NULL == pResult)
    {
        return FALSE;
    }

    const int iResultSize = min(m_Test.dwSize, pResult->dwSize);
    memcpy(pResult, &m_Test, (size_t) iResultSize);
    return TRUE;
}

// 处理网络测速过程
void CNet::Test_OnSendThread(BYTE *pTestBuffer)
{
    if (0 == m_Test.wStep)
    {
        // 开始测试，进入步骤1
        m_dbTestBeginTime[0] = GetCpuMs();
        m_dbStepNeedTime = 1000.0;
        m_dbSendElapse = 10.0;
        m_Test.wStep = 1;
    }

    int iStepIndex = (int) m_Test.wStep - 1;
    if (iStepIndex < 0 || iStepIndex > 2)
    {
        return;
    }

    // 这个步骤已经进行多久了?
    m_Test.Stat[iStepIndex].dbRunTime = GetCpuMs() - m_dbTestBeginTime[iStepIndex];
    // 需要结束这个步骤吗?
    if (m_Test.Stat[iStepIndex].dbRunTime > m_dbStepNeedTime)
    {
        if (1 == m_Test.wStep)
        {
            // 如果设备不在线，就直接停止测试
            if (m_Test.Stat[0].iPktAckCnt <= 0)
            {
                m_Test.wStep = 0;
                StopTest();
                return;
            }
            // 进入步骤2
            m_dbTestBeginTime[1] = GetCpuMs();
            m_dbStepNeedTime = ((double) (m_Test.wTestSecond - 1)) * 1000.0 / 2;
            m_dbSendElapse = m_Test.Stat[0].dbResponse * 1.5;
            m_Test.wStep = 2;
            iStepIndex = 1; // 从新设置步骤索引号
        }
        else if (2 == m_Test.wStep)
        {
            // 进入步骤3
            m_dbTestBeginTime[2] = GetCpuMs();
            m_dbStepNeedTime = ((double) (m_Test.wTestSecond - 1)) * 1000.0 / 2;
            m_dbSendElapse = min(m_Test.Stat[0].dbResponse, m_Test.Stat[1].dbResponse) * 0.3;
            m_Test.wStep = 3;
            iStepIndex = 2; // 从新设置步骤索引号
        }
        else if (3 == m_Test.wStep)
        {
            // 结束测试
            m_Test.wStep = 0;
            StopTest();
            return;
        }
    }

    // 步骤1是每隔一定时间就发送一次，不管是否收到应答包
    // 步骤2和步骤3是收到应答包、或者超时后，才发送下一包
    BOOL bNeedSend = FALSE;
    if (1 == m_Test.wStep)
    {
        if ((GetCpuMs() - m_dbSendTick) >= m_dbSendElapse)
        {
            bNeedSend = TRUE;
        }
    }
    else if (2 == m_Test.wStep || 3 == m_Test.wStep)
    {
        if ((m_dwRecvAckSeq == m_dwSendPktSeq) || ((GetCpuMs() - m_dbSendTick) >= m_dbSendElapse))
        {
            bNeedSend = TRUE;
        }
    }

    // 发送
    if (bNeedSend)
    {
        m_dbSendTick = GetCpuMs();
        m_dwSendPktSeq++;

        // 填充头信息
        T_WVM_PACKET_HEADER *hdr = (T_WVM_PACKET_HEADER *) pTestBuffer;
        memset(hdr, ((BYTE)rand()), sizeof(T_WVM_PACKET_HEADER));
        hdr->dwStartCode = WVM_START_CODE;
        hdr->dwSize = sizeof(T_WVM_PACKET_HEADER);
        hdr->dwCmd = WVM_CMD_TEST_NET;
        hdr->dwSeq = m_dwSendPktSeq;
        hdr->dwSendingTick = timeGetTime();
        hdr->dwReplyContext = m_Test.wStep; // 以当前的步骤号为context，不能为0
        hdr->dwSrcId = m_dwLocalDeviceId;
        hdr->dwDestId = m_Test.dwDestDeviceId;
        hdr->dwDataSize = m_Test.Stat[iStepIndex].iPktSize - IP_UDP_SIZE - sizeof(T_WVM_PACKET_HEADER);
        hdr->dwEncrypt = 0;
        // 校验和
        hdr->dwDataByteSum = 0;
        for (int i = 0; i < (int) hdr->dwDataSize; i++)
        {
            hdr->dwDataByteSum += *(pTestBuffer + hdr->dwSize + i);
        }
        // 发送
        if (udp_send(m_sock, pTestBuffer, hdr->dwSize + hdr->dwDataSize, m_Test.dwDestIp, m_Test.wDestPort) > 0)
        {
            m_Test.Stat[iStepIndex].iPktSendCnt++;
        }
    }
}

// 处理接收到的网络测试应答包
void CNet::Test_OnRecvThread(T_WVM_REPLY *pr)
{
    if (!m_bTestMode)
    {
        return;
    }

    // 发送这个包时，测试处于哪个步骤
    const WORD wReplyStep = (WORD) pr->dwSrcContext;
    if (wReplyStep < 1 || wReplyStep > 3)
    {
        return;
    }
    const int iStepIndex = ((int) wReplyStep) - 1;

    const double dbResponse = timeGetTime() - pr->dwSrcTick;
    // 有的包会在网络上延迟非常久，可能会是平均值的5倍。忽略这种情况，不计入平均值
    if (dbResponse < m_Test.Stat[iStepIndex].dbResponse * 5.0 || m_Test.Stat[iStepIndex].iPktAckCnt <= 0)
    {
        m_Test.Stat[iStepIndex].dbResponse =
            ((m_Test.Stat[iStepIndex].iPktAckCnt * m_Test.Stat[iStepIndex].dbResponse) + dbResponse) /
            (m_Test.Stat[iStepIndex].iPktAckCnt + 1);
    }

    m_Test.Stat[iStepIndex].iPktAckCnt++;

    m_dwRecvAckSeq = pr->dwSrcSeq;
}
