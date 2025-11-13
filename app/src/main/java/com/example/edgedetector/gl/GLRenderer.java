package com.example.edgedetector.gl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;

public class GLRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "GLRenderer";

    private int textureId = -1;
    private ByteBuffer pixelBuffer;
    private int frameWidth = 0, frameHeight = 0;

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer texBuffer;

    private final String VERTEX_SHADER =
            "attribute vec4 vPosition; \n" +
            "attribute vec2 vTexCoord; \n" +
            "varying vec2 texCoord; \n" +
            "void main() { gl_Position = vPosition; texCoord = vTexCoord; }";

    private final String FRAGMENT_SHADER =
            "precision mediump float; \n" +
            "varying vec2 texCoord; \n" +
            "uniform sampler2D uTexture; \n" +
            "void main() { vec4 c = texture2D(uTexture, texCoord); gl_FragColor = c; }";

    private int program;
    private int positionHandle, texHandle, texSamplerHandle;

    private final float[] VERTICES = {
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
    };

    private final float[] TEX_COORDS = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
    };

    public GLRenderer() {
        vertexBuffer = ByteBuffer.allocateDirect(VERTICES.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(VERTICES).position(0);
        texBuffer = ByteBuffer.allocateDirect(TEX_COORDS.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        texBuffer.put(TEX_COORDS).position(0);
    }

    public synchronized void updateFrame(byte[] gray, int w, int h) {
        if (frameWidth != w || frameHeight != h) {
            frameWidth = w; frameHeight = h;
            pixelBuffer = ByteBuffer.allocateDirect(w * h);
        }
        if (pixelBuffer != null) {
            pixelBuffer.clear();
            pixelBuffer.put(gray, 0, Math.min(gray.length, frameWidth*frameHeight));
            pixelBuffer.position(0);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        texHandle = GLES20.glGetAttribLocation(program, "vTexCoord");
        texSamplerHandle = GLES20.glGetUniformLocation(program, "uTexture");

        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        textureId = tex[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (pixelBuffer == null || frameWidth == 0 || frameHeight == 0) return;

        GLES20.glUseProgram(program);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, frameWidth, frameHeight, 0,
                GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, pixelBuffer);

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(texHandle);
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 0, texBuffer);

        GLES20.glUniform1i(texSamplerHandle, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texHandle);
    }

    private int loadShader(int type, String src) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, src);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String err = GLES20.glGetShaderInfoLog(shader);
            Log.e(TAG, "Error compiling shader: " + err);
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private int createProgram(String vSrc, String fSrc) {
        int v = loadShader(GLES20.GL_VERTEX_SHADER, vSrc);
        int f = loadShader(GLES20.GL_FRAGMENT_SHADER, fSrc);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        GLES20.glLinkProgram(p);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: " + GLES20.glGetProgramInfoLog(p));
            GLES20.glDeleteProgram(p);
            return 0;
        }
        return p;
    }
}
