package com.dauren.monitor.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureAndroidCompose(commonExtension: CommonExtension) {
    commonExtension.apply {
        buildFeatures.compose = true
    }

    val bom = libs.findLibrary("androidx-compose-bom").get()
    dependencies {
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))
    }

    // Compose compiler reports / metrics via `-PcomposeReports`.
    val composeReports = providers.gradleProperty("composeReports").map { it.toBoolean() }.orElse(false)
    if (composeReports.get()) {
        val reportsDir = layout.buildDirectory.dir("compose_metrics").get().asFile.absolutePath
        extensions.configure(org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension::class.java) {
            reportsDestination.set(layout.buildDirectory.dir("compose_metrics"))
            metricsDestination.set(layout.buildDirectory.dir("compose_metrics"))
            val stabilityFile = rootProject.file("compose_stability.conf")
            if (stabilityFile.exists()) {
                stabilityConfigurationFiles.add(layout.file(provider { stabilityFile }))
            }
        }
    } else {
        // Stability config always applied, even without the reports flag.
        val stabilityFile = rootProject.file("compose_stability.conf")
        if (stabilityFile.exists()) {
            extensions.configure(org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension::class.java) {
                stabilityConfigurationFiles.add(layout.file(provider { stabilityFile }))
            }
        }
    }
}
