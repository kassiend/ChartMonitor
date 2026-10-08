package com.dauren.monitor.core.ui.color

import androidx.compose.ui.graphics.Color

/**
 * Hue rotation by the golden angle (~137.508°) produces maximally-spaced distinct colors for any N.
 * Saturation and value stay fixed so colors read as a cohesive palette on both light and dark surfaces.
 */
object SeriesPalette {
    private const val GOLDEN_ANGLE = 137.508f
    private const val SATURATION = 0.72f
    private const val VALUE = 0.82f
    private const val START_HUE = 15f

    fun colorAt(index: Int): Color {
        val hue = ((START_HUE + index * GOLDEN_ANGLE) % 360f + 360f) % 360f
        return Color.hsv(hue, SATURATION, VALUE)
    }

    fun generate(count: Int): List<Color> = List(count) { colorAt(it) }
}
