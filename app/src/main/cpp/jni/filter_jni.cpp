/**
 * filter_jni.cpp
 * 
 * JNI bindings untuk Filter Engine
 * 
 * Exposes native filter methods ke Java layer untuk use di Android UI
 */

#include <jni.h>
#include "filter_engine.h"
#include "bitmap.h"
#include "thread_pool.h"
#include <android/log.h>
#include <memory>

#define LOG_TAG "FilterJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ============================================================================
// JNI Utility Functions
// ============================================================================

/**
 * Convert Java Status to int
 */
static int statusToJni(Status status) {
    return static_cast<int>(status);
}

/**
 * Get native FilterEngine instance dari Java object
 * 
 * Java layer maintains FilterEngine pointer sebagai long (nativePtr)
 */
static FilterEngine* getFilterEngine(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    jfieldID fieldPtr = env->GetFieldID(clazz, "nativePtr", "J");
    jlong ptr = env->GetLongField(obj, fieldPtr);
    env->DeleteLocalRef(clazz);
    return reinterpret_cast<FilterEngine*>(ptr);
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
        FilterEngine* engine = new FilterEngine();
        if (threadCount > 0) {
            engine->setThreadCount(threadCount);
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
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        // TODO: Implement Bitmap marshalling (get Bitmap pointers from Java objects)
        // For now, return OK placeholder
        LOGD("applyBlur JNI binding ready (bitmap marshalling needed)");
        return statusToJni(Status::OK);
        
    } catch (const std::exception& e) {
        LOGE("applyBlur failed: %s", e.what());
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
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        FilterEngine::ColorAdjustParams params;
        params.brightness = brightness;
        params.contrast = contrast;
        params.saturation = saturation;
        params.hue = hue;
        
        // TODO: Implement Bitmap marshalling
        LOGD("applyColorAdjust JNI binding ready (bitmap marshalling needed)");
        return statusToJni(Status::OK);
        
    } catch (const std::exception& e) {
        LOGE("applyColorAdjust failed: %s", e.what());
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
            LOGE("FilterEngine pointer is null");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        FilterEngine::EmbossParams params;
        params.amount = amount;
        params.angle = angle;
        
        // TODO: Implement Bitmap marshalling
        LOGD("applyEmboss JNI binding ready (bitmap marshalling needed)");
        return statusToJni(Status::OK);
        
    } catch (const std::exception& e) {
        LOGE("applyEmboss failed: %s", e.what());
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
            LOGE("FilterEngine pointer is null");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        // TODO: Implement Bitmap marshalling
        LOGD("applyGrayscale JNI binding ready (bitmap marshalling needed)");
        return statusToJni(Status::OK);
        
    } catch (const std::exception& e) {
        LOGE("applyGrayscale failed: %s", e.what());
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
            LOGE("FilterEngine pointer is null");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        // TODO: Implement Bitmap marshalling
        LOGD("applyInvert JNI binding ready (bitmap marshalling needed)");
        return statusToJni(Status::OK);
        
    } catch (const std::exception& e) {
        LOGE("applyInvert failed: %s", e.what());
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
            LOGE("FilterEngine pointer is null");
            return statusToJni(Status::ERROR_INVALID_PARAM);
        }
        
        // TODO: Implement Bitmap marshalling
        LOGD("applySepia JNI binding ready (bitmap marshalling needed)");
        return statusToJni(Status::OK);
        
    } catch (const std::exception& e) {
        LOGE("applySepia failed: %s", e.what());
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
