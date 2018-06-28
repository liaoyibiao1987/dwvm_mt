package com.dy.dwvm_mt.messagestructs;

import com.dy.javastruct.ForceLength;
import com.dy.javastruct.StructClass;
import com.dy.javastruct.StructField;

@StructClass
public class s_loginDDNS {
    public int getDwSize() {
        return dwSize;
    }

    public void setDwSize(int dwSize) {
        this.dwSize = dwSize;
    }

    public int getDwDeviceId() {
        return dwDeviceId;
    }

    public void setDwDeviceId(int dwDeviceId) {
        this.dwDeviceId = dwDeviceId;
    }

    public byte[] getSzUsernameEncrypt() {
        return szUsernameEncrypt;
    }

    public void setSzUsernameEncrypt(byte[] szUsernameEncrypt) {
        this.szUsernameEncrypt = szUsernameEncrypt;
    }

    public byte[] getSzPasswordEncrypt() {
        return szPasswordEncrypt;
    }

    public void setSzPasswordEncrypt(byte[] szPasswordEncrypt) {
        this.szPasswordEncrypt = szPasswordEncrypt;
    }

    public byte[] getSzTelphoneZone() {
        return szTelphoneZone;
    }

    public void setSzTelphoneZone(byte[] szTelphoneZone) {
        this.szTelphoneZone = szTelphoneZone;
    }

    public byte[] getSzTelphoneCode() {
        return szTelphoneCode;
    }

    public void setSzTelphoneCode(byte[] szTelphoneCode) {
        this.szTelphoneCode = szTelphoneCode;
    }

    public byte[] getSzDeviceVersion() {
        return szDeviceVersion;
    }

    public void setSzDeviceVersion(byte[] szDeviceVersion) {
        this.szDeviceVersion = szDeviceVersion;
    }

    public byte[] getSzDeviceName() {
        return szDeviceName;
    }

    public void setSzDeviceName(byte[] szDeviceName) {
        this.szDeviceName = szDeviceName;
    }

    public int getDwEncoderChannelNumber() {
        return dwEncoderChannelNumber;
    }

    public void setDwEncoderChannelNumber(int dwEncoderChannelNumber) {
        this.dwEncoderChannelNumber = dwEncoderChannelNumber;
    }

    public int getDwDecoderChannelNumber() {
        return dwDecoderChannelNumber;
    }

    public void setDwDecoderChannelNumber(int dwDecoderChannelNumber) {
        this.dwDecoderChannelNumber = dwDecoderChannelNumber;
    }

    public int getDwDeviceExtSize() {
        return dwDeviceExtSize;
    }

    public void setDwDeviceExtSize(int dwDeviceExtSize) {
        this.dwDeviceExtSize = dwDeviceExtSize;
    }

    public int getLoginType() {
        return LoginType;
    }

    public void setLoginType(int loginType) {
        LoginType = loginType;
    }

    public byte[] getLanIPAddr() {
        return LanIPAddr;
    }

    public void setLanIPAddr(byte[] lanIPAddr) {
        LanIPAddr = lanIPAddr;
    }

    /// <summary>
    /// 本结构的长度（不包括扩展信息）
    /// </summary>
    @StructField(order = 0)
    public int dwSize;

    /// <summary>
    /// 设备ID
    /// </summary>
    @StructField(order = 1)
    public int dwDeviceId;

    /// <summary>
    /// 登录用户名，密文
    /// </summary>
    @StructField(order = 2)
    @ForceLength(forceLen = s_messageBase.WVM_MAX_USERNAME_LEN)
    public byte[] szUsernameEncrypt = new byte[s_messageBase.WVM_MAX_USERNAME_LEN];

    /// <summary>
    /// 登录密码，密文
    /// </summary>
    @StructField(order = 3)
    @ForceLength(forceLen = s_messageBase.WVM_MAX_PASSWORD_LEN)
    public byte[] szPasswordEncrypt = new byte[s_messageBase.WVM_MAX_PASSWORD_LEN];

    /// <summary>
    /// 设备对应的电话区号
    /// </summary>
    @StructField(order = 4)
    @ForceLength(forceLen = s_messageBase.WVM_MAX_TELPHONE_CODE_LEN)
    public byte[] szTelphoneZone = new byte[s_messageBase.WVM_MAX_TELPHONE_CODE_LEN];

    /// <summary>
    /// 设备对应的电话号码
    /// </summary>
    @StructField(order = 5)
    @ForceLength(forceLen = s_messageBase.WVM_MAX_TELPHONE_CODE_LEN)
    public byte[] szTelphoneCode = new byte[s_messageBase.WVM_MAX_TELPHONE_CODE_LEN];

    /// <summary>
    /// 设备版本号
    /// </summary>
    @StructField(order = 6)
    @ForceLength(forceLen = s_messageBase.WVM_MAX_DEVICE_VERSION_LEN)
    public byte[] szDeviceVersion = new byte[s_messageBase.WVM_MAX_DEVICE_VERSION_LEN];

    /// <summary>
    /// 设备名称
    /// </summary>
    @StructField(order = 7)
    @ForceLength(forceLen = s_messageBase.WVM_MAX_DEVICE_NAME_LEN)
    public byte[] szDeviceName = new byte[s_messageBase.WVM_MAX_DEVICE_NAME_LEN];

    /// <summary>
    /// 编码通道数量。仅对MT,MP,LSS有效
    /// </summary>
    @StructField(order = 8)
    public int dwEncoderChannelNumber;

    /// <summary>
    /// 解码通道数量。仅对MT,MP,LSS有效
    /// </summary>
    @StructField(order = 9)
    public int dwDecoderChannelNumber;

    /// <summary>
    /// 结构体后跟着的设备扩展信息的长度
    /// </summary>
    @StructField(order = 10)
    public int dwDeviceExtSize;

    /// <summary>
    /// 登录类型 0为人工登录 1为自动登录
    /// </summary>
    @StructField(order = 11)
    public int LoginType;

    /// <summary>
    /// 登录类型 0为人工登录 1为自动登录
    /// </summary>
    @StructField(order = 12)
    @ForceLength(forceLen = s_messageBase.WVM_MAX_IP_PORT_LEN)
    public byte[] LanIPAddr;

}
