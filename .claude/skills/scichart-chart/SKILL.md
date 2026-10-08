---
name: scichart-chart
description: Use for any change in core:chart — adding/removing SciChart series, axes, annotations, modifiers, legend; chart lifecycle (attach / restore / detach); gesture configuration (pan, zoom, follow live).
---

# SciChart chart

**SciChart docs:** via `WebFetch` on `https://www.scichart.com/documentation/android/current/webframe.html`. Verify Android 4.x API against the official docs — the code below is a reference.

**Trigger:** any change in `core:chart/*`, adding a series/marker/modifier, gesture changes.

## Controller — not a Composable

`ChartController` in `core:chart` is a plain class:
```kotlin
class ChartController(private val config: ChartConfig = ChartConfig()) {
    private var surface: SciChartSurface? = null
    private lateinit var xAxis: DateAxis
    private val series = LinkedHashMap<String, SeriesHolder>()
    // ...
}
```

The Compose wrapper `LiveChart` is a thin `AndroidView`; `ChartController` is created in `remember { ChartController() }` **once** and reused.

## License

The key lives **only in `local.properties`** → `BuildConfig.SCICHART_LICENSE` → `SciChartSurface.setRuntimeLicenseKey(key)` **once in `Application.onCreate()`**, before any surface is created. Never hardcode as a string. `git grep -n "setRuntimeLicenseKey(\""` must be empty.

## Axes, series, legend

```kotlin
// Y: auto-range over visible series inside the visible X window
val yAxis = NumericAxis(context).apply {
    autoRange = AutoRange.Always
    growBy = DoubleRange(config.yPadding, config.yPadding)   // ~10 %
    axisAlignment = AxisAlignment.Right
}

// X: manual pan + pinch-zoom only along X, no auto-range
val xAxis = DateAxis(context).apply { autoRange = AutoRange.Never }

// One series per source
val data = XyDataSeries(Date::class.java, Double::class.javaObjectType).apply {
    seriesName = info.name
    fifoCapacity = config.fifoCapacity                       // capped memory
    acceptsUnsortedData = false
}
val renderable = FastLineRenderableSeries().apply {
    dataSeries = data
    strokeStyle = SolidPenStyle(info.colorArgb, true, config.lineThicknessPx, null)
}
```

Legend via `LegendModifier(context).setSourceMode(SourceMode.AllVisibleSeries)` — auto-driven by visibility.

## Live batch = one redraw

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

## Visibility — via a flag, no recreate

```kotlin
fun setVisible(visibleIds: Set<String>) {
    UpdateSuspender.using(surface ?: return) {
        series.forEach { (id, h) -> h.renderable.setIsVisible(id in visibleIds) }
    }
}
```

**Never** `renderableSeries.remove(...)` / `add(...)` when a checkbox flips. Series must be created once in `attach(...)` and only updated afterwards.

## Last-value marker

`HorizontalLineAnnotation` per series, **created once** in `attach(...)`; afterwards only `.x1 = ...; .y1 = ...`. Hidden and finished ones become `.isHidden = true` through `setActive(activeVisibleIds)`.

## Follow live and gestures

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

- [ ] `SciChartSurface.setRuntimeLicenseKey` is called **once** in `Application.onCreate()`, no literal key in code.
- [ ] Series and markers are created **once** in `attach(...)`, afterwards only updated.
- [ ] One `UpdateSuspender.using(surface) { ... }` per `append(batch)`.
- [ ] `AutoRange.Always` on Y, `AutoRange.Never` on X.
- [ ] `ZoomPanModifier` and `PinchZoomModifier` limited to `Direction2D.XDirection`.
- [ ] `followLive` turns off on the first manual gesture (`applyingProgrammaticRange = false`).
- [ ] `fifoCapacity` on `XyDataSeries` is set — otherwise the series grows unbounded.
- [ ] Compose wrapper creates the controller via `remember { ChartController() }` **once**.
