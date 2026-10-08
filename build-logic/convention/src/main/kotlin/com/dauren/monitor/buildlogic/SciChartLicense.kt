package com.dauren.monitor.buildlogic

import org.gradle.api.Project
import java.util.Properties

/**
 * Reads `scichart.license` from `local.properties` (never committed). Returns empty string if absent.
 * Consumed by [AndroidApplicationConventionPlugin] → BuildConfig.SCICHART_LICENSE.
 */
internal fun Project.readSciChartLicense(): String {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return ""
    val props = Properties().apply { file.inputStream().use(::load) }
    return props.getProperty("scichart.license", "").orEmpty()
}
