package com.dy.dwvm_mt.messagestructs;

public class s_messageBase {
    public static final int WVM_MAX_DEVICE_NAME_LEN = 24;     //设备名称的最大长度
    public static final int WVM_MAX_DEVICE_VERSION_LEN = 12;  //设备版本号的最大长度
    public static final int WVM_MAX_TELPHONE_CODE_LEN = 20;   //电话号码的最大长度
    public static final int WVM_MAX_USERNAME_LEN = 16;        //用户名的最大长度
    public static final int WVM_MAX_PASSWORD_LEN = 16;        //用户密码的最大长度
    public static final int WVM_MAX_IP_PORT_LEN = 24;         // IP地址+端口的最大长度
    public static final int WVM_MAX_PS_DEST_NUMBER = 12;      // PS转发时的一个源ID对应的最多目标ID数量
    public static final int WVM_MAX_MT_ENCODER_NUMBER = 64;   // MT编码通道的最大数量
    public static final int WVM_MAX_MT_DECODER_NUMBER = 64;   // MT解码通道的最大数量
    public static final int WVM_MAX_PARENT_PS_NUMBER = 4;     //设备所属的PS服务器列表数量
    public static final int WVM_MAX_PARENT_CMS_NUMBER = 4;    //设备所属的CMS服务器列表数量
    public static final int WVM_MAX_DEVICE_LIST_NAME_LEN = 100;    //设备列表名字最长数
    public static final int WVM_MAX_DEVICE_LIST_NUM = 15;     //设备列表一组最多数目
    public static final int WVM_MAX_COMMON_CMD_LEN = 10;       //普通命令使用的参数最长长度

    public static class DeviceCMD {
        public static final int WVM_CMD_POLLING = 1;
        public static final int WVM_CMD_REPLY = 2;
        public static final int WVM_CMD_TEST_NET = 3;

        public static final int WVM_CMD_DDNS_LOGIN = 101;
        public static final int WVM_CMD_DDNS_LOGIN_RESULT = 102;
        public static final int WVM_CMD_DDNS_LOGOUT = 103;
        public static final int WVM_CMD_DDNS_QUERY_TABLE = 104;
        public static final int WVM_CMD_DDNS_TABLE = 105;
        public static final int WVM_CMD_DDNS_TEMP_PS = 106;
        public static final int WVM_CMD_GET_CHILD_DEVICES = 107;
        public static final int WVM_CMD_CHILD_DEVICE_LIST = 108;
        public static final int WVM_CMD_PS_UPDATE_LIST = 109;

        public static final int WVM_CMD_PS_ID_MATCH = 202;
        public static final int WVM_CMD_PS_SET_ENC_STATUS = 203;
        public static final int WVM_CMD_PS_TRANSMIT = 201;
        public static final int WVM_CMD_PS_VA_FRAME = 204;
        public static final int WVM_CMD_PS_VA_RESEND = 205;

        public static final int WVM_CMD_USER_BASE = 900;
        public static final int WVM_CMD_DEBUG_SET = 901;
        public static final int WVM_CMD_DEBUG_UPLOAD = 902;
    }
}
