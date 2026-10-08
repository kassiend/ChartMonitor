package com.dauren.monitor.core.testing.time

import com.dauren.monitor.core.common.time.MonotonicClock
import com.dauren.monitor.core.common.time.WallClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler

/** Clock driven by the test scheduler — serves both monotonic and wall-clock reads. */
@OptIn(ExperimentalCoroutinesApi::class)
class TestMonotonicClock(
    private val scheduler: TestCoroutineScheduler,
) : MonotonicClock,
    WallClock {

    override fun nowMs(): Long = scheduler.currentTime

    override fun nowEpochMs(): Long = EPOCH_START + scheduler.currentTime

    companion object {
        /** Arbitrary epoch anchor so wall-clock timestamps look reasonable in tests. */
        const val EPOCH_START = 1_700_000_000_000L
    }
}
