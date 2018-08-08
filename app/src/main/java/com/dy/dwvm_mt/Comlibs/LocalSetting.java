package com.dy.dwvm_mt.Comlibs;

import com.dy.dwvm_mt.utilcode.util.CacheDoubleUtils;
import com.dy.javastruct.Constants;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/10.
 * PS: Not easy to write code, please indicate.
 */
public final class LocalSetting {

    public static final String Cache_Name_LoginID = "Cache_Name_LoginID";
    public static final String Cache_Name_Password = "Cache_Name_Password";
    public static final String Cache_Name_TelNumber = "Cache_Name_TelNumber";
    public static final String Cache_Name_CallingTelNumber = "Cache_Name_CallingTelNumber";
    public static final String Cache_Name_PSIPPort = "Cache_Name_PSIPPort";

    private static String loginID = "";
    private static String loginPSW = "";
    private static String telNumber = "";

    private static boolean isLogined;
    private static boolean isLocalTel;
    private static boolean forcePSTranspond;
    private static String affiliateToPS;
    private static int deviceId;
    private static int LoginTimeElapse;
    private static int callState;
    private static int meetingID;

    private static CacheDoubleUtils cacheDoubleUtils;

    public static void SetInformationByLoginResult(LoginExtMessageDissector.LoginExtMessage loginExtMessage) {
        setAffiliateToPS(loginExtMessage.getPSIPPort());
        setDeviceId(loginExtMessage.getDeviceId());
        setForcePSTranspond(loginExtMessage.isForcePSTranspond());
        setIsLocalTel(loginExtMessage.isLocalTel());
        getCacheDoubleUtils().put(Cache_Name_PSIPPort, loginExtMessage.getPSIPPort());
    }

    public static CacheDoubleUtils getCacheDoubleUtils() {
        if (cacheDoubleUtils == null) {
            cacheDoubleUtils = CacheDoubleUtils.getInstance();
        }
        return cacheDoubleUtils;
    }

    public static void ResetInformation() {
        isLogined = false;
        isLocalTel = false;
        forcePSTranspond = false;
        affiliateToPS = "";
        deviceId = -1;
        LoginTimeElapse = -1;
    }

    public static String getPSIPPort(){
        return getCacheDoubleUtils().getString(Cache_Name_PSIPPort);
    }

    public static String getLoginID() {
        return loginID;
    }

    public static void setLoginID(String loginID) {
        LocalSetting.loginID = loginID;
    }

    public static String getLoginPSW() {
        return loginPSW;
    }

    public static void setLoginPSW(String loginPSW) {
        LocalSetting.loginPSW = loginPSW;
    }

    public static String getTelNumber() {
        return telNumber;
    }

    public static void setTelNumber(String telNumber) {
        LocalSetting.telNumber = telNumber;
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
