package com.joyhonest.joycamera.sdk;

public class Worker extends Thread {
    public boolean isRunning = false;
    private static final  String TAG = "Worker";
    @Override
    public void run() {
        int re = 0;
        JoyAudioRecord.pts = 0;
        isRunning = true;
        while (isRunning) {
            re = JoyAudioRecord.audioRecord.read(JoyAudioRecord.mBuffer, 0, JoyAudioRecord.BUFFFER_SIZE);
            JoyLog.e(TAG, "read = " + re + " buffersize = " + JoyAudioRecord.BUFFFER_SIZE);
            JoyAudioRecord.AudioDataEncoder(JoyAudioRecord.mBuffer);
        }
    }
}
