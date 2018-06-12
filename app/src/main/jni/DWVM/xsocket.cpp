#include "xsocket.h"

int socket_init(void)
{
#if defined(WIN32) || defined(_WINDOWS)
    WSADATA wsaData;
    return WSAStartup( MAKEWORD(2,2), &wsaData );
#else
    return 1;
#endif
}

void socket_uninit(void)
{
#if defined(WIN32) || defined(_WINDOWS)
    WSACleanup();
#endif
}

unsigned int socket_getip(const char *name)
{
    unsigned int ip = 0;

    if (name == NULL || name[0] == '\0')
    {
        return 0;
    }

    ip = inet_addr(name);
    if (ip == 0 || ip == INADDR_NONE)
    {
        struct hostent *phe = gethostbyname(name);
        if (phe == NULL)
        {
            return 0;
        }
        memcpy(&ip, phe->h_addr, 4);
    }

    return ntohl(ip);
}

static char g_szIpToString[64] = {""};

const char *socket_getstring(unsigned int ip)
{
    unsigned char *pb = (unsigned char *) &ip;
    sprintf(g_szIpToString, "%u.%u.%u.%u", pb[3], pb[2], pb[1], pb[0]);
    return g_szIpToString;
}

BOOL socket_getopt(SOCKET s, int level, int optname, void *optval, socklen_t optlen)
{
    return (0 == getsockopt(s, level, optname, (char *) optval, &optlen));
}

BOOL socket_setopt(SOCKET s, int level, int optname, void *optval, socklen_t optlen)
{
    return (0 == setsockopt(s, level, optname, (char *) optval, optlen));
}

BOOL socket_setsendbuf(SOCKET s, int nSendBufSize)
{
    return socket_setopt(s, SOL_SOCKET, SO_SNDBUF, &nSendBufSize, sizeof(nSendBufSize));
}

BOOL socket_setrecvbuf(SOCKET s, int nRecvBufSize)
{
    return socket_setopt(s, SOL_SOCKET, SO_RCVBUF, &nRecvBufSize, sizeof(nRecvBufSize));
}

SOCKET udp_create(void)
{
    return socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
}

BOOL udp_bind(SOCKET s, unsigned short localPort, unsigned int localIP)
{
    struct sockaddr_in local_sin;

    if (s == INVALID_SOCKET)
    {
        return FALSE;
    }

    local_sin.sin_family = AF_INET;
    local_sin.sin_addr.s_addr = htonl(localIP);
    local_sin.sin_port = htons(localPort);
    if (bind(s, (const struct sockaddr *) &local_sin, sizeof(local_sin)) == SOCKET_ERROR)
    {
        return FALSE;
    }

    return TRUE;
}

void udp_destroy(SOCKET s)
{
    if (s != INVALID_SOCKET)
    {
        shutdown(s, 2);
        closesocket(s);
    }
}

int udp_send(SOCKET s, void *buf, int sendLen, unsigned int remoteIP, unsigned short remotePort)
{
    struct sockaddr_in remoteAddr;

    if (s == INVALID_SOCKET || buf == NULL || sendLen < 1 || remotePort == 0)
    {
        return SOCKET_ERROR;
    }

    remoteAddr.sin_family = AF_INET;
    remoteAddr.sin_port = htons(remotePort);
    remoteAddr.sin_addr.s_addr = htonl(remoteIP);

    return sendto(s, (char *) buf, (size_t) sendLen, 0, (struct sockaddr *) &remoteAddr, sizeof(remoteAddr));
}

int udp_receive(SOCKET s, void *buf, int bufLen, unsigned int *fromIP, unsigned short *fromPort, BOOL isBlock)
{
    struct sockaddr_in remoteAddr;
    socklen_t addrLen = 0;
    int ret = 0;

    if (s == INVALID_SOCKET || buf == NULL || bufLen < 1)
    {
        return SOCKET_ERROR;
    }

    if (!isBlock)
    {
        struct timeval timeOut;
        fd_set fds;

        FD_ZERO(&fds);
        FD_SET(s, &fds);

        timeOut.tv_sec = 0;
        timeOut.tv_usec = 0;

        if (select(s + 1, &fds, NULL, NULL, &timeOut) <= 0)
            return SOCKET_ERROR;
    }

    addrLen = sizeof(remoteAddr);
    ret = recvfrom(s, (char *) buf, (size_t) bufLen, 0, (struct sockaddr *) &remoteAddr, &addrLen);

    if (fromIP) *fromIP = ntohl(remoteAddr.sin_addr.s_addr);
    if (fromPort) *fromPort = ntohs(remoteAddr.sin_port);
    return ret;
}

BOOL ip_islan(unsigned int nHostIP)
{
    /*
    ** ��IP��ַ3����Ҫ�������������3��������Ϊ˽�е�ַ�����ַ��Χ���£�
    ** A���ַ��10.0.0.0��10.255.255.255
    ** B���ַ��172.16.0.0��172.31.255.255
    ** C���ַ��192.168.0.0��192.168.255.255
    */

    unsigned char *p = (unsigned char *) &nHostIP;
    if (p[3] == 10 ||
        (p[3] == 172 && p[2] >= 16 && p[2] <= 31) ||
        (p[3] == 192 && p[2] == 168) ||
        (nHostIP == 0x7F000001))
    {
        return TRUE;
    }

    return FALSE;
}

SOCKET tcp_create(BOOL isReuse)
{
    SOCKET s = socket(AF_INET, SOCK_STREAM, 0);
    if (s != INVALID_SOCKET)
    {
        if (isReuse)
        {
            BOOL bTrue = TRUE;
            setsockopt(s, SOL_SOCKET, SO_REUSEADDR, (char *) &bTrue, sizeof(bTrue));
        }
    }
    return s;
}

void tcp_destroy(SOCKET s)
{
    if (s != INVALID_SOCKET)
    {
        shutdown(s, 2);
        closesocket(s);
    }
}

BOOL tcp_bind(SOCKET s, unsigned short localPort, unsigned int localIP)
{
    return udp_bind(s, localPort, localIP);
}

BOOL tcp_server_listen(SOCKET s)
{
    if (s == INVALID_SOCKET || s == 0)
    {
        return FALSE;
    }

    if (listen(s, SOMAXCONN) < 0)
    {
        return FALSE;
    }

    return TRUE;
}

SOCKET tcp_server_accept(SOCKET s, unsigned int *clientIP, unsigned short *clientPort)
{
    struct sockaddr_in accept_sin;
    SOCKET sRet = 0;
    socklen_t accept_sin_len = 0;

    if (s == INVALID_SOCKET || s == 0)
    {
        return INVALID_SOCKET;
    }

    memset(&accept_sin, 0, sizeof(accept_sin));
    accept_sin_len = sizeof(accept_sin);
    sRet = accept(s, (struct sockaddr *) &accept_sin, &accept_sin_len);
    if (sRet != INVALID_SOCKET)
    {
        if (clientIP) *clientIP = ntohl(accept_sin.sin_addr.s_addr);
        if (clientPort) *clientPort = ntohs(accept_sin.sin_port);
    }
    return sRet;
}

BOOL tcp_client_connect(SOCKET s, unsigned int serverIP, unsigned short serverPort)
{
    struct sockaddr_in dest_sin;

    if (s == INVALID_SOCKET)
    {
        return FALSE;
    }

    dest_sin.sin_family = AF_INET;
    dest_sin.sin_addr.s_addr = htonl(serverIP);
    dest_sin.sin_port = htons(serverPort);
    if (connect(s, (struct sockaddr *) &dest_sin, sizeof(dest_sin)) != 0)
    {
        return FALSE;
    }

    return TRUE;
}

int tcp_send(SOCKET s, void *buf, int sendLen)
{
    if (s == INVALID_SOCKET || buf == NULL || sendLen <= 0)
    {
        return 0;
    }
    return send(s, (char *) buf, (size_t) sendLen, 0);
}

int tcp_receive(SOCKET s, void *buf, int bufLen, int timeout_milli_sec)
{
    if (s == INVALID_SOCKET || buf == NULL || bufLen <= 0)
    {
        return 0;
    }
    if (timeout_milli_sec >= 0)
    {
        struct timeval timeOut;
        fd_set fds;
        int ret = 0;

        FD_ZERO(&fds);
        FD_SET(s, &fds);

        timeOut.tv_sec = timeout_milli_sec / 1000;
        timeOut.tv_usec = (timeout_milli_sec % 1000) * 1000;

        ret = select(s + 1, &fds, NULL, NULL, &timeOut);
        if (ret <= 0)
        {
            return ret;
        }
    }
    return recv(s, (char *) buf, (size_t) bufLen, 0);
}
