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

    public static final int WVM_MAX_TELPSTATE_CODE_LEN = 20;    //电话状态

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

    public static class DeviceCMD_Sub
    {
        public static final int CMS_Video_Monitor = 91;
        public static final int CMS_Video_Monitor_List = 92;
        public static final int DDNS_DeviceLoginStates = 9;
        public static final int DDNS_ExitMeeting = 16;
        public static final int DDNS_HoldMeeting = 14;
        public static final int DDNS_HoldMeeting_Result = 12;
        public static final int DDNS_MeetingInfo = 19;
        public static final int DDNS_MT_OtherStates = 26;
        public static final int DDNS_MTApplyChairMan = 17;
        public static final int DDNS_MTInfo = 18;
        public static final int DDNS_PBX_Exit_Meeting = 2;
        public static final int DDNS_PBX_Identity_Switch = 4;
        public static final int DDNS_PBX_Manage_Member = 5;
        public static final int DDNS_PBX_Officially_Start_Meeting = 3;
        public static final int DDNS_PBX_ReSet = 6;
        public static final int DDNS_PBX_Start_Meeting = 1;
        public static final int DDNS_PStransmits = 10;
        public static final int DDNS_SendEncodeInfo = 25;
        public static final int DDNS_SendMemberStates = 13;
        public static final int DDNS_SetCoding = 8;
        public static final int DDNS_StatesMsg = 11;
        public static final int DDNS_Tel_ValidCode = 7;
        public static final int DDNS_Upgrade = 20;
        public static final int DDNS_Upgrade_Force = 21;
        public static final int DDNS_WhiteBoardConvertFileStates = 24;
        public static final int DDNS_WhiteBoardSycDada = 22;
        public static final int DDNS_WhiteBoardSycDada_ForPage = 23;
        public static final int DDNSS_MeetingApply_Result = 15;
        public static final int MT_EncodeInfo = 139;
        public static final int MT_FinishMeeting = 132;
        public static final int MT_GetMeetingList = 134;
        public static final int MT_GetUpgradeInfo = 135;
        public static final int MT_MeetingExit = 130;
        public static final int MT_MeetingMemberManage = 126;
        public static final int MT_MeetingSwitchVideo = 128;
        public static final int MT_NetSwitch = 140;
        public static final int MT_NoticFormalMeeting = 123;
        public static final int MT_StartMeeting = 125;
        public static final int MT_SwitchIdentity = 127;
        public static final int MT_SwitchIdentity_Result = 131;
        public static final int MT_Tel_States = 121;
        public static final int MT_Tel_ValidCode = 122;
        public static final int MT_UpCoding_States = 124;
        public static final int MT_WhiteBoardLogin = 138;
        public static final int MT_WhiteBoardOption = 136;
        public static final int MT_WhiteBoardpFiles = 137;
        public static final int PBX_Exit_Meeting = 63;
        public static final int PBX_InitSource = 61;
        public static final int PBX_SyncSource = 64;
        public static final int PBX_Tel_States = 62;
        public static final int S_MT_Switch_VideoList = 133;
    }


    public class DDNS_StatesMsg
    {
        public static final int ConnectComplete = 2;
        public static final int LoginOffline = 3;
        public static final int PBX_Reset = 4;
        public static final int PBX_SyncResource = 5;
        public static final int ReLogin = 1;
        public static final int TelHasUse = 6;
        public static final int WhiteBoardError = 7;
    }
}
