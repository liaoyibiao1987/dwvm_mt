#ifndef _XLOG_H_20110805_
#define _XLOG_H_20110805_

// LOG信息的级别
#define XLOG_LEVEL_DISABLE    (0) // 关闭LOG信息
#define XLOG_LEVEL_DETAIL    (1) // 详细过程
#define XLOG_LEVEL_NORMAL    (2) // 一般过程
#define XLOG_LEVEL_WARNING    (3) // 警告
#define XLOG_LEVEL_ERROR    (4) // 一般错误
#define XLOG_LEVEL_DEADLY    (5) // 致命错误

// 设定、获取LOG级别
void xlog_set_level(int iLevel);

int xlog_get_level();

// 设定、获取LOG的前缀
void xlog_set_prefix(int bShowLevel, int bShowTime, const char *szPrefix);

void xlog_get_prefix(int *pShowLevel, int *pShowTime, char *pszPrefix);

// 输出LOG
void xlog(int iLevel, const char *szFormat, ...);

#endif // _XLOG_H_20110805_
