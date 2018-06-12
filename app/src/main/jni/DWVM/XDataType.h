#ifndef _X_DATATYPE_H_20160123_
#define _X_DATATYPE_H_20160123_


//
// 数据类型
//
#if !defined(_WIN32) && !defined(WIN32)

#include <unistd.h>

typedef int BOOL;
typedef unsigned char BYTE;
typedef unsigned short WORD;
typedef unsigned int DWORD;

typedef unsigned char UCHAR;
typedef unsigned short USHORT;
typedef unsigned int ULONG;
typedef unsigned long long UINT64;

typedef int LONG;

typedef const char *LPCTSTR;
typedef void *LPVOID;

#ifndef TRUE
#define TRUE (1)
#endif

#ifndef FALSE
#define FALSE (0)
#endif

#ifndef NULL
#define NULL (0)
#endif

#ifndef HANDLE
#define HANDLE void*
#endif

#ifndef MY_SLEEP
#define MY_SLEEP(milli_sec) usleep(milli_sec*1000)
#endif

#endif


#endif // _X_DATATYPE_H_20160123_
