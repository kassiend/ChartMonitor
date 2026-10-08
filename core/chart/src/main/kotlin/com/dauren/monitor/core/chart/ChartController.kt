package com.dauren.monitor.core.chart

import android.content.Context
import android.view.Gravity
import com.dauren.monitor.core.common.model.GeneratorInfo
import com.dauren.monitor.core.common.model.PointBatch
import com.dauren.monitor.core.common.model.Points
import com.scichart.charting.ClipMode
import com.scichart.charting.Direction2D
import com.scichart.charting.model.dataSeries.XyDataSeries
import com.scichart.charting.modifiers.LegendModifier
import com.scichart.charting.modifiers.ModifierGroup
import com.scichart.charting.modifiers.PinchZoomModifier
import com.scichart.charting.modifiers.SourceMode
import com.scichart.charting.modifiers.ZoomPanModifier
import com.scichart.charting.themes.ThemeManager
import com.scichart.charting.visuals.SciChartSurface
import com.scichart.charting.visuals.annotations.AnnotationLabel
import com.scichart.charting.visuals.annotations.HorizontalLineAnnotation
import com.scichart.charting.visuals.annotations.LabelPlacement
import com.scichart.charting.visuals.axes.AutoRange
import com.scichart.charting.visuals.axes.AxisAlignment
import com.scichart.charting.visuals.axes.DateAxis
import com.scichart.charting.visuals.axes.NumericAxis
import com.scichart.charting.visuals.renderableSeries.FastLineRenderableSeries
import com.scichart.core.annotations.Orientation
import com.scichart.core.framework.UpdateSuspender
import com.scichart.data.model.DoubleRange
import com.scichart.drawing.common.SolidBrushStyle
import com.scichart.drawing.common.SolidPenStyle
import java.util.Date
import com.scichart.charting.R as SciChartR

/**
 * Owns the SciChart surface, axes, series and markers. Not a Composable — instantiate once via
 * `remember { ChartController() }`. All writes go through a single `UpdateSuspender.using` per
 * call, so one tick = one redraw.
 */
class ChartController(private val config: ChartConfig = ChartConfig()) {

    private var surface: SciChartSurface? = null
    private var xAxis: DateAxis? = null
    private val series: MutableMap<String, SeriesHolder> = linkedMapOf()
    private val lastTimestamp: MutableMap<String, Long> = HashMap()

    private var followLive: Boolean = true

    // Guards our own programmatic range updates from being misinterpreted as user navigation.
    private var applyingProgrammaticRange: Boolean = false
    private var onUserNavigated: () -> Unit = {}

    internal class SeriesHolder(
        val data: XyDataSeries<Date, Double>,
        val renderable: FastLineRenderableSeries,
        val marker: HorizontalLineAnnotation,
    )

    /**
     * Creates surface, axes and modifiers. Series are added later via [syncSources] — AndroidView.factory
     * runs on first composition when the sources list is still the empty initial state.
     */
    fun attach(
        context: Context,
        onUserNavigated: () -> Unit = {},
    ): SciChartSurface {
        check(surface == null) { "ChartController is already attached" }
        this.onUserNavigated = onUserNavigated
        val newSurface = SciChartSurface(context).also { this.surface = it }

        val newXAxis = DateAxis(context).apply {
            autoRange = AutoRange.Never
            axisTitle = AXIS_TITLE_TIME
        }
        this.xAxis = newXAxis

        val yAxis = NumericAxis(context).apply {
            autoRange = AutoRange.Always
            growBy = DoubleRange(config.yPadding, config.yPadding)
            axisAlignment = AxisAlignment.Right
            axisTitle = AXIS_TITLE_VALUE
        }

        UpdateSuspender.using(newSurface) {
            newSurface.xAxes.add(newXAxis)
            newSurface.yAxes.add(yAxis)
            newSurface.chartModifiers.add(buildModifiers(context))

            // Without an explicit range DateAxis spans decades and renders date labels; seed to
            // [now, now + defaultWindow] so the axis is HH:mm:ss from the start.
            val now = System.currentTimeMillis()
            val windowMs = config.defaultWindow.inWholeMilliseconds
            newXAxis.visibleRange?.setMinMaxDouble(now.toDouble(), (now + windowMs).toDouble())
        }

        newXAxis.setVisibleRangeChangeListener { _, _, _, _ ->
            if (!applyingProgrammaticRange && followLive) {
                followLive = false
                this.onUserNavigated()
            }
        }
        return newSurface
    }

    /** Idempotent — safe to call on every state emission. */
    fun syncSources(context: Context, sources: List<GeneratorInfo>) {
        val currentSurface = surface ?: return
        if (sources.all { it.id in series }) return
        UpdateSuspender.using(currentSurface) {
            sources.forEach { info -> if (info.id !in series) addSeriesAndMarker(context, currentSurface, info) }
        }
    }

    /** Appends only points strictly newer than `lastTimestamp[id]` — dedups overlap after a restore. */
    fun append(batch: PointBatch) {
        val currentSurface = surface ?: return
        UpdateSuspender.using(currentSurface) {
            var newest = Long.MIN_VALUE
            batch.byId.forEach { (id, points) ->
                val holder = series[id] ?: return@forEach
                val fresh = points.after(lastTimestamp[id] ?: Long.MIN_VALUE)
                if (fresh.size == 0) return@forEach
                holder.data.append(fresh.toDateValues(), fresh.toDoubleValues())
                val lastTs = fresh.lastTimestampMs!!
                lastTimestamp[id] = lastTs
                holder.marker.x1 = Date(lastTs)
                holder.marker.y1 = fresh.values[fresh.size - 1]
                if (lastTs > newest) newest = lastTs
            }
            if (followLive && newest != Long.MIN_VALUE) followTo(newest)
        }
    }

    /** Replays a history snapshot after rotation. Subsequent live overlap is deduped by `lastTimestamp`. */
    fun restore(history: Map<String, Points>) {
        val currentSurface = surface ?: return
        UpdateSuspender.using(currentSurface) {
            history.forEach { (id, points) ->
                val holder = series[id] ?: return@forEach
                holder.data.clear()
                if (points.size > 0) {
                    holder.data.append(points.toDateValues(), points.toDoubleValues())
                    val lastTs = points.lastTimestampMs!!
                    lastTimestamp[id] = lastTs
                    holder.marker.x1 = Date(lastTs)
                    holder.marker.y1 = points.values[points.size - 1]
                } else {
                    lastTimestamp.remove(id)
                }
            }
            if (followLive) {
                val newest = history.values.mapNotNull { it.lastTimestampMs }.maxOrNull()
                if (newest != null) followTo(newest)
            }
        }
    }

    fun setVisible(visibleIds: Set<String>) {
        val currentSurface = surface ?: return
        UpdateSuspender.using(currentSurface) {
            series.forEach { (id, holder) -> holder.renderable.isVisible = id in visibleIds }
        }
    }

    fun setActive(activeVisibleIds: Set<String>) {
        val currentSurface = surface ?: return
        UpdateSuspender.using(currentSurface) {
            series.forEach { (id, holder) -> holder.marker.setIsHidden(id !in activeVisibleIds) }
        }
    }

    fun setFollowLive(enabled: Boolean) {
        followLive = enabled
        if (enabled) {
            val newest = lastTimestamp.values.maxOrNull() ?: return
            val currentSurface = surface ?: return
            UpdateSuspender.using(currentSurface) { followTo(newest) }
        }
    }

    /** Prefer over calling set* back-to-back — one redraw instead of three. */
    fun applyViewState(
        visibleIds: Set<String>,
        activeVisibleIds: Set<String>,
        followLive: Boolean,
    ) {
        val currentSurface = surface ?: return
        this.followLive = followLive
        UpdateSuspender.using(currentSurface) {
            series.forEach { (id, holder) ->
                holder.renderable.isVisible = id in visibleIds
                holder.marker.setIsHidden(id !in activeVisibleIds)
            }
            if (followLive) {
                val newest = lastTimestamp.values.maxOrNull()
                if (newest != null) followTo(newest)
            }
        }
    }

    fun detach() {
        xAxis?.setVisibleRangeChangeListener(null)
        xAxis = null
        surface = null
        series.clear()
        lastTimestamp.clear()
        onUserNavigated = {}
    }

    fun applyChartTheme(context: Context, isDark: Boolean, surfaceBackgroundArgb: Int) {
        val currentSurface = surface ?: return
        // ThemeManager.applyTheme resets some axis state — snapshot visibleRange and restore after.
        val range = xAxis?.visibleRange
        val savedMin = range?.minAsDouble
        val savedMax = range?.maxAsDouble

        val themeRes =
            if (isDark) {
                SciChartR.style.SciChart_SciChartv4DarkStyle
            } else {
                SciChartR.style.SciChart_ExpressionLightStyle
            }
        ThemeManager.applyTheme(currentSurface, themeRes, context)
        currentSurface.setBackgroundColor(surfaceBackgroundArgb)
        // Plot area (inside axes) has its own fill — Expression Light draws grey tiles there.
        currentSurface.renderableSeriesAreaFillStyle = SolidBrushStyle(surfaceBackgroundArgb)
        currentSurface.renderableSeriesAreaBorderStyle = SolidPenStyle(
            if (isDark) 0x33FFFFFF.toInt() else 0xFFE0E0E0.toInt(),
            true,
            1f,
            null,
        )
        overrideGridLines(isDark)

        if (savedMin != null && savedMax != null && range != null) {
            applyingProgrammaticRange = true
            try {
                range.setMinMaxDouble(savedMin, savedMax)
            } finally {
                applyingProgrammaticRange = false
            }
        }
    }

    private fun overrideGridLines(isDark: Boolean) {
        val majorColor = if (isDark) 0x33FFFFFF.toInt() else 0xFFEFEFEF.toInt()
        val minorColor = if (isDark) 0x1AFFFFFF.toInt() else 0x00FFFFFF
        val major = SolidPenStyle(majorColor, true, 1f, null)
        val minor = SolidPenStyle(minorColor, true, 1f, null)
        xAxis?.setMajorGridLineStyle(major)
        xAxis?.setMinorGridLineStyle(minor)
        surface?.yAxes?.firstOrNull()?.let {
            it.setMajorGridLineStyle(major)
            it.setMinorGridLineStyle(minor)
        }
    }

    // Must be called inside an active UpdateSuspender. SciChart fires the range-change listener
    // synchronously inside setMinMaxDouble, so applyingProgrammaticRange guards our own update.
    private fun followTo(newestMs: Long) {
        val axis = xAxis ?: return
        val range = axis.visibleRange ?: return
        val defaultWidth = config.defaultWindow.inWholeMilliseconds.toDouble()
        val currentWidth = range.maxAsDouble - range.minAsDouble
        // 0 = uninitialized; > 24h = user zoomed out past anything useful.
        val width = if (currentWidth in 1.0..MAX_SANE_WINDOW_MS) currentWidth else defaultWidth
        applyingProgrammaticRange = true
        try {
            range.setMinMaxDouble(
                newestMs - width * FOLLOW_LEFT_FRACTION,
                newestMs + width * FOLLOW_RIGHT_FRACTION,
            )
        } finally {
            applyingProgrammaticRange = false
        }
    }

    private fun addSeriesAndMarker(context: Context, target: SciChartSurface, info: GeneratorInfo) {
        val data = XyDataSeries(Date::class.java, Double::class.javaObjectType).apply {
            seriesName = info.name
            fifoCapacity = config.fifoCapacity
            acceptsUnsortedData = false
        }
        val renderable = FastLineRenderableSeries().apply {
            dataSeries = data
            strokeStyle = SolidPenStyle(info.colorArgb, true, config.lineThicknessPx, null)
        }
        target.renderableSeries.add(renderable)

        val marker = HorizontalLineAnnotation(context).apply {
            horizontalGravity = Gravity.RIGHT
            stroke = SolidPenStyle(info.colorArgb, true, 1f, floatArrayOf(DASH_ON_PX, DASH_OFF_PX))
            setIsEditable(false)
            setIsHidden(true)
            annotationLabels.add(
                AnnotationLabel(context).apply {
                    labelPlacement = LabelPlacement.Axis
                    setBackgroundColor(info.colorArgb)
                },
            )
        }
        target.annotations.add(marker)

        series[info.id] = SeriesHolder(data, renderable, marker)
    }

    private fun buildModifiers(context: Context) = ModifierGroup(
        ZoomPanModifier().apply {
            direction = Direction2D.XDirection
            // Clip at data extents: user can't scroll into empty past before first point or far future.
            clipModeX = ClipMode.ClipAtExtents
            zoomExtentsY = false
        },
        PinchZoomModifier().apply { direction = Direction2D.XDirection },
        LegendModifier(context).apply {
            setSourceMode(SourceMode.AllVisibleSeries)
            setShowCheckboxes(false)
            setOrientation(Orientation.HORIZONTAL)
            setLegendPosition(Gravity.TOP or Gravity.START, LEGEND_MARGIN_PX)
        },
    )

    private companion object {
        const val AXIS_TITLE_TIME = "Time"
        const val AXIS_TITLE_VALUE = "Value"
        const val LEGEND_MARGIN_PX = 0
        const val DASH_ON_PX = 6f
        const val DASH_OFF_PX = 4f
        const val MAX_SANE_WINDOW_MS = 24.0 * 60.0 * 60.0 * 1_000.0
        const val FOLLOW_LEFT_FRACTION = 0.95
        const val FOLLOW_RIGHT_FRACTION = 0.05
    }
}
