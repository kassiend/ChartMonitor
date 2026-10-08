package com.dauren.monitor.core.common.model

/**
 * Columnar primitive storage — no boxing. Readonly by contract: do not mutate the arrays, do not
 * retain references past the frame. A defensive copy would double allocation per tick.
 */
class Points(
    val timestampsMs: LongArray,
    val values: DoubleArray,
) {
    init {
        require(timestampsMs.size == values.size) {
            "timestampsMs.size (${timestampsMs.size}) != values.size (${values.size})"
        }
    }

    val size: Int get() = values.size
    val lastTimestampMs: Long? get() = timestampsMs.lastOrNull()

    companion object {
        val Empty = Points(LongArray(0), DoubleArray(0))
    }
}
