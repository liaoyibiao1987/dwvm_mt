package com.dy.dwvm_mt;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.MotionEvent;
import android.widget.Button;

import com.dy.dwvm_mt.Comlibs.BaseActivity;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.fragments.HomeFragment;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.ScreenUtils;

import java.io.IOException;

import butterknife.ButterKnife;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/6/29.
 * PS: Not easy to write code, please indicate.
 */
public class DY_VideoPhoneActivity extends BaseActivity {

    private FragmentManager fragmentManager;
    private I_MT_Prime m_mt_Lib;

    private int m_pageOpenType = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videophone);
        ButterKnife.bind(this);
        m_mt_Lib = getM_mtLib();
        try{
            m_pageOpenType = getIntent().getExtras().getInt(MT_VP_PAGE_OPENTYPE);
        }catch (Exception e){
            LogUtils.d("getIntent().getExtras().getInt(MT_VP_PAGE_OPENTYPE) 获取不到数据.");
        }

        fragmentManager = getSupportFragmentManager();
        try {
            Class fragmentClass = HomeFragment.class;
            Fragment fragment = (Fragment) fragmentClass.newInstance();
            Bundle bundle = new Bundle();
            bundle.putInt(MT_VP_PAGE_OPENTYPE, m_pageOpenType);
            fragment.setArguments(bundle);
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        final int width = ScreenUtils.getScreenWidth();
        final int height = ScreenUtils.getScreenHeight();
        //每10s产生一次点击事件，点击的点坐标为(0.2W - 0.8W,0.2H - 0.8 H),W/H为手机分辨率的宽高.

        //只有模拟点击屏幕才能开启摄像头
        //有些系统为了隐私安全不容许非人为操作的开启摄像头操作。
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //生成点击坐标
                int x = (int) (Math.random() * width * 0.6 + width * 0.2);
                int y = (int) (Math.random() * height * 0.6 + height * 0.2);
                //利用ProcessBuilder执行shell命令
                String[] order = {
                        "input",
                        "tap",
                        "" + x,
                        "" + y
                };
                try {
                    new ProcessBuilder(order).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 打印点击的点的坐标
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int x = (int) event.getX();
        int y = (int) event.getY();
        //Toast.makeText(this, "X at " + x + ";Y at " + y, Toast.LENGTH_SHORT).show();
        semirmOnclick();
        return true;
    }

    public void semirmOnclick() {
        Intent intent = new Intent(BaseActivity.MT_AUTOSTARTCAMERA_ACTION);
        intent.putExtra(BaseActivity.MT_VP_PAGE_OPENTYPE, m_pageOpenType);
        sendBroadcast(intent);
    }
}
