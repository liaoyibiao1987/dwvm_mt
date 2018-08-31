package com.dy.dwvm_mt;

import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.dy.dwvm_mt.adapters.GuideAdapter;

import java.util.ArrayList;

public class GuideActivity extends AppCompatActivity  implements ViewPager.OnPageChangeListener {
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

    protected void initView() {
        //设置每一张图片都填充窗口
        ViewPager.LayoutParams mParams = new ViewPager.LayoutParams();
        imageViews = new ArrayList<ImageView>();

        for (int i = 0; i < imgs.length; i++) {
            ImageView iv = new ImageView(this);
            iv.setLayoutParams(mParams);//设置布局
            iv.setImageResource(imgs[i]);//为ImageView添加图片资源
            iv.setScaleType(ImageView.ScaleType.FIT_XY);//这里也是一个图片的适配
            imageViews.add(iv);
            if (i == imgs.length - 1) {
                //为最后一张图片添加点击事件
                iv.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        Intent toMainActivity = new Intent(GuideActivity.this, MainActivity.class);//跳转到主界面
                        startActivity(toMainActivity);
                        return true;

                    }
                });
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

    @Override
    public void onPageScrolled(int x, float y, int z) {
        for (int i = 0; i < dotViews.length; i++) {
            if (x == i) {
                dotViews[i].setSelected(true);
            } else {
                dotViews[i].setSelected(false);
            }
        }
    }

    @Override
    public void onPageSelected(int i) {

    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }
}
