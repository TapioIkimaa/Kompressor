package com.ensody.kompressor.zstd

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.core.createCleaner

public actual fun ZstdCompressor(compressionLevel: Int, dictionary: ByteArray?): SliceTransform =
    ZstdCompressorImpl(compressionLevel = compressionLevel, dictionary = dictionary)

public actual fun trainZstdDictionary(samples: List<ByteArray>, dictSize: Int): ByteArray {
    val totalSize = samples.sumOf { it.size }
    val samplesBuffer = ByteArray(totalSize)
    val sampleSizes = IntArray(samples.size)
    var offset = 0
    for ((index, sample) in samples.withIndex()) {
        sample.copyInto(samplesBuffer, destinationOffset = offset)
        sampleSizes[index] = sample.size
        offset += sample.size
    }
    val dictBuffer = ByteArray(dictSize)
    val result = ZstdWrapper.trainDictionary(samplesBuffer, sampleSizes, dictBuffer)
    checkErrorResult(result)
    return dictBuffer.copyOf(result.toInt())
}

internal class ZstdCompressorImpl(
    private val compressionLevel: Int = 3,
    private val dictionary: ByteArray? = null,
) : SliceTransform {
    private val cctx: Long = ZstdWrapper.createCompressor().also {
        check(it != 0L) { "Failed allocating zstd cctx" }
    }

    val cleaner = createCleaner(cctx, ZstdWrapper::freeCompressor)

    init {
        ZstdWrapper.setParameter(cctx, ZstdParameter.compressionLevel.toInt(), compressionLevel)
        dictionary?.let {
            checkErrorResult(ZstdWrapper.loadCompressorDictionary(cctx, it))
        }
    }

    override fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) {
        val result = ZstdWrapper.compressStream(
            cctx = cctx,
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
        output.insufficient = input.hasData || (finish && result != 0L)
    }
}

internal fun checkErrorResult(result: Long) {
    if (result != 0L) {
        ZstdWrapper.getErrorName(result)?.let {
            error("Bad zstd result code $result: $it")
        }
    }
}
