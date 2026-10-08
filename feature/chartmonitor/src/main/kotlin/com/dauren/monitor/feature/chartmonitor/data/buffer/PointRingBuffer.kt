package com.dauren.monitor.feature.chartmonitor.data.buffer

import com.dauren.monitor.core.common.model.Points

/**
 * O(1) primitive append, memory capped at [capacity]. Backing arrays grow lazily from
 * [initialCapacity] up to [capacity], then wrap as a ring — 10 × 500k doesn't allocate 80 MB upfront.
 * One writer + atomic-snapshot readers; synchronization on the monitor is cheaper than locks here.
 */
class PointRingBuffer(
    private val capacity: Int,
    initialCapacity: Int = DEFAULT_INITIAL_CAPACITY,
) {
    init {
        require(capacity > 0) { "capacity must be positive, was $capacity" }
        require(initialCapacity > 0) { "initialCapacity must be positive, was $initialCapacity" }
    }

    private var timestamps: LongArray = LongArray(minOf(initialCapacity, capacity))
    private var values: DoubleArray = DoubleArray(timestamps.size)
    private var head: Int = 0
    private var size: Int = 0

    val currentSize: Int
        @Synchronized get() = size

    @Synchronized
    fun append(timestampMs: Long, value: Double) {
        if (size == timestamps.size && size < capacity) grow()
        timestamps[head] = timestampMs
        values[head] = value
        head = (head + 1) % timestamps.size
        if (size < timestamps.size) size++
    }

    @Synchronized
    fun snapshot(): Points {
        if (size == 0) return Points.Empty
        val storage = timestamps.size
        val start = (head - size + storage) % storage
        val firstPart = minOf(size, storage - start)
        val ts = LongArray(size)
        val vs = DoubleArray(size)
        timestamps.copyInto(ts, 0, start, start + firstPart)
        values.copyInto(vs, 0, start, start + firstPart)
        if (firstPart < size) {
            timestamps.copyInto(ts, firstPart, 0, size - firstPart)
            values.copyInto(vs, firstPart, 0, size - firstPart)
        }
        return Points(ts, vs)
    }

    // Pre-capacity the data lives contiguously in [0, size), so copyOf + head=size works.
    private fun grow() {
        val newSize = minOf(timestamps.size * 2, capacity)
        timestamps = timestamps.copyOf(newSize)
        values = values.copyOf(newSize)
        head = size
    }

    companion object {
        private const val DEFAULT_INITIAL_CAPACITY = 1_024
    }
}
