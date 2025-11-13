package com.example.edgedetector;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;
import android.util.Size;
import android.hardware.camera2.*;
import android.os.Handler;
import android.os.HandlerThread;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Camera2Helper {
    public interface FrameListener {
        void onFrame(byte[] data, int width, int height);
    }

    private final Activity activity;
    private final FrameListener listener;
    private final String TAG = "Camera2Helper";

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    public Camera2Helper(Activity activity, FrameListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    @SuppressLint("MissingPermission")
    public void startCamera() {
        startBackgroundThread();

        CameraManager manager = (CameraManager) activity.getSystemService(Activity.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);
            Size[] sizes = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.YUV_420_888);
            Size chosen = new Size(640, 480);
            if (sizes != null && sizes.length > 0) chosen = sizes[0];

            imageReader = ImageReader.newInstance(chosen.getWidth(), chosen.getHeight(),
                    ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image == null) return;
                    int width = image.getWidth();
                    int height = image.getHeight();
                    byte[] nv21 = yuv420888ToNV21(image);
                    listener.onFrame(nv21, width, height);
                } catch (Exception e) {
                    Log.e(TAG, "Image callback", e);
                } finally {
                    if (image != null) image.close();
                }
            }, backgroundHandler);

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    try {
                        CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        builder.addTarget(imageReader.getSurface());
                        cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                captureSession = session;
                                try {
                                    builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                    captureSession.setRepeatingRequest(builder.build(), null, backgroundHandler);
                                } catch (Exception e) {
                                    Log.e(TAG, "setRepeatingRequest", e);
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                                Log.e(TAG, "configure failed");
                            }
                        }, backgroundHandler);
                    } catch (Exception e) {
                        Log.e(TAG, "openCamera onOpened", e);
                    }
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                }
            }, backgroundHandler);

        } catch (Exception e) {
            Log.e(TAG, "startCamera", e);
        }
    }

    public void stopCamera() {
        try {
            if (captureSession != null) captureSession.close();
            if (cameraDevice != null) cameraDevice.close();
            if (imageReader != null) imageReader.close();
            stopBackgroundThread();
        } catch (Exception ignored) {}
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("cam2-bg");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException ignored) {}
        }
    }

    // Convert YUV_420_888 -> NV21
    private byte[] yuv420888ToNV21(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuf = planes[0].getBuffer();
        ByteBuffer uBuf = planes[1].getBuffer();
        ByteBuffer vBuf = planes[2].getBuffer();

        int ySize = yBuf.remaining();
        int uSize = uBuf.remaining();
        int vSize = vBuf.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuf.get(nv21, 0, ySize);

        byte[] u = new byte[uSize];
        byte[] v = new byte[vSize];
        uBuf.get(u);
        vBuf.get(v);

        int pos = ySize;
        for (int i = 0; i < vSize; i++) {
            nv21[pos++] = v[i];
            if (i < uSize) nv21[pos++] = u[i];
        }
        return nv21;
    }
}
