package com.joyhonest.joycamera.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class JoyCameraView extends GLSurfaceView implements GLSurfaceView.Renderer {
    public JoyCameraView(Context context_) {
        super(context_);
        init(context_);
    }

    /**
     * Standard View constructor. In order to render something, you
     * must call {@link #setRenderer} to register a renderer.
     */
    public JoyCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context)
    {
        setEGLContextClientVersion(2);
        setRenderer(this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        wifiCamera.eglinit();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        wifiCamera.eglchangeLayout(width,height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        wifiCamera.egldrawFrame();

    }
}
