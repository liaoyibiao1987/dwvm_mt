/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_dy_dwvm_mt_MTLib */

#ifndef _Included_com_dy_dwvm_mt_MTLib
#define _Included_com_dy_dwvm_mt_MTLib
#ifdef __cplusplus
extern "C" {
#endif
#undef com_dy_dwvm_mt_MTLib_IMAGE_RESOLUTION_CIF
#define com_dy_dwvm_mt_MTLib_IMAGE_RESOLUTION_CIF 0L
#undef com_dy_dwvm_mt_MTLib_IMAGE_RESOLUTION_D1
#define com_dy_dwvm_mt_MTLib_IMAGE_RESOLUTION_D1 1L
#undef com_dy_dwvm_mt_MTLib_IMAGE_RESOLUTION_QCIF
#define com_dy_dwvm_mt_MTLib_IMAGE_RESOLUTION_QCIF 2L
/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    start
 * Signature: (JIIIIILjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_dy_dwvm_1mt_MTLib_start
    (JNIEnv *, jobject, jlong, jint, jint, jint, jint, jint, jstring);

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    stop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_dy_dwvm_1mt_MTLib_stop
    (JNIEnv *, jobject);

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    isWorking
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_dy_dwvm_1mt_MTLib_isWorking
    (JNIEnv *, jobject);

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    setDeviceName
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_dy_dwvm_1mt_MTLib_setDeviceName
    (JNIEnv *, jobject, jstring);

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    setDeviceId
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_dy_dwvm_1mt_MTLib_setDeviceId
    (JNIEnv *, jobject, jlong);

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    dataEncrypt
 * Signature: ([BI[B)I
 */
JNIEXPORT jint JNICALL Java_com_dy_dwvm_1mt_MTLib_dataEncrypt
    (JNIEnv *, jobject, jbyteArray, jint, jbyteArray);

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    dataDecrypt
 * Signature: ([BI[B)I
 */
JNIEXPORT jint JNICALL Java_com_dy_dwvm_1mt_MTLib_dataDecrypt
    (JNIEnv *, jobject, jbyteArray, jint, jbyteArray);

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    stringEncrypt
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_dy_dwvm_1mt_MTLib_stringEncrypt
    (JNIEnv *, jobject, jstring);

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    stringDecrypt
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_dy_dwvm_1mt_MTLib_stringDecrypt
    (JNIEnv *, jobject, jstring);

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    sendUdpPacketToDevice
 * Signature: (JJJLjava/lang/String;[BI)I
 */
JNIEXPORT jint JNICALL Java_com_dy_dwvm_1mt_MTLib_sendUdpPacketToDevice
    (JNIEnv *, jobject, jlong, jlong, jlong, jstring, jbyteArray, jint);

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    sendOneFrameToDevice
 * Signature: (IJILjava/lang/String;Ljava/lang/String;[BIIII)I
 */
JNIEXPORT jint JNICALL Java_com_dy_dwvm_1mt_MTLib_sendOneFrameToDevice
    (JNIEnv *, jobject, jint, jlong, jint, jstring, jstring, jbyteArray, jint, jint, jint, jint);

/*
 * Class:     com_dy_dwvm_mt_MTLib
 * Method:    startTestNetSpeed
 * Signature: (JLjava/lang/String;I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_dy_dwvm_1mt_MTLib_startTestNetSpeed
    (JNIEnv *, jobject, jlong, jstring, jint);

#ifdef __cplusplus
}
#endif
#endif
