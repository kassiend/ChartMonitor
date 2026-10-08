plugins {
    alias(libs.plugins.monitor.android.library)
    alias(libs.plugins.monitor.compose)
}

android {
    namespace = "com.dauren.monitor.core.chart"
}

dependencies {
    implementation(projects.core.common)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.bundles.scichart)
}
