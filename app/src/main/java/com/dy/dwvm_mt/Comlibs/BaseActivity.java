package com.dy.dwvm_mt.Comlibs;


import android.support.v7.app.AppCompatActivity;

import com.dy.dwvm_mt.MTLib;
import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.commandmanager.MTLibUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

public abstract class BaseActivity extends AppCompatActivity {
    public static final String MT_AUTOSTARTCAMERA_ACTION = "dy.dymt.AUTOSTARTCAMERA";

    public static final String MT_VP_PAGE_OPENTYPE = "dy.dymt.VP_PAGE_OPENTYPE";
    public static final int MT_VIDEOPHONE_STARTUPTYPE_CALLING = 1;
    public static final int MT_VIDEOPHONE_STARTUPTYPE_OFFHOOK = 2;

    public static final int REQUESTCODE = 1;

    static {

        MTLibUtils.Initialisation();
    }

    public BaseActivity() {

    }
}
