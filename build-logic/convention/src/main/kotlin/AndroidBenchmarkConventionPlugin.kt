import com.android.build.api.dsl.TestExtension
import com.dauren.monitor.buildlogic.configureKotlinAndroid
import com.dauren.monitor.buildlogic.libs
import com.dauren.monitor.buildlogic.suppressKotlinCompileWarnings
import com.dauren.monitor.buildlogic.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Macrobenchmark + Baseline Profile module. */
class AndroidBenchmarkConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.test")
            apply("androidx.baselineprofile")
        }

        extensions.configure<TestExtension> {
            configureKotlinAndroid(this)
            defaultConfig {
                targetSdk = libs.versionInt("targetSdk")
            }
            buildTypes {
                create("benchmark") {
                    isDebuggable = true
                    signingConfig = getByName("debug").signingConfig
                    matchingFallbacks += listOf("release")
                }
            }
        }

        dependencies {
            add("implementation", libs.findLibrary("androidx-junit").get())
            add("implementation", libs.findLibrary("androidx-espresso-core").get())
            add("implementation", libs.findLibrary("androidx-uiautomator").get())
            add("implementation", libs.findLibrary("androidx-benchmark-macro-junit4").get())
        }

        suppressKotlinCompileWarnings()
    }
}
