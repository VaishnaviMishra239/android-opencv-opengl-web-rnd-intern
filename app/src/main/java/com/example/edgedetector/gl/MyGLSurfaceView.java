package com.example.edgedetector.gl;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class MyGLSurfaceView extends GLSurfaceView {
    private final GLRenderer renderer;

    public MyGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        renderer = new GLRenderer();
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    // Queue processed frame to renderer
    public void queueFrame(byte[] grayOrEdgeBytes, int width, int height) {
        renderer.updateFrame(grayOrEdgeBytes, width, height);
    }
}
