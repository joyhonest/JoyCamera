package com.joyhonest.joycamera.sdk;

import android.util.Log;

public class JoyLog {
    private static  boolean DEBUG = false;
    public static  void  SetDebug(boolean b)
    {
        DEBUG = b;
    }
    public static  void e(String stag,String sMsg)
    {
        if(DEBUG)
        {
            Log.e(stag,sMsg);
        }
    }
    public static  void i(String stag,String sMsg)
    {
        if(DEBUG)
        {
            Log.i(stag,sMsg);
        }
    }
    public static  void v(String stag,String sMsg)
    {
        if(DEBUG)
        {
            Log.v(stag,sMsg);
        }
    }
    public static  void d(String stag,String sMsg)
    {
        if(DEBUG)
        {
            Log.d(stag,sMsg);
        }
    }
}
