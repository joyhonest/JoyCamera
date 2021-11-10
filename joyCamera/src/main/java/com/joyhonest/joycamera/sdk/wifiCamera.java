package com.joyhonest.joycamera.sdk;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.simple.eventbus.EventBus;

import java.nio.ByteBuffer;

public class wifiCamera {

    private static String sAlbumName = "JOY_Camera";

    private static Context applicationContext = null;

    public static int TYPE_ONLY_PHONE = 0;
    public static int TYPE_ONLY_SD = 1;
    public static int TYPE_BOTH_PHONE_SD = 2;

    public static int TYPE_DEST_SNADBOX = 0;
    public static int TYPE_DEST_GALLERY = 1;



    public static  int  CAMERA_NORMAL_MODE = 0;
    public static  int  CAMERA_FILELIST_MODE = 1;

    private static final int BMP_Len = (((4096 + 3) / 4) * 4) * 4 * 3072 + 2048;
    private  final  static int CmdLen = 2048;
    //private  static boolean  bProgressGP4225UDP=true;


    private static final String  TAG = "wifiCamera";
    static {
        System.loadLibrary("joycamera");
        Utility.mDirectBuffer = ByteBuffer.allocateDirect(BMP_Len + CmdLen);     //获取每帧数据，主要根据实际情况，分配足够的空间。
        naSetDirectBuffer(Utility.mDirectBuffer, BMP_Len + CmdLen);
        Utility.gp4225_Device = new GP4225_Device();
    }

    /*
     * 以下这些是JoyCameraView 使用
     * 普通用户无需直接调用
     */

    public static  void naSetApplicationContext(Context context)
    {
        applicationContext = context.getApplicationContext();
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

    public static native  int naInit(String sPara);
    private static native  int naStopA();
    public static  int naStop()
    {
        Utility.StopRecordAll();
        naStopA();
        return 0;
    }

    public static native  void naSetFlip(boolean b);
    public static native  void naSetMirror(boolean b);
    public static native void naSet3DDenoiser(boolean b); //视频是否3D降噪

    public static native void naSetEnableRotate(boolean b); //视频是否可以旋转任意角度。 如果调用naEnableSensor，会从naEnableSensor 内部调用次函数
    public static native void naSetFilterRotate(float nAngle); //一般用户无需调用
    public static native void naSetEnableEQ(boolean b);
    public static  void naSetAlbumName(String sAlbumName_)
    {
        sAlbumName = sAlbumName_;
    }

    public static native int naSetRecordWH(int ww, int hh);

    public static native int naSnapPhoto(String sPath, int nType ,int dest);
    //录像
    public static  int naStartRecord(String pFileName, int nType ,int dest,boolean bRecordAudio)
    {
        return   Utility.StartRecord(pFileName, nType ,dest,bRecordAudio);
    }

    public static  long naGetRecordTimems()
    {
        return  Utility.getRecordTimems();
    }

    public static int naStopRecord(int nType)
    {
        return  Utility.StopRecord(nType);
    }
    public static  int naStopRecordAll()
    {
        return  Utility.StopRecordAll();
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
    //nMode = 0 正常图传， 1 = SD 浏览
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
    public static native int naStartDonwLoad(String sFileName,int nLen,String sSaveName);
    public static native int naStartDonwPlay(String sFileName,int nLen);
    public static native int naStopDownLoadOrPlay();
    //变焦
    public static native int naSetZoomFocus(int nLevel);
    public static native int naGetZoomeFocus();
    public static native int naStartAutoFocus(boolean bStart);
    public static native int naGetAdjFocusValue();
    public static native int naSetAdjFocusValue(int nValue);

}
