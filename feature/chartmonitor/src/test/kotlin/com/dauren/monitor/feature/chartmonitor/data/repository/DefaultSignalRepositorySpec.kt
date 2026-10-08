package com.dauren.monitor.feature.chartmonitor.data.repository

import app.cash.turbine.test
import com.dauren.monitor.core.common.model.GeneratorInfo
import com.dauren.monitor.feature.chartmonitor.domain.model.SignalPoint
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceStatus
import com.dauren.monitor.feature.chartmonitor.domain.source.SignalSource
import com.dauren.monitor.feature.chartmonitor.domain.source.SignalSourceFactory
import com.dauren.monitor.feature.chartmonitor.fake.FakeSignalSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSignalRepositorySpec :
    FunSpec({

        test("points from every registered factory arrive in one batch per tick") {
            runTest {
                val a = FakeSignalSource(info("a"))
                val b = FakeSignalSource(info("b"))
                val appScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
                val repository = repository(appScope, listOf(a), listOf(b))

                repository.batches.test {
                    repository.init()
                    a.emit(SignalPoint(1_000, 0.1))
                    b.emit(SignalPoint(1_000, 0.2))

                    awaitItem().byId.keys shouldContainExactlyInAnyOrder setOf("a", "b")
                    cancelAndIgnoreRemainingEvents()
                }

                appScope.cancel()
            }
        }

        test("history is available to a late subscriber") {
            runTest {
                val a = FakeSignalSource(info("a"))
                val appScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
                val repository = repository(appScope, listOf(a)).apply { init() }

                a.emit(SignalPoint(1_000, 0.1))
                a.emit(SignalPoint(2_000, 0.2))
                testScheduler.advanceUntilIdle()

                val history = repository.history("a")
                history.timestampsMs.toList() shouldBe listOf(1_000L, 2_000L)
                history.values.toList() shouldBe listOf(0.1, 0.2)

                appScope.cancel()
            }
        }

        test("finished source is reported as Finished") {
            runTest {
                val a = FakeSignalSource(info("a"))
                val appScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
                val repository = repository(appScope, listOf(a)).apply { init() }

                a.finish()
                testScheduler.advanceUntilIdle()

                repository.sources.value.single().status shouldBe SourceStatus.Finished

                appScope.cancel()
            }
        }

        test("init() is idempotent — second call does not duplicate points in history") {
            runTest {
                val a = FakeSignalSource(info("a"))
                val appScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
                val repository = repository(appScope, listOf(a))

                repository.init()
                repository.init()

                a.emit(SignalPoint(1_000, 0.5))
                testScheduler.advanceUntilIdle()

                // One point in — one point in history. A second collector would have doubled it.
                repository.history("a").values.toList() shouldBe listOf(0.5)

                appScope.cancel()
            }
        }
    })

private fun info(id: String) = GeneratorInfo(id, "Source $id", colorArgb = 0)

private fun repository(
    scope: CoroutineScope,
    vararg groups: List<SignalSource>,
) = DefaultSignalRepository(
    factories = groups.map { group -> SignalSourceFactory { group } }.toSet(),
    scope = scope,
    config = RepositoryConfig(historyCapacity = 100),
)
