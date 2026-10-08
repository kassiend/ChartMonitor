package com.dauren.monitor.core.chart

import com.dauren.monitor.core.common.model.Points
import com.scichart.core.model.DateValues
import com.scichart.core.model.DoubleValues

/**
 * Strict `> afterMs` slice. Timestamps are chronologically ordered — O(log n) binary search.
 * Used to drop points already drawn after restore before the live stream resumes.
 */
internal fun Points.after(afterMs: Long): Points {
    if (size == 0) return Points.Empty
    val raw = timestampsMs.binarySearch(afterMs)
    val from = if (raw >= 0) raw + 1 else -raw - 1
    return when {
        from >= size -> Points.Empty
        from == 0 -> this
        else -> Points(
            timestampsMs.copyOfRange(from, size),
            values.copyOfRange(from, size),
        )
    }
}

internal fun Points.toDateValues(): DateValues = DateValues(timestampsMs)

internal fun Points.toDoubleValues(): DoubleValues = DoubleValues(values)
