package com.joyhonest.joycamera.sdk;


import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.util.Log;

import org.simple.eventbus.EventBus;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Utility {


    private static final int KEY_CHANNEL_COUNT = 1;
    private static final int KEY_SAMPLE_RATE = 16000;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static final int CHANNEL_MODE = AudioFormat.CHANNEL_IN_MONO;

    private static int BUFFFER_SIZE = (2048 * KEY_CHANNEL_COUNT);


    private static final int nCt = (BUFFFER_SIZE * 1000000) / (KEY_SAMPLE_RATE * 2 * KEY_CHANNEL_COUNT);


    private static int fps = 0;


    public static ByteBuffer mDirectBuffer;
    public static GP4225_Device gp4225_Device;
    private static final String TAG = "wifiCamera-Utility";

    //下载文件回调 nError =1 表示有错误。
    private static void DownloadFile_callback(int nPercentage, String sFileName, int nError) {
        jh_dowload_callback jh_dowload_callback = new jh_dowload_callback(nPercentage, sFileName, nError);
        EventBus.getDefault().post(jh_dowload_callback, "DownloadFile");
    }


    private static void onGetKey(int nKey) {
        Log.e(TAG, "Get Key = " + nKey);
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


    /*
    旧模块 比如  192.168.29 通过 udp port20000 返回按键
*/
    // 读取 20000 或者 20001端口，如果SDK内部没有处理，就通过这里返回来处理
    private static void onUdpRevData(byte[] data, int nPort) {
        if (nPort == 20001) {
            if (!gp4225_Device.GP4225_PressData(data)) {
                ;
            }
        }
    }


    //拍照或者录像完成。回调此函数。
    private static void OnSnaporRecrodOK(String sName, int nPhoto) {
        String Sn = String.format("%02d%s", nPhoto, sName);
        EventBus.getDefault().post(Sn, "SavePhotoOK");
    }


    //  当模块状态改变时回调函数
    private static void onStatusChamnge(int nStatus) {
        Integer n = nStatus;
        //EventBus.getDefault().post(n, "SDStatus_Changed");      //调用第三方库来发送消图片显示消息。
        EventBus.getDefault().post(n, "onCameraStatusChange");
        //#define  bit0_OnLine            1
        //#define  bit1_LocalRecording    2
        //#define  SD_Ready               4
        //#define  SD_Recroding           8
        //#define  SD_Photo               0x10
    }

////


    private static boolean bGAudio = false;
    private static boolean bRecording = false;
    private static boolean bMuxerStart = false;
    private static MediaMuxer mediaMuxer = null;
    private static AudioRecord audioRecord = null;
    private static MediaCodec videoMediaCode = null;
    private static MediaCodec audioMediaCode = null;
    private static long pts = 0;
    private static long pts_a = 0;
    private static long pts_a_ = 0;
    private static long pts_ = 0;
    private static long nCountFrame = 0;
    private static long nCountFrameAudio = 0;
    private static int vIndex = -1;
    private static int aIndex = -1;


    private static MediaFormat F_GetMediaFormat(int width, int height, int bitrate, int fps_, int color) {
        if (videoMediaCode != null) {
            videoMediaCode.stop();
            videoMediaCode.release();
            videoMediaCode = null;
            pts = 0;
            pts_a = 0;
            pts_ = 0;
            fps = fps_;
            nCountFrame = 0;
            nCountFrameAudio = 0;
        }
        fps = fps_;

        pts_a = 0;
        pts = 0;
        pts_ = 0;
        nCountFrame = 0;
        nCountFrameAudio = 0;
        boolean bOK = true;
        try {
            videoMediaCode = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException | IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
            bOK = false;
        }
        if (!bOK) {
            videoMediaCode = null;
            return null;
        }

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height); //height和width一般都是照相机的height和width。
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, width);
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, height);
        //描述平均位速率（以位/秒为单位）的键。 关联的值是一个整数
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        //关键帧间隔时间，单位是秒
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
        //描述视频格式的帧速率（以帧/秒为单位）的键。
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);//帧率，一般在15至30之内，太小容易造成视频卡顿。
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, color);//色彩格式，具体查看相关API，不同设备支持的色彩格式不尽相同

        try {
            videoMediaCode.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            e.printStackTrace();
            mediaFormat = null;
        }
        return mediaFormat;

    }

    private static int InitVideoMediacode(int width, int height, int bitrate, int fps1) {
        int nColor;
        nColor = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        if (F_GetMediaFormat(width, height, bitrate, fps1, nColor) == null) {
            nColor = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
            if (F_GetMediaFormat(width, height, bitrate, fps1, nColor) == null) {
                nColor = 0;
            }
        }

        if (nColor != 0) {
            videoMediaCode.start();
        } else {
            videoMediaCode.release();
            videoMediaCode = null;
        }
        return nColor;
    }


    private static int nRecDes = wifiCamera.TYPE_DEST_SNADBOX;

    private static native int naStartRecordV(int nType, int dest);

    private static native boolean naGetCameraisConnected();


    public static long getRecordTimems() {
        if (!bRecording) {
            return 0;
        }
        if (fps <= 0) {
            return 0;
        }
        return (long) (nCountFrame * (1000.0f / fps));
    }

    public static int StartRecord(String pFileName, int nType, int dest, boolean bRecordAudio) {
        if (!naGetCameraisConnected())
            return -1;
        if (bRecording)
            return -2;
        bCanStartWrite = false;
        if (nType == wifiCamera.TYPE_ONLY_PHONE || nType == wifiCamera.TYPE_BOTH_PHONE_SD) {
            nRecDes = dest;
            StartRecord_A(pFileName, bRecordAudio);
            naStartRecordV(nType, dest);
            bRecording = true;
        }
        return 0;
    }


    public static int StartRecord_A(String sfliename, boolean bAudio) {
        bGAudio = bAudio;
        if (bRecording) {
            try {
                bRecording = false;
                bMuxerStart = false;
                if (videoMediaCode != null) {
                    videoMediaCode.stop();
                    videoMediaCode.release();
                    videoMediaCode = null;
                }
                if (audioMediaCode != null) {
                    audioMediaCode.stop();
                    audioMediaCode.release();
                    audioMediaCode = null;
                }
                if (mediaMuxer != null) {
                    mediaMuxer.stop();
                    mediaMuxer.release();
                    mediaMuxer = null;
                }
            } catch (Exception e) {
                ;
            }

        }
        try {
            mediaMuxer = new MediaMuxer(sfliename, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            bMuxerStart = false;
        } catch (Exception e) {
            ;
        }
        if (bGAudio) {
            if (!isCanRecordAudio()) {
                bGAudio = false;
            }
        }
        if (bGAudio) {
            try {
                audioMediaCode = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
                MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, KEY_SAMPLE_RATE, KEY_CHANNEL_COUNT);
                int KEY_BIT_RATE = 16000;
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, KEY_BIT_RATE);
                int KEY_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
                mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, KEY_AAC_PROFILE);
                audioMediaCode.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                audioMediaCode.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            StartRecordAudio(true);
            SystemClock.sleep(220);
        } else {
            StartRecordAudio(false);
        }
        return 0;
    }


    private static byte[] mBuffer;
    private static Worker worker = null;

    private static class Worker extends Thread {
        public boolean isRunning = false;

        @Override
        public void run() {
            int re = 0;
            pts = 0;
            isRunning = true;
            while (isRunning) {
                re = audioRecord.read(mBuffer, 0, BUFFFER_SIZE);
                Log.e(TAG, "read = " + re + " buffersize = " + BUFFFER_SIZE);
                AudioDataEncoder(mBuffer);
            }
        }
    }

    private static void StartRecordAudio(boolean b) {

        if (b) {
            if (audioRecord != null) {
                try {
                    audioRecord.stop();
                    audioRecord.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            int minBufferSize = AudioRecord.getMinBufferSize(KEY_SAMPLE_RATE, CHANNEL_MODE, AUDIO_FORMAT);
            BUFFFER_SIZE = Math.max(BUFFFER_SIZE, minBufferSize);
            mBuffer = new byte[BUFFFER_SIZE];

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, KEY_SAMPLE_RATE, CHANNEL_MODE, AUDIO_FORMAT, BUFFFER_SIZE);
            audioRecord.startRecording();
            {
                if (worker != null && worker.isRunning) {
                    worker.isRunning = false;
                    SystemClock.sleep(20);
                }
                worker = new Worker();
                worker.start();
            }

        } else {
            if (audioRecord != null) {
                try {
                    audioRecord.stop();
                    audioRecord.release();
                } catch (Exception e) {
                    ;
                }
                audioRecord = null;
            }

            worker.isRunning = false;
            SystemClock.sleep(50);
            worker = null;
        }
    }


    public static boolean isCanRecordAudio() {
        boolean re = false;
        try {
            int minBufferSize = AudioRecord.getMinBufferSize(KEY_SAMPLE_RATE, CHANNEL_MODE, AUDIO_FORMAT) * 2;
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, KEY_SAMPLE_RATE, CHANNEL_MODE, AUDIO_FORMAT, minBufferSize);
            audioRecord.startRecording();
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            re = true;
        } catch (Exception e) {
            ;
        }
        return re;
    }


    static boolean bCanStartWrite = false;

    private static int writeSampleData(boolean bVideo, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        if (!bMuxerStart)
            return -5;
        if (bVideo && !bCanStartWrite && (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0)  //第一个I帧
        {
            bCanStartWrite = true;
        }

        if (!bCanStartWrite)
            return -1;
        int re = -1;
        if (bVideo) {
            if (vIndex >= 0) {
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0)  //控制 sps pps 已经在 addtrack中format种添加进去了。
                {
                    try {
                        long ta = (nCountFrame*1000000) / fps;
                        bufferInfo.presentationTimeUs = ta;
                        mediaMuxer.writeSampleData(vIndex, byteBuf, bufferInfo);
                        nCountFrame++;
                        re = 0;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            if (aIndex >= 0 ) {
                try {
                    if (bufferInfo.size > 10) {
                        long ta = nCountFrameAudio * nCt;
                        bufferInfo.presentationTimeUs = ta;
                        mediaMuxer.writeSampleData(aIndex, byteBuf, bufferInfo);
                        nCountFrameAudio++;
                    }
                    re = 0;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return re;
    }

    private static void Addtrack(MediaFormat format, boolean bVideo) {
        if (bVideo) {
            if (vIndex < 0) {
                vIndex = mediaMuxer.addTrack(format);
            }
            if (vIndex >= 0) {
                if (bGAudio) {
                    if (aIndex >= 0) {
                        if (!bMuxerStart) {
                            mediaMuxer.start();
                            bMuxerStart = true;
                            bCanStartWrite = false;
                            Log.e(TAG, "Start --- vide = " + vIndex + " audio = " + aIndex);
                        }
                    }
                } else {
                    if (!bMuxerStart) {
                        mediaMuxer.start();
                        bMuxerStart = true;
                        bCanStartWrite = false;
                        Log.e(TAG, "Start --- vide = " + vIndex + " audio = " + aIndex);
                    }
                }
            }
        } else {
            if (aIndex < 0) {
                aIndex = mediaMuxer.addTrack(format);
                if (aIndex >= 0) {
                    if (vIndex >= 0) {
                        if (!bMuxerStart) {
                            mediaMuxer.start();
                            bMuxerStart = true;
                            bCanStartWrite = false;
                            Log.e(TAG, "Start --- audio  vide = " + vIndex + " audio = " + aIndex);
                        }
                    }
                }
            }
        }
    }

    private static void VidoeDataEncoder(byte[] data) {
        if (videoMediaCode == null) {
            return;
        }
        if (!bRecording)
            return;

        int inputBufferIndex = videoMediaCode.dequeueInputBuffer(5000);
        if (inputBufferIndex >= 0) {//当输入缓冲区有效时,就是>=0
            pts_ = (pts * (1000000 / fps));
            pts++;
            ByteBuffer inputBuffer = videoMediaCode.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(data);//往输入缓冲区写入数据,
            ////五个参数，第一个是输入缓冲区的索引，第二个数据是输入缓冲区起始索引，第三个是放入的数据大小，第四个是时间戳，保证递增就是
            videoMediaCode.queueInputBuffer(inputBufferIndex, 0, data.length, pts_, 0);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = videoMediaCode.dequeueOutputBuffer(bufferInfo, 10000);//拿到输出缓冲区的索引  10ms
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = videoMediaCode.getOutputFormat();
//                ByteBuffer csd0 =  newFormat.getByteBuffer("csd-0");
//                ByteBuffer csd1 =  newFormat.getByteBuffer("csd-1");
                //vIndex = mediaMuxer.addTrack(newFormat);
                Addtrack(newFormat, true);
            }
            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = videoMediaCode.getOutputBuffer(outputBufferIndex);
                {
//                    if(vIndex<0)
//                    {
//                        MediaFormat newFormat = videoMediaCode.getOutputFormat();
//                        Addtrack(newFormat,true);
//                    }
                    if (vIndex >= 0) {
                        try {
                            writeSampleData(true, outputBuffer, bufferInfo);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                videoMediaCode.releaseOutputBuffer(outputBufferIndex, false);
                //bufferInfo = new MediaCodec.BufferInfo();
                // outputBufferIndex = videoMediaCode.dequeueOutputBuffer(bufferInfo, 10000);//拿到输出缓冲区的索引  10ms
            }
        }
    }

    private static void AudioDataEncoder(byte[] data) {
        if (audioMediaCode == null) {
            return;
        }
        if (!bRecording)
            return;
        int inputBufferIndex = audioMediaCode.dequeueInputBuffer(5000);
        if (inputBufferIndex >= 0) {//当输入缓冲区有效时,就是>=0
            pts_a_ = (pts_a * nCt);
            pts_a++;
            ByteBuffer inputBuffer = audioMediaCode.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(data);//往输入缓冲区写入数据,
            ////五个参数，第一个是输入缓冲区的索引，第二个数据是输入缓冲区起始索引，第三个是放入的数据大小，第四个是时间戳，保证递增就是
            audioMediaCode.queueInputBuffer(inputBufferIndex, 0, data.length, pts_a_, 0);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = audioMediaCode.dequeueOutputBuffer(bufferInfo, 10000);//拿到输出缓冲区的索引  10ms
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = audioMediaCode.getOutputFormat();
                Addtrack(newFormat, false);
            }
            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = audioMediaCode.getOutputBuffer(outputBufferIndex);
                {
//                    if(aIndex<0)
//                    {
//                        MediaFormat newFormat = audioMediaCode.getOutputFormat();
//                        Addtrack(newFormat,false);
//                    }
                    if (aIndex >= 0) {
                        try {
                            writeSampleData(false, outputBuffer, bufferInfo);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                audioMediaCode.releaseOutputBuffer(outputBufferIndex, false);
                bufferInfo = new MediaCodec.BufferInfo();
                // outputBufferIndex = audioMediaCode.dequeueOutputBuffer(bufferInfo, 10000);//拿到输出缓冲区的索引  10ms
            }
        }
    }

    private static native void naStopRecordV(int nType);

    public static int StopRecordAll() {
        return StopRecord(wifiCamera.TYPE_BOTH_PHONE_SD);
    }

    public static int StopRecord(int nType) {
        if (nType == wifiCamera.TYPE_BOTH_PHONE_SD || nType == wifiCamera.TYPE_ONLY_PHONE) {
            bRecording = false;
            Log.e(TAG, "Count = " + nCountFrame);

            naStopRecordV(nType);
            try {
                mediaMuxer.stop();

                mediaMuxer.release();
            } catch (Exception e) {
                ;
            }
            if(worker!=null)
            {
                worker.isRunning = false;
                try {
                    worker.join(100);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                worker=null;
            }
            mediaMuxer = null;
            bCanStartWrite = false;
            if (bGAudio) {
                try {
                    audioRecord.stop();
                    audioRecord.release();
                } catch (Exception ignored) {
                    ;
                }
                try {
                    audioMediaCode.stop();
                    audioMediaCode.release();
                } catch (Exception ignored) {
                    ;
                }

            }
            try {
                videoMediaCode.stop();
                videoMediaCode.release();
            } catch (Exception ignored) {
                ;
            }


            videoMediaCode = null;
            audioMediaCode = null;
            audioRecord = null;
            mediaMuxer = null;
            bMuxerStart = false;
            aIndex = -1;
            vIndex = -1;
        }
        return 0;
    }
}
