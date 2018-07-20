package com.dy.dwvm_mt.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.dy.dwvm_mt.Comlibs.BaseActivity;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.DY_VideoPhoneActivity;
import com.dy.dwvm_mt.MainActivity;
import com.dy.dwvm_mt.R;
import com.dy.dwvm_mt.broadcasts.AutoStartReceiver;
import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.PhoneUtils;

public class PollingService extends Service {

    private static int m_interval = 0;
    private static boolean m_isOnline = false;

    @Override
    public void onCreate() {
        //android.os.Debug.waitForDebugger();
        /*if (isRunning == false) {
            try {
                isRunning = true;
                initFloatView();
                initPhoneStateListener();
                Thread.sleep(1000);
            } catch (Exception es) {
                LogUtils.e("CallShowService onCreate error " + es.toString());
                isRunning = false;
                return;
            }
        }
*/
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (m_isOnline == true && m_interval > 0) {
                            CommandUtils.sendPolling();
                            LogUtils.d("Send polling package.");
                            Thread.sleep(m_interval);
                        } else {
                            Thread.sleep(200);
                        }
                    } catch (Exception es) {
                        LogUtils.e("PollingService error :" + es);
                    }
                }
            }
        }).start();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        m_interval = intent.getIntExtra(CommandUtils.Str_Extra_Polling, -1);
        m_isOnline = intent.getBooleanExtra(CommandUtils.Str_Extra_Online, false);

        return super.onStartCommand(intent, START_STICKY, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
