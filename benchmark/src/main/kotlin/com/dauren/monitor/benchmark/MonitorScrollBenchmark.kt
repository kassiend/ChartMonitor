package com.dauren.monitor.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** FrameTimingMetric over list scroll + chart pan. Needs a device or HW-accelerated emulator. */
@RunWith(AndroidJUnit4::class)
class MonitorScrollBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun scrollWithBaselineProfile() = scroll(CompilationMode.Partial())

    @Test
    fun scrollWithoutCompilation() = scroll(CompilationMode.None())

    private fun scroll(mode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = mode,
        startupMode = StartupMode.WARM,
        iterations = ITERATIONS,
        setupBlock = { startActivityAndWait() },
    ) {
        val list = device.findObject(By.res(TAG_GENERATOR_LIST))
        list?.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
        device.wait(Until.hasObject(By.res(TAG_GENERATOR_LIST)), UI_TIMEOUT_MS)

        repeat(SCROLL_ITERATIONS) {
            list?.fling(Direction.DOWN)
            list?.fling(Direction.UP)
        }
        device.findObject(By.res(TAG_LIVE_CHART))?.swipe(Direction.LEFT, PAN_FRACTION)
    }

    private companion object {
        const val ITERATIONS = 10
    }
}
