#ifndef _XLIB_H_20110806_
#define _XLIB_H_20110806_

#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <pthread.h>
#include "XDataType.h"
#include "xsocket.h"

#define LIMIT_RANGE(var, min, max) \
{                                  \
    if(var < min)                  \
    {                              \
        var = min;                 \
    }                              \
    if(var > max)                  \
    {                              \
        var = max;                 \
    }                              \
}

// 生成一个IP地址，主机字节顺序
#define MAKE_IPADDRESS(a1, a2, a3, a4)  (DWORD)(((DWORD)(a1&0xFF)<<24) | ((DWORD)(a2&0xFF)<<16) | ((DWORD)(a3&0xFF)<<8) | ((DWORD)(a4&0xFF)))

// IP:PORT 和字符串之间的转换
void XMakeIpPortString(const DWORD dwIp, const WORD wPort, char *szIpPort);

void XGetIpPortFromString(const char *szIpPort, DWORD *pIp, WORD *pPort);

//
// CRC
//
unsigned short XMakeCrc(void *pData, int DataLen);

unsigned int XMakeCrc32(void *pData, int DataLen);

void XMakeCrc32Ex(void *pData, int DataLen, unsigned int *pCrcValue);

//
// String
//
char *Xstrcpy(char *dest, const char *src, unsigned int dest_len);

int XStringSeparator(const char *szSrc, const char cSeparatorChar, char *szDest, int iMaxSubLen, int iMaxNumber);

//
// 将数组中的元素打散，按照随机顺序排列
//
void XArrayRand(void *pArrayStart, unsigned int nElementByte, unsigned int nElementCount);

//
// 对一个字符串进行处理，变为一个加密串
//
const char *XStringEncrypt(const char *strSrc);

const char *XStringDecrypt(const char *strSrc);

// 把一个字段的字符转换为 .csv 格式
void XCsvWriteSection(const char *szSrc, char *szDest, BOOL bLineEnd);

// 时间转换
UINT64 XMakeTime_U64(WORD wYear, WORD wMonth, WORD wDay, WORD wHour, WORD wMinute, WORD wSecond);

//
// get milli-seconds
//
double get_current_ms(void);

unsigned long get_current_ms2(void);

#define timeGetTime()   get_current_ms2()
#define GetTickCount()  get_current_ms2()

//
// get micro-seconds
//
double get_current_us(void);

//
// thread helper micro
//
#define THREAD_HANDLE                       pthread_t
#define INVALID_THREAD_HANDLE               0
#define THREAD_OBJECT(pt)                   pthread_t pt = 0;
#define THREAD_PROC(proc, context)          void* proc(void* context)
#define THREAD_CREATE(pt, proc, context)    (0 == pthread_create(&pt, NULL, proc, (void*)context))
#define THREAD_CLOSE(pt)                    if(0!=pt) {void* pt_rtn = NULL; pthread_join(pt, &pt_rtn); pthread_detach(pt); pt=0;}
#define THREAD_SET_NAME(pt, name)           if(0!=pt) {pthread_setname_np(pt,name); xlog(XLOG_LEVEL_NORMAL,"thread (0x%lx/%lu) name (%s)\n",pt,pt,name);}
#define WAIT_AND_TERMINATE_THREAD(pt, t, s) THREAD_CLOSE(pt); pt = 0;

//
// swap byte order
//
#define swap_uint16(us)    (((us>>8) & 0x00FF) | ((us<<8) & 0xFF00))
#define swap_uint32(ul)    (((ul>>24)&0xFFL) | ((ul>>8)&0xFF00L) | ((ul<<8)&0xFF0000L) | ((ul<<24)&0xFF000000L))

//
// min(),max()
//
#ifndef min
#define min(a, b) ((a)<(b) ? (a) : (b))
#endif
#ifndef max
#define max(a, b) ((a)>(b) ? (a) : (b))
#endif

#endif // _XLIB_H_20110806_
