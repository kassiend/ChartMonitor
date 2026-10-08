---
name: scichart-chart
description: Use for any change in core:chart — adding/removing SciChart series, axes, annotations, modifiers, legend; chart lifecycle (attach / restore / detach); gesture configuration (pan, zoom, follow live). Enforces plan §4 ("Chart") and performance rules PERF-03, PERF-04.
---

# SciChart chart

**Source of truth:** `docs/plan.md` §2 (FR-CHART-01…07, PERF-03, PERF-04), §3 (M3, N1, N7, N8, N9), §4 ("Chart", "Two data streams").
**SciChart docs:** via `WebFetch` on `https://www.scichart.com/documentation/android/current/webframe.html` (**M11**). Verify Android 4.x API against the official docs — the code below is a reference.

**Trigger:** any change in `core:chart/*`, adding a series/marker/modifier, gesture changes.

**Rules:** M3, N1, N7, N8, N9.

## Controller — not a Composable

`ChartController` in `core:chart` is a plain class:
```kotlin
class ChartController(private val config: ChartConfig = ChartConfig()) {
    private var surface: SciChartSurface? = null
    private lateinit var xAxis: DateAxis
    private val series = LinkedHashMap<SourceId, SeriesHolder>()
    // ...
}
```

The Compose wrapper `LiveChart` is a thin `AndroidView`; `ChartController` is created in `remember { ChartController() }` **once** and reused.

## License (N9)

The key lives **only in `local.properties`** → `BuildConfig.SCICHART_LICENSE` (stage 04) → `SciChartSurface.setRuntimeLicenseKey(key)` **once in `Application.onCreate()`**, before any surface is created. **Never** hardcode as a string (**N9**). `git grep -n "setRuntimeLicenseKey(\""` must be empty.

## Axes, series, legend

```kotlin
// Y: auto-range over visible series inside the visible X window (FR-CHART-04)
val yAxis = NumericAxis(context).apply {
    autoRange = AutoRange.Always
    growBy = DoubleRange(config.yPadding, config.yPadding)   // 10 % per plan §9
    axisAlignment = AxisAlignment.Right
}

// X: manual pan + pinch-zoom only along X, no auto-range (FR-CHART-05, N8)
val xAxis = DateAxis(context).apply { autoRange = AutoRange.Never }

// One series per source
val data = XyDataSeries(Date::class.java, Double::class.javaObjectType).apply {
    seriesName = meta.name
    fifoCapacity = config.fifoCapacity                       // capped memory
    acceptsUnsortedData = false
}
val renderable = FastLineRenderableSeries().apply {
    dataSeries = data
    strokeStyle = SolidPenStyle(meta.colorArgb, true, config.lineThicknessPx, null)
}
```

Legend via `LegendModifier(context).setSourceMode(SourceMode.AllVisibleSeries)` — auto-driven by visibility (FR-CHART-03).

## Live batch = one redraw (M3, PERF-03)

```kotlin
fun append(batch: PointBatch) {
    val surface = surface ?: return
    UpdateSuspender.using(surface) {
        batch.points.forEach { (id, points) ->
            val holder = series[id] ?: return@forEach
            val fresh = points.after(lastTimestamp[id] ?: Long.MIN_VALUE)   // de-dup after restore
            if (fresh.size == 0) return@forEach
            holder.data.append(fresh.dateValues(), fresh.doubleValues())
            lastTimestamp[id] = fresh.lastTimestampMs!!
            holder.updateMarker(fresh)
        }
        if (followLive) followTo(...)
    }
}
```

**One `UpdateSuspender` per batch** — one redraw per tick. Never do `data.clear() + data.append(all)` on every tick — append only the delta.

## Visibility — via a flag, no recreate (N7)

```kotlin
fun setVisible(visibleIds: Set<SourceId>) {
    UpdateSuspender.using(surface ?: return) {
        series.forEach { (id, h) -> h.renderable.setIsVisible(id in visibleIds) }
    }
}
```

**Never** `renderableSeries.remove(...)` / `add(...)` when a checkbox flips — that is exactly N7.

## Last-value marker (FR-CHART-07)

`HorizontalLineAnnotation` per series, **created once** in `attach(...)`; afterwards only `.x1 = ...; .y1 = ...`. Hidden and finished ones become `.isHidden = true` through `setActive(activeVisibleIds)`.

## Follow live and gestures (FR-CHART-05, FR-CHART-06, N8)

```kotlin
ModifierGroup(
    ZoomPanModifier().apply { direction = Direction2D.XDirection; clipModeX = ClipMode.None; zoomExtentsY = false },
    PinchZoomModifier().apply { direction = Direction2D.XDirection },
    LegendModifier(context).apply { setSourceMode(SourceMode.AllVisibleSeries); setShowCheckboxes(false) },
)
```

Follow live shifts the window **without changing its width** — this is not auto-range on X. A manual gesture (`VisibleRangeChangeListener`) switches follow off and triggers `onUserNavigated()` → the VM receives `ChartNavigatedByUser`.

The `applyingProgrammaticRange` flag distinguishes our shift from a user shift.

## Restore after rotation

Subscribe to `batches` via `.onSubscription { snapshotHistory(); controller.restore(snapshot) }` — while we snapshot history, the subscription is already active, no points are dropped. History snapshot runs off the main thread (`withContext(Dispatchers.Default)`); duplicates are filtered by `lastTimestamp` per series.

## Checklist

- [ ] `SciChartSurface.setRuntimeLicenseKey` is called **once** in `Application.onCreate()`, no literal key in code (**N9**).
- [ ] Series and markers are created **once** in `attach(...)`, afterwards only updated (**N7**).
- [ ] One `UpdateSuspender.using(surface) { ... }` per `append(batch)` (**M3, PERF-03**).
- [ ] `AutoRange.Always` on Y, `AutoRange.Never` on X (**FR-CHART-04, N8**).
- [ ] `ZoomPanModifier` and `PinchZoomModifier` limited to `Direction2D.XDirection`.
- [ ] `followLive` turns off on the first manual gesture (`applyingProgrammaticRange = false`).
- [ ] `fifoCapacity` on `XyDataSeries` is set — otherwise the series grows unbounded.
- [ ] Compose wrapper creates the controller via `remember { ChartController() }` **once**.
