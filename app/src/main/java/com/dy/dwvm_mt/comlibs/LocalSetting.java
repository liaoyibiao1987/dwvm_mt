package com.dy.dwvm_mt.comlibs;

import com.dy.dwvm_mt.utilcode.util.CacheMemoryUtils;
import com.dy.dwvm_mt.utilcode.util.SPUtils;

import java.lang.invoke.ConstantCallSite;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/10.
 * PS: Not easy to write code, please indicate.
 */
public final class LocalSetting {
    public static final String SPUtilsName = "DYSP";

    public static final String Cache_Name_LoginID = "Cache_Name_LoginID";
    public static final String Cache_Name_Password = "Cache_Name_Password";
    public static final String Cache_Name_TelNumber = "Cache_Name_TelNumber";
    public static final String Cache_Name_CallingTelNumber = "Cache_Name_CallingTelNumber";
    public static final String Cache_Name_PSIPPort = "Cache_Name_PSIPPort";

    public static final String StartLoginType = "StartLoginType";

    private static boolean isLogined;
    private static boolean isLocalTel;
    private static boolean forcePSTranspond;
    private static String affiliateToPS;
    private static int deviceId;
    private static int LoginTimeElapse;
    private static int callState;
    private static int meetingID;

    private static SPUtils spUtils =  SPUtils.getInstance(SPUtilsName);
    private static CacheMemoryUtils cacheMemoryUtils = CacheMemoryUtils.getInstance();

    public static void SetInformationByLoginResult(LoginExtMessageDissector.LoginExtMessage loginExtMessage) {
        setAffiliateToPS(loginExtMessage.getPSIPPort());
        setDeviceId(loginExtMessage.getDeviceId());
        setForcePSTranspond(loginExtMessage.isForcePSTranspond());
        setIsLocalTel(loginExtMessage.isLocalTel());
        cacheMemoryUtils.put(Cache_Name_PSIPPort, loginExtMessage.getPSIPPort());
    }

    public static void ResetInformation() {
        isLogined = false;
        isLocalTel = false;
        forcePSTranspond = false;
        affiliateToPS = "";
        deviceId = -1;
        LoginTimeElapse = -1;
    }
    public static SPUtils getDYSPUtil(){
        return spUtils;
    }

    public static String getPSIPPort() {
        return cacheMemoryUtils.get(Cache_Name_PSIPPort);
    }

    public static String getLoginID() {
       return spUtils.getString(Cache_Name_LoginID);
    }

    public static void setLoginID(String loginID) {
        spUtils.put(Cache_Name_LoginID,loginID);
    }

    public static String getLoginPSW() {
        return spUtils.getString(Cache_Name_Password);
    }

    public static void setLoginPSW(String loginPSW) {
        spUtils.put(Cache_Name_Password,loginPSW);
    }

    public static String getTelNumber() {
        return spUtils.getString(Cache_Name_TelNumber);
    }

    public static void setTelNumber(String telNumber) {
        spUtils.put(Cache_Name_TelNumber,telNumber);
    }

    public static boolean isIsLogined() {
        return isLogined;
    }

    public static void setIsLogined(boolean isLogined) {
        LocalSetting.isLogined = isLogined;
    }

    public static boolean isIsLocalTel() {
        return isLocalTel;
    }

    public static void setIsLocalTel(boolean isLocalTel) {
        LocalSetting.isLocalTel = isLocalTel;
    }

    public static boolean isForcePSTranspond() {
        return forcePSTranspond;
    }

    public static void setForcePSTranspond(boolean forcePSTranspond) {
        LocalSetting.forcePSTranspond = forcePSTranspond;
    }

    public static String getAffiliateToPS() {
        return affiliateToPS;
    }

    public static void setAffiliateToPS(String affiliateToPS) {
        LocalSetting.affiliateToPS = affiliateToPS;
    }

    public static int getDeviceId() {
        return deviceId;
    }

    public static void setDeviceId(int deviceId) {
        LocalSetting.deviceId = deviceId;
    }

    public static int getLoginTimeElapse() {
        return LoginTimeElapse;
    }

    public static void setLoginTimeElapse(int loginTimeElapse) {
        LoginTimeElapse = loginTimeElapse;
    }

    public static int getCallState() {
        return callState;
    }

    public static void setCallState(int callState) {
        LocalSetting.callState = callState;
    }


    public static int getMeetingID() {
        return meetingID;
    }

    public static void setMeetingID(int meetingID) {
        LocalSetting.meetingID = meetingID;
    }
}
