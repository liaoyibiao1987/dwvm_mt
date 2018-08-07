package com.dy.dwvm_mt.userview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Checkable;

import com.dy.dwvm_mt.R;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/8/6.
 * PS: Not easy to write code, please indicate.
 */
public class DYImageButton extends android.support.v7.widget.AppCompatImageButton implements Checkable {
    private OnCheckedChangeListener onCheckedChangeListener;

    public DYImageButton(Context context) {
        super(context);
    }

    public DYImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setChecked(attrs);
    }

    public DYImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setChecked(attrs);
    }

    private void setChecked(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DYImageButton);
        setChecked(a.getBoolean(R.styleable.DYImageButton_isChecked, false));
        a.recycle();
    }

    @Override
    public void setChecked(boolean checked) {
        setSelected(checked);
        if (onCheckedChangeListener != null) {
            onCheckedChangeListener.onCheckedChanged(this, checked);
        }
    }

    @Override
    public boolean isChecked() {
        return isSelected();
    }

    @Override
    public void toggle() {
        setChecked(!isChecked());
    }

    @Override
    public boolean performClick() {
        //performClick 模拟人手去触摸控件
        //在调用performClick之前必须设置了点击事件，不然无效,如果在调用performClick之前没有设置点击事件，那就直接返回了false，不会再响应点击事件了
        //这里直接响应了触摸事件
        toggle();
        return super.performClick();
    }

    public OnCheckedChangeListener getOnCheckedChangeListener() {
        return onCheckedChangeListener;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
        this.onCheckedChangeListener = onCheckedChangeListener;
    }

    public static interface OnCheckedChangeListener {
        public void onCheckedChanged(DYImageButton buttonView, boolean isChecked);
    }
}
