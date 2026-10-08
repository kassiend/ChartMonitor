package com.dauren.monitor.feature.chartmonitor.data.source

import com.dauren.monitor.core.common.model.GeneratorInfo
import com.dauren.monitor.core.testing.time.TestMonotonicClock
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RandomWalkSignalSourceSpec :
    FunSpec({

        test("first emitted point equals the initial value") {
            runTest {
                val source = generator(testScheduler, initial = 0.42)

                source.points().first().value shouldBe 0.42
            }
        }

        test("every step delta stays within [-1, 1]") {
            checkAll(50, Arb.long()) { seed ->
                runTest {
                    val points =
                        generator(
                            scheduler = testScheduler,
                            lifetime = 5.minutes,
                            seed = seed,
                        ).points().toList()

                    points
                        .zipWithNext { a, b -> abs(b.value - a.value) }
                        .forAll { (it <= 1.0) shouldBe true }
                }
            }
        }

        test("emits exactly one point per second without drift over 30 minutes") {
            runTest {
                val points = generator(testScheduler, lifetime = 30.minutes).points().toList()

                points shouldHaveSize 1_800
                points
                    .zipWithNext { a, b -> b.timestampMs - a.timestampMs }
                    .toSet() shouldBe setOf(1_000L)
            }
        }

        test("stops at the deadline and reports Finished status") {
            runTest {
                val source = generator(testScheduler, lifetime = 90.seconds)
                source.status.value.shouldBeInstanceOf<SourceStatus.Active>()

                source.points().toList() shouldHaveSize 90

                testScheduler.currentTime shouldBe 90_000L
                source.status.value shouldBe SourceStatus.Finished
            }
        }

        test("multiple generators run concurrently and independently") {
            runTest {
                val short = generator(testScheduler, lifetime = 1.minutes)
                val long = generator(testScheduler, lifetime = 2.minutes)

                val (a, b) =
                    coroutineScope {
                        awaitAll(
                            async { short.points().toList() },
                            async { long.points().toList() },
                        )
                    }

                a shouldHaveSize 60
                b shouldHaveSize 120
                // Concurrent: both ran over the same virtual window, max(60s, 120s) = 120s.
                testScheduler.currentTime shouldBe 120_000L
            }
        }
    })

@OptIn(ExperimentalCoroutinesApi::class)
private fun generator(
    scheduler: TestCoroutineScheduler,
    initial: Double = 0.0,
    lifetime: Duration = 1.minutes,
    seed: Long = 42,
): RandomWalkSignalSource {
    val clock = TestMonotonicClock(scheduler)
    return RandomWalkSignalSource(
        info = GeneratorInfo("g", "Generator #1", colorArgb = 0),
        initialValue = initial,
        lifetime = lifetime,
        interval = 1.seconds,
        random = Random(seed),
        clock = clock,
        wallClock = clock,
        dispatcher = StandardTestDispatcher(scheduler),
    )
}
