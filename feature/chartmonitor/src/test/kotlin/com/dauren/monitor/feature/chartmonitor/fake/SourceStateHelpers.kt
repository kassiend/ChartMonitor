package com.dauren.monitor.feature.chartmonitor.fake

import com.dauren.monitor.core.common.model.GeneratorInfo
import com.dauren.monitor.feature.chartmonitor.domain.model.SignalPoint
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceState
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceStatus

/** Convenience factory for an active [SourceState] with sensible test defaults. */
fun activeSource(
    id: String,
    deadlineMs: Long,
    value: Double = 0.0,
    colorArgb: Int = 0,
    name: String = "Generator $id",
): SourceState = SourceState(
    info = GeneratorInfo(id, name, colorArgb),
    status = SourceStatus.Active(deadlineMs),
    latest = SignalPoint(timestampMs = 0, value = value),
)

fun finishedSource(
    id: String,
    value: Double = 0.0,
    colorArgb: Int = 0,
    name: String = "Generator $id",
): SourceState = SourceState(
    info = GeneratorInfo(id, name, colorArgb),
    status = SourceStatus.Finished,
    latest = SignalPoint(timestampMs = 0, value = value),
)
