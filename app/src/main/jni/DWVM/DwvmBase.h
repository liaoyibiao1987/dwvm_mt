#ifndef _DWVM_BASE_H_20110808_
#define _DWVM_BASE_H_20110808_

#include <jni.h>
#include "XDataType.h"
#include "XLib.h"
#include "XList.h"
#include "XLog.h"
#include "xsocket.h"
#include "dwvm_global.h"
#include "MyEvent.h"
#include "svStreamDef2.h"


// 本dll最多允许创建256个socket.
#define MAX_SOCKET_NUMBER    (256)

// socket buffer 的最大尺寸
#define MAX_SOCKET_BUFFER_SIZE    (20*1024*1024)

// 最多允许创建多少个CTcpSenderOverUdp
#define MAX_TCP_TUNNEL_OVER_UDP    (1024)

// 统计最近若干个数据包的应答时间
#define STAT_ACK_PKT_NUM    (10)

// 最多允许创建多少个网络回调线程
#define MAX_CB_THREAD_NUMBER    (10000)
// 默认的每个回调线程的堆栈字节数
#define DEFAULT_CB_THREAD_STACK_SIZE    (64*1024)

// 网络P2P测试结果
//
// 测试P2P网络速度、响应时间等。分为3个步骤进行（假设用户设定测试时间为11秒）：
// 1) 每10ms发送一个大小为WVM_MTU的、要求对方应答的数据包，持续1秒钟
//    计算所有返回的应答包的平均响应时间
//    *** 可以得到：对方是否在线、网络空闲情况下的响应时间
// 2) 发送数据包、等待应答或者超时。。。重复这个过程，持续5秒
//    以步骤1的平均响应时间的150%为超时时间，并在过程中随时调整平均响应时间
//    *** 可以得到：正常情况下的响应时间、丢包率、网络带宽
// 3) 压力测试。以步骤1的响应时间的30%为间隔，不断发送数据包，持续5秒
//    *** 可以得到：压力模式下的响应时间、丢包率、最大网络带宽
//
typedef struct
{
    DWORD dwSize;            // IN  本结构的尺寸
    DWORD dwDestDeviceId;    // OUT 目标设备的ID
    DWORD dwDestIp;        // OUT 目标设备的IP地址
    WORD wDestPort;        // OUT 目标设备的网络端口
    WORD wStep;            // OUT 目前的测试步骤. 0表示测试完成了，1-3表示正在测试中
    WORD wTestSecond;    // OUT 设定的测试总时间，单位：秒
    WORD wReserve;        // 保留参数，目前等于0
    struct T_NET_STAT        // OUT 3个步骤各自的统计情况
    {
        int iPktSendCnt;
        //   总共发送的网络包数量
        int iPktAckCnt;    //   收到的网络应答包的数量
        int iPktSize;    //   每个网络包的大小，单位：字节
        double dbResponse;    //   平均响应时间，单位：毫秒
        double dbRunTime;    //   该步骤已经运行了多久，单位：毫秒
    } Stat[3];
    DWORD dwReserve[4];    // 保留参数，目前都等于0
} T_DWVM_TEST_NET_RESULT;

// 网络视频、音频传输的即时统计信息
//
typedef struct
{
    DWORD dwStatTimeMs;    // 统计时间长度，毫秒
    DWORD dwDelayTimeMs;    // 平均延迟时间，毫秒
    DWORD dwSendPackets;    // 发送方发送的包数量
    DWORD dwSendBytes;    // 发送发发生的字节数
    DWORD dwRecvPackets;    // 接收到的包数量
    DWORD dwRecvBytes;    // 接收到的字节数
    DWORD dwRecvFrames;    // 接收到的帧数
    DWORD dwReserve[7];    // 保留参数，目前等于0
    T_WVM_VA_REPLY Detail;    // 详细信息
} T_DWVM_NET_REALTIME_STATUS;

//
// 简单的加密、解密
//
int DWVM_Encrypt(unsigned char *pData, int iLen);

int DWVM_Decrypt(unsigned char *pData, int iLen);

//
// 字符串加密、解密，主要用于用户名和用户密码
//
#define DWVM_StringEncrypt(sz)    DWVM_Encrypt((unsigned char*)(sz), strlen(sz))
#define DWVM_StringDecrypt(sz)    DWVM_Decrypt((unsigned char*)(sz), strlen(sz))

//
// 网络
//
//
//==== 收到网络数据包后的回调函数
//==== 网络包已经经过了预先处理：
//==== 1、验证了数据的合法性（包头标志、包长度、校验码）。
//==== 2、如果是加密包，预先解密好。是针对整包的解密，并不会解析并解密用户名和用户密码
//==== 3、如果发送端要求应答，回复应答包。如果数据包的目标ID与本设备不同，不能回复应答包
//
typedef struct
{
    int todo; // 0-NOT do any, 1-get vm, 2-free vm
    JavaVM *javaVM;
    JNIEnv *javaEnv;
    jobject javaObj;
    jclass javaMTLib;
    jmethodID javaMTLibCallbackMethod;
} T_DWVM_JNI_ENV;

typedef int (*pfnDwvmNetReceive)(    // 返回值：必须返回0
    T_DWVM_JNI_ENV *javaTodo, // to do something for android-jni-java env
    SOCKET s,            // socket
    void *pCbContext,    // 回调上下文参数，对应于 DWVM_CreateNetThread() 函数的 pCbContext 参数
    DWORD dwFromIp,        // 网络包的来源IP
    WORD wFromPort,        // 网络包的来源PORT
    DWORD dwCmd,        // 网络包的命令类型
    void *pData,        // 网络包buffer
    int iLen);            // 网络包长度
//
//==== 创建网络相关资源
//
SOCKET DWVM_CreateNetThread(    // 返回值：被创建的socket. 失败返回INVALID_SOCKET (-1)
    DWORD dwLocalDeviceId,            // 本设备的ID
    WORD wLocalPort,                // 本地端口号
    DWORD dwBindLocalIp,            // 绑定本机的IP地址
    int iSocketBufferSize,            // socket buffer尺寸. 小于等于0时，用默认尺寸 WVM_DEFAULT_SOCKET_BUFFER_SIZE
    pfnDwvmNetReceive pfnCbProc,    // 接收网络包的回调函数指针
    void *pCbContext,                // 网络包回调函数的上下文参数. 会在回调函数 pfnCbProc 的 pCbContext 参数中原样返回
    DWORD dwCbThreadNumber,            // 网络包回调线程的数量。<=1 表示用单个线程回调，>1 表示用多个线程回调 (最大数量为 MAX_CB_THREAD_NUMBER 10000)
    DWORD dwCbThreadStackSize);        // 网络包回调线程的堆栈尺寸，单位：字节。0 表示使用进程默认尺寸
//
//==== 释放网络资源
//
void DWVM_DestroyNetThread(SOCKET s);

//
//==== 设置、获取网络回调函数的上下文参数
//
BOOL DWVM_SetNetContext(SOCKET s, void *pCbContext);

void *DWVM_GetNetContext(SOCKET s);

//
//==== 设置、获取本设备的ID
//
BOOL DWVM_SetLocalDeviceId(SOCKET s, DWORD dwLocalDeviceId);

DWORD DWVM_GetLocalDeviceId(SOCKET s);

//
//==== 发送网络包
//
DWORD DWVM_SendNetPacket(    // 返回值：该数据包的序号。0表示失败
    BOOL bInner,            // 是否组件内部包
    SOCKET s,                // socket
    DWORD dwCmd,            // 网络包的类型: WVM_CMD_xxx
    DWORD dwNeedReply,        // 是否需要接收方回复应答包。 0为不需要，其它值为需要
    DWORD dwDestDeviceId,    // 目标设备的ID
    DWORD dwDestIp,            // 目标IP地址
    WORD wDestPort,            // 目标端口
    void *pData,            // 网络包buffer
    int iDataSize,            // 网络包长度
    DWORD dwEncrypt);        // 加密模式，0为不加密。最高4bit表示加密模式，0表示无加密。其他28bit为加密因子
DWORD DWVM_SendNetPacketEx(    // 返回值：该数据包的序号。0表示失败
    BOOL bInner,            // 是否组件内部包
    SOCKET s,                // socket
    DWORD dwCmd,            // 网络包的类型: WVM_CMD_xxx
    DWORD dwNeedReply,        // 是否需要接收方回复应答包。 0为不需要，其它值为需要
    DWORD dwDestDeviceId,    // 目标设备的ID
    DWORD dwDestIp,            // 目标IP地址
    WORD wDestPort,            // 目标端口
    void *pData,            // 网络包buffer
    int iDataSize,            // 网络包长度
    DWORD dwEncrypt,        // 加密模式，0为不加密。最高4bit表示加密模式，0表示无加密。其他28bit为加密因子
    DWORD dwSrcDeviceId);    // 源设备ID
//
//==== 测试本机和指定设备之间的P2P网络速度、响应时间等
//
BOOL DWVM_StartTestNet(    // 开始测试。这个函数是异步工作，函数会立即返回。
    SOCKET s,                // socket
    DWORD dwDestDeviceId,    // 目标设备的ID
    DWORD dwDestIp,            // 目标IP地址
    WORD wDestPort,            // 目标端口
    WORD wTestSecond);        // 测试时间，单位：秒。取值范围：5-60秒
BOOL DWVM_StopTestNet(    // 停止测试
    SOCKET s);                // socket
BOOL DWVM_GetTestNetResult(    // 获知测试结果
    SOCKET s,                // socket
    T_DWVM_TEST_NET_RESULT *pResult);    // OUT pResult->dwSize必须在调用前设置为结构长度。详见T_DWVM_TEST_NET_RESULT的说明。
BOOL DWVM_GetTestNetResultString(    // 获知测试结果。文本格式
    SOCKET s,                // socket
    char *pszResult);        // OUT 获取测试结果的文字描述

//
// 音频、视频帧的组包类
// 这个类被 CFrameReceiver 再次封装了，外部不需要调用这个类，直接调用 CFrameReceiver 即可
//
class CRestructPacket
{
public:
    CRestructPacket();

    virtual ~CRestructPacket();

    BOOL Create(int nFramePacketCount = 200, int nTimeout = 2000, int nCachePacketCount = 0); // nTimeout: 毫秒
    void Destroy();

    void ClearFrameChain(); // 清空所有的缓冲数据

    // 获取、设定目前的超时时间，毫秒
    __inline ULONG GetTimeout()
    { return m_timeout; }

    __inline void SetTimeout(ULONG nTimeout)
    { m_timeout = nTimeout; }

    // 输入以T_WVM_VA_BLOCK_HEADER为头的小包
    BOOL Push(void *pPacket, int nPacketLen, CXSimpleChain::_node **ppNodNew = NULL);

    // 获取组合好的一帧，以tagSVStreamHeader2为帧头
    BOOL Pop(void *pFrame, int nBufferSize, int *pActualSize, bool *pIsContinue); // pIsContinue: 返回值, 这帧与上一帧之间的序号是否为连续的
    // 读取指定序号的数据包
    BOOL GetCachePacket(ULONG nPacketSeq, void *pPacket, int nPacketSize, int *nActualSize);

    // 获得所有丢失包的包序号(pkt_seq)
    void GetLostPackets(ULONG *pSeqArray, int *pLostCount, int nArraySize, int *pRecvCount, ULONG dwTimeoutMs);

    void GetLostPackets_All(ULONG *pSeqArray, int *pLostCount, int nArraySize, int *pRecvCount, ULONG dwTimeoutMs);

    void GetLostPacketsInFrame(CXSimpleChain::_node *nodNew, ULONG *pLostSeqInFrame, int *pLostCount, int nArraySize,
                               ULONG dwTimeoutMs);

    // 如果第1帧不完整、但是第2帧已经完整了，就删除第1帧
    void DeleteFirstCrashIfSecondWhole();

    // 打印缓存buffer内所有包的序号
    void PrintChainSeq(CXSimpleChain *chain, char *Symbol);

    // 获知缓存buffer内的包数量
    __inline int GetFrameChainLen()
    { return m_chainFrame.GetChainLength(); }

    // 获知最老一个包的时间戳
    DWORD GetFirstPacketTime();

protected:
    // 判断从nodStart开始，是不是一个完成的帧
    BOOL IsWholeFrame(CXSimpleChain::_node *nodStart);

    // 从frame链表头移出一帧数据. 如果pBuffer不等于NULL，就将数据复制到pBuffer中
    // 返回值为被移出的有效数据的长度
    int RemoveOneFrame(void *pDestBuffer = NULL, int nBufferSize = 0, ULONG *pLastPktSeq = NULL);

    // 回收数据包：先尝试放入cache，如果失败的话再放入idle
    void RecyclePacket(CXSimpleChain::_node *nod);

protected:
    CXSimpleChain m_chainFrame;
    CXSimpleChain m_chainIdle;

    ULONG m_timeout;
    ULONG m_nLastFrameSeq;
    ULONG m_nLastPktSeq;

    // 考虑 Push() 的特殊情况：断线重连后的seq会从1开始计数.
    ULONG m_nContinueErrPktCount;
    ULONG m_seqPrevErrPkt;

    BOOL m_bWaitAllPacket;
    ULONG m_nPopPacketCount;

    CXSimpleChain::_node **m_ppCache;
    ULONG m_nCacheLen;

#ifndef _WIN32_WCE
    HANDLE m_lock;
#endif
};

//
// 缓存视频、音频小包
// 可以指定缓存的时间
// 这个类主要为 CFrameSender 服务
//
class CTimingCache
{
public:
    CTimingCache(int iCacheSecond); // 指定缓存多少秒钟的数据
    ~CTimingCache();

    // 当前设定的缓存最大秒数
    __inline int GetMaxCacheSecond()
    {
        return m_iCacheSecond;
    }

    // 当前缓存的最老一帧数据的时间戳（从1970-1-1至今的秒数）
    // 返回0表示没有缓存任何数据
    __inline DWORD GetFirstBlockTimestamp()
    {
        CXSimpleChain::_node *pFirst = m_data.PeekHead();
        return (pFirst == NULL) ? (0) : (pFirst->timestamp);
    }

    // 把数据添加到缓存中
    BOOL Add(
        void *pData,    // 格式为 T_WVM_VA_FRAME_HEADER + T_WVM_VA_BLOCK_HEADER + frame_data
        int iSize);    // 数据长度

    // 从缓存中读取一个指定序号的小包
    int Get(            // 返回值：0表示读取失败，>0 表示读取到的数据长度
        DWORD pkt_seq,    // 希望读取的包序号。即命令码 WVM_CMD_PS_VA_RESEND 对应的 T_WVM_VA_BLOCK_HEADER::pkt_seq
        void *pData);    // OUT 接受数据的buffer，尺寸必须大于或者等于 WVM_MTU

    // 清除所有缓存数据
    // 如果iCacheSecond不等于0，还会从新设定缓存秒数
    BOOL Reset(int iCacheSecond = 0);

    // 统计
    int Stat(T_WVM_VA_REPLY *pStat);

protected:
    int m_iCacheSecond;
    CXSimpleChain m_idle;
    CXSimpleChain m_data;
};

//
// 视频、音频帧的发送类
// 把一帧拆分为多个小包发送，并缓存(以备网络丢包后、接收方要求重发)
// 每个类仅仅处理一路数据流
//
class CFrameSender
{
public:
    CFrameSender(int iCacheSecond); // 缓存多少秒钟的数据，范围0-60，为0时不缓存数据
    ~CFrameSender();

    // 发送一帧。
    // 函数内部会拆分为多个小包发送，小包的命令码为 WVM_CMD_PS_VA_FRAME
    // 结构为：T_WVM_PACKET_HEADER + T_WVM_VA_FRAME_HEADER + T_WVM_VA_BLOCK_HEADER + frame_data
    BOOL Send(
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
        void *pFrameBuffer);                // tagSVStreamHeader2为头的frame_data

    // 处理接收方的重发请求（即命令 WVM_CMD_PS_VA_RESEND）
    BOOL Resend(
        SOCKET s,
        DWORD dwNetEncryptMode,
        DWORD dwSrcDeviceId,
        DWORD dwSrcIp,
        WORD wSrcPort,
        T_WVM_VA_RESEND *pResend);

    // 处理接收方的统计请求（即命令 WVM_CMD_PS_VA_POLLING）
    BOOL Reply(
        SOCKET s,
        DWORD dwNetEncryptMode,
        DWORD dwSrcDeviceId,
        DWORD dwSrcIp,
        WORD wSrcPort,
        T_WVM_VA_POLLING *pPolling);

    // 获取一些状态
    __inline DWORD GetLastFrameTime()
    { return m_dwLastFrameTime; }

protected:
    CTimingCache m_cache;
    int m_iCacheSecond;
    DWORD m_dwFrameCount;
    DWORD m_dwBlockCount;
    DWORD m_dwLastFrameTime;
    HANDLE m_hLock;
};

//
// 视频、音频帧的接收类
// 把多个小包组合成一个完整帧，并判断是否网络丢包、请求发送方重发
// 每个类仅仅处理一路数据流
//
class CFrameReceiver
{
public:
    CFrameReceiver();

    ~CFrameReceiver();

    // 组包（即处理命令 WVM_CMD_PS_VA_FRAME）
    // 函数内部会判断是否丢包，并会请求发送端重新发送
    BOOL Push(
        T_WVM_VA_FRAME_HEADER *pFrame,    // IN  收到的音频视频小包: T_WVM_VA_FRAME_HEADER + T_WVM_VA_BLOCK_HEADER + frame_data
        SOCKET s,                        // IN  socket
        DWORD dwSrcDeviceId,            // IN  发送方的ID
        DWORD dwSrcIp,                    // IN  发送方的ip
        WORD wSrcPort,                // IN  发送方的port
        DWORD dwDestDeviceId,            // IN  接收方的ID，即自己的ID(发送重发请求时要用到)
        int *pLostSeqCount);            // OUT 丢包数

    // 取出组合后的完整帧
    // 用户应当一直调用这个函数，取空所有帧，直到返回FALSE
    BOOL Pop(
        void **ppFramePtr,    // OUT 以tagSVStreamHeader2为头的frame_data的指针
        int *pSizePtr,    // OUT 帧长度
        DWORD *pTypePtr,    // OUT 帧类型: WVM_FRAMETYPE_VIDEO_I, WVM_FRAMETYPE_VIDEO_P, WVM_FRAMETYPE_AUDIO
        DWORD *pImageRes);    // OUT 视频帧的图像分辨率：WVM_IMAGERES_CIF, WVM_IMAGERES_D1, WVM_IMAGERES_QCIF

    // 处理发送端回馈的统计信息
    BOOL OnReply(T_WVM_VA_REPLY *pReply);

    // 清除缓存
    void ClearCache();

    // 设定发送“重发请求包”的时间间隔，单位：毫秒。默认为 LAN:100ms, WAN:1000ms
    __inline void SetResendInterlace(DWORD dwNew)
    { m_dwResendInterlaceMs = dwNew; }

    __inline DWORD GetResendInterlace()
    { return m_dwResendInterlaceMs; }

    // 获取网络传输的实时状态
    void GetRealtimeStatus(T_DWVM_NET_REALTIME_STATUS *pStatus);

    // 获取一些状态
    __inline DWORD GetLastPktTime()
    { return m_dwLastPktTime; }

    __inline DWORD GetLastFrameTime()
    { return m_dwLastFrameTime; }

    __inline DWORD GetLastSrcIp()
    { return m_dwLastSrcIp; }

    __inline WORD GetLastSrcPort()
    { return m_wLastSrcPort; }

    __inline T_WVM_VA_FRAME_HEADER *GetLastFrameHeader()
    { return &m_LastPushFrame; }

    BOOL GetStatusString(char *pszStatus);

    DWORD GetCachePktNumber();  // 获取缓存中的小包数
    DWORD GetCacheDataTimeMs(); // 获取缓存中的毫秒数

protected:
    CRestructPacket *m_pRestruct;        // 组包类
    BYTE *m_pFrameBuffer;        // 组包buffer，存放组合好的帧数据
    int m_iFrameBufferSize;    // m_pFrameBuffer的总长度
    int m_iFrameValidSize;    // m_pFrameBuffer内的有效数据长度
    HANDLE m_hLock;
    DWORD m_dwResendInterlaceMs;    // 发送“重发请求包”的时间间隔，单位：毫秒
    DWORD m_dwResendPrevMs;        // 上一次发送“重发请求包”的时间点，单位：毫秒
    T_WVM_VA_FRAME_HEADER m_LastPushFrame;    // 最后一次push的帧头
    DWORD m_dwLastSrcID;        // 最后一次push时的设备ID
    DWORD m_dwLastSrcIp;        // 最后一次push时的ip
    WORD m_wLastSrcPort;        // 最后一次push时的port
    DWORD m_dwLastPktTime;    // 最后一次push时的时间点
    DWORD m_dwLastFrameTime;    // 最后POP出的有效帧
    //
    // 实时统计信息
    //
    T_WVM_VA_POLLING m_Polling;            // 发送给服务器的polling包
    T_DWVM_NET_REALTIME_STATUS m_RealtimeStatus;    // 统计的最终结果
};

//
// 帧率控制
//
class CFrameRateControl
{
public:
    CFrameRateControl();

    ~CFrameRateControl();

// dwHopeFrameRate: 希望的帧率
// dwStatInterlaceTime: 多久统计一次帧率
    BOOL Setup(DWORD dwHopeFrameRate, DWORD dwStatInterlaceTime = 5000);

// 判断该帧是否需要被跳过。TRUE-跳过，FALSE-不跳过
    BOOL InputOneFrame(BOOL *pbFramerateChanged = NULL);

// 从新开始统计
    void Reset();

// 获得input的帧率
    double GetInputFrameRate()
    { return m_dbFrameRate; }

protected:
    void OnInputFramerateChanged();

protected:
    DWORD m_dwStatInterlaceTime;
    DWORD m_dwStartTick;
    DWORD m_dwFrameCount;
    double m_dbFrameRate;
    DWORD m_dwHopeFrameRate;
    BYTE m_skip_Curr;
    BYTE m_skip_Count; // 1 -- 30
    BYTE m_skip_Mask[30];
};

//
// 在UDP基础上，模拟出TCP的发送效果 (等待前一个包到达后，再发送下一个包)
//
class CTcpSenderOverUdp
{
public:
    CTcpSenderOverUdp();

    virtual ~CTcpSenderOverUdp();

    // 更新参数
    BOOL Setup(SOCKET s, DWORD dwDestDeviceId, DWORD dwDestIp, WORD wDestPort, int iContextSize = 0);

    // 阻塞发送。等待包到达后、或者超时后才返回
    BOOL SendAndWait(
        DWORD dwCmd,        // 网络包的类型: WVM_CMD_xxx
        void *pData,        // 网络包buffer
        int iDataSize,        // 网络包长度
        DWORD dwEncrypt);    // 加密模式，0为不加密。最高4bit表示加密模式，0表示无加密。其他28bit为加密因子

    // 异步发送：发送和等待分开
    BOOL AsyncSend(
        DWORD dwCmd,        // 网络包的类型: WVM_CMD_xxx
        void *pData,        // 网络包buffer
        int iDataSize,        // 网络包长度
        DWORD dwEncrypt);    // 加密模式，0为不加密。最高4bit表示加密模式，0表示无加密。其他28bit为加密因子
    int AsyncCheckAndResend(DWORD dwWaitMs); // 返回值：0 正在发送中, 1 发送完成, -1 超时, -2 发送失败
    void AsyncCleanSend();

    // 处理应答包. 由dll内部的 CNet 收到应答包后自动调用, 外部不需要调用这个函数
    BOOL CheckAck(T_WVM_REPLY *pAck);

    __inline BOOL IsValid()
    { return (m_sock != INVALID_SOCKET && m_dwDestIp != 0 && m_wDestPort != 0); }

    __inline BOOL IsSending()
    { return (0 != m_dwSendingPktSeq); }

    __inline SOCKET GetSocket()
    { return m_sock; }

    __inline CMyEvent *GetAckEvent()
    { return &m_AckEvent; }

    __inline DWORD GetDestDeviceId()
    { return m_dwDestDeviceId; }

    __inline DWORD GetDestIp()
    { return m_dwDestIp; }

    __inline WORD GetDestPort()
    { return m_wDestPort; }

    __inline DWORD GetLastPktTimeMs()
    { return m_dwLastPktTimeMs; }

    __inline DWORD GetAvgAckTimeMs()
    { return m_dwAvgAckTimeMs; }

    __inline DWORD GetMaxAckTimeMs()
    { return m_dwMaxAckTimeMs; }

    __inline void *GetContextBuffer()
    { return m_pContextBuffer; }

    __inline int GetContextSize()
    { return m_iContextSize; }

protected:
    SOCKET m_sock;
    DWORD m_dwDestDeviceId;
    DWORD m_dwDestIp;
    WORD m_wDestPort;
    void *m_pContextBuffer;    // 用户私有空间
    int m_iContextSize;        // 用户私有空间的大小
    CMyEvent m_AckEvent;        // 应答事件。收到了应答包时激活
    DWORD m_dwAvgAckTimeMs;    // 平均的回馈时间（毫秒）
    DWORD m_dwMaxAckTimeMs;    // 最长的回馈时间（毫秒）
    DWORD m_dwSendingPktSeq;    // 正在发送的包序号
    DWORD m_dwLastPktTimeMs;    // 最后一个包的发送时刻，毫秒
    DWORD m_dwStatAckTime[STAT_ACK_PKT_NUM]; // 保存最近若干个数据包的应答时间
    int m_iAckPktNumber;
    int m_iLastPktResendNumber;
    BYTE m_LastPktData[WVM_MTU];
    int m_iLastPktDataSize;
    DWORD m_dwLastPktCmd;
    DWORD m_dwLastPktEncrypt;
};


//
// 大客户数据的发送和接收
//
//==== 回调函数：发送端：发送完毕
typedef int (*pfnDwvmBigCustomDataSendFinished)(
    void *pContext,        // 构建 CBigCustomDataSender::CBigCustomDataSender() 的 cbContext 参数
    BOOL bSucceed,        // 是否发送成功
    void *pData,        // 被发送的buffer
    int iDataLen,        // 被发送的buffer长度
    DWORD dwCustomID,    // 原样返回 CBigCustomDataSender::Push() 时的 dwCustomID 参数
    DWORD dwDurationMs);// 发送这个数据包花费的时间，毫秒
//==== 发送类
#define MULTI_SENDING    (10)

class CBigCustomDataSender
{
public:
    CBigCustomDataSender();

    ~CBigCustomDataSender();

    void Setup(pfnDwvmBigCustomDataSendFinished cbProc, void *cbContext, int iMaxBufferNumber = 3);

    BOOL Push(void *pData, int iDataLen, DWORD dwCustomID, SOCKET sock, DWORD dwDestDeviceId, DWORD dwDestIp,
              WORD wDestPort);

    static THREAD_PROC(Thread_Sender, lp)
    {
        ((CBigCustomDataSender *) lp)->OnSenderThread();
        return NULL;
    }

    void OnSenderThread();

protected:
    pfnDwvmBigCustomDataSendFinished m_cbProc;
    void *m_cbContext;
    CTcpSenderOverUdp m_tcp[MULTI_SENDING];
    THREAD_HANDLE m_hThread;
    BOOL m_bToExit;
    SOCKET m_sock;
    DWORD m_dwDestDeviceId;
    DWORD m_dwDestIp;
    WORD m_wDestPort;
    CXBufferFifo m_fifo;
};

//==== 回调函数：接收端：收到远端大数据包
typedef int (*pfnDwvmBigCustomDataReceived)(
    void *pContext,        // 构建 CBigCustomDataSender::CBigCustomDataSender() 的 cbContext 参数
    void *pData,        // 收到的数据
    int iDataLen,        // 收到的数据长度
    DWORD dwCustomID,    // 发送端定义的 dwCustomID 参数
    DWORD dwFromIp,        // 来源IP地址
    WORD wFromPort,        // 来源端口号
    DWORD dwSrcDeviceId);

// 来源设备ID
//==== 接收类
class CBigCustomDataReceiver
{
public:
    CBigCustomDataReceiver();

    ~CBigCustomDataReceiver();

    void Setup(pfnDwvmBigCustomDataReceived cbProc, void *cbContext);

    // 收到WVM_CMD_BIG_CUSTOM_DATA命令后，调用这个函数处理
    BOOL PushPkt(DWORD dwFromIp, WORD wFromPort, DWORD dwSrcDeviceId, void *pPkt, int iPktSize);

protected:
    pfnDwvmBigCustomDataReceived m_cbProc;
    void *m_cbContext;
    int m_iBigBufferSize;
    int m_iValidDataLen;
    BYTE *m_pBigBuffer;
    DWORD m_dwCurrFrameSeq;
};


#endif // _DWVM_BASE_H_20110808_
