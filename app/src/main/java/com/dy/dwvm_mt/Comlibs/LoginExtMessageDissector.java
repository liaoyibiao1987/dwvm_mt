package com.dy.dwvm_mt.Comlibs;

import com.dy.dwvm_mt.messagestructs.s_loginResultDDNS;
import com.dy.dwvm_mt.utilcode.util.ConvertUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.StringUtils;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/10.
 * PS: Not easy to write code, please indicate.
 */
public final class LoginExtMessageDissector {

    public static final LoginExtMessage getLoginExtMessage(s_loginResultDDNS loginResult) {
        LoginExtMessage ret = new LoginExtMessage();
        try {
           int extData= loginResult.getDwDeviceExtSize();
           int psIP = loginResult.getDwParentPsIDs();

            short[] shorts = ConvertUtils.spitIntToUshort(extData);
            boolean islocaltel = (shorts[0] & 1) == 1;
            boolean forcepstranspond = (shorts[0] & 2) == 2;
            int psUDPport = shorts[1];
            String ipport = ConvertUtils.intIPToString(psIP, psUDPport);
            ret.setLocalTel(islocaltel);
            ret.setForcePSTranspond(forcepstranspond);
            ret.setPSIPPort(ipport);
            ret.setDeviceId(loginResult.getDwDeviceId());
        } catch (Exception es) {
            LogUtils.e("LoginExtMessageDissector getLoginExtMessage error " + es);
        }
        return ret;
    }

    public static class LoginExtMessage {
        private boolean IsLocalTel;
        private int DeviceId;
        private boolean ForcePSTranspond;
        private String PSIPPort;

        public boolean isLocalTel() {
            return IsLocalTel;
        }

        public void setLocalTel(boolean localTel) {
            IsLocalTel = localTel;
        }

        public boolean isForcePSTranspond() {
            return ForcePSTranspond;
        }

        public int getDeviceId() {
            return DeviceId;
        }

        public void setDeviceId(int deviceId) {
            DeviceId = deviceId;
        }

        public void setForcePSTranspond(boolean forcePSTranspond) {
            ForcePSTranspond = forcePSTranspond;
        }

        public String getPSIPPort() {
            return PSIPPort;
        }

        public void setPSIPPort(String PSIPPort) {
            this.PSIPPort = PSIPPort;
        }
/* public ExtendLoginMessage(int data)
        {
            ushort[] vals = StringHelper.SpitIntToUshort(data);

            IsLocalTel = (vals[0] & 1) == 1;
            ForcePSTranspond = (vals[0] & 2) == 2;
            PSUdpPort = vals[1];
        }*/
    }
}
