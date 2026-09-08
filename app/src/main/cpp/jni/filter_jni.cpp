/**
 * filter_jni.cpp
 * 
 * JNI bindings untuk Filter Engine dengan complete bitmap marshalling
 * 
 * Exposes native filter methods ke Java layer untuk use di Android UI
 * 
 * Features:
 * - AndroidBitmap API untuk lock/unlock pixel buffers
 * - Automatic Bitmap wrapping dengan C++ Bitmap::wrap()
 * - Error handling dengan JNI exceptions
 * - Thread-safe FilterEngine access
 */

#include <jni.h>
#include "filter_engine.h"
#include "filter_optimization.h"
#include "bitmap.h"
#include "thread_pool.h"
#include <android/bitmap.h>
#include <android/log.h>
#include <memory>
#include <cstring>

#define LOG_TAG "FilterJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// ============================================================================
// JNI Utility Functions
// ============================================================================

/**
 * Convert C++ Status to Java int (mirrors FilterStatus.kt)
 */
static int statusToJni(Status status) {
    return static_cast<int>(status);
}

/**
 * Get native FilterEngine instance dari Java object
 * 
 * Java layer maintains FilterEngine pointer sebagai long (nativePtr field)
 */
static FilterEngine* getFilterEngine(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    jfieldID fieldPtr = env->GetFieldID(clazz, "nativePtr", "J");
    jlong ptr = env->GetLongField(obj, fieldPtr);
    env->DeleteLocalRef(clazz);
    return reinterpret_cast<FilterEngine*>(ptr);
}

/**
 * Throw Java exception dari C++
 */
static void throwJniException(JNIEnv* env, const char* exceptionClass, const char* message) {
    jclass exc = env->FindClass(exceptionClass);
    if (exc) {
        env->ThrowNew(exc, message);
        env->DeleteLocalRef(exc);
    }
}

/**
 * Lock Android Bitmap dan retrieve pixel buffer info
 */
struct BitmapLock {
    JNIEnv* env;
    jobject bitmap;
    AndroidBitmapInfo info;
    void* pixels;
    bool locked;
    
    BitmapLock(JNIEnv* e, jobject b) : env(e), bitmap(b), pixels(nullptr), locked(false) {
        int ret = AndroidBitmap_getInfo(env, bitmap, &info);
        if (ret < 0) {
            LOGE("AndroidBitmap_getInfo failed: %d", ret);
            return;
        }
        
        // Validate format
        if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
            LOGE("Unsupported bitmap format: %d (require RGBA_8888)", info.format);
            return;
        }
        
        ret = AndroidBitmap_lockPixels(env, bitmap, &pixels);
        if (ret < 0) {
            LOGE("AndroidBitmap_lockPixels failed: %d", ret);
            pixels = nullptr;
            return;
        }
        
        locked = true;
        LOGD("Bitmap locked: %dx%d, stride=%d", info.width, info.height, info.stride);
    }
    
    ~BitmapLock() {
        if (locked && pixels) {
            AndroidBitmap_unlockPixels(env, bitmap);
            locked = false;
            LOGD("Bitmap unlocked");
        }
    }
    
    bool isValid() const {
        return locked && pixels != nullptr;
    }
};

/**
 * Wrap locked Android Bitmap into C++ Bitmap object
 */
static Bitmap wrapAndroidBitmap(const BitmapLock& lock) {
    if (!lock.isValid()) {
        throw std::runtime_error("Cannot wrap invalid bitmap lock");
    }
    
    // Create non-owning Bitmap wrapper
    return Bitmap::wrap(
        lock.info.width,
        lock.info.height,
        lock.info.stride,
        static_cast<uint8_t*>(lock.pixels),
        PixelFormat::ARGB_8888
    );
}

// ============================================================================
// FilterEngine Lifecycle (Constructor/Destructor)
// ============================================================================

/**
 * native static long nativeCreate(int threadCount)
 */
extern "C" JNIEXPORT jlong JNICALL
Java_com_flyerpix_editor_filter_FilterEngine_nativeCreate(
    JNIEnv* env, 
    jclass clazz, 
    jint threadCount) {
    
    LOGD("Creating FilterEngine with %d threads", threadCount);
    
    try {
        // Initialize runtime optimizer (auto-detects device characteristics)
        OptimizationProfiler& optimizer = OptimizationProfiler::getInstance();
        optimizer.initialize();
        
        FilterEngine* engine = new FilterEngine();
        
        if (threadCount > 0) {
            engine->setThreadCount(threadCount);
        } else {
            // Auto-detect optimal thread count from device profile
            int optimal = optimizer.getProfile().optimal_thread_count;
            LOGI("Auto-detected optimal thread count: %d", optimal);
            engine->setThreadCount(optimal);
        }
        return reinterpret_cast<jlong>(engine);
    } catch (const std::exception& e) {
        LOGE("Failed to create FilterEngine: %s", e.what());
        return 0;
    }
}

/**
 * native void nativeDestroy()
 */
extern "C" JNIEXPORT void JNICALL
Java_com_flyerpix_editor_filter_FilterEngine_nativeDestroy(
    JNIEnv* env, 
    jobject obj) {
    
    FilterEngine* engine = getFilterEngine(env, obj);
    if (engine) {
        LOGD("Destroying FilterEngine");
        delete engine;
    }
}

// ============================================================================
// Gaussian Blur Filter
// ============================================================================

/**
 * native int nativeApplyBlur(Bitmap src, Bitmap dst, float radius, int passes)
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_flyerpix_editor_filter_FilterEngine_nativeApplyBlur(
    JNIEnv* env,
    jobject obj,
    jobject jSrc,
    jobject jDst,
    jfloat radius,
    jint passes) {
    
    LOGD("JNI: applyBlur(radius=%.1f, passes=%d)", radius, passes);
    
    try {
        FilterEngine* engine = getFilterEngine(env, obj);
        if (!engine) {
            LOGE("FilterEngine pointer is null");
            throwJniException(env, "java/lang/NullPointerException", "FilterEngine not initialized");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        // Lock source bitmap
        BitmapLock srcLock(env, jSrc);
        if (!srcLock.isValid()) {
            LOGE("Failed to lock source bitmap");
            throwJniException(env, "java/lang/IllegalArgumentException", "Invalid source bitmap");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        // Lock destination bitmap
        BitmapLock dstLock(env, jDst);
        if (!dstLock.isValid()) {
            LOGE("Failed to lock destination bitmap");
            throwJniException(env, "java/lang/IllegalArgumentException", "Invalid destination bitmap");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        // Validate dimensions match
        if (srcLock.info.width != dstLock.info.width || srcLock.info.height != dstLock.info.height) {
            LOGE("Bitmap dimensions mismatch: src=%dx%d, dst=%dx%d",
                 srcLock.info.width, srcLock.info.height,
                 dstLock.info.width, dstLock.info.height);
            throwJniException(env, "java/lang/IllegalArgumentException", 
                            "Source and destination bitmaps must have same dimensions");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        // Wrap Android Bitmaps into C++ Bitmap objects
        Bitmap srcBitmap = wrapAndroidBitmap(srcLock);
        Bitmap dstBitmap = wrapAndroidBitmap(dstLock);
        
        // Create blur parameters
        FilterEngine::BlurParams params;
        params.radius = radius;
        params.passes = passes;
        
        // Apply blur filter
        Status status = engine->applyBlur(srcBitmap, dstBitmap, params);
        
        LOGD("Blur completed with status: %d", static_cast<int>(status));
        return statusToJni(status);
        
    } catch (const std::exception& e) {
        LOGE("applyBlur exception: %s", e.what());
        throwJniException(env, "java/lang/RuntimeException", e.what());
        return statusToJni(Status::ERROR_RENDERING_FAILED);
    }
}

// ============================================================================
// Color Adjust Filter
// ============================================================================

/**
 * native int nativeApplyColorAdjust(Bitmap src, Bitmap dst, 
 *                                   float brightness, float contrast, 
 *                                   float saturation, float hue)
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_flyerpix_editor_filter_FilterEngine_nativeApplyColorAdjust(
    JNIEnv* env,
    jobject obj,
    jobject jSrc,
    jobject jDst,
    jfloat brightness,
    jfloat contrast,
    jfloat saturation,
    jfloat hue) {
    
    LOGD("JNI: applyColorAdjust(B=%.2f, C=%.2f, S=%.2f, H=%.1f)", 
         brightness, contrast, saturation, hue);
    
    try {
        FilterEngine* engine = getFilterEngine(env, obj);
        if (!engine) {
            LOGE("FilterEngine pointer is null");
            throwJniException(env, "java/lang/NullPointerException", "FilterEngine not initialized");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        // Lock bitmaps
        BitmapLock srcLock(env, jSrc);
        if (!srcLock.isValid()) {
            throwJniException(env, "java/lang/IllegalArgumentException", "Invalid source bitmap");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        BitmapLock dstLock(env, jDst);
        if (!dstLock.isValid()) {
            throwJniException(env, "java/lang/IllegalArgumentException", "Invalid destination bitmap");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        // Validate dimensions
        if (srcLock.info.width != dstLock.info.width || srcLock.info.height != dstLock.info.height) {
            throwJniException(env, "java/lang/IllegalArgumentException", 
                            "Bitmap dimensions mismatch");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        // Wrap bitmaps
        Bitmap srcBitmap = wrapAndroidBitmap(srcLock);
        Bitmap dstBitmap = wrapAndroidBitmap(dstLock);
        
        // Create color adjust parameters
        FilterEngine::ColorAdjustParams params;
        params.brightness = brightness;
        params.contrast = contrast;
        params.saturation = saturation;
        params.hue = hue;
        
        // Apply filter
        Status status = engine->applyColorAdjust(srcBitmap, dstBitmap, params);
        
        LOGD("Color adjust completed with status: %d", static_cast<int>(status));
        return statusToJni(status);
        
    } catch (const std::exception& e) {
        LOGE("applyColorAdjust exception: %s", e.what());
        throwJniException(env, "java/lang/RuntimeException", e.what());
        return statusToJni(Status::ERROR_RENDERING_FAILED);
    }
}

// ============================================================================
// Emboss Filter
// ============================================================================

/**
 * native int nativeApplyEmboss(Bitmap src, Bitmap dst, float amount, float angle)
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_flyerpix_editor_filter_FilterEngine_nativeApplyEmboss(
    JNIEnv* env,
    jobject obj,
    jobject jSrc,
    jobject jDst,
    jfloat amount,
    jfloat angle) {
    
    LOGD("JNI: applyEmboss(amount=%.2f, angle=%.1f)", amount, angle);
    
    try {
        FilterEngine* engine = getFilterEngine(env, obj);
        if (!engine) {
            throwJniException(env, "java/lang/NullPointerException", "FilterEngine not initialized");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        BitmapLock srcLock(env, jSrc);
        if (!srcLock.isValid()) {
            throwJniException(env, "java/lang/IllegalArgumentException", "Invalid source bitmap");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        BitmapLock dstLock(env, jDst);
        if (!dstLock.isValid()) {
            throwJniException(env, "java/lang/IllegalArgumentException", "Invalid destination bitmap");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        if (srcLock.info.width != dstLock.info.width || srcLock.info.height != dstLock.info.height) {
            throwJniException(env, "java/lang/IllegalArgumentException", "Bitmap dimensions mismatch");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        Bitmap srcBitmap = wrapAndroidBitmap(srcLock);
        Bitmap dstBitmap = wrapAndroidBitmap(dstLock);
        
        FilterEngine::EmbossParams params;
        params.amount = amount;
        params.angle = angle;
        
        Status status = engine->applyEmboss(srcBitmap, dstBitmap, params);
        
        LOGD("Emboss completed with status: %d", static_cast<int>(status));
        return statusToJni(status);
        
    } catch (const std::exception& e) {
        LOGE("applyEmboss exception: %s", e.what());
        throwJniException(env, "java/lang/RuntimeException", e.what());
        return statusToJni(Status::ERROR_RENDERING_FAILED);
    }
}

// ============================================================================
// Simple Filters
// ============================================================================

/**
 * native int nativeApplyGrayscale(Bitmap src, Bitmap dst)
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_flyerpix_editor_filter_FilterEngine_nativeApplyGrayscale(
    JNIEnv* env,
    jobject obj,
    jobject jSrc,
    jobject jDst) {
    
    LOGD("JNI: applyGrayscale()");
    
    try {
        FilterEngine* engine = getFilterEngine(env, obj);
        if (!engine) {
            throwJniException(env, "java/lang/NullPointerException", "FilterEngine not initialized");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        BitmapLock srcLock(env, jSrc);
        if (!srcLock.isValid()) {
            throwJniException(env, "java/lang/IllegalArgumentException", "Invalid source bitmap");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        BitmapLock dstLock(env, jDst);
        if (!dstLock.isValid()) {
            throwJniException(env, "java/lang/IllegalArgumentException", "Invalid destination bitmap");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        if (srcLock.info.width != dstLock.info.width || srcLock.info.height != dstLock.info.height) {
            throwJniException(env, "java/lang/IllegalArgumentException", "Bitmap dimensions mismatch");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        Bitmap srcBitmap = wrapAndroidBitmap(srcLock);
        Bitmap dstBitmap = wrapAndroidBitmap(dstLock);
        
        Status status = engine->applyGrayscale(srcBitmap, dstBitmap);
        
        LOGD("Grayscale completed with status: %d", static_cast<int>(status));
        return statusToJni(status);
        
    } catch (const std::exception& e) {
        LOGE("applyGrayscale exception: %s", e.what());
        throwJniException(env, "java/lang/RuntimeException", e.what());
        return statusToJni(Status::ERROR_RENDERING_FAILED);
    }
}

/**
 * native int nativeApplyInvert(Bitmap src, Bitmap dst)
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_flyerpix_editor_filter_FilterEngine_nativeApplyInvert(
    JNIEnv* env,
    jobject obj,
    jobject jSrc,
    jobject jDst) {
    
    LOGD("JNI: applyInvert()");
    
    try {
        FilterEngine* engine = getFilterEngine(env, obj);
        if (!engine) {
            throwJniException(env, "java/lang/NullPointerException", "FilterEngine not initialized");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        BitmapLock srcLock(env, jSrc);
        if (!srcLock.isValid()) {
            throwJniException(env, "java/lang/IllegalArgumentException", "Invalid source bitmap");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        BitmapLock dstLock(env, jDst);
        if (!dstLock.isValid()) {
            throwJniException(env, "java/lang/IllegalArgumentException", "Invalid destination bitmap");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        if (srcLock.info.width != dstLock.info.width || srcLock.info.height != dstLock.info.height) {
            throwJniException(env, "java/lang/IllegalArgumentException", "Bitmap dimensions mismatch");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        Bitmap srcBitmap = wrapAndroidBitmap(srcLock);
        Bitmap dstBitmap = wrapAndroidBitmap(dstLock);
        
        Status status = engine->applyInvert(srcBitmap, dstBitmap);
        
        LOGD("Invert completed with status: %d", static_cast<int>(status));
        return statusToJni(status);
        
    } catch (const std::exception& e) {
        LOGE("applyInvert exception: %s", e.what());
        throwJniException(env, "java/lang/RuntimeException", e.what());
        return statusToJni(Status::ERROR_RENDERING_FAILED);
    }
}

/**
 * native int nativeApplySepia(Bitmap src, Bitmap dst, float intensity)
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_flyerpix_editor_filter_FilterEngine_nativeApplySepia(
    JNIEnv* env,
    jobject obj,
    jobject jSrc,
    jobject jDst,
    jfloat intensity) {
    
    LOGD("JNI: applySepia(intensity=%.2f)", intensity);
    
    try {
        FilterEngine* engine = getFilterEngine(env, obj);
        if (!engine) {
            throwJniException(env, "java/lang/NullPointerException", "FilterEngine not initialized");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        BitmapLock srcLock(env, jSrc);
        if (!srcLock.isValid()) {
            throwJniException(env, "java/lang/IllegalArgumentException", "Invalid source bitmap");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        BitmapLock dstLock(env, jDst);
        if (!dstLock.isValid()) {
            throwJniException(env, "java/lang/IllegalArgumentException", "Invalid destination bitmap");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        if (srcLock.info.width != dstLock.info.width || srcLock.info.height != dstLock.info.height) {
            throwJniException(env, "java/lang/IllegalArgumentException", "Bitmap dimensions mismatch");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        Bitmap srcBitmap = wrapAndroidBitmap(srcLock);
        Bitmap dstBitmap = wrapAndroidBitmap(dstLock);
        
        Status status = engine->applySepia(srcBitmap, dstBitmap, intensity);
        
        LOGD("Sepia completed with status: %d", static_cast<int>(status));
        return statusToJni(status);
        
    } catch (const std::exception& e) {
        LOGE("applySepia exception: %s", e.what());
        throwJniException(env, "java/lang/RuntimeException", e.what());
        return statusToJni(Status::ERROR_RENDERING_FAILED);
    }
}

// ============================================================================
// Performance Control
// ============================================================================

/**
 * native void nativeSetThreadCount(int count)
 */
extern "C" JNIEXPORT void JNICALL
Java_com_flyerpix_editor_filter_FilterEngine_nativeSetThreadCount(
    JNIEnv* env,
    jobject obj,
    jint count) {
    
    FilterEngine* engine = getFilterEngine(env, obj);
    if (engine) {
        LOGD("Setting thread count to %d", count);
        engine->setThreadCount(count);
    }
}

/**
 * native void nativeSetSIMDEnabled(boolean enabled)
 */
extern "C" JNIEXPORT void JNICALL
Java_com_flyerpix_editor_filter_FilterEngine_nativeSetSIMDEnabled(
    JNIEnv* env,
    jobject obj,
    jboolean enabled) {
    
    FilterEngine* engine = getFilterEngine(env, obj);
    if (engine) {
        LOGD("Setting SIMD %s", enabled ? "enabled" : "disabled");
        engine->setSIMDEnabled(enabled);
    }
}

/**
 * native int nativeGetThreadCount()
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_flyerpix_editor_filter_FilterEngine_nativeGetThreadCount(
    JNIEnv* env,
    jobject obj) {
    
    FilterEngine* engine = getFilterEngine(env, obj);
    if (engine) {
        return engine->getThreadCount();
    }
    return 0;
}

/**
 * native boolean nativeIsSIMDEnabled()
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_flyerpix_editor_filter_FilterEngine_nativeIsSIMDEnabled(
    JNIEnv* env,
    jobject obj) {
    
    FilterEngine* engine = getFilterEngine(env, obj);
    if (engine) {
        return engine->isSIMDEnabled() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

/**
 * native String nativeGetOptimizationReport()
 *
 * Runs runtime auto-tuning (device detection, cache/bin cache benchmark)
 * dan return optimization profile sebagai String untuk logging/reporting.
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_flyerpix_editor_filter_FilterEngine_nativeGetOptimizationReport(
    JNIEnv* env,
    jobject obj) {
    
    try {
        OptimizationProfiler& optimizer = OptimizationProfiler::getInstance();
        optimizer.autoTune();
        return env->NewStringUTF(optimizer.generateReport());
    } catch (const std::exception& e) {
        LOGE("Optimization profiling failed: %s", e.what());
        return env->NewStringUTF("Optimization profiling failed");
    }
}
