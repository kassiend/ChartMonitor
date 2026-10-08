import com.diffplug.gradle.spotless.SpotlessExtension
import com.dauren.monitor.buildlogic.libs
import com.dauren.monitor.buildlogic.version
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Spotless (ktlint) + Detekt. Applied by all library / app convention plugins.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.diffplug.spotless")
            apply("io.gitlab.arturbosch.detekt")
        }

        val buildDirPath = layout.buildDirectory.get().asFile.absolutePath
        extensions.configure<SpotlessExtension> {
            kotlin {
                target("src/**/*.kt")
                targetExclude("$buildDirPath/**/*.kt")
                ktlint("1.5.0")
                    .editorConfigOverride(mapOf("ktlint_standard_filename" to "disabled"))
            }
            kotlinGradle {
                target("*.gradle.kts")
                ktlint("1.5.0")
            }
        }

        extensions.configure<DetektExtension> {
            parallel = true
            buildUponDefaultConfig = true
            allRules = false
            autoCorrect = false
            val sharedConfig = rootProject.file("config/detekt/detekt.yml")
            if (sharedConfig.exists()) {
                config.setFrom(sharedConfig)
            }
            val baseline = file("config/detekt/baseline.xml")
            if (baseline.exists()) this.baseline = baseline
        }
    }
}
