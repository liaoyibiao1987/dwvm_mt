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
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.dy.dwvm_mt.Comlibs.BaseActivity;
import com.dy.dwvm_mt.commandmanager.AnalysingUtils;
import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.commandmanager.NWCommandEventArg;
import com.dy.dwvm_mt.commandmanager.NWCommandEventHandler;
import com.dy.dwvm_mt.messagestructs.s_loginResultDDNS;
import com.dy.dwvm_mt.messagestructs.s_messageBase;
import com.dy.dwvm_mt.utilcode.constant.PermissionConstants;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.PhoneUtils;
import com.dy.dwvm_mt.utilcode.util.StringUtils;
import com.dy.dwvm_mt.utilcode.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/9.
 * PS: 东耀会议系统登录界面
 */

public class DY_LoginActivity extends BaseActivity implements NWCommandEventHandler {

    @BindView(R.id.txt_login_id)
    EditText EtLoginID;

    @BindView(R.id.txt_login_password)
    EditText EtLoginPsw;

    @BindView(R.id.btn_login)
    Button btnLogin;

    //所需要申请的权限数组
    /*private  String[] permissionsArray;*/
    private String[] permissionsArray = new String[]{
            android.Manifest.permission.WAKE_LOCK,
            android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            "Manifest.permission.RECEIVE_USER_PRESENT",
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.MODIFY_PHONE_STATE,
            Manifest.permission.READ_SMS,
            Manifest.permission.MEDIA_CONTENT_CONTROL};
    //还需申请的权限列表
    private List<String> permissionsList = new ArrayList<String>();
    //申请权限后的返回码
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        AnalysingUtils.addRecvedCommandListeners(this);
        requestMyPermission();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onLoginClicked();
            }
        });
    }

    private void onLoginClicked() {
        String loginid = EtLoginID.getText().toString();
        String psw = EtLoginPsw.getText().toString();
        String phonenumber = PhoneUtils.getLine1Number();
        if(StringUtils.isTrimEmpty(phonenumber) == false && phonenumber.startsWith("+86")){
            phonenumber = phonenumber.substring(3);
        }
        String ddnsIPAndPort = CommandUtils.DDNSIP + ":" + CommandUtils.DDNSPORT;

        CommandUtils.sendLoginData(loginid, psw, phonenumber, "", ddnsIPAndPort);

    }

    @Override
    public void doHandler(NWCommandEventArg arg) {
        if (arg != null && arg.getEventArg() != null) {
            int cmd = arg.getEventArg().getCmd();
            switch (cmd) {
                case s_messageBase.DeviceCMD.WVM_CMD_DDNS_LOGIN_RESULT:
                    try {
                        s_loginResultDDNS loginResult = arg.getEventArg().Param(s_loginResultDDNS.class);
                        LogUtils.d("Device ID：" + loginResult.getDwDeviceId());
                        if (loginResult.getDwErrorCode() == 0) {
                            ToastUtils.showShort("登录成功");
                        } else {
                            ToastUtils.showShort("登录失败");
                        }
                    } catch (Exception es) {
                        LogUtils.e("TestActivity : Analytic package error :" + es);
                    }
                    break;
            }

        }
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        AnalysingUtils.removeRecvedCommandListeners(this);
    }
}
