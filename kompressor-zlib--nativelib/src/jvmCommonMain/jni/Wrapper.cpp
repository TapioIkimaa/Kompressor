#include <zlib.h>
#include <jni.h>
#include "DefaultLoad.h"
#include "SliceClass.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_zlib_ZlibWrapper_createCompressor(
        JNIEnv *env,
        jobject type,
        jint level,
        jint windowBits,
        jint memLevel,
        jint strategy
) {
    auto stream = new z_stream();
    auto result = deflateInit2(stream, level, Z_DEFLATED, windowBits, memLevel, strategy);
    if (result != Z_OK) {
        delete stream;
        return 0;
    }
    return reinterpret_cast<jlong>(stream);
}

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_zlib_ZlibWrapper_freeCompressor(
        JNIEnv *env,
        jobject type,
        jlong streamPointer
) {
    auto stream = reinterpret_cast<z_stream *>(streamPointer);
    auto result = deflateEnd(stream);
    delete stream;
    return result;
}

JNIEXPORT jint JNICALL
Java_com_ensody_kompressor_zlib_ZlibWrapper_compressStream(
        JNIEnv *env,
        jobject type,
        jlong streamPointer,
        jobject inputSlice,
        jbyteArray inputByteArray,
        jint inputStart,
        jint inputEndExclusive,
        jobject outputSlice,
        jbyteArray outputByteArray,
        jint outputStart,
        jint outputEndExclusive,
        jboolean finish
) {
    auto stream = reinterpret_cast<z_stream *>(streamPointer);

    auto outputElements = env->GetByteArrayElements(outputByteArray, NULL);
    if (outputElements == NULL) {
        return Z_BUF_ERROR;
    }
    jbyte *inputElements = env->GetByteArrayElements(inputByteArray, NULL);
    if (inputElements == NULL) {
        env->ReleaseByteArrayElements(outputByteArray, outputElements, 0);
        return Z_BUF_ERROR;
    }
    stream->next_in = reinterpret_cast<Bytef *>(inputElements + inputStart);
    auto availIn = inputEndExclusive - inputStart;
    stream->avail_in = availIn;
    stream->next_out = reinterpret_cast<Bytef *>(outputElements + outputStart);
    auto availOut = outputEndExclusive - outputStart;
    stream->avail_out = availOut;

    auto result = deflate(stream, finish ? Z_FINISH : Z_NO_FLUSH);

    env->SetIntField(inputSlice, sliceClass->readStart, static_cast<jint>(stream->next_in - reinterpret_cast<Bytef *>(inputElements)));
    env->SetIntField(outputSlice, sliceClass->writeStart, static_cast<jint>(stream->next_out - reinterpret_cast<Bytef *>(outputElements)));

    env->ReleaseByteArrayElements(inputByteArray, inputElements, JNI_ABORT);
    env->ReleaseByteArrayElements(outputByteArray, outputElements, 0);

    return result;
}

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_zlib_ZlibWrapper_createDecompressor(
        JNIEnv *env,
        jobject type,
        jint windowBits
) {
    auto stream = new z_stream();
    auto result = inflateInit2(stream, windowBits);
    if (result != Z_OK) {
        delete stream;
        return 0;
    }
    return reinterpret_cast<jlong>(stream);
}

JNIEXPORT jlong JNICALL
Java_com_ensody_kompressor_zlib_ZlibWrapper_freeDecompressor(
        JNIEnv *env,
        jobject type,
        jlong streamPointer
) {
    auto stream = reinterpret_cast<z_stream *>(streamPointer);
    auto result = inflateEnd(stream);
    delete stream;
    return result;
}

JNIEXPORT jint JNICALL
Java_com_ensody_kompressor_zlib_ZlibWrapper_decompressStream(
        JNIEnv *env,
        jobject type,
        jlong streamPointer,
        jobject inputSlice,
        jbyteArray inputByteArray,
        jint inputStart,
        jint inputEndExclusive,
        jobject outputSlice,
        jbyteArray outputByteArray,
        jint outputStart,
        jint outputEndExclusive,
        jboolean finish
) {
    auto stream = reinterpret_cast<z_stream *>(streamPointer);

    auto outputElements = env->GetByteArrayElements(outputByteArray, NULL);
    if (outputElements == NULL) {
        return Z_BUF_ERROR;
    }
    jbyte *inputElements = env->GetByteArrayElements(inputByteArray, NULL);
    if (inputElements == NULL) {
        env->ReleaseByteArrayElements(outputByteArray, outputElements, 0);
        return Z_BUF_ERROR;
    }
    stream->next_in = reinterpret_cast<Bytef *>(inputElements + inputStart);
    auto availIn = inputEndExclusive - inputStart;
    stream->avail_in = availIn;
    stream->next_out = reinterpret_cast<Bytef *>(outputElements + outputStart);
    auto availOut = outputEndExclusive - outputStart;
    stream->avail_out = availOut;

    auto result = inflate(stream, finish ? Z_FINISH : Z_NO_FLUSH);

    env->SetIntField(inputSlice, sliceClass->readStart, static_cast<jint>(stream->next_in - reinterpret_cast<Bytef *>(inputElements)));
    env->SetIntField(outputSlice, sliceClass->writeStart, static_cast<jint>(stream->next_out - reinterpret_cast<Bytef *>(outputElements)));

    env->ReleaseByteArrayElements(inputByteArray, inputElements, JNI_ABORT);
    env->ReleaseByteArrayElements(outputByteArray, outputElements, 0);

    return result;
}

#ifdef __cplusplus
}
#endif
