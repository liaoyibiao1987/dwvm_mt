//
// Created by root on 1/25/16.
//

#include <string.h>
#include "Session.h"


//==============================
//
// CSessionSender
//
//==============================

class m_FrameHeader;

class m_FrameHeader;

class m_szCodecName;

CSession::CSession(T_SESSION::ID sessionId, const char *szCodecName, SOCKET s, DWORD dwNetEncryptMode,
                   DWORD dwLocalDeviceId)
{
    memset(m_szCodecName, 0, sizeof(m_szCodecName));
    if (szCodecName)
    {
        Xstrcpy(m_szCodecName, szCodecName, sizeof(m_szCodecName));
    }

    m_id = sessionId;
    m_sock = s;
    m_dwNetEncryptMode = dwNetEncryptMode;
    m_dwLocalDeviceId = dwLocalDeviceId;

    m_pSender = NULL;
    m_pReceiver = NULL;

    m_tmLastPacketSeconds = 0;
}

CSession::~CSession()
{
    try
    {
        if (m_pSender)
        {
            delete m_pSender;
            m_pSender = NULL;
        }
    }
    catch (...)
    {
        xlog(XLOG_LEVEL_ERROR, "CSession::~CSession(): delete m_pSender error.");
    }

    try
    {
        if (m_pReceiver)
        {
            delete m_pReceiver;
            m_pReceiver = NULL;
        }
    }
    catch (...)
    {
        xlog(XLOG_LEVEL_ERROR, "CSession::~CSession(): delete m_pReceiver error.");
    }
}

//static
void *CSession::AllocAndCopy(const void *pSrc, const int iSrcSize, void **ppDest, int *pDestSize, const int iDestOffset)
{
    if (pSrc && iSrcSize > 0 && ppDest && pDestSize && iDestOffset >= 0)
    {
        void *pDest = *ppDest;
        int iDestSize = *pDestSize;
        if (iSrcSize > (iDestSize - iDestOffset))
        {
            if (pDest)
            {
                free(pDest);
                pDest = NULL;
                *ppDest = NULL;
            }
            iDestSize = 0;
            *pDestSize = 0;
        }
        if (pDest == NULL)
        {
            iDestSize = ((iDestOffset + iSrcSize + 0xFFF) & (~0xFFFL));
            pDest = malloc((size_t) iDestSize);
            if (NULL == pDest)
            {
                return NULL;
            }
            *ppDest = pDest;
            *pDestSize = iDestSize;
        }
        return memcpy(((char *) pDest) + iDestOffset, pSrc, (size_t) iSrcSize);
    }
    return NULL;
}

int CSession::SendFrame(DWORD dwRemoteIp, WORD wRemotePort, BYTE *pFrameBuffer, int iFrameBytes, int iWidth, int iHeight)
{
    // create sender at first
    if (SESSION_SENDER != m_id.eDir)
    {
        xlog(XLOG_LEVEL_ERROR, "CSession::%s(): eDir=%d failed.", __FUNCTION__, m_id.eDir);
        return 0;
    }
    if (NULL == m_pSender)
    {
        try
        {
            m_pSender = new CFrameSender(SENDER_CACHE_SECONDS);
        }
        catch (...)
        {
            m_pSender = NULL;
        }
        if (NULL == m_pSender)
        {
            xlog(XLOG_LEVEL_ERROR, "CSession::%s(): failed to new CFrameSender", __FUNCTION__);
            return 0;
        }
    }

    // push to child-class
    if (!Push(pFrameBuffer, iFrameBytes, iWidth, iHeight))
    {
        //xlog(XLOG_LEVEL_ERROR, "CSession::%s(): failed Push().", __FUNCTION__);
        return 0;
    }

    // update last-packet-time
    m_tmLastPacketSeconds = time(NULL);

    // get whole-frame-data from child-class
    int iResult = 0;
    void *pSendData = NULL;
    int iSendData = 0;
    while (PopLock(&pSendData, &iSendData, &iWidth, &iHeight))
    {
        iResult += m_pSender->Send(m_sock, m_dwNetEncryptMode, m_dwLocalDeviceId, (DWORD) m_id.iLocalChannelIndex,
                                   (DWORD) m_id.iLocalImageResolution, FALSE, m_id.dwRemoteDeviceId,
                                   (DWORD) m_id.iRemoteChannelIndex,
                                   dwRemoteIp, wRemotePort, pSendData);
        PopUnlock();
    }

    return iResult;
}


BOOL CSession::ReceivePacket(T_WVM_VA_FRAME_HEADER *pFrame, DWORD dwRemoteIp, WORD wRemotePort)
{
    if (SESSION_RECEIVER != m_id.eDir)
    {
        return FALSE;
    }

    if (NULL == m_pReceiver)
    {
        try
        {
            m_pReceiver = new CFrameReceiver();
        }
        catch (...)
        {
            m_pReceiver = NULL;
        }
        if (NULL == m_pReceiver)
        {
            return FALSE;
        }
    }

    // update last-packet-time
    m_tmLastPacketSeconds = time(NULL);

    return m_pReceiver->Push(pFrame, m_sock, m_id.dwRemoteDeviceId, dwRemoteIp, wRemotePort, m_dwLocalDeviceId, NULL);
}

BOOL CSession::GetReceivedFrame(void **ppFrameData, int *pDataSize, int *pImageResolution, int *pWidth, int *pHeight,
                                DWORD *pFrameType)
{
    if (NULL == m_pReceiver)
    {
        return FALSE;
    }
    if (NULL == ppFrameData || NULL == pDataSize)
    {
        return FALSE;
    }

    BYTE *pFrame = NULL;
    int iFrameSize = 0;
    DWORD dwFrameType = 0;
    DWORD dwImageRes = 0;
    if (!m_pReceiver->Pop((void **) &pFrame, &iFrameSize, &dwFrameType, &dwImageRes))
    {
        return FALSE;
    }

    tagSVStreamHeader2 *pHdr = (tagSVStreamHeader2 *) pFrame;
    if (VFT_AUDIO == dwFrameType)
    {
        switch (pHdr->CodecType)
        {
        case ACODEC_MP3:
            strcpy(m_szCodecName, CODEC_AUDIO_MP3);
            break;
        default:
            return FALSE;
        }
    }
    else if (VFT_PFRAME == dwFrameType || VFT_IFRAME == dwFrameType)
    {
        switch (pHdr->CodecType)
        {
        case VCODEC_FFH264:
        case VCODEC_STD_264:
            strcpy(m_szCodecName, CODEC_VIDEO_H264);
            break;
        default:
            return FALSE;
        }
    }

    *ppFrameData = pFrame + pHdr->HeadSize;
    *pDataSize = (int) pHdr->BufferSize;
    if (pImageResolution) *pImageResolution = (int) dwImageRes;
    if (pWidth) *pWidth = (int) pHdr->VideoWidth;
    if (pHeight) *pHeight = (int) pHdr->VideoHeight;
    if (pFrameType) *pFrameType = dwFrameType;

    return TRUE;
}

//==============================
//
// CSessionSender
//
//==============================

CSessionSender::CSessionSender(T_SESSION::ID sessionId, const char *szCodecName, SOCKET s, DWORD dwNetEncryptMode,
                               DWORD dwLocalDeviceId) :
    CSession(sessionId, szCodecName, s, dwNetEncryptMode, dwLocalDeviceId)
{
    memset(&m_FrameHeader, 0, sizeof(m_FrameHeader));
    m_FrameHeader.Code = SVSTREAM_STARTCODE;
    m_FrameHeader.Standard = 0;
    m_FrameHeader.Version = 0;
    m_FrameHeader.dwOrder = 0;
    m_FrameHeader.HeadSize = (WORD) sizeof(m_FrameHeader);

    m_FrameHeader.CodecType = 0;
    if (0 == strcmp(m_szCodecName, CODEC_VIDEO_H264))
    {
        m_FrameHeader.CodecType = VCODEC_FFH264;
    }
    else if (0 == strcmp(m_szCodecName, CODEC_AUDIO_MP3))
    {
        m_FrameHeader.CodecType = ACODEC_MP3;
    }

    m_pFrameBuffer = NULL;
    m_iFrameBufferSize = 0;
    m_iFrameSize = 0;
}

CSessionSender::~CSessionSender()
{
    if (m_pFrameBuffer)
    {
        free(m_pFrameBuffer);
        m_pFrameBuffer = NULL;
    }
    m_iFrameBufferSize = 0;
}

//static
void CSessionSender::UpdateFrameHeader(tagSVStreamHeader2 *pHdr, WORD wStreamType, void *pData, int iDataSize, int iWidth,
                                       int iHeight)
{
    pHdr->StreamType = wStreamType;
    pHdr->BufferSize = (DWORD) iDataSize;
    pHdr->PTS = get_current_ms2();
    pHdr->dwOrder++;
    pHdr->VideoWidth = (WORD) iWidth;
    pHdr->VideoHeight = (WORD) iHeight;

    pHdr->VerifyMode = 1;
    pHdr->VerifySumValue = 0;
    BYTE *pb = (BYTE *) pData;
    for (int i = 0; i < iDataSize; i++)
    {
        pHdr->VerifySumValue += pb[i];
    }

    time_t t = time(NULL);
    struct tm *ptm = localtime(&t);
    if (ptm)
    {
        pHdr->tmYear = (WORD) (ptm->tm_year + 1900);
        pHdr->tmMonth = (BYTE) (ptm->tm_mon + 1);
        pHdr->tmDay = (BYTE) ptm->tm_mday;
        pHdr->tmHour = (BYTE) ptm->tm_hour;
        pHdr->tmMinute = (BYTE) ptm->tm_min;
        pHdr->tmSecond = (BYTE) ptm->tm_sec;
    }
}

bool CSessionSender::Push(void *pData, int iDataSize, int iWidth, int iHeight)
{
    if (NULL == AllocAndCopy(pData, iDataSize, &m_pFrameBuffer, &m_iFrameBufferSize, sizeof(m_FrameHeader)))
    {
        return false;
    }

    const WORD wStreamType = (WORD) ((SESSION_AUDIO == m_id.eMedia) ? VFT_AUDIO : VFT_IFRAME);
    UpdateFrameHeader(&m_FrameHeader, wStreamType, pData, iDataSize, iWidth, iHeight);
    memcpy(m_pFrameBuffer, &m_FrameHeader, sizeof(m_FrameHeader));

    m_iFrameSize = iDataSize + sizeof(m_FrameHeader);
    return true;
}

bool CSessionSender::PopLock(void **ppBuffer, int *pSize, int *pWidth, int *pHeight)
{
    if (NULL == ppBuffer || NULL == pSize)
    {
        return false;
    }
    if (m_iFrameSize <= 0)
    {
        return false;
    }

    *ppBuffer = m_pFrameBuffer;
    *pSize = m_iFrameSize;
    if (pWidth) *pWidth = (int) m_FrameHeader.VideoWidth;
    if (pHeight) *pHeight = (int) m_FrameHeader.VideoHeight;

    return true;
}

void CSessionSender::PopUnlock()
{
    m_iFrameSize = 0;
}


//==============================
//
// CSessionSenderH264
//
//==============================

CSessionSenderH264::CSessionSenderH264(T_SESSION::ID sessionId, const char *szCodecName, SOCKET s, DWORD dwNetEncryptMode,
                                       DWORD dwLocalDeviceId) :
    CSessionSender(sessionId, szCodecName, s, dwNetEncryptMode, dwLocalDeviceId)
{
    memset(m_h264SPSBuffer, 0, sizeof(m_h264SPSBuffer));
    m_h264SPSLength = 0;
    memset(m_h264PPSBuffer, 0, sizeof(m_h264PPSBuffer));
    m_h264PPSLength = 0;
}

CSessionSenderH264::~CSessionSenderH264()
{
}

bool CSessionSenderH264::Push(void *pData, int iDataSize, int iWidth, int iHeight)
{
    const BYTE *pInputData = (BYTE *) pData;
    if (iDataSize <= 5)
    {
        xlog(XLOG_LEVEL_ERROR, "CSessionSenderH264::Push(): frame-size error: %d", iDataSize);
        return 0;
    }

    // H264 Start-Code: 00 00 00 01
    if (0x00 != pInputData[0] ||
        0x00 != pInputData[1] ||
        0x00 != pInputData[2] ||
        0x01 != pInputData[3])
    {
        xlog(XLOG_LEVEL_ERROR, "CSessionSenderH264::Push(): h264 start-code error: %02X %02X %02X %02X ",
             pInputData[0], pInputData[1], pInputData[2], pInputData[3]);
        return 0;
    }

    // invalidate nal-type
    const int nalType = (pInputData[4] & 0x1F);
    if (nalType != 1 && nalType != 5 && nalType != 7 && nalType != 8)
    {
        xlog(XLOG_LEVEL_ERROR, "CSessionSenderH264::Push(): error nal-type: %02X, data: %02X", nalType, pInputData[4]);
        return 0;
    }
    // check nal-type
    const bool isSPS = (nalType == 7);
    const bool isPPS = (nalType == 8);
    const bool isNeedAddSpsPps = (nalType == 5);
    const bool isKeyFrame = (nalType == 7 || nalType == 5);

    // H264 SPS & PPS
    if (isSPS)
    {
        if (iDataSize <= sizeof(m_h264SPSBuffer))
        {
            memcpy(m_h264SPSBuffer, pInputData, (size_t) iDataSize);
            m_h264SPSLength = iDataSize;
            return 0;
        }
        else
        {
            // find SPS-PPS end (IDR begin)
            int iLen = 0;
            for (int i = 4; i < (iDataSize - 5); i++)
            {
                if (0x00 == pInputData[i] &&
                    0x00 == pInputData[i + 1] &&
                    0x00 == pInputData[i + 2] &&
                    0x01 == pInputData[i + 3] &&
                    5 == (pInputData[i + 3] & 0x1F))
                {
                    iLen = i;
                    break;
                }
            }
            if (iLen <= 0)
            {
                return 0;
            }
            // copy
            memcpy(m_h264SPSBuffer, pInputData, (size_t) iLen);
            m_h264SPSLength = iLen;
            m_h264PPSLength = 0; // PPS has been included by this packet
        }
    }
    if (isPPS)
    {
        if (iDataSize <= sizeof(m_h264PPSBuffer))
        {
            memcpy(m_h264PPSBuffer, pInputData, (size_t) iDataSize);
            m_h264PPSLength = iDataSize;
        }
        return 0;
    }

    // no SPS-PPS
    if (isNeedAddSpsPps && (m_h264SPSLength + m_h264PPSLength) <= 0)
    {
        xlog(XLOG_LEVEL_ERROR, "CSessionSenderH264::Push(): NO valid SPS-PPS");
        return 0;
    }

    const int iPutSize = (iDataSize + (isNeedAddSpsPps ? (m_h264SPSLength + m_h264PPSLength) : 0));
    if ((iPutSize + sizeof(m_FrameHeader)) > m_iFrameBufferSize)
    {
        if (m_pFrameBuffer)
        {
            free(m_pFrameBuffer);
            m_pFrameBuffer = NULL;
        }
        m_iFrameBufferSize = 0;
    }
    if (m_pFrameBuffer == NULL)
    {
        m_iFrameBufferSize = ((size_t) iPutSize + sizeof(m_FrameHeader) + 0xFFF) & (~0xFFFL);
        m_pFrameBuffer = malloc((size_t) m_iFrameBufferSize);
        if (NULL == m_pFrameBuffer)
        {
            xlog(XLOG_LEVEL_ERROR, "CSessionSenderH264::Push(): malloc failed, size=%d", iPutSize);
            m_iFrameBufferSize = 0;
            return 0;
        }
    }

    BYTE *pDestStart = ((BYTE *) m_pFrameBuffer) + sizeof(m_FrameHeader);
    int iDestSize = 0;
    if (isNeedAddSpsPps)
    {
        if (m_h264SPSLength > 0)
        {
            memcpy(pDestStart + iDestSize, m_h264SPSBuffer, (size_t) m_h264SPSLength);
            iDestSize += m_h264SPSLength;
        }
        if (m_h264PPSLength > 0)
        {
            memcpy(pDestStart + iDestSize, m_h264PPSBuffer, (size_t) m_h264PPSLength);
            iDestSize += m_h264PPSLength;
        }
    }
    memcpy(pDestStart + iDestSize, pInputData, (size_t) iDataSize);
    iDestSize += iDataSize;

    const WORD wStreamType = (WORD) (isKeyFrame ? VFT_IFRAME : VFT_PFRAME);
    UpdateFrameHeader(&m_FrameHeader, wStreamType, pDestStart, iDestSize, iWidth, iHeight);
    memcpy(m_pFrameBuffer, &m_FrameHeader, sizeof(m_FrameHeader));

    m_iFrameSize = sizeof(m_FrameHeader) + iDestSize;
    return true;
}

int CSessionSenderH264::GetCodecInitDataLength()
{
    return m_h264SPSLength + m_h264PPSLength;
}

bool CSessionSenderH264::GetCodecInitData(void* pBuffer, const int iBufferSize, int* pDataSize)
{
    if((m_h264SPSLength + m_h264PPSLength) <= 0)
    {
        return false;
    }
    if(NULL == pBuffer || iBufferSize < (m_h264SPSLength + m_h264PPSLength))
    {
        return false;
    }

    if(m_h264SPSLength > 0)
    {
        memcpy(pBuffer, m_h264SPSBuffer, (size_t)m_h264SPSLength);
    }
    if(m_h264PPSLength > 0)
    {
        memcpy((char*)pBuffer + m_h264SPSLength, m_h264PPSBuffer, (size_t)m_h264PPSLength);
    }

    if(pDataSize)
    {
        *pDataSize = (m_h264SPSLength + m_h264PPSLength);
    }
    return false;
}

bool CSessionSenderH264::SetCodecInitData(void* pData, int iDataSize)
{
    if((m_h264SPSLength + m_h264PPSLength) > 0)
    {
        return false;
    }
    if(NULL == pData)
    {
        return false;
    }
    if(iDataSize <= 0 || iDataSize > sizeof(m_h264SPSBuffer))
    {
        return false;
    }

    memcpy(m_h264SPSBuffer, pData, (size_t)iDataSize);
    m_h264SPSLength = iDataSize;
    return true;
}


//==============================
//
// CSessionReceiver
//
//==============================

CSessionReceiver::CSessionReceiver(T_SESSION::ID sessionId, const char *szCodecName, SOCKET s, DWORD dwNetEncryptMode,
                                   DWORD dwLocalDeviceId) :
    CSession(sessionId, szCodecName, s, dwNetEncryptMode, dwLocalDeviceId)
{
}

CSessionReceiver::~CSessionReceiver()
{
}

bool CSessionReceiver::Push(void *pData, int iDataSize, int iWidth, int iHeight)
{
    return true;
}

bool CSessionReceiver::PopLock(void **ppBuffer, int *pSize, int *pWidth, int *pHeight)
{
    return false;
}

void CSessionReceiver::PopUnlock()
{
}
