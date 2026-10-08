plugins {
    `kotlin-dsl`
}

group = "com.dauren.monitor.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "monitor.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "monitor.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "monitor.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("compose") {
            id = "monitor.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("jvmLibrary") {
            id = "monitor.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidTest") {
            id = "monitor.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
        register("quality") {
            id = "monitor.quality"
            implementationClass = "QualityConventionPlugin"
        }
        register("androidBenchmark") {
            id = "monitor.android.benchmark"
            implementationClass = "AndroidBenchmarkConventionPlugin"
        }
    }
}
