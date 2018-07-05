package com.dy.dwvm_mt.Comlibs;


import android.support.v7.app.AppCompatActivity;

import com.dy.dwvm_mt.MTLib;
import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

public class BaseActivity extends AppCompatActivity{
    public static final String MT_AUTOSTARTCAMERA_ACTION = "dy.dymt.AUTOSTARTCAMERA";

    public static final String MT_VP_PAGE_OPENTYPE = "dy.dymt.VP_PAGE_OPENTYPE";
    public static final int MT_VIDEOPHONE_STARTUPTYPE_CALLING = 1;
    public static final int MT_VIDEOPHONE_STARTUPTYPE_OFFHOOK = 2;

    public static final int REQUESTCODE = 1;
    // MT Library
    private static I_MT_Prime m_mtLib = null;

    public static I_MT_Prime getBaseMTLib() {
        return m_mtLib;
    }

    static {
        setupMTLib();
    }

    public static void setupMTLib() {
        if (BaseActivity.m_mtLib == null) {
            try {
                BaseActivity.m_mtLib = new MTLib();
                if (m_mtLib.isWorking() == false) {
                    m_mtLib.installCallback(null);
                    if (!m_mtLib.start(0x04000009, CommandUtils.MTPORT, 1024 * 1024, 0, 1, 1, "")) {
                        LogUtils.e("MTLib.start() failed !");
                        return;
                    }
                    m_mtLib.setDeviceName("MT");
                } else {
                    LogUtils.d("MTLib is already started !");
                }
                CommandUtils.initSetupAdapter(m_mtLib);
            } catch (Exception e) {
                LogUtils.e("MTLib.start() error: " + e.getMessage());
            }
            //m_mtLib.setDeviceName(LOCAL_DEVICE_NAME);
        } else {
            LogUtils.e("主件已经初始化过了.");
        }
    }

}
