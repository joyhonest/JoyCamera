package com.joyhonest.joycamera.sdk;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.SystemClock;


import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;


class JoyAudioRecord {
    private static final int KEY_CHANNEL_COUNT = 1;
    private static final int KEY_SAMPLE_RATE = 16000;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_MODE = AudioFormat.CHANNEL_IN_MONO;
    public static int BUFFFER_SIZE = (2048 * KEY_CHANNEL_COUNT);
    private static final int nCt = (BUFFFER_SIZE * 1000000) / (KEY_SAMPLE_RATE * 2 * KEY_CHANNEL_COUNT);
    private static int fps = 0;
    private static final String TAG = "JoyAudioRecord";
    private static boolean bGAudio = false;
    private static boolean bRecording = false;
    private static boolean bMuxerStart = false;
    private static MediaMuxer mediaMuxer = null;
    public static AudioRecord audioRecord = null;
    private static MediaCodec videoMediaCode = null;
    private static MediaCodec audioMediaCode = null;
    public static long pts = 0;
    private static long pts_a = 0;
    private static long pts_ = 0;
    private static long nCountFrame = 0;
    private static long nCountFrameAudio = 0;
    private static int vIndex = -1;
    private static int aIndex = -1;
    public static byte[] mBuffer;
    private static Worker audioWorker = null;
    private static boolean bCanStartWrite = false;

    private static MediaFormat F_GetMediaFormat(int width, int height, int bitrate, int fps_, int color) {
        if (videoMediaCode != null) {
            videoMediaCode.stop();
            videoMediaCode.release();
            videoMediaCode = null;
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
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        //描述视频格式的帧速率（以帧/秒为单位）的键。
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);//帧率，一般在15至30之内，太小容易造成视频卡顿。
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, color);//色彩格式，具体查看相关API，不同设备支持的色彩格式不尽相同
        // 调整码率的控流模式
        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        try {
            videoMediaCode.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            e.printStackTrace();
            mediaFormat = null;
        }
        return mediaFormat;
    }
    static int InitVideo(int width, int height, int bitrate, int fps1) {
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


    static long getRecordTimems() {
        if (!bRecording) {
            return 0;
        }
        if (fps <= 0) {
            return 0;
        }
        return (long) (nCountFrame * (1000.0f / fps));
    }

    static int StartRecord(String pFileName, int nType, int dest, boolean bRecordAudio) {
        if (!Utility.naGetCameraisConnected())
            return -1;
        if (bRecording)
            return -2;
        bCanStartWrite = false;
        if(nType == wifiCamera.TYPE_CONVERT)
        {
            StartRecord_A(pFileName, false);
            Utility.naStartRecordV(nType, dest);
            bRecording = true;
            return 0;
        }
        if (nType == wifiCamera.TYPE_ONLY_PHONE || nType == wifiCamera.TYPE_BOTH_PHONE_SD) {
            StartRecord_A(pFileName, bRecordAudio);
            Utility.naStartRecordV(nType, dest);
            bRecording = true;
        }
        if (nType == wifiCamera.TYPE_ONLY_SD) {
            Utility.naStartRecordV(nType, dest);
        }
        return 0;
    }

    private static void StartRecord_A(String sfliename, boolean bAudio) {
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
            } catch (Exception ignored) {

            }

        }
        try {
            mediaMuxer = new MediaMuxer(sfliename, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            bMuxerStart = false;
        } catch (Exception ignored) {
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
            int nn = 0;
            while (aIndex < 0) {
                SystemClock.sleep(10);
                nn++;
                if (nn > 50)
                    break;
            }
        } else {
            StartRecordAudio(false);
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
            if(wifiCamera.getApplicationContext()!=null)
            {
                if (ActivityCompat.checkSelfPermission(wifiCamera.getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                    audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, KEY_SAMPLE_RATE, CHANNEL_MODE, AUDIO_FORMAT, BUFFFER_SIZE);
                    audioRecord.startRecording();
                    {
                        if (audioWorker != null && audioWorker.isRunning) {
                            audioWorker.isRunning = false;
                            SystemClock.sleep(20);
                        }
                        audioWorker = new Worker();
                        audioWorker.start();
                    }
                }
            }
            else {
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, KEY_SAMPLE_RATE, CHANNEL_MODE, AUDIO_FORMAT, BUFFFER_SIZE);
                audioRecord.startRecording();
                {
                    if (audioWorker != null && audioWorker.isRunning) {
                        audioWorker.isRunning = false;
                        SystemClock.sleep(20);
                    }
                    audioWorker = new Worker();
                    audioWorker.start();
                }
            }
        } else {
            if (audioRecord != null) {
                try {
                    audioRecord.stop();
                    audioRecord.release();
                } catch (Exception ignored) {

                }
                audioRecord = null;
            }

            if (audioWorker != null) {
                audioWorker.isRunning = false;
                SystemClock.sleep(50);
                audioWorker = null;
            }
        }
    }
    private static boolean isCanRecordAudio() {
        boolean re = false;
        try {
            int minBufferSize = AudioRecord.getMinBufferSize(KEY_SAMPLE_RATE, CHANNEL_MODE, AUDIO_FORMAT) * 2;
            if(wifiCamera.getApplicationContext()!=null)
            {
                if (ActivityCompat.checkSelfPermission(wifiCamera.getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
            else {
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, KEY_SAMPLE_RATE, CHANNEL_MODE, AUDIO_FORMAT, minBufferSize);
                audioRecord.startRecording();
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                re = true;
            }
        } catch (Exception ignored) {

        }
        return re;
    }
    private static void writeSampleData(boolean bVideo, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        if (!bMuxerStart)
            return;
        if (bVideo && !bCanStartWrite && (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0)  //第一个I帧
        {
            bCanStartWrite = true;
        }
        if (!bCanStartWrite)
            return;
        if (bVideo) {
            if (vIndex >= 0) {
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0)  //控制 sps pps 已经在 addtrack中format种添加进去了。
                {
                    try {
                        bufferInfo.presentationTimeUs = (nCountFrame*1000000) / fps;
                        mediaMuxer.writeSampleData(vIndex, byteBuf, bufferInfo);
                        nCountFrame++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            if (aIndex >= 0 ) {
                try {
                    if (bufferInfo.size > 10) {
                        bufferInfo.presentationTimeUs = nCountFrameAudio * nCt;
                        mediaMuxer.writeSampleData(aIndex, byteBuf, bufferInfo);
                        nCountFrameAudio++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void Addtrack(MediaFormat format, boolean bVideo) {
        if(mediaMuxer==null)
            return;
        if(format == null)
            return;
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
                            JoyLog.e(TAG, "Start --- vide = " + vIndex + " audio = " + aIndex);
                        }
                    }
                } else {
                    if (!bMuxerStart) {
                        mediaMuxer.start();
                        bMuxerStart = true;
                        bCanStartWrite = false;
                        JoyLog.e(TAG, "Start --- vide = " + vIndex + " audio = " + aIndex);
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
                            JoyLog.e(TAG, "Start --- audio  vide = " + vIndex + " audio = " + aIndex);
                        }
                    }
                }
            }
        }
    }

    public static void VidoeDataEncoder(byte[] data) {
        if (videoMediaCode == null) {
            return;
        }
        if (!bRecording)
            return;
        int inputBufferIndex = videoMediaCode.dequeueInputBuffer(5000);
        if (inputBufferIndex >= 0) {//当输入缓冲区有效时,就是>=0
            if(fps<=0)
            {
                fps = 25;
            }
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
                Addtrack(newFormat, true);
            }
            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = videoMediaCode.getOutputBuffer(outputBufferIndex);
                {
                    if (vIndex >= 0) {
                        try {
                            writeSampleData(true, outputBuffer, bufferInfo);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                videoMediaCode.releaseOutputBuffer(outputBufferIndex, false);
            }
        }
    }

    public static void AudioDataEncoder(byte[] data) {
        if (audioMediaCode == null) {
            return;
        }
        if (!bRecording)
            return;
        int inputBufferIndex = audioMediaCode.dequeueInputBuffer(5000);
        if (inputBufferIndex >= 0) {//当输入缓冲区有效时,就是>=0
            long pts_a_ = (pts_a * nCt);
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
                    if (aIndex >= 0) {
                        try {
                            writeSampleData(false, outputBuffer, bufferInfo);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                audioMediaCode.releaseOutputBuffer(outputBufferIndex, false);
//                bufferInfo = new MediaCodec.BufferInfo();
//                // outputBufferIndex = audioMediaCode.dequeueOutputBuffer(bufferInfo, 10000);//拿到输出缓冲区的索引  10ms
            }
        }
    }

    public static int StopRecord(int nType) {
        if(nType == wifiCamera.TYPE_ONLY_SD)
        {
            Utility.naStopRecordV(wifiCamera.TYPE_ONLY_SD);
        }
        if (nType == wifiCamera.TYPE_BOTH_PHONE_SD || nType == wifiCamera.TYPE_ONLY_PHONE) {
            if(nType == wifiCamera.TYPE_BOTH_PHONE_SD)
            {
                Utility.naStopRecordV(wifiCamera.TYPE_BOTH_PHONE_SD);
            }
            bRecording = false;
            JoyLog.e(TAG, "Count = " + nCountFrame);
            Utility.naStopRecordV(nType);
            try {
                mediaMuxer.stop();
                mediaMuxer.release();
                File oldFile = new File(GP4225_Device.sRecordFileName+"_.tmp");
                File newFile = new File(GP4225_Device.sRecordFileName);
                if(newFile.exists() && newFile.isFile())
                {
                    newFile.delete();
                }
                if (oldFile.exists() && oldFile.isFile()) {
                    oldFile.renameTo(newFile);
//                    Utility.OnSnaporRecrodOK(GP4225_Device.sRecordFileName,1);
                }
                Utility.OnSnaporRecrodOK(GP4225_Device.sRecordFileName,1);
                GP4225_Device.sRecordFileName = null;

            } catch (Exception ignored) {

            }

            mediaMuxer = null;
            bCanStartWrite = false;

            if(audioWorker!=null)
            {
                audioWorker.isRunning = false;
                try {
                    audioWorker.join(100);
                }
                catch (Exception ignored)
                {

                }
                audioWorker=null;
            }

            if (bGAudio) {
                try {
                    audioRecord.stop();
                    audioRecord.release();
                } catch (Exception ignored) {

                }
                try {
                    audioMediaCode.stop();
                    audioMediaCode.release();
                } catch (Exception ignored) {

                }

            }
            try {
                videoMediaCode.stop();
                videoMediaCode.release();
            } catch (Exception ignored) {

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

    /*public static void VidoeDataEncoderA(byte[] data) {
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
            if(bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
            {
//                if(!bGetPPS)
//                {
//                    bGetPPS = true;
//                }
            }

            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = videoMediaCode.getOutputFormat();
                //Addtrack(newFormat, true);
            }
            if (outputBufferIndex >= 0)
            {

                ByteBuffer outputBuffer = videoMediaCode.getOutputBuffer(outputBufferIndex);
                {

                }
                videoMediaCode.releaseOutputBuffer(outputBufferIndex, false);
            }
        }
    }
    */


    public static int  Init_MediaConvert(int width, int height, int bitrate, int fps1)
    {
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
            } catch (Exception ignored) {

            }

        }
        return InitVideo(width, height, bitrate,fps1);

    }


}
