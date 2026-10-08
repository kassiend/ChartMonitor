package com.dauren.monitor.feature.chartmonitor.data.repository

import com.dauren.monitor.core.common.coroutines.AppInitializer
import com.dauren.monitor.core.common.coroutines.ApplicationScope
import com.dauren.monitor.core.common.model.PointBatch
import com.dauren.monitor.core.common.model.Points
import com.dauren.monitor.feature.chartmonitor.data.buffer.PointRingBuffer
import com.dauren.monitor.feature.chartmonitor.domain.model.SignalPoint
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceState
import com.dauren.monitor.feature.chartmonitor.domain.repository.SignalRepository
import com.dauren.monitor.feature.chartmonitor.domain.source.SignalSource
import com.dauren.monitor.feature.chartmonitor.domain.source.SignalSourceFactory
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class RepositoryConfig(
    val historyCapacity: Int = DEFAULT_HISTORY_CAPACITY,
    // Collect simultaneous ticks across sources into one batch — one chart redraw per window.
    val batchWindow: Duration = DEFAULT_BATCH_WINDOW,
) {
    init {
        require(historyCapacity > 0) { "historyCapacity must be positive, was $historyCapacity" }
        require(batchWindow.isPositive()) { "batchWindow must be positive, was $batchWindow" }
    }

    companion object {
        const val DEFAULT_HISTORY_CAPACITY = 500_000
        val DEFAULT_BATCH_WINDOW: Duration = 100.milliseconds
    }
}

/**
 * App-scope singleton. Point collection is deferred to [init] (wired as `AppInitializer`) — the
 * constructor only wires `stateIn(Eagerly)` for [sources], which subscribes to already-materialized
 * `source.status` flows so `.value` is current for late readers and tests.
 */
@Singleton
internal class DefaultSignalRepository @Inject constructor(
    factories: Set<@JvmSuppressWildcards SignalSourceFactory>,
    @ApplicationScope private val scope: CoroutineScope,
    private val config: RepositoryConfig,
) : SignalRepository,
    AppInitializer {

    private val signalSources: List<SignalSource> = factories.flatMap(SignalSourceFactory::create)

    private val buffers: Map<String, PointRingBuffer> =
        signalSources.associate { it.info.id to PointRingBuffer(config.historyCapacity) }

    private val pending = Channel<Pair<String, SignalPoint>>(Channel.UNLIMITED)
    private val latest = MutableStateFlow<Map<String, SignalPoint>>(emptyMap())
    private val _batches = MutableSharedFlow<PointBatch>(extraBufferCapacity = BATCHES_BUFFER)
    private val started = AtomicBoolean(false)

    override val batches: SharedFlow<PointBatch> = _batches.asSharedFlow()

    override val sources: StateFlow<List<SourceState>> =
        combineStatuses()
            .combine(latest) { statuses, latestById ->
                signalSources.mapIndexed { i, source ->
                    SourceState(source.info, statuses[i], latestById[source.info.id])
                }
            }
            .stateIn(
                scope,
                SharingStarted.Eagerly,
                signalSources.map { SourceState(it.info, it.status.value, latest = null) },
            )

    override fun history(id: String): Points =
        buffers[id]?.snapshot() ?: Points.Empty

    override fun init() {
        if (!started.compareAndSet(false, true)) return
        signalSources.forEach { source ->
            val buffer = buffers.getValue(source.info.id)
            scope.launch(CoroutineName("source-${source.info.id}")) {
                source.points().collect { point ->
                    buffer.append(point.timestampMs, point.value)
                    pending.send(source.info.id to point)
                }
            }
        }
        scope.launch(CoroutineName("batcher")) { runBatcher() }
    }

    private suspend fun runBatcher() {
        val accumulator = LinkedHashMap<String, MutableList<SignalPoint>>()
        while (true) {
            accumulator.add(pending.receive())
            delay(config.batchWindow)

            while (true) {
                val next = pending.tryReceive().getOrNull() ?: break
                accumulator.add(next)
            }

            val batch = PointBatch(accumulator.mapValues { (_, points) -> points.toPoints() })
            accumulator.clear()
            latest.update { current -> current + batch.byId.mapValues { (_, s) -> s.last() } }
            _batches.emit(batch)
        }
    }

    private fun combineStatuses() =
        if (signalSources.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(signalSources.map(SignalSource::status)) { it.toList() }
        }

    companion object {
        private const val BATCHES_BUFFER = 64
    }
}

private fun MutableMap<String, MutableList<SignalPoint>>.add(entry: Pair<String, SignalPoint>) {
    getOrPut(entry.first) { ArrayList() } += entry.second
}

private fun List<SignalPoint>.toPoints() =
    Points(
        LongArray(size) { this[it].timestampMs },
        DoubleArray(size) { this[it].value },
    )

private fun Points.last() = SignalPoint(timestampsMs.last(), values.last())
