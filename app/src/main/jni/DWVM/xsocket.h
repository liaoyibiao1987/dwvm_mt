#ifndef _ADE_SOCKET_H_20080723_
#define _ADE_SOCKET_H_20080723_

#include "dwvm_global.h"

#if defined(WIN32) || defined(_WINDOWS)
/* Windows */
	#include <winsock2.h>
	#include <ws2tcpip.h>
	typedef int socklen_t;
#else
/* Linux */
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <netdb.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

typedef int SOCKET;
#define SOCKET_ERROR (-1)
#define INVALID_SOCKET (-1)
#define closesocket close
#endif

#ifdef __cplusplus
extern "C"
{
#endif

int socket_init(void);
void socket_uninit(void);

unsigned int socket_getip(const char *name);
const char *socket_getstring(unsigned int ip);

/* 将一个域名解析为IP地址，解析后的IP地址为host字节序，如 01 00 00 7F 表示 "127.0.0.1" */
BOOL socket_getopt(SOCKET s, int level, int optname, void *optval, socklen_t optlen);
BOOL socket_setopt(SOCKET s, int level, int optname, void *optval, socklen_t optlen);
BOOL socket_setsendbuf(SOCKET s, int nSendBufSize);
BOOL socket_setrecvbuf(SOCKET s, int nRecvBufSize);

BOOL ip_islan(unsigned int nHostIP);
/* 判断一个IP是否LAN地址： TRUE-lan  FALSE-wan, nHostIP是host字节序 */

SOCKET udp_create(void);
BOOL udp_bind(SOCKET s, unsigned short localPort, unsigned int localIP /*= INADDR_ANY*/);
int udp_send(SOCKET s, void *buf, int sendLen, unsigned int remoteIP, unsigned short remotePort);
int udp_receive(SOCKET s, void *buf, int bufLen, unsigned int *fromIP /*= NULL*/, unsigned short *fromPort /*= NULL*/,
                BOOL isBlock /*= TRUE*/);
void udp_destroy(SOCKET s);

SOCKET tcp_create(BOOL isReuse);
BOOL tcp_bind(SOCKET s, unsigned short localPort, unsigned int localIP);
BOOL tcp_server_listen(SOCKET s);
SOCKET tcp_server_accept(SOCKET s, unsigned int *clientIP, unsigned short *clientPort);
BOOL tcp_client_connect(SOCKET s, unsigned int serverIP, unsigned short serverPort);
int tcp_send(SOCKET s, void *buf, int sendLen);
int tcp_receive(SOCKET s, void *buf, int bufLen, int timeout_milli_sec);
/* timeout_milli_sec: <0 阻塞等待, 0 不等待, >0 等待指定的毫秒数 */
void tcp_destroy(SOCKET s);

#ifdef __cplusplus
}
#endif

#endif /* _ADE_SOCKET_H_20080723_ */
