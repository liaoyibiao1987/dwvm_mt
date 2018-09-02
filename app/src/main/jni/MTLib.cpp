//
// Created on 2016-1-22
//
#include <string.h>
#include "com_dy_dwvm_mt_MTLib.h"
#include "CMTLibInstance.h"

CMTLibInstance *g_lib = NULL;

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    start
 * Signature: (JIIIIILjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_dy_dwvm_1mt_MTLib_start
    (JNIEnv *env, jobject obj, jlong localDeviceId, jint udpPort, jint udpSocketBufferSize, jint netEncryptMode,
     jint encoderChannelNumber, jint decoderChannelNumber, jstring jszOptionText)
{
    xlog_set_prefix(FALSE, FALSE, "MTLib-jni");

    if (NULL != g_lib)
    {
        xlog(XLOG_LEVEL_ERROR, "start() failed: is working.\n");
        return JNI_FALSE;
    }

    JavaVM *javaVM = NULL;
    env->GetJavaVM(&javaVM);
    if (env->ExceptionCheck())
    {
        env->ExceptionClear();
        xlog(XLOG_LEVEL_ERROR, "start():: get JavaVM error.\n");
        return JNI_FALSE;
    }
    if (NULL == javaVM)
    {
        xlog(XLOG_LEVEL_ERROR, "start():: get JavaVM failed.\n");
        return JNI_FALSE;
    }

    jobject objNew = env->NewGlobalRef(obj);
    if (env->ExceptionCheck())
    {
        env->ExceptionClear();
        xlog(XLOG_LEVEL_ERROR, "start():: new obj-ref error.\n");
        return JNI_FALSE;
    }
    if (NULL == objNew)
    {
        xlog(XLOG_LEVEL_ERROR, "start():: new obj-ref failed.\n");
        return JNI_FALSE;
    }

    // new class CMTLibInstance
    g_lib = new CMTLibInstance(javaVM, objNew);
    if (NULL == g_lib)
    {
        xlog(XLOG_LEVEL_ERROR, "start():: new CMTLibInstance failed.\n");
        return JNI_FALSE;
    }
    if (!g_lib->IsValid())
    {
        delete g_lib;
        g_lib = NULL;
        xlog(XLOG_LEVEL_ERROR, "start():: invalidate parameters.\n");
        return JNI_FALSE;
    }

    // start instance
    CJStringToChar cjOptionText(env, jszOptionText);
    if (!g_lib->Start((DWORD) (localDeviceId & 0x00000000FFFFFFFF),
                      (WORD) (udpPort & 0x0000FFFF), udpSocketBufferSize, netEncryptMode,
                      encoderChannelNumber, decoderChannelNumber, cjOptionText))
    {
        delete g_lib;
        g_lib = NULL;
        return JNI_FALSE;
    }
    xlog(XLOG_LEVEL_WARNING, "start():: new obj-ref Java_com_dy_dwvm_1mt_MTLib_started.\n");
    return JNI_TRUE;
}

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    stop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_dy_dwvm_1mt_MTLib_stop
    (JNIEnv *, jobject)
{
    if (g_lib != NULL)
    {
        g_lib->Stop();
        delete g_lib;
        g_lib = NULL;
    }
}

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    isWorking
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_dy_dwvm_1mt_MTLib_isWorking
    (JNIEnv *, jobject)
{
    if (g_lib != NULL && g_lib->IsWorking())
    {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    setDeviceName
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_dy_dwvm_1mt_MTLib_setDeviceName
    (JNIEnv *env, jobject, jstring strDeviceName)
{
    if (g_lib == NULL)
    {
        return JNI_FALSE;
    }

    CJStringToChar cj(env, strDeviceName);
    const char *szName = cj;
    if (NULL == szName)
    {
        return JNI_FALSE;
    }

    if (!g_lib->SetDeviceName(szName))
    {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}
/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    setDeviceName
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_dy_dwvm_1mt_MTLib_resetDeviceID
        (JNIEnv *env, jobject, int jlong1DeviceID)
{
    if (g_lib == NULL)
    {
        return JNI_FALSE;
    }
    if (!g_lib->ResetDeviceID(&jlong1DeviceID))
    {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}
/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    dataEncrypt
 * Signature: ([BI[B)I
 */
JNIEXPORT jint JNICALL Java_com_dy_dwvm_1mt_MTLib_dataEncrypt
    (JNIEnv *env, jobject, jbyteArray jbaSrc, jint jiSrcLen, jbyteArray jbaDest)
{
    if (g_lib == NULL)
    {
        return 0;
    }

    if (jiSrcLen <= 0 ||
        Get_JByteArray_Len(env, jbaSrc) < jiSrcLen ||
        Get_JByteArray_Len(env, jbaDest) < jiSrcLen)
    {
        return 0;
    }

    CJByteArrayToPointer cjpSrc(env, jbaSrc);
    void *pSrcBuffer = cjpSrc.GetPointer();
    if (NULL == pSrcBuffer)
    {
        return 0;
    }

    CJByteArrayToPointer cjpDest(env, jbaDest);
    void *pDestBuffer = cjpDest.GetPointer();
    if (NULL == pDestBuffer)
    {
        return 0;
    }

    if (pDestBuffer != pSrcBuffer)
    {
        memcpy(pDestBuffer, pSrcBuffer, (size_t) jiSrcLen);
    }

    const int iResult = DWVM_Encrypt((unsigned char *) pDestBuffer, jiSrcLen);
    if (iResult <= 0)
    {
        return iResult;
    }

    Copy_Buffer_To_JByteArray(env, jbaDest, pDestBuffer, jiSrcLen);
    return iResult;
}

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    dataDecrypt
 * Signature: ([BI[B)I
 */
JNIEXPORT jint JNICALL Java_com_dy_dwvm_1mt_MTLib_dataDecrypt
    (JNIEnv *env, jobject, jbyteArray jbaSrc, jint jiSrcLen, jbyteArray jbaDest)
{
    if (g_lib == NULL)
    {
        return 0;
    }

    if (jiSrcLen <= 0 ||
        Get_JByteArray_Len(env, jbaSrc) < jiSrcLen ||
        Get_JByteArray_Len(env, jbaDest) < jiSrcLen)
    {
        return 0;
    }

    CJByteArrayToPointer cjpSrc(env, jbaSrc);
    void *pSrcBuffer = cjpSrc.GetPointer();
    if (NULL == pSrcBuffer)
    {
        return 0;
    }

    CJByteArrayToPointer cjpDest(env, jbaDest);
    void *pDestBuffer = cjpDest.GetPointer();
    if (NULL == pDestBuffer)
    {
        return 0;
    }

    if (pDestBuffer != pSrcBuffer)
    {
        memcpy(pDestBuffer, pSrcBuffer, (size_t) jiSrcLen);
    }

    const int iResult = DWVM_Decrypt((unsigned char *) pDestBuffer, jiSrcLen);
    if (iResult <= 0)
    {
        return iResult;
    }

    Copy_Buffer_To_JByteArray(env, jbaDest, pDestBuffer, jiSrcLen);
    return iResult;
}

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    stringEncrypt
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_dy_dwvm_1mt_MTLib_stringEncrypt
    (JNIEnv *env, jobject, jstring jszSrc)
{
    if (g_lib == NULL)
    {
        return Char_to_JString(env, "");
    }

    CJStringToChar cjSrc(env, jszSrc);
    const char *szSrc = cjSrc;
    if (NULL == szSrc)
    {
        return Char_to_JString(env, "");
    }

    char szDest[256] = {""};
    memset(szDest, 0, sizeof(szDest));
    strcpy(szDest, XStringEncrypt(szSrc));

    return Char_to_JString(env, szDest);
}

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    stringDecrypt
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_dy_dwvm_1mt_MTLib_stringDecrypt
    (JNIEnv *env, jobject, jstring jszSrc)
{
    if (g_lib == NULL)
    {
        return Char_to_JString(env, "");
    }

    CJStringToChar cjSrc(env, jszSrc);
    const char *szSrc = cjSrc;
    if (NULL == szSrc)
    {
        return Char_to_JString(env, "");
    }

    char szDest[256] = {""};
    memset(szDest, 0, sizeof(szDest));
    strcpy(szDest, XStringDecrypt(szSrc));

    return Char_to_JString(env, szDest);
}

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    sendUdpPacketToDevice
 * Signature: (JJJLjava/lang/String;[BI)I
 */
JNIEXPORT jint JNICALL Java_com_dy_dwvm_1mt_MTLib_sendUdpPacketToDevice
    (JNIEnv *env, jobject,
     jlong lPacketType, jlong lNeedReplay, jlong lDestDeviceId, jstring jszDestDeviceIpPort, jbyteArray jbaDataBuffer,
     jint iDataSize)
{
    if (g_lib == NULL)
    {
        return 0;
    }
    if (iDataSize <= 0)
    {
        return 0;
    }

    DWORD dwPacketType = (DWORD) (lPacketType & 0x00000000FFFFFFFF);
    DWORD dwNeedReply = (DWORD) (lNeedReplay & 0x00000000FFFFFFFF);
    DWORD dwDestDeviceId = (DWORD) (lDestDeviceId & 0x00000000FFFFFFFF);

    CJStringToChar cj(env, jszDestDeviceIpPort);
    const char *szDestDeviceIpPort = cj;
    if (szDestDeviceIpPort == NULL)
    {
        return 0;
    }

    CJByteArrayToPointer cp(env, jbaDataBuffer);
    BYTE *pDataBuffer = (BYTE *) cp.GetPointer();
    if (NULL == pDataBuffer || iDataSize > cp.GetLength())
    {
        return 0;
    }

    return g_lib->SendUdpPacketToDevice(dwPacketType, dwNeedReply, dwDestDeviceId, szDestDeviceIpPort, pDataBuffer,
                                        iDataSize);
}

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    sendOneFrameToDevice
 * Signature: (IJILjava/lang/String;Ljava/lang/String;[BIIII)I
 */
JNIEXPORT jint JNICALL Java_com_dy_dwvm_1mt_MTLib_sendOneFrameToDevice
    (JNIEnv *env, jobject,
     jint localEncoderChannelIndex, jlong remoteDeviceId, jint remoteDeviceDecoderChannelIndex,
     jstring remoteDeviceIpPort, jstring Codec, jbyteArray frameBuffer, jint frameBytes, jint imageResolution,
     jint imageWidth, jint imageHeight)
{
    if (g_lib == NULL)
    {
        xlog(XLOG_LEVEL_ERROR, "sendOneFrameToDevice(): lib is NULL.");
        return 0;
    }
    if (frameBytes <= 0)
    {
        xlog(XLOG_LEVEL_ERROR, "sendOneFrameToDevice(): invalid frameBytes %d", frameBytes);
        return 0;
    }

    DWORD dwRemoteDeviceId = (DWORD) (remoteDeviceId & 0x00000000FFFFFFFF);

    CJStringToChar cjIp(env, remoteDeviceIpPort);
    const char *szIp = cjIp;
    if (szIp == NULL)
    {
        xlog(XLOG_LEVEL_ERROR, "sendOneFrameToDevice(): get ip string failed.");
        return 0;
    }

    CJStringToChar cjCodec(env, Codec);
    const char *szCodec = cjCodec;
    if (NULL == szCodec)
    {
        xlog(XLOG_LEVEL_ERROR, "sendOneFrameToDevice(): get codec string failed.");
        return 0;
    }

    CJByteArrayToPointer cpFrame(env, frameBuffer);
    BYTE *pFrameData = (BYTE *) cpFrame.GetPointer();
    if (pFrameData == NULL || frameBytes > cpFrame.GetLength())
    {
        xlog(XLOG_LEVEL_ERROR, "sendOneFrameToDevice(): get frame-ptr failed.");
        return 0;
    }

    return g_lib->SendOneFrameToDevice(localEncoderChannelIndex, dwRemoteDeviceId, remoteDeviceDecoderChannelIndex,
                                       szIp, szCodec, pFrameData, frameBytes, imageResolution, imageWidth, imageHeight);
}
