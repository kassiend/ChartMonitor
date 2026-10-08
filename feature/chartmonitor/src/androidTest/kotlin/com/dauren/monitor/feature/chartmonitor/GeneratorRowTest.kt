package com.dauren.monitor.feature.chartmonitor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dauren.monitor.core.ui.theme.ChartMonitorTheme
import com.dauren.monitor.feature.chartmonitor.ui.ChartMonitorRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GeneratorRowTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun row_shows_name_timer_value_and_toggles_on_click() {
        var toggled: String? = null
        compose.setContent {
            ChartMonitorTheme {
                com.dauren.monitor.feature.chartmonitor.ui.GeneratorRow(
                    item = com.dauren.monitor.feature.chartmonitor.ui.GeneratorItemUi(
                        id = "g1",
                        name = "Generator #1",
                        colorArgb = 0xFF1F77B4.toInt(),
                        timerText = "29:59",
                        valueText = "+0.42",
                        isFinished = false,
                        isChecked = true,
                    ),
                    onToggle = { toggled = it },
                )
            }
        }

        compose.onNodeWithText("Generator #1").assertIsDisplayed()
        compose.onNodeWithText("29:59").assertIsDisplayed()
        compose.onNodeWithText("+0.42").assertIsDisplayed()
        compose.onNode(isToggleable()).assertIsOn().performClick()
        assertEquals("g1", toggled)
    }
}

@Suppress("unused")
private val keepImportAlive = ChartMonitorRoute::class
