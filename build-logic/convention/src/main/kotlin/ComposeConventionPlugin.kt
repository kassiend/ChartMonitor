import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.dauren.monitor.buildlogic.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Apply AFTER `monitor.android.application` or `monitor.android.library`. */
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.findByType(ApplicationExtension::class.java)?.let { configureAndroidCompose(it) }
            extensions.findByType(LibraryExtension::class.java)?.let { configureAndroidCompose(it) }
        }
    }
}
