// XLog.cpp : Defines the entry point for the DLL application.
//

#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <android/log.h>
#include "XLog.h"
#include "XDataType.h"


static int g_iLevel = XLOG_LEVEL_NORMAL;
static BOOL g_bShowLevel = TRUE;
static BOOL g_bShowTime = TRUE;
static char g_szPrefix[64] = {"DWVM"};

static android_LogPriority g_asLogLevel = ANDROID_LOG_DEBUG;

// 必须使用一个浮点数，否则dll不会自动加载浮点支持库，导致 printf 浮点数时错误
static double g_dbFloatSupport = 0;

// 设定、获取LOG级别
void xlog_set_level(int iLevel)
{
    g_iLevel = iLevel;
    g_dbFloatSupport = 0.0;
}

int xlog_get_level()
{
    return g_iLevel;
}

android_LogPriority xlog_get_android_log_level(int iLevel)
{
    switch (iLevel)
    {
    case XLOG_LEVEL_DISABLE: // 关闭LOG信息
        return ANDROID_LOG_UNKNOWN;
    case XLOG_LEVEL_DETAIL: // 详细过程
        return ANDROID_LOG_VERBOSE;
    case XLOG_LEVEL_NORMAL: // 一般过程
        return ANDROID_LOG_DEBUG;
    case XLOG_LEVEL_WARNING: // 警告
        return ANDROID_LOG_WARN;
    case XLOG_LEVEL_ERROR: // 一般错误
        return ANDROID_LOG_ERROR;
    case XLOG_LEVEL_DEADLY: // 致命错误
        return ANDROID_LOG_FATAL;
    default:
        break;
    }
    return ANDROID_LOG_INFO;
}

// 设定、获取LOG的前缀
void xlog_set_prefix(int bShowLevel, int bShowTime, const char *szPrefix)
{
    g_bShowLevel = bShowLevel;
    g_bShowTime = bShowTime;
    if (NULL != szPrefix)
    {
        if (strlen(szPrefix) >= sizeof(g_szPrefix))
        {
            memcpy(g_szPrefix, szPrefix, sizeof(g_szPrefix) - 1);
            g_szPrefix[sizeof(g_szPrefix) - 1] = '\0';
        }
        else
        {
            strcpy(g_szPrefix, szPrefix);
        }
    }
}

void xlog_get_prefix(int *pShowLevel, int *pShowTime, char *pszPrefix)
{
    if (pShowLevel)
    {
        *pShowLevel = g_bShowLevel;
    }
    if (pShowTime)
    {
        *pShowTime = g_bShowTime;
    }
    if (pszPrefix)
    {
        strcpy(pszPrefix, g_szPrefix);
    }
}

// 输出LOG
void xlog(int iLevel, const char *szFormat, ...)
{
    if (g_iLevel <= XLOG_LEVEL_DISABLE)
    {
        return;
    }

    if (iLevel >= g_iLevel)
    {
        char sz[640] = {""};
        va_list ArgumentList;
        size_t len = 0;

        if (g_bShowLevel)
        {
            sprintf(sz, "[LV%d] ", iLevel);
        }
        if (g_bShowTime)
        {
            time_t t = time(NULL);
            struct tm *lt = localtime(&t);
            if (lt)
            {
                sprintf(sz + strlen(sz), "[%04d-%02d-%02d %02d:%02d:%02d] ", 1900 + lt->tm_year, 1 + lt->tm_mon,
                        lt->tm_mday, lt->tm_hour, lt->tm_min, lt->tm_sec);
            }
            else
            {
                sprintf(sz + strlen(sz), "[%lu] ", t);
            }
        }
#ifdef WIN32
            if(1)
            {
                const DWORD dwPid = GetCurrentProcessId();
                const DWORD dwTid = GetCurrentThreadId();
                sprintf(sz + strlen(sz), "[PID %4lu][TID %4lu] ", dwPid, dwTid);
            }
#endif

        va_start(ArgumentList, szFormat);
        vsprintf(sz + strlen(sz), szFormat, ArgumentList);
        va_end(ArgumentList);

        len = strlen(sz);
        if (len > 0 && '\n' != sz[len - 1] && '\r' != sz[len - 1])
        {
            sz[len] = '\n';
            sz[len + 1] = '\0';
        }

#ifdef WIN32
        OutputDebugString(sz);
#else
        __android_log_write(xlog_get_android_log_level(iLevel), g_szPrefix, sz);
#endif
    }
}
