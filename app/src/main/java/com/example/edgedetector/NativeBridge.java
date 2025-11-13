package com.example.edgedetector;

public class NativeBridge {
    // Native method signature - implemented in C++
    public static native byte[] processFrame(byte[] yuvData, int width, int height, int inputFormat);
}
