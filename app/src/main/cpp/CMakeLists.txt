cmake_minimum_required(VERSION 3.18.1)
project(flyerpix_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(flyerpix_native SHARED
        fp_blur.cpp
        fp_color.cpp
        jni_bridge.cpp)

target_compile_options(flyerpix_native PRIVATE -O2 -ffast-math -funroll-loops)

# Lokasi header JNI dari NDK
find_library(log-lib log)

target_link_libraries(flyerpix_native
        android
        jnigraphics
        ${log-lib})