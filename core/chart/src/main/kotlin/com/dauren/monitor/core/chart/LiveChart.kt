package com.dauren.monitor.core.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.dauren.monitor.core.common.model.GeneratorInfo
import com.dauren.monitor.core.common.model.PointBatch
import com.dauren.monitor.core.common.model.Points
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.withContext

/**
 * `onSubscription` snapshots [history] off-main before the first append, so no live point is lost
 * between restore and the subscription becoming active. Intentionally not skippable — owns an
 * AndroidView.
 */
@Suppress("LongParameterList")
@Composable
fun LiveChart(
    sources: ImmutableList<GeneratorInfo>,
    visibleIds: ImmutableSet<String>,
    activeVisibleIds: ImmutableSet<String>,
    followLive: Boolean,
    batches: SharedFlow<PointBatch>,
    history: (String) -> Points,
    onUserNavigated: () -> Unit,
    isDark: Boolean,
    surfaceBackgroundArgb: Int,
    modifier: Modifier = Modifier,
) {
    val controller = remember { ChartController() }
    val context = LocalContext.current
    val currentOnUserNavigated by rememberUpdatedState(onUserNavigated)
    val currentHistory by rememberUpdatedState(history)
    val currentSources by rememberUpdatedState(sources)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    AndroidView(
        factory = { ctx -> controller.attach(ctx) { currentOnUserNavigated() } },
        onRelease = { controller.detach() },
        modifier = modifier.testTag(TEST_TAG_LIVE_CHART),
    )

    LaunchedEffect(controller, sources) {
        if (sources.isNotEmpty()) controller.syncSources(context, sources)
    }

    LaunchedEffect(controller, isDark, surfaceBackgroundArgb) {
        controller.applyChartTheme(context, isDark, surfaceBackgroundArgb)
    }

    LaunchedEffect(controller, visibleIds, activeVisibleIds, followLive) {
        controller.applyViewState(visibleIds, activeVisibleIds, followLive)
    }

    LaunchedEffect(controller, batches) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            batches
                .onSubscription {
                    val snapshot = withContext(Dispatchers.Default) {
                        currentSources.associate { it.id to currentHistory(it.id) }
                    }
                    controller.restore(snapshot)
                }
                .collect(controller::append)
        }
    }
}

const val TEST_TAG_LIVE_CHART = "live_chart"
