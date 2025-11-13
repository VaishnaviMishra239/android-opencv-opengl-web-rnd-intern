package com.example.edgedetector;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.edgedetector.gl.MyGLSurfaceView;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int REQ_CAMERA = 1234;

    static {
        System.loadLibrary("native-lib");
    }

    private Camera2Helper camera2Helper;
    private MyGLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FrameLayout root = findViewById(R.id.rootContainer);

        glSurfaceView = new MyGLSurfaceView(this);
        root.addView(glSurfaceView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        camera2Helper = new Camera2Helper(this, new Camera2Helper.FrameListener() {
            @Override
            public void onFrame(byte[] data, int width, int height) {
                // Pass to native C++ - returns processed bytes (grayscale / edges)
                byte[] processed = NativeBridge.processFrame(data, width, height, ImageFormatConverter.INPUT_NV21);
                if (processed != null) {
                    // Hand off to GLES renderer as a luminance texture
                    glSurfaceView.queueFrame(processed, width, height);
                }
            }
        });
        camera2Helper.startCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glSurfaceView != null) glSurfaceView.onPause();
        if (camera2Helper != null) camera2Helper.stopCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] results) {
        if (requestCode == REQ_CAMERA) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Log.e(TAG, "Camera permission denied");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, perms, results);
        }
    }
}
