package com.dauren.monitor.core.chart

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * @property fifoCapacity max points per SciChart series — independent of the repository history buffer.
 */
data class ChartConfig(
    val defaultWindow: Duration = DEFAULT_WINDOW,
    val yPadding: Double = DEFAULT_Y_PADDING,
    val lineThicknessPx: Float = DEFAULT_LINE_THICKNESS_PX,
    val fifoCapacity: Int = DEFAULT_FIFO_CAPACITY,
) {
    init {
        require(defaultWindow.isPositive()) { "defaultWindow must be positive, was $defaultWindow" }
        require(yPadding >= 0.0) { "yPadding must be >= 0, was $yPadding" }
        require(lineThicknessPx > 0f) { "lineThicknessPx must be positive, was $lineThicknessPx" }
        require(fifoCapacity > 0) { "fifoCapacity must be positive, was $fifoCapacity" }
    }

    companion object {
        val DEFAULT_WINDOW: Duration = 2.minutes
        const val DEFAULT_Y_PADDING = 0.1
        const val DEFAULT_LINE_THICKNESS_PX = 3f
        const val DEFAULT_FIFO_CAPACITY = 500_000
    }
}
