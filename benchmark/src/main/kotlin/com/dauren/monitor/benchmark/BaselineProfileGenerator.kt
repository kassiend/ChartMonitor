package com.dauren.monitor.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Collects a Baseline Profile covering startup, list scroll and chart pan. Needs a rooted / userdebug device. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()

        device.wait(Until.hasObject(By.res(TAG_GENERATOR_LIST)), UI_TIMEOUT_MS)

        val list = device.findObject(By.res(TAG_GENERATOR_LIST))
        list?.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
        repeat(SCROLL_ITERATIONS) {
            list?.fling(Direction.DOWN)
            list?.fling(Direction.UP)
        }

        device.findObject(By.res(TAG_LIVE_CHART))?.swipe(Direction.LEFT, PAN_FRACTION)
    }
}
