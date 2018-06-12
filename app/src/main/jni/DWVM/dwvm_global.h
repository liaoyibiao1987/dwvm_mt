#ifndef _DY_WAN_VIDEO_MEETING_H_20110613_
#define _DY_WAN_VIDEO_MEETING_H_20110613_

#include "XDataType.h"

//
// 1. 变量的字节顺序都为主机字节序，即小头在前：如0x12345678在内存中的字节顺序为0x78,0x56,0x34,0x12
// 2. 结构体的对齐方式都为1字节对齐：#pragma pack(1)
// 3. 用户名和密码都以密文传输，加密和解密见 dwvm_encrypt.h
// 4. 字符串都为ANSI字符，非UNICODE格式
//


//
// 最多支持的设备数
//
#define WVM_MAX_DEVICE_NUMBER        (4096)

//
// 各项参数的最大长度定义。为保证边界对齐，都定义为4的整数倍
//
#define WVM_MAX_DEVICE_NAME_LEN        (24)    // 设备名称的最大长度，包括最后的结束符\0
#define WVM_MAX_DEVICE_VERSION_LEN    (12)    // 设备版本号的最大长度，包括最后的结束符\0
#define WVM_MAX_TELPHONE_CODE_LEN    (20)    // 电话号码的最大长度，包括最后的结束符\0
#define WVM_MAX_USERNAME_LEN        (16)    // 用户名的最大长度，包括最后的结束符\0
#define WVM_MAX_PASSWORD_LEN        (16)    // 用户密码的最大长度，包括最后的结束符\0
#define WVM_MAX_IP_PORT_LEN            (24)    // IP地址+端口的最大长度
#define WVM_MAX_PS_DEST_NUMBER        (12)    // PS转发时的一个源ID对应的最多目标ID数量
#define WVM_MAX_MT_ENCODER_NUMBER    (64)    // MT编码通道的最大数量
#define WVM_MAX_MT_DECODER_NUMBER    (64)    // MT解码通道的最大数量
#define WVM_MAX_PARENT_PS_NUMBER    (4)        // 设备所属的PS服务器列表数量
#define WVM_MAX_PARENT_CMS_NUMBER    (4)        // 设备所属的CMS服务器列表数量
#define WVM_MTU                        (1400)    // 公网发包的最大长度为1500，减去IP头(20字节)和UDP头(8字节)。应该为1472，但为能广泛适应不同的路由器，适当地减小了一点
#define WVM_MAX_UDP_PACKET_SIZE        (WVM_MTU - sizeof(T_WVM_PACKET_HEADER) - sizeof(T_WVM_PS_TRANSMIT) - (sizeof(DWORD)*WVM_MAX_PS_DEST_NUMBER))    // UDP包的最大长度
#define WVM_MAX_DEVICE_EXT_SIZE        (WVM_MAX_UDP_PACKET_SIZE - sizeof(T_WVM_PACKET_HEADER) - sizeof(T_WVM_DDNS_TABLE_RESULT))    // 设备扩展信息的最大长度
#define WVM_MAX_RESEND_PKT_NUM        (8)        // 一次最多重发Video/Audio包数
#define WVM_MAX_FILE_NAME_LEN        (256)    // 文件名的最大长度，包括全路径名，和最后的结束符\0

//
// 网络包的开始码
//
#define WVM_START_CODE                (0x4D565744L)    // DWVM: Dy Wan Video Meeting

//
// 设备类型定义（用设备ID的最高8bit表示设备类型,范围: 0 ~ 15）
//
#define WVM_DEVICE_TYPE_DDNS    (0x01000000)
#define WVM_DEVICE_TYPE_PS        (0x02000000)
#define WVM_DEVICE_TYPE_CMS        (0x03000000)
#define WVM_DEVICE_TYPE_MT        (0x04000000)
#define WVM_DEVICE_TYPE_PBX        (0x05000000)
#define WVM_DEVICE_TYPE_MP        (0x06000000)
//
// 从设备ID中解析出设备类型
//
#define GET_DEVICE_TYPE(id)            ( ((DWORD)(id)) & 0xFF000000L )
#define MAKE_DEVICE_ID(type, sn)    ( (((DWORD)(type)) & 0xFF000000L) | (((DWORD)(sn)) & 0x00FFFFFFL) )

//
// 各模块默认的网络端口号
//
#define WVM_DEFAULT_PORT_DDNS    (5000)
#define WVM_DEFAULT_PORT_PS        (5001)
#define WVM_DEFAULT_PORT_CMS    (5002)
#define WVM_DEFAULT_PORT_PBX    (5003)
#define WVM_DEFAULT_PORT_MT        (5004)
#define WVM_DEFAULT_PORT_MP        (5005)
//
// socket的默认buffer长度. 
// 这个值定义这么大是为了适应视频帧的收发，对于不需要收发视频帧的socket，可以设定为更小的数字，比如8192
//
#define WVM_DEFAULT_SOCKET_BUFFER_SIZE    (2*1024*1024) //(1*1024*1024)

//
// 包序号的最高位用来标示是否SDK内部包
//
#define WVM_SEQ_INNER_FLAG    ((DWORD)(1L<<31))

//
// 登录的结果
//
#define WVM_LOGIN_RET_SUCCEED        (0)    // 成功
#define WVM_LOGIN_RET_ERROR_USER    (1)    // 用户名不存在
#define WVM_LOGIN_RET_ERROR_PASS    (2)    // 密码错误
#define WVM_LOGIN_RET_ERROR_VER        (3)    // 版本不匹配
#define WVM_LOGIN_RET_UNEXPECTED    (4)    // 内部错误

//
// 打开远端文件的结果
//
#define WVM_RF_OPEN_SUCCEED                (0)    // 成功
#define WVM_RF_OPEN_FAIL                (1)    // 失败：未知原因
#define WVM_RF_OPEN_REMOTE_NO_SUPPORT    (2)    // 远端机器不支持该功能，目前仅MT和CMS支持
#define WVM_RF_OPEN_NOT_EXISTED            (3)    // 远端文件不存在
#define WVM_RF_OPEN_CAN_NOT_OPEN        (4)    // 远端机器不能打开该文件
#define WVM_RF_OPEN_ERROR_FILE_FORMAT    (5)    // 文件格式错误
#define WVM_RF_OPEN_LOCAL_NET_ERROR        (6) // 本地网络错误
#define WVM_RF_OPEN_LOCAL_PARAM_ERROR    (7)    // 本地函数调用时参数错误
#define WVM_RF_OPEN_LOCAL_NO_MEMORY        (8)    // 本地内存不足
#define WVM_RF_OPEN_TIMEOUT                (9)    // 操作超时，远端机器没有应答
#define WVM_RF_OPEN_BUSY                (10)// 正在忙

//
// 流模式
//
#define WVM_STREAMTYPE_VIDEO_ONLY    (1)
#define WVM_STREAMTYPE_AUDIO_ONLY    (2)
#define WVM_STREAMTYPE_VIDEO_AUDIO    (WVM_STREAMTYPE_VIDEO_ONLY | WVM_STREAMTYPE_AUDIO_ONLY)

//
// 音频、视频帧的类型定义
//
#define WVM_FRAMETYPE_VIDEO_I    (0)
#define WVM_FRAMETYPE_VIDEO_P    (1)
#define WVM_FRAMETYPE_AUDIO        (4)
#define WVM_FRAMETYPE_UNKNOW    (5)
#define IS_VIDEO_FRAME(ft)        (WVM_FRAMETYPE_VIDEO_I == ft || WVM_FRAMETYPE_VIDEO_P == ft)
#define IS_AUDIO_FRAME(ft)        (WVM_FRAMETYPE_AUDIO == ft)

//
// 支持的图片编码尺寸，目前有三种：CIF,D1,QCIF
//
#define WVM_IMAGERES_CIF        (0)
#define WVM_IMAGERES_D1            (1)
#define WVM_IMAGERES_QCIF        (2)
#define WVM_IMAGERES_FULL_STILL    (3)
#define WVM_MAX_IMAGERES_NUM    (4)

//
// 单张视频帧和音频帧的最大尺寸定义
//
#define WVM_MAX_VIDEO_FRAME_SIZE    (2*1024*1024) //(800*1024)
#define WVM_MAX_AUDIO_FRAME_SIZE    (640)

//
// 命令码定义
//
#define WVM_CMD_POLLING                (1)        // All <==> All: T_WVM_PACKET_HEADER
// polling包
#define WVM_CMD_REPLY                (2)        // All <==> All: T_WVM_PACKET_HEADER + T_WVM_REPLY
// 应答包
#define WVM_CMD_TEST_NET            (3)        // All <==> All: T_WVM_PACKET_HEADER
// 网络测试包，用于测试网络带宽、响应时间、丢包率等
#define WVM_CMD_DDNS_LOGIN            (101)    // Device ==> DDNS: T_WVM_PACKET_HEADER + T_WVM_DDNS_LOGIN
// 设备向DDNS登录。
#define WVM_CMD_DDNS_LOGIN_RESULT    (102)    // DDNS ==> Device: T_WVM_PACKET_HEADER + T_WVM_DDNS_LOGIN_RESULT
// DDNS响应WVM_CMD_DDNS_LOGIN命令，回馈登录的结果给设备
#define WVM_CMD_DDNS_LOGOUT            (103)    // Device ==> DDNS: T_WVM_PACKET_HEADER + T_WVM_DDNS_LOGIN
// 设备向DDNS登出。
#define WVM_CMD_DDNS_QUERY_TABLE    (104)    // Device ==> DDNS: T_WVM_PACKET_HEADER + T_WVM_DDNS_QUERY_TABLE
// 设备向DDNS请求ID-IP-TELPHONE对应表
#define WVM_CMD_DDNS_TABLE            (105)    // DDNS ==> Device: T_WVM_PACKET_HEADER + T_WVM_DDNS_TABLE_RESULT
// DDNS返回查询结果给设备。
#define WVM_CMD_DDNS_TEMP_PS        (106)    // PS ==> DDNS, DDNS ==> Device: T_WVM_PACKET_HEADER + T_WVM_DDNS_TEMP_PS
// PS发送到DDNS，DDNS再转发到设备，要求设备临时登录（或者登出）到指定的PS
#define WVM_CMD_GET_CHILD_DEVICES    (107)    // CMS,PS ==> DDNS: T_WVM_PACKET_HEADER
// CMS,PS 获取自己下辖的设备列表
#define WVM_CMD_CHILD_DEVICE_LIST    (108)    // DDNS ==> CMS,PS: T_WVM_PACKET_HEADER + T_WVM_CHILD_DEVICE_LIST
// 发送设备下辖的子设备列表到CMS,PS
#define WVM_CMD_PS_TRANSMIT            (201)    // Device ==> PS: T_WVM_PACKET_HEADER + T_WVM_PS_TRANSMIT + Dest_ID_table + data
// 设备请求PS转发数据包
#define WVM_CMD_PS_ID_MATCH            (202)    // Device ==> PS: T_WVM_PACKET_HEADER + T_WVM_PS_ID_MATCH_HEADER + 一个或多个T_WVM_ID_MATCH
// 设备请求PS为多对ID建立连接
#define WVM_CMD_PS_SET_ENC_STATUS    (203)    // PS ==> Device: T_WVM_PACKET_HEADER + T_WVM_PS_SET_ENCODE_STATUS
// PS控制设备开始编码、或者停止编码
#define WVM_CMD_PS_VA_FRAME            (204)    // Server ==> Client: T_WVM_PACKET_HEADER + T_WVM_VA_FRAME_HEADER + T_WVM_VA_BLOCK_HEADER + frame_data
// 视频、音频帧
#define WVM_CMD_PS_VA_RESEND        (205)    // Client ==> Server: T_WVM_PACKET_HEADER + T_WVM_VA_RESEND
// 接收方请求发送方重新发送在网络上丢失的视频音频包
#define WVM_CMD_PS_VA_POLLING        (206)    // Client ==> Server: T_WVM_PACKET_HEADER + T_WVM_VA_POLLING
// 接收方请求发送方的统计信息
#define WVM_CMD_PS_VA_REPLY            (207)    // Server ==> Client: T_WVM_PACKET_HEADER + T_WVM_VA_REPLY
// 发送方回馈统计信息到接收端
#define WVM_CMD_REMOTE_FILE_OPEN    (300)    // CMS,MT (Client) ==> CMS,MT (Server): T_WVM_PACKET_HEADER + T_WVM_REMOTE_FILE_OPEN
// CMS,MT 打开远端的录像文件或者JPEG实时流
#define WVM_CMD_REMOTE_FILE_INFO    (301)    // CMS,MT (Server) ==> CMS,MT (Client): T_WVM_PACKET_HEADER + T_WVM_REMOTE_FILE_INFO
// 打开远端的录像文件或者JPEG实时流的结果. 是对WVM_CMD_REMOTE_FILE_OPEN的反馈
#define WVM_CMD_REMOTE_FILE_CLOSE    (302)    // CMS,MT (Client) ==> CMS,MT (Server): T_WVM_PACKET_HEADER + T_WVM_REMOTE_FILE_CLOSE
// CMS,MT 关闭远端的录像文件或者JPEG实时流
#define WVM_CMD_REMOTE_FILE_COMMAND    (303)    // CMS,MT (Client) ==> CMS,MT (Server): T_WVM_PACKET_HEADER + T_WVM_REMOTE_FILE_COMMAND
// CMS,MT 向远端MT发送各种播放命令
#define WVM_CMD_REMOTE_FILE_FRAME    (304)    // CMS,MT (Server) ==> CMS,MT (Client): T_WVM_PACKET_HEADER + T_WVM_REMOTE_FILE_FRAME
// CMS,MT 文件服务器向客户端发送视频、音频帧（拆成小包发生）
#define WVM_CMD_BIG_CUSTOM_DATA        (800)    // All <==> All: T_WVM_PACKET_HEADER + T_WVM_BIG_CUSTOM_DATA_HEADER + 用户数据
// 传输大buffer的用户数据
#define WVM_CMD_USER_BASE            (900)    // All <==> All: T_WVM_PACKET_HEADER + 用户自定义数据
// 传输用户自定义格式的数据。从这个数字之后的命令码，用户都可以自定义使用。

#pragma pack(1) // 设定 struct 和 union 的对齐

//
// 每个UDP包都必须包含的包头
//
typedef struct
{
    DWORD dwStartCode;    // 命令开始码，必须等于 WVM_START_CODE
    DWORD dwSize;            // 本结构的长度
    DWORD dwCmd;            // 命令类型：WVM_CMD_xxx
    DWORD dwSeq;            // 本数据包的序号。约定：最高位为1的数据包由模块内部处理，为0的数据包由应用软件处理(WVM_SEQ_INNER_FLAG)。
    DWORD dwSendingTick;    // 发送时刻的时间戳，配合应答包可以统计网络延时时间。单位：毫秒
    DWORD dwReplyContext;    // 0表示接收方不需要应答，否则接收方需要回复应答包，并将该字段在应答包中返回（T_WVM_REPLY::dwSrcContext）
    DWORD dwSrcId;        // 发送者的ID
    DWORD dwDestId;        // 目标接收者的ID
    DWORD dwDataSize;        // 结构体后跟着的数据长度
    DWORD dwDataByteSum;    // 结构体后跟着的数据的字节和. 发送时如果有加密，则先加密、再求和
    DWORD dwEncrypt;        // 数据是否加密。最高4bit表示加密模式，0表示无加密。其他28bit为加密因子。包头（即本结构）不加密。
} T_WVM_PACKET_HEADER;

//
// 应答包：WVM_CMD_REPLY
//
typedef struct
{
    DWORD dwSize;            // 本结构的长度
    DWORD dwSrcCmd;        // 来源网络包的命令码（即T_WVM_PACKET_HEADER::dwCmd）
    DWORD dwSrcSeq;        // 来源网络包的序号（即T_WVM_PACKET_HEADER::dwSeq）
    DWORD dwSrcTick;        // 来源网络包在源端被发送的时间戳（即T_WVM_PACKET_HEADER::dwSendingTick）
    DWORD dwSrcContext;    // 来源网络包的用户数据（即T_WVM_PACKET_HEADER::dwReplyContext）
} T_WVM_REPLY;

//
// 设备向DDNS登录: WVM_CMD_DDNS_LOGIN, WVM_CMD_DDNS_LOGOUT
//
typedef struct
{
    DWORD dwSize;            // 本结构的长度，不包括设备扩展信息
    DWORD dwDeviceId;        // 设备ID
    char szUsernameEncrypt[WVM_MAX_USERNAME_LEN];        // 登录用户名，密文
    char szPasswordEncrypt[WVM_MAX_PASSWORD_LEN];        // 登录密码，密文
    char szTelphoneZone[WVM_MAX_TELPHONE_CODE_LEN];    // 设备对应的电话区号
    char szTelphoneCode[WVM_MAX_TELPHONE_CODE_LEN];    // 设备对应的电话号码
    char szDeviceVersion[WVM_MAX_DEVICE_VERSION_LEN];    // 设备版本号
    char szDeviceName[WVM_MAX_DEVICE_NAME_LEN];        // 设备名称
    DWORD dwEncoderChannelNumber;    // 编码通道数量。仅对MT,MP,LSS有效
    DWORD dwDecoderChannelNumber;    // 解码通道数量。仅对MT,MP,LSS有效
    DWORD dwDeviceExtSize;// 结构体后跟着的设备扩展信息的长度. 设备扩展信息的格式由用户自己定义。尺寸不能超过WVM_MAX_DEVICE_EXT_SIZE
} T_WVM_DDNS_LOGIN;

//
// 应答包：WVM_CMD_DDNS_LOGIN_RESULT
//
typedef struct
{
    DWORD dwSize;        // 本结构的长度
    DWORD dwDeviceId;    // 分配给设备的ID，0表示登录失败。DDNS会根据设备发来的用户名分配一个ID给设备。
    DWORD dwErrorCode;
    // 登录的成功、失败状态。0-成功，其他-失败（WVM_LOGIN_RET_xxx）
    DWORD dwLoginTimeElapse;    // 设备向DDNS登录的周期，单位：毫秒
    char szDDNSName[WVM_MAX_DEVICE_NAME_LEN];            // DDNS的名称
    char szDDNSVersion[WVM_MAX_DEVICE_VERSION_LEN];    // DDNS的版本号
    char szDeviceWanIp[WVM_MAX_IP_PORT_LEN];            // 设备的公网IP地址和端口
    DWORD dwParentPsIDs[WVM_MAX_PARENT_PS_NUMBER];        // 设备所属的PS服务器列表
    DWORD dwParentCmsIDs[WVM_MAX_PARENT_CMS_NUMBER];    // 设备所属的CMS服务器列表
    DWORD dwExtSize;    // 本结构之后跟随的扩展信息的长度
} T_WVM_DDNS_LOGIN_RESULT;

//
// 设备向DDNS请求ID-IP-TELPHONE表: WVM_CMD_DDNS_QUERY_TABLE
//
typedef struct
{
    DWORD dwSize;                                    // 本结构的长度
    DWORD dwDestDeviceId;                            // 被查询的目标设备ID。等于0表示查询电话号码。
    char szTelphoneZone[WVM_MAX_TELPHONE_CODE_LEN];    // 被查询的目标电话区号。dwDestDeviceId等于0时该字段才有效
    char szTelphoneCode[WVM_MAX_TELPHONE_CODE_LEN];    // 被查询的目标电话号码。dwDestDeviceId等于0时该字段才有效
} T_WVM_DDNS_QUERY_TABLE;

//
// DDNS返回查询结果给设备：WVM_CMD_DDNS_TABLE
//
typedef struct
{
    DWORD dwSize;            // 本结构长度。不包括设备扩展信息。
    DWORD dwDeviceId;        // 设备ID。等于0表示设备不在线。
    DWORD dwDeviceIp;        // 设备的真实IP地址
    DWORD dwDevicePort;    // 设备的网络端口号
    char szTelphoneZone[WVM_MAX_TELPHONE_CODE_LEN];    // 设备对应的电话区号
    char szTelphoneCode[WVM_MAX_TELPHONE_CODE_LEN];    // 设备对应的电话号码
    char szDeviceVersion[WVM_MAX_DEVICE_VERSION_LEN];    // 设备版本号
    char szDeviceName[WVM_MAX_DEVICE_NAME_LEN];        // 设备名称
    char szUsernameEncrypt[WVM_MAX_USERNAME_LEN];        // 设备登录的用户名，密文
    DWORD dwEncoderChannelNumber;    // 编码通道数量。仅对MT,MP有效
    DWORD dwDecoderChannelNumber;    // 解码通道数量。仅对MT,MP有效
    DWORD dwDeviceExtSize;// 设备扩展信息的长度。设备扩展信息跟在本结构之后，数据格式由用户自定义。
} T_WVM_DDNS_TABLE_RESULT;

//
// PS发送到DDNS，DDNS再转发到设备，要求设备临时登录（或者登出）到指定的PS: WVM_CMD_DDNS_TEMP_PS
//
typedef struct
{
    DWORD dwSize;        // 本结构长度
    DWORD dwPsId;        // 目标PS的ID. 0表示要求设备登出临时的PS
    DWORD dwDeviceId;    // 目标设备的ID
} T_WVM_DDNS_TEMP_PS;

//
// 设备请求PS转发数据包: WVM_CMD_PS_TRANSMIT
//
typedef struct
{
    DWORD dwSize;        // 本结构长度。不包括设备扩展信息。
    DWORD dwDestIdNum;
    // 目的ID表的大小。一个包可以转发给多个目标。最大为WVM_MAX_PS_DEST_NUMBER
    DWORD dwDataSize;    // 被转发数据的长度。数据跟在该结构之后。
    //
    // 该结构之后跟随两段数据：
    // 1. 目标ID表，尺寸为 dwDestIdNum * sizeof(DWORD)
    // 2. ID表之后跟随的，是需要被转发的数据，尺寸为 dwDataSize
    //
} T_WVM_PS_TRANSMIT;

//
// 设备请求PS为多对ID建立连接: WVM_CMD_PS_ID_MATCH
//
typedef struct
{
    DWORD dwSize;
    DWORD dwMatchNumber;    // 后面跟随 T_WVM_ID_MATCH 结构体的数量
    //
    // 本结构之后跟随若干个T_WVM_ID_MATCH结构体
    //
} T_WVM_PS_ID_MATCH_HEADER;
//
// 连接来源设备的编码通道与目标设备的解码通道
//
typedef struct
{
    DWORD dwSize;                            // 本结构长度
    BOOL bOpen;                            // TRUE-添加ID对，FALSE-删除ID对
    DWORD dwVideoAudioMode;                // 流模式：WVM_STREAMTYPE_VIDEO_ONLY 仅视频, WVM_STREAMTYPE_AUDIO_ONLY 仅音频, WVM_STREAMTYPE_VIDEO_AUDIO 视频和音频
//	BOOL	bVideoOnlySendKeyFrame;			// 视频帧模式: 是否仅仅发送视频关键帧 (即仅仅发送dwFrameType等于WVM_FRAMETYPE_VIDEO_I的帧)
    DWORD dwImageResolution;                // 图像分辨率：WVM_IMAGERES_CIF, WVM_IMAGERES_D1, WVM_IMAGERES_QCIF
    DWORD dwSrcDeviceId;                    // 来源设备ID
    DWORD dwSrcDeviceEncoderChannelIndex;    // 来源设备的编码器通道
    DWORD dwDestDeviceId;                    // 目标设备ID
    DWORD dwDestDeviceDecoderChannelIndex;// 目标设备的解码器通道
} T_WVM_ID_MATCH;

//
// PS控制设备开始编码、或者停止编码: WVM_CMD_PS_SET_ENC_STATUS
//
typedef struct
{
    DWORD dwSize;        // 本结构的长度
    BOOL bOpen;        // TRUE-开始编码并发送数据到PS，FALSE-停止编码
    DWORD dwDeviceId;    // 目标设备的ID
    DWORD dwEncoderChannelIndex;    // 目标设备的编码通道索引号
    DWORD dwVideoAudioMode;        // WVM_STREAMTYPE_VIDEO_ONLY 仅视频, WVM_STREAMTYPE_AUDIO_ONLY 仅音频, WVM_STREAMTYPE_VIDEO_AUDIO 视频和音频
    BOOL bVideoOnlySendKeyFrame;    // 视频帧模式: 是否仅仅发送视频关键帧
} T_WVM_PS_SET_ENCODE_STATUS;

//
// 视频、音频会话ID
//
typedef struct
{
    DWORD dwSrcDeviceEncoderChannelIndex;    // 来源设备的编码器通道
    DWORD dwDestDeviceDecoderChannelIndex;
    // 目标设备的解码器通道
    DWORD dwImageResolution;                // 图像分辨率：WVM_IMAGERES_CIF, WVM_IMAGERES_D1, WVM_IMAGERES_QCIF
    DWORD dwFrameType;                    // 帧类型：WVM_FRAMETYPE_VIDEO_I, WVM_FRAMETYPE_VIDEO_P, WVM_FRAMETYPE_AUDIO
} T_WVM_VA_SESSION;

//
// PS和设备之间互相发送的视频、音频帧: WVM_CMD_PS_VA_FRAME
// 由于公网MTU的限制，每个完整的视频、音频帧(frame)可能会被拆分为多个小包(block)发送
// 每个小包都包括帧头(T_WVM_VA_FRAME_HEADER)和包头(T_WVM_VA_BLOCK_HEADER)
//
typedef struct
{
    T_WVM_VA_SESSION Session;                // 会话ID
    DWORD dwFrameSize;            // 结构之后跟随的Frame尺寸，包括 T_WVM_VA_BLOCK_HEADER
    BOOL bVideoOnlySendKeyFrame;    // 视频帧模式: 是否仅仅发送视频关键帧
    DWORD dwReserve;                // 保留参数，等于0
} T_WVM_VA_FRAME_HEADER;
typedef struct
{
    DWORD time_stamp;  // 发送端必须设置为0; 接收端用它来保存包被接收时的时间戳，以毫秒为单位
    DWORD pkt_seq;     // 包序号(从1开始递增的流水号)，这个包在所有已经产生的视频、音频包中的序号
    DWORD frm_seq;     // 帧序号(从1开始递增的流水号)，这一帧在所有已经产生的帧中的序号，一个帧可以包含多个包
    WORD frm_pkt_seq; // 帧内包序号(从0开始计数)
    WORD frm_pkt_num; // 这个帧总共包含的包的数量
    BYTE is_independent; // 本帧是否一个独立帧(不需要依靠其他帧、可以单独存在的，比如视频I帧、音频帧)
    BYTE resend_cnt;  // 本小包是第几次被重新发送. 0为首发包，大于0为重发包
    BYTE reserve[2];  // 必须为0
} T_WVM_VA_BLOCK_HEADER;

//
// 接收方发现收到的视频音频包有丢失的话，请求发送方重新发送：WVM_CMD_PS_VA_RESEND
//
typedef struct
{
    T_WVM_VA_SESSION Session;        // 会话ID
    DWORD dwPacketNum;    // 请求的重发包的数量
    DWORD dwPacketSeqArray[WVM_MAX_RESEND_PKT_NUM];    // 所有重发包的包序号（T_WVM_VA_BLOCK_HEADER::pkt_seq）
} T_WVM_VA_RESEND;

//
// 接收方请求发送方的统计信息 : WVM_CMD_PS_VA_POLLING
//
typedef struct
{
    T_WVM_VA_SESSION Session;        // 会话ID
    DWORD dwSendingMs;    // 此包的发送时刻，毫秒
    DWORD dwReceiveMs;    // 收到回馈的应答包的时刻，毫秒（发送时填0，收到应答包再填写实际时间）
    DWORD dwStatTimeMs;    // 此次统计的时间跨度，毫秒
    DWORD dwPktSeqBegin;    // 统计开始的小包序号
    DWORD dwPktSeqEnd;    // 统计结束的小包序号
    DWORD dwRecvPackets_0;
    // 实际收到的包数   (首发包)
    DWORD dwRecvBytes_0;    // 实际收到的字节数 (首发包)
    DWORD dwRecvPackets_1;
    // 实际收到的包数   (重发包)
    DWORD dwRecvBytes_1;    // 实际收到的字节数 (重发包)
    DWORD dwReserve[4];    // 保留参数，等于0
} T_WVM_VA_POLLING;

//
// 发送方回馈统计信息到接收端 : WVM_CMD_PS_VA_REPLY
//
typedef struct
{
    T_WVM_VA_POLLING Polling;        // 对应的polling包
    DWORD dwSendPackets_0;
    // 实际发送的包数   (首发包)
    DWORD dwSendBytes_0;    // 实际发送的字节数 (首发包)
    DWORD dwSendPackets_1;
    // 实际发送的包数   (重发包)
    DWORD dwSendBytes_1;    // 实际发送的字节数 (重发包)
    DWORD dwRecvFrames;    // 接收到的帧数
    DWORD dwReserve[3];    // 保留参数，等于0
} T_WVM_VA_REPLY;

//
// DDNS 发送设备下辖的子设备列表到CMS,PS: WVM_CMD_CHILD_DEVICE_LIST
//
typedef struct
{
    DWORD dwDeviceNumber; // 设备数量
    //
    // 之后跟随设备列表, DWORD数组
    //
} T_WVM_CHILD_DEVICE_LIST;

//
// CMS,MT 打开远端的录像文件或者JPEG实时流 : WVM_CMD_REMOTE_FILE_OPEN
//
typedef struct
{
    UINT64 u64ClientContext;                    // 发起请求方的上下文参数
    char szFileName[WVM_MAX_FILE_NAME_LEN];    // 文件名(如"c:\record\test.mp4")，或者实时流名称(如":video_enc=jpeg,channel=0,image_res=1")
} T_WVM_REMOTE_FILE_OPEN;


//
// 打开远端的录像文件或者JPEG实时流的结果. 是对WVM_CMD_REMOTE_FILE_OPEN的反馈 : WVM_CMD_REMOTE_FILE_INFO
//
typedef struct
{
    DWORD dwOpenResult;        // 0表示打开文件成功，其他为失败代码 (WVM_RF_OPEN_xxx)
    UINT64 u64ClientContext;    // 发起请求方的上下文参数
    UINT64 u64FileId;            // 文件ID
    UINT64 u64FileBeginTime;    // 文件的开始时间，如：20130415174600
    UINT64 u64FileEndTime;        // 文件的结束时间
    DWORD dwVideoEnc;            // 视频编码格式
    DWORD dwAudioEnc;            // 音频编码格式
    DWORD dwVideoWidth;        // 视频宽
    DWORD dwVideoHeight;        // 视频高
} T_WVM_REMOTE_FILE_INFO;

//
// CMS,MT 关闭远端的录像文件或者JPEG实时流 : WVM_CMD_REMOTE_FILE_CLOSE
//
typedef struct
{
    UINT64 u64FileId;    // 文件ID
} T_WVM_REMOTE_FILE_CLOSE;

//
// CMS,MT 向远端MT发送各种播放命令 : WVM_CMD_REMOTE_FILE_COMMAND
//
typedef struct
{
    DWORD dwCmdSeq;            // 命令编号
    UINT64 u64ClientContext;    // 发起请求方的上下文参数
    UINT64 u64FileId;            // 文件ID
    char szCommand[256];        // 命令串
} T_WVM_REMOTE_FILE_COMMAND;

//
// CMS,MT 文件服务器向客户端发送视频、音频帧（拆成小包发送）: WVM_CMD_REMOTE_FILE_FRAME
//
typedef struct
{
    DWORD dwCmdSeq;            // 命令编号
    UINT64 u64ClientContext;    // 发起请求方的上下文参数
    UINT64 u64FileId;            // 文件ID
    BOOL bIsLastPkt;            // 这个小包是否为整帧的最后一个包
    DWORD dwFrameLen;            // 整帧的长度
    DWORD dwPktOffset;        // 这个小包的偏移位址
    DWORD dwPktBytes;            // 后面跟随的视频、音频小包的长度
} T_WVM_REMOTE_FILE_FRAME;

//
// 组件之间传输大buffer数据，拆成小包发送，每个小包之前带这个结构头 : WVM_CMD_BIG_CUSTOM_DATA
//
typedef struct
{
    DWORD dwFrameSeq;        // 发送端的全局帧序号
    DWORD dwFrameBytes;    // 该帧的总长度
    WORD wPktNumber;        // 该帧包含的小包数量
    WORD wPktSeq;        // 帧内的包序号,  0 ~ (wPktNumber-1)
    WORD wPktBytes;        // 这个小数据包的长度
    WORD wCrcVerify;        // 整帧数据的CRC16校验码
    DWORD dwPktOffset;    // 这个小包在整个帧内的偏移量 (字节)
    DWORD dwCustomID;        // 客户自定义ID
    DWORD dwReserve[4];
} T_WVM_BIG_CUSTOM_DATA_HEADER;

#pragma pack()

#endif // _DY_WAN_VIDEO_MEETING_H_20110613_
