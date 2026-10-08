plugins {
    alias(libs.plugins.monitor.android.benchmark)
}

android {
    namespace = "com.dauren.monitor.benchmark"
    targetProjectPath = ":app"
}
// All instrumentation deps (uiautomator, benchmark-macro-junit4, androidx.test.*) are added by
// the `monitor.android.benchmark` convention plugin — nothing module-specific to declare here.
