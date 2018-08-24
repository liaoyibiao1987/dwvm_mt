package com.dy.dwvm_mt.commandmanager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.dy.dwvm_mt.Comlibs.DataPackShell;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.Comlibs.LocalSetting;
import com.dy.dwvm_mt.messagestructs.NetWorkCommand;
import com.dy.dwvm_mt.messagestructs.s_MT_TelState;
import com.dy.dwvm_mt.messagestructs.s_MT_Tel_ValidCode;
import com.dy.dwvm_mt.messagestructs.s_loginDDNS;
import com.dy.dwvm_mt.messagestructs.s_messageBase;
import com.dy.dwvm_mt.utilcode.util.CacheMemoryUtils;
import com.dy.dwvm_mt.utilcode.util.ConvertUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.NetworkUtils;
import com.dy.dwvm_mt.utilcode.util.PhoneUtils;
import com.dy.dwvm_mt.utilcode.util.StringUtils;
import com.dy.javastruct.JavaStruct;

import java.nio.ByteOrder;
import java.sql.Struct;
import java.util.List;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/6/27.
 * PS: Not easy to write code, please indicate.
 */
public class CommandUtils {
    public static int MTPORT = 5007;
    private static String DDNSIP = "112.90.144.6";
    //private static String DDNSIP = "112.91.151.187";
    private static String DDNSPORT = "4998";
    private static int DDNSDEVICEID = 16777217;

    public static String getDDNSIP() {
        return DDNSIP;
    }

    public static void setDDNSIP(String DDNSIP) {
        CommandUtils.DDNSIP = DDNSIP;
    }

    public static String getDDNSPORT() {
        return DDNSPORT;
    }

    public static void setDDNSPORT(String DDNSPORT) {
        CommandUtils.DDNSPORT = DDNSPORT;
    }

    public static int getDDNSDEVICEID() {
        return DDNSDEVICEID;
    }

    @NonNull
    public static String getDDNSIPPort() {
        if (StringUtils.isTrimEmpty(DDNSIP) == false && StringUtils.isTrimEmpty(DDNSPORT) == false) {
            return DDNSIP + ":" + DDNSPORT;
        } else {
            return "0.0.0.0:4998";
        }
    }

    public static void setDDNSIPPort(String IPPORT) {
        String[] ipport = IPPORT.split(":");
        if (ipport != null && ipport.length > 1) {
            setDDNSIP(ipport[0]);
            setDDNSPORT(ipport[1]);
        }
    }

    public static void setDDNSDEVICEID(int ddnsdeviceid) {
        CommandUtils.DDNSDEVICEID = ddnsdeviceid;
    }

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

    public static final String Str_Extra_Polling = "dy.mt.polling.m_interval";
    public static final String Str_Extra_Online = "dy.mt.polling.online";

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

        byte[] e_loginid = new byte[s_messageBase.WVM_MAX_USERNAME_LEN];
        mt_prime.dataEncrypt(loginID.getBytes(), loginID.length(), e_loginid);

        byte[] e_loginpw = new byte[s_messageBase.WVM_MAX_PASSWORD_LEN];
        mt_prime.dataEncrypt(loginPw.getBytes(), loginPw.length(), e_loginpw);

        loginstruct.setSzUsernameEncrypt(e_loginid);
        loginstruct.setSzPasswordEncrypt(e_loginpw);
        loginstruct.setSzTelphoneCode(telNumber.getBytes());
        loginstruct.setSzTelphoneZone(telZone.getBytes());
        loginstruct.setLanIPAddr(NetworkUtils.getIPAddress(true).getBytes());

        try {
            byte[] databuff = JavaStruct.pack(loginstruct, ByteOrder.BIG_ENDIAN);
            System.out.println("MT-SEND DATA LEN : " + databuff.length + " DATA: " + StringUtils.toHexBinary(databuff));
            mt_prime.sendUdpPacketToDevice2(WVM_CMD_DDNS_LOGIN, 0, DDNSDEVICEID, ddnsIPAndPort, databuff, databuff.length);
        } catch (Exception es) {
            LogUtils.e("sendLoginData error" + es.toString());
        }
    }

    public static final void sendPolling() {
        try {
            byte[] databuff = new byte[1];
            mt_prime.sendUdpPacketToDevice2(WVM_CMD_POLLING, 1, getDDNSDEVICEID(), getDDNSIPPort(), databuff, databuff.length);
            System.out.println("MT-SEND CMD:WVM_CMD_POLLING \r\n DATA LEN :" + databuff.length + " DATA:" + StringUtils.toHexBinary(databuff));
        } catch (Exception es) {
            LogUtils.e("sendPolling error" + es.toString());
        }
    }

    public static final void sendTelState(int telStates, int meetingID, @Nullable String telnumber) {
        s_MT_TelState s_mtStates = new s_MT_TelState();
        s_mtStates.CMD_Sub = s_messageBase.DeviceCMD_Sub.MT_Tel_States;
        s_mtStates.MeetingID = meetingID;
        s_mtStates.TelState = telStates;
        if (telnumber == null || telnumber.isEmpty()) {
            telnumber = PhoneUtils.getLine1Number();
            if (StringUtils.isTrimEmpty(telnumber) == false) {
                if (telnumber.startsWith("+86")) {
                    telnumber = telnumber.substring(3);
                } else {
                    telnumber = telnumber.substring(1);
                }
                LogUtils.d(String.format("telStates: %s  meetingID: %s  CMD_Sub: %s  telnumber: %s", telStates, meetingID, s_mtStates.CMD_Sub, telnumber));
            }
        }
        s_mtStates.Data = telnumber.getBytes();


        try {
            byte[] databuff = JavaStruct.pack(s_mtStates);//普通命令包用小端模式
            mt_prime.sendUdpPacketToDevice2(WVM_CMD_USER_BASE, 1, DDNSDEVICEID, getDDNSIPPort(), databuff, databuff.length);
            System.out.println("\r\n MT-SEND CMD:MT_Tel_States DATA LEN : " + databuff.length + " DATA: " + StringUtils.toHexBinary(databuff));
        } catch (Exception es) {
            LogUtils.e("sendLoginData error" + es.toString());
        }
    }

    public static final void sendVerifyCode(String verifyCode, String calledNumber) {
        int vCode = 0;
        try {
            String substr = verifyCode.substring((verifyCode.length() - 2), (verifyCode.length() - 1));
            vCode = Integer.valueOf(substr).intValue();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        s_MT_Tel_ValidCode s_mt_tel_validCode = new s_MT_Tel_ValidCode();
        s_mt_tel_validCode.CMD_Sub = s_messageBase.DeviceCMD_Sub.MT_Tel_ValidCode;
        s_mt_tel_validCode.Code = vCode;
        s_mt_tel_validCode.CalledNumber = calledNumber.getBytes();
        try {
            byte[] dataBuff = JavaStruct.pack(s_mt_tel_validCode);//普通命令包用小端模式
            mt_prime.sendUdpPacketToDevice2(WVM_CMD_USER_BASE, 1, DDNSDEVICEID, getDDNSIPPort(), dataBuff, dataBuff.length);
            System.out.println("\r\n MT-SEND CMD:MT_Tel_States DATA LEN : " + dataBuff.length + " DATA: " + StringUtils.toHexBinary(dataBuff));
        } catch (Exception es) {
            LogUtils.e("sendLoginData error" + es.toString());
        }
    }
}
