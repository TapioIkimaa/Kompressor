package com.ensody.kompressor.zstd.ktor

import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.ktor.BaseSliceTransformContentEncoder
import com.ensody.kompressor.zstd.ZstdCompressor
import com.ensody.kompressor.zstd.ZstdDecompressor
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

public class ZstdContentEncoder(
    public val compressionLevel: Int = 1,
    dispatcher: CoroutineContext = Dispatchers.Default,
) : BaseSliceTransformContentEncoder(dispatcher) {
    override val name: String = "zstd"

    override suspend fun compressor(): SliceTransform =
        ZstdCompressor(compressionLevel = compressionLevel)

    override suspend fun decompressor(): SliceTransform =
        ZstdDecompressor()
}
