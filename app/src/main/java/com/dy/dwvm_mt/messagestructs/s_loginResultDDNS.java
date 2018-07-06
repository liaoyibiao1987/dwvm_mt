package com.dy.dwvm_mt.messagestructs;

import com.dy.javastruct.ForceLength;
import com.dy.javastruct.StructClass;
import com.dy.javastruct.StructField;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/6/25.
 * PS: Not easy to write code, please indicate.
 */
@StructClass
public class s_loginResultDDNS {
    /// <summary>
    /// 本结构的长度（不包括扩展信息）
    /// </summary>
    @StructField(order = 0)
    private int dwSize;

    /// <summary>
    /// 分配给设备的ID，0表示登录失败。DDNS会根据设备发来的用户名分配一个ID给设备。
    /// </summary>
    @StructField(order = 1)
    private int dwDeviceId;

    /// <summary>
    /// 登录的成功、失败状态。0-成功，其他-失败
    /// WVM_LOGIN_RET_SUCCEED	    0	成功
    /// WVM_LOGIN_RET_ERROR_USER	1	用户名不存在
    /// WVM_LOGIN_RET_ERROR_PASS	2	密码错误
    /// WVM_LOGIN_RET_ERROR_VER	    3	版本不匹配
    /// WVM_LOGIN_RET_UNEXPECTED	4	内部错误
    /// </summary>
    @StructField(order = 2)
    private int dwErrorCode;

    /// <summary>
    /// 设备向DDNS登录的周期，单位：毫秒
    /// </summary>
    @StructField(order = 3)
    private int dwLoginTimeElapse;

    /// <summary>
    /// DDNS的名称
    /// </summary>
    @StructField(order = 4)
    @ForceLength(forceLen = s_messageBase.WVM_MAX_DEVICE_NAME_LEN)
    private byte[] szDDNSName = new byte[s_messageBase.WVM_MAX_DEVICE_NAME_LEN];

    /// <summary>
    /// DDNS的版本号
    /// </summary>
    @StructField(order = 5)
    @ForceLength(forceLen = s_messageBase.WVM_MAX_DEVICE_VERSION_LEN)
    private byte[] szDDNSVersion = new byte[s_messageBase.WVM_MAX_DEVICE_VERSION_LEN];

    /// <summary>
    /// 设备的公网IP地址和端口
    /// </summary>
    @StructField(order = 6)
    @ForceLength(forceLen = s_messageBase.WVM_MAX_IP_PORT_LEN)
    private byte[] szDeviceWanIp= new byte[s_messageBase.WVM_MAX_IP_PORT_LEN];

    /// <summary>
    /// 设备所属的PS服务器列表
    /// NOTE: 当回复给终端时，表示的是所属PS的IP地址（Uint 表达形式）
    /// </summary>
    @StructField(order = 7)
    private int dwParentPsIDs;

    /// <summary>
    /// 设备所属的CMS服务器列表
    /// </summary>
    @StructField(order = 8)
    private int dwParentCmsIDs;

    /// <summary>
    /// 本结构之后跟随的扩展信息的长度
    /// </summary>
    @StructField(order = 9)
    private int dwDeviceExtSize;

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

    public int getDwErrorCode() {
        return dwErrorCode;
    }

    public void setDwErrorCode(int dwErrorCode) {
        this.dwErrorCode = dwErrorCode;
    }

    public int getDwLoginTimeElapse() {
        return dwLoginTimeElapse;
    }

    public void setDwLoginTimeElapse(int dwLoginTimeElapse) {
        this.dwLoginTimeElapse = dwLoginTimeElapse;
    }

    public byte[] getSzDDNSName() {
        return szDDNSName;
    }

    public void setSzDDNSName(byte[] szDDNSName) {
        this.szDDNSName = szDDNSName;
    }

    public byte[] getSzDDNSVersion() {
        return szDDNSVersion;
    }

    public void setSzDDNSVersion(byte[] szDDNSVersion) {
        this.szDDNSVersion = szDDNSVersion;
    }

    public byte[] getSzDeviceWanIp() {
        return szDeviceWanIp;
    }

    public void setSzDeviceWanIp(byte[] szDeviceWanIp) {
        this.szDeviceWanIp = szDeviceWanIp;
    }

    public int getDwParentPsIDs() {
        return dwParentPsIDs;
    }

    public void setDwParentPsIDs(int dwParentPsIDs) {
        this.dwParentPsIDs = dwParentPsIDs;
    }

    public int getDwParentCmsIDs() {
        return dwParentCmsIDs;
    }

    public void setDwParentCmsIDs(int dwParentCmsIDs) {
        this.dwParentCmsIDs = dwParentCmsIDs;
    }

    public int getDwDeviceExtSize() {
        return dwDeviceExtSize;
    }

    public void setDwDeviceExtSize(int dwDeviceExtSize) {
        this.dwDeviceExtSize = dwDeviceExtSize;
    }
}
