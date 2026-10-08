package com.dauren.monitor.feature.chartmonitor.data.source

import com.dauren.monitor.core.common.model.GeneratorInfo
import com.dauren.monitor.core.common.time.MonotonicClock
import com.dauren.monitor.core.common.time.WallClock
import com.dauren.monitor.feature.chartmonitor.domain.model.SignalPoint
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceStatus
import com.dauren.monitor.feature.chartmonitor.domain.source.SignalSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.random.Random
import kotlin.time.Duration

/**
 * First point = [initialValue]; next = previous + U[-1, 1]. Ticks anchored to monotonic
 * `start + n·interval` so a briefly starved collector doesn't drift.
 */
internal class RandomWalkSignalSource(
    override val info: GeneratorInfo,
    private val initialValue: Double,
    lifetime: Duration,
    private val interval: Duration,
    private val random: Random,
    private val clock: MonotonicClock,
    wallClock: WallClock,
    private val dispatcher: CoroutineDispatcher,
) : SignalSource {

    init {
        require(initialValue in -1.0..1.0) { "initialValue must be in [-1, 1], was $initialValue" }
        require(lifetime.isPositive()) { "lifetime must be positive, was $lifetime" }
        require(interval.isPositive()) { "interval must be positive, was $interval" }
    }

    private val startedAtMs: Long = clock.nowMs()
    private val startedAtEpochMs: Long = wallClock.nowEpochMs()
    private val deadlineMs: Long = startedAtMs + lifetime.inWholeMilliseconds

    private val _status = MutableStateFlow<SourceStatus>(SourceStatus.Active(deadlineMs))
    override val status: StateFlow<SourceStatus> = _status.asStateFlow()

    override fun points(): Flow<SignalPoint> = flow {
        val intervalMs = interval.inWholeMilliseconds
        var value = initialValue
        var tick = 0L
        while (true) {
            val tickAtMs = startedAtMs + tick * intervalMs
            if (tickAtMs >= deadlineMs) break
            delay((tickAtMs - clock.nowMs()).coerceAtLeast(0))
            emit(SignalPoint(timestampMs = startedAtEpochMs + tick * intervalMs, value = value))
            value += random.nextSignedUnit()
            tick++
        }
        delay((deadlineMs - clock.nowMs()).coerceAtLeast(0))
        _status.value = SourceStatus.Finished
    }.flowOn(dispatcher)
}

// nextDouble upper bound is exclusive; nextUp bumps it so 1.0 is included.
internal fun Random.nextSignedUnit(): Double = nextDouble(-1.0, Math.nextUp(1.0))
