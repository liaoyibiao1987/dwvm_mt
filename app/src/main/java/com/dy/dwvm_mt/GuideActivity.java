package com.dy.dwvm_mt;

import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.dy.dwvm_mt.adapters.GuideAdapter;
import com.dy.dwvm_mt.utilcode.util.ToastUtils;

import java.util.ArrayList;

public class GuideActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {
    private ViewPager vPager;
    private GuideAdapter vpAdapter;
    private static int[] imgs = {R.drawable.welcomepage1, R.drawable.welcomepage2, R.drawable.welcomepage3};
    private ArrayList<ImageView> imageViews;
    private ImageView[] dotViews;//

    private void initDots() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.dot_Layout);
        LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams(20, 20);
        mParams.setMargins(10, 0, 10, 0);//设置小圆点左右之间的间隔
        dotViews = new ImageView[imgs.length];
        //判断小圆点的数量，从0开始，0表示第一个
        for (int i = 0; i < imageViews.size(); i++) {
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(mParams);
            imageView.setImageResource(R.drawable.dotselector);
            if (i == 0) {
                imageView.setSelected(true);//默认启动时，选中第一个小圆点
            } else {
                imageView.setSelected(false);
            }
            dotViews[i] = imageView;//得到每个小圆点的引用，用于滑动页面时，（onPageSelected方法中）更改它们的状态。
            layout.addView(imageView);//添加到布局里面显示
        }

    }


    private class MySimpleOnGestureListener implements GestureDetector.OnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            ToastUtils.showLong("onDown");
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            ToastUtils.showLong("onScroll");
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        private int verticalMinDistance = 20;
        private int minVelocity = 0;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            ToastUtils.showLong("滑动手势");
            if (e1.getX() - e2.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {

                // 切换Activity
                // Intent intent = new Intent(ViewSnsActivity.this, UpdateStatusActivity.class);
                // startActivity(intent);
            } else if (e2.getX() - e1.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {
                Intent intent = new Intent(GuideActivity.this, DY_LoginActivity.class);//跳转到主界面
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
            /*if (velocityX < 0) {
                //快速向左滑动
            } else {
            }*/
            return true;
        }
    }

    private final String getActionName(int action) {
        String name = "";
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                name = "ACTION_DOWN";
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                name = "ACTION_MOVE";
                break;
            }
            case MotionEvent.ACTION_UP: {
                name = "ACTION_UP";
                break;
            }
            default:
                break;
        }
        return name;
    }

    protected void initView() {
        //设置每一张图片都填充窗口
        ViewPager.LayoutParams mParams = new ViewPager.LayoutParams();
        imageViews = new ArrayList<ImageView>();
        //final GestureDetector mGestureDetector = new GestureDetector(this, new MySimpleOnGestureListener());

        for (int i = 0; i < imgs.length; i++) {
            ImageView iv = new ImageView(this);
            iv.setLayoutParams(mParams);//设置布局
            iv.setImageResource(imgs[i]);//为ImageView添加图片资源
            iv.setScaleType(ImageView.ScaleType.FIT_XY);//这里也是一个图片的适配
            imageViews.add(iv);
            if (i == imgs.length - 1) {
                //为最后一张图片添加点击事件
                /*iv.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        Log.i(getClass().getName(), "onTouch-----" + getActionName(event.getAction()));
                        mGestureDetector.onTouchEvent(event);
                        // 一定要返回true，不然获取不到完整的事件
                        return true;

                    }
                });*/
            }

        }
        vpAdapter = new GuideAdapter(imageViews);

        vPager = findViewById(R.id.guide_ViewPager);
        //vPager.setPageTransformer(true, new DepthPageTransformer());
        vPager.setAdapter(vpAdapter);

        vPager.addOnPageChangeListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        initView();
        initDots();
    }

    private int mViewPagerIndex = 0;

    @Override
    public void onPageScrolled(int position, float y, int z) {
        for (int i = 0; i < dotViews.length; i++) {
            if (position == i) {
                dotViews[i].setSelected(true);
            } else {
                dotViews[i].setSelected(false);
            }
        }
        //ToastUtils.showLong("向右手势" + mViewPagerIndex + position);
        if (mViewPagerIndex == dotViews.length - 1 && mViewPagerIndex == position) {
            Intent intent = new Intent(GuideActivity.this, DY_LoginActivity.class);//跳转到主界面
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {

        }
    }

    @Override
    public void onPageSelected(int i) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == 1) {//state有三种状态下文会将，当手指刚触碰屏幕时state的值为1，我们就在这个时候给mViewPagerIndex 赋值。
            mViewPagerIndex = vPager.getCurrentItem();
        }
    }
}
