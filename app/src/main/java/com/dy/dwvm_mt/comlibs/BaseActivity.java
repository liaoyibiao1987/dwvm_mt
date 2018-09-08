package com.dy.dwvm_mt.comlibs;


import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.commandmanager.MTLibUtils;
import com.dy.dwvm_mt.services.PollingService;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.SPUtils;

public abstract class BaseActivity extends AppCompatActivity {
    public static final String MT_AUTOSTARTCAMERA_ACTION = "dy.dymt.AUTOSTARTCAMERA";

    public static final String MT_VP_PAGE_OPENTYPE = "dy.dymt.VP_PAGE_OPENTYPE";
    public static final int MT_VIDEOPHONE_STARTUPTYPE_CALLING = 1;
    public static final int MT_VIDEOPHONE_STARTUPTYPE_OFFHOOK = 2;
    protected static SPUtils spUtils;
    public static final int REQUESTCODE = 1;

    static {
        spUtils = SPUtils.getInstance(LocalSetting.SPUtilsName);
        MTLibUtils.Initialisation();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (isShouldHideKeyboard(v, ev)) {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS
                );
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    // Return whether touch the view.
    private boolean isShouldHideKeyboard(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0],
                    top = l[1],
                    bottom = top + v.getHeight(),
                    right = left + v.getWidth();
            return !(event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom);
        }
        return false;
    }


    public final void startPolling(int errorCode, @Nullable int elapse) {
        Intent pollingIntent = new Intent(this, PollingService.class);
        if (errorCode == 0) {
            pollingIntent.putExtra(CommandUtils.Str_Extra_Polling, elapse * 800);
            pollingIntent.putExtra(CommandUtils.Str_Extra_Online, true);
        } else {
            pollingIntent.putExtra(CommandUtils.Str_Extra_Polling, -1);
            pollingIntent.putExtra(CommandUtils.Str_Extra_Online, false);
        }
        startService(pollingIntent);
    }

    public final void ReWriteInformation(final int ddnsID, final String ddnsIP, final LoginExtMessageDissector.LoginExtMessage loginExtMessage) {
        try {
            final int localDeviceID = loginExtMessage.getDeviceId();
           /* this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CommandUtils.setDDNSDEVICEID(ddnsID);
                    CommandUtils.setDDNSIPPort(ddnsIP);
                    I_MT_Prime mtlib = MTLibUtils.getBaseMTLib();
                    mtlib.resetDeviceID(localDeviceID);
                }
            });*/
            CommandUtils.setDDNSDEVICEID(ddnsID);
            CommandUtils.setDDNSIPPort(ddnsIP);
            I_MT_Prime mtlib = MTLibUtils.getBaseMTLib();
            mtlib.setDeviceId(localDeviceID);

            LocalSetting.SetInformationByLoginResult(loginExtMessage);
        } catch (Exception es) {
            LogUtils.e("ReWriteInformation error:" + es);
        }
    }
}
