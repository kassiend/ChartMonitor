import com.dauren.monitor.buildlogic.configureKotestForUnitTests
import com.dauren.monitor.buildlogic.configureKotlinJvm
import com.dauren.monitor.buildlogic.libs
import com.dauren.monitor.buildlogic.suppressKotlinCompileWarnings
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Pure Kotlin JVM library (core:domain, core:common). No Android, no Compose, no SciChart (N2).
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.jvm")
            apply("monitor.quality")
        }

        configureKotlinJvm()
        configureKotestForUnitTests()
        suppressKotlinCompileWarnings()

        dependencies {
            add("implementation", libs.findLibrary("kotlinx-coroutines-core").get())
        }
    }
}
