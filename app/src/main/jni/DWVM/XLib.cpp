// XLib.cpp : Defines the entry point for the DLL application.
//

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <termios.h>
#include <fcntl.h>
#include <time.h>

#include "dwvm_global.h"
#include "xsocket.h"

static unsigned short crc_table[256] = {
    0x0000, 0xc0c1, 0xc181, 0x0140, 0xc301, 0x03c0, 0x0280, 0xc241,
    0xc601, 0x06c0, 0x0780, 0xc741, 0x0500, 0xc5c1, 0xc481, 0x0440,
    0xcc01, 0x0cc0, 0x0d80, 0xcd41, 0x0f00, 0xcfc1, 0xce81, 0x0e40,
    0x0a00, 0xcac1, 0xcb81, 0x0b40, 0xc901, 0x09c0, 0x0880, 0xc841,
    0xd801, 0x18c0, 0x1980, 0xd941, 0x1b00, 0xdbc1, 0xda81, 0x1a40,
    0x1e00, 0xdec1, 0xdf81, 0x1f40, 0xdd01, 0x1dc0, 0x1c80, 0xdc41,
    0x1400, 0xd4c1, 0xd581, 0x1540, 0xd701, 0x17c0, 0x1680, 0xd641,
    0xd201, 0x12c0, 0x1380, 0xd341, 0x1100, 0xd1c1, 0xd081, 0x1040,
    0xf001, 0x30c0, 0x3180, 0xf141, 0x3300, 0xf3c1, 0xf281, 0x3240,
    0x3600, 0xf6c1, 0xf781, 0x3740, 0xf501, 0x35c0, 0x3480, 0xf441,
    0x3c00, 0xfcc1, 0xfd81, 0x3d40, 0xff01, 0x3fc0, 0x3e80, 0xfe41,
    0xfa01, 0x3ac0, 0x3b80, 0xfb41, 0x3900, 0xf9c1, 0xf881, 0x3840,
    0x2800, 0xe8c1, 0xe981, 0x2940, 0xeb01, 0x2bc0, 0x2a80, 0xea41,
    0xee01, 0x2ec0, 0x2f80, 0xef41, 0x2d00, 0xedc1, 0xec81, 0x2c40,
    0xe401, 0x24c0, 0x2580, 0xe541, 0x2700, 0xe7c1, 0xe681, 0x2640,
    0x2200, 0xe2c1, 0xe381, 0x2340, 0xe101, 0x21c0, 0x2080, 0xe041,
    0xa001, 0x60c0, 0x6180, 0xa141, 0x6300, 0xa3c1, 0xa281, 0x6240,
    0x6600, 0xa6c1, 0xa781, 0x6740, 0xa501, 0x65c0, 0x6480, 0xa441,
    0x6c00, 0xacc1, 0xad81, 0x6d40, 0xaf01, 0x6fc0, 0x6e80, 0xae41,
    0xaa01, 0x6ac0, 0x6b80, 0xab41, 0x6900, 0xa9c1, 0xa881, 0x6840,
    0x7800, 0xb8c1, 0xb981, 0x7940, 0xbb01, 0x7bc0, 0x7a80, 0xba41,
    0xbe01, 0x7ec0, 0x7f80, 0xbf41, 0x7d00, 0xbdc1, 0xbc81, 0x7c40,
    0xb401, 0x74c0, 0x7580, 0xb541, 0x7700, 0xb7c1, 0xb681, 0x7640,
    0x7200, 0xb2c1, 0xb381, 0x7340, 0xb101, 0x71c0, 0x7080, 0xb041,
    0x5000, 0x90c1, 0x9181, 0x5140, 0x9301, 0x53c0, 0x5280, 0x9241,
    0x9601, 0x56c0, 0x5780, 0x9741, 0x5500, 0x95c1, 0x9481, 0x5440,
    0x9c01, 0x5cc0, 0x5d80, 0x9d41, 0x5f00, 0x9fc1, 0x9e81, 0x5e40,
    0x5a00, 0x9ac1, 0x9b81, 0x5b40, 0x9901, 0x59c0, 0x5880, 0x9841,
    0x8801, 0x48c0, 0x4980, 0x8941, 0x4b00, 0x8bc1, 0x8a81, 0x4a40,
    0x4e00, 0x8ec1, 0x8f81, 0x4f40, 0x8d01, 0x4dc0, 0x4c80, 0x8c41,
    0x4400, 0x84c1, 0x8581, 0x4540, 0x8701, 0x47c0, 0x4680, 0x8641,
    0x8201, 0x42c0, 0x4380, 0x8341, 0x4100, 0x81c1, 0x8081, 0x4040
};

#define my_hi(w) (*((unsigned char *)(&(w))))
#define my_lo(w) (*((unsigned char *)(&(w))+1))

unsigned short CRC(unsigned char data, unsigned short *crc)
{
    unsigned short t = *crc;
    return (*crc = my_lo(t) ^ (crc_table[data ^ my_hi(t)]));
}

unsigned short XMakeCrc(void *pData, int DataLen)
{
    int i = 0;
    unsigned short crc = 0;
    unsigned char *p = (unsigned char *) pData;
    for (i = 0; i < DataLen; i++, p++)
    {
        crc = (crc >> 8) ^ (crc_table[(*p) ^ (crc & 0x00FF)]);
    }
    return crc;
}

unsigned short XMakeCrc_old(void *pData, int DataLen)
{
    int i = 0;
    unsigned short crc = 0;
    unsigned char *p = (unsigned char *) pData;
    for (i = 0; i < DataLen; i++)
    {
        CRC(p[i], &crc);
    }
    return crc;
}

unsigned int CRC32(unsigned char data, unsigned int *crc)
{
    unsigned int t = *crc;
    unsigned short w1 = (unsigned short) ((t >> 16) & 0xFFFF);
    unsigned short w2 = (unsigned short) (t & 0xFFFF);
    w1 = CRC((unsigned char) (data & 0xF0), &w1);
    w2 = CRC((unsigned char) (data & 0x0F), &w2);
    return (*crc = (w1 << 16) | w2);
}

unsigned int XMakeCrc32(void *pData, int DataLen)
{
    int i = 0;
    unsigned int crc = 0;
    unsigned char *p = (unsigned char *) pData;
    for (i = 0; i < DataLen; i++)
    {
        CRC32(p[i], &crc);
    }
    return crc;
}

void XMakeCrc32Ex(void *pData, int DataLen, unsigned int *pCrcValue)
{
    int i = 0;
    unsigned char *p = (unsigned char *) pData;
    for (i = 0; i < DataLen; i++)
    {
        CRC32(p[i], pCrcValue);
    }
}

char *Xstrcpy(char *dest, const char *src, unsigned int dest_len)
{
    if (strlen(src) >= dest_len)
    {
        memcpy(dest, src, dest_len - 1);
        dest[dest_len - 1] = '\0';
        return dest;
    }
    else
    {
        return strcpy(dest, src);
    }
}

// 分析字串，以指定的分隔符，把解析后得到的子段分别写入数组
int XStringSeparator(const char *szSrc, const char cSeparatorChar, char *szDest, int iMaxSubLen, int iMaxNumber)
{
    const int iSrcLen = strlen(szSrc) + 1;
    int iNum = 0;
    int iBegin = 0;
    int i = 0;

    for (i = 1; i < iSrcLen && iNum < iMaxNumber; i++)
    {
        if (szSrc[i] == cSeparatorChar || szSrc[i] == '\0')
        {
            if ((i > iBegin) && (i - iBegin) < iMaxSubLen)
            {
                memcpy(szDest, &szSrc[iBegin], (size_t) (i - iBegin));
                szDest[i - iBegin] = '\0';
                szDest += iMaxSubLen;
                iNum++;
            }
            iBegin = i + 1;
        }
    }

    return iNum;
}

// 对一个字符串进行处理，变为一个加密串
static char g_szEncryptDest[256];

const char *XStringEncrypt(const char *strSrc)
{
    char *p = g_szEncryptDest;
    unsigned int i = 0;
    unsigned int nRandLen = 0, nRandLen2 = 0;
    char nKey = 0, nData = 0;
    unsigned int nSrcLen = strlen(strSrc);

    if (nSrcLen > 124)
    {
        nSrcLen = 124;
    }
    memset(g_szEncryptDest, ' ', 254);
    srand48((long) time(NULL));

    nRandLen = ((unsigned int) lrand48()) % (251 - 2 * nSrcLen);
    nRandLen = (nRandLen % 15) + 1;

    *p = (char) (nSrcLen + nRandLen + 0x20);
    p++;

    *p = (char) ((((((unsigned char) lrand48()) % 4) + 2) << 4) | nRandLen);
    p++;

    for (i = 0; i < nRandLen; i++)
    {
        *p = (char) ((((unsigned char) lrand48()) % 0x5E) + 0x20);
        p++;
    }

    for (i = 0; i < nSrcLen; i++)
    {
        while (1)
        {
            nKey = (char) ((((unsigned char) lrand48()) % 0x5E) + 0x20);
            nData = strSrc[i] ^ nKey;
            if (nData != '\0')
            {
                *p = nKey;
                p++;
                *p = nData;
                p++;
                break;
            }
        }
    }

    nRandLen2 = 254 - 2 - nRandLen - 2 * nSrcLen;
    for (i = 0; i < nRandLen2; i++)
    {
        *p = (char) ((((unsigned char) lrand48()) % 0x5E) + 0x20);
        p++;
    }

    g_szEncryptDest[254] = '\0';
    return g_szEncryptDest;
}

// 还原一个加密字符串
static char g_szDecryptDest[256];

const char *XStringDecrypt(const char *strSrc)
{
    unsigned int nRandLen = 0, nDestLen = 0, i = 0;
    unsigned char *p = NULL;
    int nSrcLen = strlen(strSrc);

    if (nSrcLen > 254)
    {
        nSrcLen = 254;
    }
    memset(g_szDecryptDest, '\0', 255);

    nRandLen = (unsigned int) ((unsigned char) strSrc[1] & 0x0F);
    nDestLen = (((unsigned char) strSrc[0]) - 0x20 - nRandLen);

    if (nDestLen > 124)
    {
        return g_szDecryptDest;
    }

    p = (unsigned char *) (strSrc + 2 + nRandLen);
    for (i = 0; i < nDestLen; i++)
    {
        g_szDecryptDest[i] = (char) ((*p) ^ (*(p + 1)));
        p += 2;
    }
    g_szDecryptDest[i] = '\0';

    return g_szDecryptDest;
}

// 将数组中的元素打散，按照随机顺序排列
void XArrayRand(void *pArrayStart, unsigned int nElementByte, unsigned int nElementCount)
{
    unsigned int r1 = 0, r2 = 0;
    unsigned char pTempBuf[4] = {0, 0, 0, 0};
    unsigned char *pArray = (unsigned char *) pArrayStart;
    if (pArrayStart == NULL || nElementByte < 1 || nElementByte > 4 || nElementCount < 2)
    {
        return;
    }

    srand48((long) time(NULL));
    for (unsigned int i = 0; i < nElementCount; i++)
    {
        r1 = ((unsigned int) lrand48()) % nElementCount;
        r2 = ((unsigned int) lrand48()) % nElementCount;
        memcpy(pTempBuf, pArray + r1 * nElementByte, nElementByte);
        memcpy(pArray + r1 * nElementByte, pArray + r2 * nElementByte, nElementByte);
        memcpy(pArray + r2 * nElementByte, pTempBuf, nElementByte);
    }
}

// 把一个字段的字符转换为 .csv 格式
void XCsvWriteSection(const char *szSrc, char *szDest, BOOL bLineEnd)
{
    // CSV格式：
    // 1. 包含 , 和 " 的字符串，以及空字符串，需要用双引号 "..." 包起来
    // 2. 字符串中的 " 需要用2个双引号 "" 替换
    // 3. 包含的换行符 \n 可以直接保留
    // 4. 行结束时末尾添加 \n，非行结束时末尾添加 ,
    const size_t iSrcLen = (NULL == szSrc) ? 0 : strlen(szSrc);
    if (0 == iSrcLen)
    {
        strcpy(szDest, "\"\"");
    }
    else if (strstr(szSrc, ",") || strstr(szSrc, "\""))
    {
        const char *s = szSrc;
        char *d = szDest;
        *d = '\"';
        d++;
        for (; '\0' != *s; s++, d++)
        {
            if ('\"' == *s)
            {
                *d = '\"';
                d++;
            }
            *d = *s;
        }
        *d = '\"';
        d++;
        *d = '\0';
    }
    else
    {
        strcpy(szDest, szSrc);
    }

    if (bLineEnd)
    {
        strcat(szDest, "\n");
    }
    else
    {
        strcat(szDest, ",");
    }
}

void XMakeIpPortString(const DWORD dwIp, const WORD wPort, char *szIpPort)
{
    BYTE *p = (BYTE *) &dwIp;
    sprintf(szIpPort, "%u.%u.%u.%u:%u", p[3], p[2], p[1], p[0], wPort);
}

void XGetIpPortFromString(const char *szIpPort, DWORD *pIp, WORD *pPort)
{
    char szTemp[64];
    strcpy(szTemp, szIpPort);
    char *sz = strstr(szTemp, ":");
    if (NULL != sz)
    {
        *sz = '\0';
        if (pPort) *pPort = (WORD) atoi(sz + 1);
    }
    else
    {
        if (pPort) *pPort = 0;
    }

    if (pIp) *pIp = socket_getip(szTemp);
}

// 时间转换
UINT64 XMakeTime_U64(WORD wYear, WORD wMonth, WORD wDay, WORD wHour, WORD wMinute, WORD wSecond)
{
    UINT64 u64 = 0;
    u64 = wYear;
    u64 = u64 * 100 + wMonth;
    u64 = u64 * 100 + wDay;
    u64 = u64 * 100 + wHour;
    u64 = u64 * 100 + wMinute;
    u64 = u64 * 100 + wSecond;
    return u64;
}

double get_current_ms(void)
{
    struct timeval tv = {0, 0};
    gettimeofday(&tv, NULL);
    return (tv.tv_sec * 1000.0) + (tv.tv_usec / 1000.0);
}

unsigned long get_current_ms2(void)
{
    struct timeval tv = {0, 0};
    gettimeofday(&tv, NULL);
    return ((unsigned long) tv.tv_sec * 1000) + (tv.tv_usec / 1000);
}

double get_current_us(void)
{
    struct timeval tv = {0, 0};
    gettimeofday(&tv, NULL);
    return (tv.tv_sec * 1000000.0) + tv.tv_usec;
}

