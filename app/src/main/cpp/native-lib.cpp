#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <vector>

#define LOG_TAG "native-lib"
#define ALOG(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

using namespace cv;
using namespace std;

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_edgedetector_NativeBridge_processFrame(JNIEnv *env, jclass clazz,
                                                       jbyteArray data_,
                                                       jint width, jint height,
                                                       jint inputFormat) {
    jbyte *yuv = env->GetByteArrayElements(data_, NULL);
    if (!yuv) return NULL;
    int yuvSize = env->GetArrayLength(data_);

    Mat yuvMat(height + height/2, width, CV_8UC1, (unsigned char*) yuv);
    Mat bgr;
    cvtColor(yuvMat, bgr, COLOR_YUV2BGR_NV21);

    Mat gray;
    cvtColor(bgr, gray, COLOR_BGR2GRAY);

    Mat edges;
    Canny(gray, edges, 80, 160);

    int outSize = width * height;
    jbyteArray outArray = env->NewByteArray(outSize);
    env->SetByteArrayRegion(outArray, 0, outSize, (jbyte*) edges.data);

    env->ReleaseByteArrayElements(data_, yuv, JNI_ABORT);
    return outArray;
}
