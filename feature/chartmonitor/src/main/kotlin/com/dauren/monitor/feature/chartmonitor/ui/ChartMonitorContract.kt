package com.dauren.monitor.feature.chartmonitor.ui

import androidx.compose.runtime.Immutable
import com.dauren.monitor.core.common.model.GeneratorInfo
import com.dauren.monitor.core.ui.mvi.BaseUIEffect
import com.dauren.monitor.core.ui.mvi.BaseUIEvent
import com.dauren.monitor.core.ui.mvi.BaseUIState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

// Chart points are kept out of state (rule N1) — they stream directly to LiveChart via a separate SharedFlow.
@Immutable
data class ChartMonitorState(
    val items: ImmutableList<GeneratorItemUi> = persistentListOf(),
    val sources: ImmutableList<GeneratorInfo> = persistentListOf(),
    val visibleIds: ImmutableSet<String> = persistentSetOf(),
    val followLive: Boolean = true,
) : BaseUIState

@Immutable
data class GeneratorItemUi(
    val id: String,
    val name: String,
    val colorArgb: Int,
    val timerText: String,
    val valueText: String,
    val isFinished: Boolean,
    val isChecked: Boolean,
)

sealed interface ChartMonitorUIEvent : BaseUIEvent {
    data class ToggleVisibility(val id: String) : ChartMonitorUIEvent
    data class SetFollowLive(val enabled: Boolean) : ChartMonitorUIEvent
    data object ChartNavigatedByUser : ChartMonitorUIEvent
}

sealed interface ChartMonitorUIEffect : BaseUIEffect
