package com.dy.dwvm_mt.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dy.dwvm_mt.R;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

import java.util.HashMap;

public class ServiceCallerShow extends Service {
    public static Context context = null;
    public static String phoneNumBerStr = null;
    HashMap<String, String> detailInfoMap;
    //定义浮动窗口布局

    private LinearLayout mFloatLayout;
    private WindowManager.LayoutParams wmParams;
    //创建浮动窗口设置布局参数的对象

    private WindowManager mWindowManager;
    private Button mFloatView;
    private TextView tvName;
    private TextView tvBm;
    private TextView tvGh;
    private TextView tvEmail;
    private TextView tvQQ;

    @Override

    public void onCreate() {
        android.os.Debug.waitForDebugger();
        super.onCreate();
        if (context != null) {
            Toast.makeText(context,"ServiceCallerShow.onCreate()",Toast.LENGTH_LONG);
            if (phoneNumBerStr != null && !phoneNumBerStr.equals("")) {
                //将来电号码的详细信息赋给悬浮窗中的对应UI
                //createFloatView();
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
        wmParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        // 以屏幕左上角为原点，设置x、y初始值
        wmParams.x = 0;
        wmParams.y = 0;
        /*// 设置悬浮窗口长宽数据
        wmParams.width = 200;
        wmParams.height = 80;*/

        //设置悬浮窗口长宽数据

        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.floatview, null);
        //添加mFloatLayout
        mWindowManager.addView(mFloatLayout, wmParams);
        //浮动窗口按钮
        mFloatView = (Button) mFloatLayout.findViewById(R.id.float_id);
        //【详细信息赋值 给UI 】++++++++++++
        tvName = (TextView) mFloatLayout.findViewById(R.id.tv_name);
        tvBm = (TextView) mFloatLayout.findViewById(R.id.tv_ssbm);
        tvGh = (TextView) mFloatLayout.findViewById(R.id.tv_gh);
        tvEmail = (TextView) mFloatLayout.findViewById(R.id.tv_email);
        tvQQ = (TextView) mFloatLayout.findViewById(R.id.tv_qq);
        String nameStr = "AAA";
        String bmStr = "SSBM";
        String ghStr = "GH";
        String qqStr = "QQ";
        String emainlStr = "EMAIL";
        //有值就赋上，没值就不显示该UI
        if (!nameStr.equals("")) {
            tvName.setText(nameStr);
        } else {
            tvName.setVisibility(View.GONE);
        }

        if (!bmStr.equals("")) {
            tvBm.setText(bmStr);
        } else {
            tvBm.setVisibility(View.GONE);
        }
        if (!ghStr.equals("")) {
            tvGh.setText(ghStr);
        } else {
            tvGh.setVisibility(View.GONE);
        }
        if (!qqStr.equals("")) {
            tvQQ.setText(qqStr);
        } else {
            tvQQ.setVisibility(View.GONE);
        }
        if (!emainlStr.equals("")) {
            tvEmail.setText(emainlStr);
        } else {
            tvEmail.setVisibility(View.GONE);
        }

        //mFloatView.setText(phoneNumBerStr);

        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));


        mFloatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(v.getContext(), ServiceCallerShow.class));
            }
        });//mFloatView.setOnClickListener[结束服务，关闭悬浮窗]
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

    }//onDestroy()//关闭悬浮窗[当该service停止的时候也执行该方法]
}
