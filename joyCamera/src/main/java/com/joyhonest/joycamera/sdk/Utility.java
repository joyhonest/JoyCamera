package com.joyhonest.joycamera.sdk;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.IntentSender;
import android.database.Cursor;
import android.graphics.Bitmap;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;


import org.simple.eventbus.EventBus;
import org.simple.eventbus.EventType;
import org.simple.eventbus.Subscription;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

public class Utility {

    private static final String TAG = "Utility";
    static ByteBuffer mDirectBuffer;
    static ByteBuffer mDirectUsbCameraYuvBuffer;
    static boolean  bJavaUdp=false;
    public static String  sCameraModel = "";

    private static int InitVideoMediacode(int width, int height, int bitrate, int fps1)
    {
        return GP4225_Device.InitVideo(width,height,bitrate,fps1);
    }

    private static int Init_MediaConvert(int width, int height, int bitrate, int fps1)
    {
        return JoyAudioRecord.Init_MediaConvert(width,height,bitrate,fps1);
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

    private static  void onGetLedPwm(int nPwm)// "GPSSOCKT'
    {
        Integer  nLed = nPwm;
        EventBus.getDefault().post(nLed,"onGetLed");
        EventBus.getDefault().post(nLed,"onGetLedPWM");
    }

    private static void onGetKey(int nKey) {
        JoyLog.e(TAG, "Get Key = " + nKey);
        Integer nKey_ = nKey;
        EventBus.getDefault().post(nKey_, "onGetKey");
    }






    private static Bitmap bmpG = null;
    private static Bitmap bmpPre = null;

    private static void onGetFrame(int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mDirectBuffer.rewind();
        bmp.copyPixelsFromBuffer(mDirectBuffer);
        EventBus.getDefault().post(bmp, "onGetFrame");


    }

    // 读取 20000 或者 20001端口，如果SDK内部没有处理，就通过这里返回来处理
    private static void onUdpRevData(byte[] data, int nPort) {
        if (nPort == 20001) {
            if(bJavaUdp)
            {
                EventBus.getDefault().post(data,"onGet20001PortData");
            }
            else {
                if (!GP4225_Device.GP4225_PressData(data)) {
                    EventBus.getDefault().post(data, "onGet20001PortData");
                }
            }
        }
        if(nPort == 20000)
        {

            if(bJavaUdp)
            {
                EventBus.getDefault().post(data,"onGet20000PortData");
            }
            else {
                if (!GP4225_Device.GP4225_PressData_20000(data)) {
                    EventBus.getDefault().post(data, "onGet20000PortData");
                }
            }

        }
    }

    //拍照完成。回调此函数。
    //录像有JAVA 调用此函数
    static void OnSnaporRecrodOK(String sName, int nPhoto) {
        if(sName!=null) {
            String Sn = String.format(Locale.ENGLISH,"%02d%s", nPhoto, sName);
            if(nPhoto == 0)
            {
                if(sCameraModel.length()>2)
                {
                    //File file1 = new File(sName);
                    try {
                        //FileInputStream fileInputStream = new FileInputStream(file1);
                        //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                        {
                            ExifInterface exifInterface = null;
                            exifInterface = new ExifInterface(sName);
                            exifInterface.setAttribute(ExifInterface.TAG_MODEL,sCameraModel);
                            exifInterface.saveAttributes();
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                if(wifiCamera.photodest == wifiCamera.DEST_GALLERY ) {
                    F_Save2ToGallery(sName);
                    wifiCamera.photodest=-1;
                    Sn = String.format(Locale.ENGLISH,"%02d%s", 3, sName);
                }
            }
            else
            {
                if(wifiCamera.videodest == wifiCamera.DEST_GALLERY ) {
                    F_Save2ToGallery(sName);
                    wifiCamera.videodest = -1;
                    Sn = String.format(Locale.ENGLISH,"%02d%s", 3, sName);
                }
            }
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

    public static boolean isAndroidQ() {
        return Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;
    }

    public static void ClearTempDir() {
        if(wifiCamera.getApplicationContext() == null)
            return;
        File file = wifiCamera.getApplicationContext().getExternalFilesDir("temp");
        if (file != null) {
            if (file.exists()) {
                file.delete();
            }
        }
    }
    public static String getNormalSDCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    static void F_CreateLocalDir(String sAlbum_) {
        if(wifiCamera.getApplicationContext() == null)
            return;
        wifiCamera.sAlbum = sAlbum_;
        if (isAndroidQ())
        {
            ClearTempDir();
            File file = wifiCamera.getApplicationContext().getExternalFilesDir(sAlbum_);
            if (file != null) {
                wifiCamera.sLocalPath = file.getAbsolutePath();
            }
        }
        else {
            String StroragePath;
            try {
                StroragePath = getNormalSDCardPath();
            } catch (Exception e) {
                return;
            }
            String recDir;
            File fdir;

            recDir = String.format("%s/%s", StroragePath, sAlbum_);

            fdir = new File(recDir);
            if (!fdir.exists()) {
                fdir.mkdirs();
            }
            wifiCamera.sLocalPath = recDir;
        }
    }

    private static native void _naSetFileName(String strname);

    public static String getFileNameFromDate(boolean bVideo) {
        if(wifiCamera.sLocalPath==null)
            return null;
        Date d = new Date();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd-HHmmssSSS", Locale.getDefault());
        String strDate = f.format(d);
        if(wifiCamera.sFileNamePre.length()>0 )
        {
            strDate = wifiCamera.sFileNamePre+"-"+strDate;
        }

        String ext = "mp4";
        if (!bVideo) {
            ext = "jpg";
        }
        _naSetFileName(strDate + "." + ext);
        String recDir = wifiCamera.sLocalPath;
        File dirPath = new File(recDir);
        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }
        return recDir + "/" + strDate + "." + ext;
    }

    public static List<Uri> F_GetAllLocalFiles(boolean bPicture) {
        if(wifiCamera.getApplicationContext() == null)
            return null;
        String sAlbum_ = Environment.DIRECTORY_DCIM + File.separator + wifiCamera.sAlbum;
        String s2 = wifiCamera.sLocalPath;
        List<Uri> list = new ArrayList<Uri>();
        Context mContext = wifiCamera.getApplicationContext().getApplicationContext();
        ContentResolver resolver = mContext.getContentResolver();
        if (!sAlbum_.endsWith("/")) {
            sAlbum_ += "/";
        }
        Cursor cursor;
        Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        if (!bPicture) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }
        if (isAndroidQ()) {
            cursor = resolver.query(contentUri, new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.RELATIVE_PATH + " = ?",
                    new String[]{sAlbum_}, null);
        }
        else
        {
            cursor = resolver.query(contentUri, new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + " like ?",
                    new String[]{"%"+s2 + "/%"}, null);
        }
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                Uri uri = ContentUris.withAppendedId(contentUri, id);
                list.add(uri);
            }
            cursor.close();
        }
        return list;
    }
    public static Uri F_CheckIsExit(String sFullPahtName) {
        if(wifiCamera.getApplicationContext() == null)
            return null;
        String stype = sFullPahtName.substring(sFullPahtName.lastIndexOf(".") + 1);
        boolean bPicture = true;
        if (stype.equalsIgnoreCase("mp4") ||
                stype.equalsIgnoreCase("avi") ||
                stype.equalsIgnoreCase("mov")) {
            bPicture = false;
        }
        return F_CheckIsExit(sFullPahtName, bPicture);


    }
    private static Uri F_CheckIsExit(String sFullPahtName, boolean bPhoto) {

        String sfile = sFullPahtName.substring(sFullPahtName.lastIndexOf("/") + 1); //文件名
        //String s2 = sLocalPath;

        String slocal = Environment.DIRECTORY_DCIM + File.separator + wifiCamera.sAlbum;

        ContentResolver resolver = wifiCamera.getApplicationContext().getContentResolver();
        Cursor cursor;

        if (!slocal.endsWith("/")) {
            slocal += "/";
        }

        Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        if (!bPhoto) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }
        if (isAndroidQ()) {
            cursor = resolver.query(contentUri, new String[]{MediaStore.Images.Media._ID},
                    MediaStore.Images.Media.RELATIVE_PATH + "=? and " + MediaStore.Images.Media.DISPLAY_NAME + "=?",
                    new String[]{slocal, sfile}, null);
        } else {
            cursor = resolver.query(contentUri, new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + " like ?",
                    new String[]{"%" + wifiCamera.sLocalPath + "/" + sfile + "%"}, null);
        }

        if (cursor != null) {
            if (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                Uri uri = ContentUris.withAppendedId(contentUri, id);
                cursor.close();
                return uri;
            } else {
                cursor.close();
                return null;
            }
        }
        return null;
    }


    //储存到图库
    public static int F_Save2ToGallery(String sFullPathName) {
        if (wifiCamera.getApplicationContext() == null)
            return -1;
        File file1 = new File(sFullPathName);
        if (!file1.exists()) {
            return -2;
        }


        boolean bPhoto = false;
        String slocal;
        String sfilename = sFullPathName.substring(sFullPathName.lastIndexOf("/") + 1);
        String stype = sFullPathName.substring(sFullPathName.lastIndexOf(".") + 1);
        if(stype.equalsIgnoreCase("jpg") ||
                stype.equalsIgnoreCase("png") ||
                stype.equalsIgnoreCase("bmp"))
        {
            bPhoto = true;
        }
        else if(stype.equalsIgnoreCase("mp4") ||
                stype.equalsIgnoreCase("mov") ||
                stype.equalsIgnoreCase("avi"))
        {
            bPhoto = false;
        }
        else
        {
            return -3;
        }
        Uri uri;
        slocal = Environment.DIRECTORY_DCIM + File.separator + wifiCamera.sAlbum;
        ContentResolver contentResolver = wifiCamera.getApplicationContext().getContentResolver();
        ContentValues values = new ContentValues();
        if (isAndroidQ()) {
            if (F_CheckIsExit(sFullPathName) != null)
                return -4;
            if (bPhoto) {
                values.put(MediaStore.Images.Media.DISPLAY_NAME, sfilename);
                if (stype.equalsIgnoreCase("png")) {
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                } else {
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                }
                values.put(MediaStore.Images.Media.RELATIVE_PATH, slocal);
                uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values);
            } else {

                values.put(MediaStore.Video.Media.DISPLAY_NAME, sfilename);
                if (stype.equalsIgnoreCase("mov")) {
                    values.put(MediaStore.Video.Media.MIME_TYPE, "video/mov");
                } else if (stype.equalsIgnoreCase("avi")) {
                    values.put(MediaStore.Video.Media.MIME_TYPE, "video/avi");
                } else {
                    values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                }
                values.put(MediaStore.Video.Media.RELATIVE_PATH, slocal);
                uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        values);
            }

            if (uri != null) {
                try (OutputStream outputStream = contentResolver.openOutputStream(uri))
                {
                    //File file = new File(sFullPathName);
                    //2、建立数据通道
                    FileInputStream fileInputStream = new FileInputStream(file1);
                    byte[] buf = new byte[1024 * 100];
                    int length = 0;
                    while ((length = fileInputStream.read(buf)) != -1) {
                        outputStream.write(buf, 0, length);
                    }
                    fileInputStream.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }


                if (file1.isFile() && file1.exists()) {
                    file1.delete();
                }
            }
        } else {
            try {
                if (bPhoto)
                {
                    if (stype.equalsIgnoreCase("png")) {
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                    } else {
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    }
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.DATA, sFullPathName);
                    uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            values);
                } else {
                    if (stype.equalsIgnoreCase("mov")) {
                        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mov");
                    }
                    else if (stype.equalsIgnoreCase("avi")) {
                        values.put(MediaStore.Video.Media.MIME_TYPE, "video/avi");
                    }
                    else
                    {
                        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                    }
                    values.put(MediaStore.Video.Media.DATA, sFullPathName);
                    uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            values);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }


    //删除图片或者视频，并且把它从系统图库中清除

    public static void DeleteImages(String[] imgPaths, Activity activity)
    {
        if(wifiCamera.getApplicationContext()==null)
            return;
        if(imgPaths ==null)
            return ;
        if(imgPaths.length==0)
            return;
        List<Uri> uris = new ArrayList<>(); // 这里存放所有需要删除的图片的uri

        Context mContext = wifiCamera.getApplicationContext().getApplicationContext();
        ContentResolver resolver = mContext.getContentResolver();
        String sPa;

        if(isAndroidQ())
        {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                {
                    for(String sImg: imgPaths)
                    {
                        uris.add(Uri.parse(sImg));
                    }
                    PendingIntent pi = MediaStore.createDeleteRequest(resolver, uris);
                    try {
                        int REQUEST_CODE_DELETE_IMAGE = 202201;
                        activity.startIntentSenderForResult(pi.getIntentSender(),REQUEST_CODE_DELETE_IMAGE, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e1) {
                        e1.printStackTrace();
                    }
                }
                else
                {
                    for(String str:imgPaths) {
                        Uri uri = Uri.parse(str);
                        try {
                            //android 10 ,请以兼容模式来运行，否则，删除APP再重新安装，之前的资料在这里删除会提示无权限
                            int count = resolver.delete(uri, null, null);
                        }
//                        catch (RecoverableSecurityException e)
//                        {
//                           这里其实如Android 11 以上那样，调用系统的弹出窗口来删除文件，但这样的话，每一个文件就要弹一次，太不人性花了。
//                            所以建议 在Manifset 中加入  android:requestLegacyExternalStorage="true" 强制Android 10 中使用兼容模式来做
//                            IntentSender intentSender= e.getUserAction().getActionIntent().getIntentSender();
//                            try {
//                                int REQUEST_CODE_DELETE_IMAGE = 202201;
//                                activity.startIntentSenderForResult(intentSender,REQUEST_CODE_DELETE_IMAGE, null, 0, 0, 0);
//                            } catch (IntentSender.SendIntentException e1) {
//                                e1.printStackTrace();
//                            }
//                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }
        }
        else {
            for (String sImg : imgPaths)
            {

                Uri uri = Uri.parse(sImg);
                final String column = "_data";
                final String[] projection = {column};
                try {
                    Cursor cursor = resolver.query(uri, projection, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        final int column_index = cursor.getColumnIndexOrThrow(column);
                        sPa = cursor.getString(column_index);    //获取文件路径和名称
                    if (sPa != null) {
                        File f = new File(sPa);
                        if (f.exists() && f.isFile()) {
                            f.delete();
                        }
                    }
                }
            } catch (Exception ignored) {

            }
            try {
                int count = resolver.delete(uri, null, null);
            } catch (Exception ignored) {
            }
        }
     }



    }

    public static int DeleteImage(String imgPath)
    {
        int re = -1;
        if(wifiCamera.getApplicationContext()==null)
            return re;

        Uri uri = Uri.parse(imgPath);
        Context mContext = wifiCamera.getApplicationContext().getApplicationContext();
        ContentResolver resolver = mContext.getContentResolver();
        String sPa;
        if(isAndroidQ())
        {
            try {
                int count = resolver.delete(uri, null, null);
                re = 0;
            } catch (Exception e) {
                e.printStackTrace();
                re = -1;
            }
            return re;
        }
        else
        {
            boolean re1=false;
            boolean re2 = false;
            final String column = "_data";
            final String[] projection = {column};
            try {
                Cursor cursor = resolver.query(uri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    final int column_index = cursor.getColumnIndexOrThrow(column);
                    sPa = cursor.getString(column_index);    //获取文件路径和名称
                    if (sPa != null) {
                        File f = new File(sPa);
                        if (f.exists() && f.isFile()) {
                            f.delete();
                        }
                    }
                }
                re1 = true;
            } catch (Exception ignored) {

            }
            try {
                int count = resolver.delete(uri, null, null);
                re2 = true;
            } catch (Exception ignored) {
            }
            if(re1 && re2)
                re = 0;
            return re;
        }
    }

    //2023-02-08 添加实时播放PCM
    private static AudioTrack audioTrack = null;
    // audioFormat  AudioFormat.ENCODING_PCM_16BIT or  AudioFormat..ENCODING_PCM_8BIT
    //  nFreq = 8000,.....
    public static  void F_StartPlayAudio(int nFreq,int audioFormat)
    {
        if(audioTrack!=null)
        {
            try {
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        //int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        int mMinBufSize = AudioTrack.getMinBufferSize(nFreq, channelConfig, audioFormat);
        try {
            audioTrack = new AudioTrack(AudioManager.USE_DEFAULT_STREAM_TYPE, nFreq, channelConfig,
                    audioFormat, mMinBufSize, AudioTrack.MODE_STREAM);
            audioTrack.play();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void F_StopPlayAudio()
    {
        if(audioTrack!=null)
        {
            try {
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private static void WriteAudioData(byte[] data)
    {
        if(audioTrack!=null)
        {
            try {
                audioTrack.write(data,0,data.length);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

}

