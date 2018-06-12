//
// Created by xy on 1/25/16.
//

#ifndef DWVM_MT_SESSION_H
#define DWVM_MT_SESSION_H


#include "DWVM/DwvmBase.h"

#define MAX_SESSION_NUMBER  (256) // max session number of all, include video & audio, sender & receiver

#define SENDER_CACHE_SECONDS    (6) // cache 6 seconds data on sender buffer

#define CODEC_VIDEO_H264    "video/avc"
#define CODEC_AUDIO_MP3     "audio/mp3"

typedef enum
{
    SESSION_VIDEO = 0,
    SESSION_AUDIO = 1
} E_SESSION_MEDIA;

typedef enum
{
    SESSION_SENDER = 0,
    SESSION_RECEIVER = 1
} E_SESSION_DIR;

class CSession;

typedef struct
{
    struct ID
    {
        E_SESSION_MEDIA eMedia;
        E_SESSION_DIR eDir;
        int iLocalChannelIndex;
        int iLocalImageResolution;
        DWORD dwRemoteDeviceId;
        int iRemoteChannelIndex;
    } Id;
    CSession *pSessionObj;
} T_SESSION;


class CSession
{
public:
    CSession(T_SESSION::ID sessionId, const char *szCodecName, SOCKET s, DWORD dwNetEncryptMode, DWORD dwSrcDeviceId);

    ~CSession();

    static void *AllocAndCopy(const void *pSrc, const int iSrcSize, void **ppDest, int *pDestSize, const int iDestOffset);

    int SendFrame(DWORD dwRemoteIp, WORD wRemotePort, BYTE *pFrameBuffer, int iFrameBytes, int iWidth, int iHeight);

    BOOL ReceivePacket(T_WVM_VA_FRAME_HEADER *pFrame, DWORD dwRemoteIp, WORD wRemotePort);

    BOOL GetReceivedFrame(void **ppFrameData, int *pDataSize, int *pImageResolution, int *pWidth, int *pHeight, DWORD *pFrameType);

    CFrameSender *GetSender()
    { return m_pSender; }

    CFrameReceiver *GetReceiver()
    { return m_pReceiver; }

    const char *GetCodecName()
    { return m_szCodecName; }

    time_t GetLastPacketTime()
    { return m_tmLastPacketSeconds; }

    virtual int GetCodecInitDataLength()
    { return 0; }

    virtual bool GetCodecInitData(void* pBuffer, const int iBufferSize, int* pDataSize)
    { return false; }

    virtual bool SetCodecInitData(void* pData, int iDataSize)
    { return false; }

protected:
    virtual bool Push(void *pData, int iDataSize, int iWidth, int iHeight) = 0;

    virtual bool PopLock(void **ppBuffer, int *pSize, int *pWidth, int *pHeight) = 0;

    virtual void PopUnlock() = 0;

protected:
    char m_szCodecName[64];
    T_SESSION::ID m_id;
    SOCKET m_sock;
    DWORD m_dwNetEncryptMode;
    DWORD m_dwLocalDeviceId;
    CFrameSender *m_pSender;
    CFrameReceiver *m_pReceiver;
    time_t m_tmLastPacketSeconds;
};


class CSessionSender : public CSession
{
public:
    CSessionSender(T_SESSION::ID sessionId, const char *szCodecName, SOCKET s, DWORD dwNetEncryptMode, DWORD dwSrcDeviceId);

    ~CSessionSender();

    static void UpdateFrameHeader(tagSVStreamHeader2 *pHdr, WORD wStreamType, void *pData, int iDataSize, int iWidth, int iHeight);

protected:
    virtual bool Push(void *pData, int iDataSize, int iWidth, int iHeight);

    virtual bool PopLock(void **ppBuffer, int *pSize, int *pWidth, int *pHeight);

    virtual void PopUnlock();

protected:
    tagSVStreamHeader2 m_FrameHeader;
    void *m_pFrameBuffer;
    int m_iFrameBufferSize;
    int m_iFrameSize;
};


class CSessionSenderH264 : public CSessionSender
{
public:
    CSessionSenderH264(T_SESSION::ID sessionId, const char *szCodecName, SOCKET s, DWORD dwNetEncryptMode, DWORD dwSrcDeviceId);

    ~CSessionSenderH264();

protected:
    virtual bool Push(void *pData, int iDataSize, int iWidth, int iHeight);

public:
    virtual int GetCodecInitDataLength();

    virtual bool GetCodecInitData(void* pBuffer, const int iBufferSize, int* pDataSize);

    virtual bool SetCodecInitData(void* pData, int iDataSize);

protected:
    BYTE m_h264SPSBuffer[1024];
    int m_h264SPSLength;
    BYTE m_h264PPSBuffer[128];
    int m_h264PPSLength;
};


class CSessionReceiver : public CSession
{
public:
    CSessionReceiver(T_SESSION::ID sessionId, const char *szCodecName, SOCKET s, DWORD dwNetEncryptMode, DWORD dwSrcDeviceId);

    ~CSessionReceiver();

protected:
    virtual bool Push(void *pData, int iDataSize, int iWidth, int iHeight);

    virtual bool PopLock(void **ppBuffer, int *pSize, int *pWidth, int *pHeight);

    virtual void PopUnlock();
};

#endif //DWVM_MT_SESSION_H
