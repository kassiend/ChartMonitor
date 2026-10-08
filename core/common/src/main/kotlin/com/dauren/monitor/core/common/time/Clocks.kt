package com.dauren.monitor.core.common.time

/** Monotonic clock for deadlines and ticks. Immune to system clock changes. */
fun interface MonotonicClock {
    fun nowMs(): Long
}

/** Wall-clock epoch milliseconds — for X-axis timestamps the user expects to recognise. */
fun interface WallClock {
    fun nowEpochMs(): Long
}

object SystemMonotonicClock : MonotonicClock {
    private const val NANOS_PER_MS = 1_000_000L
    override fun nowMs(): Long = System.nanoTime() / NANOS_PER_MS
}

object SystemWallClock : WallClock {
    override fun nowEpochMs(): Long = System.currentTimeMillis()
}
