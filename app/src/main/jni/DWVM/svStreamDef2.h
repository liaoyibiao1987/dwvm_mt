#ifndef _SVSTREAMDEF2_H_20041022_
#define _SVSTREAMDEF2_H_20041022_


#pragma pack(1)

// Frame Type
#define VFT_IFRAME    0
#define VFT_PFRAME    1
#define VFT_BFRAME    2
#define VFT_HEAD      3
#define VFT_AUDIO     4
#define VFT_UNKNOW    5
#define VFT_RECREATE  101
#define VFT_TEXT      102 // BufferSize需要包括字符串最末尾的 \0 结束符

// for VFT_RECREATE
typedef struct _tagSVRecreateDecoder
{
    WORD VideoWidth;
    WORD VideoHeight;
} tagSVRecreateDecoder;

//video CODEC type
#define    VCODEC_NONE            0    //无视频包
#define    VCODEC_MS            1
#define    VCODEC_XVID            2
#define    VCODEC_XVID_I        4
#define VCODEC_STD_264        5
#define VCODEC_JPEG            6
#define VCODEC_FFMPEG4        7
#define VCODEC_FFH264        8
#define VCODEC_TWH264        9
#define VCODEC_JPEG_FILE    10
#define VCODEC_FFHEVC        11
#define VCODEC_MINILZO        12

//audio CODEC type
#define    ACODEC_NONE            0    //无音频包
#define    ACODEC_G723            1
#define    ACODEC_PCM            2
#define    ACODEC_NIADPCM        4
#define    ACODEC_G72x            5
#define    ACODEC_G72x_FAST    6
#define ACODEC_NIADPCM_32KHz_1Chn_16Bit 8
#define ACODEC_G711            9
#define ACODEC_FF_G726        10
#define ACODEC_VIVO_A        11
#define ACODEC_PCM_8KHZ_1CHN_16BIT    12
#define ACODEC_PCM_16KHZ_1CHN_16BIT    13
#define ACODEC_PCM_32KHZ_1CHN_16BIT    14
#define ACODEC_FF_AAC        15
#define ACODEC_MP3            16

// video standard (tagSVStreamHeader2::Standard)
#define VSTD_PAL    0x01
#define VSTD_NTSC    0x02

// audio standard (tagSVStreamHeader2::Standard)
#define ASTD_EXTFLAG_MASK    0x80    // bit7: 0 - not use ext flag, 1 - use the ext flag
#define ASTD_EXTFLAG_NO        0x00
#define ASTD_EXTFLAG_YES    0x80
#define ASTD_CHANNELS_MASK    0x40    // bit6: 0 - 1 channel, 1 - 2 channel
#define ASTD_CHANNELS_1        0x00
#define ASTD_CHANNELS_2        0x40
#define ASTD_BITCOUNT_MASK    0x30    // bit4,bit5: 0 - 8 bits, 1 - 16 bits, 2 - 24 bits, 3 - 32 bits
#define ASTD_BITCOUNT_8        0x00
#define ASTD_BITCOUNT_16    0x10
#define ASTD_BITCOUNT_24    0x20
#define ASTD_BITCOUNT_32    0x30
#define ASTD_SAMPLE_MASK    0x0F    // bit0~3: 0 - 8KHz, 1 - 12KHz, 2 - 16KHz, 3 - 24KHz, 4 - 32KHz, 5 - 44.1KHz, 6 - 48KHz, 7 - reserve
#define ASTD_SAMPLE_8000    0x00
#define ASTD_SAMPLE_12000    0x01
#define ASTD_SAMPLE_16000    0x02
#define ASTD_SAMPLE_24000    0x03
#define ASTD_SAMPLE_32000    0x04
#define ASTD_SAMPLE_44100    0x05
#define ASTD_SAMPLE_48000    0x06
// help macro
#define ASTD_GET_EXT(s)            ((s) & ASTD_EXTFLAG_MASK)
#define ASTD_GET_CHANNELS(s)    ((s) & ASTD_CHANNELS_MASK)
#define ASTD_GET_BITCOUNT(s)    ((s) & ASTD_BITCOUNT_MASK)
#define ASTD_GET_SAMPLERATE(s)    ((s) & ASTD_SAMPLE_MASK)

#define SVSTREAM_STARTCODE  0xCAD3B6A6 // 0x56535A48
#define SVSTREAM_STARTCODE2 0xA6B6D3CA

typedef struct _tagSVStreamHeader
{
    ULONG Code;            // SVSTREAM_STARTCODE
    WORD StreamType;        // VFT_AUDIO , VFT_IFRAME , VFT_PFRAME , VFT_BFRAME
    BYTE Standard;       // 1: PAL,  2: NTSC
    BYTE Version;        // 0: no version, 1: ver 1,  2: ver 2 ....
    DWORD BufferSize;        //
    DWORD PTS;            // 时间
    DWORD dwOrder;        // 序号, 对音视频单独编号.每个文件从 0 开始
} tagSVStreamHeader;

// 如果 Version>=2, 用 tagSVStreamHeader2取代 tagSVStreamHeader 结构
typedef struct _tagSVStreamHeader2
{
    ULONG Code;            // SVSTREAM_STARTCODE
    WORD StreamType;        // VFT_AUDIO , VFT_IFRAME , VFT_PFRAME , VFT_BFRAME
    BYTE Standard;       // [video] VSTD_xxx / [audio] ASTD_xxx
    BYTE Version;        // 必须 >=2
    DWORD BufferSize;        //
    DWORD PTS;            // 时间
    DWORD dwOrder;        // 序号, 对音视频单独编号.每个文件从 0 开始
    WORD HeadSize;       // sizeof(tagSVStreamHeader2)
    WORD VideoWidth;
    WORD VideoHeight;
    WORD tmYear;
    BYTE tmMonth;
    BYTE tmDay;
    BYTE tmHour;
    BYTE tmMinute;
    BYTE tmSecond;
    BYTE CodecType;      // 编码类型: VCODEC_xxx 或者 ACODEC_xxx
    BYTE VerifyMode; // 1: byte-sum-verify, other: no verify
    BYTE VerifySumValue; // byte-sum value
} tagSVStreamHeader2;

typedef struct tagFILEHEADER
{
    DWORD Code;            // 0x48 0x5A 0x53 0x56 : "HZSV"
    WORD StreamType;        // VFT_HEAD
    WORD wVer;            // 版本号, 0x0100
    DWORD BufferSize;        // FILEHEADER 结构的大小

    WORD vCodeC;            // video CODEC type
    WORD vFrameRate;        // 帧率
    WORD vWidth;
    WORD vHeight;

    WORD aCodeC;            // audio CODEC type
    WORD aCompressBit;    // 压缩位数
} FILEHEADER, *LPFILEHEADER;

// 索引文件的结构定义
typedef struct tagFileIndexStruct
{
    ULONG FileOffset;      // 离文件头的偏移量
    ULONG FrameNum;      // 帧序号
    DWORD StreamType;   // VFT_AUDIO, VFT_IFRAME , VFT_PFRAME , VFT_BFRAME , VFT_HEAD
    DWORD PTS;          //
    DWORD BufferSize;   //
    UINT64 Datetime;     // ex: 20050318141652 for (2005-3-18, 14:16:52)
} FileIndexStruct;

#pragma pack()

#endif // #ifndef _SVSTREAMDEF2_H_20041022_