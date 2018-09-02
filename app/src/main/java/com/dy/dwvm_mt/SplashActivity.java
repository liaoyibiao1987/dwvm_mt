package com.dy.dwvm_mt;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.dy.dwvm_mt.utilcode.util.SPUtils;

import static com.dy.dwvm_mt.comlibs.LocalSetting.SPUtilsName;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SPUtils spUtils = SPUtils.getInstance(SPUtilsName);
        boolean isFirstUse = spUtils.getBoolean("isFirstUse", true);
        /**
         *如果用户不是第一次使用则直接调转到显示界面,否则调转到引导界面
         */
        if (isFirstUse) {
            startActivity(new Intent(this, GuideActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
        } else {
            startActivity(new Intent(this, DY_LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
        spUtils.put("isFirstUse", false);
        finish();
    }
}
