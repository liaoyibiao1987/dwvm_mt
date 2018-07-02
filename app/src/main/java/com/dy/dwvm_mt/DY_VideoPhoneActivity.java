package com.dy.dwvm_mt;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.MotionEvent;
import android.widget.Button;

import com.dy.dwvm_mt.Comlibs.BaseActivity;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.fragments.HomeFragment;
import com.dy.dwvm_mt.utilcode.constant.PermissionConstants;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.ScreenUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/6/29.
 * PS: Not easy to write code, please indicate.
 */
public class DY_VideoPhoneActivity extends BaseActivity {

    private FragmentManager fragmentManager;
    private I_MT_Prime m_mt_Lib;

    private int m_pageOpenType = 0;

    //所需要申请的权限数组
    /*private  String[] permissionsArray;*/
    private  String[] permissionsArray = new String[]{
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.ACCESS_NETWORK_STATE,
            "Manifest.permission.RECEIVE_USER_PRESENT",
            Manifest.permission.CAMERA,
            Manifest.permission.MEDIA_CONTENT_CONTROL};
    //还需申请的权限列表
    private List<String> permissionsList = new ArrayList<String>();
    //申请权限后的返回码
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videophone);
        ButterKnife.bind(this);
        requestMyPermission();
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

    private void requestMyPermission(){
        String[] permissionsArray2 = PermissionConstants.getPermissions(PermissionConstants.PHONE);
        String[] allpermiss = new String[permissionsArray2.length + permissionsArray.length];
        System.arraycopy(permissionsArray, 0, allpermiss, 0, permissionsArray.length);
        System.arraycopy(permissionsArray2, 0, allpermiss, permissionsArray.length, permissionsArray2.length);

        for (String permission : allpermiss) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.PROCESS_OUTGOING_CALLS)) {
                    // Should we show an explanation?
                    // 第一次打开App时	false
                    // 上次弹出权限点击了禁止（但没有勾选“下次不在询问”）	true
                    // 上次选择禁止并勾选：下次不在询问	false
                    permissionsList.add(permission);
                    LogUtils.e("we should explain why we need this permission!");
                }
                else {
                    permissionsList.add(permission);
                    LogUtils.d("需要手动打开权限了：" + permission);
                }
            }
        }
        if (permissionsList.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]), REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            LogUtils.e("已经获得过了全部认证");
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUESTCODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.PROCESS_OUTGOING_CALLS)) {
                    //AskForPermission();
                }
            }
        }
    }

    private void AskForPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("需要特定权限需要设置!");
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setPositiveButton("去设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName())); // 根据包名打开对应的设置界面
                startActivity(intent);
            }
        });
        builder.create().show();
    }
}
