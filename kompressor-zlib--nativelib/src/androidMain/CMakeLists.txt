cmake_minimum_required(VERSION 3.4.1)

project(zlib-jni)

set(NATIVEBUILDS_CMAKE_DIR "${PROJECT_SOURCE_DIR}/../../build/nativebuilds-cmake")
include("${NATIVEBUILDS_CMAKE_DIR}/zlib-libz-android.cmake")

file(GLOB_RECURSE SOURCES
        "../jvmCommonMain/jni/*.cpp"
        "../../../jni/common/src/*.cpp"
)
add_library(zlib-jni SHARED ${SOURCES})
include_directories(zlib-jni "../../../jni/common/include")

target_link_libraries(zlib-jni zlib-libz)
