plugins {
    alias(libs.plugins.monitor.android.test)
}

android {
    namespace = "com.dauren.monitor.core.testing"
}

dependencies {
    api(projects.core.common)
}
