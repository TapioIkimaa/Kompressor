package com.ensody.kompressor.ktor

import com.ensody.kompressor.core.SliceTransform
import com.ensody.kompressor.kotlinx.io.pipe
import io.ktor.util.ContentEncoder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.asSink
import io.ktor.utils.io.asSource
import io.ktor.utils.io.reader
import io.ktor.utils.io.writer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.io.buffered
import kotlin.coroutines.CoroutineContext

public abstract class BaseSliceTransformContentEncoder(
    private val dispatcher: CoroutineContext = Dispatchers.Default,
) : ContentEncoder {
    public abstract suspend fun compressor(): SliceTransform
    public abstract suspend fun decompressor(): SliceTransform

    override fun decode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext,
    ): ByteReadChannel = CoroutineScope(coroutineContext).writer(dispatcher) {
        source.asSource().pipe(decompressor()).use { source ->
            channel.asSink().use { sink ->
                source.buffered().transferTo(sink)
            }
        }
    }.channel

    override fun encode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext,
    ): ByteReadChannel = CoroutineScope(coroutineContext).writer(dispatcher) {
        source.asSource().pipe(compressor()).use { source ->
            channel.asSink().use { sink ->
                source.buffered().transferTo(sink)
            }
        }
    }.channel

    override fun encode(
        source: ByteWriteChannel,
        coroutineContext: CoroutineContext,
    ): ByteWriteChannel = CoroutineScope(coroutineContext).reader(dispatcher) {
        source.asSink().use { sink ->
            channel.asSource().pipe(compressor()).use { source ->
                source.buffered().transferTo(sink)
            }
        }
    }.channel
}
