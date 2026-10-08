import com.android.build.api.dsl.LibraryExtension
import com.dauren.monitor.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention for `core:testing`: Android library + test-only helpers (fakes, TestClocks, withTestMain).
 * Dependencies exposed as `api(...)` so consumers of core:testing get Kotest / Turbine / coroutines-test transitively.
 */
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("monitor.android.library")

        extensions.configure<LibraryExtension> {
            defaultConfig.consumerProguardFiles("consumer-rules.pro")
        }

        dependencies {
            add("api", libs.findLibrary("kotlinx-coroutines-core").get())
            add("api", libs.findLibrary("kotlinx-coroutines-test").get())
            add("api", libs.findLibrary("turbine").get())
            add("api", libs.findLibrary("mockk").get())
            add("api", libs.findLibrary("kotest-runner-junit5").get())
            add("api", libs.findLibrary("kotest-assertions-core").get())
            add("api", libs.findLibrary("kotest-property").get())
            add("api", libs.findLibrary("junit").get())
        }
    }
}
