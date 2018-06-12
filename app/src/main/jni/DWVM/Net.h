// Net.h: interface for the CNet class.
//
//////////////////////////////////////////////////////////////////////

#if !defined(AFX_NET_H__9FA27A91_19EC_4FF1_9691_9D3853BCF693__INCLUDED_)
#define AFX_NET_H__9FA27A91_19EC_4FF1_9691_9D3853BCF693__INCLUDED_

#include "XDataType.h"
#include "DwvmBase.h"
#include "XLib.h"
#include "xsocket.h"

class CNet
{
public:
    CNet();

    virtual ~CNet();

    //
    // 创建
    //
    BOOL Create(
        DWORD dwLocalDeviceId,            // 本设备的ID
        WORD wLocalPort,                // 本地端口号
        DWORD dwBindLocalIp,            // 绑定本机的IP地址
        int iSocketBufferSize,            // socket buffer尺寸. 小于等于0时，用默认尺寸 WVM_DEFAULT_SOCKET_BUFFER_SIZE (256000)
        pfnDwvmNetReceive pfnCbProc,    // 接收网络包的回调函数指针
        void *pCbContext,                // 网络包回调函数的上下文参数. 会在回调函数 pfnCbProc 的 pCbContext 参数中原样返回
        DWORD dwCbThreadNumber,            // 网络包回调线程的数量。<=1 表示用单个线程回调，>1 表示用多个线程回调
        DWORD dwCbThreadStackSize);        // 网络包回调线程的堆栈尺寸，单位：字节。0 表示使用进程默认尺寸

    //
    // 释放
    //
    void Destroy();

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
    // 需要注意的是：
    // 1) 测试过程应该为异步模式，不能死循环、导致正常的网络包不能被处理
    //    不能用for(;;)、while()、Sleep()等，否则容易导致阻塞
    // 2) 测试过程不能用GetTickCount()，这个函数的最小精确度只能到10ms，会影响测试结果
    //    可以用QueryPerformanceCounter()获取CPU TICK数来代替
    //    但需要注意的是，多核CPU返回的CPU TICK数是不同的，如果线程在多个CPU上切换会导致误差
    //    需要用SetThreadAffinityMask()限制测试线程运行在一个CPU上
    //
    BOOL StartTest(DWORD dwDestDeviceId, DWORD dwIp, WORD wPort, WORD TestSecond);

    void StopTest();

    BOOL GetTestStatus(T_DWVM_TEST_NET_RESULT *pResult);

    __inline void SetLocalDeviceId(DWORD dwId)
    { m_dwLocalDeviceId = dwId; };

    __inline DWORD GetLocalDeviceId()
    { return m_dwLocalDeviceId; }

    __inline SOCKET GetSocket()
    { return m_sock; }

    __inline WORD GetLocalPort()
    { return m_wLocalPort; }

    __inline int GetSocketBufferSize()
    { return m_iSocketBufferSize; }

    __inline void SetContext(void *pContext)
    { m_pCbContext = pContext; }

    __inline void *GetContext()
    { return m_pCbContext; }

    DWORD Send(BOOL bInner, DWORD dwSrcDeviceId, DWORD dwCmd, DWORD dwNeedReply, DWORD dwDestDeviceId, DWORD dwDestIp,
               WORD wDestPort, void *pData, int iDataSize, DWORD dwEncrypt, DWORD dwPktSeq);

    static THREAD_PROC(Thread_Receiver, p)
    {
        ((CNet *) p)->OnRecvThread();
        return NULL;
    }

    void OnRecvThread();

    static THREAD_PROC(Thread_Callback, p)
    {
        ((CNet *) p)->OnCallbackThread();
        return NULL;
    }

    void OnCallbackThread();

    static THREAD_PROC(Thread_Sender, p)
    {
        ((CNet *) p)->OnSendThread();
        return NULL;
    }

    void OnSendThread();

protected:
    // 处理网络测速过程
    void Test_OnSendThread(BYTE *pTestBuffer);

    void Test_OnRecvThread(T_WVM_REPLY *pr);
    // CPU的精确时间(毫秒)
    __inline double GetCpuMs()
    { return get_current_ms(); }

protected:
    DWORD m_dwLocalDeviceId;
    SOCKET m_sock;
    WORD m_wLocalPort;
    int m_iSocketBufferSize;
    pfnDwvmNetReceive m_pfnCbProc;
    void *m_pCbContext;
    THREAD_HANDLE m_hRecvThread;
    CXSimpleFifo m_RecvFifo;
    THREAD_HANDLE m_hSendThread;
    CXSimpleFifo m_SendFifo;
    // 多线程回调
    DWORD m_dwCbThreadNumber;
    DWORD m_dwCbThreadStackSize;
    THREAD_HANDLE m_hCbThread[MAX_CB_THREAD_NUMBER];
    LONG m_lCbThreadWorkingCnt;
    LONG m_lCbTotalCnt;
    // 网络测试
    BOOL m_bTestMode; // 是否进入测试模式
    double m_dbTestBeginTime[3]; // 3个步骤的开始时间
    double m_dbStepNeedTime; // 目前这个步骤需要持续的时间
    double m_dbSendTick; // 上一次发送测试包的时间
    DWORD m_dwSendPktSeq; // 上一次发送的测试包的序号
    double m_dbSendElapse; // 发送测试包的时间间隔
    DWORD m_dwRecvAckSeq; // 收到的应答包的序号
    T_DWVM_TEST_NET_RESULT m_Test;
};

#endif // !defined(AFX_NET_H__9FA27A91_19EC_4FF1_9691_9D3853BCF693__INCLUDED_)
