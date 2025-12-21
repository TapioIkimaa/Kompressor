package com.ensody.kompressor.zlib

import com.ensody.kompressor.core.AsyncSliceTransform
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.core.toAsync

/** Creates a zlib compression transformation. */
public expect fun ZlibCompressor(
    format: ZlibFormat,
    compressionLevel: Int = -1,
    windowBits: Int = 15,
    memLevel: Int = 8,
): SliceTransform

/** Creates a zlib decompression transformation. */
public expect fun ZlibDecompressor(
    format: ZlibFormat = ZlibFormat.AutoDetectZlibGzip,
    windowBits: Int = 15,
): SliceTransform

public actual fun AsyncZlibCompressor(
    format: ZlibFormat,
    compressionLevel: Int,
    windowBits: Int,
    memLevel: Int,
): AsyncSliceTransform =
    ZlibCompressor(
        format = format,
        compressionLevel = compressionLevel,
        windowBits = windowBits,
        memLevel = memLevel,
    ).toAsync()

public actual fun AsyncZlibDecompressor(
    format: ZlibFormat,
    windowBits: Int,
): AsyncSliceTransform =
    ZlibDecompressor(
        format = format,
        windowBits = windowBits,
    ).toAsync()
