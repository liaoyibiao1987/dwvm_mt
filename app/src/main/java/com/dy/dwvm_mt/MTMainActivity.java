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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.dy.dwvm_mt.Comlibs.BaseActivity;
import com.dy.dwvm_mt.Comlibs.I_MT_Prime;
import com.dy.dwvm_mt.commandmanager.MTLibUtils;
import com.dy.dwvm_mt.fragments.HomeFragment;
import com.dy.dwvm_mt.utilcode.constant.PermissionConstants;
import com.dy.dwvm_mt.utilcode.util.LogUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MTMainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    //所需要申请的权限数组
    /*private  String[] permissionsArray;*/
    private String[] permissionsArray = new String[]{
            android.Manifest.permission.WAKE_LOCK,
            android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            "Manifest.permission.RECEIVE_USER_PRESENT",
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.MODIFY_PHONE_STATE,
            Manifest.permission.MEDIA_CONTENT_CONTROL};
    //还需申请的权限列表
    private List<String> permissionsList = new ArrayList<String>();
    //申请权限后的返回码
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;

    private I_MT_Prime m_mt_Lib;


    FragmentManager fragmentManager;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.content_frame)
    FrameLayout contentFrame;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.nav_view)
    NavigationView navigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mtmain);
        ButterKnife.bind(this);
        requestMyPermission();
        fragmentManager = getSupportFragmentManager();

        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //android 从 API 26 之后，使用findViewById 可以直接写为 tv = findViewById(R.id.textView) ;
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent, 1);
            } else {
                //TODO do something you need
            }
        }
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        m_mt_Lib = MTLibUtils.getBaseMTLib();
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mtmain, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Class fragmentClass = null;
        if (id == R.id.nav_camera) {
            fragmentClass = HomeFragment.class;
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }
        try {
            Fragment fragment = (Fragment) fragmentClass.newInstance();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        item.setChecked(true);
        setTitle(item.getTitle());
        drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void requestMyPermission() {
        String[] permissionsArray2 = PermissionConstants.getPermissions(PermissionConstants.DY_PHONE);
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
                } else {
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
                    AskForPermission();
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
