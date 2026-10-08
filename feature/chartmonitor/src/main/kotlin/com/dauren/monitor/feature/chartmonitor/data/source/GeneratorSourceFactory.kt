package com.dauren.monitor.feature.chartmonitor.data.source

import com.dauren.monitor.core.common.coroutines.ChartMonitorDispatchers
import com.dauren.monitor.core.common.coroutines.Dispatcher
import com.dauren.monitor.core.common.model.GeneratorInfo
import com.dauren.monitor.core.common.time.MonotonicClock
import com.dauren.monitor.core.common.time.WallClock
import com.dauren.monitor.feature.chartmonitor.domain.source.SignalSource
import com.dauren.monitor.feature.chartmonitor.domain.source.SignalSourceFactory
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Colors are ARGB Int (not Compose Color) so this layer stays Compose-free.
data class GeneratorConfig(
    val count: Int = DEFAULT_COUNT,
    val lifetime: ClosedRange<Duration> = 1.minutes..30.minutes,
    val interval: Duration = 1.seconds,
    val colorsArgb: List<Int>,
) {
    init {
        require(count > 0) { "count must be positive, was $count" }
        require(colorsArgb.size >= count) {
            "need at least $count colors, got ${colorsArgb.size}"
        }
        require(colorsArgb.take(count).toSet().size == count) {
            "colorsArgb[0..$count) must be unique"
        }
        require(lifetime.start.isPositive()) {
            "lifetime.start must be positive, was ${lifetime.start}"
        }
        require(lifetime.start <= lifetime.endInclusive) {
            "lifetime.start (${lifetime.start}) > lifetime.endInclusive (${lifetime.endInclusive})"
        }
        require(interval.isPositive()) { "interval must be positive, was $interval" }
    }

    companion object {
        const val DEFAULT_COUNT = 10
    }
}

internal class GeneratorSourceFactory @Inject constructor(
    private val config: GeneratorConfig,
    private val random: Random,
    private val clock: MonotonicClock,
    private val wallClock: WallClock,
    @Dispatcher(ChartMonitorDispatchers.Default) private val dispatcher: CoroutineDispatcher,
) : SignalSourceFactory {

    override fun create(): List<SignalSource> = List(config.count) { index ->
        val number = index + 1
        RandomWalkSignalSource(
            info = GeneratorInfo(
                id = "generator-$number",
                name = "Generator #$number",
                colorArgb = config.colorsArgb[index],
            ),
            initialValue = random.nextSignedUnit(),
            lifetime = random.nextLong(
                config.lifetime.start.inWholeMilliseconds,
                config.lifetime.endInclusive.inWholeMilliseconds + 1,
            ).milliseconds,
            interval = config.interval,
            random = random,
            clock = clock,
            wallClock = wallClock,
            dispatcher = dispatcher,
        )
    }
}
