package com.dauren.monitor.feature.chartmonitor.fake

import com.dauren.monitor.core.common.model.GeneratorInfo
import com.dauren.monitor.core.common.model.PointBatch
import com.dauren.monitor.core.common.model.Points
import com.dauren.monitor.feature.chartmonitor.domain.model.SignalPoint
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceState
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceStatus
import com.dauren.monitor.feature.chartmonitor.domain.repository.SignalRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory [SignalRepository] driven by test code: push whole [SourceState] snapshots or individual
 * [PointBatch]es, flip sources to Finished with [finish]. History is a plain mutable map.
 */
class FakeSignalRepository(initial: List<SourceState> = emptyList()) : SignalRepository {

    private val _sources = MutableStateFlow(initial)
    override val sources: StateFlow<List<SourceState>> = _sources.asStateFlow()

    private val _batches = MutableSharedFlow<PointBatch>(extraBufferCapacity = 64)
    override val batches: SharedFlow<PointBatch> = _batches.asSharedFlow()

    private val histories: MutableMap<String, Points> = mutableMapOf()

    constructor(vararg initial: SourceState) : this(initial.toList())

    override fun history(id: String): Points = histories[id] ?: Points.Empty

    fun setHistory(id: String, series: Points) {
        histories[id] = series
    }

    fun setLatest(id: String, point: SignalPoint) {
        _sources.update { current ->
            current.map { if (it.info.id == id) it.copy(latest = point) else it }
        }
    }

    fun finish(id: String) {
        _sources.update { current ->
            current.map { if (it.info.id == id) it.copy(status = SourceStatus.Finished) else it }
        }
    }

    fun replaceSources(newSources: List<SourceState>) {
        _sources.value = newSources
    }

    fun addSource(info: GeneratorInfo, status: SourceStatus = SourceStatus.Active(Long.MAX_VALUE)) {
        _sources.update { it + SourceState(info, status, latest = null) }
    }

    suspend fun emit(batch: PointBatch) {
        _batches.emit(batch)
    }
}
