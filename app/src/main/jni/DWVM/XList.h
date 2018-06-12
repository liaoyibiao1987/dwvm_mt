#ifndef _XLIST_H_20110806_
#define _XLIST_H_20110806_

#include <pthread.h>
#include "XDataType.h"

class CXAutoLock
{
public:
    CXAutoLock(HANDLE hMutex);

    ~CXAutoLock();

    static HANDLE Create();

    static void Destroy(HANDLE&hMutex);

protected:
    HANDLE m_hMutex;
};


class CXSimpleList
{
public:
    typedef struct _tagItemData
    {
        ULONG ID;
        void *Context;
    } tagItemData;

public:
    CXSimpleList();

    ~CXSimpleList();

    bool Init(int nMaxItemCount = 4000);

    void UnInit();

    bool IsInited()
    { return (m_pItem != NULL); }

    int Found(ULONG ID);

    int Add(ULONG ID, void *Context);

    void Remove(int Index, BOOL bSort = FALSE); // bSort: TRUE--维持原来的排列顺序，FALSE--不维持原来的排列顺序，用最后一个Item取代被删除的这个
    int Remove(ULONG ID, BOOL bSort = FALSE);

    void RemoveAll();

    int GetCount()
    { return m_nItemCount; }

    void *GetItem(int Index);

    void *GetItem(ULONG ID);

    void SetItem(int nIndex, void *Context);

    void SetItem(ULONG ID, void *Context);

    ULONG GetID(int Index);

protected:
    tagItemData *m_pItem;
    int m_nMaxItemCount;
    int m_nItemCount;
    HANDLE m_lock;
};


class CXSimpleFifo
{
public:
    typedef struct _tagItemData
    {
        void *pData;
        int nDataLen;
        long Context1;
        long Context2;
    } tagItemData;

public:
    CXSimpleFifo();

    ~CXSimpleFifo();

    bool Init(int nMaxItemSize = 4096, int nMaxItemCount = 200);

    void UnInit();

    bool IsInited()
    { return (m_pItem != NULL); }

    bool Push(void *pData, int nDataLen, long Context1 = 0, long Context2 = 0);

    bool Pop(void *pData, int nBufLen, int *pActualDataLen, long *pContext1 = NULL, long *pContext2 = NULL);

    void Reset();

    int GetCount();

protected:
    tagItemData *m_pItem;
    int m_nMaxItemSize;
    int m_nMaxItemCount;
    int m_nReadPos;
    int m_nWritePos;
    int m_nItemCount;
    HANDLE m_lock;
};


class CXSimpleChain
{
public:
    typedef struct _node
    {
        void *data;
        _node *prev;
        _node *next;
        unsigned long sizeAlloc;
        unsigned long sizeActual;
        unsigned long timestamp; // 时间戳，从1970-1-1至今的秒数，即函数time(NULL)的返回值
    } _node;

public:
    CXSimpleChain();

    ~CXSimpleChain();

    bool Init(int nAllocNodeCount, int nNodeDataSize);

    void UnInit();

    int GetChainLength();

    void AddToHead(_node *p);

    void AddToTail(_node *p);

    void InsertBefore(_node *exist, _node *p);

    void InsertAfter(_node *exist, _node *p);

    _node *RemoveFromHead();

    _node *RemoveFromTail();

    _node *Remove(_node *p);

    _node *PeekHead();

    _node *PeekTail();

    static _node *PeekPrev(_node *p);

    static _node *PeekNext(_node *p);

    static _node *AllocNode(int nAllocSize);

    static void FreeNode(_node *p);

protected:
    _node *m_first;
    _node *m_last;

    HANDLE m_lock;
};


class CXSimpleList2
{
public:
    typedef struct _tagItemData
    {
        ULONG ID1;
        ULONG ID2;
        void *Context;
    } tagItemData;

public:
    CXSimpleList2();

    ~CXSimpleList2();

    bool Init(int nMaxItemCount = 4000);

    void UnInit();

    bool IsInited()
    { return (m_pItem != NULL); }

    int Found(ULONG ID1, ULONG ID2);

    int Found(UINT64 ID);

    int FoundNext(ULONG ID1, int nIndex); // 从nIndex开始(不包括nIndex)，查找ID1
    int FoundNext2(ULONG ID2, int nIndex); // 从nIndex开始(不包括nIndex)，查找ID2
    int Add(ULONG ID1, ULONG ID2, void *Context);

    int Add(UINT64 ID, void *Context);

    void Remove(int Index, BOOL bSort = FALSE); // bSort: TRUE--维持原来的排列顺序，FALSE--不维持原来的排列顺序，用最后一个Item取代被删除的这个
    int Remove(ULONG ID1, ULONG ID2, BOOL bSort = FALSE);

    int Remove(UINT64 ID, BOOL bSort = FALSE);

    void RemoveAll();

    int GetCount()
    { return m_nItemCount; }

    int GetID1Count(ULONG ID1);

    int GetID2Count(ULONG ID2);

    void *GetItem(int Index);

    void *GetItem(ULONG ID1, ULONG ID2);

    void *GetItem(UINT64 ID);

    BOOL GetID(int Index, ULONG &ID1, ULONG &ID2);

    BOOL GetID(int Index, UINT64 &ID);

protected:
    tagItemData *m_pItem;
    int m_nMaxItemCount;
    int m_nItemCount;
    HANDLE m_lock;
};


//
// 类 CXSimpleList3，管理一个结构数据：
// 1、结构长度固定
// 2、结构内必须有一个成员是具有唯一ID的、可作为索引值用。该成员位置和长度不限
//
//
// 计算一个结构体内的某一个成员的偏移量
#define MEMBER_OFFSET(struct_name, member_name)    (int)(&(((##struct_name*)0)->##member_name))
// 计算结构体内某一个成员的长度
#define MEMBER_SIZE(struct_name, member_name)    sizeof(((##struct_name*)0)->##member_name)

//
class CXSimpleList3
{
public:
    CXSimpleList3();

    ~CXSimpleList3();

    bool Create(int iItemSize, int iMaxItemCount, int iIdOffsetWithinItem, int iIdSize);

    void Destroy();

    bool IsValid();

    int Add(void *pNewItem);

    int Remove(int iIndex, bool bSort);

    int RemoveAll();

    int Find(void *IdPtr);

    void *GetItem(int iIndex);

    void *GetItem(void *IdPtr);

    int GetItemCount();

protected:
    HANDLE m_lock;

    unsigned char *m_pItem;
    int m_iItemCount;
    int m_iItemSize;
    int m_iMaxItemCount;
    int m_iIdOffsetWithinItem;
    int m_iIdSize;
};


#define IFNOFREESPACE_RETURN    0
#define IFNOFREESPACE_ALLOCNEW  1
#define IFNOFREESPACE_DELETEOLD 2

#define RETVAL_xxx          int
#define RETVAL_OK           0 // 成功
#define RETVAL_NOINIT       1 // 尚未初始化 (还没有调用Init函数)
#define RETVAL_INVALIDPARAM 2 // 无效的参数
#define RETVAL_NOFREESPACE  3 // 没有空余的可用空间
#define RETVAL_ALLOCFAIL    4 // 分配内存失败
#define RETVAL_DATATOOLARGE 5 // push的数据太大 (数据不能大于1MByte)
#define RETVAL_BUFFTOOSMALL 6 // pop的接收缓冲区太小
#define RETVAL_NOCACHEDATA  7 // 无cache数据

class CXCacheFifo
{
public:
#pragma pack(1)
    typedef struct _tagNodeHeader
    {
        int size;
        unsigned long context;
        unsigned long seqnum;
        unsigned long timestamp; // 秒
        unsigned char marker;
    } tagNodeHeader;
#pragma pack()

public:
    CXCacheFifo();

    ~CXCacheFifo();

    bool Init(int nPacketCount = 0, int nPacketSize = 1024);

    void UnInit();

    bool IsInited()
    { return (m_nPacketSize > 0); }

    RETVAL_xxx Push(void *pData, int nDataSize, unsigned long Context = 0, int nIfNoFreeSpace = IFNOFREESPACE_RETURN);

    RETVAL_xxx Pop(void *pDestBuffer, int nBufferSize, int *pActualSize, unsigned long *pContext = NULL);

    RETVAL_xxx Peek(int *pActualSize, unsigned long *pContext = NULL)
    { return Pop(NULL, 0, pActualSize, pContext); }

    RETVAL_xxx RemoveHead();

    unsigned long GetHeadTimestamp();

    int GetTotalSize();

    int GetIdleSize();

    void ClearCache();

protected:
    CXSimpleChain m_chainUsed;
    CXSimpleChain m_chainIdle;
    int m_nPacketSize;
    unsigned long m_nSeqnum;

    HANDLE m_lock;
};


class CXBufferFifo
{
public:
    typedef struct _tagItemData
    {
        void *pAllocBuffer;
        int nAllocBufferSize;
        void *pBufferAlignPtr;
        int nValidDataLen;
        void *pContext;
        unsigned long dwContext;
    } tagItemData;

public:
    CXBufferFifo();

    ~CXBufferFifo();

    bool Init(int nMaxItemCount, int nAlignBytes = 16);

    void UnInit();

    bool IsInited()
    { return (m_pItem != NULL); }

    bool Push(void *pData, int nDataLen, void *pContext1 = NULL, unsigned long dwContext2 = 0);

    bool Peek(void **ppData, int *pActualDataLen, void **ppContext1 = NULL, unsigned long *pdwContext2 = NULL);

    bool Pop(BOOL bFreeBuffer);

    void Reset();

    int GetCount();

protected:
    tagItemData *m_pItem;
    int m_nMaxItemCount;
    int m_nReadPos;
    int m_nWritePos;
    int m_nItemCount;
    int m_nAlignBytes;
    HANDLE m_lock;
};


#endif // _XLIST_H_20110806_
