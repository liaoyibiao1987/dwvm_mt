package com.dy.dwvm_mt.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.constraint.ConstraintLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dy.dwvm_mt.R;
import com.dy.dwvm_mt.broadcasts.AutoStartReceiver;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

import java.util.HashMap;

@Deprecated
public class ServiceCallerShow extends Service {
    public static Context context = null;
    public static String phoneNumBerStr = null;
    //定义浮动窗口布局

    private ConstraintLayout mFloatLayout;
    private WindowManager.LayoutParams wmParams;
    //创建浮动窗口设置布局参数的对象

    private WindowManager mWindowManager;
    private Button mFloatButton;
    private boolean isRunning;

    @Override

    public void onCreate() {
        //android.os.Debug.waitForDebugger();
        super.onCreate();
        if (context != null) {
            //Toast.makeText(context, "ServiceCallerShow.onCreate()", Toast.LENGTH_LONG);
            if (phoneNumBerStr != null && !phoneNumBerStr.equals("")) {
                //将来电号码的详细信息赋给悬浮窗中的对应UI
                createFloatView();
            }
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 创建悬浮窗
     */
    private void createFloatView() {
        try {
            wmParams = new WindowManager.LayoutParams();
            //获取WindowManagerImpl.CompatModeWrapper
            mWindowManager = (WindowManager) getApplication().getSystemService(getApplication().WINDOW_SERVICE);
            //设置window type[优先级]
            wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;//窗口的type类型决定了它的优先级，优先级越高显示越在顶层
            //设置图片格式，效果为背景透明
            wmParams.format = PixelFormat.RGBA_8888;
            //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
            wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            //调整悬浮窗显示的停靠位置为顶部水平居中
            wmParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER;

            // 以屏幕左上角为原点，设置x、y初始值
            wmParams.x = 0;
            wmParams.y = 200;
            /*// 设置悬浮窗口长宽数据
            wmParams.width = 200;
            wmParams.height = 80;*/

            //设置悬浮窗口长宽数据
            wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

            LayoutInflater inflater = LayoutInflater.from(getApplication());
            //获取浮动窗口视图所在布局
            mFloatLayout = (ConstraintLayout) inflater.inflate(R.layout.floatview, null);
            //添加mFloatLayout
            mWindowManager.addView(mFloatLayout, wmParams);
            //浮动窗口按钮
            mFloatButton = mFloatLayout.findViewById(R.id.float_id);
            //有值就赋上，没值就不显示该UI
            mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                    View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                    .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));


            mFloatButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopService(new Intent(v.getContext(), ServiceCallerShow.class));
                }
            });//mFloatView.setOnClickListener[结束服务，关闭悬浮窗]
        } catch (Exception es) {
            LogUtils.e(es.toString());
        }

    }//createFloatView()//创建悬浮窗

    @Override

    public void onDestroy()//关闭悬浮窗
    {
        // TODO Auto-generated method stub
        LogUtils.e(context, "ServiceCallerShow.onDestroy()");
        super.onDestroy();
        if (mFloatLayout != null) {
            mWindowManager.removeView(mFloatLayout);
        }
        isRunning = false;
        sendBroadcast(new Intent(AutoStartReceiver.AUTO_START_RECEIVER));
    }//onDestroy()//关闭悬浮窗[当该service停止的时候也执行该方法]
}
