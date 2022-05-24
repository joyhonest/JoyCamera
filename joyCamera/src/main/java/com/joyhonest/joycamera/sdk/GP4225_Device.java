package com.joyhonest.joycamera.sdk;

public class GP4225_Device {

    //private static final String TAG = "Device";

    public  static int     nMode = 0;
    public  static boolean bSD =false;
    public  static boolean bSDRecording =false;


    public  static byte[] MacAddress = null;//= new byte[6];
    public  static  byte  CustermerType=0;
    public  static  byte  DeviceType=0;

    public  static int VideosCount = 0;
    public  static int LockedCount = 0;
    public  static int PhotoCount = 0;

    public  static long nSDAllSize = 0;        //1024*1024 unit
    public  static long nSDAvaildSize = 0;

    public  static int nBattery = 0;
    public  static boolean bAdjfocus = false;

    public  static int nFuncMask = 0;


    public  static int nSDRecordTime = 0;

    public  static String sVer = "";

    static String sRecordFileName=null;



    static boolean GP4225_PressData(byte[] data)
    {
        return JoyProcessData.PressData(data);
    }

    static int StartRecord(String pFileName, int nType, int dest, boolean bRecordAudio)
    {
        return  JoyAudioRecord.StartRecord(pFileName,nType,dest,bRecordAudio);
    }
    static long getRecordTimems()
    {
        return JoyAudioRecord.getRecordTimems();
    }
    static int StopRecord(int nType)
    {
        return JoyAudioRecord.StopRecord(nType);
    }
    static int InitVideo(int width, int height, int bitrate, int fps1)
    {
        return  JoyAudioRecord.InitVideo(width,height,bitrate,fps1);
    }

}
