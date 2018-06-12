// DwvmBase.cpp : Defines the entry point for the DLL application.
//

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "Net.h"

//
// socket 和 CNet* 对应列表
//
CXSimpleList g_SocketList;


int DWVM_Encrypt(unsigned char *pData, int iLen)
{
    static unsigned char encrypt_tab[256] = {
        0x00, 0xCC, 0xD5, 0xA7, 0xD9, 0x67, 0x11, 0x0C, 0xB8, 0x6B, 0xC4, 0x01, 0x58, 0x82, 0x99, 0xE5,
        0x25, 0x1C, 0x44, 0xE1, 0x24, 0xDD, 0x49, 0xC5, 0xBC, 0x36, 0x87, 0x85, 0x40, 0x9F, 0x34, 0x91,
        0x7E, 0xD7, 0xC6, 0x8C, 0x29, 0xD6, 0x4D, 0xB6, 0x3A, 0xB4, 0x0E, 0x02, 0x2A, 0xB9, 0x18, 0xA9,
        0x2E, 0x83, 0x26, 0xD4, 0xE8, 0xA0, 0x1A, 0x1D, 0x5B, 0xE6, 0xDB, 0x2F, 0xCE, 0xA3, 0x69, 0xCA,
        0x31, 0x9E, 0x45, 0xE0, 0x8B, 0xDC, 0x80, 0xA5, 0x88, 0x70, 0x6F, 0x16, 0x3D, 0x54, 0x55, 0x93,
        0x9B, 0x1B, 0x1F, 0x1E, 0xFA, 0x71, 0xAB, 0xAA, 0x52, 0x12, 0x56, 0xE7, 0xF2, 0xB5, 0x37, 0x3B,
        0x97, 0x92, 0xF7, 0x07, 0x10, 0x86, 0xE9, 0x3E, 0x42, 0x23, 0x19, 0x96, 0xEA, 0xF0, 0x63, 0x3F,
        0x9A, 0xBA, 0x0B, 0x2B, 0xC8, 0x21, 0xD8, 0x5E, 0x77, 0xF5, 0xBE, 0xE2, 0x39, 0x4A, 0x64, 0x48,
        0x5C, 0x47, 0x6C, 0x98, 0xF1, 0x04, 0x6E, 0xA1, 0x0A, 0x6A, 0xCB, 0xBF, 0xAD, 0xB3, 0x4B, 0x20,
        0xCF, 0x09, 0x50, 0xB7, 0xB1, 0xCD, 0xD1, 0x8F, 0xD2, 0x75, 0x7B, 0x3C, 0x5F, 0x84, 0xDF, 0x78,
        0x8E, 0x08, 0xC0, 0x4F, 0xEB, 0x5A, 0x4E, 0x68, 0xD0, 0xEF, 0xA8, 0xC1, 0x73, 0x2D, 0xBB, 0xC2,
        0x4C, 0x35, 0x06, 0x43, 0xF6, 0x5D, 0x94, 0x6D, 0xF3, 0x60, 0x22, 0x59, 0xA2, 0x57, 0x51, 0x9D,
        0x0F, 0xEE, 0xE4, 0xFE, 0xB0, 0x38, 0x61, 0x28, 0x95, 0xAC, 0x53, 0x66, 0xC9, 0x05, 0xED, 0xB2,
        0x7F, 0x15, 0xFF, 0x14, 0x76, 0x62, 0xF9, 0x27, 0x72, 0xBD, 0x7D, 0xDA, 0x90, 0xC7, 0x89, 0xAE,
        0x17, 0xA4, 0x8A, 0xE3, 0xEC, 0x74, 0x8D, 0xAF, 0xFB, 0xF8, 0x13, 0x7A, 0x03, 0x81, 0x2C, 0x79,
        0xA6, 0xDE, 0xFC, 0x32, 0x0D, 0xD3, 0x46, 0x33, 0xC3, 0xFD, 0x65, 0x30, 0xF4, 0x9C, 0x7C, 0x41,
    };

    int i = 0;
    for (i = 0; i < iLen; i++, pData++)
    {
        *pData = encrypt_tab[*pData];
    }
    return i;
}

int DWVM_Decrypt(unsigned char *pData, int iLen)
{
    static unsigned char decrypt_tab[256] = {
        0x00, 0x0B, 0x2B, 0xEC, 0x85, 0xCD, 0xB2, 0x63, 0xA1, 0x91, 0x88, 0x72, 0x07, 0xF4, 0x2A, 0xC0,
        0x64, 0x06, 0x59, 0xEA, 0xD3, 0xD1, 0x4B, 0xE0, 0x2E, 0x6A, 0x36, 0x51, 0x11, 0x37, 0x53, 0x52,
        0x8F, 0x75, 0xBA, 0x69, 0x14, 0x10, 0x32, 0xD7, 0xC7, 0x24, 0x2C, 0x73, 0xEE, 0xAD, 0x30, 0x3B,
        0xFB, 0x40, 0xF3, 0xF7, 0x1E, 0xB1, 0x19, 0x5E, 0xC5, 0x7C, 0x28, 0x5F, 0x9B, 0x4C, 0x67, 0x6F,
        0x1C, 0xFF, 0x68, 0xB3, 0x12, 0x42, 0xF6, 0x81, 0x7F, 0x16, 0x7D, 0x8E, 0xB0, 0x26, 0xA6, 0xA3,
        0x92, 0xBE, 0x58, 0xCA, 0x4D, 0x4E, 0x5A, 0xBD, 0x0C, 0xBB, 0xA5, 0x38, 0x80, 0xB5, 0x77, 0x9C,
        0xB9, 0xC6, 0xD5, 0x6E, 0x7E, 0xFA, 0xCB, 0x05, 0xA7, 0x3E, 0x89, 0x09, 0x82, 0xB7, 0x86, 0x4A,
        0x49, 0x55, 0xD8, 0xAC, 0xE5, 0x99, 0xD4, 0x78, 0x9F, 0xEF, 0xEB, 0x9A, 0xFE, 0xDA, 0x20, 0xD0,
        0x46, 0xED, 0x0D, 0x31, 0x9D, 0x1B, 0x65, 0x1A, 0x48, 0xDE, 0xE2, 0x44, 0x23, 0xE6, 0xA0, 0x97,
        0xDC, 0x1F, 0x61, 0x4F, 0xB6, 0xC8, 0x6B, 0x60, 0x83, 0x0E, 0x70, 0x50, 0xFD, 0xBF, 0x41, 0x1D,
        0x35, 0x87, 0xBC, 0x3D, 0xE1, 0x47, 0xF0, 0x03, 0xAA, 0x2F, 0x57, 0x56, 0xC9, 0x8C, 0xDF, 0xE7,
        0xC4, 0x94, 0xCF, 0x8D, 0x29, 0x5D, 0x27, 0x93, 0x08, 0x2D, 0x71, 0xAE, 0x18, 0xD9, 0x7A, 0x8B,
        0xA2, 0xAB, 0xAF, 0xF8, 0x0A, 0x17, 0x22, 0xDD, 0x74, 0xCC, 0x3F, 0x8A, 0x01, 0x95, 0x3C, 0x90,
        0xA8, 0x96, 0x98, 0xF5, 0x33, 0x02, 0x25, 0x21, 0x76, 0x04, 0xDB, 0x3A, 0x45, 0x15, 0xF1, 0x9E,
        0x43, 0x13, 0x7B, 0xE3, 0xC2, 0x0F, 0x39, 0x5B, 0x34, 0x66, 0x6C, 0xA4, 0xE4, 0xCE, 0xC1, 0xA9,
        0x6D, 0x84, 0x5C, 0xB8, 0xFC, 0x79, 0xB4, 0x62, 0xE9, 0xD6, 0x54, 0xE8, 0xF2, 0xF9, 0xC3, 0xD2,
    };

    int i = 0;
    for (i = 0; i < iLen; i++, pData++)
    {
        *pData = decrypt_tab[*pData];
    }
    return i;
}

// 由 socket 检索到 CNet*
CNet *FindNetFromSocket(SOCKET s)
{
    CNet *pNet = NULL;
    const int iCount = g_SocketList.GetCount();
    for (int i = 0; i < iCount; i++)
    {
        pNet = (CNet *) g_SocketList.GetItem(i);
        if (pNet && s == pNet->GetSocket())
        {
            return pNet;
        }
    }
    return NULL;
}

// 由 LocalPort 检索到 CNet*
CNet *FindNetFromLocalPort(WORD wLocalPort)
{
    CNet *pNet = NULL;
    const int iCount = g_SocketList.GetCount();
    for (int i = 0; i < iCount; i++)
    {
        pNet = (CNet *) g_SocketList.GetItem(i);
        if (pNet && wLocalPort == pNet->GetLocalPort())
        {
            return pNet;
        }
    }
    return NULL;
}

// 创建网络相关资源
SOCKET DWVM_CreateNetThread(    // 返回值：被创建的socket. 失败返回INVALID_SOCKET (-1)
    DWORD dwLocalDeviceId,            // 本设备的ID
    WORD wLocalPort,                // 本地端口号
    DWORD dwBindLocalIp,            // 绑定本机的IP地址
    int iSocketBufferSize,            // socket buffer尺寸. 小于等于0时，用默认尺寸 WVM_DEFAULT_SOCKET_BUFFER_SIZE (256000)
    pfnDwvmNetReceive pfnCbProc,    // 接收网络包的回调函数指针
    void *pCbContext,                // 网络包回调函数的上下文参数. 会在回调函数 pfnCbProc 的 pCbContext 参数中原样返回
    DWORD dwCbThreadNumber,            // 网络包回调线程的数量。<=1 表示用单个线程回调，>1 表示用多个线程回调
    DWORD dwCbThreadStackSize)        // 网络包回调线程的堆栈尺寸，单位：字节。0 表示使用进程默认尺寸
{
    // 初始化socket列表
    if (!g_SocketList.IsInited())
    {
        if (!g_SocketList.Init(MAX_SOCKET_NUMBER))
        {
            xlog(XLOG_LEVEL_DEADLY, "Create socket list FAIL! number=%d", MAX_SOCKET_NUMBER);
            return INVALID_SOCKET;
        }
    }

    if (g_SocketList.GetCount() >= MAX_SOCKET_NUMBER)
    {
        xlog(XLOG_LEVEL_ERROR, "Create net FAIL! local-port=%u, list full (%d)", wLocalPort,
             g_SocketList.GetCount()

        );
        return INVALID_SOCKET;
    }

    CNet *pNet = new CNet();
    if (NULL == pNet)
    {
        xlog(XLOG_LEVEL_ERROR, "Create net FAIL! local-port=%u", wLocalPort);
        return INVALID_SOCKET;
    }

    if (!pNet->Create(dwLocalDeviceId, wLocalPort, dwBindLocalIp, iSocketBufferSize, pfnCbProc, pCbContext,
                      dwCbThreadNumber, dwCbThreadStackSize))
    {
        delete pNet;
        pNet = NULL;
        xlog(XLOG_LEVEL_ERROR, "Create net FAIL! local-port=%u", wLocalPort);
        return INVALID_SOCKET;
    }

    if (g_SocketList.Add((DWORD) pNet->GetSocket(), (void *) pNet) < 0)
    {
        xlog(XLOG_LEVEL_DEADLY, "Add socket to list FAIL! local-port=%u, socket=%d, list-count=%lu",
             wLocalPort, pNet->GetSocket(), g_SocketList.GetCount());
        pNet->Destroy();
        delete pNet;
        pNet = NULL;
        return INVALID_SOCKET;
    }

    xlog(XLOG_LEVEL_NORMAL,
         "Create net succceed. local-port=%u, socket=%d, device-id=0x%08X, bind-localip=%s", wLocalPort,
         pNet->GetSocket(), dwLocalDeviceId, socket_getstring(dwBindLocalIp));
    return pNet->GetSocket();

}

// 释放网络资源
void DWVM_DestroyNetThread(SOCKET s)
{
    CNet *pNet = FindNetFromSocket(s);
    if (NULL == pNet)
    {
        return;
    }

    g_SocketList.Remove((DWORD) s);

    const WORD wLocalPort = pNet->GetLocalPort();
    const DWORD dwId = pNet->GetLocalDeviceId();
    pNet->Destroy();
    delete pNet;
    pNet = NULL;
    xlog(XLOG_LEVEL_NORMAL, "Destroy net. local-port=%u, device-id=0x%08X", wLocalPort, dwId);
}

// 设置、获取网络回调函数的上下文参数
BOOL DWVM_SetNetContext(SOCKET s, void *pCbContext)
{
    CNet *pNet = FindNetFromSocket(s);
    if (NULL == pNet)
    {
        return FALSE;
    }
    pNet->SetContext(pCbContext);
    return TRUE;
}

void *DWVM_GetNetContext(SOCKET s)
{
    CNet *pNet = FindNetFromSocket(s);
    if (NULL == pNet)
    {
        return 0;
    }
    return pNet->GetContext();
}

// 设置、获取本设备的ID
BOOL DWVM_SetLocalDeviceId(SOCKET s, DWORD dwLocalDeviceId)
{
    CNet *pNet = FindNetFromSocket(s);
    if (NULL == pNet)
    {
        return FALSE;
    }
    pNet->SetLocalDeviceId(dwLocalDeviceId);
    return TRUE;
}

DWORD DWVM_GetLocalDeviceId(SOCKET s)
{
    CNet *pNet = FindNetFromSocket(s);
    if (NULL == pNet)
    {
        return 0;
    }
    return pNet->GetLocalDeviceId();
}

//==== 发送网络包
DWORD
DWVM_SendNetPacket(    // 返回值：该数据包的序号。0表示失败
    BOOL bInner,            // 是否组件内部包
    SOCKET s,                // socket
    DWORD dwCmd,            // 网络包的类型: WVM_CMD_xxx
    DWORD dwNeedReply,        // 是否需要接收方回复应答包。 0为不需要，其它值为需要
    DWORD dwDestDeviceId,    // 目标设备的ID
    DWORD dwDestIp,            // 目标IP地址
    WORD wDestPort,            // 目标端口
    void *pData,            // 网络包buffer
    int iDataSize,            // 网络包长度
    DWORD dwEncrypt)        // 加密模式，0为不加密。最高4bit表示加密模式，0表示无加密。其他28bit为加密因子
{
    CNet *pNet = FindNetFromSocket(s);
    if (NULL == pNet)
    {
        return FALSE;
    }
    return pNet->Send(bInner, pNet->GetLocalDeviceId(), dwCmd, dwNeedReply, dwDestDeviceId, dwDestIp, wDestPort, pData,
                      iDataSize, dwEncrypt, 0);
}

DWORD
DWVM_SendNetPacketEx(    // 返回值：该数据包的序号。0表示失败
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
    DWORD dwSrcDeviceId)    // 源设备ID
{
    CNet *pNet = FindNetFromSocket(s);
    if (NULL == pNet)
    {
        return FALSE;
    }
    return pNet->Send(bInner, dwSrcDeviceId, dwCmd, dwNeedReply, dwDestDeviceId, dwDestIp, wDestPort, pData, iDataSize,
                      dwEncrypt, 0);
}

//==== 测试本机和指定设备之间的P2P网络速度、响应时间等
BOOL
DWVM_StartTestNet(    // 开始测试。这个函数是异步工作，函数会立即返回。
    SOCKET s,                // socket
    DWORD dwDestDeviceId,    // 目标设备的ID
    DWORD dwDestIp,            // 目标IP地址
    WORD wDestPort,            // 目标端口
    WORD wTestSecond)        // 测试时间，单位：秒。必须大于或者等于5秒
{
    CNet *pNet = FindNetFromSocket(s);
    if (NULL == pNet)
    {
        return FALSE;
    }
    return pNet->StartTest(dwDestDeviceId, dwDestIp, wDestPort, wTestSecond);
}

//==== 停止测试
BOOL
DWVM_StopTestNet(SOCKET s)
{
    CNet *pNet = FindNetFromSocket(s);
    if (NULL == pNet)
    {
        return FALSE;
    }

    pNet->StopTest();
    return TRUE;
}

//==== 获知测试结果
BOOL
DWVM_GetTestNetResult(SOCKET s, T_DWVM_TEST_NET_RESULT *pResult)
{
    CNet *pNet = FindNetFromSocket(s);
    if (NULL == pNet)
    {
        return FALSE;
    }
    return pNet->GetTestStatus(pResult);
}

//==== 获知测试结果
BOOL
DWVM_GetTestNetResultString(SOCKET s, char *szResult)
{
    //返回值的格式：
    //	"ID=0x03000001
    //	IP=202.96.128.166:5002
    //	STEP=2/3
    //	SECOND=4/10
    //	ONLINE=1
    //	STEP1=6ms,100Kbps,99/100
    //	STEP2=11ms,6000Kbps,896/920
    //	STEP3=6ms,10000Kbps,988/1200
    //	"

    if (NULL == szResult)
    {
        return FALSE;
    }

    CNet *pNet = FindNetFromSocket(s);
    if (NULL == pNet)
    {
        return FALSE;
    }

    T_DWVM_TEST_NET_RESULT r;
    memset(&r, 0, sizeof(r));
    r.dwSize = sizeof(r);
    if (!pNet->GetTestStatus(&r))
    {
        strcpy(szResult, "ID=0x00000000\r\n");
        return FALSE;
    }

    sprintf(szResult, "ID=0x%08X\r\n", r.dwDestDeviceId);

    char szIp[64] = {""};
    XMakeIpPortString(r.dwDestIp, r.wDestPort, szIp);
    sprintf(szResult + strlen(szResult), "IP=%s\r\n", szIp);

    sprintf(szResult + strlen(szResult), "STEP=%u/3\r\n", r.wStep);

    double dbRun = 0;
    int i = 0;
    for (i = 0; i < 3; i++)
    {
        dbRun += r.Stat[i].dbRunTime;
    }
    sprintf(szResult + strlen(szResult), "SECOND=%.1f/%u\r\n", dbRun / 1000, r.wTestSecond);

    BOOL bOnline = TRUE;
    if (r.Stat[0].iPktAckCnt <= 0 && r.Stat[0].iPktSendCnt > 20)
    {
        bOnline = FALSE;
    }
    sprintf(szResult + strlen(szResult), "ONLINE=%d\r\n", bOnline);

    for (i = 0; i < 3; i++)
    {
        sprintf(szResult + strlen(szResult),
                "STEP%d=%.2fms,%.0fKbps,%u/%u\r\n",
                i + 1,
                r.Stat[i].dbResponse,
                (0 == r.Stat[i].dbRunTime) ? (0) : (((double) (r.Stat[i].iPktAckCnt * r.Stat[i].iPktSize)) * 8.0 /
                                                    r.Stat[i].dbRunTime),
                r.Stat[i].iPktAckCnt,
                r.Stat[i].iPktSendCnt);
    }

    return TRUE;
}


//===================
// 帧率控制
//===================

CFrameRateControl::CFrameRateControl()
{
    m_dwStartTick = GetTickCount();
    m_dwFrameCount = 0;
    m_dbFrameRate = 0.0;
    m_dwStatInterlaceTime = 5000;
    m_dwHopeFrameRate = 30;

    m_skip_Curr = 0;
    m_skip_Count = 30;
    memset(m_skip_Mask, 1, sizeof(m_skip_Mask));
}

CFrameRateControl::~CFrameRateControl()
{
}

// dwHopeFrameRate: 希望的帧率
// dwStatInterlaceTime: 多久统计一次帧率
BOOL CFrameRateControl::Setup(DWORD dwHopeFrameRate, DWORD dwStatInterlaceTime)
{
    if (dwHopeFrameRate > 0 && dwHopeFrameRate <= 30)
    {
        m_dwHopeFrameRate = dwHopeFrameRate;
    }
    if (dwStatInterlaceTime != 0)
    {
        m_dwStatInterlaceTime = dwStatInterlaceTime;
    }

    OnInputFramerateChanged();

    return TRUE;
}

// 判断该帧是否需要被跳过。TRUE-跳过，FALSE-不跳过
BOOL CFrameRateControl::InputOneFrame(BOOL *pbFramerateChanged)
{
    const DWORD dwCurrTick = GetTickCount();
    const DWORD dwElapseTick = dwCurrTick - m_dwStartTick;
    const BOOL bNewStat = (dwElapseTick >= m_dwStatInterlaceTime);
    m_dwFrameCount++;
    if (bNewStat)
    {
        const double dbOldFrameRate = m_dbFrameRate;
        m_dbFrameRate = (m_dwFrameCount * 1000.0) / dwElapseTick;
        m_dwFrameCount = 0;
        m_dwStartTick = dwCurrTick;

        const double fdb = fabs(m_dbFrameRate - dbOldFrameRate);
        if (fdb >= 2.0 || (dbOldFrameRate <= 1.0 && fdb >= 0.5))
        {
            OnInputFramerateChanged();
        }
    }
    if (pbFramerateChanged)
    {
        *pbFramerateChanged = bNewStat;
    }

    if ((++m_skip_Curr) >= m_skip_Count)
    {
        m_skip_Curr = 0;
    }
    return (0 == m_skip_Mask[m_skip_Curr]);
}

// 从新开始统计
void CFrameRateControl::Reset()
{
    m_dwStartTick = GetTickCount();
    m_dwFrameCount = 0;
    m_dbFrameRate = 0.0;
}

void CFrameRateControl::OnInputFramerateChanged()
{
    m_skip_Curr = 0;
    m_skip_Count = 0;
    memset(m_skip_Mask, 0, sizeof(m_skip_Mask));

    const DWORD dwInputFrameRate = (DWORD) (m_dbFrameRate + 0.5);
    const DWORD dwHopeFrameRate = (DWORD) m_dwHopeFrameRate;

    m_skip_Curr = 0;
    if (dwInputFrameRate <= 0)
    {
        m_skip_Count = 30;
        memset(m_skip_Mask, 1, sizeof(m_skip_Mask));
    }
    else if (dwHopeFrameRate <= 0)
    {
        m_skip_Count = 0;
        memset(m_skip_Mask, 0, sizeof(m_skip_Mask));
    }
    else if (dwInputFrameRate > 30 || dwInputFrameRate <= dwHopeFrameRate)
    {
        m_skip_Count = 30;
        memset(m_skip_Mask, 1, sizeof(m_skip_Mask));
    }
    else
    {
        m_skip_Count = (BYTE) dwInputFrameRate;
        const double dbInputInterlaceMs = 1000.0 / dwInputFrameRate;
        const double dbHopeInterlaceMs = 1000.0 / dwHopeFrameRate;
        double dbInput = 0.0, dbHope = dbHopeInterlaceMs;
        for (int i = 0; i < 30; i++, dbInput += dbInputInterlaceMs)
        {
            m_skip_Mask[i] = (BYTE) ((dbHope >= dbInput && (dbHope - dbInput) < dbInputInterlaceMs) ? 1 : 0);
            if (m_skip_Mask[i])
            {
                dbHope += dbHopeInterlaceMs;
            }
        }
    }
}
