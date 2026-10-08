package com.dauren.monitor.feature.chartmonitor.ui

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dauren.monitor.core.chart.LiveChart
import com.dauren.monitor.core.common.model.PointBatch
import com.dauren.monitor.core.common.model.Points
import com.dauren.monitor.core.ui.mvi.BaseUIEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun ChartMonitorRoute(viewModel: ChartMonitorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChartMonitorScreen(
        state = state,
        batches = viewModel.chartBatches,
        history = viewModel::history,
        onUIEvent = viewModel::onUIEvent,
    )
}

@Composable
internal fun ChartMonitorScreen(
    state: ChartMonitorState,
    batches: SharedFlow<PointBatch>,
    history: (String) -> Points,
    onUIEvent: (BaseUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val chartContent: @Composable (Modifier) -> Unit = { m ->
        ChartSection(state = state, batches = batches, history = history, onUIEvent = onUIEvent, modifier = m)
    }
    val listContent: @Composable (Modifier) -> Unit = { m ->
        GeneratorList(items = state.items, onUIEvent = onUIEvent, modifier = m)
    }

    if (isLandscape) {
        Row(modifier.fillMaxSize()) {
            chartContent(Modifier.weight(0.6f).fillMaxSize())
            listContent(Modifier.weight(0.4f).fillMaxSize())
        }
    } else {
        Column(modifier.fillMaxSize()) {
            chartContent(Modifier.weight(0.55f).fillMaxSize())
            listContent(Modifier.weight(0.45f).fillMaxSize())
        }
    }
}

@Composable
private fun ChartSection(
    state: ChartMonitorState,
    batches: SharedFlow<PointBatch>,
    history: (String) -> Points,
    onUIEvent: (BaseUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val backgroundArgb = if (isDark) MaterialTheme.colorScheme.surface.toArgb() else Color.White.toArgb()

    LiveChart(
        sources = state.sources,
        visibleIds = state.visibleIds,
        activeVisibleIds = state.visibleIds,
        followLive = state.followLive,
        batches = batches,
        history = history,
        onUserNavigated = { onUIEvent(ChartMonitorUIEvent.ChartNavigatedByUser) },
        isDark = isDark,
        surfaceBackgroundArgb = backgroundArgb,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun GeneratorList(
    items: ImmutableList<GeneratorItemUi>,
    onUIEvent: (BaseUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.testTag(TEST_TAG_GENERATOR_LIST)) {
        items(items = items, key = { it.id }, contentType = { "generator" }) { item ->
            GeneratorRow(
                item = item,
                onToggle = { onUIEvent(ChartMonitorUIEvent.ToggleVisibility(it)) },
                modifier = Modifier.animateItem(),
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 40.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
internal fun GeneratorRow(
    item: GeneratorItemUi,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetAlpha = if (item.isFinished) 0.5f else 1f
    val alpha by animateFloatAsState(targetValue = targetAlpha, label = "generator_row_alpha")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .toggleable(
                value = item.isChecked,
                enabled = !item.isFinished,
                role = Role.Checkbox,
                onValueChange = { onToggle(item.id) },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).background(Color(item.colorArgb), CircleShape))
        Spacer(Modifier.width(16.dp))
        Text(
            text = item.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.width(82.dp),
        ) {
            Text(
                text = item.valueText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.End,
            )
            Text(
                text = item.timerText,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(4.dp))
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = null,
            enabled = !item.isFinished,
        )
    }
}

const val TEST_TAG_GENERATOR_LIST = "generator_list"
