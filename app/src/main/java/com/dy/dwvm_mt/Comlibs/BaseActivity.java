package com.dy.dwvm_mt.Comlibs;


import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.dy.dwvm_mt.commandmanager.MTLibUtils;

public abstract class BaseActivity extends AppCompatActivity {
    public static final String MT_AUTOSTARTCAMERA_ACTION = "dy.dymt.AUTOSTARTCAMERA";

    public static final String MT_VP_PAGE_OPENTYPE = "dy.dymt.VP_PAGE_OPENTYPE";
    public static final int MT_VIDEOPHONE_STARTUPTYPE_CALLING = 1;
    public static final int MT_VIDEOPHONE_STARTUPTYPE_OFFHOOK = 2;

    public static final int REQUESTCODE = 1;

    static {

        MTLibUtils.Initialisation();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (isShouldHideKeyboard(v, ev)) {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS
                );
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    // Return whether touch the view.
    private boolean isShouldHideKeyboard(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0],
                    top = l[1],
                    bottom = top + v.getHeight(),
                    right = left + v.getWidth();
            return !(event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom);
        }
        return false;
    }
}
