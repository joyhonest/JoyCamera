package com.joyhonest.joycamera.sdk;

import android.content.Context;

import android.graphics.Bitmap;


import java.io.File;

import java.nio.ByteBuffer;

public class wifiCamera {


    public static String sAlbum;
    public static String sLocalPath;

    private static Context applicationContext = null;

    public static final int TYPE_ONLY_PHONE = 0;
    public static final int TYPE_ONLY_SD = 1;
    public static final int TYPE_BOTH_PHONE_SD = 2;

    public static final int TYPE_DEST_SNADBOX = 0;
    public static final int TYPE_DEST_GALLERY = 1;

    public static final int TYPE_VIDEO = 1;
    public static final int TYPE_PHOTO = 3;


    public static  final int  CAMERA_NORMAL_MODE = 0;
    public static  final int  CAMERA_FILELIST_MODE = 1;


    private static final int BMP_Len = (((4096 + 3) / 4) * 4) * 4 * 3072 + 2048;
    private  final  static int CmdLen = 2048;
    //private  static boolean  bProgressGP4225UDP=true;

    public static  void naSetApplicationContext(Context context)
    {
        applicationContext = context.getApplicationContext();
    }

    static Context   getApplicationContext()
    {
        return applicationContext;
    }

    private static final String  TAG = "wifiCamera";

    static {
        System.loadLibrary("joycamera");
        Utility.mDirectBuffer = ByteBuffer.allocateDirect(BMP_Len + CmdLen);     //获取每帧数据，主要根据实际情况，分配足够的空间。
        naSetDirectBuffer(Utility.mDirectBuffer, BMP_Len + CmdLen);
        JoyLog.SetDebug(true);
        //Utility.gp4225_Device = new GP4225_Device();
    }



    //public  static  native  void StartRecordAudio(boolean b);

    private static native void naSetDirectBuffer(Object buffer, int nLen);
    public static native void eglinit();
    public static native void eglrelease();
    public static native void eglchangeLayout(int width, int height);
    public static native void egldrawFrame();

    /*
     * 以下这些供用户使用
    */

    public static native  boolean naIsJoyCamera();
    public static native  int naInit(String sPara);
    private static native  int naStopA();
    public static  int naStop()
    {
        naStopRecordAll();
        naStopA();
        return 0;
    }



    public static native void naSetCameraStation(boolean b);


    public static native  int  naStartUdpService(boolean b);
    public static native  void naReadDeviceInfo();

    public static native  void naReadDeviceStatus();


    public static native int naSentCmd(byte[] cmd, int nLen);


    public static native  void naSetVR(boolean b);  //对应旧版本 naSet3D
    public static native  void naSetFlip(boolean b);

    public static native  void naSetVRwhiteBack(boolean bVR_WhitClolor);

    public static native  void naSetMirror(boolean b);
    public static native void naSet3DDenoiser(boolean b); //视频是否3D降噪

    public static native void naSetEnableRotate(boolean b); //视频是否可以旋转任意角度。 如果调用naEnableSensor，会从naEnableSensor 内部调用次函数
    public static native void naSetFilterRotate(float nAngle); //一般用户无需调用
    public static native void naSetEnableEQ(boolean b);

    public static native int naSetRecordWH(int ww, int hh);

    //设定是否检测 图传协议，有些固件支持 同一个固件 用不同的图传协议
    //但因为要判定协议，Open摄像头时。多需要100ms ,需要在 naInit之前调用它
    public static native  int naSetCheckTransferProtocol(boolean b);
    private static native int naSnapPhotoA(String sPath, int nType ,int dest);
    static  int photoType = -1;
    static  int photodest = -1;
    static  int videoType = -1;
    static  int videodest = -1;
    public static  int naSnapPhoto(String sPath, int nType ,int dest)
    {
        photoType = nType;
        photodest = dest;
        if(photodest== TYPE_DEST_GALLERY)
        {
            String strna = Utility.getFileNameFromDate(false);
            if(sPath !=null)
            {
                strna = sPath.substring(sPath.lastIndexOf("/") + 1);
                strna = sLocalPath+"/"+strna;
            }
            return naSnapPhotoA(strna, nType, dest);
        }
        else {
            return naSnapPhotoA(sPath, nType, dest);
        }
    }

    //录像
    public static  int naStartRecord(String sFileName2, int nType ,int dest,boolean bRecordAudio)
    {
        String sFileName1 = sFileName2;
        String  sFileName = sFileName1+"_.tmp";

        videoType = nType;
        videodest = dest;

        int  re = -1;

        if(videodest== TYPE_DEST_GALLERY)
        {
            String strna = Utility.getFileNameFromDate(true);
            sFileName1 = strna;
//            if(sFileName2 !=null)
//            {
//                strna = sFileName2.substring(sFileName2.lastIndexOf("/") + 1);
//                strna = sLocalPath+"/"+strna;
//            }
            sFileName = strna+"_.tmp";
            //re =   GP4225_Device.StartRecord(sFileName, nType ,dest,bRecordAudio);
        }
//        else {
//            re =   GP4225_Device.StartRecord(sFileName, nType ,dest,bRecordAudio);
//        }
        re =   GP4225_Device.StartRecord(sFileName, nType ,dest,bRecordAudio);
        if(re == 0)
        {
            GP4225_Device.sRecordFileName = sFileName1;
        }
        return  re;
    }


    public static native  boolean isPhoneRecording();

    public static  long naGetRecordTimems()
    {
        return  GP4225_Device.getRecordTimems();
    }

    public static int naStopRecord(int nType)
    {
        return  GP4225_Device.StopRecord(nType);
    }
    public static  int naStopRecordAll()
    {
        return JoyAudioRecord.StopRecord(wifiCamera.TYPE_BOTH_PHONE_SD);

    }
    public static native void naSetBrightness(float fBrightness);
    //镜头传过来的数据旋转 0 90 180 270
    public static native  void naSetCameraDataRota(int n);
    public static native void naSetsquare(boolean b);     //正方形显示
    public static  native  void naSetCircul(boolean b);   //圆形显示，一般 圆形显示时，建议也调用一下 正方形显示
    /*
            b = true ,每接收到一整图片，就会通过 onGetFrame(Bitmao bmp)返回
            b = false,每接收到一整图片,由SDK内部用 JoyCameraView 来显示（这适用于
            只是简单显示图像，因为SDK内部已经固定了显示。APP层无法进行更多操作)
    */
    public static native  void naSetReceiveBmp(boolean b);
    //Gsensor
    public  static  native  void naEnableSensor(boolean b);  //使能Gsensor功能
    //以下普通用户无需调用
    public static native void naSetGsensorType(int n);   //内部校准时，需要设定Gsensor的的种类
    public static native void naSetAdjGsensorData(boolean b);     //是否需要内部校准数据，默认 true
    public static native void naSetGsensor2SDK(int xx,int yy,int zz);  //这是利用Rotate Filter 来旋转
    //SD_Download
    //nMode = 0 正常图传， 1 = SD 操作
    public static native int naSetDeviceMode(int nMode);
    /*
        APP 查询文件列表
         //从nStartInx 个文件开始，读取 nCount个文件信息
        // nStartInx,  >=0  nCount>=1
        naSetDeviceMode(1），进入文件列表模式
        nType = 1;  视频
        nType = 2;  锁定视频
        nType = 3'  相片
        nType = 4'  锁定相片
 */
    public static native int naGetSdFliesList(int nFileType,int nStartInx,int nCount);
    //下载文件
    public static native int naStartDonwLoad(String sFileName,int nLen,String sSaveName);
    //在线播放视频，有些SD卡视频不支持在线播放
    public static native int naStartDonwPlay(String sFileName,int nLen);
    //停止播放
    public static native int naStopDownLoadOrPlay();
    //删除SD上的指定文件
    public static native int naDeleteSDFile(String sFileName);
    //播放手机上的视频文件，主要是手机系统的播放器兼容性不足
    public static native int naPlayVideo(String sFile);
    public static native int naPlayPuse();     //pause resume  seek  只针对 播放视频文件有效。
    public static native int naPlayResume();   //有就是要先调用 naPlayVideo(String sFile) 后才有效。
    public static native int naSeek(int ns);

    private static  native  int Joy_Convert(String sPath,String sOutPath);

    public static  int  F_Convert(String sPath,String sOutPath)
    {
        File file = new File(sOutPath);
        if(file.exists())
        {
            file.delete();
        }
        File file1 = new File(sPath);
        if(file1.exists())
            return Joy_Convert(sPath,sOutPath);
        else
            return -100;
    }


    //获取手机中视频文件的缩略图,添加这个函数是因为有时手机的系统函数兼容性不会，有时无法获取到缩略图。
    private static native int naGetVideoThumbnailB(String filename,Bitmap bmp);

    public static Bitmap naGetVideoThumbnail(String filename)
    {
        Bitmap bitmap = Bitmap.createBitmap(100,100, Bitmap.Config.ARGB_8888);
        int re = naGetVideoThumbnailB(filename,bitmap);
        if(re == 0)
            return bitmap;
        else
            return null;

    }



    //同步时间
    public static native void na4225_SyncTime(byte[] data,int nLen);
    public static native void na4225_ReadTime();
    //是否显示水印
    public static native void na4225_SetOsd(boolean  b);
    public static native void na4225_ReadOsd();
    //设备图像翻转
    public static native void na4225_SetReversal(boolean  b);
    public static native void na4225_ReadReversal();
    //设置录像分段时间 - 0  1min  1  - 3min  2 - 5min
    public static native void na4225_SetRecordTime(int n);
    public static native void na4225_ReadRecordTime();
    //格式化SD卡
    public static native void na4225_FormatSD();
    //读取固件版本信息
    public static native void na4225_ReadFireWareVer();
    //恢复出厂设置
    public static native void na4225_ResetDevice();



    public static native  int  naSetIR(int n); //红外
    public static native  int  naReadIR();   // GP4225_GetIR_Status  返回
    public static native  int  naSetPIR(boolean bEnable);  //PIR
    public static native  int  naReadPIR();// GP4225_GetPIR_Status  返回
    public static native   void naSetStatusLedDisp(boolean b); //true  led 状态灯显示  false 不打开状态灯
    public static native   void naReadStatusLedDisp(); //读取  GP4225_GetLed_Status  返回


    //变焦
    public static native int naSetZoomFocus(int nLevel);
    public static native int naGetZoomeFocus();
    public static native int naStartAutoFocus(boolean bStart);

    public  static native void naStartAdjFocus(int x,int y);

    public static native int naGetAdjFocusValue();
    public static native int naSetAdjFocusValue(int nValue);

    ///

    //建立相册目录，Android10 以下要注意权限 "sDir" 也是相册名称
    public static void naCreateLocalDir(String sDir)
    {
        Utility.F_CreateLocalDir(sDir);
    }




    public static native void naGetWifiSSID();
    public static native  void  naSetWifiSSID(String sSSid);
    public static native int naSetWifiPassword(String sPassword);
    public static native void naGetWifiPassword();

    public static native void naRebootDevice(); //重启设备
    public static native void naResetDeviceDefault(); //恢复默认值
    public static native int naEliminateBlackBorder(int x1,int y1,int x2,int y2); //截去黑边



}
