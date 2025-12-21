package com.ensody.kompressor.zlib

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.core.createCleaner

public actual fun ZlibDecompressor(
    format: ZlibFormat,
    windowBits: Int,
): SliceTransform =
    ZlibDecompressorImpl(windowBits = format.adjustWindowBitsForDecompression(windowBits))

internal class ZlibDecompressorImpl(windowBits: Int) : SliceTransform {
    private val stream: Long = ZlibWrapper.createDecompressor(windowBits).also {
        check(it != 0L) { "Failed allocating zlib stream" }
    }

    val cleaner = createCleaner(stream, ZlibWrapper::freeDecompressor)

    override fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) {
        val result = ZlibWrapper.decompressStream(
            stream = stream,
            input = input,
            inputByteArray = input.data,
            inputStart = input.readStart,
            inputEndExclusive = input.writeStart,
            output = output,
            outputByteArray = output.data,
            outputStart = output.writeStart,
            outputEndExclusive = output.writeLimit,
            finish = finish,
        )
        checkErrorResult(result)
        output.insufficient = input.hasData || (finish && result != ZlibResult.Z_STREAM_END.value)
    }
}
