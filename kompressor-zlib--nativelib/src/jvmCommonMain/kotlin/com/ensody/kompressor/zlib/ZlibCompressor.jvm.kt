package com.ensody.kompressor.zlib

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.core.createCleaner

public actual fun ZlibCompressor(
    format: ZlibFormat,
    compressionLevel: Int,
    windowBits: Int,
    memLevel: Int,
): SliceTransform =
    ZlibCompressorImpl(
        compressionLevel = compressionLevel,
        windowBits = format.adjustWindowBitsForCompression(windowBits),
        memLevel = memLevel,
    )

internal class ZlibCompressorImpl(
    private val compressionLevel: Int,
    private val windowBits: Int,
    private val memLevel: Int,
    private val strategy: Int = 0,
) : SliceTransform {
    private val stream: Long = ZlibWrapper.createCompressor(compressionLevel, windowBits, memLevel, strategy).also {
        check(it != 0L) { "Failed allocating zlib stream" }
    }

    val cleaner = createCleaner(stream, ZlibWrapper::freeCompressor)

    override fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) {
        val result = ZlibWrapper.compressStream(
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

internal fun checkErrorResult(result: Int) {
    if (result != ZlibResult.Z_OK.value &&
        result != ZlibResult.Z_STREAM_END.value &&
        result != ZlibResult.Z_BUF_ERROR.value
    ) {
        error("Bad zlib result code $result: ${ZlibResult.entries.find { it.value == result }}")
    }
}
