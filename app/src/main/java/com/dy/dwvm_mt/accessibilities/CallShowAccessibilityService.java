package com.dy.dwvm_mt.accessibilities;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.dy.dwvm_mt.SplashActivity;
import com.dy.dwvm_mt.services.CallListenerService;
import com.dy.dwvm_mt.services.CallShowService;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/10/25.
 * PS: Not easy to write code, please indicate.
 */
public class CallShowAccessibilityService extends AccessibilityService {
    private boolean isStarted = false;
    private static String TAG = "In CallShowAccessibilityService";

    public void onInterrupt() {
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (!this.isStarted) {
            this.isStarted = true;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("--------onAccessibilityEvent--------");
            stringBuilder.append(accessibilityEvent);
            Log.d(TAG, "onAccessibilityEvent: " + stringBuilder.toString());
        }
        AccessibilityBridge.getInstance().doAccessibilityEvent(this, accessibilityEvent);
    }

    protected void onServiceConnected() {
        AccessibilityBridge.getInstance().setAccessibilityService((AccessibilityService) this);
    }

}
