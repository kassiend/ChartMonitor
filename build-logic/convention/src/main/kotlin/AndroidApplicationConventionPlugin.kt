import com.android.build.api.dsl.ApplicationExtension
import com.dauren.monitor.buildlogic.configureKotlinAndroid
import com.dauren.monitor.buildlogic.libs
import com.dauren.monitor.buildlogic.readSciChartLicense
import com.dauren.monitor.buildlogic.suppressKotlinCompileWarnings
import com.dauren.monitor.buildlogic.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            apply("monitor.quality")
            apply("com.google.devtools.ksp")
            apply("com.google.dagger.hilt.android")
        }

        dependencies {
            add("implementation", libs.findLibrary("hilt-android").get())
            add("ksp", libs.findLibrary("hilt-compiler").get())
        }

        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)

            defaultConfig {
                targetSdk = libs.versionInt("targetSdk")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                vectorDrawables.useSupportLibrary = true

                val license = readSciChartLicense()
                buildConfigField("String", "SCICHART_LICENSE", "\"$license\"")
            }

            buildFeatures.buildConfig = true

            buildTypes {
                getByName("release") {
                    isMinifyEnabled = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                }
            }
        }

        suppressKotlinCompileWarnings()
    }
}
