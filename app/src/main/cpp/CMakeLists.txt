# CMakeLists.txt - FlyerPix C++ Native Engine
# Build configuration untuk Rendering, Bitmap, dan Filter Engine

cmake_minimum_required(VERSION 3.18.1)
project(FlyerPixEngine)

# ============================================================================
# C++ Standard & Compiler Flags
# ============================================================================

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC -fvisibility=hidden -Wall -Wextra")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")

# Enable SIMD optimizations for better filter performance
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv7-a -mfpu=neon")
elseif(ANDROID_ABI STREQUAL "x86")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=i686 -msse4.2")
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=x86-64 -msse4.2")
endif()

# ============================================================================
# Include Directories
# ============================================================================

include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/core
    ${CMAKE_CURRENT_SOURCE_DIR}/render
    ${CMAKE_CURRENT_SOURCE_DIR}/bitmap
    ${CMAKE_CURRENT_SOURCE_DIR}/filter
    ${ANDROID_NDK}/sources/android/native_app_glue
)

# ============================================================================
# Source Files - Core Module (Document & Layer Model)
# ============================================================================

set(CORE_SOURCES
    core/document.cpp
    core/layer.cpp
    core/bitmap_data.cpp
    core/thread_pool.cpp
)

# ============================================================================
# Source Files - Rendering Engine
# ============================================================================

set(RENDER_SOURCES
    render/render_engine.cpp
    render/dirty_region.cpp
    render/blend_modes.cpp
)

# ============================================================================
# Source Files - Bitmap Engine
# ============================================================================

set(BITMAP_SOURCES
    bitmap/bitmap_engine.cpp
    bitmap/image_loader.cpp
    bitmap/image_scaler.cpp
    bitmap/memory_pool.cpp
)

# ============================================================================
# Source Files - Filter Engine
# ============================================================================

set(FILTER_SOURCES
    filter/filter_engine.cpp
    filter/blur_filter.cpp
    filter/color_filter.cpp
    filter/emboss_filter.cpp
    filter/filter_utils.cpp
    filter/filter_optimization.cpp
)

# ============================================================================
# Source Files - JNI Bindings
# ============================================================================

set(JNI_SOURCES
    jni/flyerpix_engine_jni.cpp
    jni/document_jni.cpp
    jni/render_jni.cpp
    jni/bitmap_jni.cpp
    jni/filter_jni.cpp
)

# ============================================================================
# Create Main Shared Library
# ============================================================================

add_library(
    flyerpix_engine
    SHARED
    ${CORE_SOURCES}
    ${RENDER_SOURCES}
    ${BITMAP_SOURCES}
    ${FILTER_SOURCES}
    ${JNI_SOURCES}
)

# ============================================================================
# Link Libraries
# ============================================================================

find_library(log-lib log)

target_link_libraries(
    flyerpix_engine
    ${log-lib}
    android
    jnigraphics
)

# ============================================================================
# Compiler Warnings & Optimizations
# ============================================================================

if(MSVC)
    target_compile_options(flyerpix_engine PRIVATE /W4)
else()
    target_compile_options(flyerpix_engine PRIVATE -Wall -Wextra -Wpedantic)
endif()

# ============================================================================
# Build Info
# ============================================================================

message(STATUS "FlyerPix C++ Engine - Build Configuration:")
message(STATUS "  Architecture: ${ANDROID_ABI}")
message(STATUS "  C++ Standard: ${CMAKE_CXX_STANDARD}")
message(STATUS "  Build Type: ${CMAKE_BUILD_TYPE}")
message(STATUS "  SIMD Flags: ${CMAKE_CXX_FLAGS}")


# ============================================================================
# Unit Tests (Optional - for development only)
# ============================================================================

# Note: Tests are compile untuk desktop testing, tidak untuk Android device
# To run: cmake -DBUILD_TESTS=ON && make && ./test_render_engine

if(BUILD_TESTS)
    set(TEST_SOURCES
        test/test_render_engine.cpp
        ${CORE_SOURCES}
        ${RENDER_SOURCES}
        ${BITMAP_SOURCES}
    )
    
    add_executable(test_render_engine ${TEST_SOURCES})
    
    target_compile_options(test_render_engine PRIVATE -Wall -Wextra -g)
    
    message(STATUS "Unit tests enabled - compile dengan: cmake -DBUILD_TESTS=ON")
endif()

# Benchmark executable (for performance testing on desktop)
if(BUILD_TESTS)
    set(BENCHMARK_SOURCES
        test/benchmark_render_engine.cpp
        ${CORE_SOURCES}
        ${RENDER_SOURCES}
        ${BITMAP_SOURCES}
    )
    
    add_executable(benchmark_render_engine ${BENCHMARK_SOURCES})
    
    target_compile_options(benchmark_render_engine PRIVATE -Wall -Wextra -O3)
    
    message(STATUS "Benchmarks enabled - run dengan: ./benchmark_render_engine")
endif()

# Filter Engine Tests
if(BUILD_TESTS)
    set(FILTER_TEST_SOURCES
        test/test_filter_engine.cpp
        ${CORE_SOURCES}
        ${RENDER_SOURCES}
        ${BITMAP_SOURCES}
        ${FILTER_SOURCES}
    )
    
    add_executable(test_filter_engine ${FILTER_TEST_SOURCES})
    
    target_compile_options(test_filter_engine PRIVATE -Wall -Wextra -g)
    
    message(STATUS "Filter engine tests enabled - run dengan: ./test_filter_engine")
endif()

# Filter Engine Benchmark
if(BUILD_TESTS)
    set(FILTER_BENCHMARK_SOURCES
        test/benchmark_filter_engine.cpp
        ${CORE_SOURCES}
        ${RENDER_SOURCES}
        ${BITMAP_SOURCES}
        ${FILTER_SOURCES}
    )
    
    add_executable(benchmark_filter_engine ${FILTER_BENCHMARK_SOURCES})
    
    target_compile_options(benchmark_filter_engine PRIVATE -Wall -Wextra -O3)
    
    message(STATUS "Filter engine benchmark enabled - run dengan: ./benchmark_filter_engine")
endif()
