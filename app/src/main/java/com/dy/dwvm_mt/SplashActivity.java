package com.dy.dwvm_mt;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;

import com.dy.dwvm_mt.accessibilities.AccessibilityBridge;
import com.dy.dwvm_mt.accessibilities.IAccessibilityEventHandler;
import com.dy.dwvm_mt.services.CallShowService;
import com.dy.dwvm_mt.utilcode.util.AppUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.SPUtils;

import static com.dy.dwvm_mt.comlibs.LocalSetting.SPUtilsName;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SPUtils spUtils = SPUtils.getInstance(SPUtilsName);
        String runKey = AppUtils.getAppVersionName() + "isFirstUse";
        boolean isFirstUse = spUtils.getBoolean(runKey, true);
        /**
         *如果用户不是第一次使用则直接调转到显示界面,否则调转到引导界面
         */

        if (isFirstUse) {
            startActivity(new Intent(this, GuideActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            startActivity(new Intent(this, DY_LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        spUtils.put(runKey, false);
        finish();
    }
}
