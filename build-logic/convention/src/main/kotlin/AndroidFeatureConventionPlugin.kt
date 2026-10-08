import com.dauren.monitor.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention for `feature:*` modules: Android library + Compose + Hilt + generic presentation
 * glue (lifecycle-compose, hilt-navigation-compose, immutable collections) + core:testing for
 * unit tests.
 *
 * Each feature owns its own data/domain/ui layers as package subdirs — the plugin does NOT wire
 * any shared domain/ui modules.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("monitor.android.library")
            apply("monitor.compose")
            apply("com.google.devtools.ksp")
            apply("com.google.dagger.hilt.android")
        }

        dependencies {
            add("implementation", libs.findLibrary("hilt-android").get())
            add("ksp", libs.findLibrary("hilt-compiler").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
            add("implementation", libs.findLibrary("kotlinx-collections-immutable").get())

            add("testImplementation", project(":core:testing"))
        }
    }
}
