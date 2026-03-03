package com.ensody.kompressor.zstd

/** Trains a zstd dictionary from a list of [samples]. */
public expect fun trainZstdDictionary(samples: List<ByteArray>, dictSize: Int): ByteArray
