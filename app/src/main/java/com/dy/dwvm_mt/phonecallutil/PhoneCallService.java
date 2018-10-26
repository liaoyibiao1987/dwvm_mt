package com.dy.dwvm_mt.phonecallutil;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.utilcode.util.ActivityUtils;

/**
 * description: 监听电话通信状态的服务，实现该类的同时必须提供电话管理的UI
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class PhoneCallService extends InCallService {

    private Call.Callback callback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            switch (state) {
                case Call.STATE_ACTIVE: {
                    if (call.getState() == Call.STATE_RINGING) {

                    } else if (call.getState() == Call.STATE_CONNECTING) {

                    }
                    //CommandUtils.sendVerifyCode(COMMINGTELNUMBER, COMMINGTELNUMBER);
                    Log.d(" Call.STATE_ACTIVE", "AAAAAAAAAAAAAAAAAA");
                    break;
                }
                case Call.STATE_DISCONNECTED: {
                    ActivityUtils.finishActivity(PhoneCallActivity.class);
                    break;
                }

            }
            super.onStateChanged(call, state);
        }
    };

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);

        call.registerCallback(callback);
        PhoneCallManager.call = call;

        CallType callType = null;

        if (call.getState() == Call.STATE_RINGING) {
            callType = CallType.CALL_IN;
        } else if (call.getState() == Call.STATE_CONNECTING) {
            callType = CallType.CALL_OUT;
        }

        if (callType != null) {
            Call.Details details = call.getDetails();
            String phoneNumber = details.getHandle().getSchemeSpecificPart();
            PhoneCallActivity.actionStart(this, phoneNumber, callType);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);

        call.unregisterCallback(callback);
        PhoneCallManager.call = null;
    }

    public enum CallType {
        CALL_IN,
        CALL_OUT,
    }
}
