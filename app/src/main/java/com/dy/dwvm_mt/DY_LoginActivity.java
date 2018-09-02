package com.dy.dwvm_mt;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.dy.dwvm_mt.comlibs.BaseActivity;
import com.dy.dwvm_mt.comlibs.LocalSetting;
import com.dy.dwvm_mt.comlibs.LoginExtMessageDissector;
import com.dy.dwvm_mt.commandmanager.AnalysingUtils;
import com.dy.dwvm_mt.commandmanager.CommandUtils;
import com.dy.dwvm_mt.commandmanager.NWCommandEventArg;
import com.dy.dwvm_mt.commandmanager.NWCommandEventHandler;
import com.dy.dwvm_mt.messagestructs.s_loginResultDDNS;
import com.dy.dwvm_mt.messagestructs.s_messageBase;
import com.dy.dwvm_mt.utilcode.util.ActivityUtils;
import com.dy.dwvm_mt.utilcode.util.CrashUtils;
import com.dy.dwvm_mt.utilcode.util.IntentUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.dwvm_mt.utilcode.util.PhoneUtils;
import com.dy.dwvm_mt.utilcode.util.StringUtils;
import com.dy.dwvm_mt.utilcode.util.ToastUtils;
import com.dy.javastruct.Constants;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.DISABLE_KEYGUARD;
import static android.Manifest.permission.MODIFY_AUDIO_SETTINGS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.dy.dwvm_mt.comlibs.LocalSetting.StartLoginType;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/9.
 * PS: 东耀会议系统登录界面
 */

public class DY_LoginActivity extends BaseActivity implements NWCommandEventHandler {

    private static final int LOADVIEW = 904;

    @BindView(R.id.txt_login_id)
    EditText EtLoginID;

    @BindView(R.id.txt_login_password)
    EditText EtLoginPsw;

    @BindView(R.id.btn_login)
    Button btnLogin;

    @BindView(R.id.txt_login_telnumber)
    EditText EtTelNumber;

    @BindView(R.id.btn_login_auth_alert)
    Button btnAuthAlert;

    //所需要申请的权限数组
    /*private  String[] permissionsArray;*/
    private String[] permissionsArray = new String[]{
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.READ_PHONE_STATE,
            DISABLE_KEYGUARD,
            CALL_PHONE,
            //Manifest.permission.ANSWER_PHONE_CALLS,
            //Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.ACCESS_NETWORK_STATE,
            MODIFY_AUDIO_SETTINGS,
            WRITE_EXTERNAL_STORAGE,
            WRITE_EXTERNAL_STORAGE,
            //android.Manifest.permission.READ_PHONE_NUMBERS,
            //"android.permission.RECEIVE_USER_PRESENT",
            Manifest.permission.CAMERA,
            Manifest.permission.MODIFY_PHONE_STATE,
            Manifest.permission.READ_SMS};
    //还需申请的权限列表
    private List<String> permissionsList = new ArrayList<String>();
    //申请权限后的返回码
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;

    Handler handler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            CrashUtils.init();
        }
        Log.d("东耀视频会议系统", "登录界面开始运行");
        AnalysingUtils.addRecvedCommandListeners(this);
        int startType = getIntent().getIntExtra(StartLoginType, 0);
        if (startType == 0) {
            viewSwitch();
        } else {
            fillView();
        }

    }

    private void viewSwitch() {
        if (LocalSetting.isIsLogined() == true) {
            Intent intent = new Intent();
            intent.setClass(this, MTMainActivity.class);
            ActivityUtils.startActivity(intent);
            finish();
        } else {
            if (StringUtils.isTrimEmpty(LocalSetting.getLoginID()) == true ||
                    StringUtils.isTrimEmpty(LocalSetting.getLoginPSW()) == true ||
                    StringUtils.isTrimEmpty(LocalSetting.getTelNumber()) == true) {
                fillView();
            } else {
                CommandUtils.sendLoginData(LocalSetting.getLoginID(), LocalSetting.getLoginPSW(), LocalSetting.getTelNumber(), "", CommandUtils.getDDNSIPPort());
                /*handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        Log.i("DYMT LOGIN Handler", "hanlder handleMessage: " + Thread.currentThread().getName());
                        switch (msg.what) {
                            case LOADVIEW:
                                if (LocalSetting.isIsLogined() == false) {
                                    fillView();
                                }
                                break;
                        }
                    }
                };
                Message message = new Message();
                message.what = LOADVIEW;
                handler.sendMessageDelayed(message,3000L);*/
                handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (LocalSetting.isIsLogined() == false) {
                            fillView();
                        }
                    }
                }, 3000);
            }
        }
    }

    private void fillView() {
        requestMyPermission();
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        EtLoginID.setText(LocalSetting.getLoginID());
        EtLoginID.setSelection(EtLoginID.getText().length());

        EtLoginPsw.setText(LocalSetting.getLoginPSW());
        EtLoginPsw.setSelection(EtLoginPsw.getText().length());

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onLoginClicked();
            }
        });

        String phoneNumber = PhoneUtils.getLine1Number();
        if (StringUtils.isTrimEmpty(phoneNumber) == false && phoneNumber.startsWith("+86")) {
            phoneNumber = phoneNumber.substring(3);
            EtTelNumber.setText(phoneNumber);
        } else {
            EtTelNumber.setText(LocalSetting.getTelNumber());
        }

        btnAuthAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName())); // 根据包名打开对应的设置界面
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        /*try {
            throw new NullPointerException();
        } catch (NullPointerException e1) {
            LogUtils.e("onResume", Log.getStackTraceString(e1));
        }*/
        //onResume 天然激活两次
        super.onResume();
        if (StringUtils.isTrimEmpty(LocalSetting.getLoginID()) == false
                && StringUtils.isTrimEmpty(LocalSetting.getLoginPSW()) == false
                && StringUtils.isTrimEmpty(LocalSetting.getTelNumber()) == false) {

        }
    }


    private void onLoginClicked() {
        String loginID = EtLoginID.getText().toString();
        String psw = EtLoginPsw.getText().toString();
        String telNumber = EtTelNumber.getText().toString();
        LocalSetting.setLoginID(loginID);
        LocalSetting.setLoginPSW(psw);
        LocalSetting.setTelNumber(telNumber);

        CommandUtils.sendLoginData(loginID, psw, telNumber, "", CommandUtils.getDDNSIPPort());
    }

    @Override
    public void doNWCommandHandler(NWCommandEventArg arg) {
        if (arg != null && arg.getEventArg() != null) {
            int cmd = arg.getEventArg().getCmd();
            switch (cmd) {
                case s_messageBase.DeviceCMD.WVM_CMD_DDNS_LOGIN_RESULT:
                    try {
                        s_loginResultDDNS loginResult = arg.getEventArg().Param(s_loginResultDDNS.class);
                        LogUtils.d("Device ID：" + loginResult.getDwDeviceId());

                        if (loginResult.getDwErrorCode() == 0) {
                            ToastUtils.showShort("登录成功");

                            int ddnsID = arg.getEventArg().getHeader().dwSrcId;
                            String ddnsIP = arg.getEventArg().getIPPort();
                            LoginExtMessageDissector.LoginExtMessage loginExtMessage = LoginExtMessageDissector.getLoginExtMessage(loginResult);
                            ReWriteInformation(ddnsID, ddnsIP, loginExtMessage);
                            LogUtils.d("loginResult.getDwLoginTimeElapse" + loginResult.getDwLoginTimeElapse());
                            Intent intent = new Intent();
                            intent.setClass(this, MTMainActivity.class);
                            ActivityUtils.startActivity(intent);

                            finish();
                        } else {
                            LocalSetting.ResetInformation();
                            ToastUtils.showShort("登录失败");
                        }
                        startPolling(loginResult.getDwErrorCode(), loginResult.getDwLoginTimeElapse());
                    } catch (Exception es) {
                        LogUtils.e("TestActivity : Analytic package error :" + es);
                    }
                    break;
            }

        }
    }

    private void requestMyPermission() {
        try {
            for (String permission : permissionsArray) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
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
                LogUtils.d("已经获得过了全部认证");
            }
        } catch (Exception es) {
            LogUtils.e("requestMyPermission error:" + es.toString());
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
        btnAuthAlert.setVisibility(View.VISIBLE);
       /* AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
        builder.create().show();*/
    }

    @Override
    protected void onDestroy() {
        System.out.println("\r\n登录界面结束运行\r\n");
        AnalysingUtils.removeRecvedCommandListeners(this);
        super.onDestroy();
    }
}
