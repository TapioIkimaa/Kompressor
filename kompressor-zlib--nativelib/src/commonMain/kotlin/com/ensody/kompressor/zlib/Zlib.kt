package com.ensody.kompressor.zlib

import com.ensody.kompressor.core.AsyncSliceTransform

/**
 * The format for compression/decompression. This is actually a helper which adjusts windowBits.
 *
 * With [Gzip] and [Raw] the windowBits parameter must be in the range 8..15.
 * With [Zlib] and [AutoDetectZlibGzip] the windowBits parameter must be in the range 9..15.
 */
public enum class ZlibFormat {
    /** Uses zlib/deflate format. */
    Zlib,

    /** Uses gzip format. */
    Gzip,

    /** Raw data without zlib/gzip header and trailer and without computed check value. */
    Raw,

    /** Takes the raw windowBits parameter. */
    UnmodifiedWindowBits,

    /** Auto-detects [Zlib] and [Gzip] format. Only valid for decompression. */
    AutoDetectZlibGzip,
    ;

    internal fun adjustWindowBitsForCompression(windowBits: Int): Int =
        when (this) {
            Zlib, Gzip, Raw, UnmodifiedWindowBits -> adjustWindowBitsForDecompression(windowBits)
            AutoDetectZlibGzip -> error("Compression can't be used with auto-detection")
        }

    internal fun adjustWindowBitsForDecompression(windowBits: Int): Int =
        when (this) {
            Zlib -> {
                check(windowBits in 9..15) { "windowBits must be between 9..15" }
                windowBits
            }

            Gzip -> {
                check(windowBits in 8..15) { "windowBits must be between 8..15" }
                windowBits + 16
            }

            Raw -> {
                check(windowBits in 8..15) { "windowBits must be between 8..15" }
                -windowBits
            }

            UnmodifiedWindowBits -> {
                windowBits
            }

            AutoDetectZlibGzip -> {
                check(windowBits in 9..15) { "windowBits must be between 8..15" }
                windowBits + 32
            }
        }
}

/** Creates a zlib compression transformation. */
public expect fun AsyncZlibCompressor(
    format: ZlibFormat,
    compressionLevel: Int = -1,
    windowBits: Int = 15,
    memLevel: Int = 8,
): AsyncSliceTransform

/** Creates a zlib decompression transformation. */
public expect fun AsyncZlibDecompressor(
    format: ZlibFormat = ZlibFormat.AutoDetectZlibGzip,
    windowBits: Int = 15,
): AsyncSliceTransform
