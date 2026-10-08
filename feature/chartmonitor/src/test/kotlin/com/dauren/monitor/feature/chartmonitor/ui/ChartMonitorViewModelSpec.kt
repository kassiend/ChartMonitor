package com.dauren.monitor.feature.chartmonitor.ui

import app.cash.turbine.test
import com.dauren.monitor.core.testing.coroutines.withTestMain
import com.dauren.monitor.core.testing.time.TestMonotonicClock
import com.dauren.monitor.core.testing.turbine.awaitItemMatching
import com.dauren.monitor.feature.chartmonitor.fake.FakeSignalRepository
import com.dauren.monitor.feature.chartmonitor.fake.activeSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ChartMonitorViewModelSpec :
    FunSpec({

        test("finishing a generator moves it to the end and unchecks it") {
            runTest {
                withTestMain(testScheduler) {
                    val clock = TestMonotonicClock(testScheduler)
                    val repository = FakeSignalRepository(
                        activeSource("a", deadlineMs = 2_000),
                        activeSource("b", deadlineMs = 5_000),
                    )
                    val viewModel = ChartMonitorViewModel(
                        repository = repository,
                        clock = clock,
                        dispatcher = StandardTestDispatcher(testScheduler),
                    )

                    viewModel.uiState.test {
                        val initial = awaitItemMatching { it.items.isNotEmpty() }
                        initial.items.map { it.id } shouldContainExactly listOf("a", "b")
                        initial.items.forEach { it.isChecked shouldBe true }

                        repository.finish("a")

                        val afterFinish = awaitItemMatching { it.items.last().id == "a" }
                        afterFinish.items.last().isChecked shouldBe false
                        afterFinish.items.last().isFinished shouldBe true

                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }
        }

        test("navigating the chart by user flips followLive off") {
            runTest {
                withTestMain(testScheduler) {
                    val clock = TestMonotonicClock(testScheduler)
                    val repository = FakeSignalRepository(activeSource("a", deadlineMs = 10_000))
                    val viewModel = ChartMonitorViewModel(
                        repository = repository,
                        clock = clock,
                        dispatcher = StandardTestDispatcher(testScheduler),
                    )

                    viewModel.uiState.test {
                        awaitItemMatching { it.items.isNotEmpty() }.followLive shouldBe true

                        viewModel.onUIEvent(ChartMonitorUIEvent.ChartNavigatedByUser)

                        awaitItemMatching { !it.followLive }.followLive shouldBe false
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }
        }
    })
