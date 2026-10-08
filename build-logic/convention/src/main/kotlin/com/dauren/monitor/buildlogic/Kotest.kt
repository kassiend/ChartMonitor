package com.dauren.monitor.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureKotestForUnitTests() {
    dependencies {
        add("testImplementation", libs.findBundle("kotest").get())
    }
}
