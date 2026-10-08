package com.dauren.monitor.feature.chartmonitor.domain.repository

import com.dauren.monitor.core.common.model.PointBatch
import com.dauren.monitor.core.common.model.Points
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * - [sources] — list rendering (status + latest point).
 * - [batches] — streamed straight to the chart; never into MVI state.
 * - [history] — snapshot used after rotation to re-populate chart series in one call.
 */
interface SignalRepository {
    val sources: StateFlow<List<SourceState>>
    val batches: SharedFlow<PointBatch>
    fun history(id: String): Points
}
