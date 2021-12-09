package com.joyhonest.joycamera.sdk;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaFormat;


import org.simple.eventbus.EventBus;

import java.nio.ByteBuffer;
import java.util.Locale;

public class Utility {

    private static final String TAG = "Utility";
    static ByteBuffer mDirectBuffer;




    private static int InitVideoMediacode(int width, int height, int bitrate, int fps1)
    {
        return GP4225_Device.InitVideo(width,height,bitrate,fps1);
    }

    static native void naStopRecordV(int nType);

    static native int naStartRecordV(int nType, int dest);
    static native boolean naGetCameraisConnected();


    private static void VidoeDataEncoder(byte[] data) {
        JoyAudioRecord.VidoeDataEncoder(data);
    }





    //下载文件回调 nError =1 表示有错误。
    private static void DownloadFile_callback(int nPercentage, String sFileName, int nError) {
        jh_dowload_callback jh_dowload_callback = new jh_dowload_callback(nPercentage, sFileName, nError);
        EventBus.getDefault().post(jh_dowload_callback, "DownloadFile");
    }

    private static void onGetKey(int nKey) {
        JoyLog.e(TAG, "Get Key = " + nKey);
        Integer nKey_ = nKey;
        EventBus.getDefault().post(nKey_, "onGetKey");
    }

    private static Bitmap bmp = null;
    private static void onGetFrame(int w, int h) {
        if (bmp == null)
            bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        else {
            if (bmp.getWidth() != w || bmp.getHeight() != h) {
                bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            }
        }
        mDirectBuffer.rewind();
        bmp.copyPixelsFromBuffer(mDirectBuffer);
        EventBus.getDefault().post(bmp, "onGetFrame");
    }

    // 读取 20000 或者 20001端口，如果SDK内部没有处理，就通过这里返回来处理
    private static void onUdpRevData(byte[] data, int nPort) {
        if (nPort == 20001) {
            if (!GP4225_Device.GP4225_PressData(data)) {
                ;
            }
        }
    }

    //拍照完成。回调此函数。
    //录像有JAVA 调用此函数
    static void OnSnaporRecrodOK(String sName, int nPhoto) {
        if(sName!=null) {
            String Sn = String.format(Locale.ENGLISH,"%02d%s", nPhoto, sName);
            JoyLog.e(TAG,Sn);
            EventBus.getDefault().post(Sn, "SavePhotoOK");
        }
    }
    //  当模块状态改变时回调函数
    private static void onStatusChange(int nStatus) {
        Integer n = nStatus;
        //EventBus.getDefault().post(n, "SDStatus_Changed");      //调用第三方库来发送消图片显示消息。
        EventBus.getDefault().post(n, "onCameraStatusChange");
        //#define  bit0_OnLine            1
        //#define  bit1_LocalRecording    2
        //#define  SD_Ready               4
        //#define  SD_Recroding           8
        //#define  SD_Photo               0x10
    }

    private static void OnPlayStatus(int n)
    {
        //n !=0  Play is Start  0= Play is over
        Integer i = n;
        EventBus.getDefault().post(i,"OnPlayStatus");

    }

    private static void SentPlayDuration(long n)  //返回播放文件总时长
    {
        Integer nn = (int)n;
        EventBus.getDefault().post(nn, "onDuration");
    }


    private static void SentPlayTime(long n)
    {
        Integer nn = (int)n;
        EventBus.getDefault().post(nn, "onPlaytime");
    }

}
