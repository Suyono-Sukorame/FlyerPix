// jni_bridge.cpp - JNI bridge untuk fungsi native FlyerPix.
// Ekspos blurPixels ke Kotlin: memblur int[] RGBA8888 in-place.

#include <jni.h>
#include <cstring>
#include <vector>
#include "fp_blur.h"
#include "fp_color.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_com_flyerpix_editor_nativepix_FpNative_blurPixels(
        JNIEnv* env, jobject thiz, jintArray pixels,
        jint width, jint height, jint radius) {
    if (pixels == nullptr || width <= 0 || height <= 0 || radius <= 0) return;

    jsize len = env->GetArrayLength(pixels);
    if (len < static_cast<jsize>(width) * height) return;

    std::vector<int32_t> buf(static_cast<size_t>(len));
    env->GetIntArrayRegion(pixels, 0, len, reinterpret_cast<jint*>(buf.data()));
    if (env->ExceptionCheck()) return;

    int32_t stride = width * 4;
    fp::gaussianBlurRGBA(reinterpret_cast<uint8_t*>(buf.data()), width, height,
                         stride, radius);

    env->SetIntArrayRegion(pixels, 0, len, reinterpret_cast<const jint*>(buf.data()));
}

JNIEXPORT void JNICALL
Java_com_flyerpix_editor_nativepix_FpNative_applyColorMatrix(
        JNIEnv* env, jobject thiz, jintArray pixels,
        jfloat brightness, jfloat contrast, jfloat saturation) {
    if (pixels == nullptr) return;

    jsize len = env->GetArrayLength(pixels);
    if (len <= 0) return;

    std::vector<int32_t> buf(static_cast<size_t>(len));
    env->GetIntArrayRegion(pixels, 0, len, reinterpret_cast<jint*>(buf.data()));
    if (env->ExceptionCheck()) return;

    fp::applyColorMatrix(buf.data(), len, brightness, contrast, saturation);

    env->SetIntArrayRegion(pixels, 0, len, reinterpret_cast<const jint*>(buf.data()));
}

#ifdef __cplusplus
}
#endif