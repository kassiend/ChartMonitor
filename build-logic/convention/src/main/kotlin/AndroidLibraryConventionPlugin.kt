import com.android.build.api.dsl.LibraryExtension
import com.dauren.monitor.buildlogic.configureKotestForUnitTests
import com.dauren.monitor.buildlogic.configureKotlinAndroid
import com.dauren.monitor.buildlogic.libs
import com.dauren.monitor.buildlogic.suppressKotlinCompileWarnings
import com.dauren.monitor.buildlogic.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.library")
            apply("monitor.quality")
        }

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)

            defaultConfig {
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
        }

        configureKotestForUnitTests()
        suppressKotlinCompileWarnings()
    }
}
