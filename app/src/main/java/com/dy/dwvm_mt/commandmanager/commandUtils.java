package com.dy.dwvm_mt.commandmanager;

import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.messagestructs.s_loginDDNS;
import com.dy.dwvm_mt.messagestructs.s_messageBase;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.javastruct.JavaStruct;

import java.nio.ByteOrder;
import java.sql.Struct;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/6/27.
 * PS: Not easy to write code, please indicate.
 */
public class commandUtils {
    public static final int MTPORT = 5007;
    public static final String DDNSIP = "112.91.151.187";
    public static final String DDNSPORT = "4998";
    public  static final int DDNSDEVICEID=16777217;

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

    public static final int WVM_CMD_DEBUG_SET = 901;
    public static final int WVM_CMD_DEBUG_UPLOAD = 902;
    public static final int WVM_CMD_USER_BASE = 900;

    private static I_MT_Prime mt_prime;

    public static void initSetupAdapter(I_MT_Prime adapter) {
        if (mt_prime == null) {
            mt_prime = adapter;
        }
    }

    public static final void sendLoginData(String loginID, String loginPw, String telNumber, String telZone, String ddnsIPAndPort) {
        s_loginDDNS loginstruct = new s_loginDDNS();

        loginstruct.setDwDeviceId(0);
        loginstruct.setLoginType(0);
        loginstruct.setDwDecoderChannelNumber(2);
        loginstruct.setSzDeviceName("MT".getBytes());
        loginstruct.setSzDeviceVersion("1.0.1".getBytes());

        byte[] e_loginid =new byte[s_messageBase.WVM_MAX_USERNAME_LEN];
        mt_prime.dataEncrypt(loginID.getBytes(),loginID.length(),e_loginid);

        byte[] e_loginpw =new byte[s_messageBase.WVM_MAX_PASSWORD_LEN];
        mt_prime.dataEncrypt(loginPw.getBytes(),loginPw.length(),e_loginpw);

        loginstruct.setSzUsernameEncrypt(e_loginid);
        loginstruct.setSzPasswordEncrypt(e_loginpw);
        loginstruct.setSzTelphoneCode(telNumber.getBytes());
        loginstruct.setSzTelphoneZone(telZone.getBytes());
        loginstruct.setLanIPAddr("172.16.100.56:5007".getBytes());

        try {
            byte[] databuff = JavaStruct.pack(loginstruct, ByteOrder.BIG_ENDIAN);
            LogUtils.d("sendLoginData : databuff" + databuff + " \r\n " + "datasize:" + databuff.length);
            mt_prime.sendUdpPacketToDevice2(WVM_CMD_DDNS_LOGIN, 0, DDNSDEVICEID, ddnsIPAndPort, databuff, databuff.length);
        } catch (Exception es) {
            LogUtils.e("sendLoginData error" + es.toString());
        }
    }
}
