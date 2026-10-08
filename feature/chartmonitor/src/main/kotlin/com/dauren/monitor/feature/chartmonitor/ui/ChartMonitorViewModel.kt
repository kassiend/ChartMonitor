package com.dauren.monitor.feature.chartmonitor.ui

import com.dauren.monitor.core.common.coroutines.ChartMonitorDispatchers
import com.dauren.monitor.core.common.coroutines.Dispatcher
import com.dauren.monitor.core.common.model.PointBatch
import com.dauren.monitor.core.common.model.Points
import com.dauren.monitor.core.common.time.MonotonicClock
import com.dauren.monitor.core.ui.mvi.BaseEffectViewModel
import com.dauren.monitor.core.ui.mvi.BaseUIEvent
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceState
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceStatus
import com.dauren.monitor.feature.chartmonitor.domain.repository.SignalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ChartMonitorViewModel @Inject constructor(
    private val repository: SignalRepository,
    private val clock: MonotonicClock,
    @Dispatcher(ChartMonitorDispatchers.Default) private val dispatcher: CoroutineDispatcher,
) : BaseEffectViewModel<ChartMonitorState, ChartMonitorUIEffect>() {

    private val hiddenIds: MutableStateFlow<PersistentSet<String>> = MutableStateFlow(persistentSetOf())

    // Points bypass MVI state (N1) — streamed straight into the chart.
    val chartBatches: SharedFlow<PointBatch> = repository.batches

    override fun getStartUIState() = ChartMonitorState()

    init {
        launchWithoutCatch {
            combine(repository.sources, secondTicker(clock), hiddenIds) { sources, now, hidden ->
                buildState(sources, now, hidden, value.followLive)
            }
                .distinctUntilChanged()
                .flowOn(dispatcher)
                .collect { onUIState(it) }
        }
    }

    fun history(id: String): Points = repository.history(id)

    override fun onUIEvent(uiEvent: BaseUIEvent) {
        super.onUIEvent(uiEvent)
        when (uiEvent) {
            is ChartMonitorUIEvent.ToggleVisibility -> reduce(uiEvent)
            is ChartMonitorUIEvent.SetFollowLive -> reduce(uiEvent)
            ChartMonitorUIEvent.ChartNavigatedByUser -> reduceChartNavigated()
            else -> Unit
        }
    }

    private fun reduce(event: ChartMonitorUIEvent.ToggleVisibility) {
        val sources = repository.sources.value
        val isFinished = sources.any { it.info.id == event.id && it.status is SourceStatus.Finished }
        if (isFinished) return
        hiddenIds.update { if (event.id in it) it.remove(event.id) else it.add(event.id) }
    }

    private fun reduce(event: ChartMonitorUIEvent.SetFollowLive) {
        updateUIState { it.copy(followLive = event.enabled) }
    }

    private fun reduceChartNavigated() {
        updateUIState { it.copy(followLive = false) }
    }

    private fun buildState(
        sources: List<SourceState>,
        nowMs: Long,
        hidden: Set<String>,
        followLive: Boolean,
    ): ChartMonitorState {
        val ordered = sources.sortedWith(
            compareBy({ it.status is SourceStatus.Finished }, { it.remainingMs(nowMs) }),
        )
        val visible = sources.asSequence()
            .filter { it.status is SourceStatus.Active && it.info.id !in hidden }
            .map { it.info.id }
            .toPersistentSet()
        return ChartMonitorState(
            items = ordered.map { it.toItem(nowMs, isChecked = it.info.id in visible) }
                .toPersistentList(),
            sources = sources.map(SourceState::info).toPersistentList(),
            visibleIds = visible,
            followLive = followLive,
        )
    }

    private fun SourceState.toItem(nowMs: Long, isChecked: Boolean) = GeneratorItemUi(
        id = info.id,
        name = info.name,
        colorArgb = info.colorArgb,
        timerText = formatRemaining(remainingMs(nowMs)),
        valueText = latest?.value?.let(::formatValue) ?: EMPTY_VALUE,
        isFinished = status is SourceStatus.Finished,
        isChecked = isChecked,
    )

    private companion object {
        const val EMPTY_VALUE = "—"
    }
}

internal fun SourceState.remainingMs(nowMs: Long): Long = when (val s = status) {
    is SourceStatus.Active -> (s.deadlineMs - nowMs).coerceAtLeast(0)
    SourceStatus.Finished -> 0
}

internal fun formatRemaining(remainingMs: Long): String {
    val totalSec = remainingMs.coerceAtLeast(0) / MS_IN_SEC
    val minutes = totalSec / SEC_IN_MIN
    val seconds = totalSec % SEC_IN_MIN
    return buildString(TIMER_LEN) {
        if (minutes < TWO_DIGIT_THRESHOLD) append('0')
        append(minutes)
        append(':')
        if (seconds < TWO_DIGIT_THRESHOLD) append('0')
        append(seconds)
    }
}

internal fun formatValue(value: Double): String = String.format(Locale.US, "%+.2f", value)

internal fun secondTicker(
    clock: MonotonicClock,
    periodMs: Long = TICK_PERIOD_MS,
): Flow<Long> = flow {
    while (true) {
        val now = clock.nowMs()
        emit(now)
        delay(periodMs - now % periodMs)
    }
}

private const val MS_IN_SEC = 1_000L
private const val SEC_IN_MIN = 60L
private const val TIMER_LEN = 5
private const val TWO_DIGIT_THRESHOLD = 10L
private const val TICK_PERIOD_MS = 1_000L
