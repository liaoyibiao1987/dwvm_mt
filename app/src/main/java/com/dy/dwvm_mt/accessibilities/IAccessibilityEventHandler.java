package com.dy.dwvm_mt.accessibilities;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/10/25.
 * PS: Not easy to write code, please indicate.
 */
public interface IAccessibilityEventHandler {
    void doHandler(AccessibilityService accessibilityService, AccessibilityEvent accessibilityEvent);

}
