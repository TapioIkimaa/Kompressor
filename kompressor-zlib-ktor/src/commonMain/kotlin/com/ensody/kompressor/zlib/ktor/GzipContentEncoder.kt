package com.ensody.kompressor.zlib.ktor

import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.ktor.BaseSliceTransformContentEncoder
import com.ensody.kompressor.zlib.ZlibCompressor
import com.ensody.kompressor.zlib.ZlibDecompressor
import com.ensody.kompressor.zlib.ZlibFormat
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

public class GzipContentEncoder(
    public val compressionLevel: Int = 6,
    dispatcher: CoroutineContext = Dispatchers.Default,
) : BaseSliceTransformContentEncoder(dispatcher) {
    override val name: String = "gzip"

    override suspend fun compressor(): SliceTransform =
        ZlibCompressor(ZlibFormat.Gzip, compressionLevel = compressionLevel)

    override suspend fun decompressor(): SliceTransform =
        ZlibDecompressor(ZlibFormat.Gzip)
}
