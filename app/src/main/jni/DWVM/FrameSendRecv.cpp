#include "DwvmBase.h"
#include <time.h>
#include "svStreamDef2.h"

//
// 缓存视频、音频小包
// 可以指定缓存的时间
//
CTimingCache::CTimingCache(int iCacheSecond) : // 指定缓存多少秒钟的数据
    m_idle(),
    m_data()
{
    if (iCacheSecond <= 0)
    {
        m_iCacheSecond = 5;
    }
    else if (iCacheSecond > 60)
    {
        m_iCacheSecond = 60;
    }
    else
    {
        m_iCacheSecond = iCacheSecond;
    }

    m_idle.Init(0, WVM_MTU);
    m_data.Init(0, WVM_MTU);
}

CTimingCache::~CTimingCache()
{
    m_idle.UnInit();
    m_data.UnInit();
}

// 把数据添加到缓存中
BOOL CTimingCache::Add(
    void *pData,    // 格式为 T_WVM_VA_FRAME_HEADER + T_WVM_VA_BLOCK_HEADER + frame_data
    int iSize)    // 数据长度
{
    if (m_iCacheSecond <= 0)
    {
        return FALSE;
    }
    if (NULL == pData || iSize <= 0 || iSize > WVM_MTU)
    {
        return FALSE;
    }

    // 把超过时间的小包，从 m_data 移动到 m_idle
	const time_t tCurrTime = time(NULL);
	CXSimpleChain::_node* pNode = NULL;
	while((pNode = m_data.PeekHead()) != NULL &&
          (int)(tCurrTime - pNode->timestamp) > m_iCacheSecond)
	{
		m_idle.AddToTail( m_data.RemoveFromHead() );
	}

    // 从 m_idle 获取一个空白buffer，如果失败，再从新分配一个
    pNode = m_idle.RemoveFromHead();
    if (NULL == pNode)
    {
        pNode = CXSimpleChain::AllocNode(WVM_MTU);
        if (NULL == pNode)
        {
            return FALSE;
        }
    }

    // 复制数据，再保存到 m_data
    if ((int) pNode->sizeAlloc < iSize || NULL == pNode->data)
    {
        CXSimpleChain::FreeNode(pNode);
        pNode = NULL;
        return FALSE;
    }
    memcpy(pNode->data, pData, (size_t) iSize);
    pNode->sizeActual = (ULONG) iSize;
    m_data.AddToTail(pNode);

    return TRUE;
}

// 从缓存中读取一个指定序号的小包
int CTimingCache::Get(    // 返回值：0表示读取失败，>0 表示读取到的数据长度
    DWORD pkt_seq,        // 希望读取的包序号。即命令码 WVM_CMD_PS_VA_RESEND 对应的 T_WVM_VA_BLOCK_HEADER::pkt_seq
    void *pData)        // OUT 接受数据的buffer，尺寸必须大于或者等于 WVM_MTU
{
    if (pData)
    {
        T_WVM_VA_BLOCK_HEADER *pBlockHdr = NULL;
        CXSimpleChain::_node *pNode = m_data.PeekHead();
        while (pNode)
        {
            pBlockHdr = (T_WVM_VA_BLOCK_HEADER *) (((char *) pNode->data) + sizeof(T_WVM_VA_FRAME_HEADER));
            if (pNode->data && pkt_seq == pBlockHdr->pkt_seq)
            {
                memcpy(pData, pNode->data, (size_t)pNode->sizeActual);
                return (int) pNode->sizeActual;
            }
            pNode = m_data.PeekNext(pNode);
        }
    }
    return 0;
}

// 清除所有缓存数据
// 如果iCacheSecond不等于0，还会从新设定缓存秒数
BOOL CTimingCache::Reset(int iCacheSecond)
{
    if (iCacheSecond > 0 && iCacheSecond <= 60 && iCacheSecond != m_iCacheSecond)
    {
        m_iCacheSecond = iCacheSecond;
    }

#if 0
    // 把data链表的数据放入idel链表
    CXSimpleChain::_node* pNode = NULL;
    while((pNode = m_data.RemoveFromHead()) != NULL)
    {
        m_idle.AddToTail(pNode);
    }
#else
    // 释放所有缓存数据
    CXSimpleChain::_node *pNode = NULL;
    while ((pNode = m_data.RemoveFromHead()) != NULL)
    {
        m_data.FreeNode(pNode);
    }
    while ((pNode = m_idle.RemoveFromHead()) != NULL)
    {
        m_idle.FreeNode(pNode);
    }
#endif

    return TRUE;
}

//
// 统计
//
int CTimingCache::Stat(T_WVM_VA_REPLY *pStat)
{
    // IN:
    //	pStat->Polling.dwPktSeqBegin
    //	pStat->Polling.dwPktSeqEnd
    // OUT:
    //	pStat->dwSendBytes_0
    //	pStat->dwSendPackets_0
    //	pStat->dwSendBytes_1
    //	pStat->dwSendPackets_1
    int iPktNumber = 0;
    if (pStat)
    {
        pStat->dwSendBytes_0 = 0;
        pStat->dwSendPackets_0 = 0;
        pStat->dwSendBytes_1 = 0;
        pStat->dwSendPackets_1 = 0;

        T_WVM_VA_BLOCK_HEADER *pBlockHdr = NULL;
        CXSimpleChain::_node *pNode = m_data.PeekHead();
        while (pNode)
        {
            if (NULL != pNode->data)
            {
                pBlockHdr = (T_WVM_VA_BLOCK_HEADER *) (((char *) pNode->data) + sizeof(T_WVM_VA_FRAME_HEADER));
                if (pBlockHdr->pkt_seq < pStat->Polling.dwPktSeqBegin)
                {
                	;
                }
                else if (pBlockHdr->pkt_seq > pStat->Polling.dwPktSeqEnd)
                {
                    break;
                }
                else
                {
                    pStat->dwSendPackets_0++;
                    pStat->dwSendBytes_0 += sizeof(T_WVM_PACKET_HEADER) + pNode->sizeActual;
                    if (0 != pBlockHdr->resend_cnt)
                    {
                        pStat->dwSendPackets_1 += pBlockHdr->resend_cnt;
                        pStat->dwSendBytes_1 += pBlockHdr->resend_cnt * (sizeof(T_WVM_PACKET_HEADER) + pNode->sizeActual);
                    }
                }
            }
            pNode = m_data.PeekNext(pNode);
        }
    }
    return iPktNumber;
}

//
// CFrameSender:
// 视频、音频帧的发送类
// 把一帧拆分为多个小包发送，并缓存(以备网络丢包后、接收方要求重发)
// 每个类仅仅处理一路数据流
//

CFrameSender::CFrameSender(int iCacheSecond) :
    m_cache(iCacheSecond)
{
    m_hLock = CXAutoLock::Create();

    m_iCacheSecond = iCacheSecond;
	m_dwSenderDeviceId = 0;
    m_dwFrameCount = 0;
    m_dwBlockCount = 0;
    m_dwLastFrameTime = timeGetTime();

	// 控制重发包的频率
	m_dwResendLimit_StartTick = timeGetTime();
	m_dwResendLimit_AvgSendPackets = 128;
	m_dwResendLimit_SendPackets = 0;
	m_dwResnedLimit_ResendPackets = 0;

	// 统计信息
	m_dwStatStartTick = 0;
	m_dwStatMaxFrameSize = 0;
	m_dwStatFrames = 0;
	m_dwStatBytes0 = 0;
	m_dwStatBytes1 = 0;
	m_dwStatPackets0 = 0;
	m_dwStatPackets1 = 0;
	m_dwStatErrorCount0 = 0;
	m_dwStatErrorCount1 = 0;
}

CFrameSender::~CFrameSender()
{
    CXAutoLock::Destroy(m_hLock);
}

// 发送一帧。
// 函数内部会拆分为多个小包发送，小包的命令码为 WVM_CMD_PS_VA_FRAME
// 结构为：T_WVM_PACKET_HEADER + T_WVM_VA_FRAME_HEADER + T_WVM_VA_BLOCK_HEADER + frame_data
BOOL CFrameSender::Send(
    SOCKET s,
    DWORD dwNetEncryptMode,
    DWORD dwSrcDeviceId,
    DWORD dwSrcEncoderChannelIndex,
    DWORD dwSrcImageResolution,        // 图像分辨率：WVM_IMAGERES_CIF, WVM_IMAGERES_D1, WVM_IMAGERES_QCIF
    BOOL bVideoOnlySendKeyFrame,
    DWORD dwDestDeviceId,
    DWORD dwDestDecoderChannelIndex,    // 如果dwDestDeviceId是指向PS，这个参数被忽略
    DWORD dwDestIp,
    WORD wDestPort,
    void *pFrameBuffer)                // tagSVStreamHeader2为头的frame_data
{
    CXAutoLock lk(m_hLock);

    if (NULL == pFrameBuffer)
    {
        xlog(XLOG_LEVEL_ERROR, "CFrameSender::Send(): FrameBuffer is NULL");
        return FALSE;
    }

    const tagSVStreamHeader2 *pStreamHdr = (tagSVStreamHeader2 *) pFrameBuffer;
    if (SVSTREAM_STARTCODE != pStreamHdr->Code)
    {
        xlog(XLOG_LEVEL_ERROR, "CFrameSender::Send(): header StartCode error: %08X", pStreamHdr->Code);
        return FALSE;
    }
    const int iFrameLen = pStreamHdr->HeadSize + pStreamHdr->BufferSize;

    DWORD dwFrameType = 0;
    switch (pStreamHdr->StreamType)
    {
    case VFT_PFRAME:
        dwFrameType = WVM_FRAMETYPE_VIDEO_P;
        break;
    case VFT_IFRAME:
        dwFrameType = WVM_FRAMETYPE_VIDEO_I;
        break;
    case VFT_AUDIO :
        dwFrameType = WVM_FRAMETYPE_AUDIO;
        break;
    default:
        xlog(XLOG_LEVEL_ERROR, "CFrameSender::Send(): StreamType error: %d", pStreamHdr->StreamType);
        return FALSE;
    }

    // JPEG和Audio全是关键帧
    if (!bVideoOnlySendKeyFrame)
    {
        if ((VFT_IFRAME == pStreamHdr->StreamType || VFT_PFRAME == pStreamHdr->StreamType) &&
            VCODEC_JPEG == pStreamHdr->CodecType)
        {
            bVideoOnlySendKeyFrame = TRUE;
        }
        else if (VFT_AUDIO == pStreamHdr->StreamType)
        {
            bVideoOnlySendKeyFrame = TRUE;
        }
    }

    // 仅仅发送关键帧
    if (bVideoOnlySendKeyFrame && VFT_PFRAME == pStreamHdr->StreamType)
    {
        xlog(XLOG_LEVEL_ERROR, "CFrameSender::Send(): ignore non-key-frame");
        return FALSE;
    }

    // 当UDP的目标IP地址不在目前的arp活动表中，并且udp包大于1024时，每发一个UDP包都会导致OS等待arp回应、直到超时（4秒）
    // 但是如果udp包小于或者等于1024时，没有这个问题，udp包会被立即发送
    // 所以这里把最大长度限定在1024
    static const int iMaxLen = min(1024, WVM_MTU) - sizeof(T_WVM_PACKET_HEADER) - sizeof(T_WVM_VA_FRAME_HEADER) -
                               sizeof(T_WVM_VA_BLOCK_HEADER);
    BYTE Buffer[WVM_MTU];
    T_WVM_VA_FRAME_HEADER *pFrameHdr = (T_WVM_VA_FRAME_HEADER *) Buffer;
    T_WVM_VA_BLOCK_HEADER *pBlockHdr = (T_WVM_VA_BLOCK_HEADER *) &pFrameHdr[1];
    BYTE *pBlockData = (BYTE *) &pBlockHdr[1];
    const int iPktNum = (iFrameLen + iMaxLen - 1) / iMaxLen;
    if (iPktNum > 65535)
    {
        xlog(XLOG_LEVEL_ERROR, "CFrameSender::Send(): Frame size too large! len=%d, pkt_num=%d", iFrameLen, iPktNum);
        return FALSE;
    }

	m_dwStatFrames ++;
	if((DWORD)iFrameLen > m_dwStatMaxFrameSize)
	{
		m_dwStatMaxFrameSize = (DWORD)iFrameLen;
	}

	m_dwSenderDeviceId = dwSrcDeviceId;

    memset(Buffer, 0, sizeof(T_WVM_VA_FRAME_HEADER) + sizeof(T_WVM_VA_BLOCK_HEADER));

    pFrameHdr->Session.dwSrcDeviceEncoderChannelIndex = dwSrcEncoderChannelIndex;
    pFrameHdr->Session.dwDestDeviceDecoderChannelIndex = dwDestDecoderChannelIndex;
    pFrameHdr->Session.dwImageResolution = dwSrcImageResolution;
    pFrameHdr->Session.dwFrameType = dwFrameType;
    pFrameHdr->bVideoOnlySendKeyFrame = bVideoOnlySendKeyFrame;

    pBlockHdr->time_stamp = timeGetTime();
    pBlockHdr->frm_seq = ++m_dwFrameCount;
    pBlockHdr->frm_pkt_num = (WORD) iPktNum;
    pBlockHdr->is_independent = (BYTE) ((WVM_FRAMETYPE_VIDEO_P == dwFrameType) ? 0 : 1);
    pBlockHdr->resend_cnt = 0;

    BYTE *pSrc = (BYTE *) pFrameBuffer;
    int iRemain = iFrameLen;
    for (int i = 0; i < iPktNum; i++)
    {
        const int iBlockLen = (iRemain > iMaxLen) ? (iMaxLen) : (iRemain);
        const int iSendLen = sizeof(T_WVM_VA_FRAME_HEADER) + sizeof(T_WVM_VA_BLOCK_HEADER) + iBlockLen;

        pFrameHdr->dwFrameSize = sizeof(T_WVM_VA_BLOCK_HEADER) + iBlockLen;
        pBlockHdr->pkt_seq = ++m_dwBlockCount;
        pBlockHdr->frm_pkt_seq = (WORD) i;

        memcpy(pBlockData, pSrc, (size_t) iBlockLen);

        // 缓存
        if (m_iCacheSecond > 0)
        {
            m_cache.Add(Buffer, iSendLen);
        }

		// 随机丢包，测试重发效果
		const BOOL bTestLost = FALSE;//(0 == (((DWORD)rand()) % 20));

		// 发送
		if(bTestLost)
		{
			xlog(XLOG_LEVEL_NORMAL, "### [SENDER_%p] TEST LOST seq = %u (in frame #%u pkt %u/%u)\n", this, pBlockHdr->pkt_seq, pBlockHdr->frm_seq, pBlockHdr->frm_pkt_seq, pBlockHdr->frm_pkt_num);
		}
		else
		{
			if(0 == DWVM_SendNetPacketEx(TRUE, s, WVM_CMD_PS_VA_FRAME, 0, dwDestDeviceId, dwDestIp, wDestPort, Buffer, iSendLen, dwNetEncryptMode, m_dwSenderDeviceId))
			{
				m_dwStatErrorCount0 ++;
			}
		}

        pSrc += iBlockLen;
        iRemain -= iBlockLen;
			
		m_dwStatBytes0 += iBlockLen;
		m_dwStatPackets0 ++;

		m_dwResendLimit_SendPackets ++;
	}

	// 控制重发包的频率
	const DWORD dwCurrentTick = timeGetTime();
	if((dwCurrentTick - m_dwResendLimit_StartTick) >= 1000)
	{
		m_dwResendLimit_StartTick = dwCurrentTick;
		m_dwResendLimit_AvgSendPackets = m_dwResendLimit_SendPackets;
		m_dwResendLimit_SendPackets = 0;
		m_dwResnedLimit_ResendPackets = 0;
    }

    m_dwLastFrameTime = timeGetTime();

    return TRUE;
}

// 处理接收方的重发请求（即命令 WVM_CMD_PS_VA_RESEND）
BOOL CFrameSender::Resend(
    SOCKET s,
    DWORD  dwNetEncryptMode,
    DWORD  dwToDeviceId,
    DWORD  dwToIp,
    WORD   wToPort,
    T_WVM_VA_RESEND *pResend)
{
    CXAutoLock lk(m_hLock);

	if(m_iCacheSecond <= 0 || 0 == m_dwSenderDeviceId)
    {
        return FALSE;
    }
    if (NULL == pResend || pResend->dwPacketNum == 0)
    {
        return FALSE;
    }
	const DWORD dwCurrTime = timeGetTime();

	// 最多允许重发多少个包：控制在正常包的 1/10
	const DWORD dwMaxResendPercent = 10;
	const DWORD dwAvgPacktes = max(m_dwResendLimit_AvgSendPackets, 128);
	const DWORD dwMaxResendPackets = dwAvgPacktes * dwMaxResendPercent / 100;

	// Debug：打印重发信息
	//char szLog[1024] = {""};
	//char* pszLog = szLog;
	//if(1)
	//{
	//	pszLog += sprintf(pszLog, "### [SENDER_%p] recv resend-query, num=%u, seq=", this, pResend->dwPacketNum);
	//	const int iShowCnt = (pResend->dwPacketNum < 8) ? (pResend->dwPacketNum) : (8);
	//	for(int i=0; i<iShowCnt; i++)
	//	{
	//		char pPacket[1600];
	//		const int iLen = m_cache.Get(pResend->dwPacketSeqArray[i], pPacket);
	//		if(iLen > 0)
	//		{
	//			T_WVM_VA_FRAME_HEADER* pFrameHdr = (T_WVM_VA_FRAME_HEADER*) pPacket;
	//			T_WVM_VA_BLOCK_HEADER* pBlockHdr = (T_WVM_VA_BLOCK_HEADER*) &pFrameHdr[1];
	//			pszLog += sprintf(pszLog, " [%u] %u ms NO.%u", pResend->dwPacketSeqArray[i], dwCurrTime - pBlockHdr->time_stamp, pBlockHdr->resend_cnt+1);
	//		}
	//	}
	//	pszLog += sprintf(pszLog, "\n### [SENDER_%p] resend to decoder_%08X, ip %s:%u, limit=%u/%u, seq=", this, dwToDeviceId, socket_getstring(dwToIp), wToPort, m_dwResnedLimit_ResendPackets, dwMaxResendPackets);
	//}

	char aPacket[1600];
	const int iCnt = (pResend->dwPacketNum < WVM_MAX_RESEND_PKT_NUM) ? (pResend->dwPacketNum) : (WVM_MAX_RESEND_PKT_NUM);
	for(int i=0; i<iCnt && m_dwResnedLimit_ResendPackets < dwMaxResendPackets; i++)
	{
		const int iLen = m_cache.Get(pResend->dwPacketSeqArray[i], aPacket);
		if(iLen > 0)
        {
			T_WVM_VA_FRAME_HEADER* pFrameHdr = (T_WVM_VA_FRAME_HEADER*) aPacket;
			T_WVM_VA_BLOCK_HEADER* pBlockHdr = (T_WVM_VA_BLOCK_HEADER*) &pFrameHdr[1];

			// 限制单个包的重发次数
			if(pBlockHdr->resend_cnt >= 3)
            {
				continue;
			}
            // 记录被重发的次数
            pBlockHdr->resend_cnt++;

			// 重发包的生命周期
			if((dwCurrTime - pBlockHdr->time_stamp) > 3000)//3000,1200
			{
				continue;
			}

            // 发送
			if(0 == DWVM_SendNetPacketEx(TRUE, s, WVM_CMD_PS_VA_FRAME, 0, dwToDeviceId, dwToIp, wToPort, aPacket, iLen, dwNetEncryptMode, m_dwSenderDeviceId))
			{
				m_dwStatErrorCount0 ++;
			}
			else
			{
				//pszLog += sprintf(pszLog, " [%u]", pResend->dwPacketSeqArray[i]);
			}

			m_dwStatBytes1 += iLen;
			m_dwStatPackets1 ++;

			m_dwResnedLimit_ResendPackets ++;
		}
		else
		{
			m_dwStatErrorCount1 ++;
        }
    }

	//xlog(XLOG_LEVEL_NORMAL,"%s",szLog);

    return TRUE;
}

// 处理接收方的统计请求（即命令 WVM_CMD_PS_VA_POLLING）
BOOL CFrameSender::Reply(
    SOCKET s,
    DWORD dwNetEncryptMode,
    DWORD dwSrcDeviceId,
    DWORD dwSrcIp,
    WORD wSrcPort,
		DWORD  dwDestDeviceId,
    T_WVM_VA_POLLING *pPolling)
{
    CXAutoLock lk(m_hLock);

    if (NULL == pPolling)
    {
        return FALSE;
    }
    //if(m_iCacheSecond <= 0)
    //{
    //	return FALSE;
    //}

    T_WVM_VA_REPLY r;
    memset(&r, 0, sizeof(r));
    r.Polling = *pPolling; // 直接复制回去
    m_cache.Stat(&r); // 统计

    // 发送
	DWVM_SendNetPacketEx(TRUE, s, WVM_CMD_PS_VA_REPLY, 0, dwSrcDeviceId, dwSrcIp, wSrcPort, &r, sizeof(r), dwNetEncryptMode, dwDestDeviceId);

    return TRUE;
}

// 获取统计信息
BOOL CFrameSender::GetStatText(char* szText)
{
	if(NULL == szText)
	{
		return FALSE;
	}
	const DWORD dwCurrTick = GetTickCount();
	if(0 == m_dwStatStartTick || m_dwStatStartTick >= dwCurrTick)
	{
		m_dwStatStartTick = dwCurrTick;
		return FALSE;
	}
	const double dbSeconds = (dwCurrTick - m_dwStatStartTick) / 1000.0;
	sprintf(szText, "%.1f sec, %.1f fps, max frame %u, send %.2f kbps %.1f pps %u fail, resend %.2f kbps %.1f pps %u fail", 
		dbSeconds, m_dwStatFrames / dbSeconds, m_dwStatMaxFrameSize,
		(m_dwStatBytes0 * 8.0 / 1024.0) / dbSeconds, m_dwStatPackets0/dbSeconds, m_dwStatErrorCount0,
		(m_dwStatBytes1 * 8.0 / 1024.0) / dbSeconds, m_dwStatPackets1/dbSeconds, m_dwStatErrorCount1);

	// 还原
	m_dwStatStartTick = GetTickCount();
	m_dwStatMaxFrameSize = 0;
	m_dwStatFrames = 0;
	m_dwStatBytes0 = 0;
	m_dwStatBytes1 = 0;
	m_dwStatPackets0 = 0;
	m_dwStatPackets1 = 0;
	m_dwStatErrorCount0 = 0;
	m_dwStatErrorCount1 = 0;
	return TRUE;
}

//
// CFrameReceiver:
// 视频、音频帧的接收类
// 把多个小包组合成一个完整帧，并判断是否网络丢包、请求发送方重发
// 每个类仅仅处理一路数据流
//

CFrameReceiver::CFrameReceiver()
{
    m_pRestruct = NULL;
    m_hLock = CXAutoLock::Create();
    m_pFrameBuffer = NULL;
    m_iFrameBufferSize = 0;
    m_iFrameValidSize = 0;
    m_dwResendInterlaceMs = 0;
    m_dwResendPrevMs = 0;
    memset(&m_LastPushFrame, 0, sizeof(m_LastPushFrame));
    m_dwLastSrcID = 0;
    m_dwLastSrcIp = 0;
    m_wLastSrcPort = 0;
    m_dwLastPktTime = 0;
    m_dwLastFrameTime = timeGetTime();

    memset(&m_Polling, 0, sizeof(m_Polling));
    memset(&m_RealtimeStatus, 0, sizeof(m_RealtimeStatus));

	m_dwStatStartTick = 0;
	m_dwStatMaxFrameSize = 0;
	m_dwStatMaxResendCnt = 0;
	m_dwStatFrames = 0;
	m_dwStatBytes0 = 0;
	m_dwStatBytes1 = 0;
	m_dwStatPackets0 = 0;
	m_dwStatPackets1 = 0;
	m_dwStatResendQueryCnt = 0;
}

CFrameReceiver::~CFrameReceiver()
{
    m_iFrameBufferSize = 0;
    m_iFrameValidSize = 0;
    if (m_pRestruct)
    {
        try
        {
            m_pRestruct->Destroy();
            delete m_pRestruct;
        }
        catch (...)
        {
        }
        m_pRestruct = NULL;
    }
    if (m_pFrameBuffer)
    {
        delete[] m_pFrameBuffer;
        m_pFrameBuffer = NULL;
    }
    CXAutoLock::Destroy(m_hLock);
}

// 组包（即处理命令 WVM_CMD_PS_VA_FRAME）
// 函数内部会判断是否丢包，并会请求发送端重新发送
BOOL CFrameReceiver::Push(
    T_WVM_VA_FRAME_HEADER *pFrame,    // IN  收到的音频视频小包: T_WVM_VA_FRAME_HEADER + T_WVM_VA_BLOCK_HEADER + frame_data
    SOCKET s,                        // IN  socket
    DWORD dwSrcDeviceId,            // IN  发送方的ID
    DWORD dwSrcIp,                    // IN  发送方的ip
    WORD wSrcPort,                // IN  发送方的port
    DWORD dwDestDeviceId,            // IN  接收方的ID，即自己的ID(发送重发请求时要用到)
    int *pLostSeqCount)            // OUT 丢包数
{
    CXAutoLock lk(m_hLock);

    // 参数有效性检查
    if (NULL == pFrame)
    {
        return FALSE;
    }

    // 创建组包类
    if (NULL == m_pRestruct)
    {
        try
        {
            m_pRestruct = new CRestructPacket();
        }
        catch (...)
        {
            m_pRestruct = NULL;
        }
        if (NULL == m_pRestruct)
        {
            xlog(XLOG_LEVEL_ERROR, "Alloc CFrameReceiver::m_pRestruct FAIL!\n");
            return FALSE;
        }

        // 应该根据不同的网络情况赋值（网络延迟时间、丢包率、编码位率）
        // 这里假设是一般的公网情况：最多缓存640个包，最大延迟3秒
        const int iMaxPktNum = max(640, (WVM_MAX_VIDEO_FRAME_SIZE / 1024));
        if (!m_pRestruct->Create(iMaxPktNum, 3000, 0))//3000 or 1200
        {
            delete m_pRestruct;
            m_pRestruct = NULL;
            xlog(XLOG_LEVEL_ERROR, "CFrameReceiver::m_pRestruct create FAIL!\n");
            return FALSE;
        }
    }

	// 这个包是第几次被发送. 0为原发包，大于0为重复包
	const T_WVM_VA_BLOCK_HEADER* pBlockHdr = (T_WVM_VA_BLOCK_HEADER*) &pFrame[1];
	const BYTE cResendCnt = pBlockHdr->resend_cnt;
	// 如果重发次数太多，需要调整重发间隔时间
	if(cResendCnt >= 3)
	{
		xlog(XLOG_LEVEL_WARNING, "CFrameReceiver:: recv resend pkt: seq=%u, cnt=%u, resend_intl=%u ms\n", pBlockHdr->pkt_seq, cResendCnt, m_dwResendInterlaceMs);
	}
	// 统计信息
	if(0 == cResendCnt)
	{
		m_dwStatPackets0 ++;
		m_dwStatBytes0 += pFrame->dwFrameSize;
	}
	else
	{
		m_dwStatPackets1 ++;
		m_dwStatBytes1 += pFrame->dwFrameSize;
	}
	if(m_dwStatMaxResendCnt < (DWORD)cResendCnt)
	{
		m_dwStatMaxResendCnt = (DWORD)cResendCnt;
	}

    // 判断来源是否改变，如果改变，需要清除缓存
    if (pFrame->Session.dwSrcDeviceEncoderChannelIndex != m_LastPushFrame.Session.dwSrcDeviceEncoderChannelIndex ||
        pFrame->Session.dwImageResolution != m_LastPushFrame.Session.dwImageResolution ||
        m_dwLastSrcID != dwSrcDeviceId ||
        m_dwLastSrcIp != dwSrcIp ||
        m_wLastSrcPort != wSrcPort)
    {
        //xlog(XLOG_LEVEL_NORMAL, "CFrameReceiver::Push() clear cache: %08X_#%u_%u -> %08X_#%u_%u, %08X:%u -> %08X:%u\n",
        //	m_dwLastSrcID, pFrame->Session.dwSrcDeviceEncoderChannelIndex, pFrame->Session.dwImageResolution,
        //	dwSrcDeviceId, m_LastPushFrame.Session.dwSrcDeviceEncoderChannelIndex, m_LastPushFrame.Session.dwImageResolution,
        //	m_dwLastSrcIp, m_wLastSrcPort,
        //	dwSrcIp, wSrcPort);
        ClearCache();

		m_dwStatStartTick = 0;
		m_dwStatMaxFrameSize = 0;
		m_dwStatMaxResendCnt = 0;
		m_dwStatFrames = 0;
		m_dwStatBytes0 = 0;
		m_dwStatBytes1 = 0;
		m_dwStatPackets0 = 0;
		m_dwStatPackets1 = 0;
		m_dwStatResendQueryCnt = 0;
    }
    // save frame header
    memcpy(&m_LastPushFrame, pFrame, sizeof(m_LastPushFrame));
    m_dwLastSrcID = dwSrcDeviceId;
    m_dwLastSrcIp = dwSrcIp;
    m_wLastSrcPort = wSrcPort;
    m_dwLastPktTime = timeGetTime();

    // push
    const BOOL bResult = m_pRestruct->Push(&pFrame[1], pFrame->dwFrameSize);

    // 请求从新发送丢失的包
    if (pLostSeqCount)
    {
        *pLostSeqCount = 0;
    }
    if (0 == m_dwResendInterlaceMs)
    {
        // 默认为 LAN: 40 ms, WAN: 100 ms
        // 后面会根据实际的应答延迟时间，动态调整这个参数
        if (ip_islan(dwSrcIp))
        {
            m_dwResendInterlaceMs = 40;
        }
        else
        {
            m_dwResendInterlaceMs = 100;
        }
    }
    const DWORD dwNow = timeGetTime();
	if((dwNow - m_dwResendPrevMs) > (m_dwResendInterlaceMs+3)) // 多3个毫秒，防止边界时间的请求重发
    {
        m_dwResendPrevMs = dwNow;

        DWORD dwLostPackets[100];
        int iLostPackets = 0;
        const int iMaxNum = sizeof(dwLostPackets) / sizeof(dwLostPackets[0]);
        if (pFrame->bVideoOnlySendKeyFrame)
        {
            m_pRestruct->GetLostPackets(dwLostPackets, &iLostPackets, iMaxNum, NULL, m_dwResendInterlaceMs);
        }
        else
        {
            m_pRestruct->GetLostPackets_All(dwLostPackets, &iLostPackets, iMaxNum, NULL, m_dwResendInterlaceMs);
        }

        if (pLostSeqCount)
        {
            *pLostSeqCount = iLostPackets;
        }

		if(iLostPackets >= WVM_MAX_RESEND_PKT_NUM)
        {
			xlog(XLOG_LEVEL_NORMAL, "[%p] CFrameReceiver::Push() lost-pkt too many (%d) >= %d (MyId=%08X,SenderId=%08X)\n", this, iLostPackets, WVM_MAX_RESEND_PKT_NUM, dwDestDeviceId, dwSrcDeviceId);
        }
		if(iLostPackets > 0)
        {
            T_WVM_VA_RESEND r;
            memset(&r, 0, sizeof(r));
            r.Session = pFrame->Session;
            r.dwPacketNum = (DWORD) ((iLostPackets > WVM_MAX_RESEND_PKT_NUM) ? WVM_MAX_RESEND_PKT_NUM : iLostPackets);
            memcpy(r.dwPacketSeqArray, dwLostPackets, sizeof(DWORD) * r.dwPacketNum);
			DWVM_SendNetPacketEx(TRUE, s, WVM_CMD_PS_VA_RESEND, 0, dwSrcDeviceId, dwSrcIp, wSrcPort, &r, sizeof(r), 0, dwDestDeviceId);
			// 统计信息
			m_dwStatResendQueryCnt += r.dwPacketNum;
            //xlog(XLOG_LEVEL_NORMAL, "[%p] CFrameReceiver::Push() query resend-pkt num %d (intl %u ms)\n", this, iLostPackets, m_dwResendInterlaceMs);
        }
    }

    // 更新统计信息
    if (0 == cResendCnt)
    {
        m_Polling.dwRecvPackets_0++;
        m_Polling.dwRecvBytes_0 += sizeof(T_WVM_PACKET_HEADER) + sizeof(T_WVM_VA_FRAME_HEADER) + pFrame->dwFrameSize;
    }
    else
    {
        m_Polling.dwRecvPackets_1++;
        m_Polling.dwRecvBytes_1 += sizeof(T_WVM_PACKET_HEADER) + sizeof(T_WVM_VA_FRAME_HEADER) + pFrame->dwFrameSize;
    }
    // 每隔2秒钟回送一个统计包到发送端
    const DWORD dwCurrMs = timeGetTime();
    if (0 == m_Polling.dwPktSeqBegin)
    {
        m_Polling.dwPktSeqBegin = pBlockHdr->pkt_seq;
        m_Polling.dwSendingMs = dwCurrMs;
    }
    if ((dwCurrMs - m_Polling.dwSendingMs) >= 2000)
    {
        m_Polling.Session = pFrame->Session;
        m_Polling.dwStatTimeMs = dwCurrMs - m_Polling.dwSendingMs; // 在m_Polling.dwSendingMs改变前，先记录下时间跨度
        m_Polling.dwSendingMs = dwCurrMs;
        m_Polling.dwReceiveMs = 0;
        m_Polling.dwPktSeqEnd = pBlockHdr->pkt_seq;

        m_RealtimeStatus.dwRecvFrames = m_RealtimeStatus.Detail.dwRecvFrames;
        m_RealtimeStatus.Detail.dwRecvFrames = 0;

		DWVM_SendNetPacketEx(TRUE, s, WVM_CMD_PS_VA_POLLING, 0, dwDestDeviceId, dwSrcIp, wSrcPort, &m_Polling, sizeof(m_Polling), 0, dwDestDeviceId);

        m_Polling.dwPktSeqBegin = 0;
        m_Polling.dwRecvPackets_0 = 0;
        m_Polling.dwRecvBytes_0 = 0;
        m_Polling.dwRecvPackets_1 = 0;
        m_Polling.dwRecvBytes_1 = 0;
    }

    return bResult;
}

// 取出组合后的完整帧
// 用户应当一直调用这个函数，取空所有帧，直到返回FALSE
BOOL CFrameReceiver::Pop(
    void **ppFramePtr,    // OUT 以tagSVStreamHeader2为头的frame_data的指针
    int *pSizePtr,    // OUT 帧长度
    DWORD *pTypePtr,    // OUT 帧类型: WVM_FRAMETYPE_VIDEO_I, WVM_FRAMETYPE_VIDEO_P, WVM_FRAMETYPE_AUDIO
    DWORD *pImageRes)    // OUT 视频帧的图像分辨率：WVM_IMAGERES_CIF, WVM_IMAGERES_D1, WVM_IMAGERES_QCIF
{
    CXAutoLock lk(m_hLock);

    // 尚未有包在cache中
    if (NULL == m_pRestruct)
    {
        return FALSE;
    }

    // 创建组包buffer
    if (NULL == m_pFrameBuffer)
    {
        // 先分配一个较小的buffer.
        if (WVM_IMAGERES_FULL_STILL == m_LastPushFrame.Session.dwImageResolution)
        {
            m_iFrameBufferSize = WVM_MAX_VIDEO_FRAME_SIZE / 2;
        }
        else if (WVM_IMAGERES_D1 == m_LastPushFrame.Session.dwImageResolution)
        {
            m_iFrameBufferSize = WVM_MAX_VIDEO_FRAME_SIZE / 4;
        }
        else
        {
            m_iFrameBufferSize = WVM_MAX_VIDEO_FRAME_SIZE / 8;
        }

        try
        {
            m_pFrameBuffer = new BYTE[m_iFrameBufferSize];
        }
        catch (...)
        {
            m_pFrameBuffer = NULL;
        }
        if (NULL == m_pFrameBuffer)
        {
            xlog(XLOG_LEVEL_ERROR, "Alloc CFrameReceiver::m_pFrameBuffer FAIL! size=%u\n", m_iFrameBufferSize);
            m_iFrameBufferSize = 0;
            return FALSE;
        }
    }

    // 尝试获取完整帧.
    int iValidSize = 0;
    bool bContinue = true;
    if (!m_pRestruct->Pop(m_pFrameBuffer, m_iFrameBufferSize, &iValidSize, &bContinue))
    {
        if (iValidSize > m_iFrameBufferSize)
        {
            // 如果buffer太小，从新分配一个较大的
            delete[] m_pFrameBuffer;
            m_pFrameBuffer = NULL;

            m_iFrameBufferSize = iValidSize + (16 * 1024);
            try
            {
                m_pFrameBuffer = new BYTE[m_iFrameBufferSize];
            }
            catch (...)
            {
                m_pFrameBuffer = NULL;
            }
            if (NULL == m_pFrameBuffer)
            {
                xlog(XLOG_LEVEL_ERROR, "Realloc CFrameReceiver::m_pFrameBuffer FAIL! size=%u\n", m_iFrameBufferSize);
                m_iFrameBufferSize = 0;
            }
        }
        return FALSE;
    }
    m_iFrameValidSize = iValidSize;
	// 统计信息
    m_RealtimeStatus.Detail.dwRecvFrames++;
	if(m_dwStatMaxFrameSize < (DWORD)iValidSize)
	{
		m_dwStatMaxFrameSize = (DWORD)iValidSize;
	}
	m_dwStatFrames ++;

    // 成功获取
    if (ppFramePtr)
    {
        *ppFramePtr = m_pFrameBuffer;
    }
    if (pSizePtr)
    {
        *pSizePtr = iValidSize;
    }
    if (pTypePtr)
    {
        switch (((tagSVStreamHeader2 *) m_pFrameBuffer)->StreamType)
        {
        case VFT_PFRAME:
            *pTypePtr = WVM_FRAMETYPE_VIDEO_P;
            break;
        case VFT_IFRAME:
            *pTypePtr = WVM_FRAMETYPE_VIDEO_I;
            break;
        case VFT_AUDIO :
            *pTypePtr = WVM_FRAMETYPE_AUDIO;
            break;
        default        :
            *pTypePtr = WVM_FRAMETYPE_UNKNOW;
            break;
        }
    }
    if (pImageRes)
    {
        if (m_LastPushFrame.Session.dwImageResolution < WVM_MAX_IMAGERES_NUM)
        {
            *pImageRes = m_LastPushFrame.Session.dwImageResolution;
        }
        else
        {
            const WORD w = ((tagSVStreamHeader2 *) m_pFrameBuffer)->VideoWidth;
            //const WORD h = ((tagSVStreamHeader2*)m_pFrameBuffer)->VideoHeight;
            if (w > 360)
            {
                if (VCODEC_JPEG == ((tagSVStreamHeader2 *) m_pFrameBuffer)->CodecType)
                {
                    *pImageRes = WVM_IMAGERES_FULL_STILL;
                }
                else
                {
                    *pImageRes = WVM_IMAGERES_D1;
                }
            }
            else if (w < 320)
            {
                *pImageRes = WVM_IMAGERES_QCIF;
            }
            else
            {
                *pImageRes = WVM_IMAGERES_CIF;
            }
        }
    }

    m_dwLastFrameTime = timeGetTime();

    return TRUE;
}

//
// 处理发送端回馈的统计信息
//
BOOL CFrameReceiver::OnReply(T_WVM_VA_REPLY *pReply)
{
    CXAutoLock lk(m_hLock);

    if (NULL == pReply)
    {
        return FALSE;
    }

    m_RealtimeStatus.Detail = *pReply;
    m_RealtimeStatus.Detail.Polling.dwReceiveMs = timeGetTime();
    m_RealtimeStatus.dwStatTimeMs = m_RealtimeStatus.Detail.Polling.dwStatTimeMs;
	m_RealtimeStatus.dwDelayTimeMs = m_RealtimeStatus.Detail.Polling.dwReceiveMs - m_RealtimeStatus.Detail.Polling.dwSendingMs;
    m_RealtimeStatus.dwSendPackets = m_RealtimeStatus.Detail.dwSendPackets_0 + m_RealtimeStatus.Detail.dwSendPackets_1;
    m_RealtimeStatus.dwSendBytes = m_RealtimeStatus.Detail.dwSendBytes_0 + m_RealtimeStatus.Detail.dwSendBytes_1;
	m_RealtimeStatus.dwRecvPackets = m_RealtimeStatus.Detail.Polling.dwRecvPackets_0 + m_RealtimeStatus.Detail.Polling.dwRecvPackets_1;
	m_RealtimeStatus.dwRecvBytes = m_RealtimeStatus.Detail.Polling.dwRecvBytes_0 + m_RealtimeStatus.Detail.Polling.dwRecvBytes_1;

    // 根据响应时间，动态调整重发间隔时间。并控制范围在 10 ~ 1000 毫秒
	const DWORD dwAckTimeMs = (m_RealtimeStatus.dwDelayTimeMs < 10) ? (10) : ((m_RealtimeStatus.dwDelayTimeMs > 1000) ? (1000) : (m_RealtimeStatus.dwDelayTimeMs));
	m_dwResendInterlaceMs = min(dwAckTimeMs*3/2, 1000); // 重发时间间隔，设定为应答时间的1.5倍

    return TRUE;
}

//
// 获取网络传输的实时状态
//
void CFrameReceiver::GetRealtimeStatus(T_DWVM_NET_REALTIME_STATUS *pStatus)
{
    if (NULL == pStatus)
    {
        return;
    }

    // 连续10秒都没有收到数据了，把统计信息清零
    if (0 != m_RealtimeStatus.Detail.Polling.dwSendingMs &&
        (timeGetTime() - m_RealtimeStatus.Detail.Polling.dwSendingMs) > 10000)
    {
        memset(&m_RealtimeStatus, 0, sizeof(m_RealtimeStatus));
    }

    *pStatus = m_RealtimeStatus;
}

// 清除缓存
void CFrameReceiver::ClearCache()
{
    CXAutoLock lk(m_hLock);

    if (m_pRestruct)
    {
        m_pRestruct->ClearFrameChain();
        if (0 != m_dwLastSrcIp && 0 != m_wLastSrcPort)
        {
            //xlog(XLOG_LEVEL_NORMAL, "CFrameReceiver::ClearCache() from %08X:%u\n", m_dwLastSrcIp, m_wLastSrcPort);
        }
    }
}

// 获取状态描述文字
BOOL CFrameReceiver::GetStatusString(char *pszStatus)
{
    strcpy(pszStatus, "");
    sprintf(pszStatus, "Cache %d pkt, %u ms, timeout %u ms, resend intl %u ms",
            GetCachePktNumber(),
            GetCacheDataTimeMs(),
            m_pRestruct->GetTimeout(),
            m_dwResendInterlaceMs);
    return TRUE;
}

DWORD CFrameReceiver::GetCachePktNumber() // 获取缓存中的小包数
{
	if(NULL == m_pRestruct)
	{
		return 0;
	}
    return (DWORD) m_pRestruct->GetFrameChainLen();
}

DWORD CFrameReceiver::GetCacheDataTimeMs() // 获取缓存中的毫秒数
{
	if(NULL == m_pRestruct)
	{
		return 0;
	}
    const DWORD dwFirstPacketTime = m_pRestruct->GetFirstPacketTime();
    return (0 == dwFirstPacketTime) ? 0 : (timeGetTime() - dwFirstPacketTime);
}

// 获取统计信息
BOOL CFrameReceiver::GetStatText(char* szText)
{
	CXAutoLock lk(m_hLock);

	if(NULL == szText || NULL == m_pRestruct)
	{
		return FALSE;
	}
	const DWORD dwCurrTick = GetTickCount();
	if(0 == m_dwStatStartTick || m_dwStatStartTick >= dwCurrTick)
	{
		m_dwStatStartTick = dwCurrTick;
		return FALSE;
	}
	const double dbSeconds = (dwCurrTick - m_dwStatStartTick) / 1000.0;
	sprintf(szText, 
		"%.1f sec, %.1f fps, from %s:%u encoder %08X_#%d, "\
		"max frame %u, max resend-cnt %u, "\
		"recv %.2f kbps %.1f pps, "\
		"recv-resend %.2f kbps %.1f pps, "\
		"query-resend %.1f pps, "\
		"cache %d pkt %u ms, timeout %u ms, resend-intl %u ms", 
		dbSeconds, m_dwStatFrames / dbSeconds, socket_getstring(m_dwLastSrcIp), m_wLastSrcPort, m_dwLastSrcID, m_LastPushFrame.Session.dwSrcDeviceEncoderChannelIndex,
		m_dwStatMaxFrameSize, m_dwStatMaxResendCnt,
		(m_dwStatBytes0 * 8.0 / 1024.0) / dbSeconds, m_dwStatPackets0/dbSeconds,
		(m_dwStatBytes1 * 8.0 / 1024.0) / dbSeconds, m_dwStatPackets1/dbSeconds, 
		m_dwStatResendQueryCnt/dbSeconds,
		GetCachePktNumber(), GetCacheDataTimeMs(), m_pRestruct->GetTimeout(), m_dwResendInterlaceMs);

	m_dwStatStartTick = dwCurrTick;
	m_dwStatMaxFrameSize = 0;
	m_dwStatMaxResendCnt = 0;
	m_dwStatFrames = 0;
	m_dwStatBytes0 = 0;
	m_dwStatBytes1 = 0;
	m_dwStatPackets0 = 0;
	m_dwStatPackets1 = 0;
	m_dwStatResendQueryCnt = 0;
	return TRUE;
}
