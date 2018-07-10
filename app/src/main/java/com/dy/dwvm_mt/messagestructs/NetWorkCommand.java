package com.dy.dwvm_mt.messagestructs;

import android.webkit.JavascriptInterface;

import com.dy.dwvm_mt.utilcode.util.ConvertUtils;
import com.dy.dwvm_mt.utilcode.util.LogUtils;
import com.dy.javastruct.JavaStruct;

import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/7/4.
 * PS: Not easy to write code, please indicate.
 */
public class NetWorkCommand {
    /// <summary>
    /// 命令代号
    /// </summary>
    private int Cmd;
    /// <summary>
    /// 子命令代号
    /// </summary>
    private int SubCmd;
    /// <summary>
    /// 包头结构
    /// </summary>
    private s_headPack Header;
    /// <summary>
    /// 命令数据
    /// </summary>
    private byte[] Data;
    /// <summary>
    /// IP端口地址
    /// </summary>
    private String IPPort;

    public int getCmd() {
        return Cmd;
    }

    public void setCmd(int cmd) {
        Cmd = cmd;
    }

    public int getSubCmd() {
        return SubCmd;
    }

    public void setSubCmd(int subCmd) {
        SubCmd = subCmd;
    }

    public s_headPack getHeader() {
        return Header;
    }

    public void setHeader(s_headPack header) {
        Header = header;
    }

    public byte[] getData() {
        return Data;
    }

    public void setData(byte[] data) {
        Data = data;
    }

    public String getIPPort() {
        return IPPort;
    }

    public void setIPPort(String IPPort) {
        this.IPPort = IPPort;
    }

    public NetWorkCommand(ReceivePackEntity packEntity) {
        setCmd(packEntity.getBagType());
        setIPPort(packEntity.getSzSrcIpPort());
        setSubCmd(ConvertUtils.byte2int(packEntity.getBagBuffer(), 44));
        setData(packEntity.getBagBuffer());
        setHeader(packEntity.getHeadPack());
        //SubCmd = BitConverter.ToInt32((byte[])packEntity.getBagBuffer(), 44);
    }

    /// <summary>
    /// 将数据序反列化为指定的对象
    /// </summary>
    /// <typeparam name="T">对象类型</typeparam>
    /// <returns></returns>
    public <T> T Param(Class<T> tClass) throws InstantiationException, IllegalAccessException {
        T instance = tClass.newInstance();
        if (Data != null) {
            boolean isAnnotation = tClass.isAnnotationPresent(com.dy.javastruct.StructClass.class);//判断stu是不是使用了我们刚才定义的注解接口if(isEmpty)
            if (isAnnotation == true) {
                try {
                    byte[] buff = new byte[Data.length - 44];
                    System.arraycopy(Data, 44, buff, 0, buff.length);
                    JavaStruct.unpack(instance, buff);//获取二级命令包
                } catch (Exception es) {
                    LogUtils.e(" Param(Class<T> tClass) :" + es);
                }
            } else {
                LogUtils.e(" 该类型无StructClass注解：" + tClass.toString());
            }

        }
        return instance;
    }

    @Override
    public String toString() {
        //return super.toString();
        String ret = String.format(" Data: %s \r\n IPPort: %s  Cmd: %s SubCmd: %s", Arrays.toString(getData()), getIPPort(), getCmd(), getSubCmd());
        return ret;
    }
}
