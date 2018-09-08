// RestructPacket.cpp: implementation of the CRestructPacket class.
//
//////////////////////////////////////////////////////////////////////

#include "DwvmBase.h"
#include <stdio.h>

#define PRINT_DBG 0

//#define HTONS(us)  ((us>>8)&0xFF) | ((us<<8)&0xFF00)
//#define HTONL(ul)  ((ul>>24)&0xFFL) | ((ul>>8)&0xFF00L) | ((ul<<8)&0xFF0000L) | ((ul<<24)&0xFF000000L)

#define NODE_TIME(nod)      ((T_WVM_VA_BLOCK_HEADER*)nod->data)->time_stamp
#define NODE_PKTSEQ(nod)    ((T_WVM_VA_BLOCK_HEADER*)nod->data)->pkt_seq
#define NODE_FRMSEQ(nod)    ((T_WVM_VA_BLOCK_HEADER*)nod->data)->frm_seq
#define NODE_FRMPKTNUM(nod) ((T_WVM_VA_BLOCK_HEADER*)nod->data)->frm_pkt_num
#define NODE_FRMPKTSEQ(nod) ((T_WVM_VA_BLOCK_HEADER*)nod->data)->frm_pkt_seq
#define NODE_INDEPEND(nod)  ((T_WVM_VA_BLOCK_HEADER*)nod->data)->is_independent

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

CRestructPacket::CRestructPacket() :
    m_chainFrame(),
    m_chainIdle()
{
    m_timeout = 2000;
    m_nLastFrameSeq = 0;
    m_nLastPktSeq = 0;
    m_nContinueErrPktCount = 0;
    m_seqPrevErrPkt = 0;
    m_bWaitAllPacket = FALSE;
    m_nPopPacketCount = 0;

    m_ppCache = NULL;
    m_nCacheLen = 0;

    m_lock = CXAutoLock::Create();

#if PRINT_DBG
    XPrintDebugEnable(true);
#endif
}

CRestructPacket::~CRestructPacket()
{
    Destroy();

    CXAutoLock::Destroy(m_lock);
}

BOOL CRestructPacket::Create(int nFramePacketCount, int nTimeout, int nCachePacketCount)
{
    CXAutoLock lock(m_lock);

    if (nCachePacketCount < 0)
    {
        nCachePacketCount = 0;
    }

    if (m_chainIdle.GetChainLength() != 0 || m_chainFrame.GetChainLength() != 0)
    {
        return FALSE;
    }

    if (!m_chainIdle.Init(nFramePacketCount + nCachePacketCount, 1500))
    {
        return FALSE;
    }

    if (nCachePacketCount > 0)
    {
        try
        {
            m_ppCache = new CXSimpleChain::_node *[nCachePacketCount];
        }
        catch (...)
        {
            m_ppCache = NULL;
        }
        if (m_ppCache)
        {
            memset(m_ppCache, 0, sizeof(CXSimpleChain::_node *) * nCachePacketCount);
            //for(int i=0; i<nCachePacketCount; i++)
            //{
            //	m_pCache[i].node = NULL;
            //	m_pCache[i].pkt_seq = 0;
            //}
        }
    }
    m_nCacheLen = (ULONG) nCachePacketCount;

    m_chainFrame.Init(0, 0);

    m_timeout = (ULONG) nTimeout;
    m_nLastFrameSeq = 0;
    m_nLastPktSeq = 0;
    m_bWaitAllPacket = FALSE;
    m_nPopPacketCount = 0;

    return TRUE;
}

void CRestructPacket::Destroy()
{
    CXAutoLock lock(m_lock);

    m_nCacheLen = 0;
    if (m_ppCache != NULL)
    {
        for (ULONG i = 0; i < m_nCacheLen; i++)
        {
            m_chainIdle.AddToTail(m_ppCache[i]);
        }
        delete[] m_ppCache;
        m_ppCache = NULL;
    }

    m_chainFrame.UnInit();
    m_chainIdle.UnInit();
}

BOOL CRestructPacket::Push(void *pPacket, int nPacketLen, CXSimpleChain::_node **ppNodNew)
{
    CXAutoLock lock(m_lock);

    if (ppNodNew != NULL)
    {
        *ppNodNew = NULL;
    }

    T_WVM_VA_BLOCK_HEADER *hdr = (T_WVM_VA_BLOCK_HEADER *) pPacket;
    if (pPacket == NULL || nPacketLen <= sizeof(T_WVM_VA_BLOCK_HEADER))
    {
        xlog(XLOG_LEVEL_NORMAL, "[CRestructPacket::Push][%p]: error: pPacket=%08X, nPacketLen=%d\n", this, pPacket, nPacketLen);
        return FALSE;
    }

    //
    // hdr->frm_seq小于m_nLastFrameSeq的情况:
    //
    // 1. 有可能是删除超时包后，这个数据包才到达. 或者这个包被重发了多次
    //    这种情况，简单地将所有小于m_nLastFrameSeq的包都丢弃即可.
    //
    // 2. 有可能是设备重新启动了 (断线重连后的frm_seq会从1开始计数)
    //    如果连续3个包的frm_seq都小于m_nLastFrameSeq，并且它们的pkt_seq也是
    //    连续的，就认为是合法的包.
    //    出现这种情况时，frame-chain必然是空的, 并且seq的差距比较大
    //
    if (hdr->frm_seq <= m_nLastFrameSeq)
    {
        //xlog(XLOG_LEVEL_NORMAL, "[CRestructPacket::Push][%p]: frm_seq=%6d, last=%6d\n", this, hdr->frm_seq, m_nLastFrameSeq);

        if (hdr->frm_seq <= 30) // 从新从头发送的数据
        {
            ClearFrameChain();
        }
        else
        {
            if (m_chainFrame.PeekHead() != NULL || (m_nLastFrameSeq - hdr->frm_seq) < 320)
            {
                return TRUE;
            }

            if (hdr->pkt_seq == (m_seqPrevErrPkt + 1))
            {
                m_nContinueErrPktCount++;
            }
            else
            {
                m_nContinueErrPktCount = 0;
            }
            m_seqPrevErrPkt = hdr->pkt_seq;

            if (m_nContinueErrPktCount < 3)
            {
                return TRUE;
            }
            else
            {
                m_nLastFrameSeq = 0;
                m_nLastPktSeq = 0;
            }
        }
    }

    // 从idle链表中取出1块空闲内存，复制数据到其中
    CXSimpleChain::_node *nodNew = m_chainIdle.PeekHead();
    if (nodNew == NULL || nodNew->sizeAlloc < (ULONG) nPacketLen)
    {
        ClearFrameChain();
        return FALSE;
    }
    nodNew = m_chainIdle.RemoveFromHead();
    memcpy(nodNew->data, pPacket, (size_t) nPacketLen);
    nodNew->sizeActual = (size_t) nPacketLen;

    // 打上时间戳
    NODE_TIME(nodNew) = timeGetTime();

    // 按照包序号从小到大的顺序，将包放到链表的合适位置
    CXSimpleChain::_node *nodHead = m_chainFrame.PeekHead();
    CXSimpleChain::_node *nodTail = m_chainFrame.PeekTail();
    //
    if (nodHead == NULL || nodTail == NULL)
    {
        // 链表为空: 放在表头
        m_chainFrame.AddToHead(nodNew);
    }
    else if (NODE_PKTSEQ(nodNew) > NODE_PKTSEQ(nodTail))
    {
        // 序号大于链表的最大序号: 放在表尾
        m_chainFrame.AddToTail(nodNew);
    }
    else if (NODE_PKTSEQ(nodNew) < NODE_PKTSEQ(nodHead))
    {
        // 序号小于链表的最小序号: 放在表头
        m_chainFrame.AddToHead(nodNew);
    }
    else
    {
        // 寻找合适位置
        CXSimpleChain::_node *nod = nodTail;
        while (nod)
        {
            if (NODE_PKTSEQ(nodNew) == NODE_PKTSEQ(nod))
            {
                RecyclePacket(nodNew);
                return TRUE;
            }
            else if (NODE_PKTSEQ(nodNew) > NODE_PKTSEQ(nod))
            {
                m_chainFrame.InsertAfter(nod, nodNew);
                if (ppNodNew != NULL)
                {
                    *ppNodNew = nodNew;
                }
                return TRUE;
            }
            nod = m_chainFrame.PeekPrev(nod);
        }
        if (nod == NULL)
        {
            RecyclePacket(nodNew);
        }
    }

    if (ppNodNew != NULL)
    {
        *ppNodNew = nodNew;
    }
    return TRUE;
}

BOOL CRestructPacket::Pop(void *pFrame, int nBufferSize, int *pActualSize, bool *pIsContinue)
{
    CXAutoLock lock(m_lock);

    if (pActualSize) *pActualSize = 0;

    CXSimpleChain::_node *nodHead = m_chainFrame.PeekHead();
    if (nodHead == NULL)
    {
        return FALSE;
    }
    const ULONG nFrmNo = NODE_FRMSEQ(nodHead);

    // 如果表头是一个完整的帧: 将数据复制出来，立即返回
    if (IsWholeFrame(nodHead))
    {
        // 如果帧序号是不连续的，表示有一部份数据尚未到达，需要等待一段时间
        const bool bContinue = (0 == m_nLastFrameSeq || nFrmNo == (m_nLastFrameSeq + 1));
        if (!bContinue && !NODE_INDEPEND(nodHead))
        {
            // 如果尚未到达的那部分数据，已经达到超时时间的一半了，也不再等待
            if ((timeGetTime() - NODE_TIME(nodHead)) < (m_timeout / 2))
            {
                return FALSE;
            }
        }

        ULONG nLastPktSeq = 0;
        const int nSize = RemoveOneFrame(pFrame, nBufferSize, &nLastPktSeq);
        if (nSize > nBufferSize)
        {
            if (pActualSize)
            {
                *pActualSize = nSize;
            }
            return FALSE;
        }

        if (pActualSize) *pActualSize = nSize;
        if (pIsContinue) *pIsContinue = bContinue;
        m_nLastFrameSeq = nFrmNo;
        m_nLastPktSeq = nLastPktSeq;
        return TRUE;
    }

    // 删除所有已经超时的数据包
    ULONG nCurrTick = timeGetTime();
    while (1)
    {
        nodHead = m_chainFrame.PeekHead();
        if (nodHead == NULL || (nCurrTick - NODE_TIME(nodHead)) < m_timeout)
        {
            break;
        }

		xlog(XLOG_LEVEL_NORMAL, "[CRestructPacket::Pop][%p] delete timeout (%u ms) packet, pkt_seq=%d!", this, m_timeout, NODE_PKTSEQ(nodHead));

        //#if defined(_DEBUG)
        //	PrintChainSeq(&m_chainFrame, "Del Timeout:");
        //#endif

        m_nLastFrameSeq = NODE_FRMSEQ(nodHead);
        RemoveOneFrame(NULL, 0, &m_nLastPktSeq);
    }

    return FALSE;
}

void CRestructPacket::ClearFrameChain()
{
    CXAutoLock lock(m_lock);

	//xlog(XLOG_LEVEL_NORMAL, "[CRestructPacket::ClearFrameChain][%p] Clear cache! (last-pkt-seq = %d)\n", this, m_nLastPktSeq);
    // PrintChainSeq(&m_chainFrame, "#### ");

    CXSimpleChain::_node *nod = NULL;
    do
    {
        nod = m_chainFrame.RemoveFromHead();
        RecyclePacket(nod);

        m_nPopPacketCount++;
    }
    while (nod);

    m_nLastFrameSeq = 0;
    m_nLastPktSeq = 0;
}

// for GetLostPackets()
#define ADD_LOST_PACKET(seq, referNod)\
{\
    if(NULL == referNod || 0 == dwTimeoutMs || (dwCurrentMs - NODE_TIME(referNod)) >= dwTimeoutMs)\
    {\
        pSeqArray[*pLostCount] = (ULONG) (seq);\
        *pLostCount = (*pLostCount) +1;\
        if(*pLostCount >= nArraySize)\
        {\
            return;\
        }\
    }\
}

// 扫描frame链表，找到丢失的包: 仅仅寻找每个帧中缺少的包，不包括整个帧一起丢失的情况
void CRestructPacket::GetLostPackets(ULONG *pSeqArray, int *pLostCount, int nArraySize, int *pRecvCount,
                                     ULONG dwTimeoutMs)
{
    CXAutoLock lock(m_lock);

    int nRecv = m_nPopPacketCount;
    m_nPopPacketCount = 0;
    if (pRecvCount) *pRecvCount = 0;

    const DWORD dwCurrentMs = timeGetTime();

    m_bWaitAllPacket = FALSE;

    *pLostCount = 0;

    CXSimpleChain::_node *nodHead = m_chainFrame.PeekHead();
    if (nodHead == NULL)
    {
        return;
    }

    if (NODE_FRMPKTSEQ(nodHead) != 0)
    {
        ULONG base = NODE_PKTSEQ(nodHead) - NODE_FRMPKTSEQ(nodHead);
        for (ULONG i = 0; i < NODE_FRMPKTSEQ(nodHead); i++)
        {
            ADD_LOST_PACKET(base + i, nodHead);
        }
    }

    CXSimpleChain::_node *nod = m_chainFrame.PeekNext(nodHead);
    CXSimpleChain::_node *nodPrev = NULL;
    while (nod)
    {
        nodPrev = CXSimpleChain::PeekPrev(nod);
        if (nodPrev == NULL)
        {
            break;
        }

        if (NODE_FRMSEQ(nod) == NODE_FRMSEQ(nodPrev))
        {
            for (ULONG i = NODE_PKTSEQ(nodPrev) + 1; i < NODE_PKTSEQ(nod); i++)
            {
                ADD_LOST_PACKET(i, nodPrev);
            }
        }
        else
        {
            if (NODE_FRMPKTSEQ(nodPrev) != (NODE_FRMPKTNUM(nodPrev) - 1))
            {
                for (ULONG i = NODE_FRMPKTSEQ(nodPrev) + 1; i < NODE_FRMPKTNUM(nodPrev); i++)
                {
                    ADD_LOST_PACKET((NODE_PKTSEQ(nodPrev) + i - NODE_FRMPKTSEQ(nodPrev)), nodPrev);
                }
            }
            if (NODE_FRMPKTSEQ(nod) != 0)
            {
                ULONG nBase = NODE_PKTSEQ(nod) - NODE_FRMPKTSEQ(nod);
                for (ULONG i = 0; i < NODE_FRMPKTSEQ(nod); i++)
                {
                    ADD_LOST_PACKET((i + nBase), nod);
                }
            }
        }

        nod = m_chainFrame.PeekNext(nod);

        nRecv++;
    }

    if (pRecvCount) *pRecvCount = nRecv;
}

// 扫描frame链表，找到所有丢失的包
void CRestructPacket::GetLostPackets_All(ULONG *pSeqArray, int *pLostCount, int nArraySize, int *pRecvCount,
                                         ULONG dwTimeoutMs)
{
    CXAutoLock lock(m_lock);

    int nRecv = m_nPopPacketCount;
    m_nPopPacketCount = 0;
    if (pRecvCount) *pRecvCount = 0;

    const DWORD dwCurrentMs = timeGetTime();

    m_bWaitAllPacket = TRUE;

    *pLostCount = 0;

    CXSimpleChain::_node *nodHead = m_chainFrame.PeekHead();
    if (nodHead == NULL)
    {
        return;
    }

    if (m_nLastPktSeq != 0)
    {
        for (ULONG i = m_nLastPktSeq + 1; i < NODE_PKTSEQ(nodHead); i++)
        {
            ADD_LOST_PACKET(i, nodHead);
        }
    }
    else if (NODE_FRMPKTSEQ(nodHead) != 0)
    {
        ULONG base = NODE_PKTSEQ(nodHead) - NODE_FRMPKTSEQ(nodHead);
        for (ULONG i = 0; i < NODE_FRMPKTSEQ(nodHead); i++)
        {
            ADD_LOST_PACKET(base + i, nodHead);
        }
    }

    CXSimpleChain::_node *nod = nodHead;
    CXSimpleChain::_node *nodNext = m_chainFrame.PeekNext(nodHead);
    while (nod && nodNext)
    {
        for (ULONG i = NODE_PKTSEQ(nod) + 1; i < NODE_PKTSEQ(nodNext); i++)
        {
            ADD_LOST_PACKET(i, nod);
        }

        if (NODE_FRMSEQ(nod) != NODE_FRMSEQ(nodNext))
        {
            for (ULONG i = NODE_FRMPKTSEQ(nod) + 1; i < NODE_FRMPKTNUM(nod); i++)
            {
                ADD_LOST_PACKET((NODE_PKTSEQ(nod) + i - NODE_FRMPKTSEQ(nod)), nod);
            }
        }

        nod = nodNext;
        nodNext = CXSimpleChain::PeekNext(nod);

        nRecv++;
    }

    if (pRecvCount) *pRecvCount = nRecv;
}

// 寻找帧内丢失的包
void CRestructPacket::GetLostPacketsInFrame(CXSimpleChain::_node *nodNew, ULONG *pSeqArray, int *pLostCount,
                                            int nArraySize, ULONG dwTimeoutMs)
{
    if (pSeqArray == NULL || pLostCount == NULL)
    {
        return;
    }

    *pLostCount = 0;

    if (NODE_FRMPKTSEQ(nodNew) == 0)
    {
        return;
    }

    const DWORD dwCurrentMs = timeGetTime();

    CXSimpleChain::_node *nodPrev = m_chainFrame.PeekPrev(nodNew);
    ULONG nBeginFrmPktSeq = 0;
    if (nodPrev == NULL)
    {
        nBeginFrmPktSeq = 0;
    }
    else if (NODE_FRMSEQ(nodPrev) == NODE_FRMSEQ(nodNew))
    {
        nBeginFrmPktSeq = NODE_FRMPKTSEQ(nodPrev) + 1;
    }
    else
    {
        // 前一帧的帧尾是否完整？
        if (NODE_FRMPKTSEQ(nodPrev) < (NODE_FRMPKTNUM(nodPrev) - 1))
        {
            ULONG nBase = NODE_PKTSEQ(nodPrev) - NODE_FRMPKTSEQ(nodPrev);
            for (ULONG i = NODE_FRMPKTSEQ(nodPrev) + 1; i < NODE_FRMPKTNUM(nodPrev); i++)
            {
                ADD_LOST_PACKET((i + nBase), nodPrev);
            }
        }

        nBeginFrmPktSeq = 0;
    }

    ULONG nBase = NODE_PKTSEQ(nodNew) - NODE_FRMPKTSEQ(nodNew);
    for (ULONG i = nBeginFrmPktSeq; i < NODE_FRMPKTSEQ(nodNew); i++)
    {
        ADD_LOST_PACKET((i + nBase), nodNew);
    }
}

// 判断从nodStart开始，是不是一个完成的帧
BOOL CRestructPacket::IsWholeFrame(CXSimpleChain::_node *nodStart)
{
    int nPktCount = 0;
    CXSimpleChain::_node *nod = nodStart;
    while (nod)
    {
        if (NODE_FRMSEQ(nod) != NODE_FRMSEQ(nodStart))
        {
            break;
        }
        nPktCount++;

        nod = CXSimpleChain::PeekNext(nod);
    }

    return (nPktCount >= NODE_FRMPKTNUM(nodStart));
}

// 从链表头移出一帧数据. 如果pBuffer不等于NULL，就将数据复制到pBuffer中
// 返回值为被移出的有效数据的长度
int CRestructPacket::RemoveOneFrame(void *pDestBuffer, int nBufferSize, ULONG *pLastPktSeq)
{
    int nRemoveSize = 0;
    BYTE *pBuffer = (BYTE *) pDestBuffer;
    if (pBuffer == NULL)
    {
        nBufferSize = 0;
    }

    CXSimpleChain::_node *nodHead = m_chainFrame.PeekHead();
    const ULONG nFrmNo = (ULONG) NODE_FRMSEQ(nodHead);

    while (1)
    {
        CXSimpleChain::_node *nod = m_chainFrame.PeekHead();
        if (nod == NULL || NODE_FRMSEQ(nod) != nFrmNo)
        {
            break;
        }
        nod = m_chainFrame.RemoveFromHead();

        m_nPopPacketCount++;

        if (pLastPktSeq)
        {
            *pLastPktSeq = NODE_PKTSEQ(nod);
        }

        int nDataLen = (int) (nod->sizeActual - sizeof(T_WVM_VA_BLOCK_HEADER));
        nRemoveSize += nDataLen;

        if (nBufferSize >= nRemoveSize)
        {
            memcpy(pBuffer, ((char *) nod->data) + sizeof(T_WVM_VA_BLOCK_HEADER), (size_t) nDataLen);
            pBuffer += nDataLen;
        }

        RecyclePacket(nod);
    }

    return nRemoveSize;
}

void CRestructPacket::PrintChainSeq(CXSimpleChain *chain, char *Symbol)
{
#if 0
    char str[8192] = {""};
    char tmp[32] = {""};

    CXSimpleChain::_node * nod  = NULL;
    CXSimpleChain::_node * prev = NULL;
    CXSimpleChain::_node * next = NULL;

    strcpy(str, Symbol);

    nod = chain->PeekHead();
    while(nod)
    {
        prev = CXSimpleChain::PeekPrev(nod);
        next = CXSimpleChain::PeekNext(nod);

        if(prev && NODE_PKTSEQ(nod) != NODE_PKTSEQ(prev)+1)
        {
            strcat(str, " !");
        }

        sprintf(tmp, " %d(%d/%d)", NODE_PKTSEQ(nod), NODE_FRMPKTSEQ(nod), NODE_FRMPKTNUM(nod));
        strcat(str, tmp);

        nod = chain->PeekNext(nod);
    }

    OutputDebugString(str);
#endif
}

// 回收数据包：先尝试放入cache，如果失败的话再放入idle
void CRestructPacket::RecyclePacket(CXSimpleChain::_node *nod)
{
    if (m_ppCache == NULL)
    {
        m_chainIdle.AddToTail(nod);
    }
    else if (nod != NULL)
    {
        int nIndex = NODE_PKTSEQ(nod) % m_nCacheLen;
        if (m_ppCache[nIndex] != NULL)
        {
            m_chainIdle.AddToTail(m_ppCache[nIndex]);
        }
        m_ppCache[nIndex] = nod;
    }
}

// 读取指定序号的数据包
BOOL CRestructPacket::GetCachePacket(ULONG nPacketSeq, void *pPacket, int nPacketSize, int *nActualSize)
{
    // 先在cache中寻找，如果不存在的话，再在frame中寻找
    if (m_ppCache != NULL)
    {
        int nIndex = nPacketSeq % m_nCacheLen;
        if (m_ppCache[nIndex] != NULL && NODE_PKTSEQ(m_ppCache[nIndex]) == nPacketSeq)
        {
            if (nPacketSize < (int) m_ppCache[nIndex]->sizeActual)
            {
                return FALSE;
            }
            memcpy(pPacket, m_ppCache[nIndex]->data, m_ppCache[nIndex]->sizeActual);
            *nActualSize = (int) m_ppCache[nIndex]->sizeActual;
            return TRUE;
        }
    }

    CXSimpleChain::_node *nod = m_chainFrame.PeekHead();
    while (nod)
    {
        if (NODE_FRMSEQ(nod) == nPacketSeq)
        {
            if (nPacketSize < (int) nod->sizeActual)
            {
                return FALSE;
            }
            memcpy(pPacket, nod->data, nod->sizeActual);
            *nActualSize = (int) nod->sizeActual;
            return TRUE;
        }
        else if (NODE_FRMSEQ(nod) > nPacketSeq)
        {
            return FALSE;
        }

        nod = CXSimpleChain::PeekNext(nod);
    }
    return FALSE;
}

// 如果第1帧不完整、但是第2帧已经完整了，就删除第1帧
void CRestructPacket::DeleteFirstCrashIfSecondWhole()
{
    CXAutoLock lock(m_lock);

    CXSimpleChain::_node *nodHead = m_chainFrame.PeekHead();
    if (nodHead == NULL)
    {
        return;
    }

    // 如果表头是一个完整的帧，就不需要做任何处理
    if (IsWholeFrame(nodHead))
    {
        return;
    }

    // 寻找第2帧的帧头
    CXSimpleChain::_node *nodHead2 = NULL;
    CXSimpleChain::_node *nod = m_chainFrame.PeekNext(nodHead);
    while (nod != NULL)
    {
        if (NODE_FRMSEQ(nod) != NODE_FRMSEQ(nodHead))
        {
            nodHead2 = nod;
            break;
        }

        nod = m_chainFrame.PeekNext(nod);
    }

    // 如果第2帧完整，就删除第1帧
    if (nodHead2 != NULL && IsWholeFrame(nodHead2))
    {
		xlog(XLOG_LEVEL_NORMAL, "[CRestructPacket::DeleteFirstCrashIfSecondWhole][%p]: del one frame\n", this);

        m_nLastFrameSeq = NODE_FRMSEQ(nodHead);
        RemoveOneFrame(NULL, 0, &m_nLastPktSeq);
    }
}

// 获知最老一个包的时间戳
DWORD CRestructPacket::GetFirstPacketTime()
{
    CXSimpleChain::_node *nodHead = m_chainFrame.PeekHead();
    if (nodHead == NULL)
    {
        return 0;
    }
    return NODE_TIME(nodHead);
}
