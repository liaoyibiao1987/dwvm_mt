// XList.cpp : Defines the entry point for the DLL application.
//

#include <stdlib.h>
#include <time.h>
#include "XList.h"


CXAutoLock::CXAutoLock(HANDLE hMutex)
{
    pthread_mutex_t *mt = (pthread_mutex_t *) hMutex;
    m_hMutex = hMutex;
    if (mt)
    {
        pthread_mutex_lock(mt);
    }
}

CXAutoLock::~CXAutoLock()
{
    pthread_mutex_t *mt = (pthread_mutex_t *) m_hMutex;
    if (mt)
    {
        pthread_mutex_unlock(mt);
    }
}

//static
HANDLE CXAutoLock::Create()
{
    pthread_mutex_t *mt = (pthread_mutex_t *) malloc(sizeof(pthread_mutex_t));
    if (mt)
    {
        memset(mt, 0, sizeof(pthread_mutex_t));
        pthread_mutexattr_t attr = PTHREAD_MUTEX_RECURSIVE_NP; // 嵌套，允许同一个线程多次锁定
        pthread_mutex_init(mt, &attr);
    }
    return mt;
}

//static 
void CXAutoLock::Destroy(HANDLE&hMutex)
{
    pthread_mutex_t *mt = (pthread_mutex_t *) hMutex;
    if (mt)
    {
        pthread_mutex_destroy(mt);
        free(mt);
        hMutex = NULL;
    }
}


//============================================================================
//
// CXSimpleList
//
//============================================================================

CXSimpleList::CXSimpleList()
{
    m_pItem = NULL;
    m_nItemCount = 0;
    m_nMaxItemCount = 0;

    m_lock = CXAutoLock::Create();
}

CXSimpleList::~CXSimpleList()
{
    UnInit();

    CXAutoLock::Destroy(m_lock);
}

bool CXSimpleList::Init(int nMaxItemCount)
{
    CXAutoLock lock(m_lock);

    if (m_pItem != NULL)
    {
        return false;
    }

    size_t sizeByte = nMaxItemCount * sizeof(tagItemData);
    m_pItem = (tagItemData *) malloc(sizeByte);
    if (m_pItem == NULL)
    {
        return false;
    }

    memset(m_pItem, 0, sizeByte);
    m_nMaxItemCount = nMaxItemCount;
    m_nItemCount = 0;
    return true;
}

void CXSimpleList::UnInit()
{
    CXAutoLock lock(m_lock);

    if (m_pItem != NULL)
    {
        free(m_pItem);
        m_pItem = NULL;
    }
    m_nMaxItemCount = 0;
    m_nItemCount = 0;
}

int CXSimpleList::Found(ULONG ID)
{
    CXAutoLock lock(m_lock);

    if (ID != 0)
    {
        for (int i = 0; i < m_nItemCount; i++)
        {
            if (m_pItem[i].ID == ID)
            {
                return i;
            }
        }
    }

    return -1;
}

int CXSimpleList::Add(ULONG ID, void *Context)
{
    if (Found(ID) >= 0)
    {
        return -1;
    }

    CXAutoLock lock(m_lock);

    if (ID == 0 || m_nItemCount >= m_nMaxItemCount)
    {
        return -1;
    }

    m_pItem[m_nItemCount].ID = ID;
    m_pItem[m_nItemCount].Context = Context;
    m_nItemCount++;
    return (m_nItemCount - 1);
}

void CXSimpleList::Remove(int Index, BOOL bSort)
{
    CXAutoLock lock(m_lock);

    if (Index >= 0 && Index < m_nItemCount)
    {
        // bSort:
        // TRUE  -- 维持原来的排列顺序
        // FALSE -- 不维持原来的排列顺序，用最后一个Item取代被删除的这个

        if (bSort)
        {
            for (int i = Index; i < m_nItemCount - 1; i++)
            {
                m_pItem[i] = m_pItem[i + 1];
            }
        }
        else
        {
            if (Index != (m_nItemCount - 1))
            {
                m_pItem[Index] = m_pItem[m_nItemCount - 1];
            }
        }
        m_pItem[m_nItemCount - 1].ID = 0;
        m_pItem[m_nItemCount - 1].Context = NULL;
        m_nItemCount--;
    }
}

int CXSimpleList::Remove(ULONG ID, BOOL bSort)
{
    int Index = Found(ID);
    if (Index < 0)
    {
        return -1;
    }

    Remove(Index, bSort);
    return Index;
}

void CXSimpleList::RemoveAll()
{
    CXAutoLock lock(m_lock);

    memset(m_pItem, 0, sizeof(tagItemData) * m_nMaxItemCount);
    m_nItemCount = 0;
}

void *CXSimpleList::GetItem(int Index)
{
    CXAutoLock lock(m_lock);

    if (Index >= 0 && Index < m_nItemCount)
    {
        return m_pItem[Index].Context;
    }
    return NULL;
}

void *CXSimpleList::GetItem(ULONG ID)
{
    int Index = Found(ID);
    if (Index < 0)
    {
        return NULL;
    }

    return GetItem(Index);
}

void CXSimpleList::SetItem(int Index, void *Context)
{
    CXAutoLock lock(m_lock);

    if (Index >= 0 && Index < m_nItemCount)
    {
        m_pItem[Index].Context = Context;
    }
}

void CXSimpleList::SetItem(ULONG ID, void *Context)
{
    int Index = Found(ID);
    if (Index >= 0)
    {
        SetItem(Index, Context);
    }
}

ULONG CXSimpleList::GetID(int Index)
{
    CXAutoLock lock(m_lock);

    if (Index >= 0 && Index < m_nItemCount)
    {
        return m_pItem[Index].ID;
    }
    return 0;
}


//============================================================================
//
// CXSimpleFifo
//
//============================================================================

CXSimpleFifo::CXSimpleFifo()
{
    m_pItem = NULL;
    m_nMaxItemSize = 0;
    m_nMaxItemCount = 0;
    m_nReadPos = 0;
    m_nWritePos = 0;
    m_nItemCount = 0;

    m_lock = CXAutoLock::Create();
}

CXSimpleFifo::~CXSimpleFifo()
{
    UnInit();

    CXAutoLock::Destroy(m_lock);
}

bool CXSimpleFifo::Init(int nMaxItemSize, int nMaxItemCount)
{
    if (m_pItem != NULL || nMaxItemSize < 1 || nMaxItemCount < 3)
    {
        return false;
    }

    m_pItem = (tagItemData *) malloc(nMaxItemCount * sizeof(tagItemData));
    if (m_pItem == NULL)
    {
        return false;
    }
    memset(m_pItem, 0, nMaxItemCount * sizeof(tagItemData));

    int i = 0;
    for (i = 0; i < nMaxItemCount; i++)
    {
        m_pItem[i].pData = malloc((size_t) nMaxItemSize);
        m_pItem[i].nDataLen = 0;
        if (m_pItem[i].pData == NULL)
        {
            break;
        }
    }

    m_nMaxItemSize = nMaxItemSize;
    m_nMaxItemCount = i;

    Reset();
    return true;
}

void CXSimpleFifo::UnInit()
{
    if (m_pItem)
    {
        for (int i = 0; i < m_nMaxItemCount; i++)
        {
            if (m_pItem[i].pData)
            {
                free(m_pItem[i].pData);
                m_pItem[i].pData = NULL;
            }
        }
        free(m_pItem);
        m_pItem = NULL;
    }

    Reset();
}

bool CXSimpleFifo::Push(void *pData, int nDataLen, long Context1, long Context2)
{
    CXAutoLock lock(m_lock);

    if (m_pItem == NULL || nDataLen > m_nMaxItemSize || m_nItemCount >= m_nMaxItemCount)
    {
        return false;
    }

    memcpy(m_pItem[m_nWritePos].pData, pData, (size_t) nDataLen);
    m_pItem[m_nWritePos].nDataLen = nDataLen;
    m_pItem[m_nWritePos].Context1 = Context1;
    m_pItem[m_nWritePos].Context2 = Context2;

    m_nWritePos++;
    if (m_nWritePos >= m_nMaxItemCount)
    {
        m_nWritePos = 0;
    }

    m_nItemCount++;

    return true;
}

bool CXSimpleFifo::Pop(void *pData, int nBufLen, int *pActualDataLen, long *pContext1, long *pContext2)
{
    CXAutoLock lock(m_lock);

    if (m_pItem == NULL || pActualDataLen == NULL || m_nItemCount <= 0)
    {
        return false;
    }

    if (nBufLen < m_pItem[m_nReadPos].nDataLen)
    {
        MY_SLEEP(10);
        return false;
    }

    memcpy(pData, m_pItem[m_nReadPos].pData, (size_t) m_pItem[m_nReadPos].nDataLen);
    *pActualDataLen = m_pItem[m_nReadPos].nDataLen;
    if (pContext1) *pContext1 = m_pItem[m_nReadPos].Context1;
    if (pContext2) *pContext2 = m_pItem[m_nReadPos].Context2;

    m_nReadPos++;
    if (m_nReadPos >= m_nMaxItemCount)
    {
        m_nReadPos = 0;
    }

    m_nItemCount--;
    return true;
}

int CXSimpleFifo::GetCount()
{
    return m_nItemCount;

    //if(m_pItem == NULL || m_nMaxItemCount < 3)
    //{
    //	return 0;
    //}
    //
    //return (m_nWritePos + m_nMaxItemCount - m_nReadPos) % m_nMaxItemCount;
}

void CXSimpleFifo::Reset()
{
    m_nReadPos = 0;
    m_nWritePos = 0;
    m_nItemCount = 0;
}


//============================================================================
//
// CXSimpleChain
//
//============================================================================

CXSimpleChain::CXSimpleChain()
{
    m_first = NULL;
    m_last = NULL;

    m_lock = CXAutoLock::Create();
}

CXSimpleChain::~CXSimpleChain()
{
    UnInit();

    CXAutoLock::Destroy(m_lock);
}

bool CXSimpleChain::Init(int nAllocNodeCount, int nNodeDataSize)
{
    CXAutoLock lock(m_lock);

    if (m_first)
    {
        return false;
    }

    _node *tmp = NULL;
    for (int i = 0; i < nAllocNodeCount; i++)
    {
        tmp = AllocNode(nNodeDataSize);
        if (tmp == NULL)
        {
            UnInit();
            return false;
        }

        AddToHead(tmp);
    }

    return true;
}

void CXSimpleChain::UnInit()
{
    CXAutoLock lock(m_lock);

    _node *tmp = NULL;
    while (1)
    {
        tmp = RemoveFromHead();
        if (tmp == NULL)
        {
            break;
        }

        if (tmp->data)
        {
            free(tmp->data);
        }
        free(tmp);
    }

    m_first = NULL;
    m_last = NULL;
}

int CXSimpleChain::GetChainLength()
{
    CXAutoLock lock(m_lock);

    int count = 0;

    _node *tmp = m_first;
    while (tmp)
    {
        count++;
        tmp = PeekNext(tmp);
    }

    return count;
}

void CXSimpleChain::AddToHead(_node *p)
{
    if (p)
    {
        CXAutoLock lock(m_lock);

        p->timestamp = (unsigned long) time(NULL);

        if (m_first == NULL)
        {
            p->prev = NULL;
            p->next = NULL;
        }
        else
        {
            p->prev = NULL;
            p->next = m_first;
            m_first->prev = p;
        }
        m_first = p;

        if (m_last == NULL)
        {
            m_last = m_first;
        }
    }
}

void CXSimpleChain::AddToTail(_node *p)
{
    if (p)
    {
        CXAutoLock lock(m_lock);

        p->timestamp = (unsigned long) time(NULL);

        if (m_last == NULL)
        {
            p->prev = NULL;
            p->next = NULL;
        }
        else
        {
            p->prev = m_last;
            p->next = NULL;
            m_last->next = p;
        }
        m_last = p;

        if (m_first == NULL)
        {
            m_first = m_last;
        }
    }
}

void CXSimpleChain::InsertBefore(_node *exist, _node *p)
{
    if (p && exist)
    {
        CXAutoLock lock(m_lock);

        p->timestamp = (unsigned long) time(NULL);

        if (exist == m_first)
        {
            m_first = p;
        }

        p->prev = exist->prev;
        p->next = exist;

        if (exist->prev)
        {
            exist->prev->next = p;
        }
        exist->prev = p;
    }
}

void CXSimpleChain::InsertAfter(_node *exist, _node *p)
{
    if (p && exist)
    {
        CXAutoLock lock(m_lock);

        p->timestamp = (unsigned long) time(NULL);

        if (exist == m_last)
        {
            m_last = p;
        }

        p->prev = exist;
        p->next = exist->next;

        if (exist->next)
        {
            exist->next->prev = p;
        }
        exist->next = p;
    }
}

CXSimpleChain::_node *CXSimpleChain::RemoveFromHead()
{
    return Remove(m_first);
}

CXSimpleChain::_node *CXSimpleChain::RemoveFromTail()
{
    return Remove(m_last);
}

CXSimpleChain::_node *CXSimpleChain::Remove(_node *p)
{
    if (p)
    {
        CXAutoLock lock(m_lock);

        if (p == m_first)
        {
            m_first = m_first->next;
        }
        if (p == m_last)
        {
            m_last = m_last->prev;
        }

        if (p->prev)
        {
            p->prev->next = p->next;
        }
        if (p->next)
        {
            p->next->prev = p->prev;
        }
        p->prev = NULL;
        p->next = NULL;

        return p;
    }
    return NULL;
}

CXSimpleChain::_node *CXSimpleChain::PeekHead()
{
    return m_first;
}

CXSimpleChain::_node *CXSimpleChain::PeekTail()
{
    return m_last;
}

//static
CXSimpleChain::_node *CXSimpleChain::PeekPrev(_node *p)
{
    if (p)
    {
        return p->prev;
    }
    return NULL;
}

//static
CXSimpleChain::_node *CXSimpleChain::PeekNext(_node *p)
{
    if (p)
    {
        return p->next;
    }
    return NULL;
}

//static
CXSimpleChain::_node *CXSimpleChain::AllocNode(int nAllocSize)
{
    _node *tmp = (_node *) malloc(sizeof(_node));
    if (tmp == NULL)
    {
        return NULL;
    }

    tmp->data = malloc((size_t) nAllocSize);
    if (tmp->data == NULL)
    {
        free(tmp);
        return NULL;
    }

    tmp->prev = NULL;
    tmp->next = NULL;
    tmp->sizeAlloc = (unsigned long) nAllocSize;
    tmp->sizeActual = 0;
    tmp->timestamp = (unsigned long) time(NULL);

    return tmp;
}

//static
void CXSimpleChain::FreeNode(_node *p)
{
    if (p)
    {
        if (p->data)
        {
            free(p->data);
        }
        free(p);
    }
}


static const int constNodeHeaderSize = sizeof(CXCacheFifo::tagNodeHeader);

#define NODE_SEQ(nod)        ((tagNodeHeader*)nod->data)->seqnum
#define NODE_MARKER(nod)    ((tagNodeHeader*)nod->data)->marker
#define NODE_CONTEXT(nod)    ((tagNodeHeader*)nod->data)->context
#define NODE_SIZE(nod)        ((tagNodeHeader*)nod->data)->size
#define NODE_TIMESTAMP(nod)    ((tagNodeHeader*)nod->data)->timestamp

CXCacheFifo::CXCacheFifo() :
    m_chainUsed(),
    m_chainIdle()
{
    m_nPacketSize = 0;
    m_nSeqnum = 1;

    m_lock = CXAutoLock::Create();
}

CXCacheFifo::~CXCacheFifo()
{
    UnInit();

    CXAutoLock::Destroy(m_lock);
}

bool CXCacheFifo::Init(int nPacketCount, int nPacketSize)
{
    CXAutoLock lock(m_lock);

    if (m_nPacketSize != 0 || nPacketSize <= 0 || nPacketCount < 0)
    {
        return false;
    }

    m_nPacketSize = nPacketSize;
    m_nSeqnum = 1;

    if (nPacketCount > 0)
    {
        m_chainIdle.Init(nPacketCount, nPacketSize + constNodeHeaderSize);
    }

    return true;
}

void CXCacheFifo::UnInit()
{
    CXAutoLock lock(m_lock);

    m_chainUsed.UnInit();
    m_chainIdle.UnInit();
    m_nPacketSize = 0;
}

RETVAL_xxx CXCacheFifo::Push(void *pData, int nDataSize, unsigned long Context, int nIfNoFreeSpace)
{
    CXAutoLock lock(m_lock);

    if (m_nPacketSize == 0)
    {
        return RETVAL_NOINIT;
    }
    if (pData == NULL || nDataSize <= 0)
    {
        return RETVAL_INVALIDPARAM;
    }
    if (nDataSize > 1 * 1024 * 1024)
    {
        return RETVAL_DATATOOLARGE;
    }

    int nNeedPacketCount = (nDataSize + m_nPacketSize - 1) / m_nPacketSize;
    int nIdlePacketCount = m_chainIdle.GetChainLength();

    if (nIdlePacketCount < nNeedPacketCount)
    {
        switch (nIfNoFreeSpace)
        {
        case IFNOFREESPACE_RETURN:
            return RETVAL_NOFREESPACE;

        case IFNOFREESPACE_ALLOCNEW:
        {
            for (int i = nIdlePacketCount; i < nNeedPacketCount; i++)
            {
                CXSimpleChain::_node *pNew = CXSimpleChain::AllocNode(m_nPacketSize + constNodeHeaderSize);
                if (pNew == NULL)
                {
                    return RETVAL_ALLOCFAIL;
                }
                m_chainIdle.AddToTail(pNew);
            }
        }
            break;

        case IFNOFREESPACE_DELETEOLD:
        {
            while (m_chainIdle.GetChainLength() < nNeedPacketCount && m_chainUsed.PeekHead() != NULL)
            {
                RETVAL_xxx ret = RemoveHead();
                if (ret != RETVAL_OK)
                {
                    return ret;
                }
            }
        }
            break;

        default:
            return RETVAL_INVALIDPARAM;
        }
    }

    unsigned char *pSrc = (unsigned char *) pData;
    int nRemainDataSize = nDataSize;
    while (nRemainDataSize > 0)
    {
        CXSimpleChain::_node *pNode = m_chainIdle.RemoveFromHead();
        if (pNode == NULL)
        {
            return RETVAL_NOFREESPACE;
        }
        tagNodeHeader *pHdr = (tagNodeHeader *) pNode->data;
        unsigned char *pData2 = ((unsigned char *) pNode->data) + constNodeHeaderSize;

        if (nRemainDataSize > m_nPacketSize)
        {
            pNode->sizeActual = (unsigned long) (m_nPacketSize + constNodeHeaderSize);
            pHdr->size = m_nPacketSize;
            pHdr->marker = 0;
            nRemainDataSize -= m_nPacketSize;
        }
        else
        {
            pNode->sizeActual = (unsigned long) (nRemainDataSize + constNodeHeaderSize);
            pHdr->size = nRemainDataSize;
            pHdr->marker = 1;
            nRemainDataSize = 0;
        }
        pHdr->context = Context;
        pHdr->seqnum = m_nSeqnum++;
        pHdr->timestamp = (unsigned long) time(NULL);

        memcpy(pData2, pSrc, (size_t) pHdr->size);
        pSrc += pHdr->size;

        m_chainUsed.AddToTail(pNode);
    }

    return RETVAL_OK;
}

RETVAL_xxx CXCacheFifo::Pop(void *pDestBuffer, int nBufferSize, int *pActualSize, unsigned long *pContext)
{
    CXAutoLock lock(m_lock);

    if (m_nPacketSize == 0)
    {
        return RETVAL_NOINIT;
    }

    // 完整帧的总长度
    CXSimpleChain::_node *pNode = NULL;
    int nTotalSize = 0;
    while (1)
    {
        if (pNode == NULL) pNode = m_chainUsed.PeekHead();
        else pNode = m_chainUsed.PeekNext(pNode);

        if (pNode == NULL)
        {
            break;
        }
        nTotalSize += NODE_SIZE(pNode);

        if (NODE_MARKER(pNode))
        {
            break;
        }
    }
    if (nTotalSize <= 0)
    {
        return RETVAL_NOCACHEDATA;
    }

    if (pActualSize != NULL)
    {
        *pActualSize = nTotalSize;
    }

    if (pDestBuffer == NULL)
    {
        // 用户仅仅希望获得帧长度, 并不希望取出这一帧
        if (pActualSize == NULL)
        {
            return RETVAL_INVALIDPARAM;
        }
        else
        {
            return RETVAL_OK;
        }
    }

    if (nBufferSize < nTotalSize)
    {
        return RETVAL_BUFFTOOSMALL;
    }

    unsigned char *pDest = (unsigned char *) pDestBuffer;
    while (1)
    {
        pNode = m_chainUsed.RemoveFromHead();
        if (pNode == NULL)
        {
            break;
        }

        tagNodeHeader *pHdr = (tagNodeHeader *) pNode->data;
        unsigned char *pData = ((unsigned char *) pNode->data) + constNodeHeaderSize;

        memcpy(pDest, pData, (size_t) pHdr->size);
        pDest += pHdr->size;

        m_chainIdle.AddToTail(pNode);

        if (pHdr->marker)
        {
            if (pContext != NULL) *pContext = NODE_CONTEXT(pNode);
            break;
        }
    }

    return RETVAL_OK;
}

RETVAL_xxx CXCacheFifo::RemoveHead()
{
    if (m_nPacketSize == 0)
    {
        return RETVAL_NOINIT;
    }

    while (1)
    {
        CXSimpleChain::_node *pNode = m_chainUsed.RemoveFromHead();
        if (pNode == NULL)
        {
            return RETVAL_NOFREESPACE;
        }

        m_chainIdle.AddToTail(pNode);

        if (NODE_MARKER(pNode))
        {
            break;
        }
    }

    return RETVAL_OK;
}

int CXCacheFifo::GetTotalSize()
{
    return m_nPacketSize * m_chainUsed.GetChainLength();
}

int CXCacheFifo::GetIdleSize()
{
    return m_nPacketSize * m_chainIdle.GetChainLength();
}

void CXCacheFifo::ClearCache()
{
    while (1)
    {
        CXSimpleChain::_node *pNode = m_chainUsed.RemoveFromHead();
        if (pNode == NULL)
        {
            return;
        }

        m_chainIdle.AddToTail(pNode);
    }
}

unsigned long CXCacheFifo::GetHeadTimestamp()
{
    CXSimpleChain::_node *pNode = m_chainUsed.PeekHead();
    if (pNode == NULL)
    {
        return 0;
    }

    return NODE_TIMESTAMP(pNode);
}


//============================================================================
//
// CXBufferFifo
//
//============================================================================

CXBufferFifo::CXBufferFifo()
{
    m_pItem = NULL;
    m_nMaxItemCount = 0;
    m_nReadPos = 0;
    m_nWritePos = 0;
    m_nItemCount = 0;
    m_nAlignBytes = 16;

    m_lock = CXAutoLock::Create();
}

CXBufferFifo::~CXBufferFifo()
{
    UnInit();

    CXAutoLock::Destroy(m_lock);
}

bool CXBufferFifo::Init(int nMaxItemCount, int nAlignBytes)
{
    CXAutoLock lock(m_lock);

    if (m_pItem != NULL || nMaxItemCount < 3 || nAlignBytes <= 0 || nAlignBytes > 4096)
    {
        return false;
    }

    m_pItem = (tagItemData *) malloc(nMaxItemCount * sizeof(tagItemData));
    if (m_pItem == NULL)
    {
        return false;
    }
    memset(m_pItem, 0, nMaxItemCount * sizeof(tagItemData));

    int i = 0;
    for (i = 0; i < nMaxItemCount; i++)
    {
        m_pItem[i].pAllocBuffer = NULL;
        m_pItem[i].nAllocBufferSize = 0;
        m_pItem[i].pBufferAlignPtr = NULL;
        m_pItem[i].nValidDataLen = 0;
        m_pItem[i].pContext = NULL;
        m_pItem[i].dwContext = 0;
    }

    m_nMaxItemCount = nMaxItemCount;
    m_nAlignBytes = nAlignBytes;

    Reset();
    return true;
}

void CXBufferFifo::UnInit()
{
    CXAutoLock lock(m_lock);

    if (m_pItem)
    {
        for (int i = 0; i < m_nMaxItemCount; i++)
        {
            if (m_pItem[i].pAllocBuffer)
            {
                free(m_pItem[i].pAllocBuffer);
                m_pItem[i].pAllocBuffer = NULL;
            }
            m_pItem[i].nAllocBufferSize = 0;
            m_pItem[i].pBufferAlignPtr = NULL;
            m_pItem[i].nValidDataLen = 0;
            m_pItem[i].pContext = NULL;
            m_pItem[i].dwContext = 0;
        }
        free(m_pItem);
        m_pItem = NULL;
    }

    Reset();
}

bool CXBufferFifo::Push(void *pData, int nDataLen, void *pContext1, unsigned long dwContext2)
{
    CXAutoLock lock(m_lock);

    if (NULL == pData || nDataLen <= 0)
    {
        return false;
    }
    if (m_pItem == NULL || m_nItemCount >= m_nMaxItemCount || m_nAlignBytes <= 0)
    {
        return false;
    }

    if (m_pItem[m_nWritePos].nAllocBufferSize < nDataLen)
    {
        if (m_pItem[m_nWritePos].pAllocBuffer)
        {
            free(m_pItem[m_nWritePos].pAllocBuffer);
            m_pItem[m_nWritePos].pAllocBuffer = NULL;
        }
        m_pItem[m_nWritePos].nAllocBufferSize = 0;
    }
    if (NULL == m_pItem[m_nWritePos].pAllocBuffer)
    {
        m_pItem[m_nWritePos].pAllocBuffer = malloc((size_t) (nDataLen + m_nAlignBytes));
        if (NULL == m_pItem[m_nWritePos].pAllocBuffer)
        {
            return false;
        }
        m_pItem[m_nWritePos].nAllocBufferSize = nDataLen;
        m_pItem[m_nWritePos].pBufferAlignPtr = (void *) (
            (((unsigned long) m_pItem[m_nWritePos].pAllocBuffer + m_nAlignBytes - 1) / m_nAlignBytes) * m_nAlignBytes);
        m_pItem[m_nWritePos].nValidDataLen = 0;
    }

    memcpy(m_pItem[m_nWritePos].pBufferAlignPtr, pData, (size_t) nDataLen);
    m_pItem[m_nWritePos].nValidDataLen = nDataLen;
    m_pItem[m_nWritePos].pContext = pContext1;
    m_pItem[m_nWritePos].dwContext = dwContext2;

    m_nWritePos++;
    if (m_nWritePos >= m_nMaxItemCount)
    {
        m_nWritePos = 0;
    }

    m_nItemCount++;
    return true;
}

bool CXBufferFifo::Peek(void **ppData, int *pActualDataLen, void **ppContext1, unsigned long *pdwContext2)
{
    CXAutoLock lock(m_lock);

    if (ppData == NULL || pActualDataLen == NULL)
    {
        return false;
    }
    if (m_pItem == NULL || m_nItemCount <= 0)
    {
        return false;
    }

    *ppData = m_pItem[m_nReadPos].pBufferAlignPtr;
    *pActualDataLen = m_pItem[m_nReadPos].nValidDataLen;
    if (ppContext1) *ppContext1 = m_pItem[m_nReadPos].pContext;
    if (pdwContext2) *pdwContext2 = m_pItem[m_nReadPos].dwContext;

    return true;
}

bool CXBufferFifo::Pop(BOOL bFreeBuffer)
{
    CXAutoLock lock(m_lock);

    if (m_pItem == NULL || m_nItemCount <= 0)
    {
        return false;
    }

    m_pItem[m_nReadPos].nValidDataLen = 0;
    m_pItem[m_nReadPos].pContext = NULL;
    m_pItem[m_nReadPos].dwContext = 0;

    if (bFreeBuffer)
    {
        if (m_pItem[m_nReadPos].pAllocBuffer)
        {
            free(m_pItem[m_nReadPos].pAllocBuffer);
            m_pItem[m_nReadPos].pAllocBuffer = NULL;
        }
        m_pItem[m_nReadPos].nAllocBufferSize = 0;
    }

    m_nReadPos++;
    if (m_nReadPos >= m_nMaxItemCount)
    {
        m_nReadPos = 0;
    }

    m_nItemCount--;
    return true;
}

int CXBufferFifo::GetCount()
{
    return m_nItemCount;

    //if(m_pItem == NULL || m_nMaxItemCount < 3)
    //{
    //	return 0;
    //}
    //
    //return (m_nWritePos + m_nMaxItemCount - m_nReadPos) % m_nMaxItemCount;
}

void CXBufferFifo::Reset()
{
    m_nReadPos = 0;
    m_nWritePos = 0;
    m_nItemCount = 0;
}


CXSimpleList2::CXSimpleList2()
{
    m_pItem = NULL;
    m_nItemCount = 0;
    m_nMaxItemCount = 0;

    m_lock = CXAutoLock::Create();
}

CXSimpleList2::~CXSimpleList2()
{
    UnInit();

    CXAutoLock::Destroy(m_lock);
}

bool CXSimpleList2::Init(int nMaxItemCount)
{
    CXAutoLock lock(m_lock);

    if (m_pItem != NULL)
    {
        return false;
    }

    int sizeByte = nMaxItemCount * sizeof(tagItemData);
    m_pItem = (tagItemData *) malloc((size_t) sizeByte);
    if (m_pItem == NULL)
    {
        return false;
    }

    memset(m_pItem, 0, (size_t) sizeByte);
    m_nMaxItemCount = nMaxItemCount;
    m_nItemCount = 0;
    return true;
}

void CXSimpleList2::UnInit()
{
    CXAutoLock lock(m_lock);

    if (m_pItem != NULL)
    {
        free(m_pItem);
        m_pItem = NULL;
    }
    m_nMaxItemCount = 0;
    m_nItemCount = 0;
}

int CXSimpleList2::Found(ULONG ID1, ULONG ID2)
{
    CXAutoLock lock(m_lock);

    for (int i = 0; i < m_nItemCount; i++)
    {
        if (m_pItem[i].ID1 == ID1 && m_pItem[i].ID2 == ID2)
        {
            return i;
        }
    }

    return -1;
}

int CXSimpleList2::Found(UINT64 ID)
{
    ULONG *p = (ULONG *) &ID;
    return Found(p[0], p[1]);
}

// 从nIndex开始(不包括nIndex)，查找ID1
int CXSimpleList2::FoundNext(ULONG ID1, int nIndex)
{
    CXAutoLock lock(m_lock);

    if (nIndex < 0)
    {
        nIndex = -1;
    }

    for (int i = nIndex + 1; i < m_nItemCount; i++)
    {
        if (m_pItem[i].ID1 == ID1)
        {
            return i;
        }
    }

    return -1;
}

// 从nIndex开始(不包括nIndex)，查找ID2
int CXSimpleList2::FoundNext2(ULONG ID2, int nIndex)
{
    CXAutoLock lock(m_lock);

    if (nIndex < 0)
    {
        nIndex = -1;
    }

    for (int i = nIndex + 1; i < m_nItemCount; i++)
    {
        if (m_pItem[i].ID2 == ID2)
        {
            return i;
        }
    }

    return -1;
}

int CXSimpleList2::Add(ULONG ID1, ULONG ID2, void *Context)
{
    if (Found(ID1, ID2) >= 0)
    {
        return -1;
    }

    CXAutoLock lock(m_lock);

    if (m_nItemCount >= m_nMaxItemCount)
    {
        return -1;
    }

    m_pItem[m_nItemCount].ID1 = ID1;
    m_pItem[m_nItemCount].ID2 = ID2;
    m_pItem[m_nItemCount].Context = Context;
    m_nItemCount++;
    return (m_nItemCount - 1);
}

int CXSimpleList2::Add(UINT64 ID, void *Context)
{
    ULONG *p = (ULONG *) &ID;
    return Add(p[0], p[1], Context);
}

void CXSimpleList2::Remove(int Index, BOOL bSort)
{
    CXAutoLock lock(m_lock);

    if (Index >= 0 && Index < m_nItemCount)
    {
        // bSort:
        // TRUE  -- 维持原来的排列顺序
        // FALSE -- 不维持原来的排列顺序，用最后一个Item取代被删除的这个

        if (bSort)
        {
            for (int i = Index; i < m_nItemCount - 1; i++)
            {
                m_pItem[i] = m_pItem[i + 1];
            }
        }
        else
        {
            if (Index != (m_nItemCount - 1))
            {
                m_pItem[Index] = m_pItem[m_nItemCount - 1];
            }
        }
        m_pItem[m_nItemCount - 1].ID1 = 0;
        m_pItem[m_nItemCount - 1].ID2 = 0;
        m_pItem[m_nItemCount - 1].Context = NULL;
        m_nItemCount--;
    }
}

int CXSimpleList2::Remove(ULONG ID1, ULONG ID2, BOOL bSort)
{
    int Index = Found(ID1, ID2);
    if (Index < 0)
    {
        return -1;
    }

    Remove(Index, bSort);
    return Index;
}

int CXSimpleList2::Remove(UINT64 ID, BOOL bSort)
{
    ULONG *p = (ULONG *) &ID;
    return Remove(p[0], p[1], bSort);
}

void CXSimpleList2::RemoveAll()
{
    CXAutoLock lock(m_lock);

    memset(m_pItem, 0, sizeof(tagItemData) * m_nMaxItemCount);
    m_nItemCount = 0;
}

void *CXSimpleList2::GetItem(int Index)
{
    CXAutoLock lock(m_lock);

    if (Index >= 0 && Index < m_nItemCount)
    {
        return m_pItem[Index].Context;
    }
    return NULL;
}

void *CXSimpleList2::GetItem(ULONG ID1, ULONG ID2)
{
    int Index = Found(ID1, ID2);
    if (Index < 0)
    {
        return NULL;
    }

    return GetItem(Index);
}

void *CXSimpleList2::GetItem(UINT64 ID)
{
    ULONG *p = (ULONG *) &ID;
    return GetItem(p[0], p[1]);
}

BOOL CXSimpleList2::GetID(int Index, ULONG &ID1, ULONG &ID2)
{
    CXAutoLock lock(m_lock);

    if (Index >= 0 && Index < m_nItemCount)
    {
        ID1 = m_pItem[Index].ID1;
        ID2 = m_pItem[Index].ID2;
        return TRUE;
    }

    return FALSE;
}

BOOL CXSimpleList2::GetID(int Index, UINT64 &ID)
{
    ULONG ID1 = 0, ID2 = 0;
    if (!GetID(Index, ID1, ID2))
    {
        return FALSE;
    }

    ULONG *p = (ULONG *) &ID;
    p[0] = ID1;
    p[1] = ID2;
    return TRUE;
}

int CXSimpleList2::GetID1Count(ULONG ID1)
{
    CXAutoLock lock(m_lock);

    int iCount = 0;
    for (int i = 0; i < m_nItemCount; i++)
    {
        if (ID1 == m_pItem[i].ID1)
        {
            iCount++;
        }
    }

    return iCount;
}

int CXSimpleList2::GetID2Count(ULONG ID2)
{
    CXAutoLock lock(m_lock);

    int iCount = 0;
    for (int i = 0; i < m_nItemCount; i++)
    {
        if (ID2 == m_pItem[i].ID2)
        {
            iCount++;
        }
    }

    return iCount;
}


CXSimpleList3::CXSimpleList3()
{
    m_lock = CXAutoLock::Create();

    m_pItem = NULL;
    m_iItemCount = 0;
    m_iItemSize = 0;
    m_iMaxItemCount = 0;
    m_iIdOffsetWithinItem = 0;
    m_iIdSize = 0;
}

CXSimpleList3::~CXSimpleList3()
{
    Destroy();

    CXAutoLock::Destroy(m_lock);
}

bool CXSimpleList3::Create(int iItemSize, int iMaxItemCount, int iIdOffsetWithinItem, int iIdSize)
{
    CXAutoLock lk(m_lock);

    // 之前已经创建过了
    if (NULL != m_pItem)
    {
        return false;
    }
    // 参数不合法
    if (iItemSize < 4 ||
        iMaxItemCount < 2 ||
        iIdOffsetWithinItem < 0 ||
        iIdSize < 1 ||
        (iIdOffsetWithinItem + iIdSize) > iItemSize)
    {
        return false;
    }

    // 分配内存失败
    m_pItem = (unsigned char *) malloc((size_t) (iItemSize * iMaxItemCount));
    if (NULL == m_pItem)
    {
        return false;
    }
    memset(m_pItem, 0, (size_t) (iItemSize * iMaxItemCount));

    // 成功
    m_iItemCount = 0;
    m_iItemSize = iItemSize;
    m_iMaxItemCount = iMaxItemCount;
    m_iIdOffsetWithinItem = iIdOffsetWithinItem;
    m_iIdSize = iIdSize;
    return true;
}

void CXSimpleList3::Destroy()
{
    CXAutoLock lk(m_lock);

    if (m_pItem)
    {
        free(m_pItem);
        m_pItem = NULL;
    }
    m_iItemCount = 0;
    m_iItemSize = 0;
    m_iMaxItemCount = 0;
    m_iIdOffsetWithinItem = 0;
    m_iIdSize = 0;
}

bool CXSimpleList3::IsValid()
{
    return (m_pItem != NULL);
}

int CXSimpleList3::Add(void *pNewItem)
{
    int iNewIndex = -1;
    if (m_pItem &&
        Find(((char *) pNewItem) + m_iIdOffsetWithinItem) < 0)
    {
        CXAutoLock lk(m_lock);
        if (m_iItemCount < m_iMaxItemCount)
        {
            iNewIndex = m_iItemCount;
            m_iItemCount++;
            memcpy(m_pItem + iNewIndex * m_iItemSize, pNewItem, (size_t) m_iItemSize);
        }
    }
    return iNewIndex;
}

int CXSimpleList3::Remove(int iIndex, bool bSort)
{
    CXAutoLock lk(m_lock);
    if (m_pItem && iIndex >= 0 && iIndex < m_iItemCount)
    {
        // bSort:
        // true  -- 维持原来的排列顺序
        // false -- 不维持原来的排列顺序，用最后一个Item取代被删除的这个
        if (bSort)
        {
            for (int i = iIndex; i < m_iItemCount - 1; i++)
            {
                memcpy(m_pItem + i * m_iItemSize, m_pItem + (i + 1) * m_iItemSize, (size_t) m_iItemSize);
            }
        }
        else
        {
            if (iIndex != (m_iItemCount - 1))
            {
                memcpy(m_pItem + iIndex * m_iItemSize, m_pItem + (m_iItemCount - 1) * m_iItemSize,
                       (size_t) m_iItemSize);
            }
        }
        m_iItemCount--;
    }
    return iIndex;
}

int CXSimpleList3::RemoveAll()
{
    CXAutoLock lk(m_lock);
    m_iItemCount = 0;
    return 1;
}

int CXSimpleList3::Find(void *IdPtr)
{
    int iIndex = -1;
    CXAutoLock lk(m_lock);
    if (m_pItem)
    {
        int i = 0;
        unsigned char *pIdWithinList = m_pItem + m_iIdOffsetWithinItem;
        for (i = 0; i < m_iItemCount; i++, pIdWithinList += m_iItemSize)
        {
            if (0 == memcmp(IdPtr, pIdWithinList, (size_t) m_iIdSize))
            {
                iIndex = i;
                break;
            }
        }
    }
    return iIndex;
}

void *CXSimpleList3::GetItem(int iIndex)
{
    void *pItem = NULL;
    CXAutoLock lk(m_lock);
    if (m_pItem && iIndex < m_iItemCount)
    {
        pItem = m_pItem + iIndex * m_iItemSize;
    }
    return pItem;
}

void *CXSimpleList3::GetItem(void *IdPtr)
{
    void *pItem = NULL;
    CXAutoLock lk(m_lock);
    if (m_pItem)
    {
        int i = 0;
        unsigned char *pIdWithinList = m_pItem + m_iIdOffsetWithinItem;
        for (i = 0; i < m_iItemCount; i++, pIdWithinList += m_iItemSize)
        {
            if (0 == memcmp(IdPtr, pIdWithinList, (size_t) m_iIdSize))
            {
                pItem = m_pItem + i * m_iItemSize;
                break;
            }
        }
    }
    return pItem;
}

int CXSimpleList3::GetItemCount()
{
    return m_iItemCount;
}
