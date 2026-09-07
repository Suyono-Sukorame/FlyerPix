/**
 * jni_bridge.h
 * 
 * JNI utilities dan helper functions untuk Java/Kotlin integration
 */

#ifndef FLYERPIX_JNI_BRIDGE_H
#define FLYERPIX_JNI_BRIDGE_H

#include <jni.h>
#include <memory>

// Forward declarations
class Document;
class Bitmap;

/**
 * Global JNI objects holder (must be initialized once at JNI_OnLoad)
 */
class JNIBridge {
public:
    static void initialize(JNIEnv* env);
    static void cleanup();
    
    // Get cached JNI classes
    static jclass getDocumentClass() { return document_class_; }
    static jclass getBitmapClass() { return bitmap_class_; }
    
private:
    static jclass document_class_;
    static jclass bitmap_class_;
};

/**
 * Exception handling utilities
 */
class JNIException {
public:
    // Throw Java exception
    static void throwException(JNIEnv* env, const char* message);
    
    // Throw RuntimeException
    static void throwRuntimeException(JNIEnv* env, const char* message);
    
    // Check if exception occurred
    static bool hasException(JNIEnv* env);
    
    // Clear pending exception
    static void clearException(JNIEnv* env);
};

/**
 * Object conversion utilities
 */
class JNIConverter {
public:
    // Convert Java int array to native vector
    static std::vector<int> intArrayToVector(JNIEnv* env, jintArray arr);
    
    // Convert Java float array to native vector
    static std::vector<float> floatArrayToVector(JNIEnv* env, jfloatArray arr);
    
    // Convert native vector to Java int array
    static jintArray vectorToIntArray(JNIEnv* env, const std::vector<int>& vec);
    
    // Convert native vector to Java float array
    static jfloatArray vectorToFloatArray(JNIEnv* env, const std::vector<float>& vec);
};

/**
 * Native pointer management (wrap C++ objects in long pointers)
 */
template<typename T>
class NativePointer {
public:
    // Store pointer as jlong
    static jlong toJNI(T* ptr) {
        return reinterpret_cast<jlong>(ptr);
    }
    
    // Retrieve pointer from jlong
    static T* fromJNI(jlong handle) {
        return reinterpret_cast<T*>(handle);
    }
    
    // Create and store new object
    static jlong createAndStore(T* ptr) {
        return toJNI(new T(*ptr));
    }
    
    // Safely delete if not null
    static void deleteIfValid(jlong handle) {
        if (handle != 0) {
            delete fromJNI(handle);
        }
    }
};

#endif // FLYERPIX_JNI_BRIDGE_H
