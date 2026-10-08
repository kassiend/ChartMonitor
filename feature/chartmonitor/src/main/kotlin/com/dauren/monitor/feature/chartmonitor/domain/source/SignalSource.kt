package com.dauren.monitor.feature.chartmonitor.domain.source

import com.dauren.monitor.core.common.model.GeneratorInfo
import com.dauren.monitor.feature.chartmonitor.domain.model.SignalPoint
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * [points] is a cold Flow collected by the repository exactly once; it completes when the source
 * transitions to [SourceStatus.Finished]. Implementations pick their own dispatcher via `flowOn`.
 */
interface SignalSource {
    val info: GeneratorInfo
    val status: StateFlow<SourceStatus>
    fun points(): Flow<SignalPoint>
}

fun interface SignalSourceFactory {
    fun create(): List<SignalSource>
}
