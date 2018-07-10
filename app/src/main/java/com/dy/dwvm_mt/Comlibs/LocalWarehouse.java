package com.dy.dwvm_mt.Comlibs;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/10.
 * PS: Not easy to write code, please indicate.
 */
public final class LocalWarehouse {
    private static boolean isLogined;
    private static boolean isLocalTel;
    private static boolean forcePSTranspond;
    private static String affiliateToPS;
    private static int deviceId;
    private static int LoginTimeElapse;

    public static void SetInformationByLoginResult(LoginExtMessageDissector.LoginExtMessage loginExtMessage) {
        setAffiliateToPS(loginExtMessage.getPSIPPort());
        setDeviceId(loginExtMessage.getDeviceId());
        setForcePSTranspond(loginExtMessage.isForcePSTranspond());
        setIsLocalTel(loginExtMessage.isLocalTel());
    }

    public static void ResetInformation() {
        isLogined = false;
        isLocalTel = false;
        forcePSTranspond = false;
        affiliateToPS = "";
        deviceId = -1;
        LoginTimeElapse = -1;
    }

    public static boolean isIsLogined() {
        return isLogined;
    }

    public static void setIsLogined(boolean isLogined) {
        LocalWarehouse.isLogined = isLogined;
    }

    public static boolean isIsLocalTel() {
        return isLocalTel;
    }

    public static void setIsLocalTel(boolean isLocalTel) {
        LocalWarehouse.isLocalTel = isLocalTel;
    }

    public static boolean isForcePSTranspond() {
        return forcePSTranspond;
    }

    public static void setForcePSTranspond(boolean forcePSTranspond) {
        LocalWarehouse.forcePSTranspond = forcePSTranspond;
    }

    public static String getAffiliateToPS() {
        return affiliateToPS;
    }

    public static void setAffiliateToPS(String affiliateToPS) {
        LocalWarehouse.affiliateToPS = affiliateToPS;
    }

    public static int getDeviceId() {
        return deviceId;
    }

    public static void setDeviceId(int deviceId) {
        LocalWarehouse.deviceId = deviceId;
    }

    public static int getLoginTimeElapse() {
        return LoginTimeElapse;
    }

    public static void setLoginTimeElapse(int loginTimeElapse) {
        LoginTimeElapse = loginTimeElapse;
    }
}
