package com.ensody.kompressor.zlib

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.free
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toLong
import kotlinx.cinterop.usePinned
import platform.zlib.Z_DEFLATED
import platform.zlib.Z_FINISH
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.deflateEnd
import platform.zlib.inflate
import platform.zlib.inflateInit2
import platform.zlib.z_stream
import kotlin.native.ref.createCleaner

public actual fun ZlibDecompressor(
    format: ZlibFormat,
    windowBits: Int,
): SliceTransform =
    ZlibDecompressorImpl(windowBits = format.adjustWindowBitsForDecompression(windowBits))

@OptIn(UnsafeNumber::class)
internal class ZlibDecompressorImpl(windowBits: Int) : SliceTransform {
    private val stream: z_stream = nativeHeap.alloc<z_stream>()
    private val initResult = inflateInit2(stream.ptr, windowBits)

    val cleaner = createCleaner(stream) {
        deflateEnd(it.ptr)
        nativeHeap.free(it)
    }

    init {
        check(initResult == Z_OK) { "Failed allocating zlib stream" }
    }

    override fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) = memScoped {
        input.data.usePinned { pinnedInput ->
            output.data.usePinned { pinnedOutput ->
                stream.next_in = if (input.remainingRead == 0) {
                    null
                } else {
                    pinnedInput.addressOf(input.readStart).reinterpret()
                }
                stream.avail_in = input.remainingRead.convert()
                stream.next_out = if (output.remainingWrite == 0) {
                    null
                } else {
                    pinnedOutput.addressOf(output.writeStart).reinterpret()
                }
                stream.avail_out = output.remainingWrite.convert()
                val result = inflate(stream.ptr, if (finish) Z_FINISH else Z_NO_FLUSH)
                input.readStart += input.remainingRead - stream.avail_in.toInt()
                output.writeStart += output.remainingWrite - stream.avail_out.toInt()
                checkErrorResult(result)
                val isOkOrBufError = result == Z_OK || result == platform.zlib.Z_BUF_ERROR
                input.insufficient = isOkOrBufError && !input.hasData
                output.insufficient = isOkOrBufError && output.isFull
                if (finish && result != Z_STREAM_END && !output.insufficient) {
                    error("Zlib stream truncated")
                }
            }
        }
    }
}
