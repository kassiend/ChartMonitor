package com.dauren.monitor.feature.chartmonitor.data.repository

import com.dauren.monitor.core.testing.time.TestMonotonicClock
import com.dauren.monitor.feature.chartmonitor.data.source.GeneratorConfig
import com.dauren.monitor.feature.chartmonitor.data.source.GeneratorSourceFactory
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryStressSpec :
    FunSpec({

        test("history stays capped at historyCapacity when 10 sources each produce 180_000 points") {
            runTest {
                val clock = TestMonotonicClock(testScheduler)
                val appScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

                val factory = GeneratorSourceFactory(
                    config = GeneratorConfig(
                        // Fix lifetime at 30 min for all generators to make the invariant deterministic.
                        lifetime = 30.minutes..30.minutes,
                        // Accelerate x100: emit a point every 10 ms virtual → 180_000 points per source over 30 min.
                        interval = 10.milliseconds,
                        colorsArgb = (1..10).toList(),
                    ),
                    random = Random(1),
                    clock = clock,
                    wallClock = clock,
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

                val repository = DefaultSignalRepository(
                    factories = setOf(factory),
                    scope = appScope,
                    config = RepositoryConfig(historyCapacity = HISTORY_CAPACITY),
                ).apply { init() }

                testScheduler.advanceUntilIdle()

                // All generators finish on time.
                repository.sources.value.forAll { it.status shouldBe SourceStatus.Finished }

                // The ring buffer never exceeds its capacity, no matter how many points were produced.
                repository.sources.value.forAll { state ->
                    repository.history(state.info.id).size shouldBe HISTORY_CAPACITY
                }

                appScope.cancel()
            }
        }
    })

private const val HISTORY_CAPACITY = 100_000
