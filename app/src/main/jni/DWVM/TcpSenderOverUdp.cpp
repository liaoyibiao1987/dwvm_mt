#include "DwvmBase.h"
#include "Net.h"

extern CNet *FindNetFromSocket(SOCKET s);

CXSimpleList g_listTcpTunnelOverUdp;

CTcpSenderOverUdp::CTcpSenderOverUdp() :
    m_AckEvent(true) // 创建同步事件
{
    int i = 0;

    m_sock = INVALID_SOCKET;
    m_dwDestDeviceId = 0;
    m_dwDestIp = 0;
    m_wDestPort = 0;
    m_dwSendingPktSeq = 0;
    m_dwLastPktTimeMs = 0;
    m_iLastPktResendNumber = 0;
    memset(m_LastPktData, 0, sizeof(m_LastPktData));
    m_iLastPktDataSize = 0;
    m_dwLastPktCmd = 0;
    m_dwLastPktEncrypt = 0;
    m_pContextBuffer = NULL;
    m_iContextSize = 0;

    m_dwAvgAckTimeMs = 500;
    m_dwMaxAckTimeMs = 0;
    m_iAckPktNumber = 0;
    for (i = 0; i < STAT_ACK_PKT_NUM; i++)
    {
        m_dwStatAckTime[i] = m_dwAvgAckTimeMs;
    }

    // 如果全局列表没有初始化，初始化一次
    if (!g_listTcpTunnelOverUdp.IsInited())
    {
        g_listTcpTunnelOverUdp.Init(MAX_TCP_TUNNEL_OVER_UDP);
    }
}

CTcpSenderOverUdp::~CTcpSenderOverUdp()
{
    // 从全局列表删除
    g_listTcpTunnelOverUdp.Remove((DWORD) this);

    // 如果还在发送中，等待一下下
    if (IsSending())
    {
        m_AckEvent.Set();
        for (int i = 0; i < 10 && IsSending(); i++)
        {
            MY_SLEEP(1);
        }
    }

    // 删除用户私有数据
    if (m_pContextBuffer)
    {
        try
        {
            free(m_pContextBuffer);
        }
        catch (...)
        {
        }
        m_pContextBuffer = NULL;
    }
    m_iContextSize = 0;
}

BOOL CTcpSenderOverUdp::Setup(SOCKET s, DWORD dwDestDeviceId, DWORD dwDestIp, WORD wDestPort, int iContextSize)
{
    // 无效参数 (注意，dwDestDeviceId允许为0)
    if (INVALID_SOCKET == s || 0 == dwDestIp || 0 == wDestPort)
    {
        return FALSE;
    }

    // 如果尚未添加到全局列表，添加一次
    if (g_listTcpTunnelOverUdp.Found((DWORD) this) < 0)
    {
        if (g_listTcpTunnelOverUdp.Add((DWORD) this, this) < 0)
        {
            return FALSE;
        }
    }

    // 如果还在发送中，等待一下(大约10~20ms)
    if (IsSending())
    {
        m_AckEvent.Set();
        for (int i = 0; i < 10 && IsSending(); i++)
        {
            MY_SLEEP(1);
        }
    }

    // 目标IP改变，应答统计时间也要重新初始化一次
    //if(m_dwDestIp != dwDestIp)
    {
        // 先设一个初始值(LAN:0.05s, WAN:0.5s)，后面会根据应答包来调整
        m_dwAvgAckTimeMs = ip_islan(dwDestIp) ? 50 : 500;
        m_iAckPktNumber = 0;
        for (int i = 0; i < STAT_ACK_PKT_NUM; i++)
        {
            m_dwStatAckTime[i] = m_dwAvgAckTimeMs;
        }
    }

    // 分配用户私有空间
    if (iContextSize > 0)
    {
        if (iContextSize != m_iContextSize)
        {
            if (m_pContextBuffer)
            {
                free(m_pContextBuffer);
                m_pContextBuffer = NULL;
            }
            m_iContextSize = iContextSize;
        }
        if (NULL == m_pContextBuffer)
        {
            m_pContextBuffer = malloc((size_t) iContextSize);
            if (NULL == m_pContextBuffer)
            {
                m_iContextSize = 0;
                //return FALSE;
            }
        }
    }
    else
    {
        if (m_pContextBuffer)
        {
            free(m_pContextBuffer);
            m_pContextBuffer = NULL;
        }
        m_iContextSize = 0;
    }

    m_sock = s;
    m_dwDestDeviceId = dwDestDeviceId;
    m_dwDestIp = dwDestIp;
    m_wDestPort = wDestPort;
    m_dwSendingPktSeq = 0;

    return TRUE;
}

// 阻塞发送。等待包到达后、或者超时后才返回
BOOL CTcpSenderOverUdp::SendAndWait(
    DWORD dwCmd,        // 网络包的类型: WVM_CMD_xxx
    void *pData,        // 网络包buffer
    int iDataSize,        // 网络包长度
    DWORD dwEncrypt)    // 加密模式，0为不加密。最高4bit表示加密模式，0表示无加密。其他28bit为加密因子
{
    // 发送
    if (!AsyncSend(dwCmd, pData, iDataSize, dwEncrypt))
    {
        return FALSE;
    }

    // 等待应答，期间可能会多次重发
    const DWORD dwAckMs = m_dwAvgAckTimeMs * 3 / 2; // max( m_dwAvgAckTimeMs*3/2, 20 );
    BOOL bRet = FALSE;
    while (1)
    {
        const int iCheck = AsyncCheckAndResend(dwAckMs);
        if (1 == iCheck)
        {
            bRet = TRUE;
            break;
        }
        else if (iCheck < 0)
        {
            bRet = FALSE;
            break;
        }
    }

    // 返回结果
    if (!bRet)
    {
        xlog(XLOG_LEVEL_NORMAL,
             "[%08X] CTcpSenderOverUdp::SendAndWait: FAIL! (avg=%ums, timeout=%ums, seq=%u, resend=%d, dest_ip=%s:%u, dest_id=0x%08X)\n",
             this, m_dwAvgAckTimeMs, dwAckMs, m_dwSendingPktSeq, m_iLastPktResendNumber, socket_getstring(m_dwDestIp),
             m_wDestPort, m_dwDestDeviceId);
    }
    AsyncCleanSend();
    return bRet;
}

// 异步发送：发送和等待分开
BOOL CTcpSenderOverUdp::AsyncSend(
    DWORD dwCmd,        // 网络包的类型: WVM_CMD_xxx
    void *pData,        // 网络包buffer
    int iDataSize,        // 网络包长度
    DWORD dwEncrypt)    // 加密模式，0为不加密。最高4bit表示加密模式，0表示无加密。其他28bit为加密因子
{
    if (NULL == pData || iDataSize <= 0 || iDataSize > sizeof(m_LastPktData))
    {
        return FALSE;
    }
    if (!IsValid())
    {
        return FALSE;
    }

    // 正在发送中？
    if (IsSending())
    {
        return FALSE;
    }

    CNet *pNet = FindNetFromSocket(m_sock);
    if (NULL == pNet)
    {
        return FALSE;
    }

    // 自动判断超时时间：以平均响应时间的6倍为超时时间
    // 同时考虑到线程的切换本身需要花1~20个毫秒
    const DWORD dwAckMs = m_dwAvgAckTimeMs * 3 / 2; // max( m_dwAvgAckTimeMs*3/2, 20 );
#if 0
    if(0 == dwWaitMs)
    {
        dwWaitMs = dwAckMs * 6;
    }
    if(dwWaitMs < 20)
    {
        dwWaitMs = 20;
    }
    //if(dwWaitMs < m_dwMaxAckTimeMs)
    //{
    //	dwWaitMs = m_dwMaxAckTimeMs + 20;
    //}
    // 不允许超过6秒
    if(dwWaitMs > 6000)
    {
        dwWaitMs = 6000;
    }
#endif

    // 清除事件
    m_AckEvent.Reset();

    m_iLastPktResendNumber = 0;
    m_dwLastPktTimeMs = timeGetTime();
    memcpy(m_LastPktData, pData, (size_t) iDataSize);
    m_iLastPktDataSize = iDataSize;
    m_dwLastPktCmd = dwCmd;
    m_dwLastPktEncrypt = dwEncrypt;

    // 发送一下，获取新的包序号 (注意Send函数的最后一个参数,为0表示生成新序号,否则使用指定的序号)
    m_dwSendingPktSeq = pNet->Send(TRUE, pNet->GetLocalDeviceId(), m_dwLastPktCmd, (DWORD) this, m_dwDestDeviceId,
                                   m_dwDestIp, m_wDestPort, m_LastPktData, m_iLastPktDataSize, m_dwLastPktEncrypt, 0);
    if (0 == m_dwSendingPktSeq)
    {
        return FALSE;
    }

    return TRUE;
}

// 返回值：0 正在发送中, 1 发送完成, -1 超时, -2 发送失败
int CTcpSenderOverUdp::AsyncCheckAndResend(DWORD dwWaitMs)
{
    CNet *pNet = FindNetFromSocket(m_sock);
    if (NULL == pNet)
    {
        return -2;
    }

    if (!IsSending() || m_iLastPktDataSize <= 0)
    {
        return 1;
    }

    // 等待应答
    if (m_AckEvent.TryWait((long) (dwWaitMs & 0x7FFFFFFL)))
    {
        m_dwSendingPktSeq = 0;
        m_iLastPktDataSize = 0;
        return 1;
    }

    // 这个包被多次发送，或者超时了
    if (m_iLastPktResendNumber > 12)
    {
        return -1;
    }

    // 如果在1.5倍的应答时间内，没有收到应答的话，从新发送一次
    const DWORD dwAckMs = m_dwAvgAckTimeMs * 3 / 2; // max( m_dwAvgAckTimeMs*3/2, 20 );
    const DWORD dwCurrTimeMs = timeGetTime();
    if ((dwCurrTimeMs - m_dwLastPktTimeMs) > dwAckMs)
    {
        // 以之前的包序号重新发送一次
        m_dwLastPktTimeMs = dwCurrTimeMs;
        if (0 == pNet->Send(TRUE, pNet->GetLocalDeviceId(), m_dwLastPktCmd, (DWORD) this, m_dwDestDeviceId, m_dwDestIp,
                            m_wDestPort, m_LastPktData, m_iLastPktDataSize, m_dwLastPktEncrypt, m_dwSendingPktSeq))
        {
            return -2;
        }
        // 记录
        m_iLastPktResendNumber++;
        if (m_iLastPktResendNumber > 2)
        {
            xlog(XLOG_LEVEL_NORMAL, "[%08X] CTcpSenderOverUdp: Resend (cnt=%2d, avg=%3ums, timeout=%3ums, seq=%u)\n",
                 this, m_iLastPktResendNumber, m_dwAvgAckTimeMs, dwAckMs, m_dwSendingPktSeq);
        }
    }
    return 0;
}

void CTcpSenderOverUdp::AsyncCleanSend()
{
    m_dwSendingPktSeq = 0;
    m_iLastPktDataSize = 0;
}

// 处理应答包
BOOL CTcpSenderOverUdp::CheckAck(T_WVM_REPLY *pAck)
{
    BOOL bResult = FALSE;
    if (pAck && pAck->dwSrcContext == (DWORD) this)
    {
        // SendAndWait()还在等待吗？如果是，激活事件
        if (IsSending() && pAck->dwSrcSeq == m_dwSendingPktSeq)
        {
            m_AckEvent.Set();
            bResult = TRUE;
        }

        // 更新应答时间
        DWORD dwCurrMs = timeGetTime();
        if (dwCurrMs >= pAck->dwSrcTick)
        {
            const DWORD dwAckTime = dwCurrMs - pAck->dwSrcTick;
            if (dwAckTime > m_dwMaxAckTimeMs)
            {
                m_dwMaxAckTimeMs = dwAckTime;
                xlog(XLOG_LEVEL_NORMAL, "[%08X] CTcpSenderOverUdp max ack time = %lu ms\n", this, m_dwMaxAckTimeMs);
            }
            m_dwStatAckTime[m_iAckPktNumber % STAT_ACK_PKT_NUM] = dwAckTime;
            m_iAckPktNumber++;

            DWORD dwSum = 0;
            const int iCount = min(m_iAckPktNumber, STAT_ACK_PKT_NUM);
            int i = 0;
            for (i = 0; i < iCount; i++)
            {
                dwSum += m_dwStatAckTime[i];
            }
            m_dwAvgAckTimeMs = dwSum / iCount;
            if (m_dwAvgAckTimeMs < 2)
            {
                m_dwAvgAckTimeMs = 2;
            }
            //xlog(XLOG_LEVEL_NORMAL, "[%08X] CTcpSenderOverUdp avg ack time = %lu ms\n", this, m_dwAvgAckTimeMs);
        }
    }
    return bResult;
}
