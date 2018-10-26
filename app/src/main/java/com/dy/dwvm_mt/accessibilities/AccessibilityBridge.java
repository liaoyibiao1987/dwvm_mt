package com.dy.dwvm_mt.accessibilities;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/10/25.
 * PS: Not easy to write code, please indicate.
 */
public class AccessibilityBridge {

    private static AccessibilityBridge instance = new AccessibilityBridge();
    private AccessibilityService accessibilityService;
    private IAccessibilityEventHandler eventHandler = null;

    public static AccessibilityBridge getInstance() {
        return instance;
    }

    public void setAccessibilityService(AccessibilityService accessibilityService) {
        this.accessibilityService = accessibilityService;
    }

    public void doAccessibilityEvent(AccessibilityService accessibilityService, AccessibilityEvent accessibilityEvent) {
        if (this.eventHandler != null) {
            if (accessibilityService != null) {
                ApplicationContextInstance.getInstance().setContext(accessibilityService.getApplicationContext());
            }
            this.eventHandler.doHandler(accessibilityService, accessibilityEvent);
        }
    }

    public void setEventHandler(IAccessibilityEventHandler iAccessibilityEventHandler) {
        this.eventHandler = iAccessibilityEventHandler;
    }

    public void resetEventHandler() {
        this.eventHandler = null;
    }

    public AccessibilityService getAccessibilityService() {
        return this.accessibilityService;
    }

}
